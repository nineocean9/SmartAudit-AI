-- Demo users, role menus and A-company scoped data.
-- Safe to run repeatedly on PostgreSQL after the base RuoYi, audit and AI scripts.

DO $$
BEGIN
  IF to_regclass('sys_dept') IS NULL
     OR to_regclass('sys_user') IS NULL
     OR to_regclass('sys_role') IS NULL
     OR to_regclass('sys_menu') IS NULL
     OR to_regclass('sys_user_role') IS NULL
     OR to_regclass('sys_role_menu') IS NULL
     OR to_regclass('audit_plan') IS NULL
     OR to_regclass('audit_project') IS NULL
     OR to_regclass('project_document') IS NULL THEN
    RAISE EXCEPTION 'Missing required tables. Run the base RuoYi, audit business and AI workspace SQL scripts first.';
  END IF;
END $$;

ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS plan_id BIGINT;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS doc_count INT DEFAULT 0;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS start_date DATE;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS end_date DATE;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS progress INT DEFAULT 0;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS phase VARCHAR(20) DEFAULT '准备';
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS is_overdue INT DEFAULT 0;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 12121, '审计准备维护', COALESCE((SELECT menu_id FROM sys_menu WHERE component = 'audit/prepare' ORDER BY menu_id LIMIT 1), 1202), 20, '', '', '', '', 1, 0, 'F', '0', '0', 'audit:prepare:edit', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'audit:prepare:edit');

-- 1. Departments
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
SELECT 2104, 0, '0', '外部中介审计机构', 99, '项目经理', '13800000004', 'agency@example.com', '0', '0', 'admin', now(), '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_id = 2104);

INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time)
SELECT 2105, 2100, '0,2100', 'A公司', 20, 'A公司负责人', '13800000005', 'acompany@example.com', '0', '0', 'admin', now(), '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_id = 2105);

-- 2. Roles
UPDATE sys_role SET role_name = '校领导', role_key = 'school_leader', role_sort = 10, data_scope = '1', status = '0', del_flag = '0', update_by = 'admin', update_time = now()
WHERE role_id = 100 OR role_key = 'school_leader';
UPDATE sys_role SET role_name = '审计处长', role_key = 'audit_director', role_sort = 20, data_scope = '1', status = '0', del_flag = '0', update_by = 'admin', update_time = now()
WHERE role_id = 101 OR role_key = 'audit_director';
UPDATE sys_role SET role_name = '项目组长/主审', role_key = 'audit_project_leader', role_sort = 30, data_scope = '5', status = '0', del_flag = '0', update_by = 'admin', update_time = now()
WHERE role_id = 102 OR role_key IN ('audit_project_leader', 'audit_leader');
UPDATE sys_role SET role_name = '普通审计人员', role_key = 'audit_staff', role_sort = 40, data_scope = '5', status = '0', del_flag = '0', update_by = 'admin', update_time = now()
WHERE role_id = 103 OR role_key IN ('audit_staff', 'auditor');
UPDATE sys_role SET role_name = '被审计单位负责人', role_key = 'audited_unit_principal', role_sort = 50, data_scope = '3', status = '0', del_flag = '0', update_by = 'admin', update_time = now()
WHERE role_id = 104 OR role_key IN ('audited_unit_principal', 'unit_manager', 'unit_principal');
UPDATE sys_role SET role_name = '被审计单位联络员', role_key = 'audited_unit_liaison', role_sort = 60, data_scope = '3', status = '0', del_flag = '0', update_by = 'admin', update_time = now()
WHERE role_id = 105 OR role_key IN ('audited_unit_liaison', 'unit_contact', 'unit_liaison');
UPDATE sys_role SET role_name = '中介审计人员', role_key = 'intermediary_auditor', role_sort = 70, data_scope = '5', status = '0', del_flag = '0', update_by = 'admin', update_time = now()
WHERE role_id = 106 OR role_key IN ('intermediary_auditor', 'outsource', 'intermediary');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT role_id, role_name, role_key, role_sort, data_scope, TRUE, TRUE, '0', '0', 'admin', now(), '', NULL, remark
FROM (VALUES
  (100, '校领导', 'school_leader', 10, '1', '全校审计总览、报告阅览、整改统计和审批'),
  (101, '审计处长', 'audit_director', 20, '1', '审计处负责人，全系统审计功能和权限配置'),
  (102, '项目组长/主审', 'audit_project_leader', 30, '5', '项目维护、底稿复核、报告编制'),
  (103, '普通审计人员', 'audit_staff', 40, '5', '项目资料、底稿、问题、依据检索'),
  (104, '被审计单位负责人', 'audited_unit_principal', 50, '3', '本单位资料、报告、整改和取证查看'),
  (105, '被审计单位联络员', 'audited_unit_liaison', 60, '3', '本单位资料提交、取证回复、整改反馈'),
  (106, '中介审计人员', 'intermediary_auditor', 70, '5', '授权项目资料、底稿和问题查看')
) AS seed(role_id, role_name, role_key, role_sort, data_scope, remark)
WHERE NOT EXISTS (SELECT 1 FROM sys_role r WHERE r.role_id = seed.role_id OR r.role_key = seed.role_key);

