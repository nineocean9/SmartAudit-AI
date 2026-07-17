<template>
  <div class="app-container">
    <el-row :gutter="10">
      <el-col :span="24">
        <el-form :model="queryParams" inline>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="整改状态" clearable @change="getList">
              <el-option label="全部" :value="null" />
              <el-option label="未整改" :value="0" />
              <el-option label="整改中" :value="1" />
              <el-option label="已完成" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" @click="getList">查询</el-button></el-form-item>
          <el-form-item><el-button type="success" @click="openAdd">新增整改</el-button></el-form-item>
        </el-form>
      </el-col>
    </el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="项目" prop="projectName" width="200" show-overflow-tooltip />
      <el-table-column label="被审计单位" prop="auditedUnit" width="120" />
      <el-table-column label="问题描述" prop="issueDesc" show-overflow-tooltip />
      <el-table-column label="严重度" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.severity === 3 ? 'danger' : scope.row.severity === 2 ? 'warning' : 'info'">
            {{ scope.row.severity === 3 ? '高' : scope.row.severity === 2 ? '中' : '低' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" prop="source" width="100" />
      <el-table-column label="整改措施" prop="measure" show-overflow-tooltip />
      <el-table-column label="整改状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.rectStatus === 2 ? 'success' : scope.row.rectStatus === 1 ? 'warning' : 'danger'">
            {{ scope.row.rectStatus === 2 ? '已整改' : scope.row.rectStatus === 1 ? '整改中' : '未整改' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="截止日期" prop="deadline" width="100" />
      <el-table-column label="完成日期" prop="finishDate" width="100" />
      <el-table-column label="跟踪人" prop="evaluator" width="80" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" @click="openEdit(scope.row)">跟踪</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增整改弹窗 -->
    <el-dialog title="新增整改" v-model="addVisible" width="500px">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="100px">
        <el-form-item label="关联问题" prop="issueId">
          <el-select v-model="addForm.issueId" placeholder="请选择关联问题" filterable style="width:100%">
            <el-option v-for="item in issueOptions" :key="item.issueId" :label="item.issueDesc" :value="item.issueId" />
          </el-select>
        </el-form-item>
        <el-form-item label="整改措施" prop="measure">
          <el-input type="textarea" v-model="addForm.measure" :rows="3" placeholder="请输入整改措施" />
        </el-form-item>
        <el-form-item label="整改状态">
          <el-select v-model="addForm.status">
            <el-option :value="0" label="未整改" />
            <el-option :value="1" label="整改中" />
            <el-option :value="2" label="已完成" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible=false">取 消</el-button>
        <el-button type="primary" @click="submitAdd">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 整改跟踪弹窗 -->
    <el-dialog title="整改跟踪" v-model="editVisible" width="500px">
      <el-form :model="form">
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option :value="0" label="未整改" />
            <el-option :value="1" label="整改中" />
            <el-option :value="2" label="已完成" />
          </el-select>
        </el-form-item>
        <el-form-item label="完成日期"><el-date-picker v-model="form.finishDate" type="date" style="width:100%" /></el-form-item>
        <el-form-item label="跟踪人"><el-input v-model="form.evaluator" /></el-form-item>
        <el-form-item label="反馈"><el-input type="textarea" v-model="form.feedback" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible=false">取 消</el-button>
        <el-button type="primary" @click="submitEdit">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { listRectification, addRectification, updateRectification, delRectification } from '@/api/audit/rectification'
import { ElMessage, ElMessageBox } from 'element-plus'
const queryParams = ref({ pageNum:1, pageSize:10, status: null })
const list = ref([]); const total = ref(0); const loading = ref(false)
const editVisible = ref(false); const form = ref({})
const addVisible = ref(false); const addFormRef = ref(null)
const addForm = ref({ issueId: null, measure: '', status: 0 })
const addRules = { issueId: [{ required: true, message: '请选择关联问题', trigger: 'change' }], measure: [{ required: true, message: '请输入整改措施', trigger: 'blur' }] }
const issueOptions = ref([])

function getList() {
  loading.value = true
  listRectification(queryParams.value).then(r => { list.value=r.rows; total.value=r.total }).finally(()=>loading.value=false)
}
function openAdd() {
  addForm.value = { issueId: null, measure: '', status: 0 }
  // 获取问题列表作为下拉选项（复用现有列表接口，不带状态筛选）
  listRectification({ pageNum: 1, pageSize: 1000 }).then(r => {
    // 去重（同一 issueId 可能有多条记录）
    const map = new Map()
    r.rows.forEach(row => { if (!map.has(row.issueId)) map.set(row.issueId, row) })
    issueOptions.value = Array.from(map.values())
  })
  addVisible.value = true
}
function submitAdd() {
  addFormRef.value.validate(valid => {
    if (!valid) return
    addRectification(addForm.value).then(r => {
      if (r.code === 200) { ElMessage.success('新增成功'); addVisible.value = false; getList() }
    })
  })
}
function openEdit(row) {
  form.value = { issueId: row.issueId, status: row.rectStatus, feedback: row.feedback||'', evaluator: row.evaluator||'', finishDate: row.finishDate||'' }
  editVisible.value = true
}
function submitEdit() {
  updateRectification(form.value).then(r => { if(r.code===200){ ElMessage.success('更新成功'); editVisible.value=false; getList() } })
}
function handleDelete(row) {
  // 如果没有整改记录（rect_id 为空），提示无法删除
  if (!row.rectId && row.rectId !== 0) {
    ElMessage.warning('该问题尚无整改记录'); return
  }
  ElMessageBox.confirm('确认删除该整改记录？', '提示', { type: 'warning' }).then(() => {
    delRectification(row.rectId).then(r => {
      if (r.code === 200) { ElMessage.success('删除成功'); getList() }
    })
  }).catch(() => {})
}
getList()
</script>
