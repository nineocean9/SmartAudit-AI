package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 审计整改扩展字段（写在 issue 中）
 */
public class AuditIssueExt extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long issueId;
    private String source;
    private String deadline;

    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
}
