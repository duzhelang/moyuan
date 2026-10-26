// 推荐策略取数状态机（附录 A.3.3 / A.4）：
// 取数、5 秒超时、请求序号、取消、回退；不渲染 UI、不发全局 toast（除网络回退的一次提示）
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import {
  recommendApi,
  normalizeRecommendItems,
  MODEL_STRATEGY_LABELS,
  clampTopK
} from '@/api/modules/recommend'
import type {
  FallbackReason,
  ModelStrategyKey,
  RecommendItem,
  RecommendResponse,
  StrategyKey,
  StrategyOption
} from '@/api/modules/recommend'
import { useRecommendStrategyStore } from '@/stores/recommendStrategy'
import { useUserStore } from '@/stores/user'

const TIMEOUT_MS = 5000

const STRATEGY_HINTS: Record<StrategyKey, string> = {
  rule: '现有兴趣/朝代规则打分',
  era: 'ERA · 按古今兴趣分组训练',
  genre: 'GENRE · 按诗/词体裁分组训练',
  fusion: '两视角加权融合',
  global: '全部行为训练，不分组'
}

const FALLBACK_NOTICES: Record<FallbackReason, string> = {
  'cache-miss': '该策略缓存未命中，已切回规则推荐',
  'model-offline': '模型版本已下线，已切回规则推荐',
  empty: '该策略暂无推荐，已切回规则推荐',
  'network-error': '模型推荐暂不可用，已切回规则推荐'
}

function toError(e: unknown): Error {
  return e instanceof Error ? e : new Error('推荐加载失败')
}

export type UseRecommendStrategyReturn = ReturnType<typeof useRecommendStrategy>

