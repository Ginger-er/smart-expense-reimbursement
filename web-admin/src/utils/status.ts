/**
 * 状态字典：数字状态码 → 展示文案 + Element Plus tag 类型
 * 供各列表/详情页统一引用，避免散落硬编码
 */

export type StatusTag = 'primary' | 'success' | 'warning' | 'info' | 'danger'

export interface StatusMeta {
  label: string
  tag: StatusTag
}

// ===== 报销单状态 =====
const REIMBURSEMENT_STATUS: Record<number, StatusMeta> = {
  0: { label: '草稿', tag: 'info' },
  1: { label: '待审批', tag: 'primary' },
  2: { label: '审批中', tag: 'warning' },
  3: { label: '已通过', tag: 'success' },
  4: { label: '已驳回', tag: 'danger' },
  5: { label: '已打款', tag: 'info' }
}

// ===== 出差申请状态（单级审批，无「审批中」） =====
const TRIP_STATUS: Record<number, StatusMeta> = {
  0: { label: '草稿', tag: 'info' },
  1: { label: '已提交', tag: 'primary' },
  3: { label: '已通过', tag: 'success' },
  4: { label: '已驳回', tag: 'danger' }
}

// ===== 审批动作 =====
const APPROVAL_ACTION: Record<number, StatusMeta & { color: string }> = {
  1: { label: '通过', tag: 'success', color: '#34c759' },
  2: { label: '驳回', tag: 'danger', color: '#ff3b30' },
  3: { label: '转办', tag: 'warning', color: '#ff9500' }
}

// ===== 发票类型 =====
const INVOICE_TYPE: Record<number, StatusMeta> = {
  1: { label: '交通', tag: 'success' },
  2: { label: '住宿', tag: 'warning' },
  3: { label: '餐饮', tag: 'info' },
  4: { label: '其他', tag: 'info' }
}

// ===== 发票 OCR 状态 =====
const INVOICE_OCR_STATUS: Record<number, StatusMeta> = {
  0: { label: '待识别', tag: 'info' },
  1: { label: '识别成功', tag: 'success' },
  2: { label: '识别失败', tag: 'danger' },
  3: { label: '人工修正', tag: 'warning' }
}

export function reimbursementStatus(s: number): StatusMeta {
  return REIMBURSEMENT_STATUS[s] || { label: String(s), tag: 'info' }
}

export function tripStatus(s: number): StatusMeta {
  return TRIP_STATUS[s] || { label: String(s), tag: 'info' }
}

export function approvalAction(a: number): StatusMeta & { color: string } {
  return APPROVAL_ACTION[a] || { label: String(a), tag: 'info', color: '#aeaeb2' }
}

export function invoiceType(t?: number): StatusMeta {
  return INVOICE_TYPE[t ?? 0] || { label: '', tag: 'info' }
}

export function invoiceOcrStatus(s: number): StatusMeta {
  return INVOICE_OCR_STATUS[s] || { label: String(s), tag: 'info' }
}
