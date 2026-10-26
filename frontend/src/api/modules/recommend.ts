import request from '@/utils/request'

// ========== 推荐策略切换：类型定义（附录 A.2.1） ==========

/** 策略键：rule 走现有规则接口，其余走模型缓存接口 /api/recommend */
export type StrategyKey = 'rule' | 'era' | 'genre' | 'fusion' | 'global'
export type ModelStrategyKey = Exclude<StrategyKey, 'rule'>

export interface StrategyOption {
  key: StrategyKey
  label: '规则' | '古今' | '体裁' | '融合' | '全局'
  hint: string
  /** 如「离线 NDCG@10 0.12」，仅非规则策略有 */
  metric?: string
  /** 模型版本/指纹/指标池明细，用于指标徽标 hover */
  detail?: string
  available: boolean
}

/**
 * 直接复用 PoemSearchResult 的字段语义，避免卡片渲染和跳转二次适配。
 * source 固定为 local；id 为 poem 主键，卡片点击跳 /poem/:id。
 */
export interface RecommendItem {
  id: number
  title: string
  content: string
  author?: string
  dynasty?: string
  source: 'local'
  recommendScore?: number
  recommendReason?: string
}

export type FallbackReason = 'cache-miss' | 'model-offline' | 'empty' | 'network-error'

export interface RecommendResponse {
  requestedStrategy: StrategyKey
  strategy: StrategyKey
  fallback: boolean
  fallbackReason?: FallbackReason
  modelVersion?: string
  metrics?: Partial<Record<'recall10' | 'ndcg10' | 'precision10' | 'coverage', number>>
  availableStrategies: StrategyKey[]
  items: RecommendItem[] | null
}

export interface RecommendModelInfo {
  strategy: ModelStrategyKey
  version: string
  dataFingerprint: string
  metricsPool: string
  metrics: Partial<Record<'recall10' | 'ndcg10' | 'precision10' | 'coverage', number>>
  status: 0 | 1
  createTime: string
}

export interface RecommendModelsResponse {
  models: RecommendModelInfo[]
  availableStrategies: StrategyKey[]
}

/** 管理端指标屏实时统计（recommendation_cache / recommendation_event 表的当前状态） */
export interface RecommendLiveStats {
  cacheRows: number
  cacheUsers: number
  cachePoems: number
  cacheStrategies: number
  poolSize: number
  eventTotal: number
  eventToday: number
  lastEventTime: string | null
  models: LiveModelStat[]
  eventTrend: { date: string; count: number }[]
  eventByStrategy: { name: string; count: number }[]
  eventByPage: { name: string; count: number }[]
  recentEvents: LiveEventItem[]
}

export interface LiveModelStat {
  strategy: ModelStrategyKey
  version: string
  status: number
  metricsPool: string
  metrics: Partial<Record<'recall10' | 'ndcg10' | 'precision10' | 'coverage', number>>
  cacheRows: number
  cacheUsers: number
  createTime: string
}

export interface LiveEventItem {
  createTime: string
  userId: number
  page: string
  fromStrategy: string | null
  toStrategy: string
}

export const MODEL_STRATEGY_LABELS: Record<ModelStrategyKey, StrategyOption['label']> = {
  era: '古今',
  genre: '体裁',
  fusion: '融合',
  global: '全局'
}

/** 校验并夹紧 topK 到 1..20 */
export function clampTopK(topK: number | undefined, fallback = 6): number {
  if (!Number.isFinite(topK)) return fallback
  return Math.min(20, Math.max(1, Math.round(topK as number)))
}

/** 后端规则接口返回 Map 字段，统一整理为 RecommendItem */
function toItems(raw: any): RecommendItem[] {
  if (!Array.isArray(raw)) return []
  return raw
    .filter(item => item && typeof item.id === 'number')
    .map(item => ({
      id: item.id,
      title: item.title ?? '',
      content: item.content ?? '',
      author: item.author,
      dynasty: item.dynasty,
      source: 'local' as const,
      recommendScore: typeof item.recommendScore === 'number' ? item.recommendScore : undefined,
      recommendReason: item.recommendReason
    }))
}

// ========== 接口封装（附录 A.2.2） ==========

export const recommendApi = {
  /** 模型策略推荐（JWT 必需；strategy=rule 一律走 rule()，不在本接口范围） */
  get(params: { strategy: ModelStrategyKey; topK?: number }, signal?: AbortSignal) {
    return request.get<RecommendResponse>('/recommend', {
      params: { strategy: params.strategy, topK: clampTopK(params.topK) },
      signal,
      silent: true,
      skipCanceledToast: true
    })
  },
  /** 上线模型与可用策略（公开，JWT 可选） */
  models(signal?: AbortSignal) {
    return request.get<RecommendModelsResponse>('/recommend/models', {
      signal,
      silent: true,
      skipCanceledToast: true
    })
  },
  /** 规则策略 / 匿名兜底（现有公开接口，登录走个性化，匿名走热门） */
  rule(limit = 6, signal?: AbortSignal) {
    return request.get('/search/poems/recommended', {
      params: { limit: clampTopK(limit) },
      signal,
      silent: true,
      skipCanceledToast: true
    })
  },
  /** 策略切换埋点（JWT；上报失败静默，不阻塞交互） */
  recordEvent(payload: { page: string; from: StrategyKey; to: StrategyKey }) {
    return request.post('/recommend/events', payload, { silent: true }).catch(() => undefined)
  },
  /** 管理端指标屏实时统计（管理员 JWT） */
  liveStats(signal?: AbortSignal) {
    return request.get<RecommendLiveStats>('/recommend/stats', {
      signal,
      silent: true,
      skipCanceledToast: true
    })
  }
}

export { toItems as normalizeRecommendItems }
