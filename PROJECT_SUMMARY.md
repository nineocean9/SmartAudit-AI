# 高校智慧审计平台 — AI 工作台 · 项目总结

> 版本：v3.0 | 最后更新：2026-07-13 | 基座框架：RuoYi-Vue v3.9.2

---

## 一、项目定位

面向高校审计处的**智能审计工作平台**。核心能力是将 AI 大模型深度融入审计全流程——从计划制定、现场实施、底稿编制、问题发现到整改跟踪，每个环节都有 AI 辅助。

**一句话总结**：审计人员在一个统一聊天窗口中，用自然语言就能完成"查资料、做分析、出图表、生取证单、扫风险"等全部操作。

---

## 二、技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| 前端 | Vue 3 + Element Plus + Vite | Vue 3.4 |
| 后端 | Spring Boot 4 + MyBatis | JDK 21 |
| 数据库 | PostgreSQL + pgvector | PG 18 |
| AI 模型 | LangChain4j + OpenAI 兼容接口 | 0.36 |
| 默认模型 | 小米 MiMo (mimo-v2.5-pro) | — |
| 向量嵌入 | DashScope text-embedding-v4 (1024维) | — |
| 文档解析 | Apache POI (Word/Excel) + PDFBox | POI 5.3 |
| 图表 | ECharts 5.4.3 (本地化) | — |
| 基座框架 | RuoYi-Vue（用户/角色/菜单/权限/日志） | 3.9.2 |

**AI 模型可切换**（`application.yml` 的 `ai.model.provider`）：
- `mimo` — 小米 MiMo（当前默认）
- `dashscope` — 阿里通义千问
- `deepseek` — DeepSeek
- `openai` — OpenAI GPT
- `ollama` — 本地私有部署

---

## 三、系统架构

```
┌────────────────────────────────────────────────────┐
│                    Vue 3 前端                       │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────────┐ │
│  │AI聊天│ │审计管│ │项目资│ │数据可│ │Excel查看│ │
│  │chat  │ │理模块│ │料库  │ │视化  │ │excelView│ │
│  └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └────┬─────┘ │
└─────┼────────┼────────┼────────┼──────────┼───────┘
      │ SSE    │ REST   │ REST   │ REST     │ REST
┌─────┼────────┼────────┼────────┼──────────┼───────┐
│     ▼        ▼        ▼        ▼          ▼       │
│  ┌──────────────────────────────────────────────┐ │
│  │              Spring Boot 后端                 │ │
│  │                                              │ │
│  │  AiChatService ──→ ChatTaskParser(AI解析意图) │ │
│  │       │                                      │ │
│  │       ├── READ_PROJECT → 读文档+AI流式回答    │ │
│  │       ├── ANALYZE_PROJECT → AI生成HTML驾驶舱  │ │
│  │       ├── FORENSIC → AI生成取证单             │ │
│  │       ├── RISK_SCAN → AI风险扫描              │ │
│  │       ├── DOC_CHECK → AI文档核查              │ │
│  │       ├── LIST_PROJECTS → 列出项目资料        │ │
│  │       └── QA → RAG知识检索+AI问答             │ │
│  │                                              │ │
│  │  RAG引擎 ──→ pgvector向量搜索                 │ │
│  │       └── 5个知识源: 依据/项目/临时/案例/风险  │ │
│  │                                              │ │
│  │  文件解析 ──→ POI(Word/Excel) + PDFBox(PDF)   │ │
│  │       └── 切块 → 向量化 → 存入document_chunk  │ │
│  └──────────────────────────────────────────────┘ │
│                       │                           │
│                       ▼                           │
│              PostgreSQL + pgvector                │
│     ┌─────────────────────────────────────┐       │
│     │ 30+ 张业务表 + 向量索引              │       │
│     └─────────────────────────────────────┘       │
└───────────────────────────────────────────────────┘
```

---

## 四、核心功能模块

