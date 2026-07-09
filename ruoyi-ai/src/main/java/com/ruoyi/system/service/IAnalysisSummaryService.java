package com.ruoyi.system.service;

/**
 * 分析总结服务
 * AI 只负责解释分析数据，不负责生成图表数据
 *
 * @author ruoyi
 */
public interface IAnalysisSummaryService
{
    /**
     * 对结构化数据集生成审计总结
     * @param dataset     结构化数据集
     * @param instruction 用户指令
     * @return 审计分析总结
     */
    String summarize(IAnalysisToolService.AnalysisDataset dataset, String instruction);
}
