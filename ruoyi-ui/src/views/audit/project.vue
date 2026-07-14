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
              <el-button size="small" type="primary" @click="dlgScheme=true" style="margin-bottom:12px">新建方案</el-button>
              <el-card v-if="scheme" shadow="hover" :body-style="{ padding: 0 }">
                <template #header>
                  <div style="display:flex;align-items:center;justify-content:space-between">
                    <span><el-icon style="margin-right:4px;vertical-align:middle"><Document /></el-icon>项目审计方案</span>
                    <el-tag :type="scheme.status===1?'success':'info'" size="small">{{ scheme.status===1?'已审批':'草稿' }}</el-tag>
                  </div>
                </template>
                <div class="scheme-content" v-html="formatScheme(scheme.content)" />
              </el-card>
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
    <el-dialog title="新建方案" v-model="dlgScheme" width="650px">
      <el-form :model="f" label-width="80px">
        <el-form-item label="内容">
          <el-input type="textarea" v-model="f.content" :rows="10" placeholder="请输入方案内容，支持段落/标题格式..." />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dlgScheme=false">取消</el-button><el-button type="primary" @click="submitScheme">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ops } from '@/api/audit/auditOps'
import { getProgress } from '@/api/audit/auditInfo'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'

const projects = ref([])
const curProject = ref(null)
const scheme = ref(null)
const collabLogs = ref([])
const dlgScheme = ref(false)
const f = ref({})

function selectProject(p) {
  curProject.value = p
  ops.schemeList(p.id).then(r => { scheme.value = (r.data || [])[0] || null })
  ops.collabLog(p.id).then(r => { collabLogs.value = r.data || [] })
}

function submitScheme() {
  ops.addScheme({ projectId: curProject.value.id, content: f.value.content }).then(r => {
    if (r.code === 200) { ElMessage.success('方案已创建'); dlgScheme.value = false; selectProject(curProject.value) }
  })
}

function formatScheme(text) {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
    .replace(/^(一|二|三|四|五|六|七|八|九|十|[一二三四五六七八九十]+、.+)$/gm, '<h4 style="color:#303133;margin:16px 0 8px;font-size:15px">$1</h4>')
    .replace(/^\d+[.、．]\s*(.+)$/gm, '<p style="margin:4px 0 4px 16px">• $1</p>')
}

getProgress().then(r => { projects.value = r.data || [] })
</script>

<style scoped>
.proj-item { padding:10px 12px; margin-bottom:4px; border-radius:6px; cursor:pointer; border:1px solid #ebeef5; transition:all .15s }
.proj-item:hover, .proj-item.active { border-color:#409eff; background:#ecf5ff }
.proj-item strong { font-size:14px; color:#303133 }
.proj-item small { font-size:12px; color:#909399 }
.scheme-content {
  padding: 20px 30px;
  min-height: 200px;
  max-height: calc(100vh - 350px);
  overflow-y: auto;
  background: #fafbfc;
  font-size: 14px;
  line-height: 2;
  color: #303133;
}
</style>
