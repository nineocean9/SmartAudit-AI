package com.ruoyi.system.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.ruoyi.system.domain.AiMessage;
import com.ruoyi.system.domain.ChatTask;
import com.ruoyi.system.service.IChatTaskParserService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatTaskParserServiceImpl implements IChatTaskParserService
{
    private static final Logger log = LoggerFactory.getLogger(ChatTaskParserServiceImpl.class);

    @Autowired
    private ChatLanguageModel chatModel;

    @Override
    public ChatTask parse(String userInput, List<AiMessage> history)
    {
        List<ChatTask> tasks = parseMulti(userInput, history);
        return tasks.isEmpty() ? fallbackSingle(userInput) : tasks.get(0);
    }

    @Override
    public List<ChatTask> parseMulti(String userInput, List<AiMessage> history)
    {
        String historyText = "";
        if (history != null && !history.isEmpty())
        {
            historyText = history.stream()
                    .map(m -> m.getRole() + ": " + m.getContent())
                    .collect(Collectors.joining("\n"));
        }

        String prompt = "你是一个任务解析器。用户可能在一条消息中包含多个意图。\n"
                + "请把用户请求拆分为多个任务，输出 JSON 数组，不要返回任何解释文字。\n\n"
                + "输出格式（JSON 数组，按执行顺序排列）：\n"
                + "[\n"
                + "  {\"taskType\": \"READ_PROJECT\", \"projectName\": \"A公司\", \"needChart\": false},\n"
                + "  {\"taskType\": \"ANALYZE_PROJECT\", \"projectName\": \"A公司\", \"needChart\": true}\n"
                + "]\n\n"
                + "可用的 taskType：\n"
                + "- LIST_PROJECTS：浏览项目库、看有什么资料\n"
                + "- READ_PROJECT：针对某项目提问（预算多少、有什么问题、项目里有什么等）\n"
                + "- ANALYZE_PROJECT：生成图表/驾驶舱/可视化数据分析\n"
                + "- RISK_SCAN：扫描风险/风险点/风险线索\n"
                + "- DOC_CHECK：核查文档/文档合规检查\n"
                + "- FORENSIC：生成取证单/出具取证\n"
                + "- QA：不涉及具体项目的通用问题\n\n"
                + "规则：\n"
                + "1. 如果只有1个意图 → 返回只含1个元素的数组\n"
                + "2. 如果有多个意图 → 拆分为多个任务\n"
                + "3. 执行顺序：先查询(READ_PROJECT) → 再分析(ANALYZE_PROJECT) → 再生成(FORENSIC)\n"
                + "4. 同一项目名的多个任务共享 projectName\n"
                + "5. 相同 taskType 不重复\n"
                + "6. 最多5个任务\n\n"
                + "会话历史：\n" + historyText + "\n\n"
                + "用户输入：" + userInput;

        try
        {
            String json = chatModel.generate(prompt).trim();
            // 清理 markdown 包裹
            if (json.startsWith("```json")) json = json.substring(7).trim();
            else if (json.startsWith("```")) json = json.substring(3).trim();
            if (json.endsWith("```")) json = json.substring(0, json.length() - 3).trim();

            List<ChatTask> tasks;
            if (json.startsWith("["))
            {
                // JSON 数组
                tasks = JSONArray.parseArray(json, ChatTask.class);
            }
            else if (json.startsWith("{"))
            {
                // 单个对象，包装为数组
                tasks = new ArrayList<>();
                tasks.add(JSON.parseObject(json, ChatTask.class));
            }
            else
            {
                log.warn("AI 返回非 JSON 内容: {}", json.substring(0, Math.min(100, json.length())));
                return fallbackMulti(userInput);
            }

            // 过滤无效任务，最多5个
            tasks.removeIf(t -> t == null || t.getTaskType() == null || t.getTaskType().isBlank());
            if (tasks.size() > 5) tasks = tasks.subList(0, 5);
            if (tasks.isEmpty()) return fallbackMulti(userInput);

            return tasks;
        }
        catch (Exception e)
        {
            log.warn("任务解析失败，使用 fallback: {}", e.getMessage());
            return fallbackMulti(userInput);
        }
    }

    /**
     * fallback：从用户输入中用关键词匹配多个任务
     */
    private List<ChatTask> fallbackMulti(String userInput)
    {
        List<ChatTask> tasks = new ArrayList<>();
        String projectName = extractSimpleProjectName(userInput);

        // 按优先级检测多个意图
        if (userInput.contains("有什么") || userInput.contains("里有") || userInput.contains("包含"))
        {
            ChatTask t = new ChatTask();
            t.setTaskType(projectName != null ? "READ_PROJECT" : "LIST_PROJECTS");
            t.setProjectName(projectName);
            tasks.add(t);
        }

        if (userInput.contains("预算") || userInput.contains("收入") || userInput.contains("支出") || userInput.contains("多少"))
        {
            // 避免和上面重复的 READ_PROJECT
            boolean alreadyHasRead = tasks.stream().anyMatch(t -> "READ_PROJECT".equals(t.getTaskType()));
            if (!alreadyHasRead && projectName != null)
            {
                ChatTask t = new ChatTask();
                t.setTaskType("READ_PROJECT");
                t.setProjectName(projectName);
                tasks.add(t);
            }
        }

        if (userInput.contains("数据分析") || userInput.contains("图表") || userInput.contains("驾驶舱") || userInput.contains("可视化"))
        {
            ChatTask t = new ChatTask();
            t.setTaskType("ANALYZE_PROJECT");
            t.setProjectName(projectName);
            t.setNeedChart(true);
            tasks.add(t);
        }

        if (userInput.contains("取证单") || userInput.contains("出具取证") || userInput.contains("生成取证"))
        {
            ChatTask t = new ChatTask();
            t.setTaskType("FORENSIC");
            t.setProjectName(projectName);
            tasks.add(t);
        }

        if (userInput.contains("风险扫描") || userInput.contains("扫描风险") || userInput.contains("风险点"))
        {
            ChatTask t = new ChatTask();
            t.setTaskType("RISK_SCAN");
            t.setProjectName(projectName);
            tasks.add(t);
        }

        if (userInput.contains("文档核查") || userInput.contains("核查文档") || userInput.contains("合规检查"))
        {
            ChatTask t = new ChatTask();
            t.setTaskType("DOC_CHECK");
            t.setProjectName(projectName);
            tasks.add(t);
        }

        if (tasks.isEmpty())
        {
            tasks.add(fallbackSingle(userInput));
        }

        return tasks;
    }

    private ChatTask fallbackSingle(String userInput)
    {
        ChatTask fallback = new ChatTask();
        String projectName = extractSimpleProjectName(userInput);
        if (userInput.contains("项目库") || userInput.contains("有什么资料") || userInput.contains("资料列表"))
        {
            fallback.setTaskType("LIST_PROJECTS");
        }
        else if (projectName != null)
        {
            fallback.setTaskType("READ_PROJECT");
            fallback.setProjectName(projectName);
        }
        else
        {
            fallback.setTaskType("QA");
        }
        return fallback;
    }

    private String extractSimpleProjectName(String text)
    {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("([\\u4e00-\\u9fa5A-Za-z0-9]{2,}(?:公司|学院|项目|单位))")
                .matcher(text);
        return m.find() ? m.group(1) : null;
    }
}
