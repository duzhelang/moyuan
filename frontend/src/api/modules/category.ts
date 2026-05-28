import request from '@/utils/request'
import type { Category } from '@/types/model'

export function getCategoryList() {
  return request.get<Category[]>('/categories')
}

export function getCategoryById(id: number) {
  return request.get<Category>(`/categories/${id}`)
}

export function createCategory(data: Partial<Category>) {
  return request.post<Category>('/categories', data)
}

export function updateCategory(id: number, data: Partial<Category>) {
  return request.put<Category>(`/categories/${id}`, data)
}

export function deleteCategory(id: number) {
  return request.delete<void>(`/categories/${id}`)
}