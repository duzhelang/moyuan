export interface UserPreferences {
  theme: 'light' | 'dark' | 'auto'
  language: 'zh' | 'en'
  fontSize: number
  pageSize: number
  sidebarCollapsed: boolean
}

export interface SearchHistory {
  keyword: string
  timestamp: number
  type: 'poem' | 'poet' | 'forum' | 'all'
}

export interface FormDraft {
  id: string
  type: 'poem' | 'forum' | 'comment'
  data: Record<string, any>
  timestamp: number
}

export interface SortPreferences {
  poem: 'latest' | 'popular' | 'likes'
  poet: 'latest' | 'popular' | 'name'
  forum: 'latest' | 'popular' | 'comments'
}

export interface CacheItem<T> {
  data: T
  timestamp: number
  expiresIn: number
}

export interface StorageConfig {
  prefix: string
  defaultExpiresIn: number
}