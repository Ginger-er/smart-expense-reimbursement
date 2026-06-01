import { getTripList } from './trip'
import { getReimbursementList } from './reimbursement'

export interface ApprovalItem {
  id: number
  type: 'REIMBURSEMENT' | 'TRIP'
  orderNo: string
  applicantName: string
  deptName: string
  title: string
  amount: number
  status: number
  submitTime: string
}

function mapReimbursement(r: any): ApprovalItem {
  return {
    id: r.id,
    type: 'REIMBURSEMENT',
    orderNo: r.orderNo,
    applicantName: r.applicantName,
    deptName: r.deptName,
    title: r.remark || r.orderNo,
    amount: r.amount,
    status: r.status,
    submitTime: r.createTime
  }
}

function mapTrip(t: any): ApprovalItem {
  return {
    id: t.id,
    type: 'TRIP',
    orderNo: t.tripNo,
    applicantName: t.applicantName,
    deptName: t.deptName,
    title: t.reason || t.destination,
    amount: t.budget,
    status: t.status,
    submitTime: t.createTime
  }
}

// 待审批：报销(status 1,2) + 出差(status 1,2)
export async function getPendingApprovals(params: { keyword?: string }) {
  const base = { pageNum: 1, pageSize: 500, keyword: params.keyword }
  const results = await Promise.all([
    getReimbursementList({ ...base, status: 1 }),
    getReimbursementList({ ...base, status: 2 }),
    getTripList({ ...base, status: 1 }),
    getTripList({ ...base, status: 2 })
  ])
  const items: ApprovalItem[] = []
  ;(results[0] as any).data?.forEach((r: any) => items.push(mapReimbursement(r)))
  ;(results[1] as any).data?.forEach((r: any) => items.push(mapReimbursement(r)))
  ;(results[2] as any).data?.forEach((t: any) => items.push(mapTrip(t)))
  ;(results[3] as any).data?.forEach((t: any) => items.push(mapTrip(t)))
  return items
}

// 已审批：报销(status 3,4) + 出差(status 3,4)
export async function getProcessedApprovals(params: { keyword?: string }) {
  const base = { pageNum: 1, pageSize: 500, keyword: params.keyword }
  const results = await Promise.all([
    getReimbursementList({ ...base, status: 3 }),
    getReimbursementList({ ...base, status: 4 }),
    getTripList({ ...base, status: 3 }),
    getTripList({ ...base, status: 4 })
  ])
  const items: ApprovalItem[] = []
  ;(results[0] as any).data?.forEach((r: any) => items.push(mapReimbursement(r)))
  ;(results[1] as any).data?.forEach((r: any) => items.push(mapReimbursement(r)))
  ;(results[2] as any).data?.forEach((t: any) => items.push(mapTrip(t)))
  ;(results[3] as any).data?.forEach((t: any) => items.push(mapTrip(t)))
  return items
}
