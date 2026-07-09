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
            <p class="analysis-title">
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

async function loadData(id) {
  loading.value = true
  try {
    const res = await getAnalysisResult(id)
    if (res.code === 200 && res.data) {
      title.value = res.data.title || ''
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
.analysis-title { margin: 8px 0 0; color: #606266; font-size: 13px; display:flex; flex-wrap:wrap; align-items:center; gap:4px; }
.loading-card { margin-bottom: 12px; }
.dashboard-iframe-card { border-radius: 12px; }
.dashboard-iframe-card :deep(.el-card__body) { padding: 0; }
.dashboard-iframe {
  width: 100%;
  height: calc(100vh - 200px);
  min-height: 500px;
  border: none;
  border-radius: 0 0 12px 12px;
}
</style>
