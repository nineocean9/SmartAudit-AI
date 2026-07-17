<template>
  <div class="app-container doc-preview-page">
    <!-- 加载中 -->
    <el-card v-if="loading" v-loading="true" element-loading-text="正在加载文档..." style="min-height:400px" />

    <!-- 错误提示 -->
    <el-card v-else-if="error">
      <el-empty :description="error">
        <el-button type="primary" @click="goBack">返回</el-button>
      </el-empty>
    </el-card>

    <!-- 不支持的格式 -->
    <el-card v-else-if="unsupported">
      <el-empty description="该文件格式不支持在线预览">
        <div>
          <el-button type="primary" @click="handleDownload">下载原文件查看</el-button>
          <el-button @click="goBack">返回</el-button>
        </div>
      </el-empty>
    </el-card>

    <template v-else>
      <!-- 顶部工具栏 -->
      <el-row :gutter="10" class="mb8 preview-toolbar" align="middle">
        <el-col :span="1.5">
          <el-button plain icon="Back" @click="goBack">返回</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="warning" plain icon="Download" @click="handleDownload">下载</el-button>
        </el-col>
        <!-- Word 编辑模式切换 -->
        <template v-if="fileType === 'docx'">
          <el-col :span="1.5">
            <el-select v-model="wordScale" class="word-scale-select" @change="applyWordScale">
              <el-option label="适合宽度" value="fit" />
              <el-option label="60%" :value="0.6" />
              <el-option label="75%" :value="0.75" />
              <el-option label="90%" :value="0.9" />
              <el-option label="100%" :value="1" />
              <el-option label="125%" :value="1.25" />
            </el-select>
          </el-col>
          <el-col :span="1.5">
            <span class="page-count">共 {{ wordPageCount }} 页</span>
          </el-col>
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
        <el-col :span="6" class="preview-file-meta">
          <el-icon style="margin-right:6px;vertical-align:middle"><Document /></el-icon>
          <span class="preview-file-name" :title="fileName">{{ fileName }}</span>
        </el-col>
      </el-row>

      <!-- Word 编辑工具栏 -->
      <div v-if="fileType === 'docx' && editMode" class="editor-toolbar">
        <div class="toolbar-group">
          <!-- 字体选择 -->
          <el-select v-model="fontFamily" size="small" style="width:120px" @change="setFontFamily" placeholder="字体">
            <el-option label="宋体" value="SimSun" />
            <el-option label="微软雅黑" value="Microsoft YaHei" />
            <el-option label="黑体" value="SimHei" />
            <el-option label="楷体" value="KaiTi" />
            <el-option label="仿宋" value="FangSong" />
            <el-option label="Arial" value="Arial" />
            <el-option label="Times New Roman" value="Times New Roman" />
          </el-select>
          <!-- 字号 -->
          <el-select v-model="fontSizeVal" size="small" style="width:75px" @change="setFontSize" placeholder="字号">
            <el-option label="10px" value="1" />
            <el-option label="12px" value="2" />
            <el-option label="14px" value="3" />
            <el-option label="16px" value="4" />
            <el-option label="18px" value="5" />
            <el-option label="24px" value="6" />
            <el-option label="32px" value="7" />
          </el-select>
          <!-- 段落标题 -->
          <el-select v-model="headingVal" size="small" style="width:90px" @change="setHeading" placeholder="段落">
            <el-option label="正文" value="p" />
            <el-option label="标题1" value="h1" />
            <el-option label="标题2" value="h2" />
            <el-option label="标题3" value="h3" />
            <el-option label="标题4" value="h4" />
          </el-select>
        </div>
        <div class="toolbar-sep" />
        <div class="toolbar-group">
          <el-tooltip content="加粗 Ctrl+B"><el-button size="small" :class="{'is-active': states.bold}" @click="execCmd('bold')"><b>B</b></el-button></el-tooltip>
          <el-tooltip content="斜体 Ctrl+I"><el-button size="small" :class="{'is-active': states.italic}" @click="execCmd('italic')"><i>I</i></el-button></el-tooltip>
          <el-tooltip content="下划线 Ctrl+U"><el-button size="small" :class="{'is-active': states.underline}" @click="execCmd('underline')"><u>U</u></el-button></el-tooltip>
          <el-tooltip content="删除线"><el-button size="small" :class="{'is-active': states.strikeThrough}" @click="execCmd('strikeThrough')"><s>S</s></el-button></el-tooltip>
          <el-tooltip content="上标"><el-button size="small" @click="execCmd('superscript')">X²</el-button></el-tooltip>
          <el-tooltip content="下标"><el-button size="small" @click="execCmd('subscript')">X₂</el-button></el-tooltip>
        </div>
        <div class="toolbar-sep" />
        <div class="toolbar-group">
          <el-tooltip content="字体颜色">
            <el-color-picker v-model="fontColor" size="small" @change="setFontColor" />
          </el-tooltip>
          <!-- 字体颜色 -->
          <el-popover trigger="click" :width="220">
            <template #reference>
              <el-button size="small">
                <span class="font-color-button" :style="{ color: fontColor, borderColor: fontColor }">A</span>
              </el-button>
            </template>
            <div class="color-grid">
              <span v-for="c in colorPalette" :key="c" class="color-cell" :style="{background:c}" @click="setFontColor(c)" />
            </div>
          </el-popover>
          <!-- 背景色 -->
          <el-popover trigger="click" :width="220">
            <template #reference>
              <el-button size="small">
                <span style="background:#ff0;padding:0 3px">A</span>
              </el-button>
            </template>
            <div class="color-grid">
              <span v-for="c in bgColorPalette" :key="c" class="color-cell" :style="{background:c}" @click="setBgColor(c)" />
            </div>
          </el-popover>
          <!-- 格式刷 -->
          <el-tooltip content="格式刷（先选择样式，再选择目标文字）">
            <el-button size="small" :class="{'is-active': formatPainterOn}" @click="toggleFormatPainter">刷</el-button>
          </el-tooltip>
          <!-- 清除格式 -->
          <el-tooltip content="清除格式"><el-button size="small" @click="execCmd('removeFormat')">Tx</el-button></el-tooltip>
        </div>
        <div class="toolbar-sep" />
        <div class="toolbar-group">
          <el-tooltip content="左对齐"><el-button size="small" @click="execCmd('justifyLeft')">左</el-button></el-tooltip>
          <el-tooltip content="居中"><el-button size="small" @click="execCmd('justifyCenter')">中</el-button></el-tooltip>
          <el-tooltip content="右对齐"><el-button size="small" @click="execCmd('justifyRight')">右</el-button></el-tooltip>
          <el-tooltip content="两端对齐"><el-button size="small" @click="execCmd('justifyFull')">齐</el-button></el-tooltip>
        </div>
        <div class="toolbar-sep" />
        <div class="toolbar-group">
          <el-tooltip content="无序列表"><el-button size="small" @click="execCmd('insertUnorderedList')">•≡</el-button></el-tooltip>
          <el-tooltip content="有序列表"><el-button size="small" @click="execCmd('insertOrderedList')">1≡</el-button></el-tooltip>
          <el-tooltip content="减少缩进"><el-button size="small" @click="execCmd('outdent')">⇤</el-button></el-tooltip>
          <el-tooltip content="增加缩进"><el-button size="small" @click="execCmd('indent')">⇥</el-button></el-tooltip>
        </div>
        <div class="toolbar-sep" />
        <div class="toolbar-group">
          <el-tooltip content="插入表格"><el-button size="small" @click="showTablePicker=!showTablePicker">⊞</el-button></el-tooltip>
          <el-tooltip content="插入分割线"><el-button size="small" @click="execCmd('insertHorizontalRule')">线</el-button></el-tooltip>
          <el-tooltip content="插入图片"><el-button size="small" @click="insertImage">图</el-button></el-tooltip>
          <el-tooltip content="插入链接"><el-button size="small" @click="insertLink">链</el-button></el-tooltip>
        </div>
        <div class="toolbar-sep" />
        <div class="toolbar-group">
          <el-tooltip content="撤销 Ctrl+Z"><el-button size="small" @click="execCmd('undo')">↶</el-button></el-tooltip>
          <el-tooltip content="重做 Ctrl+Y"><el-button size="small" @click="execCmd('redo')">↷</el-button></el-tooltip>
        </div>
        <!-- 表格大小选择器 -->
        <div v-if="showTablePicker" class="table-picker-overlay" @click.self="showTablePicker=false">
          <div class="table-picker">
            <div class="table-picker-title">插入表格 ({{ tablePickerR }}×{{ tablePickerC }})</div>
            <div class="table-picker-grid">
              <div v-for="r in 6" :key="r" class="table-picker-row">
                <span v-for="c in 6" :key="c" class="table-picker-cell"
                  :class="{active: r<=tablePickerR && c<=tablePickerC}"
                  @mouseenter="tablePickerR=r;tablePickerC=c"
                  @click="insertTableRC(r,c)" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Word 预览/编辑区域 -->
      <div v-if="fileType === 'docx'" class="doc-outer">
        <div v-show="!editMode" ref="docxContainer" class="docx-stage" />
        <div v-show="editMode" ref="editorHost" class="editor-wrapper"
            @input="onEditorInput" @keydown="onEditorKeydown" @keyup="handleEditorSelectionChange"
            @mouseup="handleEditorSelectionChange" @click="onEditorClick">
          <div ref="editorArea" class="editor-content" contenteditable="true" />
        </div>
      </div>

      <!-- PDF 渲染区域 -->
      <div v-if="fileType === 'pdf'" class="pdf-wrapper">
        <canvas v-for="page in totalPages" :key="page" :ref="el => { if (el) pdfCanvases[page] = el }" class="pdf-page" />
      </div>
    </template>
    <input ref="imgInput" type="file" accept="image/*" style="display:none" @change="onImageSelected" />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Edit, View, Check } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref(null)
