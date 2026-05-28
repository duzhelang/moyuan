const PREFIX = 'moyuan_'

export function getItem<T>(key: string): T | null {
  try {
    const value = localStorage.getItem(PREFIX + key)
    if (value) {
      return JSON.parse(value) as T
    }
    return null
  } catch (error) {
    console.error('获取本地存储失败:', error)
    return null
  }
}

export function setItem<T>(key: string, value: T): void {
  try {
    localStorage.setItem(PREFIX + key, JSON.stringify(value))
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