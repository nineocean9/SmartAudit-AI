-- ============================================================
-- AI 工作台 · 知识库基础设施 DDL
-- PostgreSQL 18 + pgvector v0.8.4
-- ============================================================

-- 1. 项目文档表
CREATE TABLE IF NOT EXISTS project_document (
  id              BIGSERIAL       PRIMARY KEY,
  project_id      BIGINT          NOT NULL,
  plan_id         BIGINT,
  doc_type        VARCHAR(32)     NOT NULL DEFAULT '其他',
  file_name       VARCHAR(255)    NOT NULL,
  file_path       VARCHAR(512),
  file_size       BIGINT          DEFAULT 0,
  file_ext        VARCHAR(16),
  content_text    TEXT,
  status          SMALLINT        DEFAULT 1,
  chunk_count     INT             DEFAULT 0,
  create_by       VARCHAR(64)     DEFAULT '',
  create_time     TIMESTAMP       DEFAULT now(),
  update_time     TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE project_document IS '项目文档';
COMMENT ON COLUMN project_document.project_id IS '关联审计项目ID';
COMMENT ON COLUMN project_document.plan_id IS '关联审计计划ID';
COMMENT ON COLUMN project_document.doc_type IS '资料类型: 审计通知书/审计方案/底稿/报告/整改资料/财务数据/合同/发票/其他';
COMMENT ON COLUMN project_document.content_text IS '解析后的纯文本(用于关键词检索兜底)';
COMMENT ON COLUMN project_document.chunk_count IS '切块数量';
COMMENT ON COLUMN project_document.status IS '状态(1正常 0删除)';
CREATE INDEX IF NOT EXISTS idx_pd_project ON project_document(project_id);
CREATE INDEX IF NOT EXISTS idx_pd_plan ON project_document(plan_id);
CREATE INDEX IF NOT EXISTS idx_pd_type ON project_document(project_id, doc_type);

-- 2. 文档切块表
CREATE TABLE IF NOT EXISTS document_chunk (
  id              BIGSERIAL       PRIMARY KEY,
  document_id     BIGINT          NOT NULL,
  chunk_index     INT             NOT NULL DEFAULT 0,
  content         TEXT            NOT NULL,
  token_count     INT             DEFAULT 0,
  embedding       vector(1024),
  create_time     TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE document_chunk IS '文档向量切块';
COMMENT ON COLUMN document_chunk.document_id IS '关联 project_document.id';
COMMENT ON COLUMN document_chunk.chunk_index IS '切块序号(从0开始)';
COMMENT ON COLUMN document_chunk.content IS '切块文本内容';
COMMENT ON COLUMN document_chunk.embedding IS 'pgvector 1024维向量';
CREATE INDEX IF NOT EXISTS idx_dc_document ON document_chunk(document_id);

-- 3. 临时工作区表
CREATE TABLE IF NOT EXISTS temporary_workspace (
  id              BIGSERIAL       PRIMARY KEY,
  session_id      VARCHAR(64)     NOT NULL,
  user_id         BIGINT,
  file_name       VARCHAR(255),
  file_path       VARCHAR(512),
  content_text    TEXT,
  status          SMALLINT        DEFAULT 1,
  expire_time     TIMESTAMP       DEFAULT (now() + interval '2 hours'),
  create_time     TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE temporary_workspace IS '临时工作区(会话级)';
COMMENT ON COLUMN temporary_workspace.session_id IS '前端会话标识(UUID)';
COMMENT ON COLUMN temporary_workspace.expire_time IS '自动过期时间(默认2小时)';
COMMENT ON COLUMN temporary_workspace.status IS '1活跃 0已销毁';
CREATE INDEX IF NOT EXISTS idx_tw_session ON temporary_workspace(session_id);
CREATE INDEX IF NOT EXISTS idx_tw_expire ON temporary_workspace(expire_time);

-- 4. ai_conversation 扩展字段
ALTER TABLE ai_conversation ADD COLUMN IF NOT EXISTS workspace_mode VARCHAR(32) DEFAULT 'general';
COMMENT ON COLUMN ai_conversation.workspace_mode IS '工作模式: general/policy/project/data';

ALTER TABLE ai_conversation ADD COLUMN IF NOT EXISTS project_id BIGINT;
COMMENT ON COLUMN ai_conversation.project_id IS '当前关联的审计项目ID';

ALTER TABLE ai_conversation ADD COLUMN IF NOT EXISTS temp_session_id VARCHAR(64);
COMMENT ON COLUMN ai_conversation.temp_session_id IS '临时工作区会话ID';

-- 5. audit_project 扩展字段
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS plan_id BIGINT;
COMMENT ON COLUMN audit_project.plan_id IS '关联审计计划ID';

ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS doc_count INT DEFAULT 0;
COMMENT ON COLUMN audit_project.doc_count IS '关联文档数量';

-- ============================================================
-- 菜单权限注入
-- ============================================================

-- 项目文档管理 (menu_id=1089)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1089, '项目文档', 1062, 15, 'projectDoc', 'audit/projectLib', '', '', 1, 0, 'C', '1', '1', 'audit:projectDoc:view', 'documentation', 'admin', now(), '', NULL, '项目文档管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1089);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1090, '项目文档上传', 1089, 1, '', '', '', '', 1, 0, 'F', '1', '1', 'audit:projectDoc:upload', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1090);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1091, '项目文档删除', 1089, 2, '', '', '', '', 1, 0, 'F', '1', '1', 'audit:projectDoc:remove', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1091);

-- 上传中心 (menu_id=1093)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1093, '上传中心', 1062, 17, 'upload', 'knowledge/upload', '', '', 1, 0, 'C', '0', '0', 'audit:upload:view', 'upload', 'admin', now(), '', NULL, '上传中心'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1093);

-- 项目库 (menu_id=1094)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1094, '项目库', 1062, 18, 'projectLib', 'audit/projectLib', '', '', 1, 0, 'C', '0', '0', 'audit:projectLib:view', 'tree-table', 'admin', now(), '', NULL, '项目库(项目管理)'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1094);

-- 管理员角色关联所有新菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m WHERE m.menu_id BETWEEN 1089 AND 1094
AND m.menu_id <> 1092
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