### 4.1 AI 智能聊天（核心枢纽）

**文件**：`AiChatController` → `AiChatServiceImpl` → `ChatTaskParserServiceImpl`

**工作流程**：
```
用户输入 → ChatTaskParser(AI 解析意图) → List<ChatTask>
    → for 循环依次执行每个任务（支持复合指令）
    → SSE 流式推送结果
    → done
```

**支持 7 种任务类型**：

| taskType | 触发示例 | 执行逻辑 |
|----------|---------|---------|
| `LIST_PROJECTS` | "项目库里有什么" | 列出最近的项目文档 |
| `READ_PROJECT` | "A公司预算是多少" | 读取项目文档内容 → AI 流式回答 |
| `ANALYZE_PROJECT` | "生成A公司数据分析" | AI 生成 HTML 驾驶舱 + Markdown 总结 |
| `FORENSIC` | "生成取证单" | AI 生成完整取证单（六板块结构） |
| `RISK_SCAN` | "扫描A公司风险" | AI 分析风险点（高/中/低分级） |
| `DOC_CHECK` | "核查B项目文档" | AI 核查文档合规性 |
| `QA` | "什么是经济责任审计" | RAG 检索 + AI 问答 |

**复合指令**：一条消息可包含多个意图，系统自动拆分依次执行，任务间用分隔线分开。

**关键设计**：
- 流式任务用 `CountDownLatch` 阻塞等待完成，确保多任务顺序执行
- 所有 handler 不自行发 `done`，由外层循环统一控制
- `persistAndSend()` 只发消息不结束连接

### 4.2 RAG 知识检索

**文件**：`AuditRagServiceImpl` → `KnowledgeManagerImpl` → `EmbeddingServiceImpl`

**5 个知识源**：

| 知识源 | 数据来源 | 用途 |
|--------|---------|------|
| `POLICY` | audit_basis（审计依据库） | 法规条文检索 |
| `PROJECT` | project_document → document_chunk | 项目文档检索 |
| `TEMP` | temporary_workspace | 临时上传文件 |
| `CASE` | audit_case_lib | 审计案例参考 |
| `RISK_CASE` | audit_risk_case | 风险案例参考 |

**检索流程**：
1. 用户问题 → DashScope embedding → 1024 维向量
2. pgvector 余弦相似度搜索 document_chunk 表
3. 按知识源拆分结果 → 拼装 system prompt
4. AI 基于上下文回答

### 4.3 数据分析可视化

**文件**：`AiDataAnalyzeController` → `AiDataAnalyzeServiceImpl`

**两步生成**：
1. **HTML 驾驶舱**：AI 一次调用生成完整 HTML（含 ECharts 图表），程序保存到 `analysis_result.html_content`
2. **Markdown 总结**：第二次 AI 调用生成审计分析报告（数据概况/关键发现/风险提示/建议）

**查看方式**：
- 框架内 iframe 加载 `/ai/data/analysis/{id}/html`
- 下方显示 Markdown 渲染的 AI 分析总结
- ECharts 已本地化（`/static/js/echarts.min.js`），无外网依赖

**Prompt 设计**：提供固定 HTML 骨架（CSS + JS 循环渲染），AI 只需填充 `<<KPI>>` 和 `<<CHARTS>>` 两个占位符，减少输出量 60%。

### 4.4 项目文档管理

**文件**：`ProjectDocController` → `ProjectDocServiceImpl` → `FileParseServiceImpl`

**上传链路**：
```
文件上传 → FileParseService 解析（POI/PDFBox）
    → 纯文本存入 project_document.content_text
    → DocChunker 切块
    → EmbeddingService 向量化
    → 存入 document_chunk（含 pgvector 向量）
```

**支持格式**：Word(.docx)、Excel(.xlsx/.xls)、PDF、CSV、TXT

