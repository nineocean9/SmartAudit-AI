package com.ruoyi.system.service.impl;

import com.ruoyi.system.service.IAiDataAnalyzeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * AI 数据分析实现：自然语言 → SQL模板匹配 → 执行 → 结构化结果
 */
@Service
public class AiDataAnalyzeServiceImpl implements IAiDataAnalyzeService
{
    private static final Logger log = LoggerFactory.getLogger(AiDataAnalyzeServiceImpl.class);

    @Autowired
    private DataSource dataSource;

    /** SQL 模板定义：关键词 → 对应的分析查询 */
    private static final List<AnalysisTemplate> TEMPLATES = List.of(
        new AnalysisTemplate("采购超.*万.*无招标|采购未招标|无招标|招标异常",
            "SELECT i.issue_desc,i.severity,i.create_time,p.project_name,p.audited_unit FROM audit_issue i JOIN audit_project p ON p.id=i.project_id WHERE i.issue_desc LIKE '%招标%' OR i.issue_desc LIKE '%采购%' ORDER BY i.severity DESC",
            "采购合规分析"),

        new AnalysisTemplate("预算执行|预算.*率|执行率.*低|超预算",
            "SELECT p.project_name,p.audited_unit,p.audit_type,p.audit_year,i.issue_desc,i.severity FROM audit_issue i JOIN audit_project p ON p.id=i.project_id WHERE i.issue_desc LIKE '%预算%' ORDER BY i.severity DESC",
            "预算执行分析"),

        new AnalysisTemplate("合同.*不规范|合同|签字|盖章",
            "SELECT i.issue_desc,i.severity,i.create_time,p.project_name FROM audit_issue i JOIN audit_project p ON p.id=i.project_id WHERE i.issue_desc LIKE '%合同%' ORDER BY i.severity DESC",
            "合同合规分析"),

        new AnalysisTemplate("固定资产|资产.*盘点|账实不符|资产",
            "SELECT i.issue_desc,i.severity,p.project_name,p.audited_unit FROM audit_issue i JOIN audit_project p ON p.id=i.project_id WHERE i.issue_desc LIKE '%资产%' OR i.issue_desc LIKE '%固定资产%' ORDER BY i.severity DESC",
            "资产管理分析"),

        new AnalysisTemplate("整改|未整改|整改.*情况|整改率",
            "SELECT p.project_name,i.issue_desc,i.severity,r.measure,r.status AS rect_status,r.finish_date FROM audit_issue i JOIN audit_project p ON p.id=i.project_id LEFT JOIN audit_rectification r ON r.issue_id=i.id ORDER BY r.status ASC,i.severity DESC",
            "整改情况分析"),

        new AnalysisTemplate("各项目|项目概览|审计覆盖|所有项目|项目统计",
            "SELECT p.audited_unit,count(DISTINCT i.id) AS issue_count,count(DISTINCT r.id) FILTER(WHERE r.status=2) AS rect_done FROM audit_project p LEFT JOIN audit_issue i ON i.project_id=p.id LEFT JOIN audit_rectification r ON r.issue_id=i.id GROUP BY p.audited_unit,p.id ORDER BY issue_count DESC",
            "项目统计概览"),

        new AnalysisTemplate("各单位|部门分析|按单位|分布",
            "SELECT p.audited_unit,count(i.id) AS issue_count FROM audit_project p LEFT JOIN audit_issue i ON i.project_id=p.id GROUP BY p.audited_unit ORDER BY issue_count DESC",
            "按单位分布分析"),

        new AnalysisTemplate("违规|高风险|严重|重大问题",
            "SELECT i.issue_desc,i.severity,i.create_time,p.project_name,p.audited_unit FROM audit_issue i JOIN audit_project p ON p.id=i.project_id WHERE i.severity=3 ORDER BY i.create_time DESC",
            "高风险问题分析")
    );

    @Override
    public AnalyzeResult analyze(String instruction)
    {
        AnalyzeResult result = new AnalyzeResult();
        result.detailLines = new ArrayList<>();
        result.rawData = new ArrayList<>();

        if (instruction == null || instruction.isBlank())
        {
            result.summary = "请提供分析指令";
            return result;
        }

        // 1. 匹配 SQL 模板（关键词匹配）
        AnalysisTemplate matched = null;
        for (AnalysisTemplate t : TEMPLATES)
        {
            String[] parts = t.keyword.split("\\|");
            for (String part : parts)
            {
                String clean = part.replace(".*", "").replace("\\W", "");
                if (instruction.contains(clean))
                {
                    matched = t;
                    break;
                }
            }
            if (matched != null) break;
        }

        // 2. 执行 SQL 或给默认结果
        if (matched != null)
        {
            log.info("数据分析匹配模板: {} -> {}", matched.label, matched.sql);
            result.summary = "## " + matched.label + "\n\n根据系统数据查询到以下结果：\n";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(matched.sql);
                 ResultSet rs = ps.executeQuery())
            {
                int count = 0;
                while (rs.next())
                {
                    count++;
                    Map<String, Object> row = new LinkedHashMap<>();
                    StringBuilder line = new StringBuilder();
                    int colCount = rs.getMetaData().getColumnCount();
                    for (int i = 1; i <= colCount; i++)
                    {
                        String val = rs.getString(i);
                        row.put(rs.getMetaData().getColumnLabel(i), val);
                        if (val != null) line.append(val).append(" ");
                    }
                    result.detailLines.add(line.toString().trim());
                    result.rawData.add(row);
                }
                if (count == 0)
                {
                    result.summary += "未查询到相关数据。";
                    result.detailLines.add("无匹配记录");
                }
                else
                {
                    result.summary += "共查到 **" + count + "** 条记录。\n\n";
                }
            }
            catch (Exception e)
            {
                log.error("分析SQL执行失败", e);
                result.summary += "数据分析执行异常：" + e.getMessage();
            }
        }
        else
        {
            result.summary = "未能识别分析指令。支持的分析类型：\n"
                + "- 采购分析（采购未招标、招标异常）\n"
                + "- 预算分析（预算执行率、超预算）\n"
                + "- 合同分析（合同不规范）\n"
                + "- 资产分析（固定资产、账实不符）\n"
                + "- 整改分析（整改情况、未整改）\n"
                + "- 项目统计（各项目概览、审计覆盖）\n"
                + "- 风险分析（高风险、重大问题）";
            result.detailLines.add("请重新描述分析指令");
        }

        return result;
    }

    /** 分析模板 */
    private record AnalysisTemplate(String keyword, String sql, String label) {}
}
