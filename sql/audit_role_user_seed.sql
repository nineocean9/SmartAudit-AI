-- 审计平台业务角色与演示用户初始化
-- 来源：综合实践III---高校一体化智慧审计平台.docx、审计信息化平台业务功能要求.docx
-- 说明：
-- 1. 本脚本只写入 RuoYi 既有权限表：sys_dept / sys_role / sys_user / sys_user_role / sys_role_menu / sys_role_dept。
-- 2. 菜单权限基于当前项目已存在的审计菜单、AI 菜单和系统管理菜单。
-- 3. 细粒度“项目级、单位级、临时授权到期回收”需要业务表配合，本脚本先落地角色、用户、菜单和 RuoYi data_scope。
-- 4. 演示账号默认密码与原始 admin 一致，便于课堂演示；正式环境请首次登录后修改。
-- 执行前置：请先执行 PostgreSQL 基础库脚本 ry_20250522.sql，以及审计/AI 菜单与业务表脚本。

DO $$
BEGIN
  IF to_regclass('sys_dept') IS NULL
     OR to_regclass('sys_role') IS NULL
     OR to_regclass('sys_user') IS NULL
     OR to_regclass('sys_menu') IS NULL
     OR to_regclass('sys_user_role') IS NULL
     OR to_regclass('sys_role_menu') IS NULL
     OR to_regclass('sys_role_dept') IS NULL THEN
    RAISE EXCEPTION '缺少 RuoYi 基础权限表，请先执行 sql/ry_20250522.sql。注意：ry_20260320.sql 是 MySQL 版，不适用于 PostgreSQL。';
  END IF;
END $$;

ALTER TABLE sys_dept ALTER COLUMN email TYPE VARCHAR(50);

-- ===== 1. 组织机构：学校、审计处、被审计单位、中介机构 =====
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time)
SELECT 2100, 0, '0', '示范高校', 1, '校领导', '13800000000', 'school@example.edu.cn', '0', '0', 'admin', now(), '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_id = 2100);

INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time)
SELECT 2101, 2100, '0,2100', '审计处', 1, '审计处长', '13800000001', 'audit@example.edu.cn', '0', '0', 'admin', now(), '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_id = 2101);

INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time)
SELECT 2102, 2100, '0,2100', '信息工程学院', 2, '学院负责人', '13800000002', 'college@example.edu.cn', '0', '0', 'admin', now(), '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_id = 2102);

INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time)
SELECT 2103, 2100, '0,2100', '计划财务处', 3, '部门负责人', '13800000003', 'finance@example.edu.cn', '0', '0', 'admin', now(), '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_id = 2103);

INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time)
SELECT 2104, 0, '0', '外部中介审计机构', 99, '项目经理', '13800000004', 'agency@example.com', '0', '0', 'admin', now(), '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_id = 2104);

-- ===== 2. 业务角色 =====
-- 兼容早期脚本中的旧角色编码，避免同一业务角色在不同初始化脚本里出现两套名称。
UPDATE sys_role SET role_name = '项目组长/主审', role_key = 'audit_project_leader', role_sort = 30, data_scope = '5', update_by = 'admin', update_time = now()
WHERE role_id = 102 OR role_key = 'audit_leader';

UPDATE sys_role SET role_name = '普通审计人员', role_key = 'audit_staff', role_sort = 40, data_scope = '5', update_by = 'admin', update_time = now()
WHERE role_id = 103 OR role_key = 'auditor';

UPDATE sys_role SET role_name = '被审计单位负责人', role_key = 'audited_unit_principal', role_sort = 50, data_scope = '3', update_by = 'admin', update_time = now()
WHERE role_id = 104 OR role_key = 'unit_manager';

UPDATE sys_role SET role_name = '被审计单位联络员', role_key = 'audited_unit_liaison', role_sort = 60, data_scope = '3', update_by = 'admin', update_time = now()
WHERE role_id = 105 OR role_key = 'unit_contact';

