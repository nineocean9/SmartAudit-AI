package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AiCallLog;
import java.util.List;

public interface AiCallLogMapper
{
    List<AiCallLog> selectAiCallLogList(AiCallLog log);
    AiCallLog selectAiCallLogById(Long id);
    int insertAiCallLog(AiCallLog log);
}
