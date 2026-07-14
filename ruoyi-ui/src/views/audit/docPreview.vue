<template>
  <div class="app-container">
    <!-- 加载中 -->
    <el-card v-if="loading" v-loading="true" element-loading-text="正在加载文档..." style="min-height:400px" />

    <!-- 错误提示 -->
    <el-card v-else-if="error">
      <el-empty :description="error">
        <el-button type="primary" @click="$router.back()">返回</el-button>
      </el-empty>
    </el-card>

    <!-- 不支持的格式 -->
    <el-card v-else-if="unsupported">
      <el-empty description="该文件格式不支持在线预览">
        <div>
          <el-button type="primary" @click="handleDownload">下载原文件查看</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </el-empty>
    </el-card>

    <template v-else>
      <!-- 顶部工具栏 -->
      <el-row :gutter="10" class="mb8" align="middle">
        <el-col :span="1.5">
          <el-button plain icon="Back" @click="$router.back()">返回</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="warning" plain icon="Download" @click="handleDownload">下载原文件</el-button>
        </el-col>
        <!-- PDF 缩放控件 -->
        <template v-if="fileType === 'pdf'">
          <el-col :span="1.5">
            <el-select v-model="pdfScale" style="width:100px" @change="renderAllPages">
              <el-option label="50%" :value="0.5" />
              <el-option label="75%" :value="0.75" />
              <el-option label="100%" :value="1.0" />
              <el-option label="125%" :value="1.25" />
              <el-option label="150%" :value="1.5" />
            </el-select>
          </el-col>
          <el-col :span="1.5">
            <span style="font-size:13px;color:#909399">共 {{ totalPages }} 页</span>
          </el-col>
        </template>
        <el-col :span="8" style="margin-left:auto;text-align:right">
          <el-icon style="margin-right:6px;vertical-align:middle"><Document /></el-icon>
          <span style="font-size:15px;font-weight:600;color:#303133">{{ fileName }}</span>
        </el-col>
      </el-row>

      <!-- Word 渲染区域 -->
      <el-card v-if="fileType === 'docx'" :body-style="{ padding: 0 }">
        <div ref="docxContainer" class="docx-wrapper" />
      </el-card>

      <!-- PDF 渲染区域 -->
      <div v-if="fileType === 'pdf'" ref="pdfContainer" class="pdf-wrapper">
        <canvas
          v-for="page in totalPages"
          :key="page"
          :ref="el => { if (el) pdfCanvases[page] = el }"
          class="pdf-page"
        />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'

const route = useRoute()
const loading = ref(true)
const error = ref(null)
const unsupported = ref(false)
const fileName = ref('')
const fileType = ref('')    // 'docx' | 'pdf'
const fileBlob = ref(null)
const docxContainer = ref(null)
const pdfContainer = ref(null)
const pdfCanvases = ref({})
const pdfScale = ref(1.0)
const totalPages = ref(0)
let pdfDoc = null

