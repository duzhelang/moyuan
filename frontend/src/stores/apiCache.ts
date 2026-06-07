import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCache, setCache, removeCache } from '@/utils/storage'

export const useApiCacheStore = defineStore('apiCache', () => {
  const cache = ref<Record<string, any>>({})

  function getCachedData<T>(key: string): T | null {
    return getCache<T>(key)
  }

  function setCachedData<T>(key: string, data: T, expiresIn?: number) {
    setCache(key, data, expiresIn)
    cache.value[key] = data
  }

  function removeCachedData(key: string) {
    removeCache(key)
    delete cache.value[key]
  }

  function clearCache() {
    const keys = Object.keys(cache.value)
    keys.forEach(key => removeCache(key))
    cache.value = {}
  }

  return {
    cache,
    getCachedData,
    setCachedData,
    removeCachedData,
    clearCache
  }
})