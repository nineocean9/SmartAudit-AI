<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams">
      <el-form-item label="归档状态">
        <el-select v-model="queryParams.archiveStatus" clearable placeholder="全部" @change="getList">
          <el-option label="整理中" :value="0"/><el-option label="待审核" :value="1"/><el-option label="已归档" :value="2"/>
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="getList">搜索</el-button></el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="archiveList">
      <el-table-column label="项目名称" prop="projectName" width="200"/>
      <el-table-column label="档案编号" prop="archiveNo" width="120"/>
      <el-table-column label="档案分类" prop="archiveCategory" width="100">
        <template #default="scope"><el-tag>{{ scope.row.archiveCategory }}</el-tag></template>
      </el-table-column>
      <el-table-column label="文件名" prop="fileName" show-overflow-tooltip/>
      <el-table-column label="状态" width="80">
        <template #default="scope">
          <el-tag :type="['warning','','success'][scope.row.archiveStatus]">{{ ['整理中','待审核','已归档'][scope.row.archiveStatus] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button link type="primary" v-if="scope.row.archiveStatus===0" @click="submitReview(scope.row)">提交审核</el-button>
          <el-button link type="success" v-if="scope.row.archiveStatus===1" @click="approveArchive(scope.row)">审核通过</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="新增归档" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="项目">
          <el-select v-model="form.projectId" filterable placeholder="选择项目">
            <el-option v-for="p in projectList" :key="p.id" :label="p.project_name || p.projectName" :value="p.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="档案分类">
          <el-select v-model="form.archiveCategory">
            <el-option label="立项类" value="立项类"/><el-option label="证明类" value="证明类"/>
            <el-option label="结项类" value="结项类"/><el-option label="备查类" value="备查类"/>
            <el-option label="整改类" value="整改类"/>
          </el-select>
        </el-form-item>
        <el-form-item label="档案编号"><el-input v-model="form.archiveNo"/></el-form-item>
        <el-form-item label="文件名"><el-input v-model="form.fileName"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const archiveList = ref([])
const projectList = ref([])
const queryParams = ref({})
const dialogVisible = ref(false)
const form = ref({})

function getList() {
  loading.value = true
  request({ url: '/audit/report/archive/list', params: queryParams.value }).then(res => {
    archiveList.value = res.rows || res.data || []; loading.value = false
  })
}
function getProjects() {
  request({ url: '/audit/info/progress' }).then(res => { projectList.value = res.data || [] })
}
function submitForm() {
  request({ url: '/audit/report/archive', method: 'post', data: form.value }).then(() => {
    ElMessage.success('操作成功'); dialogVisible.value = false; getList()
  })
}
function submitReview(row) {
  request({ url: '/audit/report/archive', method: 'put', data: { id: row.id, archiveStatus: 1 } }).then(() => {
    ElMessage.success('已提交审核'); getList()
  })
}
function approveArchive(row) {
  request({ url: '/audit/report/archive', method: 'put', data: { id: row.id, archiveStatus: 2, reviewBy: 'admin' } }).then(() => {
    ElMessage.success('归档成功'); getList()
  })
}
function handleDelete(row) {
  ElMessageBox.confirm('确认删除？').then(() => {
    request({ url: '/audit/report/archive/' + row.id, method: 'delete' }).then(() => { ElMessage.success('删除成功'); getList() })
  })
}
onMounted(() => { getList(); getProjects() })
</script>
