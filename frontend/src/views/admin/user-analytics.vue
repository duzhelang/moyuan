<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart, HeatmapChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
  VisualMapComponent
} from 'echarts/components'
import {
  getAdminStatsTrend,
  getSearchMetrics,
  getUserHeatmap,
  getUserStructure,
  getVisitTrend
} from '@/api/modules/admin'
import type { SearchMetrics, UserHeatmap, UserStructure } from '@/api/modules/admin'
import UserTrendPanel from '@/components/admin/UserTrendPanel.vue'
import MetricCard from '@/components/admin/MetricCard.vue'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminDemoToggle from '@/components/admin/AdminDemoToggle.vue'
import { useAdminDemo } from '@/composables/useAdminDemo'
import {
  buildDemoUserHeatmap,
  demoSearchMetrics,
  demoUserCards,
  demoUserStatsTrend,
  demoUserStructure,
  demoUserVisitTrend
} from '@/configs/admin/demoData'
import { readChartTheme, useAdminTheme } from '@/composables/useAdminTheme'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  HeatmapChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  VisualMapComponent
])

const { resolvedTheme } = useAdminTheme()
const { demoMode } = useAdminDemo()
const chartTheme = computed(() => {
  void resolvedTheme.value
  return readChartTheme()
})

const loading = ref(true)
const autoRefresh = ref(true)
const lastUpdated = ref('—')
const dates = ref<string[]>([])
const visits = ref<number[]>([])
const uniqueVisitors = ref<number[]>([])
const users = ref<number[]>([])
const posts = ref<number[]>([])
const search = ref<SearchMetrics | null>(null)
const structure = ref<UserStructure | null>(null)
const heatmap = ref<UserHeatmap | null>(null)

/**
 * 演示数据视图：只替换展示层，不覆盖接口请求结果。
 * 关闭演示开关后仍用 ref 里的真实数据，避免来回切换时重新拉取。
 */
const demoHeatmap = buildDemoUserHeatmap()

const displayStructure = computed(() => (demoMode.value ? demoUserStructure : structure.value))
const displaySearch = computed(() => (demoMode.value ? demoSearchMetrics : search.value))
const displayHeatmap = computed(() => (demoMode.value ? demoHeatmap : heatmap.value))
const displayVisitTrend = computed(() =>
  demoMode.value
    ? demoUserVisitTrend
    : { dates: dates.value, visits: visits.value, uniqueVisitors: uniqueVisitors.value }
)
const displayStatsTrend = computed(() =>
  demoMode.value ? demoUserStatsTrend : { users: users.value, posts: posts.value }
)

const last = (arr: number[]) => (arr.length ? arr[arr.length - 1] : 0)

/** 环比：昨日为 0 时返回 null，前端不显示假箭头 */
const deltaOf = (arr: number[]): number | null => {
  if (arr.length < 2) return null
  const prev = arr[arr.length - 2]
  if (!prev) return null
  return Math.round(((arr[arr.length - 1] - prev) / prev) * 100)
}

const cards = computed(() => {
  if (demoMode.value) return demoUserCards
  const retention = structure.value?.retention7d
  return [
    {
      key: 'pv',
      label: '今日 PV',
      value: last(visits.value),
      spark: visits.value,
      tone: 0,
      delta: deltaOf(visits.value),
      note: '接口请求数'
    },
    {
      key: 'uv',
      label: '今日 UV',
      value: last(uniqueVisitors.value),
      spark: uniqueVisitors.value,
      tone: 2,
      delta: deltaOf(uniqueVisitors.value),
      note: '来源 IP 去重'
    },
    {
      key: 'new',
      label: '今日新增用户',
      value: last(users.value),
      spark: users.value,
      tone: 1,
      delta: deltaOf(users.value),
      note: '近 7 天每日新增'
    },
    {
      key: 'retention',
      label: '7 日回访率',
      value: retention != null ? retention.toFixed(1) + '%' : '—',
      spark: [] as number[],
      tone: 3,
      delta: null,
      note: structure.value ? `注册满 7 天 ${structure.value.eligible7d} 人` : '等待用户结构数据'
    }
  ]
})

/** 搜索指标：total 为 0 时视为还没积累到数据，不要显示 0% */
const searchReady = computed(
  () => !!displaySearch.value?.available && (displaySearch.value?.total || 0) > 0
)

