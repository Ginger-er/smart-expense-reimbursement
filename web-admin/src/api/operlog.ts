import request from './request'

// 操作日志列表（仅管理员）
export function getOperLogList(params: { pageNum?: number; pageSize?: number }) {
  return request.get('/operlog/list', { params })
}
