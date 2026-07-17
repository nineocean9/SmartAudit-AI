#!/usr/bin/env bash
set -euo pipefail

scripts=(
  ry_20250522.sql
  quartz.sql
  ai_init_pg.sql
  module_1.sql
  demo_project_data.sql
  module_2.sql
  modules_34.sql
  v2_feature_enhance.sql
  ai_workspace.sql
  rag_case_extend.sql
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
)

for script in "${scripts[@]}"; do
  echo "[database-init] running ${script}"
  psql --set ON_ERROR_STOP=on --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --file "/sql/${script}"
done
