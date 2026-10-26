<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import experiment from '@/data/recommend-experiment.json'
import { MODEL_STRATEGY_LABELS, recommendApi, type RecommendLiveStats } from '@/api/modules/recommend'

use([CanvasRenderer, LineChart, BarChart, GridComponent, LegendComponent, TooltipComponent])

interface Strategy {
  key: string
  label: string
  shortLabel: string
  group: string
  recall: number
  ndcg: number
  precision: number
  hr: number
  coverage: number
}

interface Cluster {
  key: string
  label: string
  users: number
  targetInClassicalPool: number
  recall: Record<string, number>
}

const strategies = experiment.strategies as Strategy[]
const clusters = experiment.clusters as Cluster[]

const GOLD = '#b9791f'
const GOLD_SOFT = 'rgba(178, 138, 62, 0.45)'
const COPPER = '#a05a2c'
const COPPER_SOFT = 'rgba(160, 90, 44, 0.4)'
const CLUSTER_A = '#cfa14d'
const CLUSTER_B = '#a9714f'

const panelRef = ref<HTMLElement | null>(null)
const isFullscreen = ref(false)

const kpis = [
  { label: '训练事件', value: experiment.fingerprint.trainEvents },
  { label: '评估用户', value: experiment.fingerprint.evalUsers },
  { label: '测试物品对', value: experiment.fingerprint.testPairs },
  { label: '实验物品', value: experiment.fingerprint.items },
  { label: '古典候选池', value: experiment.fingerprint.classicalItems }
]

// 头部一句话结论：最优策略相对全局基线的 Recall 提升
const bestStrategy = strategies.find(s => s.group === 'best')
const baselineStrategy = strategies.find(s => s.key === 'global')
const headSub = bestStrategy && baselineStrategy
  ? `${bestStrategy.shortLabel}最优 · Recall@10 ${bestStrategy.recall.toFixed(3)} · 全局基线的 ${(bestStrategy.recall / baselineStrategy.recall).toFixed(2)}×`
  : ''

// 实时统计（缓存行/今日切换事件）：30 秒轮询，失败显示占位
const LIVE_REFRESH_MS = 30_000
const liveStats = ref<RecommendLiveStats | null>(null)
let liveTimer: ReturnType<typeof setInterval> | undefined

const loadLiveStats = async () => {
  try {
    const res = await recommendApi.liveStats()
    liveStats.value = res.data
  } catch {
    liveStats.value = null
  }
}

interface MetaItem {
  label: string
  value: string
  live?: boolean
  title?: string
}

const fmtCount = (value: number) => value.toLocaleString('zh-CN')

const metaItems = computed<MetaItem[]>(() => {
  const items: MetaItem[] = kpis.map(k => ({ label: k.label, value: fmtCount(k.value) }))
  const live = liveStats.value
  items.push({
    label: '缓存推荐行',
    value: live ? fmtCount(live.cacheRows) : '—',
    live: Boolean(live),
    title: live
      ? `覆盖 ${fmtCount(live.cacheUsers)} 位用户 · 每 30 秒自动刷新`
      : '实时数据不可用（接口失败或未登录管理员）'
  })
  items.push({
    label: '今日切换事件',
    value: live ? fmtCount(live.eventToday) : '—',
    live: Boolean(live),
    title: live
      ? `累计 ${fmtCount(live.eventTotal)} 次${live.lastEventTime ? ` · 最近 ${live.lastEventTime.slice(11, 16)}` : ''}`
      : '实时数据不可用（接口失败或未登录管理员）'
  })
  return items
})

// 表格行 ↔ 图表柱双向联动：悬停任一方，其余策略同步淡出
const activeKey = ref<string | null>(null)
let outTimer: ReturnType<typeof setTimeout> | undefined

const onChartOver = (params: { componentType?: string; dataIndex?: number }) => {
  window.clearTimeout(outTimer)
  if (params?.componentType !== 'series') return
  activeKey.value = strategies[params.dataIndex ?? -1]?.key ?? null
}

const onChartOut = () => {
  window.clearTimeout(outTimer)
  outTimer = setTimeout(() => {
    activeKey.value = null
  }, 80)
}

const fmt = (value: number) => value.toFixed(4)

const axisMax = (values: number[]) => Math.ceil((Math.max(...values) * 1.18) / 0.05) * 0.05

