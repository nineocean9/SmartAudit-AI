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
          <el-button type="warning" plain icon="Download" @click="handleDownload">下载</el-button>
        </el-col>
        <!-- Word 编辑模式切换 -->
        <template v-if="fileType === 'docx'">
          <el-col :span="1.5">
            <el-button :type="editMode ? 'danger' : 'primary'" plain @click="toggleEditMode">
              <el-icon style="margin-right:4px"><component :is="editMode ? 'View' : 'Edit'" /></el-icon>
              {{ editMode ? '退出编辑' : '编辑文档' }}
            </el-button>
          </el-col>
          <el-col :span="1.5" v-if="editMode">
            <el-button type="success" plain icon="Check" @click="saveDocument" :loading="saving">保存</el-button>
          </el-col>
        </template>
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

      <!-- Word 编辑工具栏 -->
      <el-card v-if="fileType === 'docx' && editMode" class="editor-toolbar" :body-style="{ padding: '8px 12px' }">
        <div class="toolbar-row">
          <el-tooltip content="加粗" placement="bottom"><el-button size="small" @click="execCmd('bold')" :type="isBold ? 'primary' : ''"><b>B</b></el-button></el-tooltip>
          <el-tooltip content="斜体" placement="bottom"><el-button size="small" @click="execCmd('italic')"><i>I</i></el-button></el-tooltip>
          <el-tooltip content="下划线" placement="bottom"><el-button size="small" @click="execCmd('underline')"><u>U</u></el-button></el-tooltip>
          <el-tooltip content="删除线" placement="bottom"><el-button size="small" @click="execCmd('strikeThrough')"><s>S</s></el-button></el-tooltip>
          <el-divider direction="vertical" />
          <el-tooltip content="左对齐" placement="bottom"><el-button size="small" @click="execCmd('justifyLeft')">≡←</el-button></el-tooltip>
          <el-tooltip content="居中" placement="bottom"><el-button size="small" @click="execCmd('justifyCenter')">≡↔</el-button></el-tooltip>
          <el-tooltip content="右对齐" placement="bottom"><el-button size="small" @click="execCmd('justifyRight')">≡→</el-button></el-tooltip>
          <el-divider direction="vertical" />
          <el-tooltip content="无序列表" placement="bottom"><el-button size="small" @click="execCmd('insertUnorderedList')">• 列表</el-button></el-tooltip>
          <el-tooltip content="有序列表" placement="bottom"><el-button size="small" @click="execCmd('insertOrderedList')">1. 列表</el-button></el-tooltip>
          <el-divider direction="vertical" />
          <el-select v-model="fontSizeVal" size="small" style="width:80px" @change="setFontSize" placeholder="字号">
            <el-option v-for="s in [1,2,3,4,5,6,7]" :key="s" :label="['极小','小','正常','中','大','很大','极大'][s-1]" :value="s" />
          </el-select>
          <el-select v-model="headingVal" size="small" style="width:100px;margin-left:6px" @change="setHeading" placeholder="段落">
            <el-option label="正文" value="p" />
            <el-option label="标题1" value="h1" />
            <el-option label="标题2" value="h2" />
            <el-option label="标题3" value="h3" />
            <el-option label="标题4" value="h4" />
          </el-select>
          <el-divider direction="vertical" />
          <el-color-picker v-model="fontColor" size="small" @change="setFontColor" />
          <el-tooltip content="撤销" placement="bottom"><el-button size="small" @click="execCmd('undo')">↶</el-button></el-tooltip>
          <el-tooltip content="重做" placement="bottom"><el-button size="small" @click="execCmd('redo')">↷</el-button></el-tooltip>
          <el-divider direction="vertical" />
          <el-tooltip content="插入表格" placement="bottom"><el-button size="small" @click="insertTable">表格</el-button></el-tooltip>
          <el-tooltip content="插入分割线" placement="bottom"><el-button size="small" @click="execCmd('insertHorizontalRule')">—</el-button></el-tooltip>
        </div>
      </el-card>

      <!-- Word 预览/编辑区域 -->
      <el-card v-if="fileType === 'docx'" :body-style="{ padding: 0 }">
        <!-- 预览模式 -->
        <div v-show="!editMode" ref="docxContainer" class="docx-wrapper" />
        <!-- 编辑模式 -->
        <div v-show="editMode" class="editor-wrapper">
          <div
            ref="editorArea"
            class="editor-content"
            contenteditable="true"
            @input="onEditorInput"
            @keydown="onEditorKeydown"
          />
        </div>
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
import { Document, Edit, View, Check } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'

