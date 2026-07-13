package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditReportOpinion;
import java.util.List;

public interface AuditReportOpinionMapper
{
    List<AuditReportOpinion> selectByReportId(Long reportId);
    int insert(AuditReportOpinion o);
}
