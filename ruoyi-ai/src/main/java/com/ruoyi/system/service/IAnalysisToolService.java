package com.ruoyi.system.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 分析工具服务
 * 程序负责从项目资料/文本中提取真实结构化数据
 *
 * @author ruoyi
 */
public interface IAnalysisToolService
{
    class Series
    {
        public String name;
        public List<BigDecimal> data;
        public List<String> labels;   // 每个 series 自己的 labels
    }

    class AnalysisDataset
    {
        public String title;
        public String sourceType;          // excel / csv / text / sql
        public List<String> labels;
        public List<Series> series;
        public Map<String, Object> metrics;
        public String sourceDescription;
    }

    /**
     * 从文本中提取分析数据集
     * @param dataText    原始文本
     * @param instruction 用户分析指令
     * @return 结构化数据集
     */
    AnalysisDataset extract(String dataText, String instruction);
}
