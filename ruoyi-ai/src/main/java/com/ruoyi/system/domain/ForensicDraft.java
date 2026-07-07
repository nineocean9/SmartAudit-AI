package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 取证单草稿实体
 * 对应表 forensic_draft
 */
public class ForensicDraft extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String issue;
    private String basisIds;
    private String suggestion;
    private Integer reviewStatus;
    private String reviewLog;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }
    public String getBasisIds() { return basisIds; }
    public void setBasisIds(String basisIds) { this.basisIds = basisIds; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public Integer getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(Integer reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getReviewLog() { return reviewLog; }
    public void setReviewLog(String reviewLog) { this.reviewLog = reviewLog; }
}
