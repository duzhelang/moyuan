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
  role?: string
  status: number
  createTime: string
  updateTime: string
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
  translation?: string
  appreciation?: string
  background?: string
  viewCount: number
  likeCount: number
  favoriteCount: number
  status: number
  isFeatured: number
  isLiked?: boolean
  isFavorited?: boolean
  createTime: string
  updateTime: string
}

export interface Poet {
  id: number
  name: string
  courtesyName?: string
  pseudonym?: string
  dynastyId?: number
  dynastyName?: string
  birthYear?: number
  deathYear?: number
  birthplace?: string
  description?: string
  biography?: string
  lifeStory?: string
  influence?: string
  evaluation?: string
  anecdotes?: string
  avatar?: string
  status?: number
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
  createTime: string
  updateTime: string
}

export interface UserHistory {
  id: number
  userId: number
  targetId: number
  targetType: number
  createdAt: string
}

export interface Comment {
  id: number
  content: string
  userId: number
  username: string
  avatar?: string
  postId: number
  parentId?: number
  replyToUserId?: number
  likeCount: number
  status: number
  createTime: string
}

export interface PoemListParams {
  pageNum?: number
  pageSize?: number
  dynastyId?: number
  categoryId?: number
  poetId?: number
  keyword?: string
  sortBy?: 'latest' | 'popular' | 'likes'
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface VisionArticle {
  id: number
  title: string
  summary?: string
  content: string
  coverImage?: string
  author?: string
  category?: string
  viewCount: number
  likeCount: number
  status: number
  isFeatured: number
  sortOrder: number
  createTime: string
  updateTime: string
}
