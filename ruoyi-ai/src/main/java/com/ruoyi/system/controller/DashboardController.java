package com.ruoyi.system.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;

@RestController
@RequestMapping("/dashboard")
public class DashboardController extends BaseController
{
    @Autowired
    private DataSource dataSource;

    @PreAuthorize("@ss.hasPermi('dashboard:view')")
    @GetMapping("/data")
    public AjaxResult getData()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection())
        {
            // 项目统计
            String sql1 = "SELECT status,count(*) cnt FROM audit_project GROUP BY status ORDER BY status";
            try (ResultSet rs = conn.prepareStatement(sql1).executeQuery())
            {
                Map<String, Integer> projectStats = new LinkedHashMap<>();
                projectStats.put("未启动", 0); projectStats.put("实施中", 0); projectStats.put("已归档", 0);
                while (rs.next()) { String k = rs.getInt("status") == 0 ? "未启动" : rs.getInt("status") == 1 ? "实施中" : "已归档"; projectStats.put(k, rs.getInt("cnt")); }
                result.put("projectStats", projectStats);
            }

            // 问题严重度分布
            String sql2 = "SELECT severity,count(*) cnt FROM audit_issue GROUP BY severity ORDER BY severity";
            try (ResultSet rs = conn.prepareStatement(sql2).executeQuery())
            {
                Map<String, Integer> severityStats = new LinkedHashMap<>();
                severityStats.put("高", 0); severityStats.put("中", 0); severityStats.put("低", 0);
                while (rs.next()) { String k = rs.getInt("severity") == 3 ? "高" : rs.getInt("severity") == 2 ? "中" : "低"; severityStats.put(k, rs.getInt("cnt")); }
                result.put("severityStats", severityStats);
            }

            // 整改情况
            String sql3 = "SELECT status,count(*) cnt FROM audit_rectification GROUP BY status ORDER BY status";
            try (ResultSet rs = conn.prepareStatement(sql3).executeQuery())
            {
                Map<String, Integer> rectStats = new LinkedHashMap<>();
                rectStats.put("未整改", 0); rectStats.put("整改中", 0); rectStats.put("已整改", 0);
                while (rs.next()) { String k = rs.getInt("status") == 0 ? "未整改" : rs.getInt("status") == 1 ? "整改中" : "已整改"; rectStats.put(k, rs.getInt("cnt")); }
                result.put("rectStats", rectStats);
            }

            // 各单位问题数
            String sql4 = "SELECT p.audited_unit,count(i.id) cnt FROM audit_project p LEFT JOIN audit_issue i ON i.project_id=p.id GROUP BY p.id,p.audited_unit ORDER BY cnt DESC";
            try (ResultSet rs = conn.prepareStatement(sql4).executeQuery())
            {
                List<Map<String, Object>> unitStats = new ArrayList<>();
                while (rs.next()) { Map<String, Object> r = new LinkedHashMap<>(); r.put("name", rs.getString("audited_unit")); r.put("count", rs.getInt("cnt")); unitStats.add(r); }
                result.put("unitStats", unitStats);
            }

            // 最近项目
            String sql5 = "SELECT project_name,audited_unit,audit_type,audit_year,status FROM audit_project ORDER BY create_time DESC LIMIT 5";
            try (ResultSet rs = conn.prepareStatement(sql5).executeQuery())
            {
                List<Map<String, Object>> recent = new ArrayList<>();
                while (rs.next()) { Map<String, Object> r = new LinkedHashMap<>(); r.put("name", rs.getString("project_name")); r.put("unit", rs.getString("audited_unit")); r.put("type", rs.getString("audit_type")); r.put("year", rs.getInt("audit_year")); r.put("status", rs.getInt("status")); recent.add(r); }
                result.put("recentProjects", recent);
            }
        }
        catch (Exception e) { return error(e.getMessage()); }
        return success(result);
    }
}