UPDATE sys_role SET role_name = '中介审计人员', role_key = 'intermediary_auditor', role_sort = 70, data_scope = '5', update_by = 'admin', update_time = now()
WHERE role_id = 106 OR role_key = 'outsource';

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT 100, '校领导', 'school_leader', 10, '1', TRUE, TRUE, '0', '0', 'admin', now(), '', NULL, '全校审计总览、报告阅览、整改统计、档案借阅审批；不直接授予底层维护权限'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 100 OR role_key = 'school_leader');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT 101, '审计处长', 'audit_director', 20, '1', TRUE, TRUE, '0', '0', 'admin', now(), '', NULL, '审计处负责人，全系统审计功能、流程审批、中介考核、权限配置'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 101 OR role_key = 'audit_director');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT 102, '项目组长/主审', 'audit_project_leader', 30, '5', TRUE, TRUE, '0', '0', 'admin', now(), '', NULL, '负责本人项目维护、人员分工、底稿复核、报告编制、整改评价'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 102 OR role_key = 'audit_project_leader');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT 103, '普通审计人员', 'audit_staff', 40, '5', TRUE, TRUE, '0', '0', 'admin', now(), '', NULL, '查看分配项目资料、开展作业、数据分析、检索依据库'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 103 OR role_key = 'audit_staff');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT 104, '被审计单位负责人', 'audited_unit_principal', 50, '3', TRUE, TRUE, '0', '0', 'admin', now(), '', NULL, '仅查看本单位审计报告、整改台账和整改复核结果'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 104 OR role_key = 'audited_unit_principal');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT 105, '被审计单位联络员', 'audited_unit_liaison', 60, '3', TRUE, TRUE, '0', '0', 'admin', now(), '', NULL, '提交审计资料、回复取证单、填报整改材料，仅操作本单位业务数据'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 105 OR role_key = 'audited_unit_liaison');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT 106, '中介审计人员', 'intermediary_auditor', 70, '5', TRUE, TRUE, '0', '0', 'admin', now(), '', NULL, '项目服务周期内临时访问分配项目数据、资料和底稿，无全局统计权限'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 106 OR role_key = 'intermediary_auditor');

-- ===== 3. 演示用户 =====
UPDATE sys_user SET user_name = 'audit_project_leader', nick_name = '项目组长/主审', update_by = 'admin', update_time = now(), remark = '项目组长/主审演示账号'
WHERE user_id = 2003 OR user_name = 'audit_lead';

UPDATE sys_user SET user_name = 'audit_staff', nick_name = '普通审计人员', update_by = 'admin', update_time = now(), remark = '普通审计人员演示账号'
WHERE user_id = 2004 OR user_name = 'auditor';

UPDATE sys_user SET user_name = 'audited_unit_principal', nick_name = '被审计单位负责人', update_by = 'admin', update_time = now(), remark = '被审计单位负责人演示账号'
WHERE user_id = 2005 OR user_name = 'unit_principal';

UPDATE sys_user SET user_name = 'audited_unit_liaison', nick_name = '被审计单位联络员', update_by = 'admin', update_time = now(), remark = '被审计单位联络员演示账号'
WHERE user_id = 2006 OR user_name = 'unit_liaison';

UPDATE sys_user SET user_name = 'intermediary_auditor', nick_name = '中介审计人员', update_by = 'admin', update_time = now(), remark = '外部中介审计人员演示账号'
WHERE user_id = 2007 OR user_name = 'intermediary';

INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark)
SELECT 2001, 2100, 'school_leader', '校领导', '00', 'leader@example.edu.cn', '13810000001', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', now(), now(), 'admin', now(), '', NULL, '校领导演示账号'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_id = 2001 OR user_name = 'school_leader');

INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark)
SELECT 2002, 2101, 'audit_director', '审计处长', '00', 'director@example.edu.cn', '13810000002', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', now(), now(), 'admin', now(), '', NULL, '审计处长演示账号'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_id = 2002 OR user_name = 'audit_director');

INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark)
SELECT 2003, 2101, 'audit_project_leader', '项目组长/主审', '00', 'lead@example.edu.cn', '13810000003', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', now(), now(), 'admin', now(), '', NULL, '项目组长/主审演示账号'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_id = 2003 OR user_name = 'audit_project_leader');

INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark)
SELECT 2004, 2101, 'audit_staff', '普通审计人员', '00', 'auditor@example.edu.cn', '13810000004', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', now(), now(), 'admin', now(), '', NULL, '普通审计人员演示账号'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_id = 2004 OR user_name = 'audit_staff');

INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark)
SELECT 2005, 2102, 'audited_unit_principal', '被审计单位负责人', '00', 'principal@example.edu.cn', '13810000005', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', now(), now(), 'admin', now(), '', NULL, '被审计单位负责人演示账号'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_id = 2005 OR user_name = 'audited_unit_principal');

INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark)
SELECT 2006, 2102, 'audited_unit_liaison', '被审计单位联络员', '00', 'liaison@example.edu.cn', '13810000006', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', now(), now(), 'admin', now(), '', NULL, '被审计单位联络员演示账号'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_id = 2006 OR user_name = 'audited_unit_liaison');

INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark)
SELECT 2007, 2104, 'intermediary_auditor', '中介审计人员', '00', 'intermediary@example.com', '13810000007', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', now(), now(), 'admin', now(), '', NULL, '外部中介审计人员演示账号'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_id = 2007 OR user_name = 'intermediary_auditor');

-- ===== 4. 用户角色绑定 =====
INSERT INTO sys_user_role (user_id, role_id)
SELECT 2001, 100 WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 2001 AND role_id = 100);
INSERT INTO sys_user_role (user_id, role_id)
SELECT 2002, 101 WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 2002 AND role_id = 101);
INSERT INTO sys_user_role (user_id, role_id)
SELECT 2003, 102 WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 2003 AND role_id = 102);
INSERT INTO sys_user_role (user_id, role_id)
SELECT 2004, 103 WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 2004 AND role_id = 103);
INSERT INTO sys_user_role (user_id, role_id)
SELECT 2005, 104 WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 2005 AND role_id = 104);
INSERT INTO sys_user_role (user_id, role_id)
SELECT 2006, 105 WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 2006 AND role_id = 105);
INSERT INTO sys_user_role (user_id, role_id)
SELECT 2007, 106 WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 2007 AND role_id = 106);

-- ===== 5. 角色菜单授权 =====
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 12121, '审计准备维护', COALESCE((SELECT menu_id FROM sys_menu WHERE component = 'audit/prepare' ORDER BY menu_id LIMIT 1), 1202), 20, '', '', '', '', 1, 0, 'F', '0', '0', 'audit:prepare:edit', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'audit:prepare:edit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 12122, '资料提交', COALESCE((SELECT menu_id FROM sys_menu WHERE component = 'audit/prepare' ORDER BY menu_id LIMIT 1), 1202), 21, '', '', '', '', 1, 0, 'F', '0', '0', 'audit:prepare:submit', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'audit:prepare:submit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 12123, '资料确认', COALESCE((SELECT menu_id FROM sys_menu WHERE component = 'audit/prepare' ORDER BY menu_id LIMIT 1), 1202), 22, '', '', '', '', 1, 0, 'F', '0', '0', 'audit:prepare:confirm', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'audit:prepare:confirm');

-- 审计处长：审计业务全功能 + 用户/角色/菜单/部门配置 + 临时授权
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 101, m.menu_id
FROM sys_menu m
WHERE (
    m.menu_id IN (1, 100, 101, 102, 103, 1200, 1201, 1202, 1203, 1204, 1205)
    OR m.parent_id IN (100, 101, 102, 103)
    OR m.parent_id BETWEEN 1200 AND 1205
    OR m.component IN ('audit/tempAuth', 'ai/basis', 'audit/risk', 'audit/case', 'audit/unit', 'audit/leader', 'audit/plan', 'audit/projectLib', 'audit/project', 'audit/schemeTemplate', 'audit/prepare', 'knowledge/upload', 'audit/progress', 'ai/forensic', 'audit/workpaper', 'audit/issue', 'audit/report', 'audit/rectification', 'audit/archive', 'ai/visualization/index', 'ai/aiLog', 'ai/chat')
    OR m.perms LIKE 'audit:%'
    OR m.perms LIKE 'ai:%'
)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 101 AND rm.menu_id = m.menu_id);

