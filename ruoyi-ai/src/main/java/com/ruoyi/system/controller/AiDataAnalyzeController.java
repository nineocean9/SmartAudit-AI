package com.ruoyi.system.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.IAiDataAnalyzeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import static com.ruoyi.common.core.domain.AjaxResult.success;

@RestController
@RequestMapping("/ai/data")
public class AiDataAnalyzeController
{
    @Autowired
    private IAiDataAnalyzeService dataAnalyzeService;

    @GetMapping("/analyze")
    public AjaxResult analyze(@RequestParam String instruction)
    {
        return success(dataAnalyzeService.analyze(instruction));
    }
}
