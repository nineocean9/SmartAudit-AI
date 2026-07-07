package com.ruoyi.system.service.impl;

import com.ruoyi.system.config.AiModelProperties;
import com.ruoyi.system.domain.AuditBasis;
import com.ruoyi.system.domain.AuditProject;
import com.ruoyi.system.mapper.AuditBasisMapper;
import com.ruoyi.system.mapper.AuditProjectMapper;
import com.ruoyi.system.service.IAiDataAnalyzeService;
import com.ruoyi.system.service.IAuditRagService;
import com.ruoyi.system.service.IEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG 检索增强生成服务实现
 * 编排完整 RAG 管线：意图分类 → 向量检索 → Prompt拼装 → 后处理
 *
 * @author ruoyi
 */
@Service
public class AuditRagServiceImpl implements IAuditRagService
{
    private static final Logger log = LoggerFactory.getLogger(AuditRagServiceImpl.class);

    /** 依据引用标记正则：【依据:xxx】或【依据：xxx】 */
    private static final Pattern BASIS_CITE_PATTERN =
            Pattern.compile("【依据[：:]\\s*(.+?)\\s*】");

    /** 项目查询关键词 */
    private static final List<String> PROJECT_KEYWORDS = List.of(
            "审计项目", "项目进度", "整改", "审计记录", "经济责任审计",
            "财务收支", "专项审计", "工程审计", "审计报告"
    );

    @Autowired
    private IEmbeddingService embeddingService;

    @Autowired
    private IAiDataAnalyzeService dataAnalyzeService;

    @Autowired
    private AiModelProperties modelProps;

    @Autowired
    private AuditBasisMapper auditBasisMapper;

    @Autowired
    private AuditProjectMapper auditProjectMapper;

    @Autowired
    private DataSource dataSource;

    /** 审计项目（演示数据） */
    private List<AuditProject> matchedProjects;

    /** 每个项目的问题+整改 */
    private java.util.Map<Long, List<AuditProjectMapper.ProjectIssueWithRect>> projectIssuesMap;

    /** 数据分析结果 */
    private String analysisResult;

    @Override
    public RagContext executeRag(String userQuery)
    {
        RagContext ctx = new RagContext();
        matchedProjects = null;
        projectIssuesMap = null;

        // Step 1: 意图分类
        ctx.intent = classifyIntent(userQuery);
        ctx.userQuery = userQuery;
        log.info("意图分类: {} -> {}", userQuery.substring(0, Math.min(30, userQuery.length())), ctx.intent);

        // Step 2: 向量检索召回 Top-8 依据
        List<Long> basisIds = embeddingService.searchSimilar(userQuery, 8);
        ctx.recalledBasis = new ArrayList<>();
        if (basisIds != null && !basisIds.isEmpty())
        {
            for (Long id : basisIds)
            {
                AuditBasis b = auditBasisMapper.selectAuditBasisById(id);
                if (b != null)
                {
                    ctx.recalledBasis.add(b);
                }
            }
            log.info("向量检索召回 {} 条依据", ctx.recalledBasis.size());
        }
        else
        {
            log.info("向量检索未召回依据，回到关键词匹配兜底");
            ctx.recalledBasis = auditBasisMapper.searchByKeyword(userQuery, null);
        }

        // Step 3: 项目信息检索
        String unitName = extractUnitName(userQuery);
        analysisResult = null;
        if (unitName != null)
        {
            matchedProjects = auditProjectMapper.searchProjects(unitName);
            if (matchedProjects != null && !matchedProjects.isEmpty())
            {
                log.info("项目数据检索命中 {} 个项目", matchedProjects.size());
                projectIssuesMap = new java.util.HashMap<>();
                for (AuditProject p : matchedProjects)
                {
                    List<AuditProjectMapper.ProjectIssueWithRect> issues =
                            auditProjectMapper.searchProjectIssues(p.getId());
                    if (issues != null && !issues.isEmpty())
                    {
                        projectIssuesMap.put(p.getId(), issues);
                    }
                }
            }
        }

        // Step 4: 数据分析（检测到分析指令时自动执行）
        if (userQuery.contains("分析") || userQuery.contains("统计") || userQuery.contains("趋势"))
        {
            try
            {
                var result = dataAnalyzeService.analyze(userQuery);
                if (result.detailLines != null && !result.detailLines.isEmpty() && !"请重新描述分析指令".equals(result.detailLines.get(0)))
                {
                    analysisResult = result.summary;
                    log.info("数据分析完成，获取到 {} 条结果", result.detailLines.size());
                }
            }
            catch (Exception e)
            {
                log.warn("数据分析异常（不影响主流程）", e);
            }
        }

        // Step 5: 拼装增强后的 System Prompt
        ctx.augmentedSystemPrompt = buildAugmentedPrompt(ctx);

        return ctx;
    }

