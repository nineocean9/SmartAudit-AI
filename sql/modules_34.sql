-- ============================================================
-- 模块三：审计整改管理扩展 + 模块四：审计依据三库
-- ============================================================

-- 模块三：扩展已有表
ALTER TABLE audit_issue ADD COLUMN IF NOT EXISTS source VARCHAR(64) DEFAULT '审计发现';
ALTER TABLE audit_issue ADD COLUMN IF NOT EXISTS deadline DATE;
COMMENT ON COLUMN audit_issue.source IS '问题来源：审计发现/外部移送/巡视反馈';
COMMENT ON COLUMN audit_issue.deadline IS '整改截止日期';

ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS feedback TEXT;
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS evaluator VARCHAR(64);
COMMENT ON COLUMN audit_rectification.feedback IS '整改反馈';
COMMENT ON COLUMN audit_rectification.evaluator IS '跟踪评价人';

-- 整改提醒表
CREATE TABLE IF NOT EXISTS rectification_remind (
  id              BIGSERIAL       PRIMARY KEY,
  issue_id        BIGINT          NOT NULL REFERENCES audit_issue(id),
  remind_type     VARCHAR(32)     DEFAULT 'auto',
  remind_time     TIMESTAMP,
  status          SMALLINT        DEFAULT 0
);
COMMENT ON TABLE rectification_remind IS '整改提醒';
COMMENT ON COLUMN rectification_remind.remind_type IS 'auto自动/manual手动';
COMMENT ON COLUMN rectification_remind.status IS '0未处理 1已处理';

-- 模块四：问题风险库
CREATE TABLE IF NOT EXISTS audit_risk_case (
  id              BIGSERIAL       PRIMARY KEY,
  risk_name       VARCHAR(255)    NOT NULL,
  risk_desc       TEXT,
  basis_ids       VARCHAR(512),
  scenario        VARCHAR(255),
  create_time     TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE audit_risk_case IS '问题风险库';
COMMENT ON COLUMN audit_risk_case.risk_name IS '风险名称';
COMMENT ON COLUMN audit_risk_case.risk_desc IS '风险描述';
COMMENT ON COLUMN audit_risk_case.basis_ids IS '关联依据ID';
COMMENT ON COLUMN audit_risk_case.scenario IS '适用场景';

-- 模块四：案例库
CREATE TABLE IF NOT EXISTS audit_case_lib (
  id              BIGSERIAL       PRIMARY KEY,
  case_title      VARCHAR(255)    NOT NULL,
  case_content    TEXT,
  category        VARCHAR(64),
  reference       VARCHAR(512),
  create_time     TIMESTAMP       DEFAULT now()
);
COMMENT ON TABLE audit_case_lib IS '案例库';
COMMENT ON COLUMN audit_case_lib.case_title IS '案例标题';
COMMENT ON COLUMN audit_case_lib.case_content IS '案例内容';
COMMENT ON COLUMN audit_case_lib.category IS '分类';
COMMENT ON COLUMN audit_case_lib.reference IS '参考依据';

-- 种子数据：风险
INSERT INTO audit_risk_case (risk_name, risk_desc, basis_ids, scenario) VALUES
('采购未招标', '采购项目未按规定进行公开招标，存在围标串标风险', '13,14', '采购审计'),
('预算执行率低', '年度预算执行率低于规定标准，资金使用效率低', '6,7', '预算审计'),
('合同签订不规范', '合同缺少法人签字、盖章或关键条款缺失', '7', '经济合同审计'),
('往来款项长挂账', '往来款项长期挂账未清理，存在坏账风险', '6', '财务收支审计'),
('固定资产账实不符', '固定资产台账与实际盘点结果不一致', '7', '资产管理审计')
ON CONFLICT DO NOTHING;

-- 种子数据：案例
INSERT INTO audit_case_lib (case_title, case_content, category, reference) VALUES
('某学院采购设备未招标案例', '审计发现某学院采购教学设备85万元未进行公开招标，直接指定供应商。处理：要求补办招标手续，对相关责任人进行约谈。', '采购审计', '政府采购法'),
('某部门预算执行率过低案例', '某部门年度预算执行率仅52%，其中办公设备购置预算执行率不足30%。审计建议：加强预算编制科学性，定期通报执行进度。', '预算审计', '预算法'),
('某单位合同管理混乱案例', '抽查20份合同发现：5份无法人签字、3份无盖章、2份关键条款缺失。审计建议：建立合同会签制度，指定专人管理。', '经济合同审计', '合同法'),
('科研经费报销违规案例', '科研经费报销中发现部分发票与科研内容无关，存在套取科研经费嫌疑。审计建议：完善报销审核流程，严格执行经费管理办法。', '科研审计', '科研经费管理办法')
ON CONFLICT DO NOTHING;
