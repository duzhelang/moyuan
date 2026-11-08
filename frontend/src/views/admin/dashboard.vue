<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import {
  getAdminStats,
  getAdminStatsTrend,
  getAuditStats,
  getRepairStats,
  getSystemHealth,
  getSystemMetrics,
  getSearchMetrics,
  getAiMetrics,
  getSecurityMetrics,
  getVisitStats,
  getVisitTrend,
  getHotPoems
} from '@/api/modules/admin'
import { getReportStats } from '@/api/modules/report'
import type {
  AiMetrics,
  HotPoem,
  SearchMetrics,
  SecurityMetrics,
  SystemHealthNode,
  SystemMetrics
} from '@/api/modules/admin'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import HealthChain from '@/components/admin/HealthChain.vue'
import MetricsBar from '@/components/admin/MetricsBar.vue'
import ResourceDistribution from '@/components/admin/ResourceDistribution.vue'
import Sparkline from '@/components/admin/Sparkline.vue'
import TodoTimeline from '@/components/admin/TodoTimeline.vue'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminDemoToggle from '@/components/admin/AdminDemoToggle.vue'
import { useAdminDemo } from '@/composables/useAdminDemo'
import {
  buildDemoStatsTrend,
  buildDemoVisitTrend,
  demoHotPoems,
  demoMetricsItems,
  demoTodoItems,
  demoVisitStats
} from '@/configs/admin/demoData'
import { readChartInk, readChartPalette, useAdminTheme } from '@/composables/useAdminTheme'
import type { ChainNode, MetricItem, TodoItem } from '@/types/admin-dashboard'

