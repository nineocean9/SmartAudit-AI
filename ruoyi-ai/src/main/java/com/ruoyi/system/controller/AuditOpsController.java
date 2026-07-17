package com.ruoyi.system.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.AuditWorkpaper;
import com.ruoyi.system.domain.AuditReport;
import com.ruoyi.system.service.AuditProjectAccessService;
import com.ruoyi.system.service.IAuditOpsService;
import com.ruoyi.system.service.IProjectDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/audit/ops")
public class AuditOpsController extends BaseController
{
    @Autowired private IAuditOpsService service;
    @Autowired private AuditProjectAccessService projectAccessService;
    @Autowired private IProjectDocService projectDocService;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // 方案
    @PreAuthorize("@ss.hasAnyPermi('audit:project:view,audit:prepare:view')")
    @GetMapping("/scheme/list") public AjaxResult schemeList(Long projectId) { if (projectId != null && !projectAccessService.canAccessProject(projectId)) return AjaxResult.error("无权访问该项目方案"); return success(service.selectSchemeList(projectId)); }
    @PreAuthorize("@ss.hasAnyPermi('audit:project:view,audit:prepare:view')")
    @GetMapping("/scheme/{id}") public AjaxResult schemeInfo(@PathVariable Long id) { Map<String,Object> scheme = service.selectSchemeById(id); if (!canAccessScheme(scheme)) return AjaxResult.error("无权访问该项目方案"); return success(scheme); }
    @PreAuthorize("@ss.hasAnyPermi('audit:project:view,audit:prepare:edit')")
    @PostMapping("/scheme")
    @Transactional
    public AjaxResult addScheme(@RequestBody Map<String,Object> s) {
        Long projectId = toLong(s.get("projectId"));
        if (!projectAccessService.canAccessProject(projectId)) return AjaxResult.error("无权维护该项目方案");
        s.put("createBy", SecurityUtils.getUsername());
        service.insertScheme(s);
        syncFile(projectId, "审计实施方案", stringValue(s.get("title")), stringValue(s.get("fileUrl")),
                stringValue(s.get("content")), true);
        return success(s);
    }
    @PreAuthorize("@ss.hasAnyPermi('audit:project:view,audit:prepare:edit')")
    @PutMapping("/scheme/{id}/content")
    public AjaxResult updateSchemeContent(@PathVariable Long id, @RequestBody Map<String,Object> body)
    {
        Map<String,Object> scheme = service.selectSchemeById(id);
        if (!canAccessScheme(scheme)) return AjaxResult.error("无权维护该项目方案");
        body.put("id", id);
        int rows = service.updateSchemeContent(body);
        syncFile(toLong(scheme.get("project_id")), "审计实施方案", stringValue(scheme.get("title")),
                stringValue(scheme.get("file_url")), stringValue(body.get("content")), true);
        return toAjax(rows);
    }

    // 底稿
    @PreAuthorize("@ss.hasPermi('audit:workpaper:view') or @ss.hasAnyRoles('audit_project_leader,audit_staff,intermediary_auditor')")
    @GetMapping("/wp/list") public TableDataInfo wpList(AuditWorkpaper w) {
        if (w.getProjectId() != null && !projectAccessService.canAccessProject(w.getProjectId())) return getDataTable(List.of());
        startPage();
        List<AuditWorkpaper> list = service.selectWorkpaperList(w);
        list.removeIf(item -> item.getProjectId() != null && !projectAccessService.canAccessProject(item.getProjectId()));
        return getDataTable(list);
    }
    @PreAuthorize("@ss.hasPermi('audit:workpaper:view') or @ss.hasAnyRoles('audit_project_leader,audit_staff,intermediary_auditor')")
    @GetMapping("/wp/{id}") public AjaxResult wpInfo(@PathVariable Long id) { AuditWorkpaper w = service.selectWorkpaperById(id); if (w == null || !projectAccessService.canAccessProject(w.getProjectId())) return AjaxResult.error("无权访问该项目底稿"); return success(w); }
    @PreAuthorize("@ss.hasPermi('audit:workpaper:edit') or @ss.hasAnyRoles('audit_project_leader,audit_staff,intermediary_auditor')")
    @PostMapping("/wp") public AjaxResult addWp(@RequestBody AuditWorkpaper w) { if (!projectAccessService.canAccessProject(w.getProjectId())) return AjaxResult.error("无权维护该项目底稿"); w.setCreateBy(SecurityUtils.getUsername()); return toAjax(service.insertWorkpaper(w)); }
    @PreAuthorize("@ss.hasPermi('audit:workpaper:edit') or @ss.hasAnyRoles('audit_project_leader,audit_staff,intermediary_auditor')")
    @PutMapping("/wp") public AjaxResult editWp(@RequestBody AuditWorkpaper w) { if (!projectAccessService.canAccessProject(w.getProjectId())) return AjaxResult.error("无权维护该项目底稿"); return toAjax(service.updateWorkpaper(w)); }

