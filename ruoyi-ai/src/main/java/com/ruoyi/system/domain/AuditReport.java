package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class AuditReport extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id; private Long projectId; private String versionType;
    private String content; private Integer status;
    private String projectName;  // JOIN字段
    private String auditedUnit;  // JOIN字段

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getProjectId() { return projectId; } public void setProjectId(Long v) { this.projectId = v; }
    public String getVersionType() { return versionType; } public void setVersionType(String v) { this.versionType = v; }
    public String getContent() { return content; } public void setContent(String v) { this.content = v; }
    public Integer getStatus() { return status; } public void setStatus(Integer v) { this.status = v; }
    public String getProjectName() { return projectName; } public void setProjectName(String v) { this.projectName = v; }
    public String getAuditedUnit() { return auditedUnit; } public void setAuditedUnit(String v) { this.auditedUnit = v; }
}
