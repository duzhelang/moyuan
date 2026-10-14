import request from '@/utils/request'

export function getAdminVisionArticles(params: {
  pageNum?: number
  pageSize?: number
  status?: number | null
  category?: string
  keyword?: string
}) {
  return request.get<{ records: any[]; total: number }>('/admin/vision-articles', { params })
}

export function getAdminVisionArticleById(id: number) {
  return request.get<any>(`/admin/vision-articles/${id}`)
}

export function createAdminVisionArticle(data: any) {
  return request.post<any>('/admin/vision-articles', data)
}

export function updateAdminVisionArticle(id: number, data: any) {
  return request.put<any>(`/admin/vision-articles/${id}`, data)
}

export function updateAdminVisionArticleStatus(id: number, status: number) {
  return request.put<void>(`/admin/vision-articles/${id}/status`, { status })
}

export function updateAdminVisionArticleFeatured(id: number, isFeatured: number) {
  return request.put<void>(`/admin/vision-articles/${id}/featured`, { isFeatured })
}

export function deleteAdminVisionArticle(id: number) {
  return request.delete<void>(`/admin/vision-articles/${id}`)
}
