package com.ruoyi.system.service.impl;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.AuditBasis;
import com.ruoyi.system.mapper.AuditBasisMapper;
import com.ruoyi.system.service.IAuditBasisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 审计依据库 服务层实现
 *
 * @author ruoyi
 */
@Service
public class AuditBasisServiceImpl implements IAuditBasisService
{
    @Autowired
    private AuditBasisMapper auditBasisMapper;

    @Override
    public List<AuditBasis> selectAuditBasisList(AuditBasis basis)
    {
        return auditBasisMapper.selectAuditBasisList(basis);
    }

    @Override
    public AuditBasis selectAuditBasisById(Long id)
    {
        return auditBasisMapper.selectAuditBasisById(id);
    }

    @Override
    public int insertAuditBasis(AuditBasis basis)
    {
        basis.setCreateBy(SecurityUtils.getUsername());
        return auditBasisMapper.insertAuditBasis(basis);
    }

    @Override
    public int updateAuditBasis(AuditBasis basis)
    {
        basis.setUpdateBy(SecurityUtils.getUsername());
        // 修改时版本号+1
        basis.setVersion(basis.getVersion() != null ? basis.getVersion() + 1 : 2);
        return auditBasisMapper.updateAuditBasis(basis);
    }

    @Override
    public int deleteAuditBasisByIds(Long[] ids)
    {
        return auditBasisMapper.deleteAuditBasisByIds(ids);
    }

    @Override
    public int updateAuditBasisStatus(Long id, Integer status)
    {
        return auditBasisMapper.updateAuditBasisStatus(id, status);
    }

    @Override
    public List<AuditBasis> searchByKeyword(String keyword, String category)
    {
        return auditBasisMapper.searchByKeyword(keyword, category);
    }
}