use([CanvasRenderer, LineChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const { resolvedTheme } = useAdminTheme()
const { demoMode } = useAdminDemo()

/** 图表配色跟随主题，切换主题时 computed 自动重算 */
const palette = computed(() => {
  void resolvedTheme.value
  return readChartPalette()
})

/** 图表文字与网格色同样读取 token（统一走 readChartInk，避免多处重复取色） */
const chartInk = computed(() => {
  void resolvedTheme.value
  return readChartInk()
})

/** tooltip 跟随主题，深色页面不弹白块 */
const tooltipStyle = computed(() => ({
  backgroundColor: chartInk.value.surface,
  borderColor: chartInk.value.border,
  textStyle: { color: chartInk.value.regular, fontSize: 12 }
}))

const REFRESH_MS = 30_000

const stats = ref<Record<string, any>>({})
const loading = ref(true)
const trendData = ref<{ dates: string[]; poems: number[]; posts: number[]; users: number[] }>({
  dates: [],
  poems: [],
  posts: [],
  users: []
})
const trendLoading = ref(true)

const visitStats = ref({
  todayVisits: 0,
  weekVisits: 0,
  monthVisits: 0,
  totalVisits: 0
})
const visitLoading = ref(true)
const visitTrendData = ref<{ dates: string[]; visits: number[]; uniqueVisitors: number[] }>({
  dates: [],
  visits: [],
  uniqueVisitors: []
})
/** 访问趋势范围：7 / 30 天，90 天数据量大且暂无需求，不做 */
const visitRange = ref<7 | 30>(7)

/** 访问卡走势专用：30 天窗口，够画「本周累计」和「本月波动」 */
const sparkTrend = ref<{ dates: string[]; visits: number[] }>({ dates: [], visits: [] })

const auditStats = ref<Record<string, number>>({})
const reportStats = ref<Record<string, number>>({})
const repairStats = ref<Record<string, number>>({})
const healthNodes = ref<ChainNode[]>([])
const apiMetrics = ref<SystemMetrics | null>(null)
const searchMetrics = ref<SearchMetrics | null>(null)
const aiMetrics = ref<AiMetrics | null>(null)
const securityMetrics = ref<SecurityMetrics | null>(null)
const hotPoems = ref<HotPoem[]>([])
const updatedAt = ref('—')

const statDetails = computed<Record<string, any>>(() => stats.value.details || {})

/** 累加和 */
const cumulative = (arr: number[]) => {
  let sum = 0
  return arr.map((v) => (sum += v))
}
const sumOf = (arr: number[]) => arr.reduce((a, b) => a + b, 0)

/**
 * 四张访问卡各自的走势线。
 * 数据全部来自 /admin/stats/visits/trend?days=30，不做任何估算：
 *  - 今日 / 本月：日访问量折线，看波动
 *  - 本周 / 总量：累计折线，看增长节奏
 */
const visitSpark = computed<
  Record<string, { values: number[]; range: string; note: string; up?: boolean }>
>(() => {
  const v = sparkTrend.value.visits || []
  const last7 = v.slice(-7)
  const last30 = v.slice(-30)
  const total = Number(visitStats.value.totalVisits) || 0

  const today = last7.length ? last7[last7.length - 1] : 0
  const yesterday = last7.length > 1 ? last7[last7.length - 2] : 0
  const dayDelta = yesterday ? Math.round(((today - yesterday) / yesterday) * 100) : 0

  const sum30 = sumOf(last30)
  const base30 = Math.max(0, total - sum30)

  return {
    todayVisits: {
      values: last7,
      range: '近 7 天日访问',
      // 昨日为 0 时算不出百分比，直接说明「昨日无访问」，也就解释了为什么线是平的
      note: !yesterday
        ? today > 0
          ? '昨日无访问'
          : '暂无数据'
        : `较昨日 ${dayDelta >= 0 ? '+' : ''}${dayDelta}%`,
      up: yesterday ? dayDelta >= 0 : undefined
    },
    weekVisits: {
      values: cumulative(last7),
      range: '近 7 天累计',
      note: last7.length ? `峰值 ${Math.max(...last7)}` : '暂无数据'
    },
    monthVisits: {
      values: last30,
      range: '近 30 天日访问',
      note: last30.length ? `日均 ${Math.round(sum30 / last30.length)}` : '暂无数据'
    },
    totalVisits: {
      values: cumulative(last30).map((x) => base30 + x),
      range: '近 30 天累计总量',
      note: sum30 ? `30 天 +${sum30}` : '暂无数据'
    }
  }
})

/** 待审积压：后端 /admin/audit/stats 的真实值，与「运营待办」同一口径（认证与正文修订一并计入） */
const pendingTotal = computed(() => {
  const s = auditStats.value
  return (
    (s.poems || 0) +
    (s.comments || 0) +
    (s.poetProfiles || 0) +
    (s.aiContents || 0) +
    (s.poetDrafts || 0) +
    (s.poetSuggestions || 0)
  )
})

/**
 * 今日体检：接口错误率 / P95 / 慢接口来自 api_request_metric，
 * 搜索无结果率来自 search_log，待审积压来自 /admin/audit/stats，
 * 其余两项等 AI 调用日志与登录统计接入。
 */
const metricsItems = computed<MetricItem[]>(() => {
  if (demoMode.value) return demoMetricsItems
  const m = apiMetrics.value
  const hasData = !!m && m.available && m.total > 0
  const errRate = hasData ? ((m as SystemMetrics).errorRate * 100).toFixed(2) + '%' : '—'
  const p95 = hasData && (m as SystemMetrics).p95Ms != null ? `${(m as SystemMetrics).p95Ms} ms` : '—'
  const slowest = hasData && (m as SystemMetrics).slowTop.length ? (m as SystemMetrics).slowTop[0] : null

  const s = searchMetrics.value
  const searchHasData = !!s && s.available && s.total > 0
  const noResultRate = searchHasData ? ((s as SearchMetrics).noResultRate * 100).toFixed(1) + '%' : '—'

  const ai = aiMetrics.value
  const aiHasData = !!ai && ai.available && ai.total > 0
  const aiFailRate = aiHasData ? ((ai as AiMetrics).errorRate * 100).toFixed(1) + '%' : '—'

  const sec = securityMetrics.value
  const secHasData = !!sec && sec.available

  return [
    {
      key: 'errorRate',
      label: '接口错误率',
      value: errRate,
      status: hasData && (m as SystemMetrics).errorRate >= 0.01 ? 'warn' : 'ok',
      live: hasData
    },
    {
      key: 'p95',
      label: 'P95 响应',
      value: p95,
      status: hasData && ((m as SystemMetrics).p95Ms || 0) > 500 ? 'warn' : 'ok',
      live: hasData
    },
    {
      key: 'slowest',
      label: '最慢接口',
      value: slowest ? `${slowest.avgMs} ms` : '—',
      status: slowest && slowest.avgMs > 500 ? 'warn' : 'ok',
      live: hasData
    },
    {
      key: 'noResult',
      label: '搜索无结果',
      value: noResultRate,
      status: searchHasData && (s as SearchMetrics).noResultRate >= 0.2 ? 'warn' : 'ok',
      live: searchHasData
    },
    {
      key: 'aiFail',
      label: 'AI 失败率',
      value: aiFailRate,
      status: aiHasData && (ai as AiMetrics).errorRate >= 0.05 ? 'warn' : 'ok',
      live: aiHasData
    },
    {
      key: 'pending',
      label: '待审核积压',
      value: String(pendingTotal.value),
      status: pendingTotal.value >= 10 ? 'warn' : 'ok',
      live: true
    },
    {
      key: 'loginFail',
      label: '登录失败',
      value: secHasData ? String((sec as SecurityMetrics).loginFail) : '—',
      status: secHasData && (sec as SecurityMetrics).loginFail >= 20 ? 'warn' : 'ok',
      live: secHasData
    }
  ]
})

/** 今日 UV：取访问趋势的最后一天 */
const lastUv = computed(() => {
  const arr = visitTrendData.value.uniqueVisitors
  return arr.length ? arr[arr.length - 1] : 0
})

/** 内容增长是否有真实新增；没有时显示空态，避免一条贴底的空线 */
const hasContentGrowth = computed(
  () =>
    sumOf(trendData.value.users) + sumOf(trendData.value.poems) + sumOf(trendData.value.posts) > 0
)

/** 切换访问趋势范围并重新拉取，7 / 30 天共用同一接口 */
const setVisitRange = async (days: 7 | 30) => {
  if (visitRange.value === days) return
  visitRange.value = days
  if (demoMode.value) {
    visitTrendData.value = buildDemoVisitTrend(days)
    return
  }
  visitLoading.value = true
  try {
    const res = await getVisitTrend(days)
    visitTrendData.value = res.data
  } catch (error) {
    console.error('获取访问趋势失败', error)
  } finally {
    visitLoading.value = false
  }
}

/**
 * 前端访问节点：后端探测不到浏览器，用访问日志的真实数据拼出来，
 * 与 health 接口返回的 4 个节点拼成完整 5 节点链路。
 */
const webChainNode = computed<ChainNode>(() => ({
  key: 'web',
  name: '前端访问',
  status: 'ok',
  latency: '—',
  live: true,
  metrics: [
    { label: '今日 PV', value: String(visitStats.value.todayVisits || 0) },
    { label: '今日 UV', value: String(lastUv.value) },
    {
      label: '人均浏览',
      value:
        lastUv.value > 0
          ? (visitStats.value.todayVisits / lastUv.value).toFixed(1) + ' 页'
          : '—'
    },
    { label: '统计来源', value: '访问日志' }
  ]
}))

/** 各节点职责：悬浮窗底部的解释文案，帮助非运维读者理解链路 */
const CHAIN_DESC: Record<string, string> = {
  web: '浏览器到站点的入口层，PV 是接口请求数，UV 按来源 IP 去重。',
  api: 'Spring Boot 主服务，所有业务请求的必经节点，指标取自 JVM 运行时。',
  mysql: '主数据库，存放诗词、诗人、用户等核心业务数据，探活执行 SELECT 1。',
  redis: '缓存与会话存储，命中率偏低通常意味着缓存未预热或 Key 已过期。',
  rag: '诗词知识检索与生成服务，依赖本地 Ollama 模型，未启动时 AI 问答不可用。'
}

/**
 * latencyMs = 0 表示「探测在 1ms 内返回」，不是没有数据，
 * 早先直接判 >0 会把快的节点显示成「—」，看起来像没接上。
 */
const toChainNode = (n: SystemHealthNode): ChainNode => ({
  key: n.key,
  name: n.name,
  status: n.status === 'down' ? 'down' : n.status === 'warn' ? 'warn' : 'ok',
  latency:
    n.status === 'down'
      ? '断连'
      : n.latencyMs && n.latencyMs > 0
        ? `${n.latencyMs} ms`
        : '<1 ms',
  live: true,
  desc: CHAIN_DESC[n.key],
  metrics:
    n.metrics && n.metrics.length
      ? n.metrics
      : [{ label: '状态', value: n.message || '正常' }]
})

/** 顶部日期与整体健康度：状态不再写死「运行正常」，跟随 health 节点真实结果 */
const todayLabel = computed(() =>
  new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'long'
  }).format(new Date())
)