const searchCards = computed(() => {
  const s = displaySearch.value
  if (!searchReady.value || !s) {
    return [
      { key: 'total', label: '搜索总量', value: '—' },
      { key: 'rate', label: '无结果率', value: '—' },
      { key: 'empty', label: '无结果次数', value: '—' },
      { key: 'distinct', label: '去重关键词', value: '—' }
    ]
  }
  return [
    { key: 'total', label: '搜索总量', value: String(s.total) },
    { key: 'rate', label: '无结果率', value: (s.noResultRate * 100).toFixed(1) + '%' },
    { key: 'empty', label: '无结果次数', value: String(s.noResult) },
    { key: 'distinct', label: '去重关键词', value: String(s.distinctKeywords) }
  ]
})

const trendRows = computed(() => displaySearch.value?.trend || [])

const dateLabel = (d: string) => d.slice(5)

/** 用户结构 4 个环形指标 */
const structureGauges = computed(() => {
  const s = displayStructure.value
  if (!s) return []
  const pct = (value: number, total: number) =>
    total > 0 ? Math.max(0, Math.min(100, Math.round((value / total) * 100))) : 0
  return [
    {
      key: 'active7',
      label: '近 7 日活跃',
      pct: pct(s.active7d, s.totalUsers),
      value: String(s.active7d),
      note: `占全部 ${s.totalUsers}`
    },
    {
      key: 'active30',
      label: '近 30 日活跃',
      pct: pct(s.active30d, s.totalUsers),
      value: String(s.active30d),
      note: `沉默 ${s.silent30d}`
    },
    {
      key: 'retention7',
      label: '7 日回访率',
      pct: s.retention7d,
      value: s.retention7d.toFixed(1) + '%',
      note: `注册满 7 天 ${s.eligible7d}`
    },
    {
      key: 'retention30',
      label: '30 日回访率',
      pct: s.retention30d,
      value: s.retention30d.toFixed(1) + '%',
      note: `注册满 30 天 ${s.eligible30d}`
    }
  ]
})

const WEEKDAYS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const HOURS = Array.from({ length: 24 }, (_, index) => `${index}时`)

/** 热力图峰值：给标题栏提供一句可读结论 */
const heatmapPeak = computed(() => {
  const cells = displayHeatmap.value?.cells || []
  if (!cells.length) return null
  const peak = cells.reduce((best, cell) => (cell.requests > best.requests ? cell : best), cells[0])
  return {
    label: `${WEEKDAYS[peak.weekday]} ${String(peak.hour).padStart(2, '0')}:00`,
    requests: peak.requests
  }
})

/**
 * 活跃热力图：星期 × 小时，值为访问请求数；悬停同时显示去重登录用户。
 * 色阶用「底色 → 金 → 棕 → 红」四段渐变，峰值区加数值标签，
 * 让高低活跃时段一眼可辨，而不是一片同色深浅。
 */
const heatmapOption = computed(() => {
  const theme = chartTheme.value
  const cells = displayHeatmap.value?.cells || []
  const max = Math.max(1, displayHeatmap.value?.maxRequests || 1)
  // 只有高活跃格子标数值，避免 168 个数字糊成一片
  const labelThreshold = Math.round(max * 0.7)
  return {
    tooltip: {
      ...theme.tooltip,
      trigger: 'item' as const,
      formatter: (params: { dataIndex: number }) => {
        const cell = cells[params.dataIndex]
        if (!cell) return ''
        return `${WEEKDAYS[cell.weekday]} ${cell.hour}:00<br/>请求 ${cell.requests} 次 · 登录用户 ${cell.users} 位`
      }
    },
    grid: { left: 38, right: 14, top: 10, bottom: 42, containLabel: false },
    xAxis: {
      type: 'category',
      data: HOURS,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: theme.ink.muted,
        fontSize: 10,
        interval: 1
      },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'category',
      data: WEEKDAYS,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: theme.ink.muted, fontSize: 10 },
      splitLine: { show: false }
    },
    visualMap: {
      min: 0,
      max,
      calculable: false,
      orient: 'horizontal' as const,
      left: 'center',
      bottom: 2,
      itemWidth: 12,
      itemHeight: 40,
      text: ['高', '低'],
      textStyle: { color: theme.ink.muted, fontSize: 10 },
      inRange: {
        color: [
          theme.ink.surface,
          `${theme.palette[1]}66`,
          theme.palette[1],
          theme.palette[0],
          theme.palette[3]
        ]
      }
    },
    series: [
      {
        type: 'heatmap',
        data: cells.map((cell) => [cell.hour, cell.weekday, cell.requests]),
        itemStyle: { borderRadius: 3, borderColor: theme.ink.surface, borderWidth: 1.5 },
        label: {
          show: true,
          fontSize: 9,
          fontWeight: 600,
          color: '#fff8ea',
          formatter: (params: { value: number[] }) =>
            params.value[2] >= labelThreshold ? String(params.value[2]) : ''
        },
        emphasis: {
          itemStyle: {
            borderColor: theme.palette[1],
            borderWidth: 1.5,
            shadowBlur: 10,
            shadowColor: `${theme.palette[1]}66`
          }
        }
      }
    ]
  }
})

