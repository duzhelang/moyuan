import request from '@/utils/request'

export interface StaticPage {
  id: number
  pageKey: string
  title: string
  content: string
  status: number
  createTime: string
  updateTime: string
}

export function getStaticPageByPageKey(pageKey: string) {
  return request.get<StaticPage>(`/static-pages/${pageKey}`)
}

export function getAdminStaticPages() {
  return request.get<StaticPage[]>('/admin/static-pages')
}

export function getAdminStaticPageById(id: number) {
  return request.get<StaticPage>(`/admin/static-pages/${id}`)
}

export function updateAdminStaticPage(id: number, data: Partial<StaticPage>) {
  return request.put<void>(`/admin/static-pages/${id}`, data)
}
