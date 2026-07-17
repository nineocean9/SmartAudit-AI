package com.ruoyi.system.service;

import com.ruoyi.system.domain.ForensicDraft;
import com.ruoyi.system.service.IAuditRagService;
import java.util.List;

public interface IAiForensicService
{
    List<ForensicDraft> selectForensicDraftList(ForensicDraft draft);
    ForensicDraft selectForensicDraftById(Long id);
    ForensicDraft generateDraft(String issue, String basisIds);
    ForensicDraft generateDraft(String issue, String basisIds, Long projectId);
    int updateForensicDraft(ForensicDraft draft);
    int deleteForensicDraftByIds(Long[] ids);
}
