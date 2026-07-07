package com.ruoyi.system.domain;

/**
 * 整改记录（模块一 - 审计整改管理 演示数据）
 * 对应表 audit_rectification
 */
public class AuditRectification
{
    private Long id;
    private Long issueId;
    private String measure;
    private Integer status;
    private String finishDate;
    private String createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }
    public String getMeasure() { return measure; }
    public void setMeasure(String measure) { this.measure = measure; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getFinishDate() { return finishDate; }
    public void setFinishDate(String finishDate) { this.finishDate = finishDate; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
