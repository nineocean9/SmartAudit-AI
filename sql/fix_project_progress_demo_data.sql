-- 项目进度演示数据修复：正式项目名称、合理时间线、超期状态
UPDATE audit_project
SET project_name = '信息工程学院2024年经济责任审计',
    audited_unit = '信息工程学院',
    audit_type = '经责审计',
    audit_year = 2024,
    start_date = '2024-03-01',
    end_date = '2024-06-30',
    progress = 100,
    phase = '归档',
    is_overdue = 0,
    status = 2
WHERE id = 1;

UPDATE audit_project
SET project_name = '商学院2023年财务收支审计',
    audited_unit = '商学院',
    audit_type = '财务收支',
    audit_year = 2023,
    start_date = '2023-09-01',
    end_date = '2023-12-20',
    progress = 100,
    phase = '归档',
    is_overdue = 0,
    status = 2
WHERE id = 2;

UPDATE audit_project
SET project_name = '后勤处2024年专项审计',
    audited_unit = '后勤处',
    audit_type = '专项审计',
    audit_year = 2024,
    start_date = '2024-05-10',
    end_date = '2024-09-30',
    progress = 100,
    phase = '归档',
    is_overdue = 0,
    status = 2
WHERE id = 3;

UPDATE audit_project
SET project_name = '财务处2026年预算执行审计',
    audited_unit = '财务处',
    audit_type = '预算执行',
    audit_year = 2026,
    start_date = '2026-02-20',
    end_date = '2026-06-30',
    progress = 82,
    phase = '整改跟踪',
    is_overdue = 1,
    status = 1
WHERE id = 4;

UPDATE audit_project
SET project_name = '图书馆2026年工程审计',
    audited_unit = '图书馆',
    audit_type = '工程审计',
    audit_year = 2026,
    start_date = '2026-04-01',
    end_date = '2026-08-31',
    progress = 56,
    phase = '现场实施',
    is_overdue = 0,
    status = 1
WHERE id = 5;

UPDATE audit_project
SET project_name = '信息工程学院2026年预算执行审计',
    audited_unit = '信息工程学院',
    audit_type = '预算执行',
    audit_year = 2026,
    start_date = '2026-05-20',
    end_date = '2026-10-31',
    progress = 35,
    phase = '现场实施',
    is_overdue = 0,
    status = 1
WHERE id = 6;

UPDATE audit_project
SET project_name = '科研经费2026年专项审计',
    audited_unit = '科学技术研究院',
    audit_type = '专项审计',
    audit_year = 2026,
    start_date = '2026-09-01',
    end_date = '2026-12-15',
    progress = 0,
    phase = '准备',
    is_overdue = 0,
    status = 0
WHERE id = 7;

UPDATE audit_project
SET project_name = '资产经营公司2026年度财务收支审计',
    audited_unit = '资产经营公司',
    audit_type = '财务收支',
    audit_year = 2026,
    start_date = '2026-10-08',
    end_date = '2026-12-31',
    progress = 0,
    phase = '准备',
    is_overdue = 0,
    status = 0
WHERE id = 8;
