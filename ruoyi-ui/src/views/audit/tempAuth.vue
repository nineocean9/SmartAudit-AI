<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" icon="Plus" @click="handleAdd">新增授权</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" @click="revokeExpired">回收过期授权</el-button></el-col>
    </el-row>

    <el-table v-loading="loading" :data="authList">
      <el-table-column label="用户" prop="userName" width="120"/>
      <el-table-column label="授权项目" prop="projectName" width="200"/>
      <el-table-column label="授权类型" prop="authType" width="100">
        <template #default="scope"><el-tag>{{ scope.row.authType==='outsource'?'中介审计':'临时授权' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="起始日期" prop="startDate" width="120"/>
      <el-table-column label="到期日期" prop="expireDate" width="120"/>
      <el-table-column label="状态" width="80">
        <template #default="scope"><el-tag :type="scope.row.status===1?'success':'danger'">{{ scope.row.status===1?'有效':'已回收' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button link type="warning" v-if="scope.row.status===1" @click="handleRevoke(scope.row)">回收</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="新增授权" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户ID"><el-input-number v-model="form.userId" :min="1"/></el-form-item>
        <el-form-item label="授权项目">
          <el-select v-model="form.projectId" filterable>
            <el-option v-for="p in projectList" :key="p.id" :label="p.project_name || p.projectName" :value="p.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="授权类型">
          <el-select v-model="form.authType"><el-option label="中介审计" value="outsource"/><el-option label="临时授权" value="temp"/></el-select>
        </el-form-item>
        <el-form-item label="起始日期"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD"/></el-form-item>
        <el-form-item label="到期日期"><el-date-picker v-model="form.expireDate" type="date" value-format="YYYY-MM-DD"/></el-form-item>
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
const authList = ref([])
const projectList = ref([])
const dialogVisible = ref(false)
const form = ref({ authType: 'outsource' })

function getList() {
  loading.value = true
  request({ url: '/audit/permission/tempAuth/list' }).then(res => {
    authList.value = res.rows || res.data || []; loading.value = false
  })
}
function getProjects() {
  request({ url: '/audit/info/progress' }).then(res => { projectList.value = res.data || [] })
}
function handleAdd() { form.value = { authType: 'outsource' }; dialogVisible.value = true }
function submitForm() {
  request({ url: '/audit/permission/tempAuth', method: 'post', data: form.value }).then(() => {
    ElMessage.success('授权成功'); dialogVisible.value = false; getList()
  })
}
function handleRevoke(row) {
  ElMessageBox.confirm('确认回收授权？').then(() => {
    request({ url: '/audit/permission/tempAuth/revoke/' + row.id, method: 'put' }).then(() => { ElMessage.success('已回收'); getList() })
  })
}
function revokeExpired() {
  request({ url: '/audit/permission/tempAuth/revokeExpired', method: 'post' }).then(res => {
    ElMessage.success('已回收过期授权'); getList()
  })
}
function handleDelete(row) {
  ElMessageBox.confirm('确认删除？').then(() => {
    request({ url: '/audit/permission/tempAuth/' + row.id, method: 'delete' }).then(() => { ElMessage.success('删除成功'); getList() })
  })
}
onMounted(() => { getList(); getProjects() })
</script>
