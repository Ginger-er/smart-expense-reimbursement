import request from './request'

// 预警列表
export function getAbnormalList(params: {
  pageNum?: number
  pageSize?: number
  handled?: number
}) {
  return request.get('/abnormal/list', { params })
}

// 标记预警已处理
export function handleAbnormal(id: number) {
  return request.post(`/abnormal/handle/${id}`)
}

// 手动触发扫描昨日数据（管理员）
export function scanAbnormal() {
  return request.post('/abnormal/scan')
}
