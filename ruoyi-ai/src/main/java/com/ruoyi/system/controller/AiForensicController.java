package com.ruoyi.system.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.ForensicDraft;
import com.ruoyi.system.service.IAiForensicService;
import com.ruoyi.system.service.AuditProjectAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/forensic")
public class AiForensicController extends BaseController
{
    @Autowired
    private IAiForensicService aiForensicService;

    @Autowired
    private AuditProjectAccessService projectAccessService;

    @PreAuthorize("@ss.hasPermi('ai:forensic:view')")
    @GetMapping("/list")
    public TableDataInfo list(ForensicDraft draft)
    {
        startPage();
        List<ForensicDraft> list = aiForensicService.selectForensicDraftList(draft);
        list.removeIf(item -> item.getProjectId() != null && !projectAccessService.canAccessProject(item.getProjectId()));
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('ai:forensic:view')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        ForensicDraft draft = aiForensicService.selectForensicDraftById(id);
        if (draft == null) return AjaxResult.error("取证单不存在");
        if (draft.getProjectId() != null && !projectAccessService.canAccessProject(draft.getProjectId()))
        {
            return AjaxResult.error("无权访问该取证单");
        }
        return success(draft);
    }

    @PreAuthorize("@ss.hasPermi('ai:forensic:gen')")
    @Log(title = "取证单", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody Map<String, Object> body)
    {
        String issue = body.get("issue") != null ? body.get("issue").toString() : null;
        String basisIds = body.get("basisIds") != null ? body.get("basisIds").toString() : null;
        Long projectId = parseLong(body.get("projectId"));
        if (issue == null || issue.isBlank())
        {
            return AjaxResult.error("问题描述不能为空");
        }
        if (projectId != null && !projectAccessService.canAccessProject(projectId))
        {
            return AjaxResult.error("无权基于该项目生成取证单");
        }
        return success(aiForensicService.generateDraft(issue, basisIds, projectId));
    }

    @PreAuthorize("@ss.hasPermi('ai:forensic:review')")
    @PutMapping("/{id}/review")
    public AjaxResult review(@PathVariable Long id, @RequestBody Map<String, Object> body)
    {
        ForensicDraft draft = aiForensicService.selectForensicDraftById(id);
        if (draft == null) return AjaxResult.error("取证单不存在");
        if (draft.getProjectId() != null && !projectAccessService.canAccessProject(draft.getProjectId()))
        {
            return AjaxResult.error("无权复核该取证单");
        }
        Integer reviewStatus = body.get("reviewStatus") != null ? Integer.valueOf(body.get("reviewStatus").toString()) : null;
        if (draft.getReviewStatus() == null || draft.getReviewStatus() != 1)
        {
            return AjaxResult.error("只有待复核的取证草稿可以复核");
        }
        if (reviewStatus == null || (reviewStatus != 2 && reviewStatus != 3))
        {
            return AjaxResult.error("复核操作只允许通过或退回");
        }
        String reviewLog = body.get("reviewLog") != null ? body.get("reviewLog").toString() : draft.getReviewLog();
        draft.setReviewStatus(reviewStatus);
        draft.setReviewLog(reviewLog);
        return toAjax(aiForensicService.updateForensicDraft(draft));
    }

    @PreAuthorize("@ss.hasPermi('ai:forensic:submit')")
    @PutMapping("/{id}/submit")
    public AjaxResult submit(@PathVariable Long id)
    {
        ForensicDraft draft = aiForensicService.selectForensicDraftById(id);
        if (draft == null) return AjaxResult.error("取证单不存在");
        if (draft.getProjectId() != null && !projectAccessService.canAccessProject(draft.getProjectId()))
        {
            return AjaxResult.error("无权提交该取证单");
        }
        if (!isCreator(draft))
        {
            return AjaxResult.error("只能提交本人生成的取证草稿");
        }
        if (draft.getReviewStatus() != null && draft.getReviewStatus() != 0 && draft.getReviewStatus() != 3)
        {
            return AjaxResult.error("只有草稿或已退回状态可以提交复核");
        }
        draft.setReviewStatus(1);
        draft.setReviewLog("提交复核");
        return toAjax(aiForensicService.updateForensicDraft(draft));
    }

    @PreAuthorize("@ss.hasPermi('ai:forensic:delete')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        for (Long id : ids)
        {
            ForensicDraft draft = aiForensicService.selectForensicDraftById(id);
            if (draft != null && draft.getProjectId() != null && !projectAccessService.canAccessProject(draft.getProjectId()))
            {
                return AjaxResult.error("无权删除该取证单");
            }
            if (draft != null && !isCreator(draft) && !hasReviewPermission())
            {
                return AjaxResult.error("只能删除本人生成的取证草稿");
            }
        }
        return toAjax(aiForensicService.deleteForensicDraftByIds(ids));
    }

    private boolean isCreator(ForensicDraft draft)
    {
        return draft.getCreateBy() != null && draft.getCreateBy().equals(SecurityUtils.getUsername());
    }

    private boolean hasReviewPermission()
    {
        return SecurityUtils.isAdmin() || SecurityUtils.getLoginUser().getPermissions().contains("ai:forensic:review");
    }

    private Long parseLong(Object value)
    {
        if (value == null || value.toString().isBlank()) return null;
        return Long.valueOf(value.toString());
    }
}
