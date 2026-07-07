package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI调用日志实体
 * 对应表 ai_call_log
 */
public class AiCallLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String roleCode;
    private String intent;
    private String prompt;
    private String response;
    private String citedBasisIds;
    private String modelProvider;
    private Integer tokensUsed;
    private Integer status;
    private Long costTimeMs;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    public String getCitedBasisIds() { return citedBasisIds; }
    public void setCitedBasisIds(String citedBasisIds) { this.citedBasisIds = citedBasisIds; }
    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }
    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getCostTimeMs() { return costTimeMs; }
    public void setCostTimeMs(Long costTimeMs) { this.costTimeMs = costTimeMs; }
}
