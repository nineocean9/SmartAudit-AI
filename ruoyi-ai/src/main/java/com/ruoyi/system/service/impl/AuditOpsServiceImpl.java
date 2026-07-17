package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.AuditWorkpaper;
import com.ruoyi.system.domain.AuditReport;
import com.ruoyi.system.mapper.AuditOpsMapper;
import com.ruoyi.system.service.IAuditOpsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class AuditOpsServiceImpl implements IAuditOpsService
{
    @Autowired private AuditOpsMapper mapper;

    public List<Map<String,Object>> selectSchemeList(Long pId) { return mapper.selectSchemeList(pId); }
    public Map<String,Object> selectSchemeById(Long id) { return mapper.selectSchemeById(id); }
    public int insertScheme(Map<String,Object> s) { return mapper.insertScheme(s); }
    public int updateSchemeContent(Map<String,Object> s) { return mapper.updateSchemeContent(s); }
    public List<AuditWorkpaper> selectWorkpaperList(AuditWorkpaper w) { return mapper.selectWorkpaperList(w); }
    public AuditWorkpaper selectWorkpaperById(Long id) { return mapper.selectWorkpaperById(id); }
    public int insertWorkpaper(AuditWorkpaper w) { return mapper.insertWorkpaper(w); }
    public int updateWorkpaper(AuditWorkpaper w) { return mapper.updateWorkpaper(w); }
    public List<Map<String,Object>> selectReviewList(Long wId) { return mapper.selectReviewList(wId); }
    public int insertReview(Map<String,Object> r) { return mapper.insertReview(r); }
    public List<AuditReport> selectReportList(Long pId) { return mapper.selectReportList(pId); }
    public AuditReport selectReportById(Long id) { return mapper.selectReportById(id); }
    public int insertReport(AuditReport r) { return mapper.insertReport(r); }
    public int updateReport(AuditReport r) { return mapper.updateReport(r); }
    public int updateReportContent(AuditReport r) { return mapper.updateReportContent(r); }
    public List<Map<String,Object>> selectCollabLog(Long pId) { return mapper.selectCollabLog(pId); }
}
