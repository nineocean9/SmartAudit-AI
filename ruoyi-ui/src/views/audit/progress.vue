<template>
  <div class="app-container">
    <el-row style="margin-bottom:15px">
      <el-col :span="24">
        <h3 style="margin:0">项目进度概览（甘特图）</h3>
      </el-col>
    </el-row>
    <div v-loading="loading" style="height:400px">
      <div v-for="(item,idx) in projects" :key="item.id" class="gantt-row">
        <div class="gantt-label">{{ item.project_name }}<br><small>{{ item.audited_unit }} · {{ item.audit_type }}{{ item.audit_year }}</small></div>
        <div class="gantt-bar-wrap">
          <div class="gantt-bar" :class="barClass(item.status)" :style="barStyle(idx)">
            <span class="gantt-text">{{ statusLabel(item.status) }}</span>
          </div>
          <div class="gantt-stats">
            <el-tag size="small" :type="item.issue_count>0?'warning':'info'">{{ item.issue_count||0 }}问题</el-tag>
            <el-tag size="small" type="success" style="margin-left:4px">{{ item.rect_count||0 }}已整改</el-tag>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && (!projects||projects.length===0)" description="暂无项目数据" />
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'; import { getProgress } from '@/api/audit/auditInfo'
const projects=ref([]); const loading=ref(false)
function load(){loading.value=true;getProgress().then(r=>{projects.value=r.data||[]}).finally(()=>loading.value=false)}
function statusLabel(s){return s===2?'已归档':s===1?'实施中':'未启动'}
function barClass(s){return s===2?'bar-done':s===1?'bar-progress':'bar-pending'}
function barStyle(idx){return{width:60+idx*8+'%'}}
load()
</script>
<style scoped>
.gantt-row{display:flex;align-items:center;margin-bottom:8px;padding:8px 0;border-bottom:1px solid #f0f0f0}
.gantt-label{width:200px;font-size:13px;flex-shrink:0;line-height:1.4}
.gantt-bar-wrap{flex:1;display:flex;align-items:center;gap:10px}
.gantt-bar{height:30px;border-radius:6px;display:flex;align-items:center;padding:0 12px;transition:width 0.5s ease;min-width:80px}
.gantt-text{color:#fff;font-size:12px;font-weight:500}
.bar-done{background:linear-gradient(90deg,#67c23a,#95d475)}
.bar-progress{background:linear-gradient(90deg,#409eff,#79bbff)}
.bar-pending{background:linear-gradient(90deg,#909399,#c0c4cc)}
.gantt-stats{white-space:nowrap}
small{color:#909399;font-size:11px}
</style>
