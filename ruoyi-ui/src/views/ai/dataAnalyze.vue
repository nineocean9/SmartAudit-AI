<template>
  <div class="app-container">
    <el-row :gutter="12">
      <el-col :span="24">
        <el-card style="margin-bottom: 12px">
          <template #header>数据分析</template>
          <div class="upload-area" @drop.prevent="onDrop" @dragover.prevent>
            <el-icon class="upload-icon" :size="36"><UploadFilled /></el-icon>
            <p>拖拽文件到此处 或 <el-button text type="primary" @click="triggerUpload">点击上传</el-button></p>
            <p style="font-size:12px;color:#c0c4cc">支持 Excel / CSV / Word / PDF</p>
            <input ref="fileInputRef" type="file" multiple accept=".xlsx,.xls,.csv,.doc,.docx,.pdf,.txt" style="display:none" @change="onFileSelect" />
          </div>
          <div v-if="uploadedFiles.length > 0" style="margin-top: 12px">
            <h4>已上传文件 ({{ uploadedFiles.length }})</h4>
            <div v-for="(f, i) in uploadedFiles" :key="i" class="file-item">
              <el-icon><Document /></el-icon>
              <span>{{ f.name }}</span>
              <span style="color:#909399;font-size:12px">{{ formatSize(f.size) }}</span>
              <el-button text size="small" type="danger" @click="removeFile(i)" style="margin-left:auto">移除</el-button>
            </div>
          </div>
        </el-card>

        <el-card>
          <div style="display:flex;gap:8px;margin-bottom:12px">
            <el-input v-model="instruction" placeholder="输入分析指令，如「统计采购异常」" clearable />
            <el-button type="primary" :disabled="!instruction.trim() || analyzing" @click="startAnalysis">
              {{ analyzing ? '分析中...' : '分析' }}
            </el-button>
          </div>
          <el-divider />
          <div v-if="analyzing" class="result-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>AI 分析中...</span>
          </div>
          <div v-else-if="resultContent" class="result-content markdown-body" v-html="renderMarkdown(resultContent)" />
          <el-empty v-else description="上传文件并输入分析指令后开始分析" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document, Loading } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'

const fileInputRef = ref(null)
const uploadedFiles = ref([])
const instruction = ref('')
const analyzing = ref(false)
const resultContent = ref('')

const baseUrl = import.meta.env.VITE_APP_BASE_API || ''

function triggerUpload() {
  fileInputRef.value?.click()
}

function onFileSelect(e) {
  const files = Array.from(e.target.files || [])
  addFiles(files)
}

function onDrop(e) {
  const files = Array.from(e.dataTransfer.files || [])
  addFiles(files)
}

function addFiles(files) {
  for (const f of files) {
    if (!uploadedFiles.value.some(ex => ex.name === f.name && ex.size === f.size)) {
      uploadedFiles.value.push(f)
    }
  }
}

function removeFile(index) {
  uploadedFiles.value.splice(index, 1)
}

function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}

async function startAnalysis() {
  if (!instruction.value.trim() || analyzing.value) return
  analyzing.value = true
  resultContent.value = ''

  const token = getToken()
  const formData = new FormData()
  for (const f of uploadedFiles.value) {
    formData.append('files', f)
  }
  formData.append('instruction', instruction.value)

  try {
    // 使用 SSE 流式接收分析结果
    const params = new URLSearchParams()
    params.append('instruction', instruction.value)
    for (const f of uploadedFiles.value) {
      params.append('fileNames', f.name)
    }
    // 如果有文件，走 POST SSE；否则走 GET
    const url = uploadedFiles.value.length > 0
      ? `${baseUrl}/ai/data/analyze/files?instruction=${encodeURIComponent(instruction.value)}`
      : `${baseUrl}/ai/data/analyze?instruction=${encodeURIComponent(instruction.value)}`

    if (uploadedFiles.value.length > 0) {
      // 文件上传分析 - 用 fetch 上传
      const response = await fetch(url, {
        method: uploadedFiles.value.length > 0 ? 'POST' : 'GET',
        headers: { Authorization: 'Bearer ' + token },
        ...(uploadedFiles.value.length > 0 ? { body: formData } : {})
      })
      if (!response.ok) throw new Error('HTTP ' + response.status)
      const text = await response.text()
      resultContent.value = text
    } else {
      // 无需文件，直接分析
      const response = await fetch(url, {
        headers: { Authorization: 'Bearer ' + token }
      })
      const json = await response.json()
      if (json.code === 200) {
        resultContent.value = json.data?.summary || JSON.stringify(json.data)
      } else {
        ElMessage.error(json.msg || '分析失败')
      }
    }
  } catch (e) {
    ElMessage.error('分析失败: ' + (e.message || ''))
  } finally {
    analyzing.value = false
  }
}

// Markdown 轻量渲染（复用 chat.vue 中的逻辑）
function escapeHtml(str) {
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}
function renderMarkdown(text) {
  if (!text) return ''
  let html = escapeHtml(text)
  html = html.replace(/```[\w]*\n?([\s\S]*?)```/g, '<pre class="code-block"><code>$1</code></pre>')
  html = html.replace(/`([^`\n]+)`/g, '<code class="inline-code">$1</code>')
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
  html = html.replace(/^[-*] (.+)$/gm, '<li>$1</li>')
  html = html.replace(/^\d+\. (.+)$/gm, '<li>$1</li>')
  html = html.replace(/^---+$/gm, '<hr>')
  html = html.replace(/\n/g, '<br>')
  return html
}
</script>

<style scoped>
.upload-area {
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  padding: 32px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.upload-area:hover {
  border-color: #409eff;
  background: #f0f7ff;
}
.upload-icon {
  color: #c0c4cc;
  margin-bottom: 8px;
}
.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 4px;
  font-size: 13px;
}
.result-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #909399;
}
.result-content {
  padding: 8px 0;
  line-height: 1.75;
  font-size: 14px;
}
.result-content :deep(pre.code-block) {
  background: #1e1e2e;
  color: #cdd6f4;
  padding: 14px 18px;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 13px;
  margin: 8px 0;
}
.result-content :deep(code.inline-code) {
  background: #f0f2f5;
  color: #e83e8c;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 13px;
}
</style>
