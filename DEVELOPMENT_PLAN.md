# 高校智慧审计平台 — 功能补充开发计划

> 制定日期：2026-07-13 | 截止日期：2026-07-17 | 基于需求文档 + 现有代码分析

---

## 〇、现状盘点

### 已有功能（可直接复用，无需重写）

| 模块 | 已有功能 | 对应文件 |
|------|---------|---------|
| 审计计划 | 基础 CRUD + 绑定项目 + AI推荐 | `AuditInfoController` / `plan.vue` |
| 被审单位 | 基础 CRUD + AI画像 | `AuditInfoController` / `unit.vue` |
| 领导干部 | 基础 CRUD（字段少） | `AuditLeader` domain |
| 审计项目 | 基础 CRUD（字段少） | `AuditProject` domain |
| 审计底稿 | CRUD + 关联项目/依据 | `AuditOpsController` / `workpaper.vue` |
| 审计报告 | CRUD + 版本类型 | `AuditOpsController` / `report.vue` |
| 审计问题 | CRUD + 按项目/严重度筛选 | `AuditIssueController` / `issue.vue` |
| 整改跟踪 | 基础 CRUD（字段少） | `AuditRectificationController` / `rectification.vue` |
| 审计依据库 | CRUD + 语义搜索 + 状态切换 | `AuditBasisController` / `basis.vue` |
| 案例库 | CRUD + 向量化 | `AuditCaseController` / `case.vue` |
| 风险案例库 | CRUD + 向量化 | `AuditRiskCaseController` / `risk.vue` |
| AI聊天 | 7种意图(QA/分析/取证/风险/核查等) | `AiChatController` / `chat.vue` |
| 数据分析 | AI生成HTML驾驶舱 + Markdown总结 | `AiDataAnalyzeController` |
| 取证稿 | AI生成 + CRUD | `AiForensicController` / `forensic.vue` |
| 数据驾驶舱 | 基础统计（项目/问题/整改） | `DashboardController` / `dataDashboard.vue` |
| 项目文档 | 上传+解析+向量化 | `ProjectDocController` / `projectLib.vue` |
| RAG知识库 | 5个知识源 + pgvector搜索 | `AuditRagServiceImpl` |
| 进度管理 | 基础页面（已有progress.vue） | `progress.vue` |

### 需要补充的8大功能方向

| 序号 | 功能方向 | 优先级 | 预估工作量 |
|------|---------|--------|-----------|
| ① | 审计计划增强 | ⭐⭐⭐ | 中 |
| ② | 审计对象库完善 | ⭐⭐⭐ | 中 |
| ③ | 审计进度可视化 | ⭐⭐ | 中 |
| ④ | 审计准备阶段 | ⭐⭐⭐ | 大 |
| ⑤ | 报告与归档 | ⭐⭐ | 大 |
| ⑥ | 审计依据库增强 | ⭐⭐ | 小 |
| ⑦ | AI能力增强 | ⭐⭐ | 中 |
| ⑧ | 分级权限体系 | ⭐⭐⭐ | 中 |

---

## 一、审计计划增强（第1批开发）

### 1.1 需求与现状差距

| 需求功能 | 现状 | 需要做的事 |
|---------|------|-----------|
| 附件上传（委托书/会议决议等） | 仅有单个fileUrl字段 | 新增附件表 + 多文件上传组件 |
| 多格式在线预览 | 无 | 前端集成 PDF/Word/Excel/图片预览 |
| 审计方案绑定 | 无方案库 | 新增方案模板表 + 按批次绑定 |
| Excel批量导入项目 | 无 | 后端POI解析 + 前端导入弹窗 |
| 智能推荐待审对象 | 已有AI推荐（简单版） | 增强多维度规则引擎 |
| 多条件组合筛选 | 基础筛选 | 前端增加高级筛选面板 |
| 计划预警跟踪 | 无 | 定时任务 + 站内消息提醒 |
| 计划变更管理 | 无 | 新增变更记录表 + 变更日志 |
| 计划穿透查询 | 无 | 前端穿透链接跳转 |
| Excel导出 | 无 | 后端导出接口 |

### 1.2 数据库变更

