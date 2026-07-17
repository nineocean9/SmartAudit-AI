BEGIN;

DELETE FROM audit_review
WHERE workpaper_id IN (SELECT id FROM audit_workpaper WHERE project_id BETWEEN 1 AND 8);
DELETE FROM audit_collab_log WHERE project_id BETWEEN 1 AND 8;
DELETE FROM audit_report_opinion
WHERE report_id IN (SELECT id FROM audit_report WHERE project_id BETWEEN 1 AND 8);
DELETE FROM audit_report_version
WHERE report_id IN (SELECT id FROM audit_report WHERE project_id BETWEEN 1 AND 8);
DELETE FROM audit_project_member WHERE project_id BETWEEN 1 AND 8;
DELETE FROM audit_material_checklist WHERE project_id BETWEEN 1 AND 8;
DELETE FROM audit_scheme WHERE project_id BETWEEN 1 AND 8;
DELETE FROM audit_workpaper WHERE project_id BETWEEN 1 AND 8;
DELETE FROM audit_report WHERE project_id BETWEEN 1 AND 8;
DELETE FROM audit_archive WHERE project_id BETWEEN 1 AND 8;
DELETE FROM audit_plan_attachment WHERE plan_id BETWEEN 1 AND 8;
DELETE FROM audit_plan_project WHERE plan_id BETWEEN 1 AND 8 OR project_id BETWEEN 1 AND 8;
DELETE FROM audit_report_opinion
WHERE report_id IN (SELECT id FROM audit_report WHERE project_id = 9101);
DELETE FROM audit_report_version
WHERE report_id IN (SELECT id FROM audit_report WHERE project_id = 9101);
DELETE FROM audit_project_member WHERE project_id = 9101;
DELETE FROM audit_scheme WHERE project_id = 9101;
DELETE FROM audit_report WHERE project_id = 9101;
DELETE FROM audit_archive WHERE project_id = 9101;
DELETE FROM audit_plan_attachment WHERE plan_id = 9100;
DELETE FROM audit_plan_project WHERE plan_id = 9100 OR project_id = 9101;

UPDATE audit_plan SET
  plan_type = '年度',
  plan_year = 2024,
  batch = '第一批',
  plan_name = '2024年度经济责任审计计划',
  file_url = '/audit-template/default/economic-responsibility-audit-plan-template.docx',
  plan_start_date = '2024-03-01',
  plan_end_date = '2024-06-30',
  approval_status = 1,
  description = '围绕学院主要负责人经济责任履行情况，重点覆盖预算执行、采购管理、合同管理、资产管理和内控制度执行。',
  status = 1
WHERE id = 1;

UPDATE audit_plan SET
  plan_type = '年度',
  plan_year = 2023,
  batch = '第二批',
  plan_name = '2023年度财务收支审计计划',
  file_url = '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx',
  plan_start_date = '2023-09-01',
  plan_end_date = '2023-12-20',
  approval_status = 1,
  description = '核查二级学院收入完整性、支出合规性、预算科目使用和往来款项清理情况。',
  status = 1
WHERE id = 2;

UPDATE audit_plan SET
  plan_type = '专项',
  plan_year = 2024,
  batch = '专项第一批',
  plan_name = '2024年后勤服务保障专项审计计划',
  file_url = '/audit-template/default/special-audit-plan-template.docx',
  plan_start_date = '2024-05-10',
  plan_end_date = '2024-09-30',
  approval_status = 1,
  description = '聚焦后勤服务采购、供应商比价、固定资产盘点和专项资金使用绩效。',
  status = 1
WHERE id = 3;

UPDATE audit_plan SET
  plan_type = '年度',
  plan_year = 2026,
  batch = '第一批',
  plan_name = '2026年度预算执行审计计划',
  file_url = '/audit-template/default/special-audit-plan-template.docx',
  plan_start_date = '2026-02-20',
  plan_end_date = '2026-10-31',
  approval_status = 1,
  description = '覆盖预算批复、预算调整、专项资金用途、科研经费报销和采购验收等重点环节。',
  status = 1
WHERE id = 4;

UPDATE audit_plan SET
  plan_type = '临时',
  plan_year = 2026,
  batch = '委托项目',
  plan_name = '图书馆改造工程结算审计计划',
  file_url = '/audit-template/default/engineering-audit-plan-template.docx',
  plan_start_date = '2026-04-01',
  plan_end_date = '2026-08-31',
  approval_status = 1,
  description = '对图书馆改造工程立项、合同履行、变更签证、工程量清单和结算资料进行审计。',
  status = 1
WHERE id = 5;

UPDATE audit_plan SET
  plan_type = '年度',
  plan_year = 2026,
  batch = 'A公司专项',
  plan_name = 'A公司2026年度财务收支审计计划',
  file_url = '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx',
  plan_start_date = '2026-06-01',
  plan_end_date = '2026-09-30',
  approval_status = 1,
  description = '围绕 A 公司年度财务收支真实性、合法性和效益性，重点审查收入确认、成本费用、资金管理、重大合同、往来款项和内部控制执行情况。',
  status = 1
WHERE id = 9100;

INSERT INTO audit_project (id, project_name, audited_unit, audit_type, audit_year, plan_id, status, create_time)
SELECT v.id, v.project_name, v.audited_unit, v.audit_type, v.audit_year, v.plan_id, v.status, v.create_time
FROM (VALUES
  (1, '信息工程学院2024年经济责任审计', '信息工程学院', '经责审计', 2024, 1, 2, '2024-03-01'::timestamp),
  (2, '商学院2023年财务收支审计', '商学院', '财务收支', 2023, 2, 2, '2023-09-01'::timestamp),
  (3, '后勤处2024年专项审计', '后勤处', '专项审计', 2024, 3, 2, '2024-05-10'::timestamp),
  (4, '财务处2026年预算执行审计', '财务处', '预算执行', 2026, 4, 1, '2026-02-20'::timestamp),
  (5, '图书馆2026年工程审计', '图书馆', '工程审计', 2026, 5, 1, '2026-04-01'::timestamp),
  (6, '信息工程学院2026年预算执行审计', '信息工程学院', '预算执行', 2026, 4, 1, '2026-05-20'::timestamp),
  (7, '科研经费2026年专项审计', '科学技术研究院', '专项审计', 2026, 4, 0, '2026-07-16'::timestamp),
  (8, '资产经营公司2026年度财务收支审计', '资产经营公司', '财务收支', 2026, 4, 0, '2026-07-16'::timestamp)
) AS v(id, project_name, audited_unit, audit_type, audit_year, plan_id, status, create_time)
WHERE NOT EXISTS (SELECT 1 FROM audit_project p WHERE p.id = v.id);

