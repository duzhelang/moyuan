export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
  nickname?: string
  interests?: string[]
}

export interface TokenResponse {
  token: string
  refreshToken?: string
  expiresIn?: number
}

export interface PoemCreateRequest {
  title: string
  content: string
  source?: string
  dynastyId?: number
  categoryId?: number
  poetId?: number
}

export interface PoemUpdateRequest extends Partial<PoemCreateRequest> {
  id: number
}

export interface ForumPostCreateRequest {
  title: string
  content: string
  category?: string
}

export interface ForumPostUpdateRequest extends Partial<ForumPostCreateRequest> {
  id: number
}

export interface CommentCreateRequest {
  content: string
  postId?: number
  targetId?: number
  targetType?: number
  parentId?: number
  replyToUserId?: number
}

export interface PasswordUpdateRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export interface UserUpdateRequest {
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  gender?: number
  birthday?: string
  bio?: string
}