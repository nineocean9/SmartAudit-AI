package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 被审单位变更日志实体
 * 对应数据库表 audit_unit_change_log
 *
 * @author ruoyi
 */
public class AuditUnitChangeLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long unitId;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String changeBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date changeTime;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long v) { this.unitId = v; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String v) { this.fieldName = v; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String v) { this.oldValue = v; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String v) { this.newValue = v; }
    public String getChangeBy() { return changeBy; }
    public void setChangeBy(String v) { this.changeBy = v; }
    public Date getChangeTime() { return changeTime; }
    public void setChangeTime(Date v) { this.changeTime = v; }
}
