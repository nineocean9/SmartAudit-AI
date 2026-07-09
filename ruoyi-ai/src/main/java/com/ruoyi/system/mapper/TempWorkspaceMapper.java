package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.TempWorkspace;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 临时工作区 Mapper
 *
 * @author ruoyi
 */
public interface TempWorkspaceMapper
{
    /** 插入临时文件记录 */
    int insert(TempWorkspace record);

    /** 按sessionId查询 */
    List<TempWorkspace> selectBySessionId(@Param("sessionId") String sessionId);

    /** 按ID查询 */
    TempWorkspace selectById(@Param("id") Long id);

    /** 销毁session(逻辑删除) */
    int destroyBySessionId(@Param("sessionId") String sessionId);

    /** 清理过期记录 */
    int deleteExpired();
}
