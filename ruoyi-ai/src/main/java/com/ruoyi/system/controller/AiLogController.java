package com.ruoyi.system.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.AiCallLog;
import com.ruoyi.system.mapper.AiCallLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/ai/log")
public class AiLogController extends BaseController
{
    @Autowired
    private AiCallLogMapper aiCallLogMapper;

    @PreAuthorize("@ss.hasPermi('ai:log:view')")
    @GetMapping("/list")
    public TableDataInfo list(AiCallLog log)
    {
        startPage();
        List<AiCallLog> list = aiCallLogMapper.selectAiCallLogList(log);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('ai:log:view')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(aiCallLogMapper.selectAiCallLogById(id));
    }
}
