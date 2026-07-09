package com.ruoyi.system.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.TempWorkspace;
import com.ruoyi.system.service.ITempWorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ruoyi.common.core.domain.AjaxResult.success;

/**
 * 临时工作区 Controller
 *
 * 管理会话级临时知识库的创建、上传、检索、销毁
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/ai/workspace/temp")
public class TempWorkspaceController
{
    @Autowired
    private ITempWorkspaceService tempWorkspaceService;

    /**
     * 创建临时工作区
     * POST /ai/workspace/temp/create
     * @return {sessionId: "uuid"}
     */
    @PostMapping("/create")
    public AjaxResult create()
    {
        Long userId = SecurityUtils.getUserId();
        String sessionId = tempWorkspaceService.createSession(userId);
        Map<String, String> result = new HashMap<>();
        result.put("sessionId", sessionId);
        return success(result);
    }

    /**
     * 上传文件到临时工作区
     * POST /ai/workspace/temp/upload
     */
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam String sessionId,
                             @RequestParam("file") MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            return AjaxResult.error("请选择文件");
        }

        try
        {
            Long userId = SecurityUtils.getUserId();
            TempWorkspace record = tempWorkspaceService.uploadToTemp(sessionId, userId, file);
            return success(record);
        }
        catch (Exception e)
        {
            return AjaxResult.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 列出临时工作区文件
     * GET /ai/workspace/temp/files?sessionId=xxx
     */
    @GetMapping("/files")
    public AjaxResult listFiles(@RequestParam String sessionId)
    {
        List<TempWorkspace> files = tempWorkspaceService.listTempFiles(sessionId);
        return success(files);
    }

    /**
     * 销毁临时工作区
     * DELETE /ai/workspace/temp/{sessionId}
     */
    @DeleteMapping("/{sessionId}")
    public AjaxResult destroy(@PathVariable String sessionId)
    {
        tempWorkspaceService.destroySession(sessionId);
        return success();
    }
}
