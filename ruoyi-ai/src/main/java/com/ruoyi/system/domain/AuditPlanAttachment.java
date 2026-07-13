package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 审计计划附件实体
 * 对应数据库表 audit_plan_attachment
 *
 * @author ruoyi
 */
public class AuditPlanAttachment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long planId;
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String attachmentType;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long v) { this.planId = v; }
    public String getFileName() { return fileName; }
    public void setFileName(String v) { this.fileName = v; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String v) { this.filePath = v; }
    public String getFileType() { return fileType; }
    public void setFileType(String v) { this.fileType = v; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long v) { this.fileSize = v; }
    public String getAttachmentType() { return attachmentType; }
    public void setAttachmentType(String v) { this.attachmentType = v; }
}
