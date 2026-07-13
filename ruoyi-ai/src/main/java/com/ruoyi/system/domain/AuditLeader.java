package com.ruoyi.system.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 领导干部实体
 * 对应数据库表 audit_leader
 *
 * @author ruoyi
 */
public class AuditLeader extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "姓名")
    private String name;

    private Long unitId;

    @Excel(name = "职务")
    private String position;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date tenureStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date tenureEnd;

    /** 性别 */
    private String gender;

    /** 身份证号 */
    private String idNumber;

    /** 管理资金规模 */
    private BigDecimal managedFunds;

    /** 管理范围 */
    private String managedScope;

    /** 任职履历 */
    private String positionHistory;

    /** 审计评价 */
    private String auditEvaluation;

    /** JOIN字段：单位名称 */
    private String unitName;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long v) { this.unitId = v; }
    public String getPosition() { return position; }
    public void setPosition(String v) { this.position = v; }
    public Date getTenureStart() { return tenureStart; }
    public void setTenureStart(Date v) { this.tenureStart = v; }
    public Date getTenureEnd() { return tenureEnd; }
    public void setTenureEnd(Date v) { this.tenureEnd = v; }
    public String getGender() { return gender; }
    public void setGender(String v) { this.gender = v; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String v) { this.idNumber = v; }
    public BigDecimal getManagedFunds() { return managedFunds; }
    public void setManagedFunds(BigDecimal v) { this.managedFunds = v; }
    public String getManagedScope() { return managedScope; }
    public void setManagedScope(String v) { this.managedScope = v; }
    public String getPositionHistory() { return positionHistory; }
    public void setPositionHistory(String v) { this.positionHistory = v; }
    public String getAuditEvaluation() { return auditEvaluation; }
    public void setAuditEvaluation(String v) { this.auditEvaluation = v; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }
}
