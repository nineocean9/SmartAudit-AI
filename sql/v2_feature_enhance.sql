-- ============================================================
-- v2_feature_enhance.sql — 功能增强SQL（全量）
-- 适用于 PostgreSQL + pgvector
-- ============================================================
-- PostgreSQL 执行顺序建议见 sql/RUN_ORDER_POSTGRES.md

DO $$
BEGIN
  IF to_regclass('audit_plan') IS NULL THEN
    RAISE EXCEPTION '缺少 audit_plan，请先执行 sql/module_1.sql。';
  END IF;
  IF to_regclass('audit_project') IS NULL THEN
    RAISE EXCEPTION '缺少 audit_project，请先执行 sql/demo_project_data.sql。';
  END IF;
  IF to_regclass('audit_basis') IS NULL THEN
    RAISE EXCEPTION '缺少 audit_basis，请先执行 sql/ai_init_pg.sql。';
  END IF;
END $$;

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

-- ===== 3. 计划-项目关联表 =====
CREATE TABLE IF NOT EXISTS audit_plan_project (
    id            BIGSERIAL PRIMARY KEY,
    plan_id       BIGINT      NOT NULL REFERENCES audit_plan(id),
    project_id    BIGINT      NOT NULL REFERENCES audit_project(id),
    bind_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(plan_id, project_id)
);

COMMENT ON TABLE audit_plan_project IS '审计计划关联项目';

-- ===== 4. 审计方案模板表 =====
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

-- ===== 5. 计划变更记录表 =====
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

-- ===== 6. 审计项目表扩展 =====
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

-- ===== 7. 被审计单位表扩展 =====
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS unit_code       VARCHAR(50);
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS parent_leader   VARCHAR(100);
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS staff_count     INT;
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS annual_budget   DECIMAL(14,2);
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS finance_contact VARCHAR(100);
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS contact_phone   VARCHAR(20);
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS address         VARCHAR(200);
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS last_audit_date DATE;
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS status          INT DEFAULT 1;

-- ===== 8. 领导干部表扩展 =====
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS gender         VARCHAR(10);
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS id_number      VARCHAR(20);
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS managed_funds  DECIMAL(14,2);
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS managed_scope  VARCHAR(500);
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS position_history TEXT;
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS audit_evaluation TEXT;

