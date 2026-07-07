<template>
  <div class="app-container">
    <el-row :gutter="10">
      <el-col :span="24">
        <el-form :model="queryParams" inline>
          <el-form-item label="分类"><el-input v-model="queryParams.category" placeholder="分类" clearable /></el-form-item>
          <el-form-item><el-button type="primary" @click="getList">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
        </el-form>
      </el-col>
    </el-row>
    <el-row style="margin-bottom:10px"><el-button type="primary" @click="openAdd">新增案例</el-button></el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="案例标题" prop="caseTitle" width="250" show-overflow-tooltip />
      <el-table-column label="案例内容" prop="caseContent" show-overflow-tooltip />
      <el-table-column label="分类" prop="category" width="120" />
      <el-table-column label="参考依据" prop="reference" width="120" />
      <el-table-column label="创建时间" prop="createTime" width="160" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDel(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-dialog :title="form.id?'编辑':'新增'" v-model="dialogVisible" width="600px">
      <el-form :model="form">
        <el-form-item label="标题"><el-input v-model="form.caseTitle" /></el-form-item>
        <el-form-item label="内容"><el-input type="textarea" v-model="form.caseContent" :rows="4" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.category" /></el-form-item>
        <el-form-item label="参考依据"><el-input v-model="form.reference" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { listCaseLib, addCaseLib, updateCaseLib, delCaseLib } from '@/api/audit/caseLib'
import { ElMessage, ElMessageBox } from 'element-plus'
const queryParams = ref({ pageNum:1, pageSize:10, category:'' })
const list = ref([]); const total = ref(0); const loading = ref(false)
const dialogVisible = ref(false); const form = ref({})
function getList() { loading.value=true; listCaseLib(queryParams.value).then(r=>{list.value=r.rows;total.value=r.total}).finally(()=>loading.value=false) }
function reset() { queryParams.value={pageNum:1,pageSize:10,category:''}; getList() }
function openAdd() { form.value={}; dialogVisible.value=true }
function openEdit(row) { form.value={...row}; dialogVisible.value=true }
function submitForm() {
  const api = form.value.id ? updateCaseLib(form.value) : addCaseLib(form.value)
  api.then(r=>{if(r.code===200){ElMessage.success('操作成功');dialogVisible.value=false;getList()}})
}
function handleDel(id) { ElMessageBox.confirm('确认删除？').then(()=>delCaseLib(id).then(r=>{if(r.code===200){ElMessage.success('已删除');getList()}})).catch(()=>{}) }
getList()
</script>
