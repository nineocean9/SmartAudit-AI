import request from '@/utils/request'

// 创建临时工作区
export function createTempSession() {
  return request({
    url: '/ai/workspace/temp/create',
    method: 'post'
  })
}

// 上传文件到临时工作区
export function uploadToTemp(sessionId, file) {
  const formData = new FormData()
  formData.append('sessionId', sessionId)
  formData.append('file', file)
  return request({
    url: '/ai/workspace/temp/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 列出临时工作区文件
export function listTempFiles(sessionId) {
  return request({
    url: '/ai/workspace/temp/files',
    method: 'get',
    params: { sessionId }
  })
}

// 销毁临时工作区
export function destroyTempSession(sessionId) {
  return request({
    url: `/ai/workspace/temp/${sessionId}`,
    method: 'delete'
  })
}
