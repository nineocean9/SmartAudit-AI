-- Make audit preparation material submitter fields point to real sys_user accounts.
-- Password for inserted demo accounts is admin123.

INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark)
SELECT seed.user_id, seed.dept_id, seed.user_name, seed.nick_name, '00', seed.email, seed.phone, '0', '',
       '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
       '0', '0', '127.0.0.1', now(), now(), 'admin', now(), '', NULL, seed.remark
FROM (VALUES
  (2012, 202, 'business_liaison', '钱莹（商学院 被审计单位联络员）', 'business_liaison@example.edu.cn', '13810000012', '商学院联络员演示账号'),
  (2013, 200, 'logistics_liaison', '郑凯（后勤处 被审计单位联络员）', 'logistics_liaison@example.edu.cn', '13810000013', '后勤处联络员演示账号'),
  (2014, 203, 'finance_liaison', '胡敏（财务处 被审计单位联络员）', 'finance_liaison@example.edu.cn', '13810000014', '财务处联络员演示账号'),
  (2015, 201, 'library_liaison', '高远（图书馆 被审计单位联络员）', 'library_liaison@example.edu.cn', '13810000015', '图书馆联络员演示账号')
) AS seed(user_id, dept_id, user_name, nick_name, email, phone, remark)
WHERE NOT EXISTS (
  SELECT 1 FROM sys_user u WHERE u.user_id = seed.user_id OR u.user_name = seed.user_name
);

UPDATE sys_user u
SET dept_id = seed.dept_id,
    nick_name = seed.nick_name,
    email = seed.email,
    phonenumber = seed.phone,
    update_by = 'admin',
    update_time = now(),
    remark = seed.remark
FROM (VALUES
  (2012, 202, 'business_liaison', '钱莹（商学院 被审计单位联络员）', 'business_liaison@example.edu.cn', '13810000012', '商学院联络员演示账号'),
  (2013, 200, 'logistics_liaison', '郑凯（后勤处 被审计单位联络员）', 'logistics_liaison@example.edu.cn', '13810000013', '后勤处联络员演示账号'),
  (2014, 203, 'finance_liaison', '胡敏（财务处 被审计单位联络员）', 'finance_liaison@example.edu.cn', '13810000014', '财务处联络员演示账号'),
  (2015, 201, 'library_liaison', '高远（图书馆 被审计单位联络员）', 'library_liaison@example.edu.cn', '13810000015', '图书馆联络员演示账号')
) AS seed(user_id, dept_id, user_name, nick_name, email, phone, remark)
WHERE u.user_id = seed.user_id OR u.user_name = seed.user_name;

INSERT INTO sys_user_role (user_id, role_id)
SELECT seed.user_id, 105
FROM (VALUES (2012), (2013), (2014), (2015)) AS seed(user_id)
WHERE NOT EXISTS (
  SELECT 1 FROM sys_user_role ur WHERE ur.user_id = seed.user_id AND ur.role_id = 105
);

UPDATE audit_material_checklist c
SET submit_by = CASE p.id
    WHEN 1 THEN 'audited_unit_liaison'
    WHEN 2 THEN 'business_liaison'
    WHEN 3 THEN 'logistics_liaison'
    WHEN 4 THEN 'finance_liaison'
    WHEN 5 THEN 'library_liaison'
    WHEN 6 THEN 'audited_unit_liaison'
    WHEN 9101 THEN 'a_company_liaison'
    ELSE c.submit_by
  END
FROM audit_project p
WHERE p.id = c.project_id
  AND c.submit_status = 1;
