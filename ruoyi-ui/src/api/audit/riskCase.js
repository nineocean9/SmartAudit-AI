import request from '@/utils/request'

export function listRiskCase(query) { return request({ url: '/basis/risk/list', method: 'get', params: query }) }
export function addRiskCase(data) { return request({ url: '/basis/risk', method: 'post', data }) }
export function updateRiskCase(data) { return request({ url: '/basis/risk', method: 'put', data }) }
export function delRiskCase(ids) { return request({ url: '/basis/risk/' + ids, method: 'delete' }) }
