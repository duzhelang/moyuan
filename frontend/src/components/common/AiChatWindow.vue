<script setup lang="ts">
/**
 * 全站 AI 小窗（全局单例，跨路由不销毁）
 * 位置、尺寸、主题、模块码、预设问题全部由路由画像驱动；
 * 内置两个轻量助理（诗词助理 / 网页用法助理），消息分区隔离。
 */
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAiChatStore } from '@/stores/aiChat'
import { getAssistant } from '@/configs/ai-chat/assistants'
import { renderAiLinks } from '@/utils/renderAiLinks'
import AiAssistantSwitch from './AiAssistantSwitch.vue'
import LoginPrompt from './LoginPrompt.vue'

const route = useRoute()
const router = useRouter()
const store = useAiChatStore()
const { isOpen, minimized, loginPromptVisible, messages, loading, presetChips, greeting, activeAssistant, context, renderableRoutes, routeTitles, pendingSwitch, profile, currentSize, dockEl, detached } =
  storeToRefs(store)

const inputText = ref('')
const messagesRef = ref<HTMLElement | null>(null)
/** 拖动过程中的临时位置；为空时使用画像默认位置 */
const dragPos = ref<{ x: number; y: number } | null>(null)
const dragging = ref(false)
/** 缩放过程中的临时尺寸；结束后写回 store */
const resizeSize = ref<{ w: number; h: number } | null>(null)
const resizing = ref(false)
/** 小窗尺寸下限 */
const MIN_W = 300
const MIN_H = 340

/** 页内嵌入模式：窗口由页面容器承载，不使用 fixed 浮层定位 */
const docked = computed(() => !!profile.value.dock)
/** 容器是否真实可见：窄屏下右栏被隐藏时自动回退为固定浮窗 */
const dockUsable = ref(false)
let dockObserver: ResizeObserver | null = null

function measureDock() {
  const el = dockEl.value
  let usable = !!el
  let node: HTMLElement | null = el
  while (usable && node && node !== document.body && node !== document.documentElement) {
    // 页面自身的折叠（v-show 内联 display:none）不算不可用：展开后仍由该容器承载
    if (node.style.display === 'none') break
    const style = getComputedStyle(node)
    // 响应式布局隐藏整栏（如窄屏右栏 display:none）→ 回退为右下浮窗
    if (style.display === 'none' || style.visibility === 'hidden') usable = false
    node = node.parentElement
  }
  dockUsable.value = usable
  store.setDockUsable(usable)
}

watch(
  dockEl,
  el => {
    dockObserver?.disconnect()
    dockObserver = null
    measureDock()
    if (!el) return
    dockObserver = new ResizeObserver(measureDock)
    dockObserver.observe(el)
  },
  { immediate: true }
)

/** 实际生效的嵌入模式：容器不可用或已拖动脱离时回退为浮窗 */
const effectiveDocked = computed(() => docked.value && dockUsable.value && !detached.value)
/** dock 页面需等容器注册后再渲染，避免窗口先闪在 body 上再跳进右栏 */
const dockReady = computed(() => !docked.value || !!dockEl.value)
const teleportTarget = computed<Element | string>(() =>
  effectiveDocked.value && dockEl.value ? dockEl.value : 'body'
)

const visible = computed(() => isOpen.value && !profile.value.disabled && dockReady.value)

const activeSize = computed(() => resizeSize.value || currentSize.value)

const boxStyle = computed(() => {
  if (effectiveDocked.value) {
    // 嵌入模式宽高由 CSS 与页面容器决定，不写 left/top，避免 fixed 坐标残留
    return { ...profile.value.theme }
  }
  const pos = dragPos.value || store.position
  const size = activeSize.value
  return {
    left: `${pos.x}px`,
    top: `${pos.y}px`,
    width: `${size.w}px`,
    height: `${size.h}px`,
    ...profile.value.theme
  }
})

/**
 * 最小化小圆标固定在视口右下角，与入口 FAB 同一锚点。
 * 不能沿用窗口左上角坐标：默认窗口高 460~500，矮视口下左上角会落在屏幕上方，导致小圆标悬空。
 */
const miniStyle = computed(() => {
  const { anchor, dx, dy } = profile.value.entry
  const styleMap: Record<string, string> = {}
  if (anchor.startsWith('right')) styleMap.right = `${dx}px`
  else styleMap.left = `${dx}px`
  if (anchor.endsWith('bottom')) styleMap.bottom = `${dy}px`
  else styleMap.top = `${dy}px`
  return {
    ...styleMap,
    ...profile.value.theme
  }
})

