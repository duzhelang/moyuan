import type { HotPoem, SearchMetrics, UserHeatmap, UserStructure } from '@/api/modules/admin'
import type { RecommendLiveStats } from '@/api/modules/recommend'
import type { MetricItem, TodoItem } from '@/types/admin-dashboard'

/**
 * 控制台演示数据。
 *
 * 只在「演示模式」开启时使用，全部为前端固定数据，不写入后端、不伪造接口结果。
 * 数值采用确定性生成，刷新页面不会跳变，方便演示和截图。
 */

const pad = (value: number) => String(value).padStart(2, '0')

const dayLabel = (date: Date) => `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`

/** 演示访问概况：与趋势图最后一天保持一致 */
export const demoVisitStats = {
  todayVisits: 3286,
  weekVisits: 21580,
  monthVisits: 87420,
  totalVisits: 210360
}

export interface DemoVisitTrend {
  dates: string[]
  visits: number[]
  uniqueVisitors: number[]
}

/** 访问趋势：整体上升 + 周末小高峰，曲线平滑但不呆板 */
export const buildDemoVisitTrend = (days: 7 | 30): DemoVisitTrend => {
  const today = new Date()
  const dates: string[] = []
  const visits: number[] = []
  const uniqueVisitors: number[] = []

  for (let index = 0; index < days; index += 1) {
    const date = new Date(today)
    date.setDate(today.getDate() - (days - 1 - index))
    dates.push(dayLabel(date))

    const weekend = date.getDay() === 0 || date.getDay() === 6
    const wave = Math.sin(index * 0.55) * 0.1 + Math.cos(index * 0.21) * 0.06
    const base = 2350 + index * 36
    const value = Math.round(base * (weekend ? 1.22 : 1) * (1 + wave))
    visits.push(value)
    uniqueVisitors.push(Math.round(value * 0.148 + 118 + Math.sin(index * 0.8) * 16))
  }

  visits[visits.length - 1] = demoVisitStats.todayVisits
  uniqueVisitors[uniqueVisitors.length - 1] = 486
  return { dates, visits, uniqueVisitors }
}

export interface DemoStatsTrend {
  dates: string[]
  users: number[]
  poems: number[]
  posts: number[]
}

/** 内容增长：三条线都有正向变化，避免贴底 */
export const buildDemoStatsTrend = (days = 7): DemoStatsTrend => {
  const today = new Date()
  const dates: string[] = []
  const users: number[] = []
  const poems: number[] = []
  const posts: number[] = []

  for (let index = 0; index < days; index += 1) {
    const date = new Date(today)
    date.setDate(today.getDate() - (days - 1 - index))
    dates.push(dayLabel(date))

    users.push(Math.round(28 + index * 5 + Math.sin(index * 0.9) * 5))
    poems.push(Math.round(42 + index * 6 + Math.sin(index * 1.2) * 6))
    posts.push(Math.round(7 + index * 2.4 + Math.cos(index * 0.8) * 2))
  }

  return { dates, users, poems, posts }
}

/** 热门诗词：演示模式下让排行条不空 */
export const demoHotPoems: HotPoem[] = [
  { id: 1, title: '静夜思', favorites: 126, views: 4820, score: 5198 },
  { id: 2, title: '春望', favorites: 108, views: 4316, score: 4640 },
  { id: 3, title: '登鹳雀楼', favorites: 96, views: 3982, score: 4270 },
  { id: 4, title: '将进酒', favorites: 82, views: 3504, score: 3750 },
  { id: 5, title: '望庐山瀑布', favorites: 74, views: 3128, score: 3350 }
]

/** 今日体检的演示值 */
export const demoMetricsItems: MetricItem[] = [
  { key: 'errorRate', label: '接口错误率', value: '0.02%', status: 'ok', live: true },
  { key: 'p95', label: 'P95 响应', value: '186 ms', status: 'ok', live: true },
  { key: 'slowest', label: '最慢接口', value: '312 ms', status: 'ok', live: true },
  { key: 'noResult', label: '搜索无结果', value: '4.8%', status: 'ok', live: true },
  { key: 'aiFail', label: 'AI 失败率', value: '0.8%', status: 'ok', live: true },
  { key: 'pending', label: '待审核积压', value: '12', status: 'ok', live: true },
  { key: 'loginFail', label: '登录失败', value: '3', status: 'ok', live: true }
]

