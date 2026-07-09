package com.ruoyi.system.service;

import com.ruoyi.common.core.page.TableDataInfo;
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
     */
    AnalyzeResult analyze(String instruction);

    /**
     * AI 数据分析并生成图表
     */
    Map<String, Object> analyzeChart(String dataText, String instruction,
                                     String projectName, String keyword,
                                     String sourceType, String createBy);

    /**
     * 获取已保存的分析结果
     */
    Map<String, Object> getAnalysisResult(Long id);

    /**
     * 获取分析结果的 HTML 内容
     */
    String getAnalysisHtml(Long id);

    /**
     * 列出分析结果列表
     */
    TableDataInfo listAnalysisResults(String keyword, Integer pageNum, Integer pageSize);
}
