package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 审计临时授权实体
 * 对应数据库表 audit_temp_auth
 *
 * @author ruoyi
 */
public class AuditTempAuth extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long projectId;
    private String authType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date expireDate;

    private Integer status;

    /** JOIN字段：用户名 */
    private String userName;

    /** JOIN字段：项目名称 */
    private String projectName;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long v) { this.projectId = v; }
    public String getAuthType() { return authType; }
    public void setAuthType(String v) { this.authType = v; }
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date v) { this.startDate = v; }
    public Date getExpireDate() { return expireDate; }
    public void setExpireDate(Date v) { this.expireDate = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
    public String getUserName() { return userName; }
    public void setUserName(String v) { this.userName = v; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String v) { this.projectName = v; }
}
