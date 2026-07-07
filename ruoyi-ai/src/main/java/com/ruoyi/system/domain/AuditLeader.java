package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class AuditLeader extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id; private String name; private Long unitId; private String position;
    private String tenureStart; private String tenureEnd;

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    public Long getUnitId() { return unitId; } public void setUnitId(Long v) { this.unitId = v; }
    public String getPosition() { return position; } public void setPosition(String v) { this.position = v; }
    public String getTenureStart() { return tenureStart; } public void setTenureStart(String v) { this.tenureStart = v; }
    public String getTenureEnd() { return tenureEnd; } public void setTenureEnd(String v) { this.tenureEnd = v; }
}