export function useRecommendStrategy(options?: {
  defaultStrategy?: StrategyKey
  topK?: number
  autoLoad?: boolean
  /** 主面板传 true：策略写入全局 store 并按用户持久化、上报埋点；对比弹窗传 false */
  persist?: boolean
}) {
  const { defaultStrategy = 'rule', autoLoad = false, persist = false } = options ?? {}
  const topK = clampTopK(options?.topK, 6)

  // store 始终获取（Pinia 单例）：非持久化实例（对比弹窗列）也要读可用策略与模型信息，
  // 只是写路径（setStrategy/埋点/watch/loadModels）由 persist 控制
  const store = useRecommendStrategyStore()
  const userStore = useUserStore()

  const strategy = ref<StrategyKey>(persist ? store.strategy : defaultStrategy)
  const items = ref<RecommendItem[]>([])
  const loading = ref(false)
  const error = ref<Error | null>(null)
  const fallbackUsed = ref(false)
  /** 网络失败降级到规则：标签保持不变，同标签可重试 */
  const degradedToRule = ref(false)
  /** 后端缓存未命中/版本下线的轻提示（不弹 toast） */
  const fallbackNotice = ref<string | null>(null)

  const availableStrategies = computed<StrategyKey[]>(
    () => store.availableStrategies
  )

  const strategyOptions = computed<StrategyOption[]>(() => {
    const list: StrategyOption[] = []
    for (const key of availableStrategies.value) {
      if (key === 'rule') {
        list.push({ key, label: '规则', hint: STRATEGY_HINTS.rule, available: true })
        continue
      }
      const model = store.models.find(m => m.strategy === key)
      const ndcg = model?.metrics?.ndcg10
      list.push({
        key,
        label: MODEL_STRATEGY_LABELS[key],
        hint: STRATEGY_HINTS[key],
        metric: typeof ndcg === 'number' ? `离线 NDCG@10 ${ndcg.toFixed(2)}` : undefined,
        detail: model
          ? `模型版本 ${model.version} · 指标池 ${model.metricsPool} · 数据指纹 ${model.dataFingerprint}`
          : undefined,
        available: true
      })
    }
    return list
  })

  // ==================== 请求序号与取消 ====================

  let requestSeq = 0
  let activeController: AbortController | null = null
  let activeTimer: number | null = null
  let activeTimedOut = false

  function clearActiveTimer() {
    if (activeTimer !== null) {
      clearTimeout(activeTimer)
      activeTimer = null
    }
  }

  // ==================== 取数 ====================

  async function load() {
    const seq = ++requestSeq
    // 取消上一请求：被取消的请求不弹 toast、不回退、不改变当前标签（由 seq 检查保证）
    clearActiveTimer()
    activeController?.abort()
    const controller = new AbortController()
    activeController = controller
    activeTimedOut = false
    activeTimer = window.setTimeout(() => {
      activeTimedOut = true
      controller.abort()
    }, TIMEOUT_MS)

    loading.value = true
    error.value = null
    try {
      if (strategy.value === 'rule') {
        const res = await recommendApi.rule(topK, controller.signal)
        if (seq !== requestSeq) return
        items.value = normalizeRecommendItems(res.data)
        fallbackUsed.value = false
        degradedToRule.value = false
      } else {
        const res = await recommendApi.get(
          { strategy: strategy.value as ModelStrategyKey, topK },
          controller.signal
        )
        if (seq !== requestSeq) return
        applyModelResponse(res.data)
      }
    } catch (e) {
      if (seq !== requestSeq) return
      // 快速连点/组件卸载导致的取消：忽略；超时（主动 abort）按网络失败处理
      if (axios.isCancel(e) && !activeTimedOut) return
      await handleLoadFailure(e, seq)
    } finally {
      if (seq === requestSeq) {
        clearActiveTimer()
        activeController = null
        loading.value = false
      }
    }
  }

  function applyModelResponse(data: RecommendResponse) {
    if (data.fallback) {
      // 后端缓存未命中/版本下线：HTTP 200 + 规则结果，标签静默切回规则
      items.value = normalizeRecommendItems(data.items)
      fallbackUsed.value = true
      degradedToRule.value = false
      fallbackNotice.value =
        FALLBACK_NOTICES[data.fallbackReason ?? 'cache-miss'] ?? FALLBACK_NOTICES['cache-miss']
      strategy.value = 'rule'
      if (persist) {
        store.setStrategy('rule')
      }
      return
    }
    items.value = data.items ?? []
    fallbackUsed.value = false
    degradedToRule.value = false
  }

  async function handleLoadFailure(e: unknown, seq: number) {
    const status = (e as { response?: { status?: number } })?.response?.status
    if (status === 401 || status === 403) {
      // 交给全局请求层处理，不进入规则回退
      error.value = toError(e)
      return
    }
    if (strategy.value === 'rule') {
      // 规则接口失败：error 状态 + 重新加载，不做二次回退
      error.value = toError(e)
      return
    }
    // 模型策略网络失败/超时：只回退一次规则接口（同样 5 秒超时）
    const fallbackController = new AbortController()
    const fallbackTimer = window.setTimeout(() => fallbackController.abort(), TIMEOUT_MS)
    try {
      const res = await recommendApi.rule(topK, fallbackController.signal)
      if (seq !== requestSeq) return
      items.value = normalizeRecommendItems(res.data)
      fallbackUsed.value = true
      degradedToRule.value = true
      fallbackNotice.value = null
      ElMessage.warning(FALLBACK_NOTICES['network-error'])
    } catch (fallbackError) {
      if (seq !== requestSeq) return
      // 回退请求失败（含 5 秒超时）：进入错误态，不做二次回退
      error.value = toError(fallbackError)
    } finally {
      clearTimeout(fallbackTimer)
    }
  }

  // ==================== 对外动作 ====================

  async function switchTo(key: StrategyKey) {
    if (!availableStrategies.value.includes(key)) return
    if (key === strategy.value) {
      // 同标签重复点击：失败态或已降级到规则时重新加载
      if (error.value || degradedToRule.value) await load()
      return
    }
    const from = strategy.value
    strategy.value = key
    fallbackNotice.value = null
    if (persist) {
      store.setStrategy(key)
      recommendApi.recordEvent({ page: 'home', from, to: key })
    }
    await load()
  }

  async function retry() {
    fallbackNotice.value = null
    await load()
  }

  async function backToRule() {
    await switchTo('rule')
  }

  // ==================== 模型信息（仅持久化实例负责拉取） ====================

  async function loadModels(force = false) {
    if (!persist) return
    if (!force && store.modelsLoadedAt) return
    try {
      const res = await recommendApi.models()
      store.setModels(res.data.models ?? [], res.data.availableStrategies ?? ['rule'])
    } catch {
      // 静默：拿不到模型信息时只显示规则标签
    }
  }

  // 持久化实例跟随登录态：换账号按 userId 恢复策略，登出重置为规则
  if (persist) {
    watch(
      () => userStore.userInfo?.id ?? null,
      (id, prev) => {
        if (id === prev) return
        if (id == null) {
          store.reset()
        } else {
          store.hydrate(id)
          loadModels(true)
        }
        strategy.value = store.strategy
        fallbackNotice.value = null
        load()
      }
    )
    // 外部（URL 直链）改动 store 策略时同步加载
    watch(
      () => store.strategy,
      key => {
        if (key !== strategy.value) {
          strategy.value = key
          load()
        }
      }
    )
  }

  // 组件卸载：中止在途请求；requestSeq 自增保证中止的请求不触发回退、不写状态
  onBeforeUnmount(() => {
    requestSeq++
    clearActiveTimer()
    activeController?.abort()
    activeController = null
  })

  if (autoLoad) {
    load()
  }

  return {
    strategy,
    availableStrategies,
    options: strategyOptions,
    items,
    loading,
    error,
    fallbackUsed,
    fallbackNotice,
    degradedToRule,
    load,
    switchTo,
    retry,
    backToRule,
    loadModels
  }
}