const mainValues = strategies.flatMap(s => [s.recall, s.ndcg])
const clusterValues = clusters.flatMap(c => Object.values(c.recall))

const tooltipStyle = {
  backgroundColor: 'rgba(255, 252, 244, 0.97)',
  borderColor: 'rgba(150, 105, 50, 0.45)',
  borderWidth: 1,
  padding: [8, 12],
  textStyle: { color: '#5a3d24', fontSize: 12 }
}

const mainChartOption = computed(() => ({
  backgroundColor: 'transparent',
  animationDuration: 720,
  grid: { left: 4, right: 54, top: 26, bottom: 2, containLabel: true },
  legend: {
    right: 0,
    top: 0,
    itemWidth: 10,
    itemHeight: 10,
    itemGap: 18,
    textStyle: { color: '#8a6a45', fontSize: 11 }
  },
  tooltip: {
    ...tooltipStyle,
    trigger: 'axis',
    axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(150, 105, 50, 0.08)' } },
    valueFormatter: (value: number) => Number(value).toFixed(4)
  },
  xAxis: {
    type: 'value',
    max: axisMax(mainValues),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: {
      color: 'rgba(90, 61, 36, 0.55)',
      fontSize: 10,
      formatter: (value: number) => value.toFixed(2)
    },
    splitLine: { lineStyle: { color: 'rgba(150, 105, 50, 0.15)' } }
  },
  yAxis: {
    type: 'category',
    inverse: true,
    data: strategies.map(s => s.label),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#5a3d24', fontSize: 12 }
  },
  series: [
    {
      name: 'Recall@10',
      type: 'bar',
      barWidth: 9,
      barGap: '35%',
      itemStyle: { borderRadius: [0, 4, 4, 0] },
      data: strategies.map(s => ({
        value: s.recall,
        itemStyle: {
          color: s.group === 'best' ? GOLD : GOLD_SOFT,
          opacity: activeKey.value && s.key !== activeKey.value ? 0.3 : 1
        }
      })),
      label: {
        show: true,
        position: 'right',
        distance: 6,
        color: '#9a6b2f',
        fontSize: 10,
        formatter: (params: { value: number }) => Number(params.value).toFixed(3)
      }
    },
    {
      name: 'NDCG@10',
      type: 'bar',
      barWidth: 9,
      itemStyle: { borderRadius: [0, 4, 4, 0] },
      data: strategies.map(s => ({
        value: s.ndcg,
        itemStyle: {
          color: s.group === 'best' ? COPPER : COPPER_SOFT,
          opacity: activeKey.value && s.key !== activeKey.value ? 0.3 : 1
        }
      })),
      label: {
        show: true,
        position: 'right',
        distance: 6,
        color: '#8a5a2b',
        fontSize: 10,
        formatter: (params: { value: number }) => Number(params.value).toFixed(3)
      }
    }
  ]
}))

const clusterChartOption = computed(() => ({
  backgroundColor: 'transparent',
  animationDuration: 720,
  grid: { left: 4, right: 20, top: 26, bottom: 2, containLabel: true },
  legend: {
    right: 0,
    top: 0,
    itemWidth: 10,
    itemHeight: 10,
    itemGap: 16,
    textStyle: { color: '#8a6a45', fontSize: 11 }
  },
  tooltip: {
    ...tooltipStyle,
    trigger: 'axis',
    axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(150, 105, 50, 0.08)' } },
    valueFormatter: (value: number) => Number(value).toFixed(4)
  },
  xAxis: {
    type: 'value',
    max: axisMax(clusterValues),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: {
      color: 'rgba(90, 61, 36, 0.55)',
      fontSize: 10,
      formatter: (value: number) => value.toFixed(2)
    },
    splitLine: { lineStyle: { color: 'rgba(150, 105, 50, 0.15)' } }
  },
  yAxis: {
    type: 'category',
    inverse: true,
    data: strategies.map(s => s.shortLabel),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#6b5236', fontSize: 11 }
  },
  series: clusters.map((cluster, index) => ({
    name: cluster.label,
    type: 'bar',
    barWidth: 7,
    itemStyle: { color: index === 0 ? CLUSTER_A : CLUSTER_B, borderRadius: [0, 3, 3, 0] },
    data: strategies.map(s => ({
      value: cluster.recall[s.key] ?? 0,
      itemStyle: { opacity: activeKey.value && s.key !== activeKey.value ? 0.22 : 1 }
    }))
  }))
}))

