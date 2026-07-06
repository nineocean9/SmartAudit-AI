package com.ruoyi.system.service;

import com.ruoyi.system.domain.AuditBasis;
import java.util.List;

/**
 * 审计依据库 服务层接口
 *
 * @author ruoyi
 */
public interface IAuditBasisService
{
    /**
     * 查询依据列表
     */
    List<AuditBasis> selectAuditBasisList(AuditBasis basis);

    /**
     * 根据ID查询依据
     */
    AuditBasis selectAuditBasisById(Long id);

    /**
     * 新增依据
     */
    int insertAuditBasis(AuditBasis basis);

    /**
     * 修改依据（版本号+1）
     */
    int updateAuditBasis(AuditBasis basis);

    /**
     * 批量删除依据
     */
    int deleteAuditBasisByIds(Long[] ids);

    /**
     * 切换依据生效/失效状态
     */
    int updateAuditBasisStatus(Long id, Integer status);

    /**
     * 关键词全文检索
     */
    List<AuditBasis> searchByKeyword(String keyword, String category);
}