**Excel 查看**：独立页面（excelView.vue），el-table 渲染，支持：
- 多 Sheet 切换
- 智能表头识别（判断第一行是否为标题）
- 双击单元格编辑
- 新增/删除行
- 搜索过滤
- 下载原文件

### 4.5 审计业务管理

#### 审计计划 (`plan.vue` → `AuditInfoController`)
- 计划 CRUD + 绑定项目 + AI 推荐审计对象
- 支持年度计划/专项计划

#### 被审计单位 (`unit.vue`)
- 单位管理 + AI 画像生成

#### 审计实施 (`project.vue` → `AuditOpsController`)
- 审计方案、底稿、复核、报告、协同日志 CRUD

#### 审计问题 (`issue.vue` → `AuditIssueController`)
- CRUD + 按项目/严重度筛选
- 关联审计依据 + 关联整改

#### 整改跟踪 (`rectification.vue` → `AuditRectificationController`)
- 从问题发起整改 + 更新状态 + 删除

#### 审计依据库 (`basis.vue` → `AuditBasisController`)
- CRUD + 语义搜索 + 状态切换

#### 案例库 (`case.vue`) / 风险案例库 (`risk.vue`)
- CRUD + 保存时自动向量化入库（供 RAG 检索）

#### 取证稿 (`forensic.vue` → `AiForensicController`)
- AI 生成 + 列表 + 详情 + 删除
- 取证单六板块：基本信息/问题描述/事实证据/法规依据/审计结论/整改建议

#### 数据驾驶舱 (`dataDashboard.vue` → `DashboardController`)
- 项目统计 + 问题分布 + 整改情况 + 各单位问题数

---

## 五、数据库设计

### 核心业务表

| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `audit_plan` | 审计计划 | plan_type, plan_year, plan_name |
| `audit_project` | 审计项目 | project_name, audited_unit, audit_type, plan_id |
| `audit_issue` | 审计问题 | project_id, issue_desc, severity(1/2/3), basis_id |
| `audit_rectification` | 整改记录 | issue_id, measure, status(0/1/2), finish_date |
| `audit_basis` | 审计依据 | title, content, category, status |
| `audit_case_lib` | 案例库 | title, content, category |
| `audit_risk_case` | 风险案例 | title, content, risk_type |
| `audit_workpaper` | 审计底稿 | project_id, title, content, review_status |
| `audit_report` | 审计报告 | project_id, title, version, content |
| `audit_unit` | 被审计单位 | unit_name, unit_type, leader |
| `audit_leader` | 单位领导 | unit_id, name, position |

### AI 相关表

| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `ai_conversation` | 会话 | user_id, title, model |
| `ai_message` | 消息 | conversation_id, role, content, tokens |
| `analysis_result` | 分析结果 | html_content, summary, project_name |
| `project_document` | 项目文档 | project_id, file_name, file_path, content_text |
| `document_chunk` | 文档切块+向量 | doc_id, content, embedding(vector 1024), source_type |
| `forensic_draft` | 取证稿 | issue, suggestion, project_id |
| `risk_clue` | 风险线索 | project_id, clue_desc, risk_level |
| `doc_check_task` | 文档核查 | doc_id, check_result, status |
| `ai_call_log` | AI 调用日志 | intent, prompt, response, tokens |
| `temporary_workspace` | 临时工作区 | session_id, file_name, content |

---

## 六、文件结构

