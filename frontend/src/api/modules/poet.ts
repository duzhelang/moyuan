import request from '@/utils/request'
import type { Poet } from '@/types/model'

export function getPoetList(params?: { pageNum?: number; pageSize?: number; dynastyId?: number; keyword?: string }) {
  return request.get<{ list: Poet[]; total: number; pageNum: number; pageSize: number }>('/poets', { params })
}

export function getPoetById(id: number) {
  return request.get<Poet>(`/poets/${id}`)
}