const STRATEGY_NAME: Record<string, string> = {
  rule: '规则',
  era: '古今',
  genre: '体裁',
  fusion: '融合',
  global: '全局'
}

const strategyName = (key: string | null) => (key ? STRATEGY_NAME[key] ?? key : '—')

const liveTrend = computed(() => liveStats.value?.eventTrend ?? [])
const liveByStrategy = computed(() => liveStats.value?.eventByStrategy ?? [])

const pageSummary = computed(() => {
  const pages = liveStats.value?.eventByPage ?? []
  if (!pages.length) return '暂无'
  return pages.slice(0, 2).map(p => `${p.name} ${p.count}`).join(' · ')
})

const formatEventTime = (value: string) =>
  value ? value.slice(5, 16).replace('T', ' ') : '—'

// 近 7 天切换趋势：无事件时仍显示完整日期轴，图表下方给空态说明
const liveTrendOption = computed(() => ({
  backgroundColor: 'transparent',
  animationDuration: 520,
  grid: { left: 6, right: 16, top: 22, bottom: 2, containLabel: true },
  tooltip: {
    ...tooltipStyle,
    trigger: 'axis',
    valueFormatter: (value: number) => `${value} 次`
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: liveTrend.value.map(item => item.date),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: 'rgba(90, 61, 36, 0.55)', fontSize: 10 }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: 'rgba(90, 61, 36, 0.55)', fontSize: 10 },
    splitLine: { lineStyle: { color: 'rgba(150, 105, 50, 0.15)' } }
  },
  series: [
    {
      name: '切换次数',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      data: liveTrend.value.map(item => item.count),
      lineStyle: { width: 2, color: GOLD },
      itemStyle: { color: GOLD },
      areaStyle: { color: 'rgba(201, 164, 92, 0.18)' }
    }
  ]
}))

// 累计切换去向：横向条形图，按次数降序
const liveStrategyOption = computed(() => ({
  backgroundColor: 'transparent',
  animationDuration: 520,
  grid: { left: 4, right: 44, top: 12, bottom: 2, containLabel: true },
  tooltip: {
    ...tooltipStyle,
    trigger: 'axis',
    axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(150, 105, 50, 0.08)' } },
    valueFormatter: (value: number) => `${value} 次`
  },
  xAxis: {
    type: 'value',
    minInterval: 1,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: 'rgba(90, 61, 36, 0.55)', fontSize: 10 },
    splitLine: { lineStyle: { color: 'rgba(150, 105, 50, 0.15)' } }
  },
  yAxis: {
    type: 'category',
    inverse: true,
    data: liveByStrategy.value.map(item => strategyName(item.name)),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#5a3d24', fontSize: 12 }
  },
  series: [
    {
      name: '切换次数',
      type: 'bar',
      barWidth: 10,
      itemStyle: { color: GOLD, borderRadius: [0, 4, 4, 0] },
      label: {
        show: true,
        position: 'right',
        distance: 6,
        color: '#9a6b2f',
        fontSize: 10,
        formatter: (params: { value: number }) => `${params.value}`
      },
      data: liveByStrategy.value.map(item => item.count)
    }
  ]
}))

const syncFullscreen = () => {
  isFullscreen.value = Boolean(document.fullscreenElement)
}

const toggleFullscreen = async () => {
  if (!panelRef.value) return
  try {
    if (document.fullscreenElement) {
      await document.exitFullscreen()
    } else {
      await panelRef.value.requestFullscreen()
    }
  } catch (error) {
    console.error('切换全屏失败', error)
  }
  // 全屏只改变元素尺寸，主动派发 resize 让图表重新计算宽高
  window.setTimeout(() => window.dispatchEvent(new Event('resize')), 120)
}

onMounted(() => {
  document.addEventListener('fullscreenchange', syncFullscreen)
  loadLiveStats()
  liveTimer = setInterval(loadLiveStats, LIVE_REFRESH_MS)
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', syncFullscreen)
  if (liveTimer) clearInterval(liveTimer)
  window.clearTimeout(outTimer)
})
</script>

