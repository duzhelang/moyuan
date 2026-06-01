import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { User } from '@/types/model'

export function getUserList(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<{ records: User[]; total: number }>('/admin/users', { params })
}

export function getUserById(id: number) {
  return request.get<User>(`/admin/users/${id}`)
}

export function createUser(data: { username: string; password: string; email?: string; nickname?: string; role?: string; phone?: string }) {
  return request.post<User>('/admin/users', data)
}

export function updateUser(id: number, data: Record<string, any>) {
  return request.put<User>(`/admin/users/${id}`, data)
}

export function deleteUser(id: number) {
  return request.delete<void>(`/admin/users/${id}`)
}

export function getAdminPoems(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<{ records: any[]; total: number }>('/admin/poems', { params })
}

export function createAdminPoem(data: any) {
  return request.post<any>('/admin/poems', data)
}

export function updateAdminPoem(id: number, data: any) {
  return request.put<any>(`/admin/poems/${id}`, data)
}

export function deleteAdminPoem(id: number) {
  return request.delete<void>(`/admin/poems/${id}`)
}

export function getAdminCategories(params: { page?: number; size?: number }) {
  return request.get<{ records: any[]; total: number }>('/admin/categories', { params })
}

export function createAdminCategory(data: any) {
  return request.post<any>('/admin/categories', data)
}

export function updateAdminCategory(id: number, data: any) {
  return request.put<any>(`/admin/categories/${id}`, data)
}

export function deleteAdminCategory(id: number) {
  return request.delete<void>(`/admin/categories/${id}`)
}

export function getAdminDynasties(params: { page?: number; size?: number }) {
  return request.get<{ records: any[]; total: number }>('/admin/dynasties', { params })
}

export function createAdminDynasty(data: any) {
  return request.post<any>('/admin/dynasties', data)
}

export function updateAdminDynasty(id: number, data: any) {
  return request.put<any>(`/admin/dynasties/${id}`, data)
}

export function deleteAdminDynasty(id: number) {
  return request.delete<void>(`/admin/dynasties/${id}`)
}

export function getAdminPoets(params: { page?: number; size?: number }) {
  return request.get<{ records: any[]; total: number }>('/admin/poets', { params })
}

export function createAdminPoet(data: any) {
  return request.post<any>('/admin/poets', data)
}

export function updateAdminPoet(id: number, data: any) {
  return request.put<any>(`/admin/poets/${id}`, data)
}

export function deleteAdminPoet(id: number) {
  return request.delete<void>(`/admin/poets/${id}`)
}

export function getAdminForumPosts(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<{ records: any[]; total: number }>('/admin/forum-posts', { params })
}

export function updateForumPostStatus(id: number, status: number) {
  return request.put<void>(`/admin/forum-posts/${id}/status`, { status })
}

export function deleteAdminForumPost(id: number) {
  return request.delete<void>(`/admin/forum-posts/${id}`)
}

export function getAdminStats() {
  return request.get<Record<string, number>>('/admin/stats')
}

export function getAdminStatsTrend() {
  return request.get<{
    dates: string[]
    poems: number[]
    posts: number[]
    users: number[]
  }>('/admin/stats/trend')
}

export function getAdminLogs(params?: { page?: number; size?: number; keyword?: string; startTime?: string; endTime?: string }) {
  return request.get<{ records: any[]; total: number }>('/admin/logs', { params })
}

export function getAdminPoetFeatured(params: { page?: number; size?: number }) {
  return request.get<{ records: any[]; total: number }>('/admin/poet-featured', { params })
}

export function getAdminPoetFeaturedById(id: number) {
  return request.get<any>(`/admin/poet-featured/${id}`)
}

export function createAdminPoetFeatured(data: any) {
  return request.post<any>('/admin/poet-featured', data)
}

export function updateAdminPoetFeatured(id: number, data: any) {
  return request.put<any>(`/admin/poet-featured/${id}`, data)
}

export function deleteAdminPoetFeatured(id: number) {
  return request.delete<void>(`/admin/poet-featured/${id}`)
}

export function getHomeNavigationList(params?: { type?: string }) {
  return request.get<any[]>('/home-navigation', { params })
}

export function getAdminHomeNavigation(params: { page?: number; size?: number; type?: string }) {
  return request.get<{ records: any[]; total: number }>('/admin/home-navigation/manage', { params })
}

export function createHomeNavigation(data: any) {
  return request.post<any>('/admin/home-navigation', data)
}

export function updateHomeNavigation(id: number, data: any) {
  return request.put<any>(`/admin/home-navigation/${id}`, data)
}

export function deleteHomeNavigation(id: number) {
  return request.delete<void>(`/admin/home-navigation/${id}`)
}
