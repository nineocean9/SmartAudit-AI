<template>
  <div class="app-container">
    <el-form :inline="true">
      <el-form-item label="选择项目">
        <el-select v-model="projectId" filterable placeholder="请选择审计项目" @change="loadProjectData" style="width:300px">
          <el-option v-for="p in projectList" :key="p.id" :label="(p.project_name||p.projectName)+' ('+(p.audited_unit||'')+')'" :value="p.id"/>
        </el-select>
      </el-form-item>
    </el-form>
    <el-tabs v-model="activeTab" v-if="projectId">
      <el-tab-pane label="项目方案" name="scheme">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5"><el-button type="primary" @click="generateScheme">从模板生成方案</el-button></el-col>
        </el-row>
        <el-table :data="schemeList">
          <el-table-column label="方案内容" prop="content" show-overflow-tooltip/>
          <el-table-column label="模板" prop="template_id" width="100"/>
          <el-table-column label="状态" width="80">
            <template #default="scope"><el-tag :type="scope.row.status===1?'success':'warning'">{{ scope.row.status===1?'已审批':'草稿' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="创建时间" prop="create_time" width="180"/>
        </el-table>
        <el-dialog title="从模板生成方案" v-model="schemeDialog" width="700px">
          <el-form-item label="选择模板">
            <el-select v-model="selectedTemplate" @change="loadTemplate" style="width:100%">
              <el-option v-for="t in templateList" :key="t.id" :label="t.templateName+' ('+t.auditType+')'" :value="t.id"/>
            </el-select>
          </el-form-item>
          <el-input v-model="schemeContent" type="textarea" :rows="15" placeholder="方案内容" style="margin-top:10px"/>
          <template #footer>
            <el-button @click="schemeDialog=false">取消</el-button>
            <el-button type="primary" @click="saveScheme">保存方案</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
      <el-tab-pane label="人员分工" name="member">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5"><el-button type="primary" icon="Plus" @click="memberDialog=true;memberForm={}">添加成员</el-button></el-col>
        </el-row>
        <el-table :data="memberList">
          <el-table-column label="姓名" prop="userName" width="100"/>
          <el-table-column label="角色" prop="roleType" width="80">
            <template #default="scope"><el-tag :type="{'组长':'danger','主审':'warning','成员':''}[scope.row.roleType]||''">{{ scope.row.roleType }}</el-tag></template>
          </el-table-column>
          <el-table-column label="核查范围" prop="taskScope" show-overflow-tooltip/>
          <el-table-column label="交付时限" prop="taskDeadline" width="120"/>
          <el-table-column label="状态" width="80">
            <template #default="scope"><el-tag :type="scope.row.status===1?'success':'info'">{{ scope.row.status===1?'已完成':'进行中' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="scope"><el-button link type="danger" @click="delMember(scope.row)">移除</el-button></template>
          </el-table-column>
        </el-table>
        <el-dialog title="添加成员" v-model="memberDialog" width="500px">
          <el-form :model="memberForm" label-width="100px">
            <el-form-item label="用户名"><el-input v-model="memberForm.userName"/></el-form-item>
            <el-form-item label="角色">
              <el-select v-model="memberForm.roleType"><el-option label="组长" value="组长"/><el-option label="主审" value="主审"/><el-option label="成员" value="成员"/></el-select>
            </el-form-item>
            <el-form-item label="核查范围"><el-input v-model="memberForm.taskScope" type="textarea"/></el-form-item>
            <el-form-item label="交付时限"><el-date-picker v-model="memberForm.taskDeadline" type="date" value-format="YYYY-MM-DD"/></el-form-item>
          </el-form>
          <template #footer><el-button @click="memberDialog=false">取消</el-button><el-button type="primary" @click="saveMember">确定</el-button></template>
        </el-dialog>
      </el-tab-pane>
      <el-tab-pane label="资料清单" name="material">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5"><el-button type="primary" icon="Plus" @click="materialDialog=true;materialForm={required:1,source:'unit'}">添加资料项</el-button></el-col>
        </el-row>
        <el-table :data="materialList">
          <el-table-column label="资料名称" prop="materialName" width="250"/>
          <el-table-column label="分类" prop="materialType" width="100"/>
          <el-table-column label="必须" width="60"><template #default="scope">{{ scope.row.required===1?'是':'否' }}</template></el-table-column>
          <el-table-column label="提交状态" width="100">
            <template #default="scope"><el-tag :type="['danger','warning','success'][scope.row.submitStatus]">{{ ['未提交','已提交','已审核'][scope.row.submitStatus] }}</el-tag></template>
          </el-table-column>
          <el-table-column label="来源" prop="source" width="80"/>
          <el-table-column label="操作" width="80">
            <template #default="scope"><el-button link type="danger" @click="delMaterial(scope.row)">删除</el-button></template>
          </el-table-column>
        </el-table>
        <el-dialog title="添加资料项" v-model="materialDialog" width="500px">
          <el-form :model="materialForm" label-width="100px">
            <el-form-item label="资料名称"><el-input v-model="materialForm.materialName"/></el-form-item>
            <el-form-item label="分类">
              <el-select v-model="materialForm.materialType"><el-option label="财务" value="财务"/><el-option label="人事" value="人事"/><el-option label="采购" value="采购"/><el-option label="资产" value="资产"/><el-option label="其他" value="其他"/></el-select>
            </el-form-item>
            <el-form-item label="是否必须"><el-switch v-model="materialForm.required" :active-value="1" :inactive-value="0"/></el-form-item>
            <el-form-item label="来源">
              <el-radio-group v-model="materialForm.source"><el-radio label="unit">被审单位</el-radio><el-radio label="auditor">审计人员</el-radio></el-radio-group>
            </el-form-item>
          </el-form>
          <template #footer><el-button @click="materialDialog=false">取消</el-button><el-button type="primary" @click="saveMaterial">确定</el-button></template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
    <el-empty v-else description="请先选择审计项目"/>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const projectId = ref(null)
