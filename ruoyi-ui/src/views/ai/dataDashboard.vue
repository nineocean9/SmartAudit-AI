<template>
  <div class="app-container dashboard-page">
    <!-- 顶部信息区 -->
    <el-card class="header-card">
      <div class="header-row">
        <div>
          <h2 style="margin:0">{{ title || '数据分析驾驶舱' }}</h2>
          <p class="analysis-title" v-if="analysisMeta.createTime || analysisMeta.createBy || analysisMeta.sourceDescription">
            <span v-if="analysisMeta.createTime">生成时间：{{ analysisMeta.createTime }}</span>
            <span v-if="analysisMeta.createBy" style="margin-left:12px">创建人：{{ analysisMeta.createBy }}</span>
            <span v-if="analysisMeta.sourceDescription" style="margin-left:12px">数据来源：{{ analysisMeta.sourceDescription }}</span>
          </p>
        </div>
        <el-button type="primary" :disabled="!analysisId" @click="refreshData">刷新</el-button>
      </div>
    </el-card>

    <!-- 加载中 -->
    <el-card v-if="loading" class="loading-card">
      <el-skeleton :rows="6" animated />
    </el-card>

    <!-- 无数据 -->
    <el-card v-else-if="!analysisId || charts.length === 0" class="loading-card">
      <el-empty description="暂无分析数据。请在 AI 聊天中发起数据分析后再查看驾驶舱。" />
    </el-card>

    <!-- KPI 卡片 -->
    <el-row v-else :gutter="12" class="kpi-row">
      <el-col :span="6" v-for="(item, idx) in kpis" :key="idx">
        <el-card class="kpi-card">
          <div class="kpi-label">{{ item.label }}</div>
          <div class="kpi-value">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表驾驶舱 -->
    <template v-if="!loading && analysisId && charts.length > 0">
      <el-row :gutter="12" class="chart-grid">
        <el-col :span="12" v-for="(chart, idx) in normalizedCharts" :key="idx" style="margin-bottom:12px">
          <el-card class="chart-card">
            <template #header>{{ chart.title || `图表 ${idx + 1}` }}</template>
            <div :ref="el => chartRefs[idx] = el" class="chart-box"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 分析总结 -->
      <el-card v-if="summary" class="summary-card">
        <template #header>分析总结</template>
        <div class="markdown-body" v-html="renderMarkdown(summary)"></div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getAnalysisResult } from '@/api/ai/dataAnalyze'

const route = useRoute()
const analysisId = ref(null)
const title = ref('')
const charts = ref([])
const summary = ref('')
const loading = ref(false)
const chartRefs = ref({})
const analysisMeta = ref({ createTime: '', createBy: '', sourceDescription: '' })
let chartInstances = []

const kpis = computed(() => {
  const first = charts.value?.[0]
  const dataset = first?.datasets?.[0]?.data || []
  const total = dataset.reduce((a, b) => a + Number(b || 0), 0)
  return [
    { label: '图表数量', value: charts.value.length },
    { label: '数据点数量', value: dataset.length },
    { label: '首图总值', value: total.toFixed ? total.toFixed(2) : total },
    { label: '分析状态', value: summary.value ? '完成' : '待生成' }
  ]
})

const normalizedCharts = computed(() => {
  const base = charts.value || []
  const list = [...base]
  while (list.length < 4) {
    list.push({ title: '预留图表位', type: 'bar', labels: [], datasets: [] })
  }
  return list.slice(0, 4)
})

function renderMarkdown(text) {
  if (!text) return ''
  let html = String(text)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/^## (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^[-*] (.+)$/gm, '<li>$1</li>')
  html = html.replace(/^\d+\. (.+)$/gm, '<li>$1</li>')
  html = html.replace(/\n/g, '<br>')
  return html
}

function renderCharts() {
  chartInstances.forEach(inst => inst.dispose())
  chartInstances = []

  nextTick(() => {
    normalizedCharts.value.forEach((chart, idx) => {
      const el = chartRefs.value[idx]
      if (!el) return
      const instance = echarts.init(el)
      chartInstances.push(instance)

      let option = {
        tooltip: { trigger: 'axis' },
        grid: { left: 50, right: 20, bottom: 40, top: 20 },
        xAxis: { type: 'category', data: chart.labels || [] },
        yAxis: { type: 'value' },
        series: (chart.datasets || []).map(ds => ({
          name: ds.name,
          type: chart.type || 'bar',
          data: ds.data || [],
          smooth: chart.type === 'line'
        })),
        legend: { show: (chart.datasets || []).length > 1 }
      }

      if (chart.type === 'pie') {
        const pieData = (chart.labels || []).map((label, i) => ({
          name: label,
          value: (chart.datasets?.[0]?.data || [])[i] || 0
        }))
        option = {
          tooltip: { trigger: 'item' },
          legend: { bottom: 0 },
          series: [{
            type: 'pie',
            radius: ['35%', '65%'],
            data: pieData,
            label: { formatter: '{b}: {d}%' }
          }]
        }
      }

      if ((chart.datasets || []).length === 0) {
        option = {
          title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { color: '#c0c4cc', fontSize: 14 } },
          xAxis: { show: false }, yAxis: { show: false }, series: []
        }
      }

      instance.setOption(option)
    })
  })
}

async function loadData(id) {
  loading.value = true
  try {
    const res = await getAnalysisResult(id)
    if (res.code === 200 && res.data) {
      title.value = res.data.title || ''
      charts.value = res.data.charts || []
      summary.value = res.data.summary || ''
      analysisMeta.value = {
        createTime: res.data.createTime || '',
        createBy: res.data.createBy || '',
        sourceDescription: res.data.sourceDescription || '项目资料/分析结果'
      }
      nextTick(() => renderCharts())
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
  if (analysisId.value) loadData(analysisId.value)
}

watch(() => route.query.id, (newId) => {
  if (newId) {
    analysisId.value = newId
    loadData(newId)
  }
}, { immediate: true })

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
.header-row { display: flex; justify-content: space-between; align-items: center; }
.analysis-title { margin: 8px 0 0; color: #606266; font-size: 13px; }
.loading-card { margin-bottom: 12px; }
.kpi-row { margin-bottom: 4px; }
.kpi-card { text-align: center; border-radius: 12px; }
.kpi-label { font-size: 12px; color: #909399; margin-bottom: 6px; }
.kpi-value { font-size: 22px; font-weight: 700; color: #303133; }
.chart-grid { margin-top: 8px; }
.chart-card { border-radius: 12px; }
.chart-box { width: 100%; height: calc(22vh + 120px); min-height: 260px; }
.summary-card { margin-top: 4px; border-radius: 12px; }
.summary-card :deep(.markdown-body) { line-height: 1.75; font-size: 14px; }
.summary-card :deep(h3) { margin: 8px 0; color: #303133; }
.summary-card :deep(li) { margin: 4px 0; }
</style>
