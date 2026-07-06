-- ============================================================
-- AI 模块 PostgreSQL DDL
-- 从 MySQL 迁移至 PostgreSQL
-- ============================================================

-- AI 对话会话表
CREATE TABLE IF NOT EXISTS ai_conversation (
  id           BIGSERIAL       PRIMARY KEY,
  title        VARCHAR(100)    NOT NULL DEFAULT '新对话',
  user_id      BIGINT          NOT NULL,
  model        VARCHAR(50)     NOT NULL DEFAULT 'qwen-plus',
  status       SMALLINT        NOT NULL DEFAULT 1,
  create_by    VARCHAR(64)     DEFAULT '',
  create_time  TIMESTAMP       DEFAULT NULL,
  update_time  TIMESTAMP       DEFAULT NULL
);
COMMENT ON TABLE ai_conversation IS 'AI对话会话表';
COMMENT ON COLUMN ai_conversation.id IS '会话id';
COMMENT ON COLUMN ai_conversation.title IS '会话标题';
COMMENT ON COLUMN ai_conversation.user_id IS '用户id';
COMMENT ON COLUMN ai_conversation.model IS '使用模型';
COMMENT ON COLUMN ai_conversation.status IS '状态(1正常 0删除)';
COMMENT ON COLUMN ai_conversation.create_by IS '创建者';
COMMENT ON COLUMN ai_conversation.create_time IS '创建时间';
COMMENT ON COLUMN ai_conversation.update_time IS '更新时间';
CREATE INDEX idx_conv_user_id ON ai_conversation(user_id);

-- AI 对话消息表
CREATE TABLE IF NOT EXISTS ai_message (
  id              BIGSERIAL       PRIMARY KEY,
  conversation_id BIGINT          NOT NULL,
  role            VARCHAR(20)     NOT NULL,
  content         TEXT            NOT NULL,
  tokens          INT             DEFAULT 0,
  create_time     TIMESTAMP       DEFAULT NULL
);
COMMENT ON TABLE ai_message IS 'AI对话消息表';
COMMENT ON COLUMN ai_message.id IS '消息id';
COMMENT ON COLUMN ai_message.conversation_id IS '会话id';
COMMENT ON COLUMN ai_message.role IS '角色(user/assistant)';
COMMENT ON COLUMN ai_message.content IS '消息内容';
COMMENT ON COLUMN ai_message.tokens IS '消耗token数';
COMMENT ON COLUMN ai_message.create_time IS '创建时间';
CREATE INDEX idx_msg_conversation_id ON ai_message(conversation_id);

-- ============================================================
-- 菜单注入
-- ============================================================

-- 注意：需要先执行 ry_20250522.sql 创建 sys_menu 表
-- 如果菜单已存在则跳过（menu_id=1061 为已有）

-- 确保 AI 对话菜单存在（不存在则插入）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1061, 'AI对话', 0, 0, 'aiChat', 'ai/chat', '', '', 1, 0, 'C', '0', '0', '', 'user', 'admin', now(), '', NULL, 'AI对话地址'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1061);
