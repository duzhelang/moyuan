<script setup lang="ts">
/**
 * AI 小窗内的助理切换按钮组
 * 两个可爱头像 + 悬浮提示，点击切换；当前助理高亮
 */
import { AI_ASSISTANTS } from '@/configs/ai-chat/assistants'
import type { AssistantId } from '@/configs/ai-chat/assistants'

interface Props {
  modelValue: AssistantId
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: AssistantId): void
}>()

const handleSelect = (id: AssistantId) => {
  if (id === props.modelValue) return
  emit('update:modelValue', id)
}
</script>

<template>
  <div class="ai-assistant-switch">
    <button
      v-for="item in AI_ASSISTANTS"
      :key="item.id"
      type="button"
      class="assistant-btn"
      :class="{ active: item.id === modelValue }"
      :aria-label="item.name"
      @click="handleSelect(item.id)"
    >
      <img class="assistant-avatar" :src="item.avatar" :alt="item.name" />
      <span class="assistant-tip">{{ item.tip }}</span>
    </button>
  </div>
</template>

<style scoped>
.ai-assistant-switch {
  display: flex;
  align-items: center;
  gap: 6px;
}

.assistant-btn {
  position: relative;
  width: 30px;
  height: 30px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.assistant-btn:hover {
  transform: translateY(-2px);
}

.assistant-btn.active {
  border-color: #ffd700;
  box-shadow: 0 0 0 2px rgba(255, 215, 0, 0.25), 0 2px 8px rgba(0, 0, 0, 0.18);
}

.assistant-avatar {
  width: 24px;
  height: 24px;
  max-width: none;
  border-radius: 50%;
  display: block;
}

.assistant-tip {
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%) translateY(-4px);
  white-space: nowrap;
  padding: 5px 10px;
  border-radius: 6px;
  background: rgba(45, 30, 18, 0.94);
  color: #ffe9c7;
  font-size: 12px;
  line-height: 1.2;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.18s ease, transform 0.18s ease;
  z-index: 20;
}

.assistant-btn:hover .assistant-tip {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}
</style>
