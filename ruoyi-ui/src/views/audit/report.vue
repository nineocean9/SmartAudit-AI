<template>
  <div class="app-container">
    <el-form :model="q" inline>
      <el-form-item label="项目">
        <el-select v-model="q.projectId" placeholder="选择项目" clearable @change="getList">
          <el-option v-for="p in projects" :key="p.id" :label="p.project_name" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">查询</el-button>
      </el-form-item>
    </el-form>
    <el-button type="primary" icon="Plus" @click="openAdd" style="margin-bottom:12px">生成报告</el-button>

    <el-row :gutter="16">
      <!-- 左侧：报告列表 -->
      <el-col :span="viewingReport ? 8 : 24">
        <el-table v-loading="loading" :data="list" size="small" :highlight-current-row="true" @row-click="view">
          <el-table-column label="项目" prop="projectName" min-width="160" show-overflow-tooltip />
          <el-table-column label="版本" prop="versionType" width="120" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="s">
              <el-tag :type="s.row.status===2?'success':s.row.status===1?'warning':'info'" size="small">
                {{s.row.status===2?'已审批':s.row.status===1?'待审批':'草稿'}}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right" align="center" v-if="!viewingReport">
            <template #default="s">
              <el-button link type="primary" size="small" @click.stop="view(s.row)">查看</el-button>
              <el-button link type="primary" size="small" v-hasPermi="['audit:report:edit']" @click.stop="openEdit(s.row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-col>

      <!-- 右侧：报告内容展示面板 -->
      <el-col :span="16" v-if="viewingReport">
        <el-card>
          <template #header>
            <div style="display:flex;align-items:center;justify-content:space-between">
              <span style="font-weight:600">
                <el-icon style="margin-right:4px;vertical-align:middle"><Document /></el-icon>
                {{ viewingReport.projectName }} · {{ viewingReport.versionType || '报告' }}
              </span>
              <span>
                <el-button size="small" type="primary" @click="openEdit(viewingReport)">编辑</el-button>
                <el-button size="small" @click="viewingReport=null">关闭</el-button>
              </span>
            </div>
          </template>
          <div class="report-content" v-html="formatContent(viewContent)" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 编辑报告弹窗 -->
    <el-dialog title="编辑报告" v-model="dlgEdit" width="750px" top="5vh">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="项目">
          <el-select v-model="editForm.projectId" style="width:100%">
            <el-option v-for="p in projects" :key="p.id" :label="p.project_name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本">
          <el-select v-model="editForm.versionType" style="width:100%">
            <el-option label="处内审核稿" value="处内审核稿" />
            <el-option label="征求意见稿" value="征求意见稿" />
            <el-option label="正式稿" value="正式稿" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="editForm.title" placeholder="报告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input type="textarea" v-model="editForm.content" :rows="12" placeholder="请输入报告内容..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgEdit=false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
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
const q = ref({})
const list = ref([])
const loading = ref(false)
const viewingReport = ref(null)
const viewContent = ref('')
const dlgEdit = ref(false)
const editForm = ref({})

getProgress().then(r => { projects.value = r.data || [] })

function getList() {
  loading.value = true
  ops.reportList(q.value.projectId).then(r => { list.value = r.data || [] }).finally(() => loading.value = false)
}

function view(row) {
  ops.reportInfo(row.id).then(r => {
    viewContent.value = r.data?.content || ''
    viewingReport.value = row
  })
}

function openAdd() { editForm.value = {}; dlgEdit.value = true }
function openEdit(r) {
  editForm.value = { id: r.id, projectId: r.projectId, versionType: r.versionType, title: r.title, content: r.content || viewContent.value }
  dlgEdit.value = true
}

function submitEdit() {
  const api = editForm.value.id ? ops.editReport(editForm.value) : ops.addReport(editForm.value)
  api.then(r => {
    if (r.code === 200) {
      ElMessage.success('保存成功')
      dlgEdit.value = false
      getList()
      if (viewingReport.value && editForm.value.id === viewingReport.value.id) {
        viewContent.value = editForm.value.content
      }
    }
  })
}

function formatContent(text) {
  if (!text) return '<span style="color:#909399">暂无内容</span>'
  return text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
    .replace(/^(#{1,4})\s*(.+)$/gm, (_, h, t) => `<h${h.length} style="margin:16px 0 8px;color:#303133">${t}</h${h.length}>`)
    .replace(/\*\*(.+?)\*\*/g, '<b>$1</b>')
}

getList()
</script>

<style scoped>
.report-content {
  padding: 20px 30px;
  min-height: 400px;
  max-height: calc(100vh - 280px);
  overflow-y: auto;
  background: #fafbfc;
  border-radius: 6px;
  font-size: 14px;
  line-height: 2;
  color: #303133;
}
</style>