```sql
-- ===== 1. 审计计划附件表 =====
CREATE TABLE audit_plan_attachment (
    id            BIGSERIAL PRIMARY KEY,
    plan_id       BIGINT       NOT NULL,               -- 关联审计计划
    file_name     VARCHAR(255) NOT NULL,               -- 原始文件名
    file_path     VARCHAR(500) NOT NULL,               -- 服务器存储路径
    file_type     VARCHAR(20),                         -- 文件类型: pdf/docx/xlsx/jpg
    file_size     BIGINT,                              -- 文件大小(字节)
    attachment_type VARCHAR(50),                       -- 附件分类: 委托书/会议决议/任务文件/调研材料
    create_by     VARCHAR(64),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_plan_attach FOREIGN KEY (plan_id) REFERENCES audit_plan(id)
);

-- ===== 2. 审计方案模板表 =====
CREATE TABLE audit_scheme_template (
    id            BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(200) NOT NULL,               -- 模板名称
    audit_type    VARCHAR(50)  NOT NULL,               -- 适用审计类型: 经责/财务/专项/工程
    content       TEXT,                                -- 模板内容(富文本)
    status        INT DEFAULT 1,                       -- 1=启用 0=停用
    create_by     VARCHAR(64),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 3. 计划-方案绑定关系表 =====
CREATE TABLE audit_plan_scheme (
    id            BIGSERIAL PRIMARY KEY,
    plan_id       BIGINT NOT NULL,
    template_id   BIGINT,                              -- 来源模板(可空=自定义)
    scheme_name   VARCHAR(200),
    content       TEXT,                                -- 实际方案内容(可从模板复制后修改)
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 4. 计划变更记录表 =====
CREATE TABLE audit_plan_change_log (
    id            BIGSERIAL PRIMARY KEY,
    plan_id       BIGINT       NOT NULL,
    change_type   VARCHAR(20)  NOT NULL,               -- 调增/调减/修改周期
    before_json   TEXT,                                -- 变更前快照(JSON)
    after_json    TEXT,                                -- 变更后快照(JSON)
    change_reason VARCHAR(500),                        -- 变更原因
    change_by     VARCHAR(64),
    change_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 5. audit_plan 表扩展字段 =====
ALTER TABLE audit_plan ADD COLUMN IF NOT EXISTS plan_start_date DATE;        -- 计划开始日期
ALTER TABLE audit_plan ADD COLUMN IF NOT EXISTS plan_end_date   DATE;        -- 计划结束日期
ALTER TABLE audit_plan ADD COLUMN IF NOT EXISTS approval_status INT DEFAULT 0; -- 审批状态 0待审批 1通过 2驳回
ALTER TABLE audit_plan ADD COLUMN IF NOT EXISTS description     TEXT;          -- 计划描述

-- ===== 6. audit_project 表扩展字段 =====
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS plan_id        BIGINT;      -- 关联计划ID
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS leader_id      BIGINT;      -- 审计组长
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS start_date     DATE;        -- 实施开始日期
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS end_date       DATE;        -- 实施结束日期
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS coverage_start DATE;        -- 覆盖期间起
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS coverage_end   DATE;        -- 覆盖期间止
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS is_outsourced  INT DEFAULT 0; -- 是否委托中介
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS budget         DECIMAL(14,2); -- 资金规模
```

### 1.3 后端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `AuditPlanAttachment.java` | **新建** | 附件实体类 |
| `AuditSchemeTemplate.java` | **新建** | 方案模板实体类 |
| `AuditPlanChangeLog.java` | **新建** | 变更记录实体类 |
| `AuditPlan.java` | **修改** | 新增 planStartDate/planEndDate/approvalStatus/description 字段 |
| `AuditProject.java` | **修改** | 新增 planId/leaderId/startDate/endDate/coverageStart/coverageEnd/isOutsourced/budget 字段 |
| `AuditPlanAttachMapper.java` | **新建** | 附件 Mapper + XML |
| `AuditSchemeTemplateMapper.java` | **新建** | 方案模板 Mapper + XML |
| `AuditPlanChangeLogMapper.java` | **新建** | 变更记录 Mapper + XML |
| `AuditInfoMapper.xml` | **修改** | 新增字段映射、高级筛选SQL |
| `AuditInfoServiceImpl.java` | **修改** | ①Excel批量导入项目 ②智能推荐增强(多维度规则) ③计划变更逻辑 ④Excel导出 |
| `AuditInfoController.java` | **修改** | 新增接口：附件上传/下载/删除、方案绑定、Excel导入导出、变更记录、高级筛选 |
| `AuditPlanWarnJob.java` | **新建** | 定时任务：扫描超期项目 → 推送站内消息 |

### 1.4 前端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `plan.vue` | **修改** | ①高级筛选面板(年度/类型/状态/审批状态组合) ②附件上传区域(多文件+分类) ③方案绑定Tab ④变更记录弹窗 ⑤穿透查询(点击项目名跳转) ⑥Excel导入按钮 ⑦Excel导出按钮 |
| `schemeTemplate.vue` | **新建** | 审计方案模板管理页面（CRUD + 富文本编辑器） |
| `api/audit/planAttach.js` | **新建** | 附件API |
| `api/audit/schemeTemplate.js` | **新建** | 方案模板API |

---

## 二、审计对象库完善（第1批开发）

### 2.1 需求与现状差距

| 需求功能 | 现状 | 需要做的事 |
|---------|------|-----------|
| 分类分级管理(职能部门/学院/企业等) | 仅有unitType字段 | 丰富分类字典 + 前端分组展示 |
| 完整档案字段 | 仅5个字段 | 扩展10+字段 |
| 历年审计记录自动关联 | 无 | 从audit_project自动聚合 |
| 经济责任领导干部库完善 | AuditLeader仅有基础字段 | 大幅扩展字段 |
| 信息自动回填 | 无 | 前端选择单位后自动填充关联字段 |
| 动态更新日志 | 无 | 新增更新日志记录 |

