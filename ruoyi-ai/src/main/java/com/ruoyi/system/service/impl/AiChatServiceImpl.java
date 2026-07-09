package com.ruoyi.system.service.impl;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.config.AiModelProperties;
import com.ruoyi.system.domain.AiConversation;
import com.ruoyi.system.domain.ChatTask;
import com.ruoyi.system.mapper.AiChatMapper;
import com.ruoyi.system.service.IAiChatService;
import com.ruoyi.system.service.IAiDataAnalyzeService;
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

/**
 * AI 对话 Service 实现类
 *
 * 当前版本：
 * 1. 统一聊天入口
 * 2. ChatTaskParser 先解析任务
 * 3. 按 taskType 分流到 项目列表 / 项目资料阅读 / 项目分析 / 普通RAG问答
 *
 * @author ruoyi
 */
@Service
public class AiChatServiceImpl implements IAiChatService
{
    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    @Autowired
    private StreamingChatLanguageModel streamingModel;

    @Autowired
    private AiModelProperties modelProps;

    @Autowired
    private IAuditRagService auditRagService;

    @Autowired
    private IAiDataAnalyzeService dataAnalyzeService;

    @Autowired
    private IChatTaskParserService chatTaskParserService;

    @Autowired
    private IProjectDocService projectDocService;

    @Autowired
    private AiChatMapper aiChatMapper;

    // ----------------------------------------------------------------
    // 会话管理
    // ----------------------------------------------------------------

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
        if (conv == null)
        {
            return new ArrayList<>();
        }
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

    // ----------------------------------------------------------------
    // 核心：流式对话
    // ----------------------------------------------------------------

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

        // 4. AI 先解析任务
        List<com.ruoyi.system.domain.AiMessage> history = aiChatMapper.selectMessagesByConversationId(conversationId);
        ChatTask task = chatTaskParserService.parse(userInput, history);
        String taskType = task != null && task.getTaskType() != null ? task.getTaskType() : "QA";
        log.info("聊天任务解析: input='{}' -> taskType={}, projectName={}, keyword={}, needChart={}",
                userInput,
                taskType,
                task != null ? task.getProjectName() : null,
                task != null ? task.getKeyword() : null,
                task != null ? task.getNeedChart() : null);

