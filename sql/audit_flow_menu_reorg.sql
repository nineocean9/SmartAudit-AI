-- 按审计流程重排 RuoYi 菜单
-- 说明：
-- 1. 仅调整 sys_menu / sys_role_menu，继续使用 RuoYi 自带“系统管理-菜单管理”维护菜单。
-- 2. 本脚本兼容菜单已存在或尚未插入的情况，可重复执行。
-- 3. 执行前建议先执行 module_1.sql、module_2.sql、menu_34.sql、ai_workspace.sql、analysis_result_alter.sql、v2_feature_enhance.sql。

-- ===== 1. 创建流程分类目录 =====
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

-- ===== 2. 补齐审计流程中可能缺失的菜单项 =====
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '领导干部库', 1200, 2, 'leader', 'audit/leader', '', '', 1, 0, 'C', '0', '0', 'audit:leader:view', 'peoples', 'admin', now(), '', NULL, '领导干部信息库'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = 'leader' AND component = 'audit/leader');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '方案模板库', 1202, 1, 'schemeTemplate', 'audit/schemeTemplate', '', '', 1, 0, 'C', '0', '0', 'audit:template:view', 'documentation', 'admin', now(), '', NULL, '审计方案模板库'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = 'schemeTemplate' AND component = 'audit/schemeTemplate');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '审计准备', 1202, 2, 'prepare', 'audit/prepare', '', '', 1, 0, 'C', '0', '0', 'audit:prepare:view', 'guide', 'admin', now(), '', NULL, '审计准备工作台'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = 'prepare' AND component = 'audit/prepare');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '审计问题', 1203, 4, 'issue', 'audit/issue', '', '', 1, 0, 'C', '0', '0', 'audit:issue:view', 'bug', 'admin', now(), '', NULL, '审计问题管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = 'issue' AND component = 'audit/issue');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '项目归档', 1204, 3, 'archive', 'audit/archive', '', '', 1, 0, 'C', '0', '0', 'audit:archive:view', 'zip', 'admin', now(), '', NULL, '审计项目归档'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = 'archive' AND component = 'audit/archive');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '归档维护', m.menu_id, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'audit:archive:edit', '#', 'admin', now(), '', NULL, '项目归档提交、审核、删除'
FROM sys_menu m
WHERE m.path = 'archive' AND m.component = 'audit/archive'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'audit:archive:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '临时授权', 1, 10, 'tempAuth', 'audit/tempAuth', '', '', 1, 0, 'C', '0', '0', 'audit:auth:view', 'lock', 'admin', now(), '', NULL, '审计临时授权'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = 'tempAuth' AND component = 'audit/tempAuth');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1061, 'AI分析', 1205, 1, 'aiChat', 'ai/chat', '', '', 1, 0, 'C', '0', '0', '', 'user', 'admin', now(), '', NULL, 'AI分析入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1061);

-- ===== 3. 按流程归类排序 =====
-- 审计资源：先管对象，再管依据和知识沉淀
UPDATE sys_menu SET parent_id = 1200, order_num = 1, menu_name = '审计对象', path = 'unit', component = 'audit/unit', visible = '0', status = '0' WHERE menu_id = 1079;
UPDATE sys_menu SET parent_id = 1200, order_num = 2, menu_name = '领导干部库', visible = '0', status = '0' WHERE path = 'leader' AND component = 'audit/leader';
UPDATE sys_menu SET parent_id = 1200, order_num = 3, menu_name = '依据库管理', path = 'basis', component = 'ai/basis', visible = '0', status = '0' WHERE menu_id = 1063;
UPDATE sys_menu SET parent_id = 1200, order_num = 4, menu_name = '问题风险库', path = 'risk', component = 'audit/risk', visible = '0', status = '0' WHERE menu_id = 1076;
UPDATE sys_menu SET parent_id = 1200, order_num = 5, menu_name = '案例库', path = 'case', component = 'audit/case', visible = '0', status = '0' WHERE menu_id = 1077;

-- 计划立项：从计划到项目库，再进入具体项目
UPDATE sys_menu SET parent_id = 1201, order_num = 1, menu_name = '审计计划', path = 'plan', component = 'audit/plan', visible = '0', status = '0' WHERE menu_id = 1078;
UPDATE sys_menu SET parent_id = 1201, order_num = 2, menu_name = '项目库', path = 'projectLib', component = 'audit/projectLib', visible = '0', status = '0' WHERE menu_id = 1094;
UPDATE sys_menu SET parent_id = 1201, order_num = 3, menu_name = '项目工作台', path = 'project-workspace', component = 'audit/project', visible = '0', status = '0' WHERE menu_id = 1083;

-- 审计准备：方案模板、准备事项、资料上传/文档
UPDATE sys_menu SET parent_id = 1202, order_num = 1, menu_name = '方案模板库', visible = '0', status = '0' WHERE path = 'schemeTemplate' AND component = 'audit/schemeTemplate';
UPDATE sys_menu SET parent_id = 1202, order_num = 2, menu_name = '审计准备', visible = '0', status = '0' WHERE path = 'prepare' AND component = 'audit/prepare';
UPDATE sys_menu SET parent_id = 1202, order_num = 3, menu_name = '上传中心', path = 'upload', component = 'knowledge/upload', visible = '0', status = '0' WHERE menu_id = 1093;
UPDATE sys_menu SET parent_id = 1202, order_num = 4, menu_name = '项目文档', path = 'projectDoc', component = 'audit/projectLib', visible = '1', status = '1' WHERE menu_id = 1089;

