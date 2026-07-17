package com.ruoyi.system.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.mapper.AuditInfoMapper;
import com.ruoyi.system.mapper.AuditProjectMemberMapper;
import com.ruoyi.system.service.AuditProjectAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/audit/progress")
public class AuditProgressController extends BaseController
{
    @Autowired private AuditInfoMapper infoMapper;
    @Autowired private AuditProjectMemberMapper memberMapper;
    @Autowired private AuditProjectAccessService projectAccessService;

    @PreAuthorize("@ss.hasPermi('audit:progress:view')")
    @GetMapping("/gantt")
    public AjaxResult ganttData() {
        List<Map<String, Object>> projects = infoMapper.selectProjectProgress();
        projects.removeIf(p -> !projectAccessService.canAccessProject(toLong(p.get("id"))));
        return success(projects);
    }

    @PreAuthorize("@ss.hasPermi('audit:progress:view')")
    @GetMapping("/workload")
    public AjaxResult workload() {
        return success(memberMapper.selectUserWorkload());
    }

    @PreAuthorize("@ss.hasPermi('audit:progress:view')")
    @GetMapping("/overdue")
    public AjaxResult overdue() {
        List<Map<String, Object>> all = infoMapper.selectProjectProgress();
        all.removeIf(p -> !projectAccessService.canAccessProject(toLong(p.get("id"))));
        List<Map<String, Object>> overdue = new ArrayList<>();
        for (Map<String, Object> p : all) {
            Object status = p.get("status");
            if (status != null && Integer.parseInt(status.toString()) == 1) {
                Object isOverdue = p.get("is_overdue");
                if (isOverdue != null && Integer.parseInt(isOverdue.toString()) == 1) {
                    overdue.add(p);
                }
            }
        }
        return success(overdue);
    }

    private Long toLong(Object value)
    {
        if (value == null || value.toString().isBlank()) return null;
        return Long.valueOf(value.toString());
    }
}
