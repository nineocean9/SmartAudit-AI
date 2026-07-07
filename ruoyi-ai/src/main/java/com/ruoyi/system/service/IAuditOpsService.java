package com.ruoyi.system.service;

import com.ruoyi.system.domain.AuditWorkpaper;
import com.ruoyi.system.domain.AuditReport;
import java.util.List;
import java.util.Map;

public interface IAuditOpsService
{
    List<Map<String,Object>> selectSchemeList(Long projectId);
    int insertScheme(Map<String,Object> s);
    List<AuditWorkpaper> selectWorkpaperList(AuditWorkpaper w);
    AuditWorkpaper selectWorkpaperById(Long id);
    int insertWorkpaper(AuditWorkpaper w);
    int updateWorkpaper(AuditWorkpaper w);
    List<Map<String,Object>> selectReviewList(Long workpaperId);
    int insertReview(Map<String,Object> r);
    List<AuditReport> selectReportList(Long projectId);
    AuditReport selectReportById(Long id);
    int insertReport(AuditReport r);
    int updateReport(AuditReport r);
    List<Map<String,Object>> selectCollabLog(Long projectId);
}
