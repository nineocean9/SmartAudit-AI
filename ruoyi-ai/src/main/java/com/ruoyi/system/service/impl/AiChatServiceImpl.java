package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.config.AiModelProperties;
import com.ruoyi.system.domain.AiConversation;
import com.ruoyi.system.mapper.AiChatMapper;
import com.ruoyi.system.mapper.AuditBasisMapper;
import com.ruoyi.system.service.IAiChatService;
import com.ruoyi.system.service.IAuditRagService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;

/**
 * AI 对话 Service 实现类 —— 基于 LangChain4j
 *
 * 核心设计思路： 业务代码只依赖 LangChain4j 的 StreamingChatLanguageModel 接口， 不感知底层是哪个厂商的模型。 切换模型（通义千问 / DeepSeek / OpenAI / 本地
 * Ollama） 只需修改 application.yml 中的 ai.model.provider，本类代码零改动。
 * 
 * @author ruoyi
 */
@Service
public class AiChatServiceImpl implements IAiChatService
{
    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    /**
     * 流式聊天语言模型
     * 由 AiModelConfig 工厂 Bean 根据 application.yml 中的 provider 配置动态注入，
     * 可能是 OpenAiStreamingChatModel / OllamaStreamingChatModel 等具体实现
     */
    @Autowired
    private StreamingChatLanguageModel streamingModel;

    /** AI 模型配置属性（模型名称、温度、最大 Token、系统提示词等） */
    @Autowired
    private AiModelProperties modelProps;

    @Autowired
    private IAuditRagService auditRagService;

    @Autowired
    private AiChatMapper aiChatMapper;

    @Autowired
    private AuditBasisMapper auditBasisMapper;

    // ----------------------------------------------------------------
    // 会话管理
    // ----------------------------------------------------------------

    /**
     * 新建会话 model 参数为空时，取 application.yml 中配置的默认模型名
     */
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
        // 防御性处理：MyBatis 查无数据时可能返回 null，统一转为空列表
        return list != null ? list : new ArrayList<>();
    }

    /**
     * 查询会话消息
     * 先鉴权（校验会话是否属于当前用户），再查消息，防止越权访问
     */
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

    /**
     * 删除会话 先物理删除该会话下所有消息，再逻辑删除会话本身
     */
    @Override
    public int deleteConversation(Long id, Long userId)
    {
        aiChatMapper.deleteMessagesByConversationId(id);
        return aiChatMapper.deleteConversation(id, userId);
    }

    // ----------------------------------------------------------------
    // 核心：LangChain4j 流式对话
    // ----------------------------------------------------------------

    /**
     * 发送消息，流式返回 AI 回复
     *
     * 完整流程：
     *   1. 鉴权 —— 校验会话归属
     *   2. 持久化用户消息
     *   3. 首条消息自动命名会话标题
     *   4. RAG管线 —— 向量检索召回依据 → 增强System Prompt
     *   5. 加载历史消息 + 增强Prompt → 构建 LangChain4j 消息列表
     *   6. 调用流式模型，逐 token 通过 SSE 推送给前端
     *   7. 流式结束后持久化完整 AI 回复及 Token 消耗
     *   8. 提取引用的依据编号 → 写入 ai_call_log 审计日志
     */
    @Override
    public void chat(Long conversationId, String userInput, Long userId, SseEmitter emitter)
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

        // 4. 执行 RAG 管线（向量检索 → 增强 Prompt）
        final IAuditRagService.RagContext ragCtx = auditRagService.executeRag(userInput);
        final List<String> citedIds = new ArrayList<>();
        final long startTime = System.currentTimeMillis();

        // 5. 构建消息上下文（含召回依据的增强 System Prompt + 历史消息）
        List<ChatMessage> messages = buildMessages(conversationId, ragCtx.getAugmentedSystemPrompt());

        // 6. 流式调用
        StringBuilder fullReply = new StringBuilder();

        streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>()
        {
            /**
             * 每收到一个 token 片段时触发
             * 累积到 fullReply 的同时，实时通过 SSE 推送给前端实现打字机效果
             */
            @Override
            public void onNext(String token)
            {
                fullReply.append(token);
                sendSse(emitter, "message", token);
            }

            /**
             * 流式输出全部完成时触发
             * 将完整回复持久化到数据库，并通知前端对话结束
             */
            @Override
            public void onComplete(Response<AiMessage> response)
            {
                // 7. 持久化 AI 完整回复
                if (fullReply.length() > 0)
                {
                    com.ruoyi.system.domain.AiMessage aiMsg = new com.ruoyi.system.domain.AiMessage();
                    aiMsg.setConversationId(conversationId);
                    aiMsg.setRole("assistant");
                    aiMsg.setContent(fullReply.toString());
                    // 记录本次对话消耗的 Token 总数（用于统计和计费）
                    int tokens = 0;
                    if (response.tokenUsage() != null)
                    {
                        tokens = response.tokenUsage().totalTokenCount();
                        aiMsg.setTokens(tokens);
                    }
                    aiChatMapper.insertMessage(aiMsg);

                    // 8. 记录调用日志
                    citedIds.addAll(auditRagService.extractCitedBasisIds(fullReply.toString()));
                    long elapsed = System.currentTimeMillis() - startTime;
                    auditRagService.logAiCall(userInput, fullReply.toString(),
                            ragCtx.getIntent(), citedIds, tokens, elapsed, 1);
                }
                // 通知前端流式结束，前端收到后关闭 EventSource
                sendSse(emitter, "done", "[DONE]");
                emitter.complete();
            }

            /**
             * 调用过程中发生异常时触发（网络超时、模型服务异常等）
             */
            @Override
            public void onError(Throwable error)
            {
                log.error("LangChain4j 流式调用异常", error);
                sendSse(emitter, "error", "AI 服务异常：" + error.getMessage());
                emitter.completeWithError(error);
            }
        });
    }

    // ----------------------------------------------------------------
    // 私有工具方法
    // ----------------------------------------------------------------

    /**
     * 构建发送给 AI 的完整消息列表
     *
     * 结构：[增强SystemMessage(含召回依据)] + [历史 user/assistant 消息（最近 N 条）]
     *
     * @param conversationId    会话 ID
     * @param augmentedPrompt   RAG增强后的完整系统提示词（含召回依据）
     * @return LangChain4j 格式的消息列表
     */
    private List<ChatMessage> buildMessages(Long conversationId, String augmentedPrompt)
    {
        List<com.ruoyi.system.domain.AiMessage> history = aiChatMapper.selectMessagesByConversationId(conversationId);

        List<ChatMessage> list = new ArrayList<>();

        // 插入增强后的系统提示词（审计角色 + 召回依据）
        if (augmentedPrompt != null && !augmentedPrompt.isEmpty())
        {
            list.add(SystemMessage.from(augmentedPrompt));
        }

        // 裁剪历史消息，只保留最近 maxHistoryMessages 条
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
                // assistant 消息转为 LangChain4j 的 AiMessage 类型
                list.add(AiMessage.from(m.getContent()));
            }
        }
        return list;
    }

    /**
     * 向前端安全推送一条 SSE 事件
     * 捕获异常防止因客户端断开连接而导致整个线程崩溃
     *
     * @param emitter SSE 发射器
     * @param event   事件名称（message / done / error）
     * @param data    事件数据
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
