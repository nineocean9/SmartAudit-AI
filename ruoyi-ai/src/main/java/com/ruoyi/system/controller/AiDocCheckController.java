package com.ruoyi.system.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.DocCheckTask;
import com.ruoyi.system.service.IAiDocCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/ai/doc")
public class AiDocCheckController extends BaseController
{
    @Autowired
    private IAiDocCheckService aiDocCheckService;

    @PreAuthorize("@ss.hasPermi('ai:doc:check')")
    @GetMapping("/list")
    public TableDataInfo list(DocCheckTask task)
    {
        startPage();
        List<DocCheckTask> list = aiDocCheckService.selectDocCheckTaskList(task);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('ai:doc:check')")
    @PostMapping("/check")
    public AjaxResult check(@RequestParam("file") MultipartFile file)
    {
        Long userId = SecurityUtils.getUserId();
        return success(aiDocCheckService.checkDocument(file, userId));
    }

    @PreAuthorize("@ss.hasPermi('ai:doc:check')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(aiDocCheckService.selectDocCheckTaskById(id));
    }
}
