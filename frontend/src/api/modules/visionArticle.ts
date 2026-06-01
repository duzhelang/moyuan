import request from '@/utils/request'
import type { VisionArticle, PageResult } from '@/types/model'

export function getVisionArticleList(params?: { pageNum?: number; pageSize?: number; category?: string }) {
  return request.get<PageResult<VisionArticle>>('/vision/articles', { params })
}

export function getFeaturedVisionArticles(params?: { pageNum?: number; pageSize?: number }) {
  return request.get<{ list: VisionArticle[]; total: number }>('/vision/articles/featured', { params })
}

export function getVisionArticleById(id: number) {
  return request.get<VisionArticle>(`/vision/articles/${id}`)
}

export function likeVisionArticle(id: number) {
  return request.post<void>(`/vision/articles/${id}/like`)
}