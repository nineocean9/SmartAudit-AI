package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.DocCheckTask;
import java.util.List;

public interface DocCheckTaskMapper
{
    List<DocCheckTask> selectDocCheckTaskList(DocCheckTask task);
    DocCheckTask selectDocCheckTaskById(Long id);
    int insertDocCheckTask(DocCheckTask task);
    int updateDocCheckTask(DocCheckTask task);
    int deleteDocCheckTaskByIds(Long[] ids);
}