const activeTab = ref('scheme')
const projectList = ref([])
const schemeList = ref([])
const memberList = ref([])
const materialList = ref([])
const templateList = ref([])
const schemeDialog = ref(false)
const memberDialog = ref(false)
const materialDialog = ref(false)
const selectedTemplate = ref(null)
const schemeContent = ref('')
const memberForm = ref({})
const materialForm = ref({ required: 1, source: 'unit' })

function loadProjects() {
  request({ url: '/audit/info/progress' }).then(res => { projectList.value = res.data || [] })
}
function loadProjectData() {
  if (!projectId.value) return
  request({ url: '/audit/ops/scheme/list', params: { projectId: projectId.value } }).then(res => { schemeList.value = res.rows || res.data || [] })
  request({ url: '/audit/prepare/member/' + projectId.value }).then(res => { memberList.value = res.data || [] })
  request({ url: '/audit/prepare/material/' + projectId.value }).then(res => { materialList.value = res.data || [] })
}
function generateScheme() {
  schemeDialog.value = true; schemeContent.value = ''
  request({ url: '/audit/prepare/template/list' }).then(res => { templateList.value = res.rows || res.data || [] })
}
function loadTemplate() {
  const t = templateList.value.find(x => x.id === selectedTemplate.value)
  if (t) schemeContent.value = t.content
}
function saveScheme() {
  request({ url: '/audit/ops/scheme', method: 'post', data: { projectId: projectId.value, content: schemeContent.value, templateId: selectedTemplate.value } }).then(() => {
    ElMessage.success('方案已保存'); schemeDialog.value = false; loadProjectData()
  })
}
function saveMember() {
  memberForm.value.projectId = projectId.value
  request({ url: '/audit/prepare/member', method: 'post', data: memberForm.value }).then(() => {
    ElMessage.success('添加成功'); memberDialog.value = false; loadProjectData()
  })
}
function delMember(row) {
  ElMessageBox.confirm('确认移除？').then(() => {
    request({ url: '/audit/prepare/member/' + row.id, method: 'delete' }).then(() => { ElMessage.success('已移除'); loadProjectData() })
  })
}
function saveMaterial() {
  materialForm.value.projectId = projectId.value
  request({ url: '/audit/prepare/material', method: 'post', data: materialForm.value }).then(() => {
    ElMessage.success('添加成功'); materialDialog.value = false; loadProjectData()
  })
}
function delMaterial(row) {
  ElMessageBox.confirm('确认删除？').then(() => {
    request({ url: '/audit/prepare/material/' + row.id, method: 'delete' }).then(() => { ElMessage.success('已删除'); loadProjectData() })
  })
}
onMounted(() => loadProjects())
</script>
