import request from '@/utils/request'

export function listTempAuth(query) { return request({ url: '/audit/permission/tempAuth/list', params: query }) }
export function grantAuth(data) { return request({ url: '/audit/permission/tempAuth', method: 'post', data }) }
export function revokeAuth(id) { return request({ url: '/audit/permission/tempAuth/revoke/' + id, method: 'put' }) }
export function delAuth(id) { return request({ url: '/audit/permission/tempAuth/' + id, method: 'delete' }) }
export function revokeExpiredAuth() { return request({ url: '/audit/permission/tempAuth/revokeExpired', method: 'post' }) }
