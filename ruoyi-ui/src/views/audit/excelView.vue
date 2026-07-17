<template>
  <div class="app-container">
    <!-- 加载中 -->
    <el-card v-if="loading" v-loading="true" style="min-height:300px" />

    <!-- 错误提示 -->
    <el-card v-else-if="error">
      <el-empty :description="error" />
    </el-card>

    <template v-else>
      <!-- 顶部信息栏 -->
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button plain icon="Back" @click="goBack">返回</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="primary" plain icon="Plus" @click="handleAddRow">新增行</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="!selectedRows.length" @click="handleDeleteRows">删除行</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="warning" plain icon="Download" @click="handleDownload">下载原文件</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Refresh" @click="loadData">刷新</el-button>
        </el-col>
        <el-col :span="8" style="margin-left:auto;text-align:right">
          <span style="font-size:15px;font-weight:600;color:#303133">{{ fileName }}</span>
          <span style="margin-left:12px;font-size:13px;color:#909399">{{ formatSize(fileSize) }}</span>
        </el-col>
      </el-row>

      <!-- Sheet 标签页 -->
      <el-card>
        <el-tabs v-model="activeSheet" @tab-change="onSheetChange">
          <el-tab-pane v-for="(sheet, idx) in sheets" :key="idx" :label="sheet.name + ' (' + sheet.rows.length + '行)'" :name="String(idx)" />
        </el-tabs>

        <!-- 搜索栏 -->
        <el-input v-model="searchText" placeholder="搜索表格内容..." clearable prefix-icon="Search" style="width:300px;margin-bottom:12px" @input="onSearch" />

        <!-- 数据表格 -->
        <el-table
          :data="filteredRows"
          border
          stripe
          highlight-current-row
          @selection-change="handleSelectionChange"
          @cell-dblclick="handleCellDblClick"
          style="width:100%"
          max-height="calc(100vh - 320px)"
        >
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="#" width="60" align="center">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column
            v-for="(header, colIdx) in currentHeaders"
            :key="colIdx"
            :label="header || ('列' + (colIdx + 1))"
            :prop="String(colIdx)"
            min-width="120"
            :show-overflow-tooltip="true"
          >
            <template #default="{ row, $index }">
              <!-- 编辑模式 -->
              <el-input
                v-if="editingCell.row === $index && editingCell.col === colIdx"
                v-model="editingCell.value"
                size="small"
                @blur="saveCell"
                @keyup.enter="saveCell"
                @keyup.escape="cancelEdit"
                ref="cellInputRef"
                autofocus
              />
              <span v-else>{{ row[colIdx] }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div style="margin-top:12px;color:#909399;font-size:13px">
          共 {{ currentSheet?.rows?.length || 0 }} 行 × {{ currentHeaders.length }} 列
          <span v-if="searchText"> · 筛选后 {{ filteredRows.length }} 行</span>
          <span style="margin-left:16px;color:#e6a23c">双击单元格可编辑</span>
        </div>
      </el-card>

      <!-- 新增行弹窗 -->
      <el-dialog title="新增数据行" v-model="addRowOpen" width="600px" append-to-body>
        <el-form label-width="auto">
          <el-form-item v-for="(header, idx) in currentHeaders" :key="idx" :label="header || ('列' + (idx + 1))">
            <el-input v-model="newRowData[idx]" :placeholder="'请输入' + (header || '')" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button type="primary" @click="submitAddRow">确 定</el-button>
          <el-button @click="addRowOpen = false">取 消</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getToken } from '@/utils/auth'
import { getExcelData, getDocDetail, updateExcelCell, addExcelRow, deleteExcelRow } from '@/api/audit/projectDoc'

const route = useRoute()
const router = useRouter()

function goBack() {
  const returnPath = route.query.returnPath
  if (returnPath) {
    router.push(String(returnPath))
    return
  }
  router.back()
}
const docId = ref(null)
const loading = ref(false)
const error = ref(null)
const fileName = ref('')
const fileSize = ref(0)
const sheets = ref([])
const activeSheet = ref('0')
const searchText = ref('')
const selectedRows = ref([])
const editingCell = ref({ row: -1, col: -1, value: '' })
const addRowOpen = ref(false)
const newRowData = ref({})
const cellInputRef = ref(null)

const currentSheet = computed(() => sheets.value[Number(activeSheet.value)] || null)
const currentHeaders = computed(() => currentSheet.value?.headers || [])

