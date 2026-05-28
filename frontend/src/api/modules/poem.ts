import request from '@/utils/request'
import type { Poem, PoemListParams, PageResult } from '@/types/model'
import type { PoemCreateRequest, PoemUpdateRequest } from '@/types/api'

export function getPoemList(params: PoemListParams) {
  return request.get<PageResult<Poem>>('/poems', { params })
}

export function getPoemById(id: number) {
  return request.get<Poem>(`/poems/${id}`)
}

export function getModernPoems() {
  return request.get<Poem[]>('/poems/modern')
}

export function createPoem(data: PoemCreateRequest) {
  return request.post<Poem>('/poems', data)
}

export function updatePoem(data: PoemUpdateRequest) {
  return request.put<Poem>(`/poems/${data.id}`, data)
}

export function deletePoem(id: number) {
  return request.delete<void>(`/poems/${id}`)
}

export function searchPoems(keyword: string) {
  return request.get<Poem[]>('/poems/search', { params: { keyword } })
}

export function getPoemsByDynasty(dynastyId: number, params?: { page?: number; size?: number }) {
  return request.get<PageResult<Poem>>(`/poems/dynasty/${dynastyId}`, { params })
}

export function getPoemsByCategory(categoryId: number, params?: { page?: number; size?: number }) {
  return request.get<PageResult<Poem>>(`/poems/category/${categoryId}`, { params })
}

export function getPoemsByPoet(poetId: number, params?: { page?: number; size?: number }) {
  return request.get<PageResult<Poem>>(`/poems/poet/${poetId}`, { params })
}

export function likePoem(id: number) {
  return request.post<void>(`/poems/${id}/like`)
}

export function favoritePoem(id: number) {
  return request.post<void>(`/poems/${id}/favorite`)
}