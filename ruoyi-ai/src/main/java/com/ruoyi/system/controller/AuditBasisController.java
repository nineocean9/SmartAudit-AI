package com.ruoyi.system.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.AuditBasis;
import com.ruoyi.system.domain.AuditBasisRelation;
import com.ruoyi.system.mapper.AuditBasisRelationMapper;
import com.ruoyi.system.service.IAuditBasisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private AuditBasisRelationMapper auditBasisRelationMapper;

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
     * 依据关联信息
     * GET /basis/{id}/related
     */
    @PreAuthorize("@ss.hasPermi('ai:basis:query')")
    @GetMapping("/{id}/related")
    public AjaxResult related(@PathVariable Long id)
    {
        List<AuditBasisRelation> relations = auditBasisRelationMapper.selectByBasisId(id);
        List<Map<String, Object>> upper = new ArrayList<>();
        List<Map<String, Object>> lower = new ArrayList<>();
        List<Map<String, Object>> revisions = new ArrayList<>();

        for (AuditBasisRelation relation : relations)
        {
            boolean currentIsSource = id.equals(relation.getBasisId());
            Long targetId = currentIsSource ? relation.getRelatedId() : relation.getBasisId();
            AuditBasis basis = auditBasisService.selectAuditBasisById(targetId);
            if (basis == null)
            {
                continue;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", basis.getId());
            item.put("title", basis.getTitle());
            item.put("docNumber", basis.getDocNumber());
            item.put("hierarchyLevel", basis.getHierarchyLevel());
            item.put("relationType", relation.getRelationType());

            String type = relation.getRelationType() == null ? "" : relation.getRelationType();
            if (type.contains("替代") || type.contains("修订"))
            {
                revisions.add(item);
            }
            else if ((currentIsSource && type.contains("上位")) || (!currentIsSource && type.contains("下位")))
            {
                upper.add(item);
            }
            else if ((currentIsSource && type.contains("下位")) || (!currentIsSource && type.contains("上位")))
            {
                lower.add(item);
            }
            else
            {
                lower.add(item);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("upper", upper);
        data.put("lower", lower);
        data.put("revisions", revisions);
        return success(data);
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
