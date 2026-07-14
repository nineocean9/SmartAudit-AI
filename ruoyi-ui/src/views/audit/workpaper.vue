<template>
  <div class="app-container">
    <el-form :model="q" inline>
      <el-form-item label="项目">
        <el-select v-model="q.projectId" placeholder="选择项目" clearable @change="getList">
          <el-option v-for="p in projects" :key="p.id" :label="p.project_name" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">查询</el-button>
      </el-form-item>
    </el-form>

    <el-button type="primary" icon="Plus" @click="openAdd" style="margin-bottom:12px">新增底稿</el-button>

    <el-row :gutter="16">
      <!-- 左侧：底稿列表 -->
      <el-col :span="viewingWp ? 8 : 24">
        <el-table v-loading="loading" :data="list" row-key="id" size="small" :highlight-current-row="true" @row-click="viewDetail">
          <el-table-column label="项目" prop="projectName" min-width="140" show-overflow-tooltip />
          <el-table-column label="标题" prop="title" min-width="160" show-overflow-tooltip />
          <el-table-column label="类型" prop="category" width="70" align="center" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="s">
              <el-tag :type="s.row.status===2?'success':s.row.status===1?'warning':'info'" size="small">
                {{s.row.status===2?'已复核':s.row.status===1?'待复核':'草稿'}}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right" align="center" v-if="!viewingWp">
            <template #default="s">
              <el-button link type="primary" size="small" @click.stop="viewDetail(s.row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total>0" :total="total" v-model:page="q.pageNum" v-model:limit="q.pageSize" @pagination="getList" />
      </el-col>

      <!-- 右侧：底稿内容面板 -->
      <el-col :span="16" v-if="viewingWp">
        <el-card>
          <template #header>
            <div style="display:flex;align-items:center;justify-content:space-between">
              <span style="font-weight:600">
                <el-icon style="margin-right:4px;vertical-align:middle"><Document /></el-icon>
                {{ detail.title }}
                <el-tag :type="detail.status===2?'success':detail.status===1?'warning':'info'" size="small" style="margin-left:8px">
                  {{detail.status===2?'已复核':detail.status===1?'待复核':'草稿'}}
                </el-tag>
              </span>
              <span>
                <el-button size="small" type="warning" v-hasPermi="['audit:workpaper:review']" @click="openReview(detail.id)">复核</el-button>
                <el-button size="small" @click="viewingWp=false">关闭</el-button>
              </span>
            </div>
          </template>

          <!-- 底稿元信息 -->
          <el-descriptions :column="3" size="small" border style="margin-bottom:16px">
            <el-descriptions-item label="项目">{{ detail.projectName }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ detail.category }}</el-descriptions-item>
            <el-descriptions-item label="创建人">{{ detail.createBy }}</el-descriptions-item>
          </el-descriptions>

          <!-- 底稿正文 -->
          <div class="wp-content" v-html="formatText(detail.content)" />

          <!-- 复核记录 -->
          <el-divider content-position="left">复核记录</el-divider>
          <el-timeline v-if="reviews && reviews.length">
            <el-timeline-item v-for="r in reviews" :key="r.id" :timestamp="r.create_time" placement="top">
              <span style="font-weight:500">{{ r.level }}</span>：{{ r.opinion }}
              <el-tag size="small" :type="r.status===1?'success':'danger'" style="margin-left:8px">
                {{ r.status===1?'通过':'驳回' }}
              </el-tag>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无复核记录" :image-size="50" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增底稿弹窗 -->
    <el-dialog title="新增底稿" v-model="dlgAdd" width="650px">
      <el-form :model="f" label-width="80px">
        <el-form-item label="项目">
          <el-select v-model="f.projectId" style="width:100%">
            <el-option v-for="p in projects" :key="p.id" :label="p.project_name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="f.category">
            <el-radio value="底稿">底稿</el-radio>
            <el-radio value="取证记录">取证记录</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="f.title" placeholder="请输入底稿标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input type="textarea" v-model="f.content" :rows="8" placeholder="请输入底稿内容..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgAdd=false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 复核弹窗 -->
    <el-dialog title="底稿复核" v-model="dlgReview" width="500px">
      <el-form :model="rf" label-width="80px">
        <el-form-item label="级别">
          <el-select v-model="rf.level" style="width:100%">
            <el-option label="主审" value="主审" />
            <el-option label="组长" value="组长" />
            <el-option label="负责人" value="负责人" />
          </el-select>
        </el-form-item>
        <el-form-item label="意见">
          <el-input type="textarea" v-model="rf.opinion" :rows="4" placeholder="请输入复核意见..." />
        </el-form-item>
        <el-form-item label="结果">
          <el-radio-group v-model="rf.status">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgReview=false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ops } from '@/api/audit/auditOps'
import { getProgress } from '@/api/audit/auditInfo'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'

const projects = ref([])
const q = ref({ pageNum: 1, pageSize: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const viewingWp = ref(false)
const detail = ref({})
const reviews = ref([])
const dlgAdd = ref(false)
const f = ref({ category: '底稿' })
const dlgReview = ref(false)
const rf = ref({ level: '主审', opinion: '', status: 1 })

getProgress().then(r => { projects.value = r.data || [] })

function getList() {
  loading.value = true
  ops.wpList(q.value).then(r => { list.value = r.rows; total.value = r.total }).finally(() => loading.value = false)
}

function viewDetail(row) {
  ops.wpInfo(row.id || row).then(r => { detail.value = r.data || {} })
  ops.reviewList(row.id || row).then(r => { reviews.value = r.data || [] })
  viewingWp.value = true
}

function formatText(text) {
  if (!text) return '<span style="color:#909399">暂无内容</span>'
  return text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
}

function openAdd() { f.value = { category: '底稿' }; dlgAdd.value = true }
function submitAdd() {
  ops.addWp(f.value).then(r => {
    if (r.code === 200) { ElMessage.success('创建成功'); dlgAdd.value = false; getList() }
  })
}
function openReview(wpId) {
  rf.value = { workpaperId: wpId, level: '主审', opinion: '', status: 1 }
  dlgReview.value = true
}
function submitReview() {
  ops.addReview(rf.value).then(r => {
    if (r.code === 200) { ElMessage.success('已提交'); dlgReview.value = false; viewDetail({ id: rf.value.workpaperId }) }
  })
}

getList()
</script>

<style scoped>
.wp-content {
  padding: 16px 24px;
  min-height: 200px;
  max-height: calc(100vh - 450px);
  overflow-y: auto;
  background: #fafbfc;
  border-radius: 6px;
  font-size: 14px;
  line-height: 2;
  color: #303133;
  border: 1px solid #ebeef5;
}
</style>
