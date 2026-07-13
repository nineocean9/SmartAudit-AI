package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditPlanChangeLog;
import java.util.List;

public interface AuditPlanChangeLogMapper
{
    List<AuditPlanChangeLog> selectByPlanId(Long planId);
    int insert(AuditPlanChangeLog log);
}
