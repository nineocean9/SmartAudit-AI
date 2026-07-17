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
        <el-card v-if="canViewWorkload" shadow="hover">
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
        <el-table-column label="被审计单位" prop="audited_unit"/>
        <el-table-column label="审计类型" prop="audit_type" width="120"/>
        <el-table-column label="计划周期" width="190">
          <template #default="scope">{{ formatDateRange(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="阶段" prop="phase" width="100"/>
        <el-table-column label="进度" width="110">
          <template #default="scope">{{ scope.row.progress || 0 }}%</template>
        </el-table-column>
        <el-table-column label="问题数" prop="issue_count" width="80"/>
      </el-table>
    </el-card>

    <!-- 项目列表 -->
    <el-card shadow="hover" class="mt20">
      <template #header><span style="font-weight:bold">📋 项目列表</span></template>
      <el-table :data="projectList" @row-click="gotoProject">
        <el-table-column label="项目名称" prop="project_name" width="200"/>
        <el-table-column label="被审计单位" prop="audited_unit" width="160"/>
        <el-table-column label="审计类型" prop="audit_type" width="120"/>
        <el-table-column label="年度" prop="audit_year" width="80"/>
        <el-table-column label="计划周期" width="190">
          <template #default="scope">{{ formatDateRange(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="阶段" prop="phase" width="80">
          <template #default="scope"><el-tag size="small">{{ scope.row.phase || '准备' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="进度" width="160">
          <template #default="scope">
            <el-tooltip placement="top" :content="formatProgressDetail(scope.row)">
              <el-progress :percentage="scope.row.progress || 0" :color="scope.row.is_overdue ? '#F56C6C' : '#409EFF'"/>
            </el-tooltip>
          </template>
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
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import request from '@/utils/request'
import { checkPermi } from '@/utils/permission'
import { ElMessage } from 'element-plus'

const router = useRouter()
const ganttChart = ref(null)
let chartInstance = null
let resizeHandler = null
const projectList = ref([])
const workload = ref([])
const overdueList = ref([])
const stats = ref({ total: 0, active: 0, archived: 0, overdue: 0 })
const canViewWorkload = checkPermi(['audit:prepare:view'])
const canOpenProjectWorkspace = checkPermi(['audit:project:view'])

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
  if (canViewWorkload) {
    request({ url: '/audit/prepare/workload' }).then(res => { workload.value = res.data || [] }).catch(() => {})
  }
}

