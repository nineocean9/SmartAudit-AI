import request from '@/utils/request'

// 获取分析结果列表
export function listAnalysisResults(keyword, pageNum, pageSize) {
  return request({
    url: '/ai/data/analysis/list',
    method: 'get',
    params: { keyword, pageNum, pageSize }
  })
}

// 获取分析结果详情
export function getAnalysisResult(id) {
  return request({
    url: `/ai/data/analysis/${id}`,
    method: 'get'
  })
}

// 从项目库分析
export function analyzeProject(data) {
  return request({
    url: '/ai/data/analyze-project',
    method: 'post',
    data,
    timeout: 120000
  })
}

// 上传文件分析
export function analyzeUpload(formData) {
  return request({
    url: '/ai/data/analyze-upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

// 分析文本数据
export function analyzeChart(data) {
  return request({
    url: '/ai/data/analyze-chart',
    method: 'post',
    data,
    timeout: 120000
  })
}
