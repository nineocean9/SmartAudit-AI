<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="18">
        <el-card shadow="hover">
          <template #header><span style="font-weight:bold">📊 项目进度甘特图</span></template>
          <div ref="ganttChart" style="height:500px"></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="mb20">
          <template #header><span style="font-weight:bold">📈 进度概览</span></template>
          <el-statistic title="总项目数" :value="stats.total"/>
          <el-statistic title="实施中" :value="stats.active" class="mt10" style="color:#409EFF"/>
          <el-statistic title="已归档" :value="stats.archived" class="mt10" style="color:#67C23A"/>
          <el-statistic title="超期预警" :value="stats.overdue" class="mt10" style="color:#F56C6C"/>
        </el-card>
        <el-card shadow="hover">
          <template #header><span style="font-weight:bold">👥 人员负载</span></template>
          <div v-for="w in workload" :key="w.user_name" class="mb10">
            <span>{{ w.user_name }} ({{ w.role_type }})</span>
            <el-progress :percentage="Math.min(w.project_count * 25, 100)" :color="w.project_count > 3 ? '#F56C6C' : '#409EFF'" :format="() => w.project_count + '个项目'"/>
          </div>
          <el-empty v-if="!workload.length" description="暂无数据" :image-size="60"/>
        </el-card>
      </el-col>
    </el-row>

    <!-- 超期预警列表 -->
    <el-card shadow="hover" class="mt20" v-if="overdueList.length">
      <template #header><span style="font-weight:bold;color:#F56C6C">⚠️ 超期预警项目</span></template>
      <el-table :data="overdueList" size="small">
        <el-table-column label="项目名称" prop="project_name"/>
        <el-table-column label="被审单位" prop="audited_unit"/>
        <el-table-column label="审计类型" prop="audit_type" width="120"/>
        <el-table-column label="问题数" prop="issue_count" width="80"/>
      </el-table>
    </el-card>

    <!-- 项目列表 -->
    <el-card shadow="hover" class="mt20">
      <template #header><span style="font-weight:bold">📋 项目列表</span></template>
      <el-table :data="projectList" @row-click="gotoProject">
        <el-table-column label="项目名称" prop="project_name" width="200"/>
        <el-table-column label="被审单位" prop="audited_unit" width="160"/>
        <el-table-column label="审计类型" prop="audit_type" width="120"/>
        <el-table-column label="年度" prop="audit_year" width="80"/>
        <el-table-column label="阶段" prop="phase" width="80">
          <template #default="scope"><el-tag size="small">{{ scope.row.phase || '准备' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="进度" width="160">
          <template #default="scope"><el-progress :percentage="scope.row.progress || 0" :color="scope.row.is_overdue ? '#F56C6C' : '#409EFF'"/></template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="scope"><el-tag :type="['info','','success'][scope.row.status]">{{ ['未启动','实施中','已归档'][scope.row.status] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="问题/整改" width="100">
          <template #default="scope">{{ scope.row.issue_count || 0 }} / {{ scope.row.rect_count || 0 }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const ganttChart = ref(null)
const projectList = ref([])
const workload = ref([])
const overdueList = ref([])
const stats = ref({ total: 0, active: 0, archived: 0, overdue: 0 })

function loadData() {
  request({ url: '/audit/info/progress' }).then(res => {
    projectList.value = res.data || []
    stats.value.total = projectList.value.length
    stats.value.active = projectList.value.filter(p => p.status == 1).length
    stats.value.archived = projectList.value.filter(p => p.status == 2).length
    stats.value.overdue = projectList.value.filter(p => p.is_overdue == 1).length
    overdueList.value = projectList.value.filter(p => p.is_overdue == 1)
    nextTick(() => renderGantt())
  })
  request({ url: '/audit/prepare/workload' }).then(res => { workload.value = res.data || [] }).catch(() => {})
}

function renderGantt() {
  if (!ganttChart.value || !projectList.value.length) return
  const echarts = window.echarts || (typeof require !== 'undefined' ? require('echarts') : null)
  if (!echarts) return

  const chart = echarts.init(ganttChart.value)
  const names = projectList.value.map(p => p.project_name)
  const statusColors = { 0: '#909399', 1: '#409EFF', 2: '#67C23A' }

  const data = projectList.value.map((p, i) => {
    const year = p.audit_year || 2026
    const start = p.start_date || year + '-01-01'
    const end = p.end_date || year + '-12-31'
    return {
      name: p.project_name,
      value: [i, new Date(start).getTime(), new Date(end).getTime(), p.status],
      itemStyle: { color: p.is_overdue ? '#F56C6C' : statusColors[p.status] || '#409EFF' }
    }
  })

  chart.setOption({
    tooltip: { formatter: p => p.name + '<br/>' + (projectList.value[p.value[0]]?.audited_unit || '') },
    grid: { left: '20%', right: '5%', top: '5%', bottom: '10%' },
    xAxis: { type: 'time', min: '2025-01-01', max: '2027-01-01' },
    yAxis: { type: 'category', data: names, inverse: true },
    series: [{
      type: 'custom',
      renderItem: (params, api) => {
        const catIdx = api.value(0)
        const start = api.coord([api.value(1), catIdx])
        const end = api.coord([api.value(2), catIdx])
        const height = api.size([0, 1])[1] * 0.6
        return { type: 'rect', shape: { x: start[0], y: start[1] - height / 2, width: end[0] - start[0], height: height }, style: api.style() }
      },
      encode: { x: [1, 2], y: 0 },
      data: data
    }]
  })
  window.addEventListener('resize', () => chart.resize())
}

function gotoProject(row) {
  // 穿透跳转到项目详情
  router.push({ path: '/audit/project', query: { projectId: row.id } }).catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.mt10 { margin-top: 10px; }
.mt20 { margin-top: 20px; }
.mb10 { margin-bottom: 10px; }
.mb20 { margin-bottom: 20px; }
</style>
