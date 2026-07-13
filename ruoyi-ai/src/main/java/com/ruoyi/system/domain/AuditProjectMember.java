package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 审计项目成员实体
 * 对应数据库表 audit_project_member
 *
 * @author ruoyi
 */
public class AuditProjectMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private Long userId;
    private String userName;
    private String roleType;
    private String taskScope;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date taskDeadline;

    private Integer status;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long v) { this.projectId = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getUserName() { return userName; }
    public void setUserName(String v) { this.userName = v; }
    public String getRoleType() { return roleType; }
    public void setRoleType(String v) { this.roleType = v; }
    public String getTaskScope() { return taskScope; }
    public void setTaskScope(String v) { this.taskScope = v; }
    public Date getTaskDeadline() { return taskDeadline; }
    public void setTaskDeadline(Date v) { this.taskDeadline = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
}