SELECT setval(pg_get_serial_sequence('audit_project', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM audit_project), 1), true);

INSERT INTO audit_unit (unit_name, unit_type, profile, history_audit, create_time)
SELECT v.unit_name, v.unit_type, v.profile, v.history_audit, now()
FROM (VALUES
  ('科学技术研究院', '职能部门', '负责学校科研项目管理、科研平台建设和横向纵向科研经费统筹。', '2024年科研经费专项检查；2022年科研平台建设资金审计。'),
  ('资产经营公司', '直属单位', '负责学校经营性资产运营、校办企业管理和经营收益归集。', '2024年经营性资产专项检查；2021年财务收支审计。')
) AS v(unit_name, unit_type, profile, history_audit)
WHERE NOT EXISTS (SELECT 1 FROM audit_unit u WHERE u.unit_name = v.unit_name);

UPDATE audit_unit SET
  unit_code = 'XXGC-001',
  unit_type = '学院',
  parent_leader = '李明副校长',
  staff_count = 120,
  annual_budget = 1200.00,
  finance_contact = '陈会计',
  contact_phone = '13810001001',
  address = '知行楼 A 座 3-5 层',
  last_audit_date = '2024-06-30',
  status = 1,
  profile = '承担计算机、软件工程、人工智能等学科建设与人才培养任务，管理教学科研经费、实验室资产和学院采购事项。',
  history_audit = '2024年经济责任审计；2022年财务收支审计；2021年实验室建设专项检查。'
WHERE unit_name = '信息工程学院';

UPDATE audit_unit SET
  unit_code = 'SXY-002',
  unit_type = '学院',
  parent_leader = '王敏副校长',
  staff_count = 88,
  annual_budget = 860.00,
  finance_contact = '刘会计',
  contact_phone = '13810001002',
  address = '经管楼 B 座',
  last_audit_date = '2023-12-20',
  status = 1,
  profile = '承担工商管理、会计、金融等专业教学科研工作，涉及培训收入、实践基地合作和科研项目经费。',
  history_audit = '2023年财务收支审计；2020年培训收入专项审计。'
WHERE unit_name = '商学院';

UPDATE audit_unit SET
  unit_code = 'HQC-003',
  unit_type = '处室',
  parent_leader = '赵强副校长',
  staff_count = 65,
  annual_budget = 1500.00,
  finance_contact = '周会计',
  contact_phone = '13810001003',
  address = '后勤服务楼',
  last_audit_date = '2024-09-30',
  status = 1,
  profile = '负责校园物业、食堂监管、维修维护、能源保障和后勤采购管理。',
  history_audit = '2024年专项审计；2021年食堂采购专项检查。'
WHERE unit_name = '后勤处';

UPDATE audit_unit SET
  unit_code = 'CWC-004',
  unit_type = '处室',
  parent_leader = '刘芳总会计师',
  staff_count = 32,
  annual_budget = 3200.00,
  finance_contact = '孙会计',
  contact_phone = '13810001004',
  address = '行政楼 2 层',
  last_audit_date = '2026-06-30',
  status = 1,
  profile = '负责学校预算管理、会计核算、资金收付和财务制度执行。',
  history_audit = '2026年预算执行审计；2024年预算执行专项检查。'
WHERE unit_name = '财务处';

UPDATE audit_unit SET
  unit_code = 'TSG-005',
  unit_type = '直属单位',
  parent_leader = '李明副校长',
  staff_count = 45,
  annual_budget = 720.00,
  finance_contact = '吴会计',
  contact_phone = '13810001005',
  address = '图书馆主馆',
  last_audit_date = '2026-08-31',
  status = 1,
  profile = '负责馆藏资源建设、读者服务、数字资源采购和馆舍改造项目管理。',
  history_audit = '2026年工程审计；2022年数字资源采购专项检查。'
WHERE unit_name = '图书馆';

UPDATE audit_unit SET
  unit_code = 'KYY-006',
  unit_type = '职能部门',
  parent_leader = '王敏副校长',
  staff_count = 28,
  annual_budget = 2600.00,
  finance_contact = '郑会计',
  contact_phone = '13810001006',
  address = '科研楼 6 层',
  last_audit_date = '2024-12-31',
  status = 1
WHERE unit_name = '科学技术研究院';

UPDATE audit_unit SET
  unit_code = 'ZCJY-007',
  unit_type = '直属单位',
  parent_leader = '刘芳总会计师',
  staff_count = 38,
  annual_budget = 1800.00,
  finance_contact = '钱会计',
  contact_phone = '13810001007',
  address = '资产经营大楼',
  last_audit_date = '2024-11-30',
  status = 1
WHERE unit_name = '资产经营公司';

DELETE FROM audit_leader
WHERE unit_id IN (
  SELECT id FROM audit_unit
  WHERE unit_name IN ('信息工程学院', '商学院', '后勤处', '财务处', '图书馆', '科学技术研究院', '资产经营公司')
);

INSERT INTO audit_leader (name, unit_id, position, tenure_start, tenure_end, gender, managed_funds, managed_scope, position_history, audit_evaluation, create_time)
SELECT v.name, u.id, v.position, v.tenure_start::date, v.tenure_end::date, v.gender, v.managed_funds, v.managed_scope, v.position_history, v.audit_evaluation, now()
FROM (VALUES
  ('张三', '信息工程学院', '院长', '2020-01-01', NULL, '男', 1200.00, '学院预算、采购、资产和科研平台建设', '2020年至今任信息工程学院院长', '经济责任履行总体规范，需加强采购论证和预算执行进度管理。'),
  ('李四', '商学院', '院长', '2021-06-01', NULL, '女', 860.00, '学院财务收支、培训收入和实践基地合作', '2021年至今任商学院院长', '财务审批流程基本完整，需持续清理长期往来款项。'),
  ('王五', '后勤处', '处长', '2019-03-01', '2024-12-31', '男', 1500.00, '后勤采购、食堂监管、维修维护和能源保障', '2019年至2024年任后勤处处长', '专项资金使用基本合规，供应商比价机制需进一步固化。'),
  ('赵六', '财务处', '处长', '2022-01-01', NULL, '女', 3200.00, '预算管理、会计核算和资金支付', '2022年至今任财务处处长', '预算管理制度较完整，需强化专项资金用途审核。'),
  ('钱七', '图书馆', '馆长', '2023-01-01', NULL, '男', 720.00, '馆藏采购、数字资源采购和馆舍改造项目', '2023年至今任图书馆馆长', '工程管理资料链条需进一步补充完整。'),
  ('孙八', '科学技术研究院', '院长', '2024-01-01', NULL, '女', 2600.00, '科研项目、科研经费和平台建设资金', '2024年至今任科学技术研究院院长', '科研经费管理制度较完整，外协合同验收资料需重点关注。'),
  ('周九', '资产经营公司', '总经理', '2023-05-01', NULL, '男', 1800.00, '经营性资产运营、合同收入和资金管理', '2023年至今任资产经营公司总经理', '经营收支规模增长较快，需强化重大合同和资金闭环管理。')
) AS v(name, unit_name, position, tenure_start, tenure_end, gender, managed_funds, managed_scope, position_history, audit_evaluation)
JOIN audit_unit u ON u.unit_name = v.unit_name
WHERE NOT EXISTS (
  SELECT 1 FROM audit_leader l
  WHERE l.name = v.name AND l.position = v.position AND l.unit_id = u.id
);

