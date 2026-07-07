package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 风险线索实体
 * 对应表 risk_clue
 */
public class RiskClue extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String clueType;
    private String content;
    private Integer severity;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getClueType() { return clueType; }
    public void setClueType(String clueType) { this.clueType = clueType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getSeverity() { return severity; }
    public void setSeverity(Integer severity) { this.severity = severity; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