function renderGantt() {
  if (!ganttChart.value || !projectList.value.length) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(ganttChart.value)
  const rows = [...projectList.value].sort((a, b) => {
    return getProjectStart(a).getTime() - getProjectStart(b).getTime()
  })
  const names = rows.map(p => p.project_name)
  const statusColors = { 0: '#9CA3AF', 1: '#3B82F6', 2: '#22C55E' }
  const plannedColor = '#E5E7EB'

  const data = rows.map((p, i) => {
    const start = getProjectStart(p)
    const end = getProjectEnd(p)
    const progress = Math.max(0, Math.min(Number(p.progress || 0), 100))
    return {
      name: p.project_name,
      value: [
        i,
        start.getTime(),
        end.getTime(),
        progress,
        Number(p.status || 0),
        Number(p.is_overdue || 0)
      ],
      itemStyle: { color: p.is_overdue ? '#EF4444' : statusColors[p.status] || '#3B82F6' }
    }
  })
  const minTime = Math.min(...data.map(item => item.value[1]))
  const maxTime = Math.max(...data.map(item => item.value[2]))
  const axisMin = addMonths(new Date(minTime), -2).getTime()
  const axisMax = addMonths(new Date(maxTime), 2).getTime()
  const today = new Date()

  chartInstance.setOption({
    legend: {
      top: 8,
      right: 18,
      itemWidth: 12,
      itemHeight: 8,
      data: ['未启动', '实施中', '已归档', '超期']
    },
    tooltip: {
      formatter: p => {
        const row = rows[p.value[0]]
        if (!row) return p.name
        return [
          row.project_name,
          `被审计单位：${row.audited_unit || '-'}`,
          `计划周期：${formatDateRange(row)}`,
          `阶段进度：${row.phase || '准备'} / ${row.progress || 0}%`,
          formatProgressDetail(row)
        ].join('<br/>')
      }
    },
    grid: { left: 230, right: 48, top: 64, bottom: 66 },
    xAxis: {
      type: 'time',
      min: axisMin,
      max: axisMax,
      axisLabel: { formatter: '{yyyy}-{MM}' },
      nameGap: 28,
      splitLine: { lineStyle: { color: '#EEF2F7' } }
    },
    yAxis: {
      type: 'category',
      data: names,
      inverse: true,
      axisTick: { show: false },
      axisLabel: { width: 200, overflow: 'truncate' }
    },
    series: [{
      name: '项目进度',
      type: 'custom',
      renderItem: (params, api) => {
        const catIdx = api.value(0)
        const startCoord = api.coord([api.value(1), catIdx])
        const endCoord = api.coord([api.value(2), catIdx])
        const progressEndCoord = api.coord([
          api.value(1) + (api.value(2) - api.value(1)) * api.value(3) / 100,
          catIdx
        ])
        const height = api.size([0, 1])[1] * 0.6
        const radius = Math.min(5, height / 2)
        const fullWidth = Math.max(endCoord[0] - startCoord[0], 2)
        const progressWidth = Math.max(progressEndCoord[0] - startCoord[0], api.value(3) > 0 ? 2 : 0)
        return {
          type: 'group',
          children: [
            {
              type: 'rect',
              shape: { x: startCoord[0], y: startCoord[1] - height / 2, width: fullWidth, height, r: radius },
              style: { fill: plannedColor }
            },
            {
              type: 'rect',
              shape: { x: startCoord[0], y: startCoord[1] - height / 2, width: progressWidth, height, r: radius },
              style: api.style()
            },
            {
              type: 'text',
              style: {
                x: endCoord[0] + 8,
                y: startCoord[1],
                text: `${api.value(3)}%`,
                fill: '#4B5563',
                fontSize: 12,
                textVerticalAlign: 'middle'
              }
            }
          ]
        }
      },
      encode: { x: [1, 2], y: 0 },
      data,
      markLine: {
        symbol: 'none',
        silent: true,
        lineStyle: { color: '#F59E0B', type: 'dashed', width: 1 },
        label: {
          formatter: '今日',
          color: '#B45309',
          position: 'insideStartTop',
          distance: [0, 8],
          backgroundColor: '#FFFBEB',
          borderColor: '#F59E0B',
          borderWidth: 1,
          borderRadius: 4,
          padding: [2, 6]
        },
        data: [{ xAxis: today.getTime() }]
      }
    }, {
      name: '未启动',
      type: 'bar',
      data: [],
      itemStyle: { color: statusColors[0] }
    }, {
      name: '实施中',
      type: 'bar',
      data: [],
      itemStyle: { color: statusColors[1] }
    }, {
      name: '已归档',
      type: 'bar',
      data: [],
      itemStyle: { color: statusColors[2] }
    }, {
      name: '超期',
      type: 'bar',
      data: [],
      itemStyle: { color: '#EF4444' }
    }]
  })
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
  resizeHandler = () => chartInstance && chartInstance.resize()
  window.addEventListener('resize', resizeHandler)
}

function formatDateRange(row) {
  const start = row.start_date || (row.audit_year ? `${row.audit_year}-01-01` : '-')
  const end = row.end_date || (row.audit_year ? `${row.audit_year}-12-31` : '-')
  return `${start} 至 ${end}`
}

function formatProgressDetail(row) {
  if (Number(row.status) === 2) return '进度依据：项目已归档，进度计为100%'
  const scheme = Number(row.scheme_approved || 0) === 1 ? '方案已审批' : '方案未审批'
  const material = `资料${row.material_done || 0}/${row.material_total || 0}`
  const workpaper = `底稿${row.workpaper_done || 0}/${row.workpaper_total || 0}`
  const rectification = `整改${row.rect_done || 0}/${row.issue_total || 0}`
  const reportMap = { '-1': '未形成报告', 0: '报告草稿', 1: '报告待审批', 2: '报告已审批' }
  const report = reportMap[Number(row.report_status ?? -1)] || '未形成报告'
  return `进度依据：启动10分，${scheme}，${material}，${workpaper}，${rectification}，${report}`
}

function getProjectStart(row) {
  return parseProjectDate(row.start_date, row.audit_year, '01-01')
}

function getProjectEnd(row) {
  return parseProjectDate(row.end_date, row.audit_year, '12-31')
}

function parseProjectDate(value, year, fallbackMonthDay) {
  const dateValue = value || (year ? `${year}-${fallbackMonthDay}` : null)
  const date = dateValue ? new Date(dateValue) : new Date()
  return Number.isNaN(date.getTime()) ? new Date() : date
}

function addMonths(date, months) {
  const next = new Date(date)
  next.setMonth(next.getMonth() + months)
  return next
}

function gotoProject(row) {
  if (!canOpenProjectWorkspace) {
    ElMessage.info('当前角色仅可查看项目进度概览')
    return
  }
  // 穿透跳转到项目详情
  router.push({ path: '/audit/project', query: { projectId: row.id } }).catch(() => {})
}

onMounted(() => loadData())

onUnmounted(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null
  }
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.mt10 { margin-top: 10px; }
.mt20 { margin-top: 20px; }
.mb10 { margin-bottom: 10px; }
.mb20 { margin-bottom: 20px; }
</style>
