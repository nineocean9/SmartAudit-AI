-- Allow audited unit liaisons to confirm submitted preparation materials.
-- This keeps the workflow usable for units such as Library that have a liaison account
-- but no separate principal demo account.

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms = 'audit:prepare:confirm'
WHERE r.role_key = 'audited_unit_liaison'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.role_id = r.role_id
      AND rm.menu_id = m.menu_id
  );
