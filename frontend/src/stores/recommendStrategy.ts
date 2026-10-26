import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RecommendModelInfo, StrategyKey } from '@/api/modules/recommend'
import { useUserStore } from './user'

const STRATEGY_KEYS: StrategyKey[] = ['rule', 'era', 'genre', 'fusion', 'global']

/** 只持久化策略，不缓存结果列表（换账号不串数据）；匿名不持久化 */
function storageKey(userId: number | null | undefined) {
  return `moyuan:recommend:strategy:${userId}`
}

function normalizeStrategy(key: unknown): StrategyKey | null {
  return typeof key === 'string' && (STRATEGY_KEYS as string[]).includes(key)
    ? (key as StrategyKey)
    : null
}

export const useRecommendStrategyStore = defineStore('recommendStrategy', () => {
  const strategy = ref<StrategyKey>('rule')
  const availableStrategies = ref<StrategyKey[]>(['rule'])
  const models = ref<RecommendModelInfo[]>([])
  const modelsLoadedAt = ref<number | null>(null)

  /** 登录态变化时调用：按 userId 恢复持久化的策略，无记录回落规则 */
  function hydrate(userId: number | null | undefined) {
    if (userId == null) {
      strategy.value = 'rule'
      availableStrategies.value = ['rule']
      return
    }
    const saved = normalizeStrategy(localStorage.getItem(storageKey(userId)))
    strategy.value = saved ?? 'rule'
  }

  function setStrategy(key: StrategyKey) {
    strategy.value = key
    const userStore = useUserStore()
    const userId = userStore.userInfo?.id
    if (userId != null) {
      localStorage.setItem(storageKey(userId), key)
    }
  }

  function setAvailableStrategies(keys: StrategyKey[]) {
    const valid = keys.filter(key => (STRATEGY_KEYS as string[]).includes(key))
    availableStrategies.value = valid.length > 0 ? valid : ['rule']
  }

  function setModels(next: RecommendModelInfo[], available: StrategyKey[]) {
    models.value = next ?? []
    modelsLoadedAt.value = Date.now()
    setAvailableStrategies(available ?? ['rule'])
  }

  /** 退出登录/切换账号：重置为规则，清除可用模型标签 */
  function reset() {
    strategy.value = 'rule'
    availableStrategies.value = ['rule']
    models.value = []
    modelsLoadedAt.value = null
  }

  return {
    strategy,
    availableStrategies,
    models,
    modelsLoadedAt,
    hydrate,
    setStrategy,
    setAvailableStrategies,
    setModels,
    reset
  }
})
