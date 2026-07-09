import request from '@/utils/request'

// 获取项目列表树
export function listProjectTree() {
  return request({
    url: '/ai/workspace/projects',
    method: 'get'
  })
}

// 获取计划列表
export function listPlans() {
  return request({
    url: '/ai/workspace/plans',
    method: 'get'
  })
}

// 获取计划下项目列表
export function listProjectsByPlan(planId) {
  return request({
    url: `/ai/workspace/plans/${planId}/projects`,
    method: 'get'
  })
}

// 获取资料类型
export function listDocTypes() {
  return request({
    url: '/ai/workspace/doc-types',
    method: 'get'
  })
}

// 获取审计类型
export function listAuditTypes() {
  return request({
    url: '/ai/workspace/audit-types',
    method: 'get'
  })
}

// 创建审计项目
export function createProject(data) {
  return request({
    url: '/ai/workspace/project',
    method: 'post',
    data: data
  })
}
