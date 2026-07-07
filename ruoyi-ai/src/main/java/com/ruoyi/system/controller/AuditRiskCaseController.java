package com.ruoyi.system.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.AuditRiskCase;
import com.ruoyi.system.service.IAuditBasisLibService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/basis/risk")
public class AuditRiskCaseController extends BaseController
{
    @Autowired
    private IAuditBasisLibService service;

    @PreAuthorize("@ss.hasPermi('audit:basis:query')")
    @GetMapping("/list")
    public TableDataInfo list(AuditRiskCase r) { startPage(); return getDataTable(service.selectRiskCaseList(r)); }

    @PreAuthorize("@ss.hasPermi('audit:basis:add')")
    @Log(title = "问题风险库", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AuditRiskCase r) { return toAjax(service.insertRiskCase(r)); }

    @PreAuthorize("@ss.hasPermi('audit:basis:edit')")
    @Log(title = "问题风险库", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AuditRiskCase r) { return toAjax(service.updateRiskCase(r)); }

    @PreAuthorize("@ss.hasPermi('audit:basis:remove')")
    @Log(title = "问题风险库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) { return toAjax(service.deleteRiskCaseByIds(ids)); }
}
