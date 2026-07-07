-- 菜单重组：不再全塞在 AI智能辅助(1062)下，按业务模块分
-- Step 1: 创建新的顶级菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1089, '审计管理', 0, 7, 'audit-mgmt', NULL, '', '', 1, 0, 'M', '0', '0', '', 'guide', 'admin', now(), '', NULL, '审计信息管理目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1089);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1090, '审计依据', 0, 4, 'audit-basis', NULL, '', '', 1, 0, 'M', '0', '0', '', 'documentation', 'admin', now(), '', NULL, '审计依据库目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1090);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1091, '审计作业', 0, 8, 'audit-ops', NULL, '', '', 1, 0, 'M', '0', '0', '', 'component', 'admin', now(), '', NULL, '审计作业目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1091);

-- Step 2: 重新分配子菜单的 parent_id
-- 审计信息管理(1089)下面
UPDATE sys_menu SET parent_id = 1089 WHERE menu_id IN (1078, 1079, 1080);

-- 审计依据库(1090)下面
UPDATE sys_menu SET parent_id = 1090 WHERE menu_id IN (1063, 1076, 1077);

-- 审计作业(1091)下面
UPDATE sys_menu SET parent_id = 1091 WHERE menu_id IN (1074, 1084, 1085);

-- 项目工作台(1083) → 系统工具(3)
UPDATE sys_menu SET parent_id = 3, order_num = 4 WHERE menu_id = 1083;

-- 数据驾驶舱 → 系统监控(2)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1092, '数据驾驶舱', 2, 6, 'dashboard', 'dashboard/index', '', '', 1, 0, 'C', '0', '0', 'dashboard:view', 'dashboard', 'admin', now(), '', NULL, '审计数据驾驶舱'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1092);

-- 清除空白页面菜单（文档校验/风险线索——没有前端页面）
DELETE FROM sys_role_menu WHERE menu_id IN (1067, 1068);
DELETE FROM sys_menu WHERE menu_id IN (1067, 1068);

-- 角色关联新增菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m WHERE m.menu_id BETWEEN 1089 AND 1092
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
