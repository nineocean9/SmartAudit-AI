package com.ruoyi.system.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 审计计划实体
 * 对应数据库表 audit_plan
 *
 * @author ruoyi
 */
public class AuditPlan extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "计划类型")
    private String planType;

    @Excel(name = "计划年度")
    private Integer planYear;

    @Excel(name = "批次")
    private String batch;

    @Excel(name = "计划名称")
    private String planName;

    private String fileUrl;
    private Integer status;

    /** 计划开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date planStartDate;

    /** 计划结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date planEndDate;

    /** 审批状态 */
    private Integer approvalStatus;

    /** 描述 */
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlanType() { return planType; }
    public void setPlanType(String v) { this.planType = v; }
    public Integer getPlanYear() { return planYear; }
    public void setPlanYear(Integer v) { this.planYear = v; }
    public String getBatch() { return batch; }
    public void setBatch(String v) { this.batch = v; }
    public String getPlanName() { return planName; }
    public void setPlanName(String v) { this.planName = v; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String v) { this.fileUrl = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
    public Date getPlanStartDate() { return planStartDate; }
    public void setPlanStartDate(Date v) { this.planStartDate = v; }
    public Date getPlanEndDate() { return planEndDate; }
    public void setPlanEndDate(Date v) { this.planEndDate = v; }
    public Integer getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(Integer v) { this.approvalStatus = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
}
