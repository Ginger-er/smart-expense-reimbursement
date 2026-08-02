import { getTripList } from './trip'
import { getReimbursementList } from './reimbursement'
import { fetchAll } from '@/utils/page'

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
  const keyword = params.keyword
  const [r1, r2, t1, t2] = await Promise.all([
    fetchAll(pageNum => getReimbursementList({ pageNum, pageSize: 100, status: 1, keyword })),
    fetchAll(pageNum => getReimbursementList({ pageNum, pageSize: 100, status: 2, keyword })),
    fetchAll(pageNum => getTripList({ pageNum, pageSize: 100, status: 1, keyword })),
    fetchAll(pageNum => getTripList({ pageNum, pageSize: 100, status: 2, keyword }))
  ])
  const items: ApprovalItem[] = []
  r1.forEach((r: any) => items.push(mapReimbursement(r)))
  r2.forEach((r: any) => items.push(mapReimbursement(r)))
  t1.forEach((t: any) => items.push(mapTrip(t)))
  t2.forEach((t: any) => items.push(mapTrip(t)))
  return items
}

// 已审批：报销(status 3,4) + 出差(status 3,4)
export async function getProcessedApprovals(params: { keyword?: string }) {
  const keyword = params.keyword
  const [r1, r2, t1, t2] = await Promise.all([
    fetchAll(pageNum => getReimbursementList({ pageNum, pageSize: 100, status: 3, keyword })),
    fetchAll(pageNum => getReimbursementList({ pageNum, pageSize: 100, status: 4, keyword })),
    fetchAll(pageNum => getTripList({ pageNum, pageSize: 100, status: 3, keyword })),
    fetchAll(pageNum => getTripList({ pageNum, pageSize: 100, status: 4, keyword }))
  ])
  const items: ApprovalItem[] = []
  r1.forEach((r: any) => items.push(mapReimbursement(r)))
  r2.forEach((r: any) => items.push(mapReimbursement(r)))
  t1.forEach((t: any) => items.push(mapTrip(t)))
  t2.forEach((t: any) => items.push(mapTrip(t)))
  return items
}
