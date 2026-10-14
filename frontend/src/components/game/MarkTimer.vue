<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  remainSeconds: number
  /** 计时环分母：缓冲态为 5 秒，其余为单回合总时长 */
  total?: number
  size?: 'full' | 'mini'
  /** AI 生成阶段：暂停展示 */
  paused?: boolean
  /** 环下提示文案（如「研墨中」「停笔五秒后续时」「缓冲时限」） */
  hint?: string
}>(), {
  total: 30,
  size: 'full',
  paused: false,
  hint: ''
})

const RADIUS = 52
const CIRCUMFERENCE = 2 * Math.PI * RADIUS

/** 弧长与中央秒数同源：均由 remainSeconds / total 计算 */
const ratio = computed(() => {
  if (props.total <= 0) return 0
  return Math.max(0, Math.min(1, props.remainSeconds / props.total))
})
const dashOffset = computed(() => CIRCUMFERENCE * (1 - ratio.value))
const isUrgent = computed(() => props.remainSeconds <= 5)
</script>

<template>
  <div
    class="mark-timer"
    :class="[`is-${size}`, { 'is-urgent': isUrgent, 'is-paused': paused }]"
    role="timer"
  >
    <svg class="ring" viewBox="0 0 120 120" aria-hidden="true">
      <circle class="ring-track" cx="60" cy="60" :r="RADIUS" />
      <circle
        class="ring-arc"
        cx="60"
        cy="60"
        :r="RADIUS"
        :stroke-dasharray="CIRCUMFERENCE"
        :stroke-dashoffset="dashOffset"
      />
    </svg>
    <div class="ring-center">
      <span class="seconds">{{ remainSeconds }}</span>
      <span class="unit">秒</span>
      <span v-if="hint" class="paused-text">{{ hint }}</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.mark-timer {
  position: relative;
  width: 148px;
  height: 148px;

  &.is-mini {
    width: 88px;
    height: 88px;
  }
}

.ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-track {
  fill: none;
  stroke: rgba($border-color-bronze, 0.55);
  stroke-width: 6;
}

.ring-arc {
  fill: none;
  stroke: $accent-color;
  stroke-width: 6;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.9s linear, stroke $transition-base;
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 2px;
  color: $primary-700;
}

.seconds {
  font-family: $font-family-poetry;
  font-size: 40px;
  line-height: 1;
  font-weight: 600;
}

.unit {
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.paused-text {
  position: absolute;
  left: 0;
  right: 0;
  bottom: -2px;
  text-align: center;
  font-size: $font-size-xs;
  color: $primary-500;
  letter-spacing: 0.08em;
}

.mark-timer.is-urgent .ring-arc {
  stroke: $accent-color-vermilion;
}

.mark-timer.is-urgent .seconds {
  color: $accent-color-vermilion;
}

.mark-timer.is-urgent:not(.is-paused) {
  animation: markPulse 1.4s ease-in-out infinite;
}

.mark-timer.is-paused .ring-arc {
  stroke: rgba($accent-color, 0.5);
}

.mark-timer.is-mini .seconds {
  font-size: 24px;
}

.mark-timer.is-mini .unit {
  font-size: $font-size-xs;
}

.mark-timer.is-mini .paused-text {
  display: none;
}

/* 轻脉冲：不闪烁，避免视觉干扰 */
@keyframes markPulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.04);
  }
}
</style>
