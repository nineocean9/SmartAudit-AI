-- 修复并精简智能分析菜单
-- 1. 删除“数据分析”小页面菜单。
-- 2. 将 AI 分析放到“智能分析支撑”第一项。
-- 3. 隐藏原 AI智能辅助 空目录。
-- 4. 删除旧的空白审计大类。
-- 5. 系统监控放到底部，删除系统工具和若依官网。

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1200, '审计资源', 0, 4, 'audit-resource', NULL, '', '', 1, 0, 'M', '0', '0', '', 'education', 'admin', now(), '', NULL, '审计对象、依据、案例等基础资源'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1200);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1201, '计划立项', 0, 5, 'audit-planning', NULL, '', '', 1, 0, 'M', '0', '0', '', 'date', 'admin', now(), '', NULL, '审计计划、项目立项与项目库'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1201);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1202, '准备阶段', 0, 6, 'audit-prepare-flow', NULL, '', '', 1, 0, 'M', '0', '0', '', 'guide', 'admin', now(), '', NULL, '审计通知、方案模板、资料准备'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1202);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1203, '审计实施', 0, 7, 'audit-execution', NULL, '', '', 1, 0, 'M', '0', '0', '', 'component', 'admin', now(), '', NULL, '取证、底稿、问题和进度管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1203);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1204, '报告整改归档', 0, 8, 'audit-closeout', NULL, '', '', 1, 0, 'M', '0', '0', '', 'documentation', 'admin', now(), '', NULL, '审计报告、整改跟踪与项目归档'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1204);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1205, '智能分析支撑', 0, 9, 'audit-ai-support', NULL, '', '', 1, 0, 'M', '0', '0', '', 'chart', 'admin', now(), '', NULL, 'AI分析、可视化和智能辅助'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1205);

DELETE FROM sys_role_menu
WHERE menu_id IN (
  SELECT menu_id
  FROM sys_menu
  WHERE menu_id IN (4, 1092)
     OR menu_name = '若依官网'
     OR (path = 'dataAnalyze' AND component IN ('ai/dataAnalyze', 'ai/dataDashboard'))
);

DELETE FROM sys_menu
WHERE menu_id = 1092
   OR (path = 'dataAnalyze' AND component IN ('ai/dataAnalyze', 'ai/dataDashboard'));

UPDATE sys_menu
SET visible = '1'
WHERE menu_id = 1062
  AND menu_name = 'AI智能辅助'
  AND menu_type = 'M';

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1061, 'AI分析', 1205, 1, 'aiChat', 'ai/chat', '', '', 1, 0, 'C', '0', '0', '', 'user', 'admin', now(), '', NULL, 'AI分析入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1061)
  AND EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1205);

UPDATE sys_menu
SET parent_id = 1205,
    order_num = 1,
    menu_name = 'AI分析',
    path = 'aiChat',
    component = 'ai/chat',
    visible = '0',
    status = '0'
WHERE menu_id = 1061
  AND component = 'ai/chat'
  AND EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1205);

UPDATE sys_menu
SET order_num = 2
WHERE menu_id = 1095
  AND component = 'ai/visualization/index'
  AND EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1205);

UPDATE sys_menu
SET order_num = 3
WHERE menu_id = 1072
  AND component = 'ai/aiLog'
  AND EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1205);

-- 旧版菜单重组脚本创建过这些顶级目录，重排后容易变成空白入口。
-- 先把仍挂在旧目录下的可见子菜单按流程挪走，再删除旧目录。
UPDATE sys_menu SET parent_id = 1200, order_num = 1, visible = '0', status = '0' WHERE component = 'audit/unit';
UPDATE sys_menu SET parent_id = 1200, order_num = 2, visible = '0', status = '0' WHERE component = 'audit/leader';
UPDATE sys_menu SET parent_id = 1200, order_num = 3, visible = '0', status = '0' WHERE component = 'ai/basis';
UPDATE sys_menu SET parent_id = 1200, order_num = 4, visible = '0', status = '0' WHERE component = 'audit/risk';
UPDATE sys_menu SET parent_id = 1200, order_num = 5, visible = '0', status = '0' WHERE component = 'audit/case';

