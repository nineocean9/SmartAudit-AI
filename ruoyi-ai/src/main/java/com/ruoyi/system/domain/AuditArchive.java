package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 审计档案实体
 * 对应数据库表 audit_archive
 *
 * @author ruoyi
 */
public class AuditArchive extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String archiveNo;
    private Integer archiveStatus;
    private String archiveCategory;
    private String fileName;
    private String filePath;
    private Integer sortOrder;
    private String reviewBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reviewTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date archiveTime;

    /** JOIN字段：项目名称 */
    private String projectName;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long v) { this.projectId = v; }
    public String getArchiveNo() { return archiveNo; }
    public void setArchiveNo(String v) { this.archiveNo = v; }
    public Integer getArchiveStatus() { return archiveStatus; }
    public void setArchiveStatus(Integer v) { this.archiveStatus = v; }
    public String getArchiveCategory() { return archiveCategory; }
    public void setArchiveCategory(String v) { this.archiveCategory = v; }
    public String getFileName() { return fileName; }
    public void setFileName(String v) { this.fileName = v; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String v) { this.filePath = v; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer v) { this.sortOrder = v; }
    public String getReviewBy() { return reviewBy; }
    public void setReviewBy(String v) { this.reviewBy = v; }
    public Date getReviewTime() { return reviewTime; }
    public void setReviewTime(Date v) { this.reviewTime = v; }
    public Date getArchiveTime() { return archiveTime; }
    public void setArchiveTime(Date v) { this.archiveTime = v; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String v) { this.projectName = v; }
}
