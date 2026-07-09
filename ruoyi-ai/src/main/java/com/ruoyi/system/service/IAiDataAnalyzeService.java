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

    AnalyzeResult analyze(String instruction);

    Map<String, Object> analyzeChart(String dataText, String instruction,
                                     String projectName, String keyword,
                                     String sourceType, String createBy);

    Map<String, Object> getAnalysisResult(Long id);

    String getAnalysisHtml(Long id);

    TableDataInfo listAnalysisResults(String keyword, Integer pageNum, Integer pageSize);

    int deleteAnalysisResult(Long id);

    int deleteAnalysisResults(Long[] ids);

    int updateAnalysisResult(Long id, String title, String projectName, String keyword);
}