UPDATE audit_project SET
  project_name = '信息工程学院2024年经济责任审计',
  audited_unit = '信息工程学院',
  audit_type = '经责审计',
  audit_year = 2024,
  plan_id = 1,
  start_date = '2024-03-01',
  end_date = '2024-06-30',
  phase = '归档',
  is_overdue = 0,
  status = 2
WHERE id = 1;

UPDATE audit_project SET
  project_name = '商学院2023年财务收支审计',
  audited_unit = '商学院',
  audit_type = '财务收支',
  audit_year = 2023,
  plan_id = 2,
  start_date = '2023-09-01',
  end_date = '2023-12-20',
  phase = '归档',
  is_overdue = 0,
  status = 2
WHERE id = 2;

UPDATE audit_project SET
  project_name = '后勤处2024年专项审计',
  audited_unit = '后勤处',
  audit_type = '专项审计',
  audit_year = 2024,
  plan_id = 3,
  start_date = '2024-05-10',
  end_date = '2024-09-30',
  phase = '归档',
  is_overdue = 0,
  status = 2
WHERE id = 3;

UPDATE audit_project SET
  project_name = '财务处2026年预算执行审计',
  audited_unit = '财务处',
  audit_type = '预算执行',
  audit_year = 2026,
  plan_id = 4,
  start_date = '2026-02-20',
  end_date = '2026-06-30',
  phase = '整改跟踪',
  is_overdue = 1,
  status = 1
WHERE id = 4;

UPDATE audit_project SET
  project_name = '图书馆2026年工程审计',
  audited_unit = '图书馆',
  audit_type = '工程审计',
  audit_year = 2026,
  plan_id = 5,
  start_date = '2026-04-01',
  end_date = '2026-08-31',
  phase = '现场实施',
  is_overdue = 0,
  status = 1
WHERE id = 5;

UPDATE audit_project SET
  project_name = '信息工程学院2026年预算执行审计',
  audited_unit = '信息工程学院',
  audit_type = '预算执行',
  audit_year = 2026,
  plan_id = 4,
  start_date = '2026-05-20',
  end_date = '2026-10-31',
  phase = '底稿复核',
  is_overdue = 0,
  status = 1
WHERE id = 6;

UPDATE audit_project SET
  project_name = '科研经费2026年专项审计',
  audited_unit = '科学技术研究院',
  audit_type = '专项审计',
  audit_year = 2026,
  plan_id = 4,
  start_date = '2026-09-01',
  end_date = '2026-12-15',
  phase = '准备',
  is_overdue = 0,
  status = 0
WHERE id = 7;

UPDATE audit_project SET
  project_name = '资产经营公司2026年度财务收支审计',
  audited_unit = '资产经营公司',
  audit_type = '财务收支',
  audit_year = 2026,
  plan_id = 4,
  start_date = '2026-10-08',
  end_date = '2026-12-31',
  phase = '准备',
  is_overdue = 0,
  status = 0
WHERE id = 8;

INSERT INTO audit_plan_project (plan_id, project_id, bind_time) VALUES
(1, 1, '2024-03-01'),
(2, 2, '2023-09-01'),
(3, 3, '2024-05-10'),
(4, 4, '2026-02-20'),
(5, 5, '2026-04-01'),
(4, 6, '2026-05-20'),
(4, 7, '2026-07-16'),
(4, 8, '2026-07-16')
ON CONFLICT (plan_id, project_id) DO NOTHING;

INSERT INTO audit_plan_attachment (plan_id, file_name, file_path, file_type, file_size, attachment_type, create_by, create_time) VALUES
(1, '2024年度经济责任审计计划.docx', '/audit-template/default/economic-responsibility-audit-plan-template.docx', 'docx', 38335, '计划正文', 'audit_director', '2024-02-26'),
(1, '信息工程学院经济责任审计立项审批表.docx', '/audit-template/default/economic-responsibility-audit-plan-template.docx', 'docx', 38335, '立项附件', 'audit_project_leader', '2024-02-28'),
(2, '2023年度财务收支审计计划.docx', '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx', 'docx', 38093, '计划正文', 'audit_director', '2023-08-25'),
(3, '2024年后勤服务保障专项审计计划.docx', '/audit-template/default/special-audit-plan-template.docx', 'docx', 38051, '计划正文', 'audit_director', '2024-05-06'),
(4, '2026年度预算执行审计计划.docx', '/audit-template/default/special-audit-plan-template.docx', 'docx', 38051, '计划正文', 'audit_director', '2026-02-18'),
(4, '2026年度预算执行审计项目清单.docx', '/audit-template/default/audit-report-draft-template.docx', 'docx', 37657, '项目清单', 'audit_project_leader', '2026-02-18'),
(5, '图书馆改造工程结算审计计划.docx', '/audit-template/default/engineering-audit-plan-template.docx', 'docx', 38200, '计划正文', 'audit_director', '2026-03-28');

INSERT INTO audit_plan_project (plan_id, project_id, bind_time)
VALUES (9100, 9101, '2026-06-01')
ON CONFLICT (plan_id, project_id) DO NOTHING;

INSERT INTO audit_plan_attachment (plan_id, file_name, file_path, file_type, file_size, attachment_type, create_by, create_time) VALUES
(9100, 'A公司2026年度财务收支审计计划.docx', '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx', 'docx', 38093, '计划正文', 'audit_director', '2026-05-25'),
(9100, 'A公司财务收支审计项目清单.docx', '/audit-template/default/audit-report-draft-template.docx', 'docx', 37657, '项目清单', 'audit_project_leader', '2026-05-26');

UPDATE audit_rectification r
SET status = 2,
    finish_date = COALESCE(r.finish_date, '2025-01-31')
