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
 * 使用 DeepSeek Embedding API 生成向量，pgvector 存储和检索
 *
 * @author ruoyi
 */
@Service
public class EmbeddingServiceImpl implements IEmbeddingService
{
    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceImpl.class);

    /** DeepSeek Embedding API 地址（OpenAI 兼容） */
    private static final String EMBEDDING_URL = "https://api.deepseek.com/v1/embeddings";

    /** 向量维度（deepseek-embedding 为 1024） */
    private static final int EMBEDDING_DIM = 1024;

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
        try
        {
            // 构建 OpenAI-compatible Embedding 请求
            Map<String, Object> reqBody = new LinkedHashMap<>();
            reqBody.put("model", "deepseek-embedding");
            reqBody.put("input", text);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + modelProps.getApiKey());
            headers.set("Content-Type", "application/json");

            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(reqBody, headers);

            String response = restTemplate.postForObject(EMBEDDING_URL, entity, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode embeddingNode = root.path("data").get(0).path("embedding");

            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++)
            {
                vector[i] = embeddingNode.get(i).floatValue();
            }
            return vector;
        }
        catch (Exception e)
        {
            log.error("调用 Embedding API 失败", e);
            throw new RuntimeException("向量化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Long> searchSimilar(String query, int topK)
    {
        int k = topK > 0 ? topK : DEFAULT_TOP_K;
        float[] queryVector = embed(query);
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
            // 查询依据内容
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

            // 生成向量（标题+正文）
            float[] vector = embed(title + " " + content);
            String vectorStr = arrayToPgVector(vector);

            // 更新 pgvector 列
            String updateSql = "UPDATE audit_basis SET embedding = ?::vector WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql))
            {
                ps.setString(1, vectorStr);
                ps.setLong(2, basisId);
                ps.executeUpdate();
            }

            // 记录向量化日志
            String logSql = "INSERT INTO ai_embedding_log (basis_id, embedding_dim, model, tokens_used, update_time) "
                          + "VALUES (?, ?, 'deepseek-embedding', ?, now()) "
                          + "ON CONFLICT DO NOTHING";
            try (PreparedStatement ps = conn.prepareStatement(logSql))
            {
                ps.setLong(1, basisId);
                ps.setInt(2, EMBEDDING_DIM);
                ps.setInt(3, (title + content).length());
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
                // 控制频率，避免 API 限流
                if (count % 5 == 0)
                {
                    Thread.sleep(1000);
                }
            }
        }
        catch (Exception e)
        {
            log.error("批量向量化失败", e);
        }
        log.info("批量向量化完成: {} 条", count);
        return count;
    }

    /**
     * float[] → pgvector 兼容字符串格式: [0.1,0.2,...]
     */
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
