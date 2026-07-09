package com.ruoyi.system.service.impl;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.system.domain.DocumentChunk;
import com.ruoyi.system.domain.ProjectDocument;
import com.ruoyi.system.mapper.DocumentChunkMapper;
import com.ruoyi.system.mapper.ProjectDocumentMapper;
import com.ruoyi.system.service.IFileParseService;
import com.ruoyi.system.service.IEmbeddingService;
import com.ruoyi.system.service.IProjectDocService;
import com.ruoyi.system.util.DocChunker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * 项目文档服务实现
 *
 * 完整管线：上传 → 解析 → 切块 → 向量化 → 入库
 *
 * @author ruoyi
 */
@Service
public class ProjectDocServiceImpl implements IProjectDocService
{
    private static final Logger log = LoggerFactory.getLogger(ProjectDocServiceImpl.class);

    @Autowired
    private ProjectDocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper chunkMapper;

    @Autowired
    private IFileParseService fileParseService;

    @Autowired
    private IEmbeddingService embeddingService;

    @Autowired
    private DataSource dataSource;

    @Override
    @Transactional
    public ProjectDocument uploadDocument(Long projectId, Long planId, String docType,
                                          MultipartFile file, String createBy)
    {
        // 1. 先解析文件（在 transferTo 消费文件之前）
        IFileParseService.FileParseResult parseResult = fileParseService.parse(file);
        String plainText;
        if (parseResult.success && parseResult.plainText != null && !parseResult.plainText.isBlank())
        {
            plainText = parseResult.plainText;
            log.info("文件解析成功: fileName={}, chars={}", file.getOriginalFilename(), plainText.length());
        }
        else
        {
            plainText = "[文件: " + file.getOriginalFilename() + " - "
                        + (parseResult.errorMsg != null ? parseResult.errorMsg : "无法解析") + "]";
            log.warn("文件解析失败: fileName={}, msg={}", file.getOriginalFilename(), parseResult.errorMsg);
        }

        // 2. 再保存原始文件到磁盘（transferTo 会消费文件流，所以后执行）
        String fileUrl = null;
        try
        {
            fileUrl = FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file);
            log.info("文件已保存到磁盘: url={}", fileUrl);
        }
        catch (Exception e)
        {
            log.warn("文件保存到磁盘失败: {}", e.getMessage());
        }

        // 3. 保存文档记录（含已解析的文本）
        ProjectDocument doc = new ProjectDocument();
        doc.setProjectId(projectId);
        doc.setPlanId(planId);
        doc.setDocType(docType);
        doc.setFileName(file.getOriginalFilename());
        doc.setFileSize(file.getSize());
        String ext = file.getOriginalFilename();
        if (ext != null && ext.contains("."))
        {
            doc.setFileExt(ext.substring(ext.lastIndexOf('.') + 1).toLowerCase());
        }
        doc.setContentText(plainText.length() > 100000 ? plainText.substring(0, 100000) : plainText);
        doc.setFilePath(fileUrl);  // 磁盘文件URL
        doc.setCreateBy(createBy);
        documentMapper.insertDocument(doc);
        Long docId = doc.getId();
        log.info("文档记录已保存: id={}, fileName={}, contentLen={}", docId, doc.getFileName(),
                doc.getContentText() != null ? doc.getContentText().length() : 0);

        // 3. 切块
        List<String> chunks = DocChunker.chunk(plainText,
                DocChunker.DEFAULT_MAX_CHARS, DocChunker.DEFAULT_OVERLAP);
        log.info("文档切块完成: id={}, chunkCount={}", docId, chunks.size());

        // 4. 逐块向量化 + 入库
        int savedCount = 0;
        for (int i = 0; i < chunks.size(); i++)
        {
            String chunkText = chunks.get(i);
            if (chunkText.isBlank()) continue;

            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(docId);
            chunk.setChunkIndex(i);
            chunk.setContent(chunkText);
            chunk.setTokenCount(DocChunker.estimateTokens(chunkText));

            // 向量化
            try
            {
                float[] vector = embeddingService.embed(chunkText);
                chunk.setEmbedding(vector);
            }
            catch (Exception e)
            {
                log.warn("向量化失败跳过: docId={}, chunkIdx={}: {}", docId, i, e.getMessage());
            }

            // 插入数据库
            chunkMapper.insertChunk(chunk);

            // 如果有向量，更新到 pgvector
            if (chunk.getEmbedding() != null && chunk.getId() != null)
            {
                updateChunkEmbedding(chunk.getId(), chunk.getEmbedding());
            }
            savedCount++;
        }

        // 5. 更新切块数量
        documentMapper.updateChunkCount(docId, savedCount);
        log.info("文档入库完成: id={}, chunks={}", docId, savedCount);

