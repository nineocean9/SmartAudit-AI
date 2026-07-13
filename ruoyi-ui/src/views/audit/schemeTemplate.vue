<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams">
      <el-form-item label="审计类型">
        <el-select v-model="queryParams.auditType" clearable placeholder="全部" @change="getList">
          <el-option label="经济责任审计" value="经济责任审计"/>
          <el-option label="财务收支审计" value="财务收支审计"/>
          <el-option label="专项审计" value="专项审计"/>
          <el-option label="工程审计" value="工程审计"/>
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="getList">搜索</el-button></el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" icon="Plus" @click="handleAdd">新增模板</el-button></el-col>
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="模板名称" prop="templateName" width="250"/>
      <el-table-column label="审计类型" prop="auditType" width="150"/>
      <el-table-column label="状态" width="80">
        <template #default="scope"><el-tag :type="scope.row.status===1?'success':'info'">{{ scope.row.status===1?'启用':'停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="180"/>
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button link type="primary" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button link type="primary" @click="handlePreview(scope.row)">预览</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="800px" top="5vh">
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板名称"><el-input v-model="form.templateName"/></el-form-item>
        <el-form-item label="审计类型">
          <el-select v-model="form.auditType">
            <el-option label="经济责任审计" value="经济责任审计"/>
            <el-option label="财务收支审计" value="财务收支审计"/>
            <el-option label="专项审计" value="专项审计"/>
            <el-option label="工程审计" value="工程审计"/>
          </el-select>
        </el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0"/></el-form-item>
        <el-form-item label="模板内容">
          <el-input v-model="form.content" type="textarea" :rows="15" placeholder="请输入审计方案模板内容..."/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="模板预览" v-model="previewVisible" width="700px">
      <div style="white-space:pre-wrap;line-height:1.8">{{ previewContent }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const list = ref([])
const queryParams = ref({ auditType: '' })
const dialogVisible = ref(false)
const previewVisible = ref(false)
const dialogTitle = ref('')
const form = ref({ status: 1 })
const previewContent = ref('')

function getList() {
  loading.value = true
  request({ url: '/audit/prepare/template/list', params: queryParams.value }).then(res => {
    list.value = res.rows || res.data || []; loading.value = false
  })
}
function handleAdd() { form.value = { status: 1 }; dialogTitle.value = '新增模板'; dialogVisible.value = true }
function handleEdit(row) { form.value = { ...row }; dialogTitle.value = '编辑模板'; dialogVisible.value = true }
function handlePreview(row) { previewContent.value = row.content; previewVisible.value = true }
function submitForm() {
  const method = form.value.id ? 'put' : 'post'
  request({ url: '/audit/prepare/template', method, data: form.value }).then(() => {
    ElMessage.success('操作成功'); dialogVisible.value = false; getList()
  })
}
function handleDelete(row) {
  ElMessageBox.confirm('确认删除？').then(() => {
    request({ url: '/audit/prepare/template/' + row.id, method: 'delete' }).then(() => { ElMessage.success('删除成功'); getList() })
  })
}
onMounted(() => getList())
</script>
