package com.ruoyi.system.service;

import java.io.File;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件解析服务接口
 * 将各类文档（Word/PDF/Excel/CSV/TXT）解析为纯文本
 *
 * @author ruoyi
 */
public interface IFileParseService
{
    /**
     * 解析结果
     */
    class FileParseResult
    {
        /** 解析后的纯文本 */
        public String plainText;
        /** 文件类型标签 */
        public String fileType;
        /** 字符数 */
        public int charCount;
        /** 是否成功 */
        public boolean success;
        /** 错误信息 */
        public String errorMsg;

        public static FileParseResult ok(String text, String fileType)
        {
            FileParseResult r = new FileParseResult();
            r.plainText = text;
            r.fileType = fileType;
            r.charCount = text != null ? text.length() : 0;
            r.success = true;
            return r;
        }

        public static FileParseResult fail(String errorMsg)
        {
            FileParseResult r = new FileParseResult();
            r.success = false;
            r.errorMsg = errorMsg;
            return r;
        }
    }

    /**
     * 解析 MultipartFile
     */
    FileParseResult parse(MultipartFile file);

    /**
     * 解析本地文件
     */
    FileParseResult parse(File file);

    /**
     * 解析并返回文件名
     */
    FileParseResult parse(MultipartFile file, String originalFileName);
}
