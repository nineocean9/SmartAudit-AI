package com.ruoyi.system.service;

import com.ruoyi.system.domain.DocCheckTask;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface IAiDocCheckService
{
    List<DocCheckTask> selectDocCheckTaskList(DocCheckTask task);
    DocCheckTask checkDocument(MultipartFile file, Long userId);
    DocCheckTask selectDocCheckTaskById(Long id);
}
