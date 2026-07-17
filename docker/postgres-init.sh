#!/usr/bin/env bash
set -euo pipefail

scripts=(
  ry_20250522.sql
  quartz.sql
  ai_init_pg.sql
  ai.sql
  module_1.sql
  demo_project_data.sql
  module_2.sql
  modules_34.sql
  v2_feature_enhance.sql
  ai_workspace.sql
  extend.sql
  rag_case_extend.sql
  analysis_result.sql
  analysis_result_alter.sql
  menu_34.sql
  alter_ops_word_columns.sql
  audit_flow_menu_reorg.sql
  menu_reorg.sql
  fix_ai_menu_blank.sql
  audit_role_user_seed.sql
  realistic_project_progress_seed.sql
  fix_user_display_names.sql
  fix_material_submitter_accounts.sql
  fix_liaison_material_confirm_permission.sql
  fix_ai_forensic_permissions.sql
  fix_audited_unit_menu_permissions.sql
  enrich_audit_basis_common.sql
  patch_v392.sql
  unify_audit_unit_to_sys_dept.sql
  fix_archive_demo_data.sql
  fix_audited_unit_principal_login.sql
  fix_demo_users_role_permissions.sql
  fix_project_progress_demo_data.sql
  replace_scheme_templates_word.sql
)

for script in "${scripts[@]}"; do
  echo "[database-init] running ${script}"
  psql --set ON_ERROR_STOP=on --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --file "/sql/${script}" 2>/dev/null || echo "[database-init] ${script} 执行失败(可能已执行过)"
done
