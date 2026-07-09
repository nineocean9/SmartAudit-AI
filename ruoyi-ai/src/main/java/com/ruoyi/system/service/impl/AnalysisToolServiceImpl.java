package com.ruoyi.system.service.impl;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.system.service.IAnalysisToolService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 分析工具实现
 * AI 全权负责类型判断 + 数据提取，程序不再硬编码任何标签
 *
 * @author ruoyi
 */
@Service
public class AnalysisToolServiceImpl implements IAnalysisToolService
{
    private static final Logger log = LoggerFactory.getLogger(AnalysisToolServiceImpl.class);

    @Autowired
    private ChatLanguageModel chatModel;

    @Override
    public AnalysisDataset extract(String dataText, String instruction)
    {
        AnalysisDataset dataset = new AnalysisDataset();
        dataset.labels = new ArrayList<>();
        dataset.series = new ArrayList<>();
        dataset.metrics = new LinkedHashMap<>();
        dataset.sourceType = "text";
        dataset.sourceDescription = "项目资料文本";
        dataset.title = instruction != null && !instruction.isBlank() ? instruction : "数据分析";

        if (dataText == null || dataText.isBlank())
        {
            return dataset;
        }

        // AI 一次调用完成类型判断 + 数据提取
        String truncated = dataText.length() > 6000 ? dataText.substring(0, 6000) + "..." : dataText;
        String prompt = "你是一个审计数据分析助手。以下是一份审计相关数据，请完成两件事：\n"
                + "1. 分析数据内容，确定数据是什么类型（不需要从预定义列表中选择，自由描述即可）\n"
                + "2. 从数据中提取结构化数值，生成合适的图表配置\n\n"
                + "输出格式（严格遵守，不要有任何额外文字）：\n"
                + "{\n"
                + "  \"dataType\": \"自由描述，如'利润表分析'/'预算收入构成'/'采购清单对比'/'整改完成率'\",\n"
                + "  \"summary\": \"对这份数据的审计分析总结\",\n"
                + "  \"charts\": [\n"
                + "    {\n"
                + "      \"title\": \"图表标题\",\n"
                + "      \"type\": \"bar|pie|line|radar|scatter\",\n"
                + "      \"labels\": [\"项1\",\"项2\",\"项3\"],\n"
                + "      \"values\": [值1,值2,值3]\n"
                + "    }\n"
                + "  ]\n"
                + "}\n\n"
                + "规则：\n"
                + "- 根据数据内容决定生成几个图表（1-4个），不要强制填满\n"
                + "- 选择合适的图表类型（构成→pie，对比→bar，趋势→line，多维→radar）\n"
                + "- 如果有单位（万元/元/%），在 title 中标明\n"
                + "- 不要编造数据，只使用数据中实际存在的数值\n"
                + "- dataType 是自然语言描述，不需要匹配任何预定义枚举\n"
                + "- values 中必须是纯数字数组，不要包含字符串\n\n"
                + (instruction != null ? "用户分析要求：" + instruction + "\n\n" : "")
                + "数据内容：\n" + truncated;

        try
        {
            String response = chatModel.generate(prompt).trim();
            // 清理 markdown 包裹
            if (response.startsWith("```json")) response = response.substring(7).trim();
            else if (response.startsWith("```")) response = response.substring(3).trim();
            if (response.endsWith("```")) response = response.substring(0, response.length() - 3).trim();
            log.info("AI 分析结果长度: {}", response.length());

            Map<String, Object> parsed = JSON.parseObject(response);
            dataset.sourceType = (String) parsed.getOrDefault("dataType", "text");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> charts = (List<Map<String, Object>>) parsed.get("charts");
            if (charts != null)
            {
                for (Map<String, Object> ch : charts)
                {
                    @SuppressWarnings("unchecked")
                    List<Object> labels = (List<Object>) ch.get("labels");
                    @SuppressWarnings("unchecked")
                    List<Object> values = (List<Object>) ch.get("values");
                    if (labels != null && values != null && !labels.isEmpty())
                    {
                        Series s = new Series();
                        s.name = (String) ch.getOrDefault("title", "数据");
                        s.labels = new ArrayList<>();
                        s.data = new ArrayList<>();
                        for (Object l : labels) s.labels.add(String.valueOf(l));
                        for (Object v : values)
                        {
                            if (v instanceof Number n) s.data.add(BigDecimal.valueOf(n.doubleValue()));
                            else if (v instanceof String str)
                            {
                                try { s.data.add(new BigDecimal(str.replace(",", ""))); }
                                catch (Exception ignore) {}
                            }
                        }
                        dataset.series.add(s);
                    }
                }
            }

            dataset.metrics.put("aiSummary", parsed.getOrDefault("summary", ""));
            dataset.metrics.put("aiDataType", parsed.getOrDefault("dataType", ""));
        }
        catch (Exception e)
        {
            log.error("AI 数据提取失败", e);
            // 回退到简单表格提取
            fallbackTabExtract(dataText, dataset);
        }

        if (dataset.series.isEmpty())
        {
            fallbackTabExtract(dataText, dataset);
        }
        return dataset;
    }

    private void fallbackTabExtract(String dataText, AnalysisDataset dataset)
    {
        Series s = new Series();
        s.name = "数据";
        s.labels = new ArrayList<>();
        s.data = new ArrayList<>();
        String[] lines = dataText.split("\\n");
        for (String line : lines)
        {
            String[] cols = line.split("\\t");
            if (cols.length >= 2)
            {
                String label = cols[0].trim();
                String val = cols[1].trim();
                try
                {
                    BigDecimal num = new BigDecimal(val.replace(",", ""));
                    if (label.length() >= 2 && !Character.isDigit(label.charAt(0)))
                    {
                        s.labels.add(label);
                        s.data.add(num);
                    }
                }
                catch (Exception ignore) {}
            }
        }
        if (!s.data.isEmpty()) dataset.series.add(s);
    }

    // 保留旧方法签名，不再使用硬编码提取
    private boolean looksLikeBudgetAnalysis(String dataText, String instruction) { return false; }
    private void putIfFound(Map<String, BigDecimal> map, String text, String label) {}
}
