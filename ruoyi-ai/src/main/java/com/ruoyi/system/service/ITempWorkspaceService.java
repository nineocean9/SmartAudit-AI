package com.ruoyi.system.service;

import com.ruoyi.system.domain.TempWorkspace;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * 临时工作区服务接口
 * 会话级别的临时知识库，到期自动销毁
 *
 * @author ruoyi
 */
public interface ITempWorkspaceService
{
    /**
     * 创建临时工作区会话
     * @return sessionId
     */
    String createSession(Long userId);

    /**
     * 上传文件到临时工作区
     */
    TempWorkspace uploadToTemp(String sessionId, Long userId, MultipartFile file);

    /**
     * 列出临时工作区文件
     */
    List<TempWorkspace> listTempFiles(String sessionId);

    /**
     * 在临时工作区中检索
     */
    List<String> searchInTemp(String sessionId, String query, int topK);

    /**
     * 销毁临时工作区
     */
    void destroySession(String sessionId);

    /**
     * 清理所有过期的临时数据
     */
    int cleanupExpired();
}
