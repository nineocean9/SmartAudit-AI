package com.ruoyi.system.service;

import com.ruoyi.system.domain.ProjectDocument;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * 项目文档服务接口
 * 负责文档上传→解析→切块→向量化→入库的完整管线
 *
 * @author ruoyi
 */
public interface IProjectDocService
{
    /** 文档搜索结果 */
    class DocSearchResult
    {
        public Long documentId;
        public String fileName;
        public String docType;
        public String chunkContent;
        public int chunkIndex;
        public double distance;   // pgvector 距离(越小越相似)

        public Long getDocumentId() { return documentId; }
        public String getFileName() { return fileName; }
        public String getDocType() { return docType; }
        public String getChunkContent() { return chunkContent; }
        public int getChunkIndex() { return chunkIndex; }
        public double getDistance() { return distance; }
    }

    /**
     * 上传文档到项目
     *
     * @param projectId 所属项目ID
     * @param planId    所属计划ID(可选)
     * @param docType   资料类型
     * @param file      上传文件
     * @return 保存的文档记录
     */
    ProjectDocument uploadDocument(Long projectId, Long planId, String docType,
                                   MultipartFile file, String createBy);

    /** 将其他业务页面已经上传的文件登记或更新到项目库。 */
    ProjectDocument syncUploadedDocument(Long projectId, Long planId, String docType,
                                         String fileName, String filePath, String contentText,
                                         String createBy, boolean visible);

    int hideSyncedDocument(Long projectId, String filePath);

    /**
     * 根据ID获取文档（含 filePath，用于下载）
     */
    ProjectDocument getDocumentById(Long docId);

    /**
     * 列出项目文档
     */
    List<ProjectDocument> listDocuments(Long projectId, String docType);

    /**
     * 按计划列出文档
     */
    List<ProjectDocument> listByPlan(Long planId);

    /**
     * 删除文档及其所有切块
     */
    int deleteDocument(Long docId);

    /**
     * 获取文档纯文本内容
     */
    String getDocumentContent(Long docId);

    /** 将在线编辑内容写回原 DOCX 文件。 */
    void saveDocx(Long docId, String htmlContent) throws java.io.IOException;

    /**
     * 获取项目下所有文档的合并文本（用于数据分析）
     */
    String getMergedProjectText(Long projectId);

    /**
     * 列出最近上传的项目文档
     */
    List<ProjectDocument> listRecentDocs(int limit);

    /**
     * 根据项目名列出项目文档
     */
    List<ProjectDocument> listProjectDocsByProjectName(String projectName);

    /**
     * 根据项目名获取合并后的项目文本
     */
    String getMergedProjectTextByProjectName(String projectName);

    /**
     * 根据关键词列出项目文档
     */
    List<ProjectDocument> listProjectDocsByKeyword(String keyword);

    /**
     * 根据关键词获取合并后的项目文本
     */
    String getMergedProjectTextByKeyword(String keyword);

    /**
     * 在指定项目中检索
     */
    List<DocSearchResult> searchInProject(Long projectId, String query, int topK);

    /**
     * 跨所有项目检索
     */
    List<DocSearchResult> searchInAllProjects(String query, int topK);
}
