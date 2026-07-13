package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 审计依据关联关系实体
 * 对应数据库表 audit_basis_relation
 *
 * @author ruoyi
 */
public class AuditBasisRelation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long basisId;
    private Long relatedId;
    private String relationType;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getBasisId() { return basisId; }
    public void setBasisId(Long v) { this.basisId = v; }
    public Long getRelatedId() { return relatedId; }
    public void setRelatedId(Long v) { this.relatedId = v; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String v) { this.relationType = v; }
}
