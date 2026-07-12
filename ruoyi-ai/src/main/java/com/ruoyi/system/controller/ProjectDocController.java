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
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
     * 获取 Excel 结构化数据（表格渲染用）
     * GET /project/doc/{id}/excel-data
     */
    @GetMapping("/{id}/excel-data")
    public AjaxResult getExcelData(@PathVariable Long id)
    {
        ProjectDocument doc = projectDocService.getDocumentById(id);
        if (doc == null) return AjaxResult.error("文档不存在");

        String ext = doc.getFileExt();
        if (!"xlsx".equalsIgnoreCase(ext) && !"xls".equalsIgnoreCase(ext))
        {
            return AjaxResult.error("非 Excel 文件");
        }

        String filePath = doc.getFilePath();
        if (filePath != null && filePath.startsWith("/profile"))
        {
            filePath = RuoYiConfig.getProfile() + filePath.substring("/profile".length());
        }

        File file = filePath != null ? new File(filePath) : null;
        if (file == null || !file.exists())
        {
            return AjaxResult.error("文件不存在或已被删除");
        }

        try
        {
            Workbook wb;
            if ("xlsx".equalsIgnoreCase(ext))
                wb = new XSSFWorkbook(new FileInputStream(file));
            else
                wb = new HSSFWorkbook(new FileInputStream(file));

            List<Map<String, Object>> sheets = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();

            for (int i = 0; i < wb.getNumberOfSheets(); i++)
            {
                Sheet sheet = wb.getSheetAt(i);
                Map<String, Object> sheetData = new LinkedHashMap<>();
                sheetData.put("name", sheet.getSheetName());

                List<String> headers = new ArrayList<>();
                List<List<String>> rows = new ArrayList<>();

                boolean firstRow = true;
                for (Row row : sheet)
                {
                    List<String> cells = new ArrayList<>();
                    int lastCol = row.getLastCellNum();
                    for (int c = 0; c < lastCol; c++)
                    {
                        Cell cell = row.getCell(c);
                        cells.add(cell != null ? formatter.formatCellValue(cell) : "");
                    }

                    if (firstRow)
                    {
                        headers.addAll(cells);
                        firstRow = false;
                    }
                    else
                    {
                        rows.add(cells);
                    }
                }

                sheetData.put("headers", headers);
                sheetData.put("rows", rows);
                sheets.add(sheetData);
            }
            wb.close();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fileName", doc.getFileName());
            result.put("fileSize", doc.getFileSize());
            result.put("sheets", sheets);
            return success(result);
        }
        catch (Exception e)
        {
            return AjaxResult.error("Excel 解析失败: " + e.getMessage());
        }
    }

    /**
     * 修改 Excel 单元格
     * PUT /project/doc/{id}/excel-cell
     */
    @PutMapping("/{id}/excel-cell")
    public AjaxResult updateExcelCell(@PathVariable Long id, @RequestBody Map<String, Object> body)
    {
        ProjectDocument doc = projectDocService.getDocumentById(id);
        if (doc == null) return AjaxResult.error("文档不存在");

        String ext = doc.getFileExt();
        if (!"xlsx".equalsIgnoreCase(ext) && !"xls".equalsIgnoreCase(ext))
            return AjaxResult.error("非 Excel 文件");

        String filePath = doc.getFilePath();
        if (filePath != null && filePath.startsWith("/profile"))
            filePath = RuoYiConfig.getProfile() + filePath.substring("/profile".length());

        File file = filePath != null ? new File(filePath) : null;
        if (file == null || !file.exists()) return AjaxResult.error("文件不存在");

        int sheetIdx = Integer.parseInt(body.getOrDefault("sheetIndex", "0").toString());
        int rowIdx = Integer.parseInt(body.get("rowIndex").toString());
        int colIdx = Integer.parseInt(body.get("colIndex").toString());
        String value = body.get("value") != null ? body.get("value").toString() : "";

        try (FileInputStream fis = new FileInputStream(file))
        {
            Workbook wb = "xlsx".equalsIgnoreCase(ext) ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis);
            Sheet sheet = wb.getSheetAt(sheetIdx);
            Row row = sheet.getRow(rowIdx);
            if (row == null) row = sheet.createRow(rowIdx);
            Cell cell = row.getCell(colIdx);
            if (cell == null) cell = row.createCell(colIdx);
            cell.setCellValue(value);

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file))
            {
                wb.write(fos);
            }
            wb.close();
            return success();
        }
        catch (Exception e)
        {
            return AjaxResult.error("修改失败: " + e.getMessage());
        }
    }

    /**
     * 新增 Excel 行
     * POST /project/doc/{id}/excel-row
     */
    @PostMapping("/{id}/excel-row")
    public AjaxResult addExcelRow(@PathVariable Long id, @RequestBody Map<String, Object> body)
    {
        ProjectDocument doc = projectDocService.getDocumentById(id);
        if (doc == null) return AjaxResult.error("文档不存在");

        String ext = doc.getFileExt();
        String filePath = doc.getFilePath();
        if (filePath != null && filePath.startsWith("/profile"))
            filePath = RuoYiConfig.getProfile() + filePath.substring("/profile".length());
        File file = filePath != null ? new File(filePath) : null;
        if (file == null || !file.exists()) return AjaxResult.error("文件不存在");

        int sheetIdx = Integer.parseInt(body.getOrDefault("sheetIndex", "0").toString());
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) body.get("values");

        try (FileInputStream fis = new FileInputStream(file))
        {
            Workbook wb = "xlsx".equalsIgnoreCase(ext) ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis);
            Sheet sheet = wb.getSheetAt(sheetIdx);
            int newRowIdx = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(newRowIdx);
            if (values != null)
            {
                for (int c = 0; c < values.size(); c++)
                {
                    row.createCell(c).setCellValue(values.get(c) != null ? values.get(c) : "");
                }
            }

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file))
            {
                wb.write(fos);
            }
            wb.close();
            return success();
        }
        catch (Exception e)
        {
            return AjaxResult.error("新增行失败: " + e.getMessage());
        }
    }

    /**
     * 删除 Excel 行
     * DELETE /project/doc/{id}/excel-row
     */
    @DeleteMapping("/{id}/excel-row")
    public AjaxResult deleteExcelRow(@PathVariable Long id, @RequestBody Map<String, Object> body)
    {
        ProjectDocument doc = projectDocService.getDocumentById(id);
        if (doc == null) return AjaxResult.error("文档不存在");

        String ext = doc.getFileExt();
        String filePath = doc.getFilePath();
        if (filePath != null && filePath.startsWith("/profile"))
            filePath = RuoYiConfig.getProfile() + filePath.substring("/profile".length());
        File file = filePath != null ? new File(filePath) : null;
        if (file == null || !file.exists()) return AjaxResult.error("文件不存在");

        int sheetIdx = Integer.parseInt(body.getOrDefault("sheetIndex", "0").toString());
        int rowIdx = Integer.parseInt(body.get("rowIndex").toString());

        try (FileInputStream fis = new FileInputStream(file))
        {
            Workbook wb = "xlsx".equalsIgnoreCase(ext) ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis);
            Sheet sheet = wb.getSheetAt(sheetIdx);
            int lastRow = sheet.getLastRowNum();
            // +1 因为第0行是 header，data row 0 对应 sheet row 1
            int actualRow = rowIdx + 1;
            if (actualRow <= lastRow)
            {
                sheet.removeRow(sheet.getRow(actualRow));
                if (actualRow < lastRow)
                {
                    sheet.shiftRows(actualRow + 1, lastRow, -1);
                }
            }

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file))
            {
                wb.write(fos);
            }
            wb.close();
            return success();
        }
        catch (Exception e)
        {
            return AjaxResult.error("删除行失败: " + e.getMessage());
        }
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
