export interface User {
  id: number
  username: string
  email: string
  phone?: string
  avatar?: string
  nickname?: string
  gender?: number
  birthday?: string
  bio?: string
  status: number
  createdAt: string
  updatedAt: string
}

export interface Poem {
  id: number
  title: string
  content: string
  source?: string
  dynastyId?: number
  dynastyName?: string
  categoryId?: number
  categoryName?: string
  poetId?: number
  poetName?: string
  viewCount: number
  likeCount: number
  favoriteCount: number
  status: number
  isFeatured: number
  createdAt: string
  updatedAt: string
}

export interface Poet {
  id: number
  name: string
  dynastyId?: number
  dynastyName?: string
  birthYear?: string
  deathYear?: string
  description?: string
  avatar?: string
  createdAt: string
}

export interface Dynasty {
  id: number
  name: string
  startYear?: number
  endYear?: number
  description?: string
}

export interface Category {
  id: number
  name: string
  description?: string
  parentId?: number
  sortOrder: number
}

export interface ForumPost {
  id: number
  title: string
  content: string
  userId: number
  username: string
  avatar?: string
  viewCount: number
  likeCount: number
  commentCount: number
  status: number
  createdAt: string
  updatedAt: string
}

export interface Comment {
  id: number
  content: string
  userId: number
  username: string
  avatar?: string
  targetId: number
  targetType: number
  parentId?: number
  likeCount: number
  status: number
  createdAt: string
}

export interface PoemListParams {
  page?: number
  size?: number
  dynastyId?: number
  categoryId?: number
  poetId?: number
  keyword?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}