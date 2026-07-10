package com.ruoyi.system.service.impl;

import com.ruoyi.system.config.AiModelProperties;
import com.ruoyi.system.service.IAiDataAnalyzeService;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.page.TableDataInfo;
import dev.langchain4j.model.chat.ChatLanguageModel;
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
 * AI 数据分析实现
 */
@Service
public class AiDataAnalyzeServiceImpl implements IAiDataAnalyzeService
{
    private static final Logger log = LoggerFactory.getLogger(AiDataAnalyzeServiceImpl.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AiModelProperties modelProps;

    @Autowired
    private ChatLanguageModel chatModel;


    /** SQL 模板定义 */
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
            "SELECT i.issue_desc,i.severity,p.project_name,p.audited_unit FROM audit_issue i JOIN audit_project p ON p.id=i.project_id WHERE i.issue_desc LIKE '%资产%' ORDER BY i.severity DESC",
            "资产管理分析"),
        new AnalysisTemplate("整改|未整改|整改.*情况|整改率",
            "SELECT p.project_name,i.issue_desc,i.severity,r.measure,r.status,r.finish_date FROM audit_issue i JOIN audit_project p ON p.id=i.project_id LEFT JOIN audit_rectification r ON r.issue_id=i.id ORDER BY r.status ASC,i.severity DESC",
            "整改情况分析"),
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

