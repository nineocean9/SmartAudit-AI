<template>
  <div class="app-container project-workbench">
    <aside class="project-sidebar">
      <div class="sidebar-head">
        <div>
          <span>项目工作台</span>
          <h2>我的审计项目</h2>
        </div>
        <el-button circle :icon="Refresh" @click="loadProjects" />
      </div>
      <el-input v-model="keyword" placeholder="搜索项目或单位" clearable class="project-search" />
      <div class="project-list">
        <button
          v-for="p in filteredProjects"
          :key="p.id"
          class="project-card"
          :class="{ active: curProject?.id === p.id }"
          type="button"
          @click="selectProject(p)"
        >
          <strong>{{ p.project_name }}</strong>
          <span>{{ p.audited_unit }} · {{ p.audit_type }} · {{ p.audit_year }}</span>
          <el-tag size="small" :type="statusMeta(p.status).type">{{ statusMeta(p.status).label }}</el-tag>
        </button>
      </div>
      <el-empty v-if="!loadingProjects && filteredProjects.length === 0" description="暂无可用项目" :image-size="80" />
    </aside>

    <main class="workbench-main">
      <section v-if="curProject" class="project-hero">
        <div>
          <div class="eyebrow">当前项目</div>
          <h1>{{ curProject.project_name }}</h1>
          <p>{{ curProject.audited_unit }} · {{ curProject.audit_type }} · {{ curProject.audit_year }} 年度</p>
        </div>
        <div class="hero-actions">
          <el-button v-if="canAccess(['audit:prepare:view'])" type="primary" :icon="FolderOpened" @click="goPage('/audit/prepare')">进入审计准备</el-button>
          <el-button v-if="canAccess(['audit:projectLib:view'])" :icon="Files" @click="goPage('/audit/projectLib')">查看项目库</el-button>
        </div>
      </section>

      <el-empty v-else class="empty-main" description="请先选择一个项目" />

      <template v-if="curProject">
        <section class="summary-grid" v-loading="loadingDetail">
          <div v-for="item in summaryCards" :key="item.label" class="summary-card">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </div>
        </section>

        <section class="action-panel">
          <div class="section-title">
            <div>
              <span>办理入口</span>
              <h2>按项目继续作业</h2>
            </div>
          </div>
          <div class="action-grid">
            <button v-for="item in actionItems" :key="item.title" type="button" class="action-card" @click="goPage(item)">
              <el-icon><component :is="item.icon" /></el-icon>
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </button>
          </div>
        </section>

        <el-row :gutter="16" class="content-row">
          <el-col :xs="24" :lg="15">
            <section class="content-panel">
              <div class="section-title">
                <div>
                  <span>项目材料</span>
                  <h2>资料、方案与报告</h2>
                </div>
              </div>

              <div class="resource-list">
                <div class="resource-row">
                  <el-icon><Document /></el-icon>
                  <div>
                    <strong>审计方案</strong>
                    <p>{{ scheme ? scheme.title || '已形成项目审计方案' : '暂无方案，建议先在审计准备中生成或上传方案。' }}</p>
                  </div>
                  <el-tag :type="scheme?.status === 1 ? 'success' : 'info'">{{ scheme?.status === 1 ? '已审批' : scheme ? '草稿' : '未建立' }}</el-tag>
                </div>

                <div class="resource-row">
                  <el-icon><Files /></el-icon>
                  <div>
                    <strong>项目资料</strong>
                    <p>已归集 {{ documents.length }} 份资料，覆盖通知书、方案、底稿、报告和整改材料。</p>
                  </div>
                  <el-button v-if="canAccess(['audit:projectLib:view'])" link type="primary" @click="goPage('/audit/projectLib')">查看</el-button>
                </div>

                <div v-if="canAccess(['audit:report:view'])" class="resource-row">
                  <el-icon><Memo /></el-icon>
                  <div>
                    <strong>审计报告</strong>
                    <p>{{ reports.length ? `已有 ${reports.length} 份报告版本。` : '暂无报告版本。' }}</p>
                  </div>
                  <el-button link type="primary" @click="goPage('/audit/report')">办理</el-button>
                </div>
              </div>
            </section>
          </el-col>

          <el-col :xs="24" :lg="9">
            <section class="content-panel">
              <div class="section-title">
                <div>
                  <span>协同动态</span>
                  <h2>最近记录</h2>
                </div>
              </div>
              <el-timeline v-if="collabLogs.length">
                <el-timeline-item v-for="log in collabLogs.slice(0, 5)" :key="log.id" :timestamp="log.create_time">
                  {{ log.user_name || log.create_by || '系统' }} · {{ log.action || '更新' }} {{ log.target || '' }}
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无协同记录" :image-size="64" />
            </section>
          </el-col>
        </el-row>

        <section v-if="canAccess(['audit:issue:view'])" class="content-panel">
          <div class="section-title">
            <div>
              <span>风险与整改</span>
              <h2>问题清单</h2>
            </div>
            <el-button link type="primary" @click="goPage('/audit/issue')">查看全部</el-button>
          </div>
          <el-table :data="issues" size="small" empty-text="暂无问题">
            <el-table-column prop="title" label="问题标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="category" label="类型" width="120" show-overflow-tooltip />
            <el-table-column prop="severity" label="严重程度" width="110">
              <template #default="{ row }">
                <el-tag :type="severityType(row.severity)" size="small">{{ row.severity || '一般' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="rectStatus" label="整改状态" width="120" show-overflow-tooltip />
            <el-table-column prop="createTime" label="登记时间" width="170" show-overflow-tooltip />
          </el-table>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ChatLineRound,
  DataAnalysis,
  Document,
  Files,
  FolderOpened,
  Memo,
  Notebook,
  Position,
  Refresh,
  Warning
} from '@element-plus/icons-vue'
import { ops } from '@/api/audit/auditOps'
import { listProjectTree } from '@/api/ai/workspace'
import { listProjectDoc } from '@/api/audit/projectDoc'
import { listIssues } from '@/api/audit/issue'
import auth from '@/plugins/auth'

const route = useRoute()
const router = useRouter()

const loadingProjects = ref(false)
const loadingDetail = ref(false)
const keyword = ref('')
const projects = ref([])
const curProject = ref(null)
const scheme = ref(null)
const documents = ref([])
const workpapers = ref([])
const workpaperTotal = ref(0)
const issues = ref([])
const issueTotal = ref(0)
const reports = ref([])
const collabLogs = ref([])

const filteredProjects = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  if (!word) return projects.value
  return projects.value.filter(p =>
    [p.project_name, p.audited_unit, p.audit_type, String(p.audit_year)]
      .some(text => String(text || '').toLowerCase().includes(word))
  )
})

