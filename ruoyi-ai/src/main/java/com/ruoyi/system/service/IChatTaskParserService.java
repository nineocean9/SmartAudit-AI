package com.ruoyi.system.service;

import com.ruoyi.system.domain.ChatTask;
import com.ruoyi.system.domain.AiMessage;
import java.util.List;

/**
 * 聊天任务解析服务
 * 先由 AI 把自然语言解析为结构化任务 ChatTask，再由程序执行
 *
 * @author ruoyi
 */
public interface IChatTaskParserService
{
    /**
     * 解析用户请求
     * @param userInput 用户原始输入
     * @param history   会话历史（可选，帮助理解“这个项目/该文档”等指代）
     * @return 结构化任务
     */
    ChatTask parse(String userInput, List<AiMessage> history);
}
