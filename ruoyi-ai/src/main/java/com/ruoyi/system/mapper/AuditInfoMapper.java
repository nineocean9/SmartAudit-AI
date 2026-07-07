package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AuditPlan;
import com.ruoyi.system.domain.AuditUnit;
import com.ruoyi.system.domain.AuditLeader;
import java.util.List;
import java.util.Map;

public interface AuditInfoMapper
{
    // 计划
    List<AuditPlan> selectPlanList(AuditPlan p);
    int insertPlan(AuditPlan p); int updatePlan(AuditPlan p); int deletePlanByIds(Long[] ids);
    // 单位
    List<AuditUnit> selectUnitList(AuditUnit u);
    int insertUnit(AuditUnit u); int updateUnit(AuditUnit u); int deleteUnitByIds(Long[] ids);
    // 领导
    List<AuditLeader> selectLeaderList(AuditLeader l);
    int insertLeader(AuditLeader l); int updateLeader(AuditLeader l); int deleteLeaderByIds(Long[] ids);
    // 进度（各项目状态聚合）
    List<Map<String, Object>> selectProjectProgress();

    // 闭环⓪：推荐应审 + 单位档案 + 计划项目绑定
    List<Map<String, Object>> selectRecommendUnits();
    List<Map<String, Object>> selectRecommendLeaders();
    List<Map<String, Object>> selectUnitLeaders(Long unitId);
    List<Map<String, Object>> selectUnitProjects(Long unitId);
    int bindPlanProject(Long planId, Long projectId);
    int unbindPlanProject(Long planId, Long projectId);
    List<Map<String, Object>> selectPlanProjects(Long planId);
    List<Map<String, Object>> selectSchemeByPlan(Long planId);
}
