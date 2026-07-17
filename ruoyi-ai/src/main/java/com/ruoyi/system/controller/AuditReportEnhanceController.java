package com.ruoyi.system.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.AuditProjectAccessService;
import com.ruoyi.system.service.IAuditOpsService;
import com.ruoyi.system.service.IProjectDocService;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

@RestController
@RequestMapping("/audit/report")
public class AuditReportEnhanceController extends BaseController
{
    @Autowired private AuditReportOpinionMapper opinionMapper;
    @Autowired private AuditReportVersionMapper versionMapper;
    @Autowired private AuditArchiveMapper archiveMapper;
    @Autowired private AuditProjectAccessService projectAccessService;
    @Autowired private DataSource dataSource;
    @Autowired private IAuditOpsService opsService;
    @Autowired private IProjectDocService projectDocService;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // === 报告意见 ===
    @PreAuthorize("@ss.hasPermi('audit:report:view')")
    @GetMapping("/opinion/{reportId}")
    public AjaxResult opinionList(@PathVariable Long reportId) { return success(opinionMapper.selectByReportId(reportId)); }

    @PreAuthorize("@ss.hasPermi('audit:report:edit')")
    @PostMapping("/opinion")
    @Transactional
    public AjaxResult addOpinion(@RequestBody AuditReportOpinion o) {
        AuditReport report = opsService.selectReportById(o.getReportId());
        if (report == null || !projectAccessService.canAccessProject(report.getProjectId())) return AjaxResult.error("无权提交该项目报告意见");
        o.setSubmitBy(SecurityUtils.getUsername());
        int rows = opinionMapper.insert(o);
        if (rows > 0 && o.getAttachment() != null && !o.getAttachment().isBlank()) {
            String title = "报告意见-第" + (o.getRoundNo() == null ? 1 : o.getRoundNo()) + "轮";
            projectDocService.syncUploadedDocument(report.getProjectId(), findPlanId(report.getProjectId()),
                    "报告征求意见资料", logicalFileName(title, o.getAttachment()), o.getAttachment(),
                    o.getContent(), SecurityUtils.getUsername(), true);
        }
        return toAjax(rows);
    }

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
    public TableDataInfo archiveList(AuditArchive q) {
        if (q.getProjectId() != null && !projectAccessService.canAccessProject(q.getProjectId())) return getDataTable(List.of());
        startPage();
        List<AuditArchive> list = archiveMapper.selectList(q);
        list.removeIf(item -> item.getProjectId() != null && !projectAccessService.canAccessProject(item.getProjectId()));
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('audit:archive:view')")
    @GetMapping("/archive/{projectId}")
    public AjaxResult archiveByProject(@PathVariable Long projectId) { if (!projectAccessService.canAccessProject(projectId)) return AjaxResult.error("无权访问该项目归档"); return success(archiveMapper.selectByProjectId(projectId)); }

    @PreAuthorize("@ss.hasPermi('audit:archive:edit')")
    @PostMapping("/archive")
    @Transactional
    public AjaxResult addArchive(@RequestBody AuditArchive a) { if (!projectAccessService.canAccessProject(a.getProjectId())) return AjaxResult.error("无权维护该项目归档"); a.setCreateBy(SecurityUtils.getUsername()); int rows = archiveMapper.insert(a); if (rows > 0) syncArchive(a); return toAjax(rows); }

    @PreAuthorize("@ss.hasPermi('audit:archive:edit')")
    @PutMapping("/archive")
    public AjaxResult updateArchive(@RequestBody AuditArchive a) { if (!canAccessArchive(a.getId())) return AjaxResult.error("无权维护该项目归档"); return toAjax(archiveMapper.update(a)); }

    @PreAuthorize("@ss.hasPermi('audit:archive:edit')")
    @DeleteMapping("/archive/{id}")
    @Transactional
    public AjaxResult delArchive(@PathVariable Long id) { AuditArchive archive = archiveMapper.selectById(id); if (archive == null || !canAccessArchive(id)) return AjaxResult.error("无权维护该项目归档"); return toAjax(archiveMapper.deleteById(id)); }

    private boolean canAccessArchive(Long id)
    {
        if (id == null) return false;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT project_id FROM audit_archive WHERE id=?"))
        {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next() && projectAccessService.canAccessProject(rs.getLong("project_id"));
            }
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private void syncArchive(AuditArchive archive)
    {
        if (archive.getFilePath() == null || archive.getFilePath().isBlank()) return;
        projectDocService.syncUploadedDocument(archive.getProjectId(), findPlanId(archive.getProjectId()),
                archive.getArchiveCategory() == null ? "审计归档资料" : archive.getArchiveCategory(),
                archive.getFileName(), archive.getFilePath(), "审计归档资料：" + archive.getFileName(),
                SecurityUtils.getUsername(), true);
    }

    private Long findPlanId(Long projectId)
    {
        List<Long> ids = jdbcTemplate.queryForList("SELECT plan_id FROM audit_project WHERE id=?", Long.class, projectId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private String logicalFileName(String title, String filePath)
    {
        String clean = filePath.split("[?#]", 2)[0];
        int dot = clean.lastIndexOf('.');
        return dot >= 0 ? title + clean.substring(dot) : title;
    }
}
