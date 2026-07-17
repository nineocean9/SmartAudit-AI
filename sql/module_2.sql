-- 模块二：审计作业管理
-- 1. 项目方案
CREATE TABLE IF NOT EXISTS audit_scheme (
  id              BIGSERIAL       PRIMARY KEY,
  project_id      BIGINT          REFERENCES audit_project(id),
  title           VARCHAR(200),
  content         TEXT,
  template_id     VARCHAR(64),
  file_url        VARCHAR(500),
  status          SMALLINT        DEFAULT 0,
  create_by       VARCHAR(64), create_time TIMESTAMP DEFAULT now(), update_time TIMESTAMP
);
COMMENT ON TABLE audit_scheme IS '项目方案';
COMMENT ON COLUMN audit_scheme.status IS '0草稿 1已审批';

-- 2. 审计底稿
CREATE TABLE IF NOT EXISTS audit_workpaper (
  id              BIGSERIAL       PRIMARY KEY,
  project_id      BIGINT          REFERENCES audit_project(id),
  category        VARCHAR(32)     DEFAULT '底稿',
  title           VARCHAR(255),
  content         TEXT,
  basis_ids       VARCHAR(512),
  status          SMALLINT        DEFAULT 0,
  create_by       VARCHAR(64), create_time TIMESTAMP DEFAULT now(), update_time TIMESTAMP
);
COMMENT ON TABLE audit_workpaper IS '审计底稿';
COMMENT ON COLUMN audit_workpaper.category IS '取证记录/底稿';
COMMENT ON COLUMN audit_workpaper.status IS '0草稿 1待复核 2已复核 3已归档';

-- 3. 底稿复核
CREATE TABLE IF NOT EXISTS audit_review (
  id              BIGSERIAL       PRIMARY KEY,
  workpaper_id    BIGINT          REFERENCES audit_workpaper(id),
  reviewer        VARCHAR(64),    level VARCHAR(32),
  opinion         TEXT,           status SMALLINT DEFAULT 0,
  create_time     TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE audit_review IS '底稿复核';
COMMENT ON COLUMN audit_review.level IS '主审/项目组长/审计处长';
COMMENT ON COLUMN audit_review.status IS '0待审 1通过 2驳回';

-- 4. 审计报告
CREATE TABLE IF NOT EXISTS audit_report (
  id              BIGSERIAL       PRIMARY KEY,
  project_id      BIGINT          REFERENCES audit_project(id),
  title           VARCHAR(200),
  version_type    VARCHAR(32),
  content         TEXT,
  file_url        VARCHAR(500),
  status          SMALLINT        DEFAULT 0,
  create_by       VARCHAR(64), create_time TIMESTAMP DEFAULT now(), update_time TIMESTAMP
);
COMMENT ON TABLE audit_report IS '审计报告';
COMMENT ON COLUMN audit_report.version_type IS '处内审核稿/征求意见稿/正式稿';
COMMENT ON COLUMN audit_report.status IS '0草稿 1待审批 2已审批';

-- 5. 协同操作日志
CREATE TABLE IF NOT EXISTS audit_collab_log (
  id              BIGSERIAL       PRIMARY KEY,
  project_id      BIGINT          REFERENCES audit_project(id),
  user_id         BIGINT,         action VARCHAR(64),
  target          VARCHAR(255),   create_time TIMESTAMP DEFAULT now()
);
COMMENT ON TABLE audit_collab_log IS '协同操作日志';

-- 种子数据
INSERT INTO audit_scheme (project_id, content, template_id, status, create_by) VALUES
(1, '根据年度审计计划，对信息工程学院开展2024年度经济责任审计。审计范围：2024年度财务收支、预算执行、资产管理等情况。', '经责审计模板', 1, 'admin'),
(2, '对商学院2023年度财务收支进行审计。重点审查：收入真实性、支出合规性、预算执行情况。', '财务审计模板', 1, 'admin'),
(3, '对后勤处2024年度专项经费使用情况进行审计。', '专项审计模板', 1, 'admin')
ON CONFLICT DO NOTHING;

INSERT INTO audit_workpaper (project_id, category, title, content, basis_ids, status, create_by) VALUES
(1, '底稿', '采购招标情况底稿', '经抽查发现：信息工程学院2024年度有1项采购项目（金额85万元）未按规定进行公开招标。该采购项目于2024年3月立项，5月完成采购，采购方式为询价，未达到公开招标限额。', '13,14', 2, 'admin'),
(1, '取证记录', '预算执行率取证', '信息工程学院2024年度预算总额1200万元，实际支出780万元，执行率65%。低于学校规定的80%标准。', '6,7', 1, 'admin'),
(2, '底稿', '超预算支出底稿', '商学院2023年度存在部分支出未按预算科目执行的情况，超预算金额约8万元。', '7', 0, 'admin')
ON CONFLICT DO NOTHING;

INSERT INTO audit_review (workpaper_id, reviewer, level, opinion, status) VALUES
(1, 'admin', '主审', '底稿内容完整，依据引用准确，同意归档。', 1),
(1, 'admin', '项目组长', '核实无误，同意主审意见。', 1)
ON CONFLICT DO NOTHING;

INSERT INTO audit_report (project_id, title, version_type, content, file_url, status, create_by) VALUES
(1, '信息工程学院2024年度经济责任审计报告', '处内审核稿', '信息工程学院2024年度经济责任审计报告（初稿）\n\n一、基本情况\n信息工程学院2024年度预算1200万元，支出780万元。\n\n二、审计发现\n1. 采购未招标问题（金额85万元）\n2. 预算执行率仅65%\n3. 合同签订不规范\n\n三、审计评价\n该单位整体经济责任履行基本到位，但在采购管理、预算执行方面存在不足。', '/audit-template/default/audit-report-draft-template.docx', 1, 'admin')
ON CONFLICT DO NOTHING;

INSERT INTO audit_collab_log (project_id, user_id, action, target) VALUES
(1, 1, '创建方案', '信息工程学院经责审计方案'),
(1, 1, '上传底稿', '采购招标情况底稿'),
(1, 1, '提交复核', '采购招标情况底稿复核申请')
ON CONFLICT DO NOTHING;

-- 菜单注入
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1083, '项目工作台', 1062, 12, 'project-workspace', 'audit/project', '', '', 1, 0, 'C', '0', '0', 'audit:project:view', 'list', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1083);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1084, '底稿管理', 1062, 13, 'workpaper', 'audit/workpaper', '', '', 1, 0, 'C', '0', '0', 'audit:workpaper:view', 'documentation', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1084);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1085, '审计报告', 1062, 14, 'report', 'audit/report', '', '', 1, 0, 'C', '0', '0', 'audit:report:view', 'education', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1085);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1086, '底稿编辑', 1084, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'audit:workpaper:edit', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1086);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1087, '底稿复核', 1084, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'audit:workpaper:review', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1087);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1088, '报告编辑', 1085, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'audit:report:edit', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1088);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m WHERE m.menu_id BETWEEN 1083 AND 1088
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
