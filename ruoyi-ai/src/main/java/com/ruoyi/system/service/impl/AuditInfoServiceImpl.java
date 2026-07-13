package com.ruoyi.system.service.impl;

import com.ruoyi.system.mapper.AuditInfoMapper;
import com.ruoyi.system.service.IAuditInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditInfoServiceImpl implements IAuditInfoService
{
    @Autowired private AuditInfoMapper mapper;

    public List<Map<String, Object>> selectPlanList(String type, Integer year) {
        var p = new com.ruoyi.system.domain.AuditPlan();
        p.setPlanType(type); p.setPlanYear(year);
        return (List) mapper.selectPlanList(p);
    }
    public int insertPlan(Map<String,Object> p) {
        var e = new com.ruoyi.system.domain.AuditPlan();
        e.setPlanType((String)p.get("planType"));
        // planYear 可能是 String 或 Number，统一转换
        Object yearVal = p.get("planYear");
        if (yearVal != null) {
            e.setPlanYear(Integer.parseInt(String.valueOf(yearVal)));
        }
        e.setBatch((String)p.get("batch")); e.setPlanName((String)p.get("planName"));
        e.setFileUrl((String)p.get("fileUrl"));
        return mapper.insertPlan(e);
    }
    public int deletePlanByIds(Long[] ids) { return mapper.deletePlanByIds(ids); }

    public List<Map<String, Object>> selectUnitList(String type) {
        var u = new com.ruoyi.system.domain.AuditUnit(); u.setUnitType(type);
        return (List) mapper.selectUnitList(u);
    }
    public int insertUnit(Map<String,Object> u) {
        var e = new com.ruoyi.system.domain.AuditUnit();
        e.setUnitName((String)u.get("unitName")); e.setUnitType((String)u.get("unitType"));
        e.setProfile((String)u.get("profile")); e.setHistoryAudit((String)u.get("historyAudit"));
        e.setUnitCode((String)u.get("unitCode")); e.setParentLeader((String)u.get("parentLeader"));
        Object sc = u.get("staffCount"); if(sc!=null) e.setStaffCount(Integer.parseInt(String.valueOf(sc)));
        e.setFinanceContact((String)u.get("financeContact")); e.setContactPhone((String)u.get("contactPhone"));
        e.setAddress((String)u.get("address"));
        return mapper.insertUnit(e);
    }
    public int deleteUnitByIds(Long[] ids) { return mapper.deleteUnitByIds(ids); }

    public List<Map<String, Object>> selectLeaderList(Long unitId) {
        var l = new com.ruoyi.system.domain.AuditLeader();
        if (unitId != null) l.setUnitId(unitId);
        return (List) mapper.selectLeaderList(l);
    }
    public List<Map<String, Object>> selectProjectProgress() { return mapper.selectProjectProgress(); }

    // 闭环⓪：单位完整档案（领导+历史审计）
    public Map<String, Object> selectUnitProfile(Long unitId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("leaders", mapper.selectUnitLeaders(unitId));
        result.put("projects", mapper.selectUnitProjects(unitId));
        return result;
    }

    // 闭环⓪：推荐应审（单位+领导）
    public Map<String, Object> recommendAuditTargets() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("units", mapper.selectRecommendUnits());
        result.put("leaders", mapper.selectRecommendLeaders());
        return result;
    }

    public int bindPlanProject(Long planId, Long projectId) { return mapper.bindPlanProject(planId, projectId); }
    public int unbindPlanProject(Long planId, Long projectId) { return mapper.unbindPlanProject(planId, projectId); }
    public List<Map<String, Object>> selectPlanProjects(Long planId) { return mapper.selectPlanProjects(planId); }
    public List<Map<String, Object>> selectSchemeByPlan(Long planId) { return mapper.selectSchemeByPlan(planId); }

    // 闭环① 增强
    public int updatePlan(Map<String,Object> p) {
        var e = new com.ruoyi.system.domain.AuditPlan();
        e.setId(Long.parseLong(String.valueOf(p.get("id"))));
        e.setPlanType((String)p.get("planType"));
        Object yearVal = p.get("planYear");
        if (yearVal != null) e.setPlanYear(Integer.parseInt(String.valueOf(yearVal)));
        e.setBatch((String)p.get("batch")); e.setPlanName((String)p.get("planName"));
        e.setDescription((String)p.get("description"));
        Object status = p.get("status");
        if (status != null) e.setStatus(Integer.parseInt(String.valueOf(status)));
        return mapper.updatePlan(e);
    }
    public int updateUnit(Map<String,Object> u) {
        var e = new com.ruoyi.system.domain.AuditUnit();
        e.setId(Long.parseLong(String.valueOf(u.get("id"))));
        e.setUnitName((String)u.get("unitName")); e.setUnitType((String)u.get("unitType"));
        e.setProfile((String)u.get("profile")); e.setHistoryAudit((String)u.get("historyAudit"));
        e.setUnitCode((String)u.get("unitCode")); e.setParentLeader((String)u.get("parentLeader"));
        Object sc = u.get("staffCount"); if(sc!=null) e.setStaffCount(Integer.parseInt(String.valueOf(sc)));
        e.setFinanceContact((String)u.get("financeContact")); e.setContactPhone((String)u.get("contactPhone"));
        e.setAddress((String)u.get("address"));
        return mapper.updateUnit(e);
    }
    public int insertLeader(Map<String,Object> l) {
        var e = new com.ruoyi.system.domain.AuditLeader();
        e.setName((String)l.get("name"));
        Object uid = l.get("unitId"); if(uid!=null) e.setUnitId(Long.parseLong(String.valueOf(uid)));
        e.setPosition((String)l.get("position")); e.setGender((String)l.get("gender"));
        e.setIdNumber((String)l.get("idNumber")); e.setManagedScope((String)l.get("managedScope"));
        return mapper.insertLeader(e);
    }
    public int updateLeader(Map<String,Object> l) {
        var e = new com.ruoyi.system.domain.AuditLeader();
        e.setId(Long.parseLong(String.valueOf(l.get("id"))));
        e.setName((String)l.get("name")); e.setPosition((String)l.get("position"));
        e.setGender((String)l.get("gender")); e.setIdNumber((String)l.get("idNumber"));
        e.setManagedScope((String)l.get("managedScope"));
        return mapper.updateLeader(e);
    }
    public int deleteLeaderByIds(Long[] ids) { return mapper.deleteLeaderByIds(ids); }
}
