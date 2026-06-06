import request from '@/utils/request'
import type { Poem, Poet, ForumPost, PoemListParams, PageResult, PoemRatingsData, PoemRating } from '@/types/model'
import type { PoemCreateRequest, PoemUpdateRequest } from '@/types/api'

export function getPoemList(params: PoemListParams) {
  return request.get<PageResult<Poem>>('/poems', { params })
}

export function getPoemById(id: number) {
  return request.get<Poem>(`/poems/${id}`)
}

export function getModernPoems(params?: { pageNum?: number; pageSize?: number; isOriginal?: boolean; hasCertifiedPoet?: boolean; sortBy?: string }) {
  return request.get<PageResult<Poem>>('/poems/modern/page', { params })
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
  return request.get<Poem[]>('/search', { params: { keyword } })
}

export function getPoemsByDynasty(dynastyId: number, params?: { pageNum?: number; pageSize?: number }) {
  return request.get<PageResult<Poem>>('/poems', { params: { ...params, dynastyId } })
}

export function getPoemsByCategory(categoryId: number, params?: { pageNum?: number; pageSize?: number }) {
  return request.get<PageResult<Poem>>('/poems', { params: { ...params, categoryId } })
}

export function getPoemsByPoet(poetId: number, params?: { pageNum?: number; pageSize?: number }) {
  return request.get<PageResult<Poem>>('/poems', { params: { ...params, poetId } })
}

export function likePoem(id: number) {
  return request.post<void>(`/poems/${id}/like`)
}

export function favoritePoem(id: number) {
  return request.post<void>(`/poems/${id}/favorite`)
}

export function getMyFavorites(params?: { pageNum?: number; pageSize?: number }) {
  return request.get('/poems/favorites', { params })
}

export function getFeaturedPoems(params?: { pageSize?: number }) {
  return request.get<PageResult<Poem>>('/poems', { params: { ...params, pageNum: 1 } })
}

export function getRandomPoem() {
  return request.get<Poem>('/poems/random')
}

export function getDailyPoem() {
  return request.get<Poem[]>('/poems/daily')
}

export function globalSearch(keyword: string) {
  return request.get<{ poems: Poem[]; poets: Poet[]; posts: ForumPost[] }>('/search', { params: { keyword } })
}

export function getPoemRatings(poemId: number) {
  return request.get<PoemRatingsData>(`/poems/${poemId}/ratings`)
}

export function ratePoem(poemId: number, score: number, comment?: string) {
  return request.post<void>(`/poems/${poemId}/ratings`, null, {
    params: { score, comment }
  })
}

export function requestAiRating(poemId: number) {
  return request.post<void>(`/poems/${poemId}/ratings/ai`)
}

export function getAiRating(poemId: number) {
  return request.get<PoemRating>(`/poems/${poemId}/ratings/ai`)
}
