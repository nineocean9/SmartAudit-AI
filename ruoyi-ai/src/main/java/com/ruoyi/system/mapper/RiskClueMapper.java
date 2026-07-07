package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.RiskClue;
import java.util.List;

public interface RiskClueMapper
{
    List<RiskClue> selectRiskClueList(RiskClue clue);
    RiskClue selectRiskClueById(Long id);
    int insertRiskClue(RiskClue clue);
    int updateRiskClue(RiskClue clue);
    int deleteRiskClueByIds(Long[] ids);
}
