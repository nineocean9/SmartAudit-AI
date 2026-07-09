package com.ruoyi.system.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.IAiDataAnalyzeService;
import com.ruoyi.system.service.IProjectDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    private DataSource dataSource;

    @GetMapping("/analyze")
    public AjaxResult analyze(@RequestParam String instruction)
    {
        return success(dataAnalyzeService.analyze(instruction));
    }

    @GetMapping("/analysis/list")
    public TableDataInfo listAnalysis(@RequestParam(required = false) String keyword,
                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize)
    {
        return dataAnalyzeService.listAnalysisResults(keyword, pageNum, pageSize);
    }

    @PostMapping("/analyze-chart")
    public AjaxResult analyzeChart(@RequestBody Map<String, String> body)
    {
        String dataText = body.getOrDefault("dataText", "");
        String instruction = body.getOrDefault("instruction", "分析数据");
        String projectName = body.getOrDefault("projectName", null);
        String keyword = body.getOrDefault("keyword", null);
        String sourceType = body.getOrDefault("sourceType", "chat");
        String createBy = SecurityUtils.getUsername();
        return success(dataAnalyzeService.analyzeChart(dataText, instruction, projectName, keyword, sourceType, createBy));
    }

    @PostMapping("/analyze-upload")
    public AjaxResult analyzeUpload(@RequestParam("file") MultipartFile file,
                                     @RequestParam(required = false) String projectName,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String title)
    {
        if (file == null || file.isEmpty()) return AjaxResult.error("请选择文件");
        try
        {
            String createBy = SecurityUtils.getUsername();
            Long projectId = lookupProjectId(projectName);
            var parseResult = projectDocService.uploadDocument(projectId, null, keyDocType(keyword), file, createBy);
            String dataText = parseResult.getContentText();
            if (dataText == null || dataText.isBlank())
                return AjaxResult.error("文件内容为空，无法分析");
            return success(dataAnalyzeService.analyzeChart(dataText, keyword != null ? keyword : "数据分析", projectName, keyword, "upload", createBy));
        }
        catch (Exception e) { return AjaxResult.error("分析失败: " + e.getMessage()); }
    }

    @PostMapping("/analyze-project")
    public AjaxResult analyzeProject(@RequestBody Map<String, String> body)
    {
        String projectName = body.getOrDefault("projectName", "");
        String keyword = body.getOrDefault("keyword", "分析");
        String createBy = SecurityUtils.getUsername();
        if (projectName.isBlank()) return AjaxResult.error("请输入项目名称");

        String dataText = projectDocService.getMergedProjectTextByProjectName(projectName);
        if (dataText == null || dataText.isBlank())
            return AjaxResult.error("未找到项目" + projectName + "的资料，请先上传相关文件");

        return success(dataAnalyzeService.analyzeChart(dataText, keyword, projectName, keyword, "project", createBy));
    }

    @GetMapping("/analysis/{id}")
    public AjaxResult getAnalysis(@PathVariable Long id)
    {
        return success(dataAnalyzeService.getAnalysisResult(id));
    }

    /**
     * 直接返回 AI 生成的 HTML 驾驶舱页面
     * GET /ai/data/analysis/{id}/html
     */
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

    private Long lookupProjectId(String projectName)
    {
        if (projectName == null || projectName.isBlank()) return null;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM audit_project WHERE project_name ILIKE ? OR audited_unit ILIKE ? LIMIT 1"))
        {
            ps.setString(1, "%" + projectName + "%");
            ps.setString(2, "%" + projectName + "%");
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getLong("id"); }
        }
        catch (Exception e) { /* ignore */ }
        return null;
    }

    private String keyDocType(String keyword)
    {
        if (keyword == null) return "其他";
        if (keyword.contains("预算") || keyword.contains("收入") || keyword.contains("支出")) return "财务数据";
        if (keyword.contains("采购") || keyword.contains("合同")) return "合同";
        return "其他";
    }
}