const filteredRows = computed(() => {
  const rows = currentSheet.value?.rows || []
  if (!searchText.value) return rows
  const kw = searchText.value.toLowerCase()
  return rows.filter(row => row.some(cell => String(cell).toLowerCase().includes(kw)))
})

function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}

async function loadData() {
  loading.value = true
  error.value = null
  try {
    const res = await getExcelData(docId.value)
    if (res.code === 200 && res.data) {
      fileName.value = res.data.fileName || ''
      fileSize.value = res.data.fileSize || 0
      sheets.value = res.data.sheets || []
      if (sheets.value.length > 0 && Number(activeSheet.value) >= sheets.value.length) {
        activeSheet.value = '0'
      }
    } else {
      error.value = res.msg || '加载失败'
    }
  } catch (e) {
    error.value = '加载失败: ' + (e.message || '')
  } finally {
    loading.value = false
  }
}

function onSheetChange() {
  searchText.value = ''
  selectedRows.value = []
  editingCell.value = { row: -1, col: -1, value: '' }
}

function onSearch() { /* reactive via computed */ }

function handleSelectionChange(selection) {
  selectedRows.value = selection
}

function handleCellDblClick(row, column) {
  if (column.type === 'selection' || column.label === '#') return
  const rowIdx = filteredRows.value.indexOf(row)
  const colIdx = Number(column.property)
  if (isNaN(colIdx)) return
  editingCell.value = { row: rowIdx, col: colIdx, value: row[colIdx] || '' }
  nextTick(() => {
    const input = document.querySelector('.el-table .el-input__inner:focus') ||
                  document.querySelector('.el-table .el-input input')
    if (input) input.focus()
  })
}

async function saveCell() {
  const { row, col, value } = editingCell.value
  if (row < 0) return
  const originalRow = filteredRows.value[row]
  const originalRowIdx = currentSheet.value.rows.indexOf(originalRow)

  try {
    const offset = currentSheet.value?.headerFromData ? 1 : 0
    const res = await updateExcelCell(docId.value, {
      sheetIndex: Number(activeSheet.value),
      rowIndex: originalRowIdx + offset,
      colIndex: col,
      value: value
    })
    if (res.code === 200) {
      originalRow[col] = value
      ElMessage.success('已保存')
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch {
    ElMessage.error('保存失败')
  }
  editingCell.value = { row: -1, col: -1, value: '' }
}

function cancelEdit() {
  editingCell.value = { row: -1, col: -1, value: '' }
}

function handleAddRow() {
  newRowData.value = {}
  currentHeaders.value.forEach((_, idx) => { newRowData.value[idx] = '' })
  addRowOpen.value = true
}

async function submitAddRow() {
  const values = currentHeaders.value.map((_, idx) => newRowData.value[idx] || '')
  try {
    const res = await addExcelRow(docId.value, {
      sheetIndex: Number(activeSheet.value),
      values
    })
    if (res.code === 200) {
      ElMessage.success('新增成功')
      addRowOpen.value = false
      await loadData()
    } else {
      ElMessage.error(res.msg || '新增失败')
    }
  } catch {
    ElMessage.error('新增失败')
  }
}

async function handleDeleteRows() {
  if (!selectedRows.value.length) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 行？`, '提示', { type: 'warning' })
  } catch { return }

  const allRows = currentSheet.value.rows
  const headerOffset = currentSheet.value?.headerFromData ? 1 : 0
  const indices = selectedRows.value.map(r => allRows.indexOf(r)).filter(i => i >= 0).sort((a, b) => b - a)

  for (const idx of indices) {
    try {
      await deleteExcelRow(docId.value, {
        sheetIndex: Number(activeSheet.value),
        rowIndex: idx,
        headerOffset: headerOffset
      })
    } catch { /* continue */ }
  }

  ElMessage.success('删除成功')
  selectedRows.value = []
  await loadData()
}

function handleDownload() {
  const baseUrl = import.meta.env.VITE_APP_BASE_API || ''
  const url = `${baseUrl}/project/doc/${docId.value}/file`
  const token = getToken()
  fetch(url, { headers: { Authorization: 'Bearer ' + token } })
    .then(res => { if (!res.ok) throw new Error('下载失败'); return res.blob() })
    .then(blob => {
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = fileName.value
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(link.href)
    })
    .catch(() => ElMessage.error('下载失败'))
}

onMounted(() => {
  const id = route.query.id
  if (id) {
    docId.value = id
    loadData()
  } else {
    error.value = '缺少文档 ID 参数'
  }
})
</script>
