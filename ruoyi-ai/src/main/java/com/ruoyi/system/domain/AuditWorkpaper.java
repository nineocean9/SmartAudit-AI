package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class AuditWorkpaper extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id; private Long projectId; private String category; private String title;
    private String content; private String basisIds; private Integer status;
    private String projectName;  // JOIN字段

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Long getProjectId() { return projectId; } public void setProjectId(Long v) { this.projectId = v; }
    public String getCategory() { return category; } public void setCategory(String v) { this.category = v; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getContent() { return content; } public void setContent(String v) { this.content = v; }
    public String getBasisIds() { return basisIds; } public void setBasisIds(String v) { this.basisIds = v; }
    public Integer getStatus() { return status; } public void setStatus(Integer v) { this.status = v; }
    public String getProjectName() { return projectName; } public void setProjectName(String v) { this.projectName = v; }
}