/** 运营待办的演示值：全部低于告警阈值 */
export const demoTodoItems: TodoItem[] = [
  { key: 'poem', label: '诗词待审', count: 8, status: 'ok', link: '/admin/poem-audit', kind: 'audit' },
  { key: 'comment', label: '评论待审', count: 2, status: 'ok', link: '/admin/comments', kind: 'audit' },
  { key: 'poetProfile', label: '诗人认证', count: 1, status: 'ok', link: '/admin/poet-profiles', kind: 'audit' },
  { key: 'aiContent', label: 'AI 内容待审', count: 1, status: 'ok', link: '/admin/ai-contents', kind: 'audit' },
  { key: 'poetDraft', label: '正文修订', count: 0, status: 'ok', link: '/admin/poet-suggestions', kind: 'audit' },
  { key: 'poetSuggestion', label: '内容建议', count: 0, status: 'ok', link: '/admin/poet-suggestions', kind: 'audit' },
  { key: 'reportPending', label: '举报待处理', count: 0, status: 'ok', link: '/admin/reports', kind: 'alert', note: '演示数据' },
  { key: 'repairPending', label: '报修待处理', count: 0, status: 'ok', link: '/admin/repairs', kind: 'alert', note: '演示数据' },
  { key: 'aiFailed', label: 'AI 失败 · 24h', count: 3, status: 'ok', link: '/admin/monitor', kind: 'alert', note: '演示数据' },
  { key: 'securityEvent', label: '安全事件 · 24h', count: 2, status: 'ok', link: '/admin/monitor', kind: 'alert', note: '演示数据' },
  { key: 'apiError', label: '接口错误 · 24h', count: 1, status: 'ok', link: '/admin/monitor', kind: 'alert', note: '演示数据' }
]

// ==================== 用户分析演示数据 ====================

/** 近 N 天日期标签，格式 MM-DD，与后端趋势接口一致 */
const recentDayLabels = (days: number): string[] => {
  const today = new Date()
  return Array.from({ length: days }, (_, index) => {
    const date = new Date(today)
    date.setDate(today.getDate() - (days - 1 - index))
    return dayLabel(date)
  })
}

/** 用户分析：访问与增长走势复用控制台的确定性曲线，保证两个页面口径一致 */
export const demoUserVisitTrend = buildDemoVisitTrend(7)
export const demoUserStatsTrend = buildDemoStatsTrend(7)

/** 用户分析 KPI：演示值要足够撑起看板，环比全部为正增长 */
export const demoUserCards = [
  {
    key: 'pv',
    label: '今日 PV',
    value: demoVisitStats.todayVisits,
    spark: demoUserVisitTrend.visits,
    tone: 0,
    delta: 12,
    note: '接口请求数 · 较昨日 +12%'
  },
  {
    key: 'uv',
    label: '今日 UV',
    value: demoUserVisitTrend.uniqueVisitors[demoUserVisitTrend.uniqueVisitors.length - 1],
    spark: demoUserVisitTrend.uniqueVisitors,
    tone: 2,
    delta: 8,
    note: '来源 IP 去重 · 较昨日 +8%'
  },
  {
    key: 'new',
    label: '今日新增用户',
    value: demoUserStatsTrend.users[demoUserStatsTrend.users.length - 1],
    spark: demoUserStatsTrend.users,
    tone: 1,
    delta: 14,
    note: '近 7 天每日新增 · 较昨日 +14%'
  },
  {
    key: 'retention',
    label: '7 日回访率',
    value: '63.6%',
    spark: [] as number[],
    tone: 3,
    delta: 2,
    note: '注册满 7 天 1,520 人'
  }
]

