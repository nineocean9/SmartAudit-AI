package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditArchive;
import java.util.List;

public interface AuditArchiveMapper
{
    List<AuditArchive> selectByProjectId(Long projectId);
    AuditArchive selectById(Long id);
    List<AuditArchive> selectList(AuditArchive q);
    int insert(AuditArchive a);
    int update(AuditArchive a);
    int deleteById(Long id);
}
