package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 统一知识库管理器接口
 * 屏蔽底层知识库差异，给 AI Workspace Router 提供统一检索入口
 *
 * @author ruoyi
 */
public interface IKnowledgeManager
{
    /** 知识来源枚举 */
    enum KnowledgeSource
    {
        POLICY,      // 法规知识库 (audit_basis)
        PROJECT,     // 项目知识库 (project_document + document_chunk)
        TEMP,        // 临时工作区 (temporary_workspace)
        CASE,        // 案例库 (audit_case_lib → document_chunk)
        RISK_CASE,   // 风险案例库 (audit_risk_case → document_chunk)
        ALL          // 全部来源
    }

    /** 统一检索结果 */
    class SearchResult
    {
        public KnowledgeSource source;
        public String sourceLabel;   // 法规名称/项目名称/文件名
        public String content;       // chunk 内容
        public double score;         // 相似度分数
        public Map<String, Object> metadata; // 附加信息

        public KnowledgeSource getSource() { return source; }
        public String getSourceLabel() { return sourceLabel; }
        public String getContent() { return content; }
        public double getScore() { return score; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    /**
     * 统一检索入口
     *
     * @param query          用户查询
     * @param sources        检索来源集合
     * @param projectId      项目ID(来源含PROJECT时有效)
     * @param tempSessionId  临时会话ID(来源含TEMP时有效)
     * @param topK           各来源返回数
     * @return 合并排序后的检索结果
     */
    List<SearchResult> search(String query, Set<KnowledgeSource> sources,
                              Long projectId, String tempSessionId, int topK);
}
