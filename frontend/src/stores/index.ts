import { createPinia } from 'pinia'

const pinia = createPinia()

export default pinia
export { useAppStore } from './app'
export { useUserStore } from './user'
export { usePoemStore } from './poem'
export { usePreferencesStore } from './preferences'
export { useSearchHistoryStore } from './searchHistory'
export { useFormDraftStore } from './formDraft'
export { useApiCacheStore } from './apiCache'