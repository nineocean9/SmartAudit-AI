<template>
  <div class="app-container">
    <el-form :model="q" inline><el-form-item label="项目"><el-select v-model="q.projectId" placeholder="选择项目" clearable>
      <el-option v-for="p in projects" :key="p.id" :label="p.project_name" :value="p.id" /></el-select></el-form-item>
    <el-form-item><el-button type="primary" @click="getList">查询</el-button></el-form-item></el-form>
    <el-button type="primary" @click="openAdd" style="margin-bottom:10px">生成报告</el-button>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="项目" prop="projectName" width="200" />
      <el-table-column label="版本" prop="versionType" width="140" />
      <el-table-column label="状态" width="100"><template #default="s">
        <el-tag :type="s.row.status===2?'success':s.row.status===1?'warning':'info'">{{s.row.status===2?'已审批':s.row.status===1?'待审批':'草稿'}}</el-tag>
      </template></el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="s"><el-button size="small" @click="view(s.row.id)">查看</el-button>
          <el-button size="small" type="primary" v-hasPermi="['audit:report:edit']" @click="openEdit(s.row)">编辑</el-button></template>
      </el-table-column>
    </el-table>
    <el-dialog title="报告内容" v-model="dlgView" width="700px"><pre style="background:#f5f7fa;padding:15px;border-radius:6px;white-space:pre-wrap">{{viewContent}}</pre></el-dialog>
    <el-dialog title="编辑报告" v-model="dlgEdit" width="700px">
      <el-form :model="editForm"><el-form-item label="项目"><el-select v-model="editForm.projectId"><el-option v-for="p in projects" :key="p.id" :label="p.project_name" :value="p.id" /></el-select></el-form-item>
      <el-form-item label="版本"><el-select v-model="editForm.versionType"><el-option label="处内审核稿" value="处内审核稿" /><el-option label="征求意见稿" value="征求意见稿" /><el-option label="正式稿" value="正式稿" /></el-select></el-form-item>
      <el-form-item label="内容"><el-input type="textarea" v-model="editForm.content" :rows="8" /></el-form-item></el-form>
      <template #footer><el-button @click="dlgEdit=false">取消</el-button><el-button type="primary" @click="submitEdit">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref } from 'vue'; import { ops } from '@/api/audit/auditOps'; import { getProgress } from '@/api/audit/auditInfo'
import { ElMessage } from 'element-plus'
const projects=ref([]); const q=ref({}); const list=ref([]); const loading=ref(false)
const dlgView=ref(false); const viewContent=ref('')
const dlgEdit=ref(false); const editForm=ref({})
getProgress().then(r=>{projects.value=r.data||[]})
function getList(){loading.value=true;ops.reportList(q.value.projectId).then(r=>{list.value=r.data||[]}).finally(()=>loading.value=false)}
function view(id){ops.reportInfo(id).then(r=>{viewContent.value=r.data?.content||'';dlgView.value=true})}
function openAdd(){editForm.value={};dlgEdit.value=true}
function openEdit(r){editForm.value={id:r.id,projectId:r.projectId,versionType:r.versionType,content:r.content};dlgEdit.value=true}
function submitEdit(){
  const api=editForm.value.id?ops.editReport(editForm.value):ops.addReport(editForm.value)
  api.then(r=>{if(r.code===200){ElMessage.success('成功');dlgEdit.value=false;getList()}})
}
getList()
</script>
