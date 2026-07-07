<template>
  <div class="app-container">
    <el-form :model="q" inline><el-form-item label="项目"><el-select v-model="q.projectId" placeholder="选择项目" clearable>
      <el-option v-for="p in projects" :key="p.id" :label="p.project_name" :value="p.id" /></el-select></el-form-item>
    <el-form-item><el-button type="primary" @click="getList">查询</el-button></el-form-item></el-form>
    <el-button type="primary" @click="openAdd" style="margin-bottom:10px">新增底稿</el-button>
    <el-table v-loading="loading" :data="list" row-key="id">
      <el-table-column label="项目" prop="projectName" width="200" />
      <el-table-column label="标题" prop="title" width="200" show-overflow-tooltip />
      <el-table-column label="类型" prop="category" width="80" />
      <el-table-column label="状态" width="100"><template #default="s">
        <el-tag :type="s.row.status===2?'success':s.row.status===1?'warning':'info'">{{s.row.status===2?'已复核':s.row.status===1?'待复核':'草稿'}}</el-tag>
      </template></el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="s">
          <el-button size="small" @click="viewDetail(s.row.id)">查看</el-button>
          <el-button size="small" type="primary" v-hasPermi="['audit:workpaper:review']" @click="openReview(s.row.id)">复核</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="q.pageNum" v-model:limit="q.pageSize" @pagination="getList" />
    <el-dialog title="底稿详情" v-model="dlgDetail" width="700px">
      <h3>{{detail.title}}</h3><pre style="background:#f5f7fa;padding:10px;border-radius:6px;white-space:pre-wrap">{{detail.content}}</pre>
      <el-divider />复核记录<el-timeline v-if="reviews&&reviews.length"><el-timeline-item v-for="r in reviews" :key="r.id" :timestamp="r.create_time">{{r.level}}：{{r.opinion}} <el-tag size="small" :type="r.status===1?'success':'danger'">{{r.status===1?'通过':'驳回'}}</el-tag></el-timeline-item></el-timeline>
      <el-empty v-else description="暂无复核" />
    </el-dialog>
    <el-dialog title="新增底稿" v-model="dlgAdd" width="600px">
      <el-form :model="f"><el-form-item label="项目"><el-select v-model="f.projectId"><el-option v-for="p in projects" :key="p.id" :label="p.project_name" :value="p.id" /></el-select></el-form-item>
      <el-form-item label="类型"><el-radio-group v-model="f.category"><el-radio value="底稿">底稿</el-radio><el-radio value="取证记录">取证记录</el-radio></el-radio-group></el-form-item>
      <el-form-item label="标题"><el-input v-model="f.title" /></el-form-item>
      <el-form-item label="内容"><el-input type="textarea" v-model="f.content" :rows="4" /></el-form-item></el-form>
      <template #footer><el-button @click="dlgAdd=false">取消</el-button><el-button type="primary" @click="submitAdd">确定</el-button></template>
    </el-dialog>
    <el-dialog title="底稿复核" v-model="dlgReview" width="500px">
      <el-form :model="rf"><el-form-item label="级别"><el-select v-model="rf.level"><el-option label="主审" value="主审" /><el-option label="组长" value="组长" /><el-option label="负责人" value="负责人" /></el-select></el-form-item>
      <el-form-item label="意见"><el-input type="textarea" v-model="rf.opinion" :rows="3" /></el-form-item>
      <el-form-item label="结果"><el-radio-group v-model="rf.status"><el-radio :value="1">通过</el-radio><el-radio :value="2">驳回</el-radio></el-radio-group></el-form-item></el-form>
      <template #footer><el-button @click="dlgReview=false">取消</el-button><el-button type="primary" @click="submitReview">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref } from 'vue'; import { ops } from '@/api/audit/auditOps'; import { getProgress } from '@/api/audit/auditInfo'
import { ElMessage } from 'element-plus'
const projects=ref([]); const q=ref({pageNum:1,pageSize:10}); const list=ref([]); const total=ref(0); const loading=ref(false)
const dlgDetail=ref(false); const detail=ref({}); const reviews=ref([])
const dlgAdd=ref(false); const f=ref({category:'底稿'})
const dlgReview=ref(false); const rf=ref({level:'主审',opinion:'',status:1})
getProgress().then(r=>{projects.value=r.data||[]})
function getList(){loading.value=true;ops.wpList(q.value).then(r=>{list.value=r.rows;total.value=r.total}).finally(()=>loading.value=false)}
function viewDetail(id){ops.wpInfo(id).then(r=>{detail.value=r.data||{}});ops.reviewList(id).then(r=>{reviews.value=r.data||[]});dlgDetail.value=true}
function openAdd(){f.value={category:'底稿'};dlgAdd.value=true}
function submitAdd(){ops.addWp(f.value).then(r=>{if(r.code===200){ElMessage.success('成功');dlgAdd.value=false;getList()}})}
function openReview(wpId){rf.value={workpaperId:wpId,level:'主审',opinion:'',status:1};dlgReview.value=true}
function submitReview(){ops.addReview(rf.value).then(r=>{if(r.code===200){ElMessage.success('已提交');dlgReview.value=false;viewDetail(rf.value.workpaperId)}})}
getList()
</script>
