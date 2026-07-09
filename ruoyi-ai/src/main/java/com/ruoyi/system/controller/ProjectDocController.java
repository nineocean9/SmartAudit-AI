package com.ruoyi.system.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.system.domain.ProjectDocument;
import com.ruoyi.system.service.IProjectDocService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.ruoyi.common.core.domain.AjaxResult.success;

/**
 * 项目文档 Controller
 *
 * 提供项目文档的上传、查询、删除等 REST 接口
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/project/doc")
public class ProjectDocController extends BaseController
{
    @Autowired
    private IProjectDocService projectDocService;

    /**
     * 获取项目文档列表
     * GET /project/doc/list?projectId=1&docType=底稿
     */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam Long projectId,
                           @RequestParam(required = false) String docType)
    {
        List<ProjectDocument> list = projectDocService.listDocuments(projectId, docType);
        return success(list);
    }

    /**
     * 上传文档到项目
     * POST /project/doc/upload
     */
    @Log(title = "项目文档", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam Long projectId,
                             @RequestParam(required = false) Long planId,
                             @RequestParam(defaultValue = "其他") String docType,
                             @RequestParam("file") MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            return AjaxResult.error("请选择文件");
        }

        try
        {
            String createBy = SecurityUtils.getUsername();
            ProjectDocument doc = projectDocService.uploadDocument(projectId, planId, docType, file, createBy);
            return success(doc);
        }
        catch (Exception e)
        {
            return AjaxResult.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档内容
     * GET /project/doc/{id}/content
     */
    @GetMapping("/{id}/content")
    public AjaxResult getContent(@PathVariable Long id)
    {
        String content = projectDocService.getDocumentContent(id);
        if (content == null)
        {
            return AjaxResult.error("文档不存在");
        }
        return success(content);
    }

    /**
     * 删除项目文档
     * DELETE /project/doc/{id}
     */
    @Log(title = "项目文档", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        int rows = projectDocService.deleteDocument(id);
        return rows > 0 ? success() : AjaxResult.error("删除失败");
    }

    /**
     * 按计划查询文档
     * GET /project/doc/byPlan?planId=1
     */
    @GetMapping("/byPlan")
    public AjaxResult listByPlan(@RequestParam Long planId)
    {
        List<ProjectDocument> list = projectDocService.listByPlan(planId);
        return success(list);
    }

    /**
     * 获取文档详情（含 filePath）
     * GET /project/doc/{id}/detail
     */
    @GetMapping("/{id}/detail")
    public AjaxResult getDetail(@PathVariable Long id)
    {
        ProjectDocument doc = projectDocService.getDocumentById(id);
        if (doc == null) return AjaxResult.error("文档不存在");
        return success(doc);
    }

    /**
     * 下载原始文件
     * GET /project/doc/{id}/file
     */
    @GetMapping("/{id}/file")
    public void downloadFile(@PathVariable Long id, HttpServletResponse response) throws IOException
    {
        ProjectDocument doc = projectDocService.getDocumentById(id);
        if (doc == null || doc.getFilePath() == null)
        {
            response.setStatus(404);
            return;
        }

        // filePath 格式如: /profile/upload/2026/07/07/xxx.docx
        String filePath = doc.getFilePath();
        if (filePath.startsWith("/profile"))
        {
            filePath = RuoYiConfig.getProfile() + filePath.substring("/profile".length());
        }

        File file = new File(filePath);
        if (!file.exists())
        {
            response.setStatus(404);
            return;
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode(doc.getFileName(), StandardCharsets.UTF_8)
                        .replace("+", "%20"));
        response.setContentLengthLong(file.length());

        FileUtils.writeBytes(filePath, response.getOutputStream());
        response.getOutputStream().flush();
    }
}
