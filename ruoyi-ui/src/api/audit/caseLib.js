import request from '@/utils/request'

export function listCaseLib(query) { return request({ url: '/basis/case/list', method: 'get', params: query }) }
export function addCaseLib(data) { return request({ url: '/basis/case', method: 'post', data }) }
export function updateCaseLib(data) { return request({ url: '/basis/case', method: 'put', data }) }
export function delCaseLib(ids) { return request({ url: '/basis/case/' + ids, method: 'delete' }) }
