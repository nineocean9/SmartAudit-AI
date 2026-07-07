<template>
  <div class="app-container">
    <el-form :model="q" inline><el-form-item label="分类"><el-select v-model="q.category" clearable><el-option label="法规" value="法规"/><el-option label="制度" value="制度"/><el-option label="标准" value="标准"/></el-select></el-form-item>
    <el-form-item label="关键词"><el-input v-model="q.title" placeholder="搜索标题" clearable /></el-form-item>
    <el-form-item><el-button type="primary" @click="getList">查询</el-button><el-button @click="q={};getList()">重置</el-button></el-form-item></el-form>
    <el-row style="margin-bottom:10px"><el-button type="primary" @click="openAdd" v-hasPermi="['ai:basis:add']">新增</el-button></el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="标题" prop="title" width="250" show-overflow-tooltip />
      <el-table-column label="分类" prop="category" width="80" />
      <el-table-column label="颁发单位" prop="issueOrg" width="120" />
      <el-table-column label="状态" width="80"><template #default="s"><el-tag :type="s.row.status===1?'success':'info'">{{s.row.status===1?'生效':'失效'}}</el-tag></template></el-table-column>
      <el-table-column label="生效日期" prop="effectiveDate" width="100" />
      <el-table-column label="操作" width="200" fixed="right"><template #default="s">
        <el-button size="small" @click="openEdit(s.row)" v-hasPermi="['ai:basis:edit']">编辑</el-button>
        <el-button size="small" type="danger" @click="del(s.row.id)" v-hasPermi="['ai:basis:remove']">删除</el-button>
      </template></el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="q.pageNum" v-model:limit="q.pageSize" @pagination="getList" />
    <el-dialog :title="f.id?'编辑':'新增'" v-model="dlg" width="600px">
      <el-form :model="f"><el-form-item label="标题"><el-input v-model="f.title" /></el-form-item>
      <el-form-item label="分类"><el-select v-model="f.category"><el-option label="法规" value="法规"/><el-option label="制度" value="制度"/><el-option label="标准" value="标准"/></el-select></el-form-item>
      <el-form-item label="颁发单位"><el-input v-model="f.issueOrg" /></el-form-item>
      <el-form-item label="内容"><el-input type="textarea" v-model="f.content" :rows="4" /></el-form-item>
      <el-form-item label="生效日期"><el-date-picker v-model="f.effectiveDate" type="date" style="width:100%" /></el-form-item></el-form>
      <template #footer><el-button @click="dlg=false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref } from 'vue'; import request from '@/utils/request'; import { ElMessage, ElMessageBox } from 'element-plus'
const q=ref({pageNum:1,pageSize:10}); const list=ref([]); const total=ref(0); const loading=ref(false)
const dlg=ref(false); const f=ref({category:'法规'})
function getList(){loading.value=true;request({url:'/basis/list',params:q.value}).then(r=>{list.value=r.rows;total.value=r.total}).finally(()=>loading.value=false)}
function openAdd(){f.value={category:'法规'};dlg.value=true}
function openEdit(row){f.value={...row};dlg.value=true}
function submit(){const api=f.value.id?request({url:'/basis',method:'put',data:f.value}):request({url:'/basis',method:'post',data:f.value});api.then(r=>{if(r.code===200){ElMessage.success('成功');dlg.value=false;getList()}})}
function del(id){ElMessageBox.confirm('确认删除？').then(()=>request({url:'/basis/'+id,method:'delete'}).then(r=>{if(r.code===200){ElMessage.success('已删除');getList()}})).catch(()=>{})}
getList()
</script>
