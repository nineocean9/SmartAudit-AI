package com.ruoyi.system.service.impl;

import com.ruoyi.system.service.IAnalysisToolService;
import com.ruoyi.system.service.IChartToolService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 图表工具实现
 * 程序根据结构化数据直接生成数据驾驶舱 JSON
 *
 * @author ruoyi
 */
@Service
public class ChartToolServiceImpl implements IChartToolService
{
    @Override
    public Map<String, Object> buildDashboard(IAnalysisToolService.AnalysisDataset dataset, String query)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> charts = new ArrayList<>();

        if (dataset == null || dataset.series == null || dataset.series.isEmpty())
        {
            result.put("title", "数据分析结果");
            result.put("charts", charts);
            return result;
        }

        // 预算类：一次性生成多图
        if ("budget".equals(dataset.sourceType))
        {
            charts.add(buildIncomePie(dataset));
            charts.add(buildExpensePie(dataset));
            charts.add(buildIncomeExpenseBar(dataset));
            charts.add(buildBalanceBar(dataset));
        }
        else if ("financial".equals(dataset.sourceType))
        {
            // 财务报表：每个 series 生成一个图
            for (int i = 0; i < dataset.series.size(); i++)
            {
                charts.add(buildFinancialChart(dataset, i));
            }
            if (charts.isEmpty()) charts.add(buildGenericChart(dataset, query));
        }
        else
        {
            charts.add(buildGenericChart(dataset, query));
        }

        result.put("title", dataset.title);
        result.put("charts", charts);
        result.put("sourceDescription", dataset.sourceDescription);
        return result;
    }

    private Map<String, Object> buildFinancialChart(IAnalysisToolService.AnalysisDataset dataset, int seriesIdx)
    {
        var s = dataset.series.get(seriesIdx);
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("type", "bar");
        String unit = (String) dataset.metrics.getOrDefault("unit", "");
        chart.put("title", dataset.title + " - " + s.name + (unit.isEmpty() ? "" : "（" + unit + "）"));
        chart.put("labels", dataset.labels);
        chart.put("datasets", List.of(Map.of("name", s.name, "data", s.data)));
        return chart;
    }

    private Map<String, Object> buildIncomePie(IAnalysisToolService.AnalysisDataset dataset)
    {
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("type", "pie");
        chart.put("title", "收入构成");
        chart.put("labels", dataset.labels);
        chart.put("datasets", List.of(Map.of(
                "name", "收入金额",
                "data", dataset.series.get(0).data
        )));
        return chart;
    }

    private Map<String, Object> buildExpensePie(IAnalysisToolService.AnalysisDataset dataset)
    {
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("type", "pie");
        chart.put("title", "支出构成");

        List<String> labels = List.of("工资福利支出", "商品和服务支出", "资本性支出", "对个人和家庭的补助");
        List<BigDecimal> data = new ArrayList<>();
        data.add((BigDecimal) dataset.metrics.getOrDefault("工资福利支出", BigDecimal.ZERO));
        data.add((BigDecimal) dataset.metrics.getOrDefault("商品和服务支出", BigDecimal.ZERO));
        data.add((BigDecimal) dataset.metrics.getOrDefault("资本性支出", BigDecimal.ZERO));
        data.add((BigDecimal) dataset.metrics.getOrDefault("对个人和家庭的补助", BigDecimal.ZERO));

        chart.put("labels", labels);
        chart.put("datasets", List.of(Map.of("name", "支出金额", "data", data)));
        return chart;
    }

    private Map<String, Object> buildIncomeExpenseBar(IAnalysisToolService.AnalysisDataset dataset)
    {
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("type", "bar");
        chart.put("title", "收支对比");
        chart.put("labels", List.of("收入总额", "支出总额"));
        chart.put("datasets", List.of(Map.of(
                "name", "金额",
                "data", List.of(
                        dataset.metrics.getOrDefault("incomeTotal", BigDecimal.ZERO),
                        dataset.metrics.getOrDefault("expenseTotal", BigDecimal.ZERO)
                )
        )));
        return chart;
    }

    private Map<String, Object> buildBalanceBar(IAnalysisToolService.AnalysisDataset dataset)
    {
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("type", "bar");
        chart.put("title", "预算结余/差额");
        chart.put("labels", List.of("结余"));
        chart.put("datasets", List.of(Map.of(
                "name", "差额",
                "data", List.of(dataset.metrics.getOrDefault("balance", BigDecimal.ZERO))
        )));
        return chart;
    }

    private Map<String, Object> buildGenericChart(IAnalysisToolService.AnalysisDataset dataset, String query)
    {
        String chartType = detectBestChartType(dataset, query);
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("type", chartType);
        chart.put("title", dataset.title);
        chart.put("labels", dataset.labels != null ? dataset.labels : List.of());

        List<Map<String, Object>> datasets = new ArrayList<>();
        for (IAnalysisToolService.Series s : dataset.series)
        {
            Map<String, Object> ds = new LinkedHashMap<>();
            ds.put("name", s.name);
            ds.put("data", s.data);
            datasets.add(ds);
        }
        chart.put("datasets", datasets);
        return chart;
    }

    private String detectBestChartType(IAnalysisToolService.AnalysisDataset dataset, String query)
    {
        String q = query == null ? "" : query;
        if (q.contains("占比") || q.contains("构成") || q.contains("比例")) return "pie";
        if (q.contains("趋势") || q.contains("变化")) return "line";
        if (q.contains("分布") || q.contains("对比") || q.contains("比较")) return "bar";
        return "bar";
    }
}
