package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.AuditBasis;
import com.ruoyi.system.mapper.AuditBasisMapper;
import com.ruoyi.system.service.IEmbeddingService;
import com.ruoyi.system.service.IKnowledgeManager;
import com.ruoyi.system.service.IProjectDocService;
import com.ruoyi.system.service.ITempWorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 统一知识库管理器实现
 *
 * 编排多源检索：法规库 + 项目知识库 + 临时工作区
 * 按相似度合并排序，返回统一格式
 *
 * @author ruoyi
 */
@Service
public class KnowledgeManagerImpl implements IKnowledgeManager
{
    private static final Logger log = LoggerFactory.getLogger(KnowledgeManagerImpl.class);

    /** 默认每来源 Top-K */
    private static final int DEFAULT_TOP_K = 8;

    @Autowired
    private IEmbeddingService embeddingService;

    @Autowired
    private AuditBasisMapper auditBasisMapper;

    @Autowired
    private IProjectDocService projectDocService;

    @Autowired
    private ITempWorkspaceService tempWorkspaceService;

    @Override
    public List<SearchResult> search(String query, Set<KnowledgeSource> sources,
                                     Long projectId, String tempSessionId, int topK)
    {
        int k = topK > 0 ? topK : DEFAULT_TOP_K;
        List<SearchResult> allResults = new ArrayList<>();

        // 法规知识库
        if (sources.contains(KnowledgeSource.ALL) || sources.contains(KnowledgeSource.POLICY))
        {
            searchPolicyKB(query, k, allResults);
        }

        // 项目知识库
        if (sources.contains(KnowledgeSource.ALL) || sources.contains(KnowledgeSource.PROJECT))
        {
            searchProjectKB(query, projectId, k, allResults);
        }

        // 临时工作区
        if (sources.contains(KnowledgeSource.ALL) || sources.contains(KnowledgeSource.TEMP))
        {
            searchTempKB(query, tempSessionId, k, allResults);
        }

        // 按分数排序（越小越相似，pgvector distance）
        allResults.sort(Comparator.comparingDouble(SearchResult::getScore));

        // 截取 topK（确保至少保留 1 条项目结果）
        if (allResults.size() > k)
        {
            List<SearchResult> truncated = new ArrayList<>();
            boolean hasProject = false;
            for (int i = 0; i < allResults.size(); i++)
            {
                if (truncated.size() >= k && hasProject) break;
                SearchResult sr = allResults.get(i);
                if (sr.getSource() == KnowledgeSource.PROJECT)
                {
                    hasProject = true;
                }
                if (truncated.size() < k || sr.getSource() == KnowledgeSource.PROJECT)
                {
                    truncated.add(sr);
                }
            }
            return truncated;
        }
        return allResults;
    }

    // ---- private ----

    private void searchPolicyKB(String query, int topK, List<SearchResult> allResults)
    {
        try
        {
            List<Long> basisIds = embeddingService.searchSimilar(query, topK);
            if (basisIds != null && !basisIds.isEmpty())
            {
                int rank = 0;
                for (Long id : basisIds)
                {
                    rank++;
                    AuditBasis basis = auditBasisMapper.selectAuditBasisById(id);
                    if (basis != null)
                    {
                        SearchResult sr = new SearchResult();
                        sr.source = KnowledgeSource.POLICY;
                        sr.sourceLabel = "《" + basis.getTitle() + "》";
                        sr.content = basis.getContent();
                        sr.score = rank * 0.1;  // 近似分数
                        sr.metadata = new HashMap<>();
                        sr.metadata.put("basisId", id);
                        sr.metadata.put("category", basis.getCategory());
                        sr.metadata.put("title", basis.getTitle());
                        allResults.add(sr);
                    }
                }
                log.info("法规库检索召回 {} 条", allResults.size());
            }
            else
            {
                // 向量无结果 → 关键词降级
                List<AuditBasis> keywordResults = auditBasisMapper.searchByKeyword(query, null);
                if (keywordResults != null)
                {
                    for (AuditBasis basis : keywordResults)
                    {
                        SearchResult sr = new SearchResult();
                        sr.source = KnowledgeSource.POLICY;
                        sr.sourceLabel = "《" + basis.getTitle() + "》";
                        sr.content = basis.getContent();
                        sr.score = 0.5;
                        sr.metadata = new HashMap<>();
                        sr.metadata.put("basisId", basis.getId());
                        sr.metadata.put("category", basis.getCategory());
                        allResults.add(sr);
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("法规库检索异常", e);
        }
    }

    private void searchProjectKB(String query, Long projectId, int topK,
                                  List<SearchResult> allResults)
    {
        try
        {
            List<IProjectDocService.DocSearchResult> docResults;
            if (projectId != null)
            {
                docResults = projectDocService.searchInProject(projectId, query, topK);
            }
            else
            {
                docResults = projectDocService.searchInAllProjects(query, topK);
            }

            if (docResults != null)
            {
                for (IProjectDocService.DocSearchResult dr : docResults)
                {
                    SearchResult sr = new SearchResult();
                    sr.source = KnowledgeSource.PROJECT;
                    sr.sourceLabel = "[" + dr.getDocType() + "] " + dr.getFileName();
                    sr.content = dr.getChunkContent();
                    sr.score = dr.getDistance();
                    sr.metadata = new HashMap<>();
                    sr.metadata.put("documentId", dr.getDocumentId());
                    sr.metadata.put("docType", dr.getDocType());
                    sr.metadata.put("fileName", dr.getFileName());
                    allResults.add(sr);
                }
            }
        }
        catch (Exception e)
        {
            log.error("项目知识库检索异常", e);
        }
    }

    private void searchTempKB(String query, String tempSessionId, int topK,
                               List<SearchResult> allResults)
    {
        if (tempSessionId == null) return;
        try
        {
            List<String> tempResults = tempWorkspaceService.searchInTemp(tempSessionId, query, topK);
            if (tempResults != null)
            {
                for (int i = 0; i < tempResults.size(); i++)
                {
                    SearchResult sr = new SearchResult();
                    sr.source = KnowledgeSource.TEMP;
                    sr.sourceLabel = "临时文件";
                    sr.content = tempResults.get(i);
                    sr.score = 0.3 + i * 0.1;  // 临时文件分数排在法规之后
                    sr.metadata = new HashMap<>();
                    allResults.add(sr);
                }
            }
        }
        catch (Exception e)
        {
            log.error("临时工作区检索异常", e);
        }
    }
}
