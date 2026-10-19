<script setup lang="ts">
/**
 * 全站 AI 小窗（全局单例，跨路由不销毁）
 * 位置、尺寸、主题、模块码、预设问题全部由路由画像驱动；
 * 内置两个轻量助理（诗词助理 / 网页用法助理），消息分区隔离。
 */
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAiChatStore } from '@/stores/aiChat'
import { getAssistant } from '@/configs/ai-chat/assistants'
import { renderAiLinks } from '@/utils/renderAiLinks'
import AiAssistantSwitch from './AiAssistantSwitch.vue'
import LoginPrompt from './LoginPrompt.vue'

const route = useRoute()
const store = useAiChatStore()
const { isOpen, minimized, loginPromptVisible, messages, loading, presetChips, greeting, activeAssistant, context, renderableRoutes, profile } =
  storeToRefs(store)

const inputText = ref('')
const messagesRef = ref<HTMLElement | null>(null)
/** 拖动过程中的临时位置；为空时使用画像默认位置 */
const dragPos = ref<{ x: number; y: number } | null>(null)
const dragging = ref(false)

const visible = computed(() => isOpen.value && !profile.value.disabled)
const boxStyle = computed(() => {
  const pos = dragPos.value || store.position
  return {
    left: `${pos.x}px`,
    top: `${pos.y}px`,
    width: `${profile.value.size.w}px`,
    height: `${profile.value.size.h}px`,
    ...profile.value.theme
  }
})

const contextLabel = computed(() => {
  const ctx = context.value
  if (!ctx || ctx.type === 'none') return ''
  if (ctx.type === 'poet') return ctx.name ? `诗人 · ${ctx.name}` : '诗人'
  return ctx.name ? `诗词 · ${ctx.name}` : '诗词'
})

