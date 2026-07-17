package com.ruoyi.system.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 审计项目实体
 * 对应数据库表 audit_project
 *
 * @author ruoyi
 */
public class AuditProject extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "项目名称")
    private String projectName;

    @Excel(name = "被审计单位")
    private String auditedUnit;

    private Long deptId;

    @Excel(name = "审计类型")
    private String auditType;

    @Excel(name = "审计年度")
    private Integer auditYear;

    private Integer status;
    private Long planId;
    private Integer docCount;

    /** 主审人ID */
    private Long leaderId;

    /** 主审人姓名 */
    private String leaderName;

    /** 项目开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    /** 项目结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    /** 审计覆盖期间起 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date coverageStart;

    /** 审计覆盖期间止 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date coverageEnd;

    /** 是否外包 */
    private Integer isOutsourced;

    /** 预算 */
    private BigDecimal budget;

    /** 进度 */
    private Integer progress;

    /** 阶段 */
    private String phase;

    /** 是否逾期 */
    private Integer isOverdue;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String v) { this.projectName = v; }
    public String getAuditedUnit() { return auditedUnit; }
    public void setAuditedUnit(String v) { this.auditedUnit = v; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long v) { this.deptId = v; }
    public String getAuditType() { return auditType; }
    public void setAuditType(String v) { this.auditType = v; }
    public Integer getAuditYear() { return auditYear; }
    public void setAuditYear(Integer v) { this.auditYear = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long v) { this.planId = v; }
    public Integer getDocCount() { return docCount; }
    public void setDocCount(Integer v) { this.docCount = v; }
    public Long getLeaderId() { return leaderId; }
    public void setLeaderId(Long v) { this.leaderId = v; }
    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String v) { this.leaderName = v; }
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date v) { this.startDate = v; }
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date v) { this.endDate = v; }
    public Date getCoverageStart() { return coverageStart; }
    public void setCoverageStart(Date v) { this.coverageStart = v; }
    public Date getCoverageEnd() { return coverageEnd; }
    public void setCoverageEnd(Date v) { this.coverageEnd = v; }
    public Integer getIsOutsourced() { return isOutsourced; }
    public void setIsOutsourced(Integer v) { this.isOutsourced = v; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal v) { this.budget = v; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer v) { this.progress = v; }
    public String getPhase() { return phase; }
    public void setPhase(String v) { this.phase = v; }
    public Integer getIsOverdue() { return isOverdue; }
    public void setIsOverdue(Integer v) { this.isOverdue = v; }
}
