package com.ruoyi.system.util;

/**
 * 文件类型检测工具
 *
 * @author ruoyi
 */
public class FileTypeDetector
{
    /**
     * 根据文件名判断文件类型
     */
    public static String detect(String fileName)
    {
        if (fileName == null) return "unknown";

        String lower = fileName.toLowerCase();
        if (lower.endsWith(".txt"))  return "txt";
        if (lower.endsWith(".md"))   return "md";
        if (lower.endsWith(".docx")) return "docx";
        if (lower.endsWith(".doc"))  return "doc";
        if (lower.endsWith(".pdf"))  return "pdf";
        if (lower.endsWith(".xlsx")) return "xlsx";
        if (lower.endsWith(".xls"))  return "xls";
        if (lower.endsWith(".csv"))  return "csv";
        if (lower.endsWith(".png") || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg") || lower.endsWith(".gif")
                || lower.endsWith(".bmp"))
        {
            return "image";
        }
        return "unknown";
    }

    /**
     * 是否支持文本解析
     */
    public static boolean isParseable(String fileName)
    {
        String type = detect(fileName);
        return !"image".equals(type) && !"unknown".equals(type);
    }

    /**
     * 是否为图片
     */
    public static boolean isImage(String fileName)
    {
        return "image".equals(detect(fileName));
    }
}
