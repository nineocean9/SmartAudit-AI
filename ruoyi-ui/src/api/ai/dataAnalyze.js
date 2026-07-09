import request from '@/utils/request'

export function listAnalysisResults(keyword, pageNum, pageSize) {
  return request({
    url: '/ai/data/analysis/list',
    method: 'get',
    params: { keyword, pageNum, pageSize }
  })
}

export function getAnalysisResult(id) {
  return request({
    url: `/ai/data/analysis/${id}`,
    method: 'get'
  })
}

export function deleteAnalysis(ids) {
  return request({
    url: `/ai/data/analysis/${ids}`,
    method: 'delete'
  })
}

export function updateAnalysis(data) {
  return request({
    url: '/ai/data/analysis',
    method: 'put',
    data
  })
}

export function analyzeProject(data) {
  return request({
    url: '/ai/data/analyze-project',
    method: 'post',
    data,
    timeout: 120000
  })
}

export function analyzeUpload(formData) {
  return request({
    url: '/ai/data/analyze-upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export function analyzeChart(data) {
  return request({
    url: '/ai/data/analyze-chart',
    method: 'post',
    data,
    timeout: 120000
  })
}
