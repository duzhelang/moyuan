<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { readChartInk, readChartPalette, useAdminTheme } from '@/composables/useAdminTheme'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent])

const { resolvedTheme } = useAdminTheme()

/** 图表配色跟随后台主题 */
const palette = computed(() => {
  void resolvedTheme.value
  return readChartPalette()
})

/** 刻度/网格/tooltip 取主题色，保证深色主题下文字不发灰 */
const ink = computed(() => {
  void resolvedTheme.value
  return readChartInk()
})

const props = withDefaults(
  defineProps<{
    dates: string[]
    visits: number[]
    uniqueVisitors: number[]
    users: number[]
    posts: number[]
    rangeLabel?: string
  }>(),
  { rangeLabel: '近 7 天' }
)

const last = (arr: number[]) => (arr.length ? arr[arr.length - 1] : 0)

const todayVisits = computed(() => last(props.visits))
const todayUv = computed(() => last(props.uniqueVisitors))
const todayUsers = computed(() => last(props.users))

/**
 * 人均浏览：PV / UV。
 * 注意 PV 实为「接口请求数」（VisitLogAspect 按每次 /api 调用记一条），
 * UV 是 IP 去重，本地开发常常只有 1~2 个 IP，算出来会是 136 这种无意义值，
 * 因此 UV 小于 5 时不展示，避免给出误导数字。
 */
const perVisit = computed(() => {
  if (todayUv.value < 5) return '样本不足'
  return (todayVisits.value / todayUv.value).toFixed(1) + ' 次'
})

/** UV 占比：UV / PV */
const uvRatio = computed(() => {
  if (!todayVisits.value) return '—'
  return ((todayUv.value / todayVisits.value) * 100).toFixed(1) + '%'
})

const axisStyle = computed(() => ({
  axisLine: { show: false },
  axisTick: { show: false },
  axisLabel: { color: ink.value.muted, fontSize: 11 },
  splitLine: { lineStyle: { color: ink.value.border, type: 'dashed' as const } }
}))

/** tooltip 也跟随主题，避免深色页面弹出白色块 */
const tooltipStyle = computed(() => ({
  trigger: 'axis' as const,
  backgroundColor: ink.value.surface,
  borderColor: ink.value.border,
  textStyle: { color: ink.value.regular, fontSize: 12 }
}))

/** 面积渐变：用 8 位十六进制叠加透明度，ECharts 可直接识别 */
const areaFade = (hex: string) => ({
  color: {
    type: 'linear',
    x: 0,
    y: 0,
    x2: 0,
    y2: 1,
    colorStops: [
      { offset: 0, color: `${hex}38` },
      { offset: 1, color: `${hex}05` }
    ]
  }
})

const trafficOption = computed(() => ({
  tooltip: tooltipStyle.value,
  grid: { left: 8, right: 12, top: 18, bottom: 4, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: props.dates,
    ...axisStyle.value,
    splitLine: { show: false }
  },
  yAxis: { type: 'value', ...axisStyle.value },
  series: [
    {
      name: '请求量 PV',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: props.visits,
      color: palette.value[0],
      lineStyle: { width: 2 },
      areaStyle: areaFade(palette.value[0])
    },
    {
      name: '访客 UV',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: props.uniqueVisitors,
      color: palette.value[1],
      lineStyle: { width: 1.5, type: 'dashed' }
    }
  ]
}))

const growthOption = computed(() => ({
  tooltip: tooltipStyle.value,
  grid: { left: 8, right: 12, top: 18, bottom: 4, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: props.dates,
    ...axisStyle.value,
    splitLine: { show: false }
  },
  // 人数是整数，避免出现 0.2 / 0.4 这种刻度
  yAxis: { type: 'value', minInterval: 1, ...axisStyle.value },
  series: [
    {
      name: '新增用户',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: props.users,
      color: palette.value[2],
      lineStyle: { width: 2 },
      areaStyle: areaFade(palette.value[2])
    },
    {
      name: '新增发帖',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: props.posts,
      color: palette.value[4],
      lineStyle: { width: 1.5, type: 'dashed' }
    }
  ]
}))
</script>

<template>
  <el-card class="monitor-card" shadow="never">
    <div class="card-head">
      <span class="card-title">用户监测</span>
      <span class="card-note">{{ rangeLabel }}</span>
    </div>

    <div class="charts">
      <div class="chart-box">
        <p class="chart-label">
          请求量 PV <i class="lg-solid"></i> / 访客 UV <i class="lg-dash"></i>
        </p>
        <VChart class="chart" :option="trafficOption" autoresize />
      </div>
      <div class="chart-box">
        <p class="chart-label">
          新增用户 <i class="lg-solid purple"></i> / 新增发帖
          <i class="lg-dash gray"></i>
        </p>
        <VChart class="chart" :option="growthOption" autoresize />
      </div>
    </div>

    <div class="summary">
      <span>人均请求 <b>{{ perVisit }}</b></span>
      <span>UV 占比 <b>{{ uvRatio }}</b></span>
      <span>今日新增 <b>{{ todayUsers }}</b></span>
    </div>
  </el-card>
</template>

<style scoped lang="scss">
.monitor-card {
  border-radius: 8px;
  margin-bottom: 20px;

  :deep(.el-card__body) {
    padding: 16px 20px 14px;
  }
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.card-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--adm-text);
}

.card-note {
  font-size: 12px;
  color: var(--adm-text-muted);
}

.charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
}

.chart-box {
  min-width: 0;
}

.chart-label {
  font-size: 12px;
  color: var(--adm-text-muted);
  margin: 6px 0 2px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.chart {
  height: 168px;
  width: 100%;
}

.lg-solid {
  display: inline-block;
  width: 12px;
  height: 2px;
  background: var(--adm-primary);

  &.purple {
    background: var(--adm-chart-2);
  }
}

.lg-dash {
  display: inline-block;
  width: 12px;
  height: 2px;
  background: repeating-linear-gradient(90deg, var(--adm-warn) 0 3px, transparent 3px 6px);

  &.gray {
    background: repeating-linear-gradient(90deg, var(--adm-text-muted) 0 3px, transparent 3px 6px);
  }
}

.summary {
  display: flex;
  gap: 20px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--adm-border-soft);
  font-size: 12px;
  color: var(--adm-text-muted);

  b {
    color: var(--adm-text);
    font-weight: 500;
    font-variant-numeric: tabular-nums;
  }
}
</style>
