package com.ruoyi.system.domain;

/**
 * 聊天任务解析结果
 * 由 AI 先把自然语言请求解析成结构化任务，再由程序执行
 *
 * @author ruoyi
 */
public class ChatTask
{
    /** LIST_PROJECTS / READ_PROJECT / ANALYZE_PROJECT / RISK_SCAN / DOC_CHECK / FORENSIC
     *  / MATCH_BASIS / QUERY_RECTIFICATION / RECOMMEND_OBJECT / QA */
    private String taskType;

    /** A公司 / 图书馆工程审计委托项目 / 科研经费专项审计 */
    private String projectName;

    /** 预算 / 收入 / 支出 / 科研经费 */
    private String keyword;

    /** 是否需要图表 */
    private Boolean needChart;

    /** 查询整改情况时的单位名称 */
    private String unitName;

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Boolean getNeedChart() { return needChart; }
    public void setNeedChart(Boolean needChart) { this.needChart = needChart; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
}