const healthLevel = computed(() => {
  if (!healthNodes.value.length) return 'unknown'
  if (healthNodes.value.some((n) => n.status === 'down')) return 'down'
  if (healthNodes.value.some((n) => n.status === 'warn')) return 'warn'
  return 'ok'
})

const healthText = computed(() => {
  const total = healthNodes.value.length
  if (!total) return '状态未知'
  const down = healthNodes.value.filter((n) => n.status === 'down').length
  const warn = healthNodes.value.filter((n) => n.status === 'warn').length
  if (down) return `${down} 个节点异常`
  if (warn) return `${warn} 个节点降级`
  return '运行正常'
})

/** 演示模式下顶部状态只表达「演示中」，不覆盖真实探活结果，也不参与后端判断 */
const displayHealthLevel = computed(() => (demoMode.value ? 'ok' : healthLevel.value))
const displayHealthText = computed(() => (demoMode.value ? '演示模式' : healthText.value))

/** health 接口失败时返回空数组，组件自动回落到占位链路 */
const chainNodes = computed<ChainNode[]>(() =>
  healthNodes.value.length ? [webChainNode.value, ...healthNodes.value] : []
)

/**
 * 全局结论层：先把「有没有问题、先处理什么」用一句话说清楚，
 * 再让下面的资源/趋势/链路作为证据，而不是把数字平铺给读者自己判断。
 */
const globalVerdict = computed(() => {
  if (demoMode.value) {
    return {
      level: 'ok' as const,
      title: '演示模式 · 全局运行平稳',
      summary: '当前展示的是演示数据，只影响本页图表与数字展示，不会写入后端；点击顶部按钮可恢复真实数据。',
      pendingActions: 12,
      facts: [
        { label: '待处理总数', value: '12' },
        { label: '异常节点', value: '0/5' },
        { label: '举报 / 报修', value: '0 / 0' },
        { label: 'AI 失败率', value: '0.8%' }
      ]
    }
  }
  const downNodes = healthNodes.value.filter((n) => n.status === 'down')
  const warnNodes = healthNodes.value.filter((n) => n.status === 'warn')
  const ai = aiMetrics.value
  const api = apiMetrics.value
  const issues: string[] = []
  let level: 'ok' | 'warn' | 'down' = 'ok'

  if (downNodes.length) {
    issues.push(`${downNodes.map((n) => n.name).join('、')} 未连接`)
    level = 'down'
  } else if (warnNodes.length) {
    issues.push(`${warnNodes.map((n) => n.name).join('、')} 响应降级`)
    level = 'warn'
  }
  if (api && api.available && api.total > 0 && api.errorRate >= 0.05) {
    issues.push(`接口错误率 ${(api.errorRate * 100).toFixed(1)}%`)
    level = api.errorRate >= 0.1 ? 'down' : 'warn'
  }
  if (ai && ai.available && ai.total > 0 && ai.errorRate >= 0.3) {
    issues.push(`AI 失败率 ${(ai.errorRate * 100).toFixed(1)}%`)
    level = ai.errorRate >= 0.5 ? 'down' : 'warn'
  }
  if ((securityMetrics.value?.total || 0) > 0) {
    issues.push(`安全事件 ${securityMetrics.value?.total} 起`)
    level = level === 'down' ? 'down' : 'warn'
  }
  if ((reportStats.value.pending || 0) > 0) {
    issues.push(`举报待处理 ${reportStats.value.pending}`)
    level = level === 'down' ? 'down' : 'warn'
  }
  if ((repairStats.value.pending || 0) > 0) {
    issues.push(`报修待处理 ${repairStats.value.pending}`)
    level = level === 'down' ? 'down' : 'warn'
  }
  if (pendingTotal.value >= 20) {
    issues.push(`审核积压 ${pendingTotal.value}`)
    level = level === 'down' ? 'down' : 'warn'
  }

  const pendingActions =
    pendingTotal.value +
    (reportStats.value.pending || 0) +
    (repairStats.value.pending || 0)

  return {
    level,
    title:
      level === 'down'
        ? '存在风险，需要立即处理'
        : level === 'warn'
          ? '需要关注，建议优先处置'
          : '全局运行平稳',
    summary: issues.length
      ? `优先关注：${issues.slice(0, 3).join('；')}`
      : '链路、内容、举报与报修均无高优异常，可继续按运营节奏推进。',
    pendingActions,
    facts: [
      { label: '待处理总数', value: `${pendingActions}` },
      { label: '异常节点', value: `${downNodes.length + warnNodes.length}/${healthNodes.value.length || 0}` },
      { label: '举报 / 报修', value: `${reportStats.value.pending || 0} / ${repairStats.value.pending || 0}` },
      {
        label: 'AI 失败率',
        value:
          ai && ai.available && ai.total > 0
            ? `${(ai.errorRate * 100).toFixed(1)}%`
            : '—'
      }
    ]
  }
})

/**
 * 资源总览：每格只保留「图标 + 标签 + 大数字 + 近 7 天变化」，
 * 不再画归一化进度条，避免看起来像存在业务占比含义。
 */
const kpis = computed(() => {
  const s = stats.value
  const items = [
    {
      key: 'userCount',
      label: '用户总数',
      icon: 'User',
      tone: 0,
      tipTitle: '用户总数构成',
      sub: `近 7 天新增 ${sumOf(trendData.value.users)}`
    },
    {
      key: 'poemCount',
      label: '诗词总数',
      icon: 'Reading',
      tone: 1,
      tipTitle: '诗词总数构成',
      sub: `近 7 天新增 ${sumOf(trendData.value.poems)}`
    },
    {
      key: 'categoryCount',
      label: '分类总数',
      icon: 'FolderOpened',
      tone: 2,
      tipTitle: '分类总数构成',
      sub: `覆盖 ${s.dynastyCount || 0} 个朝代`
    },
    {
      key: 'poetCount',
      label: '诗人总数',
      icon: 'EditPen',
      tone: 4,
      tipTitle: '诗人总数构成',
      sub: `认证待审 ${auditStats.value.poetProfiles || 0}`
    },
    {
      key: 'postCount',
      label: '帖子总数',
      icon: 'ChatLineSquare',
      tone: 5,
      tipTitle: '帖子总数构成',
      sub: `近 7 天新增 ${sumOf(trendData.value.posts)}`
    },
    {
      // 朝代总量为固定的朝代枚举，无可拆维度，不提供悬浮说明
      key: 'dynastyCount',
      label: '朝代总数',
      icon: 'Clock',
      tone: 3,
      tipTitle: '',
      sub: `诗词分类 ${s.categoryCount || 0} 类`
    }
  ]
  return items.map((item) => ({ ...item, value: Number(s[item.key]) || 0 }))
})