-- 3. Users. Password is admin123.
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark)
SELECT user_id, dept_id, user_name, nick_name, '00', email, phone, '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', now(), now(), 'admin', now(), '', NULL, remark
FROM (VALUES
  (2001, 2100, 'school_leader', '校领导', 'school_leader@example.edu.cn', '13810000001', '校领导演示账号'),
  (2002, 2101, 'audit_director', '审计处长', 'audit_director@example.edu.cn', '13810000002', '审计处长演示账号'),
  (2003, 2101, 'audit_project_leader', '项目组长/主审', 'lead@example.edu.cn', '13810000003', '项目组长/主审演示账号'),
  (2004, 2101, 'audit_staff', '普通审计人员', 'auditor@example.edu.cn', '13810000004', '普通审计人员演示账号'),
  (2005, 2102, 'audited_unit_principal', '信息工程学院负责人', 'principal@example.edu.cn', '13810000005', '信息工程学院负责人演示账号'),
  (2006, 2102, 'audited_unit_liaison', '信息工程学院联络员', 'liaison@example.edu.cn', '13810000006', '信息工程学院联络员演示账号'),
  (2007, 2104, 'intermediary_auditor', '中介审计人员', 'intermediary@example.com', '13810000007', '外部中介审计人员演示账号'),
  (2010, 2105, 'a_company_principal', 'A公司负责人', 'principal@acompany.example.com', '13810000010', 'A公司负责人演示账号'),
  (2011, 2105, 'a_company_liaison', 'A公司联络员', 'liaison@acompany.example.com', '13810000011', 'A公司联络员演示账号')
) AS seed(user_id, dept_id, user_name, nick_name, email, phone, remark)
WHERE NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.user_id = seed.user_id OR u.user_name = seed.user_name);

UPDATE sys_user u
SET dept_id = seed.dept_id,
    user_name = seed.user_name,
    nick_name = seed.nick_name,
    email = seed.email,
    phonenumber = seed.phone,
    password = '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
    status = '0',
    del_flag = '0',
    update_by = 'admin',
    update_time = now(),
    remark = seed.remark
FROM (VALUES
  (2001, 2100, 'school_leader', '校领导', 'school_leader@example.edu.cn', '13810000001', '校领导演示账号'),
  (2002, 2101, 'audit_director', '审计处长', 'audit_director@example.edu.cn', '13810000002', '审计处长演示账号'),
  (2003, 2101, 'audit_project_leader', '项目组长/主审', 'lead@example.edu.cn', '13810000003', '项目组长/主审演示账号'),
  (2004, 2101, 'audit_staff', '普通审计人员', 'auditor@example.edu.cn', '13810000004', '普通审计人员演示账号'),
  (2005, 2102, 'audited_unit_principal', '信息工程学院负责人', 'principal@example.edu.cn', '13810000005', '信息工程学院负责人演示账号'),
  (2006, 2102, 'audited_unit_liaison', '信息工程学院联络员', 'liaison@example.edu.cn', '13810000006', '信息工程学院联络员演示账号'),
  (2007, 2104, 'intermediary_auditor', '中介审计人员', 'intermediary@example.com', '13810000007', '外部中介审计人员演示账号'),
  (2010, 2105, 'a_company_principal', 'A公司负责人', 'principal@acompany.example.com', '13810000010', 'A公司负责人演示账号'),
  (2011, 2105, 'a_company_liaison', 'A公司联络员', 'liaison@acompany.example.com', '13810000011', 'A公司联络员演示账号')
) AS seed(user_id, dept_id, user_name, nick_name, email, phone, remark)
WHERE u.user_id = seed.user_id OR u.user_name = seed.user_name;

