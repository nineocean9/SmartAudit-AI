<template>
  <div class="file-uploader">
    <el-upload
      ref="uploadRef"
      :action="''"
      :headers="uploadHeaders"
      :before-upload="handleBeforeUpload"
      :show-file-list="true"
      :limit="5"
      :accept="acceptTypes"
      drag
    >
      <el-icon class="upload-icon"><UploadFilled /></el-icon>
      <div class="upload-text">
        <span>拖拽文件到此处 或 <em>点击上传</em></span>
      </div>
      <template #tip>
        <div class="upload-tip">
          支持 Word / PDF / Excel / CSV / TXT / 图片，单文件不超过 10MB
        </div>
      </template>
    </el-upload>

    <!-- 归属选择弹窗 -->
    <el-dialog v-model="showTargetDialog" title="请选择资料归属" width="480px" :close-on-click-modal="false" :destroy-on-close="true">
      <el-form label-width="100px">

        <!-- 归属方式 -->
        <el-form-item label="归属方式">
          <el-radio-group v-model="targetType">
            <el-radio value="project">保存到已有项目</el-radio>
            <el-radio value="newProject">创建新项目</el-radio>
            <el-radio value="temp">临时知识库</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 保存到已有项目 -->
        <template v-if="targetType === 'project'">
          <el-form-item label="所属计划">
            <el-select v-model="selectedPlanId" placeholder="选择计划" filterable style="width:100%">
              <el-option v-for="p in plans" :key="p.id" :label="`${p.planName} (${p.planYear})`" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="所属项目">
            <el-select v-model="selectedProjectId" placeholder="先选择计划" filterable style="width:100%">
              <el-option v-for="p in planProjects" :key="p.id" :label="p.projectName" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="资料类型">
            <el-select v-model="docType" placeholder="选择类型" style="width:100%">
              <el-option v-for="t in docTypes" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 创建新项目 -->
        <template v-if="targetType === 'newProject'">
          <el-form-item label="项目名称" required>
            <el-input v-model="newProjectName" placeholder="如：2026年信息工程学院预算执行审计" />
          </el-form-item>
          <el-form-item label="所属计划" required>
            <el-select v-model="newPlanId" placeholder="选择所属计划" filterable style="width:100%">
              <el-option v-for="p in plans" :key="p.id" :label="`${p.planName} (${p.planYear})`" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="审计类型" required>
            <el-select v-model="newAuditType" placeholder="选择审计类型" style="width:100%">
              <el-option v-for="t in auditTypes" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="被审计单位">
            <el-input v-model="newAuditedUnit" placeholder="如：信息工程学院" />
          </el-form-item>
          <el-form-item label="项目组长/主审">
            <el-input v-model="newLeader" placeholder="项目组长或主审姓名" />
          </el-form-item>
          <el-form-item label="资料类型">
            <el-select v-model="docType" placeholder="选择类型" style="width:100%">
              <el-option v-for="t in docTypes" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 临时知识库 -->
        <template v-if="targetType === 'temp'">
          <el-form-item label="说明">
            <span style="font-size:13px;color:#909399">文件将存入临时知识库，仅本次聊天有效，2小时后自动清理</span>
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="showTargetDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!canSubmit" :loading="submitting" @click="submitUpload">
          {{ submitting ? '处理中...' : '确定上传' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'
import { listPlans, listProjectsByPlan, listDocTypes, listAuditTypes, createProject } from '@/api/ai/workspace'
import { uploadProjectDoc } from '@/api/audit/projectDoc'
import { uploadToTemp } from '@/api/knowledge/tempWorkspace'

const props = defineProps({
  tempSessionId: { type: String, default: '' },
  autoOpen: { type: Boolean, default: false }
})
const emit = defineEmits(['uploadSuccess', 'uploadComplete'])

const uploadRef = ref(null)
const uploadHeaders = ref({ Authorization: 'Bearer ' + getToken() })
const acceptTypes = '.doc,.docx,.pdf,.xls,.xlsx,.csv,.txt,.md,.png,.jpg,.jpeg'

// 归属选择
const showTargetDialog = ref(false)
const targetType = ref('project')
const submitting = ref(false)

// 保存到已有项目的表单
const selectedPlanId = ref(null)
const selectedProjectId = ref(null)

// 创建新项目的表单
const newProjectName = ref('')
const newPlanId = ref(null)
const newAuditType = ref('')
const newAuditedUnit = ref('')
const newLeader = ref('')

// 共用
const docType = ref('其他')
const currentFile = ref(null)

// 数据
const plans = ref([])
const planProjects = ref([])
const docTypes = ref([])
const auditTypes = ref([])

// 加载计划列表
async function loadPlans() {
  try {
    const res = await listPlans()
    if (res.code === 200) plans.value = res.data || []
  } catch { /* ignore */ }
}

// 加载审计类型
async function loadAuditTypes() {
  try {
    const res = await listAuditTypes()
    if (res.code === 200) auditTypes.value = res.data || []
  } catch { /* ignore */ }
}

// 监听计划变化，获取项目
watch(selectedPlanId, async (val) => {
  if (!val) { planProjects.value = []; return }
  try {
    const res = await listProjectsByPlan(val)
    if (res.code === 200) planProjects.value = res.data || []
  } catch { planProjects.value = [] }
})

// 加载资料类型
async function loadDocTypes() {
  try {
    const res = await listDocTypes()
    if (res.code === 200) docTypes.value = res.data || []
  } catch { /* ignore */ }
}

// 是否可以提交
const canSubmit = computed(() => {
  if (!currentFile.value) return false
  switch (targetType.value) {
    case 'project':
      return selectedPlanId.value && selectedProjectId.value && docType.value
    case 'newProject':
      return newProjectName.value && newPlanId.value && newAuditType.value && docType.value
    case 'temp':
      return true
    default:
      return false
  }
})

function handleBeforeUpload(file) {
  currentFile.value = file
  loadPlans()
  loadDocTypes()
  loadAuditTypes()
  showTargetDialog.value = true
  return false // 阻止默认上传
}

async function submitUpload() {
  const file = currentFile.value
  if (!file || submitting.value) return
  submitting.value = true

  try {
    if (targetType.value === 'newProject') {
      // Step 1: 创建项目
      const projRes = await createProject({
        projectName: newProjectName.value,
        planId: newPlanId.value,
        auditType: newAuditType.value,
        auditedUnit: newAuditedUnit.value,
        leader: newLeader.value
      })
      if (projRes.code !== 200) {
        ElMessage.error(projRes.msg || '创建项目失败')
        return
      }
      const newId = projRes.data.id
      ElMessage.success('项目「' + newProjectName.value + '」创建成功')

      // Step 2: 上传文档到新项目
      const formData = new FormData()
      formData.append('file', file)
      formData.append('projectId', newId)
      formData.append('planId', newPlanId.value)
      formData.append('docType', docType.value)
      const docRes = await uploadProjectDoc(formData)
      if (docRes.code === 200) {
        ElMessage.success('文档已上传到新项目')
        emit('uploadSuccess', { target: 'newProject', projectId: newId, projectName: newProjectName.value })
      } else {
        ElMessage.error('文档上传失败: ' + (docRes.msg || ''))
      }

    } else if (targetType.value === 'project') {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('projectId', selectedProjectId.value)
      formData.append('planId', selectedPlanId.value)
      formData.append('docType', docType.value)
      const res = await uploadProjectDoc(formData)
      if (res.code === 200) {
        ElMessage.success('上传成功')
        emit('uploadSuccess', { target: 'project', projectId: selectedProjectId.value })
      } else {
        ElMessage.error(res.msg || '上传失败')
      }

    } else if (targetType.value === 'temp') {
      const res = await uploadToTemp(props.tempSessionId, file)
      if (res.code === 200) {
        ElMessage.success('已存入临时知识库')
        emit('uploadSuccess', { target: 'temp', record: res.data })
      } else {
        ElMessage.error(res.msg || '上传失败')
      }
    }
  } catch (e) {
    ElMessage.error('操作失败: ' + (e.message || ''))
  } finally {
    submitting.value = false
    showTargetDialog.value = false
    resetForm()
    emit('uploadComplete')
  }
}

function resetForm() {
  currentFile.value = null
  selectedPlanId.value = null
  selectedProjectId.value = null
  newProjectName.value = ''
  newPlanId.value = null
  newAuditType.value = ''
  newAuditedUnit.value = ''
  newLeader.value = ''
  docType.value = '其他'
}

// autoOpen: 组件挂载后自动触发文件选择
watch(() => props.autoOpen, (val) => {
  if (val) {
    nextTick(() => {
      const el = uploadRef.value?.$el
      if (el) {
        const input = el.querySelector('input[type=file]')
        if (input) input.click()
      }
    })
  }
}, { immediate: true })
</script>

<style scoped>
.upload-icon { font-size: 28px; color: #c0c4cc; margin-bottom: 6px; }
.upload-text { font-size: 14px; color: #606266; }
.upload-text em { color: #409eff; font-style: normal; }
.upload-tip { font-size: 12px; color: #c0c4cc; margin-top: 4px; }
</style>
