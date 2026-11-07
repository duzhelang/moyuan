import request from '@/utils/request'

/** 看板：统计 + 工单列 + 审核员负载（管理员/审核员返回范围不同） */
export function getAuditBoard() {
  return request.get<any>('/admin/audit-pipeline/board')
}

/** 审核记录：审核员只看自己，管理员可看全组并按审核员筛选 */
export function getAuditRecords(params: {
  page?: number
  size?: number
  bizType?: string
  outcome?: string
  reviewerId?: number
  startDate?: string
  endDate?: string
  keyword?: string
}) {
  return request.get<any>('/admin/audit-pipeline/records', { params })
}

/** 扫描六类待审数据，补建工单（已有工单的跳过）并尝试分派（管理员） */
export function scanAuditTickets() {
  return request.post<any>('/admin/audit-pipeline/scan')
}

/** 把待分派工单按规则重新分派一次，返回分派成功的条数（管理员） */
export function assignPendingTickets() {
  return request.post<number>('/admin/audit-pipeline/assign-pending')
}

export function claimTicket(id: number) {
  return request.post<void>(`/admin/audit-pipeline/${id}/claim`)
}

export function transferTicket(id: number, reviewerId: number) {
  return request.post<void>(`/admin/audit-pipeline/${id}/transfer`, null, { params: { reviewerId } })
}

export function returnTicket(id: number) {
  return request.post<void>(`/admin/audit-pipeline/${id}/return`)
}

export function getAuditRules() {
  return request.get<any[]>('/admin/audit-pipeline/rules')
}

export function saveAuditRule(data: {
  id?: number
  scopeType: string
  scopeValue?: string | null
  reviewerId: number
  priority?: number
  enabled?: number
  remark?: string
}) {
  return request.post<void>('/admin/audit-pipeline/rules', data)
}

export function deleteAuditRule(id: number) {
  return request.delete<void>(`/admin/audit-pipeline/rules/${id}`)
}

export function getAuditReviewers() {
  return request.get<any[]>('/admin/audit-pipeline/reviewers')
}

/** 可登记为审核员的候选账号（role=reviewer 且尚未登记） */
export function getReviewerCandidates() {
  return request.get<any[]>('/admin/audit-pipeline/reviewer-candidates')
}

export function addReviewer(data: {
  userId: number
  displayName?: string
  specialties?: string
  maxConcurrent?: number
  status?: number
}) {
  return request.post<void>('/admin/audit-pipeline/reviewers', data)
}

export function updateReviewer(id: number, data: {
  displayName?: string
  specialties?: string
  maxConcurrent?: number
  status?: number
}) {
  return request.put<void>(`/admin/audit-pipeline/reviewers/${id}`, data)
}

export function deleteReviewer(id: number) {
  return request.delete<void>(`/admin/audit-pipeline/reviewers/${id}`)
}
