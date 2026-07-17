<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams">
      <el-form-item label="审计类型">
        <el-select v-model="queryParams.auditType" clearable placeholder="全部" @change="getList">
          <el-option v-for="type in auditTypes" :key="type" :label="type" :value="type" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" icon="Plus" @click="handleAdd">新增模板</el-button>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="previewData ? 6 : 24">
        <el-table v-loading="loading" :data="list" highlight-current-row @row-click="handlePreview" :row-class-name="rowClass">
          <el-table-column label="模板名称" prop="templateName" min-width="220" show-overflow-tooltip />
          <el-table-column label="审计类型" prop="auditType" width="140" align="center">
            <template #default="scope">
              <el-tag>{{ scope.row.auditType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="90" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.fileUrl ? 'success' : 'info'" size="small">{{ scope.row.fileUrl ? 'Word' : '文本' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">{{ scope.row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" v-if="!previewData">
            <template #default="scope">
              <el-button link type="primary" @click.stop="handlePreview(scope.row)">预览</el-button>
              <el-button link type="primary" @click.stop="handleEdit(scope.row)">编辑</el-button>
              <el-button link type="danger" @click.stop="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-col>

      <el-col :span="18" v-if="previewData">
        <el-card class="preview-card">
          <template #header>
            <div class="preview-header">
              <div>
                <el-icon style="margin-right:4px;vertical-align:middle"><Document /></el-icon>
                <span style="font-weight:600">{{ previewData.templateName }}</span>
                <el-tag style="margin-left:8px" size="small">{{ previewData.auditType }}</el-tag>
              </div>
              <div>
                <el-button size="small" type="primary" @click="handleEdit(previewData)">编辑</el-button>
                <el-button size="small" @click="previewData = null">关闭</el-button>
              </div>
            </div>
          </template>
          <div class="template-preview" v-html="formatPreview(previewData.content)" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="640px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板名称">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="审计类型">
          <el-select v-model="form.auditType" style="width:100%">
            <el-option v-for="type in auditTypes" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>

        <template v-if="!form.id">
          <el-form-item label="创建方式">
            <el-radio-group v-model="createMode">
              <el-radio-button label="new">新建Word</el-radio-button>
              <el-radio-button label="import">导入Word</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="createMode === 'import'" label="Word文件">
            <el-upload action="#" :http-request="doUploadWord" :limit="1" accept=".docx" drag>
              <el-icon class="el-icon--upload upload-icon"><Upload /></el-icon>
              <div class="el-upload__text">拖拽 Word 文件到此处，或 <em>点击导入</em></div>
              <template #tip>
                <div class="el-upload__tip">支持 .docx，导入后会直接打开预览页面。</div>
              </template>
            </el-upload>
          </el-form-item>
        </template>

        <el-alert
          v-else
          type="info"
          :closable="false"
          show-icon
          title="模板正文请通过预览页面编辑；这里仅维护模板名称、类型和状态。"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="form.id" type="primary" @click="submitForm">保存</el-button>
        <el-button v-else-if="createMode === 'new'" type="primary" :loading="creatingWord" @click="createWordTemplate">新建Word</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Upload } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { cleanupUploadedFile } from '@/api/upload'

const router = useRouter()
const route = useRoute()
const auditTypes = ['经济责任审计', '财务收支审计', '专项审计', '工程审计']
const loading = ref(false)
const list = ref([])
const queryParams = ref({ auditType: '' })
const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = ref({ status: 1, auditType: '经济责任审计' })
const createMode = ref('new')
const creatingWord = ref(false)
const previewData = ref(null)

function getList() {
  loading.value = true
  request({ url: '/audit/prepare/template/list', params: queryParams.value }).then(res => {
    list.value = res.rows || res.data || []
  }).finally(() => {
    loading.value = false
  })
}

function handlePreview(row) {
  if (row.fileUrl) {
    viewWord(row)
    return
  }
  previewData.value = row
}

function viewWord(row, edit = false) {
  if (!row.fileUrl) return
  const url = row.fileUrl.includes('/audit-template/default/')
    ? row.fileUrl + (row.fileUrl.includes('?') ? '&' : '?') + 'v=20260714'
    : row.fileUrl
  const query = [
    'url=' + encodeURIComponent(url),
    'name=' + encodeURIComponent(getPreviewName(row.fileUrl, row.templateName || '方案模板')),
    'templateId=' + encodeURIComponent(row.id || ''),
    'returnPath=' + encodeURIComponent(route.fullPath)
  ]
  if (edit) query.push('edit=1')
  router.push('/audit/doc-preview?' + query.join('&'))
}

function rowClass({ row }) {
  return previewData.value?.id === row.id ? 'current-row' : ''
}

function handleAdd() {
  form.value = { status: 1, auditType: queryParams.value.auditType || '经济责任审计' }
  createMode.value = 'new'
  dialogTitle.value = '新增模板'
  dialogVisible.value = true
}

function handleEdit(row) {
  form.value = { ...row }
  dialogTitle.value = '编辑模板'
  dialogVisible.value = true
}

function submitForm() {
  request({ url: '/audit/prepare/template', method: 'put', data: form.value }).then(() => {
    ElMessage.success('保存成功')
    dialogVisible.value = false
    getList()
  })
}

function createWordTemplate() {
  if (!validateBaseForm()) return
  creatingWord.value = true
  request({
    url: '/audit/prepare/template/generate-docx',
    method: 'post',
    data: form.value
  }).then(res => {
    const data = res.data
    ElMessage.success('Word模板已创建')
    dialogVisible.value = false
    getList()
    if (data?.fileUrl) viewWord(data, true)
  }).finally(() => {
    creatingWord.value = false
  })
}

function doUploadWord(opt) {
  if (!validateBaseForm()) {
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
    return request({
      url: '/audit/prepare/template',
      method: 'post',
      data: {
        ...form.value,
        content: '[Word模板] ' + form.value.templateName,
        fileUrl,
        status: form.value.status ?? 1
      }
    }).then(recordRes => {
      const record = recordRes.data || { ...form.value, fileUrl, id: recordRes.id }
      ElMessage.success('Word模板已导入')
      dialogVisible.value = false
      opt.onSuccess?.(recordRes)
      getList()
      viewWord(record, true)
    })
  }).catch(() => {
    cleanupUploadedFile(uploadedPath)
    ElMessage.error('导入失败')
    opt.onError?.()
  })
}

function handleDelete(row) {
  ElMessageBox.confirm('确认删除该模板？').then(() => {
    request({ url: '/audit/prepare/template/' + row.id, method: 'delete' }).then(() => {
      ElMessage.success('删除成功')
      if (previewData.value?.id === row.id) previewData.value = null
      getList()
    })
  })
}

function validateBaseForm() {
  if (!form.value.templateName) {
    ElMessage.warning('请先填写模板名称')
    return false
  }
  if (!form.value.auditType) {
    ElMessage.warning('请选择审计类型')
    return false
  }
  return true
}

function formatPreview(text) {
  if (!text) return '<div style="text-align:center;color:#909399;padding:40px">暂无内容</div>'
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br>')
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

onMounted(() => getList())
</script>

<style scoped>
:deep(.preview-card .el-card__body) {
  padding: 8px;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.template-preview {
  padding: 20px 24px;
  min-height: 520px;
  max-height: calc(100vh - 245px);
  overflow-y: auto;
  font-size: 14px;
  line-height: 2;
  color: #303133;
  background: #fafbfc;
  border-radius: 4px;
}

.upload-icon {
  font-size: 40px;
  color: #909399;
}

:deep(.el-table .current-row) {
  background-color: #ecf5ff !important;
}
</style>
