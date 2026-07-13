-- ============================================================
-- v2_feature_enhance.sql — 功能增强SQL（全量）
-- 适用于 PostgreSQL + pgvector
-- ============================================================

-- ===== 1. 审计计划表扩展 =====
ALTER TABLE audit_plan ADD COLUMN IF NOT EXISTS plan_start_date DATE;
ALTER TABLE audit_plan ADD COLUMN IF NOT EXISTS plan_end_date DATE;
ALTER TABLE audit_plan ADD COLUMN IF NOT EXISTS approval_status INT DEFAULT 0;
ALTER TABLE audit_plan ADD COLUMN IF NOT EXISTS description TEXT;

-- ===== 2. 审计计划附件表 =====
CREATE TABLE IF NOT EXISTS audit_plan_attachment (
    id            BIGSERIAL PRIMARY KEY,
    plan_id       BIGINT       NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    file_path     VARCHAR(500) NOT NULL,
    file_type     VARCHAR(20),
    file_size     BIGINT,
    attachment_type VARCHAR(50),
    create_by     VARCHAR(64),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 3. 审计方案模板表 =====
CREATE TABLE IF NOT EXISTS audit_scheme_template (
    id            BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(200) NOT NULL,
    audit_type    VARCHAR(50)  NOT NULL,
    content       TEXT,
    status        INT DEFAULT 1,
    create_by     VARCHAR(64),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 4. 计划变更记录表 =====
CREATE TABLE IF NOT EXISTS audit_plan_change_log (
    id            BIGSERIAL PRIMARY KEY,
    plan_id       BIGINT       NOT NULL,
    change_type   VARCHAR(20)  NOT NULL,
    before_json   TEXT,
    after_json    TEXT,
    change_reason VARCHAR(500),
    change_by     VARCHAR(64),
    change_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 5. 审计项目表扩展 =====
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS leader_id      BIGINT;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS leader_name    VARCHAR(64);
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS start_date     DATE;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS end_date       DATE;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS coverage_start DATE;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS coverage_end   DATE;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS is_outsourced  INT DEFAULT 0;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS budget         DECIMAL(14,2);
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS progress       INT DEFAULT 0;
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS phase          VARCHAR(20) DEFAULT '准备';
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS is_overdue     INT DEFAULT 0;

-- ===== 6. 被审单位表扩展 =====
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS unit_code       VARCHAR(50);
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS parent_leader   VARCHAR(100);
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS staff_count     INT;
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS annual_budget   DECIMAL(14,2);
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS finance_contact VARCHAR(100);
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS contact_phone   VARCHAR(20);
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS address         VARCHAR(200);
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS last_audit_date DATE;
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS status          INT DEFAULT 1;

-- ===== 7. 领导干部表扩展 =====
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS gender         VARCHAR(10);
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS id_number      VARCHAR(20);
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS managed_funds  DECIMAL(14,2);
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS managed_scope  VARCHAR(500);
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS position_history TEXT;
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS audit_evaluation TEXT;

-- ===== 8. 单位变更日志表 =====
CREATE TABLE IF NOT EXISTS audit_unit_change_log (
    id           BIGSERIAL PRIMARY KEY,
    unit_id      BIGINT      NOT NULL,
    field_name   VARCHAR(50) NOT NULL,
    old_value    TEXT,
    new_value    TEXT,
    change_by    VARCHAR(64),
    change_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 9. 项目成员分工表 =====
CREATE TABLE IF NOT EXISTS audit_project_member (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    user_name    VARCHAR(64),
    role_type    VARCHAR(20) NOT NULL,
    task_scope   VARCHAR(500),
    task_deadline DATE,
    status       INT DEFAULT 0,
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 10. 审前资料清单表 =====
CREATE TABLE IF NOT EXISTS audit_material_checklist (
    id            BIGSERIAL PRIMARY KEY,
    project_id    BIGINT      NOT NULL,
    material_name VARCHAR(200) NOT NULL,
    material_type VARCHAR(50),
    required      INT DEFAULT 1,
    submit_status INT DEFAULT 0,
    file_path     VARCHAR(500),
    submit_by     VARCHAR(64),
    submit_time   TIMESTAMP,
    source        VARCHAR(20) DEFAULT 'unit',
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 11. 报告意见交互表 =====
CREATE TABLE IF NOT EXISTS audit_report_opinion (
    id           BIGSERIAL PRIMARY KEY,
    report_id    BIGINT      NOT NULL,
    round_no     INT DEFAULT 1,
    opinion_type VARCHAR(20),
    content      TEXT,
    attachment   VARCHAR(500),
    submit_by    VARCHAR(64),
    submit_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 12. 报告版本快照表 =====
CREATE TABLE IF NOT EXISTS audit_report_version (
    id           BIGSERIAL PRIMARY KEY,
    report_id    BIGINT NOT NULL,
    version_no   INT    NOT NULL,
    content      TEXT,
    change_desc  VARCHAR(500),
    create_by    VARCHAR(64),
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 13. 报告表扩展 =====
ALTER TABLE audit_report ADD COLUMN IF NOT EXISTS title          VARCHAR(200);
ALTER TABLE audit_report ADD COLUMN IF NOT EXISTS version_no     INT DEFAULT 1;
ALTER TABLE audit_report ADD COLUMN IF NOT EXISTS opinion_status INT DEFAULT 0;

-- ===== 14. 项目归档表 =====
CREATE TABLE IF NOT EXISTS audit_archive (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    archive_no      VARCHAR(50),
    archive_status  INT DEFAULT 0,
    archive_category VARCHAR(20),
    file_name       VARCHAR(200),
    file_path       VARCHAR(500),
    sort_order      INT DEFAULT 0,
    review_by       VARCHAR(64),
    review_time     TIMESTAMP,
    archive_time    TIMESTAMP,
    create_by       VARCHAR(64),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 15. 法条关联关系表 =====
CREATE TABLE IF NOT EXISTS audit_basis_relation (
    id            BIGSERIAL PRIMARY KEY,
    basis_id      BIGINT     NOT NULL,
    related_id    BIGINT     NOT NULL,
    relation_type VARCHAR(20) NOT NULL,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 16. 审计依据表扩展 =====
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS doc_number      VARCHAR(100);
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS audit_scope     VARCHAR(200);
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS fund_type       VARCHAR(100);
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS expire_date     DATE;
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS hierarchy_level VARCHAR(20);

-- ===== 17. 中介临时授权表 =====
CREATE TABLE IF NOT EXISTS audit_temp_auth (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    project_id   BIGINT NOT NULL,
    auth_type    VARCHAR(20) DEFAULT 'outsource',
    start_date   DATE NOT NULL,
    expire_date  DATE NOT NULL,
    status       INT DEFAULT 1,
    create_by    VARCHAR(64),
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 18. 整改表扩展 =====
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS rectify_plan    TEXT;
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS delay_reason    VARCHAR(500);
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS delay_status    INT DEFAULT 0;
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS review_result   VARCHAR(20);
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS review_comment  TEXT;
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS review_by       VARCHAR(64);
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS review_time     TIMESTAMP;
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS amount_involved DECIMAL(14,2);
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS amount_recovered DECIMAL(14,2);

-- ===== 19. 问题表扩展 =====
ALTER TABLE audit_issue ADD COLUMN IF NOT EXISTS amount         DECIMAL(14,2);
ALTER TABLE audit_issue ADD COLUMN IF NOT EXISTS responsible_unit VARCHAR(200);
ALTER TABLE audit_issue ADD COLUMN IF NOT EXISTS responsible_person VARCHAR(100);
ALTER TABLE audit_issue ADD COLUMN IF NOT EXISTS issue_type       VARCHAR(50);

-- ===== 20. 预设角色 =====
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, create_time)
SELECT 100, '校领导', 'school_leader', 10, '1', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'school_leader');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, create_time)
SELECT 101, '审计处长', 'audit_director', 11, '1', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'audit_director');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, create_time)
SELECT 102, '项目组长', 'audit_leader', 12, '3', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'audit_leader');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, create_time)
SELECT 103, '普通审计员', 'auditor', 13, '4', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'auditor');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, create_time)
SELECT 104, '被审单位负责人', 'unit_manager', 14, '5', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'unit_manager');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, create_time)
SELECT 105, '被审单位联络员', 'unit_contact', 15, '5', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'unit_contact');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, create_time)
SELECT 106, '中介审计人员', 'outsource', 16, '5', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'outsource');

