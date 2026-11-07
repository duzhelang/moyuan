import request from '@/utils/request'

export interface UserReport {
  id: number
  reporterId: number
  targetType: string
  targetId: number
  targetUserId?: number | null
  reasonType: string
  reason?: string
  evidence?: string
  status: string
  handleNote?: string
  handlerId?: number | null
  handledAt?: string | null
  aiFlag: number
  aiRiskLevel?: string | null
  aiReason?: string | null
  createTime?: string
  updateTime?: string
}

export interface ReportPage {
  list?: UserReport[]
  records?: UserReport[]
  total: number
  pageNum?: number
  pageSize?: number
}

export interface ReportCreatePayload {
  targetType: string
  targetId: number
  reasonType: string
  reason?: string
  evidence?: string
}

export interface ReportHandlePayload {
  status: string
  handleNote?: string
  action?: 'none' | 'hide' | 'delete'
}

/** 提交举报（用户对用户/内容违规） */
export function submitReport(data: ReportCreatePayload) {
  return request.post<UserReport>('/reports', data)
}

/** 我的举报记录 */
export function getMyReports(params: { pageNum?: number; pageSize?: number; status?: string }) {
  return request.get<ReportPage>('/reports/mine', { params })
}

/** 检查是否已举报 */
export function checkReported(params: { targetType: string; targetId: number }) {
  return request.get<{ reported: boolean }>('/reports/check', { params, silent: true })
}

/** 后台举报分页 */
export function getAdminReports(params: {
  page?: number
  size?: number
  status?: string
  targetType?: string
  aiFlag?: number | null
  keyword?: string
}) {
  return request.get<ReportPage>('/admin/reports', { params })
}

/** 后台举报统计 */
export function getReportStats() {
  return request.get<Record<string, number>>('/admin/reports/stats')
}

/** 处理举报 */
export function handleReport(id: number, data: ReportHandlePayload) {
  return request.put<void>(`/admin/reports/${id}/handle`, data)
}