WHERE EXISTS (
  SELECT 1 FROM audit_issue i
  WHERE i.id = r.issue_id AND i.project_id IN (1, 2, 3)
);

UPDATE audit_rectification r
SET status = CASE WHEN i.id = 8 THEN 2 ELSE 1 END,
    finish_date = CASE WHEN i.id = 8 THEN '2026-06-15'::date ELSE NULL END
FROM audit_issue i
WHERE i.id = r.issue_id AND i.project_id = 4;

INSERT INTO audit_project_member (project_id, user_id, user_name, role_type, task_scope, task_deadline, status, create_time) VALUES
(1, 2003, '项目组长/主审', '项目组长/主审', '统筹经济责任审计实施、质量复核、报告定稿', '2024-06-30', 2, '2024-03-01'),
(1, 2004, '普通审计人员', '普通审计人员', '采购、预算执行、合同管理审计取证', '2024-06-10', 2, '2024-03-01'),
(2, 2003, '项目组长/主审', '项目组长/主审', '财务收支审计组织与报告审核', '2023-12-20', 2, '2023-09-01'),
(2, 2004, '普通审计人员', '普通审计人员', '收支凭证、往来款项审计', '2023-12-05', 2, '2023-09-01'),
(3, 2003, '项目组长/主审', '项目组长/主审', '专项审计方案执行与问题定性', '2024-09-30', 2, '2024-05-10'),
(3, 2004, '普通审计人员', '普通审计人员', '供应商比价、固定资产盘点审计', '2024-09-15', 2, '2024-05-10'),
(4, 2003, '项目组长/主审', '项目组长/主审', '预算执行审计整改督办与报告编制', '2026-06-30', 1, '2026-02-20'),
(4, 2004, '普通审计人员', '普通审计人员', '专项资金、科研经费报销审计取证', '2026-05-31', 1, '2026-02-20'),
(5, 2003, '项目组长/主审', '项目组长/主审', '工程审计现场组织与造价复核', '2026-08-31', 1, '2026-04-01'),
(5, 2007, '中介审计人员', '中介审计人员', '工程量清单、结算资料辅助核验', '2026-08-15', 1, '2026-04-01'),
(6, 2003, '项目组长/主审', '项目组长/主审', '预算执行审计质量控制', '2026-10-31', 1, '2026-05-20'),
(6, 2004, '普通审计人员', '普通审计人员', '预算执行、采购合同、资产管理底稿编制', '2026-09-30', 1, '2026-05-20');

INSERT INTO audit_material_checklist (project_id, material_name, material_type, required, submit_status, file_path, submit_by, submit_time, source, create_time)
SELECT p.id, m.material_name, m.material_type, 1, 1,
       '/profile/submitted/' || p.id || '/' || m.no || '.docx',
       p.audited_unit,
       p.start_date + (m.no || ' day')::interval,
       'unit',
       p.start_date
FROM audit_project p
CROSS JOIN (VALUES
  (1, '年度预算批复及调整资料', '预算资料'),
  (2, '会计凭证及明细账', '财务资料'),
  (3, '采购合同及招投标资料', '合同采购'),
  (4, '固定资产台账', '资产资料'),
  (5, '内部控制制度汇编', '制度资料'),
  (6, '整改落实支撑材料', '整改资料')
) AS m(no, material_name, material_type)
WHERE p.id IN (1, 2, 3, 4);

INSERT INTO audit_material_checklist (project_id, material_name, material_type, required, submit_status, file_path, submit_by, submit_time, source, create_time)
SELECT 5, m.material_name, m.material_type, 1,
       CASE WHEN m.no <= 5 THEN 1 ELSE 0 END,
       CASE WHEN m.no <= 5 THEN '/profile/submitted/5/' || m.no || '.docx' ELSE NULL END,
       CASE WHEN m.no <= 5 THEN '图书馆' ELSE NULL END,
       CASE WHEN m.no <= 5 THEN '2026-04-01'::timestamp + (m.no || ' day')::interval ELSE NULL END,
       'unit',
       '2026-04-01'
FROM (VALUES
  (1, '工程立项及批复文件', '工程资料'),
  (2, '施工合同及补充协议', '合同资料'),
  (3, '工程量清单与签证资料', '造价资料'),
  (4, '竣工验收资料', '验收资料'),
  (5, '付款凭证及发票', '财务资料'),
  (6, '变更审批资料', '工程资料')
) AS m(no, material_name, material_type);

INSERT INTO audit_material_checklist (project_id, material_name, material_type, required, submit_status, file_path, submit_by, submit_time, source, create_time)
SELECT 6, m.material_name, m.material_type, 1,
       CASE WHEN m.no <= 4 THEN 1 ELSE 0 END,
       CASE WHEN m.no <= 4 THEN '/profile/submitted/6/' || m.no || '.docx' ELSE NULL END,
       CASE WHEN m.no <= 4 THEN '信息工程学院' ELSE NULL END,
       CASE WHEN m.no <= 4 THEN '2026-05-20'::timestamp + (m.no || ' day')::interval ELSE NULL END,
       'unit',
       '2026-05-20'
FROM (VALUES
  (1, '年度预算批复及执行明细', '预算资料'),
  (2, '科研经费收支明细', '财务资料'),
  (3, '采购合同与验收单', '合同采购'),
  (4, '资产购置及领用台账', '资产资料'),
  (5, '内控制度执行说明', '制度资料'),
  (6, '历史整改情况说明', '整改资料')
) AS m(no, material_name, material_type);

INSERT INTO audit_material_checklist (project_id, material_name, material_type, required, submit_status, file_path, submit_by, submit_time, source, create_time)
SELECT 7, m.material_name, m.material_type, 1, 0, NULL, NULL, NULL, 'unit', '2026-07-16'
FROM (VALUES
  (1, '科研项目立项与任务书', '科研资料'),
  (2, '科研经费预算批复', '预算资料'),
  (3, '经费收支明细账', '财务资料'),
  (4, '外协合同及验收资料', '合同采购'),
  (5, '绩效支出发放明细', '薪酬资料'),
  (6, '项目结题或中期检查资料', '成果资料')
) AS m(no, material_name, material_type);

INSERT INTO audit_material_checklist (project_id, material_name, material_type, required, submit_status, file_path, submit_by, submit_time, source, create_time)
SELECT 8, m.material_name, m.material_type, 1, 0, NULL, NULL, NULL, 'unit', '2026-07-16'
FROM (VALUES
  (1, '年度财务报表及审计报告', '财务资料'),
  (2, '收入合同及开票明细', '收入资料'),
  (3, '成本费用明细账', '财务资料'),
  (4, '重大经济合同台账', '合同资料'),
  (5, '银行账户及资金流水', '资金资料'),
  (6, '内部控制制度及授权审批清单', '制度资料')
) AS m(no, material_name, material_type);

