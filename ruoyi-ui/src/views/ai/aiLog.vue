<template>
  <div class="app-container">
    <el-table v-loading="loading" :data="list">
      <el-table-column label="用户ID" prop="userId" width="80" />
      <el-table-column label="意图" prop="intent" width="80" />
      <el-table-column label="模型" prop="modelProvider" width="100" />
      <el-table-column label="Token" prop="tokensUsed" width="80" />
      <el-table-column label="耗时ms" prop="costTimeMs" width="90" />
      <el-table-column label="状态" width="80"><template #default="s"><el-tag :type="s.row.status===1?'success':'danger'">{{s.row.status===1?'成功':'失败'}}</el-tag></template></el-table-column>
      <el-table-column label="时间" prop="createTime" width="160" />
      <el-table-column label="操作" width="80"><template #default="s"><el-button size="small" @click="view(s.row.id)">详情</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="q.pageNum" v-model:limit="q.pageSize" @pagination="getList" />
    <el-dialog title="调用详情" v-model="dlgView" width="800px">
      <h4>Prompt</h4><pre style="background:#f5f7fa;padding:10px;border-radius:6px;max-height:200px;overflow:auto">{{detail.prompt}}</pre>
      <h4>Response</h4><pre style="background:#f5f7fa;padding:10px;border-radius:6px;max-height:300px;overflow:auto">{{detail.response}}</pre>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref } from 'vue'; import request from '@/utils/request'
const q=ref({pageNum:1,pageSize:10}); const list=ref([]); const total=ref(0); const loading=ref(false)
const dlgView=ref(false); const detail=ref({})
function getList(){loading.value=true;request({url:'/ai/log/list',params:q.value}).then(r=>{list.value=r.rows;total.value=r.total}).finally(()=>loading.value=false)}
function view(id){request({url:'/ai/log/'+id}).then(r=>{detail.value=r.data||{}});dlgView.value=true}
getList()
</script>
