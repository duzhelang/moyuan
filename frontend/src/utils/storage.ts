const PREFIX = 'moyuan_'
const DEFAULT_EXPIRES_IN = 24 * 60 * 60 * 1000

export function getItem<T>(key: string): T | null {
  try {
    const value = localStorage.getItem(PREFIX + key)
    if (value) {
      const parsed = JSON.parse(value)
      if (parsed && typeof parsed === 'object' && 'expiresIn' in parsed) {
        if (Date.now() > parsed.timestamp + parsed.expiresIn) {
          localStorage.removeItem(PREFIX + key)
          return null
        }
        return parsed.data as T
      }
      return parsed as T
    }
    return null
  } catch (error) {
    console.error('获取本地存储失败:', error)
    return null
  }
}

export function setItem<T>(key: string, value: T, expiresIn?: number): void {
  try {
    const storageValue = {
      data: value,
      timestamp: Date.now(),
      expiresIn: expiresIn || DEFAULT_EXPIRES_IN
    }
    localStorage.setItem(PREFIX + key, JSON.stringify(storageValue))
  } catch (error) {
    console.error('设置本地存储失败:', error)
  }
}

export function removeItem(key: string): void {
  try {
    localStorage.removeItem(PREFIX + key)
  } catch (error) {
    console.error('删除本地存储失败:', error)
  }
}

export function clear(): void {
  try {
    const keys = Object.keys(localStorage)
    keys.forEach(key => {
      if (key.startsWith(PREFIX)) {
        localStorage.removeItem(key)
      }
    })
  } catch (error) {
    console.error('清空本地存储失败:', error)
  }
}

export function getToken(): string | null {
  return getItem<string>('token')
}

export function setToken(token: string): void {
  setItem('token', token)
}

export function removeToken(): void {
  removeItem('token')
}

export function getUserPreferences(): import('@/types/storage').UserPreferences {
  return getItem<import('@/types/storage').UserPreferences>('user_preferences') || {
    theme: 'light',
    language: 'zh',
    fontSize: 14,
    pageSize: 10,
    sidebarCollapsed: false
  }
}

export function setUserPreferences(preferences: import('@/types/storage').UserPreferences): void {
  setItem('user_preferences', preferences)
}

export function getSearchHistory(): import('@/types/storage').SearchHistory[] {
  return getItem<import('@/types/storage').SearchHistory[]>('search_history') || []
}

export function addSearchHistory(keyword: string, type: 'poem' | 'poet' | 'forum' | 'all' = 'all'): void {
  const history = getSearchHistory()
  const existingIndex = history.findIndex(item => item.keyword === keyword)
  
  if (existingIndex > -1) {
    history.splice(existingIndex, 1)
  }
  
  history.unshift({
    keyword,
    timestamp: Date.now(),
    type
  })
  
  setItem('search_history', history.slice(0, 20))
}

export function clearSearchHistory(): void {
  removeItem('search_history')
}

export function getFormDraft(type: 'poem' | 'forum' | 'comment'): import('@/types/storage').FormDraft | null {
  return getItem<import('@/types/storage').FormDraft>(`form_draft_${type}`)
}

export function setFormDraft(type: 'poem' | 'forum' | 'comment', data: Record<string, any>): void {
  setItem(`form_draft_${type}`, {
    id: `${type}_${Date.now()}`,
    type,
    data,
    timestamp: Date.now()
  })
}

export function removeFormDraft(type: 'poem' | 'forum' | 'comment'): void {
  removeItem(`form_draft_${type}`)
}

export function getSortPreferences(): import('@/types/storage').SortPreferences {
  return getItem<import('@/types/storage').SortPreferences>('sort_preferences') || {
    poem: 'latest',
    poet: 'latest',
    forum: 'latest'
  }
}

export function setSortPreferences(preferences: import('@/types/storage').SortPreferences): void {
  setItem('sort_preferences', preferences)
}

export function getCache<T>(key: string): T | null {
  return getItem<T>(`cache_${key}`)
}

export function setCache<T>(key: string, data: T, expiresIn?: number): void {
  setItem(`cache_${key}`, data, expiresIn)
}

export function removeCache(key: string): void {
  removeItem(`cache_${key}`)
}