import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const cachedViews = ref<string[]>([])
  const sidebarCollapsed = ref(false)
  const loading = ref(false)

  function addCachedView(viewName: string) {
    if (!cachedViews.value.includes(viewName)) {
      cachedViews.value.push(viewName)
    }
  }

  function removeCachedView(viewName: string) {
    const index = cachedViews.value.indexOf(viewName)
    if (index > -1) {
      cachedViews.value.splice(index, 1)
    }
  }

  function clearCachedViews() {
    cachedViews.value = []
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setLoading(status: boolean) {
    loading.value = status
  }

  return {
    cachedViews,
    sidebarCollapsed,
    loading,
    addCachedView,
    removeCachedView,
    clearCachedViews,
    toggleSidebar,
    setLoading
  }
})