    // 复核
    @PreAuthorize("@ss.hasAnyPermi('audit:workpaper:view,audit:workpaper:review') or @ss.hasAnyRoles('audit_project_leader,audit_staff,intermediary_auditor')")
    @GetMapping("/review/list") public AjaxResult reviewList(Long workpaperId) { return success(service.selectReviewList(workpaperId)); }
    @PreAuthorize("@ss.hasPermi('audit:workpaper:review')")
    @PostMapping("/review") public AjaxResult addReview(@RequestBody Map<String,Object> r) { return toAjax(service.insertReview(r)); }

    // 报告
    @PreAuthorize("@ss.hasPermi('audit:report:view')")
    @GetMapping("/report/list") public AjaxResult reportList(Long projectId) { if (projectId != null && !projectAccessService.canAccessProject(projectId)) return success(List.of()); List<AuditReport> list = service.selectReportList(projectId); list.removeIf(item -> item.getProjectId() != null && !projectAccessService.canAccessProject(item.getProjectId())); return success(list); }
    @PreAuthorize("@ss.hasPermi('audit:report:view')")
    @GetMapping("/report/{id}") public AjaxResult reportInfo(@PathVariable Long id) { AuditReport r = service.selectReportById(id); if (r == null || !projectAccessService.canAccessProject(r.getProjectId())) return AjaxResult.error("无权访问该项目报告"); return success(r); }
    @PreAuthorize("@ss.hasPermi('audit:report:edit')")
    @PostMapping("/report")
    @Transactional
    public AjaxResult addReport(@RequestBody AuditReport r) { if (!projectAccessService.canAccessProject(r.getProjectId())) return AjaxResult.error("无权维护该项目报告"); r.setCreateBy(SecurityUtils.getUsername()); service.insertReport(r); syncFile(r.getProjectId(), "审计报告", r.getTitle(), r.getFileUrl(), r.getContent(), true); return success(r); }
    @PreAuthorize("@ss.hasPermi('audit:report:edit')")
    @PutMapping("/report")
    @Transactional
    public AjaxResult editReport(@RequestBody AuditReport r) { if (!projectAccessService.canAccessProject(r.getProjectId())) return AjaxResult.error("无权维护该项目报告"); AuditReport old = service.selectReportById(r.getId()); int rows = service.updateReport(r); if (old != null && old.getFileUrl() != null && !old.getFileUrl().equals(r.getFileUrl())) projectDocService.hideSyncedDocument(old.getProjectId(), old.getFileUrl()); syncFile(r.getProjectId(), "审计报告", r.getTitle(), r.getFileUrl(), r.getContent(), true); return toAjax(rows); }
    @PreAuthorize("@ss.hasPermi('audit:report:edit')")
    @PutMapping("/report/{id}/content")
    public AjaxResult updateReportContent(@PathVariable Long id, @RequestBody Map<String,String> body)
    {
        AuditReport existing = service.selectReportById(id);
        if (existing == null || !projectAccessService.canAccessProject(existing.getProjectId())) return AjaxResult.error("无权维护该项目报告");
        AuditReport r = new AuditReport();
        r.setId(id);
        r.setContent(body.get("content"));
        int rows = service.updateReportContent(r);
        syncFile(existing.getProjectId(), "审计报告", existing.getTitle(), existing.getFileUrl(), r.getContent(), true);
        return toAjax(rows);
    }

    // 协同日志
    @PreAuthorize("@ss.hasPermi('audit:project:view')")
    @GetMapping("/collab/list") public AjaxResult collabLog(Long projectId) { return success(service.selectCollabLog(projectId)); }

    private boolean canAccessScheme(Map<String,Object> scheme)
    {
        if (scheme == null) return false;
        return projectAccessService.canAccessProject(toLong(scheme.get("project_id")));
    }

    private Long toLong(Object value)
    {
        if (value == null || value.toString().isBlank()) return null;
        return Long.valueOf(value.toString());
    }

    private void syncFile(Long projectId, String docType, String title, String filePath, String content, boolean visible)
    {
        if (filePath == null || filePath.isBlank()) return;
        String fileName = logicalFileName(title, filePath);
        projectDocService.syncUploadedDocument(projectId, findPlanId(projectId), docType,
                fileName, filePath, content, SecurityUtils.getUsername(), visible);
    }

    private Long findPlanId(Long projectId)
    {
        List<Long> ids = jdbcTemplate.queryForList("SELECT plan_id FROM audit_project WHERE id=?", Long.class, projectId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private String logicalFileName(String title, String filePath)
    {
        String name = title == null || title.isBlank() ? "项目文档" : title;
        String clean = filePath.split("[?#]", 2)[0];
        int dot = clean.lastIndexOf('.');
        return dot >= 0 ? name + clean.substring(dot) : name;
    }

    private String stringValue(Object value) { return value == null ? null : value.toString(); }
}
