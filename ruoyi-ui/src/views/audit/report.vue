<template>
  <div class="app-container">
    <el-form :model="q" inline>
      <el-form-item label="项目">
        <el-select v-model="q.projectId" placeholder="全部项目" clearable @change="getList" style="width:260px">
          <el-option v-for="p in projects" :key="p.id" :label="p.project_name || p.projectName" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="版本">
        <el-select v-model="q.versionType" placeholder="全部" clearable @change="getList" style="width:150px">
          <el-option v-for="v in versions" :key="v" :label="v" :value="v" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">查询</el-button>
        <el-button icon="Refresh" @click="q = {}; getList()">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" icon="Plus" @click="openAdd">新建报告</el-button></el-col>
    </el-row>

    <el-table v-loading="loading" :data="list" border highlight-current-row>
      <el-table-column label="项目" prop="projectName" min-width="180" show-overflow-tooltip />
      <el-table-column label="报告标题" prop="title" min-width="220" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.title || scope.row.projectName || '未命名报告' }}</template>
      </el-table-column>
      <el-table-column label="版本" prop="versionType" width="130" align="center">
        <template #default="scope"><el-tag :type="versionTag(scope.row.versionType)" size="small">{{ scope.row.versionType }}</el-tag></template>
      </el-table-column>
      <el-table-column label="来源" width="90" align="center">
        <template #default="scope"><el-tag :type="scope.row.fileUrl ? 'success' : 'info'" size="small">{{ scope.row.fileUrl ? 'Word' : '文本' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="scope"><el-tag :type="scope.row.status === 2 ? 'success' : scope.row.status === 1 ? 'warning' : 'info'" size="small">{{ scope.row.status === 2 ? '已审定' : scope.row.status === 1 ? '待审定' : '草稿' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="180" />
      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="primary" @click="openReportDoc(scope.row, false)">预览</el-button>
          <el-button link type="primary" @click="openReportDoc(scope.row, true)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="新建报告" v-model="dlgAdd" width="620px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="项目">
          <el-select v-model="form.projectId" style="width:100%" filterable>
            <el-option v-for="p in projects" :key="p.id" :label="p.project_name || p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本">
          <el-select v-model="form.versionType" style="width:100%">
            <el-option v-for="v in versions" :key="v" :label="v" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="报告标题"><el-input v-model="form.title" placeholder="请输入报告标题" /></el-form-item>
        <el-form-item label="创建方式">
          <el-radio-group v-model="createMode">
            <el-radio-button label="new">新建Word</el-radio-button>
            <el-radio-button label="import">导入Word</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="createMode === 'import'" label="Word文件">
          <el-upload action="#" :http-request="uploadReportWord" :limit="1" accept=".docx" drag>
            <el-icon class="el-icon--upload upload-icon"><Upload /></el-icon>
            <div class="el-upload__text">拖拽 Word 文件到此处，或 <em>点击导入</em></div>
            <template #tip><div class="el-upload__tip">支持 .docx，导入后自动打开预览编辑。</div></template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgAdd = false">取消</el-button>
        <el-button v-if="createMode === 'new'" type="primary" @click="createBlankReport">新建Word</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ops } from '@/api/audit/auditOps'
import { getProgress } from '@/api/audit/auditInfo'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { cleanupUploadedFile } from '@/api/upload'

const router = useRouter()
const versions = ['处内审核稿', '征求意见稿', '正式稿']
const projects = ref([])
const q = ref({})
const list = ref([])
const loading = ref(false)
const dlgAdd = ref(false)
const createMode = ref('new')
const form = ref({ versionType: '处内审核稿' })
const blankReportUrl = '/audit-template/default/audit-report-draft-template.docx'

onMounted(() => {
  getProgress().then(r => { projects.value = r.data || [] })
  getList()
})

function getList() {
  loading.value = true
  ops.reportList(q.value.projectId).then(r => {
    let data = r.data || r.rows || []
    if (q.value.versionType) data = data.filter(d => d.versionType === q.value.versionType)
    list.value = data
  }).finally(() => { loading.value = false })
}

function openAdd() {
  form.value = { versionType: '处内审核稿' }
  createMode.value = 'new'
  dlgAdd.value = true
}

function createBlankReport() {
  if (!validateBase()) return
  const title = form.value.title || '审计报告'
  ops.addReport({
    projectId: form.value.projectId,
    versionType: form.value.versionType,
    title,
    content: '[Word报告] ' + title,
    fileUrl: blankReportUrl
  }).then(r => {
    ElMessage.success('报告已创建')
    dlgAdd.value = false
    getList()
    const id = r.data?.id || r.id
    openReportDoc({ ...form.value, id, title, fileUrl: blankReportUrl }, true)
  })
}

function uploadReportWord(opt) {
  if (!validateBase()) {
    opt.onError?.()
    return
  }
  if (getFileExt(opt.file.name) !== 'docx') {
    ElMessage.warning('请导入 .docx 文件')
    opt.onError?.()
    return
  }
  const fd = new FormData()
  fd.append('file', opt.file)
  let uploadedPath = ''
  request({ url: '/common/upload', method: 'post', data: fd, headers: { 'Content-Type': 'multipart/form-data' } }).then(res => {
    const fileUrl = res.fileName || res.url
    uploadedPath = fileUrl
    const title = form.value.title || opt.file.name.replace(/\.docx$/i, '')
    return ops.addReport({ projectId: form.value.projectId, versionType: form.value.versionType, title, content: '[Word报告] ' + title, fileUrl }).then(r => ({ r, title, fileUrl }))
  }).then(({ r, title, fileUrl }) => {
    ElMessage.success('报告已导入')
    dlgAdd.value = false
    opt.onSuccess?.(r)
    getList()
    const id = r.data?.id || r.id
    openReportDoc({ ...form.value, id, title, fileUrl }, true)
  }).catch(() => {
    cleanupUploadedFile(uploadedPath)
    ElMessage.error('导入失败')
    opt.onError?.()
  })
}

function openReportDoc(row, edit) {
  if (!row.fileUrl) return ElMessage.warning('该报告没有可预览的 Word 文件')
  const url = row.fileUrl.includes('/audit-template/default/') ? row.fileUrl + '?v=20260714' : row.fileUrl
  router.push('/audit/doc-preview?url=' + encodeURIComponent(url) + '&name=' + encodeURIComponent(getPreviewName(row.fileUrl, row.title || row.projectName || '审计报告')) + '&reportId=' + encodeURIComponent(row.id) + '&returnPath=' + encodeURIComponent('/audit/report') + (edit ? '&edit=1' : ''))
}

function validateBase() {
  if (!form.value.projectId) {
    ElMessage.warning('请选择项目')
    return false
  }
  if (!form.value.versionType) {
    ElMessage.warning('请选择版本')
    return false
  }
  return true
}

function versionTag(version) {
  return { '处内审核稿': '', '征求意见稿': 'warning', '正式稿': 'success' }[version] || ''
}

function getFileExt(name) {
  const cleanName = decodeURIComponent(String(name || '').split('?')[0])
  return cleanName.includes('.') ? cleanName.split('.').pop().toLowerCase() : ''
}

function getPreviewName(fileUrl, fallback) {
  const ext = getFileExt(fileUrl)
  const fallbackName = fallback || '文档'
  if (!ext) return fallbackName
  return fallbackName.toLowerCase().endsWith('.' + ext) ? fallbackName : fallbackName + '.' + ext
}
</script>

<style scoped>
.upload-icon {
  font-size: 40px;
  color: #909399;
}
</style>
