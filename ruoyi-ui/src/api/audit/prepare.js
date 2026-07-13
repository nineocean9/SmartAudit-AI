import request from '@/utils/request'

// 方案模板
export function listTemplate(query) { return request({ url: '/audit/prepare/template/list', params: query }) }
export function getTemplate(id) { return request({ url: '/audit/prepare/template/' + id }) }
export function addTemplate(data) { return request({ url: '/audit/prepare/template', method: 'post', data }) }
export function updateTemplate(data) { return request({ url: '/audit/prepare/template', method: 'put', data }) }
export function delTemplate(ids) { return request({ url: '/audit/prepare/template/' + ids, method: 'delete' }) }

// 项目成员
export function listMember(projectId) { return request({ url: '/audit/prepare/member/' + projectId }) }
export function addMember(data) { return request({ url: '/audit/prepare/member', method: 'post', data }) }
export function updateMember(data) { return request({ url: '/audit/prepare/member', method: 'put', data }) }
export function delMember(id) { return request({ url: '/audit/prepare/member/' + id, method: 'delete' }) }

// 资料清单
export function listMaterial(projectId) { return request({ url: '/audit/prepare/material/' + projectId }) }
export function addMaterial(data) { return request({ url: '/audit/prepare/material', method: 'post', data }) }
export function updateMaterial(data) { return request({ url: '/audit/prepare/material', method: 'put', data }) }
export function delMaterial(id) { return request({ url: '/audit/prepare/material/' + id, method: 'delete' }) }

// 人员负载
export function getWorkload() { return request({ url: '/audit/prepare/workload' }) }
