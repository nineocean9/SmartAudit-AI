import request from '@/utils/request'

// 获取项目文档列表
export function listProjectDoc(projectId, docType) {
  return request({
    url: '/project/doc/list',
    method: 'get',
    params: { projectId, docType }
  })
}

// 上传文档到项目
export function uploadProjectDoc(data) {
  return request({
    url: '/project/doc/upload',
    method: 'post',
    data: data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 获取文档内容
export function getProjectDocContent(id) {
  return request({
    url: `/project/doc/${id}/content`,
    method: 'get'
  })
}

// 删除项目文档
export function deleteProjectDoc(id) {
  return request({
    url: `/project/doc/${id}`,
    method: 'delete'
  })
}

// 按计划查询文档
export function listProjectDocByPlan(planId) {
  return request({
    url: '/project/doc/byPlan',
    method: 'get',
    params: { planId }
  })
}