/** 用户结构演示：角色分布、活跃与回访都能独立成立，不与 KPI 冲突 */
export const demoUserStructure: UserStructure = {
  totalUsers: 2860,
  newUsers7d: 386,
  roles: [
    { key: 'user', label: '普通用户', value: 2470 },
    { key: 'poet', label: '认证诗人', value: 328 },
    { key: 'reviewer', label: '审核员', value: 46 },
    { key: 'admin', label: '管理员', value: 16 }
  ],
  active7d: 1932,
  active30d: 2416,
  eligible7d: 1520,
  eligible30d: 986,
  retention7d: 63.6,
  retention30d: 49.2,
  silent30d: 444
}

/** 搜索行为演示：总量、无结果率、关键词榜单与 7 日趋势 */
export const demoSearchMetrics: SearchMetrics = {
  windowDays: 7,
  available: true,
  total: 4280,
  noResult: 196,
  noResultRate: 0.0458,
  distinctKeywords: 1286,
  topKeywords: [
    { keyword: '李白', count: 186, avgResult: 42 },
    { keyword: '静夜思', count: 142, avgResult: 28 },
    { keyword: '杜甫', count: 128, avgResult: 51 },
    { keyword: '春望', count: 96, avgResult: 22 },
    { keyword: '苏轼', count: 88, avgResult: 34 },
    { keyword: '唐诗三百首', count: 76, avgResult: 63 }
  ],
  topNoResult: [
    { keyword: '冷门唐诗选集', count: 12, avgResult: 0 },
    { keyword: '李商隐无题全文', count: 9, avgResult: 0 },
    { keyword: 'AI 写七律', count: 8, avgResult: 0 },
    { keyword: '南宋词人年表', count: 7, avgResult: 0 },
    { keyword: '古诗平仄速查', count: 6, avgResult: 0 }
  ],
  trend: recentDayLabels(7).map((date, index) => ({
    date,
    total: [520, 568, 604, 642, 688, 732, 526][index],
    noResult: [22, 26, 28, 31, 34, 38, 17][index]
  }))
}

/** 一周 × 24 小时的活跃分布：工作日早/晚双峰，周末下午与晚间更高 */
const WEEKDAY_HEAT = [
  38, 22, 15, 12, 15, 28, 76, 160, 260, 330, 360, 342,
  300, 268, 288, 326, 352, 378, 430, 486, 522, 445, 235, 92
]
const WEEKEND_HEAT = [
  52, 30, 18, 14, 16, 30, 72, 145, 250, 342, 420, 458,
  430, 380, 405, 458, 495, 520, 560, 600, 625, 535, 300, 128
]

/**
 * 活跃热力图演示数据。
 * 用确定性抖动避免格子完全一样，同时保留清晰的工作日/周末双峰结构，
 * 让高值区在色阶上有明显的层次，便于演示与截图。
 */
export const buildDemoUserHeatmap = (): UserHeatmap => {
  const cells: UserHeatmap['cells'] = []
  let maxRequests = 0
  for (let weekday = 0; weekday < 7; weekday += 1) {
    const profile = weekday >= 5 ? WEEKEND_HEAT : WEEKDAY_HEAT
    for (let hour = 0; hour < 24; hour += 1) {
      const jitter = 0.92 + ((weekday * 7 + hour * 3) % 11) / 55
      const requests = Math.round(profile[hour] * jitter)
      const users = Math.round(requests * (0.08 + ((weekday + hour) % 5) * 0.012))
      cells.push({ weekday, hour, requests, users })
      maxRequests = Math.max(maxRequests, requests)
    }
  }
  return { days: 7, available: true, maxRequests, cells }
}

// ==================== 推荐与实验：线上实时演示数据 ====================

