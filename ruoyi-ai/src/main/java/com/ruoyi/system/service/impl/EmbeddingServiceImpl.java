package com.ruoyi.system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.system.config.AiModelProperties;
import com.ruoyi.system.service.IEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * 向量化服务实现
 * 支持 DashScope / OpenAI Embedding API，pgvector 存储和检索
 * Embedding 不可用时自动降级，不影响主对话流程
 *
 * @author ruoyi
 */
@Service
public class EmbeddingServiceImpl implements IEmbeddingService
{
    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceImpl.class);

    /** 默认检索 Top-K */
    private static final int DEFAULT_TOP_K = 8;

    @Autowired
    private AiModelProperties modelProps;

    @Autowired
    private DataSource dataSource;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public float[] embed(String text)
    {
        AiModelProperties.Embedding emb = modelProps.getEmbedding();
        String apiKey = getEffectiveApiKey(emb);

        // 未配置 API Key 则跳过向量化
        if (apiKey == null || apiKey.isEmpty())
        {
            log.debug("Embedding API Key 未配置，跳过向量化");
            return null;
        }

        try
        {
            String url = getEmbeddingUrl();
            Map<String, Object> reqBody = new LinkedHashMap<>();
            reqBody.put("model", emb.getModel());
            reqBody.put("input", text);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");

            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(reqBody, headers);

            String response = restTemplate.postForObject(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode embeddingNode = root.path("data").get(0).path("embedding");

            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++)
            {
                vector[i] = embeddingNode.get(i).floatValue();
            }
            log.debug("向量化成功: provider={}, model={}, dim={}", emb.getProvider(), emb.getModel(), vector.length);
            return vector;
        }
        catch (Exception e)
        {
            log.warn("Embedding API 调用失败 ({} {}): {}。降级到关键词检索。",
                    emb.getProvider(), emb.getModel(), e.getMessage());
            return null;
        }
    }

    @Override
    public List<Long> searchSimilar(String query, int topK)
    {
        int k = topK > 0 ? topK : DEFAULT_TOP_K;
        float[] queryVector = embed(query);

        // Embedding 不可用时返回空列表，让 AuditRagService 走关键词兜底
        if (queryVector == null)
        {
            return Collections.emptyList();
        }

        String vectorStr = arrayToPgVector(queryVector);
        List<Long> basisIds = new ArrayList<>();
        String sql = "SELECT id FROM audit_basis WHERE embedding IS NOT NULL AND status = 1 "
                   + "ORDER BY embedding <=> ?::vector LIMIT ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, vectorStr);
            ps.setInt(2, k);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    basisIds.add(rs.getLong("id"));
                }
            }
        }
        catch (Exception e)
        {
            log.error("pgvector 向量检索失败", e);
        }
        return basisIds;
    }

    @Override
    public void syncBasisEmbedding(Long basisId)
    {
        try (Connection conn = dataSource.getConnection())
        {
            String selectSql = "SELECT title, content FROM audit_basis WHERE id = ?";
            String title = "", content = "";
            try (PreparedStatement ps = conn.prepareStatement(selectSql))
            {
                ps.setLong(1, basisId);
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next())
                    {
                        title = rs.getString("title");
                        content = rs.getString("content");
                    }
                }
            }

            if (content == null) return;

            float[] vector = embed(title + " " + content);
            if (vector == null) return; // Embedding 不可用则跳过

            String vectorStr = arrayToPgVector(vector);
            String updateSql = "UPDATE audit_basis SET embedding = ?::vector WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql))
            {
                ps.setString(1, vectorStr);
                ps.setLong(2, basisId);
                ps.executeUpdate();
            }

            String logSql = "INSERT INTO ai_embedding_log (basis_id, embedding_dim, model, tokens_used, update_time) "
                          + "VALUES (?, ?, ?, ?, now())";
            try (PreparedStatement ps = conn.prepareStatement(logSql))
            {
                ps.setLong(1, basisId);
                ps.setInt(2, vector.length);
                ps.setString(3, modelProps.getEmbedding().getModel());
                ps.setInt(4, (title + content).length());
                ps.executeUpdate();
            }
        }
        catch (Exception e)
        {
            log.error("同步依据向量失败: basisId={}", basisId, e);
        }
    }

    @Override
    public int batchSyncAll()
    {
        int count = 0;
        String sql = "SELECT id FROM audit_basis WHERE embedding IS NULL";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery())
        {
            while (rs.next())
            {
                syncBasisEmbedding(rs.getLong("id"));
                count++;
                if (count % 5 == 0) Thread.sleep(1000);
            }
        }
        catch (Exception e)
        {
            log.error("批量向量化失败", e);
        }
        log.info("批量向量化完成: {} 条", count);
        return count;
    }

    // ---- private ----

    /** 获取 Embedding API 的 apiKey（embedding 单独配置则用它，否则复用 chat） */
    private String getEffectiveApiKey(AiModelProperties.Embedding emb)
    {
        if (emb.getApiKey() != null && !emb.getApiKey().isEmpty())
            return emb.getApiKey();
        return modelProps.getApiKey();
    }

    /** 获取 Embedding API 地址（配置优先，兜底用 DashScope 默认） */
    private String getEmbeddingUrl()
    {
        AiModelProperties.Embedding emb = modelProps.getEmbedding();
        if (emb.getBaseUrl() != null && !emb.getBaseUrl().isEmpty())
            return emb.getBaseUrl().replaceAll("/+$", "") + "/embeddings";
        // 兜底：DashScope 默认地址
        return "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";
    }

    /** float[] → pgvector 字符串: [0.1,0.2,...] */
    private String arrayToPgVector(float[] array)
    {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++)
        {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
