package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 审计依据库实体
 * 对应数据库表 audit_basis
 * 存储审计工作所需的法规、制度、标准等依据条目
 *
 * @author ruoyi
 */
public class AuditBasis extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 分类：法规/制度/标准 */
    private String category;

    /** 标题 */
    private String title;

    /** 正文内容 */
    private String content;

    /** 颁发单位 */
    private String issueOrg;

    /** 生效日期 */
    private String effectiveDate;

    /** 状态：1=生效 0=失效 */
    private Integer status;

    /** 版本号 */
    private Integer version;

    /** pgvector向量字段(Java层不直接操作，通过EmbeddingService管理) */
    private String embedding;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getIssueOrg()
    {
        return issueOrg;
    }

    public void setIssueOrg(String issueOrg)
    {
        this.issueOrg = issueOrg;
    }

    public String getEffectiveDate()
    {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate)
    {
        this.effectiveDate = effectiveDate;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    public String getEmbedding()
    {
        return embedding;
    }

    public void setEmbedding(String embedding)
    {
        this.embedding = embedding;
    }
}
