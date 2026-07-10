package com.ruoyi.system.service;

import com.ruoyi.system.domain.ChatTask;
import com.ruoyi.system.domain.AiMessage;
import java.util.List;

public interface IChatTaskParserService
{
    ChatTask parse(String userInput, List<AiMessage> history);

    /**
     * 解析用户请求为多个任务（支持复合指令）
     */
    List<ChatTask> parseMulti(String userInput, List<AiMessage> history);
}
