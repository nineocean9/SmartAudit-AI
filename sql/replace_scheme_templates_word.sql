-- 一次性替换方案模板库默认数据为 Word 模板
-- 执行对象：当前 PostgreSQL 开发库 ry-vue

ALTER TABLE audit_scheme_template ADD COLUMN IF NOT EXISTS file_url VARCHAR(500);

DELETE FROM audit_scheme_template
WHERE create_by = 'admin'
  AND (file_url IS NULL OR file_url = '')
  AND template_name IN (
    '经济责任审计方案模板',
    '财务收支审计方案模板',
    '专项审计方案模板',
    '工程审计方案模板'
  );

INSERT INTO audit_scheme_template (template_name, audit_type, content, file_url, status, create_by, create_time)
SELECT '经济责任审计实施方案模板（Word版）', '经济责任审计', '[Word模板] 经济责任审计实施方案模板（Word版）', '/audit-template/default/economic-responsibility-audit-plan-template.docx', 1, 'admin', now()
WHERE NOT EXISTS (SELECT 1 FROM audit_scheme_template WHERE template_name = '经济责任审计实施方案模板（Word版）' AND file_url IS NOT NULL AND file_url <> '');

INSERT INTO audit_scheme_template (template_name, audit_type, content, file_url, status, create_by, create_time)
SELECT '财务收支审计实施方案模板（Word版）', '财务收支审计', '[Word模板] 财务收支审计实施方案模板（Word版）', '/audit-template/default/financial-revenue-expenditure-audit-plan-template.docx', 1, 'admin', now()
WHERE NOT EXISTS (SELECT 1 FROM audit_scheme_template WHERE template_name = '财务收支审计实施方案模板（Word版）' AND file_url IS NOT NULL AND file_url <> '');

INSERT INTO audit_scheme_template (template_name, audit_type, content, file_url, status, create_by, create_time)
SELECT '专项审计实施方案模板（Word版）', '专项审计', '[Word模板] 专项审计实施方案模板（Word版）', '/audit-template/default/special-audit-plan-template.docx', 1, 'admin', now()
WHERE NOT EXISTS (SELECT 1 FROM audit_scheme_template WHERE template_name = '专项审计实施方案模板（Word版）' AND file_url IS NOT NULL AND file_url <> '');

INSERT INTO audit_scheme_template (template_name, audit_type, content, file_url, status, create_by, create_time)
SELECT '工程审计实施方案模板（Word版）', '工程审计', '[Word模板] 工程审计实施方案模板（Word版）', '/audit-template/default/engineering-audit-plan-template.docx', 1, 'admin', now()
WHERE NOT EXISTS (SELECT 1 FROM audit_scheme_template WHERE template_name = '工程审计实施方案模板（Word版）' AND file_url IS NOT NULL AND file_url <> '');