<template>
  <section ref="panelRef" class="data-screen">
    <span class="screen-glow" aria-hidden="true"></span>

    <header class="screen-head">
      <div class="head-main">
        <p class="head-eyebrow">推荐实验 · 离线评估</p>
        <h3 class="head-title">多视角推荐 · 指标全景</h3>
        <p class="head-sub">{{ headSub }}</p>
      </div>
      <div class="head-side">
        <div class="head-meta">
          <span>数据截至 {{ experiment.updatedAt }}</span>
          <i class="meta-dot"></i>
          <span>{{ experiment.modelVersion }}</span>
        </div>
        <button class="fs-btn" type="button" @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon>
          <span>{{ isFullscreen ? '退出全屏' : '全屏演示' }}</span>
        </button>
      </div>
    </header>

    <div class="screen-meta">
      <span
        v-for="item in metaItems"
        :key="item.label"
        class="meta-item"
        :class="{ 'meta-live': item.live }"
        :title="item.title"
      >
        <i v-if="item.live" class="live-dot" aria-hidden="true"></i><b>{{ item.value }}</b>{{ item.label }}
      </span>
    </div>

    <div class="screen-grid">
      <div class="pane pane-main">
        <div class="pane-head">
          <span class="pane-title">四策略主指标</span>
          <span class="pane-note">全量 {{ experiment.fingerprint.items }} 候选池 · 宏平均</span>
        </div>
        <v-chart
          class="chart chart-main"
          :option="mainChartOption"
          autoresize
          @mouseover="onChartOver"
          @mouseout="onChartOut"
        />
      </div>
      <div class="pane">
        <div class="pane-head">
          <span class="pane-title">分簇 Recall@10</span>
          <span class="pane-note">{{ clusters[0].users }} 人 / 簇</span>
        </div>
        <v-chart
          class="chart chart-cluster"
          :option="clusterChartOption"
          autoresize
          @mouseover="onChartOver"
          @mouseout="onChartOut"
        />
        <p class="cluster-foot">
          古典池目标覆盖：{{ clusters[0].label }}
          {{ (clusters[0].targetInClassicalPool * 100).toFixed(1) }}% ·
          {{ clusters[1].label }}
          {{ (clusters[1].targetInClassicalPool * 100).toFixed(1) }}%
        </p>
      </div>
    </div>

    <div class="screen-grid screen-grid-lower">
      <div class="pane">
        <div class="pane-head">
          <span class="pane-title">完整指标</span>
          <span class="pane-note">Recall / NDCG / Precision / HR / 覆盖率</span>
        </div>
        <div class="metric-table">
          <div class="mt-row mt-head">
            <span class="mt-name">策略</span>
            <span>Recall</span>
            <span>NDCG</span>
            <span>Precision</span>
            <span>HR@10</span>
            <span>覆盖率</span>
          </div>
          <div
            v-for="s in strategies"
            :key="s.key"
            class="mt-row"
            :class="{ 'mt-best': s.group === 'best', 'mt-active': s.key === activeKey }"
          >
            <span class="mt-name">
              {{ s.shortLabel }}
              <i v-if="s.group === 'best'" class="best-tag">最优</i>
            </span>
            <span>{{ fmt(s.recall) }}</span>
            <span>{{ fmt(s.ndcg) }}</span>
            <span>{{ fmt(s.precision) }}</span>
            <span>{{ fmt(s.hr) }}</span>
            <span>{{ fmt(s.coverage) }}</span>
          </div>
        </div>
        <p class="metric-foot">
          口径：排除训练已交互物品；测试集 {{ experiment.fingerprint.testPairs }} 对 ·
          验证集 {{ experiment.fingerprint.validPairs }} 对 · 权重 1 / 3 / 5（同物品取最大）
        </p>
      </div>

      <div class="pane">
        <div class="pane-head">
          <span class="pane-title">结论与口径</span>
          <span class="pane-note">α {{ experiment.params.alpha }} · Top-{{ experiment.params.kSim }} 邻居</span>
        </div>
        <ul class="insight-list">
          <li v-for="insight in experiment.insights" :key="insight.title" class="insight">
            <div class="insight-top">
              <span class="insight-title">{{ insight.title }}</span>
              <span class="insight-value">{{ insight.value }}</span>
            </div>
            <p class="insight-desc">{{ insight.desc }}</p>
          </li>
        </ul>
        <div class="chips">
          <span class="chip">α = {{ experiment.params.alpha }}</span>
          <span class="chip">Top-{{ experiment.params.kSim }} 邻居</span>
          <span class="chip">权重 1 / 3 / 5</span>
          <span class="chip">缓存 {{ experiment.cache.rows.toLocaleString('zh-CN') }} 行</span>
        </div>
      </div>
    </div>

    <div class="screen-grid screen-grid-live">
      <div class="pane">
        <div class="pane-head">
          <span class="pane-title">近 7 天策略切换</span>
          <span class="pane-note">30 秒自动刷新 · 今日 {{ liveStats?.eventToday ?? 0 }} 次</span>
        </div>
        <v-chart class="chart chart-live" :option="liveTrendOption" autoresize />
        <p v-if="!liveStats?.eventTotal" class="live-empty">暂无切换事件，前台切换推荐策略后自动累计</p>
      </div>
      <div class="pane">
        <div class="pane-head">
          <span class="pane-title">累计切换去向</span>
          <span class="pane-note">来源页面 {{ pageSummary }}</span>
        </div>
        <v-chart class="chart chart-live" :option="liveStrategyOption" autoresize />
        <p v-if="!liveStats?.eventTotal" class="live-empty">暂无数据</p>
      </div>
    </div>

    <div class="screen-grid screen-grid-live">
      <div class="pane">
        <div class="pane-head">
          <span class="pane-title">模型与缓存状态</span>
          <span class="pane-note">
            古典池 {{ liveStats?.poolSize ?? '—' }} 首 · 缓存 {{ fmtCount(liveStats?.cacheRows ?? 0) }} 行
          </span>
        </div>
        <div class="metric-table live-table">
          <div class="mt-row lm-head">
            <span class="mt-name">策略</span>
            <span>版本</span>
            <span>用户</span>
            <span>行数</span>
            <span>NDCG@10</span>
            <span>状态</span>
          </div>
          <div v-for="m in (liveStats?.models ?? [])" :key="m.strategy" class="mt-row lm-row">
            <span class="mt-name">{{ MODEL_STRATEGY_LABELS[m.strategy] || m.strategy }}</span>
            <span>{{ m.version }}</span>
            <span>{{ fmtCount(m.cacheUsers) }}</span>
            <span>{{ fmtCount(m.cacheRows) }}</span>
            <span>{{ m.metrics?.ndcg10 != null ? m.metrics.ndcg10.toFixed(4) : '—' }}</span>
            <span>{{ m.status === 1 ? '上线' : '下线' }}</span>
          </div>
          <p v-if="!liveStats?.models?.length" class="live-empty">暂无上线模型</p>
        </div>
      </div>
      <div class="pane">
        <div class="pane-head">
          <span class="pane-title">最近切换事件</span>
          <span class="pane-note">
            覆盖 {{ fmtCount(liveStats?.cacheUsers ?? 0) }} 位用户 · 累计 {{ fmtCount(liveStats?.eventTotal ?? 0) }} 次
          </span>
        </div>
        <div class="event-list">
          <div v-for="(e, idx) in (liveStats?.recentEvents ?? [])" :key="idx" class="event-row">
            <span class="ev-time">{{ formatEventTime(e.createTime) }}</span>
            <span class="ev-user">用户 {{ e.userId }}</span>
            <span class="ev-flow">{{ strategyName(e.fromStrategy) }} → {{ strategyName(e.toStrategy) }}</span>
            <span class="ev-page">{{ e.page }}</span>
          </div>
          <p v-if="!liveStats?.recentEvents?.length" class="live-empty">暂无事件</p>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.data-screen {
  position: relative;
  margin-top: 8px;
  margin-bottom: 20px;
  padding: 22px 24px 20px;
  border-radius: 14px;
  border: 1px solid rgba(150, 105, 50, 0.28);
  background:
    radial-gradient(1100px 380px at 12% -12%, rgba(201, 164, 92, 0.18), transparent 62%),
    radial-gradient(900px 460px at 102% 4%, rgba(178, 118, 62, 0.1), transparent 58%),
    linear-gradient(158deg, #fdfaf3 0%, #f8efdd 55%, #f2e4c9 100%);
  box-shadow:
    inset 0 0 60px rgba(255, 255, 255, 0.4),
    0 10px 26px rgba(120, 85, 40, 0.14);
  overflow: hidden;

  &::before,
  &::after {
    content: '';
    position: absolute;
    width: 26px;
    height: 26px;
    border-color: rgba(150, 105, 50, 0.45);
    border-style: solid;
    pointer-events: none;
  }

  &::before {
    top: 10px;
    left: 10px;
    border-width: 1px 0 0 1px;
  }

  &::after {
    right: 10px;
    bottom: 10px;
    border-width: 0 1px 1px 0;
  }

  &:fullscreen {
    width: 100vw;
    height: 100vh;
    padding: 26px 30px;
    border-radius: 0;
    overflow: auto;
  }
}

.screen-glow {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;

  &::before {
    content: '';
    position: absolute;
    top: -40%;
    left: -22%;
    width: 58%;
    height: 180%;
    background: radial-gradient(closest-side, rgba(212, 162, 84, 0.12), transparent 70%);
    animation: screen-drift 16s ease-in-out infinite;
  }
}

@keyframes screen-drift {
  0%,
  100% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(62%);
  }
}