-- ===== 21. 新增菜单 =====
-- 审计准备
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_time)
SELECT '审计准备', (SELECT menu_id FROM sys_menu WHERE menu_name='审计作业' LIMIT 1), 1, 'prepare', 'audit/prepare', 'C', 'audit:prepare:view', 'guide', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path='prepare' AND component='audit/prepare');

-- 领导干部库
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_time)
SELECT '领导干部库', (SELECT menu_id FROM sys_menu WHERE menu_name='审计管理' LIMIT 1), 4, 'leader', 'audit/leader', 'C', 'audit:leader:view', 'peoples', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path='leader' AND component='audit/leader');

-- 方案模板库
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_time)
SELECT '方案模板库', (SELECT menu_id FROM sys_menu WHERE menu_name='审计作业' LIMIT 1), 0, 'schemeTemplate', 'audit/schemeTemplate', 'C', 'audit:template:view', 'documentation', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path='schemeTemplate' AND component='audit/schemeTemplate');

-- 项目归档
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_time)
SELECT '项目归档', (SELECT menu_id FROM sys_menu WHERE menu_name='审计作业' LIMIT 1), 5, 'archive', 'audit/archive', 'C', 'audit:archive:view', 'zip', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path='archive' AND component='audit/archive');

-- 临时授权管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_time)
SELECT '临时授权', (SELECT menu_id FROM sys_menu WHERE menu_name='系统管理' LIMIT 1), 10, 'tempAuth', 'audit/tempAuth', 'C', 'audit:auth:view', 'lock', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path='tempAuth' AND component='audit/tempAuth');

