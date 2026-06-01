import request from '@/utils/request'

export interface PoetFeatured {
  id: number
  poetId: number | null
  name: string
  dynasty: string
  description: string
  biography: string
  imageUrl: string
  sortOrder: number
  status: number
}

export function getRandomPoetFeatured(count: number = 4) {
  return request.get<PoetFeatured[]>('/poet-featured/random', { params: { count } })
}

export function getPoetFeaturedList() {
  return request.get<PoetFeatured[]>('/poet-featured/list')
}

export function getPoetFeaturedDetail(id: number) {
  return request.get<PoetFeatured>(`/poet-featured/${id}`)
}