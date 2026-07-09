package com.ruoyi.system.domain;

import java.util.Date;

/**
 * 临时工作区实体
 * 对应数据库表 temporary_workspace
 *
 * @author ruoyi
 */
public class TempWorkspace
{
    private Long id;
    private String sessionId;
    private Long userId;
    private String fileName;
    private String filePath;
    private String contentText;
    private Integer status;
    private Date expireTime;
    private Date createTime;

    // ---- getter / setter ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Date getExpireTime() { return expireTime; }
    public void setExpireTime(Date expireTime) { this.expireTime = expireTime; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