UPDATE audit_material_checklist
SET file_path = CASE WHEN submit_status = 1 THEN '/profile/submitted/9101/' || id || '.docx' ELSE file_path END,
    submit_by = CASE WHEN submit_status = 1 THEN COALESCE(submit_by, 'A公司') ELSE submit_by END,
    submit_time = CASE WHEN submit_status = 1 THEN COALESCE(submit_time, '2026-06-05'::timestamp) ELSE submit_time END
WHERE project_id = 9101;

INSERT INTO audit_project_member (project_id, user_id, user_name, role_type, task_scope, task_deadline, status, create_time) VALUES
(9101, 2003, '项目组长/主审', '项目组长/主审', '统筹 A 公司财务收支审计实施、质量复核、报告定稿和整改督办。', '2026-09-30', 1, '2026-06-01'),
(9101, 2004, '普通审计人员', '普通审计人员', '负责收入确认、成本费用、往来款项和资金流水核查，形成审计底稿。', '2026-08-20', 1, '2026-06-01'),
(9101, 2007, '中介审计人员', '中介审计人员', '协助抽查重大合同、银行流水和内控执行资料。', '2026-08-10', 1, '2026-06-01');

INSERT INTO audit_scheme (project_id, plan_id, title, content, template_id, file_url, status, create_by, create_time, update_time) VALUES
(1, 1, '信息工程学院经济责任审计实施方案', '围绕预算执行、采购管理、资产管理和合同管理开展经济责任审计。', '经济责任审计模板', '/audit-template/default/economic-responsibility-audit-plan-template.docx', 1, 'audit_project_leader', '2024-03-03', '2024-03-05'),
(2, 2, '商学院财务收支审计实施方案', '重点核查收入完整性、支出合规性和往来款项清理情况。', '财务收支审计模板', '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx', 1, 'audit_project_leader', '2023-09-03', '2023-09-05'),
(3, 3, '后勤处专项审计实施方案', '重点关注供应商比价机制、资产盘点和专项资金使用。', '专项审计模板', '/audit-template/default/special-audit-plan-template.docx', 1, 'audit_project_leader', '2024-05-12', '2024-05-15'),
(4, 4, '财务处预算执行审计实施方案', '重点审查预算调整、专项资金用途和科研经费报销合规性。', '预算执行审计模板', '/audit-template/default/special-audit-plan-template.docx', 1, 'audit_project_leader', '2026-02-22', '2026-02-25'),
(5, 5, '图书馆工程审计实施方案', '重点审查工程立项、合同履行、工程变更和结算资料。', '工程审计模板', '/audit-template/default/engineering-audit-plan-template.docx', 1, 'audit_project_leader', '2026-04-03', '2026-04-05'),
(6, 4, '信息工程学院预算执行审计实施方案', '重点审查预算执行率、采购验收和资产配置效率。', '预算执行审计模板', '/audit-template/default/special-audit-plan-template.docx', 1, 'audit_project_leader', '2026-05-22', '2026-05-25'),
(7, 4, '科研经费专项审计实施方案', '拟重点审查科研项目立项、预算执行、外协合同、绩效支出和结题验收情况。', '专项审计模板', '/audit-template/default/special-audit-plan-template.docx', 0, 'audit_project_leader', '2026-07-16', NULL),
(8, 4, '资产经营公司财务收支审计实施方案', '拟重点审查收入确认、成本费用列支、资金管理、重大合同和内部控制执行情况。', '财务收支审计模板', '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx', 0, 'audit_project_leader', '2026-07-16', NULL);

INSERT INTO audit_scheme (project_id, plan_id, title, content, template_id, file_url, status, create_by, create_time, update_time) VALUES
(9101, 9100, 'A公司2026年度财务收支审计实施方案', '重点审查 A 公司收入确认、成本费用列支、资金管理、重大合同、往来款项清理和内部控制执行情况。', '财务收支审计模板', '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx', 1, 'audit_project_leader', '2026-06-03', '2026-06-05');

INSERT INTO audit_workpaper (project_id, category, title, content, basis_ids, status, create_by, create_time, update_time) VALUES
(1, '底稿', '采购招标合规性审计底稿', '采购项目抽查、招标方式判断和法规依据核验。', '13,14', 3, 'audit_staff', '2024-04-01', '2024-06-12'),
(1, '底稿', '预算执行率审计底稿', '预算批复、执行明细和执行率测算。', '6,7', 3, 'audit_staff', '2024-04-08', '2024-06-12'),
(1, '底稿', '合同签订规范性底稿', '合同审批、签章和履约节点审查。', '7', 3, 'audit_staff', '2024-04-15', '2024-06-12'),
(2, '底稿', '收支凭证抽查底稿', '抽查收入确认、支出审批和预算科目使用。', '7', 3, 'audit_staff', '2023-10-01', '2023-12-10'),
(2, '底稿', '往来款项清理底稿', '核查长期挂账款项形成原因和清理计划。', '6', 3, 'audit_staff', '2023-10-12', '2023-12-10'),
(3, '底稿', '供应商比价机制底稿', '核验食堂采购比价制度和执行记录。', '14', 3, 'audit_staff', '2024-06-01', '2024-09-10'),
(3, '底稿', '固定资产盘点底稿', '对资产台账和实物盘点差异进行核对。', '7', 3, 'audit_staff', '2024-06-18', '2024-09-10'),
(4, '底稿', '预算调整审批底稿', '核验预算调整审批链条和执行依据。', '6', 2, 'audit_staff', '2026-03-15', '2026-05-25'),
(4, '底稿', '专项资金使用底稿', '核查专项资金用途、支出凭证和审批记录。', '6', 2, 'audit_staff', '2026-03-25', '2026-05-25'),
(4, '底稿', '科研经费报销底稿', '抽查科研经费发票、合同、验收和报销审批。', '7', 2, 'audit_staff', '2026-04-10', '2026-05-28'),
(4, '底稿', '整改跟踪底稿', '跟踪已发现问题整改进度和证明材料。', '7', 2, 'audit_project_leader', '2026-06-01', '2026-06-20'),
(5, '底稿', '工程立项审批底稿', '核验工程立项批复和资金来源。', '7', 2, 'intermediary_auditor', '2026-05-01', '2026-06-15'),
(5, '底稿', '施工合同履行底稿', '核查合同履行、签证和付款进度。', '7', 2, 'intermediary_auditor', '2026-05-12', '2026-06-20'),
(5, '底稿', '工程变更审计底稿', '变更审批资料尚需补充。', '7', 1, 'intermediary_auditor', '2026-06-10', '2026-07-01'),
(5, '底稿', '结算资料审计底稿', '结算资料正在核验。', '7', 0, 'intermediary_auditor', '2026-07-01', NULL),
(6, '底稿', '预算执行率测算底稿', '已完成预算执行率测算和异常项目标注。', '6', 2, 'audit_staff', '2026-06-10', '2026-07-01'),
(6, '底稿', '采购验收底稿', '采购验收资料正在补充核验。', '7', 1, 'audit_staff', '2026-06-20', '2026-07-05'),
(6, '底稿', '资产配置效率底稿', '资产台账待与实物盘点结果核对。', '7', 0, 'audit_staff', '2026-07-01', NULL);

