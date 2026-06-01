import request from './request'

// 创建出差申请
export function createTrip(data: any) {
  return request.post('/trip/create', data)
}

// 更新出差申请
export function updateTrip(data: any) {
  return request.put('/trip/update', data)
}

// 删除出差申请
export function deleteTrip(id: number) {
  return request.delete(`/trip/${id}`)
}

// 出差申请详情
export function getTripDetail(id: number) {
  return request.get(`/trip/detail/${id}`)
}

// 出差申请列表
export function getTripList(params: {
  pageNum?: number
  pageSize?: number
  status?: number
  keyword?: string
  startDate?: string
  endDate?: string
}) {
  return request.get('/trip/list', { params })
}

// 提交出差申请
export function submitTrip(id: number) {
  return request.post(`/trip/submit/${id}`)
}

// 审批出差申请（action: 1通过 2驳回）
export function approveTrip(id: number, action: number, comment?: string) {
  return request.post(`/trip/approve/${id}`, { action, comment })
}
