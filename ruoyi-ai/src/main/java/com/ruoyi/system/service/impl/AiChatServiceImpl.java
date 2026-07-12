package com.ruoyi.system.service.impl;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.config.AiModelProperties;
import com.ruoyi.system.domain.AiConversation;
import com.ruoyi.system.domain.ChatTask;
import com.ruoyi.system.mapper.AiChatMapper;
import com.ruoyi.system.service.IAiChatService;
import com.ruoyi.system.service.IAiDataAnalyzeService;
import com.ruoyi.system.service.IAiForensicService;
import com.ruoyi.system.service.IAuditRagService;
import com.ruoyi.system.service.IChatTaskParserService;
import com.ruoyi.system.service.IProjectDocService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class AiChatServiceImpl implements IAiChatService
{
    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    @Autowired private StreamingChatLanguageModel streamingModel;
    @Autowired private AiModelProperties modelProps;
    @Autowired private IAuditRagService auditRagService;
    @Autowired private IAiDataAnalyzeService dataAnalyzeService;
    @Autowired private IAiForensicService aiForensicService;
    @Autowired private IChatTaskParserService chatTaskParserService;
    @Autowired private IProjectDocService projectDocService;
    @Autowired private AiChatMapper aiChatMapper;

    // ================================================================
    // 会话管理
    // ================================================================

    @Override
    public AiConversation createConversation(Long userId, String model)
    {
        AiConversation conv = new AiConversation();
        conv.setUserId(userId);
        conv.setTitle("新对话");
        conv.setModel(model == null ? modelProps.getModelName() : model);
        conv.setCreateBy(SecurityUtils.getUsername());
        aiChatMapper.insertConversation(conv);
        return conv;
    }

    @Override
    public List<AiConversation> listConversations(Long userId)
    {
        List<AiConversation> list = aiChatMapper.selectConversationsByUserId(userId);
        return list != null ? list : new ArrayList<>();
    }

    @Override
    public List<com.ruoyi.system.domain.AiMessage> listMessages(Long conversationId, Long userId)
    {
        AiConversation conv = aiChatMapper.selectConversationById(conversationId, userId);
        if (conv == null) return new ArrayList<>();
        return aiChatMapper.selectMessagesByConversationId(conversationId);
    }

    @Override
    public int renameConversation(Long id, String title, Long userId)
    {
        return aiChatMapper.updateConversationTitle(id, title, userId);
    }

    @Override
    public int deleteConversation(Long id, Long userId)
    {
        aiChatMapper.deleteMessagesByConversationId(id);
        return aiChatMapper.deleteConversation(id, userId);
    }

    // ================================================================
    // 核心：流式对话（支持复合指令）
    // ================================================================

    @Override
    public void chat(Long conversationId, String userInput, Long userId, String username, SseEmitter emitter)
    {
        // 1. 鉴权
        AiConversation conv = aiChatMapper.selectConversationById(conversationId, userId);
        if (conv == null)
        {
            sendSse(emitter, "error", "会话不存在或无权限");
            return;
        }

        // 2. 持久化用户消息
        com.ruoyi.system.domain.AiMessage userMsg = new com.ruoyi.system.domain.AiMessage();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(userInput);
        aiChatMapper.insertMessage(userMsg);

        // 3. 首条消息自动命名
        if ("新对话".equals(conv.getTitle()))
        {
            String autoTitle = userInput.length() > 15 ? userInput.substring(0, 15) + "…" : userInput;
            aiChatMapper.updateConversationTitle(conversationId, autoTitle, userId);
        }

        // 4. 解析为多个任务
        List<com.ruoyi.system.domain.AiMessage> history = aiChatMapper.selectMessagesByConversationId(conversationId);
        List<ChatTask> tasks = chatTaskParserService.parseMulti(userInput, history);
        log.info("复合任务解析: input='{}' -> {} 个任务: {}", userInput, tasks.size(),
                tasks.stream().map(ChatTask::getTaskType).toList());

        // 5. 循环执行每个任务
        for (int i = 0; i < tasks.size(); i++)
        {
            ChatTask task = tasks.get(i);
            String taskType = task.getTaskType() != null ? task.getTaskType() : "QA";

            // 任务间分隔线
            if (i > 0)
            {
                sendSse(emitter, "message", "\n\n---\n\n");
            }

            try
            {
                switch (taskType)
                {
                    case "LIST_PROJECTS":
                        executeListProjects(conversationId, emitter);
                        break;
                    case "READ_PROJECT":
                        executeReadProject(conversationId, task, userInput, emitter);
                        break;
                    case "ANALYZE_PROJECT":
                        executeAnalyzeProject(conversationId, task, username, emitter);
                        break;
                    case "RISK_SCAN":
                        executeStreamingTask(conversationId, task, userInput, emitter, buildRiskScanPrompt(task));
                        break;
                    case "DOC_CHECK":
                        executeStreamingTask(conversationId, task, userInput, emitter, buildDocCheckPrompt(task));
                        break;
                    case "FORENSIC":
                        executeForensic(conversationId, task, userInput, emitter);
                        break;
                    default:
                        executeNormalChat(conversationId, userInput, emitter);
                        break;
                }
            }
            catch (Exception e)
            {
                log.error("任务 {} 执行失败", taskType, e);
                sendSse(emitter, "message", "\n⚠ " + taskType + " 执行失败：" + e.getMessage());
            }
        }

        // 6. 所有任务完成，发送 done
        sendSse(emitter, "done", "[DONE]");
        emitter.complete();
    }

    // ================================================================
    // 各任务执行器（不发 done / 不 complete）
    // ================================================================

    private void executeListProjects(Long conversationId, SseEmitter emitter)
    {
        List<com.ruoyi.system.domain.ProjectDocument> docs = projectDocService.listRecentDocs(10);
        String summary;
        if (docs == null || docs.isEmpty())
        {
            summary = "当前项目库中暂无资料。";
        }
        else
        {
            StringBuilder sb = new StringBuilder("当前项目库中包含以下资料：\n");
            for (int i = 0; i < docs.size(); i++)
            {
                var d = docs.get(i);
                sb.append(i + 1).append(". ").append(d.getFileName())
                        .append("（").append(d.getDocType() != null ? d.getDocType() : "其他").append("）\n");
            }
            summary = sb.toString();
        }
        persistAndSend(conversationId, summary, emitter);
    }

    private void executeReadProject(Long conversationId, ChatTask task, String userInput, SseEmitter emitter)
    {
        String projectName = task.getProjectName();
        if (projectName == null || projectName.isBlank())
        {
            persistAndSend(conversationId, "⚠ 无法识别要查询的项目名称。", emitter);
            return;
        }

        String dataText = projectDocService.getMergedProjectTextByProjectName(projectName);
        if (dataText == null || dataText.isBlank())
        {
            List<com.ruoyi.system.domain.ProjectDocument> docs = projectDocService.listProjectDocsByProjectName(projectName);
            if (docs == null || docs.isEmpty())
            {
                persistAndSend(conversationId, "⚠ 未检索到\"" + projectName + "\"的资料。", emitter);
            }
            else
            {
                StringBuilder sb = new StringBuilder(projectName + " 项目包含以下资料（内容无法解析）：\n");
                for (int i = 0; i < docs.size(); i++)
                    sb.append(i + 1).append(". ").append(docs.get(i).getFileName()).append("\n");
                persistAndSend(conversationId, sb.toString(), emitter);
            }
            return;
        }

        String truncated = dataText.length() > 8000 ? dataText.substring(0, 8000) + "\n..." : dataText;
        String systemPrompt = "你是专业审计助手。以下是"" + projectName + ""的文档资料。\n"
                + "请仅根据资料回答用户关于该项目内容和数据的问题。\n"
                + "注意：不要生成图表、驾驶舱、取证单等内容，只做文字回答。\n\n"
                + "--- 项目资料 ---\n" + truncated + "\n--- 结束 ---";

        executeStreamingWithPrompt(conversationId, systemPrompt, userInput, emitter);
    }

    private void executeAnalyzeProject(Long conversationId, ChatTask task, String username, SseEmitter emitter)
    {
        String projectName = task.getProjectName();
        if (projectName == null || projectName.isBlank())
        {
            persistAndSend(conversationId, "⚠ 无法识别要分析的项目名称。", emitter);
            return;
        }

        String dataText = projectDocService.getMergedProjectTextByProjectName(projectName);
        if (dataText == null || dataText.isBlank())
        {
            persistAndSend(conversationId, "⚠ 未找到\"" + projectName + "\"的可分析资料。", emitter);
            return;
        }

        Map<String, Object> analysis = dataAnalyzeService.analyzeChart(
                dataText, projectName, projectName, null, "chat", username);
        Object analysisId = analysis.get("analysisId");
        String chatReply = "已为\"" + projectName + "\"生成数据驾驶舱，请点击下方链接查看。";

        com.ruoyi.system.domain.AiMessage aiMsg = new com.ruoyi.system.domain.AiMessage();
        aiMsg.setConversationId(conversationId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(chatReply + (analysisId != null ? "\n[dashboard:" + analysisId + "]" : ""));
        aiChatMapper.insertMessage(aiMsg);

        sendSse(emitter, "message", chatReply);
        if (analysisId != null)
        {
            sendSse(emitter, "dashboard", String.valueOf(analysisId));
        }
    }

    private void executeForensic(Long conversationId, ChatTask task, String userInput, SseEmitter emitter)
    {
        String projectName = task.getProjectName();
        if (projectName == null || projectName.isBlank())
        {
            persistAndSend(conversationId, "⚠ 无法识别要生成取证单的项目名称。", emitter);
            return;
        }

        String issue = userInput.replaceAll("请|为|的|生成取证单|出具取证|取证单|【|】", "")
                .replaceAll(projectName, "").trim();
        if (issue.isBlank()) issue = projectName + "项目审计问题";

        String projectContext = null;
        try { projectContext = projectDocService.getMergedProjectTextByProjectName(projectName); }
        catch (Exception e) { log.warn("读取项目资料失败: {}", e.getMessage()); }

        com.ruoyi.system.domain.ForensicDraft draft =
                ((AiForensicServiceImpl) aiForensicService).generateDraft(issue, null, projectName, projectContext);

        if (draft == null)
        {
            persistAndSend(conversationId, "⚠ 取证单生成失败。", emitter);
            return;
        }

        StringBuilder reply = new StringBuilder("## 📋 审计取证单\n\n");
        if (draft.getSuggestion() != null && !draft.getSuggestion().isBlank())
            reply.append(draft.getSuggestion());
        else
            reply.append("**项目：**").append(projectName).append("\n**问题：**").append(issue).append("\n（内容生成失败）");
        reply.append("\n\n---\n*取证单已保存，编号：").append(draft.getId()).append("*");

        persistAndSend(conversationId, reply.toString(), emitter);
    }

    /**
     * 通用流式任务执行器（风险扫描/文档核查共用）
     * 阻塞等待流式完成，供多任务循环使用
     */
    private void executeStreamingTask(Long conversationId, ChatTask task, String userInput, SseEmitter emitter, String systemPrompt)
    {
        if (systemPrompt == null)
        {
            persistAndSend(conversationId, "⚠ 无法执行该任务（缺少项目资料）。", emitter);
            return;
        }
        executeStreamingWithPrompt(conversationId, systemPrompt, userInput, emitter);
    }

    /**
     * 流式问答（阻塞等待完成）
     */
    private void executeStreamingWithPrompt(Long conversationId, String systemPrompt, String userInput, SseEmitter emitter)
    {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));

        List<com.ruoyi.system.domain.AiMessage> history = aiChatMapper.selectMessagesByConversationId(conversationId);
        int max = modelProps.getMaxHistoryMessages();
        int start = Math.max(0, history.size() - max);
        for (int i = start; i < history.size(); i++)
        {
            com.ruoyi.system.domain.AiMessage m = history.get(i);
            if ("user".equals(m.getRole())) messages.add(UserMessage.from(m.getContent()));
            else messages.add(AiMessage.from(m.getContent()));
        }

        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder fullReply = new StringBuilder();

        streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>()
        {
            @Override
            public void onNext(String token)
            {
                fullReply.append(token);
                sendSse(emitter, "message", token);
            }

            @Override
            public void onComplete(Response<AiMessage> response)
            {
                if (fullReply.length() > 0)
                {
                    com.ruoyi.system.domain.AiMessage aiMsg = new com.ruoyi.system.domain.AiMessage();
                    aiMsg.setConversationId(conversationId);
                    aiMsg.setRole("assistant");
                    aiMsg.setContent(fullReply.toString());
                    if (response.tokenUsage() != null)
                        aiMsg.setTokens(response.tokenUsage().totalTokenCount());
                    aiChatMapper.insertMessage(aiMsg);
                }
                latch.countDown();
            }

            @Override
            public void onError(Throwable error)
            {
                log.error("流式调用异常: {}", error.getMessage());
                if (fullReply.length() > 0)
                    sendSse(emitter, "message", "\n\n---\n*⚠ AI 响应中断*");
                else
                    sendSse(emitter, "message", "⚠ AI 服务异常：" + error.getMessage());
                latch.countDown();
            }
        });

        // 阻塞等待流式完成（最多120秒）
        try { latch.await(120, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /**
     * 普通 RAG 问答（阻塞等待完成）
     */
    private void executeNormalChat(Long conversationId, String userInput, SseEmitter emitter)
    {
        final IAuditRagService.RagContext ragCtx = auditRagService.executeRag(userInput);
        List<ChatMessage> messages = buildMessages(conversationId, ragCtx.getAugmentedSystemPrompt());

        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder fullReply = new StringBuilder();
        final long startTime = System.currentTimeMillis();

        streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>()
        {
            @Override
            public void onNext(String token)
            {
                fullReply.append(token);
                sendSse(emitter, "message", token);
            }

            @Override
            public void onComplete(Response<AiMessage> response)
            {
                if (fullReply.length() > 0)
                {
                    com.ruoyi.system.domain.AiMessage aiMsg = new com.ruoyi.system.domain.AiMessage();
                    aiMsg.setConversationId(conversationId);
                    aiMsg.setRole("assistant");
                    aiMsg.setContent(fullReply.toString());
                    int tokens = 0;
                    if (response.tokenUsage() != null)
                    {
                        tokens = response.tokenUsage().totalTokenCount();
                        aiMsg.setTokens(tokens);
                    }
                    aiChatMapper.insertMessage(aiMsg);

                    List<String> citedIds = auditRagService.extractCitedBasisIds(fullReply.toString());
                    long elapsed = System.currentTimeMillis() - startTime;
                    auditRagService.logAiCall(userInput, fullReply.toString(),
                            ragCtx.getIntent(), citedIds, tokens, elapsed, 1);
                }
                latch.countDown();
            }

            @Override
            public void onError(Throwable error)
            {
                log.error("RAG 流式异常: {}", error.getMessage());
                if (fullReply.length() > 0)
                    sendSse(emitter, "message", "\n\n---\n*⚠ AI 响应中断*");
                else
                    sendSse(emitter, "message", "⚠ AI 服务异常：" + error.getMessage());
                latch.countDown();
            }
        });

        try { latch.await(120, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ================================================================
    // Prompt 构建
    // ================================================================

    private String buildRiskScanPrompt(ChatTask task)
    {
        String projectName = task.getProjectName();
        if (projectName == null || projectName.isBlank()) return null;
        String dataText = projectDocService.getMergedProjectTextByProjectName(projectName);
        if (dataText == null || dataText.isBlank()) return null;
        String truncated = dataText.length() > 8000 ? dataText.substring(0, 8000) + "\n..." : dataText;
        return "你是专业审计风险分析师。请基于\"" + projectName + "\"的资料分析风险点。\n"
                + "按高/中/低风险分类，每项说明风险描述、影响和建议。\n\n"
                + "--- 项目资料 ---\n" + truncated + "\n--- 结束 ---";
    }

    private String buildDocCheckPrompt(ChatTask task)
    {
        String projectName = task.getProjectName();
        if (projectName == null || projectName.isBlank()) return null;
        String dataText = projectDocService.getMergedProjectTextByProjectName(projectName);
        if (dataText == null || dataText.isBlank()) return null;
        String truncated = dataText.length() > 8000 ? dataText.substring(0, 8000) + "\n..." : dataText;
        return "你是专业审计文档核查专家。请对\"" + projectName + "\"的文档进行合规性核查。\n"
                + "检查完整性、数据一致性、程序合规性、异常项。\n\n"
                + "--- 项目资料 ---\n" + truncated + "\n--- 结束 ---";
    }

    // ================================================================
    // 工具方法
    // ================================================================

    /**
     * 持久化并发送消息（不发 done，不 complete）
     */
    private void persistAndSend(Long conversationId, String content, SseEmitter emitter)
    {
        com.ruoyi.system.domain.AiMessage aiMsg = new com.ruoyi.system.domain.AiMessage();
        aiMsg.setConversationId(conversationId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(content);
        aiChatMapper.insertMessage(aiMsg);
        sendSse(emitter, "message", content);
    }

    private List<ChatMessage> buildMessages(Long conversationId, String augmentedPrompt)
    {
        List<com.ruoyi.system.domain.AiMessage> history = aiChatMapper.selectMessagesByConversationId(conversationId);
        List<ChatMessage> list = new ArrayList<>();
        if (augmentedPrompt != null && !augmentedPrompt.isEmpty())
            list.add(SystemMessage.from(augmentedPrompt));
        int max = modelProps.getMaxHistoryMessages();
        int start = Math.max(0, history.size() - max);
        for (int i = start; i < history.size(); i++)
        {
            com.ruoyi.system.domain.AiMessage m = history.get(i);
            if ("user".equals(m.getRole())) list.add(UserMessage.from(m.getContent()));
            else list.add(AiMessage.from(m.getContent()));
        }
        return list;
    }

    private void sendSse(SseEmitter emitter, String event, String data)
    {
        try { emitter.send(SseEmitter.event().name(event).data(data)); }
        catch (Exception e) { log.warn("SSE 推送失败: event={}", event); }
    }
}