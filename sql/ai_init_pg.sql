-- ============================================================
-- 模块六 AI 智能审计辅助系统 · 数据底座
-- PostgreSQL 18 + pgvector v0.8.4
-- ============================================================

-- 1. 审计依据库
CREATE TABLE IF NOT EXISTS audit_basis (
  id              BIGSERIAL       PRIMARY KEY,
  category        VARCHAR(64)     NOT NULL DEFAULT '法规',
  title           VARCHAR(255)    NOT NULL,
  content         TEXT            NOT NULL,
  issue_org       VARCHAR(128),
  effective_date  DATE,
  status          SMALLINT        NOT NULL DEFAULT 1,
  version         INT             NOT NULL DEFAULT 1,
  embedding       vector(1024),
  create_by       VARCHAR(64)     DEFAULT '',
  create_time     TIMESTAMP       DEFAULT now(),
  update_by       VARCHAR(64)     DEFAULT '',
  update_time     TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE audit_basis IS '审计依据库';
COMMENT ON COLUMN audit_basis.id IS '主键';
COMMENT ON COLUMN audit_basis.category IS '分类：法规/制度/标准';
COMMENT ON COLUMN audit_basis.title IS '标题';
COMMENT ON COLUMN audit_basis.content IS '正文';
COMMENT ON COLUMN audit_basis.issue_org IS '颁发单位';
COMMENT ON COLUMN audit_basis.effective_date IS '生效日期';
COMMENT ON COLUMN audit_basis.status IS '状态(1生效 0失效)';
COMMENT ON COLUMN audit_basis.version IS '版本号';
COMMENT ON COLUMN audit_basis.embedding IS 'pgvector向量';
CREATE INDEX idx_basis_category ON audit_basis(category, status);

-- 2. AI调用日志
CREATE TABLE IF NOT EXISTS ai_call_log (
  id              BIGSERIAL       PRIMARY KEY,
  user_id         BIGINT,
  role_code       VARCHAR(64),
  intent          VARCHAR(32),
  prompt          TEXT,
  response        TEXT,
  cited_basis_ids VARCHAR(512),
  model_provider  VARCHAR(32),
  tokens_used     INT             DEFAULT 0,
  status          SMALLINT        DEFAULT 1,
  cost_time_ms    INT,
  create_time     TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE ai_call_log IS 'AI调用日志';
COMMENT ON COLUMN ai_call_log.intent IS '意图：问答/校验/匹配/取证单/风险';
COMMENT ON COLUMN ai_call_log.cited_basis_ids IS '引用依据ID(逗号分隔)';
COMMENT ON COLUMN ai_call_log.status IS '1成功 0失败';
CREATE INDEX idx_log_user ON ai_call_log(user_id);
CREATE INDEX idx_log_time ON ai_call_log(create_time);

-- 3. AI调用配额
CREATE TABLE IF NOT EXISTS ai_call_quota (
  id                BIGSERIAL       PRIMARY KEY,
  role_code         VARCHAR(64)     NOT NULL,
  daily_limit       INT             DEFAULT 100,
  allowed_functions VARCHAR(255),
  create_time       TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE ai_call_quota IS 'AI调用配额';
INSERT INTO ai_call_quota (role_code, daily_limit, allowed_functions) VALUES
  ('school_leader',         500, 'ask,match,check,forensic,risk'),
  ('audit_director',        500, 'ask,match,check,forensic,risk'),
  ('audit_project_leader',  300, 'ask,match,check,forensic,risk'),
  ('audit_staff',           200, 'ask,match,check'),
  ('audited_unit_principal', 80, 'ask,match'),
  ('audited_unit_liaison',   80, 'ask,match'),
  ('intermediary_auditor',  150, 'ask,match,check'),
  ('admin',                9999, 'all')
ON CONFLICT DO NOTHING;

-- 4. 文档校验任务
CREATE TABLE IF NOT EXISTS doc_check_task (
  id              BIGSERIAL       PRIMARY KEY,
  project_id      BIGINT,
  uploader        BIGINT,
  file_name       VARCHAR(255),
  file_path       VARCHAR(512),
  status          SMALLINT        DEFAULT 0,
  issues_json     TEXT,
  create_time     TIMESTAMP       DEFAULT now(),
  finish_time     TIMESTAMP
);
COMMENT ON TABLE doc_check_task IS '文档校验任务';
COMMENT ON COLUMN doc_check_task.status IS '0待处理 1处理中 2完成 3失败';

-- 5. 取证单草稿
CREATE TABLE IF NOT EXISTS forensic_draft (
  id              BIGSERIAL       PRIMARY KEY,
  project_id      BIGINT,
  issue           TEXT,
  basis_ids       VARCHAR(512),
  suggestion      TEXT,
  review_status   SMALLINT        DEFAULT 0,
  review_log      TEXT,
  create_by       VARCHAR(64),
  create_time     TIMESTAMP       DEFAULT now(),
  update_by       VARCHAR(64),
  update_time     TIMESTAMP
);
COMMENT ON TABLE forensic_draft IS '取证单草稿';
COMMENT ON COLUMN forensic_draft.review_status IS '0草稿 1主审 2项目组长 3审计处长 9已归档';

-- 6. 风险线索
CREATE TABLE IF NOT EXISTS risk_clue (
  id              BIGSERIAL       PRIMARY KEY,
  project_id      BIGINT,
  clue_type       VARCHAR(32),
  content         TEXT,
  severity        SMALLINT        DEFAULT 1,
  status          SMALLINT        DEFAULT 0,
  create_time     TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE risk_clue IS '风险线索';
COMMENT ON COLUMN risk_clue.clue_type IS '时间矛盾/依据缺失/金额波动/签字缺失';
COMMENT ON COLUMN risk_clue.severity IS '1低 2中 3高';
COMMENT ON COLUMN risk_clue.status IS '0待核 1已核 2已转取证';

-- 7. 向量化同步日志
CREATE TABLE IF NOT EXISTS ai_embedding_log (
  id              BIGSERIAL       PRIMARY KEY,
  basis_id        BIGINT          NOT NULL,
  embedding_dim   INT,
  model           VARCHAR(64),
  tokens_used     INT             DEFAULT 0,
  update_time     TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE ai_embedding_log IS '向量化同步日志';

-- ============================================================
-- pgvector 向量索引（先插入种子数据再建索引）
-- ============================================================

-- 种子依据数据（审计法规）
INSERT INTO audit_basis (category, title, content, issue_org, effective_date, status) VALUES
('法规', '中华人民共和国审计法 第十六条',
 '审计机关对本级各部门（含直属单位）和下级政府预算的执行情况和决算以及其他财政收支情况，进行审计监督。',
 '全国人大常委会', '2022-01-01', 1),
('法规', '中华人民共和国审计法 第十八条',
 '审计机关对国有金融机构的资产、负债、损益，进行审计监督。',
 '全国人大常委会', '2022-01-01', 1),
('法规', '中华人民共和国审计法 第二十一条',
 '审计机关对国有资本占控股地位或者主导地位的企业、金融机构的资产、负债、损益，进行审计监督。',
 '全国人大常委会', '2022-01-01', 1),
('制度', '教育系统内部审计工作规定 第十二条',
 '内部审计机构应当对所属单位财务收支及有关经济活动进行审计监督。审计内容包括：预算执行、财务收支、资产管理、专项资金使用等。',
 '教育部', '2021-05-01', 1),
('制度', '教育系统内部审计工作规定 第十五条',
 '内部审计机构应当对所属单位领导干部履行经济责任情况进行审计。经济责任审计内容包括：贯彻执行重大经济方针政策、重大经济事项决策执行、财政财务收支、固定资产管理等。',
 '教育部', '2021-05-01', 1),
('制度', '高等学校财务制度 第四十二条',
 '高等学校各项支出应当严格执行国家有关财务规章制度规定的开支范围及开支标准；国家没有统一规定的，由学校结合实际情况制定，报主管部门备案。',
 '教育部', '2022-01-01', 1),
('制度', '高等学校财务制度 第五十一条',
 '高等学校应当建立健全内部控制制度，对经济活动的风险进行防范和管控。内部控制主要包括：预算管理、收支管理、采购管理、资产管理、建设项目管理、合同管理等。',
 '教育部', '2022-01-01', 1),
('标准', '高校劳务费管理办法 第六条',
 '劳务费标准：教授每学时不超过200元，副教授每学时不超过150元，讲师每学时不超过100元。超出标准部分需单独审批并附说明。',
 '教育部', '2023-01-01', 1),
('标准', '高校劳务费管理办法 第八条',
 '劳务费发放须提供劳务协议、工作量证明、银行卡号等材料。单笔劳务费超过5000元的，需经部门负责人审批；超过10000元的，需经分管校领导审批。',
 '教育部', '2023-01-01', 1),
('法规', '中国共产党发展党员工作细则 第十三条',
 '入党申请人应当向工作、学习所在单位党组织提出入党申请。党组织收到入党申请书后，应当在一个月内派人同入党申请人谈话。',
 '中共中央办公厅', '2024-06-01', 1),
('法规', '中国共产党发展党员工作细则 第十五条',
 '入党积极分子经过一年以上培养教育和考察，基本具备党员条件的，在听取党小组、培养联系人、党员和群众意见的基础上，支部委员会讨论同意并报上级党委备案后，可列为发展对象。',
 '中共中央办公厅', '2024-06-01', 1),
('法规', '中国共产党发展党员工作细则 第二十二条',
 '支部大会讨论接收预备党员时，赞成人数超过应到会有表决权的正式党员的半数，才能通过接收预备党员的决议。因故不能到会的有表决权的正式党员，在支部大会召开前正式向党支部提出书面意见的，应当统计在票数内。',
 '中共中央办公厅', '2024-06-01', 1),
('制度', '政府采购货物和服务招标投标管理办法 第六十五条',
 '评标委员会发现招标文件存在歧义、重大缺陷导致评标工作无法进行，或者招标文件内容违反国家有关强制性规定的，应当停止评标工作，与采购人或者采购代理机构沟通并作书面记录。',
 '财政部', '2023-01-01', 1),
('制度', '政府采购货物和服务招标投标管理办法 第七十七条',
 '采购人有下列情形之一的，由财政部门责令限期改正；情节严重的，给予警告，对直接负责的主管人员和其他直接责任人员依法给予处分：（一）未按照规定编制政府采购预算的；（二）未按照规定执行政府采购政策的；（三）未按照规定选择政府采购方式的；（四）未按照规定组织采购活动的。',
 '财政部', '2023-01-01', 1),
('标准', '行政事业单位内部控制规范 第二十八条',
 '单位应当建立健全内部审计制度。内部审计机构应当独立于财务部门，对单位经济活动进行审计监督。审计发现的问题应当及时向单位负责人报告，并督促整改落实。',
 '财政部', '2023-01-01', 1)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 菜单权限注入（模块六 + 模块八）
-- ============================================================

-- 一级菜单：AI智能辅助 (menu_id=1062)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1062, 'AI智能辅助', 0, 6, 'ai', NULL, '', '', 1, 0, 'M', '0', '0', '', 'guide', 'admin', now(), '', NULL, 'AI智能辅助目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1062);

-- 子菜单：依据库管理 (menu_id=1063)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1063, '依据库管理', 1062, 1, 'basis', 'ai/basis', '', '', 1, 0, 'C', '0', '0', 'ai:basis:query', 'documentation', 'admin', now(), '', NULL, '依据库管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1063);

-- 依据库按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1064, '依据库新增', 1063, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'ai:basis:add', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1064);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1065, '依据库修改', 1063, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'ai:basis:edit', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1065);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1066, '依据库删除', 1063, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'ai:basis:remove', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1066);

-- 子菜单：文档校验 (menu_id=1067)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1067, '文档校验', 1062, 2, 'docCheck', 'ai/docCheck', '', '', 1, 0, 'C', '0', '0', 'ai:doc:check', 'check', 'admin', now(), '', NULL, '文档校验菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1067);

-- 子菜单：风险线索 (menu_id=1068)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1068, '风险线索', 1062, 3, 'riskClue', 'ai/riskClue', '', '', 1, 0, 'C', '0', '0', 'ai:risk:view', 'warning', 'admin', now(), '', NULL, '风险线索菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1068);

-- 子菜单：取证单 (menu_id=1069)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1069, '取证单', 1062, 4, 'forensic', 'ai/forensic', '', '', 1, 0, 'C', '0', '0', 'ai:forensic:view', 'form', 'admin', now(), '', NULL, '取证单菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1069);

-- 取证单按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1070, '取证单生成', 1069, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'ai:forensic:gen', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1070);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1071, '取证单复核', 1069, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'ai:forensic:review', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1071);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 12124, '取证单提交复核', 1069, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'ai:forensic:submit', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:forensic:submit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 12125, '取证单删除', 1069, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'ai:forensic:delete', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:forensic:delete');

-- 子菜单：调用日志 (menu_id=1072)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1072, '调用日志', 1062, 5, 'aiLog', 'ai/aiLog', '', '', 1, 0, 'C', '0', '0', 'ai:log:view', 'log', 'admin', now(), '', NULL, '调用日志菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1072);

-- 配额配置按钮权限 (menu_id=1073)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1073, '配额配置', 1062, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'ai:quota:config', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1073);

-- 角色-菜单关联（管理员角色 menu_id=1，关联所有新菜单）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m WHERE m.menu_id BETWEEN 1062 AND 1073
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
