-- 扩展 analysis_result 表，支持数据可视化中心
ALTER TABLE analysis_result ADD COLUMN IF NOT EXISTS project_name VARCHAR(255);
ALTER TABLE analysis_result ADD COLUMN IF NOT EXISTS source_type VARCHAR(32) DEFAULT 'chat';
ALTER TABLE analysis_result ADD COLUMN IF NOT EXISTS keyword VARCHAR(255);
COMMENT ON COLUMN analysis_result.project_name IS '关联项目名称';
COMMENT ON COLUMN analysis_result.source_type IS '来源: chat / upload / project';
COMMENT ON COLUMN analysis_result.keyword IS '分析关键词';

-- 菜单：数据可视化 (menu_id=1095)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1095, '数据可视化', 1062, 20, 'visualization/index', 'ai/visualization/index', '', '', 1, 0, 'C', '0', '0', 'audit:visual:view', 'chart', 'admin', now(), '', NULL, '数据可视化中心'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1095);

-- 管理员角色关联
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 1095
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = 1095);
