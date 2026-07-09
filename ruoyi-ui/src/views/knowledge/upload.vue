<template>
  <div class="app-container">
    <h3>上传中心</h3>
    <p style="color:#909399;font-size:13px;margin-bottom:16px">上传文件到项目知识库或临时知识库</p>
    <FileUploader ref="uploaderRef" :tempSessionId="tempSessionId" @uploadSuccess="onSuccess" @uploadComplete="onComplete" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createTempSession } from '@/api/knowledge/tempWorkspace'
import FileUploader from '@/components/AiChat/FileUploader.vue'

const uploaderRef = ref(null)
const tempSessionId = ref(null)

// 初始化临时 session
createTempSession().then(res => {
  if (res.code === 200) tempSessionId.value = res.data.sessionId
}).catch(() => {})

function onSuccess(result) {
  ElMessage.success('上传成功')
}
function onComplete() {}
</script>
