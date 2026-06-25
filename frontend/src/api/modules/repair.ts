import request from '@/utils/request'

export function createRepairOrder(data: { title: string; description: string; category: string; priority?: number; images?: string }) {
  return request.post<any>('/repair/orders', data)
}

export function getMyRepairOrders(params: { pageNum?: number; pageSize?: number; status?: number | null }) {
  return request.get<{ list: any[]; total: number }>('/repair/orders', { params })
}

export function getRepairOrderDetail(id: number) {
  return request.get<any>(`/repair/orders/${id}`)
}

export function closeRepairOrder(id: number) {
  return request.put<void>(`/repair/orders/${id}/close`)
}

export function submitSatisfaction(id: number, data: { satisfaction: number; comment?: string }) {
  return request.put<void>(`/repair/orders/${id}/satisfaction`, data)
}

export function addRepairComment(orderId: number, data: { content: string; images?: string; isInternal?: number }) {
  return request.post<any>(`/repair/orders/${orderId}/comments`, data)
}

export function getRepairComments(orderId: number) {
  return request.get<any[]>(`/repair/orders/${orderId}/comments`)
}