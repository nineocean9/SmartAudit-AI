package com.ruoyi.system.service.impl;

import com.ruoyi.system.service.IFileParseService;
import com.ruoyi.system.util.FileTypeDetector;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 文件解析服务实现
 *
 * 直接使用 Apache POI 解析 Word/Excel，PDFBox 解析 PDF，
 * 不依赖 Tika 的 ServiceLoader SPI 机制（避免类路径问题）
 *
 * 支持：
 * - Word (.doc / .docx)
 * - PDF (.pdf)
 * - Excel (.xlsx / .xls)
 * - CSV / TXT / Markdown
 * - 图片 (.png/.jpg) — 仅标记，不做 OCR
 *
 * @author ruoyi
 */
@Service
public class FileParseServiceImpl implements IFileParseService
{
    private static final Logger log = LoggerFactory.getLogger(FileParseServiceImpl.class);

    /** 最大解析字符数 */
    private static final int MAX_CHARS = 1_000_000;

    @Override
    public FileParseResult parse(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            return FileParseResult.fail("文件为空");
        }
        return parse(file, file.getOriginalFilename());
    }

    @Override
    public FileParseResult parse(MultipartFile file, String originalFileName)
    {
        if (file == null || file.isEmpty())
        {
            return FileParseResult.fail("文件为空");
        }

        String fileName = originalFileName != null ? originalFileName : file.getOriginalFilename();
        String fileType = FileTypeDetector.detect(fileName != null ? fileName : "");

        try
        {
            byte[] bytes = file.getBytes();

            // 图片文件仅标记
            if (FileTypeDetector.isImage(fileName))
            {
                FileParseResult r = FileParseResult.ok("[图片文件: " + fileName + "]", fileType);
                r.charCount = 0;
                return r;
            }

            // 按文件类型解析
            switch (fileType)
            {
                case "docx":
                    return parseDocx(bytes, fileName);
                case "doc":
                    return parseDoc(bytes, fileName);
                case "xlsx":
                    return parseXlsx(bytes, fileName);
                case "xls":
                    return parseXls(bytes, fileName);
                case "pdf":
                    return parsePdf(bytes, fileName);
                case "txt":
                case "md":
                case "csv":
                    return parsePlainText(bytes, fileName, fileType);
                default:
                    // 未知格式，尝试用 Tika 兜底
                    return parseWithTika(bytes, fileName, fileType);
            }
        }
        catch (Exception e)
        {
            log.error("文件解析失败: {}", fileName, e);
            return FileParseResult.fail("解析异常: " + e.getMessage());
        }
    }

    @Override
    public FileParseResult parse(File file)
    {
        if (file == null || !file.exists())
        {
            return FileParseResult.fail("文件不存在");
        }
        try
        {
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            String fileName = file.getName();
            String fileType = FileTypeDetector.detect(fileName);

            if (FileTypeDetector.isImage(fileName))
            {
                FileParseResult r = FileParseResult.ok("[图片文件: " + fileName + "]", fileType);
                r.charCount = 0;
                return r;
            }

            switch (fileType)
            {
                case "docx": return parseDocx(bytes, fileName);
                case "doc":  return parseDoc(bytes, fileName);
                case "xlsx": return parseXlsx(bytes, fileName);
                case "xls":  return parseXls(bytes, fileName);
                case "pdf":  return parsePdf(bytes, fileName);
                default:     return parsePlainText(bytes, fileName, fileType);
            }
        }
        catch (Exception e)
        {
            return FileParseResult.fail("读取文件失败: " + e.getMessage());
        }
    }

    // ---- 格式专用解析器 ----

    /**
     * 解析 .docx (Word 2007+)
     */
    private FileParseResult parseDocx(byte[] bytes, String fileName)
    {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes)))
        {
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
            String text = extractor.getText();
            if (text == null || text.isBlank())
            {
                return FileParseResult.fail("文档内容为空");
            }
            log.info("POI 解析 docx 成功: {}", fileName);
            return FileParseResult.ok(truncate(text.trim()), "docx");
        }
        catch (Exception e)
        {
            log.error("POI 解析 docx 失败: {}", fileName, e);
            return FileParseResult.fail("Word文档解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析 .doc (Word 97-2003)
     */
    private FileParseResult parseDoc(byte[] bytes, String fileName)
    {
        try
        {
            // HWPF 在 poi-scratchpad 中，可能不可用，捕获异常
            org.apache.poi.hwpf.HWPFDocument hdoc =
                    new org.apache.poi.hwpf.HWPFDocument(new ByteArrayInputStream(bytes));
            org.apache.poi.hwpf.extractor.WordExtractor extractor =
                    new org.apache.poi.hwpf.extractor.WordExtractor(hdoc);
            String text = extractor.getText();
            if (text == null || text.isBlank())
            {
                return FileParseResult.fail("文档内容为空");
            }
            log.info("POI 解析 doc 成功: {}", fileName);
            return FileParseResult.ok(truncate(text.trim()), "doc");
        }
        catch (NoClassDefFoundError e)
        {
            log.warn("poi-scratchpad 不可用，无法解析 .doc: {}", fileName);
            return FileParseResult.fail("旧版Word格式(.doc)需安装额外组件");
        }
        catch (Exception e)
        {
            log.error("POI 解析 doc 失败: {}", fileName, e);
            return FileParseResult.fail("Word文档解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析 .xlsx (Excel 2007+)
     */
    private FileParseResult parseXlsx(byte[] bytes, String fileName)
    {
        StringBuilder sb = new StringBuilder();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes)))
        {
            for (int i = 0; i < wb.getNumberOfSheets(); i++)
            {
                Sheet sheet = wb.getSheetAt(i);
                sb.append("=== ").append(sheet.getSheetName()).append(" ===\n");
                for (Row row : sheet)
                {
                    for (int c = 0; c < row.getLastCellNum(); c++)
                    {
                        Cell cell = row.getCell(c);
                        if (cell != null)
                        {
                            cell.setCellType(CellType.STRING);
                            sb.append(cell.getStringCellValue()).append("\t");
                        }
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
            String text = sb.toString().trim();
            if (text.isEmpty())
            {
                return FileParseResult.fail("Excel内容为空");
            }
            log.info("POI 解析 xlsx 成功: {}", fileName);
            return FileParseResult.ok(truncate(text), "xlsx");
        }
        catch (Exception e)
        {
            log.error("POI 解析 xlsx 失败: {}", fileName, e);
            return FileParseResult.fail("Excel解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析 .xls (Excel 97-2003)
     */
    private FileParseResult parseXls(byte[] bytes, String fileName)
    {
        StringBuilder sb = new StringBuilder();
        try (HSSFWorkbook wb = new HSSFWorkbook(new ByteArrayInputStream(bytes)))
        {
            for (int i = 0; i < wb.getNumberOfSheets(); i++)
            {
                Sheet sheet = wb.getSheetAt(i);
                sb.append("=== ").append(sheet.getSheetName()).append(" ===\n");
                for (Row row : sheet)
                {
                    for (int c = 0; c < row.getLastCellNum(); c++)
                    {
                        Cell cell = row.getCell(c);
                        if (cell != null)
                        {
                            cell.setCellType(CellType.STRING);
                            sb.append(cell.getStringCellValue()).append("\t");
                        }
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
            String text = sb.toString().trim();
            if (text.isEmpty())
            {
                return FileParseResult.fail("Excel内容为空");
            }
            log.info("POI 解析 xls 成功: {}", fileName);
            return FileParseResult.ok(truncate(text), "xls");
        }
        catch (Exception e)
        {
            log.error("POI 解析 xls 失败: {}", fileName, e);
            return FileParseResult.fail("Excel解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析 PDF
     */
    private FileParseResult parsePdf(byte[] bytes, String fileName)
    {
        try (PDDocument doc = Loader.loadPDF(bytes))
        {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            if (text == null || text.isBlank())
            {
                return FileParseResult.fail("PDF内容为空");
            }
            log.info("PDFBox 解析 pdf 成功: {}, pages={}", fileName, doc.getNumberOfPages());
            return FileParseResult.ok(truncate(text.trim()), "pdf");
        }
        catch (Exception e)
        {
            log.error("PDFBox 解析 pdf 失败: {}", fileName, e);
            return FileParseResult.fail("PDF解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析纯文本
     */
    private FileParseResult parsePlainText(byte[] bytes, String fileName, String fileType)
    {
        try
        {
            String text = new String(bytes, StandardCharsets.UTF_8);
            return FileParseResult.ok(truncate(text), fileType);
        }
        catch (Exception e)
        {
            try
            {
                String text = new String(bytes, "GBK");
                return FileParseResult.ok(truncate(text), fileType);
            }
            catch (Exception ex)
            {
                return FileParseResult.fail("文本编码识别失败");
            }
        }
    }

    /**
     * Tika 兜底解析（用于未知格式）
     */
    private FileParseResult parseWithTika(byte[] bytes, String fileName, String fileType)
    {
        try
        {
            org.apache.tika.parser.AutoDetectParser tikaParser =
                    new org.apache.tika.parser.AutoDetectParser();
            org.apache.tika.sax.BodyContentHandler handler =
                    new org.apache.tika.sax.BodyContentHandler(MAX_CHARS);
            org.apache.tika.metadata.Metadata metadata = new org.apache.tika.metadata.Metadata();
            metadata.set("resourceName", fileName);
            org.apache.tika.parser.ParseContext context = new org.apache.tika.parser.ParseContext();

            tikaParser.parse(new ByteArrayInputStream(bytes), handler, metadata, context);
            String text = handler.toString();
            if (text == null || text.isBlank())
            {
                return FileParseResult.fail("不支持的文件格式");
            }
            log.info("Tika 兜底解析成功: {}", fileName);
            return FileParseResult.ok(truncate(text.trim()), fileType);
        }
        catch (Exception e)
        {
            log.warn("Tika 兜底解析失败: {}", fileName);
            return FileParseResult.fail("不支持的文件格式");
        }
    }

    private String truncate(String text)
    {
        if (text != null && text.length() > MAX_CHARS)
        {
            return text.substring(0, MAX_CHARS) + "\n\n[文本过长，已截断...]";
        }
        return text;
    }
}
