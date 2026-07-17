package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;

/**
 * 审计项目访问控制。
 *
 * 菜单权限只决定能不能进入功能，项目数据还需要按用户所在单位或临时授权过滤。
 */
@Service
public class AuditProjectAccessService
{
    @Autowired
    private DataSource dataSource;

    public boolean canAccessProject(Long projectId)
    {
        return canAccessProject(projectId, currentLoginUser());
    }

    public boolean canAccessProject(Long projectId, LoginUser loginUser)
    {
        if (projectId == null) return false;
        if (isUnrestrictedRole(loginUser)) return true;

        if (isProjectMember(projectId, loginUser))
        {
            return true;
        }

        if (hasAnyRole(loginUser, "audited_unit_principal", "audited_unit_liaison"))
        {
            return isCurrentDeptProject(projectId, loginUser);
        }

        if (hasAnyRole(loginUser, "intermediary_auditor"))
        {
            return hasActiveTempAuth(projectId, loginUser);
        }

        return false;
    }

    public boolean canManageProjectMembers(Long projectId)
    {
        if (projectId == null) return false;
        if (isAdminUser()) return true;
        if (!hasAnyRole("audit_director", "audit_project_leader")) return false;
        return canAccessProject(projectId);
    }

    private boolean isAdminUser()
    {
        try
        {
            return SecurityUtils.isAdmin();
        }
        catch (Exception ignored)
        {
            return false;
        }
    }

    public boolean canAccessAuditedUnit(String auditedUnit)
    {
        if (isUnrestrictedRole()) return true;
        if (!hasAnyRole("audited_unit_principal", "audited_unit_liaison")) return false;
        Long deptId = findDeptIdByName(auditedUnit);
        if (deptId != null && deptId.equals(currentDeptId())) return true;
        String unitName = currentDeptName();
        return unitName != null && auditedUnit != null && unitName.equals(auditedUnit);
    }

    public boolean shouldShowProject(Long projectId, String auditedUnit)
    {
        if (isUnrestrictedRole()) return true;
        if (projectId != null)
        {
            return canAccessProject(projectId);
        }
        if (hasAnyRole("audited_unit_principal", "audited_unit_liaison"))
        {
            String unitName = currentDeptName();
            return unitName != null && auditedUnit != null && unitName.equals(auditedUnit);
        }
        if (hasAnyRole("intermediary_auditor"))
        {
            return isProjectMember(projectId) || hasActiveTempAuth(projectId);
        }
        return false;
    }