const unsupported = ref(false)
const fileName = ref('')
const fileType = ref('')
const fileBlob = ref(null)
const docId = ref(null)
const docxContainer = ref(null)
const wordScale = ref('fit')
const wordPageCount = ref(0)
const pdfCanvases = ref({})
const pdfScale = ref(1.0)
const totalPages = ref(0)
let pdfDoc = null
const A4_WIDTH_PX = 794
const A4_HEIGHT_PX = 1123
const FOOTER_SAFE_GAP_PX = 24

// 编辑相关
const editMode = ref(false)
const editorArea = ref(null)
const saving = ref(false)
const isDirty = ref(false)
const imgInput = ref(null)
const fontFamily = ref('Microsoft YaHei')
const fontSizeVal = ref('3')
const headingVal = ref('p')
const fontColor = ref('#303133')
const formatPainterOn = ref(false)
const showTablePicker = ref(false)
const tablePickerR = ref(2)
const tablePickerC = ref(3)
let painterStyle = null
let savedRange = null
let editSnapshot = ''

const states = ref({ bold: false, italic: false, underline: false, strikeThrough: false })

const colorPalette = [
  '#000000','#434343','#666666','#999999','#b7b7b7','#cccccc','#d9d9d9','#efefef','#f3f3f3','#ffffff',
  '#980000','#ff0000','#ff9900','#ffff00','#00ff00','#00ffff','#4a86e8','#0000ff','#9900ff','#ff00ff',
  '#e6b8af','#f4cccc','#fce5cd','#fff2cc','#d9ead3','#d0e0e3','#c9daf8','#cfe2f3','#d9d2e9','#ead1dc',
  '#dd7e6b','#ea9999','#f9cb9c','#ffe599','#b6d7a8','#a2c4c9','#a4c2f4','#9fc5e8','#b4a7d6','#d5a6bd',
  '#cc4125','#e06666','#f6b26b','#ffd966','#93c47d','#76a5af','#6d9eeb','#6fa8dc','#8e7cc3','#c27ba0'
]
const bgColorPalette = [
  '#ffffff','#fff2cc','#fce5cd','#f4cccc','#d9ead3','#cfe2f3','#d9d2e9','#ead1dc',
  '#ffff00','#ff9900','#ff0000','#00ff00','#00ffff','#0000ff','#9900ff','transparent'
]

