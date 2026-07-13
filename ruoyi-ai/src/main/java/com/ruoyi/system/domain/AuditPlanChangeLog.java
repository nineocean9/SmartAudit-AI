package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 审计计划变更日志实体
 * 对应数据库表 audit_plan_change_log
 *
 * @author ruoyi
 */
public class AuditPlanChangeLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long planId;
    private String changeType;
    private String beforeJson;
    private String afterJson;
    private String changeReason;
    private String changeBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date changeTime;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long v) { this.planId = v; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String v) { this.changeType = v; }
    public String getBeforeJson() { return beforeJson; }
    public void setBeforeJson(String v) { this.beforeJson = v; }
    public String getAfterJson() { return afterJson; }
    public void setAfterJson(String v) { this.afterJson = v; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String v) { this.changeReason = v; }
    public String getChangeBy() { return changeBy; }
    public void setChangeBy(String v) { this.changeBy = v; }
    public Date getChangeTime() { return changeTime; }
    public void setChangeTime(Date v) { this.changeTime = v; }
}
