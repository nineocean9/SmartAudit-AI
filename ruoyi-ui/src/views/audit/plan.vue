<template>
  <div class="app-container">
    <!-- Advanced search panel -->
    <el-form :model="queryParams" :inline="true" ref="queryRef">
      <el-form-item label="计划类型" prop="planType">
        <el-select v-model="queryParams.planType" clearable placeholder="全部" @change="getList">
          <el-option label="年度" value="年度"/><el-option label="专项" value="专项"/><el-option label="临时" value="临时"/>
        </el-select>
      </el-form-item>
      <el-form-item label="年度" prop="planYear">
        <el-input-number v-model="queryParams.planYear" :min="2020" :max="2030" controls-position="right" clearable @change="getList" style="width:120px"/>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="全部" @change="getList">
          <el-option label="草稿" :value="0"/><el-option label="已发布" :value="1"/>
        </el-select>
      </el-form-item>
      <el-form-item label="审批状态" prop="approvalStatus">
        <el-select v-model="queryParams.approvalStatus" clearable placeholder="全部" @change="getList">
          <el-option label="待审批" :value="0"/><el-option label="已通过" :value="1"/><el-option label="已驳回" :value="2"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- Toolbar -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" icon="Plus" @click="handleAdd">新增计划</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" icon="Upload" @click="handleImport">导入</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" icon="Download" @click="handleExport">导出</el-button></el-col>
    </el-row>

    <!-- Table -->
    <el-table v-loading="loading" :data="planList" @row-click="handleDetail" row-key="id" border>
      <el-table-column label="计划名称" prop="planName" min-width="200" show-overflow-tooltip/>
      <el-table-column label="计划类型" prop="planType" width="80" align="center"/>
      <el-table-column label="年度" prop="planYear" width="60" align="center"/>
      <el-table-column label="批次" prop="batch" width="80" align="center"/>
      <el-table-column label="描述" prop="description" min-width="150" show-overflow-tooltip/>
      <el-table-column label="起止日期" width="200" align="center">
        <template #default="scope">{{ scope.row.planStartDate || '-' }} ~ {{ scope.row.planEndDate || '-' }}</template>
      </el-table-column>
      <el-table-column label="审批状态" width="100" align="center">
        <template #default="scope">
          <el-tag :type="['warning','success','danger'][scope.row.approvalStatus||0]">
            {{ ['待审批','已通过','已驳回'][scope.row.approvalStatus||0] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click.stop="handleEdit(scope.row)">编辑</el-button>
          <el-button link type="primary" icon="View" @click.stop="viewProjects(scope.row)">项目</el-button>
          <el-button link type="primary" @click.stop="recommend">推荐</el-button>
          <el-button link type="danger" icon="Delete" @click.stop="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList"/>

    <!-- Add/Edit Dialog with Tabs -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="720px" destroy-on-close>
      <el-tabs v-model="editTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
            <el-form-item label="计划名称" prop="planName">
              <el-input v-model="form.planName" placeholder="请输入计划名称"/>
            </el-form-item>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="计划类型" prop="planType">
                  <el-select v-model="form.planType" style="width:100%">
                    <el-option label="年度" value="年度"/><el-option label="专项" value="专项"/><el-option label="临时" value="临时"/>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="计划年度" prop="planYear">
                  <el-input-number v-model="form.planYear" :min="2020" :max="2030" controls-position="right" style="width:100%"/>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="批次" prop="batch">
                  <el-input v-model="form.batch" placeholder="如：第一批"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="状态" prop="status">
                  <el-select v-model="form.status" style="width:100%">
                    <el-option label="草稿" :value="0"/><el-option label="已发布" :value="1"/>
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="开始日期" prop="planStartDate">
                  <el-date-picker v-model="form.planStartDate" type="date" value-format="YYYY-MM-DD" placeholder="选择开始日期" style="width:100%"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="结束日期" prop="planEndDate">
                  <el-date-picker v-model="form.planEndDate" type="date" value-format="YYYY-MM-DD" placeholder="选择结束日期" style="width:100%"/>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="描述" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入计划描述"/>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="附件" name="attachment" v-if="form.id">
          <el-upload
            action="#"
            :http-request="uploadAttachment"
            :file-list="attachmentList"
            :on-remove="removeAttachment"
          >
            <el-button type="primary" icon="Upload" size="small">上传附件</el-button>
            <template #tip><div class="el-upload__tip">支持 PDF/Word/Excel/图片等格式，单个文件不超过 20MB</div></template>
          </el-upload>
          <el-table :data="attachmentList" size="small" style="margin-top:12px" v-if="attachmentList.length">
            <el-table-column label="文件名" prop="name" show-overflow-tooltip/>
            <el-table-column label="类型" prop="fileType" width="80" align="center"/>
            <el-table-column label="大小" width="100" align="center">
              <template #default="scope">{{ formatFileSize(scope.row.fileSize) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button link type="primary" @click="previewAttachment(scope.row)">查看</el-button>
                <el-button link type="danger" @click="removeAttachment(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="变更记录" name="changelog" v-if="form.id">
          <el-timeline>
            <el-timeline-item v-for="log in changeLogs" :key="log.id" :timestamp="log.changeTime" placement="top">
              <el-card shadow="hover">
                <p><b>{{ log.changeType }}</b> — {{ log.changeReason || '无说明' }}</p>
                <p style="color:#999;font-size:12px">操作人: {{ log.changeBy }}</p>
              </el-card>
            </el-timeline-item>
            <el-timeline-item v-if="!changeLogs.length" timestamp="">暂无变更记录</el-timeline-item>
          </el-timeline>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- Projects Drawer -->
    <el-drawer v-model="projectDrawer" :title="(currentPlan?.planName || '') + ' - 关联项目'" size="650px">
      <el-button size="small" type="primary" style="margin-bottom:12px" icon="Plus" @click="openBind">批量绑定项目</el-button>
      <el-table :data="planProjects" size="small" border>
        <el-table-column label="项目名称" prop="project_name" min-width="180">
          <template #default="scope">
            <el-link type="primary" @click="navigateToProject(scope.row)">{{ scope.row.project_name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="被审单位" prop="audited_unit" width="120"/>
        <el-table-column label="年度" prop="audit_year" width="80" align="center"/>
        <el-table-column label="状态" width="80" align="center">
          <template #default="scope">
            <el-tag :type="['info','','success'][scope.row.status]" size="small">
              {{ ['未启动','实施中','已归档'][scope.row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="scope">
            <el-button link type="danger" size="small" @click="unbind(currentPlan.id, scope.row.project_id)">解绑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- Bind projects dialog -->
    <el-dialog title="选择项目绑定" v-model="dlgBind" width="600px">
      <el-checkbox-group v-model="selectedProjects">
        <div v-for="p in allProjects" :key="p.id" style="margin:6px 0">
          <el-checkbox :label="p.id">{{ p.project_name }}（{{ p.audited_unit }} {{ p.audit_year }}）</el-checkbox>
        </div>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="dlgBind=false">取消</el-button>
        <el-button type="primary" @click="doBind">绑定选中</el-button>
      </template>
    </el-dialog>

    <!-- Recommend dialog -->
    <el-dialog title="推荐应审单位与领导" v-model="dlgRecommend" width="700px">
      <h4>近2年未审计单位</h4>
      <el-table :data="recUnits" size="small" max-height="200" border>
        <el-table-column label="单位" prop="unit_name"/><el-table-column label="类型" prop="unit_type" width="100"/>
        <el-table-column label="历史审计" prop="history_audit" show-overflow-tooltip/>
      </el-table>
      <h4 style="margin-top:12px">任期将满领导</h4>
      <el-table :data="recLeaders" size="small" max-height="200" border>
        <el-table-column label="姓名" prop="name" width="100"/><el-table-column label="单位" prop="unit_name"/>
        <el-table-column label="职务" prop="position" width="120"/><el-table-column label="任期至" prop="tenure_end" width="120"/>
      </el-table>
    </el-dialog>

    <!-- Import dialog -->
    <el-dialog title="导入审计计划" v-model="importDialogVisible" width="400px" destroy-on-close>
      <el-upload
        ref="importRef"
        action="#"
        :http-request="submitImport"
        :limit="1"
        accept=".xlsx,.xls"
        drag
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip><div class="el-upload__tip">仅支持 .xlsx / .xls 格式文件</div></template>
      </el-upload>
      <template #tip>
        <el-link type="primary" @click="downloadTemplate" style="margin-top:10px">下载导入模板</el-link>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPlan, addPlan, delPlan, recommendTargets, bindPlanProject, unbindPlanProject, getPlanProjects, getPlanSchemes, getProgress } from '@/api/audit/auditInfo'
import { getToken } from '@/utils/auth'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const planList = ref([])
const total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 10 })
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editTab = ref('basic')
const form = ref({})
const formRef = ref(null)
const attachmentList = ref([])
const changeLogs = ref([])
const projectDrawer = ref(false)
const currentPlan = ref(null)
const planProjects = ref([])
const importDialogVisible = ref(false)

// Bind dialog
const dlgBind = ref(false)
const allProjects = ref([])
const selectedProjects = ref([])

// Recommend dialog
const dlgRecommend = ref(false)
const recUnits = ref([])
const recLeaders = ref([])

const rules = {
  planName: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  planType: [{ required: true, message: '请选择计划类型', trigger: 'change' }],
  planYear: [{ required: true, message: '请输入计划年度', trigger: 'blur' }]
}

function getList() {
  loading.value = true
  listPlan(queryParams.value).then(res => {
    planList.value = res.rows || res.data || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function resetQuery() {
  queryParams.value = { pageNum: 1, pageSize: 10 }
  getList()
}

function handleAdd() {
  form.value = { planYear: new Date().getFullYear(), status: 0, approvalStatus: 0 }
  editTab.value = 'basic'
  dialogTitle.value = '新增计划'
  attachmentList.value = []
  changeLogs.value = []
  dialogVisible.value = true
}

function handleEdit(row) {
  form.value = { ...row }
  editTab.value = 'basic'
  dialogTitle.value = '编辑计划'
  dialogVisible.value = true
  if (row.id) {
    loadAttachments(row.id)
    loadChangeLogs(row.id)
  }
}

function loadAttachments(planId) {
  request({ url: '/audit/info/plan/' + planId + '/attachments' }).then(res => {
    attachmentList.value = (res.data || []).map(a => ({
      name: a.fileName, url: a.filePath, id: a.id, fileType: a.fileType, fileSize: a.fileSize
    }))
  }).catch(() => { attachmentList.value = [] })
}

function loadChangeLogs(planId) {
  request({ url: '/audit/info/plan/' + planId + '/changeLogs' }).then(res => {
    changeLogs.value = res.data || []
  }).catch(() => { changeLogs.value = [] })
}

function submitForm() {
  formRef.value?.validate(valid => {
    if (!valid) return
    const method = form.value.id ? 'put' : 'post'
    request({ url: '/audit/info/plan', method, data: form.value }).then(() => {
      ElMessage.success('操作成功')
      dialogVisible.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  ElMessageBox.confirm('确认删除该计划吗？', '提示', { type: 'warning' }).then(() => {
    delPlan(row.id).then(res => {
      if (res.code === 200) { ElMessage.success('删除成功'); getList() }
    })
  }).catch(() => {})
}

function handleDetail(row) {
  handleEdit(row)
}

function viewProjects(row) {
  currentPlan.value = row
  getPlanProjects(row.id).then(res => {
    planProjects.value = res.data || []
    projectDrawer.value = true
  })
}

function navigateToProject(project) {
  router.push({ path: '/audit/project', query: { id: project.project_id || project.id } })
}

function openBind() {
  getProgress().then(r => {
    allProjects.value = r.data || []
    selectedProjects.value = []
    dlgBind.value = true
  })
}

function doBind() {
  const tasks = selectedProjects.value.map(pid => bindPlanProject(currentPlan.value.id, pid))
  Promise.all(tasks).then(() => {
    ElMessage.success('已绑定')
    dlgBind.value = false
    getPlanProjects(currentPlan.value.id).then(r => { planProjects.value = r.data || [] })
  })
}

function unbind(planId, projectId) {
  ElMessageBox.confirm('确认解绑该项目？').then(() => {
    unbindPlanProject(planId, projectId).then(() => {
      ElMessage.success('已解绑')
      getPlanProjects(planId).then(r => { planProjects.value = r.data || [] })
    })
  }).catch(() => {})
}

function recommend() {
  recommendTargets().then(r => {
    recUnits.value = r.data?.units || []
    recLeaders.value = r.data?.leaders || []
    dlgRecommend.value = true
  })
}

function handleExport() {
  const params = new URLSearchParams()
  Object.entries(queryParams.value).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') params.append(k, v) })
  window.open(import.meta.env.VITE_APP_BASE_API + '/audit/info/plan/export?' + params.toString(), '_blank')
}

function handleImport() {
  importDialogVisible.value = true
}

function submitImport(opt) {
  const fd = new FormData()
  fd.append('file', opt.file)
  request({
    url: '/audit/info/plan/import',
    method: 'post',
    data: fd,
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(res => {
    ElMessage.success('导入成功，共导入 ' + (res.data?.count || 0) + ' 条')
    importDialogVisible.value = false
    getList()
  }).catch(() => {
    ElMessage.error('导入失败，请检查文件格式')
  })
}

function downloadTemplate() {
  window.open(import.meta.env.VITE_APP_BASE_API + '/audit/info/plan/importTemplate', '_blank')
}

function uploadAttachment(opt) {
  const fd = new FormData()
  fd.append('file', opt.file)
  request({
    url: '/common/upload',
    method: 'post',
    data: fd,
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(res => {
    request({
      url: '/audit/info/plan/attachment',
      method: 'post',
      data: {
        planId: form.value.id,
        fileName: opt.file.name,
        filePath: res.fileName || res.url,
        fileType: opt.file.name.split('.').pop(),
        fileSize: opt.file.size
      }
    }).then(() => {
      ElMessage.success('上传成功')
      loadAttachments(form.value.id)
    })
  })
}

function removeAttachment(file) {
  if (file.id) {
    ElMessageBox.confirm('确认删除该附件？').then(() => {
      request({ url: '/audit/info/plan/attachment/' + file.id, method: 'delete' }).then(() => {
        ElMessage.success('删除成功')
        loadAttachments(form.value.id)
      })
    }).catch(() => {})
  }
}

function previewAttachment(file) {
  if (file.url) {
    window.open(import.meta.env.VITE_APP_BASE_API + file.url, '_blank')
  }
}

function formatFileSize(size) {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(1) + ' MB'
}

onMounted(() => getList())
</script>
