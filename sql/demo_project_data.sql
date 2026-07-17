-- ============================================================
-- 项目查询演示数据（模块一 审计信息管理 子集）
-- ============================================================
-- 执行前置：
-- 1. 已执行 PostgreSQL 基础库脚本 ry_20250522.sql。
-- 2. 已执行 ai_init_pg.sql，确保 audit_basis 已存在。
-- 3. 已执行 module_1.sql，确保 audit_plan 已存在。

DO $$
BEGIN
  IF to_regclass('audit_basis') IS NULL THEN
    RAISE EXCEPTION '缺少 audit_basis，请先执行 sql/ai_init_pg.sql。';
  END IF;
  IF to_regclass('audit_plan') IS NULL THEN
    RAISE EXCEPTION '缺少 audit_plan，请先执行 sql/module_1.sql。';
  END IF;
END $$;

-- 1. 审计项目表
CREATE TABLE IF NOT EXISTS audit_project (
  id              BIGSERIAL       PRIMARY KEY,
  project_name    VARCHAR(255)    NOT NULL,
  audited_unit    VARCHAR(255)    NOT NULL,
  audit_type      VARCHAR(64)     NOT NULL,
  audit_year      INT             NOT NULL,
  plan_id         BIGINT,
  status          SMALLINT        DEFAULT 0,
  create_time     TIMESTAMP       DEFAULT now(),
  update_time     TIMESTAMP
);
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS plan_id BIGINT;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS start_date DATE;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS end_date DATE;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS progress INT DEFAULT 0;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS phase VARCHAR(20) DEFAULT '准备';
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS is_overdue INT DEFAULT 0;
COMMENT ON TABLE audit_project IS '审计项目';
COMMENT ON COLUMN audit_project.project_name IS '项目名称';
COMMENT ON COLUMN audit_project.audited_unit IS '被审计单位';
COMMENT ON COLUMN audit_project.audit_type IS '审计类型：经责审计/财务收支/专项审计/工程审计';
COMMENT ON COLUMN audit_project.audit_year IS '审计年度';
COMMENT ON COLUMN audit_project.plan_id IS '关联计划ID';
COMMENT ON COLUMN audit_project.status IS '0未启动 1实施中 2已归档';

-- 2. 审计问题表
CREATE TABLE IF NOT EXISTS audit_issue (
  id              BIGSERIAL       PRIMARY KEY,
  project_id      BIGINT          NOT NULL REFERENCES audit_project(id),
  issue_desc      TEXT            NOT NULL,
  severity        SMALLINT        DEFAULT 1,
  basis_id        BIGINT          REFERENCES audit_basis(id),
  source          VARCHAR(64)     DEFAULT '审计发现',
  deadline        DATE,
  create_time     TIMESTAMP       DEFAULT now()
);
ALTER TABLE audit_issue ADD COLUMN IF NOT EXISTS source VARCHAR(64) DEFAULT '审计发现';
ALTER TABLE audit_issue ADD COLUMN IF NOT EXISTS deadline DATE;
COMMENT ON TABLE audit_issue IS '审计问题';
COMMENT ON COLUMN audit_issue.severity IS '1低 2中 3高';
COMMENT ON COLUMN audit_issue.basis_id IS '关联依据';
COMMENT ON COLUMN audit_issue.source IS '问题来源：审计发现/外部移送/巡视反馈';
COMMENT ON COLUMN audit_issue.deadline IS '整改截止日期';
CREATE INDEX idx_issue_project ON audit_issue(project_id);