```
F:\project1\RuoYi-Vue-v3.9.2\
├── ruoyi-admin/                    # 启动模块
│   └── src/main/resources/
│       ├── application.yml         # 主配置（AI模型、数据库）
│       └── static/static/js/
│           └── echarts.min.js      # ECharts 本地化
│
├── ruoyi-ai/                       # AI 审计业务模块（核心）
│   └── src/main/java/com/ruoyi/system/
│       ├── controller/             # 17 个 Controller
│       │   ├── AiChatController        # AI 聊天（SSE 流式）
│       │   ├── AiDataAnalyzeController # 数据分析（HTML 驾驶舱）
│       │   ├── AiForensicController    # 取证稿
│       │   ├── AiRiskController        # 风险扫描
│       │   ├── AiDocCheckController    # 文档核查
│       │   ├── AuditInfoController     # 计划+单位+项目
│       │   ├── AuditIssueController    # 审计问题 CRUD
│       │   ├── AuditOpsController      # 方案/底稿/报告
│       │   ├── AuditRectificationController # 整改跟踪
│       │   ├── ProjectDocController    # 文档管理+Excel操作
│       │   └── DashboardController     # 数据驾驶舱统计
│       │
│       ├── service/impl/           # 19 个 Service 实现
│       │   ├── AiChatServiceImpl       # 聊天核心（多任务循环）
│       │   ├── ChatTaskParserServiceImpl# 意图解析（JSON数组）
│       │   ├── AuditRagServiceImpl     # RAG 引擎
│       │   ├── KnowledgeManagerImpl    # 知识库管理（5源搜索）
│       │   ├── EmbeddingServiceImpl    # 向量嵌入（DashScope）
│       │   ├── FileParseServiceImpl    # 文件解析（POI/PDFBox）
│       │   ├── ProjectDocServiceImpl   # 文档上传+切块+向量化
│       │   ├── AiDataAnalyzeServiceImpl# 数据分析（AI生成HTML）
│       │   └── AiForensicServiceImpl   # 取证稿（AI生成内容）
│       │
│       ├── domain/                 # 21 个实体类
│       ├── mapper/                 # 14 个 Mapper 接口
│       └── util/                   # DocChunker, FileTypeDetector
│
├── ruoyi-ui/                       # Vue 3 前端
│   └── src/
│       ├── views/
│       │   ├── ai/                 # AI 功能页面
│       │   │   ├── chat.vue            # 统一聊天入口（SSE）
│       │   │   ├── visualization/      # 数据分析列表+详情
│       │   │   ├── forensic.vue        # 取证稿管理
│       │   │   ├── basis.vue           # 审计依据库
│       │   │   ├── aiLog.vue           # AI 调用日志
│       │   │   └── dataDashboard.vue   # 统计驾驶舱
│       │   │
│       │   ├── audit/              # 审计业务页面
│       │   │   ├── plan.vue            # 审计计划
│       │   │   ├── project.vue         # 审计实施
│       │   │   ├── issue.vue           # 审计问题
│       │   │   ├── rectification.vue   # 整改跟踪
│       │   │   ├── projectLib.vue      # 项目资料库
│       │   │   ├── excelView.vue       # Excel 表格查看
│       │   │   ├── workpaper.vue       # 底稿管理
│       │   │   ├── report.vue          # 报告管理
│       │   │   ├── unit.vue            # 被审计单位
│       │   │   ├── case.vue            # 案例库
│       │   │   └── risk.vue            # 风险案例库
│       │   └── knowledge/
│       │       └── upload.vue          # 知识上传
│       │
│       ├── api/                    # 11 个 API 模块
│       ├── components/
│       │   └── AiChat/FileUploader.vue # 文件上传组件
│       └── router/index.js         # 路由配置
│
└── sql/                            # 17 个 SQL 脚本
    ├── ry_20260320.sql             # RuoYi 基础表
    ├── ai.sql / ai_init_pg.sql     # AI 模块表
    ├── demo_project_data.sql       # 审计业务表+种子数据
    ├── modules_34.sql              # 模块扩展字段
    ├── analysis_result.sql         # 分析结果表
    └── rag_case_extend.sql         # 案例向量化扩展
```

---

## 七、关键设计决策

### 7.1 AI 直接生成 HTML 驾驶舱
- **为什么不用程序构建 ECharts JSON**：AI 更灵活，能根据数据自动选择图表类型
- **为什么给骨架**：减少 AI 输出 token（CSS 在骨架里硬编码），避免截断
- **兼容旧记录**：`getAnalysisHtml()` 优先返回 `html_content`，为空时从 `chart_data` 回退渲染