const contextLabel = computed(() => {
  const ctx = context.value
  if (!ctx || ctx.type === 'none') return ''
  if (ctx.type === 'poet') return ctx.name ? `诗人 · ${ctx.name}` : '诗人'
  return ctx.name ? `诗词 · ${ctx.name}` : '诗词'
})

/** 页面专属外观：诗人详情页复刻原右栏面板，诗词详情页复刻原右下对话弹窗 */
const variantClass = computed(() => {
  if (profile.value.key === 'poet:detail') return 'variant-poet'
  if (profile.value.key === 'poem:detail') return 'variant-poem'
  return ''
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
  // 头部的按钮（助理切换/复位/最小化/关闭）不参与拖动，避免吞掉点击
  if ((e.target as HTMLElement).closest('button')) return

  const startX = e.clientX
  const startY = e.clientY
  let origin = { ...(dragPos.value || store.position) }

  // 卡内展开状态下拖动：先按当前视觉矩形脱离卡片转为浮窗，再继续跟手，避免跳位
  if (effectiveDocked.value) {
    const win = (e.currentTarget as HTMLElement).closest('.ai-chat-window') as HTMLElement | null
    const rect = win?.getBoundingClientRect()
    if (rect) {
      origin = { x: Math.round(rect.left), y: Math.round(rect.top) }
      resizeSize.value = { w: Math.round(rect.width), h: Math.round(rect.height) }
      dragPos.value = origin
      store.setDetached(true)
    }
  }

  dragging.value = true

  const onMove = (ev: PointerEvent) => {
    const nextX = origin.x + (ev.clientX - startX)
    const nextY = origin.y + (ev.clientY - startY)
    const maxX = window.innerWidth - activeSize.value.w - 8
    const maxY = window.innerHeight - activeSize.value.h - 8
    dragPos.value = {
      x: Math.max(8, Math.min(nextX, Math.max(8, maxX))),
      y: Math.max(8, Math.min(nextY, Math.max(8, maxY)))
    }
  }

  // 脱离时 Teleport 会移动节点，指针捕获会失效，这里统一监听 window
  const onEnd = () => {
    dragging.value = false
    window.removeEventListener('pointermove', onMove)
    window.removeEventListener('pointerup', onEnd)
    window.removeEventListener('pointercancel', onEnd)
    if (dragPos.value) store.setPos(store.currentKey, dragPos.value)
  }

  window.addEventListener('pointermove', onMove)
  window.addEventListener('pointerup', onEnd)
  window.addEventListener('pointercancel', onEnd)
}

/* ---------------- 缩放（右下角手柄） ---------------- */

function onResizeStart(e: PointerEvent) {
  if (profile.value.disabled || effectiveDocked.value) return
  e.preventDefault()
  const startX = e.clientX
  const startY = e.clientY
  const origin = { ...activeSize.value }
  const originPos = { ...(dragPos.value || store.position) }
  resizing.value = true
  const target = e.currentTarget as HTMLElement
  target.setPointerCapture(e.pointerId)

  const onMove = (ev: PointerEvent) => {
    // 可用空间随窗口左上角位置变化，避免缩放后超出视口
    const maxW = window.innerWidth - originPos.x - 8
    const maxH = window.innerHeight - originPos.y - 8
    const lowerW = Math.min(MIN_W, maxW)
    const lowerH = Math.min(MIN_H, maxH)
    resizeSize.value = {
      w: Math.round(Math.max(lowerW, Math.min(origin.w + (ev.clientX - startX), maxW))),
      h: Math.round(Math.max(lowerH, Math.min(origin.h + (ev.clientY - startY), maxH)))
    }
  }

  const onEnd = () => {
    resizing.value = false
    target.removeEventListener('pointermove', onMove)
    target.removeEventListener('pointerup', onEnd)
    target.removeEventListener('pointercancel', onEnd)
    if (resizeSize.value) {
      store.setSize(store.currentKey, resizeSize.value)
      resizeSize.value = null
    }
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

/** 路由表中真实存在的静态路径，作为知识库白名单未就绪时的跳转兜底 */
const knownRoutes = computed(() =>
  Array.from(
    new Set(
      router
        .getRoutes()
        .map(item => item.path)
        .filter(path => path.startsWith('/') && !path.includes(':') && path !== '/')
    )
  )
)

function segmentsOf(content: string) {
  // 知识库白名单优先；未就绪时回退到路由表中真实存在的静态路径，保证历史回答里的跳转链接仍可点击
  const allowed = renderableRoutes.value.length ? renderableRoutes.value : knownRoutes.value
  return renderAiLinks(content, allowed, routeTitles.value)
}

/** 回答中的跳转入口：显式路由导航，避免 Teleport 场景下链接行为异常 */
function openRoute(target?: string) {
  if (!target) return
  router.push(target)
}

/** 恢复默认位置与尺寸：同时清掉拖动/缩放的临时值，避免覆盖画像默认值 */
function handleReset() {
  resizeSize.value = null
  store.resetPos()
  if (profile.value.dock) {
    // 卡内页复位：重新回到紫色卡片内展开（拖动位置随卡片定位失效）
    store.setDetached(false)
    return
  }
  dragPos.value = null
}

/** 最小化：卡内展开的页面回到紫色卡片（保留会话），其余页面收为右下小圆标 */
function handleMinimize() {
  if (profile.value.dock) store.hide()
  else store.minimize()
}

/* ---------------- 生命周期 ---------------- */

onMounted(() => {
  store.syncRoute(route.fullPath)
  // 预热知识库：刷新后历史回答中的跳转链接依赖知识库白名单，不能等用户再次提问才加载
  void store.loadKnowledge()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  dockObserver?.disconnect()
  dockObserver = null
})

function onResize() {
  measureDock()
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
  <Teleport :to="teleportTarget">
    <!-- 最小化后的悬浮小圆标（仅浮动模式） -->
    <button
      v-if="visible && minimized && !effectiveDocked"
      class="ai-chat-mini"
      :style="miniStyle"
      title="展开 AI 助手"
      @click="store.restore()"
    >
      <img :src="getAssistant(activeAssistant).avatar" alt="AI 助手" />
    </button>

    <Transition name="ai-window">
      <section
        v-if="visible && !minimized"
        class="ai-chat-window"
        :class="[variantClass, { dragging, resizing, docked: effectiveDocked }]"
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
            <button v-if="!effectiveDocked" class="icon-btn" title="复位到默认位置" @click.stop="handleReset()">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <path d="M3 12a9 9 0 1 0 3-6.7" /><polyline points="3 4 3 9 8 9" />
              </svg>
            </button>
            <button v-if="!effectiveDocked" class="icon-btn" title="最小化" @click.stop="handleMinimize()">
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
                  <a v-if="seg.type === 'link'" class="message-link" @click.prevent="openRoute(seg.route)">
                    {{ seg.text }} →
                  </a>
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

        <div v-if="pendingSwitch" class="ai-window-switch-tip">
          <span class="switch-text">这更像是{{ getAssistant(pendingSwitch.to).name }}的问题，是否切换到{{ getAssistant(pendingSwitch.to).name }}？</span>
          <div class="switch-actions">
            <button class="switch-btn" @click="store.confirmSwitchAndSend()">切换并发送</button>
            <button class="switch-btn ghost" @click="store.keepAssistantAndSend()">继续用{{ getAssistant(pendingSwitch.from).name }}</button>
          </div>
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

        <!-- 右下角缩放柄（仅浮动模式） -->
        <div v-if="!effectiveDocked" class="ai-window-resize" title="拖拽调整大小" @pointerdown.stop="onResizeStart"></div>
      </section>
    </Transition>
  </Teleport>

  <Teleport to="body">
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
  /* 不能裁切子元素：助理切换的能力提示要浮在头部上方 */
  overflow: visible;
  backdrop-filter: blur(10px);
}

/* 页内嵌入：宽度跟随页面容器，取消 fixed 浮层与拖拽定位 */
.ai-chat-window.docked {
  position: static;
  width: 100%;
  height: 420px;
  max-height: 70vh;
  box-shadow: none;
}

.ai-chat-window.docked .ai-window-header {
  cursor: move;
}

.ai-chat-window.dragging {
  opacity: 0.94;
  transition: none;
}

.ai-chat-window.resizing {
  transition: none;
  user-select: none;
}

/* 右下角缩放柄：三条斜线提示可拖拽 */
.ai-window-resize {
  position: absolute;
  right: 2px;
  bottom: 2px;
  width: 14px;
  height: 14px;
  cursor: nwse-resize;
  touch-action: none;
  z-index: 5;
}

.ai-window-resize::before,
.ai-window-resize::after {
  content: '';
  position: absolute;
  background: var(--ai-accent, #8b4513);
  opacity: 0.55;
  border-radius: 1px;
  transform: rotate(-45deg);
}

.ai-window-resize::before {
  right: 2px;
  bottom: 6px;
  width: 9px;
  height: 1.5px;
}

.ai-window-resize::after {
  right: 1px;
  bottom: 2px;
  width: 6px;
  height: 1.5px;
}

.ai-window-resize:hover::before,
.ai-window-resize:hover::after {
  opacity: 0.95;
}

.ai-window-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  background: linear-gradient(135deg, var(--ai-accent, #8b4513) 0%, var(--ai-accent-2, #d2691e) 100%);
  color: var(--ai-header-text, #fff8ec);
  border-radius: 14px 14px 0 0;
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
  cursor: pointer;
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

.ai-window-switch-tip {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0 10px 8px;
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px solid rgba(184, 134, 11, 0.4);
  background: rgba(255, 215, 0, 0.14);
}

.switch-text {
  font-size: 12px;
  color: #6b4a2c;
  line-height: 1.5;
}

.switch-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.switch-btn {
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--ai-accent, #8b4513);
  background: var(--ai-accent, #8b4513);
  color: #fff8ec;
  font-size: 12px;
  cursor: pointer;
}

.switch-btn.ghost {
  background: transparent;
  color: var(--ai-accent, #8b4513);
}

.ai-window-input {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 10px 10px;
  border-top: 1px solid rgba(184, 134, 11, 0.22);
  background: rgba(255, 255, 255, 0.6);
  border-radius: 0 0 14px 14px;
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

/* ============================================================
   页面专属外观：诗人详情页（复刻原右侧 AI 面板）
   白色玻璃底 + 淡紫头部 + 原气泡与输入区配色
   ============================================================ */
.ai-chat-window.variant-poet {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
}

.variant-poet .ai-window-header {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  color: #333333;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px 12px 0 0;
}

.variant-poet .header-avatar {
  border-color: rgba(118, 75, 162, 0.45);
}

.variant-poet .icon-btn {
  background: rgba(102, 126, 234, 0.12);
  color: #5a5a7a;
}

.variant-poet .icon-btn:hover {
  background: rgba(102, 126, 234, 0.24);
}

.variant-poet .ai-window-messages {
  padding: 12px 16px;
}

.variant-poet .ai-window-greeting {
  color: #666666;
}

.variant-poet .message-content {
  background: #f4f4f5;
  border: none;
  border-radius: 18px 18px 18px 4px;
  padding: 10px 14px;
  color: #333333;
}

.variant-poet .chat-message.user .message-content {
  background: linear-gradient(135deg, #8b4513 0%, #602f0d 100%);
  border: none;
  border-radius: 18px 18px 4px 18px;
  color: #ffffff;
}

.variant-poet .ai-window-input {
  background: linear-gradient(to bottom, #fafafa, #f5f5f5);
  border-top-color: #e8e8e8;
  border-radius: 0 0 12px 12px;
}

.variant-poet .send-btn {
  border-radius: 20px;
}

/* ============================================================
   页面专属外观：诗词详情页（复刻原右下对话弹窗）
   $surface-primary 底 + 棕金头部 + 原气泡圆角与头像尺寸
   ============================================================ */
.ai-chat-window.variant-poem {
  background: #fffcf7;
  border: 1px solid rgba(212, 197, 169, 0.5);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
}

.variant-poem .ai-window-header {
  background: linear-gradient(135deg, #8b4513 0%, #602f0d 100%);
  border-radius: 12px 12px 0 0;
}

.variant-poem .ai-window-messages {
  padding: 16px;
  gap: 12px;
}

.variant-poem .message-avatar {
  width: 32px;
  height: 32px;
}

.variant-poem .user-avatar {
  font-size: 12px;
}

.variant-poem .message-content {
  background: #f8f4ee;
  border: none;
  border-radius: 18px 18px 18px 4px;
  padding: 10px 14px;
}

.variant-poem .chat-message.user .message-content {
  background: linear-gradient(135deg, #c97d3a 0%, #8b4513 100%);
  border: none;
  border-radius: 18px 18px 4px 18px;
  color: #ffffff;
}

.variant-poem .ai-window-input {
  background: linear-gradient(to bottom, #fffcf7, #f8f4ee);
  border-top-color: rgba(212, 197, 169, 0.7);
  border-radius: 0 0 12px 12px;
}

.variant-poem .send-btn {
  border-radius: 20px;
}
</style>
