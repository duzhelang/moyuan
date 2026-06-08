import request from '@/utils/request'
import type { Poet } from '@/types/model'

export function getPoetList(params?: { pageNum?: number; pageSize?: number; dynastyId?: number; keyword?: string; poetType?: string }) {
  return request.get<{ list: Poet[]; total: number; pageNum: number; pageSize: number }>('/poets', { params })
}

export function getPoetById(id: number) {
  return request.get<Poet>(`/poets/${id}`)
}

export function submitPoetSuggestion(data: { poetId: number; section: string; content: string }) {
  return request.post('/poet-suggestions', data)
}