### 7.2 ChatTaskParser 返回数组
- **为什么不用单任务**：用户经常一句话多个意图
- **兼容性**：AI 返回单对象时自动包装为数组；解析失败走关键词 fallback

### 7.3 流式 SSE + CountDownLatch
- **为什么用 SSE 不用 WebSocket**：RuoYi 默认不配 WebSocket，SSE 更轻量
- **为什么用 CountDownLatch**：流式 handler 是异步的（callback），多任务需要等前一个完成才能开始下一个

### 7.4 SecurityUtils 异步线程问题
- 聊天在异步线程执行，Spring SecurityContext 不可用
- 解决：Controller 在 HTTP 线程取 username，传入 Service 方法参数；Service 内部用 try-catch 兜底

### 7.5 ECharts 本地化
- 内网环境 CDN 加载 8 秒 → 下载到 `public/static/js/` + `resources/static/static/js/`
- `getAnalysisHtml()` 返回前自动替换旧记录中的 CDN 地址

---

## 八、部署要点

### 8.1 数据库初始化
```bash
# 按顺序执行
psql -f sql/ry_20260320.sql          # RuoYi 基础表
psql -f sql/ai.sql                   # AI 模块表
psql -f sql/ai_init_pg.sql           # pgvector 扩展
psql -f sql/demo_project_data.sql    # 审计业务表+种子数据
psql -f sql/modules_34.sql           # 扩展字段
psql -f sql/analysis_result.sql      # 分析结果表
psql -f sql/rag_case_extend.sql      # 案例向量化
psql -f sql/ai_workspace.sql         # 工作区表
psql -f sql/menu_reorg.sql           # 菜单配置
```

### 8.2 配置 AI 模型
```yaml
# application.yml
ai:
  model:
    provider: mimo                    # 或 dashscope/deepseek/openai
    api-key: your-api-key-here
    model-name: mimo-v2.5-pro
    max-tokens: 8192
  embedding:
    provider: dashscope
    api-key: your-dashscope-key
    model: text-embedding-v4
```

### 8.3 pgvector 扩展
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 8.4 前端环境变量
```env
# .env.development
VITE_APP_BASE_API = '/dev-api'
```

---

## 九、已知限制与后续方向

| 现状 | 可改进方向 |
|------|----------|
| 审计方案/报告无审批流 | 接入工作流引擎 |
| 取证稿 → 问题无自动转化 | 一键从取证稿创建审计问题 |
| 报告不能从底稿/问题自动汇总 | AI 自动汇总生成报告 |
| 无审计档案归档 | 项目完结后自动归档 |
| AI 模型单线程调用 | 可改为异步并发 |
| Excel 编辑直接写原文件 | 可改为版本管理 |

---

## 十、Git 提交历史

```
e4257be fix: Excel 表头智能识别 + 复合指令去重驾驶舱
ac602d6 feat: Excel 文件表格化查看 + 增删改查
30270fb feat: 支持复合指令 — 一条消息执行多个任务
063fd86 fix: 取证单生成改为 AI 生成完整内容
23af97a fix: 修复取证单生成时异步线程 SecurityContext 异常
22dab15 feat: 审计平台全面修复 — 6阶段一次性完成
4d221f8 fix: READ_PROJECT 改为读取文档内容 + AI 流式回答
c1ee9b2 fix: 旧记录 HTML 中 CDN 地址替换为本地路径
d618493 perf: ECharts 本地化，消除 CDN 加载延迟
7c454f9 feat: 数据分析页面增删改查 + AI 文字分析总结
66c8318 feat: AI 直接生成 HTML 驾驶舱 + 聊天架构重构
5a2fd2f feat: SmartAudit-AI v1.0
f9d11fd feat: SmartAudit-AI 高校智慧审计平台初始化
```
