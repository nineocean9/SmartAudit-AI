<template>
  <div class="app-container">
    <!-- Advanced search panel -->
    <el-form :model="q" :inline="true" ref="queryRef">
      <el-form-item label="分类" prop="category">
        <el-select v-model="q.category" clearable placeholder="全部" @change="getList">
          <el-option label="法规" value="法规"/><el-option label="制度" value="制度"/><el-option label="标准" value="标准"/>
        </el-select>
      </el-form-item>
      <el-form-item label="关键词" prop="title">
        <el-input v-model="q.title" placeholder="搜索标题" clearable @keyup.enter="getList" style="width:180px"/>
      </el-form-item>
      <el-form-item label="层级" prop="hierarchyLevel">
        <el-select v-model="q.hierarchyLevel" clearable placeholder="全部" @change="getList">
          <el-option label="法律" value="法律"/><el-option label="行政法规" value="行政法规"/>
          <el-option label="部门规章" value="部门规章"/><el-option label="规范性文件" value="规范性文件"/>
          <el-option label="内部制度" value="内部制度"/>
        </el-select>
      </el-form-item>
      <el-form-item label="审计范围" prop="auditScope">
        <el-select v-model="q.auditScope" clearable placeholder="全部" @change="getList">
          <el-option label="财务审计" value="财务审计"/><el-option label="经济责任" value="经济责任"/>
          <el-option label="基建审计" value="基建审计"/><el-option label="内控审计" value="内控审计"/>
          <el-option label="专项审计" value="专项审计"/>
        </el-select>
      </el-form-item>
      <el-form-item label="资金类型" prop="fundType">
        <el-select v-model="q.fundType" clearable placeholder="全部" @change="getList">
          <el-option label="财政资金" value="财政资金"/><el-option label="自筹资金" value="自筹资金"/>
          <el-option label="科研经费" value="科研经费"/><el-option label="基建资金" value="基建资金"/>
        </el-select>
      </el-form-item>
      <el-form-item label="颁发单位" prop="issueOrg">
        <el-input v-model="q.issueOrg" placeholder="搜索颁发单位" clearable style="width:150px"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- Toolbar -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" icon="Plus" @click="openAdd" v-hasPermi="['ai:basis:add']">新增</el-button></el-col>
    </el-row>

    <!-- Table -->
    <el-table v-loading="loading" :data="list" row-key="id" border @row-click="showRelated">
      <el-table-column label="标题" prop="title" min-width="220" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ scope.row.title }}</span>
          <el-tag v-if="isExpired(scope.row)" type="danger" size="small" style="margin-left:6px">已过期</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文号" prop="docNumber" width="150" show-overflow-tooltip/>
      <el-table-column label="分类" prop="category" width="80" align="center"/>
      <el-table-column label="层级" prop="hierarchyLevel" width="100" align="center">
        <template #default="scope">
          <el-tag v-if="scope.row.hierarchyLevel" :type="hierarchyTagType(scope.row.hierarchyLevel)" size="small">
            {{ scope.row.hierarchyLevel }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="颁发单位" prop="issueOrg" width="120" show-overflow-tooltip/>
      <el-table-column label="状态" width="80" align="center">
        <template #default="scope">
          <el-tag :type="isExpired(scope.row) ? 'info' : (scope.row.status===1 ? 'success' : 'info')">
            {{ isExpired(scope.row) ? '已过期' : (scope.row.status===1 ? '生效' : '失效') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生效日期" prop="effectiveDate" width="110" align="center"/>
      <el-table-column label="过期日期" prop="expireDate" width="110" align="center">
        <template #default="scope">
          <span :class="{ 'expired-text': isExpired(scope.row) }">{{ scope.row.expireDate || '长期有效' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="primary" icon="CopyDocument" @click.stop="quickCopy(scope.row)">复制引用</el-button>
          <el-button link type="primary" icon="Edit" @click.stop="openEdit(scope.row)" v-hasPermi="['ai:basis:edit']">编辑</el-button>
          <el-button link type="danger" icon="Delete" @click.stop="del(scope.row.id)" v-hasPermi="['ai:basis:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="q.pageNum" v-model:limit="q.pageSize" @pagination="getList"/>

    <!-- Add/Edit Dialog -->
    <el-dialog :title="f.id ? '编辑审计依据' : '新增审计依据'" v-model="dlg" width="700px" destroy-on-close>
      <el-form :model="f" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="f.title" placeholder="请输入标题"/>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="文号" prop="docNumber">
              <el-input v-model="f.docNumber" placeholder="如：财会〔2023〕1号"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="f.category" style="width:100%">
                <el-option label="法规" value="法规"/><el-option label="制度" value="制度"/><el-option label="标准" value="标准"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="层级" prop="hierarchyLevel">
              <el-select v-model="f.hierarchyLevel" style="width:100%" clearable>
                <el-option label="法律" value="法律"/><el-option label="行政法规" value="行政法规"/>
                <el-option label="部门规章" value="部门规章"/><el-option label="规范性文件" value="规范性文件"/>
                <el-option label="内部制度" value="内部制度"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="颁发单位" prop="issueOrg">
              <el-input v-model="f.issueOrg" placeholder="请输入颁发单位"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="审计范围" prop="auditScope">
              <el-select v-model="f.auditScope" style="width:100%" clearable>
                <el-option label="财务审计" value="财务审计"/><el-option label="经济责任" value="经济责任"/>
                <el-option label="基建审计" value="基建审计"/><el-option label="内控审计" value="内控审计"/>
                <el-option label="专项审计" value="专项审计"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资金类型" prop="fundType">
              <el-select v-model="f.fundType" style="width:100%" clearable>
                <el-option label="财政资金" value="财政资金"/><el-option label="自筹资金" value="自筹资金"/>
                <el-option label="科研经费" value="科研经费"/><el-option label="基建资金" value="基建资金"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="生效日期" prop="effectiveDate">
              <el-date-picker v-model="f.effectiveDate" type="date" value-format="YYYY-MM-DD" style="width:100%" placeholder="选择生效日期"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="过期日期" prop="expireDate">
              <el-date-picker v-model="f.expireDate" type="date" value-format="YYYY-MM-DD" style="width:100%" placeholder="不填则长期有效"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="内容" prop="content">
          <el-input type="textarea" v-model="f.content" :rows="5" placeholder="请输入依据内容"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg=false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Related Basis Drawer -->
    <el-drawer v-model="relatedDrawer" :title="(currentBasis?.title || '') + ' - 关联依据'" size="550px">
      <el-descriptions :column="1" border style="margin-bottom:16px">
        <el-descriptions-item label="标题">{{ currentBasis?.title }}</el-descriptions-item>
        <el-descriptions-item label="文号">{{ currentBasis?.docNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="层级">{{ currentBasis?.hierarchyLevel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="颁发单位">{{ currentBasis?.issueOrg || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="isExpired(currentBasis) ? 'danger' : 'success'" size="small">
            {{ isExpired(currentBasis) ? '已过期' : '生效中' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">上位法</el-divider>
      <el-table :data="relatedBasis.upper || []" size="small" border>
        <el-table-column label="标题" prop="title" show-overflow-tooltip/>
        <el-table-column label="文号" prop="docNumber" width="140" show-overflow-tooltip/>
        <el-table-column label="层级" prop="hierarchyLevel" width="100" align="center"/>
      </el-table>
      <el-empty v-if="!(relatedBasis.upper||[]).length" description="暂无上位法" :image-size="40"/>

      <el-divider content-position="left">下位法</el-divider>
      <el-table :data="relatedBasis.lower || []" size="small" border>
        <el-table-column label="标题" prop="title" show-overflow-tooltip/>
        <el-table-column label="文号" prop="docNumber" width="140" show-overflow-tooltip/>
        <el-table-column label="层级" prop="hierarchyLevel" width="100" align="center"/>
      </el-table>
      <el-empty v-if="!(relatedBasis.lower||[]).length" description="暂无下位法" :image-size="40"/>

      <el-divider content-position="left">修订/替代</el-divider>
      <el-table :data="relatedBasis.revisions || []" size="small" border>
        <el-table-column label="标题" prop="title" show-overflow-tooltip/>
        <el-table-column label="文号" prop="docNumber" width="140" show-overflow-tooltip/>
        <el-table-column label="关系" prop="relationType" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.relationType === '替代' ? 'danger' : 'warning'" size="small">
              {{ scope.row.relationType }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!(relatedBasis.revisions||[]).length" description="暂无修订记录" :image-size="40"/>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useClipboard } from '@vueuse/core'

const q = ref({ pageNum: 1, pageSize: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const dlg = ref(false)
const f = ref({ category: '法规' })
const formRef = ref(null)
const relatedDrawer = ref(false)
const currentBasis = ref(null)
const relatedBasis = ref({ upper: [], lower: [], revisions: [] })

const formRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

function getList() {
  loading.value = true
  request({ url: '/basis/list', params: q.value }).then(r => {
    list.value = r.rows || r.data || []
    total.value = r.total || 0
  }).finally(() => { loading.value = false })
}

function resetQuery() {
  q.value = { pageNum: 1, pageSize: 10 }
  getList()
}

function openAdd() {
  f.value = { category: '法规', status: 1 }
  dlg.value = true
}

function openEdit(row) {
  f.value = { ...row }
  dlg.value = true
}

function submit() {
  formRef.value?.validate(valid => {
    if (!valid) return
    const api = f.value.id
      ? request({ url: '/basis', method: 'put', data: f.value })
      : request({ url: '/basis', method: 'post', data: f.value })
    api.then(r => {
      if (r.code === 200) {
        ElMessage.success('操作成功')
        dlg.value = false
        getList()
      }
    })
  })
}

function del(id) {
  ElMessageBox.confirm('确认删除该审计依据？', '提示', { type: 'warning' }).then(() => {
    request({ url: '/basis/' + id, method: 'delete' }).then(r => {
      if (r.code === 200) { ElMessage.success('已删除'); getList() }
    })
  }).catch(() => {})
}

function isExpired(row) {
  if (!row || !row.expireDate) return false
  return new Date(row.expireDate) < new Date()
}

function hierarchyTagType(level) {
  const map = { '法律': '', '行政法规': 'success', '部门规章': 'warning', '规范性文件': 'info', '内部制度': 'danger' }
  return map[level] || 'info'
}

function showRelated(row) {
  currentBasis.value = row
  request({ url: '/basis/' + row.id + '/related' }).then(res => {
    relatedBasis.value = {
      upper: res.data?.upper || [],
      lower: res.data?.lower || [],
      revisions: res.data?.revisions || []
    }
    relatedDrawer.value = true
  }).catch(() => {
    relatedBasis.value = { upper: [], lower: [], revisions: [] }
    relatedDrawer.value = true
  })
}

function quickCopy(row) {
  const ref = buildReference(row)
  if (navigator.clipboard) {
    navigator.clipboard.writeText(ref).then(() => {
      ElMessage.success('已复制引用信息到剪贴板')
    }).catch(() => {
      fallbackCopy(ref)
    })
  } else {
    fallbackCopy(ref)
  }
}

function buildReference(row) {
  let ref = ''
  if (row.docNumber) ref += '【' + row.docNumber + '】'
  ref += '《' + row.title + '》'
  if (row.issueOrg) ref += '（' + row.issueOrg + '）'
  if (row.effectiveDate) ref += '，' + row.effectiveDate + '起施行'
  return ref
}

function fallbackCopy(text) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  try {
    document.execCommand('copy')
    ElMessage.success('已复制引用信息到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
  document.body.removeChild(textarea)
}

getList()
</script>

<style scoped>
.expired-text {
  color: #f56c6c;
  font-weight: bold;
}
</style>
