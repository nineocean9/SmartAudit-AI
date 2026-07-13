package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import java.math.BigDecimal;

/**
 * 审计问题实体
 * 对应数据库表 audit_issue
 *
 * @author ruoyi
 */
public class AuditIssue extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String issueDesc;
    private Integer severity;
    private Long basisId;
    private String source;
    private String deadline;

    /** 涉及金额 */
    private BigDecimal amount;

    /** 责任单位 */
    private String responsibleUnit;

    /** 责任人 */
    private String responsiblePerson;

    /** 问题类型 */
    private String issueType;

    /** 关联字段（非表字段） */
    private String projectName;
    private String basisTitle;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long v) { this.projectId = v; }
    public String getIssueDesc() { return issueDesc; }
    public void setIssueDesc(String v) { this.issueDesc = v; }
    public Integer getSeverity() { return severity; }
    public void setSeverity(Integer v) { this.severity = v; }
    public Long getBasisId() { return basisId; }
    public void setBasisId(Long v) { this.basisId = v; }
    public String getSource() { return source; }
    public void setSource(String v) { this.source = v; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String v) { this.deadline = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public String getResponsibleUnit() { return responsibleUnit; }
    public void setResponsibleUnit(String v) { this.responsibleUnit = v; }
    public String getResponsiblePerson() { return responsiblePerson; }
    public void setResponsiblePerson(String v) { this.responsiblePerson = v; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String v) { this.issueType = v; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String v) { this.projectName = v; }
    public String getBasisTitle() { return basisTitle; }
    public void setBasisTitle(String v) { this.basisTitle = v; }
}
