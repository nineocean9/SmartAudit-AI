package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.ProjectDocument;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 项目文档 Mapper
 *
 * @author ruoyi
 */
public interface ProjectDocumentMapper
{
    /** 插入文档 */
    int insertDocument(ProjectDocument doc);

    /** 根据ID查询 */
    ProjectDocument selectById(@Param("id") Long id);

    /** 按项目ID查询文档列表 */
    List<ProjectDocument> selectByProjectId(@Param("projectId") Long projectId);

    /** 按项目+类型查询 */
    List<ProjectDocument> selectByProjectAndType(@Param("projectId") Long projectId,
                                                  @Param("docType") String docType);

    /** 按计划ID查询 */
    List<ProjectDocument> selectByPlanId(@Param("planId") Long planId);

    /** 更新文档切块数量 */
    int updateChunkCount(@Param("id") Long id, @Param("chunkCount") int chunkCount);

    /** 删除文档（逻辑删除） */
    int deleteById(@Param("id") Long id);
}
