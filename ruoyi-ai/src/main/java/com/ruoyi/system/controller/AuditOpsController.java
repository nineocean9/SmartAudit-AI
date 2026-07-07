package com.ruoyi.system.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.AuditWorkpaper;
import com.ruoyi.system.domain.AuditReport;
import com.ruoyi.system.service.IAuditOpsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/audit/ops")
public class AuditOpsController extends BaseController
{
    @Autowired private IAuditOpsService service;

    // 方案
    @PreAuthorize("@ss.hasPermi('audit:project:view')")
    @GetMapping("/scheme/list") public AjaxResult schemeList(Long projectId) { return success(service.selectSchemeList(projectId)); }
    @PreAuthorize("@ss.hasPermi('audit:project:view')")
    @PostMapping("/scheme") public AjaxResult addScheme(@RequestBody Map<String,Object> s) { return toAjax(service.insertScheme(s)); }

    // 底稿
    @PreAuthorize("@ss.hasPermi('audit:workpaper:view')")
    @GetMapping("/wp/list") public TableDataInfo wpList(AuditWorkpaper w) { startPage(); return getDataTable(service.selectWorkpaperList(w)); }
    @PreAuthorize("@ss.hasPermi('audit:workpaper:view')")
    @GetMapping("/wp/{id}") public AjaxResult wpInfo(@PathVariable Long id) { return success(service.selectWorkpaperById(id)); }
    @PreAuthorize("@ss.hasPermi('audit:workpaper:edit')")
    @PostMapping("/wp") public AjaxResult addWp(@RequestBody AuditWorkpaper w) { w.setCreateBy(SecurityUtils.getUsername()); return toAjax(service.insertWorkpaper(w)); }
    @PreAuthorize("@ss.hasPermi('audit:workpaper:edit')")
    @PutMapping("/wp") public AjaxResult editWp(@RequestBody AuditWorkpaper w) { return toAjax(service.updateWorkpaper(w)); }

    // 复核
    @PreAuthorize("@ss.hasPermi('audit:workpaper:review')")
    @GetMapping("/review/list") public AjaxResult reviewList(Long workpaperId) { return success(service.selectReviewList(workpaperId)); }
    @PreAuthorize("@ss.hasPermi('audit:workpaper:review')")
    @PostMapping("/review") public AjaxResult addReview(@RequestBody Map<String,Object> r) { return toAjax(service.insertReview(r)); }

    // 报告
    @PreAuthorize("@ss.hasPermi('audit:report:view')")
    @GetMapping("/report/list") public AjaxResult reportList(Long projectId) { return success(service.selectReportList(projectId)); }
    @PreAuthorize("@ss.hasPermi('audit:report:view')")
    @GetMapping("/report/{id}") public AjaxResult reportInfo(@PathVariable Long id) { return success(service.selectReportById(id)); }
    @PreAuthorize("@ss.hasPermi('audit:report:edit')")
    @PostMapping("/report") public AjaxResult addReport(@RequestBody AuditReport r) { r.setCreateBy(SecurityUtils.getUsername()); return toAjax(service.insertReport(r)); }
    @PreAuthorize("@ss.hasPermi('audit:report:edit')")
    @PutMapping("/report") public AjaxResult editReport(@RequestBody AuditReport r) { return toAjax(service.updateReport(r)); }

    // 协同日志
    @PreAuthorize("@ss.hasPermi('audit:project:view')")
    @GetMapping("/collab/list") public AjaxResult collabLog(Long projectId) { return success(service.selectCollabLog(projectId)); }
}
