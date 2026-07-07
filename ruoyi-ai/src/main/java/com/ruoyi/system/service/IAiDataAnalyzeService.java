package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;

/**
 * AI 数据分析服务
 * 自然语言分析指令 → SQL模板匹配 → 执行 → 返回结果供 AI 解读
 */
public interface IAiDataAnalyzeService
{
    /** 分析结果 */
    class AnalyzeResult
    {
        public String summary;
        public List<String> detailLines;
        public List<Map<String, Object>> rawData;
    }

    /**
     * 执行数据分析指令
     * @param instruction 自然语言指令，如"统计采购超50万无招标项目"
     * @return 结构化结果
     */
    AnalyzeResult analyze(String instruction);
}
