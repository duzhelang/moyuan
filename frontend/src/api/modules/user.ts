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
  return request.get<User>('/user/info')
}

export function updateUser(data: UserUpdateRequest) {
  return request.put<User>('/user/update', data)
}

export function updatePassword(data: PasswordUpdateRequest) {
  return request.put<void>('/user/password', data)
}

export function logout() {
  return request.post<void>('/auth/logout')
}

export function getUserList(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<{ records: User[]; total: number }>('/user/list', { params })
}