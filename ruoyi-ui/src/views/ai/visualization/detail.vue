<template>
  <div class="app-container dashboard-page">
    <!-- 加载 -->
    <el-card v-if="loading" class="loading-card">
      <el-skeleton :rows="6" animated />
    </el-card>

    <!-- 无数据 -->
    <el-card v-else-if="!analysisId" class="loading-card">
      <el-empty description="未找到分析结果。请从数据可视化列表页选择一条记录查看。" />
    </el-card>

    <template v-else>
      <!-- 顶部信息 -->
      <el-card class="header-card">
        <div class="header-row">
          <div>
            <h2 style="margin:0">{{ title || '数据分析驾驶舱' }}</h2>
            <p class="analysis-meta">
              <span v-if="meta.createTime">时间：{{ meta.createTime }}</span>
              <span v-if="meta.projectName" style="margin-left:12px">项目：{{ meta.projectName }}</span>
              <span v-if="meta.keyword" style="margin-left:12px">主题：{{ meta.keyword }}</span>
              <el-tag v-if="meta.sourceType === 'chat'" type="success" size="small" style="margin-left:8px">聊天生成</el-tag>
              <el-tag v-else-if="meta.sourceType === 'upload'" type="warning" size="small" style="margin-left:8px">页面上传</el-tag>
              <el-tag v-else-if="meta.sourceType === 'project'" type="primary" size="small" style="margin-left:8px">项目分析</el-tag>
            </p>
          </div>
          <div>
            <el-button @click="openHtmlNewTab">新窗口打开</el-button>
            <el-button @click="$router.back()">返回列表</el-button>
            <el-button type="primary" @click="refreshData">刷新</el-button>
          </div>
        </div>
      </el-card>

      <!-- AI 生成的 HTML 驾驶舱 -->
      <el-card class="dashboard-iframe-card">
        <iframe ref="dashboardIframe" :src="htmlUrl" class="dashboard-iframe" frameborder="0" allowfullscreen></iframe>
      </el-card>

      <!-- AI 分析总结 -->
      <el-card v-if="summary" class="summary-card">
        <template #header>
          <div class="summary-header">
            <span class="summary-title-icon">&#x2728;</span>
            <span>AI 审计分析总结</span>
          </div>
        </template>
        <div class="markdown-body" v-html="renderMarkdown(summary)"></div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAnalysisResult } from '@/api/ai/dataAnalyze'

const route = useRoute()
const analysisId = ref(null)
const title = ref('')
const summary = ref('')
const loading = ref(false)
const meta = ref({ createTime: '', projectName: '', keyword: '', sourceType: '' })

const baseUrl = import.meta.env.VITE_APP_BASE_API || ''

const htmlUrl = computed(() => {
  if (!analysisId.value) return ''
  return `${baseUrl}/ai/data/analysis/${analysisId.value}/html`
})

function openHtmlNewTab() {
  if (htmlUrl.value) window.open(htmlUrl.value, '_blank')
}