onMounted(async () => {
  const docId = route.query.id
  const fileUrl = route.query.url
  const nameParam = route.query.name

  if (!docId && !fileUrl) {
    loading.value = false
    error.value = '缺少文档参数'
    return
  }

  try {
    let arrayBuffer, name

    if (docId) {
      // 通过项目文档 ID 获取
      const baseUrl = import.meta.env.VITE_APP_BASE_API || ''
      // 先获取文件信息
      const detailRes = await fetch(`${baseUrl}/project/doc/${docId}/detail`, {
        headers: { Authorization: 'Bearer ' + getToken() }
      })
      if (detailRes.ok) {
        const detailData = await detailRes.json()
        name = detailData.data?.fileName || detailData.fileName || ''
      }
      // 获取文件内容
      const res = await fetch(`${baseUrl}/project/doc/${docId}/file`, {
        headers: { Authorization: 'Bearer ' + getToken() }
      })
      if (!res.ok) throw new Error('文件加载失败 (' + res.status + ')')
      arrayBuffer = await res.arrayBuffer()
    } else {
      // 通过 URL 直接获取（计划附件等）
      name = nameParam || fileUrl.split('/').pop()
      const baseUrl = import.meta.env.VITE_APP_BASE_API || ''
      const fullUrl = fileUrl.startsWith('http') ? fileUrl : baseUrl + fileUrl
      const res = await fetch(fullUrl, {
        headers: { Authorization: 'Bearer ' + getToken() }
      })
      if (!res.ok) throw new Error('文件加载失败')
      arrayBuffer = await res.arrayBuffer()
    }

    fileName.value = name || '未知文件'
    const ext = fileName.value.split('.').pop()?.toLowerCase()

    if (ext === 'docx') {
      fileType.value = 'docx'
      fileBlob.value = arrayBuffer
      loading.value = false
      await nextTick()
      await renderDocx(arrayBuffer)
    } else if (ext === 'pdf') {
      fileType.value = 'pdf'
      fileBlob.value = arrayBuffer
      loading.value = false
      await nextTick()
      await renderPdf(arrayBuffer)
    } else if (ext === 'doc') {
      loading.value = false
      unsupported.value = true
    } else {
      loading.value = false
      unsupported.value = true
    }
  } catch (e) {
    loading.value = false
    error.value = e.message || '文档加载失败'
  }
})

/** Word 渲染 */
async function renderDocx(buffer) {
  try {
    const { renderAsync } = await import('docx-preview')
    await renderAsync(buffer, docxContainer.value, null, {
      className: 'docx-body',
      inWrapper: true,
      ignoreWidth: false,
      ignoreHeight: false,
      ignoreFonts: false,
      breakPages: true,
      useBase64URL: true
    })
  } catch (e) {
    error.value = 'Word 文档渲染失败: ' + (e.message || '')
  }
}

/** PDF 渲染 */
async function renderPdf(buffer) {
  try {
    const pdfjsLib = await import('pdfjs-dist')
    pdfjsLib.GlobalWorkerOptions.workerSrc = '/pdf.worker.mjs'

    pdfDoc = await pdfjsLib.getDocument({ data: buffer }).promise
    totalPages.value = pdfDoc.numPages

    await nextTick()
    await renderAllPages()
  } catch (e) {
    error.value = 'PDF 渲染失败: ' + (e.message || '')
  }
}

async function renderAllPages() {
  if (!pdfDoc) return
  await nextTick()
  for (let i = 1; i <= totalPages.value; i++) {
    const page = await pdfDoc.getPage(i)
    const viewport = page.getViewport({ scale: pdfScale.value })
    const canvas = pdfCanvases.value[i]
    if (!canvas) continue
    const ctx = canvas.getContext('2d')
    canvas.width = viewport.width
    canvas.height = viewport.height
    await page.render({ canvasContext: ctx, viewport }).promise
  }
}

/** 下载原文件 */
function handleDownload() {
  if (fileBlob.value) {
    const blob = new Blob([fileBlob.value])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = fileName.value
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
  } else {
    ElMessage.warning('文件未加载完成')
  }
}
</script>

<style scoped>
/* Word 预览容器 */
.docx-wrapper {
  background: #f0f2f5;
  min-height: 500px;
  max-height: calc(100vh - 180px);
  overflow-y: auto;
  padding: 20px;
}
/* docx-preview 生成的内容样式覆盖 */
.docx-wrapper :deep(.docx-wrapper) {
  background: #fff;
  padding: 40px 60px;
  margin: 0 auto;
  max-width: 900px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}
.docx-wrapper :deep(.docx-wrapper > section) {
  margin-bottom: 20px;
}

/* PDF 预览容器 */
.pdf-wrapper {
  background: #525659;
  min-height: 500px;
  max-height: calc(100vh - 180px);
  overflow-y: auto;
  padding: 20px 0;
  text-align: center;
  border-radius: 4px;
}
.pdf-page {
  display: block;
  margin: 0 auto 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  background: #fff;
}
</style>
