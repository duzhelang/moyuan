<script lang="ts">
export interface ResultSentence {
  sentence: string
}
</script>

<script setup lang="ts">
import { computed } from 'vue'
import PoemSlip from './PoemSlip.vue'
import type { GameOutcome } from '@/stores/game'

const props = withDefaults(defineProps<{
  result: GameOutcome
  sentences?: ResultSentence[]
  sentencesClickable?: boolean
  gameType?: 'feihua' | 'chain'
  historyCount?: number
  winTargetCount?: number | null
  isGuest?: boolean
  /** 后端裁判文案，优先展示 */
  reason?: string
}>(), {
  sentences: () => [],
  sentencesClickable: false,
  gameType: 'feihua',
  historyCount: 0,
  winTargetCount: null,
  isGuest: false,
  reason: ''
})

const emit = defineEmits<{
  (e: 'restart'): void
  (e: 'back'): void
  (e: 'detail', sentence: string): void
}>()

const isWin = computed(() => props.result === 'win')
const title = computed(() => {
  if (props.result === 'win') return '挑战成功'
  if (props.result === 'lose') return '挑战失败'
  return '已放弃'
})

const message = computed(() => {
  if (props.reason) return props.reason
  if (props.gameType === 'chain') {
    if (props.result === 'win') return '妙极！AI 已无句可接，这场接龙你赢了！'
    if (props.result === 'lose') return '接龙中断，再接再厉！'
    return '已结束本轮诗词接龙挑战，期待下次再战！'
  }
  if (props.result === 'win') {
    if (props.winTargetCount === null) return '博学多才，本轮 AI 已无诗句可接，你成功战胜 AI！'
    if (props.winTargetCount >= 20) return '毅力十足，成功完成高阶接龙挑战！'
    return '恭喜完成初级飞花令挑战！'
  }
  if (props.result === 'lose') return '本轮答题失误次数已用尽，再接再厉！'
  return '已结束本轮飞花令挑战，期待下次再战！'
})

const scoreLabel = computed(() =>
  props.gameType === 'chain' ? '接龙链长' : '有效诗句'
)
</script>

<template>
  <div class="result-scroll" :class="isWin ? 'is-win' : 'is-lose'">
    <div class="scroll-roll scroll-roll--top" />
    <div class="scroll-body">
      <div class="scroll-mark">
        <span v-if="isWin" class="gold-seal">胜</span>
        <span v-else class="ink-blot">憾</span>
      </div>
      <h2 class="scroll-title">{{ title }}</h2>
      <p class="scroll-message">{{ message }}</p>

      <div class="scroll-score">
        <span class="score-num">{{ historyCount }}</span>
        <span class="score-label">{{ scoreLabel }}</span>
        <span v-if="winTargetCount !== null" class="score-target">/ 目标 {{ winTargetCount }}</span>
      </div>

      <div v-if="sentences.length" class="scroll-sentences">
        <PoemSlip
          v-for="(item, idx) in sentences"
          :key="idx"
          :sentence="item.sentence"
          :clickable="sentencesClickable"
          @detail="emit('detail', item.sentence)"
        />
      </div>

      <p v-if="isGuest" class="scroll-guest">游客对局 · 战绩不会保存，登录后可留存对局记录</p>

      <div class="scroll-actions">
        <button class="scroll-btn scroll-btn--primary" type="button" @click="emit('restart')">再战一局</button>
        <button class="scroll-btn" type="button" @click="emit('back')">返回</button>
      </div>
    </div>
    <div class="scroll-roll scroll-roll--bottom" />
  </div>
</template>

<style scoped lang="scss">
.result-scroll {
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
  animation: scrollOpen 0.6s ease-out;
}

.scroll-roll {
  height: 16px;
  border-radius: 8px;
  background: linear-gradient(90deg, $primary-700, $secondary-color, $primary-700);
  box-shadow: $box-shadow-md;
}

.scroll-body {
  padding: $spacing-lg $spacing-xl;
  background:
    linear-gradient(180deg, rgba($surface-primary, 0.98), rgba($primary-50, 0.95));
  border-left: 1px solid rgba($border-color-bronze, 0.7);
  border-right: 1px solid rgba($border-color-bronze, 0.7);
  text-align: center;
}

.scroll-mark {
  display: flex;
  justify-content: center;
  margin-bottom: $spacing-sm;
}

.gold-seal,
.ink-blot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  font-family: $font-family-seal;
  font-size: 32px;
  border-radius: $border-radius-md;
}

.gold-seal {
  color: #ffd700;
  border: 3px solid #ffd700;
  box-shadow: 0 0 16px rgba(#ffd700, 0.6);
  background: rgba($primary-700, 0.85);
}

.ink-blot {
  color: $text-color-light;
  border: 3px solid rgba($text-color-secondary, 0.6);
  background: rgba($text-color-ink, 0.12);
}

.scroll-title {
  margin: $spacing-sm 0;
  font-family: $font-family-seal;
  font-size: $font-size-title;
  color: $primary-700;
  letter-spacing: 0.08em;
}

.scroll-message {
  margin: 0 0 $spacing-md;
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: $line-height-loose;
}

.scroll-score {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  padding: $spacing-xs $spacing-md;
  border-radius: $border-radius-sm;
  background: rgba($primary-color, 0.08);
  margin-bottom: $spacing-md;
}

.score-num {
  font-family: $font-family-poetry;
  font-size: $font-size-hero;
  color: $accent-color-vermilion;
  line-height: 1;
}

.score-label {
  font-size: $font-size-sm;
  color: $primary-700;
}

.score-target {
  font-size: $font-size-xs;
  color: $text-color-light;
}

.scroll-sentences {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  max-height: 260px;
  overflow-y: auto;
  margin-bottom: $spacing-md;
  text-align: left;
}

.scroll-guest {
  margin: 0 0 $spacing-md;
  font-size: $font-size-xs;
  color: $primary-600;
}

.scroll-actions {
  display: flex;
  justify-content: center;
  gap: $spacing-md;
}

.scroll-btn {
  padding: 8px 20px;
  border-radius: $border-radius-md;
  border: 1px solid $border-color-bronze;
  background: rgba($surface-primary, 0.95);
  color: $primary-700;
  cursor: pointer;
  font-size: $font-size-base;
  transition: all $transition-base;
}

.scroll-btn:hover {
  border-color: $primary-400;
  box-shadow: $box-shadow;
}

.scroll-btn--primary {
  background: $primary-500;
  border-color: $primary-500;
  color: #fff;
}

.scroll-btn--primary:hover {
  background: $primary-600;
}

@keyframes scrollOpen {
  from {
    opacity: 0;
    transform: scaleY(0.4);
  }
  to {
    opacity: 1;
    transform: scaleY(1);
  }
}
</style>
