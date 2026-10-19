<script setup lang="ts">
/**
 * 全站 AI 小窗的唤起入口
 * 形态与位置由路由画像的 entry 配置决定（圆钮 / 胶囊），小窗展开时自动隐藏
 */
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useAiChatStore } from '@/stores/aiChat'
import { getAssistant } from '@/configs/ai-chat/assistants'

const store = useAiChatStore()
const { isOpen, profile, activeAssistant } = storeToRefs(store)

const entry = computed(() => profile.value.entry)
const visible = computed(() => entry.value.show && !isOpen.value && !profile.value.disabled)

const style = computed(() => {
  const config = entry.value
  const styleMap: Record<string, string> = {}
  if (config.anchor.startsWith('right')) styleMap.right = `${config.dx}px`
  else styleMap.left = `${config.dx}px`
  if (config.anchor.endsWith('bottom')) styleMap.bottom = `${config.dy}px`
  else styleMap.top = `${config.dy}px`
  return {
    ...styleMap,
    ...profile.value.theme
  }
})

const avatar = computed(() => getAssistant(activeAssistant.value).avatar)
const tip = computed(() => getAssistant(activeAssistant.value).tip)
</script>

<template>
  <Teleport to="body">
    <Transition name="ai-fab">
      <button
        v-if="visible"
        class="ai-chat-fab"
        :class="[entry.shape, entry.anchor]"
        :style="style"
        :title="tip"
        @click="store.toggle()"
      >
        <img v-if="entry.shape === 'circle'" class="fab-avatar" :src="avatar" alt="AI 助手" />
        <template v-else>
          <img class="fab-avatar" :src="avatar" alt="AI 助手" />
          <span class="fab-label">{{ entry.label || 'AI 助手' }}</span>
        </template>
      </button>
    </Transition>
  </Teleport>
</template>

<style scoped>
.ai-chat-fab {
  position: fixed;
  z-index: 3000;
  border: 1px solid var(--ai-border, rgba(184, 134, 11, 0.45));
  background: rgba(255, 253, 247, 0.96);
  box-shadow: 0 6px 20px rgba(60, 34, 12, 0.28);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  backdrop-filter: blur(8px);
}

.ai-chat-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 26px rgba(60, 34, 12, 0.34);
}

.ai-chat-fab.circle {
  width: 48px;
  height: 48px;
  padding: 0;
  border-radius: 50%;
  border: 2px solid rgba(255, 215, 0, 0.65);
  background: linear-gradient(135deg, var(--ai-accent, #8b4513), var(--ai-accent-2, #d2691e));
}

.ai-chat-fab.pill {
  padding: 6px 14px 6px 8px;
  border-radius: 999px;
  color: var(--ai-accent, #8b4513);
  font-size: 13px;
  font-weight: 600;
}

.fab-avatar {
  width: 32px;
  height: 32px;
  max-width: none;
  border-radius: 50%;
  background: #fff;
  border: 1.5px solid rgba(255, 215, 0, 0.6);
}

.ai-chat-fab.circle .fab-avatar {
  width: 40px;
  height: 40px;
  border-color: rgba(255, 255, 255, 0.75);
}

.fab-label {
  white-space: nowrap;
}

.ai-fab-enter-active,
.ai-fab-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.ai-fab-enter-from,
.ai-fab-leave-to {
  opacity: 0;
  transform: scale(0.9);
}
</style>
