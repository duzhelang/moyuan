<script lang="ts">
/** 接龙链节点：source 决定人机分色 */
export interface ChainNode {
  sentence: string
  title?: string
  author?: string
  source: 'user' | 'ai'
  /** 可点击查看详情（本地库诗句，默认 true） */
  clickable?: boolean
}
</script>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  nodes: ChainNode[]
  /** AI 正在接句：链尾追加研墨占位节点 */
  waiting?: boolean
}>(), {
  waiting: false
})

const emit = defineEmits<{ (e: 'detail', sentence: string): void }>()

/** 逐字拆分并标出首末衔接字 */
function edges(sentence: string) {
  const chars = Array.from(sentence)
  return chars.map((c, i) => ({
    c,
    edge: i === 0 || i === chars.length - 1
  }))
}

const rendered = computed(() =>
  props.nodes.map((node) => ({ ...node, chars: edges(node.sentence) }))
)
</script>

<template>
  <div class="chain-track">
    <div
      v-for="(node, idx) in rendered"
      :key="idx"
      class="chain-node"
      :class="[`is-${node.source}`, { 'is-clickable': node.clickable !== false }]"
      @click="node.clickable !== false && emit('detail', node.sentence)"
    >
      <span class="node-dot" />
      <div class="node-slip">
        <p class="node-sentence">
          <template v-for="(ch, ci) in node.chars" :key="ci">
            <em v-if="ch.edge" class="edge-char">{{ ch.c }}</em>
            <span v-else>{{ ch.c }}</span>
          </template>
        </p>
        <p v-if="node.author || node.title" class="node-meta">
          <span v-if="node.author">{{ node.author }}</span>
          <span v-if="node.author && node.title"> · </span>
          <span v-if="node.title">《{{ node.title }}》</span>
        </p>
        <span class="node-tag">{{ node.source === 'user' ? '你' : 'AI' }}</span>
      </div>
    </div>

    <div v-if="waiting" class="chain-node is-ai is-waiting">
      <span class="node-dot" />
      <div class="node-slip">
        <span class="ink-dots"><i /><i /><i /></span>
        <span class="node-tag">AI</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.chain-track {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.chain-node {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: $spacing-md;
}

/* 节点间接续金线 */
.chain-node:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 9px;
  top: 26px;
  bottom: calc(-1 * #{$spacing-md});
  width: 2px;
  background: linear-gradient(180deg, rgba(#ffd700, 0.7), rgba($accent-color, 0.3));
}

.node-dot {
  position: relative;
  z-index: 1;
  flex: 0 0 auto;
  width: 20px;
  height: 20px;
  margin-top: 4px;
  border-radius: 50%;
  border: 2px solid $accent-color;
  background: $surface-primary;
  box-shadow: 0 0 6px rgba(#ffd700, 0.4);
}

.node-slip {
  position: relative;
  flex: 1;
  min-width: 0;
  padding: $spacing-sm $spacing-md;
  border: 1px solid rgba($border-color-bronze, 0.8);
  border-radius: $border-radius-md;
  background: rgba($surface-elevated, 0.98);
}

.chain-node.is-ai .node-slip {
  background: linear-gradient(135deg, rgba($success-color, 0.08), rgba($surface-secondary, 0.96));
  border-color: rgba($primary-200, 0.9);
}

.chain-node.is-clickable .node-slip {
  cursor: pointer;
}

.chain-node.is-clickable .node-slip:hover {
  box-shadow: $card-hover-shadow;
}

.node-sentence {
  margin: 0;
  font-family: $font-family-poetry;
  font-size: $font-size-poem;
  letter-spacing: $letter-spacing-poem;
  color: $text-color-ink;
  line-height: $line-height-poem;
}

/* 上句末字与下句首字同为金色高亮 */
.edge-char {
  font-style: normal;
  color: $primary-400;
  text-shadow: 0 0 8px rgba(#ffd700, 0.6);
}

.node-meta {
  margin: 2px 0 0;
  font-size: $font-size-xs;
  color: $text-color-secondary;
}

.node-tag {
  position: absolute;
  top: -8px;
  right: $spacing-sm;
  padding: 0 6px;
  font-size: $font-size-xs;
  border-radius: $badge-radius;
  background: rgba($primary-color, 0.08);
  color: $primary-700;
}

.chain-node.is-ai .node-tag {
  background: rgba($success-color, 0.16);
}

.ink-dots {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  height: 22px;
}

.ink-dots i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $primary-500;
  animation: inkSpin 1.2s ease-in-out infinite;
}

.ink-dots i:nth-child(2) {
  animation-delay: 0.15s;
}

.ink-dots i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes inkSpin {
  0%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  50% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
