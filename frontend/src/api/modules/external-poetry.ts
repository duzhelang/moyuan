import axios from 'axios'
import request from '@/utils/request'

// 今日诗词 API
const jinrishiciApi = axios.create({
  baseURL: 'https://v2.jinrishici.com',
  timeout: 10000
})

// PoetryDB API
const poetryDbApi = axios.create({
  baseURL: 'https://poetrydb.org',
  timeout: 10000
})

// 今日诗词响应类型
export interface JinrishiciResponse {
  status: string
  data: {
    id: string
    content: string
    popularity: number
    origin: {
      title: string
      dynasty: string
      author: string
      content: string[]
      translate: string[] | null
    }
    matchTags: string[]
    recommendedReason: string
    cacheAt: string
  }
  token: string
  ipAddress: string
  warning: string | null
}

// PoetryDB 响应类型
export interface PoetryDbPoem {
  title: string
  author: string
  lines: string[]
  linecount: string
}

/**
 * 获取今日推荐诗词（中文古诗词）
 */
export async function getDailyPoetry(): Promise<JinrishiciResponse['data'] | null> {
  try {
    const response = await jinrishiciApi.get<JinrishiciResponse>('/one.json')
    if (response.data.status === 'success') {
      return response.data.data
    }
    return null
  } catch (error) {
    console.error('获取今日诗词失败:', error)
    return null
  }
}

/**
 * 按标题搜索英文诗词
 */
export async function searchPoetryByTitle(title: string): Promise<PoetryDbPoem[]> {
  try {
    const response = await poetryDbApi.get<PoetryDbPoem[]>(`/title/${encodeURIComponent(title)}`)
    if (Array.isArray(response.data)) {
      return response.data
    }
    return []
  } catch (error) {
    console.error('搜索诗词失败:', error)
    return []
  }
}

/**
 * 按作者搜索英文诗词
 */
export async function searchPoetryByAuthor(author: string): Promise<PoetryDbPoem[]> {
  try {
    const response = await poetryDbApi.get<PoetryDbPoem[]>(`/author/${encodeURIComponent(author)}`)
    if (Array.isArray(response.data)) {
      return response.data
    }
    return []
  } catch (error) {
    console.error('搜索作者诗词失败:', error)
    return []
  }
}

/**
 * 获取随机英文诗词
 */
export async function getRandomPoetry(count: number = 1): Promise<PoetryDbPoem[]> {
  try {
    const response = await poetryDbApi.get<PoetryDbPoem[]>(`/random/${count}`)
    if (Array.isArray(response.data)) {
      return response.data
    }
    return []
  } catch (error) {
    console.error('获取随机诗词失败:', error)
    return []
  }
}

/**
 * 按标题和作者组合搜索
 */
export async function searchPoetry(title: string, author: string): Promise<PoetryDbPoem[]> {
  try {
    const response = await poetryDbApi.get<PoetryDbPoem[]>(
      `/title,author/${encodeURIComponent(title)};${encodeURIComponent(author)}`
    )
    if (Array.isArray(response.data)) {
      return response.data
    }
    return []
  } catch (error) {
    console.error('组合搜索诗词失败:', error)
    return []
  }
}

// ========== 后端诗词搜索推荐API ==========

export interface PoemSearchResult {
  id?: number
  title: string
  content: string
  author?: string
  dynasty?: string
  source: 'local' | 'external'
  recommendScore?: number
  recommendReason?: string
  viewCount?: number
  likeCount?: number
  favoriteCount?: number
}

/**
 * 诗词搜索（支持协同过滤推荐）
 */
export async function searchPoemsWithRecommend(
  keyword: string,
  page: number = 1,
  size: number = 10
): Promise<PoemSearchResult[]> {
  try {
    const response = await request.get('/search/poems', {
      params: { keyword, page, size }
    })
    return response.data || []
  } catch (error) {
    console.error('诗词搜索失败:', error)
    return []
  }
}

/**
 * 获取推荐诗词（基于协同过滤）
 */
export async function getRecommendedPoems(limit: number = 10): Promise<PoemSearchResult[]> {
  try {
    const response = await request.get('/search/poems/recommended', {
      params: { limit }
    })
    return response.data || []
  } catch (error) {
    console.error('获取推荐诗词失败:', error)
    return []
  }
}

/**
 * 获取热门诗词
 */
export async function getPopularPoems(limit: number = 10): Promise<PoemSearchResult[]> {
  try {
    const response = await request.get('/search/poems/popular', {
      params: { limit }
    })
    return response.data || []
  } catch (error) {
    console.error('获取热门诗词失败:', error)
    return []
  }
}

/**
 * 从外部API获取古诗词
 */
export async function getExternalPoems(
  keyword: string,
  limit: number = 5
): Promise<PoemSearchResult[]> {
  try {
    const response = await request.get('/search/poems/external', {
      params: { keyword, limit }
    })
    return response.data || []
  } catch (error) {
    console.error('获取外部诗词失败:', error)
    return []
  }
}

// ========== 接口盒子 API (apihz.cn) - 通过后端代理 ==========

export interface ApihzPoetryItem {
  name: string
  content: string
  author: string
  dynasty: string
  tag: string | null
  ywjzsy: string | null
  ywjzse: string | null
  czbj: string | null
  jsy: string | null
  jse: string | null
  sxy: string | null
  sxe: string | null
  jj: string | null
  wyzs: string | null
  yj: string | null
  xzsf: string | null
  dj: string | null
  pj: string | null
  jx: string | null
}

export async function searchApihzPoems(
  keyword: string,
  limit: number = 10
): Promise<PoemSearchResult[]> {
  try {
    const response = await request.get('/search/poems/external', {
      params: { keyword, limit }
    })
    return response.data || []
  } catch (error) {
    console.error('外部诗词搜索失败:', error)
    return []
  }
}

export async function getApihzPoetryDetail(
  keyword: string
): Promise<ApihzPoetryItem | null> {
  try {
    const response = await request.get('/search/poems/external/detail', {
      params: { keyword }
    })
    return response.data || null
  } catch (error) {
    console.error('获取接口盒子诗词详情失败:', error)
    return null
  }
}