const visitCards = [
  { key: 'todayVisits', label: '今日访问', icon: 'View', tone: 0, tip: '今日 00:00 起记录的接口访问次数，按每次请求逐次计数，含游客访问' },
  { key: 'weekVisits', label: '本周访问', icon: 'DataLine', tone: 2, tip: '近 7 天（含今日）累计接口访问次数，非自然周' },
  { key: 'monthVisits', label: '本月访问', icon: 'Calendar', tone: 1, tip: '近 30 天（含今日）累计接口访问次数，非自然月' },
  { key: 'totalVisits', label: '总访问量', icon: 'TrendCharts', tone: 3, tip: '自系统记录访问日志以来的累计接口访问次数' }
]

/** 审核积压 + 异常处置：统一到「运营待办」两个页签 */
const todoItems = computed<TodoItem[]>(() => {
  if (demoMode.value) return demoTodoItems
  const s = auditStats.value
  const build = (
    key: string,
    label: string,
    count: number | undefined,
    link: string,
    kind: 'audit' | 'alert' = 'audit',
    note = ''
  ): TodoItem => ({
    key,
    label,
    count: count || 0,
    status: (count || 0) > 0 ? 'warn' : 'ok',
    link,
    kind,
    note
  })
  const auditItems: TodoItem[] = [
    build('poem', '诗词待审', s.poems, '/admin/poem-audit'),
    build('comment', '评论待审', s.comments, '/admin/comments'),
    build('poetProfile', '诗人认证', s.poetProfiles, '/admin/poet-profiles'),
    build('aiContent', 'AI 内容待审', s.aiContents, '/admin/ai-contents'),
    build('poetDraft', '正文修订', s.poetDrafts, '/admin/poet-suggestions'),
    build('poetSuggestion', '内容建议', s.poetSuggestions, '/admin/poet-suggestions')
  ]
  // 审核类阈值保持原口径：>=10 才标红；这里统一覆盖为 warn / ok
  auditItems.forEach((item) => {
    item.status = item.count >= 10 ? 'warn' : 'ok'
  })

  const alertItems: TodoItem[] = [
    build('reportPending', '举报待处理', reportStats.value.pending, '/admin/reports', 'alert',
      '用户举报与 AI 风控自动建单中尚未处理的数量'),
    build('repairPending', '报修待处理', repairStats.value.pending, '/admin/repairs', 'alert',
      '系统 Bug 工单中待处理的数量，处理中工单见报修管理页'),
    build('aiFailed', 'AI 失败 · 24h', aiMetrics.value?.errorCount, '/admin/monitor', 'alert',
      '近 24 小时 AI 调用失败次数，连续失败需检查模型密钥与网络'),
    build('securityEvent', '安全事件 · 24h', securityMetrics.value?.total, '/admin/monitor', 'alert',
      '登录失败、越权 403、限流等安全事件合计'),
    build('apiError', '接口错误 · 24h', apiMetrics.value?.errorCount, '/admin/monitor', 'alert',
      '近 24 小时 HTTP 5xx / 业务失败请求数')
  ]

  return [...auditItems, ...alertItems]
})

const lineChartOption = computed(() => ({
  tooltip: tooltipStyle.value,
  legend: {
    data: ['用户新增', '诗词新增', '帖子新增'],
    top: 0,
    right: 0,
    itemWidth: 10,
    itemHeight: 6,
    itemGap: 12,
    textStyle: { color: chartInk.value.muted, fontSize: 11 }
  },
  grid: { left: 2, right: 8, bottom: 2, top: 30, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trendData.value.dates,
    axisLine: { lineStyle: { color: chartInk.value.border } },
    axisTick: { show: false },
    axisLabel: { color: chartInk.value.muted, fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: chartInk.value.muted, fontSize: 11 },
    splitLine: { lineStyle: { color: chartInk.value.grid, type: 'dashed' as const } }
  },
  series: [
    {
      name: '用户新增',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: trendData.value.users,
      color: palette.value[0],
      lineStyle: { width: 2 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: `${palette.value[0]}30` },
            { offset: 1, color: `${palette.value[0]}05` }
          ]
        }
      }
    },
    {
      name: '诗词新增',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: trendData.value.poems,
      color: palette.value[1],
      lineStyle: { width: 1.5 }
    },
    {
      name: '帖子新增',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: trendData.value.posts,
      color: palette.value[2],
      lineStyle: { width: 1.5, type: 'dashed' }
    }
  ]
}))

const visitChartOption = computed(() => ({
  tooltip: tooltipStyle.value,
  legend: {
    data: ['访问量 PV', '独立访客 UV'],
    top: 0,
    right: 0,
    itemWidth: 10,
    itemHeight: 6,
    itemGap: 12,
    textStyle: { color: chartInk.value.muted, fontSize: 11 }
  },
  grid: { left: 2, right: 8, bottom: 2, top: 30, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: visitTrendData.value.dates,
    axisLine: { lineStyle: { color: chartInk.value.border } },
    axisTick: { show: false },
    axisLabel: { color: chartInk.value.muted, fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: chartInk.value.muted, fontSize: 11 },
    splitLine: { lineStyle: { color: chartInk.value.grid, type: 'dashed' as const } }
  },
  series: [
    {
      name: '访问量 PV',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: visitTrendData.value.visits,
      color: palette.value[0],
      lineStyle: { width: 2 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: `${palette.value[0]}2e` },
            { offset: 1, color: `${palette.value[0]}05` }
          ]
        }
      },
      markPoint: {
        symbolSize: 7,
        label: { show: false },
        data: [{ type: 'max' }]
      }
    },
    {
      name: '独立访客 UV',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: visitTrendData.value.uniqueVisitors,
      color: palette.value[3],
      lineStyle: { width: 1.6, type: 'dashed' }
    }
  ]
}))