    @Override
    public List<String> extractCitedBasisIds(String aiResponse)
    {
        List<String> cited = new ArrayList<>();
        if (aiResponse == null) return cited;

        Matcher m = BASIS_CITE_PATTERN.matcher(aiResponse);
        while (m.find())
        {
            cited.add(m.group(1).trim());
        }
        return cited;
    }

    @Override
    public void logAiCall(String userQuery, String aiResponse, Intent intent,
                          List<String> citedBasisIds, int tokensUsed, long costTimeMs, int status)
    {
        try (Connection conn = dataSource.getConnection())
        {
            String sql = "INSERT INTO ai_call_log (user_id, intent, prompt, response, "
                       + "cited_basis_ids, model_provider, tokens_used, status, cost_time_ms, create_time) "
                       + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql))
            {
                ps.setLong(1, 1L); // TODO: 获取真实 userId
                ps.setString(2, intent != null ? intent.name() : "QA");
                ps.setString(3, userQuery.length() > 2000 ? userQuery.substring(0, 2000) : userQuery);
                ps.setString(4, aiResponse != null && aiResponse.length() > 4000
                        ? aiResponse.substring(0, 4000) : aiResponse);
                ps.setString(5, citedBasisIds != null ? String.join(",", citedBasisIds) : "");
                ps.setString(6, modelProps.getProvider());
                ps.setInt(7, tokensUsed);
                ps.setInt(8, status);
                ps.setLong(9, costTimeMs);
                ps.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
            }
        }
        catch (Exception e)
        {
            log.error("记录 AI 调用日志失败", e);
        }
    }

    /**
     * 意图分类：基于关键词规则（后续可升级为模型分类）
     */
    private Intent classifyIntent(String query)
    {
        String lower = query.toLowerCase();

        if (containsAny(lower, "取证单", "生成取证", "出具取证", "forensic"))
            return Intent.FORENSIC;

        if (containsAny(lower, "风险", "疑点", "异常", "线索", "risk"))
            return Intent.RISK;

        if (containsAny(lower, "匹配", "查找依据", "查询依据", "法规检索", "match", "依据是什么"))
            return Intent.MATCH;

        if (containsAny(lower, "校验", "检查", "核对", "合规", "是否合规", "审核", "verify"))
            return Intent.FORENSIC; // 走取证单生成流程

        return Intent.QA;
    }

    /**
     * 拼装增强后的 System Prompt（基础审计角色 + 召回依据）
     */
    private String buildAugmentedPrompt(RagContext ctx)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(modelProps.getSystemPrompt());

        // 追加召回的审计依据
        if (ctx.recalledBasis != null && !ctx.recalledBasis.isEmpty())
        {
            sb.append("\n\n## 当前可参考的审计依据（请优先引用）\n\n");
            int idx = 1;
            for (AuditBasis b : ctx.recalledBasis)
            {
                sb.append("### 依据").append(idx).append(": ");
                sb.append(b.getTitle()).append("\n");
                sb.append("> ").append(b.getContent()).append("\n");
                sb.append("- 分类: ").append(b.getCategory())
                  .append(" | 颁发单位: ")
                  .append(b.getIssueOrg() != null ? b.getIssueOrg() : "未知")
                  .append("\n\n");
                idx++;
            }
            sb.append("---\n");
            sb.append("回答时请引用以上依据，格式：【依据:xxx】。如果以上依据不足以回答问题，请明确说明\"建议人工复核，依据不足\"。\n");
        }
        else
        {
            sb.append("\n\n> 注意：未检索到相关审计依据，请如实告知用户并建议人工复核。\n");
        }

        // 追加审计项目数据（如有匹配）
        if (matchedProjects != null && !matchedProjects.isEmpty())
        {
            sb.append("\n\n## 当前查询到的审计项目数据\n\n");
            for (AuditProject p : matchedProjects)
            {
                String statusLabel = switch (p.getStatus()) {
                    case 0 -> "未启动";
                    case 1 -> "实施中";
                    case 2 -> "已归档";
                    default -> "未知";
                };
                sb.append("### 项目：").append(p.getProjectName()).append("\n");
                sb.append("- 被审单位：").append(p.getAuditedUnit()).append("\n");
                sb.append("- 审计类型：").append(p.getAuditType()).append("\n");
                sb.append("- 审计年度：").append(p.getAuditYear()).append("\n");
                sb.append("- 状态：").append(statusLabel).append("\n");

                List<AuditProjectMapper.ProjectIssueWithRect> issues = projectIssuesMap != null
                        ? projectIssuesMap.get(p.getId()) : null;
                if (issues != null && !issues.isEmpty())
                {
                    sb.append("- 发现问题：").append(issues.size()).append("项\n");
                    for (int i = 0; i < issues.size(); i++)
                    {
                        var issue = issues.get(i);
                        String sevLabel = switch (issue.severity) {
                            case 3 -> "高";
                            case 2 -> "中";
                            default -> "低";
                        };
                        String rectLabel = issue.rectStatus == null ? "未整改"
                                : switch (issue.rectStatus) {
                                    case 0 -> "未整改";
                                    case 1 -> "整改中";
                                    case 2 -> "已整改";
                                    default -> "未知";
                                };
                        sb.append("  ").append(i + 1).append(". 【").append(sevLabel).append("】");
                        sb.append(issue.issueDesc).append(" → ").append(rectLabel);
                        if (issue.finishDate != null) sb.append("(").append(issue.finishDate).append(")");
                        sb.append("\n");
                    }
                }
                sb.append("\n");
            }
        }

        // 追加数据分析结果（如有）
        if (analysisResult != null && !analysisResult.isEmpty())
        {
            sb.append("\n\n## 数据分析结果\n\n").append(analysisResult).append("\n");
            sb.append("请基于以上数据进行分析解读，给出审计建议。\n");
        }

        return sb.toString();
    }

    /**
     * 从查询中提取被审单位名
     * 规则：匹配常见学院/部门名称
     */
    private String extractUnitName(String query)
    {
        if (query == null) return null;
        // 不包含项目关键词则跳过
        boolean hasProjKw = PROJECT_KEYWORDS.stream().anyMatch(query::contains);
        if (!hasProjKw) return null;

        // 常见高校部门/学院名
        String[] units = {"信息工程学院", "商学院", "后勤处", "财务处", "图书馆",
                "文学院", "理学院", "外国语学院", "教务处", "人事处", "学生处"};

        for (String unit : units)
        {
            if (query.contains(unit)) return unit;
        }

        // 兜底：匹配"X学院"模式
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(.{2,5}(?:学院|处|部))").matcher(query);
        if (m.find()) return m.group(1);

        return null;
    }

    private boolean containsAny(String text, String... keywords)
    {
        for (String kw : keywords)
        {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
