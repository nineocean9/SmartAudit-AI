-- ============================================================
-- 项目查询演示数据（模块一 审计信息管理 子集）
-- ============================================================

-- 1. 审计项目表
CREATE TABLE IF NOT EXISTS audit_project (
  id              BIGSERIAL       PRIMARY KEY,
  project_name    VARCHAR(255)    NOT NULL,
  audited_unit    VARCHAR(255)    NOT NULL,
  audit_type      VARCHAR(64)     NOT NULL,
  audit_year      INT             NOT NULL,
  status          SMALLINT        DEFAULT 0,
  create_time     TIMESTAMP       DEFAULT now(),
  update_time     TIMESTAMP
);
COMMENT ON TABLE audit_project IS '审计项目';
COMMENT ON COLUMN audit_project.project_name IS '项目名称';
COMMENT ON COLUMN audit_project.audited_unit IS '被审单位';
COMMENT ON COLUMN audit_project.audit_type IS '审计类型：经责审计/财务收支/专项审计/工程审计';
COMMENT ON COLUMN audit_project.audit_year IS '审计年度';
COMMENT ON COLUMN audit_project.status IS '0未启动 1实施中 2已归档';

-- 2. 审计问题表
CREATE TABLE IF NOT EXISTS audit_issue (
  id              BIGSERIAL       PRIMARY KEY,
  project_id      BIGINT          NOT NULL REFERENCES audit_project(id),
  issue_desc      TEXT            NOT NULL,
  severity        SMALLINT        DEFAULT 1,
  basis_id        BIGINT          REFERENCES audit_basis(id),
  create_time     TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE audit_issue IS '审计问题';
COMMENT ON COLUMN audit_issue.severity IS '1低 2中 3高';
COMMENT ON COLUMN audit_issue.basis_id IS '关联依据';
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
