package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditCaseLib;
import java.util.List;

public interface AuditCaseLibMapper
{
    List<AuditCaseLib> selectCaseLibList(AuditCaseLib caseLib);
    AuditCaseLib selectCaseLibById(Long id);
    int insertCaseLib(AuditCaseLib caseLib);
    int updateCaseLib(AuditCaseLib caseLib);
    int deleteCaseLibByIds(Long[] ids);
}