    public Long currentDeptId()
    {
        try
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            SysUser user = loginUser.getUser();
            if (user != null && user.getDeptId() != null)
            {
                return user.getDeptId();
            }
            return SecurityUtils.getDeptId();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public String currentDeptName()
    {
        try
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            SysUser user = loginUser.getUser();
            if (user != null)
            {
                SysDept dept = user.getDept();
                if (dept != null && dept.getDeptName() != null && !dept.getDeptName().isBlank())
                {
                    return dept.getDeptName();
                }
            }
            Long deptId = currentDeptId();
            if (deptId == null) return null;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT dept_name FROM sys_dept WHERE dept_id = ?"))
            {
                ps.setLong(1, deptId);
                try (ResultSet rs = ps.executeQuery())
                {
                    return rs.next() ? rs.getString("dept_name") : null;
                }
            }
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public boolean canAccessAllProjects()
    {
        return isUnrestrictedRole(currentLoginUser());
    }

    public boolean canAccessAllProjects(LoginUser loginUser)
    {
        return isUnrestrictedRole(loginUser);
    }

    public boolean canAccessAnalysis(Long projectId, String createBy)
    {
        LoginUser loginUser = currentLoginUser();
        if (isUnrestrictedRole(loginUser)) return true;
        if (projectId != null) return canAccessProject(projectId, loginUser);
        return loginUser != null && loginUser.getUsername() != null
                && loginUser.getUsername().equals(createBy);
    }

    public boolean canManageAnalysis(String createBy)
    {
        LoginUser loginUser = currentLoginUser();
        if (isUnrestrictedRole(loginUser)) return true;
        return loginUser != null && loginUser.getUsername() != null
                && loginUser.getUsername().equals(createBy);
    }

    public Long findAccessibleProjectId(String projectName)
    {
        return findAccessibleProjectId(projectName, currentLoginUser());
    }

    public Long findAccessibleProjectId(String projectName, LoginUser loginUser)
    {
        if (projectName == null || projectName.isBlank() || loginUser == null) return null;
        List<Long> candidates = new ArrayList<>();
        String sql = "SELECT id FROM audit_project "
                   + "WHERE trim(project_name) = trim(?) OR trim(audited_unit) = trim(?) "
                   + "OR project_name ILIKE ? OR audited_unit ILIKE ? "
                   + "OR ? ILIKE '%' || project_name || '%' "
                   + "ORDER BY CASE WHEN trim(project_name) = trim(?) THEN 0 ELSE 1 END, id DESC LIMIT 50";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, projectName);
            ps.setString(2, projectName);
            ps.setString(3, "%" + projectName + "%");
            ps.setString(4, "%" + projectName + "%");
            ps.setString(5, projectName);
            ps.setString(6, projectName);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next()) candidates.add(rs.getLong("id"));
            }
        }
        catch (Exception e)
        {
            return null;
        }
        for (Long projectId : candidates)
        {
            if (canAccessProject(projectId, loginUser)) return projectId;
        }
        return null;
    }

    public String getProjectName(Long projectId)
    {
        if (projectId == null) return null;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT project_name FROM audit_project WHERE id = ?"))
        {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next() ? rs.getString("project_name") : null;
            }
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private boolean isUnrestrictedRole()
    {
        return isUnrestrictedRole(currentLoginUser());
    }

    private boolean isUnrestrictedRole(LoginUser loginUser)
    {
        if (loginUser == null) return false;
        SysUser user = loginUser.getUser();
        if (user != null && user.isAdmin()) return true;
        return hasAnyRole(loginUser, "admin", "school_leader", "audit_director", "audit_project_leader", "audit_staff");
    }

    private boolean hasAnyRole(String... roleKeys)
    {
        return hasAnyRole(currentLoginUser(), roleKeys);
    }

    private boolean hasAnyRole(LoginUser loginUser, String... roleKeys)
    {
        try
        {
            if (loginUser == null) return false;
            SysUser user = loginUser.getUser();
            List<SysRole> roles = user != null ? user.getRoles() : null;
            if (roles == null || roles.isEmpty()) return false;
            for (SysRole role : roles)
            {
                if (role == null || role.getRoleKey() == null) continue;
                for (String roleKey : roleKeys)
                {
                    if (role.getRoleKey().equals(roleKey)) return true;
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return false;
    }

    private boolean isCurrentDeptProject(Long projectId)
    {
        return isCurrentDeptProject(projectId, currentLoginUser());
    }

    private boolean isCurrentDeptProject(Long projectId, LoginUser loginUser)
    {
        Long deptId = loginUser != null && loginUser.getUser() != null ? loginUser.getUser().getDeptId() : null;
        if (deptId == null) return false;
        String sql = "SELECT 1 FROM audit_project p "
                   + "WHERE p.id = ? AND (p.dept_id = ? "
                   + "OR (p.dept_id IS NULL AND p.audited_unit = (SELECT dept_name FROM sys_dept WHERE dept_id = ?))) "
                   + "LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setLong(1, projectId);
            ps.setLong(2, deptId);
            ps.setLong(3, deptId);
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private Long findDeptIdByName(String deptName)
    {
        if (deptName == null || deptName.isBlank()) return null;
        String sql = "SELECT dept_id FROM sys_dept WHERE del_flag = '0' AND trim(dept_name) = trim(?) LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, deptName);
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next() ? rs.getLong("dept_id") : null;
            }
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private boolean isProjectMember(Long projectId)
    {
        return isProjectMember(projectId, currentLoginUser());
    }

    private boolean isProjectMember(Long projectId, LoginUser loginUser)
    {
        if (loginUser == null || loginUser.getUser() == null) return false;
        String sql = "SELECT 1 FROM audit_project_member WHERE project_id = ? AND (user_id = ? OR user_name = ?) LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setLong(1, projectId);
            ps.setLong(2, loginUser.getUserId());
            ps.setString(3, loginUser.getUsername());
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private boolean hasActiveTempAuth(Long projectId)
    {
        return hasActiveTempAuth(projectId, currentLoginUser());
    }

    private boolean hasActiveTempAuth(Long projectId, LoginUser loginUser)
    {
        if (loginUser == null) return false;
        String sql = "SELECT 1 FROM audit_temp_auth WHERE project_id = ? AND user_id = ? AND status = 1 AND expire_date >= CURRENT_DATE LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setLong(1, projectId);
            ps.setLong(2, loginUser.getUserId());
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private LoginUser currentLoginUser()
    {
        try
        {
            return SecurityUtils.getLoginUser();
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
