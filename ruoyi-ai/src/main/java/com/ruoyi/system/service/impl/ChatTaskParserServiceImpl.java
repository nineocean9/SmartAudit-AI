package com.ruoyi.system.service.impl;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.system.domain.AiMessage;
import com.ruoyi.system.domain.ChatTask;
import com.ruoyi.system.service.IChatTaskParserService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目级 ChatTask 解析器
 * AI 只负责把自然语言解析成结构化任务，不做业务分析
 *
 * @author ruoyi
 */
@Service
public class ChatTaskParserServiceImpl implements IChatTaskParserService
{
    @Autowired
    private ChatLanguageModel chatModel;

    @Override
    public ChatTask parse(String userInput, List<AiMessage> history)
    {
        String historyText = "";
        if (history != null && !history.isEmpty())
        {
            historyText = history.stream()
                    .map(m -> m.getRole() + ": " + m.getContent())
                    .collect(Collectors.joining("\n"));
        }

        String prompt = "你是一个任务解析器。请把用户请求解析成 JSON，不要返回任何解释文字。\n\n"
                + "输出格式：\n"
                + "{\n"
                + "  \"taskType\": \"LIST_PROJECTS | READ_PROJECT | ANALYZE_PROJECT | QA\",\n"
                + "  \"projectName\": \"A公司\",\n"
                + "  \"needChart\": true\n"
                + "}\n\n"
                + "规则：\n"
                + "1. 浏览项目库、看有什么资料 → LIST_PROJECTS\n"
                + "2. 针对某个项目提问（如\"A公司预算多少\"\"B项目有什么问题\"\"某公司的收入\"等） → READ_PROJECT，projectName=项目名\n"
                + "3. 要求生成图表/驾驶舱/可视化分析 → ANALYZE_PROJECT，projectName=项目名，needChart=true\n"
                + "4. 不涉及具体项目的通用问题 → QA\n"
                + "5. 只要用户提到了某个具体项目/公司/单位名称并在问问题，就是 READ_PROJECT\n"
                + "6. 不再需要 keyword 字段\n\n"
                + "会话历史：\n" + historyText + "\n\n"
                + "用户输入：" + userInput;

        try
        {
            String json = chatModel.generate(prompt).trim();
            // 去掉 markdown 代码块包裹
            if (json.startsWith("```json")) {
                json = json.substring(7).trim();
            } else if (json.startsWith("```")) {
                json = json.substring(3).trim();
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3).trim();
            }
            return JSON.parseObject(json, ChatTask.class);
        }
        catch (Exception e)
        {
            // 兜底：简单规则，不让流程中断
            ChatTask fallback = new ChatTask();
            if (userInput.contains("项目库") || userInput.contains("有什么资料") || userInput.contains("资料列表"))
            {
                fallback.setTaskType("LIST_PROJECTS");
                fallback.setNeedChart(false);
            }
            else if (userInput.contains("图表") || userInput.contains("驾驶舱") || userInput.contains("可视化"))
            {
                fallback.setTaskType("ANALYZE_PROJECT");
                fallback.setNeedChart(true);
                fallback.setProjectName(extractSimpleProjectName(userInput));
            }
            else if (extractSimpleProjectName(userInput) != null)
            {
                // 提到了具体项目/公司名 → 针对项目提问
                fallback.setTaskType("READ_PROJECT");
                fallback.setNeedChart(false);
                fallback.setProjectName(extractSimpleProjectName(userInput));
            }
            else
            {
                fallback.setTaskType("QA");
                fallback.setNeedChart(false);
            }
            return fallback;
        }
    }

    private String extractSimpleProjectName(String text)
    {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("([\\u4e00-\\u9fa5A-Za-z0-9]{2,}(?:公司|学院|项目|单位))")
                .matcher(text);
        return m.find() ? m.group(1) : null;
    }
}
