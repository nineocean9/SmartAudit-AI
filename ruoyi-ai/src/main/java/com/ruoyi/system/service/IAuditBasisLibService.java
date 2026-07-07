package com.ruoyi.system.service;

import com.ruoyi.system.domain.AuditRiskCase;
import com.ruoyi.system.domain.AuditCaseLib;
import java.util.List;

public interface IAuditBasisLibService
{
    // 风险库
    List<AuditRiskCase> selectRiskCaseList(AuditRiskCase riskCase);
    int insertRiskCase(AuditRiskCase riskCase);
    int updateRiskCase(AuditRiskCase riskCase);
    int deleteRiskCaseByIds(Long[] ids);

    // 案例库
    List<AuditCaseLib> selectCaseLibList(AuditCaseLib caseLib);
    int insertCaseLib(AuditCaseLib caseLib);
    int updateCaseLib(AuditCaseLib caseLib);
    int deleteCaseLibByIds(Long[] ids);
}
