package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.AuditRiskCase;
import com.ruoyi.system.domain.AuditCaseLib;
import com.ruoyi.system.domain.DocumentChunk;
import com.ruoyi.system.mapper.AuditRiskCaseMapper;
import com.ruoyi.system.mapper.AuditCaseLibMapper;
import com.ruoyi.system.mapper.DocumentChunkMapper;
import com.ruoyi.system.service.IAuditBasisLibService;
import com.ruoyi.system.service.IEmbeddingService;
import com.ruoyi.system.util.DocChunker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

@Service
public class AuditBasisLibServiceImpl implements IAuditBasisLibService
{
    private static final Logger log = LoggerFactory.getLogger(AuditBasisLibServiceImpl.class);

    @Autowired
    private AuditRiskCaseMapper riskCaseMapper;
    @Autowired
    private AuditCaseLibMapper caseLibMapper;
    @Autowired
    private IEmbeddingService embeddingService;
    @Autowired
    private DocumentChunkMapper chunkMapper;
    @Autowired
    private DataSource dataSource;

    // ---- 风险库 ----

    public List<AuditRiskCase> selectRiskCaseList(AuditRiskCase r) { return riskCaseMapper.selectRiskCaseList(r); }

    public int insertRiskCase(AuditRiskCase r)
    {
        int rows = riskCaseMapper.insertRiskCase(r);
        if (rows > 0 && r.getId() != null)
        {
            vectorizeRiskCase(r);
        }
        return rows;
    }

    public int updateRiskCase(AuditRiskCase r)
    {
        int rows = riskCaseMapper.updateRiskCase(r);
        if (rows > 0 && r.getId() != null)
        {
            // 先删旧向量，再重新入库
            chunkMapper.deleteBySourceTypeAndSourceId("RISK_CASE", r.getId());
            vectorizeRiskCase(r);
        }
        return rows;
    }

    public int deleteRiskCaseByIds(Long[] ids)
    {
        // 先删除关联的向量切块
        for (Long id : ids)
        {
            chunkMapper.deleteBySourceTypeAndSourceId("RISK_CASE", id);
        }
        return riskCaseMapper.deleteRiskCaseByIds(ids);
    }

    // ---- 案例库 ----

    public List<AuditCaseLib> selectCaseLibList(AuditCaseLib c) { return caseLibMapper.selectCaseLibList(c); }

    public int insertCaseLib(AuditCaseLib c)
    {
        int rows = caseLibMapper.insertCaseLib(c);
        if (rows > 0 && c.getId() != null)
        {
            vectorizeCaseLib(c);
        }
        return rows;
    }

    public int updateCaseLib(AuditCaseLib c)
    {
        int rows = caseLibMapper.updateCaseLib(c);
        if (rows > 0 && c.getId() != null)
        {
            // 先删旧向量，再重新入库
            chunkMapper.deleteBySourceTypeAndSourceId("CASE", c.getId());
            vectorizeCaseLib(c);
        }
        return rows;
    }

    public int deleteCaseLibByIds(Long[] ids)
    {
        // 先删除关联的向量切块
        for (Long id : ids)
        {
            chunkMapper.deleteBySourceTypeAndSourceId("CASE", id);
        }
        return caseLibMapper.deleteCaseLibByIds(ids);
    }

    // ---- 向量化入库 ----

