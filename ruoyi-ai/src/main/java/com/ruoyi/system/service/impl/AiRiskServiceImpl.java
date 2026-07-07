package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.RiskClue;
import com.ruoyi.system.mapper.RiskClueMapper;
import com.ruoyi.system.service.IAiRiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AiRiskServiceImpl implements IAiRiskService
{
    @Autowired
    private RiskClueMapper riskClueMapper;

    @Override
    public List<RiskClue> selectRiskClueList(RiskClue clue) { return riskClueMapper.selectRiskClueList(clue); }

    @Override
    public int insertRiskClue(RiskClue clue) { return riskClueMapper.insertRiskClue(clue); }

    @Override
    public int updateRiskClue(RiskClue clue) { return riskClueMapper.updateRiskClue(clue); }

    @Override
    public int deleteRiskClueByIds(Long[] ids) { return riskClueMapper.deleteRiskClueByIds(ids); }
}
