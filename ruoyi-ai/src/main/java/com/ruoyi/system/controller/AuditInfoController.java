package com.ruoyi.system.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.service.IAuditInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/audit/info")
public class AuditInfoController extends BaseController
{
    @Autowired private IAuditInfoService service;

    // 计划
    @PreAuthorize("@ss.hasPermi('audit:plan:view')")
    @GetMapping("/plan/list")
    public TableDataInfo planList(String planType, Integer planYear) {
        startPage(); return getDataTable(service.selectPlanList(planType, planYear));
    }
    @PreAuthorize("@ss.hasPermi('audit:plan:edit')")
    @PostMapping("/plan") public AjaxResult addPlan(@RequestBody Map<String,Object> p) { return toAjax(service.insertPlan(p)); }
    @PreAuthorize("@ss.hasPermi('audit:plan:edit')")
    @DeleteMapping("/plan/{ids}") public AjaxResult delPlan(@PathVariable Long[] ids) { return toAjax(service.deletePlanByIds(ids)); }

    // 单位
    @PreAuthorize("@ss.hasPermi('audit:unit:view')")
    @GetMapping("/unit/list")
    public TableDataInfo unitList(String unitType) { startPage(); return getDataTable(service.selectUnitList(unitType)); }
    @PreAuthorize("@ss.hasPermi('audit:unit:edit')")
    @PostMapping("/unit") public AjaxResult addUnit(@RequestBody Map<String,Object> u) { return toAjax(service.insertUnit(u)); }
    @PreAuthorize("@ss.hasPermi('audit:unit:edit')")
    @DeleteMapping("/unit/{ids}") public AjaxResult delUnit(@PathVariable Long[] ids) { return toAjax(service.deleteUnitByIds(ids)); }

    // 领导
    @PreAuthorize("@ss.hasPermi('audit:unit:view')")
    @GetMapping("/leader/list")
    public TableDataInfo leaderList(Long unitId) { startPage(); return getDataTable(service.selectLeaderList(unitId)); }

    // 进度
    @PreAuthorize("@ss.hasPermi('audit:progress:view')")
    @GetMapping("/progress")
    public AjaxResult progress() { return success(service.selectProjectProgress()); }

    // ========== 闭环⓪ ==========

    // 单位完整档案（领导+历史审计）
    @PreAuthorize("@ss.hasPermi('audit:unit:view')")
    @GetMapping("/unit/{id}/profile")
    public AjaxResult unitProfile(@PathVariable Long id) { return success(service.selectUnitProfile(id)); }

    // 推荐应审（单位+领导）
    @PreAuthorize("@ss.hasPermi('audit:plan:recommend')")
    @GetMapping("/plan/recommend")
    public AjaxResult recommend() { return success(service.recommendAuditTargets()); }

    // 计划-项目绑定
    @PreAuthorize("@ss.hasPermi('audit:plan:edit')")
    @PostMapping("/plan/{planId}/bind/{projectId}")
    public AjaxResult bind(@PathVariable Long planId, @PathVariable Long projectId) { return toAjax(service.bindPlanProject(planId, projectId)); }
    @PreAuthorize("@ss.hasPermi('audit:plan:edit')")
    @DeleteMapping("/plan/{planId}/bind/{projectId}")
    public AjaxResult unbind(@PathVariable Long planId, @PathVariable Long projectId) { return toAjax(service.unbindPlanProject(planId, projectId)); }

    // 计划下的项目和方案
    @PreAuthorize("@ss.hasPermi('audit:plan:view')")
    @GetMapping("/plan/{planId}/projects")
    public AjaxResult planProjects(@PathVariable Long planId) { return success(service.selectPlanProjects(planId)); }
    @PreAuthorize("@ss.hasPermi('audit:plan:view')")
    @GetMapping("/plan/{planId}/schemes")
    public AjaxResult planSchemes(@PathVariable Long planId) { return success(service.selectSchemeByPlan(planId)); }

    // Excel 导出
    @PreAuthorize("@ss.hasPermi('audit:plan:view')")
    @GetMapping("/plan/export")
    public void exportPlan(jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.http.HttpServletRequest req)
    {
        java.util.List<com.ruoyi.system.domain.AuditPlan> list = (java.util.List) service.selectPlanList(
                req.getParameter("planType"),
                req.getParameter("planYear") == null ? null : Integer.parseInt(req.getParameter("planYear")));
        com.ruoyi.common.utils.poi.ExcelUtil<com.ruoyi.system.domain.AuditPlan> util =
                new com.ruoyi.common.utils.poi.ExcelUtil<>(com.ruoyi.system.domain.AuditPlan.class);
        try { util.exportExcel(response, list, "审计计划数据"); } catch (Exception e) { throw new RuntimeException(e); }
    }
}
