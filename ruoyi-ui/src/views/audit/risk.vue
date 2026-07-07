<template>
  <div class="app-container">
    <el-row :gutter="10">
      <el-col :span="24">
        <el-form :model="queryParams" inline>
          <el-form-item label="场景"><el-input v-model="queryParams.scenario" placeholder="适用场景" clearable /></el-form-item>
          <el-form-item><el-button type="primary" @click="getList">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
        </el-form>
      </el-col>
    </el-row>
    <el-row style="margin-bottom:10px"><el-button type="primary" @click="openAdd">新增风险</el-button></el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="风险名称" prop="riskName" width="200" />
      <el-table-column label="风险描述" prop="riskDesc" show-overflow-tooltip />
      <el-table-column label="适用场景" prop="scenario" width="120" />
      <el-table-column label="关联依据ID" prop="basisIds" width="100" />
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
        <el-form-item label="风险名称"><el-input v-model="form.riskName" /></el-form-item>
        <el-form-item label="风险描述"><el-input type="textarea" v-model="form.riskDesc" :rows="3" /></el-form-item>
        <el-form-item label="适用场景"><el-input v-model="form.scenario" /></el-form-item>
        <el-form-item label="关联依据ID"><el-input v-model="form.basisIds" placeholder="逗号分隔" /></el-form-item>
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
import { listRiskCase, addRiskCase, updateRiskCase, delRiskCase } from '@/api/audit/riskCase'
import { ElMessage, ElMessageBox } from 'element-plus'
const queryParams = ref({ pageNum:1, pageSize:10, scenario:'' })
const list = ref([]); const total = ref(0); const loading = ref(false)
const dialogVisible = ref(false); const form = ref({})
function getList() { loading.value=true; listRiskCase(queryParams.value).then(r=>{list.value=r.rows;total.value=r.total}).finally(()=>loading.value=false) }
function reset() { queryParams.value={pageNum:1,pageSize:10,scenario:''}; getList() }
function openAdd() { form.value={}; dialogVisible.value=true }
function openEdit(row) { form.value={...row}; dialogVisible.value=true }
function submitForm() {
  const api = form.value.id ? updateRiskCase(form.value) : addRiskCase(form.value)
  api.then(r=>{if(r.code===200){ElMessage.success('操作成功');dialogVisible.value=false;getList()}})
}
function handleDel(id) { ElMessageBox.confirm('确认删除？').then(()=>delRiskCase(id).then(r=>{if(r.code===200){ElMessage.success('已删除');getList()}})).catch(()=>{}) }
getList()
</script>
