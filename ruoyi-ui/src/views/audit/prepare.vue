<template>
  <div class="app-container prepare-page">
    <div class="prepare-header">
      <div>
        <div class="eyebrow">审计准备</div>
        <h2>项目准备工作台</h2>
        <p>围绕项目方案、人员分工和资料清单完成审计进场前协同。被审计单位账号只展示本单位项目。</p>
      </div>
      <el-select
        v-model="projectId"
        filterable
        placeholder="请选择审计项目"
        class="project-select"
        :disabled="projectList.length === 0"
        @change="loadProjectData"
      >
        <el-option v-for="p in projectList" :key="p.id" :label="projectLabel(p)" :value="p.id" />
      </el-select>
    </div>

    <el-empty v-if="projectList.length === 0" description="暂无可参与的审计准备项目" class="empty-panel">
      <div class="empty-help">
        当前账号所属单位还没有被分配到审计项目。请由审计处先创建项目，并把“被审计单位”设置为该账号所在单位。
      </div>
    </el-empty>

    <template v-else>
      <el-row :gutter="16" class="summary-row">
        <el-col :xs="12" :sm="6">
          <div class="summary-card">
            <span>可访问项目</span>
            <strong>{{ projectList.length }}</strong>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="summary-card">
            <span>资料清单</span>
            <strong>{{ materialList.length }}</strong>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="summary-card">
            <span>已提交</span>
            <strong>{{ submittedCount }}</strong>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="summary-card warning">
            <span>待确认</span>
            <strong>{{ pendingConfirmCount }}</strong>
          </div>
        </el-col>
      </el-row>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="项目方案" name="scheme">
          <div class="toolbar" v-if="canEditPrepare">
            <el-button type="primary" @click="openGenerateScheme">从模板生成方案</el-button>
            <el-button type="success" :icon="Upload" @click="openImportScheme">导入方案</el-button>
          </div>
          <el-table :data="schemeList" border>
            <el-table-column label="方案名称" prop="title" min-width="260" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.title || scope.row.content || '未命名方案' }}</template>
            </el-table-column>
            <el-table-column label="模板" prop="template_name" min-width="180" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.template_name || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.status === 1 ? 'success' : 'warning'">
                  {{ scope.row.status === 1 ? '已审定' : '草稿' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" prop="create_time" width="180" />
            <el-table-column label="操作" width="150" align="center">
              <template #default="scope">
                <el-button link type="primary" @click="openSchemeDoc(scope.row, false)">预览</el-button>
                <el-button v-if="canEditPrepare" link type="primary" @click="openSchemeDoc(scope.row, true)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="人员分工" name="member">
          <div class="toolbar" v-if="canManageMembers">
            <el-button type="primary" icon="Plus" @click="openMemberDialog">添加成员</el-button>
          </div>
          <el-table :data="memberList" border>
            <el-table-column label="姓名" prop="userName" width="140" />
            <el-table-column label="角色" prop="roleType" width="130">
              <template #default="scope"><el-tag>{{ scope.row.roleType || '-' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="核查范围" prop="taskScope" show-overflow-tooltip />
            <el-table-column label="交付期限" prop="taskDeadline" width="140" />
            <el-table-column v-if="canManageMembers" label="操作" width="100" align="center">
              <template #default="scope"><el-button link type="danger" @click="delMember(scope.row)">移除</el-button></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="资料清单" name="material">
          <div class="material-guide">
            <div>
              <strong>被审计单位资料待办</strong>
              <p>联络员可上传提交资料，负责人可对已提交资料进行确认；审计组负责发布和维护资料清单。</p>
            </div>
            <el-button v-if="canEditPrepare" type="primary" icon="Plus" @click="openMaterialDialog">添加资料项</el-button>
          </div>

          <el-table :data="materialList" border>
            <el-table-column label="资料名称" prop="materialName" min-width="260" show-overflow-tooltip />
            <el-table-column label="分类" prop="materialType" width="120" />
            <el-table-column label="必需" width="80" align="center">
              <template #default="scope">{{ scope.row.required === 1 ? '是' : '否' }}</template>
            </el-table-column>
            <el-table-column label="来源" width="170" show-overflow-tooltip>
              <template #default="scope">{{ formatMaterialSource(scope.row) }}</template>
            </el-table-column>
            <el-table-column label="提交状态" width="120" align="center">
              <template #default="scope">
                <el-tag :type="materialStatus(scope.row).type">{{ materialStatus(scope.row).label }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提交人" prop="submitBy" width="260" show-overflow-tooltip>
              <template #default="scope">{{ formatSubmitter(scope.row) }}</template>
            </el-table-column>
            <el-table-column label="提交时间" prop="submitTime" width="180">
              <template #default="scope">{{ scope.row.submitTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="320" fixed="right" align="center">
              <template #default="scope">
                <el-button v-if="canViewMaterialFile(scope.row)" link type="primary" @click="openMaterialFile(scope.row)">查看文件</el-button>
                <el-tooltip v-else-if="scope.row.filePath" content="待被审计单位确认，当前无法查看" placement="top">
                  <el-tag type="warning" effect="plain">待单位确认</el-tag>
                </el-tooltip>
                <el-upload
                  v-if="canSubmitPrepare"
                  action="#"
                  :show-file-list="false"
                  :http-request="opt => submitMaterial(scope.row, opt)"
                  class="inline-upload"
                >
                  <el-button link type="primary">{{ scope.row.filePath ? '重新提交' : '提交资料' }}</el-button>
                </el-upload>
                <el-button
                  v-if="canConfirmPrepare && scope.row.submitStatus === 1"
                  link
                  type="success"
                  @click="confirmMaterial(scope.row)"
                >确认</el-button>
                <el-button v-if="canEditPrepare" link type="danger" @click="delMaterial(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="materialList.length === 0" description="暂无资料清单" class="inner-empty">
            <span>等待审计组发布资料需求后，被审计单位可在这里提交材料。</span>
          </el-empty>
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-dialog title="从模板生成方案" v-model="schemeDialog" width="560px" destroy-on-close>
      <el-form :model="schemeForm" label-width="100px">
        <el-form-item label="方案名称">
          <el-input v-model="schemeForm.title" placeholder="请输入方案名称" />
        </el-form-item>
        <el-form-item label="选择模板">
          <el-select v-model="selectedTemplate" @change="onTemplateChange" style="width:100%" filterable>
            <el-option v-for="t in templateList" :key="t.id" :label="t.templateName" :value="t.id">
              <span>{{ t.templateName }}</span>
              <span style="float:right;color:#909399">{{ t.auditType }}</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="schemeDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSchemeFromTemplate">生成并编辑</el-button>
      </template>
    </el-dialog>

    <el-dialog title="导入方案" v-model="importDialog" width="560px" destroy-on-close>
      <el-form :model="schemeForm" label-width="100px">
        <el-form-item label="方案名称">
          <el-input v-model="schemeForm.title" placeholder="请输入方案名称" />
        </el-form-item>
        <el-form-item label="Word文件">
          <el-upload action="#" :http-request="uploadSchemeWord" :limit="1" accept=".docx" drag>
            <el-icon class="el-icon--upload upload-icon"><Upload /></el-icon>
            <div class="el-upload__text">拖拽 Word 文件到此处，或 <em>点击导入</em></div>
            <template #tip><div class="el-upload__tip">支持 .docx，导入后自动打开预览编辑。</div></template>
          </el-upload>
        </el-form-item>
      </el-form>
    </el-dialog>

    <el-dialog title="添加成员" v-model="memberDialog" width="500px">
      <el-form :model="memberForm" label-width="100px">
        <el-form-item label="系统用户">
          <el-select v-model="memberForm.userId" filterable style="width:100%" placeholder="请选择审计人员账号" @change="onMemberUserChange">
            <el-option
              v-for="user in memberCandidates"
              :key="user.userId"
              :label="formatCandidate(user)"
              :value="user.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="memberForm.roleType">
            <el-option label="项目组长/主审" value="项目组长/主审" />
            <el-option label="普通审计人员" value="普通审计人员" />
            <el-option label="中介审计人员" value="中介审计人员" />
          </el-select>
        </el-form-item>
        <el-form-item label="核查范围"><el-input v-model="memberForm.taskScope" type="textarea" /></el-form-item>
        <el-form-item label="交付期限"><el-date-picker v-model="memberForm.taskDeadline" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="memberDialog = false">取消</el-button>
        <el-button type="primary" @click="saveMember">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="添加资料项" v-model="materialDialog" width="500px">
      <el-form :model="materialForm" label-width="100px">
        <el-form-item label="资料名称"><el-input v-model="materialForm.materialName" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="materialForm.materialType">
            <el-option label="财务" value="财务" />
            <el-option label="人事" value="人事" />
            <el-option label="采购" value="采购" />
            <el-option label="资产" value="资产" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否必需"><el-switch v-model="materialForm.required" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="来源">
          <el-radio-group v-model="materialForm.source">
            <el-radio label="unit">被审计单位</el-radio>
            <el-radio label="audit_staff">审计人员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="materialDialog = false">取消</el-button>
        <el-button type="primary" @click="saveMaterial">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { checkPermi, checkRole } from '@/utils/permission'
import { listProjectTree } from '@/api/ai/workspace'
import { cleanupUploadedFile } from '@/api/upload'

const router = useRouter()
const route = useRoute()
const projectId = ref(null)
const activeTab = ref('material')
const projectList = ref([])
const schemeList = ref([])
const memberList = ref([])
const memberCandidates = ref([])
const materialList = ref([])
const templateList = ref([])
const schemeDialog = ref(false)
const importDialog = ref(false)
const memberDialog = ref(false)
const materialDialog = ref(false)
const selectedTemplate = ref(null)
const schemeForm = ref({ title: '' })
const memberForm = ref({})
const materialForm = ref({ required: 1, source: 'unit' })

const canEditPrepare = computed(() => checkPermi(['audit:prepare:edit']))
const canManageMembers = computed(() => checkPermi(['audit:prepare:edit']) && checkRole(['admin', 'audit_director', 'audit_project_leader']))
const canSubmitPrepare = computed(() => checkPermi(['audit:prepare:submit', 'audit:prepare:edit']))
const canConfirmPrepare = computed(() => checkPermi(['audit:prepare:confirm']))
const submittedCount = computed(() => materialList.value.filter(item => Number(item.submitStatus) >= 1).length)
const pendingConfirmCount = computed(() => materialList.value.filter(item => Number(item.submitStatus) === 1).length)
const submitterFallbackMap = {
  audited_unit_liaison: { name: '陈晨（信息工程学院 被审计单位联络员）', dept: '信息工程学院', role: '被审计单位联络员' },
  business_liaison: { name: '钱莹（商学院 被审计单位联络员）', dept: '商学院', role: '被审计单位联络员' },
  logistics_liaison: { name: '郑凯（后勤处 被审计单位联络员）', dept: '后勤处', role: '被审计单位联络员' },
  finance_liaison: { name: '胡敏（财务处 被审计单位联络员）', dept: '财务处', role: '被审计单位联络员' },
  library_liaison: { name: '高远（图书馆 被审计单位联络员）', dept: '图书馆', role: '被审计单位联络员' },
  a_company_liaison: { name: '吴磊（A公司 被审计单位联络员）', dept: 'A公司', role: '被审计单位联络员' }
}

function projectLabel(p) {
  return (p.project_name || p.projectName) + (p.audited_unit ? ' (' + p.audited_unit + ')' : '')
}

function formatMaterialSource(row) {
  const source = row?.source
  if (row?.submitByDept) return row.submitByDept
  const fallback = submitterFallbackMap[row?.submitBy]
  if (fallback?.dept) return fallback.dept
  if (source === 'unit') return '被审计单位'
  if (source === 'audit_staff' || source === 'auditor') return '审计人员'
  return source || '-'
}

function formatSubmitter(row) {
  if (!row?.submitBy && !row?.submitByName) return '-'
  const fallback = submitterFallbackMap[row?.submitBy]
  const name = row.submitByName || fallback?.name || row.submitBy
  const dept = row.submitByDept || fallback?.dept
  const role = row.submitByRole || fallback?.role
  if (dept && role && !String(name).includes('（')) return `${name}（${dept} ${role}）`
  if (dept && !String(name).includes('（')) return `${name}（${dept}）`
  return name
}

function materialStatus(row) {
  const status = Number(row.submitStatus || 0)
  if (status === 2) return { label: '已确认', type: 'success' }
  if (status === 1) return { label: '已提交', type: 'warning' }
  return { label: '未提交', type: 'info' }
}

function formatCandidate(user) {
  return `${user.nickName || user.userName} (${user.roleName || user.roleKey})`
}

function roleTypeFromRoleKey(roleKey) {
  if (roleKey === 'audit_project_leader' || roleKey === 'audit_director') return '项目组长/主审'
  if (roleKey === 'intermediary_auditor') return '中介审计人员'
  return '普通审计人员'
}

function loadProjects() {
  listProjectTree().then(res => {
    const projects = []
    ;(res.data || []).forEach(plan => {
      ;(plan.projects || []).forEach(project => projects.push({
        ...project,
        project_name: project.projectName || project.project_name,
        audited_unit: project.auditedUnit || project.audited_unit
      }))
    })
    projectList.value = projects
    const queryProjectId = route.query.projectId ? Number(route.query.projectId) : null
    const matched = queryProjectId && projects.some(project => Number(project.id) === queryProjectId)
    projectId.value = matched ? queryProjectId : (projects[0]?.id || null)
    if (projectId.value) loadProjectData()
  }).catch(() => {
    projectList.value = []
  })
}

function loadProjectData() {
  if (!projectId.value) return
  syncProjectQuery()
  request({ url: '/audit/ops/scheme/list', params: { projectId: projectId.value } }).then(res => { schemeList.value = res.data || [] })
  request({ url: '/audit/prepare/member/' + projectId.value }).then(res => { memberList.value = res.data || [] })
  request({ url: '/audit/prepare/material/' + projectId.value }).then(res => { materialList.value = res.data || [] })
}

function syncProjectQuery() {
  const currentProjectId = route.query.projectId ? String(route.query.projectId) : ''
  const nextProjectId = String(projectId.value)
  if (currentProjectId === nextProjectId) return
  router.replace({
    path: route.path,
    query: { ...route.query, projectId: nextProjectId }
  })
}

function openGenerateScheme() {
  schemeForm.value = { title: '' }
  selectedTemplate.value = null
  schemeDialog.value = true
  request({ url: '/audit/prepare/template/list' }).then(res => { templateList.value = res.rows || res.data || [] })
}

function onTemplateChange() {
  const t = templateList.value.find(x => x.id === selectedTemplate.value)
  if (t && !schemeForm.value.title) schemeForm.value.title = t.templateName.replace(/模板.*$/, '方案')
}

function saveSchemeFromTemplate() {
  const t = templateList.value.find(x => x.id === selectedTemplate.value)
  if (!t) return ElMessage.warning('请选择模板')
  const title = schemeForm.value.title || t.templateName.replace(/模板.*$/, '方案')
  request({
    url: '/audit/ops/scheme',
    method: 'post',
    data: { projectId: projectId.value, title, content: '[Word方案] ' + title, templateId: t.id, fileUrl: t.fileUrl }
  }).then(res => {
    ElMessage.success('方案已生成')
    schemeDialog.value = false
    loadProjectData()
    const id = res.data?.id || res.id
    openSchemeDoc({ id, title, file_url: t.fileUrl }, true)
  })
}

function openImportScheme() {
  schemeForm.value = { title: '' }
  importDialog.value = true
}

function uploadSchemeWord(opt) {
  if (getFileExt(opt.file.name) !== 'docx') {
    ElMessage.warning('请导入 .docx 文件')
    opt.onError?.()
    return
  }
  const fd = new FormData()
  fd.append('file', opt.file)
  let uploadedPath = ''
  request({ url: '/common/upload', method: 'post', data: fd, headers: { 'Content-Type': 'multipart/form-data' } }).then(res => {
    const fileUrl = res.fileName || res.url
    uploadedPath = fileUrl
    const title = schemeForm.value.title || opt.file.name.replace(/\.docx$/i, '')
    return request({ url: '/audit/ops/scheme', method: 'post', data: { projectId: projectId.value, title, content: '[Word方案] ' + title, fileUrl } }).then(r => ({ r, title, fileUrl }))
  }).then(({ r, title, fileUrl }) => {
    ElMessage.success('方案已导入')
    importDialog.value = false
    opt.onSuccess?.()
    loadProjectData()
    const id = r.data?.id || r.id
    openSchemeDoc({ id, title, file_url: fileUrl }, true)
  }).catch(() => {
    cleanupUploadedFile(uploadedPath)
    ElMessage.error('导入失败')
    opt.onError?.()
  })
}

function openSchemeDoc(row, edit) {
  if (!row.file_url) return ElMessage.warning('该方案没有可预览的 Word 文件')
  const name = getPreviewName(row.file_url, row.title || '项目方案')
  const returnPath = '/audit/prepare?projectId=' + encodeURIComponent(projectId.value)
  router.push('/audit/doc-preview?url=' + encodeURIComponent(row.file_url) + '&name=' + encodeURIComponent(name) + '&schemeId=' + encodeURIComponent(row.id) + '&returnPath=' + encodeURIComponent(returnPath) + (edit ? '&edit=1' : ''))
}

function openMemberDialog() {
  memberForm.value = { status: 1 }
  memberDialog.value = true
  request({ url: '/audit/prepare/member/candidates' }).then(res => {
    memberCandidates.value = res.data || []
  })
}

function onMemberUserChange(userId) {
  const user = memberCandidates.value.find(item => item.userId === userId)
  if (!user) return
  memberForm.value.userName = user.nickName || user.userName
  memberForm.value.roleType = roleTypeFromRoleKey(user.roleKey)
}

function saveMember() {
  if (!memberForm.value.userId) return ElMessage.warning('请选择系统中的审计人员账号')
  memberForm.value.projectId = projectId.value
  request({ url: '/audit/prepare/member', method: 'post', data: memberForm.value }).then(() => {
    ElMessage.success('添加成功')
    memberDialog.value = false
    loadProjectData()
  })
}

function delMember(row) {
  ElMessageBox.confirm('确认移除该成员？').then(() => {
    request({ url: '/audit/prepare/member/' + row.id, method: 'delete' }).then(() => {
      ElMessage.success('已移除')
      loadProjectData()
    })
  })
}

function openMaterialDialog() {
  materialForm.value = { required: 1, source: 'unit' }
  materialDialog.value = true
}

function saveMaterial() {
  materialForm.value.projectId = projectId.value
  request({ url: '/audit/prepare/material', method: 'post', data: materialForm.value }).then(() => {
    ElMessage.success('添加成功')
    materialDialog.value = false
    loadProjectData()
  })
}

function submitMaterial(row, opt) {
  const fd = new FormData()
  fd.append('file', opt.file)
  let uploadedPath = ''
  request({ url: '/common/upload', method: 'post', data: fd, headers: { 'Content-Type': 'multipart/form-data' } }).then(res => {
    const filePath = res.fileName || res.url
    uploadedPath = filePath
    return request({ url: '/audit/prepare/material/' + row.id + '/submit', method: 'put', data: { filePath } })
  }).then(() => {
    ElMessage.success('资料已提交')
    opt.onSuccess?.()
    loadProjectData()
  }).catch(() => {
    cleanupUploadedFile(uploadedPath)
    ElMessage.error('资料提交失败')
    opt.onError?.()
  })
}

function canViewMaterialFile(row) {
  if (!row.filePath) return false
  const status = Number(row.submitStatus || 0)
  return status === 2 || canConfirmPrepare.value
}

function openMaterialFile(row) {
  if (!row.filePath) return
  if (!canViewMaterialFile(row)) {
    ElMessage.warning('待被审计单位确认，当前无法查看')
    return
  }
  const ext = getFileExt(row.filePath)
  if (ext === 'docx' || ext === 'pdf') {
    const name = getPreviewName(row.filePath, row.materialName || '资料文件')
    const returnPath = '/audit/prepare?projectId=' + encodeURIComponent(projectId.value)
    router.push('/audit/doc-preview?url=' + encodeURIComponent(row.filePath) + '&name=' + encodeURIComponent(name) + '&returnPath=' + encodeURIComponent(returnPath))
    return
  }
  window.open((import.meta.env.VITE_APP_BASE_API || '') + row.filePath, '_blank')
}

function confirmMaterial(row) {
  ElMessageBox.confirm('确认该资料已经完整提交？').then(() => {
    request({ url: '/audit/prepare/material/' + row.id + '/confirm', method: 'put' }).then(() => {
      ElMessage.success('资料已确认')
      loadProjectData()
    })
  })
}

function delMaterial(row) {
  ElMessageBox.confirm('确认删除该资料项？').then(() => {
    request({ url: '/audit/prepare/material/' + row.id, method: 'delete' }).then(() => {
      ElMessage.success('已删除')
      loadProjectData()
    })
  })
}

function getFileExt(name) {
  const cleanName = decodeURIComponent(String(name || '').split('?')[0])
  return cleanName.includes('.') ? cleanName.split('.').pop().toLowerCase() : ''
}

function getPreviewName(fileUrl, fallback) {
  const ext = getFileExt(fileUrl)
  const fallbackName = fallback || '文档'
  if (!ext) return fallbackName
  return fallbackName.toLowerCase().endsWith('.' + ext) ? fallbackName : fallbackName + '.' + ext
}

onMounted(loadProjects)
</script>

<style scoped>
.prepare-page {
  color: #1f2d3d;
}

.prepare-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 22px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #dfe6f0;
  border-radius: 6px;
}

.prepare-header h2 {
  margin: 4px 0 8px;
  font-size: 24px;
  line-height: 1.25;
  color: #0f2742;
}

.prepare-header p {
  margin: 0;
  color: #64748b;
}

.eyebrow {
  font-size: 13px;
  font-weight: 700;
  color: #0f7897;
}

.project-select {
  width: 360px;
  max-width: 100%;
}

.summary-row {
  margin-bottom: 16px;
}

.summary-card {
  padding: 16px;
  background: #fff;
  border: 1px solid #dfe6f0;
  border-radius: 6px;
}

.summary-card span {
  display: block;
  color: #64748b;
  margin-bottom: 8px;
}

.summary-card strong {
  font-size: 26px;
  color: #0f2742;
}

.summary-card.warning strong {
  color: #d97706;
}

.toolbar {
  margin-bottom: 12px;
}

.material-guide {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 14px 16px;
  margin-bottom: 12px;
  background: #f8fafc;
  border: 1px solid #dfe6f0;
  border-radius: 6px;
}

.material-guide p {
  margin: 6px 0 0;
  color: #64748b;
}

.inline-upload {
  display: inline-block;
  margin-left: 8px;
}

.empty-panel,
.inner-empty {
  padding: 36px 0;
  background: #fff;
  border: 1px solid #dfe6f0;
  border-radius: 6px;
}

.empty-help {
  max-width: 520px;
  color: #64748b;
  line-height: 1.7;
}

.upload-icon {
  font-size: 40px;
  color: #909399;
}

@media (max-width: 768px) {
  .prepare-header,
  .material-guide {
    display: block;
  }

  .project-select {
    width: 100%;
    margin-top: 14px;
  }
}
</style>
