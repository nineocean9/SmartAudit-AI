package com.ruoyi.system.service.impl;

import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.system.domain.DocCheckTask;
import com.ruoyi.system.mapper.DocCheckTaskMapper;
import com.ruoyi.system.service.IAiDocCheckService;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class AiDocCheckServiceImpl implements IAiDocCheckService
{
    @Autowired
    private DocCheckTaskMapper docCheckTaskMapper;

    private final Tika tika = new Tika();

    @Override
    public List<DocCheckTask> selectDocCheckTaskList(DocCheckTask task)
    {
        return docCheckTaskMapper.selectDocCheckTaskList(task);
    }

    @Override
    public DocCheckTask selectDocCheckTaskById(Long id)
    {
        return docCheckTaskMapper.selectDocCheckTaskById(id);
    }

    @Override
    public DocCheckTask checkDocument(MultipartFile file, Long userId)
    {
        DocCheckTask task = new DocCheckTask();
        task.setFileName(file.getOriginalFilename());
        task.setUploader(userId);
        task.setStatus(1); // 处理中

        try
        {
            // 使用 Tika 提取文本
            String text;
            try (InputStream is = file.getInputStream())
            {
                text = tika.parseToString(is);
            }

            task.setIssuesJson("{\"text_length\":" + text.length() + ",\"message\":\"文档解析完成，待AI校验\"}");
            task.setStatus(2); // 完成
        }
        catch (Exception e)
        {
            task.setIssuesJson("{\"error\":\"" + e.getMessage() + "\"}");
            task.setStatus(3); // 失败
        }

        docCheckTaskMapper.insertDocCheckTask(task);
        return task;
    }
}
