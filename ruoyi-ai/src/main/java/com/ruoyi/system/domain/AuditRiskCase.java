package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 问题风险库
 * 对应表 audit_risk_case
 */
public class AuditRiskCase extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private String riskName;
    private String riskDesc;
    private String basisIds;
    private String scenario;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRiskName() { return riskName; }
    public void setRiskName(String riskName) { this.riskName = riskName; }
    public String getRiskDesc() { return riskDesc; }
    public void setRiskDesc(String riskDesc) { this.riskDesc = riskDesc; }
    public String getBasisIds() { return basisIds; }
    public void setBasisIds(String basisIds) { this.basisIds = basisIds; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
}
