<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="所属单位" prop="unitId">
        <el-select v-model="queryParams.unitId" placeholder="全部" clearable @change="getList">
          <el-option v-for="u in unitList" :key="u.id" :label="u.unitName || u.unit_name" :value="u.id"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" icon="Plus" @click="handleAdd">新增</el-button></el-col>
    </el-row>

    <el-table v-loading="loading" :data="leaderList" @row-click="handleDetail">
      <el-table-column label="姓名" prop="name" width="100"/>
      <el-table-column label="性别" prop="gender" width="60"/>
      <el-table-column label="工号" prop="id_number" width="100"/>
      <el-table-column label="所属单位" prop="unit_name" width="160"/>
      <el-table-column label="职务" prop="position" width="150"/>
      <el-table-column label="任期起始" prop="tenure_start" width="120"/>
      <el-table-column label="任期结束" prop="tenure_end" width="120"/>
      <el-table-column label="分管业务" prop="managed_scope" show-overflow-tooltip/>
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button link type="primary" @click.stop="handleEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" @click.stop="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="姓名"><el-input v-model="form.name"/></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender"><el-radio label="男"/><el-radio label="女"/></el-radio-group>
        </el-form-item>
        <el-form-item label="工号"><el-input v-model="form.idNumber"/></el-form-item>
        <el-form-item label="所属单位">
          <el-select v-model="form.unitId" placeholder="请选择">
            <el-option v-for="u in unitList" :key="u.id" :label="u.unitName || u.unit_name" :value="u.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="职务"><el-input v-model="form.position"/></el-form-item>
        <el-form-item label="任期起始"><el-date-picker v-model="form.tenureStart" type="date" value-format="YYYY-MM-DD"/></el-form-item>
        <el-form-item label="任期结束"><el-date-picker v-model="form.tenureEnd" type="date" value-format="YYYY-MM-DD"/></el-form-item>
        <el-form-item label="分管资金规模"><el-input-number v-model="form.managedFunds" :min="0" :precision="2"/></el-form-item>
        <el-form-item label="分管业务"><el-input v-model="form.managedScope" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="detailData.name + ' - 详细信息'" size="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="姓名">{{ detailData.name }}</el-descriptions-item>
        <el-descriptions-item label="职务">{{ detailData.position }}</el-descriptions-item>
        <el-descriptions-item label="所属单位">{{ detailData.unit_name }}</el-descriptions-item>
        <el-descriptions-item label="任期">{{ detailData.tenure_start }} ~ {{ detailData.tenure_end }}</el-descriptions-item>
        <el-descriptions-item label="分管业务">{{ detailData.managed_scope }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">审计履历</el-divider>
      <el-timeline>
        <el-timeline-item v-for="p in detailData.auditHistory" :key="p.id" :timestamp="p.audit_year + '年'" placement="top">
          <el-card shadow="hover"><p>{{ p.project_name }} ({{ p.audit_type }})</p><p>状态: {{ ['未启动','实施中','已归档'][p.status] }}</p></el-card>
        </el-timeline-item>
        <el-timeline-item v-if="!detailData.auditHistory?.length" timestamp="">暂无审计记录</el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const leaderList = ref([])
const unitList = ref([])
const queryParams = ref({ unitId: null })
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const dialogTitle = ref('')
const form = ref({})
const detailData = ref({})

function getList() {
  loading.value = true
  request({ url: '/audit/info/leader/list', params: queryParams.value }).then(res => {
    leaderList.value = res.rows || res.data || res
    loading.value = false
  })
}

function getUnits() {
  request({ url: '/audit/info/unit/list' }).then(res => { unitList.value = res.rows || res.data || res })
}

function resetQuery() { queryParams.value = { unitId: null }; getList() }
function handleAdd() { form.value = {}; dialogTitle.value = '新增领导干部'; dialogVisible.value = true }
function handleEdit(row) { form.value = { ...row, unitId: row.unit_id, idNumber: row.id_number, tenureStart: row.tenure_start, tenureEnd: row.tenure_end, managedFunds: row.managed_funds, managedScope: row.managed_scope }; dialogTitle.value = '编辑领导干部'; dialogVisible.value = true }

function submitForm() {
  const method = form.value.id ? 'put' : 'post'
  request({ url: '/audit/info/leader', method, data: form.value }).then(() => {
    ElMessage.success('操作成功'); dialogVisible.value = false; getList()
  })
}

function handleDelete(row) {
  ElMessageBox.confirm('确认删除？').then(() => {
    request({ url: '/audit/info/leader/' + row.id, method: 'delete' }).then(() => { ElMessage.success('删除成功'); getList() })
  })
}

function handleDetail(row) {
  detailData.value = { ...row }
  // 查询该领导所在单位的审计项目
  if (row.unit_id) {
    request({ url: '/audit/info/unit/' + row.unit_id + '/profile' }).then(res => {
      detailData.value.auditHistory = res.data?.projects || []
      drawerVisible.value = true
    })
  } else { drawerVisible.value = true }
}

onMounted(() => { getList(); getUnits() })
</script>
