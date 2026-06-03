import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { ApiResponse } from '@/types/api'

const isDev = import.meta.env.DEV

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json; charset=utf-8'
  },
  responseType: 'json'
})

let isRedirectingToLogin = false

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
  '/search',
  '/auth'
]

function isPublicRequest(url: string = ''): boolean {
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

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>): any => {
    if (isDev) {
      console.log(`[Response] ${response.config.url}`, response.data)
    }
    const { code, message } = response.data
    if (code === 200) {
      return response.data
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      const requestUrl = error.config?.url || ''
      switch (status) {
        case 401:
          if (!isPublicRequest(requestUrl) && !isRedirectingToLogin) {
            isRedirectingToLogin = true
            ElMessage.error('登录已过期，请重新登录')
            const userStore = useUserStore()
            userStore.logout()
            setTimeout(() => {
              window.location.href = '/user/login'
              isRedirectingToLogin = false
            }, 100)
          }
          break
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