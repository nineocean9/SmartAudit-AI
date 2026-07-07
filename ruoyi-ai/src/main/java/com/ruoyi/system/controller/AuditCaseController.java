package com.ruoyi.system.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.AuditCaseLib;
import com.ruoyi.system.service.IAuditBasisLibService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/basis/case")
public class AuditCaseController extends BaseController
{
    @Autowired
    private IAuditBasisLibService service;

    @PreAuthorize("@ss.hasPermi('audit:basis:query')")
    @GetMapping("/list")
    public TableDataInfo list(AuditCaseLib c) { startPage(); return getDataTable(service.selectCaseLibList(c)); }

    @PreAuthorize("@ss.hasPermi('audit:basis:add')")
    @Log(title = "案例库", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AuditCaseLib c) { return toAjax(service.insertCaseLib(c)); }

    @PreAuthorize("@ss.hasPermi('audit:basis:edit')")
    @Log(title = "案例库", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AuditCaseLib c) { return toAjax(service.updateCaseLib(c)); }

    @PreAuthorize("@ss.hasPermi('audit:basis:remove')")
    @Log(title = "案例库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) { return toAjax(service.deleteCaseLibByIds(ids)); }
}