onMounted(async () => {
  document.addEventListener('selectionchange', saveSelection)
  window.addEventListener('resize', applyWordScale)
  docId.value = route.query.id
  const fileUrl = route.query.url
  const nameParam = route.query.name
  if (!docId.value && !fileUrl) { loading.value = false; error.value = '缺少文档参数'; return }
  try {
    let arrayBuffer, name
    let templateHtml = ''
    const baseUrl = import.meta.env.VITE_APP_BASE_API || ''
    if (route.query.templateId || route.query.schemeId || route.query.reportId) {
      const detailUrl = route.query.templateId
        ? '/audit/prepare/template/' + route.query.templateId
        : route.query.schemeId
          ? '/audit/ops/scheme/' + route.query.schemeId
          : '/audit/ops/report/' + route.query.reportId
      const templateRes = await request({ url: detailUrl })
      const savedContent = templateRes.data?.content || ''
      if (savedContent.includes('<')) templateHtml = savedContent
    }
    if (docId.value) {
      const detailRes = await fetch(`${baseUrl}/project/doc/${docId.value}/detail`, { headers: { Authorization: 'Bearer ' + getToken() } })
      if (detailRes.ok) { const d = await detailRes.json(); name = d.data?.fileName || d.fileName || '' }
      const res = await fetch(`${baseUrl}/project/doc/${docId.value}/file`, { headers: { Authorization: 'Bearer ' + getToken() } })
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
    const ext = getFileExt(fileName.value || fileUrl || '')
    if (ext === 'docx') {
      fileType.value = 'docx'
      fileBlob.value = arrayBuffer
      loading.value = false
      await nextTick()
      await renderDocx(arrayBuffer)
      restoreTemplateHtml(templateHtml)
      if (route.query.edit === '1') await toggleEditMode()
    }
    else if (ext === 'pdf') { fileType.value = 'pdf'; fileBlob.value = arrayBuffer; loading.value = false; await nextTick(); await renderPdf(arrayBuffer) }
    else { loading.value = false; unsupported.value = true }
  } catch (e) { loading.value = false; error.value = e.message || '文档加载失败' }
})

onBeforeUnmount(() => {
  document.removeEventListener('selectionchange', saveSelection)
  window.removeEventListener('resize', applyWordScale)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

onBeforeRouteLeave(async () => {
  if (!isDirty.value) return true
  try {
    await confirmDiscardChanges()
    isDirty.value = false
    return true
  } catch {
    return false
  }
})

window.addEventListener('beforeunload', handleBeforeUnload)

async function renderDocx(buffer) {
  try {
    const { renderAsync } = await import('docx-preview')
    docxContainer.value.innerHTML = ''
    await renderAsync(buffer, docxContainer.value, null, { className: 'docx-body', inWrapper: true, ignoreWidth: false, ignoreHeight: false, ignoreFonts: false, breakPages: true, useBase64URL: true })
    if (document.fonts?.ready) await document.fonts.ready
    await waitForDocxImages()
    await nextTick()
    paginateRenderedDocx()
  } catch (e) { error.value = 'Word 文档渲染失败: ' + (e.message || '') }
}

function restoreTemplateHtml(templateHtml) {
  if (!templateHtml || !docxContainer.value) return
  const wrapper = getDocxWrapper() || docxContainer.value
  wrapper.innerHTML = templateHtml
  nextTick(() => paginateRenderedDocx())
}

async function toggleEditMode() {
  if (editMode.value) {
    if (isDirty.value) {
      try {
        await confirmDiscardChanges()
      } catch {
        return
      }
    }
    isDirty.value = false
    editMode.value = false
    return
  }

  editMode.value = true
  await nextTick()
  if (editorArea.value) {
    const wrapper = getDocxWrapper() || docxContainer.value
    if (wrapper) editorArea.value.innerHTML = wrapper.innerHTML
    editSnapshot = editorArea.value.innerHTML
    isDirty.value = false
    editorArea.value.focus()
    saveSelection()
  }
}

function confirmDiscardChanges() {
  return ElMessageBox.confirm(
    '文档有尚未保存的修改，退出后这些修改将丢失。',
    '退出编辑',
    {
      confirmButtonText: '放弃修改',
      cancelButtonText: '继续编辑',
      type: 'warning',
      distinguishCancelAndClose: true
    }
  )
}

function handleBeforeUnload(event) {
  if (!isDirty.value) return
  event.preventDefault()
  event.returnValue = ''
}

function paginateRenderedDocx() {
  const wrapper = getDocxWrapper()
  if (!wrapper) return
  normalizeDocxPages(wrapper)
  let pages = Array.from(wrapper.querySelectorAll(':scope > section'))
  if (!pages.length) {
    const section = document.createElement('section')
    section.className = 'docx-body'
    while (wrapper.firstChild) section.appendChild(wrapper.firstChild)
    wrapper.appendChild(section)
    pages = [section]
  }
  pages.forEach(ensurePageArticle)
  splitOverflowPages(wrapper)
  wordPageCount.value = wrapper.querySelectorAll(':scope > section').length
  nextTick(() => applyWordScale())
}

function normalizeDocxPages(wrapper) {
  Array.from(wrapper.querySelectorAll(':scope > section')).forEach(page => {
    page.style.width = `${A4_WIDTH_PX}px`
    page.style.minHeight = `${A4_HEIGHT_PX}px`
    page.style.height = `${A4_HEIGHT_PX}px`
    page.style.boxSizing = 'border-box'
  })
}

function splitOverflowPages(wrapper) {
  let page = wrapper.querySelector(':scope > section')
  let guard = 0
  while (page && guard < 160) {
    guard++
    if (!isPageOverflowing(page) || countMovableChildren(page) === 0) {
      page = page.nextElementSibling
      continue
    }
    const nextPage = cloneEmptyPage(page)
    page.after(nextPage)
    let moveGuard = 0
    while (isPageOverflowing(page) && countMovableChildren(page) > 0 && moveGuard < 500) {
      moveGuard++
      const movable = lastMovableChild(page)
      if (!movable) break
      const targetArticle = getPageArticles(nextPage)[movable.articleIndex]
      targetArticle.insertBefore(movable.node, targetArticle.firstChild)
      movePrecedingHeading(movable.article, targetArticle)
    }
    removeEmptyArticles(page)
    page = nextPage
  }
}

function isPageOverflowing(page) {
  if (page.scrollHeight > page.clientHeight + 2) return true
  const footer = Array.from(page.children).find(child => child.tagName === 'FOOTER')
  const articles = getPageArticles(page).filter(article => Array.from(article.childNodes).some(isMovableNode))
  if (!footer || !articles.length) return false
  const articleBottom = Math.max(...articles.map(article => article.getBoundingClientRect().bottom))
  return articleBottom > footer.getBoundingClientRect().top - FOOTER_SAFE_GAP_PX
}

function countMovableChildren(page) {
  return getPageArticles(page).reduce((count, article) => count + Array.from(article.childNodes).filter(isMovableNode).length, 0)
}

function lastMovableChild(page) {
  const articles = getPageArticles(page)
  for (let index = articles.length - 1; index >= 0; index--) {
    const node = Array.from(articles[index].childNodes).reverse().find(isMovableNode)
    if (node) return { node, article: articles[index], articleIndex: index }
  }
  return null
}

function movePrecedingHeading(sourceArticle, targetArticle) {
  const previous = Array.from(sourceArticle.childNodes).reverse().find(isMovableNode)
  if (previous?.nodeType !== Node.ELEMENT_NODE) return
  if (!String(previous.className || '').split(/\s+/).some(name => /_heading[1-6]$/.test(name))) return
  targetArticle.insertBefore(previous, targetArticle.firstChild)
}

function cloneEmptyPage(page) {
  const clone = page.cloneNode(false)
  clone.removeAttribute('id')
  clone.dataset.autoPage = 'true'
  clone.style.width = `${A4_WIDTH_PX}px`
  clone.style.minHeight = `${A4_HEIGHT_PX}px`
  clone.style.height = `${A4_HEIGHT_PX}px`
  clone.style.boxSizing = 'border-box'
  Array.from(page.children).forEach(child => {
    if (child.tagName === 'ARTICLE') clone.appendChild(child.cloneNode(false))
    else if (child.tagName === 'HEADER' || child.tagName === 'FOOTER') clone.appendChild(child.cloneNode(true))
  })
  if (!getPageArticles(clone).length) clone.appendChild(document.createElement('article'))
  return clone
}

function ensurePageArticle(page) {
  if (getPageArticles(page).length) return
  const article = document.createElement('article')
  Array.from(page.childNodes).forEach(node => {
    const tag = node.nodeType === Node.ELEMENT_NODE ? node.tagName : ''
    if (tag !== 'HEADER' && tag !== 'FOOTER') article.appendChild(node)
  })
  const footer = Array.from(page.children).find(child => child.tagName === 'FOOTER')
  page.insertBefore(article, footer || null)
}

function getPageArticles(page) {
  return Array.from(page.children).filter(child => child.tagName === 'ARTICLE')
}

function isMovableNode(node) {
  return node.nodeType === Node.ELEMENT_NODE || String(node.textContent || '').trim().length > 0
}

function removeEmptyArticles(page) {
  getPageArticles(page).forEach((article, index) => {
    if (index > 0 && !Array.from(article.childNodes).some(isMovableNode)) article.remove()
  })
}

function applyWordScale() {
  const wrapper = getDocxWrapper()
  const stage = docxContainer.value
  if (!wrapper || !stage) return
  const availableWidth = Math.max(280, stage.clientWidth - 48)
  const scale = wordScale.value === 'fit'
    ? Math.min(1, availableWidth / A4_WIDTH_PX)
    : Number(wordScale.value) || 1
  wrapper.style.zoom = String(scale)
}

function getDocxWrapper() {
  return docxContainer.value?.querySelector('.docx-body-wrapper, .docx-wrapper') || null
}

async function waitForDocxImages() {
  const images = Array.from(docxContainer.value?.querySelectorAll('img') || [])
  await Promise.all(images.filter(img => !img.complete).map(img => new Promise(resolve => {
    img.addEventListener('load', resolve, { once: true })
    img.addEventListener('error', resolve, { once: true })
  })))
}

function execCmd(cmd, val = null) {
  restoreSelection()
  document.execCommand(cmd, false, val)
  editorArea.value?.focus()
  saveSelection()
  updateStates()
  queueMicrotask(markDirty)
}
function setFontSize(size) {
  const sizeMap = { 1: '10px', 2: '12px', 3: '14px', 4: '16px', 5: '18px', 6: '24px', 7: '32px' }
  applyInlineStyle({ fontSize: sizeMap[size] || '14px' })
}
function setHeading(tag) { execCmd('formatBlock', tag === 'p' ? 'p' : tag) }
function setFontFamily(f) { applyInlineStyle({ fontFamily: f }) }
function setFontColor(c) { fontColor.value = c; applyInlineStyle({ color: c }) }
function setBgColor(c) { applyInlineStyle({ backgroundColor: c === 'transparent' ? 'transparent' : c }) }

function handleEditorSelectionChange() {
  saveSelection()
  updateStates()
}

function selectionBelongsToEditor(range) {
  return !!(editorArea.value && range && editorArea.value.contains(range.commonAncestorContainer))
}

function saveSelection() {
  if (!editMode.value || !editorArea.value) return
  const sel = window.getSelection()
  if (!sel || !sel.rangeCount) return
  const range = sel.getRangeAt(0)
  if (selectionBelongsToEditor(range)) savedRange = range.cloneRange()
}

function restoreSelection() {
  if (!savedRange || !editorArea.value) return false
  const sel = window.getSelection()
  sel.removeAllRanges()
  sel.addRange(savedRange)
  return true
}

function applyInlineStyle(styleMap) {
  restoreSelection()
  const sel = window.getSelection()
  if (!sel || !sel.rangeCount) return
  const range = sel.getRangeAt(0)
  if (!selectionBelongsToEditor(range)) return
  editorArea.value?.focus()
  const styleText = toStyleText(styleMap)
  if (range.collapsed) {
    document.execCommand('insertHTML', false, `<span style="${styleText}">\u200b</span>`)
  } else {
    const html = serializeFragment(range.cloneContents())
    document.execCommand('insertHTML', false, `<span style="${styleText}">${html}</span>`)
  }
  saveSelection()
  updateStates()
}

function toStyleText(styleMap) {
  return Object.entries(styleMap)
    .filter(([, value]) => value)
    .map(([key, value]) => `${key.replace(/[A-Z]/g, s => '-' + s.toLowerCase())}:${String(value).replace(/"/g, '&quot;')}`)
    .join(';')
}

function serializeFragment(fragment) {
  const box = document.createElement('div')
  box.appendChild(fragment)
  return box.innerHTML
}

function updateStates() {
  states.value.bold = document.queryCommandState('bold')
  states.value.italic = document.queryCommandState('italic')
  states.value.underline = document.queryCommandState('underline')
  states.value.strikeThrough = document.queryCommandState('strikeThrough')
}

function toggleFormatPainter() {
  restoreSelection()
  if (formatPainterOn.value) { formatPainterOn.value = false; painterStyle = null; return }
  const sel = window.getSelection()
  if (!sel.rangeCount || sel.isCollapsed) { ElMessage.warning('请先选中带格式的文字'); return }
  const node = sel.anchorNode?.parentElement
  if (node) {
    painterStyle = { fontWeight: getComputedStyle(node).fontWeight, fontStyle: getComputedStyle(node).fontStyle, textDecoration: getComputedStyle(node).textDecoration, color: getComputedStyle(node).color, fontSize: getComputedStyle(node).fontSize, fontFamily: getComputedStyle(node).fontFamily, backgroundColor: getComputedStyle(node).backgroundColor }
    formatPainterOn.value = true
  }
}

function onEditorClick() {
  saveSelection()
  if (formatPainterOn.value && painterStyle) {
    const sel = window.getSelection()
    if (sel.rangeCount && !sel.isCollapsed) {
      const range = sel.getRangeAt(0)
      const html = serializeFragment(range.cloneContents())
      document.execCommand('insertHTML', false, `<span style="${toStyleText(painterStyle)}">${html}</span>`)
      saveSelection()
    }
    formatPainterOn.value = false; painterStyle = null
  }
}

function insertTableRC(rows, cols) {
  let html = '<table style="border-collapse:collapse;width:100%;margin:10px 0">'
  for (let r = 0; r < rows; r++) {
    html += '<tr>'
    for (let c = 0; c < cols; c++) html += '<td style="border:1px solid #ccc;padding:8px;min-width:60px">&nbsp;</td>'
    html += '</tr>'
  }
  html += '</table><p><br></p>'
  execCmd('insertHTML', html); showTablePicker.value = false
}

function insertImage() { imgInput.value?.click() }
function onImageSelected(e) {
  const file = e.target.files?.[0]; if (!file) return
  const reader = new FileReader()
  reader.onload = () => { execCmd('insertHTML', `<img src="${reader.result}" style="max-width:100%;margin:8px 0" />`) }
  reader.readAsDataURL(file); e.target.value = ''
}

function insertLink() {
  ElMessageBox.prompt('请输入链接地址', '插入链接', { confirmButtonText: '插入', cancelButtonText: '取消', inputValue: 'https://' }).then(({ value }) => {
    if (value) execCmd('insertHTML', `<a href="${value}" target="_blank" style="color:#409eff">${value}</a>`)
  }).catch(() => {})
}

function onEditorInput() { markDirty(); saveSelection(); updateStates() }
function onEditorKeydown(e) { if (e.ctrlKey && e.key === 's') { e.preventDefault(); saveDocument() } }

function markDirty() {
  if (!editMode.value || !editorArea.value) return
  isDirty.value = editorArea.value.innerHTML !== editSnapshot
}

async function saveDocument() {
  if (!editorArea.value) return; saving.value = true
  try {
    const html = serializeEditorHtml()
    if (route.query.templateId || route.query.schemeId || route.query.reportId) {
      const saveUrl = route.query.templateId
        ? '/audit/prepare/template/' + route.query.templateId + '/content'
        : route.query.schemeId
          ? '/audit/ops/scheme/' + route.query.schemeId + '/content'
          : '/audit/ops/report/' + route.query.reportId + '/content'
      await request({
        url: saveUrl,
        method: 'put',
        data: { content: html }
      })
      ElMessage.success('文档已保存')
      editSnapshot = html
      isDirty.value = false
      return
    }
    if (docId.value) {
      await request({
        url: `/project/doc/${docId.value}/docx`,
        method: 'put',
        data: { htmlContent: html }
      })
      editSnapshot = html
      isDirty.value = false
      await reloadSavedDocx()
      ElMessage.success('文档已保存到原 DOCX 文件')
    } else {
      ElMessage.error('该文档未关联可写入的业务记录，无法保存')
    }
  } catch (e) {
    ElMessage.error(e?.msg || e?.message || '文档保存失败')
  }
  finally { saving.value = false }
}

function serializeEditorHtml() {
  const clone = editorArea.value.cloneNode(true)
  const sourceNodes = [editorArea.value, ...editorArea.value.querySelectorAll('*')]
  const cloneNodes = [clone, ...clone.querySelectorAll('*')]
  const properties = [
    'font-family', 'font-size', 'font-weight', 'font-style', 'text-decoration',
    'color', 'background-color', 'text-align', 'line-height',
    'margin-top', 'margin-right', 'margin-bottom', 'margin-left',
    'padding-top', 'padding-right', 'padding-bottom', 'padding-left',
    'border-top', 'border-right', 'border-bottom', 'border-left',
    'width', 'height', 'vertical-align'
  ]
  sourceNodes.forEach((node, index) => {
    const target = cloneNodes[index]
    if (!(node instanceof Element) || !(target instanceof Element)) return
    const computed = window.getComputedStyle(node)
    properties.forEach(property => target.style.setProperty(property, computed.getPropertyValue(property)))
  })
  return clone.innerHTML
}

async function reloadSavedDocx() {
  const baseUrl = import.meta.env.VITE_APP_BASE_API || ''
  const res = await fetch(`${baseUrl}/project/doc/${docId.value}/file?t=${Date.now()}`, {
    headers: { Authorization: 'Bearer ' + getToken() }
  })
  if (!res.ok) throw new Error('文档已保存，但重新加载失败')
  const buffer = await res.arrayBuffer()
  fileBlob.value = buffer
  await renderDocx(buffer)
}

function goBack() {
  const returnPath = route.query.returnPath
  if (returnPath) {
    router.push(String(returnPath))
    return
  }
  router.back()
}

async function renderPdf(buffer) {
  try {
    const pdfjsLib = await import('pdfjs-dist'); pdfjsLib.GlobalWorkerOptions.workerSrc = '/pdf.worker.mjs'
    pdfDoc = await pdfjsLib.getDocument({ data: buffer }).promise; totalPages.value = pdfDoc.numPages
    await nextTick(); await renderAllPages()
  } catch (e) { error.value = 'PDF 渲染失败: ' + (e.message || '') }
}

async function renderAllPages() {
  if (!pdfDoc) return; await nextTick()
  for (let i = 1; i <= totalPages.value; i++) {
    const page = await pdfDoc.getPage(i); const viewport = page.getViewport({ scale: pdfScale.value })
    const canvas = pdfCanvases.value[i]; if (!canvas) continue
    const ctx = canvas.getContext('2d'); canvas.width = viewport.width; canvas.height = viewport.height
    await page.render({ canvasContext: ctx, viewport }).promise
  }
}

function handleDownload() {
  if (fileBlob.value) {
    const blob = new Blob([fileBlob.value]); const link = document.createElement('a')
    link.href = URL.createObjectURL(blob); link.download = fileName.value
    document.body.appendChild(link); link.click(); document.body.removeChild(link); URL.revokeObjectURL(link.href)
  } else ElMessage.warning('文件未加载完成')
}

function getFileExt(name) {
  const cleanName = decodeURIComponent(String(name).split('?')[0] || '')
  return cleanName.includes('.') ? cleanName.split('.').pop().toLowerCase() : ''
}
</script>

<style scoped>
:global(.app-container.doc-preview-page) {
  padding: 4px 8px;
}

:global(.doc-preview-page .mb8) {
  margin-bottom: 4px;
}

.word-scale-select { width: 112px; }
.page-count {
  color: #606266;
  font-size: 13px;
  white-space: nowrap;
}
.preview-file-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
  margin-left: auto;
  text-align: right;
}
.preview-file-name {
  min-width: 0;
  overflow: hidden;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 工具栏 */
.editor-toolbar {
  display: flex; align-items: center; flex-wrap: wrap; gap: 2px;
  padding: 4px 8px; background: #fff; border: 1px solid #e4e7ed; border-bottom: none;
  border-radius: 4px 4px 0 0; position: relative;
}
.toolbar-group { display: flex; align-items: center; gap: 2px; }
.toolbar-sep { width: 1px; height: 24px; background: #dcdfe6; margin: 0 6px; }
.toolbar-group .el-button { min-width: 30px; padding: 4px 7px; font-size: 13px; }
.toolbar-group .el-button.is-active { background: #ecf5ff; border-color: #409eff; color: #409eff; }
.font-color-button {
  display: inline-block;
  min-width: 14px;
  font-weight: 700;
  border-bottom: 3px solid currentColor;
  line-height: 1;
}
.color-grid { display: grid; grid-template-columns: repeat(10, 1fr); gap: 2px; }
.color-cell { width: 20px; height: 20px; cursor: pointer; border: 1px solid #eee; border-radius: 2px; }
.color-cell:hover { border-color: #409eff; transform: scale(1.2); }

/* 表格选择器 */
.table-picker-overlay { position: absolute; top: 100%; left: 0; right: 0; z-index: 100; }
.table-picker { display: inline-block; background: #fff; border: 1px solid #e4e7ed; border-radius: 4px; padding: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); margin-top: 4px; margin-left: 400px; }
.table-picker-title { font-size: 12px; color: #909399; margin-bottom: 6px; text-align: center; }
.table-picker-row { display: flex; gap: 2px; margin-bottom: 2px; }
.table-picker-cell { width: 20px; height: 20px; border: 1px solid #ddd; cursor: pointer; }
.table-picker-cell.active { background: #409eff; border-color: #409eff; }

/* 文档外层 - 无额外边距 */
.doc-outer {
  border: 1px solid #d7dde7;
  border-radius: 0 0 6px 6px;
  overflow: hidden;
  background: #d9dde4;
}

/* Word 预览 - 最大化显示空间 */
.docx-stage {
  min-height: calc(100vh - 124px);
  max-height: calc(100vh - 124px);
  overflow: auto;
  padding: 36px 24px 48px;
  background: #d6d9de;
  scrollbar-gutter: stable both-edges;
}
.docx-stage :deep(.docx-body-wrapper) {
  width: fit-content;
  min-width: 794px;
  transform-origin: top center;
  margin: 0 auto;
  padding: 0 !important;
  background: transparent !important;
}
.docx-stage :deep(.docx-body-wrapper > section) {
  position: relative;
  width: 210mm !important;
  min-height: 297mm !important;
  height: 297mm !important;
  box-sizing: border-box;
  margin: 0 auto 28px !important;
  background: #fff !important;
  border: 1px solid rgba(148, 163, 184, 0.45);
  border-radius: 1px;
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.12),
    0 12px 28px rgba(15, 23, 42, 0.18);
  overflow: hidden;
}
.docx-stage :deep(.docx-body-wrapper > section:last-child) {
  margin-bottom: 0 !important;
}
.docx-stage :deep(.docx-body-wrapper > section::after) {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.65);
}
.docx-stage :deep(table) { max-width: 100%; }
.docx-stage :deep(.docx-body-wrapper > section > article) {
  min-height: 0;
}
.docx-stage :deep(.docx-body-wrapper > section > article > img) {
  max-width: 100%;
  height: auto;
}

/* Word 编辑 - 最大化编辑空间 */
.editor-wrapper {
  background: #d9dde4;
  min-height: calc(100vh - 166px);
  max-height: calc(100vh - 166px);
  overflow: auto;
  padding: 36px 24px 48px;
  scrollbar-gutter: stable both-edges;
}
.editor-content {
  width: fit-content;
  min-width: 794px;
  margin: 0 auto;
  outline: none;
  font-family: 'Microsoft YaHei', SimSun, serif;
  font-size: 14px;
  color: #303133;
}
.editor-content :deep(> section) {
  position: relative;
  width: 210mm !important;
  min-height: 297mm !important;
  height: 297mm !important;
  box-sizing: border-box;
  margin: 0 auto 28px !important;
  overflow: hidden;
  background: #fff !important;
  border: 1px solid rgba(148, 163, 184, 0.45);
  border-radius: 1px;
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.12),
    0 12px 28px rgba(15, 23, 42, 0.18);
}
.editor-content :deep(> section:last-child) { margin-bottom: 0 !important; }
.editor-content:focus :deep(> section) {
  box-shadow:
    0 0 0 2px rgba(64, 158, 255, 0.18),
    0 12px 28px rgba(15, 23, 42, 0.18);
}
.editor-content :deep(table) { border-collapse: collapse; width: 100%; margin: 8px 0; }
.editor-content :deep(td), .editor-content :deep(th) { border: 1px solid #ccc; padding: 6px 10px; min-width: 50px; }
.editor-content :deep(img) { max-width: 100%; }

@media (max-width: 768px) {
  :global(.app-container.doc-preview-page) { padding: 0; }
  :global(.doc-preview-page .mb8) {
    row-gap: 6px;
    padding: 6px 8px;
  }
  .preview-toolbar > :deep(.el-col) {
    flex: 0 0 auto;
    width: auto;
    max-width: none;
  }
  .preview-file-meta {
    order: -1;
    flex: 1 0 100% !important;
    justify-content: flex-start;
    width: 100% !important;
    padding: 0 4px 4px !important;
    text-align: left;
  }
  .preview-file-name { font-size: 14px; }
  .doc-outer { border-left: 0; border-right: 0; border-radius: 0; }
  .docx-stage {
    min-height: calc(100vh - 154px);
    max-height: calc(100vh - 154px);
    padding: 20px 12px 32px;
  }
  .docx-stage :deep(.docx-body-wrapper > section) { margin-bottom: 18px !important; }
  .editor-wrapper { padding: 20px 12px 32px; }
}

/* PDF */
.pdf-wrapper {
  background: #525659; min-height: 500px; max-height: calc(100vh - 180px);
  overflow-y: auto; padding: 16px 0; text-align: center; border-radius: 4px;
}
.pdf-page { display: block; margin: 0 auto 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.3); background: #fff; }
</style>
