<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  keywords: string[]
  /** 飞花令模式：ancient / normal / super */
  mode?: string
  /** 当前回合序号：ancient 位次格的来源 */
  roundNo?: number
  size?: 'full' | 'mini'
}>(), {
  mode: '',
  roundNo: 1,
  size: 'full'
})

const isAncient = computed(() => props.mode === 'ancient')
const isSuper = computed(() => props.keywords.length > 1)
const mainKeyword = computed(() => props.keywords[0] || '')
const sealCaption = computed(() => {
  if (props.mode === 'ancient') return '位次令'
  if (props.mode === 'super') return '超级令'
  return '令'
})
/** ancient 位次格：1..roundNo，最后一格为目标位次 */
const slots = computed(() => {
  const n = Math.max(1, props.roundNo || 1)
  return Array.from({ length: n }, (_, i) => i + 1)
})
const targetIndex = computed(() => Math.max(1, props.roundNo || 1) - 1)
</script>

<template>
  <div
    class="seal-keyword"
    :class="[`is-${size}`, { 'is-ancient': isAncient, 'is-super': isSuper }]"
  >
    <div class="seal">
      <span class="seal-frame">
        <em v-for="k in keywords" :key="k" class="seal-char">{{ k }}</em>
      </span>
      <span class="seal-caption">{{ sealCaption }}</span>
    </div>

    <div v-if="isAncient" class="ancient-slots">
      <span
        v-for="(s, i) in slots"
        :key="s"
        class="slot"
        :class="{ 'is-target': i === targetIndex }"
      >
        <em v-if="i === targetIndex" class="slot-char">{{ mainKeyword }}</em>
        <span v-else class="slot-index">{{ s }}</span>
      </span>
      <span class="ancient-hint">令字须落于第 {{ roundNo }} 字位</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.seal-keyword {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
}

.seal {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.seal-frame {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  min-width: 78px;
  height: 78px;
  padding: 0 8px;
  border: 3px solid $accent-color-vermilion;
  border-radius: $border-radius-md;
  color: $accent-color-vermilion;
  background: rgba($accent-color-vermilion, 0.06);
  box-shadow: inset 0 0 0 1px rgba($accent-color-vermilion, 0.35);
  animation: sealDrop $transition-slow ease-out;
}

.seal-char {
  font-family: $font-family-seal;
  font-size: 44px;
  font-style: normal;
  line-height: 1;
}

.seal-caption {
  font-family: $font-family-seal;
  font-size: $font-size-xs;
  color: $primary-600;
  letter-spacing: 0.12em;
}

.ancient-slots {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.slot {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba($border-color-bronze, 0.85);
  border-radius: $border-radius-sm;
  background: rgba($surface-primary, 0.9);
  color: $text-color-light;
  font-size: $font-size-sm;
}

.slot.is-target {
  border-color: $primary-400;
  background: linear-gradient(135deg, rgba(#ffd700, 0.18), rgba($primary-100, 0.9));
  box-shadow: 0 0 10px rgba(#ffd700, 0.4);
  color: $primary-700;
}

.slot-char {
  font-family: $font-family-seal;
  font-size: 20px;
  font-style: normal;
}

.ancient-hint {
  margin-left: 4px;
  font-size: $font-size-xs;
  color: $primary-600;
}

.seal-keyword.is-mini {
  gap: $spacing-md;
}

.seal-keyword.is-mini .seal-frame {
  min-width: 52px;
  height: 52px;
  border-width: 2px;
}

.seal-keyword.is-mini .seal-char {
  font-size: 28px;
}

.seal-keyword.is-mini .slot {
  width: 26px;
  height: 26px;
}

.seal-keyword.is-mini .ancient-hint {
  display: none;
}

@keyframes sealDrop {
  from {
    opacity: 0;
    transform: scale(1.6);
    filter: blur(3px);
  }
  to {
    opacity: 1;
    transform: scale(1);
    filter: blur(0);
  }
}
</style>