function renderMarkdown(text) {
  if (!text) return ''
  let html = String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // headers
  html = html.replace(/^#### (.+)$/gm, '<h4>$1</h4>')
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2 class="md-h2">$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')

  // code blocks
  html = html.replace(/```[\w]*\n?([\s\S]*?)```/g, '<pre class="md-code-block"><code>$1</code></pre>')
  html = html.replace(/`([^`\n]+)`/g, '<code class="md-inline-code">$1</code>')

  // bold & italic
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)/g, '<em>$1</em>')

  // ordered list
  html = html.replace(/^\d+\.\s+(.+)$/gm, '<li class="md-ol">$1</li>')

  // unordered list
  html = html.replace(/^[-*]\s+(.+)$/gm, '<li class="md-ul">$1</li>')

  // wrap consecutive <li> tags
  html = html.replace(/((?:<li class="md-ol">[\s\S]*?<\/li>\s*)+)/g, '<ol>$1</ol>')
  html = html.replace(/((?:<li class="md-ul">[\s\S]*?<\/li>\s*)+)/g, '<ul>$1</ul>')

  // hr
  html = html.replace(/^---+$/gm, '<hr class="md-hr">')

  // line breaks (skip those inside tags)
  html = html.replace(/\n/g, '<br>')

  // clean up double <br> after block elements
  html = html.replace(/(<\/h[1-4]>)<br>/g, '$1')
  html = html.replace(/(<\/li>)<br>/g, '$1')
  html = html.replace(/(<\/[uo]l>)<br>/g, '$1')
  html = html.replace(/(<hr[^>]*>)<br>/g, '$1')

  return html
}

async function loadData(id) {
  loading.value = true
  try {
    const res = await getAnalysisResult(id)
    if (res.code === 200 && res.data) {
      title.value = res.data.title || ''
      summary.value = res.data.summary || ''
      meta.value = {
        createTime: res.data.createTime || '',
        projectName: res.data.projectName || '',
        keyword: res.data.keyword || '',
        sourceType: res.data.sourceType || ''
      }
    } else {
      ElMessage.warning('未找到分析结果')
    }
  } catch {
    ElMessage.error('加载分析数据失败')
  } finally {
    loading.value = false
  }
}

function refreshData() {
  const iframe = document.querySelector('.dashboard-iframe')
  if (iframe) iframe.src = iframe.src
  if (analysisId.value) loadData(analysisId.value)
}

onMounted(() => {
  const id = route.query.id
  if (id) {
    analysisId.value = id
    loadData(id)
  }
})
</script>

<style scoped>
.dashboard-page { min-height: calc(100vh - 84px); background: #f5f7fa; }
.header-card { margin-bottom: 12px; }
.header-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; }
.analysis-meta { margin: 8px 0 0; color: #606266; font-size: 13px; display:flex; flex-wrap:wrap; align-items:center; gap:4px; }
.loading-card { margin-bottom: 12px; }

.dashboard-iframe-card { border-radius: 12px; margin-bottom: 12px; }
.dashboard-iframe-card :deep(.el-card__body) { padding: 0; }
.dashboard-iframe {
  width: 100%;
  height: calc(100vh - 240px);
  min-height: 500px;
  border: none;
  border-radius: 0 0 12px 12px;
}

/* AI 分析总结卡片 */
.summary-card { border-radius: 12px; margin-bottom: 16px; }
.summary-card :deep(.el-card__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 12px 12px 0 0;
  padding: 14px 20px;
}
.summary-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}
.summary-title-icon { font-size: 18px; }

/* Markdown 渲染样式 */
.markdown-body {
  padding: 8px 6px;
  font-size: 14px;
  line-height: 1.85;
  color: #303133;
  word-break: break-word;
}
.markdown-body :deep(.md-h2) {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 20px 0 10px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e8ecf1;
}
.markdown-body :deep(h3) {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 16px 0 8px;
}
.markdown-body :deep(h4) {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin: 12px 0 6px;
}
.markdown-body :deep(strong) { font-weight: 600; color: #1a1a2e; }
.markdown-body :deep(em) { font-style: italic; color: #606266; }
.markdown-body :deep(ul), .markdown-body :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}
.markdown-body :deep(li) { margin: 4px 0; }
.markdown-body :deep(.md-hr) { border: none; border-top: 1px solid #e8ecf1; margin: 16px 0; }
.markdown-body :deep(.md-code-block) {
  background: #1e1e2e;
  color: #cdd6f4;
  padding: 14px 18px;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 13px;
  margin: 10px 0;
  font-family: 'Fira Code', 'JetBrains Mono', monospace;
  line-height: 1.6;
}
.markdown-body :deep(.md-inline-code) {
  background: #f0f2f5;
  color: #e83e8c;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'Fira Code', monospace;
}
</style>