### 2.2 数据库变更

```sql
-- ===== 1. audit_unit 表扩展字段 =====
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS unit_code       VARCHAR(50);   -- 单位编码
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS parent_leader   VARCHAR(100);  -- 分管校领导
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS staff_count     INT;           -- 编制人数
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS annual_budget   DECIMAL(14,2); -- 年度经费规模
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS finance_contact VARCHAR(100);  -- 财务联系人
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS contact_phone   VARCHAR(20);   -- 联系电话
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS address         VARCHAR(200);  -- 办公地址
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS last_audit_date DATE;          -- 上次审计日期
ALTER TABLE audit_unit ADD COLUMN IF NOT EXISTS status          INT DEFAULT 1; -- 状态 1正常 0停用

-- ===== 2. audit_leader 表扩展字段 =====
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS gender        VARCHAR(10);
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS id_number     VARCHAR(20);   -- 工号
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS position_start DATE;          -- 现任职务起始日期
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS position_end   DATE;          -- 现任职务结束日期
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS managed_funds  DECIMAL(14,2); -- 分管资金规模
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS managed_scope  VARCHAR(500);  -- 分管业务范围
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS position_history TEXT;         -- 任职履历(JSON数组)
ALTER TABLE audit_leader ADD COLUMN IF NOT EXISTS audit_evaluation TEXT;        -- 历次审计评价汇总

-- ===== 3. 单位更新日志表 =====
CREATE TABLE audit_unit_change_log (
    id           BIGSERIAL PRIMARY KEY,
    unit_id      BIGINT      NOT NULL,
    field_name   VARCHAR(50) NOT NULL,                 -- 变更字段
    old_value    TEXT,
    new_value    TEXT,
    change_by    VARCHAR(64),
    change_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 2.3 后端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `AuditUnit.java` | **修改** | 新增8个字段 |
| `AuditLeader.java` | **修改** | 新增7个字段 |
| `AuditUnitChangeLog.java` | **新建** | 更新日志实体 |
| `AuditInfoMapper.xml` | **修改** | 新增字段映射 + 关联查询(历年审计记录) |
| `AuditInfoServiceImpl.java` | **修改** | ①自动回填逻辑 ②变更日志记录 ③历年审计记录聚合查询 ④领导干部经责计划联动 |
| `AuditInfoController.java` | **修改** | 新增接口：单位详情(含历年审计)、领导干部详情(含审计履历)、变更日志查询 |

### 2.4 前端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `unit.vue` | **修改** | ①详情抽屉(展示完整档案+历年审计记录时间线) ②分类分组展示(树形/标签页) ③新增字段表单 |
| `leader.vue` | **新建** | 领导干部库独立页面：任职履历时间线 + 审计履历 + 经责计划联动 |
| `api/audit/auditInfo.js` | **修改** | 新增API |

---

## 三、审计进度可视化（第2批开发）

### 3.1 需求与现状差距

| 需求功能 | 现状 | 需要做的事 |
|---------|------|-----------|
| 甘特图展示项目进度 | progress.vue已存在但功能简单 | 集成甘特图组件 |
| 穿透式联动 | 无 | 点击项目名跳转详情 |
| 资源调度视图 | 无 | 展示审计组长/人员负载 |
| 进度预警 | 无 | 超期标红 + 消息推送 |

### 3.2 数据库变更

```sql
-- audit_project 表扩展字段（如未在第一步添加）
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS progress      INT DEFAULT 0;   -- 进度百分比 0-100
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS phase         VARCHAR(20);     -- 当前阶段: 准备/实施/报告/归档
ALTER TABLE audit_project ADD COLUMN IF NOT EXISTS is_overdue    INT DEFAULT 0;   -- 是否超期

