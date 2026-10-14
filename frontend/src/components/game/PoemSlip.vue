<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  sentence: string
  title?: string
  author?: string
  matchDesc?: string
  /** 竖排诗笺（writing-mode: vertical-rl） */
  vertical?: boolean
  /** 人机分色：user 宣纸白，ai 淡墨青 */
  source?: 'user' | 'ai' | ''
  /** 衔接字高亮：first 首字 / last 末字 / both 首末 */
  highlightEdge?: 'first' | 'last' | 'both' | ''
  /** 可点击查看详情（本地库诗句） */
  clickable?: boolean
  /** 锦囊样式（悬浮金色标题） */
  pouch?: boolean
}>(), {
  title: '',
  author: '',
  matchDesc: '',
  vertical: false,
  source: '',
  highlightEdge: '',
  clickable: false,
  pouch: false
})

const emit = defineEmits<{ (e: 'detail'): void }>()

const chars = computed(() => Array.from(props.sentence))
const edgeSet = computed(() => {
  const set = new Set<number>()
  if (!chars.value.length) return set
  if (props.highlightEdge === 'first' || props.highlightEdge === 'both') set.add(0)
  if (props.highlightEdge === 'last' || props.highlightEdge === 'both') set.add(chars.value.length - 1)
  return set
})
const clickable = computed(() => props.clickable)
</script>

<template>
  <div
    class="poem-slip"
    :class="{
      'is-vertical': vertical,
      'is-user': source === 'user',
      'is-ai': source === 'ai',
      'is-pouch': pouch,
      'is-clickable': clickable
    }"
    @click="clickable && emit('detail')"
  >
    <p class="slip-sentence">
      <template v-for="(c, i) in chars" :key="i">
        <em v-if="edgeSet.has(i)" class="edge-char">{{ c }}</em>
        <span v-else>{{ c }}</span>
      </template>
    </p>
    <p v-if="author || title" class="slip-meta">
      <span v-if="author">{{ author }}</span>
      <span v-if="author && title"> · </span>
      <span v-if="title">《{{ title }}》</span>
    </p>
    <p v-if="matchDesc" class="slip-desc">{{ matchDesc }}</p>
    <span v-if="clickable" class="slip-more">查看详情</span>
  </div>
</template>

<style scoped lang="scss">
.poem-slip {
  position: relative;
  padding: $spacing-sm $spacing-md;
  border: 1px solid rgba($border-color-bronze, 0.8);
  border-radius: $border-radius-md;
  background: rgba($surface-primary, 0.96);
  box-shadow: $box-shadow;
  transition: transform $transition-base, box-shadow $transition-base;

  &.is-ai {
    background: linear-gradient(135deg, rgba($success-color, 0.08), rgba($surface-secondary, 0.96));
    border-color: rgba($primary-200, 0.9);
  }

  &.is-user {
    background: rgba($surface-elevated, 0.98);
  }
}

.slip-sentence {
  margin: 0;
  font-family: $font-family-poetry;
  font-size: $font-size-poem;
  letter-spacing: $letter-spacing-poem;
  color: $text-color-ink;
  line-height: $line-height-poem;
}

.edge-char {
  font-style: normal;
  color: $primary-400;
  text-shadow: 0 0 8px rgba(#ffd700, 0.5);
}

.slip-meta {
  margin: 2px 0 0;
  font-size: $font-size-xs;
  color: $text-color-secondary;
}

.slip-desc {
  margin: 2px 0 0;
  font-size: $font-size-xs;
  color: $accent-color-vermilion;
}

.slip-more {
  position: absolute;
  right: $spacing-sm;
  bottom: $spacing-xs;
  font-size: $font-size-xs;
  color: $primary-500;
  opacity: 0;
  transition: opacity $transition-base;
}

.poem-slip.is-clickable {
  cursor: pointer;
}

.poem-slip.is-clickable:hover {
  transform: translateY(-2px);
  box-shadow: $card-hover-shadow;

  .slip-sentence {
    color: #ffd700;
    text-shadow: 0 0 12px rgba(#ffd700, 0.75);
  }

  .slip-more {
    opacity: 1;
  }
}

/* 锦囊形态：轻叠的诗笺 */
.poem-slip.is-pouch {
  background: linear-gradient(160deg, rgba($primary-50, 0.96), rgba($surface-primary, 0.98));
}

/* 竖排 */
.poem-slip.is-vertical {
  writing-mode: vertical-rl;
  text-orientation: upright;
  padding: $spacing-md $spacing-sm;
  display: inline-block;
}

.poem-slip.is-vertical .slip-sentence {
  line-height: 1.6;
}

.poem-slip.is-vertical .slip-meta,
.poem-slip.is-vertical .slip-desc {
  margin: 0 0 0 2px;
  letter-spacing: 0;
}

.poem-slip.is-vertical .slip-more {
  position: static;
  display: block;
  margin-top: $spacing-xs;
}
</style>