-- ===== 9. 单位变更日志表 =====
CREATE TABLE IF NOT EXISTS audit_unit_change_log (
    id           BIGSERIAL PRIMARY KEY,
    unit_id      BIGINT      NOT NULL,
    field_name   VARCHAR(50) NOT NULL,
    old_value    TEXT,
    new_value    TEXT,
    change_by    VARCHAR(64),
    change_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 10. 项目成员分工表 =====
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

-- ===== 11. 审前资料清单表 =====
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

-- ===== 12. 报告意见交互表 =====
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

-- ===== 13. 报告版本快照表 =====
CREATE TABLE IF NOT EXISTS audit_report_version (
    id           BIGSERIAL PRIMARY KEY,
    report_id    BIGINT NOT NULL,
    version_no   INT    NOT NULL,
    content      TEXT,
    change_desc  VARCHAR(500),
    create_by    VARCHAR(64),
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 14. 报告表扩展 =====
ALTER TABLE audit_report ADD COLUMN IF NOT EXISTS title          VARCHAR(200);
ALTER TABLE audit_report ADD COLUMN IF NOT EXISTS version_no     INT DEFAULT 1;
ALTER TABLE audit_report ADD COLUMN IF NOT EXISTS opinion_status INT DEFAULT 0;
ALTER TABLE audit_report ADD COLUMN IF NOT EXISTS file_url       VARCHAR(500);
ALTER TABLE audit_scheme ADD COLUMN IF NOT EXISTS title          VARCHAR(200);
ALTER TABLE audit_scheme ADD COLUMN IF NOT EXISTS plan_id        BIGINT;
ALTER TABLE audit_scheme ADD COLUMN IF NOT EXISTS file_url       VARCHAR(500);
ALTER TABLE audit_scheme ADD COLUMN IF NOT EXISTS update_time    TIMESTAMP;

-- ===== 15. 项目归档表 =====
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

-- ===== 16. 法条关联关系表 =====
CREATE TABLE IF NOT EXISTS audit_basis_relation (
    id            BIGSERIAL PRIMARY KEY,
    basis_id      BIGINT     NOT NULL,
    related_id    BIGINT     NOT NULL,
    relation_type VARCHAR(20) NOT NULL,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 17. 审计依据表扩展 =====
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS doc_number      VARCHAR(100);
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS audit_scope     VARCHAR(200);
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS fund_type       VARCHAR(100);
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS expire_date     DATE;
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS hierarchy_level VARCHAR(20);
ALTER TABLE audit_scheme_template ADD COLUMN IF NOT EXISTS file_url VARCHAR(500);

-- ===== 18. 中介临时授权表 =====
CREATE TABLE IF NOT EXISTS audit_temp_auth (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    project_id   BIGINT NOT NULL,
    auth_type    VARCHAR(20) DEFAULT 'intermediary',
    start_date   DATE NOT NULL,
    expire_date  DATE NOT NULL,
    status       INT DEFAULT 1,
    create_by    VARCHAR(64),
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE audit_temp_auth ALTER COLUMN auth_type SET DEFAULT 'intermediary';

-- ===== 19. 整改表扩展 =====
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS rectify_plan    TEXT;
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS delay_reason    VARCHAR(500);
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS delay_status    INT DEFAULT 0;
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS review_result   VARCHAR(20);
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS review_comment  TEXT;
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS review_by       VARCHAR(64);
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS review_time     TIMESTAMP;
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS amount_involved DECIMAL(14,2);
ALTER TABLE audit_rectification ADD COLUMN IF NOT EXISTS amount_recovered DECIMAL(14,2);

-- ===== 20. 问题表扩展 =====
ALTER TABLE audit_issue ADD COLUMN IF NOT EXISTS amount         DECIMAL(14,2);
ALTER TABLE audit_issue ADD COLUMN IF NOT EXISTS responsible_unit VARCHAR(200);
ALTER TABLE audit_issue ADD COLUMN IF NOT EXISTS responsible_person VARCHAR(100);
ALTER TABLE audit_issue ADD COLUMN IF NOT EXISTS issue_type       VARCHAR(50);

-- ===== 21. 预设角色 =====
UPDATE sys_role SET role_name = '项目组长/主审', role_key = 'audit_project_leader', role_sort = 30, data_scope = '5'
WHERE role_id = 102 OR role_key = 'audit_leader';

UPDATE sys_role SET role_name = '普通审计人员', role_key = 'audit_staff', role_sort = 40, data_scope = '5'
WHERE role_id = 103 OR role_key = 'auditor';

UPDATE sys_role SET role_name = '被审计单位负责人', role_key = 'audited_unit_principal', role_sort = 50, data_scope = '3'
WHERE role_id = 104 OR role_key = 'unit_manager';

UPDATE sys_role SET role_name = '被审计单位联络员', role_key = 'audited_unit_liaison', role_sort = 60, data_scope = '3'
WHERE role_id = 105 OR role_key = 'unit_contact';

UPDATE sys_role SET role_name = '中介审计人员', role_key = 'intermediary_auditor', role_sort = 70, data_scope = '5'
WHERE role_id = 106 OR role_key = 'outsource';

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, create_time)
SELECT 100, '校领导', 'school_leader', 10, '1', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 100 OR role_key = 'school_leader');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, create_time)
SELECT 101, '审计处长', 'audit_director', 20, '1', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 101 OR role_key = 'audit_director');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, create_time)
SELECT 102, '项目组长/主审', 'audit_project_leader', 30, '5', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 102 OR role_key = 'audit_project_leader');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, create_time)
SELECT 103, '普通审计人员', 'audit_staff', 40, '5', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 103 OR role_key = 'audit_staff');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, create_time)
SELECT 104, '被审计单位负责人', 'audited_unit_principal', 50, '3', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 104 OR role_key = 'audited_unit_principal');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, create_time)
SELECT 105, '被审计单位联络员', 'audited_unit_liaison', 60, '3', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 105 OR role_key = 'audited_unit_liaison');

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, create_time)
SELECT 106, '中介审计人员', 'intermediary_auditor', 70, '5', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 106 OR role_key = 'intermediary_auditor');

