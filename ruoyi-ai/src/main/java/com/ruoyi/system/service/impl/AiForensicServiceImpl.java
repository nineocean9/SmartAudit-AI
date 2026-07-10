package com.ruoyi.system.service.impl;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.ForensicDraft;
import com.ruoyi.system.mapper.ForensicDraftMapper;
import com.ruoyi.system.service.IAiForensicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AiForensicServiceImpl implements IAiForensicService
{
    @Autowired
    private ForensicDraftMapper forensicDraftMapper;

    @Override
    public List<ForensicDraft> selectForensicDraftList(ForensicDraft draft)
    {
        return forensicDraftMapper.selectForensicDraftList(draft);
    }

    @Override
    public ForensicDraft selectForensicDraftById(Long id)
    {
        return forensicDraftMapper.selectForensicDraftById(id);
    }

    @Override
    public ForensicDraft generateDraft(String issue, String basisIds)
    {
        ForensicDraft draft = new ForensicDraft();
        draft.setIssue(issue);
        draft.setBasisIds(basisIds);
        try {
            draft.setCreateBy(SecurityUtils.getUsername());
        } catch (Exception e) {
            // 异步线程中 SecurityContext 不可用，使用默认值
            draft.setCreateBy("system");
        }
        forensicDraftMapper.insertForensicDraft(draft);
        return draft;
    }

    @Override
    public int deleteForensicDraftByIds(Long[] ids)
    {
        return forensicDraftMapper.deleteForensicDraftByIds(ids);
    }
}
