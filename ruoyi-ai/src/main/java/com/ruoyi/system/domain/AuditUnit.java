package com.ruoyi.system.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 被审单位实体
 * 对应数据库表 audit_unit
 *
 * @author ruoyi
 */
public class AuditUnit extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "单位名称")
    private String unitName;

    @Excel(name = "单位类型")
    private String unitType;

    private String profile;
    private String historyAudit;

    /** 单位编码 */
    private String unitCode;

    /** 上级领导 */
    private String parentLeader;

    /** 员工数量 */
    private Integer staffCount;

    /** 年度预算 */
    private BigDecimal annualBudget;

    /** 财务联系人 */
    private String financeContact;

    /** 联系电话 */
    private String contactPhone;

    /** 地址 */
    private String address;

    /** 上次审计日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date lastAuditDate;

    /** 状态 */
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }
    public String getUnitType() { return unitType; }
    public void setUnitType(String v) { this.unitType = v; }
    public String getProfile() { return profile; }
    public void setProfile(String v) { this.profile = v; }
    public String getHistoryAudit() { return historyAudit; }
    public void setHistoryAudit(String v) { this.historyAudit = v; }
    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String v) { this.unitCode = v; }
    public String getParentLeader() { return parentLeader; }
    public void setParentLeader(String v) { this.parentLeader = v; }
    public Integer getStaffCount() { return staffCount; }
    public void setStaffCount(Integer v) { this.staffCount = v; }
    public BigDecimal getAnnualBudget() { return annualBudget; }
    public void setAnnualBudget(BigDecimal v) { this.annualBudget = v; }
    public String getFinanceContact() { return financeContact; }
    public void setFinanceContact(String v) { this.financeContact = v; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String v) { this.contactPhone = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = v; }
    public Date getLastAuditDate() { return lastAuditDate; }
    public void setLastAuditDate(Date v) { this.lastAuditDate = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
}
