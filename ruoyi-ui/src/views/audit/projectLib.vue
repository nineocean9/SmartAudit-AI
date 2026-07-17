<template>
  <div class="app-container">
    <el-row :gutter="12" style="height: calc(100vh - 100px)">
      <!-- 左侧：项目树 -->
      <el-col :span="8" style="height: 100%">
        <el-card style="height: 100%; overflow-y: auto">
          <template #header>
            <span>项目库</span>
            <el-button size="small" type="primary" style="float:right" @click="showUpload = true">上传</el-button>
          </template>
          <el-tree
            :data="projectTree"
            :props="treeProps"
            node-key="id"
            :current-node-key="curProjectId"
            :highlight-current="true"
            @node-click="onNodeClick"
          >
            <template #default="{ node, data }">
              <span :class="{ 'tree-node-plan': data.isPlan, 'tree-node-proj': !data.isPlan }">
                <el-icon v-if="data.isPlan"><Folder /></el-icon>
                <el-icon v-else><Document /></el-icon>
                {{ data.isPlan ? data.label : data.projectName }}
                <el-tag v-if="data.docCount !== undefined" size="small" type="info">{{ data.docCount }}</el-tag>
              </span>
            </template>
          </el-tree>
          <el-empty v-if="!projectTree || projectTree.length === 0" description="暂无项目" />
        </el-card>
      </el-col>

      <!-- 右侧：项目资料列表 -->
      <el-col :span="16" style="height: 100%">
        <el-card v-if="curProjectId" style="height: 100%; overflow-y: auto">
          <template #header>
            <span>{{ curProjectName }} · 资料列表</span>
            <el-button size="small" style="float:right" @click="showUpload = true">+ 上传</el-button>
          </template>

          <div v-if="loadingDocs" v-loading="true" style="height: 200px" />
          <template v-else>
            <div v-for="(group, type) in docsByType" :key="type" style="margin-bottom: 16px">
              <h4 style="margin: 0 0 8px; color: #606266;">{{ type }} ({{ group.length }})</h4>
              <div v-for="doc in group" :key="doc.id" class="doc-item">
                <el-icon><Document /></el-icon>
                <span class="doc-name">{{ doc.fileName }}</span>
                <el-tag v-if="doc.status === 0" size="small" type="warning">待确认</el-tag>
                <span class="doc-meta">{{ formatSize(doc.fileSize) }} · {{ doc.createTime }}</span>
                <span style="margin-left: auto">
                  <el-button v-if="doc.status === 0" text size="small" type="warning" @click="goConfirm">去确认</el-button>
                  <el-button v-else text size="small" @click="viewDoc(doc)">查看</el-button>
                  <el-button v-if="doc.status !== 0" text size="small" @click="downloadDoc(doc)">下载</el-button>
                  <el-button v-if="doc.status !== 0" text size="small" type="danger" @click="removeDoc(doc.id)">删除</el-button>
                </span>
              </div>
              <el-empty v-if="group.length === 0" :description="`暂无${type}`" />
            </div>
            <el-empty v-if="Object.keys(docsByType).length === 0" description="暂无资料" />
          </template>
        </el-card>
        <el-card v-else style="height: 100%">
          <el-empty description="请从左侧选择一个项目" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 上传弹窗 -->
    <FileUploader v-if="showUpload" :autoOpen="true" :tempSessionId="tempSessionId" @uploadSuccess="onUploaded" @uploadComplete="showUpload = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, Document } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'
import { listProjectTree } from '@/api/ai/workspace'
import { listProjectDoc, deleteProjectDoc, getProjectDocContent } from '@/api/audit/projectDoc'
import { createTempSession } from '@/api/knowledge/tempWorkspace'
import FileUploader from '@/components/AiChat/FileUploader.vue'

const router = useRouter()
const route = useRoute()
const projectTree = ref([])
const curProjectId = ref(null)
const curProjectName = ref('')
const documents = ref([])
const loadingDocs = ref(false)
const showUpload = ref(false)
const tempSessionId = ref(null)

onMounted(async () => {
  await loadTree()
  restoreSelectedProject()
  // 初始化临时 session
  createTempSession().then(res => {
    if (res.code === 200) tempSessionId.value = res.data.sessionId
  }).catch(() => {})
})

const treeProps = {
  children: 'children',
  label: 'label'
}

async function loadTree() {
  try {
    const res = await listProjectTree()
    if (res.code === 200) {
      // 转换为树形数据
      projectTree.value = (res.data || []).map(plan => ({
        id: 'plan_' + plan.planId,
        isPlan: true,
        label: `${plan.planName} (${plan.planYear})`,
        children: (plan.projects || []).map(p => ({
          id: p.id,
          isPlan: false,
          projectName: p.projectName,
          auditedUnit: p.auditedUnit,
          auditType: p.auditType,
          docCount: p.docCount,
          status: p.status,
          label: p.projectName
        }))
      }))
    }
  } catch { /* ignore */ }
}