const route = useRoute()
const loading = ref(true)
const error = ref(null)
const unsupported = ref(false)
const fileName = ref('')
const fileType = ref('')
const fileBlob = ref(null)
const docId = ref(null)
const docxContainer = ref(null)
const pdfContainer = ref(null)
const pdfCanvases = ref({})
const pdfScale = ref(1.0)
const totalPages = ref(0)
let pdfDoc = null

// Word 编辑相关
const editMode = ref(false)
const editorArea = ref(null)
const saving = ref(false)
const isBold = ref(false)
const fontSizeVal = ref(3)
const headingVal = ref('p')
const fontColor = ref('#303133')
let docxHtml = ''

onMounted(async () => {
  docId.value = route.query.id
  const fileUrl = route.query.url
  const nameParam = route.query.name

  if (!docId.value && !fileUrl) {
    loading.value = false
    error.value = '缺少文档参数'
    return
  }

  try {
    let arrayBuffer, name
    const baseUrl = import.meta.env.VITE_APP_BASE_API || ''

    if (docId.value) {
      const detailRes = await fetch(`${baseUrl}/project/doc/${docId.value}/detail`, {
        headers: { Authorization: 'Bearer ' + getToken() }
      })
      if (detailRes.ok) {
        const detailData = await detailRes.json()
        name = detailData.data?.fileName || detailData.fileName || ''
      }
      const res = await fetch(`${baseUrl}/project/doc/${docId.value}/file`, {
        headers: { Authorization: 'Bearer ' + getToken() }
      })
      if (!res.ok) throw new Error('文件加载失败 (' + res.status + ')')
      arrayBuffer = await res.arrayBuffer()
    } else {
      name = nameParam || fileUrl.split('/').pop()
      const fullUrl = fileUrl.startsWith('http') ? fileUrl : baseUrl + fileUrl
      const res = await fetch(fullUrl, { headers: { Authorization: 'Bearer ' + getToken() } })
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
    // 保存渲染后的HTML，供编辑模式使用
    docxHtml = docxContainer.value.innerHTML
  } catch (e) {
    error.value = 'Word 文档渲染失败: ' + (e.message || '')
  }
}

/** 切换编辑模式 */
async function toggleEditMode() {
  editMode.value = !editMode.value
  if (editMode.value) {
    await nextTick()
    // 将渲染的HTML复制到编辑器
    if (editorArea.value) {
      // 从docx-preview的渲染结果提取内容
      const wrapper = docxContainer.value?.querySelector('.docx-wrapper') || docxContainer.value
      if (wrapper) {
        editorArea.value.innerHTML = wrapper.innerHTML
      }
      editorArea.value.focus()
    }
  } else {
    // 退出编辑模式时，将编辑结果回写到预览区
    if (editorArea.value && docxContainer.value) {
      const wrapper = docxContainer.value.querySelector('.docx-wrapper')
      if (wrapper) {
        wrapper.innerHTML = editorArea.value.innerHTML
      }
    }
  }
}

/** 执行编辑命令 */
function execCmd(cmd, val = null) {
  document.execCommand(cmd, false, val)
  editorArea.value?.focus()
}

function setFontSize(size) {
  execCmd('fontSize', size)
}

function setHeading(tag) {
  execCmd('formatBlock', tag === 'p' ? 'p' : tag)
}

function setFontColor(color) {
  if (color) execCmd('foreColor', color)
}

function insertTable() {
  const html = `<table style="border-collapse:collapse;width:100%;margin:10px 0" border="1">
    <tr><td style="padding:8px;border:1px solid #ddd">单元格1</td><td style="padding:8px;border:1px solid #ddd">单元格2</td><td style="padding:8px;border:1px solid #ddd">单元格3</td></tr>
    <tr><td style="padding:8px;border:1px solid #ddd">&nbsp;</td><td style="padding:8px;border:1px solid #ddd">&nbsp;</td><td style="padding:8px;border:1px solid #ddd">&nbsp;</td></tr>
    <tr><td style="padding:8px;border:1px solid #ddd">&nbsp;</td><td style="padding:8px;border:1px solid #ddd">&nbsp;</td><td style="padding:8px;border:1px solid #ddd">&nbsp;</td></tr>
  </table><p></p>`
  execCmd('insertHTML', html)
}

function onEditorInput() {
  // 实时检测格式状态
  isBold.value = document.queryCommandState('bold')
}

function onEditorKeydown(e) {
  // Ctrl+B 加粗等快捷键已由 contenteditable 原生支持
  if (e.ctrlKey && e.key === 's') {
    e.preventDefault()
    saveDocument()
  }
}

/** 保存文档 */
async function saveDocument() {
  if (!editorArea.value) return
  saving.value = true

  try {
    const htmlContent = editorArea.value.innerHTML
    const baseUrl = import.meta.env.VITE_APP_BASE_API || ''

    if (docId.value) {
      // 保存为HTML内容到后端
      const res = await fetch(`${baseUrl}/project/doc/${docId.value}/saveHtml`, {
        method: 'POST',
        headers: {
          'Authorization': 'Bearer ' + getToken(),
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ htmlContent })
      })
      if (res.ok) {
        const data = await res.json()
        if (data.code === 200) {
          ElMessage.success('文档已保存')
        } else {
          ElMessage.error(data.msg || '保存失败')
        }
      } else {
        // 后端接口不存在时，降级为下载保存
        downloadAsHtml(htmlContent)
      }
    } else {
      // 无文档ID时，下载为HTML
      downloadAsHtml(htmlContent)
    }
  } catch {
    // 降级为下载保存
    downloadAsHtml(editorArea.value.innerHTML)
  } finally {
    saving.value = false
  }
}

function downloadAsHtml(html) {
  const fullHtml = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>${fileName.value}</title>
  <style>body{font-family:SimSun,'Microsoft YaHei',sans-serif;padding:40px 60px;max-width:900px;margin:0 auto}
  table{border-collapse:collapse;width:100%}td,th{border:1px solid #ccc;padding:6px 10px}</style></head>
  <body>${html}</body></html>`
  const blob = new Blob([fullHtml], { type: 'text/html;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = fileName.value.replace(/\.docx?$/i, '') + '_编辑版.html'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
  ElMessage.success('已导出为HTML文件')
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
/* 编辑工具栏 */
.editor-toolbar {
  margin-bottom: 0;
  border-radius: 4px 4px 0 0;
}
.toolbar-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
.toolbar-row .el-button { min-width: 32px; padding: 5px 8px; }

/* Word 预览容器 */
.docx-wrapper {
  background: #f0f2f5;
  min-height: 500px;
  max-height: calc(100vh - 180px);
  overflow-y: auto;
  padding: 20px;
}
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

/* Word 编辑容器 */
.editor-wrapper {
  background: #f0f2f5;
  min-height: 500px;
  max-height: calc(100vh - 240px);
  overflow-y: auto;
  padding: 20px;
}
.editor-content {
  background: #fff;
  padding: 40px 60px;
  margin: 0 auto;
  max-width: 900px;
  min-height: 600px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  outline: none;
  font-family: SimSun, 'Microsoft YaHei', serif;
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
}
.editor-content:focus {
  box-shadow: 0 2px 16px rgba(64, 158, 255, 0.2);
}
.editor-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 10px 0;
}
.editor-content :deep(td),
.editor-content :deep(th) {
  border: 1px solid #ddd;
  padding: 6px 10px;
  min-width: 60px;
}
.editor-content :deep(img) {
  max-width: 100%;
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
