import request from '@/utils/request'

export function listProjectDoc(projectId, docType) {
  return request({ url: '/project/doc/list', method: 'get', params: { projectId, docType } })
}

export function uploadProjectDoc(data) {
  return request({ url: '/project/doc/upload', method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' } })
}

export function getProjectDocContent(id) {
  return request({ url: `/project/doc/${id}/content`, method: 'get' })
}

export function deleteProjectDoc(id) {
  return request({ url: `/project/doc/${id}`, method: 'delete' })
}

export function listProjectDocByPlan(planId) {
  return request({ url: '/project/doc/byPlan', method: 'get', params: { planId } })
}

export function getExcelData(id) {
  return request({ url: `/project/doc/${id}/excel-data`, method: 'get' })
}

export function getDocDetail(id) {
  return request({ url: `/project/doc/${id}/detail`, method: 'get' })
}

export function updateExcelCell(id, data) {
  return request({ url: `/project/doc/${id}/excel-cell`, method: 'put', data })
}

export function addExcelRow(id, data) {
  return request({ url: `/project/doc/${id}/excel-row`, method: 'post', data })
}

export function deleteExcelRow(id, data) {
  return request({ url: `/project/doc/${id}/excel-row`, method: 'delete', data })
}
