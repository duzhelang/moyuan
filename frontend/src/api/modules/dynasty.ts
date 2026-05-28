import request from '@/utils/request'
import type { Dynasty } from '@/types/model'

export function getDynastyList() {
  return request.get<Dynasty[]>('/dynasties')
}

export function getDynastyById(id: number) {
  return request.get<Dynasty>(`/dynasties/${id}`)
}

export function createDynasty(data: Partial<Dynasty>) {
  return request.post<Dynasty>('/dynasties', data)
}

export function updateDynasty(id: number, data: Partial<Dynasty>) {
  return request.put<Dynasty>(`/dynasties/${id}`, data)
}

export function deleteDynasty(id: number) {
  return request.delete<void>(`/dynasties/${id}`)
}