-- ===== 22. 新增菜单 =====
-- 审计准备
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_time)
SELECT '审计准备', 1202, 2, 'prepare', 'audit/prepare', 'C', 'audit:prepare:view', 'guide', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path='prepare' AND component='audit/prepare');

-- 领导干部库
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_time)
SELECT '领导干部库', 1200, 2, 'leader', 'audit/leader', 'C', 'audit:leader:view', 'peoples', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path='leader' AND component='audit/leader');

-- 方案模板库
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_time)
SELECT '方案模板库', 1202, 1, 'schemeTemplate', 'audit/schemeTemplate', 'C', 'audit:template:view', 'documentation', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path='schemeTemplate' AND component='audit/schemeTemplate');

-- 项目归档
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_time)
SELECT '项目归档', 1204, 3, 'archive', 'audit/archive', 'C', 'audit:archive:view', 'zip', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path='archive' AND component='audit/archive');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_time)
SELECT '归档维护', m.menu_id, 1, '', '', 'F', 'audit:archive:edit', '#', now()
FROM sys_menu m
WHERE m.path='archive' AND m.component='audit/archive'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='audit:archive:edit');

-- 临时授权管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_time)
SELECT '临时授权', (SELECT menu_id FROM sys_menu WHERE menu_name='系统管理' LIMIT 1), 10, 'tempAuth', 'audit/tempAuth', 'C', 'audit:auth:view', 'lock', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path='tempAuth' AND component='audit/tempAuth');

-- ===== 23. 方案模板种子数据（Word版） =====
DELETE FROM audit_scheme_template
WHERE create_by = 'admin'
  AND (file_url IS NULL OR file_url = '')
  AND template_name IN (
    '经济责任审计方案模板',
    '财务收支审计方案模板',
    '专项审计方案模板',
    '工程审计方案模板'
  );

INSERT INTO audit_scheme_template (template_name, audit_type, content, file_url, status, create_by, create_time)
SELECT '经济责任审计实施方案模板（Word版）', '经济责任审计', '[Word模板] 经济责任审计实施方案模板（Word版）', '/audit-template/default/economic-responsibility-audit-plan-template.docx', 1, 'admin', now()
WHERE NOT EXISTS (SELECT 1 FROM audit_scheme_template WHERE template_name = '经济责任审计实施方案模板（Word版）' AND file_url IS NOT NULL AND file_url <> '');

INSERT INTO audit_scheme_template (template_name, audit_type, content, file_url, status, create_by, create_time)
SELECT '财务收支审计实施方案模板（Word版）', '财务收支审计', '[Word模板] 财务收支审计实施方案模板（Word版）', '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx', 1, 'admin', now()
WHERE NOT EXISTS (SELECT 1 FROM audit_scheme_template WHERE template_name = '财务收支审计实施方案模板（Word版）' AND file_url IS NOT NULL AND file_url <> '');

INSERT INTO audit_scheme_template (template_name, audit_type, content, file_url, status, create_by, create_time)
SELECT '专项审计实施方案模板（Word版）', '专项审计', '[Word模板] 专项审计实施方案模板（Word版）', '/audit-template/default/special-audit-plan-template.docx', 1, 'admin', now()
WHERE NOT EXISTS (SELECT 1 FROM audit_scheme_template WHERE template_name = '专项审计实施方案模板（Word版）' AND file_url IS NOT NULL AND file_url <> '');

INSERT INTO audit_scheme_template (template_name, audit_type, content, file_url, status, create_by, create_time)
SELECT '工程审计实施方案模板（Word版）', '工程审计', '[Word模板] 工程审计实施方案模板（Word版）', '/audit-template/default/engineering-audit-plan-template.docx', 1, 'admin', now()
WHERE NOT EXISTS (SELECT 1 FROM audit_scheme_template WHERE template_name = '工程审计实施方案模板（Word版）' AND file_url IS NOT NULL AND file_url <> '');
