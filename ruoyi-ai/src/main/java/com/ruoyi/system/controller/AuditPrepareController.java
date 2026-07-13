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
@RequestMapping("/audit/prepare")
public class AuditPrepareController extends BaseController
{
    @Autowired private AuditSchemeTemplateMapper templateMapper;
    @Autowired private AuditProjectMemberMapper memberMapper;
    @Autowired private AuditMaterialChecklistMapper materialMapper;

    // === 方案模板 ===
    @PreAuthorize("@ss.hasPermi('audit:template:view')")
    @GetMapping("/template/list")
    public TableDataInfo templateList(AuditSchemeTemplate q) { startPage(); return getDataTable(templateMapper.selectList(q)); }

    @PreAuthorize("@ss.hasPermi('audit:template:view')")
    @GetMapping("/template/{id}")
    public AjaxResult getTemplate(@PathVariable Long id) { return success(templateMapper.selectById(id)); }

    @PreAuthorize("@ss.hasPermi('audit:template:view')")
    @GetMapping("/template/byType/{auditType}")
    public AjaxResult templateByType(@PathVariable String auditType) { return success(templateMapper.selectByAuditType(auditType)); }

    @PreAuthorize("@ss.hasPermi('audit:template:edit')")
    @PostMapping("/template")
    public AjaxResult addTemplate(@RequestBody AuditSchemeTemplate t) { return toAjax(templateMapper.insert(t)); }

    @PreAuthorize("@ss.hasPermi('audit:template:edit')")
    @PutMapping("/template")
    public AjaxResult updateTemplate(@RequestBody AuditSchemeTemplate t) { return toAjax(templateMapper.update(t)); }

    @PreAuthorize("@ss.hasPermi('audit:template:edit')")
    @DeleteMapping("/template/{ids}")
    public AjaxResult delTemplate(@PathVariable Long[] ids) { return toAjax(templateMapper.deleteByIds(ids)); }

    // === 项目成员 ===
    @PreAuthorize("@ss.hasPermi('audit:prepare:view')")
    @GetMapping("/member/{projectId}")
    public AjaxResult memberList(@PathVariable Long projectId) { return success(memberMapper.selectByProjectId(projectId)); }

    @PreAuthorize("@ss.hasPermi('audit:prepare:edit')")
    @PostMapping("/member")
    public AjaxResult addMember(@RequestBody AuditProjectMember m) { return toAjax(memberMapper.insert(m)); }

    @PreAuthorize("@ss.hasPermi('audit:prepare:edit')")
    @PutMapping("/member")
    public AjaxResult updateMember(@RequestBody AuditProjectMember m) { return toAjax(memberMapper.update(m)); }

    @PreAuthorize("@ss.hasPermi('audit:prepare:edit')")
    @DeleteMapping("/member/{id}")
    public AjaxResult delMember(@PathVariable Long id) { return toAjax(memberMapper.deleteById(id)); }

    // === 资源负载 ===
    @PreAuthorize("@ss.hasPermi('audit:prepare:view')")
    @GetMapping("/workload")
    public AjaxResult workload() { return success(memberMapper.selectUserWorkload()); }

    // === 资料清单 ===
    @PreAuthorize("@ss.hasPermi('audit:prepare:view')")
    @GetMapping("/material/{projectId}")
    public AjaxResult materialList(@PathVariable Long projectId) { return success(materialMapper.selectByProjectId(projectId)); }

    @PreAuthorize("@ss.hasPermi('audit:prepare:edit')")
    @PostMapping("/material")
    public AjaxResult addMaterial(@RequestBody AuditMaterialChecklist m) { return toAjax(materialMapper.insert(m)); }

    @PreAuthorize("@ss.hasPermi('audit:prepare:edit')")
    @PutMapping("/material")
    public AjaxResult updateMaterial(@RequestBody AuditMaterialChecklist m) { return toAjax(materialMapper.update(m)); }

    @PreAuthorize("@ss.hasPermi('audit:prepare:edit')")
    @DeleteMapping("/material/{id}")
    public AjaxResult delMaterial(@PathVariable Long id) { return toAjax(materialMapper.deleteById(id)); }
}
