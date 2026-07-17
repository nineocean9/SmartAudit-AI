-- 被审计单位角色不参与审计组内部实施作业，移除“审计实施”父菜单。
DELETE FROM sys_role_menu rm
USING sys_role r
WHERE rm.role_id = r.role_id
  AND r.role_key IN ('audited_unit_principal', 'audited_unit_liaison')
  AND rm.menu_id = 1203;
