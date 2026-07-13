package com.ruoyi.system.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.AuditTempAuth;
import com.ruoyi.system.mapper.AuditTempAuthMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit/permission")
public class AuditPermissionController extends BaseController
{
    @Autowired private AuditTempAuthMapper authMapper;

    @PreAuthorize("@ss.hasPermi('audit:auth:view')")
    @GetMapping("/tempAuth/list")
    public TableDataInfo list(AuditTempAuth q) { startPage(); return getDataTable(authMapper.selectList(q)); }

    @PreAuthorize("@ss.hasPermi('audit:auth:edit')")
    @PostMapping("/tempAuth")
    public AjaxResult grant(@RequestBody AuditTempAuth a) { return toAjax(authMapper.insert(a)); }

    @PreAuthorize("@ss.hasPermi('audit:auth:edit')")
    @PutMapping("/tempAuth/revoke/{id}")
    public AjaxResult revoke(@PathVariable Long id) {
        AuditTempAuth a = new AuditTempAuth(); a.setId(id); a.setStatus(0);
        return toAjax(authMapper.update(a));
    }

    @PreAuthorize("@ss.hasPermi('audit:auth:edit')")
    @DeleteMapping("/tempAuth/{id}")
    public AjaxResult delete(@PathVariable Long id) { return toAjax(authMapper.deleteById(id)); }

    @PreAuthorize("@ss.hasPermi('audit:auth:edit')")
    @PostMapping("/tempAuth/revokeExpired")
    public AjaxResult revokeExpired() { return toAjax(authMapper.revokeExpired()); }
}
