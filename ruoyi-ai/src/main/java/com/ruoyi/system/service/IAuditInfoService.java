package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;

public interface IAuditInfoService
{
    List<Map<String, Object>> selectPlanList(String type, Integer year);
    int insertPlan(Map<String,Object> p); int deletePlanByIds(Long[] ids);
    List<Map<String, Object>> selectUnitList(String type);
    int insertUnit(Map<String,Object> u); int deleteUnitByIds(Long[] ids);
    List<Map<String, Object>> selectLeaderList(Long unitId);
    List<Map<String, Object>> selectProjectProgress();

    // 闭环⓪
    Map<String, Object> selectUnitProfile(Long unitId);
    Map<String, Object> recommendAuditTargets();
    int bindPlanProject(Long planId, Long projectId);
    int unbindPlanProject(Long planId, Long projectId);
    List<Map<String, Object>> selectPlanProjects(Long planId);
    List<Map<String, Object>> selectSchemeByPlan(Long planId);

    // 闭环① 增强
    int updatePlan(Map<String,Object> p);
    int updateUnit(Map<String,Object> u);
    int insertLeader(Map<String,Object> l);
    int updateLeader(Map<String,Object> l);
    int deleteLeaderByIds(Long[] ids);
}
