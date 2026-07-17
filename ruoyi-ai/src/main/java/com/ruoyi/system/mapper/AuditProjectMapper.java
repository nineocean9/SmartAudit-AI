package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditProject;
import com.ruoyi.system.domain.AuditRectification;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 审计项目数据查询 Mapper（演示数据）
 */
public interface AuditProjectMapper
{
    /** 按关键词搜索项目（匹配项目名称、被审计单位） */
    List<AuditProject> searchProjects(@Param("keyword") String keyword);

    /** 查询指定项目的所有问题（含整改情况） */
    List<ProjectIssueWithRect> searchProjectIssues(@Param("projectId") Long projectId);

    /** 问题+整改 JOIN 结果 */
    class ProjectIssueWithRect
    {
        public Long issueId;
        public String issueDesc;
        public Integer severity;
        public String rectMeasure;
        public Integer rectStatus;
        public String finishDate;
    }
}
