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
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.math.BigInteger;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    @Transactional
    public ProjectDocument syncUploadedDocument(Long projectId, Long planId, String docType,
                                                 String fileName, String filePath, String contentText,
                                                 String createBy, boolean visible)
    {
        ProjectDocument doc = documentMapper.selectByBusinessKey(projectId, docType, fileName);
        if (doc == null)
        {
            ProjectDocument sameFile = documentMapper.selectByProjectAndPath(projectId, filePath);
            if (sameFile != null && !Objects.equals(sameFile.getDocType(), docType))
            {
                return sameFile;
            }
            doc = sameFile;
        }
        if (doc == null)
        {
            doc = new ProjectDocument();
            doc.setProjectId(projectId);
            doc.setDocType(docType);
            doc.setFileName(fileName);
        }
        doc.setPlanId(planId);
        doc.setFilePath(filePath);
        doc.setFileSize(resolveFileSize(filePath));
        doc.setFileExt(fileExtension(fileName));
        doc.setContentText(contentText);
        doc.setCreateBy(createBy);
        doc.setStatus(visible ? 1 : 0);

        if (doc.getId() == null)
        {
            documentMapper.insertDocument(doc);
        }
        else
        {
            documentMapper.updateSyncedDocument(doc);
        }
        return doc;
    }

    @Override
    @Transactional
    public int hideSyncedDocument(Long projectId, String filePath)
    {
        if (projectId == null || filePath == null || filePath.isBlank()) return 0;
        return documentMapper.hideByProjectAndPath(projectId, filePath);
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
    @Transactional(rollbackFor = Exception.class)
    public void saveDocx(Long docId, String htmlContent) throws IOException
    {
        ProjectDocument stored = documentMapper.selectById(docId);
        if (stored == null)
        {
            throw new IOException("文档不存在");
        }
        if (!"docx".equalsIgnoreCase(stored.getFileExt())
                && (stored.getFileName() == null || !stored.getFileName().toLowerCase().endsWith(".docx")))
        {
            throw new IOException("仅支持保存 DOCX 文档");
        }

        Path target = resolveWritableProfilePath(stored.getFilePath());
        org.jsoup.nodes.Document html = Jsoup.parseBodyFragment(htmlContent == null ? "" : htmlContent);
        Path temporary = Files.createTempFile(target.getParent(), "docx-edit-", ".tmp");
        try
        {
            try (XWPFDocument word = new XWPFDocument(); FileOutputStream output = new FileOutputStream(temporary.toFile()))
            {
                configureA4(word);
                writeHtmlBody(word, html.body());
                word.write(output);
            }
            try
            {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException ignored)
            {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally
        {
            Files.deleteIfExists(temporary);
        }

        String plainText = html.text();
        if (plainText.length() > 100000) plainText = plainText.substring(0, 100000);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE project_document SET content_text=?, file_size=?, update_time=now() WHERE id=?"))
        {
            statement.setString(1, plainText);
            statement.setLong(2, Files.size(target));
            statement.setLong(3, docId);
            statement.executeUpdate();
        }
        catch (Exception e)
        {
            throw new IOException("DOCX 已写入，但文档信息更新失败", e);
        }
    }

    private Path resolveWritableProfilePath(String filePath) throws IOException
    {
        if (filePath == null || !filePath.replace('\\', '/').startsWith("/profile/"))
        {
            throw new IOException("该文档不是可编辑的项目文件");
        }
        Path profile = new File(RuoYiConfig.getProfile()).getCanonicalFile().toPath();
        String relative = filePath.replace('\\', '/').substring("/profile/".length());
        Path target = profile.resolve(relative).normalize();
        if (!target.startsWith(profile) || !Files.isRegularFile(target))
        {
            throw new IOException("文档文件不存在或路径无效");
        }
        return target;
    }

    private void configureA4(XWPFDocument word)
    {
        CTSectPr section = word.getDocument().getBody().addNewSectPr();
        CTPageSz size = section.addNewPgSz();
        size.setW(BigInteger.valueOf(11906));
        size.setH(BigInteger.valueOf(16838));
        CTPageMar margin = section.addNewPgMar();
        margin.setTop(BigInteger.valueOf(1440));
        margin.setRight(BigInteger.valueOf(1440));
        margin.setBottom(BigInteger.valueOf(1440));
        margin.setLeft(BigInteger.valueOf(1440));
    }

    private void writeHtmlBody(XWPFDocument word, Element body)
    {
        List<Element> pages = body.children().stream()
                .filter(element -> "section".equals(element.normalName()))
                .toList();
        if (pages.isEmpty())
        {
            writeBlocks(word, body.childNodes());
            return;
        }
        for (int index = 0; index < pages.size(); index++)
        {
            if (index > 0) word.createParagraph().setPageBreak(true);
            writeBlocks(word, pages.get(index).childNodes());
        }
    }

    private void writeBlocks(XWPFDocument word, List<Node> nodes)
    {
        for (Node node : nodes)
        {
            if (node instanceof TextNode textNode)
            {
                if (!textNode.text().isBlank()) appendText(word.createParagraph(), textNode);
                continue;
            }
            if (!(node instanceof Element element)) continue;
            String tag = element.normalName();
            if ("style".equals(tag) || "script".equals(tag)) continue;
            if ("table".equals(tag))
            {
                writeTable(word, element);
            }
            else if (Set.of("p", "h1", "h2", "h3", "h4", "h5", "h6", "li").contains(tag))
            {
                XWPFParagraph paragraph = word.createParagraph();
                applyParagraphStyle(paragraph, element);
                appendInline(paragraph, element.childNodes());
            }
            else if ("img".equals(tag))
            {
                appendImage(word.createParagraph().createRun(), element);
            }
            else if ("br".equals(tag))
            {
                word.createParagraph();
            }
            else
            {
                writeBlocks(word, element.childNodes());
            }
        }
    }

    private void writeTable(XWPFDocument word, Element source)
    {
        List<Element> rows = source.select("tr");
        if (rows.isEmpty()) return;
        int columnCount = rows.stream().mapToInt(row -> row.select(":scope > th, :scope > td").size()).max().orElse(1);
        XWPFTable table = word.createTable(rows.size(), columnCount);
        table.setWidth("100%");
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++)
        {
            List<Element> cells = rows.get(rowIndex).select(":scope > th, :scope > td");
            for (int columnIndex = 0; columnIndex < cells.size(); columnIndex++)
            {
                XWPFTableCell cell = table.getRow(rowIndex).getCell(columnIndex);
                XWPFParagraph paragraph = cell.getParagraphs().get(0);
                appendInline(paragraph, cells.get(columnIndex).childNodes());
                if ("th".equals(cells.get(columnIndex).normalName()))
                {
                    paragraph.getRuns().forEach(run -> run.setBold(true));
                }
            }
        }
    }

    private void appendInline(XWPFParagraph paragraph, List<Node> nodes)
    {
        for (Node node : nodes)
        {
            if (node instanceof TextNode textNode)
            {
                appendText(paragraph, textNode);
            }
            else if (node instanceof Element element)
            {
                if ("br".equals(element.normalName())) paragraph.createRun().addBreak();
                else if ("img".equals(element.normalName())) appendImage(paragraph.createRun(), element);
                else if ("table".equals(element.normalName()))
                {
                    paragraph.createRun().setText(element.text());
                }
                else appendInline(paragraph, element.childNodes());
            }
        }
    }

    private void appendText(XWPFParagraph paragraph, TextNode textNode)
    {
        String text = textNode.getWholeText().replace('\u00a0', ' ').replaceAll("[\\t\\r\\n]+", " ");
        if (text.isEmpty()) return;
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        Element element = textNode.parent() instanceof Element parent ? parent : null;
        if (element == null) return;

        String family = inheritedCss(element, "font-family");
        if (family != null) run.setFontFamily(family.split(",")[0].replace("\"", "").replace("'", "").trim());
        Double fontSize = cssPointSize(inheritedCss(element, "font-size"));
        if (fontSize != null && fontSize > 0) run.setFontSize(Math.max(1, (int)Math.round(fontSize)));
        String weight = inheritedCss(element, "font-weight");
        run.setBold(hasAncestorTag(element, "b", "strong") || "bold".equalsIgnoreCase(weight) || numericWeight(weight) >= 600);
        String fontStyle = inheritedCss(element, "font-style");
        run.setItalic(hasAncestorTag(element, "i", "em") || "italic".equalsIgnoreCase(fontStyle));
        String decoration = inheritedCss(element, "text-decoration");
        run.setUnderline(hasAncestorTag(element, "u") || (decoration != null && decoration.contains("underline"))
                ? UnderlinePatterns.SINGLE : UnderlinePatterns.NONE);
        run.setStrikeThrough(hasAncestorTag(element, "s", "strike", "del")
                || (decoration != null && decoration.contains("line-through")));
        String color = cssColor(inheritedCss(element, "color"));
        if (color != null) run.setColor(color);
        if (hasAncestorTag(element, "sup")) run.setSubscript(VerticalAlign.SUPERSCRIPT);
        if (hasAncestorTag(element, "sub")) run.setSubscript(VerticalAlign.SUBSCRIPT);
    }

    private void applyParagraphStyle(XWPFParagraph paragraph, Element element)
    {
        String alignment = inheritedCss(element, "text-align");
        if ("center".equalsIgnoreCase(alignment)) paragraph.setAlignment(ParagraphAlignment.CENTER);
        else if ("right".equalsIgnoreCase(alignment)) paragraph.setAlignment(ParagraphAlignment.RIGHT);
        else if ("justify".equalsIgnoreCase(alignment)) paragraph.setAlignment(ParagraphAlignment.BOTH);
        else paragraph.setAlignment(ParagraphAlignment.LEFT);

        String tag = element.normalName();
        if (tag.matches("h[1-6]"))
        {
            int level = Integer.parseInt(tag.substring(1));
            int size = Math.max(12, 24 - (level - 1) * 2);
            if (element.childNodes().isEmpty()) element.appendText(" ");
            appendInline(paragraph, element.childNodes());
            paragraph.getRuns().forEach(run -> { run.setBold(true); run.setFontSize(size); });
            element.empty();
        }
        paragraph.setSpacingAfter(80);
    }

    private void appendImage(XWPFRun run, Element image)
    {
        String src = image.attr("src");
        if (!src.startsWith("data:image/") || !src.contains(",")) return;
        try
        {
            String metadata = src.substring(5, src.indexOf(','));
            byte[] bytes = Base64.getDecoder().decode(src.substring(src.indexOf(',') + 1));
            BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(bytes));
            if (buffered == null) return;
            double scale = Math.min(1d, 600d / Math.max(1, buffered.getWidth()));
            int width = Math.max(1, (int)Math.round(buffered.getWidth() * scale));
            int height = Math.max(1, (int)Math.round(buffered.getHeight() * scale));
            int pictureType = metadata.contains("png") ? XWPFDocument.PICTURE_TYPE_PNG
                    : metadata.contains("gif") ? XWPFDocument.PICTURE_TYPE_GIF
                    : XWPFDocument.PICTURE_TYPE_JPEG;
            try (ByteArrayInputStream input = new ByteArrayInputStream(bytes))
            {
                run.addPicture(input, pictureType, "image", Units.pixelToEMU(width), Units.pixelToEMU(height));
            }
        }
        catch (Exception e)
        {
            log.warn("在线编辑图片写入 DOCX 失败: {}", e.getMessage());
        }
    }

    private String inheritedCss(Element element, String property)
    {
        for (Element current = element; current != null; current = current.parent())
        {
            String value = inlineCssValue(current.attr("style"), property);
            if (value != null && !value.isBlank() && !"inherit".equalsIgnoreCase(value)) return value.trim();
        }
        return null;
    }

    private String inlineCssValue(String style, String property)
    {
        if (style == null || style.isBlank() || property == null || property.isBlank()) return null;
        for (String declaration : style.split(";"))
        {
            int separator = declaration.indexOf(':');
            if (separator <= 0) continue;
            String name = declaration.substring(0, separator).trim();
            if (property.equalsIgnoreCase(name)) return declaration.substring(separator + 1).trim();
        }
        return null;
    }

    private boolean hasAncestorTag(Element element, String... tags)
    {
        Set<String> expected = Set.of(tags);
        for (Element current = element; current != null; current = current.parent())
        {
            if (expected.contains(current.normalName())) return true;
        }
        return false;
    }

    private int numericWeight(String value)
    {
        try { return value == null ? 0 : Integer.parseInt(value.trim()); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private Double cssPointSize(String value)
    {
        if (value == null) return null;
        try
        {
            String normalized = value.trim().toLowerCase();
            if (normalized.endsWith("px")) return Double.parseDouble(normalized.substring(0, normalized.length() - 2)) * 0.75d;
            if (normalized.endsWith("pt")) return Double.parseDouble(normalized.substring(0, normalized.length() - 2));
        }
        catch (NumberFormatException ignored) { }
        return null;
    }

    private String cssColor(String value)
    {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase();
        if (normalized.matches("#[0-9a-f]{6}")) return normalized.substring(1).toUpperCase();
        if (normalized.matches("#[0-9a-f]{3}"))
        {
            return ("" + normalized.charAt(1) + normalized.charAt(1)
                    + normalized.charAt(2) + normalized.charAt(2)
                    + normalized.charAt(3) + normalized.charAt(3)).toUpperCase();
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("rgba?\\((\\d+),\\s*(\\d+),\\s*(\\d+)").matcher(normalized);
        if (!matcher.find()) return null;
        return String.format("%02X%02X%02X", Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
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

    private Long resolveFileSize(String filePath)
    {
        if (filePath == null || filePath.isBlank()) return 0L;
        String relativePath = filePath.replace('\\', '/');
        int profileIndex = relativePath.indexOf("/profile/");
        if (profileIndex >= 0)
        {
            relativePath = relativePath.substring(profileIndex + "/profile/".length());
        }
        else
        {
            relativePath = relativePath.replaceFirst("^/+", "");
        }
        java.io.File file = new java.io.File(RuoYiConfig.getProfile(), relativePath);
        return file.isFile() ? file.length() : 0L;
    }

    private String fileExtension(String fileName)
    {
        if (fileName == null) return null;
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && dot < fileName.length() - 1
                ? fileName.substring(dot + 1).toLowerCase()
                : null;
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
