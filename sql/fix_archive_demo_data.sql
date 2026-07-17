DELETE FROM audit_archive WHERE project_id BETWEEN 1 AND 8;

INSERT INTO audit_archive (project_id, archive_no, archive_status, archive_category, file_name, file_path, sort_order, review_by, review_time, archive_time, create_by, create_time) VALUES
(1, 'DA-2024-JJZR-001-01', 2, '立项类', '信息工程学院2024年经济责任审计立项资料.docx', '/audit-template/default/economic-responsibility-audit-plan-template.docx', 10, 'audit_director', '2024-06-28', '2024-06-30', 'audit_project_leader', '2024-06-25'),
(1, 'DA-2024-JJZR-001-02', 2, '证明类', '信息工程学院2024年经济责任审计底稿汇编.docx', '/audit-template/default/economic-responsibility-audit-plan-template.docx', 20, 'audit_director', '2024-06-28', '2024-06-30', 'audit_project_leader', '2024-06-25'),
(1, 'DA-2024-JJZR-001-03', 2, '结论类', '信息工程学院2024年经济责任审计报告.docx', '/audit-template/default/audit-report-draft-template.docx', 30, 'audit_director', '2024-06-28', '2024-06-30', 'audit_project_leader', '2024-06-25'),
(1, 'DA-2024-JJZR-001-04', 2, '整改类', '信息工程学院2024年经济责任审计整改销号资料.docx', '/audit-template/default/audit-report-draft-template.docx', 40, 'audit_director', '2024-06-28', '2024-06-30', 'audit_project_leader', '2024-06-25'),
(2, 'DA-2023-CWSZ-002-01', 2, '立项类', '商学院2023年财务收支审计方案.docx', '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx', 10, 'audit_director', '2023-12-18', '2023-12-20', 'audit_project_leader', '2023-12-15'),
(2, 'DA-2023-CWSZ-002-02', 2, '结论类', '商学院2023年财务收支审计报告.docx', '/audit-template/default/audit-report-draft-template.docx', 30, 'audit_director', '2023-12-18', '2023-12-20', 'audit_project_leader', '2023-12-15'),
(2, 'DA-2023-CWSZ-002-03', 2, '备查类', '商学院2023年财务收支审计会议纪要.docx', '/audit-template/default/audit-report-draft-template.docx', 50, 'audit_director', '2023-12-18', '2023-12-20', 'audit_project_leader', '2023-12-15'),
(3, 'DA-2024-ZX-003-01', 2, '立项类', '后勤处2024年专项审计方案.docx', '/audit-template/default/special-audit-plan-template.docx', 10, 'audit_director', '2024-09-28', '2024-09-30', 'audit_project_leader', '2024-09-25'),
(3, 'DA-2024-ZX-003-02', 2, '证明类', '后勤处2024年专项审计取证材料.docx', '/audit-template/default/special-audit-plan-template.docx', 20, 'audit_director', '2024-09-28', '2024-09-30', 'audit_project_leader', '2024-09-25'),
(3, 'DA-2024-ZX-003-03', 2, '结论类', '后勤处2024年专项审计报告.docx', '/audit-template/default/audit-report-draft-template.docx', 30, 'audit_director', '2024-09-28', '2024-09-30', 'audit_project_leader', '2024-09-25'),
(4, 'DA-2026-YSZX-004-01', 1, '立项类', '财务处2026年预算执行审计方案.docx', '/audit-template/default/special-audit-plan-template.docx', 10, 'audit_project_leader', '2026-06-28', NULL, 'audit_project_leader', '2026-06-28'),
(4, 'DA-2026-YSZX-004-02', 1, '结论类', '财务处2026年预算执行审计征求意见稿.docx', '/audit-template/default/audit-report-draft-template.docx', 30, 'audit_project_leader', '2026-06-28', NULL, 'audit_project_leader', '2026-06-28'),
(5, 'DA-2026-GC-005-01', 0, '立项类', '图书馆2026年工程审计方案.docx', '/audit-template/default/engineering-audit-plan-template.docx', 10, NULL, NULL, NULL, 'audit_project_leader', '2026-07-01'),
(6, 'DA-2026-YSZX-006-01', 0, '立项类', '信息工程学院2026年预算执行审计方案.docx', '/audit-template/default/special-audit-plan-template.docx', 10, NULL, NULL, NULL, 'audit_project_leader', '2026-07-05');
