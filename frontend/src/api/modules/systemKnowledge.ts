import request from '@/utils/request'

/** 系统知识库条目（与后端 SystemKnowledge 实体字段一致） */
export interface SystemKnowledgeItem {
  id: number
  code: string
  title: string
  category: string
  keywords?: string | null
  aliases?: string | null
  body: string
  steps?: string | null
  faq?: string | null
  route?: string | null
  isShow?: number
  sortOrder?: number
  createTime?: string
  updateTime?: string
}

export interface SystemKnowledgeQuery {
  pageNum?: number
  pageSize?: number
  category?: string
  keyword?: string
}

export interface SystemKnowledgePage {
  records: SystemKnowledgeItem[]
  total: number
  pageNum: number
  pageSize: number
}

/** 公开接口：拉取全部启用的知识条目（供 AI 小助手检索） */
export function getSystemKnowledgeList() {
  return request.get<SystemKnowledgeItem[]>('/system-knowledge')
}

/** 后台：分页查询 */
export function getAdminSystemKnowledgeList(params: SystemKnowledgeQuery) {
  return request.get<SystemKnowledgePage>('/admin/system-knowledge', { params })
}

/** 后台：详情 */
export function getAdminSystemKnowledgeDetail(id: number) {
  return request.get<SystemKnowledgeItem>(`/admin/system-knowledge/${id}`)
}

/** 后台：新建 */
export function createAdminSystemKnowledge(data: Partial<SystemKnowledgeItem>) {
  return request.post<SystemKnowledgeItem>('/admin/system-knowledge', data)
}

/** 后台：更新 */
export function updateAdminSystemKnowledge(id: number, data: Partial<SystemKnowledgeItem>) {
  return request.put<SystemKnowledgeItem>(`/admin/system-knowledge/${id}`, data)
}

/** 后台：启停 */
export function updateAdminSystemKnowledgeShow(id: number, isShow: number) {
  return request.put<void>(`/admin/system-knowledge/${id}/show`, { isShow })
}

/** 后台：删除 */
export function deleteAdminSystemKnowledge(id: number) {
  return request.delete<void>(`/admin/system-knowledge/${id}`)
}
