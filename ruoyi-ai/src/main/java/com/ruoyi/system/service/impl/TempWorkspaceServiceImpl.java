package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.TempWorkspace;
import com.ruoyi.system.mapper.TempWorkspaceMapper;
import com.ruoyi.system.service.IFileParseService;
import com.ruoyi.system.service.ITempWorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;

/**
 * 临时工作区服务实现
 *
 * 会话级临时知识库，文件存DB（content_text），2小时自动过期
 *
 * @author ruoyi
 */
@Service
public class TempWorkspaceServiceImpl implements ITempWorkspaceService
{
    private static final Logger log = LoggerFactory.getLogger(TempWorkspaceServiceImpl.class);

    /** 每个会话最多文件数 */
    private static final int MAX_FILES_PER_SESSION = 10;

    /** 单文件最大字符数 */
    private static final int MAX_CHARS_PER_FILE = 200_000;

    @Autowired
    private TempWorkspaceMapper tempMapper;

    @Autowired
    private IFileParseService fileParseService;

    @Autowired
    private DataSource dataSource;

    @Override
    public String createSession(Long userId)
    {
        return UUID.randomUUID().toString();
    }

    @Override
    @Transactional
    public TempWorkspace uploadToTemp(String sessionId, Long userId, MultipartFile file)
    {
        // 检查文件数限制
        List<TempWorkspace> existing = tempMapper.selectBySessionId(sessionId);
        if (existing.size() >= MAX_FILES_PER_SESSION)
        {
            log.warn("临时工作区文件数超限: sessionId={}, count={}", sessionId, existing.size());
            throw new RuntimeException("临时工作区最多上传 " + MAX_FILES_PER_SESSION + " 个文件");
        }

        // 解析文件
        IFileParseService.FileParseResult parseResult = fileParseService.parse(file);
        String contentText;
        if (parseResult.success && parseResult.plainText != null)
        {
            contentText = parseResult.plainText.length() > MAX_CHARS_PER_FILE
                    ? parseResult.plainText.substring(0, MAX_CHARS_PER_FILE) + "\n\n[文本过长，已截断]"
                    : parseResult.plainText;
        }
        else
        {
            contentText = "[无法解析: " +
                    (parseResult.errorMsg != null ? parseResult.errorMsg : "未知错误") + "]";
        }

        // 保存
        TempWorkspace record = new TempWorkspace();
        record.setSessionId(sessionId);
        record.setUserId(userId);
        record.setFileName(file.getOriginalFilename());
        record.setContentText(contentText);
        // 2小时后过期
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 2);
        record.setExpireTime(new Date(cal.getTimeInMillis()));

        tempMapper.insert(record);
        log.info("临时文件已保存: sessionId={}, fileName={}, chars={}",
                sessionId, file.getOriginalFilename(), contentText != null ? contentText.length() : 0);

        return record;
    }

    @Override
    public List<TempWorkspace> listTempFiles(String sessionId)
    {
        return tempMapper.selectBySessionId(sessionId);
    }

    @Override
    public List<String> searchInTemp(String sessionId, String query, int topK)
    {
        List<String> results = new ArrayList<>();
        // 临时知识库使用关键词检索(ILIKE)，不走向量(临时文件不需要高精度召回)
        String sql = "SELECT content_text FROM temporary_workspace "
                   + "WHERE session_id = ? AND status = 1 AND content_text ILIKE ? "
                   + "ORDER BY LENGTH(content_text) ASC LIMIT ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, sessionId);
            ps.setString(2, "%" + query + "%");
            ps.setInt(3, topK);

            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    String text = rs.getString("content_text");
                    if (text != null)
                    {
                        // 提取匹配关键词的上下文片段
                        results.add(extractContext(text, query, 300));
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("临时工作区检索失败: sessionId={}", sessionId, e);
        }
        return results;
    }

    @Override
    public void destroySession(String sessionId)
    {
        tempMapper.destroyBySessionId(sessionId);
        log.info("临时工作区已销毁: sessionId={}", sessionId);
    }

    @Override
    public int cleanupExpired()
    {
        int count = tempMapper.deleteExpired();
        if (count > 0)
        {
            log.info("清理过期临时数据: {} 条", count);
        }
        return count;
    }

    /**
     * 从文本中提取包含关键词的上下文
     */
    private String extractContext(String text, String keyword, int contextLen)
    {
        if (text == null || text.length() <= contextLen) return text;

        int idx = text.toLowerCase().indexOf(keyword.toLowerCase());
        if (idx < 0) return text.substring(0, contextLen) + "...";

        int start = Math.max(0, idx - contextLen / 2);
        int end = Math.min(text.length(), start + contextLen);
        String ctx = text.substring(start, end);
        return (start > 0 ? "..." : "") + ctx + (end < text.length() ? "..." : "");
    }
}
