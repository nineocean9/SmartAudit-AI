package com.ruoyi.system.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.ITempWorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

import static com.ruoyi.common.core.domain.AjaxResult.success;

/**
 * AI 工作台统一入口 Controller
 *
 * 提供：
 * - 项目列表树（计划→项目）
 * - 工作台统一上传分发
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/ai/workspace")
public class AiWorkspaceController
{
    @Autowired
    private DataSource dataSource;

    @Autowired
    private ITempWorkspaceService tempWorkspaceService;

    /**
     * 获取项目列表树
     * GET /ai/workspace/projects
     */
    @GetMapping("/projects")
    public AjaxResult listProjects()
    {
        List<Map<String, Object>> planList = new ArrayList<>();
        try (Connection conn = dataSource.getConnection())
        {
            // 查询所有计划
            String planSql = "SELECT id, plan_type, plan_year, batch, plan_name, status FROM audit_plan ORDER BY plan_year DESC, id DESC";
            try (PreparedStatement ps = conn.prepareStatement(planSql);
                 ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    Map<String, Object> plan = new LinkedHashMap<>();
                    plan.put("planId", rs.getLong("id"));
                    plan.put("planType", rs.getString("plan_type"));
                    plan.put("planYear", rs.getInt("plan_year"));
                    plan.put("batch", rs.getString("batch"));
                    plan.put("planName", rs.getString("plan_name"));
                    plan.put("status", rs.getInt("status"));

                    // 查询该计划下的项目
                    List<Map<String, Object>> projects = new ArrayList<>();
                    String projSql = "SELECT id, project_name, audited_unit, audit_type, audit_year, status, "
                                   + "COALESCE(doc_count, 0) AS doc_count "
                                   + "FROM audit_project WHERE plan_id = ? ORDER BY id DESC";
                    try (PreparedStatement ps2 = conn.prepareStatement(projSql))
                    {
                        ps2.setLong(1, rs.getLong("id"));
                        try (ResultSet rs2 = ps2.executeQuery())
                        {
                            while (rs2.next())
                            {
                                Map<String, Object> proj = new LinkedHashMap<>();
                                proj.put("id", rs2.getLong("id"));
                                proj.put("projectName", rs2.getString("project_name"));
                                proj.put("auditedUnit", rs2.getString("audited_unit"));
                                proj.put("auditType", rs2.getString("audit_type"));
                                proj.put("auditYear", rs2.getInt("audit_year"));
                                proj.put("status", rs2.getInt("status"));
                                proj.put("docCount", rs2.getInt("doc_count"));
                                projects.add(proj);
                            }
                        }
                    }
                    plan.put("projects", projects);
                    planList.add(plan);
                }
            }
        }
        catch (Exception e)
        {
            return AjaxResult.error("查询项目列表失败: " + e.getMessage());
        }
        return success(planList);
    }

    /**
     * 获取资料类型下拉选项
     * GET /ai/workspace/doc-types
     */
    @GetMapping("/doc-types")
    public AjaxResult listDocTypes()
    {
        List<String> types = List.of(
            "审计通知书", "审计方案", "底稿", "报告",
            "整改资料", "财务数据", "合同", "发票", "其他"
        );
        return success(types);
    }

    /**
     * 获取计划列表（简单列表，供上传选择）
     * GET /ai/workspace/plans
     */
    @GetMapping("/plans")
    public AjaxResult listPlans()
    {
        List<Map<String, Object>> plans = new ArrayList<>();
        String sql = "SELECT id, plan_type, plan_year, batch, plan_name FROM audit_plan ORDER BY plan_year DESC, id DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery())
        {
            while (rs.next())
            {
                Map<String, Object> plan = new LinkedHashMap<>();
                plan.put("id", rs.getLong("id"));
                plan.put("planName", rs.getString("plan_name"));
                plan.put("planYear", rs.getInt("plan_year"));
                plans.add(plan);
            }
        }
        catch (Exception e)
        {
            return AjaxResult.error("查询计划列表失败: " + e.getMessage());
        }
        return success(plans);
    }

    /**
     * 获取指定计划下的项目列表
     * GET /ai/workspace/plans/{planId}/projects
     */
    @GetMapping("/plans/{planId}/projects")
    public AjaxResult listProjectsByPlan(@PathVariable Long planId)
    {
        List<Map<String, Object>> projects = new ArrayList<>();
        String sql = "SELECT id, project_name, audited_unit, audit_type, audit_year, status "
                   + "FROM audit_project WHERE plan_id = ? ORDER BY id DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setLong(1, planId);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    Map<String, Object> proj = new LinkedHashMap<>();
                    proj.put("id", rs.getLong("id"));
                    proj.put("projectName", rs.getString("project_name"));
                    proj.put("auditedUnit", rs.getString("audited_unit"));
                    proj.put("auditType", rs.getString("audit_type"));
                    proj.put("auditYear", rs.getInt("audit_year"));
                    projects.add(proj);
                }
            }
        }
        catch (Exception e)
        {
            return AjaxResult.error("查询项目列表失败: " + e.getMessage());
        }
        return success(projects);
    }

    /**
     * 获取审计类型下拉选项
     * GET /ai/workspace/audit-types
     */
    @GetMapping("/audit-types")
    public AjaxResult listAuditTypes()
    {
        List<String> types = List.of("经责审计", "财务收支", "专项审计", "工程审计", "科研审计", "预算审计", "资产管理审计", "其他");
        return success(types);
    }

    /**
     * 创建审计项目
     * POST /ai/workspace/project
     * Body: { projectName, planId, auditType, auditedUnit, leader(可选) }
     */
    @PostMapping("/project")
    public AjaxResult createProject(@RequestBody Map<String, Object> body)
    {
        String projectName = (String) body.get("projectName");
        Number planIdNum = (Number) body.get("planId");
        String auditType = (String) body.get("auditType");
        String auditedUnit = (String) body.get("auditedUnit");
        String leader = (String) body.get("leader");
        String createBy = SecurityUtils.getUsername();

        if (projectName == null || projectName.isBlank())
        {
            return AjaxResult.error("项目名称不能为空");
        }

        try (Connection conn = dataSource.getConnection())
        {
            String sql = "INSERT INTO audit_project (project_name, audited_unit, audit_type, audit_year, "
                       + "plan_id, status, create_time, update_time) "
                       + "VALUES (?, ?, ?, ?, ?, 0, now(), now()) RETURNING id";
            try (PreparedStatement ps = conn.prepareStatement(sql))
            {
                ps.setString(1, projectName);
                ps.setString(2, auditedUnit != null ? auditedUnit : "");
                ps.setString(3, auditType != null ? auditType : "其他");
                ps.setInt(4, java.time.LocalDate.now().getYear());
                ps.setObject(5, planIdNum != null ? planIdNum.longValue() : null, java.sql.Types.BIGINT);

                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next())
                    {
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("id", rs.getLong("id"));
                        result.put("projectName", projectName);
                        result.put("auditType", auditType);
                        result.put("auditedUnit", auditedUnit);
                        result.put("createBy", createBy);
                        return success(result);
                    }
                }
            }
        }
        catch (Exception e)
        {
            return AjaxResult.error("创建项目失败: " + e.getMessage());
        }
        return AjaxResult.error("创建项目失败");
    }
}
