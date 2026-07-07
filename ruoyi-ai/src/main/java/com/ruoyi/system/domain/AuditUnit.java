package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class AuditUnit extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id; private String unitName; private String unitType; private String profile; private String historyAudit;

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public String getUnitName() { return unitName; } public void setUnitName(String v) { this.unitName = v; }
    public String getUnitType() { return unitType; } public void setUnitType(String v) { this.unitType = v; }
    public String getProfile() { return profile; } public void setProfile(String v) { this.profile = v; }
    public String getHistoryAudit() { return historyAudit; } public void setHistoryAudit(String v) { this.historyAudit = v; }
}