const summaryCards = computed(() => [
  { label: '项目资料', value: documents.value.length, hint: '已归集文件', icon: Files },
  { label: '审计方案', value: scheme.value ? '1' : '0', hint: scheme.value?.status === 1 ? '已审批' : '待完善', icon: Document },
  { label: '底稿记录', value: workpaperTotal.value, hint: '作业成果', icon: Notebook },
  { label: '审计问题', value: issueTotal.value, hint: '需跟踪整改', icon: Warning }
])

const actionItems = computed(() => [
  { title: '审计准备', desc: '维护方案、成员、资料清单', icon: FolderOpened, path: '/audit/prepare', perms: ['audit:prepare:view'] },
  { title: '项目资料', desc: '上传、预览和管理项目文件', icon: Files, path: '/audit/projectLib', perms: ['audit:projectLib:view'] },
  { title: '底稿管理', desc: '编写底稿和查看复核记录', icon: Notebook, path: '/audit/workpaper', perms: ['audit:workpaper:view'] },
  { title: '问题登记', desc: '登记问题、关联依据和整改', icon: Warning, path: '/audit/issue', perms: ['audit:issue:view'] },
  { title: '审计报告', desc: '编写报告、版本和定稿', icon: Memo, path: '/audit/report', perms: ['audit:report:view'] },
  { title: 'AI 取证', desc: '生成取证单和分析建议', icon: DataAnalysis, path: '/ai/forensic', perms: ['ai:forensic:view'] }
].filter(item => canAccess(item.perms)))

loadProjects()

async function loadProjects() {
  loadingProjects.value = true
  try {
    const r = await listProjectTree()
    const plans = r.data || []
    projects.value = plans.flatMap(plan => (plan.projects || []).map(project => ({
      id: project.id,
      project_name: project.projectName,
      audited_unit: project.auditedUnit,
      audit_type: project.auditType,
      audit_year: project.auditYear,
      status: project.status,
      plan_name: plan.planName
    })))

    const routeProjectId = Number(route.query.projectId || route.query.id)
    const target = projects.value.find(p => Number(p.id) === routeProjectId) || projects.value[0]
    if (target) selectProject(target)
  } catch (e) {
    ElMessage.error('项目列表加载失败')
  } finally {
    loadingProjects.value = false
  }
}

async function selectProject(project) {
  curProject.value = project
  loadingDetail.value = true
  try {
    const [schemeRes, logRes, docRes, wpRes, issueRes, reportRes] = await Promise.allSettled([
      ops.schemeList(project.id),
      ops.collabLog(project.id),
      canAccess(['audit:projectLib:view']) ? listProjectDoc(project.id) : emptyData(),
      canAccess(['audit:workpaper:view']) ? ops.wpList({ projectId: project.id, pageNum: 1, pageSize: 5 }) : emptyTable(),
      canAccess(['audit:issue:view']) ? listIssues({ projectId: project.id, pageNum: 1, pageSize: 5 }) : emptyTable(),
      canAccess(['audit:report:view']) ? ops.reportList(project.id) : emptyData()
    ])

    scheme.value = firstData(schemeRes, [])[0] || null
    collabLogs.value = firstData(logRes, [])
    documents.value = firstData(docRes, [])
    workpapers.value = wpRes.status === 'fulfilled' ? (wpRes.value.rows || []) : []
    workpaperTotal.value = wpRes.status === 'fulfilled' ? Number(wpRes.value.total || workpapers.value.length) : 0
    issues.value = issueRes.status === 'fulfilled' ? (issueRes.value.rows || []) : []
    issueTotal.value = issueRes.status === 'fulfilled' ? Number(issueRes.value.total || issues.value.length) : 0
    reports.value = firstData(reportRes, [])
  } finally {
    loadingDetail.value = false
  }
}

