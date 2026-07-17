<template>
  <div class="app-container forensic-page">
    <div class="forensic-toolbar">
      <div>
        <div class="page-kicker">AI 取证分析</div>
        <h2>取证问题识别与底稿生成</h2>
        <p>围绕当前可访问项目生成取证建议，引用制度依据，形成可复核的取证草稿。</p>
      </div>
      <el-button v-if="canGenerate" type="primary" :icon="Plus" @click="openGenerate">
        生成取证草稿
      </el-button>
    </div>

    <el-row :gutter="16" class="summary-row">
      <el-col :xs="12" :sm="6" v-for="item in summaryCards" :key="item.label">
        <div class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never" class="filter-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="项目">
          <el-select
            v-model="queryParams.projectId"
            clearable
            filterable
            placeholder="全部可访问项目"
            style="width: 260px"
          >
            <el-option
              v-for="project in projectOptions"
              :key="project.id"
              :label="project.projectName"
              :value="project.id"
            >
              <span>{{ project.projectName }}</span>
              <span class="option-meta">{{ project.auditedUnit }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.reviewStatus" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="草稿" :value="0" />
            <el-option label="待复核" :value="1" />
            <el-option label="已通过" :value="2" />
            <el-option label="已退回" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题">
          <el-input
            v-model="queryParams.issue"
            clearable
            placeholder="搜索问题关键词"
            style="width: 220px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table v-loading="loading" :data="draftList" border class="forensic-table">
      <el-table-column label="项目" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="project-cell">
            <strong>{{ row.projectName || '未关联项目' }}</strong>
            <span>{{ row.auditedUnit || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="取证问题" prop="issue" min-width="260" show-overflow-tooltip />
      <el-table-column label="依据" width="110">
        <template #default="{ row }">
          <el-tag type="info" effect="plain">{{ formatBasisCount(row.basisIds) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.reviewStatus]?.type || 'info'">
            {{ statusMap[row.reviewStatus]?.label || '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生成时间" prop="createTime" width="170" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          <el-button v-if="canSubmit && [0, 3].includes(row.reviewStatus)" link type="primary" @click="submitDraft(row)">提交复核</el-button>
          <el-button v-if="canReview && row.reviewStatus === 1" link type="success" @click="reviewDraft(row, 2)">通过</el-button>
          <el-button v-if="canReview && row.reviewStatus === 1" link type="warning" @click="reviewDraft(row, 3)">退回</el-button>
          <el-button v-if="canDelete" link type="danger" @click="deleteDraft(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog v-model="generateOpen" title="生成取证草稿" width="720px" append-to-body>
      <el-form :model="form" label-width="96px">
        <el-form-item label="关联项目" required>
          <el-select v-model="form.projectId" filterable placeholder="请选择项目" style="width: 100%">
            <el-option
              v-for="project in projectOptions"
              :key="project.id"
              :label="project.projectName"
              :value="project.id"
            >
              <span>{{ project.projectName }}</span>
              <span class="option-meta">{{ project.auditedUnit }} · {{ project.auditType }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="取证问题" required>
          <el-input
            v-model="form.issue"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="例如：抽查发现部分合同付款审批附件缺失，需核实审批链条和资金支付依据。"
          />
        </el-form-item>
        <el-form-item label="引用依据">
          <el-select
            v-model="basisSelection"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="可选制度依据"
            style="width: 100%"
          >
            <el-option
              v-for="basis in basisOptions"
              :key="basis.id"
              :label="basis.title"
              :value="basis.id"
            >
              <span>{{ basis.title }}</span>
              <span class="option-meta">{{ basis.category || '依据' }} {{ basis.docNumber || '' }}</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateOpen = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="submitGenerate">生成</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailOpen" title="取证草稿详情" size="46%">
      <template v-if="currentDraft">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="项目">{{ currentDraft.projectName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="被审计单位">{{ currentDraft.auditedUnit || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusMap[currentDraft.reviewStatus]?.type || 'info'">
              {{ statusMap[currentDraft.reviewStatus]?.label || '草稿' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="问题描述">{{ currentDraft.issue }}</el-descriptions-item>
          <el-descriptions-item label="引用依据">
            <div v-if="currentDraft.basisList?.length" class="basis-tags">
              <el-popover
                v-for="basis in currentDraft.basisList"
                :key="basis.id"
                placement="top-start"
                trigger="hover"
                :width="440"
              >
                <template #reference>
                  <el-tag type="info" effect="plain" class="basis-tag">{{ basis.title }}</el-tag>
                </template>
                <div class="basis-popover">
                  <strong>{{ basis.title }}</strong>
                  <div class="basis-meta">
                    {{ [basis.docNumber, basis.issueOrg, basis.hierarchyLevel].filter(Boolean).join(' · ') }}
                  </div>
                  <div class="basis-content">{{ basis.content || '暂无条文内容' }}</div>
                </div>
              </el-popover>
            </div>
            <span v-else class="empty-basis">未关联法规依据</span>
          </el-descriptions-item>
          <el-descriptions-item label="复核记录">{{ currentDraft.reviewLog || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="draft-section">
          <div class="section-title">
            <span>AI 建议</span>
            <el-button link type="primary" :icon="CopyDocument" @click="copySuggestion">复制</el-button>
          </div>
          <div
            v-if="currentDraft.suggestion"
            class="markdown-body"
            v-html="renderMarkdown(currentDraft.suggestion)"
          ></div>
          <div v-else class="empty-suggestion">暂无生成内容</div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, Plus, Refresh, Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { checkPermi } from '@/utils/permission'
import { listProjectTree } from '@/api/ai/workspace'

const loading = ref(false)
const generating = ref(false)
const generateOpen = ref(false)
const detailOpen = ref(false)
const draftList = ref([])
const total = ref(0)
const projectOptions = ref([])
const basisOptions = ref([])
const basisSelection = ref([])
const currentDraft = ref(null)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  projectId: undefined,
  reviewStatus: undefined,
  issue: ''
})

const form = ref({
  projectId: undefined,
  issue: '',
  basisIds: ''
})

const statusMap = {
  0: { label: '草稿', type: 'info' },
  1: { label: '待复核', type: 'warning' },
  2: { label: '已通过', type: 'success' },
  3: { label: '已退回', type: 'danger' }
}

const canGenerate = computed(() => checkPermi(['ai:forensic:gen']))
const canSubmit = computed(() => checkPermi(['ai:forensic:submit']))
const canReview = computed(() => checkPermi(['ai:forensic:review']))
const canDelete = computed(() => checkPermi(['ai:forensic:delete']))

const summaryCards = computed(() => {
  const rows = draftList.value || []
  return [
    { label: '当前列表', value: total.value },
    { label: '待复核', value: rows.filter(item => item.reviewStatus === 1).length },
    { label: '已通过', value: rows.filter(item => item.reviewStatus === 2).length },
    { label: '可访问项目', value: projectOptions.value.length }
  ]
})

onMounted(() => {
  loadProjects()
  loadBasis()
  getList()
})

function getList() {
  loading.value = true
  request({
    url: '/ai/forensic/list',
    method: 'get',
    params: queryParams.value
  }).then(res => {
    draftList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

async function loadProjects() {
  const res = await listProjectTree()
  const projects = []
  ;(res.data || []).forEach(plan => {
    ;(plan.projects || []).forEach(project => projects.push(project))
  })
  projectOptions.value = projects
}

function loadBasis() {
  if (!checkPermi(['ai:basis:query', 'audit:basis:query'])) {
    basisOptions.value = []
    return
  }
  request({
    url: '/basis/list',
    method: 'get',
    params: { pageNum: 1, pageSize: 100, status: 1 }
  }).then(res => {
    basisOptions.value = res.rows || []
  }).catch(() => {
    basisOptions.value = []
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    projectId: undefined,
    reviewStatus: undefined,
    issue: ''
  }
  getList()
}

function openGenerate() {
  form.value = {
    projectId: queryParams.value.projectId,
    issue: '',
    basisIds: ''
  }
  basisSelection.value = []
  generateOpen.value = true
}

function submitGenerate() {
  if (!form.value.projectId) {
    ElMessage.warning('请选择关联项目')
    return
  }
  if (!form.value.issue || !form.value.issue.trim()) {
    ElMessage.warning('请填写取证问题')
    return
  }
  generating.value = true
  request({
    url: '/ai/forensic/generate',
    method: 'post',
    data: {
      ...form.value,
      issue: form.value.issue.trim(),
      basisIds: basisSelection.value.join(',')
    }
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('取证草稿已生成')
      generateOpen.value = false
      getList()
    }
  }).finally(() => {
    generating.value = false
  })
}

function openDetail(row) {
  request({
    url: `/ai/forensic/${row.id}`,
    method: 'get'
  }).then(res => {
    if (res.code === 200) {
      currentDraft.value = res.data
      detailOpen.value = true
    }
  })
}

function reviewDraft(row, status) {
  const actionText = status === 2 ? '通过复核' : '退回草稿'
  ElMessageBox.confirm(`确认${actionText}该取证草稿？`, '提示', { type: 'warning' }).then(() => {
    return request({
      url: `/ai/forensic/${row.id}/review`,
      method: 'put',
      data: {
        reviewStatus: status,
        reviewLog: actionText
      }
    })
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('状态已更新')
      getList()
    }
  }).catch(() => {})
}

function submitDraft(row) {
  ElMessageBox.confirm('确认提交该取证草稿进行复核？', '提示', { type: 'warning' }).then(() => {
    return request({
      url: `/ai/forensic/${row.id}/submit`,
      method: 'put'
    })
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('已提交复核')
      getList()
    }
  }).catch(() => {})
}

function deleteDraft(id) {
  ElMessageBox.confirm('确认删除该取证草稿？', '提示', { type: 'warning' }).then(() => {
    return request({
      url: `/ai/forensic/${id}`,
      method: 'delete'
    })
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('已删除')
      getList()
    }
  }).catch(() => {})
}

function formatBasisCount(value) {
  if (!value) return '0 条'
  return `${value.split(',').filter(Boolean).length} 条`
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function renderMarkdown(text) {
  if (!text) return ''
  let html = escapeHtml(text)
  html = html.replace(/```[\w]*\n?([\s\S]*?)```/g, '<pre class="md-code"><code>$1</code></pre>')
  html = html.replace(/`([^`\n]+)`/g, '<code>$1</code>')
  html = html.replace(/^####\s+(.+)$/gm, '<h4>$1</h4>')
  html = html.replace(/^###\s+(.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^##\s+(.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^#\s+(.+)$/gm, '<h1>$1</h1>')
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)/g, '<em>$1</em>')
  html = html.replace(/^\d+\.\s+(.+)$/gm, '<li class="md-ordered">$1</li>')
  html = html.replace(/^[-*]\s+(.+)$/gm, '<li class="md-unordered">$1</li>')
  html = html.replace(/((?:<li class="md-ordered">[\s\S]*?<\/li>\s*)+)/g, '<ol>$1</ol>')
  html = html.replace(/((?:<li class="md-unordered">[\s\S]*?<\/li>\s*)+)/g, '<ul>$1</ul>')
  html = html.replace(/^---+$/gm, '<hr>')
  html = html.replace(/\n/g, '<br>')
  html = html.replace(/(<\/h[1-4]>|<\/li>|<\/[uo]l>|<hr>)<br>/g, '$1')
  return html
}

function copySuggestion() {
  const text = currentDraft.value?.suggestion || ''
  if (!text) return
  navigator.clipboard.writeText(text).then(() => ElMessage.success('已复制'))
}
</script>

<style scoped>
.forensic-page {
  background: #f5f7fb;
  min-height: calc(100vh - 84px);
}

.forensic-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 18px 20px;
  margin-bottom: 14px;
  background: #fff;
  border: 1px solid #dfe7f1;
  border-radius: 6px;
}

.forensic-toolbar h2 {
  margin: 4px 0 6px;
  font-size: 22px;
  color: #102033;
}

.forensic-toolbar p {
  margin: 0;
  color: #627086;
}

.page-kicker {
  color: #0f7294;
  font-weight: 700;
}

.summary-row {
  margin-bottom: 14px;
}

.summary-card {
  padding: 16px;
  background: #fff;
  border: 1px solid #dfe7f1;
  border-radius: 6px;
}

.summary-card span {
  display: block;
  color: #6b778c;
  font-size: 13px;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
  color: #102033;
}

.filter-card,
.forensic-table {
  margin-bottom: 14px;
}

.project-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.project-cell span,
.option-meta {
  color: #7a869a;
  font-size: 12px;
}

.option-meta {
  float: right;
  margin-left: 16px;
}

.draft-section {
  margin-top: 18px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: 700;
}

.basis-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.basis-tag {
  max-width: 100%;
  cursor: help;
}

.basis-popover strong {
  display: block;
  color: #172b4d;
  line-height: 1.5;
}

.basis-meta {
  margin: 6px 0 10px;
  color: #7a869a;
  font-size: 12px;
}

.basis-content {
  max-height: 220px;
  overflow: auto;
  color: #344563;
  line-height: 1.7;
  white-space: pre-wrap;
}

.empty-basis,
.empty-suggestion {
  color: #909399;
}

.markdown-body {
  padding: 18px 22px;
  color: #24344d;
  line-height: 1.75;
  background: #fff;
  border: 1px solid #dfe7f1;
  border-radius: 8px;
  word-break: break-word;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 20px 0 10px;
  color: #102033;
  line-height: 1.35;
}

.markdown-body :deep(h1) { font-size: 22px; }
.markdown-body :deep(h2) { font-size: 18px; border-bottom: 1px solid #e8edf3; padding-bottom: 8px; }
.markdown-body :deep(h3) { font-size: 16px; }
.markdown-body :deep(h4) { font-size: 15px; }
.markdown-body :deep(ul),
.markdown-body :deep(ol) { padding-left: 24px; }
.markdown-body :deep(li) { margin: 5px 0; }
.markdown-body :deep(hr) { border: 0; border-top: 1px solid #dfe7f1; margin: 20px 0; }
.markdown-body :deep(code) { padding: 2px 5px; background: #f1f4f8; border-radius: 4px; }
.markdown-body :deep(.md-code) { overflow: auto; padding: 12px; background: #f6f8fb; border-radius: 6px; }
.markdown-body :deep(h1:first-child),
.markdown-body :deep(h2:first-child) { margin-top: 0; }
</style>
