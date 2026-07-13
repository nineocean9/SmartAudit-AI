package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 审计方案模板实体
 * 对应数据库表 audit_scheme_template
 *
 * @author ruoyi
 */
public class AuditSchemeTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String templateName;
    private String auditType;
    private String content;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String v) { this.templateName = v; }
    public String getAuditType() { return auditType; }
    public void setAuditType(String v) { this.auditType = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
}
