<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <el-row justify="space-between" align="middle">
          <el-col :span="12">
            <span style="font-size:16px;font-weight:600">数据可视化</span>
          </el-col>
          <el-col :span="12" style="text-align:right">
            <el-button type="primary" @click="openCreateDialog('project')">新建分析</el-button>
            <el-button @click="openCreateDialog('upload')">上传文件生成</el-button>
          </el-col>
        </el-row>
        <el-input v-model="searchKey" placeholder="搜索分析名称、项目名、关键词..." clearable style="margin-top:8px" @clear="loadList" @keyup.enter="loadList" />
      </template>

      <el-table :data="analysisList" v-loading="loading">
        <el-table-column prop="title" label="分析名称" min-width="200">
          <template #default="{row}">
            <span style="color:#409eff;cursor:pointer;font-weight:500" @click="viewDashboard(row)">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源" width="90">
          <template #default="{row}">
            <el-tag v-if="row.sourceType === 'chat'" type="success" size="small">聊天生成</el-tag>
            <el-tag v-else-if="row.sourceType === 'upload'" type="warning" size="small">页面上传</el-tag>
            <el-tag v-else-if="row.sourceType === 'project'" type="primary" size="small">项目分析</el-tag>
            <el-tag v-else size="small">{{ row.sourceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="projectName" label="项目" width="150" />
        <el-table-column prop="keyword" label="关键词" width="100" />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{row}">
            <el-button text type="primary" size="small" @click="viewDashboard(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        style="margin-top:12px;text-align:right"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadList"
      />

      <el-empty v-if="!loading && analysisList.length === 0" description="暂无分析结果，点击上方按钮新建分析" />
    </el-card>

    <!-- 新建分析对话框 -->
    <el-dialog v-model="dialogVisible" :title="sourceType === 'project' ? '新建分析' : '上传文件生成分析'" width="560px" :close-on-click-modal="false" :destroy-on-close="true">
      <el-form label-width="100px">
        <el-form-item label="数据来源">
          <el-radio-group v-model="sourceType">
            <el-radio value="project">从项目库选择</el-radio>
            <el-radio value="upload">上传文件</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="sourceType === 'project'">
          <el-form-item label="选择项目" required>
            <el-select v-model="projectName" placeholder="搜索或选择项目" filterable allow-create style="width:100%" @change="onProjectChange">
              <el-option-group v-for="plan in projectTree" :key="plan.planId" :label="plan.label || plan.planName">
                <el-option v-for="p in plan.projects" :key="p.id" :label="`${p.projectName}（${p.auditedUnit || ''}·${p.auditType || ''}）`" :value="p.projectName" />
              </el-option-group>
              <el-option v-if="projectTree.length === 0" label="暂无项目数据，可手动输入" value="" disabled />
            </el-select>
          </el-form-item>
          <el-form-item label="分析主题" required>
            <el-select v-model="keyword" placeholder="选择或输入" filterable allow-create style="width:100%" @change="onKeywordChange">
              <el-option v-for="t in topicOptions" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="上传文件" required>
            <el-upload drag :action="''" :before-upload="handleUpload" :limit="1" accept=".xlsx,.xls,.csv,.docx,.txt">
              <el-icon><UploadFilled /></el-icon>
              <div>拖拽或点击上传</div>
            </el-upload>
            <div v-if="uploadFile" style="margin-top:6px;font-size:13px;color:#409eff">已选择: {{ uploadFile.name }}</div>
          </el-form-item>
          <el-form-item label="关联项目">
            <el-select v-model="projectName" placeholder="可选，选择关联项目" filterable allow-create clearable style="width:100%">
              <el-option-group v-for="plan in projectTree" :key="plan.planId" :label="plan.label || plan.planName">
                <el-option v-for="p in plan.projects" :key="p.id" :label="p.projectName" :value="p.projectName" />
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-form-item label="分析主题" required>
            <el-select v-model="keyword" placeholder="选择或输入" filterable allow-create style="width:100%" @change="onKeywordChange">
              <el-option v-for="t in topicOptions" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
        </template>

        <el-form-item label="分析名称">
          <el-input v-model="customTitle" placeholder="AI 将自动生成分析名称，也可手动修改" />
          <div style="font-size:12px;color:#909399;margin-top:4px">自动生成: {{ autoTitle }}</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="analyzing" :disabled="!canSubmit" @click="doAnalyze">
          {{ analyzing ? '分析中...' : '生成分析' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { listAnalysisResults, analyzeProject, analyzeUpload } from '@/api/ai/dataAnalyze'
import { listProjectTree } from '@/api/ai/workspace'

const router = useRouter()
const baseApiUrl = import.meta.env.VITE_APP_BASE_API || ''
const searchKey = ref('')
const analysisList = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const sourceType = ref('project')
const projectName = ref('')
const keyword = ref('')
const customTitle = ref('')
const uploadFile = ref(null)
const analyzing = ref(false)
const projectTree = ref([])

const topicOptions = ['自动检测', '预算', '收入', '支出', '收支结构', '采购', '合同', '资产', '整改', '风险分析', '执行率', '趋势分析', '财务报表']

const autoTitle = computed(() => {
  if (!projectName.value && !keyword.value) return 'AI 将自动生成分析名称'
  const parts = []
  if (projectName.value) parts.push(projectName.value)
  if (keyword.value) parts.push(keyword.value)
  parts.push('分析')
  return parts.join('')
})

const canSubmit = computed(() => {
  if (sourceType.value === 'project') return projectName.value.trim() && keyword.value.trim()
  return uploadFile.value != null
})

async function loadProjectTree() {
  try {
    const res = await listProjectTree()
    if (res.code === 200) projectTree.value = (res.data || []).map(plan => ({
      ...plan,
      label: `${plan.planName || '计划'}(${plan.planYear || ''})`
    }))
  } catch { /* ignore */ }
}

async function loadList() {
  loading.value = true
  try {
    const res = await listAnalysisResults(searchKey.value || undefined, pageNum.value, pageSize.value)
    if (res.code === 0 || res.code === 200) {
      analysisList.value = res.rows || []
      total.value = res.total || 0
    }
  } catch {
    ElMessage.error('加载列表失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog(type) {
  sourceType.value = type
  projectName.value = ''
  keyword.value = ''
  customTitle.value = ''
  uploadFile.value = null
  dialogVisible.value = true
  loadProjectTree()
}

function viewDashboard(row) {
  window.open(`${baseApiUrl}/ai/data/analysis/${row.id}/html`, '_blank')
}

function handleUpload(file) {
  uploadFile.value = file
  return false
}

async function doAnalyze() {
  analyzing.value = true
  try {
    let res
    if (sourceType.value === 'project') {
      res = await analyzeProject({
        projectName: projectName.value,
        keyword: keyword.value === '自动检测' ? '自动分析' : keyword.value
      })
    } else {
      const formData = new FormData()
      formData.append('file', uploadFile.value)
      if (keyword.value) formData.append('keyword', keyword.value)
      if (projectName.value) formData.append('projectName', projectName.value)
      res = await analyzeUpload(formData)
    }
    if (res.code === 200 && res.data) {
      ElMessage.success('分析完成！')
      dialogVisible.value = false
      await loadList()
      if (res.data.analysisId) window.open(`${baseApiUrl}/ai/data/analysis/${res.data.analysisId}/html`, '_blank')
    } else {
      ElMessage.error(res.msg || '分析失败')
    }
  } catch (e) {
    ElMessage.error('分析失败: ' + (e.message || ''))
  } finally {
    analyzing.value = false
  }
}

onMounted(() => { loadList(); loadProjectTree() })
</script>
