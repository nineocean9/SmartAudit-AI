package com.ruoyi.system.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.mapper.AuditProjectMapper;
import com.ruoyi.system.mapper.AuditBasisMapper;
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

    @PreAuthorize("@ss.hasPermi('audit:rectification:view')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) Integer status)
    {
        startPage();
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT i.id AS issue_id, i.issue_desc, i.severity, i.source, i.deadline, i.create_time, "
                   + "r.id AS rect_id, r.measure, r.status AS rect_status, r.finish_date, r.evaluator, r.feedback, "
                   + "p.project_name, p.audited_unit "
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
                    list.add(row);
                }
            }
        }
        catch (Exception e) { /* 空 */ }
        return getDataTable(list);
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
            ps.setInt(1, (int) body.get("status"));
            ps.setString(2, (String) body.get("feedback"));
            ps.setString(3, (String) body.get("evaluator"));
            ps.setString(4, (String) body.get("finishDate"));
            ps.setLong(5, Long.parseLong(body.get("issueId").toString()));
            ps.executeUpdate();
            return success();
        }
        catch (Exception e) { return error(e.getMessage()); }
    }
}