-- 审计实施：进度、取证、底稿、问题
UPDATE sys_menu SET parent_id = 1203, order_num = 1, menu_name = '项目进度', path = 'progress', component = 'audit/progress', visible = '0', status = '0' WHERE menu_id = 1080;
UPDATE sys_menu SET parent_id = 1203, order_num = 2, menu_name = 'AI取证分析', path = 'forensic', component = 'ai/forensic', visible = '0', status = '0' WHERE menu_id = 1069;
UPDATE sys_menu SET parent_id = 1203, order_num = 3, menu_name = '底稿管理', path = 'workpaper', component = 'audit/workpaper', visible = '0', status = '0' WHERE menu_id = 1084;
UPDATE sys_menu SET parent_id = 1203, order_num = 4, menu_name = '审计问题', visible = '0', status = '0' WHERE path = 'issue' AND component = 'audit/issue';

-- 报告整改归档：报告、整改、归档
UPDATE sys_menu SET parent_id = 1204, order_num = 1, menu_name = '审计报告', path = 'report', component = 'audit/report', visible = '0', status = '0' WHERE menu_id = 1085;
UPDATE sys_menu SET parent_id = 1204, order_num = 2, menu_name = '整改台账', path = 'rectification', component = 'audit/rectification', visible = '0', status = '0' WHERE menu_id = 1074;
UPDATE sys_menu SET parent_id = 1204, order_num = 3, menu_name = '项目归档', visible = '0', status = '0' WHERE path = 'archive' AND component = 'audit/archive';

-- 智能分析支撑：作为审计过程的辅助工具集中展示
DELETE FROM sys_role_menu WHERE menu_id = 1092;
DELETE FROM sys_menu WHERE menu_id = 1092 OR (path = 'dataAnalyze' AND component IN ('ai/dataAnalyze', 'ai/dataDashboard'));
UPDATE sys_menu SET parent_id = 1205, order_num = 1, menu_name = 'AI分析', path = 'aiChat', component = 'ai/chat', visible = '0', status = '0' WHERE menu_id = 1061;
UPDATE sys_menu SET parent_id = 1205, order_num = 2, menu_name = '数据可视化', path = 'visualization/index', component = 'ai/visualization/index', visible = '0', status = '0' WHERE menu_id = 1095;
UPDATE sys_menu SET parent_id = 1205, order_num = 3, menu_name = '调用日志', path = 'aiLog', component = 'ai/aiLog', visible = '0', status = '0' WHERE menu_id = 1072;

-- 临时授权属于权限支撑，保留在系统管理下，排在菜单管理之后。
UPDATE sys_menu
SET parent_id = (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name = '系统管理' AND parent_id = 0 LIMIT 1) t),
    order_num = 10,
    menu_name = '临时授权'
WHERE path = 'tempAuth' AND component = 'audit/tempAuth';

-- 原 AI 智能辅助目录已拆分到流程分类下，隐藏空目录，避免点击后出现空白。
UPDATE sys_menu SET visible = '1', order_num = 20 WHERE menu_id = 1062 AND menu_type = 'M';

-- RuoYi 默认系统监控收尾；删除系统工具和官网外链。
DELETE FROM sys_role_menu WHERE menu_id = 4;
DELETE FROM sys_menu WHERE menu_id = 4 OR menu_name = '若依官网';
UPDATE sys_menu SET order_num = 98 WHERE menu_id = 2 AND menu_name = '系统监控' AND parent_id = 0;

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

-- 清理旧版菜单重组脚本遗留的空白审计顶级目录。
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

-- ===== 4. 管理员授权新目录和补齐菜单 =====
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE (m.menu_id IN (1200, 1201, 1202, 1203, 1204, 1205)
   OR m.component IN (
        'audit/unit',
        'audit/leader',
        'ai/basis',
        'audit/risk',
        'audit/case',
        'audit/plan',
        'audit/projectLib',
        'audit/project',
        'audit/schemeTemplate',
        'audit/prepare',
        'knowledge/upload',
        'ai/forensic',
        'audit/workpaper',
        'audit/issue',
        'audit/report',
        'audit/rectification',
        'audit/archive',
        'ai/visualization/index',
        'ai/aiLog',
        'ai/chat',
        'audit/tempAuth'
      ))
AND NOT EXISTS (
  SELECT 1
  FROM sys_role_menu rm
  WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
);

-- ===== 5. 查看重排结果 =====
SELECT parent.menu_name AS category,
       child.order_num,
       child.menu_name,
       child.path,
       child.component,
       child.perms
FROM sys_menu parent
JOIN sys_menu child ON child.parent_id = parent.menu_id
WHERE parent.menu_id BETWEEN 1200 AND 1205
ORDER BY parent.order_num, child.order_num, child.menu_id;
