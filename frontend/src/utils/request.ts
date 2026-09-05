import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { ApiResponse } from '@/types/api'

const isDev = import.meta.env.DEV

const REQUEST_NONCE_HEADER = 'x-request-nonce'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 120000,
  headers: {
    'Content-Type': 'application/json; charset=utf-8'
  },
  responseType: 'json'
})

// 用于"静默刷新"请求的独立实例，不挂载业务响应拦截器，避免 401 时相互触发的死循环
const refreshService: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 120000,
  headers: {
    'Content-Type': 'application/json; charset=utf-8'
  }
})

let isRedirectingToLogin = false
// 刷新令牌请求的在途 Promise（避免并发 401 时重复刷新）
let refreshPromise: Promise<boolean> | null = null

// 需要绕过 401 自动刷新处理的请求（如 refresh / logout 本身）
const SKIP_REFRESH_PATHS = ['/auth/refresh', '/auth/logout']

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    if (isDev) {
      console.log(`[Request] ${config.method?.toUpperCase()} ${config.url}`)
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

const PUBLIC_PATHS = [
  '/poems',
  '/poets',
  '/dynasties',
  '/categories',
  '/forum/posts',
  '/forum/comments',
  '/poet-featured',
  '/home-navigation',
  '/vision',
  '/ai',
  '/rhyme',
  '/search',
  '/static-pages',
  '/auth'
]

function isPublicRequest(url: string = ''): boolean {
  if (url.includes('/admin/')) {
    return false
  }
  return PUBLIC_PATHS.some(path => url.includes(path))
}

function handleNetworkError(error: any) {
  if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
    ElMessage.error('请求超时，请检查网络后重试')
  } else if (!navigator.onLine) {
    ElMessage.error('网络已断开，请检查网络连接')
  } else {
    ElMessage.error('网络连接失败，请稍后重试')
  }
}

/**
 * 尝试用 refresh token 换新 access token。
 * 成功：持久化新 token 并返回 true；失败：清理登录态并返回 false。
 */
async function tryRefreshToken(): Promise<boolean> {
  const userStore = useUserStore()
  if (!userStore.refreshToken) {
    return false
  }
  try {
    const response = await refreshService.post<ApiResponse<any>>('/auth/refresh', {
      refreshToken: userStore.refreshToken
    })
    if (response.data?.code === '200' && response.data?.data?.token) {
      userStore.setToken(response.data.data.token)
      if (response.data.data.refreshToken) {
        // 后端实现了 refresh token 轮换，同步更新本地 refresh token
        userStore.refreshToken = response.data.data.refreshToken
        localStorage.setItem('refresh_token', response.data.data.refreshToken)
      }
      return true
    }
    return false
  } catch (e) {
    console.warn('刷新令牌失败:', e)
    return false
  }
}

/**
 * 统一的 401 处理：若存在 refresh token 且非 refresh/logout 请求，则静默刷新后重放原请求；
 * 刷新失败或无 refresh token 时，才提示并跳转登录。
 */
async function handleUnauthorized(originalError: any): Promise<any> {
  const requestUrl = originalError?.config?.url || ''
  if (skipRefresh(requestUrl)) {
    return Promise.reject(originalError)
  }
  if (isPublicRequest(requestUrl) || isRedirectingToLogin) {
    return Promise.reject(originalError)
  }

  if (!refreshPromise) {
    refreshPromise = tryRefreshToken().finally(() => {
      refreshPromise = null
    })
  }
  const refreshed = await refreshPromise

  if (refreshed) {
    const config = originalError.config
    if (config) {
      // 用新的 access token 重放原请求
      const userStore = useUserStore()
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${userStore.token}`
      config.__isRetry = true
      return service(config)
    }
    return Promise.reject(originalError)
  }

  if (!isRedirectingToLogin) {
    isRedirectingToLogin = true
    ElMessage.error('登录已过期，请重新登录')
    const userStore = useUserStore()
    userStore.logout()
    setTimeout(() => {
      window.location.href = '/user/login'
      isRedirectingToLogin = false
    }, 100)
  }
  return Promise.reject(originalError)
}

function skipRefresh(url: string): boolean {
  return SKIP_REFRESH_PATHS.some(path => url.includes(path))
}

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>): any => {
    if (isDev) {
      console.log(`[Response] ${response.config.url}`, response.data)
    }
    const { code, message } = response.data
    if (code === '200') {
      return response.data
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  async (error) => {
    // 二次重试时若仍 401，避免再次触发刷新
    if (error.config?.__isRetry) {
      return Promise.reject(error)
    }
    if (error.response) {
      const { status } = error.response
      const requestUrl = error.config?.url || ''
      switch (status) {
        case 401:
          return handleUnauthorized(error)
        case 403:
          ElMessage.error('没有权限访问该资源')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 422:
          ElMessage.error(error.response.data?.message || '请求参数错误')
          break
        case 429:
          ElMessage.error('请求过于频繁，请稍后重试')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        case 502:
        case 503:
        case 504:
          ElMessage.error('服务暂时不可用，请稍后重试')
          break
        default:
          ElMessage.error(error.response.data?.message || error.message || '请求失败')
      }
    } else {
      handleNetworkError(error)
    }
    return Promise.reject(error)
  }
)

declare module 'axios' {
  export interface InternalAxiosRequestConfig {
    __isRetry?: boolean
  }
}

export function get<T>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.get(url, config)
}

export function post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.post(url, data, config)
}

export function put<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.put(url, data, config)
}

export function del<T>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.delete(url, config)
}

export default service