function onNodeClick(data) {
  if (!data.id || data.isPlan) return // 跳过计划节点
  curProjectId.value = data.id
  curProjectName.value = data.projectName || data.label
  router.replace({ path: route.path, query: { ...route.query, projectId: String(data.id) } })
  loadDocs(data.id)
}

function restoreSelectedProject() {
  const projectId = Number(route.query.projectId)
  if (!projectId) return
  for (const plan of projectTree.value) {
    const project = (plan.children || []).find(item => Number(item.id) === projectId)
    if (project) {
      curProjectId.value = project.id
      curProjectName.value = project.projectName || project.label
      loadDocs(project.id)
      return
    }
  }
}

async function loadDocs(projectId) {
  loadingDocs.value = true
  try {
    const res = await listProjectDoc(projectId)
    if (res.code === 200) documents.value = res.data || []
  } catch {
    documents.value = []
  } finally {
    loadingDocs.value = false
  }
}

const docsByType = computed(() => {
  const groups = {}
  for (const doc of documents.value) {
    const t = doc.docType || '其他'
    if (!groups[t]) groups[t] = []
    groups[t].push(doc)
  }
  return groups
})

function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}

function viewDoc(doc) {
  // Excel 文件 → 跳转到独立表格查看页
  const ext = doc.fileName?.split('.').pop()?.toLowerCase()
  if (ext === 'xlsx' || ext === 'xls') {
    router.push(`/audit/excel-view?id=${doc.id}&returnPath=${encodeURIComponent(projectReturnPath())}`)
    return
  }
  // Word / PDF 文件 → 跳转到文档预览页
  if (ext === 'docx' || ext === 'pdf') {
    router.push(`/audit/doc-preview?id=${doc.id}&returnPath=${encodeURIComponent(projectReturnPath())}`)
    return
  }
  // 其他文件 → 弹窗显示纯文本
  const content = doc.contentText || ''
  console.log('viewDoc content length:', content.length)
  if (content.startsWith('[文件:')) {
    ElMessageBox.confirm(
      '该文档无法在线预览，是否下载原文件查看？',
      doc.fileName,
      { confirmButtonText: '下载原文件', cancelButtonText: '取消', type: 'info' }
    ).then(() => downloadDoc(doc)).catch(() => {})
    return
  }
  if (!content || content.trim() === '') {
    ElMessage.warning('文档内容为空，请下载原文件查看')
    return
  }
  ElMessageBox.alert(
    `<div style="max-height:400px;overflow-y:auto;white-space:pre-wrap;font-size:13px;background:#f5f7fa;padding:12px;border-radius:6px">${escapeHtml(content)}</div>`,
    doc.fileName,
    { dangerouslyUseHTMLString: true, width: '700px' }
  )
}

function projectReturnPath() {
  return '/audit/projectLib?projectId=' + encodeURIComponent(curProjectId.value || '')
}

function goConfirm() {
  ElMessage.warning('待被审计单位负责人确认，确认后才能在项目库查看')
  router.push({ path: '/audit/prepare', query: { projectId: curProjectId.value } })
}

function downloadDoc(doc) {
  const baseUrl = import.meta.env.VITE_APP_BASE_API || ''
  const url = `${baseUrl}/project/doc/${doc.id}/file`
  const token = getToken()
  // 用 fetch 下载（带认证头）
  fetch(url, { headers: { Authorization: 'Bearer ' + token } })
    .then(res => {
      if (!res.ok) throw new Error('下载失败')
      return res.blob()
    })
    .then(blob => {
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = doc.fileName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(link.href)
    })
    .catch(() => {
      ElMessage.error('下载失败')
    })
}

function escapeHtml(str) {
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

async function removeDoc(id) {
  try {
    await ElMessageBox.confirm('确认删除该文档？', '提示', { type: 'warning' })
  } catch { return }
  try {
    const res = await deleteProjectDoc(id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      loadDocs(curProjectId.value)
    }
  } catch { /* ignore */ }
}

function onUploaded() {
  if (curProjectId.value) loadDocs(curProjectId.value)
}
</script>

<style scoped>
.doc-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 4px;
  font-size: 13px;
  transition: background 0.15s;
}
.doc-item:hover {
  background: #f5f7fa;
}
.doc-name {
  font-weight: 500;
  color: #303133;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc-meta {
  color: #c0c4cc;
  font-size: 12px;
}
.tree-node-plan, .tree-node-proj {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
