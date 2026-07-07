package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.ForensicDraft;
import java.util.List;

public interface ForensicDraftMapper
{
    List<ForensicDraft> selectForensicDraftList(ForensicDraft draft);
    ForensicDraft selectForensicDraftById(Long id);
    int insertForensicDraft(ForensicDraft draft);
    int updateForensicDraft(ForensicDraft draft);
    int deleteForensicDraftByIds(Long[] ids);
}
