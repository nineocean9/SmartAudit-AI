package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditTempAuth;
import java.util.List;

public interface AuditTempAuthMapper
{
    List<AuditTempAuth> selectList(AuditTempAuth q);
    List<AuditTempAuth> selectByUserId(Long userId);
    int insert(AuditTempAuth a);
    int update(AuditTempAuth a);
    int revokeExpired();
    int deleteById(Long id);
}
