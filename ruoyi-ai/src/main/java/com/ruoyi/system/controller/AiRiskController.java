package com.ruoyi.system.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.RiskClue;
import com.ruoyi.system.service.IAiRiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/ai/risk")
public class AiRiskController extends BaseController
{
    @Autowired
    private IAiRiskService aiRiskService;

    @PreAuthorize("@ss.hasPermi('ai:risk:view')")
    @GetMapping("/list")
    public TableDataInfo list(RiskClue clue)
    {
        startPage();
        List<RiskClue> list = aiRiskService.selectRiskClueList(clue);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('ai:risk:view')")
    @PostMapping
    public AjaxResult add(@RequestBody RiskClue clue)
    {
        return toAjax(aiRiskService.insertRiskClue(clue));
    }

    @PreAuthorize("@ss.hasPermi('ai:risk:view')")
    @PutMapping
    public AjaxResult edit(@RequestBody RiskClue clue)
    {
        return toAjax(aiRiskService.updateRiskClue(clue));
    }

    @PreAuthorize("@ss.hasPermi('ai:risk:view')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(aiRiskService.deleteRiskClueByIds(ids));
    }
}
