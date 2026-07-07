package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 案例库
 * 对应表 audit_case_lib
 */
public class AuditCaseLib extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private String caseTitle;
    private String caseContent;
    private String category;
    private String reference;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseTitle() { return caseTitle; }
    public void setCaseTitle(String caseTitle) { this.caseTitle = caseTitle; }
    public String getCaseContent() { return caseContent; }
    public void setCaseContent(String caseContent) { this.caseContent = caseContent; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
