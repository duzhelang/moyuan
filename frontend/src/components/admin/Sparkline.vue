<script lang="ts">
/** 模块级自增，保证同页多个实例的渐变 id 不冲突 */
let sparkSeed = 0
const nextSparkId = () => `spark-fade-${++sparkSeed}`
</script>

<script setup lang="ts">
import { computed } from 'vue'

/**
 * 极简走势线：无坐标轴、无刻度、无交互。
 * 只表达「近期是上升还是下降」，细节留给下方趋势区。
 */
const props = withDefaults(
  defineProps<{
    values: number[]
    color?: string
    /** 组件高度（px） */
    height?: number
    /** 是否高亮末点 */
    highlight?: boolean
    /** 是否填充面积 */
    area?: boolean
  }>(),
  {
    color: '#8a5a2b',
    height: 38,
    highlight: true,
    area: true
  }
)

/** 有效点（过滤 null / NaN） */
const series = computed(() =>
  props.values.map((v) => (Number.isFinite(v) ? v : 0))
)

const valid = computed(() => series.value.length >= 2)

/** 归一化到 0~100 的坐标系，上下各留 12 的余量避免贴边 */
const points = computed(() => {
  const vals = series.value
  if (vals.length === 0) return []
  if (vals.length === 1) return [{ x: 100, y: 50 }]
  const min = Math.min(...vals)
  const max = Math.max(...vals)
  const span = max - min || 1
  return vals.map((v, i) => ({
    x: (i / (vals.length - 1)) * 100,
    y: 88 - ((v - min) / span) * 76
  }))
})

const polyline = computed(() =>
  points.value.map((p) => `${p.x.toFixed(2)},${p.y.toFixed(2)}`).join(' ')
)

/** 面积路径：首点垂到基线，沿折线走到末点，再垂回基线 */
const areaPath = computed(() => {
  const pts = points.value
  if (pts.length < 2) return ''
  const head = `M${pts[0].x.toFixed(2)},100`
  const body = pts.map((p) => `L${p.x.toFixed(2)},${p.y.toFixed(2)}`).join(' ')
  return `${head} ${body} L${pts[pts.length - 1].x.toFixed(2)},100 Z`
})

const lastPoint = computed(() => points.value[points.value.length - 1])

const dotStyle = computed(() => {
  const p = lastPoint.value
  if (!p) return {}
  return {
    left: `${p.x}%`,
    top: `${(p.y / 100) * 100}%`,
    backgroundColor: props.color,
    boxShadow: `0 0 0 3px color-mix(in srgb, ${props.color} 14%, transparent)`
  }
})

const lineColor = computed(() => props.color)
const fadeId = nextSparkId()
</script>

<template>
  <div class="sparkline" :style="{ height: height + 'px' }">
    <!-- 无数据时给一条虚线底纹，避免卡片底部空一块 -->
    <div v-if="!valid" class="spark-empty"></div>

    <template v-else>
      <svg
        class="spark-svg"
        viewBox="0 0 100 100"
        preserveAspectRatio="none"
        aria-hidden="true"
      >
        <defs>
          <linearGradient :id="fadeId" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" :stop-color="lineColor" stop-opacity="0.18" />
            <stop offset="100%" :stop-color="lineColor" stop-opacity="0" />
          </linearGradient>
        </defs>
        <path v-if="area" :d="areaPath" :fill="`url(#${fadeId})`" />
        <polyline
          :points="polyline"
          fill="none"
          :stroke="lineColor"
          stroke-width="1.5"
          stroke-linecap="round"
          stroke-linejoin="round"
          vector-effect="non-scaling-stroke"
        />
      </svg>
      <span v-if="highlight" class="spark-dot" :style="dotStyle"></span>
    </template>
  </div>
</template>

<style scoped lang="scss">
.sparkline {
  position: relative;
  width: 100%;
  overflow: hidden;
}

.spark-svg {
  display: block;
  width: 100%;
  height: 100%;
}

.spark-dot {
  position: absolute;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.spark-empty {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  border-top: 1px dashed var(--adm-border);
}
</style>