        AnalysisTemplate matched = null;
        for (AnalysisTemplate t : TEMPLATES)
        {
            String[] parts = t.keyword.split("\\|");
            for (String part : parts)
            {
                String clean = part.replace(".*", "").replace("\\W", "");
                if (instruction.contains(clean)) { matched = t; break; }
            }
            if (matched != null) break;
        }

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
                result.summary += count == 0 ? "未查询到相关数据。" : "共查到 **" + count + "** 条记录。\n\n";
            }
            catch (Exception e)
            {
                log.error("分析SQL执行失败", e);
                result.summary += "数据分析执行异常：" + e.getMessage();
            }
        }
        else
        {
            result.summary = "未能识别分析指令。支持：采购/预算/合同/资产/整改/风险分析";
            result.detailLines.add("请重新描述分析指令");
        }
        return result;
    }

    @Override
    public Map<String, Object> analyzeChart(String dataText, String instruction,
                                            String projectName, String keyword,
                                            String sourceType, String createBy)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        try
        {
            // 截取数据（避免超长）
            String truncated = dataText != null && dataText.length() > 6000
                    ? dataText.substring(0, 6000) + "\n..."
                    : dataText;

            // AI 一次调用生成完整 HTML 驾驶舱
            String prompt = buildDashboardPrompt(truncated, projectName);
            String html = chatModel.generate(prompt);

            // 清理 markdown 包裹
            if (html.startsWith("```html")) html = html.substring(7).trim();
            else if (html.startsWith("```")) html = html.substring(3).trim();
            if (html.endsWith("```")) html = html.substring(0, html.length() - 3).trim();

            // 校验：占位符未替换则标记异常
            if (html.contains("<<KPI>>") || html.contains("<<CHARTS>>"))
            {
                log.warn("AI 输出包含未替换的占位符，可能输出被截断或 AI 未遵循指令");
            }
            else if (!html.contains("echarts.init"))
            {
                log.warn("AI 输出的 HTML 中没有 echarts.init 调用，可能缺少图表代码");
            }

            String title = (projectName != null && !projectName.isBlank() ? projectName : "数据") + " - 数据驾驶舱";

            // 第二次 AI 调用：生成文字分析总结（markdown 格式）
            String summaryText;
            try
            {
                String summaryPrompt = buildSummaryPrompt(truncated, projectName);
                summaryText = chatModel.generate(summaryPrompt);
            }
            catch (Exception e2)
            {
                log.warn("AI 生成文字总结失败，使用默认值", e2);
                summaryText = "AI 生成的数据驾驶舱";
            }

            // 保存（html_content 存 HTML，chart_data 存空数组兼容旧逻辑）
            String sql = "INSERT INTO analysis_result (title, chart_data, html_content, summary, project_name, source_type, keyword, create_by, create_time) "
                       + "VALUES (?, '[]'::jsonb, ?, ?, ?, ?, ?, ?, now()) RETURNING id";
            Long analysisId = null;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql))
            {
                ps.setString(1, title);
                ps.setString(2, html);
                ps.setString(3, summaryText);
                ps.setString(4, projectName);
                ps.setString(5, sourceType != null ? sourceType : "chat");
                ps.setString(6, keyword);
                ps.setString(7, createBy);
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next()) analysisId = rs.getLong("id");
                }
            }

            result.put("analysisId", analysisId);
            result.put("title", title);
            result.put("summary", summaryText);
        }
        catch (Exception e)
        {
            log.error("数据分析 HTML 生成失败", e);
            result.put("error", "分析失败: " + e.getMessage());
        }
        return result;
    }

    private String buildDashboardPrompt(String dataText, String projectName)
    {
        String name = projectName != null ? projectName : "数据";
        return "你是一个审计数据可视化助手。\n\n"
            + "复制下面的 HTML 骨架并完整输出，只替换 <<KPI>> 和 <<CHARTS>> 为实际数据。\n"
            + "不要修改骨架的任何其他部分。\n"
            + "========================================\n"
            + "<!DOCTYPE html>\n<html>\n<head>\n"
            + "<meta charset='UTF-8'>\n"
            + "<meta name='viewport' content='width=device-width,initial-scale=1.0'>\n"
            + "<title>📊 " + name + " · 数据驾驶舱</title>\n"
            + "<script src='/static/js/echarts.min.js'></script>\n"
            + "<style>"
            + "body{font-family:-apple-system,'Microsoft YaHei',sans-serif;background:#f0f4f8;padding:24px;margin:0;color:#333}"
            + ".kpi{display:flex;gap:16px;margin-bottom:24px;flex-wrap:wrap}"
            + ".k{flex:1;min-width:150px;background:#fff;border-radius:12px;padding:20px;text-align:center;box-shadow:0 1px 4px rgba(0,0,0,.06)}"
            + ".kl{font-size:13px;color:#999;margin-bottom:4px}.kv{font-size:26px;font-weight:700;color:#2a6df4}"
            + ".g{display:grid;grid-template-columns:1fr 1fr;gap:16px}"
            + "@media(max-width:768px){.g{grid-template-columns:1fr}}"
            + ".c{background:#fff;border-radius:12px;padding:20px;box-shadow:0 1px 4px rgba(0,0,0,.06)}"
            + ".c h3{font-size:15px;color:#333;margin:0 0 10px;padding-bottom:8px;border-bottom:1px solid #eee}"
            + ".cb{width:100%;height:340px}.fw{grid-column:1/-1}"
            + ".ft{text-align:center;color:#bbb;font-size:12px;padding:16px 0}"
            + "</style>\n</head>\n<body>\n"
            + "<h1 style='text-align:center;font-size:22px;color:#1a202c;margin-bottom:24px'>📊 " + name + " · 数据驾驶舱</h1>\n"
            + "<div class='kpi'>\n"
            + "<<KPI>>\n"
            + "</div>\n"
            + "<div class='g' id='g'></div>\n"
            + "<div class='ft'>数据来源：项目资料 ｜ 图表由 ECharts 渲染</div>\n"
            + "<script>\n"
            + "var charts = <<CHARTS>>;\n"
            + "var g=document.getElementById('g');g.innerHTML='';\n"
            + "charts.forEach(function(ch,i){\n"
            + "  var el=document.createElement('div');el.className='c'+(ch.fw?' fw':'');\n"
            + "  el.innerHTML='<h3>'+ch.t+'</h3><div class=\"cb\" id=\"c'+i+'\"></div>';\n"
            + "  g.appendChild(el);\n"
            + "  var dom=document.getElementById('c'+i);\n"
            + "  if(dom)echarts.init(dom).setOption(ch.o);\n"
            + "});\n"
            + "</script>\n</body>\n</html>\n"
            + "========================================\n\n"
            + "【替换说明】\n"
            + "1. <<KPI>> → 替换为实际 KPI 卡片：<div class='k'><div class='kl'>指标名</div><div class='kv'>数值</div></div>（4~6个）\n"
            + "2. <<CHARTS>> → JS 数组，每个元素: {t:'标题', o:ECharts配置, fw:true/false}\n"
            + "   - t: 图表标题\n"
            + "   - o: 完整的 ECharts option（含 tooltip、series、xAxis/yAxis 等）\n"
            + "   - fw: true=跨两列（用于重要图表）\n"
            + "3. 生成 4~6 个图表，混合 bar(柱状)/pie(饼图)/line(折线)/radar(雷达) 类型\n"
            + "4. 不要编造数据\n"
            + "5. 只输出完整 HTML，不要任何额外文字\n\n"
            + "项目：" + name + "\n"
            + "数据：\n" + dataText;
    }

    private String buildSummaryPrompt(String dataText, String projectName)
    {
        String name = projectName != null ? projectName : "数据";
        return "你是一个专业的审计数据分析师。请根据以下数据撰写一份**审计分析总结��告**。\n\n"
            + "要求：\n"
            + "1. 使用 Markdown 格式\n"
            + "2. 包含以下结构：\n"
            + "   ## 数据概况\n"
            + "   简要说明数据来源、时间范围、数据规模\n\n"
            + "   ## 关键发现\n"
            + "   列出 3~5 个关键数据特征或异常点（使用带序号的列表）\n\n"
            + "   ## 风险提示\n"
            + "   列出数据中可能存在的审计风险（如有）\n\n"
            + "   ## 建议\n"
            + "   给出 2~3 条可操作的审计建议\n\n"
            + "3. 语言专业、简洁，适合审计人员阅读\n"
            + "4. 不要编造数据，只基于提供的数据分析\n"
            + "5. 总字数控制在 300~600 字\n\n"
            + "项目：" + name + "\n"
            + "数据：\n" + dataText;
    }

    @Override
    public Map<String, Object> getAnalysisResult(Long id)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        String sql = "SELECT * FROM analysis_result WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    result.put("id", rs.getLong("id"));
                    result.put("title", rs.getString("title"));
                    result.put("summary", rs.getString("summary"));
                    result.put("projectName", rs.getString("project_name"));
                    result.put("sourceType", rs.getString("source_type"));
                    result.put("keyword", rs.getString("keyword"));
                    result.put("createBy", rs.getString("create_by"));
                    result.put("createTime", rs.getTimestamp("create_time") != null
                            ? rs.getTimestamp("create_time").toString() : null);

                    String chartDataJson = rs.getString("chart_data");
                    if (chartDataJson != null)
                    {
                        result.put("charts", JSON.parseArray(chartDataJson));
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("获取分析结果失败", e);
        }
        return result;
    }

    @Override
    public String getAnalysisHtml(Long id)
    {
        String sql = "SELECT html_content, chart_data, title, project_name, source_type, summary FROM analysis_result WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    String html = rs.getString("html_content");
                    if (html != null && !html.isBlank())
                    {
                        return replaceCdnToLocal(html);
                    }

                    // 回退：从 chart_data 生成 HTML（兼容旧记录）
                    String chartJson = rs.getString("chart_data");
                    if (chartJson != null && !chartJson.isBlank() && !"[]".equals(chartJson.trim()) && !"null".equals(chartJson.trim()))
                    {
                        String title = rs.getString("title");
                        String projectName = rs.getString("project_name");
                        String sourceType = rs.getString("source_type");
                        String summary = rs.getString("summary");
                        return buildFallbackHtml(chartJson, title, projectName, sourceType, summary);
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("获取分析 HTML 失败", e);
        }
        return null;
    }

    /**
     * 从 chart_data JSON 生成兼容的 HTML 驾驶舱页面（旧记录回退）
     */
    private String buildFallbackHtml(String chartJson, String title, String projectName, String sourceType, String summary)
    {
        String safeTitle = (title != null ? title : "数据分析驾驶舱").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;").replace("\"", "&quot;");
        String safeMeta = (projectName != null ? "项目: " + projectName.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + " · " : "")
                        + "来源: " + (sourceType != null ? sourceType : "-");
        String safeSummary = (summary != null ? summary : "").replace("\\", "\\\\").replace("'", "\\'").replace("\r", "").replace("\n", "<br>");

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>" + safeTitle + "</title>"
            + "<script src='/static/js/echarts.min.js'></script>"
            + "<style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,'Microsoft YaHei',sans-serif;background:#f0f4f8;padding:20px;color:#333}.h{background:#fff;border-radius:16px;padding:20px 24px;margin-bottom:12px;box-shadow:0 2px 8px rgba(0,0,0,0.04)}.h h1{font-size:22px;margin-bottom:4px}.h p{color:#999;font-size:13px}.kpi{display:flex;gap:12px;margin-bottom:12px;flex-wrap:wrap}.k{flex:1;background:#fff;border-radius:16px;padding:18px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.04);min-width:140px}.kl{font-size:12px;color:#999;margin-bottom:6px}.kv{font-size:28px;font-weight:700;color:#2a6df4}.g{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:12px}@media(max-width:768px){.g{grid-template-columns:1fr}}.gc{background:#fff;border-radius:16px;padding:16px;box-shadow:0 2px 8px rgba(0,0,0,0.04)}.gc h3{margin-bottom:8px;font-size:15px;color:#303133}.cb{width:100%;height:360px}.s{background:#fff;border-radius:16px;padding:20px 24px;line-height:1.8;font-size:14px;box-shadow:0 2px 8px rgba(0,0,0,0.04)}.ft{text-align:center;color:#bbb;font-size:12px;padding:16px 0 4px}.empty{display:flex;align-items:center;justify-content:center;height:360px;color:#ccc;font-size:14px}</style></head><body>"
            + "<div class='h'><h1>" + safeTitle + "</h1><p>" + safeMeta + "</p></div>"
            + "<div class='kpi' id='kpi'></div><div class='g' id='charts'></div>"
            + (safeSummary.isEmpty() ? "" : "<div class='s'>" + safeSummary + "</div>")
            + "<div class='ft'>图表由 ECharts 渲染</div>"
            + "<script>var charts=" + chartJson + ";"
            + "(function(){var c=charts&&charts[0];var d=c&&c.datasets&&c.datasets[0]?c.datasets[0].data:[];var t=d.reduce(function(a,b){return a+Number(b||0)},0);var kpiHtml='<div class=\"k\"><div class=\"kl\">图表数</div><div class=\"kv\">'+charts.length+'</div></div><div class=\"k\"><div class=\"kl\">数据维度</div><div class=\"kv\">'+(c&&c.labels?c.labels.length:0)+'</div></div><div class=\"k\"><div class=\"kl\">数据点</div><div class=\"kv\">'+d.length+'</div></div><div class=\"k\"><div class=\"kl\">合计</div><div class=\"kv\">'+t.toFixed(2)+'</div></div>';document.getElementById('kpi').innerHTML=kpiHtml;})();"
            + "if(!charts||charts.length===0||!charts[0].labels||charts[0].labels.length===0){document.getElementById('charts').innerHTML='<div class=\"gc\" style=\"grid-column:1/-1;text-align:center;padding:60px;color:#ccc\">暂无数据</div>'}else{var g=document.getElementById('charts');charts.forEach(function(ch,i){var card=document.createElement('div');card.className='gc';card.innerHTML='<h3>'+(ch.title||'图表'+(i+1))+'</h3><div class=\"cb\" id=\"ec'+i+'\"></div>';g.appendChild(card);var el=document.getElementById('ec'+i);if(!el)return;var inst=echarts.init(el);if(ch.type==='pie'){inst.setOption({tooltip:{trigger:'item'},legend:{bottom:0,textStyle:{fontSize:12}},series:[{type:'pie',radius:['38%','65%'],data:ch.labels.map(function(l,j){return{name:l,value:(ch.datasets&&ch.datasets[0]?ch.datasets[0].data[j]:0)||0}}),label:{formatter:'{b}: {d}%'},emphasis:{label:{show:true,fontSize:14,fontWeight:'bold'}}}]})}else{inst.setOption({tooltip:{trigger:'axis'},grid:{left:50,right:20,bottom:40,top:20},xAxis:{type:'category',data:ch.labels,axisLabel:{fontSize:11}},yAxis:{type:'value',axisLabel:{fontSize:11}},series:(ch.datasets||[]).map(function(ds){return{name:ds.name,type:ch.type||'bar',data:ds.data||[],smooth:ch.type==='line',itemStyle:{color:function(){return['#2a6df4','#f97316','#22c55e','#a855f7','#ef4444','#eab308'][arguments[1]%6]}}()}}}),legend:{show:(ch.datasets||[]).length>1,textStyle:{fontSize:12}}})}window.addEventListener('resize',function(){inst.resize()})})}"
            + "</script></body></html>";
    }

    @Override
    public TableDataInfo listAnalysisResults(String keyword, Integer pageNum, Integer pageSize)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        String countSql = "SELECT count(*) FROM analysis_result"
                + (keyword != null && !keyword.isBlank()
                    ? " WHERE title ILIKE ? OR project_name ILIKE ? OR keyword ILIKE ?"
                    : "");
        String dataSql = "SELECT * FROM analysis_result"
                + (keyword != null && !keyword.isBlank()
                    ? " WHERE title ILIKE ? OR project_name ILIKE ? OR keyword ILIKE ?"
                    : "")
                + " ORDER BY create_time DESC LIMIT ? OFFSET ?";

        try (Connection conn = dataSource.getConnection())
        {
            long total = 0;
            try (PreparedStatement ps = conn.prepareStatement(countSql))
            {
                if (keyword != null && !keyword.isBlank())
                {
                    ps.setString(1, "%" + keyword + "%");
                    ps.setString(2, "%" + keyword + "%");
                    ps.setString(3, "%" + keyword + "%");
                }
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next()) total = rs.getLong(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(dataSql))
            {
                int idx = 1;
                if (keyword != null && !keyword.isBlank())
                {
                    ps.setString(idx++, "%" + keyword + "%");
                    ps.setString(idx++, "%" + keyword + "%");
                    ps.setString(idx++, "%" + keyword + "%");
                }
                ps.setInt(idx++, pageSize);
                ps.setInt(idx, (pageNum - 1) * pageSize);

                try (ResultSet rs = ps.executeQuery())
                {
                    while (rs.next())
                    {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", rs.getLong("id"));
                        row.put("title", rs.getString("title"));
                        row.put("projectName", rs.getString("project_name"));
                        row.put("sourceType", rs.getString("source_type"));
                        row.put("keyword", rs.getString("keyword"));
                        row.put("createBy", rs.getString("create_by"));
                        row.put("createTime", rs.getTimestamp("create_time") != null
                                ? rs.getTimestamp("create_time").toString() : null);
                        list.add(row);
                    }
                }
            }

            return new TableDataInfo(list, total);
        }
        catch (Exception e)
        {
            log.error("查询分析结果列表失败", e);
            return new TableDataInfo(List.of(), 0);
        }
    }

    @Override
    public int deleteAnalysisResult(Long id)
    {
        String sql = "DELETE FROM analysis_result WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
        catch (Exception e)
        {
            log.error("删除分析结果失败", e);
            return 0;
        }
    }

    @Override
    public int deleteAnalysisResults(Long[] ids)
    {
        if (ids == null || ids.length == 0) return 0;
        StringBuilder sb = new StringBuilder("DELETE FROM analysis_result WHERE id IN (");
        for (int i = 0; i < ids.length; i++) { if (i > 0) sb.append(","); sb.append("?"); }
        sb.append(")");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sb.toString()))
        {
            for (int i = 0; i < ids.length; i++) ps.setLong(i + 1, ids[i]);
            return ps.executeUpdate();
        }
        catch (Exception e)
        {
            log.error("批量删除分析结果失败", e);
            return 0;
        }
    }

    @Override
    public int updateAnalysisResult(Long id, String title, String projectName, String keyword)
    {
        String sql = "UPDATE analysis_result SET title = ?, project_name = ?, keyword = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, title);
            ps.setString(2, projectName);
            ps.setString(3, keyword);
            ps.setLong(4, id);
            return ps.executeUpdate();
        }
        catch (Exception e)
        {
            log.error("更新分析结果失败", e);
            return 0;
        }
    }

    private String replaceCdnToLocal(String html)
    {
        return html.replace("https://cdn.bootcdn.net/ajax/libs/echarts/5.4.3/echarts.min.js", "/static/js/echarts.min.js")
                   .replace("https://cdn.bootcdn.net/ajax/libs/echarts/5.6.0/echarts.min.js", "/static/js/echarts.min.js");
    }

    private record AnalysisTemplate(String keyword, String sql, String label) {}
}
