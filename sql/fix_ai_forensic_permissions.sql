-- AI 取证草稿职责分离：审计人员制单、主审复核、被审计单位不访问内部草稿。

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 12124, '取证单提交复核', 1069, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'ai:forensic:submit', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:forensic:submit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 12125, '取证单删除', 1069, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'ai:forensic:delete', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'ai:forensic:delete');

-- 被审计单位只能在对外协同功能中查看、回复正式取证事项，不能访问审计组内部草稿。
DELETE FROM sys_role_menu rm
USING sys_menu m
WHERE rm.menu_id = m.menu_id
  AND rm.role_id IN (104, 105)
  AND (m.component = 'ai/forensic' OR m.perms LIKE 'ai:forensic:%');

-- 普通审计人员和中介审计人员：生成、提交、删除本人草稿。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_key IN ('audit_staff', 'intermediary_auditor')
  AND m.perms IN ('ai:forensic:view', 'ai:forensic:gen', 'ai:forensic:submit', 'ai:forensic:delete')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id);

-- 项目组长和审计处长：可生成、提交、复核并管理授权项目内的草稿。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_key IN ('audit_project_leader', 'audit_director')
  AND m.perms IN ('ai:forensic:view', 'ai:forensic:gen', 'ai:forensic:submit', 'ai:forensic:review', 'ai:forensic:delete')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id);