        // 5. 分流执行
        switch (taskType)
        {
            case "LIST_PROJECTS":
                handleListProjects(conversationId, emitter);
                return;
            case "READ_PROJECT":
                handleReadProject(conversationId, task, emitter);
                return;
            case "ANALYZE_PROJECT":
                handleAnalyzeProject(conversationId, task, username, emitter);
                return;
            default:
                handleNormalChat(conversationId, userInput, emitter);
        }
    }

    /**
     * 列出项目库资料（泛问）
     */
    private void handleListProjects(Long conversationId, SseEmitter emitter)
    {
        try
        {
            List<com.ruoyi.system.domain.ProjectDocument> docs = projectDocService.listRecentDocs(10);
            String summary;
            if (docs == null || docs.isEmpty())
            {
                summary = "当前项目库中暂无资料。";
            }
            else
            {
                StringBuilder sb = new StringBuilder();
                sb.append("当前项目库中包含以下资料：\n");
                for (int i = 0; i < docs.size(); i++)
                {
                    var d = docs.get(i);
                    sb.append(i + 1).append(". ")
                      .append(d.getFileName())
                      .append("（")
                      .append(d.getDocType() != null ? d.getDocType() : "其他资料")
                      .append("）\n");
                }
                summary = sb.toString();
            }

            persistAndSendAssistantMessage(conversationId, summary, emitter);
        }
        catch (Exception e)
        {
            log.error("项目列表返回失败", e);
            persistAndSendAssistantMessage(conversationId, "⚠ 项目列表获取失败：" + e.getMessage(), emitter);
        }
    }

    /**
     * 读取某项目下资料清单
     */
    private void handleReadProject(Long conversationId, ChatTask task, SseEmitter emitter)
    {
        try
        {
            String projectName = task != null ? task.getProjectName() : null;
            if (projectName == null || projectName.isBlank())
            {
                persistAndSendAssistantMessage(conversationId, "⚠ 无法识别要读取的项目名称。", emitter);
                return;
            }

            List<com.ruoyi.system.domain.ProjectDocument> docs = projectDocService.listProjectDocsByProjectName(projectName);
            String summary;
            if (docs == null || docs.isEmpty())
            {
                summary = "当前项目库中未检索到与“" + projectName + "”相关的资料。";
            }
            else
            {
                StringBuilder sb = new StringBuilder();
                sb.append(projectName).append(" 项目中包含以下资料：\n");
                for (int i = 0; i < docs.size(); i++)
                {
                    var d = docs.get(i);
                    sb.append(i + 1).append(". ")
                      .append(d.getFileName())
                      .append("（")
                      .append(d.getDocType() != null ? d.getDocType() : "其他资料")
                      .append("）\n");
                }
                summary = sb.toString();
            }

            persistAndSendAssistantMessage(conversationId, summary, emitter);
        }
        catch (Exception e)
        {
            log.error("项目资料读取失败", e);
            persistAndSendAssistantMessage(conversationId, "⚠ 项目资料读取失败：" + e.getMessage(), emitter);
        }
    }

    /**
     * 按项目做分析并生成数据驾驶舱
     */
    private void handleAnalyzeProject(Long conversationId, ChatTask task, String username, SseEmitter emitter)
    {
        try
        {
            String projectName = task != null ? task.getProjectName() : null;
            if (projectName == null || projectName.isBlank())
            {
                persistAndSendAssistantMessage(conversationId, "⚠ 无法识别要分析的项目名称。", emitter);
                return;
            }

            String dataText = projectDocService.getMergedProjectTextByProjectName(projectName);
            if (dataText == null || dataText.isBlank())
            {
                persistAndSendAssistantMessage(conversationId,
                        "⚠ 项目库中未检索到与“" + projectName + "”相关的可分析资料。请先上传相关预算/收入/支出文件。",
                        emitter);
                return;
            }

            Map<String, Object> analysis = dataAnalyzeService.analyzeChart(
                    dataText, projectName, projectName, null, "chat", username);
            Object analysisId = analysis.get("analysisId");
            String chatReply = "已为“" + projectName + "”生成数据驾驶舱，请点击下方链接查看完整图表与审计分析。";

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
            sendSse(emitter, "done", "[DONE]");
            emitter.complete();
        }
        catch (Exception e)
        {
            log.error("项目分析失败", e);
            persistAndSendAssistantMessage(conversationId, "⚠ 项目分析失败：" + e.getMessage(), emitter);
        }
    }

    /**
     * 普通 RAG 问答
     */
    private void handleNormalChat(Long conversationId, String userInput, SseEmitter emitter)
    {
        final IAuditRagService.RagContext ragCtx = auditRagService.executeRag(userInput);
        final List<String> citedIds = new ArrayList<>();
        final long startTime = System.currentTimeMillis();

        List<ChatMessage> messages = buildMessages(conversationId, ragCtx.getAugmentedSystemPrompt());

        int totalChars = 0;
        for (ChatMessage cm : messages)
        {
            totalChars += cm.toString().length();
        }
        log.info("发送AI请求: msgCount={}, totalChars={}, queryLen={}", messages.size(), totalChars, userInput.length());

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
                    int tokens = 0;
                    if (response.tokenUsage() != null)
                    {
                        tokens = response.tokenUsage().totalTokenCount();
                        aiMsg.setTokens(tokens);
                    }
                    aiChatMapper.insertMessage(aiMsg);

                    citedIds.addAll(auditRagService.extractCitedBasisIds(fullReply.toString()));
                    long elapsed = System.currentTimeMillis() - startTime;
                    auditRagService.logAiCall(userInput, fullReply.toString(),
                            ragCtx.getIntent(), citedIds, tokens, elapsed, 1);
                }

                sendSse(emitter, "done", "[DONE]");
                emitter.complete();
            }

            @Override
            public void onError(Throwable error)
            {
                log.error("LangChain4j 流式调用异常: {} (queryLen={})", error.getMessage(), userInput.length());

                if (fullReply.length() > 0)
                {
                    com.ruoyi.system.domain.AiMessage aiMsg = new com.ruoyi.system.domain.AiMessage();
                    aiMsg.setConversationId(conversationId);
                    aiMsg.setRole("assistant");
                    aiMsg.setContent(fullReply.toString() + "\n\n---\n*⚠ AI服务响应中断，以上为已生成的部分内容。请尝试缩短提问或刷新后重试。*");
                    aiChatMapper.insertMessage(aiMsg);
                    sendSse(emitter, "message", "\n\n---\n*⚠ AI服务响应中断，以上为已生成的部分内容。请尝试缩短提问或刷新后重试。*");
                }
                else
                {
                    String fallback = "⚠ AI 服务暂时不可用（" + error.getMessage() + "）。建议：\n"
                            + "1. 缩短提问内容后重试\n"
                            + "2. 检查网络连接\n"
                            + "3. 联系管理员检查 API 配额";
                    sendSse(emitter, "message", fallback);
                }
                sendSse(emitter, "done", "[DONE]");
                emitter.complete();
            }
        });
    }

    private void persistAndSendAssistantMessage(Long conversationId, String content, SseEmitter emitter)
    {
        com.ruoyi.system.domain.AiMessage aiMsg = new com.ruoyi.system.domain.AiMessage();
        aiMsg.setConversationId(conversationId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(content);
        aiChatMapper.insertMessage(aiMsg);

        sendSse(emitter, "message", content);
        sendSse(emitter, "done", "[DONE]");
        emitter.complete();
    }

    // ----------------------------------------------------------------
    // 私有工具方法
    // ----------------------------------------------------------------

    /**
     * 构建发送给 AI 的完整消息列表
     */
    private List<ChatMessage> buildMessages(Long conversationId, String augmentedPrompt)
    {
        List<com.ruoyi.system.domain.AiMessage> history = aiChatMapper.selectMessagesByConversationId(conversationId);
        List<ChatMessage> list = new ArrayList<>();

        if (augmentedPrompt != null && !augmentedPrompt.isEmpty())
        {
            list.add(SystemMessage.from(augmentedPrompt));
        }

        int max = modelProps.getMaxHistoryMessages();
        int start = Math.max(0, history.size() - max);
        for (int i = start; i < history.size(); i++)
        {
            com.ruoyi.system.domain.AiMessage m = history.get(i);
            if ("user".equals(m.getRole()))
            {
                list.add(UserMessage.from(m.getContent()));
            }
            else
            {
                list.add(AiMessage.from(m.getContent()));
            }
        }
        return list;
    }

    /**
     * 安全发送 SSE
     */
    private void sendSse(SseEmitter emitter, String event, String data)
    {
        try
        {
            emitter.send(SseEmitter.event().name(event).data(data));
        }
        catch (Exception e)
        {
            log.warn("SSE 推送失败，客户端可能已断开连接: event={}", event);
        }
    }
}
