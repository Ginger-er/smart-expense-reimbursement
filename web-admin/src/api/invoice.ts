import request from './request'

// 上传发票
export function uploadInvoice(file: File, opts?: { tripId?: number; reimbursementId?: number }) {
  const formData = new FormData()
  formData.append('file', file)
  if (opts?.tripId) formData.append('tripId', String(opts.tripId))
  if (opts?.reimbursementId) formData.append('reimbursementId', String(opts.reimbursementId))
  return request.post('/invoice/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 确认/修正发票信息
export function confirmInvoice(data: any) {
  return request.post('/invoice/confirm', data)
}

// 获取发票列表
export function getInvoiceList(params: { tripId?: number; reimbursementId?: number }) {
  return request.get('/invoice/list', { params })
}

// 获取发票详情
export function getInvoiceDetail(id: number) {
  return request.get(`/invoice/${id}`)
}

// 删除发票
export function deleteInvoice(id: number) {
  return request.delete(`/invoice/${id}`)
}
