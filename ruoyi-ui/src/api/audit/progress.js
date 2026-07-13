import request from '@/utils/request'

export function getGanttData() { return request({ url: '/audit/progress/gantt' }) }
export function getWorkload() { return request({ url: '/audit/progress/workload' }) }
export function getOverdue() { return request({ url: '/audit/progress/overdue' }) }