UPDATE sys_menu SET parent_id = 1201, order_num = 1, visible = '0', status = '0' WHERE component = 'audit/plan';
UPDATE sys_menu SET parent_id = 1201, order_num = 2, visible = '0', status = '0' WHERE component = 'audit/projectLib' AND menu_name = '项目库';
UPDATE sys_menu SET parent_id = 1201, order_num = 3, visible = '0', status = '0' WHERE component = 'audit/project';

UPDATE sys_menu SET parent_id = 1202, order_num = 1, visible = '0', status = '0' WHERE component = 'audit/schemeTemplate';
UPDATE sys_menu SET parent_id = 1202, order_num = 2, visible = '0', status = '0' WHERE component = 'audit/prepare';
UPDATE sys_menu SET parent_id = 1202, order_num = 3, visible = '0', status = '0' WHERE component = 'knowledge/upload';

UPDATE sys_menu SET parent_id = 1203, order_num = 1, visible = '0', status = '0' WHERE component = 'audit/progress';
UPDATE sys_menu SET parent_id = 1203, order_num = 2, visible = '0', status = '0' WHERE component = 'ai/forensic';
UPDATE sys_menu SET parent_id = 1203, order_num = 3, visible = '0', status = '0' WHERE component = 'audit/workpaper';
UPDATE sys_menu SET parent_id = 1203, order_num = 4, visible = '0', status = '0' WHERE component = 'audit/issue';

UPDATE sys_menu SET parent_id = 1204, order_num = 1, visible = '0', status = '0' WHERE component = 'audit/report';
UPDATE sys_menu SET parent_id = 1204, order_num = 2, visible = '0', status = '0' WHERE component = 'audit/rectification';
UPDATE sys_menu SET parent_id = 1204, order_num = 3, visible = '0', status = '0' WHERE component = 'audit/archive';

UPDATE sys_menu
SET parent_id = 1200, visible = '0', status = '0'
WHERE parent_id IN (
  SELECT menu_id
  FROM sys_menu
  WHERE parent_id = 0
    AND menu_type = 'M'
    AND menu_name IN ('审计管理', '审计依据', '审计作业')
)
AND menu_type <> 'F';

DELETE FROM sys_role_menu
WHERE menu_id IN (
  SELECT menu_id
  FROM sys_menu
  WHERE parent_id = 0
    AND menu_type = 'M'
    AND menu_name IN ('审计管理', '审计依据', '审计作业')
);

DELETE FROM sys_menu
WHERE parent_id = 0
  AND menu_type = 'M'
  AND menu_name IN ('审计管理', '审计依据', '审计作业');

DELETE FROM sys_menu
WHERE menu_id = 4
   OR menu_name = '若依官网';

DELETE FROM sys_role_menu
WHERE menu_id IN (
  SELECT menu_id
  FROM sys_menu
  WHERE menu_id = 3
     OR parent_id = 3
     OR parent_id IN (SELECT menu_id FROM sys_menu WHERE parent_id = 3)
);

DELETE FROM sys_menu
WHERE parent_id IN (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE parent_id = 3) t);

DELETE FROM sys_menu
WHERE parent_id = 3;

DELETE FROM sys_menu
WHERE menu_id = 3 OR menu_name = '系统工具';

UPDATE sys_menu SET order_num = 98 WHERE menu_id = 2 AND menu_name = '系统监控' AND parent_id = 0;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 1061
WHERE EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1061)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 1061);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.menu_id IN (1200, 1201, 1202, 1203, 1204, 1205)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);

-- 查看修复结果
SELECT menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status
FROM sys_menu
WHERE menu_id IN (2, 3, 4, 1061, 1062, 1072, 1092, 1095, 1200, 1201, 1202, 1203, 1204, 1205)
   OR menu_name IN ('审计管理', '审计依据', '审计作业')
   OR path = 'dataAnalyze'
ORDER BY menu_id;
