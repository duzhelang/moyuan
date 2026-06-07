import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { SearchHistory } from '@/types/storage'
import { getSearchHistory, addSearchHistory, clearSearchHistory } from '@/utils/storage'

export const useSearchHistoryStore = defineStore('searchHistory', () => {
  const history = ref<SearchHistory[]>(getSearchHistory())

  function addHistory(keyword: string, type: 'poem' | 'poet' | 'forum' | 'all' = 'all') {
    addSearchHistory(keyword, type)
    history.value = getSearchHistory()
  }

  function clearHistory() {
    clearSearchHistory()
    history.value = []
  }

  function getRecentSearches(limit: number = 5): SearchHistory[] {
    return history.value.slice(0, limit)
  }

  return {
    history,
    addHistory,
    clearHistory,
    getRecentSearches
  }
})