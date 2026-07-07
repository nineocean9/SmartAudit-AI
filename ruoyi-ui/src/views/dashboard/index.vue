<template>
  <div class="app-container">
    <el-row :gutter="16">
      <el-col :span="6"><el-card><div class="stat-box" style="background:#ecf5ff;color:#409eff"><h3>{{data.projectStats?.['已归档']||0}}</h3><p>已归档项目</p></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat-box" style="background:#fdf6ec;color:#e6a23c"><h3>{{data.projectStats?.['实施中']||0}}</h3><p>实施中项目</p></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat-box" style="background:#fef0f0;color:#f56c6c"><h3>{{data.severityStats?.['高']||0}}</h3><p>高风险问题</p></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat-box" style="background:#f0f9eb;color:#67c23a"><h3>{{data.rectStats?.['已整改']||0}}</h3><p>已完成整改</p></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12"><el-card><template #header>问题严重度分布</template><div ref="pieChart" style="height:280px" /></el-card></el-col>
      <el-col :span="12"><el-card><template #header>整改完成率</template><div ref="ringChart" style="height:280px" /></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12"><el-card><template #header>各单位审计问题数</template><div ref="barChart" style="height:300px" /></el-card></el-col>
      <el-col :span="12"><el-card><template #header>最近项目</template>
        <el-table :data="recent" size="small"><el-table-column prop="name" label="项目" /><el-table-column prop="unit" label="单位" width="100" /><el-table-column label="状态" width="80"><template #default="s"><el-tag size="small" :type="s.row.status===2?'success':'warning'">{{s.row.status===2?'已归档':'实施中'}}</el-tag></template></el-table-column></el-table>
      </el-card></el-col>
    </el-row>
  </div>
</template>
<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'
const data = ref({}); const recent = ref([])
const pieChart = ref(null); const ringChart = ref(null); const barChart = ref(null)
async function load() {
  const r = await request({url:'/dashboard/data',method:'get'})
  data.value = r.data || {}; recent.value = data.value.recentProjects || []
  await nextTick()
  // 饼图
  if (pieChart.value) {
    const c = echarts.init(pieChart.value)
    const s = data.value.severityStats || {}
    c.setOption({tooltip:{trigger:'item'},series:[{type:'pie',radius:'60%',data:[{name:'高',value:s['高']||0,itemStyle:{color:'#f56c6c'}},{name:'中',value:s['中']||0,itemStyle:{color:'#e6a23c'}},{name:'低',value:s['低']||0,itemStyle:{color:'#909399'}}]}]})
  }
  // 环形图
  if (ringChart.value) {
    const c = echarts.init(ringChart.value)
    const s = data.value.rectStats || {}
    c.setOption({series:[{type:'pie',radius:['50%','75%'],label:{show:false},data:[{name:'已整改',value:s['已整改']||0,itemStyle:{color:'#67c23a'}},{name:'整改中',value:s['整改中']||0,itemStyle:{color:'#e6a23c'}},{name:'未整改',value:s['未整改']||0,itemStyle:{color:'#f56c6c'}}]}]})
  }
  // 柱状图
  if (barChart.value) {
    const c = echarts.init(barChart.value)
    const u = data.value.unitStats || []
    c.setOption({xAxis:{type:'category',data:u.map(x=>x.name),axisLabel:{rotate:20}},yAxis:{type:'value'},series:[{type:'bar',data:u.map(x=>x.count),itemStyle:{color:'#409eff'},barMaxWidth:40}]})
  }
}
onMounted(load)
</script>
<style scoped>
.stat-box{text-align:center;padding:16px;border-radius:8px}
.stat-box h3{font-size:28px;margin:0 0 4px 0}
.stat-box p{font-size:13px;margin:0;opacity:0.8}
</style>
