package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 审计报告实体
 * 对应数据库表 audit_report
 *
 * @author ruoyi
 */
public class AuditReport extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String title;
    private String versionType;
    private String content;
    private String fileUrl;
    private Integer status;

    /** 版本号 */
    private Integer versionNo;

    /** 意见征求状态 */
    private Integer opinionStatus;

    /** JOIN字段：项目名称 */
    private String projectName;

    /** JOIN字段：被审计单位 */
    private String auditedUnit;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long v) { this.projectId = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getVersionType() { return versionType; }
    public void setVersionType(String v) { this.versionType = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String v) { this.fileUrl = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer v) { this.versionNo = v; }
    public Integer getOpinionStatus() { return opinionStatus; }
    public void setOpinionStatus(Integer v) { this.opinionStatus = v; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String v) { this.projectName = v; }
    public String getAuditedUnit() { return auditedUnit; }
    public void setAuditedUnit(String v) { this.auditedUnit = v; }
}
