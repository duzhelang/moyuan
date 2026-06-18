import request from '@/utils/request'

export interface SmartSearchParams {
  dynastyId?: number
  categoryId?: number
  poetId?: number
  keyword?: string
  sortBy?: 'latest' | 'popular' | 'likes'
  pageNum?: number
  pageSize?: number
}

export interface SmartSearchResult {
  list: any[]
  total: number
  searchLevel: 'exact' | 'fuzzy' | 'pinyin' | 'none'
  message: string
  suggestExternal?: boolean
}

export interface SearchSuggestion {
  keyword: string
  type: 'poem' | 'poet'
}

export async function smartSearch(params: SmartSearchParams): Promise<SmartSearchResult> {
  const response = await request.get('/search/smart', { params })
  return response.data
}

export async function getSearchSuggestions(keyword: string, limit: number = 10): Promise<string[]> {
  const response = await request.get('/search/suggestions', {
    params: { keyword, limit }
  })
  return response.data || []
}

export async function getHotSearches(limit: number = 8): Promise<string[]> {
  const response = await request.get('/search/hot', {
    params: { limit }
  })
  return response.data || []
}

export async function getSearchHistory(limit: number = 10): Promise<string[]> {
  const response = await request.get('/search/history', {
    params: { limit }
  })
  return response.data || []
}

export async function clearSearchHistory(): Promise<void> {
  await request.delete('/search/history')
}