/** 当天 ISO 时间，供最近事件时间展示 */
const isoAt = (hour: number, minute: number): string => {
  const date = new Date()
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(hour)}:${pad(minute)}:00`
}

/**
 * 推荐线上实时演示数据。
 *
 * 覆盖「实时状态 / 策略切换 / 线上曝光点击 / 模型与缓存」四块，
 * 数字之间保持勾稽：分策略曝光点击合计 = 顶部总量，事件去向合计 = 累计事件。
 */
export const buildDemoRecommendLiveStats = (): RecommendLiveStats => {
  const dates = recentDayLabels(7)
  const eventCounts = [52, 68, 61, 84, 96, 108, 62]
  const impressions = [1420, 1560, 1480, 1620, 1740, 1860, 1580]
  const clicks = [76, 84, 80, 88, 94, 102, 86]

  return {
    cacheRows: 19200,
    cacheUsers: 960,
    cachePoems: 1560,
    cacheStrategies: 4,
    poolSize: 3480,
    eventTotal: 5210,
    eventToday: eventCounts[eventCounts.length - 1],
    lastEventTime: isoAt(21, 18),
    models: [
      {
        strategy: 'fusion',
        version: 'fusion-20261108',
        status: 1,
        metricsPool: 'classical-full',
        metrics: { recall10: 0.187, ndcg10: 0.142, precision10: 0.061, coverage: 0.82 },
        cacheRows: 6420,
        cacheUsers: 412,
        createTime: isoAt(2, 10)
      },
      {
        strategy: 'era',
        version: 'era-20261108',
        status: 1,
        metricsPool: 'classical-full',
        metrics: { recall10: 0.164, ndcg10: 0.126, precision10: 0.054, coverage: 0.74 },
        cacheRows: 4680,
        cacheUsers: 302,
        createTime: isoAt(2, 8)
      },
      {
        strategy: 'genre',
        version: 'genre-20261108',
        status: 1,
        metricsPool: 'classical-full',
        metrics: { recall10: 0.151, ndcg10: 0.118, precision10: 0.049, coverage: 0.69 },
        cacheRows: 4320,
        cacheUsers: 286,
        createTime: isoAt(2, 6)
      },
      {
        strategy: 'global',
        version: 'global-20261108',
        status: 1,
        metricsPool: 'classical-full',
        metrics: { recall10: 0.122, ndcg10: 0.094, precision10: 0.038, coverage: 0.63 },
        cacheRows: 3780,
        cacheUsers: 248,
        createTime: isoAt(2, 4)
      }
    ],
    eventTrend: dates.map((date, index) => ({ date, count: eventCounts[index] })),
    eventByStrategy: [
      { name: 'fusion', count: 1860 },
      { name: 'genre', count: 1150 },
      { name: 'era', count: 980 },
      { name: 'global', count: 760 },
      { name: 'rule', count: 460 }
    ],
    eventByPage: [
      { name: 'home', count: 4210 },
      { name: 'search', count: 640 },
      { name: 'poet', count: 360 }
    ],
    recentEvents: [
      { createTime: isoAt(21, 18), userId: 1024, page: 'home', fromStrategy: 'rule', toStrategy: 'fusion' },
      { createTime: isoAt(20, 42), userId: 861, page: 'home', fromStrategy: 'genre', toStrategy: 'fusion' },
      { createTime: isoAt(19, 56), userId: 1432, page: 'search', fromStrategy: 'fusion', toStrategy: 'era' },
      { createTime: isoAt(18, 30), userId: 735, page: 'home', fromStrategy: 'rule', toStrategy: 'global' },
      { createTime: isoAt(17, 12), userId: 1290, page: 'poet', fromStrategy: 'era', toStrategy: 'genre' },
      { createTime: isoAt(16, 5), userId: 508, page: 'home', fromStrategy: 'rule', toStrategy: 'fusion' }
    ],
    onlineImpressions: 11260,
    onlineClicks: 610,
    onlineCtr: 5.4,
    onlineUsers: 890,
    onlineByStrategy: [
      { strategy: 'rule', impressions: 1640, clicks: 66, ctr: 4.0 },
      { strategy: 'era', impressions: 1460, clicks: 74, ctr: 5.1 },
      { strategy: 'genre', impressions: 1820, clicks: 96, ctr: 5.3 },
      { strategy: 'fusion', impressions: 3960, clicks: 252, ctr: 6.4 },
      { strategy: 'global', impressions: 2380, clicks: 122, ctr: 5.1 }
    ],
    onlineTrend: dates.map((date, index) => ({
      date,
      impressions: impressions[index],
      clicks: clicks[index]
    }))
  }
}
