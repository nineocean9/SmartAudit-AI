<template>
  <div class="app-container">
    <el-row :gutter="12">
      <el-col :span="8">
        <el-card><template #header>项目选择</template>
          <div v-for="p in projects" :key="p.id" class="proj-item" :class="{active:curProject?.id===p.id}" @click="selectProject(p)">
            <strong>{{p.project_name}}</strong><br><small>{{p.audited_unit}} · {{p.audit_type}} · {{p.audit_year}}</small>
          </div>
          <el-empty v-if="!projects||projects.length===0" description="暂无项目" />
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card v-if="curProject"><template #header>{{curProject.project_name}} · 工作台</template>
          <el-tabs>
            <el-tab-pane label="方案">
              <el-button size="small" type="primary" @click="dlgScheme=true">新建方案</el-button>
              <div v-if="scheme" style="margin-top:8px"><pre style="background:#f5f7fa;padding:10px;border-radius:6px">{{scheme.content}}</pre></div>
              <el-empty v-else description="暂无方案，请新建" />
            </el-tab-pane>
            <el-tab-pane label="协同日志">
              <el-timeline><el-timeline-item v-for="l in collabLogs" :key="l.id" :timestamp="l.create_time">{{l.user_name}} · {{l.action}}：{{l.target}}</el-timeline-item></el-timeline>
              <el-empty v-if="!collabLogs||collabLogs.length===0" description="暂无日志" />
            </el-tab-pane>
          </el-tabs>
        </el-card>
        <el-empty v-else description="请从左侧选择一个项目" />
      </el-col>
    </el-row>
    <el-dialog title="新建方案" v-model="dlgScheme" width="500px">
      <el-form :model="f"><el-form-item label="内容"><el-input type="textarea" v-model="f.content" :rows="6" /></el-form-item></el-form>
      <template #footer><el-button @click="dlgScheme=false">取消</el-button><el-button type="primary" @click="submitScheme">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref } from 'vue'; import { ops } from '@/api/audit/auditOps'; import { getProgress } from '@/api/audit/auditInfo'; import { ElMessage } from 'element-plus'
const projects=ref([]); const curProject=ref(null); const scheme=ref(null); const collabLogs=ref([]); const dlgScheme=ref(false); const f=ref({})
function selectProject(p){ curProject.value=p; ops.schemeList(p.id).then(r=>{scheme.value=(r.data||[])[0]||null}); ops.collabLog(p.id).then(r=>{collabLogs.value=r.data||[]}) }
function submitScheme(){ ops.addScheme({projectId:curProject.value.id,content:f.value.content}).then(r=>{if(r.code===200){ElMessage.success('方案已创建');dlgScheme.value=false;selectProject(curProject.value)}}) }
getProgress().then(r=>{projects.value=r.data||[]})
</script>
<style scoped>
.proj-item{padding:10px 12px;margin-bottom:4px;border-radius:6px;cursor:pointer;border:1px solid #ebeef5;transition:all .15s}
.proj-item:hover,.proj-item.active{border-color:#409eff;background:#ecf5ff}
.proj-item strong{font-size:14px;color:#303133}.proj-item small{font-size:12px;color:#909399}
</style>
