import request from '@/utils/request'

export function cleanupUploadedFile(filePath) {
  if (!filePath) return Promise.resolve()
  return request({
    url: '/common/upload/cleanup',
    method: 'post',
    data: { filePath }
  }).catch(() => {})
}
