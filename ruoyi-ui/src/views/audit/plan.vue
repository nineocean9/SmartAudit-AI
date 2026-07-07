<template>
  <div class="app-container">
    <el-form :model="q" inline>
      <el-form-item label="类型"><el-select v-model="q.planType" clearable><el-option label="年度计划" value="年度计划" /><el-option label="专项计划" value="专项计划" /><el-option label="临时计划" value="临时计划" /></el-select></el-form-item>
      <el-form-item label="年度"><el-input v-model="q.planYear" placeholder="如2025" clearable style="width:100px" /></el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="success" @click="openAdd">新增计划</el-button>
        <el-button type="warning" @click="exportExcel">导出Excel</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" row-key="id">
      <el-table-column label="计划名称" prop="planName" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" prop="planType" width="100" />
      <el-table-column label="年度" prop="planYear" width="80" />
      <el-table-column label="批次" prop="batch" width="100" />
      <el-table-column label="附件" width="120"><template #default="s">
        <el-link v-if="s.row.fileUrl" type="primary" :href="s.row.fileUrl" target="_blank">查看文件</el-link>
        <span v-else>—</span>
      </template></el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="s">
          <el-button size="small" type="primary" @click="openDetail(s.row)">详情</el-button>
          <el-button size="small" @click="recommend">推荐应审</el-button>
          <el-button size="small" type="danger" @click="del(s.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="form.id?'编辑':'新增'" v-model="dlgAdd" width="600px">
      <el-form :model="form">
        <el-form-item label="计划名称"><el-input v-model="form.planName" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="form.planType"><el-option label="年度计划" value="年度计划" /><el-option label="专项计划" value="专项计划" /><el-option label="临时计划" value="临时计划" /></el-select></el-form-item>
        <el-form-item label="年度"><el-input v-model="form.planYear" /></el-form-item>
        <el-form-item label="批次"><el-input v-model="form.batch" /></el-form-item>
        <el-form-item label="委托书/决议">
          <el-upload :action="uploadUrl" :headers="uploadHeaders" :on-success="onUploadSuccess" :show-file-list="true" :limit="3">
            <el-button type="primary">上传文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dlgAdd=false">取消</el-button><el-button type="primary" @click="submitAdd">确定</el-button></template>
    </el-dialog>

    <el-dialog title="计划详情" v-model="dlgDetail" width="800px">
      <el-tabs>
        <el-tab-pane label="关联项目">
          <el-button size="small" type="primary" style="margin-bottom:8px" @click="openBind">批量绑定项目</el-button>
          <el-table :data="planProjects" size="small">
            <el-table-column label="项目" prop="project_name" /><el-table-column label="单位" prop="audited_unit" width="120" /><el-table-column label="年度" prop="audit_year" width="80" /><el-table-column label="操作" width="80"><template #default="s"><el-button size="small" type="danger" @click="unbind(curPlanId, s.row.project_id)">解绑</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="审计方案">
          <el-table :data="planSchemes" size="small">
            <el-table-column label="方案内容" prop="content" show-overflow-tooltip /><el-table-column label="关联项目" prop="project_name" width="160" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog title="选择项目绑定" v-model="dlgBind" width="600px">
      <el-checkbox-group v-model="selectedProjects">
        <div v-for="p in allProjects" :key="p.id" style="margin:6px 0">
          <el-checkbox :label="p.id">{{p.project_name}}（{{p.audited_unit}} {{p.audit_year}}）</el-checkbox>
        </div>
      </el-checkbox-group>
      <template #footer><el-button @click="dlgBind=false">取消</el-button><el-button type="primary" @click="doBind">绑定选中</el-button></template>
    </el-dialog>

    <el-dialog title="推荐应审单位与领导" v-model="dlgRecommend" width="700px">
      <h4>近2年未审计单位</h4>
      <el-table :data="recUnits" size="small" max-height="200"><el-table-column label="单位" prop="unit_name" /><el-table-column label="类型" prop="unit_type" width="100" /><el-table-column label="历史审计" prop="history_audit" show-overflow-tooltip /></el-table>
      <h4 style="margin-top:12px">任期将满领导</h4>
      <el-table :data="recLeaders" size="small" max-height="200"><el-table-column label="姓名" prop="name" width="100" /><el-table-column label="单位" prop="unit_name" /><el-table-column label="职务" prop="position" width="120" /><el-table-column label="任期至" prop="tenure_end" width="120" /></el-table>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { listPlan, addPlan, delPlan, recommendTargets, bindPlanProject, unbindPlanProject, getPlanProjects, getPlanSchemes, getProgress } from '@/api/audit/auditInfo'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getToken } from '@/utils/auth'

const q = ref({ pageNum:1, pageSize:10, planType:'', planYear:'' })
const list = ref([]); const total = ref(0); const loading = ref(false)
const dlgAdd = ref(false); const form = ref({})
const dlgDetail = ref(false); const curPlanId = ref(null)
const planProjects = ref([]); const planSchemes = ref([])
const dlgBind = ref(false); const allProjects = ref([]); const selectedProjects = ref([])
const dlgRecommend = ref(false); const recUnits = ref([]); const recLeaders = ref([])
const uploadUrl = import.meta.env.VITE_APP_BASE_API + '/common/upload'
const uploadHeaders = { Authorization: 'Bearer ' + getToken() }

function getList() { loading.value=true; listPlan(q.value).then(r=>{list.value=r.rows;total.value=r.total}).finally(()=>loading.value=false) }
function reset() { q.value={pageNum:1,pageSize:10,planType:'',planYear:''}; getList() }
function openAdd() { form.value={}; dlgAdd.value=true }
function onUploadSuccess(res) { if (res.code===200) form.value.fileUrl = res.url || (res.data && res.data.url) }
function submitAdd() { addPlan(form.value).then(r=>{if(r.code===200){ElMessage.success('已创建');dlgAdd.value=false;getList()}}) }
function del(id) { ElMessageBox.confirm('确认删除？').then(()=>delPlan(id).then(r=>{if(r.code===200){ElMessage.success('已删除');getList()}})).catch(()=>{}) }
function openDetail(row) { curPlanId.value=row.id; getPlanProjects(row.id).then(r=>planProjects.value=r.data||[]); getPlanSchemes(row.id).then(r=>planSchemes.value=r.data||[]); dlgDetail.value=true }
function openBind() { getProgress().then(r=>{allProjects.value=r.data||[];selectedProjects.value=[];dlgBind.value=true}) }
function doBind() { const tasks=selectedProjects.value.map(pid=>bindPlanProject(curPlanId.value,pid)); Promise.all(tasks).then(()=>{ElMessage.success('已绑定');dlgBind.value=false;getPlanProjects(curPlanId.value).then(r=>planProjects.value=r.data||[])}) }
function recommend() { recommendTargets().then(r=>{recUnits.value=r.data?.units||[];recLeaders.value=r.data?.leaders||[];dlgRecommend.value=true}) }
function exportExcel() { window.open(import.meta.env.VITE_APP_BASE_API + '/audit/info/plan/export?planType='+q.value.planType+'&planYear='+q.value.planYear, '_blank') }
getList()
</script>
