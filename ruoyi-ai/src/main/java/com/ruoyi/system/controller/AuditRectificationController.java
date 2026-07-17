package com.ruoyi.system.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.mapper.AuditProjectMapper;
import com.ruoyi.system.mapper.AuditBasisMapper;
import com.ruoyi.system.service.AuditProjectAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@RestController
@RequestMapping("/basis/issue")
public class AuditRectificationController extends BaseController
{
    @Autowired
    private DataSource dataSource;

    @Autowired
    private AuditProjectAccessService projectAccessService;

    @PreAuthorize("@ss.hasPermi('audit:rectification:view')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) Integer status)
    {
        startPage();
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT i.id AS issue_id, i.issue_desc, i.severity, i.source, i.deadline, i.create_time, "
                   + "r.id AS rect_id, r.measure, r.status AS rect_status, r.finish_date, r.evaluator, r.feedback, "
                   + "p.id AS project_id, p.project_name, p.audited_unit "
                   + "FROM audit_issue i "
                   + "JOIN audit_project p ON p.id = i.project_id "
                   + "LEFT JOIN audit_rectification r ON r.issue_id = i.id "
                   + (status != null ? "WHERE r.status = ? " : "")
                   + "ORDER BY i.severity DESC, i.create_time DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            if (status != null) ps.setInt(1, status);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("rectId", rs.getObject("rect_id"));
                    row.put("issueId", rs.getLong("issue_id"));
                    row.put("issueDesc", rs.getString("issue_desc"));
                    row.put("severity", rs.getInt("severity"));
                    row.put("source", rs.getString("source"));
                    row.put("deadline", rs.getString("deadline"));
                    row.put("createTime", rs.getString("create_time"));
                    row.put("measure", rs.getString("measure"));
                    row.put("rectStatus", rs.getInt("rect_status"));
                    row.put("finishDate", rs.getString("finish_date"));
                    row.put("evaluator", rs.getString("evaluator"));
                    row.put("feedback", rs.getString("feedback"));
                    row.put("projectName", rs.getString("project_name"));
                    row.put("auditedUnit", rs.getString("audited_unit"));
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

    @PreAuthorize("@ss.hasPermi('audit:rectification:add')")
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> body)
    {
        String sql = "INSERT INTO audit_rectification (issue_id, measure, status, create_time) VALUES (?, ?, ?, now())";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            Long issueId = Long.parseLong(body.get("issueId").toString());
            if (!canAccessIssue(conn, issueId)) return error("无权维护该项目整改");
            ps.setLong(1, issueId);
            ps.setString(2, (String) body.get("measure"));
            ps.setInt(3, body.containsKey("status") && body.get("status") != null ? (int) body.get("status") : 0);
            ps.executeUpdate();
            return success();
        }
        catch (Exception e) { return error(e.getMessage()); }
    }

    @PreAuthorize("@ss.hasPermi('audit:rectification:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        String sql = "DELETE FROM audit_rectification WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            if (!canAccessRectification(conn, id)) return error("无权维护该项目整改");
            ps.setLong(1, id);
            ps.executeUpdate();
            return success();
        }
        catch (Exception e) { return error(e.getMessage()); }
    }

    @PreAuthorize("@ss.hasPermi('audit:rectification:edit')")
    @PutMapping
    public AjaxResult update(@RequestBody Map<String, Object> body)
    {
        String sql = "UPDATE audit_rectification SET status=?, feedback=?, evaluator=?, finish_date=?"
                   + "WHERE issue_id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            Long issueId = Long.parseLong(body.get("issueId").toString());
            if (!canAccessIssue(conn, issueId)) return error("无权维护该项目整改");
            ps.setInt(1, (int) body.get("status"));
            ps.setString(2, (String) body.get("feedback"));
            ps.setString(3, (String) body.get("evaluator"));
            ps.setString(4, (String) body.get("finishDate"));
            ps.setLong(5, issueId);
            ps.executeUpdate();
            return success();
        }
        catch (Exception e) { return error(e.getMessage()); }
    }

    private boolean canAccessIssue(Connection conn, Long issueId) throws Exception
    {
        try (PreparedStatement ps = conn.prepareStatement("SELECT project_id FROM audit_issue WHERE id=?"))
        {
            ps.setLong(1, issueId);
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next() && projectAccessService.canAccessProject(rs.getLong("project_id"));
            }
        }
    }

    private boolean canAccessRectification(Connection conn, Long rectId) throws Exception
    {
        try (PreparedStatement ps = conn.prepareStatement("SELECT i.project_id FROM audit_rectification r JOIN audit_issue i ON i.id=r.issue_id WHERE r.id=?"))
        {
            ps.setLong(1, rectId);
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next() && projectAccessService.canAccessProject(rs.getLong("project_id"));
            }
        }
    }
}
