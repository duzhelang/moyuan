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
  poetVerified?: number
  poetProfileId?: number
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
  isOriginal?: boolean
  poemType?: 'classical' | 'modern'
  avgScore?: number
  ratingCount?: number
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
  biography?: string
  lifeStory?: string
  influence?: string
  evaluation?: string
  anecdotes?: string
  avatar?: string
  poetType?: string
  status?: number
  createTime: string
  updateTime?: string
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
  createTime: string
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
  poemType?: 'classical' | 'modern'
  isOriginal?: boolean
  hasCertifiedPoet?: boolean
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface PoetProfile {
  id: number
  userId: number
  penName?: string
  realName?: string
  specialty?: string
  introduction?: string
  literaryConcept?: string
  achievements?: string
  contactInfo?: string
  verifiedStatus: number
  verifiedTime?: string
  verifiedReason?: string
  workCount: number
  likeCount: number
  favoriteCount: number
  followerCount: number
  createTime: string
  updateTime: string
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

export interface PoemRating {
  id: number
  poemId: number
  userId?: number
  username?: string
  avatar?: string
  score: number
  ratingType: number
  dimension?: string
  comment?: string
  aiModel?: string
  aiAnalysis?: string
  createTime: string
}

export interface PoemRatingsData {
  averageScore: number
  ratingCount: number
  aiRating?: PoemRating
  userRatings: PoemRating[]
}
