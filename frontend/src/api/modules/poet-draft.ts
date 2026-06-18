import request from '@/utils/request'

export interface PoetDraft {
  id?: number
  poetId: number
  section: string
  content: string
  originalContent?: string
  editorId?: number
  status?: number
  reviewerId?: number
  reviewComment?: string
  reviewTime?: string
  ip?: string
  createTime?: string
  updateTime?: string
}

export function savePoetDraft(data: { poetId: number; section: string; content: string }) {
  return request.post('/poet-drafts', data)
}

export function getAdminPoetDrafts(params: { pageNum?: number; pageSize?: number; poetId?: number; status?: number }) {
  return request.get<{ records: PoetDraft[]; total: number }>('/admin/poet-drafts', { params })
}

export function getAdminPoetDraftById(id: number) {
  return request.get<PoetDraft>(`/admin/poet-drafts/${id}`)
}

export function reviewPoetDraft(id: number, status: number, reviewComment?: string) {
  return request.put(`/admin/poet-drafts/${id}/review`, null, {
    params: { status, reviewComment }
  })
}