.screen-head {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(150, 105, 50, 0.22);

  &::after {
    content: '';
    position: absolute;
    left: 0;
    bottom: -1px;
    width: 180px;
    height: 1px;
    background: linear-gradient(90deg, #b98a3e, rgba(185, 138, 62, 0));
  }
}

.head-eyebrow {
  margin: 0 0 6px;
  font-size: 10px;
  letter-spacing: 3px;
  color: rgba(150, 105, 50, 0.75);
}

.head-title {
  margin: 0;
  font-family: $font-family-seal;
  font-size: 23px;
  font-weight: 700;
  letter-spacing: 2px;
  background: linear-gradient(92deg, #5f4423, #a06428 46%, #5f4423);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  filter: drop-shadow(0 2px 10px rgba(166, 124, 60, 0.22));
}

.head-sub {
  margin: 6px 0 0;
  font-size: 12px;
  color: rgba(90, 61, 36, 0.72);
}

.head-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  flex-shrink: 0;
}

.head-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: rgba(90, 61, 36, 0.55);
}

.meta-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(150, 105, 50, 0.55);
}

.fs-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  font-size: 12px;
  color: #7a5230;
  background: rgba(201, 164, 92, 0.14);
  border: 1px solid rgba(150, 105, 50, 0.4);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    color: #5a3d24;
    background: rgba(201, 164, 92, 0.26);
    border-color: rgba(150, 105, 50, 0.6);
    box-shadow: 0 0 12px rgba(201, 164, 92, 0.3);
  }
}