INSERT INTO audit_report (project_id, title, version_type, content, file_url, status, create_by, create_time, update_time) VALUES
(1, '信息工程学院2024年经济责任审计报告', '正式稿', '审计报告已完成处内审核并归档。', '/audit-template/default/audit-report-draft-template.docx', 2, 'audit_project_leader', '2024-06-20', '2024-06-30'),
(2, '商学院2023年财务收支审计报告', '正式稿', '审计报告已完成处内审核并归档。', '/audit-template/default/audit-report-draft-template.docx', 2, 'audit_project_leader', '2023-12-12', '2023-12-20'),
(3, '后勤处2024年专项审计报告', '正式稿', '审计报告已完成处内审核并归档。', '/audit-template/default/audit-report-draft-template.docx', 2, 'audit_project_leader', '2024-09-20', '2024-09-30'),
(4, '财务处2026年预算执行审计报告', '征求意见稿', '报告已形成征求意见稿，正在跟踪未完成整改事项。', '/audit-template/default/audit-report-draft-template.docx', 0, 'audit_project_leader', '2026-06-18', '2026-06-28'),
(5, '图书馆2026年工程审计阶段性报告', '处内审核稿', '工程审计已完成立项、合同履行和部分变更资料核验，结算资料仍在审查。', '/audit-template/default/audit-report-draft-template.docx', 0, 'audit_project_leader', '2026-07-08', '2026-07-12'),
(6, '信息工程学院2026年预算执行审计阶段性报告', '处内审核稿', '已完成预算执行率测算和采购验收核验，资产配置效率事项仍需补充材料。', '/audit-template/default/audit-report-draft-template.docx', 0, 'audit_project_leader', '2026-07-10', '2026-07-14');

INSERT INTO audit_report (project_id, title, version_type, content, file_url, status, create_by, create_time, update_time) VALUES
(9101, 'A公司2026年度财务收支审计阶段性报告', '处内审核稿', '已完成收入确认、成本费用和资金流水初步核查，重大合同与往来款项仍在补充取证。', '/audit-template/default/audit-report-draft-template.docx', 0, 'audit_project_leader', '2026-08-28', '2026-09-05');

INSERT INTO audit_report_version (report_id, version_no, content, change_desc, create_by, create_time)
SELECT r.id, v.version_no, v.content, v.change_desc, 'audit_project_leader', v.create_time
FROM audit_report r
JOIN (VALUES
  (1, 1, '经济责任审计报告初稿，列示采购、预算执行、合同管理三类问题。', '形成报告初稿', '2024-06-15'::timestamp),
  (1, 2, '经济责任审计正式稿，补充整改建议和责任界定。', '根据处内审核意见完善正式稿', '2024-06-30'::timestamp),
  (2, 1, '财务收支审计报告初稿，列示预算科目和往来款项问题。', '形成报告初稿', '2023-12-10'::timestamp),
  (2, 2, '财务收支审计正式稿，补充被审计单位反馈说明。', '吸收反馈后定稿', '2023-12-20'::timestamp),
  (3, 1, '专项审计报告初稿，列示供应商比价和资产盘点事项。', '形成报告初稿', '2024-09-18'::timestamp),
  (4, 1, '预算执行审计征求意见稿，列示专项资金用途和报销凭证问题。', '形成征求意见稿', '2026-06-28'::timestamp),
  (5, 1, '工程审计阶段性报告，列示合同履行和变更资料待补事项。', '形成阶段性报告', '2026-07-12'::timestamp),
  (6, 1, '预算执行审计阶段性报告，列示预算执行率和资产配置事项。', '形成阶段性报告', '2026-07-14'::timestamp)
) AS v(project_id, version_no, content, change_desc, create_time)
ON r.project_id = v.project_id;

INSERT INTO audit_report_version (report_id, version_no, content, change_desc, create_by, create_time)
SELECT r.id, 1, 'A 公司财务收支审计阶段性报告，列示收入确认、成本费用、资金管理和重大合同核查情况。', '形成阶段性报告', 'audit_project_leader', '2026-09-05'::timestamp
FROM audit_report r
WHERE r.project_id = 9101;

INSERT INTO audit_report_opinion (report_id, round_no, opinion_type, content, attachment, submit_by, submit_time)
SELECT r.id, o.round_no, o.opinion_type, o.content, '/audit-template/default/audit-report-draft-template.docx', o.submit_by, o.submit_time
FROM audit_report r
JOIN (VALUES
  (1, 1, '处内审核', '报告事实清楚、依据充分，同意形成正式稿。', 'audit_director', '2024-06-28'::timestamp),
  (2, 1, '被审计单位反馈', '对往来款项清理期限无异议，承诺按期完成整改。', 'audited_unit_principal', '2023-12-16'::timestamp),
  (3, 1, '处内审核', '请补充供应商比价制度执行频次说明，已完成补充。', 'audit_director', '2024-09-25'::timestamp),
  (4, 1, '征求意见', '被审计单位正在核对科研经费报销凭证缺失明细。', 'audited_unit_principal', '2026-06-29'::timestamp),
  (5, 1, '项目组复核', '工程变更审批资料尚需进一步补充。', 'audit_project_leader', '2026-07-12'::timestamp),
  (6, 1, '项目组复核', '资产配置效率问题需要结合实物盘点结果完善。', 'audit_project_leader', '2026-07-14'::timestamp)
) AS o(project_id, round_no, opinion_type, content, submit_by, submit_time)
ON r.project_id = o.project_id;

