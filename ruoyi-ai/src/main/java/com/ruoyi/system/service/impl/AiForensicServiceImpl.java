package com.ruoyi.system.service.impl;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.ForensicDraft;
import com.ruoyi.system.domain.AuditBasis;
import com.ruoyi.system.mapper.AuditBasisMapper;
import com.ruoyi.system.mapper.ForensicDraftMapper;
import com.ruoyi.system.service.IAiForensicService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiForensicServiceImpl implements IAiForensicService
{
    private static final Logger log = LoggerFactory.getLogger(AiForensicServiceImpl.class);

    @Autowired
    private ForensicDraftMapper forensicDraftMapper;

    @Autowired
    private AuditBasisMapper auditBasisMapper;

    @Autowired
    private ChatLanguageModel chatModel;

    @Autowired
    private DataSource dataSource;

    @Override
    public List<ForensicDraft> selectForensicDraftList(ForensicDraft draft)
    {
        return forensicDraftMapper.selectForensicDraftList(draft);
    }

    @Override
    public ForensicDraft selectForensicDraftById(Long id)
    {
        ForensicDraft draft = forensicDraftMapper.selectForensicDraftById(id);
        if (draft != null)
        {
            draft.setBasisList(resolveBasisList(draft.getBasisIds()));
        }
        return draft;
    }

    private List<AuditBasis> resolveBasisList(String basisIds)
    {
        List<AuditBasis> result = new ArrayList<>();
        if (basisIds == null || basisIds.isBlank()) return result;
        for (String idText : basisIds.split(","))
        {
            try
            {
                AuditBasis basis = auditBasisMapper.selectAuditBasisById(Long.parseLong(idText.trim()));
                if (basis != null) result.add(basis);
            }
            catch (NumberFormatException e)
            {
                log.warn("忽略无效的取证依据ID: {}", idText);
            }
        }
        return result;
    }

    @Override
    public ForensicDraft generateDraft(String issue, String basisIds)
    {
        return generateDraft(issue, basisIds, null, null);
    }

    @Override
    public ForensicDraft generateDraft(String issue, String basisIds, Long projectId)
    {
        String projectName = null;
        String projectContext = null;
        if (projectId != null)
        {
            projectName = lookupProjectName(projectId);
            projectContext = loadProjectContext(projectId);
        }
        ForensicDraft draft = generateDraft(issue, basisIds, projectName, projectContext);
        if (projectId != null)
        {
            draft.setProjectId(projectId);
            forensicDraftMapper.updateForensicDraft(draft);
        }
        return draft;
    }

    /**
     * 生成取证单（带项目上下文）
     */
    public ForensicDraft generateDraft(String issue, String basisIds, String projectName, String projectContext)
    {
        // 1. 查询关联的审计依据内容
        String basisContent = loadBasisContent(basisIds);

        // 2. 构建 AI prompt 生成取证单
        String prompt = buildForensicPrompt(issue, projectName, projectContext, basisContent);

        String aiResult;
        try
        {
            aiResult = chatModel.generate(prompt);
        }
        catch (Exception e)
        {
            log.error("AI 生成取证单失败", e);
            aiResult = "取证单生成失败，请重试。";
        }

        // 3. 查找项目ID
        Long projectId = lookupProjectId(projectName);

        // 4. 保存取证单
        ForensicDraft draft = new ForensicDraft();
        draft.setProjectId(projectId);
        draft.setIssue(issue);
        draft.setBasisIds(basisIds);
        draft.setSuggestion(aiResult);
        try
        {
            draft.setCreateBy(SecurityUtils.getUsername());
        }
        catch (Exception e)
        {
            draft.setCreateBy("system");
        }
        forensicDraftMapper.insertForensicDraft(draft);
        return draft;
    }

    private String buildForensicPrompt(String issue, String projectName, String projectContext, String basisContent)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的审计取证专家。请根据以下信息生成一份**完整的审计取证单**。\n\n");
        sb.append("要求使用 Markdown 格式，包含以下板块：\n");
        sb.append("## 一、基本信息\n");
        sb.append("列出：项目名称、被审计单位、取证日期、取证编号\n\n");
        sb.append("## 二、审计问题描述\n");
        sb.append("详细描述发现的审计问题，包括问题性质、涉及金额、时间范围\n\n");
        sb.append("## 三、审计事实与证据\n");
        sb.append("列出支撑问题的具体事实和证据材料（编号列表）\n\n");
        sb.append("## 四、适用法规依据\n");
        sb.append("引用具体的法规条文作为审计定性依据\n\n");
        sb.append("## 五、审计结论\n");
        sb.append("对问题作出明确的审计定性\n\n");
        sb.append("## 六、整改建议\n");
        sb.append("给出具体可操作的整改措施建议\n\n");
        sb.append("---\n\n");

        if (projectName != null && !projectName.isBlank())
        {
            sb.append("**项目名称：**").append(projectName).append("\n\n");
        }

        sb.append("**审计问题：**").append(issue).append("\n\n");

        if (basisContent != null && !basisContent.isBlank())
        {
            sb.append("**可参考的审计依据：**\n").append(basisContent).append("\n\n");
            sb.append("法规引用约束：只能引用上方已提供的审计依据，不得虚构法规名称、文号或条款；")
                    .append("依据内容无法支撑的事项，请明确标注“待人工补充依据”。\n\n");
        }
        else
        {
            sb.append("**审计依据：** 当前依据库未检索到可确认的法规。\n\n");
            sb.append("法规引用约束：不得依据模型记忆自行编造法规名称、文号或条款，")
                    .append("“四、适用法规依据”请填写“待人工补充依据”。\n\n");
        }

        if (projectContext != null && !projectContext.isBlank())
        {
            String truncated = projectContext.length() > 4000 ? projectContext.substring(0, 4000) + "\n..." : projectContext;
            sb.append("**项目资料摘要：**\n").append(truncated).append("\n\n");
        }

        sb.append("请直接输出取证单内容，不要输出其他解释文字。");
        return sb.toString();
    }

    /**
     * 加载审计依据内容
     */
    private String loadBasisContent(String basisIds)
    {
        if (basisIds == null || basisIds.isBlank()) return null;
        List<String> contents = new ArrayList<>();
        try (Connection conn = dataSource.getConnection())
        {
            for (String idStr : basisIds.split(","))
            {
                long id = Long.parseLong(idStr.trim());
                try (PreparedStatement ps = conn.prepareStatement("SELECT title, content FROM audit_basis WHERE id = ?"))
                {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery())
                    {
                        if (rs.next())
                        {
                            String title = rs.getString("title");
                            String content = rs.getString("content");
                            contents.add("【" + title + "】" + (content != null ? content : ""));
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("加载审计依据失败", e);
        }
        return contents.isEmpty() ? null : String.join("\n", contents);
    }

    /**
     * 根据项目名查找项目ID
     */
    private Long lookupProjectId(String projectName)
    {
        if (projectName == null || projectName.isBlank()) return null;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM audit_project WHERE project_name ILIKE ? LIMIT 1"))
        {
            ps.setString(1, "%" + projectName + "%");
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getLong("id"); }
        }
        catch (Exception e) { /* ignore */ }
        return null;
    }

    private String lookupProjectName(Long projectId)
    {
        if (projectId == null) return null;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT project_name FROM audit_project WHERE id = ?"))
        {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next() ? rs.getString("project_name") : null;
            }
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String loadProjectContext(Long projectId)
    {
        if (projectId == null) return null;
        List<String> parts = new ArrayList<>();
        String sql = "SELECT doc_type, file_name, content_text FROM project_document WHERE project_id = ? AND status = 1 ORDER BY create_time DESC LIMIT 6";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    String content = rs.getString("content_text");
                    if (content == null || content.isBlank()) continue;
                    parts.add("【" + rs.getString("doc_type") + "】" + rs.getString("file_name") + "\n" + content);
                }
            }
        }
        catch (Exception e)
        {
            log.warn("加载项目资料上下文失败: projectId={}", projectId, e);
        }
        return parts.isEmpty() ? null : String.join("\n\n", parts);
    }

    @Override
    public int updateForensicDraft(ForensicDraft draft)
    {
        return forensicDraftMapper.updateForensicDraft(draft);
    }

    @Override
    public int deleteForensicDraftByIds(Long[] ids)
    {
        return forensicDraftMapper.deleteForensicDraftByIds(ids);
    }
}
