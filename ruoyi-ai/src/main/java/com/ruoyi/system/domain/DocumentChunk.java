package com.ruoyi.system.domain;

import java.util.Date;

/**
 * 文档切块实体
 * 对应数据库表 document_chunk
 *
 * @author ruoyi
 */
public class DocumentChunk
{
    private Long id;
    private Long documentId;
    private Integer chunkIndex;
    private String content;
    private Integer tokenCount;
    /** pgvector 向量（程序中使用，不直接写入MyBatis） */
    private float[] embedding;
    /** 来源类型: PROJECT-项目文档, CASE-案例库, RISK_CASE-风险案例库 */
    private String sourceType;
    /** 来源记录ID（案例库id/风险案例id，source_type非PROJECT时使用） */
    private Long sourceId;
    private Date createTime;

    // ---- getter / setter ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }

    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