.screen-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  row-gap: 4px;
  margin-top: 12px;
  font-size: 11px;
  color: rgba(90, 61, 36, 0.6);

  .meta-item {
    display: inline-flex;
    align-items: baseline;
    gap: 5px;

    & + .meta-item::before {
      content: '·';
      margin: 0 12px;
      color: rgba(150, 105, 50, 0.5);
    }
  }

  b {
    font-family: 'Georgia', $font-family-base;
    font-size: 13px;
    font-weight: 600;
    color: #8a5a2b;
    font-variant-numeric: tabular-nums;
  }

  .meta-live {
    cursor: default;
  }

  .meta-live b {
    color: #a06428;
  }

  .live-dot {
    align-self: center;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #4e9a51;
    animation: live-pulse 2s ease-in-out infinite;
  }
}

@keyframes live-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(78, 154, 81, 0.4);
  }

  50% {
    box-shadow: 0 0 0 4px rgba(78, 154, 81, 0);
  }
}

.screen-grid {
  position: relative;
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 14px;
  margin-top: 14px;
}

.screen-grid-lower {
  grid-template-columns: 1.15fr 1fr;
}

.pane {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 12px 14px 10px;
  border-radius: 10px;
  border: 1px solid rgba(150, 105, 50, 0.2);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.55), rgba(255, 250, 240, 0.3));
  transition: border-color 0.2s ease, background 0.2s ease;

  &:hover {
    border-color: rgba(150, 105, 50, 0.42);
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.75), rgba(255, 250, 240, 0.4));
  }
}

.pane-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 6px;
}

.pane-title {
  position: relative;
  padding-left: 10px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #5a3d24;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 2px;
    width: 3px;
    height: 12px;
    border-radius: 2px;
    background: linear-gradient(180deg, #c9a45c, #a06428);
  }
}

.pane-note {
  font-size: 11px;
  color: rgba(90, 61, 36, 0.5);
  white-space: nowrap;
}

.chart {
  width: 100%;
}

.chart-main,
.chart-cluster {
  height: 300px;
}

