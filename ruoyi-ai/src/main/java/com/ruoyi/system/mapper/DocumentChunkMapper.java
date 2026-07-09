package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.DocumentChunk;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 文档切块 Mapper
 *
 * @author ruoyi
 */
public interface DocumentChunkMapper
{
    /** 批量插入切块 */
    int insertChunk(DocumentChunk chunk);

    /** 按文档ID查询所有切块 */
    List<DocumentChunk> selectByDocumentId(@Param("documentId") Long documentId);

    /** 批量删除文档的所有切块 */
    int deleteByDocumentId(@Param("documentId") Long documentId);

    /** 按项目ID查询所有切块内容（用于检索） */
    List<String> selectContentByProjectId(@Param("projectId") Long projectId);

    /** 删除指定ID的切块 */
    int deleteById(@Param("id") Long id);
}
