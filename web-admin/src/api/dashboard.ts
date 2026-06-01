import request from './request'

// 工作台统计数据（按角色返回不同范围）
export function getDashboardStats() {
  return request.get('/dashboard/stats')
}