    /**
     * 将案例库内容向量化并存入 document_chunk
     * 参考 ProjectDocServiceImpl 的切块→向量化→入库流程
     */
    private void vectorizeCaseLib(AuditCaseLib c)
    {
        try
        {
            // 拼装完整文本：标题 + 分类 + 内容
            StringBuilder text = new StringBuilder();
            if (c.getCaseTitle() != null) text.append("案例标题：").append(c.getCaseTitle()).append("\n");
            if (c.getCategory() != null) text.append("分类：").append(c.getCategory()).append("\n");
            if (c.getCaseContent() != null) text.append(c.getCaseContent());

            String fullText = text.toString();
            if (fullText.isBlank()) return;

            // 切块
            List<String> chunks = DocChunker.chunk(fullText,
                    DocChunker.DEFAULT_MAX_CHARS, DocChunker.DEFAULT_OVERLAP);

            for (int i = 0; i < chunks.size(); i++)
            {
                String chunkText = chunks.get(i);
                if (chunkText.isBlank()) continue;

                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(0L);  // 非项目文档，设为0
                chunk.setChunkIndex(i);
                chunk.setContent(chunkText);
                chunk.setTokenCount(DocChunker.estimateTokens(chunkText));
                chunk.setSourceType("CASE");
                chunk.setSourceId(c.getId());

                // 向量化
                try
                {
                    float[] vector = embeddingService.embed(chunkText);
                    chunk.setEmbedding(vector);
                }
                catch (Exception e)
                {
                    log.warn("案例向量化失败: caseId={}, chunkIdx={}: {}", c.getId(), i, e.getMessage());
                }

                chunkMapper.insertChunk(chunk);

                if (chunk.getEmbedding() != null && chunk.getId() != null)
                {
                    updateChunkEmbedding(chunk.getId(), chunk.getEmbedding());
                }
            }
            log.info("案例库向量化完成: caseId={}, chunks={}", c.getId(), chunks.size());
        }
        catch (Exception e)
        {
            log.error("案例库向量化异常: caseId={}", c.getId(), e);
        }
    }

    /**
     * 将风险案例内容向量化并存入 document_chunk
     */
    private void vectorizeRiskCase(AuditRiskCase r)
    {
        try
        {
            // 拼装完整文本：名称 + 场景 + 描述
            StringBuilder text = new StringBuilder();
            if (r.getRiskName() != null) text.append("风险名称：").append(r.getRiskName()).append("\n");
            if (r.getScenario() != null) text.append("适用场景：").append(r.getScenario()).append("\n");
            if (r.getRiskDesc() != null) text.append(r.getRiskDesc());

            String fullText = text.toString();
            if (fullText.isBlank()) return;

            // 切块
            List<String> chunks = DocChunker.chunk(fullText,
                    DocChunker.DEFAULT_MAX_CHARS, DocChunker.DEFAULT_OVERLAP);

            for (int i = 0; i < chunks.size(); i++)
            {
                String chunkText = chunks.get(i);
                if (chunkText.isBlank()) continue;

                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(0L);  // 非项目文档，设为0
                chunk.setChunkIndex(i);
                chunk.setContent(chunkText);
                chunk.setTokenCount(DocChunker.estimateTokens(chunkText));
                chunk.setSourceType("RISK_CASE");
                chunk.setSourceId(r.getId());

                // 向量化
                try
                {
                    float[] vector = embeddingService.embed(chunkText);
                    chunk.setEmbedding(vector);
                }
                catch (Exception e)
                {
                    log.warn("风险案例向量化失败: riskId={}, chunkIdx={}: {}", r.getId(), i, e.getMessage());
                }

                chunkMapper.insertChunk(chunk);

                if (chunk.getEmbedding() != null && chunk.getId() != null)
                {
                    updateChunkEmbedding(chunk.getId(), chunk.getEmbedding());
                }
            }
            log.info("风险案例向量化完成: riskId={}, chunks={}", r.getId(), chunks.size());
        }
        catch (Exception e)
        {
            log.error("风险案例向量化异常: riskId={}", r.getId(), e);
        }
    }

    /** 更新 chunk 的 pgvector 向量 */
    private void updateChunkEmbedding(Long chunkId, float[] vector)
    {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++)
        {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");

        String sql = "UPDATE document_chunk SET embedding = ?::vector WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, sb.toString());
            ps.setLong(2, chunkId);
            ps.executeUpdate();
        }
        catch (Exception e)
        {
            log.error("更新chunk向量失败: chunkId={}", chunkId, e);
        }
    }
}
