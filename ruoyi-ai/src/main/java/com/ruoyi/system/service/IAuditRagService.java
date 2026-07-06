package com.ruoyi.system.service;

import com.ruoyi.system.domain.AuditBasis;
import java.util.List;

/**
 * RAG 检索增强生成服务接口
 * 编排整个 RAG 管线：意图分类 → 向量检索 → Prompt拼装 → AI调用 → 后处理
 *
 * @author ruoyi
 */
public interface IAuditRagService
{
    /**
     * 意图分类结果
     */
    enum Intent {
        QA,        // 智能问答
        MATCH,     // 依据匹配
        FORENSIC,  // 取证单生成
        RISK       // 风险分析
    }

    /**
     * RAG 检索结果
     */
    class RagContext
    {
        /** 识别出的意图 */
        public Intent intent;
        /** 召回的 Top-K 依据 */
        public List<AuditBasis> recalledBasis;
        /** 拼装好的完整 System Message（含召回依据） */
        public String augmentedSystemPrompt;
        /** 用户原始问题 */
        public String userQuery;

        public Intent getIntent() { return intent; }
        public List<AuditBasis> getRecalledBasis() { return recalledBasis; }
        public String getAugmentedSystemPrompt() { return augmentedSystemPrompt; }
        public String getUserQuery() { return userQuery; }
    }

    /**
     * 执行 RAG 管线
     * @param userQuery 用户问题
     * @return RAG 上下文（含增强后的 System Prompt 和召回依据列表）
     */
    RagContext executeRag(String userQuery);

    /**
     * 从 AI 回复中提取引用的依据编号
     */
    List<String> extractCitedBasisIds(String aiResponse);

    /**
     * 记录 AI 调用日志
     */
    void logAiCall(String userQuery, String aiResponse, Intent intent,
                   List<String> citedBasisIds, int tokensUsed, long costTimeMs, int status);
}
