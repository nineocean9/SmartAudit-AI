package com.ruoyi.system.config;

import com.ruoyi.system.service.IEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动时自动向量化未同步的依据数据
 * 确保 audit_basis 表中的 embedding 列不为 NULL
 *
 * @author ruoyi
 */
@Component
public class EmbeddingInitRunner implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(EmbeddingInitRunner.class);

    @Autowired(required = false)
    private IEmbeddingService embeddingService;

    @Override
    public void run(ApplicationArguments args)
    {
        if (embeddingService == null)
        {
            log.warn("EmbeddingService 未注入，跳过启动向量化");
            return;
        }

        log.info(">>> 检查并同步未向量化的依据数据...");
        try
        {
            int count = embeddingService.batchSyncAll();
            log.info("<<< 启动向量化完成，共同步 {} 条", count);
        }
        catch (Exception e)
        {
            log.error("启动向量化失败（不影响应用启动，下次启动会重试）", e);
        }
    }
}
