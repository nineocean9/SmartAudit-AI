package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditSchemeTemplate;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface AuditSchemeTemplateMapper
{
    List<AuditSchemeTemplate> selectList(AuditSchemeTemplate q);
    AuditSchemeTemplate selectById(Long id);
    List<AuditSchemeTemplate> selectByAuditType(String auditType);
    int insert(AuditSchemeTemplate t);
    int update(AuditSchemeTemplate t);
    int updateContent(@Param("id") Long id, @Param("content") String content);
    int deleteByIds(Long[] ids);
}