/** 搜索趋势：柱=搜索总量，线=无结果率（右轴百分比） */
const searchOption = computed(() => {
  const theme = chartTheme.value
  const rows = trendRows.value
  if (!rows.length) return {}
  const rates = rows.map((row) =>
    row.total > 0 ? Number(((row.noResult / row.total) * 100).toFixed(1)) : 0
  )
  return {
    tooltip: theme.tooltip,
    legend: {
      ...theme.legend,
      data: ['搜索总量', '无结果率']
    },
    grid: { ...theme.grid, right: 42, top: 30 },
    xAxis: {
      type: 'category',
      data: rows.map((row) => dateLabel(row.date)),
      ...theme.categoryAxis
    },
    yAxis: [
      { type: 'value', name: '次', ...theme.valueAxis },
      {
        type: 'value',
        name: '%',
        max: 100,
        ...theme.valueAxis,
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '搜索总量',
        type: 'bar',
        data: rows.map((row) => row.total),
        barMaxWidth: 16,
        itemStyle: { color: theme.barGradient(theme.palette[0]), borderRadius: [3, 3, 0, 0] }
      },
      {
        name: '无结果率',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        showSymbol: false,
        data: rates,
        color: theme.palette[3],
        lineStyle: { width: 1.8 }
      }
    ]
  }
})

/** 用户来源：按要求只保留演示框架，数据为固定示例，卡片角标标注「演示」 */
const DEMO_USER_SOURCE = [
  { name: '直接访问', value: 52.3 },
  { name: '搜索引擎', value: 24.1 },
  { name: '外部链接', value: 15.6 },
  { name: '其他来源', value: 8.0 }
]

const sourceOption = computed(() => {
  const theme = chartTheme.value
  return {
    tooltip: { ...theme.tooltip, trigger: 'item' as const },
    legend: { ...theme.legend, top: 'middle', right: 0, orient: 'vertical' as const },
    title: {
      text: '演示',
      subtext: '来源分布',
      left: '30%',
      top: '40%',
      textAlign: 'center' as const,
      textStyle: { color: theme.ink.text, fontSize: 15, fontWeight: 600 },
      subtextStyle: { color: theme.ink.muted, fontSize: 10, lineHeight: 16 }
    },
    series: [
      {
        type: 'pie',
        radius: ['48%', '70%'],
        center: ['30%', '50%'],
        itemStyle: { borderRadius: 4, borderColor: theme.ink.surface, borderWidth: 2 },
        label: { show: false },
        labelLine: { show: false },
        data: DEMO_USER_SOURCE.map((item, index) => ({
          ...item,
          itemStyle: { color: theme.palette[index % theme.palette.length] }
        }))
      }
    ]
  }
})

const heatmapReady = computed(
  () =>
    !!displayHeatmap.value?.available &&
    (displayHeatmap.value?.cells || []).some((cell) => cell.requests > 0)
)

const load = async () => {
  loading.value = true
  try {
    const [trendRes, visitRes, searchRes, structureRes, heatmapRes] = await Promise.all([
      getAdminStatsTrend(7),
      getVisitTrend(7),
      getSearchMetrics(7),
      getUserStructure(),
      getUserHeatmap(7)
    ])
    const trend = trendRes.data
    const visit = visitRes.data
    dates.value = visit?.dates || []
    visits.value = visit?.visits || []
    uniqueVisitors.value = visit?.uniqueVisitors || []
    users.value = trend?.users || []
    posts.value = trend?.posts || []
    search.value = searchRes.data || null
    structure.value = structureRes.data || null
    heatmap.value = heatmapRes.data || null
    lastUpdated.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } catch (error) {
    console.error('获取用户分析数据失败', error)
  } finally {
    loading.value = false
  }
}

