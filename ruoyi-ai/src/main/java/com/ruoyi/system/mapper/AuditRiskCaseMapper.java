package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditRiskCase;
import java.util.List;

public interface AuditRiskCaseMapper
{
    List<AuditRiskCase> selectRiskCaseList(AuditRiskCase riskCase);
    AuditRiskCase selectRiskCaseById(Long id);
    int insertRiskCase(AuditRiskCase riskCase);
    int updateRiskCase(AuditRiskCase riskCase);
    int deleteRiskCaseByIds(Long[] ids);
}
