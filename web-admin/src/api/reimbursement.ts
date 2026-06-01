import request from './request'

// 创建报销单
export function createReimbursement(data: any) {
  return request.post('/reimbursement/create', data)
}

// 提交审批
export function submitReimbursement(id: number) {
  return request.post(`/reimbursement/submit/${id}`)
}

// 审批（通过/驳回）
export function approveReimbursement(id: number, action: number, comment?: string) {
  return request.post(`/reimbursement/approve/${id}`, { action, comment })
}

// 打款（财务/管理员，已通过→已打款）
export function payReimbursement(id: number) {
  return request.post(`/reimbursement/pay/${id}`)
}

// 报销单列表
export function getReimbursementList(params: {
  pageNum?: number
  pageSize?: number
  status?: number
  keyword?: string
  startDate?: string
  endDate?: string
}) {
  return request.get('/reimbursement/list', { params })
}

// 报销单详情
export function getReimbursementDetail(id: number) {
  return request.get(`/reimbursement/detail/${id}`)
}

// 删除报销单
export function deleteReimbursement(id: number) {
  return request.delete(`/reimbursement/${id}`)
}

// 导出报销单（Excel，返回 Blob）
export function exportReimbursement(params: {
  status?: number
  keyword?: string
  startDate?: string
  endDate?: string
}): Promise<Blob> {
  return request.get('/reimbursement/export', { params, responseType: 'blob' }) as unknown as Promise<Blob>
}
