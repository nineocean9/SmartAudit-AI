package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditMaterialChecklist;
import java.util.List;

public interface AuditMaterialChecklistMapper
{
    AuditMaterialChecklist selectById(Long id);
    List<AuditMaterialChecklist> selectByProjectId(Long projectId);
    int insert(AuditMaterialChecklist m);
    int update(AuditMaterialChecklist m);
    int deleteById(Long id);
}
