import axios from 'axios'

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
