package com.ruoyi.system.service;

import com.ruoyi.system.domain.RiskClue;
import java.util.List;

public interface IAiRiskService
{
    List<RiskClue> selectRiskClueList(RiskClue clue);
    int insertRiskClue(RiskClue clue);
    int updateRiskClue(RiskClue clue);
    int deleteRiskClueByIds(Long[] ids);
}