-- 校领导：门户、驾驶舱、进度、报告、整改、归档、可视化，只读为主
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 100, m.menu_id
FROM sys_menu m
WHERE (
    m.menu_id IN (1203, 1204, 1205)
    OR m.component IN ('audit/progress', 'audit/report', 'audit/rectification', 'audit/archive', 'ai/visualization/index', 'ai/chat')
    OR m.perms IN ('audit:progress:view', 'audit:report:view', 'audit:rectification:view', 'audit:archive:view', 'audit:visual:view')
)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 100 AND rm.menu_id = m.menu_id);

-- 项目组长/主审：项目维护、准备、进度、取证、底稿复核、报告、问题、整改评价
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 102, m.menu_id
FROM sys_menu m
WHERE (
    m.menu_id IN (1200, 1201, 1202, 1203, 1204, 1205)
    OR m.component IN ('audit/unit', 'audit/plan', 'audit/projectLib', 'audit/project', 'audit/schemeTemplate', 'audit/prepare', 'knowledge/upload', 'audit/progress', 'ai/forensic', 'audit/workpaper', 'audit/issue', 'audit/report', 'audit/rectification', 'audit/archive', 'ai/basis', 'audit/risk', 'audit/case', 'ai/visualization/index', 'ai/chat')
    OR m.perms IN ('audit:plan:view', 'audit:plan:edit', 'audit:unit:view', 'audit:projectLib:view', 'audit:projectDoc:view', 'audit:projectDoc:upload', 'audit:project:view', 'audit:template:view', 'audit:prepare:view', 'audit:upload:view', 'audit:progress:view', 'ai:forensic:view', 'ai:forensic:gen', 'ai:forensic:submit', 'ai:forensic:review', 'ai:forensic:delete', 'audit:workpaper:view', 'audit:workpaper:edit', 'audit:workpaper:review', 'audit:issue:view', 'audit:report:view', 'audit:report:edit', 'audit:rectification:view', 'audit:rectification:edit', 'audit:archive:view', 'audit:archive:edit', 'ai:basis:query', 'audit:basis:query', 'audit:visual:view')
)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 102 AND rm.menu_id = m.menu_id);

-- 普通审计人员：分配项目资料、作业录入、数据分析、依据检索，不授予复核和系统配置
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 103, m.menu_id
FROM sys_menu m
WHERE (
    m.menu_id IN (1200, 1201, 1202, 1203, 1205)
    OR m.component IN ('audit/projectLib', 'audit/project', 'audit/prepare', 'knowledge/upload', 'ai/forensic', 'audit/workpaper', 'audit/issue', 'ai/basis', 'audit/risk', 'audit/case', 'ai/visualization/index', 'ai/chat')
    OR m.perms IN ('audit:projectLib:view', 'audit:projectDoc:view', 'audit:projectDoc:upload', 'audit:project:view', 'audit:prepare:view', 'audit:upload:view', 'ai:forensic:view', 'ai:forensic:gen', 'ai:forensic:submit', 'ai:forensic:delete', 'audit:workpaper:view', 'audit:workpaper:edit', 'audit:issue:view', 'ai:basis:query', 'audit:basis:query', 'audit:visual:view')
)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 103 AND rm.menu_id = m.menu_id);

-- 被审计单位负责人：本单位报告、整改台账、整改复核结果、必要的资料查阅
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 104, m.menu_id
FROM sys_menu m
WHERE (
    m.menu_id IN (1201, 1202, 1204)
    OR m.component IN ('audit/projectLib', 'audit/prepare', 'audit/report', 'audit/rectification', 'audit/archive', 'ai/basis')
    OR m.perms IN ('audit:projectLib:view', 'audit:projectDoc:view', 'audit:prepare:view', 'audit:prepare:submit', 'audit:prepare:confirm', 'audit:template:view', 'audit:report:view', 'audit:rectification:view', 'audit:archive:view', 'ai:basis:query', 'audit:basis:query')
)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 104 AND rm.menu_id = m.menu_id);

