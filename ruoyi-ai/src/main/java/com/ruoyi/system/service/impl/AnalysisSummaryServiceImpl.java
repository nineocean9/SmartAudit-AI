package com.ruoyi.system.service.impl;

import com.ruoyi.system.service.IAnalysisSummaryService;
import com.ruoyi.system.service.IAnalysisToolService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * 分析总结服务
 * AI 只负责解释结构化结果，不负责生成图表 JSON
 *
 * @author ruoyi
 */
@Service
public class AnalysisSummaryServiceImpl implements IAnalysisSummaryService
{
    @Autowired
    private ChatLanguageModel chatModel;

    @Override
    public String summarize(IAnalysisToolService.AnalysisDataset dataset, String instruction)
    {
        if (dataset == null || dataset.series == null || dataset.series.isEmpty())
        {
            return "未能提取到可分析的数据。";
        }

        String dataJson = buildSimpleJson(dataset);
        String prompt = "下面是已经统计完成的审计数据（JSON）：\n"
                + dataJson + "\n\n"
                + "请用审计语言完成：\n"
                + "1. 总体情况\n"
                + "2. 风险提示\n"
                + "3. 审计建议\n\n"
                + "要求：不要修改数据，不要重新计算，不要返回 JSON。";

        try
        {
            return chatModel.generate(prompt);
        }
        catch (Exception e)
        {
            return "根据当前数据，共提取到 " + dataset.labels.size() + " 个维度，建议人工进一步复核明细。";
        }
    }

    private String buildSimpleJson(IAnalysisToolService.AnalysisDataset dataset)
    {
        String labels = dataset.labels.stream().map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(", "));
        String values = dataset.series.get(0).data.stream().map(Object::toString)
                .collect(Collectors.joining(", "));
        return "{\n"
                + "  \"title\": \"" + dataset.title + "\",\n"
                + "  \"labels\": [" + labels + "],\n"
                + "  \"values\": [" + values + "]\n"
                + "}";
    }
}
