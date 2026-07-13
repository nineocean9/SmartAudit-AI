import request from '@/utils/request'

// 报告意见
export function listOpinion(reportId) { return request({ url: '/audit/report/opinion/' + reportId }) }
export function addOpinion(data) { return request({ url: '/audit/report/opinion', method: 'post', data }) }

// 报告版本
export function listVersion(reportId) { return request({ url: '/audit/report/version/' + reportId }) }
export function getVersion(id) { return request({ url: '/audit/report/version/detail/' + id }) }
export function saveVersion(data) { return request({ url: '/audit/report/version', method: 'post', data }) }

// 项目归档
export function listArchive(query) { return request({ url: '/audit/report/archive/list', params: query }) }
export function archiveByProject(projectId) { return request({ url: '/audit/report/archive/' + projectId }) }
export function addArchive(data) { return request({ url: '/audit/report/archive', method: 'post', data }) }
export function updateArchive(data) { return request({ url: '/audit/report/archive', method: 'put', data }) }
export function delArchive(id) { return request({ url: '/audit/report/archive/' + id, method: 'delete' }) }
