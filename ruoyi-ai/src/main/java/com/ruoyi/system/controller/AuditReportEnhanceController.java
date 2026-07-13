package com.ruoyi.system.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit/report")
public class AuditReportEnhanceController extends BaseController
{
    @Autowired private AuditReportOpinionMapper opinionMapper;
    @Autowired private AuditReportVersionMapper versionMapper;
    @Autowired private AuditArchiveMapper archiveMapper;

    // === 报告意见 ===
    @PreAuthorize("@ss.hasPermi('audit:report:view')")
    @GetMapping("/opinion/{reportId}")
    public AjaxResult opinionList(@PathVariable Long reportId) { return success(opinionMapper.selectByReportId(reportId)); }

    @PreAuthorize("@ss.hasPermi('audit:report:edit')")
    @PostMapping("/opinion")
    public AjaxResult addOpinion(@RequestBody AuditReportOpinion o) { return toAjax(opinionMapper.insert(o)); }

    // === 报告版本 ===
    @PreAuthorize("@ss.hasPermi('audit:report:view')")
    @GetMapping("/version/{reportId}")
    public AjaxResult versionList(@PathVariable Long reportId) { return success(versionMapper.selectByReportId(reportId)); }

    @PreAuthorize("@ss.hasPermi('audit:report:view')")
    @GetMapping("/version/detail/{id}")
    public AjaxResult versionDetail(@PathVariable Long id) { return success(versionMapper.selectById(id)); }

    @PreAuthorize("@ss.hasPermi('audit:report:edit')")
    @PostMapping("/version")
    public AjaxResult saveVersion(@RequestBody AuditReportVersion v) { return toAjax(versionMapper.insert(v)); }

    // === 项目归档 ===
    @PreAuthorize("@ss.hasPermi('audit:archive:view')")
    @GetMapping("/archive/list")
    public TableDataInfo archiveList(AuditArchive q) { startPage(); return getDataTable(archiveMapper.selectList(q)); }

    @PreAuthorize("@ss.hasPermi('audit:archive:view')")
    @GetMapping("/archive/{projectId}")
    public AjaxResult archiveByProject(@PathVariable Long projectId) { return success(archiveMapper.selectByProjectId(projectId)); }

    @PreAuthorize("@ss.hasPermi('audit:archive:edit')")
    @PostMapping("/archive")
    public AjaxResult addArchive(@RequestBody AuditArchive a) { return toAjax(archiveMapper.insert(a)); }

    @PreAuthorize("@ss.hasPermi('audit:archive:edit')")
    @PutMapping("/archive")
    public AjaxResult updateArchive(@RequestBody AuditArchive a) { return toAjax(archiveMapper.update(a)); }

    @PreAuthorize("@ss.hasPermi('audit:archive:edit')")
    @DeleteMapping("/archive/{id}")
    public AjaxResult delArchive(@PathVariable Long id) { return toAjax(archiveMapper.deleteById(id)); }
}
