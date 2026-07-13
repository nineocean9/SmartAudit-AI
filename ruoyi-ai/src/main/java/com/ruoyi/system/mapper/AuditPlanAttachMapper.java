package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditPlanAttachment;
import java.util.List;

public interface AuditPlanAttachMapper
{
    List<AuditPlanAttachment> selectByPlanId(Long planId);
    int insert(AuditPlanAttachment att);
    int deleteById(Long id);
    int deleteByPlanId(Long planId);
}