let timer: ReturnType<typeof setInterval> | undefined

const startTimer = () => {
  if (timer) clearInterval(timer)
  timer = undefined
  if (autoRefresh.value) timer = setInterval(load, 30_000)
}

watch(autoRefresh, startTimer)

onMounted(() => {
  load()
  startTimer()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div v-loading="loading" class="analytics-page">
    <div class="page-head">
      <div class="page-title-box">
        <h2 class="page-title">用户分析</h2>
        <p class="page-desc">
          访问、增长与搜索行为的近期走势。搜索数据来自 `search_log` 埋点，无结果率越高说明内容覆盖越不足。
        </p>
      </div>
      <div class="page-actions">
        <AdminDemoToggle />
        <span v-if="lastUpdated !== '—'" class="updated">更新于 {{ lastUpdated }}</span>
        <el-switch v-model="autoRefresh" size="small" active-text="自动" />
        <el-button size="small" text @click="load">刷新</el-button>
      </div>
    </div>

    <div class="kpi-grid">
      <MetricCard
        v-for="c in cards"
        :key="c.key"
        :label="c.label"
        :value="c.value"
        :spark="c.spark"
        :tone="c.tone"
        :delta="c.delta"
        :note="c.note"
        :loading="loading"
      />
    </div>

    <el-card shadow="never" class="structure-card">
      <div class="card-head">
        <div class="card-title-wrap">
          <span class="card-title">用户结构 · 活跃与回访</span>
          <span class="card-note">角色分布 / 活跃 / 回访率</span>
        </div>
        <el-tag v-if="demoMode" size="small" type="info" effect="plain">演示</el-tag>
        <el-tag v-if="displayStructure" size="small" type="info" effect="plain">
          总计 {{ displayStructure.totalUsers }} 人
        </el-tag>
      </div>

      <div v-if="displayStructure" class="structure-grid">
        <div class="role-list">
          <div v-for="role in displayStructure.roles" :key="role.key" class="role-row">
            <span class="role-label">{{ role.label }}</span>
            <span class="role-bar">
              <i :style="{ width: (role.value / Math.max(1, ...displayStructure.roles.map((r) => r.value))) * 100 + '%' }"></i>
            </span>
            <span class="role-value">{{ role.value }}</span>
          </div>
        </div>

        <div class="active-grid">
          <div v-for="gauge in structureGauges" :key="gauge.key" class="active-cell">
            <span class="active-label">{{ gauge.label }}</span>
            <div class="gauge-ring" :style="{ '--pct': gauge.pct + '%' }">
              <b class="active-value">{{ gauge.value }}</b>
            </div>
            <span class="active-sub">{{ gauge.note }}</span>
          </div>
        </div>
      </div>

      <p v-if="displayStructure" class="structure-note">
        回访率口径：注册满 N 天且最近 N 天有登录记录的用户 / 注册满 N 天的用户；近 7 天新增 {{ displayStructure.newUsers7d }} 人。
      </p>
      <div v-else class="structure-empty">用户结构指标暂不可用</div>
    </el-card>

    <div class="split-row">
      <el-card shadow="never" class="heatmap-card">
        <div class="card-head">
          <div class="card-title-wrap">
            <span class="card-title">活跃热力图 · 近 7 天</span>
            <span class="card-note">
              星期 × 小时 · 访问请求量（含游客）
              <template v-if="heatmapPeak"> · 峰值 {{ heatmapPeak.label }} {{ heatmapPeak.requests }} 次</template>
            </span>
          </div>
          <el-tag v-if="demoMode" size="small" type="info" effect="plain">演示</el-tag>
        </div>
        <v-chart v-if="heatmapReady" class="heatmap-chart" :option="heatmapOption" autoresize />
        <AdminEmptyState
          v-else
          title="暂无活跃数据"
          note="产生访问请求后，这里会自动生成星期与时段的活跃分布。"
        />
      </el-card>

      <el-card shadow="never" class="source-card">
        <div class="card-head">
          <div class="card-title-wrap">
            <span class="card-title">用户来源</span>
            <span class="card-note">直接 / 搜索 / 外链</span>
          </div>
          <el-tag v-if="demoMode" size="small" type="info" effect="plain">演示</el-tag>
        </div>
        <v-chart v-if="demoMode" class="source-chart" :option="sourceOption" autoresize />
        <AdminEmptyState
          v-else
          title="来源埋点未接入"
          note="当前没有 referrer 数据；打开顶部演示数据开关可查看图表框架。"
        />
        <p v-if="demoMode" class="source-note">
          当前为演示数据，只影响前端展示；接入 referrer 字段后再替换为真实来源。
        </p>
      </el-card>
    </div>

    <UserTrendPanel
      :dates="displayVisitTrend.dates"
      :visits="displayVisitTrend.visits"
      :unique-visitors="displayVisitTrend.uniqueVisitors"
      :users="displayStatsTrend.users"
      :posts="displayStatsTrend.posts"
    />

    <el-card shadow="never" class="search-card">
      <div class="card-head">
        <div class="card-title-wrap">
          <span class="card-title">搜索行为 · 近 7 天</span>
          <span class="card-note">搜索量 / 无结果率 / 关键词</span>
        </div>
        <el-tag v-if="demoMode" size="small" type="info" effect="plain">演示</el-tag>
        <el-tag v-else-if="!searchReady" size="small" type="info" effect="plain">暂无埋点数据</el-tag>
      </div>

      <el-row v-if="searchReady" :gutter="20" class="search-metrics">
        <el-col v-for="c in searchCards" :key="c.key" :xs="12" :sm="6">
          <div class="mini">
            <span class="mini-label">{{ c.label }}</span>
            <span class="mini-value">{{ c.value }}</span>
          </div>
        </el-col>
      </el-row>
      <div v-else class="search-empty">
        <span class="empty-badge">埋点已上线</span>
        <p class="empty-text">
          近 7 天还没有搜索记录。用户在诗词页发起搜索后，搜索量、无结果率与关键词榜单会自动出现在这里。
        </p>
      </div>

      <div v-if="trendRows.length" class="search-chart-wrap">
        <v-chart class="search-chart" :option="searchOption" autoresize />
      </div>

      <el-row v-if="searchReady" :gutter="20" class="kw-row">
        <el-col :xs="24" :sm="12">
          <div class="kw-title">
            <span>搜不到的关键词 TOP</span>
            <em v-if="displaySearch?.available === false" class="kw-hint">{{ displaySearch?.message }}</em>
          </div>
          <el-table
            :data="displaySearch?.topNoResult || []"
            size="small"
            :show-header="true"
            empty-text="暂无搜不到的关键词"
            max-height="280"
          >
            <el-table-column prop="keyword" label="关键词" min-width="120" show-overflow-tooltip />
            <el-table-column prop="count" label="次数" width="70" align="right" />
          </el-table>
        </el-col>
        <el-col :xs="24" :sm="12">
          <div class="kw-title"><span>热搜关键词 TOP</span></div>
          <el-table
            :data="displaySearch?.topKeywords || []"
            size="small"
            empty-text="暂无搜索记录"
            max-height="280"
          >
            <el-table-column prop="keyword" label="关键词" min-width="120" show-overflow-tooltip />
            <el-table-column prop="count" label="次数" width="70" align="right" />
            <el-table-column prop="avgResult" label="平均命中" width="90" align="right" />
          </el-table>
        </el-col>
      </el-row>

      <p class="search-note">
        无结果率 = 命中数为 0 的搜索次数 ÷ 搜索总次数。匿名搜索同样计入，但不带用户标识。
      </p>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.page-title-box {
  min-width: 0;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 2px;
}

.updated {
  font-size: 12px;
  color: var(--adm-text-faint);
  font-variant-numeric: tabular-nums;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 20px;

  @media (max-width: 900px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  @media (max-width: 520px) {
    grid-template-columns: 1fr;
  }
}

.split-row {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(0, 1fr);
  gap: 20px;
  margin-bottom: 20px;

  @media (max-width: 1000px) {
    grid-template-columns: 1fr;
  }
}

.heatmap-card,
.source-card {
  border-radius: 8px;
}

.heatmap-chart {
  width: 100%;
  height: 260px;
}

.source-chart {
  width: 100%;
  height: 220px;
}

.source-note {
  margin: 6px 0 0;
  font-size: 11px;
  line-height: 1.7;
  color: var(--adm-text-faint);
}

.search-chart-wrap {
  margin: 8px 0 18px;
}

.search-chart {
  width: 100%;
  height: 220px;
}

.card-title-wrap {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.card-note {
  font-size: 11px;
  color: var(--adm-text-faint);
}

.gauge-ring {
  position: relative;
  width: 58px;
  height: 58px;
  margin: 6px auto 2px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: conic-gradient(
    var(--adm-primary) var(--pct, 0%),
    var(--adm-surface) var(--pct, 0%)
  );

  &::before {
    content: '';
    position: absolute;
    inset: 5px;
    border-radius: 50%;
    background: var(--adm-surface-muted);
  }

  .active-value {
    position: relative;
    z-index: 1;
    font-size: 14px;
  }
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--adm-text);
  margin: 0 0 6px;
}

.page-desc {
  font-size: 13px;
  color: var(--adm-text-muted);
  margin: 0;
  max-width: 860px;
  line-height: 1.7;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  margin-bottom: 20px;
}

.stat-label {
  font-size: 13px;
  color: var(--adm-text-muted);
  margin: 0 0 6px;
}

.stat-value {
  font-size: 26px;
  font-weight: 600;
  color: var(--adm-text);
  margin: 0;
  font-variant-numeric: tabular-nums;
}

.stat-unit {
  font-size: 13px;
  font-weight: 400;
  color: var(--adm-text-muted);
  margin-left: 3px;
}

.search-card {
  border-radius: 8px;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--adm-text);
}

.search-metrics {
  margin-bottom: 8px;
}

.mini {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  background: var(--adm-surface-muted);
  border-radius: 6px;
  margin-bottom: 12px;
}

.search-empty {
  padding: 22px 0 18px;
  text-align: center;
  margin-bottom: 8px;
}

.empty-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--adm-chart-1) 12%, transparent);
  color: var(--adm-primary);
  font-size: 11px;
  margin-bottom: 8px;
}

