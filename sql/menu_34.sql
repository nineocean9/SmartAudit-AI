-- 模块三四：菜单与权限码注入
-- 注意：需要先有 audit_rectification, audit_risk_case, audit_case_lib 表

-- 整改台账 (menu_id=1074)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1074, '整改台账', 1062, 6, 'rectification', 'audit/rectification', '', '', 1, 0, 'C', '0', '0', 'audit:rectification:view', 'checkbox', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1074);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1075, '整改跟踪', 1074, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'audit:rectification:edit', '#', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1075);

-- 问题风险库 (menu_id=1076)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1076, '问题风险库', 1062, 7, 'risk', 'audit/risk', '', '', 1, 0, 'C', '0', '0', 'audit:basis:query', 'warning', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1076);

-- 案例库 (menu_id=1077)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1077, '案例库', 1062, 8, 'case', 'audit/case', '', '', 1, 0, 'C', '0', '0', 'audit:basis:query', 'example', 'admin', now(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1077);

-- 角色关联（管理员角色 menu_id=1）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m WHERE m.menu_id BETWEEN 1074 AND 1077
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
