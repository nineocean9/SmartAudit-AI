import request from '@/utils/request'

// 审计问题列表
export function listIssues(query) {
  return request({ url: '/audit/issue/list', method: 'get', params: query })
}

// 审计问题详情
export function getIssue(id) {
  return request({ url: '/audit/issue/' + id, method: 'get' })
}

// 新增审计问题
export function addIssue(data) {
  return request({ url: '/audit/issue', method: 'post', data })
}

// 修改审计问题
export function updateIssue(data) {
  return request({ url: '/audit/issue', method: 'put', data })
}

// 删除审计问题
export function deleteIssue(ids) {
  return request({ url: '/audit/issue/' + ids, method: 'delete' })
}

// 获取项目列表（下拉选择）
export function listProjects() {
  return request({ url: '/audit/issue/projects', method: 'get' })
}

// 获取审计依据列表（下拉选择）
export function listBasisOptions() {
  return request({ url: '/audit/issue/basis-options', method: 'get' })
}
