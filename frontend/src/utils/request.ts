import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { ApiResponse } from '@/types/api'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
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

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
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
          if (!isPublicRequest(requestUrl)) {
            ElMessage.error('登录已过期，请重新登录')
            const userStore = useUserStore()
            userStore.logout()
            window.location.href = '/user/login'
          }
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(error.message || '网络错误')
      }
    } else {
      ElMessage.error('网络连接失败')
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