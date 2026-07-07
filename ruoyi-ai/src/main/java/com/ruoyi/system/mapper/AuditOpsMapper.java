package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditWorkpaper;
import com.ruoyi.system.domain.AuditReport;
import java.util.List;
import java.util.Map;

public interface AuditOpsMapper
{
    // 方案
    List<Map<String,Object>> selectSchemeList(Long projectId);
    int insertScheme(Map<String,Object> s);

    // 底稿
    List<AuditWorkpaper> selectWorkpaperList(AuditWorkpaper w);
    AuditWorkpaper selectWorkpaperById(Long id);
    int insertWorkpaper(AuditWorkpaper w);
    int updateWorkpaper(AuditWorkpaper w);

    // 复核
    List<Map<String,Object>> selectReviewList(Long workpaperId);
    int insertReview(Map<String,Object> r);

    // 报告
    List<AuditReport> selectReportList(Long projectId);
    AuditReport selectReportById(Long id);
    int insertReport(AuditReport r);
    int updateReport(AuditReport r);

    // 协同日志
    List<Map<String,Object>> selectCollabLog(Long projectId);
}
