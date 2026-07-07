package com.ruoyi.system.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.ForensicDraft;
import com.ruoyi.system.service.IAiForensicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/ai/forensic")
public class AiForensicController extends BaseController
{
    @Autowired
    private IAiForensicService aiForensicService;

    @PreAuthorize("@ss.hasPermi('ai:forensic:view')")
    @GetMapping("/list")
    public TableDataInfo list(ForensicDraft draft)
    {
        startPage();
        List<ForensicDraft> list = aiForensicService.selectForensicDraftList(draft);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('ai:forensic:view')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(aiForensicService.selectForensicDraftById(id));
    }

    @PreAuthorize("@ss.hasPermi('ai:forensic:gen')")
    @Log(title = "取证单", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public AjaxResult generate(@RequestParam String issue, @RequestParam(required = false) String basisIds)
    {
        return success(aiForensicService.generateDraft(issue, basisIds));
    }

    @PreAuthorize("@ss.hasPermi('ai:forensic:view')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(aiForensicService.deleteForensicDraftByIds(ids));
    }
}
