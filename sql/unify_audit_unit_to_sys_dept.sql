-- ============================================================
-- 审计单位主数据统一到 sys_dept
-- PostgreSQL
-- 目标：
-- 1. sys_dept 扩展为“学校/学院/部门/公司/被审计单位”统一主数据表。
-- 2. audit_project 使用 dept_id 作为被审计单位外键，audited_unit 仅保留为展示兼容字段。
-- 3. audit_unit 保留兼容，但通过 dept_id 关联 sys_dept，后续可逐步下线。
-- ============================================================

DO $$
BEGIN
  IF to_regclass('sys_dept') IS NULL THEN
    RAISE EXCEPTION '缺少 sys_dept，请先执行若依基础库脚本。';
  END IF;
  IF to_regclass('audit_project') IS NULL THEN
    RAISE EXCEPTION '缺少 audit_project，请先执行审计业务基础脚本。';
  END IF;
  IF to_regclass('audit_unit') IS NULL THEN
    RAISE EXCEPTION '缺少 audit_unit，请先执行审计对象基础脚本。';
  END IF;
END $$;

-- 1. 扩展 sys_dept，使其能承载审计对象档案信息
ALTER TABLE sys_dept ADD COLUMN IF NOT EXISTS unit_type VARCHAR(50);
ALTER TABLE sys_dept ADD COLUMN IF NOT EXISTS profile TEXT;
ALTER TABLE sys_dept ADD COLUMN IF NOT EXISTS history_audit TEXT;
ALTER TABLE sys_dept ADD COLUMN IF NOT EXISTS is_audit_target SMALLINT DEFAULT 0;

COMMENT ON COLUMN sys_dept.unit_type IS '单位类型：学校/学院/处室/公司/中介机构/其他';
COMMENT ON COLUMN sys_dept.profile IS '单位基本情况介绍';
COMMENT ON COLUMN sys_dept.history_audit IS '历史审计情况';
COMMENT ON COLUMN sys_dept.is_audit_target IS '是否可作为被审计对象：1是 0否';

-- 2. 扩展业务表，建立到 sys_dept 的稳定 ID 关联
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS dept_id BIGINT;
COMMENT ON COLUMN audit_project.dept_id IS '被审计单位对应的 sys_dept.dept_id';

DO $$
BEGIN
  IF to_regclass('audit_unit') IS NOT NULL THEN
    ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS dept_id BIGINT;
    COMMENT ON COLUMN audit_unit.dept_id IS '对应的 sys_dept.dept_id，兼容字段';
  END IF;
END $$;

-- 3. 恢复同名但被逻辑删除的部门，避免重复创建
UPDATE sys_dept d
SET del_flag = '0',
    status = '0',
    update_by = 'admin',
    update_time = now()
WHERE d.del_flag = '2'
  AND (
    (to_regclass('audit_unit') IS NOT NULL AND EXISTS (
      SELECT 1 FROM audit_unit u
      WHERE trim(u.unit_name) = trim(d.dept_name)
    ))
    OR EXISTS (
      SELECT 1 FROM audit_project p
      WHERE trim(p.audited_unit) = trim(d.dept_name)
    )
  );

-- 4. 将 audit_unit 中存在、sys_dept 中缺失的单位补入 sys_dept
DO $$
DECLARE
  root_id BIGINT;
  root_ancestors VARCHAR(255);
BEGIN
  SELECT dept_id, ancestors
    INTO root_id, root_ancestors
  FROM sys_dept
  WHERE del_flag = '0'
    AND dept_name IN ('示范高校', '若依科技')
  ORDER BY CASE WHEN dept_name = '示范高校' THEN 0 ELSE 1 END, dept_id
  LIMIT 1;

  IF root_id IS NULL THEN
    root_id := 0;
    root_ancestors := '0';
  END IF;

  IF to_regclass('audit_unit') IS NOT NULL THEN
    INSERT INTO sys_dept (
      parent_id, ancestors, dept_name, order_num, leader, phone, email,
      status, del_flag, create_by, create_time, update_by, update_time,
      unit_type, profile, history_audit, is_audit_target
    )
    SELECT
      root_id,
      CASE WHEN root_id = 0 THEN '0' ELSE root_ancestors || ',' || root_id END,
      trim(u.unit_name),
      50,
      '',
      '',
      '',
      '0',
      '0',
      'admin',
      now(),
      '',
      NULL,
      COALESCE(NULLIF(u.unit_type, ''), '被审计单位'),
      u.profile,
      u.history_audit,
      1
    FROM audit_unit u
    WHERE u.unit_name IS NOT NULL
      AND trim(u.unit_name) <> ''
      AND NOT EXISTS (
        SELECT 1 FROM sys_dept d
        WHERE d.del_flag = '0'
          AND trim(d.dept_name) = trim(u.unit_name)
      );
  END IF;