-- 被审计单位联络员：提交资料、回复取证单、填报整改材料
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 105, m.menu_id
FROM sys_menu m
WHERE (
    m.menu_id IN (1201, 1202, 1204)
    OR m.component IN ('audit/projectLib', 'audit/prepare', 'knowledge/upload', 'audit/rectification', 'ai/basis')
    OR m.perms IN ('audit:projectLib:view', 'audit:projectDoc:view', 'audit:projectDoc:upload', 'audit:prepare:view', 'audit:prepare:submit', 'audit:prepare:confirm', 'audit:prepare:edit', 'audit:template:view', 'audit:upload:view', 'ai:basis:query', 'audit:basis:query', 'audit:rectification:view', 'audit:rectification:edit')
)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 105 AND rm.menu_id = m.menu_id);

-- 中介审计人员：临时访问项目资料、准备材料、取证、底稿录入、问题查看，无全局驾驶舱
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 106, m.menu_id
FROM sys_menu m
WHERE (
    m.menu_id IN (1201, 1202, 1203)
    OR m.component IN ('audit/projectLib', 'audit/project', 'audit/prepare', 'knowledge/upload', 'ai/forensic', 'audit/workpaper', 'audit/issue')
    OR m.perms IN ('audit:projectLib:view', 'audit:projectDoc:view', 'audit:projectDoc:upload', 'audit:project:view', 'audit:prepare:view', 'audit:upload:view', 'ai:forensic:view', 'ai:forensic:gen', 'ai:forensic:submit', 'ai:forensic:delete', 'audit:workpaper:view', 'audit:workpaper:edit', 'audit:issue:view')
)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 106 AND rm.menu_id = m.menu_id);

-- ===== 6. 自定义数据范围部门绑定 =====
-- 预留给后续改为 data_scope = 2 时使用；当前只有被审计单位角色采用本部门数据范围。
INSERT INTO sys_role_dept (role_id, dept_id)
SELECT 104, 2102 WHERE NOT EXISTS (SELECT 1 FROM sys_role_dept WHERE role_id = 104 AND dept_id = 2102);
INSERT INTO sys_role_dept (role_id, dept_id)
SELECT 105, 2102 WHERE NOT EXISTS (SELECT 1 FROM sys_role_dept WHERE role_id = 105 AND dept_id = 2102);

-- ===== 7. A 公司审计准备资料清单 =====
DO $$
BEGIN
  IF to_regclass('audit_material_checklist') IS NOT NULL THEN
    INSERT INTO audit_material_checklist (project_id, material_name, material_type, required, submit_status, file_path, submit_by, source, create_time)
    SELECT 9101, item.material_name, item.material_type, item.required, 0, NULL, NULL, 'unit', now()
    FROM (VALUES
      ('2026年度收入明细表', '财务', 1),
      ('采购合同与付款凭证', '采购', 1),
      ('银行流水及余额调节表', '财务', 1),
      ('固定资产盘点清单', '资产', 0),
      ('内部控制制度及审批流程说明', '其他', 1)
    ) AS item(material_name, material_type, required)
    WHERE NOT EXISTS (
      SELECT 1 FROM audit_material_checklist c
      WHERE c.project_id = 9101 AND c.material_name = item.material_name
    );
  END IF;
END $$;

-- ===== 7. 校验输出 =====
SELECT r.role_id,
       r.role_name,
       r.role_key,
       r.data_scope,
       COUNT(rm.menu_id) AS menu_count
FROM sys_role r
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
WHERE r.role_id BETWEEN 100 AND 106
GROUP BY r.role_id, r.role_name, r.role_key, r.data_scope
ORDER BY r.role_id;

SELECT u.user_id,
       u.user_name,
       u.nick_name,
       d.dept_name,
       r.role_name
FROM sys_user u
JOIN sys_user_role ur ON ur.user_id = u.user_id
JOIN sys_role r ON r.role_id = ur.role_id
LEFT JOIN sys_dept d ON d.dept_id = u.dept_id
WHERE u.user_id BETWEEN 2001 AND 2007
ORDER BY u.user_id;