.chart-live {
  height: 220px;
}

:fullscreen .chart-live {
  height: 26vh;
}

.live-empty {
  margin: 6px 2px 0;
  font-size: 11px;
  line-height: 1.7;
  color: rgba(90, 61, 36, 0.45);
}

.live-table {
  .lm-head,
  .lm-row {
    grid-template-columns: 1.1fr 1.8fr 0.7fr 0.8fr 0.9fr 0.6fr;
  }

  .lm-row {
    font-size: 11px;
  }
}

.event-list {
  display: flex;
  flex-direction: column;
}

.event-row {
  display: grid;
  grid-template-columns: 0.9fr 0.8fr 1.2fr 0.6fr;
  gap: 8px;
  align-items: center;
  padding: 7px 8px;
  font-size: 11px;
  color: #6b5236;
  font-variant-numeric: tabular-nums;

  & + .event-row {
    border-top: 1px dashed rgba(150, 105, 50, 0.18);
  }
}

.ev-time {
  color: rgba(90, 61, 36, 0.55);
}

.ev-flow {
  color: #8a5a2b;
}

.ev-page {
  text-align: right;
  color: rgba(90, 61, 36, 0.55);
}

.cluster-foot {
  margin: 2px 2px 0;
  font-size: 11px;
  line-height: 1.7;
  color: rgba(90, 61, 36, 0.5);
}

:fullscreen .chart-main,
:fullscreen .chart-cluster {
  height: 34vh;
}

.metric-table {
  display: flex;
  flex-direction: column;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.mt-row {
  display: grid;
  grid-template-columns: 1.55fr repeat(5, 1fr);
  align-items: center;
  padding: 7px 8px;
  border-radius: 6px;
  color: #6b5236;
  transition: background 0.18s ease, color 0.18s ease;

  & + .mt-row {
    border-top: 1px solid rgba(150, 105, 50, 0.14);
  }

  & > span:not(.mt-name) {
    text-align: right;
  }
}

.mt-row.mt-active {
  background: rgba(201, 164, 92, 0.18);
  color: #4a3218;
  box-shadow: inset 2px 0 0 rgba(160, 100, 40, 0.55);
}

.mt-head {
  border-top: 0 !important;
  font-size: 11px;
  letter-spacing: 0.5px;
  color: #a08050;
}

.mt-name {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #5a3d24;
}

.mt-best {
  background: rgba(201, 164, 92, 0.2);
  color: #4a3218;
  box-shadow: inset 2px 0 0 #b98a3e;
}

.best-tag {
  padding: 0 5px;
  font-size: 10px;
  font-style: normal;
  line-height: 16px;
  color: #3a2a14;
  background: linear-gradient(90deg, #e8c987, #c99a4e);
  border-radius: 3px;
}

.metric-foot {
  margin: 8px 2px 2px;
  font-size: 11px;
  line-height: 1.7;
  color: rgba(90, 61, 36, 0.5);
}

.insight-list {
  flex: 1;
  margin: 2px 0 10px;
  padding: 0;
  list-style: none;
}

.insight {
  padding: 9px 0;

  & + .insight {
    border-top: 1px dashed rgba(150, 105, 50, 0.25);
  }
}

.insight-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.insight-title {
  font-size: 12px;
  color: #5a3d24;
}

.insight-value {
  font-family: 'Georgia', $font-family-base;
  font-size: 14px;
  font-weight: 700;
  color: #9a6b2f;
  white-space: nowrap;
}

.insight-desc {
  margin: 4px 0 0;
  font-size: 11px;
  line-height: 1.7;
  color: rgba(90, 61, 36, 0.55);
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  padding: 3px 10px;
  font-size: 11px;
  color: #7a5a30;
  border: 1px solid rgba(150, 105, 50, 0.35);
  border-radius: 999px;
  background: rgba(201, 164, 92, 0.12);
}

@media (prefers-reduced-motion: reduce) {
  .screen-glow::before,
  .live-dot {
    animation: none;
  }
}

@media (max-width: 1200px) {
  .screen-grid,
  .screen-grid-lower {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .data-screen {
    padding: 18px 14px;
  }

  .screen-head {
    flex-direction: column;
  }

  .head-side {
    align-items: flex-start;
  }

  .mt-row {
    grid-template-columns: 1.5fr repeat(5, 1fr);
    font-size: 11px;
  }
}
</style>
