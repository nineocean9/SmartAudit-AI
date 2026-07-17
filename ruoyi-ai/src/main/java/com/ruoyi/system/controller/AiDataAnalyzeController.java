package com.ruoyi.system.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.IAiDataAnalyzeService;
import com.ruoyi.system.service.IProjectDocService;
import com.ruoyi.system.service.AuditProjectAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static com.ruoyi.common.core.domain.AjaxResult.success;

@RestController
@RequestMapping("/ai/data")
public class AiDataAnalyzeController
{
    @Autowired
    private IAiDataAnalyzeService dataAnalyzeService;

    @Autowired
    private IProjectDocService projectDocService;

    @Autowired
    private AuditProjectAccessService projectAccessService;

    @PreAuthorize("@ss.hasAnyRoles('admin,school_leader,audit_director,audit_project_leader,audit_staff')")
    @GetMapping("/analyze")
    public AjaxResult analyze(@RequestParam String instruction)
    {
        return success(dataAnalyzeService.analyze(instruction));
    }

    @PreAuthorize("@ss.hasPermi('audit:visual:view')")
    @GetMapping("/analysis/list")
    public TableDataInfo listAnalysis(@RequestParam(required = false) String keyword,
                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize)
    {
        return dataAnalyzeService.listAnalysisResults(keyword, pageNum, pageSize);
    }

    @PreAuthorize("@ss.hasPermi('audit:visual:view')")
    @PostMapping("/analyze-chart")
    public AjaxResult analyzeChart(@RequestBody Map<String, String> body)
    {
        String dataText = body.getOrDefault("dataText", "");
        String instruction = body.getOrDefault("instruction", "分析数据");
        String projectName = body.getOrDefault("projectName", null);
        String keyword = body.getOrDefault("keyword", null);
        String sourceType = body.getOrDefault("sourceType", "chat");
        String createBy = SecurityUtils.getUsername();
        Long projectId = parseLong(body.get("projectId"));
        if (projectId != null && !projectAccessService.canAccessProject(projectId))
            return AjaxResult.error("无权分析该项目");
        projectName = projectId != null ? projectAccessService.getProjectName(projectId) : null;
        return success(dataAnalyzeService.analyzeChart(dataText, instruction, projectId, projectName, keyword, sourceType, createBy));
    }

    @PreAuthorize("@ss.hasPermi('audit:visual:view')")
    @PostMapping("/analyze-upload")
    public AjaxResult analyzeUpload(@RequestParam("file") MultipartFile file,
                                     @RequestParam(required = false) Long projectId,
                                     @RequestParam(required = false) String projectName,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String title)
    {
        if (file == null || file.isEmpty()) return AjaxResult.error("请选择文件");
        try
        {
            String createBy = SecurityUtils.getUsername();
            if (projectId == null && projectName != null) projectId = projectAccessService.findAccessibleProjectId(projectName);
            if (projectId == null) return AjaxResult.error("请选择有权访问的关联项目");
            if (projectId != null && !projectAccessService.canAccessProject(projectId))
                return AjaxResult.error("无权向该项目上传或分析文件");
            projectName = projectId != null ? projectAccessService.getProjectName(projectId) : null;
            var parseResult = projectDocService.uploadDocument(projectId, null, keyDocType(keyword), file, createBy);
            String dataText = parseResult.getContentText();
            if (dataText == null || dataText.isBlank())
                return AjaxResult.error("文件内容为空，无法分析");
            return success(dataAnalyzeService.analyzeChart(dataText, keyword != null ? keyword : "数据分析", projectId, projectName, keyword, "upload", createBy));
        }
        catch (Exception e) { return AjaxResult.error("分析失败: " + e.getMessage()); }
    }

    @PreAuthorize("@ss.hasPermi('audit:visual:view')")
    @PostMapping("/analyze-project")
    public AjaxResult analyzeProject(@RequestBody Map<String, String> body)
    {
        String projectName = body.getOrDefault("projectName", "");
        String keyword = body.getOrDefault("keyword", "分析");
        String createBy = SecurityUtils.getUsername();
        Long projectId = parseLong(body.get("projectId"));
        if (projectId == null && !projectName.isBlank()) projectId = projectAccessService.findAccessibleProjectId(projectName);
        if (projectId == null || !projectAccessService.canAccessProject(projectId))
            return AjaxResult.error("项目不存在或无权访问");
        projectName = projectAccessService.getProjectName(projectId);

        String dataText = projectDocService.getMergedProjectText(projectId);
        if (dataText == null || dataText.isBlank())
            return AjaxResult.error("未找到项目" + projectName + "的资料，请先上传相关文件");

        return success(dataAnalyzeService.analyzeChart(dataText, keyword, projectId, projectName, keyword, "project", createBy));
    }

    @PreAuthorize("@ss.hasPermi('audit:visual:view')")
    @GetMapping("/analysis/{id}")
    public AjaxResult getAnalysis(@PathVariable Long id)
    {
        return success(dataAnalyzeService.getAnalysisResult(id));
    }

    @PreAuthorize("@ss.hasPermi('audit:visual:view')")
    @DeleteMapping("/analysis/{ids}")
    public AjaxResult deleteAnalysis(@PathVariable Long[] ids)
    {
        return AjaxResult.success(dataAnalyzeService.deleteAnalysisResults(ids));
    }

    @PreAuthorize("@ss.hasPermi('audit:visual:view')")
    @PutMapping("/analysis")
    public AjaxResult updateAnalysis(@RequestBody Map<String, Object> body)
    {
        Long id = Long.valueOf(body.get("id").toString());
        String title = (String) body.getOrDefault("title", "");
        String projectName = (String) body.getOrDefault("projectName", "");
        String keyword = (String) body.getOrDefault("keyword", "");
        return AjaxResult.success(dataAnalyzeService.updateAnalysisResult(id, title, projectName, keyword));
    }

    /**
     * 直接返回 AI 生成的 HTML 驾驶舱页面
     * GET /ai/data/analysis/{id}/html
     */
    @PreAuthorize("@ss.hasPermi('audit:visual:view')")
    @GetMapping(value = "/analysis/{id}/html", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> getAnalysisHtml(@PathVariable Long id)
    {
        String html = dataAnalyzeService.getAnalysisHtml(id);
        if (html != null && !html.isBlank())
        {
            return ResponseEntity.ok(html);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("<h2 style='text-align:center;padding:80px;color:#999'>未找到分析结果 id=" + id + "</h2>");
    }

    private Long parseLong(String value)
    {
        try { return value == null || value.isBlank() ? null : Long.valueOf(value); }
        catch (NumberFormatException e) { return null; }
    }

    private String keyDocType(String keyword)
    {
        if (keyword == null) return "其他";
        if (keyword.contains("预算") || keyword.contains("收入") || keyword.contains("支出")) return "财务数据";
        if (keyword.contains("采购") || keyword.contains("合同")) return "合同";
        return "其他";
    }
}
