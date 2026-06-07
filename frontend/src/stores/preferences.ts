import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import type { UserPreferences, SortPreferences } from '@/types/storage'
import { getUserPreferences, setUserPreferences, getSortPreferences, setSortPreferences } from '@/utils/storage'

export const usePreferencesStore = defineStore('preferences', () => {
  const userPreferences = ref<UserPreferences>(getUserPreferences())
  const sortPreferences = ref<SortPreferences>(getSortPreferences())

  function updateUserPreferences(preferences: Partial<UserPreferences>) {
    userPreferences.value = { ...userPreferences.value, ...preferences }
    setUserPreferences(userPreferences.value)
  }

  function updateSortPreferences(preferences: Partial<SortPreferences>) {
    sortPreferences.value = { ...sortPreferences.value, ...preferences }
    setSortPreferences(sortPreferences.value)
  }

  function setTheme(theme: 'light' | 'dark' | 'auto') {
    updateUserPreferences({ theme })
  }

  function setLanguage(language: 'zh' | 'en') {
    updateUserPreferences({ language })
  }

  function setFontSize(fontSize: number) {
    updateUserPreferences({ fontSize })
  }

  function setPageSize(pageSize: number) {
    updateUserPreferences({ pageSize })
  }

  function setSidebarCollapsed(collapsed: boolean) {
    updateUserPreferences({ sidebarCollapsed: collapsed })
  }

  return {
    userPreferences,
    sortPreferences,
    updateUserPreferences,
    updateSortPreferences,
    setTheme,
    setLanguage,
    setFontSize,
    setPageSize,
    setSidebarCollapsed
  }
})