<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="分析名称" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="请输入名称/项目/关键词" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="来源" prop="sourceType">
        <el-select v-model="queryParams.sourceType" placeholder="全部来源" clearable style="width: 160px">
          <el-option label="聊天生成" value="chat" />
          <el-option label="页面上传" value="upload" />
          <el-option label="项目分析" value="project" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="openCreateDialog('project')">新建分析</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Upload" @click="openCreateDialog('upload')">上传文件</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="loadList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="analysisList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" :selectable="row => row.canManage" />
      <el-table-column label="编号" align="center" prop="id" width="70" />
      <el-table-column label="分析名称" align="center" prop="title" :show-overflow-tooltip="true" min-width="200">
        <template #default="{ row }">
          <el-link type="primary" :underline="false" @click="viewDashboard(row)">{{ row.title }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="来源" align="center" prop="sourceType" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.sourceType === 'chat'" type="success" size="small">聊天生成</el-tag>
          <el-tag v-else-if="row.sourceType === 'upload'" type="warning" size="small">页面上传</el-tag>
          <el-tag v-else-if="row.sourceType === 'project'" type="primary" size="small">项目分析</el-tag>
          <el-tag v-else size="small">{{ row.sourceType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="关联项目" align="center" prop="projectName" :show-overflow-tooltip="true" width="160" />
      <el-table-column label="关键词" align="center" prop="keyword" :show-overflow-tooltip="true" width="120" />
      <el-table-column label="创建者" align="center" prop="createBy" width="100" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="{ row }">
          <span>{{ row.createTime }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template #default="{ row }">
          <el-tooltip content="查看" placement="top">
            <el-button link type="primary" icon="View" @click="viewDashboard(row)"></el-button>
          </el-tooltip>
          <el-tooltip content="修改" placement="top">
            <el-button v-if="row.canManage" link type="primary" icon="Edit" @click="handleUpdate(row)"></el-button>
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button v-if="row.canManage" link type="primary" icon="Delete" @click="handleDelete(row)"></el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="loadList" />

    <!-- 修改对话框 -->
    <el-dialog :title="editTitle" v-model="editOpen" width="500px" append-to-body>
      <el-form :model="editForm" :rules="editRules" ref="editRef" label-width="80px">
        <el-form-item label="分析名称" prop="title">
          <el-input v-model="editForm.title" placeholder="请输入分析名称" />
        </el-form-item>
        <el-form-item label="关联项目" prop="projectName">
          <el-input v-model="editForm.projectName" disabled />
        </el-form-item>
        <el-form-item label="关键词" prop="keyword">
          <el-input v-model="editForm.keyword" placeholder="请输入关键词" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitEdit">确 定</el-button>
          <el-button @click="editOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 新建分析对话框 -->
    <el-dialog v-model="createOpen" :title="sourceType === 'project' ? '新建项目分析' : '上传文件生成分析'" width="560px" :close-on-click-modal="false" :destroy-on-close="true">
      <el-form label-width="100px">
        <el-form-item label="数据来源">
          <el-radio-group v-model="sourceType">
            <el-radio value="project">从项目库选择</el-radio>
            <el-radio value="upload">上传文件</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="sourceType === 'project'">
          <el-form-item label="选择项目" required>
            <el-select v-model="createForm.projectId" placeholder="搜索或选择有权访问的项目" filterable style="width:100%">
              <el-option-group v-for="plan in projectTree" :key="plan.planId" :label="plan.label || plan.planName">
                <el-option v-for="p in plan.projects" :key="p.id" :label="`${p.projectName}（${p.auditedUnit || ''}·${p.auditType || ''}）`" :value="p.id" />
              </el-option-group>
              <el-option v-if="projectTree.length === 0" label="暂无项目数据，可手动输入" value="" disabled />
            </el-select>
          </el-form-item>
          <el-form-item label="分析主题" required>
            <el-select v-model="createForm.keyword" placeholder="选择或输入" filterable allow-create style="width:100%">
              <el-option v-for="t in topicOptions" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="上传文件" required>
            <el-upload drag :action="''" :before-upload="handleUploadFile" :limit="1" accept=".xlsx,.xls,.csv,.docx,.txt,.pdf">
              <el-icon><UploadFilled /></el-icon>
              <div>拖拽或点击上传</div>
            </el-upload>
            <div v-if="uploadFile" style="margin-top:6px;font-size:13px;color:#409eff">已选择: {{ uploadFile.name }}</div>
          </el-form-item>
          <el-form-item label="关联项目" required>
            <el-select v-model="createForm.projectId" placeholder="请选择有权访问的关联项目" filterable style="width:100%">
              <el-option-group v-for="plan in projectTree" :key="plan.planId" :label="plan.label || plan.planName">
                <el-option v-for="p in plan.projects" :key="p.id" :label="p.projectName" :value="p.id" />
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-form-item label="分析主题">
            <el-select v-model="createForm.keyword" placeholder="选择或输入" filterable allow-create style="width:100%">
              <el-option v-for="t in topicOptions" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="createOpen = false">取消</el-button>
        <el-button type="primary" :loading="analyzing" :disabled="!canSubmit" @click="doAnalyze">
          {{ analyzing ? '分析中...' : '生成分析' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { listAnalysisResults, analyzeProject, analyzeUpload, deleteAnalysis, updateAnalysis, getAnalysisResult } from '@/api/ai/dataAnalyze'
import { listProjectTree } from '@/api/ai/workspace'

const router = useRouter()
const showSearch = ref(true)
const analysisList = ref([])
const loading = ref(false)
const total = ref(0)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const queryRef = ref(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: undefined,
  sourceType: undefined
})

// 编辑
const editOpen = ref(false)
const editTitle = ref('修改分析')
const editRef = ref(null)
const editForm = reactive({ id: null, title: '', projectName: '', keyword: '' })
const editRules = { title: [{ required: true, message: '请输入分析名称', trigger: 'blur' }] }

// 新建
const createOpen = ref(false)
const sourceType = ref('project')
const createForm = reactive({ projectId: null, keyword: '' })
const uploadFile = ref(null)
const analyzing = ref(false)
const projectTree = ref([])
const topicOptions = ['自动检测', '预算', '收入', '支出', '收支结构', '采购', '合同', '资产', '整改', '风险分析', '执行率', '趋势分析', '财务报表']

const canSubmit = computed(() => {
  if (sourceType.value === 'project') return createForm.projectId && createForm.keyword?.trim()
  return uploadFile.value != null && createForm.projectId
})

function handleQuery() {
  queryParams.pageNum = 1
  loadList()
}

function resetQuery() {
  queryRef.value?.resetFields()
  queryParams.keyword = undefined
  queryParams.sourceType = undefined
  handleQuery()
}

async function loadList() {
  loading.value = true
  try {
    const res = await listAnalysisResults(queryParams.keyword || undefined, queryParams.pageNum, queryParams.pageSize)
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

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function viewDashboard(row) {
  router.push(`/visualization/detail?id=${row.id}`)
}

async function handleUpdate(row) {
  const id = row?.id || ids.value[0]
  try {
    const res = await getAnalysisResult(id)
    if (res.code === 200 && res.data) {
      editForm.id = res.data.id
      editForm.title = res.data.title || ''
      editForm.projectName = res.data.projectName || ''
      editForm.keyword = res.data.keyword || ''
      editTitle.value = '修改分析'
      editOpen.value = true
    }
  } catch {
    ElMessage.error('获取详情失败')
  }
}

async function submitEdit() {
  try {
    await editRef.value?.validate()
    const res = await updateAnalysis(editForm)
    if (res.code === 200) {
      ElMessage.success('修改成功')
      editOpen.value = false
      loadList()
    } else {
      ElMessage.error(res.msg || '修改失败')
    }
  } catch { /* validate fail */ }
}

async function handleDelete(row) {
  const delIds = row?.id ? [row.id] : ids.value
  if (!delIds.length) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${delIds.length} 条分析记录？`, '系统提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
  } catch { return }
  try {
    const res = await deleteAnalysis(delIds.join(','))
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadList()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch {
    ElMessage.error('删除失败')
  }
}

function openCreateDialog(type) {
  sourceType.value = type
  createForm.projectId = null
  createForm.keyword = ''
  uploadFile.value = null
  createOpen.value = true
  loadProjectTree()
}

function handleUploadFile(file) {
  uploadFile.value = file
  return false
}

async function loadProjectTree() {
  try {
    const res = await listProjectTree()
    if (res.code === 200) projectTree.value = (res.data || []).map(plan => ({
      ...plan,
      label: `${plan.planName || '计划'}(${plan.planYear || ''})`
    }))
  } catch { /* ignore */ }
}

async function doAnalyze() {
  analyzing.value = true
  try {
    let res
    if (sourceType.value === 'project') {
      res = await analyzeProject({
        projectId: String(createForm.projectId),
        keyword: createForm.keyword === '自动检测' ? '自动分析' : createForm.keyword
      })
    } else {
      const formData = new FormData()
      formData.append('file', uploadFile.value)
      if (createForm.keyword) formData.append('keyword', createForm.keyword)
      formData.append('projectId', String(createForm.projectId))
      res = await analyzeUpload(formData)
    }
    if (res.code === 200 && res.data) {
      ElMessage.success('分析完成！')
      createOpen.value = false
      await loadList()
      if (res.data.analysisId) router.push(`/visualization/detail?id=${res.data.analysisId}`)
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
