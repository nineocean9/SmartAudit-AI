-- 模块一：审计信息管理
-- 1. 审计计划
CREATE TABLE IF NOT EXISTS audit_plan (
  id              BIGSERIAL       PRIMARY KEY,
  plan_type       VARCHAR(64)     NOT NULL,
  plan_year       INT             NOT NULL,
  batch           VARCHAR(64),
  plan_name       VARCHAR(255),
  file_url        VARCHAR(512),
  status          SMALLINT        DEFAULT 0,
  create_time     TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE audit_plan IS '审计计划';
COMMENT ON COLUMN audit_plan.plan_type IS '计划类型：年度/专项/临时';
COMMENT ON COLUMN audit_plan.plan_year IS '计划年度';
COMMENT ON COLUMN audit_plan.batch IS '批次';
COMMENT ON COLUMN audit_plan.plan_name IS '计划名称';

-- 2. 审计对象（单位）
CREATE TABLE IF NOT EXISTS audit_unit (
  id              BIGSERIAL       PRIMARY KEY,
  unit_name       VARCHAR(255)    NOT NULL,
  unit_type       VARCHAR(64),
  profile         TEXT,
  history_audit   TEXT,
  create_time     TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE audit_unit IS '审计对象';
COMMENT ON COLUMN audit_unit.unit_name IS '单位名称';
COMMENT ON COLUMN audit_unit.unit_type IS '类型：学院/处室/直属';
COMMENT ON COLUMN audit_unit.profile IS '基本情况';
COMMENT ON COLUMN audit_unit.history_audit IS '历史审计情况';

-- 3. 领导干部
CREATE TABLE IF NOT EXISTS audit_leader (
  id              BIGSERIAL       PRIMARY KEY,
  name            VARCHAR(64)     NOT NULL,
  unit_id         BIGINT          REFERENCES audit_unit(id),
  position        VARCHAR(128),
  tenure_start    DATE,
  tenure_end      DATE,
  create_time     TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE audit_leader IS '领导干部';
COMMENT ON COLUMN audit_leader.name IS '姓名';
COMMENT ON COLUMN audit_leader.position IS '职务';

-- 种子数据
INSERT INTO audit_plan (plan_type, plan_year, batch, plan_name, status) VALUES
('年度计划', 2024, '第一批', '2024年度经济责任审计计划', 2),
('年度计划', 2024, '第二批', '2024年度财务收支审计计划', 2),
('专项计划', 2024, '', '2024年后勤专项审计计划', 2),
('年度计划', 2025, '第一批', '2025年度经济责任审计计划', 1),
('临时计划', 2025, '', '图书馆工程审计委托项目', 0)
ON CONFLICT DO NOTHING;

INSERT INTO audit_unit (unit_name, unit_type, profile, history_audit) VALUES
('信息工程学院', '学院', '成立于2002年，现有教职工120人，在校生2200人', '2024年经责审计；2022年财务收支审计'),
('商学院', '学院', '成立于2000年，教职工98人，在校生1800人', '2023年财务收支审计'),
('后勤处', '处室', '负责全校后勤保障，员工85人', '2024年专项审计'),
('财务处', '处室', '负责全校财务管理，员工32人', '2024年预算执行审计'),
('图书馆', '直属', '馆藏图书120万册，员工45人', '2025年工程审计')
ON CONFLICT DO NOTHING;

INSERT INTO audit_leader (name, unit_id, position, tenure_start, tenure_end) VALUES
('张三', 1, '信息工程学院院长', '2020-01-01', NULL),
('李四', 2, '商学院院长', '2021-06-01', NULL),
('王五', 3, '后勤处处长', '2019-03-01', '2024-12-31'),
('赵六', 4, '财务处处长', '2022-01-01', NULL)
ON CONFLICT DO NOTHING;

-- 菜单注入（AI智能辅助 1062 之下）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1078, '审计计划', 1062, 9, 'plan', 'audit/plan', '', '', 1, 0, 'C', '0', '0', 'audit:plan:view', 'date', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1078);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1079, '审计对象', 1062, 10, 'unit', 'audit/unit', '', '', 1, 0, 'C', '0', '0', 'audit:unit:view', 'user', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1079);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1080, '项目进度', 1062, 11, 'progress', 'audit/progress', '', '', 1, 0, 'C', '0', '0', 'audit:progress:view', 'chart', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1080);

-- 按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1081, '计划编辑', 1078, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'audit:plan:edit', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1081);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1082, '对象编辑', 1079, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'audit:unit:edit', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1082);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m WHERE m.menu_id BETWEEN 1078 AND 1082
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
