import request from './request'

// 报表统计
export function getReportStats(params?: { startDate?: string; endDate?: string }) {
  return request.get('/report/stats', { params })
}

// 导出报表（Excel，返回 Blob）
export function exportReport(params?: { startDate?: string; endDate?: string }): Promise<Blob> {
  return request.get('/report/export', { params, responseType: 'blob' }) as unknown as Promise<Blob>
}
