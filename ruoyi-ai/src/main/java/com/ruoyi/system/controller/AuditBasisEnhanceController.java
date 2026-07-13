package com.ruoyi.system.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.AuditBasisRelation;
import com.ruoyi.system.mapper.AuditBasisRelationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit/basis")
public class AuditBasisEnhanceController extends BaseController
{
    @Autowired private AuditBasisRelationMapper relationMapper;

    @PreAuthorize("@ss.hasPermi('audit:basis:view')")
    @GetMapping("/relation/{basisId}")
    public AjaxResult relationList(@PathVariable Long basisId) { return success(relationMapper.selectByBasisId(basisId)); }

    @PreAuthorize("@ss.hasPermi('audit:basis:edit')")
    @PostMapping("/relation")
    public AjaxResult addRelation(@RequestBody AuditBasisRelation r) { return toAjax(relationMapper.insert(r)); }

    @PreAuthorize("@ss.hasPermi('audit:basis:edit')")
    @DeleteMapping("/relation/{id}")
    public AjaxResult delRelation(@PathVariable Long id) { return toAjax(relationMapper.deleteById(id)); }
}
