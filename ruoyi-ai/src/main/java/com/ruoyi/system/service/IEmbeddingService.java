package com.ruoyi.system.service;

import java.util.List;

/**
 * 向量化服务接口
 * 负责文本→向量转换和 pgvector 相似度检索
 *
 * @author ruoyi
 */
public interface IEmbeddingService
{
    /**
     * 将文本转为向量
     */
    float[] embed(String text);

    /**
     * 向量相似度检索，从依据库召回 Top-K 条依据
     * 返回依据ID列表（按相似度降序）
     */
    List<Long> searchSimilar(String query, int topK);

    /**
     * 同步单条依据的向量（新增/修改依据时调用）
     */
    void syncBasisEmbedding(Long basisId);

    /**
     * 全量同步所有依据的向量（种子数据初始化用）
     */
    int batchSyncAll();
}