DELETE FROM sys_user_role WHERE user_id IN (2001,2002,2003,2004,2005,2006,2007,2010,2011);
INSERT INTO sys_user_role (user_id, role_id)
SELECT * FROM (VALUES
  (2001, 100),
  (2002, 101),
  (2003, 102),
  (2004, 103),
  (2005, 104),
  (2006, 105),
  (2007, 106),
  (2010, 104),
  (2011, 105)
) AS seed(user_id, role_id)
WHERE NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = seed.user_id AND ur.role_id = seed.role_id);

-- 4. Role menus. Include parent menu 1201 so sidebar and homepage stay consistent.
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 103, m.menu_id
FROM sys_menu m
WHERE (
    m.menu_id IN (1200, 1201, 1202, 1203, 1205)
    OR m.component IN ('audit/projectLib', 'audit/project', 'audit/prepare', 'knowledge/upload', 'ai/forensic', 'audit/workpaper', 'audit/issue', 'ai/basis', 'audit/risk', 'audit/case', 'ai/visualization/index', 'ai/chat')
    OR m.perms IN ('audit:projectLib:view', 'audit:projectDoc:view', 'audit:projectDoc:upload', 'audit:project:view', 'audit:prepare:view', 'audit:upload:view', 'ai:forensic:view', 'ai:forensic:gen', 'ai:forensic:submit', 'ai:forensic:delete', 'audit:workpaper:view', 'audit:workpaper:edit', 'audit:issue:view', 'ai:basis:query', 'audit:basis:query', 'audit:visual:view')
)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 103 AND rm.menu_id = m.menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 104, m.menu_id
FROM sys_menu m
WHERE (
    m.menu_id IN (1201, 1202, 1204)
    OR m.component IN ('audit/projectLib', 'audit/prepare', 'audit/report', 'audit/rectification', 'audit/archive', 'ai/basis')
    OR m.perms IN ('audit:projectLib:view', 'audit:projectDoc:view', 'audit:prepare:view', 'audit:template:view', 'audit:report:view', 'audit:rectification:view', 'audit:archive:view', 'ai:basis:query', 'audit:basis:query')
)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 104 AND rm.menu_id = m.menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 105, m.menu_id
FROM sys_menu m
WHERE (
    m.menu_id IN (1201, 1202, 1204)
    OR m.component IN ('audit/projectLib', 'audit/prepare', 'knowledge/upload', 'audit/rectification', 'ai/basis')
    OR m.perms IN ('audit:projectLib:view', 'audit:projectDoc:view', 'audit:projectDoc:upload', 'audit:prepare:view', 'audit:prepare:edit', 'audit:template:view', 'audit:upload:view', 'ai:basis:query', 'audit:basis:query', 'audit:rectification:view', 'audit:rectification:edit')
)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 105 AND rm.menu_id = m.menu_id);

-- 5. A-company plan, project and demo documents.
INSERT INTO audit_plan (id, plan_type, plan_year, batch, plan_name, status, create_time)
SELECT 9100, '年度计划', 2026, 'A公司专项', 'A公司2026年度财务收支审计计划', 1, now()
WHERE NOT EXISTS (SELECT 1 FROM audit_plan WHERE id = 9100 OR plan_name = 'A公司2026年度财务收支审计计划');

INSERT INTO audit_project (id, project_name, audited_unit, audit_type, audit_year, status, plan_id, doc_count, start_date, end_date, progress, phase, is_overdue, create_time, update_time)
SELECT 9101, 'A公司2026年财务收支审计', 'A公司', '财务收支', 2026, 1, 9100, 3, DATE '2026-06-01', DATE '2026-09-30', 42, '实施', 0, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM audit_project WHERE id = 9101 OR project_name = 'A公司2026年财务收支审计');

