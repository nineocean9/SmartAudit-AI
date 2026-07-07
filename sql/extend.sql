-- ============================================================
-- 联动优化 · 表结构扩展
-- 闭环⓪：审计计划/对象实质功能 + 闭环①⑤字段
-- ============================================================

-- 闭环⓪：审计方案关联计划
ALTER TABLE audit_scheme ADD COLUMN IF NOT EXISTS plan_id BIGINT;
COMMENT ON COLUMN audit_scheme.plan_id IS '关联审计计划ID';

-- 闭环⓪：计划-项目多对多关联表
CREATE TABLE IF NOT EXISTS audit_plan_project (
  id              BIGSERIAL       PRIMARY KEY,
  plan_id         BIGINT          NOT NULL REFERENCES audit_plan(id),
  project_id      BIGINT          NOT NULL REFERENCES audit_project(id),
  bind_time       TIMESTAMP       DEFAULT now(),
  UNIQUE(plan_id, project_id)
);
COMMENT ON TABLE audit_plan_project IS '计划-项目关联';

-- 闭环①：底稿关联问题
ALTER TABLE audit_workpaper ADD COLUMN IF NOT EXISTS issue_id BIGINT;
COMMENT ON COLUMN audit_workpaper.issue_id IS '关联问题ID（复核转问题）';

-- 闭环①⑤：依据引用次数
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS ref_count INT DEFAULT 0;
COMMENT ON COLUMN audit_basis.ref_count IS '被引用次数';

-- 闭环③：审计通知书
CREATE TABLE IF NOT EXISTS audit_notice (
  id              BIGSERIAL       PRIMARY KEY,
  project_id      BIGINT          REFERENCES audit_project(id),
  recipient       VARCHAR(255),
  content         TEXT,
  file_url        VARCHAR(512),
  send_time       TIMESTAMP       DEFAULT now(),
  status          SMALLINT        DEFAULT 1
);
COMMENT ON TABLE audit_notice IS '审计通知书';
COMMENT ON COLUMN audit_notice.recipient IS '送达对象';
COMMENT ON COLUMN audit_notice.status IS '1已发送 0草稿';

-- 闭环③：协同日志 insert 支持（表已存在，仅确认）
