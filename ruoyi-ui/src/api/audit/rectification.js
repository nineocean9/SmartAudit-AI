import request from '@/utils/request'

// 整改台账列表
export function listRectification(query) {
  return request({ url: '/basis/issue/list', method: 'get', params: query })
}
// 新增整改记录
export function addRectification(data) {
  return request({ url: '/basis/issue', method: 'post', data })
}
// 更新整改状态
export function updateRectification(data) {
  return request({ url: '/basis/issue', method: 'put', data })
}
// 删除整改记录
export function delRectification(id) {
  return request({ url: '/basis/issue/' + id, method: 'delete' })
}
