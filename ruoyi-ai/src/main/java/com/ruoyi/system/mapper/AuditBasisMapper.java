package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditBasis;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 审计依据库 数据访问层
 * 对应 mapper/ai/AuditBasisMapper.xml
 *
 * @author ruoyi
 */
public interface AuditBasisMapper
{
    /**
     * 查询依据列表（分页+条件筛选）
     */
    List<AuditBasis> selectAuditBasisList(AuditBasis basis);

    /**
     * 根据ID查询依据详情
     */
    AuditBasis selectAuditBasisById(Long id);

    /**
     * 新增依据
     */
    int insertAuditBasis(AuditBasis basis);

    /**
     * 修改依据
     */
    int updateAuditBasis(AuditBasis basis);

    /**
     * 批量删除依据
     */
    int deleteAuditBasisByIds(Long[] ids);

    /**
     * 更新依据生效状态
     */
    int updateAuditBasisStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 关键词全文检索（标题+正文模糊匹配）
     */
    List<AuditBasis> searchByKeyword(@Param("keyword") String keyword, @Param("category") String category);
}
