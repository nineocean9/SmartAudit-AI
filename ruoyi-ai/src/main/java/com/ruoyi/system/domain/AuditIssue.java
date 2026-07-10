package com.ruoyi.system.domain;

/**
 * 审计问题（模块二 - 审计问题管理）
 * 对应表 audit_issue
 */
public class AuditIssue
{
    private Long id;
    private Long projectId;
    private String issueDesc;
    private Integer severity;
    private Long basisId;
    private String source;
    private String deadline;
    private String createTime;

    /** 关联字段（非表字段） */
    private String projectName;
    private String basisTitle;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getIssueDesc() { return issueDesc; }
    public void setIssueDesc(String issueDesc) { this.issueDesc = issueDesc; }
    public Integer getSeverity() { return severity; }
    public void setSeverity(Integer severity) { this.severity = severity; }
    public Long getBasisId() { return basisId; }
    public void setBasisId(Long basisId) { this.basisId = basisId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getBasisTitle() { return basisTitle; }
    public void setBasisTitle(String basisTitle) { this.basisTitle = basisTitle; }
}
