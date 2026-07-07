<template>
  <div class="app-container">
    <el-button type="primary" @click="dlgAdd=true" style="margin-bottom:10px">生成取证单</el-button>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="审计问题" prop="issue" show-overflow-tooltip />
      <el-table-column label="引用依据" prop="basisIds" width="120" />
      <el-table-column label="状态" width="100"><template #default="s"><el-tag :type="s.row.reviewStatus===0?'info':'warning'">{{s.row.reviewStatus===0?'草稿':'复核中'}}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="120" fixed="right"><template #default="s"><el-button size="small" type="danger" @click="del(s.row.id)">删除</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="q.pageNum" v-model:limit="q.pageSize" @pagination="getList" />
    <el-dialog title="生成取证单" v-model="dlgAdd" width="500px">
      <el-form :model="f"><el-form-item label="问题描述"><el-input type="textarea" v-model="f.issue" :rows="4" /></el-form-item>
      <el-form-item label="依据ID"><el-input v-model="f.basisIds" placeholder="逗号分隔" /></el-form-item></el-form>
      <template #footer><el-button @click="dlgAdd=false">取消</el-button><el-button type="primary" @click="submitAdd">生成</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref } from 'vue'; import request from '@/utils/request'; import { ElMessage, ElMessageBox } from 'element-plus'
const q=ref({pageNum:1,pageSize:10}); const list=ref([]); const total=ref(0); const loading=ref(false)
const dlgAdd=ref(false); const f=ref({})
function getList(){loading.value=true;request({url:'/ai/forensic/list',params:q.value}).then(r=>{list.value=r.rows;total.value=r.total}).finally(()=>loading.value=false)}
function submitAdd(){request({url:'/ai/forensic/generate',method:'post',params:f.value}).then(r=>{if(r.code===200){ElMessage.success('已生成');dlgAdd.value=false;getList()}})}
function del(id){ElMessageBox.confirm('确认删除？').then(()=>request({url:'/ai/forensic/'+id,method:'delete'}).then(r=>{if(r.code===200){ElMessage.success('已删除');getList()}})).catch(()=>{})}
getList()
</script>
