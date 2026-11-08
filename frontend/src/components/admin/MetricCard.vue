<script setup lang="ts">
import { computed } from 'vue'
import Sparkline from '@/components/admin/Sparkline.vue'
import { readChartPalette } from '@/composables/useAdminTheme'

const props = withDefaults(
  defineProps<{
    label: string
    value: string | number
    unit?: string
    note?: string
    /** 环比百分比；没有历史对比时传 null，不显示假箭头 */
    delta?: number | null
    icon?: string
    /** 色板序号 0-5 */
    tone?: number
    spark?: number[]
    sparkColor?: string
    loading?: boolean
    live?: boolean
  }>(),
  { tone: 0, delta: null, spark: () => [], loading: false, live: false, unit: '', note: '', icon: '', sparkColor: '' }
)

const palette = computed(() => readChartPalette())
const color = computed(
  () => props.sparkColor || palette.value[props.tone % palette.value.length] || palette.value[0]
)
const deltaText = computed(() => {
  if (props.delta == null || !Number.isFinite(props.delta)) return ''
  return `${props.delta >= 0 ? '+' : ''}${props.delta}%`
})
const deltaUp = computed(() => (props.delta ?? 0) >= 0)
</script>

<template>
  <div class="metric-card">
    <div class="mc-head">
      <span v-if="icon" class="mc-icon" :style="{ color }">
        <el-icon :size="15"><component :is="icon" /></el-icon>
      </span>
      <span class="mc-label">{{ label }}</span>
      <i v-if="live" class="mc-live" aria-label="实时"></i>
    </div>

    <div class="mc-value">
      <span class="mc-num">{{ loading ? '…' : value }}</span>
      <span v-if="unit" class="mc-unit">{{ unit }}</span>
    </div>

    <div v-if="deltaText || note" class="mc-foot">
      <span v-if="deltaText" class="mc-delta" :class="{ down: !deltaUp }">{{ deltaText }}</span>
      <span v-if="note" class="mc-note">{{ note }}</span>
    </div>

    <Sparkline
      v-if="spark.length"
      class="mc-spark"
      :values="spark"
      :color="color"
      :height="34"
    />
  </div>
</template>

<style scoped lang="scss">
.metric-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 12px 14px 10px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 10px;
  background: var(--adm-surface);
  overflow: hidden;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    border-color: color-mix(in srgb, var(--adm-primary) 30%, var(--adm-border-soft));
    box-shadow: var(--adm-shadow);
  }
}

.mc-head {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.mc-icon {
  display: inline-flex;
  flex-shrink: 0;
}

.mc-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--adm-text-muted);
}

.mc-live {
  width: 5px;
  height: 5px;
  margin-left: auto;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--adm-ok);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--adm-ok) 12%, transparent);
}

.mc-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-top: 6px;
}

.mc-num {
  font-family: var(--adm-font-serif);
  font-size: 22px;
  font-weight: 600;
  line-height: 1.15;
  color: var(--adm-text);
  font-variant-numeric: tabular-nums;
}

.mc-unit {
  font-size: 11px;
  color: var(--adm-text-faint);
}

.mc-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 16px;
  margin-top: 2px;
}

.mc-delta {
  font-size: 11px;
  color: var(--adm-ok);
  font-variant-numeric: tabular-nums;

  &.down {
    color: var(--adm-down);
  }
}

.mc-note {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  color: var(--adm-text-faint);
}

.mc-spark {
  margin-top: 6px;
}
</style>