-- 项目人员分工表（也为审计准备阶段服务）
CREATE TABLE audit_project_member (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,                 -- 系统用户ID
    user_name    VARCHAR(64),                          -- 用户姓名
    role_type    VARCHAR(20) NOT NULL,                 -- 角色: 组长/主审/成员
    task_scope   VARCHAR(500),                         -- 负责核查范围
    task_deadline DATE,                                -- 交付时限
    status       INT DEFAULT 0,                        -- 0进行中 1已完成
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3.3 后端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `AuditProjectMember.java` | **新建** | 项目成员实体 |
| `AuditProgressController.java` | **新建** | 进度管理接口：甘特图数据、资源负载、预警列表 |
| `AuditProgressServiceImpl.java` | **新建** | ①甘特图数据组装 ②人员负载统计 ③超期检测 |
| `AuditProjectMemberMapper.java` | **新建** | 成员 Mapper + XML |
| `AuditProjectMapper.xml` | **修改** | 新增进度/阶段字段映射 |

### 3.4 前端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `progress.vue` | **重写** | ①ECharts甘特图(按年度/类型分组) ②进度条+阶段标签 ③点击穿透跳转 ④超期标红预警 |
| `resourceLoad.vue` | **新建** | 资源调度视图：每个审计组长/成员的项目负载柱状图 |
| `api/audit/progress.js` | **新建** | 进度API |

---

## 四、审计准备阶段（第2批开发）

### 4.1 需求与现状差距

| 需求功能 | 现状 | 需要做的事 |
|---------|------|-----------|
| 审计方案自动生成 | 无 | 按项目类型匹配模板一键生成 |
| 审计通知书 | 无 | 标准化模板 + 自动填充 + 在线发送 |
| 人员分工 | 无 | 项目成员管理（已在进度模块建表） |
| 审前资料收集 | 仅有项目文档上传 | 资料清单 + 被审单位上传 + 审计人员上传 |

### 4.2 数据库变更

```sql
-- ===== 1. 审计通知书表 =====
CREATE TABLE audit_notice (
    id            BIGSERIAL PRIMARY KEY,
    project_id    BIGINT      NOT NULL,                -- 关联项目
    notice_no     VARCHAR(50),                         -- 通知书编号
    content       TEXT,                                -- 通知书内容(富文本)
    send_status   INT DEFAULT 0,                       -- 0未发送 1已发送
    send_time     TIMESTAMP,                           -- 发送时间
    receiver_unit VARCHAR(200),                        -- 接收单位
    receiver_name VARCHAR(100),                        -- 接收人
    material_list TEXT,                                -- 需报送资料清单(JSON数组)
    create_by     VARCHAR(64),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 2. 审前资料清单表 =====
CREATE TABLE audit_material_checklist (
    id            BIGSERIAL PRIMARY KEY,
    project_id    BIGINT      NOT NULL,
    material_name VARCHAR(200) NOT NULL,               -- 资料名称
    material_type VARCHAR(50),                         -- 分类: 财务/人事/采购/资产/其他
    required      INT DEFAULT 1,                       -- 是否必须提供
    submit_status INT DEFAULT 0,                       -- 0未提交 1已提交 2已审核
    file_path     VARCHAR(500),                        -- 上传文件路径
    submit_by     VARCHAR(64),                         -- 提交人
    submit_time   TIMESTAMP,
    source        VARCHAR(20) DEFAULT 'unit',          -- 来源: unit=被审单位 auditor=审计人员
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 3. 审计方案表(项目实际方案，区别于模板) =====
CREATE TABLE audit_scheme (
    id            BIGSERIAL PRIMARY KEY,
    project_id    BIGINT NOT NULL,
    template_id   BIGINT,                              -- 来源模板
    scheme_name   VARCHAR(200),
    content       TEXT,                                -- 方案内容(富文本)
    status        INT DEFAULT 0,                       -- 0草稿 1已定稿
    create_by     VARCHAR(64),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 4.3 后端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `AuditNotice.java` | **新建** | 通知书实体 |
| `AuditMaterialChecklist.java` | **新建** | 资料清单实体 |
| `AuditScheme.java` | **新建** | 审计方案实体 |
| `AuditPrepareController.java` | **新建** | 准备阶段接口：方案生成/通知书/资料清单 |
| `AuditPrepareServiceImpl.java` | **新建** | ①方案自动生成(匹配模板+填充项目信息) ②通知书生成(模板+自动填充) ③资料清单管理 |
| `AuditNoticeMapper.java` | **新建** | + XML |
| `AuditMaterialMapper.java` | **新建** | + XML |
| `AuditSchemeMapper.java` | **新建** | + XML |

### 4.4 前端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `project.vue` | **修改** | 增加Tab：准备阶段(方案/通知书/分工/资料) |
| `projectPrepare.vue` | **新建** | 准备阶段独立页面(或作为project.vue的子组件) |
| `noticeTemplate.vue` | **新建** | 通知书模板编辑+预览+下载Word |
| `materialChecklist.vue` | **新建** | 资料清单管理：勾选/上传/审核状态 |
| `api/audit/prepare.js` | **新建** | 准备阶段API |

---

## 五、报告与归档（第3批开发）

### 5.1 需求与现状差距

| 需求功能 | 现状 | 需要做的事 |
|---------|------|-----------|
| 多版本管理(内审核/征求意见/正式) | 仅有versionType字段 | 版本管理逻辑 + 版本切换UI |
| 征求意见在线流程 | 无 | 意见收发 + 多轮交互 + 留痕 |
| 版本修订对比 | 无 | 文本diff高亮 |
| 项目归档 | 无 | 归档流程 + 档案目录 + 权限查阅 |

### 5.2 数据库变更

```sql
-- ===== 1. 报告意见交互表 =====
CREATE TABLE audit_report_opinion (
    id           BIGSERIAL PRIMARY KEY,
    report_id    BIGINT      NOT NULL,                 -- 关联报告
    round_no     INT DEFAULT 1,                        -- 轮次
    opinion_type VARCHAR(20),                          -- 类型: 征求意见/回复/审计处采纳
    content      TEXT,                                 -- 意见内容
    attachment   VARCHAR(500),                         -- 附件路径
    submit_by    VARCHAR(64),
    submit_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 2. 报告版本快照表 =====
CREATE TABLE audit_report_version (
    id           BIGSERIAL PRIMARY KEY,
    report_id    BIGINT NOT NULL,
    version_no   INT    NOT NULL,                      -- 版本序号
    content      TEXT,                                 -- 该版本内容快照
    change_desc  VARCHAR(500),                         -- 修改说明
    create_by    VARCHAR(64),
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 3. 项目归档表 =====
CREATE TABLE audit_archive (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT NOT NULL,
    archive_no   VARCHAR(50),                          -- 档案编号
    archive_status INT DEFAULT 0,                      -- 0整理中 1待审核 2已归档
    archive_category VARCHAR(20),                      -- 立项类/证明类/结项类/备查类/整改类
    file_name    VARCHAR(200),
    file_path    VARCHAR(500),
    sort_order   INT DEFAULT 0,                        -- 排序
    review_by    VARCHAR(64),                          -- 审核人
    review_time  TIMESTAMP,
    archive_time TIMESTAMP,
    create_by    VARCHAR(64),
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 4. audit_report 扩展字段 =====
ALTER TABLE audit_report ADD COLUMN IF NOT EXISTS title        VARCHAR(200);
ALTER TABLE audit_report ADD COLUMN IF NOT EXISTS version_no   INT DEFAULT 1;    -- 版本序号
ALTER TABLE audit_report ADD COLUMN IF NOT EXISTS opinion_status INT DEFAULT 0;  -- 0未征求 1征求中 2已采纳
```

### 5.3 后端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `AuditReportOpinion.java` | **新建** | 意见实体 |
| `AuditReportVersion.java` | **新建** | 版本快照实体 |
| `AuditArchive.java` | **新建** | 归档实体 |
| `AuditReport.java` | **修改** | 新增title/versionNo/opinionStatus字段 |
| `AuditReportController.java` | **新建** | 独立报告管理接口：版本管理/征求意见/对比/归档 |
| `AuditReportServiceImpl.java` | **新建** | ①版本保存快照 ②征求意见推送 ③文本diff ④归档流程 |
| `AuditArchiveController.java` | **新建** | 归档管理接口 |
| `AuditArchiveServiceImpl.java` | **新建** | 自动归集资料 + 生成目录 + 审核流程 |

### 5.4 前端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `report.vue` | **重写** | ①三版本Tab切换 ②富文本编辑器 ③版本对比弹窗(diff高亮) ④征求意见面板 |
| `archive.vue` | **新建** | 归档管理页面：五大分类目录树 + 文件上传 + 审核按钮 + 权限查阅 |
| `api/audit/report.js` | **新建** | 报告API |
| `api/audit/archive.js` | **新建** | 归档API |

---

## 六、审计依据库增强（第3批开发）

### 6.1 需求与现状差距

| 需求功能 | 现状 | 需要做的事 |
|---------|------|-----------|
| 法条关联维护 | 无 | 新增关联关系表 + 关联展示 |
| 一键插入底稿/报告 | 无 | 底稿/报告编辑器增加"引用法条"按钮 |
| 定期更新与失效标记 | 已有status字段 | 增强批量更新+过期标记UI |
| 多维度检索增强 | 已有语义搜索 | 增加审计事项/资金类型/发文单位组合检索 |

### 6.2 数据库变更

```sql
-- ===== 法条关联关系表 =====
CREATE TABLE audit_basis_relation (
    id            BIGSERIAL PRIMARY KEY,
    basis_id      BIGINT     NOT NULL,                 -- 当前法条
    related_id    BIGINT     NOT NULL,                 -- 关联法条
    relation_type VARCHAR(20) NOT NULL,                -- 关系类型: 上位法/下位法/修订/补充
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== audit_basis 扩展字段 =====
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS doc_number      VARCHAR(100);  -- 文号
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS audit_scope     VARCHAR(200);  -- 适用审计事项
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS fund_type       VARCHAR(100);  -- 适用资金类型
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS expire_date     DATE;          -- 过期日期
ALTER TABLE audit_basis ADD COLUMN IF NOT EXISTS hierarchy_level VARCHAR(20);   -- 位阶: 法律/法规/规章/校内制度
```

### 6.3 后端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `AuditBasisRelation.java` | **新建** | 关联关系实体 |
| `AuditBasis.java` | **修改** | 新增4个字段 |
| `AuditBasisMapper.xml` | **修改** | 新增关联查询 + 多维筛选SQL |
| `AuditBasisServiceImpl.java` | **修改** | ①关联维护 ②批量失效标记 ③多维检索 |
| `AuditBasisController.java` | **修改** | 新增接口：关联管理、批量操作、引用插入 |

### 6.4 前端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `basis.vue` | **修改** | ①关联法条展示(点击查看关联链) ②高级筛选面板(位阶/事项/资金类型/发文单位) ③批量失效按钮 ④一键引用按钮 |
| `workpaper.vue` | **修改** | 编辑器增加"引用法条"侧边栏(搜索+一键插入) |
| `report.vue` | **修改** | 同上 |

---

## 七、AI能力增强（第3批开发）

### 7.1 需求与现状差距

| 需求功能 | 现状 | 需要做的事 |
|---------|------|-----------|
| AI自动匹配定性依据 | 无 | 上传文档→AI匹配法条+提示遗漏 |
| AI审计项目信息查询增强 | 仅READ_PROJECT | 增强意图：查询整改/历年记录/汇总 |
| AI智能推荐审计对象 | 已有简单推荐 | 多维度规则引擎+AI综合评分 |

### 7.2 后端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ChatTaskParserServiceImpl.java` | **修改** | 新增意图类型：MATCH_BASIS(匹配依据)、QUERY_RECTIFICATION(查询整改)、RECOMMEND_OBJECT(推荐对象) |
| `AiChatServiceImpl.java` | **修改** | 新增3个Handler处理逻辑 |
| `ChatTask.java` | **修改** | 新增taskType枚举值 |
| `AiRecommendServiceImpl.java` | **新建** | 智能推荐引擎：①任职年限规则 ②资金规模规则 ③上次审计距今年限 ④AI综合评分 |

### 7.3 前端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `chat.vue` | **修改** | 新增快捷指令按钮（匹配依据/查询整改/推荐对象） |

---

## 八、分级权限体系（第1批开发）

### 8.1 需求与现状差距

| 需求功能 | 现状 | 需要做的事 |
|---------|------|-----------|
| 四类用户角色 | RuoYi基础角色 | 预设4个角色+配置权限 |
| 数据权限隔离 | 无 | RuoYi数据权限注解 + SQL过滤 |
| 中介临时授权 | 无 | 临时账号 + 自动回收 |
| 财务数据强隔离 | 无 | 接口层权限校验 |

### 8.2 实现方案（基于RuoYi已有能力扩展）

**利用RuoYi框架已有的角色-菜单-部门权限体系**，无需重写权限底层：

```
步骤1：创建4个预设角色
  ├── 校领导角色（school_leader）→ 菜单权限：驾驶舱+报告阅览+整改统计
  ├── 被审单位角色（audit_unit）  → 菜单权限：本单位报告+整改+资料提交
  ├── 审计人员角色（auditor）     → 菜单权限：全部审计业务模块
  │   ├── 子角色：普通审计员      → 仅分配项目可见
  │   ├── 子角色：项目组长/主审    → +项目维护+复核
  │   └── 子角色：审计处长        → 全量数据+系统配置
  └── 中介机构角色（outsource）   → 菜单权限：仅分配项目的作业模块

步骤2：数据权限配置
  ├── 使用 @DataScope 注解实现按部门(单位)过滤
  ├── 审计项目查询SQL注入 "AND project_id IN (用户分配的项目)"
  └── 中介用户SQL注入 "AND project_id IN (临时授权的项目) AND 授权未过期"

步骤3：中介临时授权
  ├── 新增 audit_temp_auth 表：user_id + project_id + expire_date
  └── 定时任务：每天检查过期授权 → 自动禁用账号
```

### 8.3 数据库变更

```sql
-- ===== 1. 中介临时授权表 =====
CREATE TABLE audit_temp_auth (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL,                      -- 系统用户ID
    project_id   BIGINT NOT NULL,                      -- 授权项目
    auth_type    VARCHAR(20) DEFAULT 'outsource',      -- 授权类型
    start_date   DATE NOT NULL,                        -- 授权起始
    expire_date  DATE NOT NULL,                        -- 授权到期
    status       INT DEFAULT 1,                        -- 1有效 0已回收
    create_by    VARCHAR(64),
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 2. 预设角色数据（插入sys_role） =====
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status)
VALUES
(100, '校领导',     'school_leader', 10, '1', '0'),
(101, '审计处长',   'audit_director', 11, '1', '0'),
(102, '项目组长',   'audit_leader',   12, '3', '0'),
(103, '普通审计员', 'auditor',        13, '4', '0'),
(104, '被审单位负责人', 'unit_manager', 14, '5', '0'),
(105, '被审单位联络员', 'unit_contact', 15, '5', '0'),
(106, '中介审计人员',   'outsource',   16, '5', '0');
```

### 8.4 后端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `AuditTempAuth.java` | **新建** | 临时授权实体 |
| `AuditTempAuthMapper.java` | **新建** | + XML |
| `AuditPermissionServiceImpl.java` | **新建** | ①临时授权管理 ②过期检测与回收 ③项目级数据权限过滤 |
| `AuditPermissionController.java` | **新建** | 临时授权接口：授权/回收/查询 |
| `AuditTempAuthExpireJob.java` | **新建** | 定时任务：每天扫描过期授权 |
| 各业务Mapper.xml | **修改** | 查询SQL添加数据权限过滤（@DataScope） |
| 各业务ServiceImpl | **修改** | 添加 @DataScope 注解 |

### 8.5 前端开发清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `tempAuth.vue` | **新建** | 临时授权管理页面：授权列表+新增授权弹窗+一键回收 |
| 各业务页面 | **修改** | 根据角色动态显隐按钮（v-hasPermi指令） |
| 菜单SQL | **新建** | 配置菜单权限标识 |

---

## 九、开发排期（总计4天）

### 建议分3批开发，按依赖关系排列：

```
┌─────────────────────────────────────────────────────────────────────┐
│                        开发排期总览                                  │
├──────────┬──────────────────────────────────────┬───────────────────┤
│   批次   │           功能模块                    │    建议日期        │
├──────────┼──────────────────────────────────────┼───────────────────┤
│          │ ⑧ 分级权限体系（基础，被其他模块依赖）  │                   │
│  第1批   │ ① 审计计划增强                        │  7月13日 ~ 7月14日│
│ (基础层) │ ② 审计对象库完善                      │                   │
├──────────┼──────────────────────────────────────┼───────────────────┤
│  第2批   │ ③ 审计进度可视化                      │  7月15日           │
│ (流程层) │ ④ 审计准备阶段                        │                   │
├──────────┼──────────────────────────────────────┼───────────────────┤
│          │ ⑤ 报告与归档                          │                   │
│  第3批   │ ⑥ 审计依据库增强                      │  7月16日           │
│ (增强层) │ ⑦ AI能力增强                          │                   │
├──────────┼──────────────────────────────────────┼───────────────────┤
│  缓冲    │ 联调测试 + 修bug + 写报告             │  7月17日           │
└──────────┴──────────────────────────────────────┴───────────────────┘
```

### 详细开发时间线

#### 📅 第1天（7月13日）：基础层 — 权限 + 计划增强

| 时段 | 任务 | 产出 |
|------|------|------|
| 上午 | ⑧ 分级权限体系 | 7个预设角色 + 临时授权表 + @DataScope注入 + 临时授权页面 |
| 下午 | ① 审计计划增强(前半) | SQL变更 + 附件表/方案模板表/变更记录表 + 后端CRUD |
| 晚上 | ① 审计计划增强(后半) | plan.vue改造(高级筛选+附件+穿透) + Excel导入导出 |

#### 📅 第2天（7月14日）：基础层 — 对象库 + 计划收尾

| 时段 | 任务 | 产出 |
|------|------|------|
| 上午 | ② 审计对象库完善 | SQL扩展 + 后端Service + 单位详情(历年审计聚合) |
| 下午 | ② 领导干部库 + 自动回填 | leader.vue + 任职履历 + 全流程自动回填 |
| 晚上 | 联调 + 修bug | 第1批功能联调通过 |

#### 📅 第3天（7月15日）：流程层 — 进度 + 准备

| 时段 | 任务 | 产出 |
|------|------|------|
| 上午 | ③ 审计进度可视化 | 甘特图(ECharts) + 穿透跳转 + 超期预警标红 |
| 下午 | ④ 审计准备阶段(前半) | 通知书表+方案表+资料清单表 + 后端 |
| 晚上 | ④ 审计准备阶段(后半) | project.vue准备阶段Tab + 通知书生成 + 资料清单UI |

#### 📅 第4天（7月16日）：增强层 — 报告/依据/AI

| 时段 | 任务 | 产出 |
|------|------|------|
| 上午 | ⑤ 报告与归档 | 多版本管理 + 意见表 + 归档表 + report.vue重写 |
| 下午 | ⑤ 归档页面 + ⑥ 依据库增强 | archive.vue + 法条关联 + 一键引用 + 多维检索 |
| 晚上 | ⑦ AI能力增强 | 3个新意图(匹配依据/查询整改/推荐对象) + chat.vue快捷按钮 |

#### 📅 第5天（7月17日）：缓冲 — 测试 + 报告

| 时段 | 任务 | 产出 |
|------|------|------|
| 上午 | 全功能联调测试 | 修复bug + 数据校验 |
| 下午 | 撰写综合实践III报告 | 功能说明 + 界面截图 + 源码打包 |

---

## 十、文件新增/修改汇总

### 新增文件（共约45个）

**后端 Java（约25个）**：
```
domain/
  ├── AuditPlanAttachment.java        (计划附件)
  ├── AuditSchemeTemplate.java        (方案模板)
  ├── AuditPlanChangeLog.java         (计划变更)
  ├── AuditUnitChangeLog.java         (单位变更)
  ├── AuditProjectMember.java         (项目成员)
  ├── AuditNotice.java                (审计通知书)
  ├── AuditMaterialChecklist.java     (资料清单)
  ├── AuditScheme.java                (审计方案)
  ├── AuditReportOpinion.java         (报告意见)
  ├── AuditReportVersion.java         (报告版本)
  ├── AuditArchive.java               (项目归档)
  ├── AuditBasisRelation.java         (法条关联)
  └── AuditTempAuth.java              (临时授权)

mapper/（13个 Mapper + 13个 XML）

controller/
  ├── AuditProgressController.java    (进度管理)
  ├── AuditPrepareController.java     (准备阶段)
  ├── AuditReportController.java      (报告管理-独立)
  ├── AuditArchiveController.java     (归档管理)
  └── AuditPermissionController.java  (权限管理)

service/impl/
  ├── AuditProgressServiceImpl.java   (进度)
  ├── AuditPrepareServiceImpl.java    (准备)
  ├── AuditReportServiceImpl.java     (报告-独立)
  ├── AuditArchiveServiceImpl.java    (归档)
  ├── AuditPermissionServiceImpl.java (权限)
  ├── AuditRecommendServiceImpl.java  (AI推荐)
  ├── AuditPlanWarnJob.java           (计划预警定时任务)
  └── AuditTempAuthExpireJob.java     (授权过期定时任务)
```

**前端 Vue（约10个）**：
```
views/audit/
  ├── leader.vue                      (领导干部库)
  ├── schemeTemplate.vue              (方案模板)
  ├── projectPrepare.vue              (准备阶段)
  ├── noticeTemplate.vue              (通知书)
  ├── materialChecklist.vue           (资料清单)
  ├── archive.vue                     (项目归档)
  ├── resourceLoad.vue                (资源调度)
  └── tempAuth.vue                    (临时授权)

api/audit/
  ├── planAttach.js                   (附件API)
  ├── schemeTemplate.js               (方案模板API)
  ├── prepare.js                      (准备阶段API)
  ├── progress.js                     (进度API)
  ├── report.js                       (报告API)
  └── archive.js                      (归档API)
```

**SQL 脚本（1个合并）**：
```
sql/
  └── v2_feature_enhance.sql          (所有新增表 + 扩展字段 + 预设角色)
```

### 修改文件（共约20个）

**后端**：
- `AuditPlan.java` — 新增4个字段
- `AuditProject.java` — 新增8个字段
- `AuditUnit.java` — 新增8个字段
- `AuditLeader.java` — 新增7个字段
- `AuditBasis.java` — 新增5个字段
- `AuditReport.java` — 新增3个字段
- `AuditInfoMapper.xml` — 高级筛选 + 字段映射
- `AuditInfoServiceImpl.java` — Excel导入导出 + 推荐增强 + 变更逻辑
- `AuditInfoController.java` — 新增10+接口
- `AuditBasisServiceImpl.java` — 关联维护 + 多维检索
- `AuditBasisController.java` — 新增接口
- `ChatTaskParserServiceImpl.java` — 3个新意图
- `AiChatServiceImpl.java` — 3个新Handler
- `ChatTask.java` — 新增枚举值
- 各业务Mapper.xml — 添加@DataScope

**前端**：
- `plan.vue` — 大改(高级筛选+附件+穿透)
- `unit.vue` — 中改(详情抽屉+分类)
- `project.vue` — 中改(增加准备阶段Tab)
- `progress.vue` — 重写(甘特图)
- `report.vue` — 重写(多版本+意见)
- `workpaper.vue` — 小改(引用法条)
- `basis.vue` — 中改(关联+高级筛选)
- `chat.vue` — 小改(快捷按钮)
- `router/index.js` — 新增路由
- 菜单SQL — 新增菜单配置

---

## 十一、技术要点

### 11.1 ECharts甘特图实现
```javascript
// 使用ECharts自定义系列(custom series)实现甘特图
option = {
  xAxis: { type: 'time', min: '2026-01-01', max: '2026-12-31' },
  yAxis: { type: 'category', data: projectNames },
  series: [{
    type: 'custom',
    renderItem: (params, api) => {
      // 绘制每个项目的时间条
      return { type: 'rect', ... }
    }
  }]
}
```

### 11.2 文本Diff对比
```java
// 使用 java-diff-utils 库实现文本对比
// pom.xml 添加依赖：io.github.java-diff-utils:java-diff-utils:4.12
DiffUtils.diff(oldLines, newLines).getDeltas()
```

### 11.3 @DataScope 数据权限
```java
// RuoYi已有的数据权限注解
@DataScope(deptAlias = "u")
public List<AuditProject> selectProjectList(AuditProject query) {
    // SQL自动注入 AND u.dept_id IN (用户数据权限范围)
}
```

### 11.4 Excel批量导入
```java
// 使用RuoYi已有的ExcelUtil
ExcelUtil<AuditProject> util = new ExcelUtil<>(AuditProject.class);
List<AuditProject> list = util.importExcel(file.getInputStream());
```

---

> **注意**：本计划假设1人全职开发。如果是多人协作，可以按模块并行：
> - 人员A：① + ② + ⑧（信息管理 + 权限）
> - 人员B：③ + ④ + ⑤（进度 + 准备 + 报告归档）
> - 人员C：⑥ + ⑦（依据库 + AI增强）
