import request from '@/utils/request'
import type { UserHistory } from '@/types/model'

export function addHistory(targetId: number, targetType: number) {
  return request.post<void>('/history', null, { params: { targetId, targetType } })
}

export function getHistory(params?: { targetType?: number; pageNum?: number; pageSize?: number }) {
  return request.get<{ list: UserHistory[]; total: number; pageNum: number; pageSize: number }>('/history', { params })
}

export function clearHistory(targetType?: number) {
  return request.delete<void>('/history', { params: targetType != null ? { targetType } : {} })
}