INSERT INTO audit_report_opinion (report_id, round_no, opinion_type, content, attachment, submit_by, submit_time)
SELECT r.id, 1, '项目组复核', '请继续补充重大合同审批链条和长期往来款项形成原因说明。', '/audit-template/default/audit-report-draft-template.docx', 'audit_project_leader', '2026-09-06'::timestamp
FROM audit_report r
WHERE r.project_id = 9101;

INSERT INTO audit_archive (project_id, archive_no, archive_status, archive_category, file_name, file_path, sort_order, review_by, review_time, archive_time, create_by, create_time) VALUES
(1, 'DA-2024-JJZR-001-01', 2, '立项类', '信息工程学院2024年经济责任审计立项资料.docx', '/audit-template/default/economic-responsibility-audit-plan-template.docx', 10, 'audit_director', '2024-06-28', '2024-06-30', 'audit_project_leader', '2024-06-25'),
(1, 'DA-2024-JJZR-001-02', 2, '证明类', '信息工程学院2024年经济责任审计底稿汇编.docx', '/audit-template/default/economic-responsibility-audit-plan-template.docx', 20, 'audit_director', '2024-06-28', '2024-06-30', 'audit_project_leader', '2024-06-25'),
(1, 'DA-2024-JJZR-001-03', 2, '结论类', '信息工程学院2024年经济责任审计报告.docx', '/audit-template/default/audit-report-draft-template.docx', 30, 'audit_director', '2024-06-28', '2024-06-30', 'audit_project_leader', '2024-06-25'),
(1, 'DA-2024-JJZR-001-04', 2, '整改类', '信息工程学院2024年经济责任审计整改销号资料.docx', '/audit-template/default/audit-report-draft-template.docx', 40, 'audit_director', '2024-06-28', '2024-06-30', 'audit_project_leader', '2024-06-25'),
(2, 'DA-2023-CWSZ-002-01', 2, '立项类', '商学院2023年财务收支审计方案.docx', '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx', 10, 'audit_director', '2023-12-18', '2023-12-20', 'audit_project_leader', '2023-12-15'),
(2, 'DA-2023-CWSZ-002-02', 2, '结论类', '商学院2023年财务收支审计报告.docx', '/audit-template/default/audit-report-draft-template.docx', 30, 'audit_director', '2023-12-18', '2023-12-20', 'audit_project_leader', '2023-12-15'),
(2, 'DA-2023-CWSZ-002-03', 2, '备查类', '商学院2023年财务收支审计会议纪要.docx', '/audit-template/default/audit-report-draft-template.docx', 50, 'audit_director', '2023-12-18', '2023-12-20', 'audit_project_leader', '2023-12-15'),
(3, 'DA-2024-ZX-003-01', 2, '立项类', '后勤处2024年专项审计方案.docx', '/audit-template/default/special-audit-plan-template.docx', 10, 'audit_director', '2024-09-28', '2024-09-30', 'audit_project_leader', '2024-09-25'),
(3, 'DA-2024-ZX-003-02', 2, '证明类', '后勤处2024年专项审计取证材料.docx', '/audit-template/default/special-audit-plan-template.docx', 20, 'audit_director', '2024-09-28', '2024-09-30', 'audit_project_leader', '2024-09-25'),
(3, 'DA-2024-ZX-003-03', 2, '结论类', '后勤处2024年专项审计报告.docx', '/audit-template/default/audit-report-draft-template.docx', 30, 'audit_director', '2024-09-28', '2024-09-30', 'audit_project_leader', '2024-09-25'),
(4, 'DA-2026-YSZX-004-01', 1, '立项类', '财务处2026年预算执行审计方案.docx', '/audit-template/default/special-audit-plan-template.docx', 10, 'audit_project_leader', '2026-06-28', NULL, 'audit_project_leader', '2026-06-28'),
(4, 'DA-2026-YSZX-004-02', 1, '结论类', '财务处2026年预算执行审计征求意见稿.docx', '/audit-template/default/audit-report-draft-template.docx', 30, 'audit_project_leader', '2026-06-28', NULL, 'audit_project_leader', '2026-06-28'),
(5, 'DA-2026-GC-005-01', 0, '立项类', '图书馆2026年工程审计方案.docx', '/audit-template/default/engineering-audit-plan-template.docx', 10, NULL, NULL, NULL, 'audit_project_leader', '2026-07-01'),
(5, 'DA-2026-GC-005-02', 0, '证明类', '图书馆2026年工程审计底稿汇编.docx', '/audit-template/default/engineering-audit-plan-template.docx', 20, NULL, NULL, NULL, 'intermediary_auditor', '2026-07-10'),
(5, 'DA-2026-GC-005-03', 0, '结论类', '图书馆2026年工程审计阶段性报告.docx', '/audit-template/default/audit-report-draft-template.docx', 30, NULL, NULL, NULL, 'audit_project_leader', '2026-07-12'),
(6, 'DA-2026-YSZX-006-01', 0, '立项类', '信息工程学院2026年预算执行审计方案.docx', '/audit-template/default/special-audit-plan-template.docx', 10, NULL, NULL, NULL, 'audit_project_leader', '2026-07-05'),
(6, 'DA-2026-YSZX-006-02', 0, '证明类', '信息工程学院2026年预算执行审计底稿汇编.docx', '/audit-template/default/special-audit-plan-template.docx', 20, NULL, NULL, NULL, 'audit_staff', '2026-07-08'),
(6, 'DA-2026-YSZX-006-03', 0, '结论类', '信息工程学院2026年预算执行审计阶段性报告.docx', '/audit-template/default/audit-report-draft-template.docx', 30, NULL, NULL, NULL, 'audit_project_leader', '2026-07-14'),
(7, 'DA-2026-KYZX-007-01', 0, '立项类', '科研经费2026年专项审计方案.docx', '/audit-template/default/special-audit-plan-template.docx', 10, NULL, NULL, NULL, 'audit_project_leader', '2026-07-16'),
(8, 'DA-2026-CWSZ-008-01', 0, '立项类', '资产经营公司2026年度财务收支审计方案.docx', '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx', 10, NULL, NULL, NULL, 'audit_project_leader', '2026-07-16');

INSERT INTO audit_archive (project_id, archive_no, archive_status, archive_category, file_name, file_path, sort_order, review_by, review_time, archive_time, create_by, create_time) VALUES
(9101, 'DA-2026-AGS-9101-01', 0, '立项类', 'A公司2026年度财务收支审计方案.docx', '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx', 10, NULL, NULL, NULL, 'audit_project_leader', '2026-06-05'),
(9101, 'DA-2026-AGS-9101-02', 0, '证明类', 'A公司2026年度财务收支审计底稿汇编.docx', '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx', 20, NULL, NULL, NULL, 'audit_staff', '2026-08-20'),
(9101, 'DA-2026-AGS-9101-03', 0, '结论类', 'A公司2026年度财务收支审计阶段性报告.docx', '/audit-template/default/audit-report-draft-template.docx', 30, NULL, NULL, NULL, 'audit_project_leader', '2026-09-05');

