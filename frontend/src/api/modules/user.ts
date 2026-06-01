import request from '@/utils/request'
import type { User } from '@/types/model'
import type { LoginRequest, RegisterRequest, TokenResponse, PasswordUpdateRequest, UserUpdateRequest } from '@/types/api'

export function login(data: LoginRequest) {
  return request.post<TokenResponse>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return request.post<TokenResponse>('/auth/register', data)
}

export function getUserInfo() {
  return request.get<User>('/users/me')
}

export function updateUser(data: UserUpdateRequest) {
  return request.put<User>('/users/me', data)
}

export function updatePassword(data: PasswordUpdateRequest) {
  return request.put<void>('/users/me/password', data)
}

export function logout() {
  return Promise.resolve()
}

export function getUserList(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<{ records: User[]; total: number }>('/admin/users', { params })
}

export function getMyPosts(params?: { pageNum?: number; pageSize?: number }) {
  return request.get('/users/me/posts', { params })
}

export function getUserStats() {
  return request.get('/users/me/stats')
}