package com.ruoyi.system.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.AuditBasis;
import com.ruoyi.system.service.IAuditBasisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审计依据库 Controller
 * 提供依据条目的增删改查 REST API
 *
 * 接口路径前缀：/basis
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/basis")
public class AuditBasisController extends BaseController
{
    @Autowired
    private IAuditBasisService auditBasisService;

    /**
     * 查询依据库列表（分页）
     * GET /basis/list
     */
    @PreAuthorize("@ss.hasPermi('ai:basis:query')")
    @GetMapping("/list")
    public TableDataInfo list(AuditBasis basis)
    {
        startPage();
        List<AuditBasis> list = auditBasisService.selectAuditBasisList(basis);
        return getDataTable(list);
    }

    /**
     * 依据详情
     * GET /basis/{id}
     */
    @PreAuthorize("@ss.hasPermi('ai:basis:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(auditBasisService.selectAuditBasisById(id));
    }

    /**
     * 新增依据
     * POST /basis
     */
    @PreAuthorize("@ss.hasPermi('ai:basis:add')")
    @Log(title = "依据库管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AuditBasis basis)
    {
        return toAjax(auditBasisService.insertAuditBasis(basis));
    }

    /**
     * 修改依据
     * PUT /basis
     */
    @PreAuthorize("@ss.hasPermi('ai:basis:edit')")
    @Log(title = "依据库管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AuditBasis basis)
    {
        return toAjax(auditBasisService.updateAuditBasis(basis));
    }

    /**
     * 删除依据
     * DELETE /basis/{ids}
     */
    @PreAuthorize("@ss.hasPermi('ai:basis:remove')")
    @Log(title = "依据库管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(auditBasisService.deleteAuditBasisByIds(ids));
    }

    /**
     * 切换依据生效/失效状态
     * PUT /basis/status/{id}/{status}
     */
    @PreAuthorize("@ss.hasPermi('ai:basis:edit')")
    @Log(title = "依据库管理", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{id}/{status}")
    public AjaxResult changeStatus(@PathVariable Long id, @PathVariable Integer status)
    {
        return toAjax(auditBasisService.updateAuditBasisStatus(id, status));
    }

    /**
     * 关键词全文检索
     * GET /basis/search?keyword=xxx&category=xxx
     */
    @PreAuthorize("@ss.hasPermi('ai:basis:query')")
    @GetMapping("/search")
    public AjaxResult search(@RequestParam String keyword,
                             @RequestParam(required = false) String category)
    {
        List<AuditBasis> list = auditBasisService.searchByKeyword(keyword, category);
        return success(list);
    }
}
