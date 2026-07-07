package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.AuditRiskCase;
import com.ruoyi.system.domain.AuditCaseLib;
import com.ruoyi.system.mapper.AuditRiskCaseMapper;
import com.ruoyi.system.mapper.AuditCaseLibMapper;
import com.ruoyi.system.service.IAuditBasisLibService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuditBasisLibServiceImpl implements IAuditBasisLibService
{
    @Autowired
    private AuditRiskCaseMapper riskCaseMapper;
    @Autowired
    private AuditCaseLibMapper caseLibMapper;

    public List<AuditRiskCase> selectRiskCaseList(AuditRiskCase r) { return riskCaseMapper.selectRiskCaseList(r); }
    public int insertRiskCase(AuditRiskCase r) { return riskCaseMapper.insertRiskCase(r); }
    public int updateRiskCase(AuditRiskCase r) { return riskCaseMapper.updateRiskCase(r); }
    public int deleteRiskCaseByIds(Long[] ids) { return riskCaseMapper.deleteRiskCaseByIds(ids); }

    public List<AuditCaseLib> selectCaseLibList(AuditCaseLib c) { return caseLibMapper.selectCaseLibList(c); }
    public int insertCaseLib(AuditCaseLib c) { return caseLibMapper.insertCaseLib(c); }
    public int updateCaseLib(AuditCaseLib c) { return caseLibMapper.updateCaseLib(c); }
    public int deleteCaseLibByIds(Long[] ids) { return caseLibMapper.deleteCaseLibByIds(ids); }
}