UPDATE audit_project
SET project_name = 'A公司2026年财务收支审计',
    audited_unit = 'A公司',
    audit_type = '财务收支',
    audit_year = 2026,
    status = 1,
    plan_id = 9100,
    doc_count = 3,
    start_date = DATE '2026-06-01',
    end_date = DATE '2026-09-30',
    progress = 42,
    phase = '实施',
    is_overdue = 0,
    update_time = now()
WHERE id = 9101 OR project_name = 'A公司2026年财务收支审计';

SELECT setval(pg_get_serial_sequence('audit_plan', 'id'), (SELECT COALESCE(MAX(id), 1) FROM audit_plan));
SELECT setval(pg_get_serial_sequence('audit_project', 'id'), (SELECT COALESCE(MAX(id), 1) FROM audit_project));

DELETE FROM project_document WHERE project_id = 9101 AND create_by = 'a_company_liaison';
INSERT INTO project_document (project_id, plan_id, doc_type, file_name, file_path, file_size, file_ext, content_text, status, chunk_count, create_by, create_time, update_time)
VALUES
  (9101, 9100, '财务数据', 'A公司2026年收入明细说明.txt', NULL, 128000, 'txt', 'A公司2026年收入明细、合同回款、往来款项示例数据。', 1, 0, 'a_company_liaison', now(), now()),
  (9101, 9100, '合同', 'A公司采购合同抽样说明.txt', NULL, 96000, 'txt', 'A公司采购合同、审批流程、验收单据和付款节点示例资料。', 1, 0, 'a_company_liaison', now(), now()),
  (9101, 9100, '整改资料', 'A公司整改反馈说明.txt', NULL, 78000, 'txt', 'A公司针对财务收支审计问题提交的整改说明和佐证材料。', 1, 0, 'a_company_liaison', now(), now());

-- 6. Quick validation output.
SELECT u.user_id, u.user_name, u.nick_name, d.dept_name, r.role_name, r.role_key
FROM sys_user u
JOIN sys_dept d ON d.dept_id = u.dept_id
JOIN sys_user_role ur ON ur.user_id = u.user_id
JOIN sys_role r ON r.role_id = ur.role_id
WHERE u.user_id IN (2001,2002,2003,2004,2005,2006,2007,2010,2011)
ORDER BY u.user_id;

SELECT p.id, p.project_name, p.audited_unit, p.plan_id, p.doc_count, COUNT(d.id) AS actual_doc_count
FROM audit_project p
LEFT JOIN project_document d ON d.project_id = p.id AND d.status = 1
WHERE p.id = 9101
GROUP BY p.id, p.project_name, p.audited_unit, p.plan_id, p.doc_count;

-- 7. Audit preparation workflow permissions and A-company material checklist.
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 12122, '资料提交', COALESCE((SELECT menu_id FROM sys_menu WHERE component = 'audit/prepare' ORDER BY menu_id LIMIT 1), 1202), 21, '', '', '', '', 1, 0, 'F', '0', '0', 'audit:prepare:submit', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'audit:prepare:submit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 12123, '资料确认', COALESCE((SELECT menu_id FROM sys_menu WHERE component = 'audit/prepare' ORDER BY menu_id LIMIT 1), 1202), 22, '', '', '', '', 1, 0, 'F', '0', '0', 'audit:prepare:confirm', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'audit:prepare:confirm');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role_id, m.menu_id
FROM (VALUES (104), (105)) AS roles(role_id)
JOIN sys_menu m ON m.perms IN ('audit:prepare:view', 'audit:prepare:submit')
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = roles.role_id AND rm.menu_id = m.menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 104, m.menu_id
FROM sys_menu m
WHERE m.perms = 'audit:prepare:confirm'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 104 AND rm.menu_id = m.menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 105, m.menu_id
FROM sys_menu m
WHERE m.perms = 'audit:prepare:confirm'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 105 AND rm.menu_id = m.menu_id);

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