        return doc;
    }

    @Override
    public ProjectDocument getDocumentById(Long docId)
    {
        return documentMapper.selectById(docId);
    }

    @Override
    public List<ProjectDocument> listDocuments(Long projectId, String docType)
    {
        if (docType != null && !docType.isEmpty())
        {
            return documentMapper.selectByProjectAndType(projectId, docType);
        }
        return documentMapper.selectByProjectId(projectId);
    }

    @Override
    public List<ProjectDocument> listByPlan(Long planId)
    {
        return documentMapper.selectByPlanId(planId);
    }

    @Override
    @Transactional
    public int deleteDocument(Long docId)
    {
        // 先删除所有切块
        chunkMapper.deleteByDocumentId(docId);
        // 再逻辑删除文档
        return documentMapper.deleteById(docId);
    }

    @Override
    public String getDocumentContent(Long docId)
    {
        ProjectDocument doc = documentMapper.selectById(docId);
        return doc != null ? doc.getContentText() : null;
    }

    @Override
    public String getMergedProjectText(Long projectId)
    {
        List<ProjectDocument> docs = documentMapper.selectByProjectId(projectId);
        if (docs == null || docs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (ProjectDocument doc : docs)
        {
            if (doc.getContentText() != null && !doc.getContentText().isBlank())
            {
                sb.append("【文档：").append(doc.getFileName()).append("】\n");
                sb.append(doc.getContentText()).append("\n\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String getMergedProjectTextByKeyword(String keyword)
    {
        List<ProjectDocument> docs = listProjectDocsByKeyword(keyword);
        if (docs == null || docs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (ProjectDocument doc : docs)
        {
            if (doc.getContentText() != null && !doc.getContentText().isBlank())
            {
                sb.append("【文档：").append(doc.getFileName()).append("】\n");
                sb.append(doc.getContentText()).append("\n\n");
            }
        }
        return sb.toString();
    }

    @Override
    public List<ProjectDocument> listRecentDocs(int limit)
    {
        List<ProjectDocument> docs = new ArrayList<>();
        String sql = "SELECT * FROM project_document WHERE status = 1 ORDER BY create_time DESC LIMIT ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    ProjectDocument doc = new ProjectDocument();
                    doc.setId(rs.getLong("id"));
                    doc.setProjectId(rs.getLong("project_id"));
                    doc.setPlanId(rs.getObject("plan_id") != null ? rs.getLong("plan_id") : null);
                    doc.setDocType(rs.getString("doc_type"));
                    doc.setFileName(rs.getString("file_name"));
                    doc.setFilePath(rs.getString("file_path"));
                    doc.setFileSize(rs.getLong("file_size"));
                    doc.setFileExt(rs.getString("file_ext"));
                    doc.setContentText(rs.getString("content_text"));
                    doc.setStatus(rs.getInt("status"));
                    doc.setChunkCount(rs.getInt("chunk_count"));
                    docs.add(doc);
                }
            }
        }
        catch (Exception e)
        {
            log.error("列最近项目文档失败", e);
        }
        return docs;
    }

    @Override
    public List<ProjectDocument> listProjectDocsByProjectName(String projectName)
    {
        if (projectName == null || projectName.isBlank()) return List.of();

        List<ProjectDocument> docs = new ArrayList<>();
        String sql = "SELECT pd.* FROM project_document pd "
                   + "JOIN audit_project ap ON ap.id = pd.project_id "
                   + "WHERE pd.status = 1 AND (ap.project_name ILIKE ? OR ap.audited_unit ILIKE ?) "
                   + "ORDER BY pd.create_time DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, "%" + projectName + "%");
            ps.setString(2, "%" + projectName + "%");
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    ProjectDocument doc = new ProjectDocument();
                    doc.setId(rs.getLong("id"));
                    doc.setProjectId(rs.getLong("project_id"));
                    doc.setPlanId(rs.getObject("plan_id") != null ? rs.getLong("plan_id") : null);
                    doc.setDocType(rs.getString("doc_type"));
                    doc.setFileName(rs.getString("file_name"));
                    doc.setFilePath(rs.getString("file_path"));
                    doc.setFileSize(rs.getLong("file_size"));
                    doc.setFileExt(rs.getString("file_ext"));
                    doc.setContentText(rs.getString("content_text"));
                    doc.setStatus(rs.getInt("status"));
                    doc.setChunkCount(rs.getInt("chunk_count"));
                    docs.add(doc);
                }
            }
        }
        catch (Exception e)
        {
            log.error("按项目名列出项目文档失败", e);
        }
        return docs;
    }

    @Override
    public String getMergedProjectTextByProjectName(String projectName)
    {
        List<ProjectDocument> docs = listProjectDocsByProjectName(projectName);
        if (docs == null || docs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (ProjectDocument doc : docs)
        {
            if (doc.getContentText() != null && !doc.getContentText().isBlank())
            {
                sb.append("【文档：").append(doc.getFileName()).append("】\n");
                sb.append(doc.getContentText()).append("\n\n");
            }
        }
        return sb.toString();
    }

    @Override
    public List<ProjectDocument> listProjectDocsByKeyword(String keyword)
    {
        if (keyword == null || keyword.isBlank()) return List.of();

        List<ProjectDocument> docs = new ArrayList<>();
        String sql = "SELECT * FROM project_document WHERE status = 1 AND (file_name ILIKE ? OR content_text ILIKE ?) ORDER BY create_time DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    ProjectDocument doc = new ProjectDocument();
                    doc.setId(rs.getLong("id"));
                    doc.setProjectId(rs.getLong("project_id"));
                    doc.setPlanId(rs.getObject("plan_id") != null ? rs.getLong("plan_id") : null);
                    doc.setDocType(rs.getString("doc_type"));
                    doc.setFileName(rs.getString("file_name"));
                    doc.setFilePath(rs.getString("file_path"));
                    doc.setFileSize(rs.getLong("file_size"));
                    doc.setFileExt(rs.getString("file_ext"));
                    doc.setContentText(rs.getString("content_text"));
                    doc.setStatus(rs.getInt("status"));
                    doc.setChunkCount(rs.getInt("chunk_count"));
                    docs.add(doc);
                }
            }
        }
        catch (Exception e)
        {
            log.error("按关键词列出项目文档失败", e);
        }
        return docs;
    }

    @Override
    public List<DocSearchResult> searchInProject(Long projectId, String query, int topK)
    {
        float[] queryVector = embeddingService.embed(query);
        List<DocSearchResult> results;
        if (queryVector == null)
        {
            results = keywordSearchInProject(projectId, query, topK);
        }
        else
        {
            results = vectorSearch(projectId, queryVector, topK);
        }

        // 如果检索结果为0，兜底返回前几条文档chunk
        // 让AI至少知道有哪些文档存在
        if (results.isEmpty())
        {
            List<DocSearchResult> fallback = listFirstChunks(projectId, topK);
            if (!fallback.isEmpty())
            {
                log.info("向量/关键词检索无结果，兜底返回 {} 条chunk", fallback.size());
                return fallback;
            }
        }
        return results;
    }

    @Override
    public List<DocSearchResult> searchInAllProjects(String query, int topK)
    {
        return searchInProject(null, query, topK);
    }

    // ---- private ----

    private void updateChunkEmbedding(Long chunkId, float[] vector)
    {
        String vectorStr = arrayToPgVector(vector);
        String sql = "UPDATE document_chunk SET embedding = ?::vector WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, vectorStr);
            ps.setLong(2, chunkId);
            ps.executeUpdate();
        }
        catch (Exception e)
        {
            log.error("更新chunk向量失败: chunkId={}", chunkId, e);
        }
    }

    private List<DocSearchResult> vectorSearch(Long projectId, float[] queryVector, int topK)
    {
        List<DocSearchResult> results = new ArrayList<>();
        String vectorStr = arrayToPgVector(queryVector);

        String sql;
        if (projectId != null)
        {
            sql = "SELECT dc.id, dc.document_id, dc.chunk_index, dc.content, "
                + "pd.file_name, pd.doc_type, "
                + "(dc.embedding <=> ?::vector) AS distance "
                + "FROM document_chunk dc "
                + "JOIN project_document pd ON pd.id = dc.document_id "
                + "WHERE pd.project_id = ? AND pd.status = 1 AND dc.embedding IS NOT NULL "
                + "ORDER BY dc.embedding <=> ?::vector LIMIT ?";
        }
        else
        {
            sql = "SELECT dc.id, dc.document_id, dc.chunk_index, dc.content, "
                + "pd.file_name, pd.doc_type, "
                + "(dc.embedding <=> ?::vector) AS distance "
                + "FROM document_chunk dc "
                + "JOIN project_document pd ON pd.id = dc.document_id "
                + "WHERE pd.status = 1 AND dc.embedding IS NOT NULL "
                + "ORDER BY dc.embedding <=> ?::vector LIMIT ?";
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, vectorStr);
            if (projectId != null)
            {
                ps.setLong(2, projectId);
                ps.setString(3, vectorStr);
                ps.setInt(4, topK);
            }
            else
            {
                ps.setString(2, vectorStr);
                ps.setInt(3, topK);
            }

            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    DocSearchResult r = new DocSearchResult();
                    r.documentId = rs.getLong("document_id");
                    r.fileName = rs.getString("file_name");
                    r.docType = rs.getString("doc_type");
                    r.chunkContent = rs.getString("content");
                    r.chunkIndex = rs.getInt("chunk_index");
                    r.distance = rs.getDouble("distance");
                    results.add(r);
                }
            }
        }
        catch (Exception e)
        {
            log.error("项目文档向量检索失败", e);
        }
        return results;
    }

    private List<DocSearchResult> keywordSearchInProject(Long projectId, String query, int topK)
    {
        List<DocSearchResult> results = new ArrayList<>();
        // 提取有意义的单关键词（至少2个中文字符）
        List<String> keywords = extractKeywords(query);
        if (keywords.isEmpty()) return results;

        StringBuilder likeClause = new StringBuilder();
        List<Object> params = new ArrayList<>();

        for (int i = 0; i < keywords.size(); i++)
        {
            if (i > 0) likeClause.append(" OR ");
            likeClause.append("content_text ILIKE ?");
            params.add("%" + keywords.get(i) + "%");
        }

        String sql = projectId != null
            ? "SELECT id, project_id, file_name, doc_type, content_text FROM project_document "
              + "WHERE project_id = ? AND status = 1 AND (" + likeClause + ") LIMIT ?"
            : "SELECT id, project_id, file_name, doc_type, content_text FROM project_document "
              + "WHERE status = 1 AND (" + likeClause + ") LIMIT ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            int idx = 1;
            if (projectId != null) ps.setLong(idx++, projectId);
            for (Object p : params) { ps.setString(idx++, (String) p); }
            ps.setInt(idx, topK);

            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    DocSearchResult r = new DocSearchResult();
                    r.documentId = rs.getLong("id");
                    r.fileName = rs.getString("file_name");
                    r.docType = rs.getString("doc_type");
                    r.chunkContent = truncate(rs.getString("content_text"), 500);
                    r.chunkIndex = 0;
                    r.distance = 0.5;  // 关键词匹配给中等优先级，避免被截断
                    results.add(r);
                }
            }
        }
        catch (Exception e)
        {
            log.error("关键词检索失败", e);
        }
        return results;
    }

    /**
     * 提取中文关键词（至少2个字符，过滤掉常见的疑问词）
     */
    private List<String> extractKeywords(String text)
    {
        List<String> keywords = new ArrayList<>();
        if (text == null || text.isBlank()) return keywords;

        // 去掉常见的疑问词和介词
        String cleaned = text.replaceAll("[吗？?的了的在给于与和或是就都到]", " ");
        // 按空格拆分
        for (String part : cleaned.split("\\s+"))
        {
            part = part.trim();
            if (part.length() >= 2)
            {
                keywords.add(part);
            }
        }
        // 如果拆分后没有关键词，回退到按连续中英文提取（至少2字符）
        if (keywords.isEmpty())
        {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("[\\u4e00-\\u9fa5]{2,}|[a-zA-Z0-9]{2,}|[\\u4e00-\\u9fa5a-zA-Z0-9]{3,}").matcher(text);
            while (m.find())
            {
                String kw = m.group().trim();
                if (kw.length() >= 2) keywords.add(kw);
            }
        }
        return keywords;
    }

    /**
     * 兜底：直接返回项目的前几条文档chunk（不匹配任何查询）
     */
    private List<DocSearchResult> listFirstChunks(Long projectId, int limit)
    {
        List<DocSearchResult> results = new ArrayList<>();
        String sql = "SELECT dc.id, dc.document_id, dc.chunk_index, dc.content, "
                   + "pd.file_name, pd.doc_type, pd.project_id "
                   + "FROM document_chunk dc "
                   + "JOIN project_document pd ON pd.id = dc.document_id "
                   + "WHERE pd.status = 1 "
                   + (projectId != null ? "AND pd.project_id = ? " : "")
                   + "ORDER BY pd.id, dc.chunk_index LIMIT ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            int idx = 1;
            if (projectId != null) ps.setLong(idx++, projectId);
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    DocSearchResult r = new DocSearchResult();
                    r.documentId = rs.getLong("document_id");
                    r.fileName = rs.getString("file_name");
                    r.docType = rs.getString("doc_type");
                    r.chunkContent = rs.getString("content");
                    r.chunkIndex = rs.getInt("chunk_index");
                    r.distance = 0.5;  // 兜底chunk给中等优先级，避免被截断
                    results.add(r);
                }
            }
        }
        catch (Exception e)
        {
            log.error("兜底chunk查询失败", e);
        }
        return results;
    }

    private String truncate(String text, int maxLen)
    {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    /** float[] → pgvector 字符串 */
    private String arrayToPgVector(float[] array)
    {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++)
        {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
