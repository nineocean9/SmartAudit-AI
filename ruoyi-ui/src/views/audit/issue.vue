<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="问题描述关键词" clearable style="width:200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="项目" prop="projectId">
        <el-select v-model="queryParams.projectId" placeholder="选择项目" clearable filterable style="width:200px">
          <el-option v-for="p in projectOptions" :key="p.id" :label="p.project_name" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="严重度" prop="severity">
        <el-select v-model="queryParams.severity" placeholder="严重度" clearable style="width:120px">
          <el-option label="低" :value="1" />
          <el-option label="中" :value="2" />
          <el-option label="高" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="issueList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" prop="id" width="70" align="center" />
      <el-table-column label="项目名" prop="projectName" width="180" show-overflow-tooltip />
      <el-table-column label="问题描述" prop="issueDesc" show-overflow-tooltip />
      <el-table-column label="严重度" width="80" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.severity === 3 ? 'danger' : scope.row.severity === 2 ? 'warning' : 'info'">
            {{ scope.row.severity === 3 ? '高' : scope.row.severity === 2 ? '中' : '低' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="关联依据" prop="basisTitle" width="160" show-overflow-tooltip />
      <el-table-column label="来源" prop="source" width="100" />
      <el-table-column label="截止日期" prop="deadline" width="110" />
      <el-table-column label="创建时间" prop="createTime" width="160" />
      <el-table-column label="操作" width="200" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          <el-button link type="warning" icon="Document" @click="handleRectify(scope.row)">发起整改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="选择项目" filterable style="width:100%">
            <el-option v-for="p in projectOptions" :key="p.id" :label="p.project_name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述" prop="issueDesc">
          <el-input type="textarea" v-model="form.issueDesc" :rows="4" placeholder="请输入问题描述" />
        </el-form-item>
        <el-form-item label="严重度" prop="severity">
          <el-select v-model="form.severity" placeholder="严重度" style="width:100%">
            <el-option label="低" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="高" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联依据" prop="basisId">
          <el-select v-model="form.basisId" placeholder="选择依据（可选）" clearable filterable style="width:100%">
            <el-option v-for="b in basisOptions" :key="b.id" :label="b.title" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源" prop="source">
          <el-input v-model="form.source" placeholder="问题来源，如：审计发现" />
        </el-form-item>
        <el-form-item label="截止日期" prop="deadline">
          <el-date-picker v-model="form.deadline" type="date" value-format="YYYY-MM-DD" placeholder="选择截止日期" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { listIssues, getIssue, addIssue, updateIssue, deleteIssue } from '@/api/audit/issue'
import { addRectification } from '@/api/audit/rectification'
import { getProgress } from '@/api/audit/auditInfo'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

// 查询参数
const queryParams = ref({ pageNum: 1, pageSize: 10, keyword: '', projectId: null, severity: null })
const issueList = ref([])
const total = ref(0)
const loading = ref(false)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = ref({})
const formRef = ref(null)
const rules = reactive({
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  issueDesc: [{ required: true, message: '请输入问题描述', trigger: 'blur' }],
  severity: [{ required: true, message: '请选择严重度', trigger: 'change' }]
})

// 下拉选项
const projectOptions = ref([])
const basisOptions = ref([])

/** 加载项目列表 */
function loadProjects() {
  getProgress().then(r => { projectOptions.value = r.data || [] })
}

/** 加载依据列表 */
function loadBasis() {
  request({ url: '/basis/list', method: 'get', params: { pageNum: 1, pageSize: 999 } }).then(r => {
    basisOptions.value = r.rows || []
  })
}

/** 查询列表 */
function getList() {
  loading.value = true
  listIssues(queryParams.value).then(r => {
    issueList.value = r.rows
    total.value = r.total
  }).finally(() => loading.value = false)
}

/** 搜索 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置 */
function resetQuery() {
  queryParams.value = { pageNum: 1, pageSize: 10, keyword: '', projectId: null, severity: null }
  getList()
}

/** 多选 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 新增 */
function handleAdd() {
  form.value = { severity: 1, source: '审计发现' }
  dialogTitle.value = '新增审计问题'
  dialogVisible.value = true
}

/** 修改 */
function handleUpdate(row) {
  const id = row && row.id ? row.id : ids.value[0]
  getIssue(id).then(r => {
    form.value = r.data
    dialogTitle.value = '修改审计问题'
    dialogVisible.value = true
  })
}

/** 提交 */
function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    const api = form.value.id ? updateIssue(form.value) : addIssue(form.value)
    api.then(r => {
      if (r.code === 200) {
        ElMessage.success('操作成功')
        dialogVisible.value = false
        getList()
      }
    })
  })
}

/** 删除 */
function handleDelete(row) {
  const delIds = row && row.id ? [row.id] : ids.value
  ElMessageBox.confirm('确认删除选中的 ' + delIds.length + ' 条问题记录？', '提示', { type: 'warning' }).then(() => {
    deleteIssue(delIds.join(',')).then(r => {
      if (r.code === 200) {
        ElMessage.success('删除成功')
        getList()
      }
    })
  }).catch(() => {})
}

/** 发起整改 */
function handleRectify(row) {
  ElMessageBox.prompt('请输入整改措施', '发起整改 - ' + row.issueDesc?.substring(0, 20), {
    confirmButtonText: '提交',
    cancelButtonText: '取消',
    inputType: 'textarea'
  }).then(({ value }) => {
    addRectification({ issueId: row.id, measure: value, status: 0 }).then(r => {
      if (r.code === 200) {
        ElMessage.success('整改已发起')
      }
    })
  }).catch(() => {})
}

// 初始化
loadProjects()
loadBasis()
getList()
</script>