const loadAll = async () => {
  loading.value = true
  trendLoading.value = true
  visitLoading.value = true

  const tasks = [
    (async () => {
      try {
        const res = await getAdminStats()
        stats.value = res.data
      } catch (error) {
        console.error('获取统计数据失败', error)
      } finally {
        loading.value = false
      }
    })(),
    (async () => {
      try {
        const res = await getAdminStatsTrend()
        trendData.value = demoMode.value ? buildDemoStatsTrend(7) : res.data
      } catch (error) {
        console.error('获取趋势数据失败', error)
      } finally {
        trendLoading.value = false
      }
    })(),
    (async () => {
      try {
        const [visitRes, trendRes, sparkRes] = await Promise.all([
          getVisitStats(),
          getVisitTrend(visitRange.value),
          getVisitTrend(30)
        ])
        visitStats.value = demoMode.value ? demoVisitStats : visitRes.data
        visitTrendData.value = demoMode.value
          ? buildDemoVisitTrend(visitRange.value)
          : trendRes.data
        const demoSpark = buildDemoVisitTrend(30)
        sparkTrend.value = demoMode.value
          ? { dates: demoSpark.dates, visits: demoSpark.visits }
          : sparkRes.data
      } catch (error) {
        console.error('获取访问统计数据失败', error)
      } finally {
        visitLoading.value = false
      }
    })(),
    (async () => {
      try {
        const res = await getAuditStats()
        auditStats.value = res.data || {}
      } catch (error) {
        console.error('获取审核统计失败', error)
      }
    })(),
    (async () => {
      try {
        const res = await getReportStats()
        reportStats.value = res.data || {}
      } catch (error) {
        console.error('获取举报统计失败', error)
      }
    })(),
    (async () => {
      try {
        const res = await getRepairStats()
        repairStats.value = res.data || {}
      } catch (error) {
        console.error('获取报修统计失败', error)
      }
    })(),
    (async () => {
      try {
        const res = await getSystemHealth()
        healthNodes.value = (res.data?.nodes || []).map(toChainNode)
      } catch (error) {
        console.error('获取系统健康状态失败', error)
        healthNodes.value = []
      }
    })(),
    (async () => {
      try {
        const res = await getSystemMetrics(24)
        apiMetrics.value = res.data || null
      } catch (error) {
        console.error('获取接口指标失败', error)
        apiMetrics.value = null
      }
    })(),
    (async () => {
      try {
        const res = await getSearchMetrics(7)
        searchMetrics.value = res.data || null
      } catch (error) {
        console.error('获取搜索指标失败', error)
        searchMetrics.value = null
      }
    })(),
    (async () => {
      try {
        const res = await getAiMetrics(24)
        aiMetrics.value = res.data || null
      } catch (error) {
        console.error('获取 AI 指标失败', error)
        aiMetrics.value = null
      }
    })(),
    (async () => {
      try {
        const res = await getSecurityMetrics(24)
        securityMetrics.value = res.data || null
      } catch (error) {
        console.error('获取安全指标失败', error)
        securityMetrics.value = null
      }
    })(),
    (async () => {
      try {
        const res = await getHotPoems(30, 5)
        hotPoems.value = demoMode.value ? demoHotPoems : res.data || []
      } catch (error) {
        console.error('获取热门诗词失败', error)
        hotPoems.value = []
      }
    })()
  ]

  await Promise.allSettled(tasks)
  updatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

let timer: ReturnType<typeof setInterval> | undefined

watch(demoMode, () => {
  loadAll()
})

onMounted(() => {
  loadAll()
  timer = setInterval(loadAll, REFRESH_MS)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="dashboard">
    <header class="dash-head">
      <div class="brand">
        <span class="brand-seal">墨</span>
        <div class="brand-text">
          <h2 class="page-title">墨里数章，一屏尽览</h2>
          <p class="head-sub">墨渊后台 · 数据总览 · {{ todayLabel }}</p>
        </div>
      </div>

      <div class="head-side">
        <AdminDemoToggle />
        <span class="status-pill" :class="displayHealthLevel">
          <i class="dot"></i>{{ displayHealthText }}
        </span>
        <span class="head-time"><i class="live-dot"></i>{{ updatedAt }}</span>
      </div>
    </header>

    <section class="verdict-band" :class="globalVerdict.level">
      <div class="verdict-main">
        <span class="verdict-led"></span>
        <div class="verdict-text">
          <b>{{ globalVerdict.title }}</b>
          <p>{{ globalVerdict.summary }}</p>
        </div>
      </div>
      <div class="verdict-facts">
        <div v-for="fact in globalVerdict.facts" :key="fact.label" class="verdict-fact">
          <span>{{ fact.label }}</span>
          <b>{{ fact.value }}</b>
        </div>
      </div>
    </section>

    <section class="panel hero-panel">
      <div class="panel-head">
        <span class="panel-title">资源总览</span>
        <span class="panel-note">核心资源规模 · 近 7 天变化</span>
      </div>

      <div class="kpi-grid">
        <el-tooltip
          v-for="kpi in kpis"
          :key="kpi.key"
          :disabled="!statDetails[kpi.key]"
          placement="bottom"
          effect="light"
          popper-class="poem-stat-popper"
          :show-after="120"
        >
          <template #content>
            <div class="psd">
              <div class="psd-bar"></div>
              <div class="psd-title">{{ kpi.tipTitle }}</div>
              <div v-for="g in (statDetails[kpi.key]?.groups || [])" :key="g.title" class="psd-sec">
                <div class="psd-slabel">{{ g.title }}</div>
                <div v-for="r in g.rows" :key="r.name" class="psd-row">
                  <i class="psd-dot" :style="{ background: r.color }"></i>
                  <span class="psd-name">{{ r.name }}</span>
                  <b class="psd-num">{{ r.value }}</b>
                </div>
                <div v-if="g.sub" class="psd-sub">{{ g.sub }}</div>
              </div>
              <div v-if="statDetails[kpi.key]?.tip" class="psd-tip">{{ statDetails[kpi.key].tip }}</div>
            </div>
          </template>

          <div class="kpi">
            <i
              class="kpi-glow"
              :style="{
                background: `radial-gradient(circle at 100% 0%, ${palette[kpi.tone]}26, transparent 68%)`
              }"
            ></i>
            <div class="kpi-top">
              <span class="kpi-icon" :style="{ color: palette[kpi.tone] }">
                <el-icon :size="15"><component :is="kpi.icon" /></el-icon>
              </span>
              <span class="kpi-label">{{ kpi.label }}</span>
            </div>
            <div class="kpi-num">{{ loading ? '...' : kpi.value }}</div>
            <div class="kpi-sub">{{ kpi.sub }}</div>
          </div>
        </el-tooltip>
      </div>
    </section>

    <section class="access-strip">
      <el-tooltip
        v-for="card in visitCards"
        :key="card.key"
        placement="bottom"
        effect="light"
        popper-class="poem-stat-popper"
        :show-after="120"
      >
        <template #content>
          <div class="psd">
            <div class="psd-bar"></div>
            <div class="psd-title">{{ card.label }} · 统计口径</div>
            <div class="psd-tip">{{ card.tip }}</div>
          </div>
        </template>

        <div class="panel access-card">
          <div class="access-main">
            <div class="kpi-top">
              <span class="kpi-icon" :style="{ color: palette[card.tone] }">
                <el-icon :size="15"><component :is="card.icon" /></el-icon>
              </span>
              <span class="kpi-label">{{ card.label }}</span>
            </div>
            <div class="access-num">
              {{ visitLoading ? '...' : (visitStats[card.key as keyof typeof visitStats] || 0) }}
            </div>
            <div v-if="card.key === 'todayVisits'" class="access-uv">
              独立访客 UV {{ visitLoading ? '…' : lastUv }}
            </div>
            <div class="spark-foot">
              <span class="spark-range">{{ visitSpark[card.key]?.range }}</span>
              <span
                class="spark-note"
                :class="{ up: visitSpark[card.key]?.up === true, down: visitSpark[card.key]?.up === false }"
              >
                {{ visitSpark[card.key]?.note }}
              </span>
            </div>
          </div>

          <div class="access-spark">
            <Sparkline
              :values="visitSpark[card.key]?.values || []"
              :color="palette[card.tone]"
              :height="58"
            />
          </div>
        </div>
      </el-tooltip>
    </section>

    <div class="charts-row">
      <section v-loading="visitLoading" class="panel trend-panel">
        <div class="panel-head">
          <span class="panel-title">访问趋势 · 近 {{ visitRange }} 天</span>
          <div class="trend-head-side">
            <span class="panel-note">请求量 PV / 独立访客 UV</span>
            <div class="range-switch" role="group" aria-label="访问趋势时间范围">
              <button
                class="range-btn"
                :class="{ active: visitRange === 7 }"
                @click="setVisitRange(7)"
              >
                7 天
              </button>
              <button
                class="range-btn"
                :class="{ active: visitRange === 30 }"
                @click="setVisitRange(30)"
              >
                30 天
              </button>
            </div>
          </div>
        </div>
        <v-chart class="trend-chart" :option="visitChartOption" autoresize />
      </section>

      <section v-loading="trendLoading" class="panel trend-panel">
        <div class="panel-head">
          <span class="panel-title">内容增长 · 近 7 天</span>
          <span class="panel-note">用户 / 诗词 / 帖子每日新增</span>
        </div>
        <v-chart v-if="hasContentGrowth" class="trend-chart" :option="lineChartOption" autoresize />
        <AdminEmptyState
          v-else
          title="近 7 天暂无新增"
          note="用户、诗词、帖子产生新增后，这里会自动出现增长曲线。"
        />
      </section>
    </div>

    <section class="panel hot-panel">
      <div class="panel-head">
        <span class="panel-title">热门诗词 · 近 30 天</span>
        <span class="panel-note">收藏 ×3 + 浏览 · 只统计真实互动</span>
      </div>
      <div v-if="hotPoems.length" class="hot-row">
        <div v-for="(poem, index) in hotPoems" :key="poem.id" class="hot-item">
          <span class="hot-rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
          <div class="hot-meta">
            <span class="hot-title" :title="poem.title">{{ poem.title }}</span>
            <span class="hot-sub">收藏 {{ poem.favorites }} · 浏览 {{ poem.views }}</span>
          </div>
          <span class="hot-score">{{ poem.score }}</span>
        </div>
      </div>
      <AdminEmptyState
        v-else
        title="暂无热门诗词"
        note="产生收藏或浏览记录后，这里会按真实互动自动排序。"
      />
    </section>

    <div class="split-row">
      <ResourceDistribution class="resource-band" />
      <TodoTimeline :items="todoItems" />
    </div>

    <MetricsBar :items="metricsItems" />

    <div class="health-band">
      <HealthChain :nodes="chainNodes" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  // ---------------- 顶部指挥栏 ----------------
  .dash-head {
    display: flex;
    align-items: center;
    gap: 24px;
    padding: 2px 4px 14px;
    flex-wrap: wrap;
  }

  .brand {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-shrink: 0;
  }

  .brand-seal {
    width: 42px;
    height: 42px;
    display: grid;
    place-items: center;
    border-radius: 10px;
    font-family: var(--adm-font-serif);
    font-size: 21px;
    font-weight: 600;
    color: var(--adm-on-primary);
    background: linear-gradient(135deg, var(--adm-primary), color-mix(in srgb, var(--adm-primary) 62%, var(--adm-chart-2)));
    box-shadow: 0 6px 16px color-mix(in srgb, var(--adm-primary) 28%, transparent);
  }

  .brand-text {
    min-width: 0;
  }

  .page-title {
    margin: 0 0 4px;
    font-family: var(--adm-font-serif);
    font-size: 23px;
    font-weight: 600;
    line-height: 1.25;
    letter-spacing: 2px;
    color: var(--adm-text);
  }

  .head-sub {
    margin: 0;
    font-size: 12px;
    letter-spacing: 1px;
    color: var(--adm-text-faint);
  }

  .head-facts {
    display: flex;
    align-items: stretch;
    gap: 0;
    margin-left: auto;
    padding: 2px 0;
    border-left: 1px solid var(--adm-border-soft);
  }

  .fact {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 0 20px;
    border-right: 1px solid var(--adm-border-soft);

    &:last-child {
      border-right: none;
    }
  }

  .fact-label {
    font-size: 11px;
    letter-spacing: 0.5px;
    color: var(--adm-text-faint);
    white-space: nowrap;
  }

  .fact-value {
    font-family: var(--adm-font-serif);
    font-size: 18px;
    font-weight: 600;
    line-height: 1.2;
    color: var(--adm-text);
    font-variant-numeric: tabular-nums;
  }

  .head-side {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-left: auto;
    flex-shrink: 0;
  }

  .status-pill {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 3px 10px;
    border-radius: 11px;
    font-size: 12px;
    background: color-mix(in srgb, var(--adm-ok) 14%, transparent);
    color: var(--adm-ok);

    .dot {
      width: 5px;
      height: 5px;
      border-radius: 50%;
      background: currentColor;
      flex-shrink: 0;
    }

    &.warn {
      background: color-mix(in srgb, var(--adm-warn) 14%, transparent);
      color: var(--adm-warn);
    }

    &.down {
      background: color-mix(in srgb, var(--adm-down) 14%, transparent);
      color: var(--adm-down);
    }

    &.unknown {
      background: color-mix(in srgb, var(--adm-unknown) 14%, transparent);
      color: var(--adm-unknown);
    }
  }

  .head-time {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: var(--adm-text-faint);
    font-variant-numeric: tabular-nums;
  }

  .live-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--adm-ok);
    animation: live-pulse 2s ease-out infinite;
  }

  @keyframes live-pulse {
    0% {
      box-shadow: 0 0 0 0 color-mix(in srgb, var(--adm-ok) 40%, transparent);
    }
    70%,
    100% {
      box-shadow: 0 0 0 5px transparent;
    }
  }

  // ---------------- 全局结论条 ----------------
  .verdict-band {
    display: flex;
    align-items: center;
    gap: 18px;
    margin-bottom: 14px;
    padding: 12px 18px;
    border: 1px solid var(--adm-border-soft);
    border-radius: 10px;
    background: linear-gradient(
      100deg,
      color-mix(in srgb, var(--adm-ok) 8%, transparent) 0%,
      var(--adm-surface) 48%
    );
    flex-wrap: wrap;

    &.warn {
      background: linear-gradient(
        100deg,
        color-mix(in srgb, var(--adm-warn) 10%, transparent) 0%,
        var(--adm-surface) 48%
      );
    }

    &.down {
      background: linear-gradient(
        100deg,
        color-mix(in srgb, var(--adm-down) 12%, transparent) 0%,
        var(--adm-surface) 48%
      );
    }
  }

  .verdict-main {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    min-width: 0;
    flex: 1;
  }

  .verdict-led {
    width: 10px;
    height: 10px;
    margin-top: 4px;
    border-radius: 50%;
    flex-shrink: 0;
    background: var(--adm-ok);
    box-shadow: 0 0 0 4px color-mix(in srgb, var(--adm-ok) 18%, transparent);

    .warn & {
      background: var(--adm-warn);
      box-shadow: 0 0 0 4px color-mix(in srgb, var(--adm-warn) 18%, transparent);
    }

    .down & {
      background: var(--adm-down);
      box-shadow: 0 0 0 4px color-mix(in srgb, var(--adm-down) 18%, transparent);
    }
  }

  .verdict-text {
    min-width: 0;

    b {
      display: block;
      font-size: 14px;
      font-weight: 600;
      letter-spacing: 0.5px;
      color: var(--adm-text);
    }

    p {
      margin: 4px 0 0;
      font-size: 12px;
      line-height: 1.6;
      color: var(--adm-text-muted);
    }
  }

  .verdict-facts {
    display: flex;
    align-items: center;
    gap: 18px;
    flex-wrap: wrap;
  }

  .verdict-fact {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 66px;

    span {
      font-size: 10.5px;
      letter-spacing: 0.5px;
      color: var(--adm-text-faint);
    }

    b {
      font-size: 16px;
      font-weight: 600;
      color: var(--adm-text);
      font-variant-numeric: tabular-nums;
    }
  }

  // ---------------- 通用数据面板 ----------------
  .panel {
    position: relative;
    border: 1px solid var(--adm-border-soft);
    border-radius: 10px;
    background: var(--adm-surface);
    overflow: hidden;
    transition: border-color 0.2s, box-shadow 0.2s;

    &::before,
    &::after {
      content: '';
      position: absolute;
      width: 12px;
      height: 12px;
      pointer-events: none;
      opacity: 0.45;
      z-index: 1;
    }

    &::before {
      top: 6px;
      left: 6px;
      border-top: 1px solid var(--adm-primary);
      border-left: 1px solid var(--adm-primary);
    }

    &::after {
      right: 6px;
      bottom: 6px;
      border-right: 1px solid var(--adm-primary);
      border-bottom: 1px solid var(--adm-primary);
    }
  }

  .hero-panel {
    background-image: linear-gradient(
      118deg,
      color-mix(in srgb, var(--adm-primary) 7%, transparent) 0%,
      transparent 45%
    );
  }

  .panel-head {
    display: flex;
    align-items: baseline;
    gap: 10px;
    padding: 13px 18px 10px;
  }

  .panel-title {
    position: relative;
    padding-left: 10px;
    font-size: 13px;
    font-weight: 600;
    letter-spacing: 1px;
    color: var(--adm-text);

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      width: 3px;
      height: 12px;
      transform: translateY(-50%);
      border-radius: 2px;
      background: linear-gradient(180deg, var(--adm-primary), transparent);
    }
  }

  .panel-note {
    font-size: 11px;
    letter-spacing: 0.5px;
    color: var(--adm-text-faint);
  }

  // ---------------- 核心资源 KPI ----------------
  .kpi-grid {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    padding: 0 6px 10px;

    @media (max-width: 1500px) {
      grid-template-columns: repeat(3, minmax(0, 1fr));
      row-gap: 4px;
    }

    @media (max-width: 760px) {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  .kpi {
    position: relative;
    min-width: 0;
    padding: 4px 16px 12px;

    & + & {
      border-left: 1px solid var(--adm-border-soft);
    }

    @media (max-width: 1500px) {
      &:nth-child(3n + 1) {
        border-left: none;
      }
    }

    @media (max-width: 760px) {
      &:nth-child(2n + 1) {
        border-left: none;
      }

      &:nth-child(3n + 1) {
        border-left: 1px solid var(--adm-border-soft);
      }

      &:first-child {
        border-left: none;
      }
    }
  }

  .kpi-glow {
    position: absolute;
    inset: 0;
    opacity: 0.9;
    pointer-events: none;
  }

  .kpi-top {
    position: relative;
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 6px;
  }

  .kpi-icon {
    display: inline-flex;
    align-items: center;
    flex-shrink: 0;
  }

  .kpi-label {
    font-size: 12px;
    letter-spacing: 0.5px;
    color: var(--adm-text-muted);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .kpi-num {
    position: relative;
    font-family: var(--adm-font-serif);
    font-size: 36px;
    font-weight: 600;
    line-height: 1.08;
    color: var(--adm-text);
    font-variant-numeric: tabular-nums;
    letter-spacing: 0.5px;
    margin-bottom: 8px;
  }

  .kpi-sub {
    position: relative;
    font-size: 11px;
    color: var(--adm-text-faint);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  // ---------------- 访问四卡 ----------------
  .access-strip {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
    margin-top: 14px;

    @media (max-width: 1200px) {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    @media (max-width: 700px) {
      grid-template-columns: 1fr;
    }
  }

  .access-card {
    display: flex;
    align-items: stretch;
    gap: 12px;
    padding: 13px 16px 11px;
    cursor: default;

    &:hover {
      border-color: var(--adm-border);
      box-shadow: var(--adm-shadow);
    }
  }

  .access-main {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  .access-num {
    font-family: var(--adm-font-serif);
    font-size: 30px;
    font-weight: 600;
    line-height: 1.12;
    color: var(--adm-text);
    font-variant-numeric: tabular-nums;
    margin: 2px 0 2px;
  }

  .access-spark {
    width: 42%;
    min-width: 72px;
    align-self: center;
  }

  .spark-foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-top: auto;
    font-size: 11px;
    line-height: 1.4;
  }

  .spark-range {
    color: var(--adm-text-faint);
    white-space: nowrap;
  }

  .spark-note {
    color: var(--adm-text-muted);
    font-variant-numeric: tabular-nums;
    white-space: nowrap;

    &.up {
      color: var(--adm-ok);
    }

    &.down {
      color: var(--adm-warn);
    }
  }

  // ---------------- 主区三栏 ----------------
  .main-grid {
    display: grid;
    grid-template-columns: minmax(0, 1.08fr) minmax(0, 1.08fr) minmax(300px, 0.9fr);
    gap: 14px;
    margin-top: 14px;

    @media (max-width: 1360px) {
      grid-template-columns: repeat(2, minmax(0, 1fr));

      > :last-child {
        grid-column: 1 / -1;
      }
    }

    @media (max-width: 900px) {
      grid-template-columns: 1fr;
    }
  }

  // ---------------- 热门诗词 ----------------
  .hot-panel {
    margin-top: 14px;
  }

  .hot-row {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 10px;
    padding: 0 14px 14px;

    @media (max-width: 1200px) {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    @media (max-width: 640px) {
      grid-template-columns: 1fr;
    }
  }

  .hot-item {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
    padding: 8px 10px;
    border: 1px solid var(--adm-border-soft);
    border-radius: 8px;
    background: var(--adm-surface-muted);
  }

  .hot-rank {
    width: 20px;
    height: 20px;
    display: grid;
    place-items: center;
    flex-shrink: 0;
    border-radius: 6px;
    font-size: 11px;
    font-weight: 600;
    color: var(--adm-text-muted);
    background: var(--adm-surface);
    font-variant-numeric: tabular-nums;

    &.top {
      color: var(--adm-on-primary);
      background: var(--adm-primary);
    }
  }

  .hot-meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
    flex: 1;
  }

  .hot-title {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 12px;
    color: var(--adm-text);
  }

  .hot-sub {
    font-size: 10px;
    color: var(--adm-text-faint);
    font-variant-numeric: tabular-nums;
  }

  .hot-score {
    flex-shrink: 0;
    font-size: 12px;
    font-weight: 600;
    color: var(--adm-primary);
    font-variant-numeric: tabular-nums;
  }

  // ---------------- 趋势分析双栏 ----------------
  .charts-row,
  .split-row {
    display: grid;
    grid-template-columns: minmax(0, 2.05fr) minmax(0, 1fr);
    gap: 14px;
    margin-top: 14px;

    @media (max-width: 1200px) {
      grid-template-columns: 1fr;
    }
  }

  .split-row .resource-band {
    margin-top: 0;
  }

  .trend-head-side {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-left: auto;
    flex-shrink: 0;
  }

  .range-switch {
    display: inline-flex;
    gap: 2px;
    padding: 2px;
    border: 1px solid var(--adm-border-soft);
    border-radius: 12px;
    background: var(--adm-surface-muted);
  }

  .range-btn {
    padding: 2px 9px;
    border: none;
    border-radius: 9px;
    background: transparent;
    color: var(--adm-text-muted);
    font-size: 11px;
    cursor: pointer;
    transition: all 0.18s ease;

    &:hover {
      color: var(--adm-primary);
    }

    &.active {
      color: var(--adm-on-primary);
      background: var(--adm-primary);
    }
  }

  .access-uv {
    margin-top: 2px;
    font-size: 11px;
    color: var(--adm-text-faint);
    font-variant-numeric: tabular-nums;
  }

  .trend-panel {
    display: flex;
    flex-direction: column;
    min-height: 262px;
  }

  .trend-chart {
    width: 100%;
    height: 214px;
    padding: 0 10px 8px;
  }

  // ---------------- 资源底座 + 链路 ----------------
  .resource-band {
    margin-top: 14px;
  }

  .health-band {
    margin-top: 14px;

    :deep(.chain-card) {
      margin-bottom: 0;
      border-radius: 10px;
    }
  }
}
</style>

<style lang="scss">
/* 诗词总数悬浮说明面板：popper 会被 teleport 到 body，故使用全局样式 */
.poem-stat-popper.el-popper {
  padding: 0;
  background: var(--adm-surface-muted);
  border: 1px solid var(--adm-border);
  border-radius: 12px;
  box-shadow: var(--adm-shadow);
  overflow: hidden;
}

.poem-stat-popper.el-popper .el-popper__arrow::before {
  background: var(--adm-surface-muted);
  border: 1px solid var(--adm-border);
}

.poem-stat-popper .psd {
  position: relative;
  min-width: 248px;
  padding: 16px 16px 14px;
}

.poem-stat-popper .psd-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--adm-chart-2), var(--adm-primary), var(--adm-chart-2));
}

.poem-stat-popper .psd-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--adm-text);
  letter-spacing: 1px;
  margin-bottom: 10px;
}

.poem-stat-popper .psd-sec {
  margin-bottom: 10px;
}

.poem-stat-popper .psd-slabel {
  font-size: 12px;
  color: var(--adm-text-faint);
  margin-bottom: 6px;
  padding-left: 8px;
  border-left: 3px solid var(--adm-chart-2);
}

.poem-stat-popper .psd-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--adm-text-muted);
  line-height: 1.9;
}

.poem-stat-popper .psd-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.poem-stat-popper .psd-name {
  flex: 1;
}

.poem-stat-popper .psd-num {
  color: var(--adm-primary);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.poem-stat-popper .psd-sub {
  font-size: 12px;
  color: var(--adm-text-faint);
  padding-left: 16px;
  margin-top: 2px;
}

.poem-stat-popper .psd-tip {
  margin-top: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--adm-text-2);
  background: color-mix(in srgb, var(--adm-primary) 12%, transparent);
  border: 1px dashed var(--adm-border);
}
</style>