function firstData(result, fallback) {
  if (result.status !== 'fulfilled') return fallback
  return result.value?.data || fallback
}

function canAccess(perms = []) {
  if (!perms.length) return true
  if (auth.hasPermiOr(perms)) return true
  if (perms.some(perm => perm === 'audit:workpaper:view' || perm === 'audit:workpaper:edit')) {
    return auth.hasRoleOr(['audit_project_leader', 'audit_staff', 'intermediary_auditor'])
  }
  return false
}

function emptyData() {
  return Promise.resolve({ data: [] })
}

function emptyTable() {
  return Promise.resolve({ rows: [], total: 0 })
}

function goPage(target) {
  if (!curProject.value) return
  const path = typeof target === 'string' ? target : target.path
  const perms = typeof target === 'string' ? [] : (target.perms || [])
  if (!canAccess(perms)) {
    ElMessage.warning('当前角色没有该功能权限')
    return
  }
  router.push({ path, query: { projectId: curProject.value.id } })
}

function statusMeta(status) {
  if (Number(status) === 2) return { label: '已归档', type: 'success' }
  if (Number(status) === 1) return { label: '实施中', type: 'primary' }
  return { label: '准备', type: 'info' }
}

function severityType(severity) {
  if (['重大', '高', '严重'].includes(severity)) return 'danger'
  if (['较大', '中'].includes(severity)) return 'warning'
  return 'info'
}
</script>

<style scoped>
.project-workbench {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  min-height: calc(100vh - 84px);
  background: #f3f6f9;
}

.project-sidebar,
.project-hero,
.summary-card,
.action-panel,
.content-panel {
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
}

.project-sidebar {
  padding: 16px;
  align-self: start;
  position: sticky;
  top: 12px;
}

.sidebar-head,
.section-title,
.project-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sidebar-head span,
.section-title span,
.eyebrow {
  color: #176b87;
  font-size: 13px;
  font-weight: 700;
}

.sidebar-head h2,
.section-title h2,
.project-hero h1 {
  margin: 4px 0 0;
  color: #12263a;
  letter-spacing: 0;
}

.sidebar-head h2,
.section-title h2 {
  font-size: 18px;
}

.project-search {
  margin: 14px 0;
}

.project-list {
  display: grid;
  gap: 8px;
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.project-card,
.action-card {
  border: 0;
  text-align: left;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease, background-color 0.16s ease;
}

.project-card {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-radius: 6px;
  background: #f8fafc;
  color: #12263a;
}

.project-card:hover,
.project-card.active {
  background: #e8f3f6;
}

.project-card strong {
  font-size: 14px;
}

.project-card span {
  color: #64748b;
  font-size: 12px;
}

.workbench-main {
  min-width: 0;
}

.project-hero {
  padding: 22px 24px;
}

.project-hero h1 {
  font-size: 26px;
}

.project-hero p {
  margin: 8px 0 0;
  color: #64748b;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.summary-card {
  display: grid;
  gap: 6px;
  padding: 16px;
}

.summary-card .el-icon {
  color: #176b87;
  font-size: 22px;
}

.summary-card span,
.summary-card small {
  color: #64748b;
}

.summary-card strong {
  color: #12263a;
  font-size: 26px;
  font-variant-numeric: tabular-nums;
}

.action-panel,
.content-panel {
  margin-top: 16px;
  padding: 18px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.action-card {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 4px 10px;
  min-height: 82px;
  padding: 14px;
  border-radius: 6px;
  background: #f8fafc;
}

.action-card:hover {
  background: #eef6f8;
  box-shadow: 0 8px 18px rgba(23, 107, 135, 0.1);
  transform: translateY(-1px);
}

.action-card .el-icon {
  grid-row: span 2;
  color: #176b87;
  font-size: 24px;
}

.action-card strong {
  color: #12263a;
}

.action-card span {
  color: #64748b;
  line-height: 1.5;
}

.resource-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.resource-row {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 6px;
  background: #f8fafc;
}

.resource-row .el-icon {
  color: #176b87;
  font-size: 22px;
}

.resource-row strong {
  color: #12263a;
}

.resource-row p {
  margin: 4px 0 0;
  color: #64748b;
  line-height: 1.55;
}

.content-row {
  margin-top: 0;
}

.empty-main {
  height: calc(100vh - 140px);
  border-radius: 6px;
  background: #fff;
}

@media (max-width: 1100px) {
  .project-workbench {
    grid-template-columns: 1fr;
  }

  .project-sidebar {
    position: static;
  }

  .project-list {
    max-height: none;
  }

  .summary-grid,
  .action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .project-workbench {
    padding: 12px;
  }

  .project-hero,
  .sidebar-head,
  .section-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-grid,
  .action-grid {
    grid-template-columns: 1fr;
  }
}
</style>
