package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditReportVersion;
import java.util.List;

public interface AuditReportVersionMapper
{
    List<AuditReportVersion> selectByReportId(Long reportId);
    AuditReportVersion selectById(Long id);
    int insert(AuditReportVersion v);
}
