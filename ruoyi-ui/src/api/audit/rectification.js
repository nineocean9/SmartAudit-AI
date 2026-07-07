import request from '@/utils/request'

// 整改台账列表
export function listRectification(query) {
  return request({ url: '/basis/issue/list', method: 'get', params: query })
}
// 更新整改状态
export function updateRectification(data) {
  return request({ url: '/basis/issue', method: 'put', data })
}
