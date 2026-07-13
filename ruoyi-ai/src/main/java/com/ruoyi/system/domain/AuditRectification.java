package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import java.math.BigDecimal;

/**
 * 审计整改记录实体
 * 对应数据库表 audit_rectification
 *
 * @author ruoyi
 */
public class AuditRectification extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long issueId;
    private String measure;
    private Integer status;
    private String finishDate;
    private String feedback;
    private String evaluator;

    /** 整改方案 */
    private String rectifyPlan;

    /** 延期原因 */
    private String delayReason;

    /** 延期状态 */
    private Integer delayStatus;

    /** 审核结果 */
    private String reviewResult;

    /** 审核意见 */
    private String reviewComment;

    /** 审核人 */
    private String reviewBy;

    /** 审核时间 */
    private String reviewTime;

    /** 涉及金额 */
    private BigDecimal amountInvolved;

    /** 已追回金额 */
    private BigDecimal amountRecovered;

    /** JOIN字段：问题描述 */
    private String issueDesc;

    /** JOIN字段：项目名称 */
    private String projectName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIssueId() { return issueId; }
    public void setIssueId(Long v) { this.issueId = v; }
    public String getMeasure() { return measure; }
    public void setMeasure(String v) { this.measure = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
    public String getFinishDate() { return finishDate; }
    public void setFinishDate(String v) { this.finishDate = v; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String v) { this.feedback = v; }
    public String getEvaluator() { return evaluator; }
    public void setEvaluator(String v) { this.evaluator = v; }
    public String getRectifyPlan() { return rectifyPlan; }
    public void setRectifyPlan(String v) { this.rectifyPlan = v; }
    public String getDelayReason() { return delayReason; }
    public void setDelayReason(String v) { this.delayReason = v; }
    public Integer getDelayStatus() { return delayStatus; }
    public void setDelayStatus(Integer v) { this.delayStatus = v; }
    public String getReviewResult() { return reviewResult; }
    public void setReviewResult(String v) { this.reviewResult = v; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String v) { this.reviewComment = v; }
    public String getReviewBy() { return reviewBy; }
    public void setReviewBy(String v) { this.reviewBy = v; }
    public String getReviewTime() { return reviewTime; }
    public void setReviewTime(String v) { this.reviewTime = v; }
    public BigDecimal getAmountInvolved() { return amountInvolved; }
    public void setAmountInvolved(BigDecimal v) { this.amountInvolved = v; }
    public BigDecimal getAmountRecovered() { return amountRecovered; }
    public void setAmountRecovered(BigDecimal v) { this.amountRecovered = v; }
    public String getIssueDesc() { return issueDesc; }
    public void setIssueDesc(String v) { this.issueDesc = v; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String v) { this.projectName = v; }
}
