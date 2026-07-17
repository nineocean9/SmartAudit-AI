package com.ruoyi.system.service.impl;

import com.ruoyi.system.mapper.AuditInfoMapper;
import com.ruoyi.system.service.IAuditInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditInfoServiceImpl implements IAuditInfoService
{
    @Autowired private AuditInfoMapper mapper;
    @Autowired private JdbcTemplate jdbcTemplate;

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
        e.setDeptId(ensureAuditDept(e));
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
        e.setDeptId(ensureAuditDept(e));
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

    private Long ensureAuditDept(com.ruoyi.system.domain.AuditUnit unit)
    {
        String unitName = unit.getUnitName() == null ? null : unit.getUnitName().trim();
        if (unitName == null || unitName.isEmpty())
        {
            return null;
        }

        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT dept_id, del_flag FROM sys_dept WHERE trim(dept_name) = trim(?) ORDER BY CASE WHEN del_flag = '0' THEN 0 ELSE 1 END, dept_id LIMIT 1",
                unitName);
        if (!existing.isEmpty())
        {
            Long deptId = ((Number) existing.get(0).get("dept_id")).longValue();
            jdbcTemplate.update(
                    "UPDATE sys_dept SET del_flag='0', status='0', unit_type=COALESCE(NULLIF(?, ''), unit_type, '被审计单位'), "
                            + "profile=?, history_audit=?, is_audit_target=1, leader=COALESCE(NULLIF(?, ''), leader), "
                            + "phone=COALESCE(NULLIF(?, ''), phone), update_by='admin', update_time=now() WHERE dept_id=?",
                    unit.getUnitType(), unit.getProfile(), unit.getHistoryAudit(),
                    unit.getParentLeader(), unit.getContactPhone(), deptId);
            return deptId;
        }

        Map<String, Object> parent = findDefaultAuditParent();
        Long parentId = ((Number) parent.get("dept_id")).longValue();
        String parentAncestors = String.valueOf(parent.get("ancestors"));
        String ancestors = parentId == 0 ? "0" : parentAncestors + "," + parentId;

        Number deptId = jdbcTemplate.queryForObject(
                "INSERT INTO sys_dept(parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, unit_type, profile, history_audit, is_audit_target) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING dept_id",
                Number.class,
                parentId, ancestors, unitName, 60,
                emptyToDefault(unit.getParentLeader(), ""),
                emptyToDefault(unit.getContactPhone(), ""),
                "", "0", "0", "admin", new java.sql.Timestamp(System.currentTimeMillis()),
                "", null,
                emptyToDefault(unit.getUnitType(), "被审计单位"),
                unit.getProfile(), unit.getHistoryAudit(), 1);
        return deptId == null ? null : deptId.longValue();
    }

    private Map<String, Object> findDefaultAuditParent()
    {
        List<Map<String, Object>> parents = jdbcTemplate.queryForList(
                "SELECT dept_id, ancestors FROM sys_dept WHERE del_flag='0' AND dept_name IN ('示范高校','若依科技') "
                        + "ORDER BY CASE WHEN dept_name='示范高校' THEN 0 ELSE 1 END, dept_id LIMIT 1");
        if (!parents.isEmpty())
        {
            return parents.get(0);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("dept_id", 0L);
        root.put("ancestors", "0");
        return root;
    }

    private String emptyToDefault(String value, String defaultValue)
    {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