.empty-text {
  margin: 0 auto;
  max-width: 460px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--adm-text-faint);
}

.mini-label {
  font-size: 12px;
  color: var(--adm-text-muted);
}

.mini-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--adm-text);
  font-variant-numeric: tabular-nums;
}

.trend-wrap {
  margin: 8px 0 20px;
}

.trend-legend {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: var(--adm-text-muted);
  margin-bottom: 8px;
}

.dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 5px;
}

.dot-total {
  background: var(--adm-primary);
}

.dot-empty {
  background: var(--adm-warn);
}

.trend-range {
  margin-left: auto;
  font-variant-numeric: tabular-nums;
}

.trend-svg {
  width: 100%;
  height: 120px;
  display: block;
}

.trend-axis {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--adm-text-faint);
  margin-top: 4px;
  font-variant-numeric: tabular-nums;
}

.kw-row {
  margin-top: 4px;
}

.kw-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--adm-text);
  margin-bottom: 8px;
}

.kw-hint {
  font-style: normal;
  font-size: 12px;
  font-weight: 400;
  color: var(--adm-warn);
}

.search-note {
  font-size: 12px;
  color: var(--adm-text-muted);
  margin: 12px 0 0;
  line-height: 1.7;
}

.structure-card {
  border-radius: 8px;
  margin-bottom: 20px;
}

