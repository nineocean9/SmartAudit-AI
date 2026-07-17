# PostgreSQL SQL 执行顺序

当前项目使用 PostgreSQL 时，先跑 `ry_20250522.sql`。`ry_20260320.sql` 是 MySQL 版脚本，不要在 PostgreSQL 库里执行。

推荐顺序：

1. `ry_20250522.sql`
2. `quartz.sql`
3. `ai_init_pg.sql`
4. `module_1.sql`
5. `demo_project_data.sql`
6. `module_2.sql`
7. `modules_34.sql`
8. `v2_feature_enhance.sql`
9. `ai_workspace.sql`
10. `rag_case_extend.sql`
11. `audit_flow_menu_reorg.sql`
12. `menu_reorg.sql`
13. `fix_ai_menu_blank.sql`
14. `audit_role_user_seed.sql`
15. `realistic_project_progress_seed.sql`
16. `fix_user_display_names.sql`
17. `fix_material_submitter_accounts.sql`
18. `fix_liaison_material_confirm_permission.sql`

说明：

- `audit_role_user_seed.sql` 依赖 RuoYi 的 `sys_dept/sys_role/sys_user/sys_menu` 等基础权限表，必须放在后面。
- `realistic_project_progress_seed.sql` 用于补齐审计计划、关联项目、审前资料、方案、报告、归档和项目文档演示数据，建议在全部表结构与菜单脚本执行后最后运行。
- `demo_project_data.sql` 会生成审计项目、问题和整改演示数据，要在 `ai_init_pg.sql` 和 `module_1.sql` 之后执行。
- `v2_feature_enhance.sql` 会扩展 `audit_project/audit_plan/audit_basis` 等表，因此要在 demo 项目表创建之后执行。
- 角色统一后的演示账号默认密码沿用原始 `admin` 密码哈希，正式环境需要改密。
