package com.ruoyi.system.service;

import java.util.Map;

/**
 * 图表工具服务
 * 程序负责把结构化数据集转换成 ECharts 可用的 JSON
 *
 * @author ruoyi
 */
public interface IChartToolService
{
    /**
     * 构建仪表盘图表 JSON
     * @param dataset 结构化数据集
     * @param query   用户原始问题
     * @return { title, charts }
     */
    Map<String, Object> buildDashboard(IAnalysisToolService.AnalysisDataset dataset, String query);
}
