package com.ruoyi.system.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.service.AuditProjectAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * 审计问题管理 Controller
 * 直接使用 DataSource + JDBC，与 AuditRectificationController 风格一致
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/audit/issue")
public class AuditIssueController extends BaseController
{
    @Autowired
    private DataSource dataSource;

    @Autowired
    private AuditProjectAccessService projectAccessService;

    /**
     * 分页列表
     * GET /audit/issue/list?keyword=&projectId=&severity=&pageNum=&pageSize=
     */
    @PreAuthorize("@ss.hasPermi('audit:issue:view')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) Long projectId,
                              @RequestParam(required = false) Integer severity)
    {
        startPage();
        List<Map<String, Object>> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT i.id, i.project_id, i.issue_desc, i.severity, i.basis_id, i.source, "
          + "i.deadline, i.create_time, "
          + "p.project_name, b.title AS basis_title "
          + "FROM audit_issue i "
          + "JOIN audit_project p ON p.id = i.project_id "
          + "LEFT JOIN audit_basis b ON b.id = i.basis_id "
          + "WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isEmpty())
        {
            sql.append("AND i.issue_desc LIKE ? ");
            params.add("%" + keyword + "%");
        }
        if (projectId != null)
        {
            sql.append("AND i.project_id = ? ");
            params.add(projectId);
        }
        if (severity != null)
        {
            sql.append("AND i.severity = ? ");
            params.add(severity);
        }
        sql.append("ORDER BY i.severity DESC, i.create_time DESC");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString()))
        {
            for (int idx = 0; idx < params.size(); idx++)
            {
                ps.setObject(idx + 1, params.get(idx));
            }
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("projectId", rs.getLong("project_id"));
                    row.put("issueDesc", rs.getString("issue_desc"));
                    row.put("severity", rs.getInt("severity"));
                    row.put("basisId", rs.getObject("basis_id"));
                    row.put("source", rs.getString("source"));
                    row.put("deadline", rs.getString("deadline"));
                    row.put("createTime", rs.getString("create_time"));
                    row.put("projectName", rs.getString("project_name"));
                    row.put("basisTitle", rs.getString("basis_title"));
                    if (projectAccessService.canAccessProject(rs.getLong("project_id")))
                    {
                        list.add(row);
                    }
                }
            }
        }
        catch (Exception e) { /* 空 */ }
        return getDataTable(list);
    }

    /**
     * 详情
     * GET /audit/issue/{id}
     */
    @PreAuthorize("@ss.hasPermi('audit:issue:view')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        String sql = "SELECT i.id, i.project_id, i.issue_desc, i.severity, i.basis_id, i.source, "
                   + "i.deadline, i.create_time, p.project_name, b.title AS basis_title "
                   + "FROM audit_issue i "
                   + "JOIN audit_project p ON p.id = i.project_id "
                   + "LEFT JOIN audit_basis b ON b.id = i.basis_id "
                   + "WHERE i.id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("projectId", rs.getLong("project_id"));
                    row.put("issueDesc", rs.getString("issue_desc"));
                    row.put("severity", rs.getInt("severity"));
                    row.put("basisId", rs.getObject("basis_id"));
                    row.put("source", rs.getString("source"));
                    row.put("deadline", rs.getString("deadline"));
                    row.put("createTime", rs.getString("create_time"));
                    row.put("projectName", rs.getString("project_name"));
                    row.put("basisTitle", rs.getString("basis_title"));
                    if (!projectAccessService.canAccessProject(rs.getLong("project_id")))
                    {
                        return error("无权访问该项目问题");
                    }
                    return success(row);
                }
            }
        }
        catch (Exception e) { return error(e.getMessage()); }
        return error("问题不存在");
    }

    /**
     * 新增
     * POST /audit/issue
     */
    @PreAuthorize("@ss.hasPermi('audit:issue:add')")
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> body)
    {
        String sql = "INSERT INTO audit_issue (project_id, issue_desc, severity, basis_id, source, deadline, create_time) "
                   + "VALUES (?, ?, ?, ?, ?, ?::date, now())";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            Long projectId = Long.parseLong(body.get("projectId").toString());
            if (!projectAccessService.canAccessProject(projectId)) return error("无权维护该项目问题");
            ps.setLong(1, projectId);
            ps.setString(2, (String) body.get("issueDesc"));
            ps.setInt(3, body.get("severity") != null ? Integer.parseInt(body.get("severity").toString()) : 1);
            if (body.get("basisId") != null && !body.get("basisId").toString().isEmpty())
            {
                ps.setLong(4, Long.parseLong(body.get("basisId").toString()));
            }
            else
            {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            ps.setString(5, body.get("source") != null ? body.get("source").toString() : "审计发现");
            ps.setString(6, body.get("deadline") != null ? body.get("deadline").toString() : null);
            ps.executeUpdate();
            return success();
        }
        catch (Exception e) { return error(e.getMessage()); }
    }

    /**
     * 修改
     * PUT /audit/issue
     */
    @PreAuthorize("@ss.hasPermi('audit:issue:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody Map<String, Object> body)
    {
        String sql = "UPDATE audit_issue SET project_id=?, issue_desc=?, severity=?, basis_id=?, source=?, deadline=?::date "
                   + "WHERE id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            Long projectId = Long.parseLong(body.get("projectId").toString());
            if (!projectAccessService.canAccessProject(projectId)) return error("无权维护该项目问题");
            ps.setLong(1, projectId);
            ps.setString(2, (String) body.get("issueDesc"));
            ps.setInt(3, body.get("severity") != null ? Integer.parseInt(body.get("severity").toString()) : 1);
            if (body.get("basisId") != null && !body.get("basisId").toString().isEmpty())
            {
                ps.setLong(4, Long.parseLong(body.get("basisId").toString()));
            }
            else
            {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            ps.setString(5, body.get("source") != null ? body.get("source").toString() : "审计发现");
            ps.setString(6, body.get("deadline") != null ? body.get("deadline").toString() : null);
            ps.setLong(7, Long.parseLong(body.get("id").toString()));
            ps.executeUpdate();
            return success();
        }
        catch (Exception e) { return error(e.getMessage()); }
    }

    /**
     * 批量删除
     * DELETE /audit/issue/{ids}
     */
    @PreAuthorize("@ss.hasPermi('audit:issue:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        if (ids == null || ids.length == 0) return error("参数为空");
        StringBuilder sql = new StringBuilder("DELETE FROM audit_issue WHERE id IN (");
        for (int i = 0; i < ids.length; i++)
        {
            sql.append(i > 0 ? ",?" : "?");
        }
        sql.append(")");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString()))
        {
            for (int i = 0; i < ids.length; i++)
            {
                ps.setLong(i + 1, ids[i]);
            }
            ps.executeUpdate();
            return success();
        }
        catch (Exception e) { return error(e.getMessage()); }
    }

    /** 获取项目列表（供下拉选择） */
    @GetMapping("/projects")
    public AjaxResult listProjects()
    {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, project_name FROM audit_project ORDER BY id");
             ResultSet rs = ps.executeQuery())
        {
            while (rs.next())
            {
                Map<String, Object> m = new LinkedHashMap<>();
                Long projectId = rs.getLong("id");
                if (!projectAccessService.canAccessProject(projectId))
                {
                    continue;
                }
                m.put("id", projectId);
                m.put("projectName", rs.getString("project_name"));
                list.add(m);
            }
        }
        catch (Exception e) { /* ignore */ }
        return success(list);
    }

    /** 获取审计依据列表（供下拉选择） */
    @GetMapping("/basis-options")
    public AjaxResult listBasis()
    {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, title FROM audit_basis WHERE status=1 ORDER BY id");
             ResultSet rs = ps.executeQuery())
        {
            while (rs.next())
            {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getLong("id"));
                m.put("title", rs.getString("title"));
                list.add(m);
            }
        }
        catch (Exception e) { /* ignore */ }
        return success(list);
    }
}