DO $$
BEGIN
  IF to_regclass('project_document') IS NOT NULL THEN
    DELETE FROM project_document WHERE project_id BETWEEN 1 AND 8;

    INSERT INTO project_document (project_id, plan_id, doc_type, file_name, file_path, file_size, file_ext, content_text, status, chunk_count, create_by, create_time, update_time)
    VALUES
    (1, 1, '审计方案', '信息工程学院2024年经济责任审计实施方案.docx', '/audit-template/default/economic-responsibility-audit-plan-template.docx', 38335, 'docx', '经济责任审计实施方案，覆盖预算执行、采购管理、资产管理和合同管理。', 1, 0, 'audit_project_leader', '2024-03-05', '2024-03-05'),
    (1, 1, '审计报告', '信息工程学院2024年经济责任审计报告.docx', '/audit-template/default/audit-report-draft-template.docx', 37657, 'docx', '经济责任审计正式报告，包含采购招标、预算执行和合同管理问题。', 1, 0, 'audit_project_leader', '2024-06-30', '2024-06-30'),
    (2, 2, '审计方案', '商学院2023年财务收支审计方案.docx', '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx', 38093, 'docx', '财务收支审计方案，覆盖收入完整性、支出合规性和往来款项清理。', 1, 0, 'audit_project_leader', '2023-09-05', '2023-09-05'),
    (2, 2, '审计报告', '商学院2023年财务收支审计报告.docx', '/audit-template/default/audit-report-draft-template.docx', 37657, 'docx', '财务收支审计正式报告，包含预算科目使用和往来款项问题。', 1, 0, 'audit_project_leader', '2023-12-20', '2023-12-20'),
    (3, 3, '审计方案', '后勤处2024年专项审计方案.docx', '/audit-template/default/special-audit-plan-template.docx', 38051, 'docx', '后勤服务保障专项审计方案，覆盖供应商比价和资产盘点。', 1, 0, 'audit_project_leader', '2024-05-15', '2024-05-15'),
    (3, 3, '审计报告', '后勤处2024年专项审计报告.docx', '/audit-template/default/audit-report-draft-template.docx', 37657, 'docx', '专项审计正式报告，包含供应商比价机制和固定资产盘点问题。', 1, 0, 'audit_project_leader', '2024-09-30', '2024-09-30'),
    (4, 4, '审计方案', '财务处2026年预算执行审计方案.docx', '/audit-template/default/special-audit-plan-template.docx', 38051, 'docx', '预算执行审计方案，覆盖预算调整、专项资金和科研经费报销。', 1, 0, 'audit_project_leader', '2026-02-25', '2026-02-25'),
    (4, 4, '审计报告', '财务处2026年预算执行审计征求意见稿.docx', '/audit-template/default/audit-report-draft-template.docx', 37657, 'docx', '预算执行审计征求意见稿，正在跟踪未完成整改事项。', 1, 0, 'audit_project_leader', '2026-06-28', '2026-06-28'),
    (5, 5, '审计方案', '图书馆2026年工程审计方案.docx', '/audit-template/default/engineering-audit-plan-template.docx', 38200, 'docx', '工程审计方案，覆盖立项、合同履行、工程变更和结算资料。', 1, 0, 'audit_project_leader', '2026-04-05', '2026-04-05'),
    (5, 5, '审计报告', '图书馆2026年工程审计阶段性报告.docx', '/audit-template/default/audit-report-draft-template.docx', 37657, 'docx', '工程审计阶段性报告，结算资料仍在核验。', 1, 0, 'audit_project_leader', '2026-07-12', '2026-07-12'),
    (6, 4, '审计方案', '信息工程学院2026年预算执行审计方案.docx', '/audit-template/default/special-audit-plan-template.docx', 38051, 'docx', '预算执行审计方案，覆盖预算执行率、采购验收和资产配置效率。', 1, 0, 'audit_project_leader', '2026-05-25', '2026-05-25'),
    (6, 4, '审计报告', '信息工程学院2026年预算执行审计阶段性报告.docx', '/audit-template/default/audit-report-draft-template.docx', 37657, 'docx', '预算执行审计阶段性报告，资产配置效率事项仍需补充材料。', 1, 0, 'audit_project_leader', '2026-07-14', '2026-07-14'),
    (7, 4, '审计方案', '科研经费2026年专项审计方案.docx', '/audit-template/default/special-audit-plan-template.docx', 38051, 'docx', '科研经费专项审计方案，待启动现场实施。', 1, 0, 'audit_project_leader', '2026-07-16', '2026-07-16'),
    (8, 4, '审计方案', '资产经营公司2026年度财务收支审计方案.docx', '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx', 38093, 'docx', '资产经营公司财务收支审计方案，待启动现场实施。', 1, 0, 'audit_project_leader', '2026-07-16', '2026-07-16');
    INSERT INTO project_document (project_id, plan_id, doc_type, file_name, file_path, file_size, file_ext, content_text, status, chunk_count, create_by, create_time, update_time)
    VALUES
    (9101, 9100, '审计方案', 'A公司2026年度财务收支审计方案.docx', '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx', 38093, 'docx', 'A 公司财务收支审计方案，覆盖收入确认、成本费用、资金管理、重大合同和内部控制。', 1, 0, 'audit_project_leader', '2026-06-05', '2026-06-05'),
    (9101, 9100, '审计报告', 'A公司2026年度财务收支审计阶段性报告.docx', '/audit-template/default/audit-report-draft-template.docx', 37657, 'docx', 'A 公司财务收支审计阶段性报告，重大合同与往来款项仍在补充取证。', 1, 0, 'audit_project_leader', '2026-09-05', '2026-09-05');
  END IF;
END $$;

INSERT INTO audit_collab_log (project_id, user_id, action, target, create_time) VALUES
(4, 2003, '审批方案', '财务处预算执行审计实施方案', '2026-02-25'),
(4, 2004, '提交底稿', '科研经费报销底稿', '2026-05-28'),
(4, 2003, '发起整改跟踪', '专项资金使用问题整改', '2026-06-20'),
(5, 2007, '提交底稿', '工程立项审批底稿', '2026-06-15'),
(6, 2004, '提交底稿', '预算执行率测算底稿', '2026-07-01');

COMMIT;