function scrollToBottom() {
  nextTick(() => {
    const el = messagesRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

watch(() => messages.value.length, scrollToBottom)

/* ---------------- 拖动（Pointer Events） ---------------- */

function onDragStart(e: PointerEvent) {
  if (profile.value.disabled) return
  const startX = e.clientX
  const startY = e.clientY
  const origin = { ...(dragPos.value || store.position) }
  dragging.value = true
  const target = e.currentTarget as HTMLElement
  target.setPointerCapture(e.pointerId)

  const onMove = (ev: PointerEvent) => {
    const nextX = origin.x + (ev.clientX - startX)
    const nextY = origin.y + (ev.clientY - startY)
    const maxX = window.innerWidth - profile.value.size.w - 8
    const maxY = window.innerHeight - profile.value.size.h - 8
    dragPos.value = {
      x: Math.max(8, Math.min(nextX, Math.max(8, maxX))),
      y: Math.max(8, Math.min(nextY, Math.max(8, maxY)))
    }
  }

  const onEnd = () => {
    dragging.value = false
    target.removeEventListener('pointermove', onMove)
    target.removeEventListener('pointerup', onEnd)
    target.removeEventListener('pointercancel', onEnd)
    if (dragPos.value) store.setPos(store.currentKey, dragPos.value)
  }

  target.addEventListener('pointermove', onMove)
  target.addEventListener('pointerup', onEnd)
  target.addEventListener('pointercancel', onEnd)
}

/* ---------------- 交互 ---------------- */

function handleSend() {
  const text = inputText.value.trim()
  if (!text) return
  inputText.value = ''
  if (store.profile.disabled) return
  if (!store.isOpen) store.open()
  store.send(text)
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function handleChip(text: string, assistant: 'poetry' | 'system') {
  inputText.value = ''
  store.askWith(text, assistant)
}

function segmentsOf(content: string) {
  return renderAiLinks(content, renderableRoutes.value)
}

/* ---------------- 生命周期 ---------------- */

onMounted(() => {
  store.syncRoute(route.fullPath)
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
})

function onResize() {
  if (!dragPos.value) store.refreshViewport()
  else dragPos.value = null
}

watch(() => route.fullPath, path => {
  dragPos.value = null
  store.syncRoute(path)
})

watch(() => store.currentKey, () => {
  dragPos.value = null
})
</script>

<template>
  <Teleport to="body">
    <!-- 最小化后的悬浮胶囊 -->
    <button
      v-if="visible && minimized"
      class="ai-chat-mini"
      :style="{ left: `${store.position.x}px`, top: `${store.position.y}px`, ...profile.theme }"
      title="展开 AI 助手"
      @click="store.restore()"
    >
      <img :src="getAssistant(activeAssistant).avatar" alt="AI 助手" />
    </button>

    <Transition name="ai-window">
      <section
        v-if="visible && !minimized"
        class="ai-chat-window"
        :class="{ dragging }"
        :style="boxStyle"
      >
        <header class="ai-window-header" @pointerdown="onDragStart">
          <div class="header-left">
            <img class="header-avatar" :src="getAssistant(activeAssistant).avatar" alt="AI 助手" />
            <div class="header-titles">
              <span class="header-title">{{ getAssistant(activeAssistant).name }}</span>
              <span v-if="contextLabel" class="header-context">{{ contextLabel }}</span>
            </div>
          </div>
          <div class="header-right">
            <AiAssistantSwitch
              :model-value="activeAssistant"
              @update:model-value="store.setAssistant($event)"
            />
            <button class="icon-btn" title="复位到默认位置" @click.stop="store.resetPos()">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <path d="M3 12a9 9 0 1 0 3-6.7" /><polyline points="3 4 3 9 8 9" />
              </svg>
            </button>
            <button class="icon-btn" title="最小化" @click.stop="store.minimize()">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
            </button>
            <button class="icon-btn" title="关闭" @click.stop="store.close()">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="6" y1="6" x2="18" y2="18" /><line x1="18" y1="6" x2="6" y2="18" />
              </svg>
            </button>
          </div>
        </header>

        <div ref="messagesRef" class="ai-window-messages">
          <div v-if="!messages.length && greeting" class="ai-window-greeting">
            <img class="greeting-avatar" :src="getAssistant(activeAssistant).avatar" alt="AI 助手" />
            <p>{{ greeting }}</p>
          </div>

          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="chat-message"
            :class="msg.role"
          >
            <img
              v-if="msg.role === 'assistant'"
              class="message-avatar"
              :src="getAssistant(msg.assistant || activeAssistant).avatar"
              :alt="getAssistant(msg.assistant || activeAssistant).name"
            />
            <div v-else class="message-avatar user-avatar">我</div>
            <div class="message-body">
              <div v-if="msg.note" class="message-note">{{ msg.note }}</div>
              <div class="message-content">
                <template v-for="(seg, segIndex) in segmentsOf(msg.content)" :key="segIndex">
                  <router-link v-if="seg.type === 'link'" class="message-link" :to="seg.route!">
                    {{ seg.text }} →
                  </router-link>
                  <span v-else>{{ seg.text }}</span>
                </template>
              </div>
            </div>
          </div>

          <div v-if="loading" class="chat-message assistant">
            <img class="message-avatar" :src="getAssistant(activeAssistant).avatar" alt="AI 助手" />
            <div class="message-body">
              <div class="message-content loading">思考中...</div>
            </div>
          </div>
        </div>

        <div v-if="presetChips.length" class="ai-window-chips">
          <button
            v-for="chip in presetChips"
            :key="chip.text"
            class="chip"
            :disabled="loading"
            @click="handleChip(chip.text, chip.assistant)"
          >
            {{ chip.text }}
          </button>
        </div>

        <div class="ai-window-input">
          <textarea
            v-model="inputText"
            rows="2"
            placeholder="输入问题，Enter 发送，Shift+Enter 换行"
            :disabled="loading"
            @keydown="handleKeydown"
          ></textarea>
          <button class="send-btn" :disabled="loading || !inputText.trim()" @click="handleSend">
            <span v-if="loading" class="send-loading"></span>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" />
            </svg>
          </button>
        </div>
      </section>
    </Transition>

    <LoginPrompt
      :visible="loginPromptVisible"
      message="AI对话功能需要登录后才能使用"
      @update:visible="store.closeLoginPrompt()"
    />
  </Teleport>
</template>

<style scoped>
.ai-chat-window {
  position: fixed;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  background: var(--ai-bg, rgba(255, 253, 247, 0.97));
  border: 1px solid var(--ai-border, rgba(184, 134, 11, 0.45));
  border-radius: 14px;
  box-shadow: 0 18px 48px rgba(60, 34, 12, 0.28);
  overflow: hidden;
  backdrop-filter: blur(10px);
}

.ai-chat-window.dragging {
  opacity: 0.94;
  transition: none;
}

.ai-window-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  background: linear-gradient(135deg, var(--ai-accent, #8b4513) 0%, var(--ai-accent-2, #d2691e) 100%);
  color: var(--ai-header-text, #fff8ec);
  cursor: move;
  touch-action: none;
  user-select: none;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.header-avatar {
  width: 26px;
  height: 26px;
  max-width: none;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 215, 0, 0.7);
  background: #fff;
  flex-shrink: 0;
}

.header-titles {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.header-title {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.2;
}

.header-context {
  font-size: 11px;
  opacity: 0.85;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.icon-btn {
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.16);
  color: inherit;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.18s ease;
}

.icon-btn:hover {
  background: rgba(255, 255, 255, 0.32);
}

.ai-window-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ai-window-greeting {
  text-align: center;
  color: #8a6b4f;
  font-size: 13px;
  padding: 18px 8px 6px;
}

.greeting-avatar {
  width: 44px;
  height: 44px;
  max-width: none;
  border-radius: 50%;
  border: 2px solid rgba(255, 215, 0, 0.55);
  margin-bottom: 8px;
}

.chat-message {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.chat-message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 26px;
  height: 26px;
  max-width: none;
  border-radius: 50%;
  flex-shrink: 0;
  border: 1px solid rgba(184, 134, 11, 0.35);
  background: #fff;
}

.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--ai-accent, #8b4513), var(--ai-accent-2, #d2691e));
  color: #fff8ec;
  font-size: 11px;
}

.message-body {
  max-width: 78%;
  min-width: 0;
}

.message-note {
  font-size: 11px;
  color: var(--ai-accent, #8b4513);
  margin-bottom: 3px;
  opacity: 0.85;
}

.message-content {
  padding: 8px 10px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(184, 134, 11, 0.22);
  color: #3f3125;
}

.chat-message.user .message-content {
  background: rgba(139, 69, 19, 0.08);
  border-color: rgba(139, 69, 19, 0.22);
}

.message-content.loading {
  color: #a1887f;
}

.message-link {
  display: inline-block;
  margin: 2px 0;
  padding: 1px 6px;
  border-radius: 6px;
  background: rgba(255, 215, 0, 0.22);
  color: var(--ai-accent, #8b4513);
  font-weight: 600;
  text-decoration: none;
}

.message-link:hover {
  background: rgba(255, 215, 0, 0.4);
}

.ai-window-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 12px 8px;
}

.chip {
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--ai-border, rgba(184, 134, 11, 0.45));
  background: rgba(255, 255, 255, 0.85);
  color: #6b4a2c;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.chip:hover:not(:disabled) {
  background: rgba(255, 215, 0, 0.28);
  color: #5a3a1c;
}

.chip:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ai-window-input {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 10px 10px;
  border-top: 1px solid rgba(184, 134, 11, 0.22);
  background: rgba(255, 255, 255, 0.6);
}

.ai-window-input textarea {
  flex: 1;
  resize: none;
  border: 1px solid rgba(184, 134, 11, 0.35);
  border-radius: 8px;
  padding: 7px 9px;
  font-size: 13px;
  line-height: 1.5;
  background: #fff;
  color: #3f3125;
  font-family: inherit;
  outline: none;
}

.ai-window-input textarea:focus {
  border-color: var(--ai-accent, #8b4513);
}

.send-btn {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 9px;
  background: linear-gradient(135deg, var(--ai-accent, #8b4513), var(--ai-accent-2, #d2691e));
  color: #fff8ec;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.18s ease;
}

.send-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.send-loading {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: ai-spin 0.8s linear infinite;
}

@keyframes ai-spin {
  to {
    transform: rotate(360deg);
  }
}

.ai-chat-mini {
  position: fixed;
  z-index: 3000;
  width: 46px;
  height: 46px;
  padding: 0;
  border: 2px solid rgba(255, 215, 0, 0.75);
  border-radius: 50%;
  background: #fff;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(60, 34, 12, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-chat-mini img {
  width: 38px;
  height: 38px;
  max-width: none;
  border-radius: 50%;
}

.ai-window-enter-active,
.ai-window-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.ai-window-enter-from,
.ai-window-leave-to {
  opacity: 0;
  transform: scale(0.96);
}
</style>
