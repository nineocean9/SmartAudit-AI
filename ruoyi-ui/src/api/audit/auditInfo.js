import request from '@/utils/request'

export function listPlan(query) { return request({ url: '/audit/info/plan/list', method: 'get', params: query }) }
export function addPlan(data) { return request({ url: '/audit/info/plan', method: 'post', data }) }
export function delPlan(ids) { return request({ url: '/audit/info/plan/' + ids, method: 'delete' }) }
export function listUnit(query) { return request({ url: '/audit/info/unit/list', method: 'get', params: query }) }
export function addUnit(data) { return request({ url: '/audit/info/unit', method: 'post', data }) }
export function delUnit(ids) { return request({ url: '/audit/info/unit/' + ids, method: 'delete' }) }
export function listLeader(query) { return request({ url: '/audit/info/leader/list', method: 'get', params: query }) }
export function getProgress() { return request({ url: '/audit/info/progress', method: 'get' }) }
// 闭环⓪
export function getUnitProfile(id) { return request({ url: '/audit/info/unit/' + id + '/profile', method: 'get' }) }
export function recommendTargets() { return request({ url: '/audit/info/plan/recommend', method: 'get' }) }
export function bindPlanProject(planId, projectId) { return request({ url: '/audit/info/plan/' + planId + '/bind/' + projectId, method: 'post' }) }
export function unbindPlanProject(planId, projectId) { return request({ url: '/audit/info/plan/' + planId + '/bind/' + projectId, method: 'delete' }) }
export function getPlanProjects(planId) { return request({ url: '/audit/info/plan/' + planId + '/projects', method: 'get' }) }
export function getPlanSchemes(planId) { return request({ url: '/audit/info/plan/' + planId + '/schemes', method: 'get' }) }
