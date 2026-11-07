import request from '@/utils/request'

export interface UserNotification {
  id: number
  userId: number
  type: string
  title: string
  content?: string
  bizType?: string | null
  bizId?: number | null
  link?: string | null
  isRead: number
  readAt?: string | null
  createTime?: string
}

export interface NotificationPage {
  list: UserNotification[]
  total: number
  pageNum: number
  pageSize: number
}

export function getNotifications(params: { pageNum?: number; pageSize?: number; unreadOnly?: boolean }) {
  return request.get<NotificationPage>('/notifications', { params })
}

export function getUnreadCount() {
  return request.get<{ count: number }>('/notifications/unread-count', { silent: true })
}

export function markNotificationRead(id: number) {
  return request.put<void>(`/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return request.put<void>('/notifications/read-all')
}
