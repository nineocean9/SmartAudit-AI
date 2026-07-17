-- Targeted fix for one demo account:
--   username: audited_unit_principal
--   password: admin123
-- PostgreSQL. Safe to run repeatedly.

DO $$
BEGIN
  IF to_regclass('sys_dept') IS NULL
     OR to_regclass('sys_role') IS NULL
     OR to_regclass('sys_user') IS NULL
     OR to_regclass('sys_user_role') IS NULL THEN
    RAISE EXCEPTION 'Missing RuoYi auth tables. Run sql/ry_20250522.sql first.';
  END IF;
END $$;

ALTER TABLE sys_dept ALTER COLUMN email TYPE VARCHAR(50);

INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time)
SELECT 2100, 0, '0', '示范高校', 1, '校领导', '13800000000', 'school@example.edu.cn', '0', '0', 'admin', now(), '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_id = 2100);

INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time)
SELECT 2102, 2100, '0,2100', '信息工程学院', 2, '学院负责人', '13800000002', 'college@example.edu.cn', '0', '0', 'admin', now(), '', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_id = 2102);

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT 104, '被审计单位负责人', 'audited_unit_principal', 50, '3', TRUE, TRUE, '0', '0', 'admin', now(), '', NULL, '本单位负责人'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 104 OR role_key = 'audited_unit_principal');

UPDATE sys_role
SET role_name = '被审计单位负责人',
    role_key = 'audited_unit_principal',
    role_sort = 50,
    data_scope = '3',
    status = '0',
    del_flag = '0',
    update_by = 'admin',
    update_time = now()
WHERE role_id = 104 OR role_key = 'audited_unit_principal';

DO $$
DECLARE
  target_user_id BIGINT;
BEGIN
  SELECT user_id INTO target_user_id
  FROM sys_user
  WHERE user_name = 'audited_unit_principal'
  ORDER BY CASE WHEN user_id = 2005 THEN 0 ELSE 1 END, user_id
  LIMIT 1;

  IF target_user_id IS NULL THEN
    IF EXISTS (SELECT 1 FROM sys_user WHERE user_id = 2005) THEN
      target_user_id := 2005;
    ELSE
      INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark)
      VALUES (2005, 2102, 'audited_unit_principal', '信息工程学院负责人', '00', 'principal@example.edu.cn', '13810000005', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', now(), now(), 'admin', now(), '', NULL, '信息工程学院负责人演示账号');
      target_user_id := 2005;
    END IF;
  END IF;

  UPDATE sys_user
  SET dept_id = 2102,
      user_name = 'audited_unit_principal',
      nick_name = '信息工程学院负责人',
      user_type = '00',
      email = 'principal@example.edu.cn',
      phonenumber = '13810000005',
      password = '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
      status = '0',
      del_flag = '0',
      update_by = 'admin',
      update_time = now(),
      remark = '信息工程学院负责人演示账号'
  WHERE user_id = target_user_id;

  DELETE FROM sys_user_role WHERE user_id = target_user_id;
  INSERT INTO sys_user_role (user_id, role_id)
  SELECT target_user_id, 104
  WHERE NOT EXISTS (
    SELECT 1 FROM sys_user_role WHERE user_id = target_user_id AND role_id = 104
  );
END $$;

SELECT u.user_id,
       u.user_name,
       u.nick_name,
       u.status,
       u.del_flag,
       r.role_name,
       r.role_key
FROM sys_user u
LEFT JOIN sys_user_role ur ON ur.user_id = u.user_id
LEFT JOIN sys_role r ON r.role_id = ur.role_id
WHERE u.user_name = 'audited_unit_principal';
