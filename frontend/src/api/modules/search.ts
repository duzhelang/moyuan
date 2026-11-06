import request from '@/utils/request'

export interface SmartSearchParams {
  dynastyId?: number
  categoryId?: number
  poetId?: number
  poetName?: string
  keyword?: string
  sortBy?: 'latest' | 'popular' | 'likes'
  pageNum?: number
  pageSize?: number
}

export interface PingZeMatch {
  poemId: number
  /** 命中的诗句原文 */
  line: string
  plainLine?: string
  /** 该句的完整平仄串 */
  pattern: string
  /** 命中位置（平仄串内起始下标，0 起） */
  start: number
}

export interface SmartSearchResult {
  list: any[]
  total: number
  searchLevel: 'exact' | 'fuzzy' | 'pinyin' | 'none'
  message: string
  suggestExternal?: boolean
  /** 自适应路由：pinyin / like / ngram / ngram->like / pingze */
  route?: string
  /** 候选集规模（路由命中的结果数） */
  candidateCount?: number
  /** 格律检索命中的诗句明细 */
  pingzeMatches?: PingZeMatch[]
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
