<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  /** correct=墨绿扩散 / wrong=朱砂判字章 / repeat=灰墨 / none=隐藏 */
  variant?: 'correct' | 'wrong' | 'repeat' | 'none'
  message?: string
}>(), {
  variant: 'none',
  message: ''
})

/** 混入 key 以在每次反馈变化时重播入场动画 */
const animKey = computed(() => `${props.variant}|${props.message}`)
const showSeal = computed(() => props.variant === 'wrong')
</script>

<template>
  <transition name="verdict-fade">
    <div
      v-if="variant !== 'none' && message"
      :key="animKey"
      class="verdict-banner"
      :class="`is-${variant}`"
      role="status"
    >
      <span v-if="showSeal" class="verdict-seal">判</span>
      <span class="verdict-text">{{ message }}</span>
    </div>
  </transition>
</template>

<style scoped lang="scss">
.verdict-banner {
  display: inline-flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  border-radius: $border-radius-md;
  font-family: $font-family-seal;
  font-size: $font-size-lg;
  letter-spacing: 0.04em;
  border: 1px solid transparent;
}

.verdict-text {
  line-height: $line-height-base;
}

/* 正确：墨绿扩散 */
.verdict-banner.is-correct {
  color: darken($success-color, 20%);
  background: radial-gradient(circle at left center, rgba($success-color, 0.22), transparent 70%);
  border-color: rgba($success-color, 0.35);
  animation: verdictSpread 0.5s ease-out;
}

/* 错误：朱砂「判」字章（保留对局，不播失败动画） */
.verdict-banner.is-wrong {
  color: $accent-color-vermilion;
  background: rgba($accent-color-vermilion, 0.08);
  border-color: rgba($accent-color-vermilion, 0.4);
}

/* 重复：灰墨 */
.verdict-banner.is-repeat {
  color: $text-color-secondary;
  background: rgba($text-color-light, 0.12);
  border-color: rgba($text-color-light, 0.4);
}

.verdict-seal {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 2px solid $accent-color-vermilion;
  border-radius: $border-radius-sm;
  font-size: 16px;
  animation: verdictStamp 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes verdictSpread {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes verdictStamp {
  from {
    opacity: 0;
    transform: scale(2.2) rotate(-12deg);
  }
  to {
    opacity: 1;
    transform: scale(1) rotate(0);
  }
}

.verdict-fade-enter-active,
.verdict-fade-leave-active {
  transition: opacity $transition-base;
}

.verdict-fade-enter-from,
.verdict-fade-leave-to {
  opacity: 0;
}
</style>