-- ===== 22. 方案模板种子数据 =====
INSERT INTO audit_scheme_template (template_name, audit_type, content, status, create_by, create_time) VALUES
('经济责任审计方案模板', '经济责任审计', '一、审计目标\n对被审计领导干部任职期间经济责任履行情况进行审计。\n\n二、审计范围\n任职期间所在单位的财务收支、经济决策、资产管理等情况。\n\n三、审计重点\n1. 重大经济决策事项\n2. 财务收支合规性\n3. 国有资产管理\n4. 内控制度建设\n\n四、审计方法\n1. 审阅财务账簿和凭证\n2. 核实资产实物\n3. 访谈相关人员\n4. 数据分析比对\n\n五、审计步骤\n1. 审前调查（5个工作日）\n2. 现场审计（15个工作日）\n3. 报告编制（5个工作日）\n4. 征求意见（5个工作日）', 1, 'admin', now()),
('财务收支审计方案模板', '财务收支审计', '一、审计目标\n对被审计单位财务收支的真实性、合法性和效益性进行审计。\n\n二、审计范围\n审计期间内的全部财务收入和支出。\n\n三、审计重点\n1. 收入完整性\n2. 支出合规性\n3. 预算执行情况\n4. 专项资金管理\n\n四、审计方法\n1. 账表核对\n2. 凭证抽查\n3. 银行对账\n4. 实物盘点\n\n五、审计步骤\n1. 审前准备（3个工作日）\n2. 现场审计（10个工作日）\n3. 报告编制（5个工作日）', 1, 'admin', now()),
('专项审计方案模板', '专项审计', '一、审计目标\n对专项资金/事项的管理和使用情况进行审计。\n\n二、审计范围\n专项资金的申请、分配、使用、绩效全过程。\n\n三、审计重点\n1. 资金使用合规性\n2. 项目执行进度\n3. 绩效目标实现\n4. 管理制度执行\n\n四、审计方法\n1. 资金流向追踪\n2. 项目现场核查\n3. 受益对象抽查\n4. 绩效评价分析', 1, 'admin', now()),
('工程审计方案模板', '工程审计', '一、审计目标\n对工程项目建设管理及资金使用情况进行审计。\n\n二、审计范围\n工程立项、招投标、合同签订、施工管理、竣工验收、资金支付全过程。\n\n三、审计重点\n1. 招投标合规性\n2. 合同管理\n3. 工程变更和签证\n4. 工程造价真实性\n\n四、审计方法\n1. 合同审查\n2. 现场勘察\n3. 工程量核实\n4. 造价审核', 1, 'admin', now())
ON CONFLICT DO NOTHING;
