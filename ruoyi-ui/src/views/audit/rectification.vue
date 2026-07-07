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
        </el-form>
      </el-col>
    </el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="项目" prop="projectName" width="200" show-overflow-tooltip />
      <el-table-column label="被审单位" prop="auditedUnit" width="120" />
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
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

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
import { listRectification, updateRectification } from '@/api/audit/rectification'
import { ElMessage } from 'element-plus'
const queryParams = ref({ pageNum:1, pageSize:10, status: null })
const list = ref([]); const total = ref(0); const loading = ref(false)
const editVisible = ref(false); const form = ref({})
function getList() {
  loading.value = true
  listRectification(queryParams.value).then(r => { list.value=r.rows; total.value=r.total }).finally(()=>loading.value=false)
}
function openEdit(row) {
  form.value = { issueId: row.issueId, status: row.rectStatus, feedback: row.feedback||'', evaluator: row.evaluator||'', finishDate: row.finishDate||'' }
  editVisible.value = true
}
function submitEdit() {
  updateRectification(form.value).then(r => { if(r.code===200){ ElMessage.success('更新成功'); editVisible.value=false; getList() } })
}
getList()
</script>
