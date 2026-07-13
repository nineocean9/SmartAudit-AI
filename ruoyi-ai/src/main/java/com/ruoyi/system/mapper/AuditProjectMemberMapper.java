package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditProjectMember;
import java.util.List;
import java.util.Map;

public interface AuditProjectMemberMapper
{
    List<AuditProjectMember> selectByProjectId(Long projectId);
    int insert(AuditProjectMember m);
    int update(AuditProjectMember m);
    int deleteById(Long id);
    List<Map<String, Object>> selectUserWorkload();
}
