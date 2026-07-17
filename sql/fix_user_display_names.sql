-- Give every demo account a human display name with organization and position.
-- Passwords and usernames are unchanged.

UPDATE sys_user
SET nick_name = seed.nick_name,
    update_by = 'admin',
    update_time = now(),
    remark = seed.remark
FROM (VALUES
  (1, 'admin', '系统管理员（平台管理员）', '平台管理员账号'),
  (2, 'ry', '测试用户（平台普通用户）', '平台普通用户账号'),
  (100, '1233444', '何静（A公司 被审计单位联络员）', 'A公司联络员演示账号'),
  (2001, 'school_leader', '张三（某高校 校领导）', '某高校校领导演示账号'),
  (2002, 'audit_director', '王明（某高校 审计处长）', '某高校审计处长演示账号'),
  (2003, 'audit_project_leader', '刘强（某高校 项目组长/主审）', '某高校项目组长/主审演示账号'),
  (2004, 'audit_staff', '李娜（某高校 普通审计人员）', '某高校普通审计人员演示账号'),
  (2005, 'audited_unit_principal', '赵敏（信息工程学院 被审计单位负责人）', '信息工程学院负责人演示账号'),
  (2006, 'audited_unit_liaison', '陈晨（信息工程学院 被审计单位联络员）', '信息工程学院联络员演示账号'),
  (2007, 'intermediary_auditor', '周航（外部中介机构 中介审计人员）', '外部中介审计人员演示账号'),
  (2010, 'a_company_principal', '孙丽（A公司 被审计单位负责人）', 'A公司负责人演示账号'),
  (2011, 'a_company_liaison', '吴磊（A公司 被审计单位联络员）', 'A公司联络员演示账号'),
  (2012, 'business_liaison', '钱莹（商学院 被审计单位联络员）', '商学院联络员演示账号'),
  (2013, 'logistics_liaison', '郑凯（后勤处 被审计单位联络员）', '后勤处联络员演示账号'),
  (2014, 'finance_liaison', '胡敏（财务处 被审计单位联络员）', '财务处联络员演示账号'),
  (2015, 'library_liaison', '高远（图书馆 被审计单位联络员）', '图书馆联络员演示账号')
) AS seed(user_id, user_name, nick_name, remark)
WHERE sys_user.user_id = seed.user_id
   OR sys_user.user_name = seed.user_name;
