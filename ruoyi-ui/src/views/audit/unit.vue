<template>
  <div class="app-container">
    <el-row :gutter="10">
      <el-col :span="6">
        <el-input v-model="filterText" placeholder="筛选单位" clearable style="margin-bottom:10px" />
        <div v-loading="loading">
          <div v-for="u in units" :key="u.id" class="unit-item" :class="{active: curUnit?.id===u.id}" @click="selectUnit(u)">
            <strong>{{u.unit_name}}</strong><br><small>{{u.unit_type}}</small>
          </div>
          <el-empty v-if="!loading && units.length===0" description="暂无单位" />
        </div>
        <el-button type="primary" size="small" style="margin-top:10px;width:100%" @click="openAdd">新增单位</el-button>
        <el-button type="danger" size="small" style="margin-top:6px;width:100%" :disabled="!curUnit" @click="handleDelete">删除选中单位</el-button>
      </el-col>
      <el-col :span="18">
        <el-card v-if="curUnit">
          <template #header>{{curUnit.unit_name}} · {{curUnit.unit_type}}</template>
          <el-tabs>
            <el-tab-pane label="基本情况">
              <p><strong>概况：</strong>{{curUnit.profile || '—'}}</p>
              <p><strong>历史审计：</strong>{{curUnit.history_audit || '—'}}</p>
              <el-button size="small" type="primary" @click="dlgEdit=true">编辑</el-button>
            </el-tab-pane>
            <el-tab-pane label="干部任免">
              <el-table :data="leaders" size="small"><el-table-column label="姓名" prop="name" width="100" /><el-table-column label="职务" prop="position" /><el-table-column label="任期起" prop="tenure_start" width="120" /><el-table-column label="任期止" prop="tenure_end" width="120" /></el-table>
              <el-empty v-if="!leaders||leaders.length===0" description="暂无干部记录" />
            </el-tab-pane>
            <el-tab-pane label="历史审计">
              <el-table :data="projects" size="small"><el-table-column label="项目" prop="project_name" /><el-table-column label="类型" prop="audit_type" width="100" /><el-table-column label="年度" prop="audit_year" width="80" /><el-table-column label="状态" width="80"><template #default="s">{{s.row.status===2?'已归档':s.row.status===1?'实施中':'未启动'}}</template></el-table-column></el-table>
              <el-empty v-if="!projects||projects.length===0" description="暂无审计记录" />
            </el-tab-pane>
          </el-tabs>
        </el-card>
        <el-empty v-else description="请从左侧选择单位" />
      </el-col>
    </el-row>

    <el-dialog :title="editForm.id?'编辑单位':'新增单位'" v-model="dlgEdit" width="500px">
      <el-form :model="editForm">
        <el-form-item label="名称"><el-input v-model="editForm.unit_name" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="editForm.unit_type"><el-option label="学院" value="学院" /><el-option label="处室" value="处室" /><el-option label="直属" value="直属" /></el-select></el-form-item>
        <el-form-item label="概况"><el-input type="textarea" v-model="editForm.profile" :rows="3" /></el-form-item>
        <el-form-item label="历史审计"><el-input type="textarea" v-model="editForm.history_audit" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dlgEdit=false">取消</el-button><el-button type="primary" @click="submitEdit">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, watch } from 'vue'
import { listUnit, addUnit, delUnit, getUnitProfile } from '@/api/audit/auditInfo'
import { ElMessage, ElMessageBox } from 'element-plus'
const filterText = ref(''); const loading = ref(false)
const units = ref([]); const curUnit = ref(null)
const leaders = ref([]); const projects = ref([])
const dlgEdit = ref(false); const editForm = ref({})
watch(filterText, (v) => { loadUnits(v) })
function loadUnits(filter='') { loading.value=true; listUnit({unitName:filter}).then(r=>{units.value=r.rows||[]}).finally(()=>loading.value=false) }
function selectUnit(u) { curUnit.value=u; getUnitProfile(u.id).then(r=>{leaders.value=r.data?.leaders||[];projects.value=r.data?.projects||[]}) }
function openAdd() { editForm.value={unit_type:'学院'}; dlgEdit.value=true }
function submitEdit() { addUnit(editForm.value).then(r=>{if(r.code===200){ElMessage.success('已保存');dlgEdit.value=false;loadUnits(filterText.value)}}) }
function handleDelete() {
  if (!curUnit.value) return
  ElMessageBox.confirm('确定要删除单位「' + curUnit.value.unit_name + '」吗？', '提示', { type: 'warning' }).then(() => {
    delUnit(curUnit.value.id).then(r => {
      if (r.code === 200) { ElMessage.success('删除成功'); curUnit.value = null; leaders.value = []; projects.value = []; loadUnits(filterText.value) }
    })
  }).catch(() => {})
}
loadUnits()
</script>
<style scoped>
.unit-item{padding:10px 12px;margin-bottom:6px;border-radius:6px;cursor:pointer;border:1px solid #ebeef5}
.unit-item:hover,.unit-item.active{border-color:#409eff;background:#ecf5ff}
.unit-item strong{font-size:14px}.unit-item small{font-size:12px;color:#909399}
</style>
