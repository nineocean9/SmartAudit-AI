package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditBasisRelation;
import java.util.List;

public interface AuditBasisRelationMapper
{
    List<AuditBasisRelation> selectByBasisId(Long basisId);
    int insert(AuditBasisRelation r);
    int deleteById(Long id);
}