END $$;

-- 5. 将 audit_project.audited_unit 中存在、sys_dept 中缺失的单位补入 sys_dept
DO $$
DECLARE
  root_id BIGINT;
  root_ancestors VARCHAR(255);
BEGIN
  SELECT dept_id, ancestors
    INTO root_id, root_ancestors
  FROM sys_dept
  WHERE del_flag = '0'
    AND dept_name IN ('示范高校', '若依科技')
  ORDER BY CASE WHEN dept_name = '示范高校' THEN 0 ELSE 1 END, dept_id
  LIMIT 1;

  IF root_id IS NULL THEN
    root_id := 0;
    root_ancestors := '0';
  END IF;

  INSERT INTO sys_dept (
    parent_id, ancestors, dept_name, order_num, leader, phone, email,
    status, del_flag, create_by, create_time, update_by, update_time,
    unit_type, profile, history_audit, is_audit_target
  )
  SELECT
    root_id,
    CASE WHEN root_id = 0 THEN '0' ELSE root_ancestors || ',' || root_id END,
    unit_name,
    60,
    '',
    '',
    '',
    '0',
    '0',
    'admin',
    now(),
    '',
    NULL,
    '被审计单位',
    '',
    '',
    1
  FROM (
    SELECT DISTINCT trim(audited_unit) AS unit_name
    FROM audit_project
    WHERE audited_unit IS NOT NULL
      AND trim(audited_unit) <> ''
  ) p
  WHERE NOT EXISTS (
    SELECT 1 FROM sys_dept d
    WHERE d.del_flag = '0'
      AND trim(d.dept_name) = p.unit_name
  );
END $$;

-- 6. 回填 sys_dept 的审计扩展字段
DO $$
BEGIN
  IF to_regclass('audit_unit') IS NOT NULL THEN
    UPDATE sys_dept d
    SET unit_type = COALESCE(NULLIF(d.unit_type, ''), NULLIF(u.unit_type, ''), '被审计单位'),
        profile = COALESCE(NULLIF(d.profile, ''), u.profile),
        history_audit = COALESCE(NULLIF(d.history_audit, ''), u.history_audit),
        is_audit_target = 1,
        update_by = 'admin',
        update_time = now()
    FROM audit_unit u
    WHERE d.del_flag = '0'
      AND trim(d.dept_name) = trim(u.unit_name);
  END IF;
END $$;

UPDATE sys_dept d
SET is_audit_target = 1,
    unit_type = COALESCE(NULLIF(d.unit_type, ''), '被审计单位')
WHERE d.del_flag = '0'
  AND EXISTS (
    SELECT 1 FROM audit_project p
    WHERE trim(p.audited_unit) = trim(d.dept_name)
  );

-- 7. 回填 audit_unit.dept_id 与 audit_project.dept_id
DO $$
BEGIN
  IF to_regclass('audit_unit') IS NOT NULL THEN
    UPDATE audit_unit u
    SET dept_id = d.dept_id
    FROM sys_dept d
    WHERE d.del_flag = '0'
      AND trim(d.dept_name) = trim(u.unit_name)
      AND (u.dept_id IS NULL OR u.dept_id <> d.dept_id);
  END IF;
END $$;

UPDATE audit_project p
SET dept_id = d.dept_id
FROM sys_dept d
WHERE d.del_flag = '0'
  AND trim(d.dept_name) = trim(p.audited_unit)
  AND (p.dept_id IS NULL OR p.dept_id <> d.dept_id);

-- 8. 索引和外键。外键只在不存在时创建，避免重复执行报错。
CREATE INDEX IF NOT EXISTS idx_sys_dept_audit_target ON sys_dept(is_audit_target);
CREATE INDEX IF NOT EXISTS idx_audit_project_dept_id ON audit_project(dept_id);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_audit_project_dept'
  ) THEN
    ALTER TABLE audit_project
      ADD CONSTRAINT fk_audit_project_dept
      FOREIGN KEY (dept_id) REFERENCES sys_dept(dept_id);
  END IF;

  IF to_regclass('audit_unit') IS NOT NULL
     AND NOT EXISTS (
       SELECT 1 FROM pg_constraint WHERE conname = 'fk_audit_unit_dept'
     ) THEN
    ALTER TABLE audit_unit
      ADD CONSTRAINT fk_audit_unit_dept
      FOREIGN KEY (dept_id) REFERENCES sys_dept(dept_id);
  END IF;
END $$;

-- 9. 检查仍未完成 ID 关联的数据
SELECT 'audit_project_missing_dept_id' AS check_name, id, project_name, audited_unit
FROM audit_project
WHERE dept_id IS NULL;

DO $$
BEGIN
  RAISE NOTICE '完成：sys_dept 已作为审计单位主数据，audit_project.dept_id / audit_unit.dept_id 已回填。';
END $$;
