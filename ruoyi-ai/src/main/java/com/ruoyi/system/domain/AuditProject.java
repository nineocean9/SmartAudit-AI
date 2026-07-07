package com.ruoyi.system.domain;

/**
 * 审计项目（模块一 - 审计信息管理 演示数据）
 * 对应表 audit_project
 */
public class AuditProject
{
    private Long id;
    private String projectName;
    private String auditedUnit;
    private String auditType;
    private Integer auditYear;
    private Integer status;
    private String createTime;
    private String updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getAuditedUnit() { return auditedUnit; }
    public void setAuditedUnit(String auditedUnit) { this.auditedUnit = auditedUnit; }
    public String getAuditType() { return auditType; }
    public void setAuditType(String auditType) { this.auditType = auditType; }
    public Integer getAuditYear() { return auditYear; }
    public void setAuditYear(Integer auditYear) { this.auditYear = auditYear; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