.structure-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.25fr);
  gap: 24px;

  @media (max-width: 900px) {
    grid-template-columns: 1fr;
    gap: 14px;
  }
}

.role-list {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.role-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--adm-text-2);
}

.role-label {
  width: 60px;
  flex-shrink: 0;
}

.role-bar {
  flex: 1;
  height: 5px;
  border-radius: 3px;
  background: var(--adm-surface-muted);
  overflow: hidden;

  i {
    display: block;
    height: 100%;
    border-radius: 3px;
    background: var(--adm-primary);
    opacity: 0.75;
  }
}

.role-value {
  width: 44px;
  text-align: right;
  font-variant-numeric: tabular-nums;
  color: var(--adm-text);
  flex-shrink: 0;
}

.active-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.active-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 12px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 8px;
  background: var(--adm-surface-muted);
}

.active-label {
  font-size: 11px;
  color: var(--adm-text-faint);
}

.active-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--adm-text);
  font-variant-numeric: tabular-nums;
}

.active-sub {
  font-size: 10.5px;
  color: var(--adm-text-faint);
}

.structure-note {
  margin: 12px 0 0;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--adm-text-faint);
}

.structure-empty {
  padding: 18px 0;
  text-align: center;
  font-size: 12px;
  color: var(--adm-text-faint);
}
</style>
