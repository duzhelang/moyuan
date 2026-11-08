<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import {
  getAiMetrics,
  getRecentEvents,
  getSecurityMetrics,
  getSystemHealth,
  getSystemMetrics
} from '@/api/modules/admin'
import type {
  AiMetrics,
  RecentEvent,
  SecurityMetrics,
  SystemHealthNode,
  SystemMetrics
} from '@/api/modules/admin'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import { readChartTheme, useAdminTheme } from '@/composables/useAdminTheme'
import type { StatusLevel } from '@/types/admin-dashboard'

use([CanvasRenderer, LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const { resolvedTheme } = useAdminTheme()
const chartTheme = computed(() => {
  void resolvedTheme.value
  return readChartTheme()
})

interface MonitorRow {
  label: string
  value: string
  status?: StatusLevel
  /** true = 真实数据，false/缺省 = 待接入 */
  live?: boolean
}

interface MonitorGroup {
  key: string
  title: string
  note: string
  rows: MonitorRow[]
  /** 数据缺口说明 */
  gap: string
  drill?: { text: string; link: string }
}

const route = useRoute()

/** 监控时间窗：6 / 24 小时，接口与按小时序列共用 */
const hours = ref<6 | 24>(24)
const autoRefresh = ref(true)
const lastUpdated = ref('—')
/** 趋势区当前指标：接口 / AI / 安全 */
const trendTab = ref<'api' | 'ai' | 'security'>('api')

/** 趋势图实例：点击图例会隐藏系列，需要「恢复原图」一键重置 */
const trendChartRef = ref<unknown>(null)

const resetTrend = () => {
  // 恢复默认的接口视图，再把被点掉的图例全部选回来
  trendTab.value = 'api'
  nextTick(() => {
    const chart = trendChartRef.value as {
      dispatchAction?: (action: { type: string }) => void
    } | null
    chart?.dispatchAction?.({ type: 'legendAllSelect' })
  })
}

const activeTab = computed(() => String(route.query.tab || ''))

const isActive = (key: string) => activeTab.value === key

const statusOf = (row: MonitorRow): StatusLevel => row.status || 'ok'

/** 整组都没有真实数据时走空态，避免一屏横杠看起来像页面坏了 */
const hasLive = (group: MonitorGroup) => group.rows.some((row) => row.live)

const STATUS_TEXT: Record<string, string> = {
  ok: '正常',
  warn: '降级',
  down: '断连'
}

// ==================== 可用性（接口指标表） ====================
const metrics = ref<SystemMetrics | null>(null)

const loadMetrics = async () => {
  try {
    const res = await getSystemMetrics(hours.value)
    metrics.value = res.data || null
  } catch (error) {
    console.error('获取接口指标失败', error)
    metrics.value = null
  }
}

const availabilityRows = computed<MonitorRow[]>(() => {
  const m = metrics.value
  const ok = !!m && m.available && m.total > 0
  const slowest = ok && m && m.slowTop.length ? m.slowTop[0] : null
  return [
    {
      label: '接口错误率（5xx）',
      value: ok && m ? (m.errorRate * 100).toFixed(2) + '%' : '—',
      status: ok && m && m.errorRate >= 0.01 ? 'warn' : 'ok',
      live: ok
    },
    {
      label: 'P95 响应',
      value: ok && m && m.p95Ms != null ? m.p95Ms + ' ms' : '—',
      status: ok && m && (m.p95Ms || 0) > 500 ? 'warn' : 'ok',
      live: ok
    },
    {
      label: '最慢接口',
      value: slowest ? slowest.path + ' · ' + slowest.avgMs + ' ms' : '—',
      status: slowest && slowest.avgMs > 500 ? 'warn' : 'ok',
      live: !!slowest
    },
    {
      label: '24h 样本 / 平均耗时',
      value: ok && m ? m.total + ' 次 / ' + m.avgMs + ' ms' : '—',
      live: ok
    },
    { label: '前端 JS 错误', value: '—', live: false }
  ]
})

// ==================== AI 链路（ai_call_log） ====================
const aiMetrics = ref<AiMetrics | null>(null)

const loadAiMetrics = async () => {
  try {
    const res = await getAiMetrics(hours.value)
    aiMetrics.value = res.data || null
  } catch (error) {
    console.error('获取 AI 指标失败', error)
    aiMetrics.value = null
  }
}

const aiRows = computed<MonitorRow[]>(() => {
  const a = aiMetrics.value
  const ok = !!a && a.available && a.total > 0
  const quickest = ok && a && a.byEndpoint.length
    ? [...a.byEndpoint].sort((x, y) => y.count - x.count)[0]
    : null
  return [
    {
      label: '24h 调用轮次',
      value: ok && a ? String(a.total) : '—',
      live: ok
    },
    {
      label: '首字延迟（流式均值）',
      value: ok && a && a.ttftAvgMs != null ? a.ttftAvgMs + ' ms' : '—',
      status: ok && a && (a.ttftAvgMs || 0) > 3000 ? 'warn' : 'ok',
      live: ok && !!a && a.ttftAvgMs != null
    },
    {
      label: '平均 / P95 耗时',
      value: ok && a ? a.avgMs + ' / ' + (a.p95Ms ?? '—') + ' ms' : '—',
      status: ok && a && (a.p95Ms || 0) > 30_000 ? 'warn' : 'ok',
      live: ok
    },
    {
      label: '流式调用数',
      value: ok && a ? String(a.streamCount) : '—',
      live: ok
    },
    {
      label: '调用失败率',
      value: ok && a ? (a.errorRate * 100).toFixed(1) + '%' : '—',
      status: ok && a && a.errorRate >= 0.05 ? 'warn' : 'ok',
      live: ok
    },
    {
      label: '调用最多入口',
      value: quickest ? quickest.endpoint + ' · ' + quickest.count + ' 次' : '—',
      live: !!quickest
    }
  ]
})

// ==================== 安全审计（security_event） ====================
const securityMetrics = ref<SecurityMetrics | null>(null)

const loadSecurityMetrics = async () => {
  try {
    const res = await getSecurityMetrics(hours.value)
    securityMetrics.value = res.data || null
  } catch (error) {
    console.error('获取安全指标失败', error)
    securityMetrics.value = null
  }
}

const securityRows = computed<MonitorRow[]>(() => {
  const s = securityMetrics.value
  const ok = !!s && s.available
  const topIp = ok && s && s.topIps.length ? s.topIps[0] : null
  return [
    {
      label: '登录失败（24h）',
      value: ok && s ? String(s.loginFail) : '—',
      status: ok && s && s.loginFail >= 20 ? 'warn' : 'ok',
      live: ok
    },
    {
      label: '越权 403',
      value: ok && s ? String(s.forbidden) : '—',
      status: ok && s && s.forbidden >= 10 ? 'warn' : 'ok',
      live: ok
    },
    {
      label: '限流触发',
      value: ok && s ? String(s.rateLimit) : '—',
      status: ok && s && s.rateLimit >= 20 ? 'warn' : 'ok',
      live: ok
    },
    {
      label: '登录失败涉及 IP',
      value: ok && s ? String(s.distinctIp) : '—',
      live: ok
    },
    {
      label: '失败最多 IP',
      value: topIp ? topIp.ip + ' · ' + topIp.count + ' 次' : '—',
      live: !!topIp
    },
    {
      label: '操作失败（24h）',
      value: ok && s && s.highRiskOps != null ? String(s.highRiskOps) : '—',
      status: ok && s && (s.highRiskOps || 0) >= 5 ? 'warn' : 'ok',
      live: ok && !!s && s.highRiskOps != null
    }
  ]
})

/** 安全事件总量：三个计数器相加，作为大屏「安全事件」的主数值 */
const totalSecurity = computed(() => {
  const s = securityMetrics.value
  return s && s.available ? s.loginFail + s.forbidden + s.rateLimit : null
})

// ==================== 运维资源（health 节点指标） ====================
const healthLoading = ref(true)
const healthNodes = ref<SystemHealthNode[]>([])
const checkedAt = ref('')

const loadHealth = async () => {
  try {
    const res = await getSystemHealth()
    healthNodes.value = res.data?.nodes || []
    checkedAt.value = res.data?.checkedAt
      ? new Date(res.data.checkedAt).toLocaleTimeString('zh-CN', { hour12: false })
      : ''
  } catch (error) {
    console.error('获取健康检查失败', error)
    healthNodes.value = []
  } finally {
    healthLoading.value = false
  }
}

/** 从 health 节点里按标签关键字取指标值，取不到返回 null */
const metricOf = (nodeKey: string, label: string): string | null => {
  const node = healthNodes.value.find((n) => n.key === nodeKey)
  if (!node) return null
  const hit = node.metrics.find((m) => m.label.includes(label))
  return hit ? hit.value : null
}

const nodeLatency = (nodeKey: string): number | null => {
  const node = healthNodes.value.find((n) => n.key === nodeKey)
  return node && node.latencyMs != null && node.latencyMs > 0 ? node.latencyMs : null
}

const opsRows = computed<MonitorRow[]>(() => {
  const redisHit = metricOf('redis', '命中率')
  const redisMem = metricOf('redis', '内存')
  const mysqlLat = nodeLatency('mysql')
  const ragLat = nodeLatency('rag')
  const mysqlOk = healthNodes.value.some((n) => n.key === 'mysql' && n.status !== 'down')
  const ragOk = healthNodes.value.some((n) => n.key === 'rag' && n.status !== 'down')
  return [
    {
      label: 'MySQL 探活耗时',
      value: mysqlLat != null ? mysqlLat + ' ms' : mysqlOk ? '< 1 ms' : '—',
      status: mysqlOk ? 'ok' : 'down',
      live: healthNodes.value.length > 0
    },
    {
      label: 'Redis 命中率',
      value: redisHit ?? '—',
      status: redisHit && redisHit !== '—' && parseFloat(redisHit) < 80 ? 'warn' : 'ok',
      live: !!redisHit
    },
    {
      label: 'Redis 内存占用',
      value: redisMem ?? '—',
      live: !!redisMem
    },
    {
      label: 'Redis Key 数量',
      value: metricOf('redis', 'Key 数量') ?? '—',
      live: !!metricOf('redis', 'Key 数量')
    },
    {
      label: 'RAG 服务耗时',
      value: ragLat != null ? ragLat + ' ms' : '—',
      status: ragOk ? 'ok' : 'down',
      live: ragOk
    },
    { label: '上传目录 / 备份', value: '—', live: false }
  ]
})

const groups = computed<MonitorGroup[]>(() => [
  {
    key: 'availability',
    title: '可用性',
    note: '接口错误率与响应耗时',
    rows: availabilityRows.value,
    gap:
      metrics.value && !metrics.value.available
        ? metrics.value.message || '指标表不可用'
        : '前端 JS 错误需接入前端错误上报'
  },
  {
    key: 'ai',
    title: 'AI 链路',
    note: '调用耗时与失败率',
    rows: aiRows.value,
    gap:
      aiMetrics.value && !aiMetrics.value.available
        ? aiMetrics.value.message || 'AI 调用日志表不可用'
        : '拒答次数需在生成侧标记后才可统计',
    drill: { text: '查看模型配置 →', link: '/admin/ai-models' }
  },
  {
    key: 'security',
    title: '安全审计',
    note: '登录与越权行为',
    rows: securityRows.value,
    gap:
      securityMetrics.value && !securityMetrics.value.available
        ? securityMetrics.value.message || '安全事件表不可用'
        : '按类型明细与 IP 聚合已入库，可对接操作日志页做下钻',
    drill: { text: '查看操作日志 →', link: '/admin/logs' }
  },
  {
    key: 'ops',
    title: '运维资源',
    note: '数据库、缓存与服务耗时',
    rows: opsRows.value,
    gap: '连接池明细、慢查询、磁盘与备份待补（探活数据来自上方 health 接口）',
    drill: { text: '文件管理 →', link: '/admin/files' }
  }
])

const onlineCount = computed(
  () => healthNodes.value.filter((n) => n.status !== 'down').length
)

/** 顶部四块计分板：每组抽一个最关键的数值，先把结论摆出来 */
const scoreboard = computed(() => {
  const m = metrics.value
  const mOk = !!m && m.available && m.total > 0
  const a = aiMetrics.value
  const aOk = !!a && a.available && a.total > 0
  const s = securityMetrics.value
  const sOk = !!s && s.available
  return [
    {
      key: 'availability',
      label: '接口可用性',
      value: mOk && m ? (m.errorRate * 100).toFixed(2) + '%' : '—',
      unit: '5xx 错误率',
      note: mOk && m ? `P95 ${m.p95Ms ?? '—'} ms · ${m.total} 次请求` : '等待首批请求样本',
      status: (mOk && m && m.errorRate >= 0.01 ? 'warn' : 'ok') as StatusLevel,
      link: '/admin/monitor?tab=availability'
    },
    {
      key: 'ai',
      label: 'AI 链路',
      value: aOk && a ? (a.errorRate * 100).toFixed(1) + '%' : '—',
      unit: '调用失败率',
      note: aOk && a ? `24h ${a.total} 次 · 首字 ${a.ttftAvgMs ?? '—'} ms` : '等待 AI 调用样本',
      status: (aOk && a && a.errorRate >= 0.05 ? 'warn' : 'ok') as StatusLevel,
      link: '/admin/monitor?tab=ai'
    },
    {
      key: 'security',
      label: '安全事件',
      value: sOk ? String(totalSecurity.value) : '—',
      unit: '24h 累计',
      note: sOk && s ? `登录失败 ${s.loginFail} · 越权 ${s.forbidden}` : '等待安全事件样本',
      status: (sOk && s && (s.loginFail >= 20 || s.forbidden >= 10) ? 'warn' : 'ok') as StatusLevel,
      link: '/admin/monitor?tab=security'
    },
    {
      key: 'ops',
      label: '节点在线',
      value: healthNodes.value.length
        ? `${onlineCount.value}/${healthNodes.value.length}`
        : '—',
      unit: '探活结果',
      note: checkedAt.value ? `检查于 ${checkedAt.value}` : '等待健康检查返回',
      status: (!healthNodes.value.length
        ? 'warn'
        : healthNodes.value.some((n) => n.status === 'down')
          ? 'down'
          : healthNodes.value.some((n) => n.status === 'warn')
            ? 'warn'
            : 'ok') as StatusLevel,
      link: '/admin/monitor?tab=ops'
    }
  ]
})

// ==================== 性能概览与趋势 ====================
/** 概览事实：成功率 / 错误率 / 请求量 / 平均耗时 / P95 */
const performanceFacts = computed(() => {
  const m = metrics.value
  const ok = !!m && m.available && m.total > 0
  return [
    {
      key: 'success',
      label: '成功率',
      value: ok && m ? ((1 - m.errorRate) * 100).toFixed(2) + '%' : '—',
      status: (ok && m && m.errorRate >= 0.01 ? 'warn' : 'ok') as StatusLevel
    },
    {
      key: 'error',
      label: '错误率',
      value: ok && m ? (m.errorRate * 100).toFixed(2) + '%' : '—',
      status: (ok && m && m.errorRate >= 0.01 ? 'warn' : 'ok') as StatusLevel
    },
    { key: 'total', label: '请求总量', value: ok && m ? String(m.total) : '—', status: 'ok' as StatusLevel },
    { key: 'avg', label: '平均耗时', value: ok && m ? m.avgMs + ' ms' : '—', status: 'ok' as StatusLevel },
    {
      key: 'p95',
      label: 'P95 耗时',
      value: ok && m && m.p95Ms != null ? m.p95Ms + ' ms' : '—',
      status: (ok && m && (m.p95Ms || 0) > 500 ? 'warn' : 'ok') as StatusLevel
    }
  ]
})

/** 慢接口排行：用同组最大值归一化，右端显示耗时；显示名去掉公共 /api/ 前缀省宽度 */
const slowTop = computed(() => {
  const rows = metrics.value?.slowTop || []
  const max = Math.max(1, ...rows.map((row) => row.avgMs))
  return rows.map((row) => ({
    ...row,
    label: row.path.replace(/^\/api\//, ''),
    ratio: Math.max(4, Math.round((row.avgMs / max) * 100))
  }))
})

const statusTotal = computed(() =>
  (metrics.value?.statusBuckets || []).reduce((sum, bucket) => sum + bucket.count, 0)
)

/** 请求状态分布环图 */
const statusOption = computed(() => {
  const theme = chartTheme.value
  const buckets = (metrics.value?.statusBuckets || []).filter((bucket) => bucket.count > 0)
  return {
    tooltip: { ...theme.tooltip, trigger: 'item' as const },
    legend: {
      ...theme.legend,
      top: 'middle',
      right: 0,
      orient: 'vertical' as const
    },
    title: {
      text: String(statusTotal.value),
      subtext: `${hours.value} 小时请求`,
      left: '30%',
      top: '40%',
      textAlign: 'center' as const,
      textStyle: {
        color: theme.ink.text,
        fontSize: 17,
        fontWeight: 600,
        fontFamily: 'var(--adm-font-serif)'
      },
      subtextStyle: { color: theme.ink.muted, fontSize: 10, lineHeight: 16 }
    },
    series: [
      {
        type: 'pie',
        radius: ['48%', '70%'],
        center: ['30%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 4, borderColor: theme.ink.surface, borderWidth: 2 },
        label: { show: false },
        emphasis: {
          label: {
            show: true,
            fontSize: 11,
            fontWeight: 'bold',
            color: theme.ink.text,
            formatter: '{b}\n{d}%'
          }
        },
        labelLine: { show: false },
        data: buckets.map((bucket, index) => ({
          name: bucket.name,
          value: bucket.count,
          itemStyle: { color: theme.palette[index % theme.palette.length] }
        }))
      }
    ]
  }
})

/** 当前趋势指标的数据源 */
const trendPoints = computed(() => {
  if (trendTab.value === 'ai') {
    const rows = aiMetrics.value?.hourly || []
    return {
      labels: rows.map((row) => row.label),
      count: rows.map((row) => row.count),
      avg: rows.map((row) => row.avgMs),
      loginFail: [] as number[]
    }
  }
  if (trendTab.value === 'security') {
    const rows = securityMetrics.value?.hourly || []
    return {
      labels: rows.map((row) => row.label),
      count: rows.map((row) => row.count),
      avg: [] as number[],
      loginFail: rows.map((row) => row.loginFail)
    }
  }
  const rows = metrics.value?.hourly || []
  return {
    labels: rows.map((row) => row.label),
    count: rows.map((row) => row.count),
    avg: rows.map((row) => row.avgMs),
    loginFail: [] as number[]
  }
})

const trendHasData = computed(() => trendPoints.value.labels.length > 0)

/** 响应趋势：柱=请求/调用次数，线=平均耗时；安全页签则柱=事件、线=登录失败 */
const trendOption = computed(() => {
  const theme = chartTheme.value
  const data = trendPoints.value
  const isSecurity = trendTab.value === 'security'
  return {
    tooltip: theme.tooltip,
    legend: {
      ...theme.legend,
      // 留出右侧安全距离，避免图例文字贴边被裁
      right: 16,
      top: 2,
      // 保留图例点击切换；误点隐藏系列后可用右上角「恢复原图」重置
      data: isSecurity ? ['事件数', '登录失败'] : ['请求/调用次数', '平均耗时']
    },
    grid: { ...theme.grid, right: 56, top: 42 },
    xAxis: {
      type: 'category',
      data: data.labels,
      ...theme.categoryAxis
    },
    yAxis: isSecurity
      ? [{ type: 'value', name: '次', ...theme.valueAxis }]
      : [
          { type: 'value', name: '次', ...theme.valueAxis },
          {
            type: 'value',
            name: 'ms',
            ...theme.valueAxis,
            splitLine: { show: false }
          }
        ],
    series: isSecurity
      ? [
          {
            name: '事件数',
            type: 'bar',
            data: data.count,
            barMaxWidth: 14,
            itemStyle: { color: theme.barGradient(theme.palette[3]), borderRadius: [3, 3, 0, 0] }
          },
          {
            name: '登录失败',
            type: 'line',
            smooth: true,
            // 只有一个小时桶时，没有数据点标记的折线不可见
            showSymbol: data.labels.length <= 1,
            data: data.loginFail,
            color: theme.palette[0],
            lineStyle: { width: 1.6, type: 'dashed' }
          }
        ]
      : [
          {
            name: '请求/调用次数',
            type: 'bar',
            data: data.count,
            barMaxWidth: 14,
            itemStyle: { color: theme.barGradient(theme.palette[0]), borderRadius: [3, 3, 0, 0] }
          },
          {
            name: '平均耗时',
            type: 'line',
            yAxisIndex: 1,
            smooth: true,
            // 只有一个小时桶时，没有数据点标记的折线不可见
            showSymbol: data.labels.length <= 1,
            data: data.avg,
            color: theme.palette[2],
            lineStyle: { width: 1.8 }
          }
        ]
  }
})

// ==================== 最近异常事件 ====================
const recentEvents = ref<RecentEvent[]>([])
const eventLoading = ref(true)

const loadRecentEvents = async () => {
  try {
    const res = await getRecentEvents(12)
    recentEvents.value = res.data || []
  } catch (error) {
    console.error('获取最近异常事件失败', error)
    recentEvents.value = []
  } finally {
    eventLoading.value = false
  }
}

const SOURCE_TEXT: Record<string, string> = {
  ai: 'AI 调用',
  security: '安全',
  api: '接口'
}

const fmtTime = (t: string) => {
  if (!t) return '—'
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return t
  return `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(
    d.getHours()
  ).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const levelOf = (status: string): StatusLevel =>
  status === 'down' ? 'down' : status === 'warn' ? 'warn' : 'ok'

let timer: ReturnType<typeof setInterval> | undefined

const stopTimer = () => {
  if (timer) {
    clearInterval(timer)
    timer = undefined
  }
}

const startTimer = () => {
  stopTimer()
  if (autoRefresh.value) timer = setInterval(loadAll, 30_000)
}

const loadAll = async () => {
  await Promise.allSettled([
    loadHealth(),
    loadMetrics(),
    loadAiMetrics(),
    loadSecurityMetrics(),
    loadRecentEvents()
  ])
  lastUpdated.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

// 切换时间窗只重新拉取指标类接口，health / 事件时间轴保持实时
watch(hours, () => {
  loadMetrics()
  loadAiMetrics()
  loadSecurityMetrics()
})

watch(autoRefresh, startTimer)

onMounted(() => {
  loadAll()
  startTimer()
})

onUnmounted(stopTimer)
</script>

<template>
  <div class="monitor-page">
    <header class="mon-head">
      <div class="mon-title-box">
        <h2 class="mon-title">系统监测</h2>
        <p class="mon-desc">
          可用性、AI 链路、安全审计、运维资源四组诊断指标。显示 — 的行是尚未接入的指标，底部标注了缺口。
        </p>
      </div>
      <div class="mon-live">
        <span class="live-badge" :class="{ paused: !autoRefresh }">
          <i class="live-dot"></i>{{ autoRefresh ? '30 秒刷新' : '已暂停' }}
        </span>
        <div class="range-switch" role="group" aria-label="监控时间范围">
          <button class="range-btn" :class="{ active: hours === 6 }" @click="hours = 6">6 小时</button>
          <button class="range-btn" :class="{ active: hours === 24 }" @click="hours = 24">24 小时</button>
        </div>
        <el-switch v-model="autoRefresh" size="small" active-text="自动" />
        <el-button size="small" text @click="loadAll">刷新</el-button>
        <span v-if="lastUpdated !== '—'" class="mon-checked">更新于 {{ lastUpdated }}</span>
      </div>
    </header>

    <section class="scoreboard">
      <router-link
        v-for="item in scoreboard"
        :key="item.key"
        :to="item.link"
        class="panel score-item"
        :class="item.status"
      >
        <div class="score-top">
          <span class="score-label">{{ item.label }}</span>
          <i class="score-led" :class="item.status"></i>
        </div>
        <div class="score-value">{{ item.value }}</div>
        <div class="score-unit">{{ item.unit }}</div>
        <div class="score-foot">{{ item.note }}</div>
      </router-link>
    </section>

    <section class="panel perf-panel">
      <div class="panel-head">
        <span class="panel-title">性能概览 · 近 {{ hours }} 小时</span>
        <span class="panel-note">数据来自 api_request_metric / ai_call_log / security_event</span>
      </div>

      <div class="perf-grid">
        <div class="perf-facts">
          <div
            v-for="fact in performanceFacts"
            :key="fact.key"
            class="perf-fact"
            :class="fact.status"
          >
            <span class="pf-label">{{ fact.label }}</span>
            <b class="pf-value">{{ fact.value }}</b>
          </div>
        </div>

        <div class="perf-donut">
          <v-chart v-if="statusTotal > 0" class="donut-chart" :option="statusOption" autoresize />
          <AdminEmptyState v-else title="暂无请求分布" note="产生接口请求后自动统计" />
        </div>

        <div class="perf-slow">
          <div class="slow-head">慢接口 TOP</div>
          <div v-if="slowTop.length" class="slow-list">
            <el-tooltip
              v-for="row in slowTop"
              :key="row.path"
              :content="`${row.path} · ${row.count} 次 · 最大 ${row.maxMs} ms`"
              placement="top"
            >
              <div class="slow-item">
                <div class="slow-row">
                  <span class="slow-name" :title="row.path">{{ row.label }}</span>
                  <span class="slow-val">{{ row.avgMs }} ms</span>
                </div>
                <span class="slow-bar"><i :style="{ width: row.ratio + '%' }"></i></span>
              </div>
            </el-tooltip>
          </div>
          <AdminEmptyState v-else title="暂无慢接口" note="近窗口内没有可统计的接口样本" />
        </div>
      </div>
    </section>

    <section class="panel perf-trend-panel">
      <div class="panel-head">
        <span class="panel-title">响应趋势 · 近 {{ hours }} 小时</span>
        <div class="trend-tabs">
          <button class="range-btn" :class="{ active: trendTab === 'api' }" @click="trendTab = 'api'">接口</button>
          <button class="range-btn" :class="{ active: trendTab === 'ai' }" @click="trendTab = 'ai'">AI</button>
          <button class="range-btn" :class="{ active: trendTab === 'security' }" @click="trendTab = 'security'">安全</button>
          <button
            class="reset-btn"
            type="button"
            title="重置为接口视图，并恢复被图例隐藏的曲线"
            @click="resetTrend"
          >
            恢复原图
          </button>
        </div>
      </div>
      <v-chart
        v-if="trendHasData"
        ref="trendChartRef"
        class="perf-trend-chart"
        :option="trendOption"
        autoresize
      />
      <AdminEmptyState
        v-else
        title="暂无按小时数据"
        note="产生接口请求、AI 调用或安全事件后，这里会出现真实曲线。"
      />
    </section>

    <section v-loading="healthLoading" class="panel probe-panel">
      <div class="panel-head">
        <span class="panel-title">实时探活 · health 接口</span>
        <span class="probe-meta">
          <em class="dot" :class="{ warn: onlineCount < healthNodes.length }"></em>
          {{ onlineCount }} / {{ healthNodes.length }} 在线
          <span class="probe-tip">判定：连接失败＝断连，超过 800ms＝降级</span>
        </span>
      </div>

      <div v-if="healthNodes.length" class="probe-grid">
        <div v-for="n in healthNodes" :key="n.key" class="probe-item" :class="levelOf(n.status)">
          <div class="probe-item-head">
            <i class="dot" :class="levelOf(n.status)"></i>
            <span class="probe-name">{{ n.name }}</span>
            <span class="probe-status">{{ STATUS_TEXT[n.status] || n.status }}</span>
            <span class="probe-lat">
              {{ n.latencyMs && n.latencyMs > 0 ? n.latencyMs + ' ms' : '—' }}
            </span>
          </div>
          <div class="probe-metrics">
            <span v-for="m in n.metrics" :key="m.label">
              {{ m.label }} <b>{{ m.value }}</b>
            </span>
          </div>
          <div v-if="n.message" class="probe-msg">{{ n.message }}</div>
        </div>
      </div>
      <div v-else class="probe-empty">健康检查接口未返回数据</div>
    </section>

    <div class="group-grid">
      <section
        v-for="g in groups"
        :key="g.key"
        class="panel group-card"
        :class="{ focus: isActive(g.key) }"
      >
        <div class="panel-head">
          <span class="panel-title">{{ g.title }}</span>
          <span class="panel-note">{{ g.note }}</span>
        </div>

        <div v-if="hasLive(g)" class="rows">
          <div v-for="row in g.rows" :key="row.label" class="row" :class="{ ghost: !row.live }">
            <span class="row-led" :class="row.live ? statusOf(row) : 'ghost'"></span>
            <span class="row-label">{{ row.label }}</span>
            <span class="row-value" :class="[row.live ? statusOf(row) : 'ghost']">
              {{ row.live ? row.value : '待采样' }}
            </span>
          </div>
        </div>
        <div v-else class="rows-empty">
          <span class="empty-badge">埋点已上线</span>
          <p class="empty-text">
            该组数据源已接入，暂无样本。产生真实调用或访问后，指标会自动出现在这里。
          </p>
        </div>

        <div class="group-foot">
          <span class="gap">{{ g.gap }}</span>
          <router-link v-if="g.drill" :to="g.drill.link" class="drill">
            {{ g.drill.text }}
          </router-link>
        </div>
      </section>
    </div>

    <section v-loading="eventLoading" class="panel events-panel">
      <div class="panel-head">
        <span class="panel-title">最近异常事件</span>
        <span class="panel-note">AI 失败 / 安全事件 / 接口 5xx 合并时间线</span>
        <router-link to="/admin/logs" class="events-link">操作日志 →</router-link>
      </div>

      <div v-if="recentEvents.length" class="events">
        <div v-for="(e, i) in recentEvents" :key="i" class="event-row" :class="e.source">
          <span class="ev-time">{{ fmtTime(e.time) }}</span>
          <span class="ev-tag" :class="e.source">{{ SOURCE_TEXT[e.source] || e.source }}</span>
          <span class="ev-title">{{ e.title }}</span>
          <span class="ev-detail">{{ e.detail }}</span>
        </div>
      </div>
      <div v-else class="events-empty">
        近 24 小时没有 AI 失败、安全事件或接口 5xx 记录。
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.monitor-page {
  display: flex;
  flex-direction: column;
}

// ---------------- 顶部 ----------------
.mon-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 2px 4px 14px;
  flex-wrap: wrap;
}

.mon-title-box {
  min-width: 0;
}

.mon-title {
  margin: 0 0 5px;
  font-family: var(--adm-font-serif);
  font-size: 23px;
  font-weight: 600;
  letter-spacing: 2px;
  color: var(--adm-text);
}

.mon-desc {
  margin: 0;
  max-width: 780px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--adm-text-faint);
}

.mon-live {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 2px;
}

.live-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 11px;
  font-size: 12px;
  color: var(--adm-ok);
  background: color-mix(in srgb, var(--adm-ok) 12%, transparent);
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
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

.mon-checked {
  font-size: 12px;
  color: var(--adm-text-faint);
  font-variant-numeric: tabular-nums;
}

.live-badge.paused {
  color: var(--adm-text-muted);
  background: var(--adm-surface-muted);

  .live-dot {
    animation: none;
  }
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

.reset-btn {
  margin-left: 4px;
  padding: 2px 9px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 9px;
  background: var(--adm-surface-muted);
  color: var(--adm-text-muted);
  font-size: 11px;
  cursor: pointer;
  transition: all 0.18s ease;

  &:hover {
    color: var(--adm-primary);
    border-color: color-mix(in srgb, var(--adm-primary) 36%, var(--adm-border-soft));
  }
}

// ---------------- 性能概览 ----------------
.perf-panel,
.perf-trend-panel {
  margin-top: 14px;
}

.perf-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(190px, 0.8fr) minmax(0, 1.15fr);
  gap: 18px;
  padding: 0 18px 16px;
  align-items: stretch;

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
}

.perf-facts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
  gap: 8px;
  align-content: start;
}

.perf-fact {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 10px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 8px;
  background: var(--adm-surface-muted);

  &.warn .pf-value {
    color: var(--adm-warn);
  }
}

.pf-label {
  font-size: 11px;
  color: var(--adm-text-faint);
}

.pf-value {
  font-family: var(--adm-font-serif);
  font-size: 17px;
  font-weight: 600;
  color: var(--adm-text);
  font-variant-numeric: tabular-nums;
}

.perf-donut {
  min-height: 150px;
  display: grid;
  place-items: center;
}

.donut-chart {
  width: 100%;
  height: 160px;
}

.perf-slow {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 150px;
}

.slow-head {
  font-size: 11px;
  letter-spacing: 0.5px;
  color: var(--adm-text-faint);
}

.slow-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.slow-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.slow-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  font-size: 11.5px;
}

.slow-name {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  color: var(--adm-text-2);
}

.slow-bar {
  width: 100%;
  height: 5px;
  border-radius: 3px;
  background: var(--adm-surface-muted);
  overflow: hidden;

  i {
    display: block;
    height: 100%;
    border-radius: 3px;
    background: linear-gradient(
      90deg,
      color-mix(in srgb, var(--adm-primary) 55%, transparent),
      var(--adm-primary)
    );
  }
}

.slow-val {
  flex-shrink: 0;
  color: var(--adm-text);
  font-variant-numeric: tabular-nums;
}

.perf-trend-chart {
  width: 100%;
  height: 252px;
  padding: 0 14px 12px;
}

.trend-tabs {
  display: flex;
  gap: 4px;
  margin-left: auto;
}

// ---------------- 通用面板 ----------------
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
  color: var(--adm-text-faint);
}

// ---------------- 计分板 ----------------
.scoreboard {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  @media (max-width: 700px) {
    grid-template-columns: 1fr;
  }
}

.score-item {
  display: flex;
  flex-direction: column;
  padding: 13px 16px 12px;
  text-decoration: none;
  background-image: linear-gradient(
    135deg,
    color-mix(in srgb, var(--adm-primary) 6%, transparent),
    transparent 58%
  );

  * {
    text-decoration: none;
  }

  &:hover {
    border-color: var(--adm-border);
    box-shadow: var(--adm-shadow);
  }

  &.warn .score-led {
    background: var(--adm-warn);
    box-shadow: 0 0 8px color-mix(in srgb, var(--adm-warn) 50%, transparent);
  }

  &.down .score-led {
    background: var(--adm-down);
    box-shadow: 0 0 8px color-mix(in srgb, var(--adm-down) 50%, transparent);
  }
}

.score-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.score-label {
  font-size: 12px;
  color: var(--adm-text-muted);
  letter-spacing: 0.5px;
}

.score-led {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--adm-ok);
}

.score-value {
  margin: 6px 0 1px;
  font-family: var(--adm-font-serif);
  font-size: 32px;
  font-weight: 600;
  line-height: 1.1;
  color: var(--adm-text);
  font-variant-numeric: tabular-nums;
}

.score-unit {
  font-size: 11px;
  color: var(--adm-text-faint);
}

.score-foot {
  margin-top: 8px;
  padding-top: 7px;
  border-top: 1px dashed var(--adm-border-soft);
  font-size: 11px;
  color: var(--adm-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// ---------------- 探活 ----------------
.probe-panel {
  margin-top: 14px;

  :deep(.el-loading-mask) {
    border-radius: 10px;
  }
}

.probe-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  font-size: 11px;
  color: var(--adm-ok);

  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--adm-ok);

    &.warn {
      background: var(--adm-warn);
    }
  }
}

.probe-tip {
  margin-left: 6px;
  padding-left: 8px;
  border-left: 1px solid var(--adm-border-soft);
  color: var(--adm-text-faint);
}

.probe-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  padding: 0 16px 14px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  @media (max-width: 700px) {
    grid-template-columns: 1fr;
  }
}

.probe-item {
  position: relative;
  border: 1px solid var(--adm-border-soft);
  border-left: 2px solid var(--adm-ok);
  border-radius: 7px;
  padding: 9px 11px 8px;
  min-width: 0;
  background: var(--adm-surface-muted);

  &.warn {
    border-left-color: var(--adm-warn);
  }

  &.down {
    border-left-color: var(--adm-down);
  }
}

.probe-item-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.probe-name {
  font-size: 13px;
  color: var(--adm-text);
  font-weight: 600;
}

.probe-status {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 8px;
  color: var(--adm-ok);
  background: color-mix(in srgb, var(--adm-ok) 12%, transparent);

  .warn & {
    color: var(--adm-warn);
    background: color-mix(in srgb, var(--adm-warn) 12%, transparent);
  }

  .down & {
    color: var(--adm-down);
    background: color-mix(in srgb, var(--adm-down) 12%, transparent);
  }
}

.probe-lat {
  margin-left: auto;
  font-size: 11px;
  color: var(--adm-text-muted);
  font-variant-numeric: tabular-nums;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--adm-ok);
  flex-shrink: 0;

  &.warn {
    background: var(--adm-warn);
  }

  &.down {
    background: var(--adm-down);
  }
}

.probe-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 3px 10px;
  font-size: 11px;
  color: var(--adm-text-muted);

  b {
    color: var(--adm-text);
    font-weight: 500;
    font-variant-numeric: tabular-nums;
  }
}

.probe-msg {
  margin-top: 4px;
  font-size: 11px;
  color: var(--adm-warn);
}

.probe-empty {
  padding: 0 18px 16px;
  font-size: 12px;
  color: var(--adm-text-faint);
}

// ---------------- 四组诊断 ----------------
.group-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;

  @media (max-width: 1000px) {
    grid-template-columns: 1fr;
  }
}

.group-card {
  display: flex;
  flex-direction: column;

  &.focus {
    border-color: var(--adm-primary);
    box-shadow: 0 0 0 1px color-mix(in srgb, var(--adm-primary) 22%, transparent);
  }
}

.rows {
  display: grid;
  grid-template-columns: 1fr;
  padding: 0 16px;
  flex: 1;
}

.rows-empty {
  padding: 18px 16px 14px;
  text-align: center;
  flex: 1;
}

.empty-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--adm-primary) 12%, transparent);
  color: var(--adm-primary);
  font-size: 11px;
  margin-bottom: 8px;
}

.empty-text {
  margin: 0 auto;
  max-width: 320px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--adm-text-faint);
}

.row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
  border-bottom: 1px dashed var(--adm-border-soft);
  font-size: 12px;

  &:last-child {
    border-bottom: none;
  }
}

.row-led {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--adm-ok);
  flex-shrink: 0;

  &.warn {
    background: var(--adm-warn);
  }

  &.down {
    background: var(--adm-down);
  }

  &.ghost {
    background: var(--adm-border);
  }
}

.row-label {
  flex: 1;
  min-width: 0;
  color: var(--adm-text-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-value {
  color: var(--adm-text);
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;

  &.warn {
    color: var(--adm-warn);
  }

  &.down {
    color: var(--adm-down);
  }

  &.ghost {
    font-weight: 400;
    color: var(--adm-text-faint);
  }
}

.group-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 0 16px;
  padding: 8px 0 12px;
  border-top: 1px solid var(--adm-border-soft);
  font-size: 11px;
}

.gap {
  color: var(--adm-text-faint);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drill {
  color: var(--adm-primary);
  text-decoration: none;
  white-space: nowrap;
  flex-shrink: 0;

  &:hover {
    opacity: 0.8;
  }
}

// ---------------- 事件时间线 ----------------
.events-panel {
  margin-top: 14px;
}

.events-link {
  margin-left: auto;
  font-size: 11px;
  color: var(--adm-primary);
  text-decoration: none;
  flex-shrink: 0;
}

.events {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 0 18px 8px 30px;

  &::before {
    content: '';
    position: absolute;
    left: 20px;
    top: 4px;
    bottom: 12px;
    width: 1px;
    background: var(--adm-border-soft);
  }
}

.event-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 7px 0;
  font-size: 12px;

  &::before {
    content: '';
    position: absolute;
    left: -13px;
    top: 50%;
    width: 5px;
    height: 5px;
    border-radius: 50%;
    transform: translateY(-50%);
    background: var(--adm-text-faint);
  }

  &.ai::before {
    background: var(--adm-chart-2);
  }

  &.security::before {
    background: var(--adm-down);
  }

  &.api::before {
    background: var(--adm-primary);
  }
}

.ev-time {
  width: 84px;
  color: var(--adm-text-faint);
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

.ev-tag {
  padding: 1px 7px;
  border-radius: 3px;
  font-size: 11px;
  flex-shrink: 0;

  &.ai {
    background: color-mix(in srgb, var(--adm-chart-2) 12%, transparent);
    color: var(--adm-chart-2);
  }

  &.security {
    background: color-mix(in srgb, var(--adm-warn) 12%, transparent);
    color: var(--adm-down);
  }

  &.api {
    background: color-mix(in srgb, var(--adm-chart-1) 12%, transparent);
    color: var(--adm-primary);
  }
}

.ev-title {
  color: var(--adm-text);
  flex-shrink: 0;
  min-width: 130px;
}

.ev-detail {
  color: var(--adm-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.events-empty {
  padding: 16px 0 18px;
  text-align: center;
  font-size: 12px;
  color: var(--adm-text-faint);
}
</style>