-- 3. 整改记录表
CREATE TABLE IF NOT EXISTS audit_rectification (
  id              BIGSERIAL       PRIMARY KEY,
  issue_id        BIGINT          NOT NULL REFERENCES audit_issue(id),
  measure         TEXT            NOT NULL,
  status          SMALLINT        DEFAULT 0,
  finish_date     DATE,
  create_time     TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE audit_rectification IS '整改记录';
COMMENT ON COLUMN audit_rectification.status IS '0未整改 1整改中 2已完成';
CREATE INDEX idx_rect_issue ON audit_rectification(issue_id);

-- ============================================================
-- 种子数据
-- ============================================================

-- 项目
INSERT INTO audit_project (project_name, audited_unit, audit_type, audit_year, status) VALUES
('信息工程学院2024年经济责任审计', '信息工程学院', '经责审计', 2024, 2),
('商学院2023年财务收支审计', '商学院', '财务收支', 2023, 2),
('后勤处2024年专项审计', '后勤处', '专项审计', 2024, 2),
('财务处2024年预算执行审计', '财务处', '专项审计', 2024, 1),
('图书馆2025年工程审计', '图书馆', '工程审计', 2025, 0);

-- 关联项目到计划（建立 plan_id 关联）
UPDATE audit_project SET plan_id = 1 WHERE id = 1;
UPDATE audit_project SET plan_id = 2 WHERE id = 2;
UPDATE audit_project SET plan_id = 3 WHERE id = 3;
UPDATE audit_project SET plan_id = 4 WHERE id = 4;
UPDATE audit_project SET plan_id = 5 WHERE id = 5;

-- 项目进度演示数据：用于“项目进度”甘特图、进度概览和项目列表展示。
UPDATE audit_project
SET start_date = '2024-03-01', end_date = '2024-06-30', progress = 100, phase = '归档', is_overdue = 0, status = 2
WHERE id = 1;

UPDATE audit_project
SET start_date = '2023-09-01', end_date = '2023-12-20', progress = 100, phase = '归档', is_overdue = 0, status = 2
WHERE id = 2;

UPDATE audit_project
SET start_date = '2024-05-10', end_date = '2024-09-30', progress = 100, phase = '归档', is_overdue = 0, status = 2
WHERE id = 3;

UPDATE audit_project
SET start_date = '2026-02-20', end_date = '2026-06-30', progress = 82, phase = '整改跟踪', is_overdue = 1, status = 1
WHERE id = 4;

UPDATE audit_project
SET start_date = '2026-04-01', end_date = '2026-08-31', progress = 56, phase = '现场实施', is_overdue = 0, status = 1
WHERE id = 5;

UPDATE audit_project
SET project_name = '信息工程学院2026年预算执行审计',
    audited_unit = '信息工程学院',
    audit_type = '预算执行',
    audit_year = 2026,
    start_date = '2026-05-20',
    end_date = '2026-10-31',
    progress = 35,
    phase = '现场实施',
    is_overdue = 0,
    status = 1
WHERE id = 6;

UPDATE audit_project
SET project_name = '科研经费2026年专项审计',
    audited_unit = '科学技术研究院',
    audit_type = '专项审计',
    audit_year = 2026,
    start_date = '2026-09-01',
    end_date = '2026-12-15',
    progress = 0,
    phase = '准备',
    is_overdue = 0,
    status = 0
WHERE id = 7;

UPDATE audit_project
SET project_name = '资产经营公司2026年度财务收支审计',
    audited_unit = '资产经营公司',
    audit_type = '财务收支',
    audit_year = 2026,
    start_date = '2026-10-08',
    end_date = '2026-12-31',
    progress = 0,
    phase = '准备',
    is_overdue = 0,
    status = 0
WHERE id = 8;

-- 问题（ID从1开始）
INSERT INTO audit_issue (project_id, issue_desc, severity, basis_id) VALUES
(1, '采购项目未按规定进行公开招标，涉及金额85万元', 3, 13),
(1, '年度预算执行率仅65%，低于学校规定80%标准', 2, 6),
(1, '合同签订不规范，缺少法人签字和盖章', 2, 7),
(2, '部分支出未按预算科目执行，存在超预算支出', 2, 7),
(2, '往来款项长期挂账未清理，涉及金额12万元', 2, 6),
(3, '食堂采购未建立供应商比价制度', 2, 14),
(3, '固定资产台账与实际盘点不符', 1, 7),
(4, '专项资金未按规定用途使用', 3, 6),
(4, '科研经费报销缺少原始凭证', 2, 7);

-- 整改记录
INSERT INTO audit_rectification (issue_id, measure, status, finish_date) VALUES
(1, '已补办招标手续，补充公开招标公告', 2, '2025-01-15'),
(2, '已制定预算执行整改计划，加快执行进度', 1, NULL),
(3, '已补签合同手续，完善法人签字盖章', 2, '2025-02-20'),
(4, '已调账处理，按规定调整预算科目', 2, '2024-06-30'),
(5, '正在清理往来款项，预计2025年底完成', 1, NULL),
(6, '已建立供应商比价制度并报后勤处备案', 2, '2024-12-01'),
(7, '已组织全面资产盘点，补充未登记资产', 2, '2025-01-10'),
(8, '已整改，追回违规使用资金并调整预算', 2, '2024-11-30'),
(9, '正在补充报销凭证，通知经办人补交材料', 1, NULL);
