<template>
  <div class="app-container">
    <!-- Search form -->
    <el-form :inline="true" :model="queryParams" ref="queryRef">
      <el-form-item label="单位名称" prop="unitName">
        <el-input v-model="queryParams.unitName" placeholder="请输入单位名称" clearable @keyup.enter="getList" style="width:180px"/>
      </el-form-item>
      <el-form-item label="单位类型" prop="unitType">
        <el-select v-model="queryParams.unitType" clearable placeholder="全部" @change="getList">
          <el-option label="职能部门" value="职能部门"/><el-option label="二级学院" value="二级学院"/>
          <el-option label="直属单位" value="直属单位"/><el-option label="校办企业" value="校办企业"/>
          <el-option label="基建项目部" value="基建项目部"/><el-option label="附属医院" value="附属医院"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- Toolbar -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" icon="Plus" @click="handleAdd">新增单位</el-button></el-col>
    </el-row>

    <!-- Categorized tabs -->
    <el-tabs v-model="activeTab" @tab-click="handleTabClick" type="card" style="margin-bottom:12px">
      <el-tab-pane label="全部" name=""/>
      <el-tab-pane label="职能部门" name="职能部门"/>
      <el-tab-pane label="二级学院" name="二级学院"/>
      <el-tab-pane label="直属单位" name="直属单位"/>
      <el-tab-pane label="校办企业" name="校办企业"/>
      <el-tab-pane label="基建项目部" name="基建项目部"/>
      <el-tab-pane label="附属医院" name="附属医院"/>
    </el-tabs>

    <!-- Table -->
    <el-table v-loading="loading" :data="unitList" @row-click="handleDetail" row-key="id" border highlight-current-row>
      <el-table-column label="单位编码" prop="unitCode" width="100" align="center"/>
      <el-table-column label="单位名称" prop="unitName" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <el-link type="primary" :underline="false">{{ scope.row.unitName || scope.row.unit_name }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="单位类型" prop="unitType" width="100" align="center">
        <template #default="scope">
          <el-tag>{{ scope.row.unitType || scope.row.unit_type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分管校领导" prop="parentLeader" width="100" align="center"/>
      <el-table-column label="编制人数" prop="staffCount" width="80" align="center"/>
      <el-table-column label="财务联系人" prop="financeContact" width="100" align="center"/>
      <el-table-column label="联系电话" prop="contactPhone" width="130"/>
      <el-table-column label="操作" width="160" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click.stop="handleEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" icon="Delete" @click.stop="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList"/>

    <!-- Add/Edit Dialog -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="680px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="单位名称" prop="unitName">
              <el-input v-model="form.unitName" placeholder="请输入单位名称"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位编码" prop="unitCode">
              <el-input v-model="form.unitCode" placeholder="请输入单位编码"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="单位类型" prop="unitType">
              <el-select v-model="form.unitType" style="width:100%" placeholder="请选择单位类型">
                <el-option label="职能部门" value="职能部门"/><el-option label="二级学院" value="二级学院"/>
                <el-option label="直属单位" value="直属单位"/><el-option label="校办企业" value="校办企业"/>
                <el-option label="基建项目部" value="基建项目部"/><el-option label="附属医院" value="附属医院"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分管校领导" prop="parentLeader">
              <el-input v-model="form.parentLeader" placeholder="请输入分管校领导"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="编制人数" prop="staffCount">
              <el-input-number v-model="form.staffCount" :min="0" controls-position="right" style="width:100%"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年度经费" prop="annualBudget">
              <el-input-number v-model="form.annualBudget" :min="0" :precision="2" controls-position="right" style="width:100%" placeholder="万元"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="财务联系人" prop="financeContact">
              <el-input v-model="form.financeContact" placeholder="请输入财务联系人"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="办公地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入办公地址"/>
        </el-form-item>
        <el-form-item label="基本情况" prop="profile">
          <el-input v-model="form.profile" type="textarea" :rows="3" placeholder="请输入单位基本情况"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- Detail Drawer -->
    <el-drawer v-model="drawerVisible" :title="(detailUnit.unitName || detailUnit.unit_name || '') + ' - 单位档案'" size="580px">
      <el-descriptions :column="2" border style="margin-bottom:20px">
        <el-descriptions-item label="单位编码">{{ detailUnit.unitCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="单位类型">
          <el-tag>{{ detailUnit.unitType || detailUnit.unit_type || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="分管校领导">{{ detailUnit.parentLeader || '-' }}</el-descriptions-item>
        <el-descriptions-item label="编制人数">{{ detailUnit.staffCount || '-' }}</el-descriptions-item>
        <el-descriptions-item label="年度经费">{{ detailUnit.annualBudget ? detailUnit.annualBudget + ' 万元' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="财务联系人">{{ detailUnit.financeContact || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailUnit.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="办公地址">{{ detailUnit.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="基本情况" :span="2">{{ detailUnit.profile || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">领导干部</el-divider>
      <el-table :data="detailUnit.leaders || []" size="small" border>
        <el-table-column label="姓名" prop="name" width="80" align="center"/>
        <el-table-column label="职务" prop="position"/>
        <el-table-column label="任期" width="200" align="center">
          <template #default="scope">{{ scope.row.tenure_start || '-' }} ~ {{ scope.row.tenure_end || '-' }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!(detailUnit.leaders||[]).length" description="暂无干部记录" :image-size="60"/>

      <el-divider content-position="left">历年审计</el-divider>
      <el-timeline>
        <el-timeline-item v-for="p in (detailUnit.projects||[])" :key="p.id" :timestamp="p.audit_year + '年'" placement="top">
          <el-card shadow="hover">
            <p style="margin:0">{{ p.project_name }} <el-tag size="small" style="margin-left:6px">{{ p.audit_type }}</el-tag></p>
            <el-tag :type="['info','','success'][p.status]" size="small" style="margin-top:6px">
              {{ ['未启动','实施中','已归档'][p.status] }}
            </el-tag>
          </el-card>
        </el-timeline-item>
        <el-timeline-item v-if="!(detailUnit.projects||[]).length" timestamp="">暂无审计记录</el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUnit, addUnit, delUnit, getUnitProfile } from '@/api/audit/auditInfo'
import request from '@/utils/request'

const loading = ref(false)
const unitList = ref([])
const total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 10 })
const activeTab = ref('')
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const dialogTitle = ref('')
const form = ref({})
const formRef = ref(null)
const detailUnit = ref({})

const rules = {
  unitName: [{ required: true, message: '请输入单位名称', trigger: 'blur' }],
  unitType: [{ required: true, message: '请选择单位类型', trigger: 'change' }]
}

function getList() {
  loading.value = true
  const params = { ...queryParams.value }
  if (activeTab.value) params.unitType = activeTab.value
  listUnit(params).then(res => {
    unitList.value = res.rows || res.data || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function resetQuery() {
  queryParams.value = { pageNum: 1, pageSize: 10 }
  activeTab.value = ''
  getList()
}

function handleTabClick(tab) {
  queryParams.value.unitType = tab.props.name || ''
  queryParams.value.pageNum = 1
  getList()
}

function handleAdd() {
  form.value = { unitType: '职能部门' }
  dialogTitle.value = '新增单位'
  dialogVisible.value = true
}

function handleEdit(row) {
  form.value = { ...row }
  // Normalize field names from snake_case
  if (row.unit_name && !row.unitName) form.value.unitName = row.unit_name
  if (row.unit_type && !row.unitType) form.value.unitType = row.unit_type
  dialogTitle.value = '编辑单位'
  dialogVisible.value = true
}

function submitForm() {
  formRef.value?.validate(valid => {
    if (!valid) return
    const method = form.value.id ? 'put' : 'post'
    request({ url: '/audit/info/unit', method, data: form.value }).then(() => {
      ElMessage.success('操作成功')
      dialogVisible.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  const name = row.unitName || row.unit_name
  ElMessageBox.confirm('确定要删除单位「' + name + '」吗？', '提示', { type: 'warning' }).then(() => {
    delUnit(row.id).then(res => {
      if (res.code === 200) {
        ElMessage.success('删除成功')
        getList()
      }
    })
  }).catch(() => {})
}

function handleDetail(row) {
  detailUnit.value = { ...row }
  // Normalize field names
  if (row.unit_name && !row.unitName) detailUnit.value.unitName = row.unit_name
  if (row.unit_type && !row.unitType) detailUnit.value.unitType = row.unit_type

  const id = row.id
  getUnitProfile(id).then(res => {
    detailUnit.value.leaders = res.data?.leaders || []
    detailUnit.value.projects = res.data?.projects || []
    drawerVisible.value = true
  }).catch(() => {
    detailUnit.value.leaders = []
    detailUnit.value.projects = []
    drawerVisible.value = true
  })
}

onMounted(() => getList())
</script>
