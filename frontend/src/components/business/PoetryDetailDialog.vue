<script setup lang="ts">
import { ref, watch, nextTick, onUnmounted } from 'vue'
import type { ApihzPoetryItem } from '@/api/modules/external-poetry'
import { getApihzPoetryDetail } from '@/api/modules/external-poetry'
import { getAiModuleConfig, type AiModuleConfig } from '@/api/modules/ai'
import { useUserStore } from '@/stores/user'
import LoginPrompt from '@/components/common/LoginPrompt.vue'

const props = defineProps<{
  visible: boolean
  title: string
  author: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const userStore = useUserStore()
const loading = ref(false)
const detail = ref<ApihzPoetryItem | null>(null)
const activeTab = ref('translation')

const detailSections = [
  { key: 'translation', label: '译文注释', fields: ['ywjzsy', 'ywjzse'] },
  { key: 'background', label: '创作背景', fields: ['czbj'] },
  { key: 'appreciation', label: '鉴赏赏析', fields: ['jsy', 'jse', 'sxy', 'sxe'] },
  { key: 'analysis', label: '深度解读', fields: ['jj', 'yj', 'xzsf', 'dj', 'jx'] },
  { key: 'evaluation', label: '评价', fields: ['pj'] },
]

const hasContent = (fields: string[]): boolean => {
  if (!detail.value) return false
  return fields.some(f => {
    const val = detail.value?.[f as keyof ApihzPoetryItem]
    return val && typeof val === 'string' && val.trim().length > 0
  })
}

const getContent = (fields: string[]): string => {
  if (!detail.value) return ''
  return fields
    .map(f => detail.value?.[f as keyof ApihzPoetryItem])
    .filter(v => v && typeof v === 'string' && v.trim().length > 0)
    .join('\n\n')
}

const parsePoemContent = (content: string): string[] => {
  if (!content) return []
  const cleaned = content
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n')
    .replace(/<\/div>/gi, '\n')
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&emsp;/g, '　')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')

  const lines = cleaned.split(/\n/).filter(line => line.trim())
  const sentences: string[] = []
  for (const line of lines) {
    const parts = line.split(/(?<=[。！？；!?;])/).filter(s => s.trim())
    if (parts.length === 0 && line.trim()) {
      sentences.push(line.trim())
    } else {
      sentences.push(...parts.map(s => s.trim()).filter(Boolean))
    }
  }
  return sentences
}

const poemSentences = ref<string[]>([])

watch(() => detail.value?.content, (content) => {
  if (content) {
    poemSentences.value = parsePoemContent(content)
  } else {
    poemSentences.value = []
  }
})

const resources = [
  { name: '古诗文网', url: 'https://www.gushiwen.org', desc: '经典古诗文鉴赏' },
  { name: '中华典藏', url: 'https://www.zhonghuadiancang.com', desc: '中华古籍全文检索' },
  { name: '中国哲学书电子化计划', url: 'https://ctext.org', desc: '先秦至清代文献' },
]

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}

const aiChatVisible = ref(false)
const aiChatMessages = ref<ChatMessage[]>([])
const aiChatInput = ref('')
const aiChatLoading = ref(false)
const aiChatContainerRef = ref<HTMLElement | null>(null)
const aiConfig = ref<AiModuleConfig | null>(null)
const loginPromptVisible = ref(false)
let aiChatInitialized = false

const buildAiPrompt = (userMessage: string, isFirst: boolean = false) => {
  const poemTitle = detail.value?.title || props.title || '该诗词'
  const poemAuthor = detail.value?.author || props.author || ''
  const poemContent = poemSentences.value.join('')

  const poemContext = `\n\n当前诗词：《${poemTitle}》${poemAuthor ? ' - ' + poemAuthor : ''}\n诗词内容：${poemContent}`

  let systemConstraint = ''
  if (aiConfig.value?.promptTemplate) {
    const maxLength = isFirst ? (aiConfig.value.firstResponseLength || 120) : (aiConfig.value.maxLength || 300)
    const styleHint = isFirst
      ? `这是首次提问，请用2-3句话简要概括即可，不超过${aiConfig.value.firstResponseLength || 120}字`
      : `根据问题复杂度适当展开，但不超过${maxLength}字`

    systemConstraint = aiConfig.value.promptTemplate
      .replace(/\{poetName\}/g, poemAuthor)
      .replace(/\{poemTitle\}/g, poemTitle)
      .replace(/\{poemContent\}/g, poemContent)
      .replace(/\{maxLength\}/g, String(maxLength))
      .replace(/\{styleHint\}/g, styleHint)

    if (!systemConstraint.includes(poemTitle)) {
      systemConstraint += poemContext
    }
  } else {
    const maxLength = isFirst ? 120 : 300
    systemConstraint = `你是一个文化渊博的大学者，精通古今中外诗歌知识。正在为用户讲解一首古诗。
当前诗词：《${poemTitle}》${poemAuthor ? ' - ' + poemAuthor : ''}
诗词内容：${poemContent}

回答要求：
1. 语言简洁精炼，避免冗余
2. 使用通俗易懂的中文
3. 重点突出，条理清晰
4. 不要使用markdown格式，直接输出纯文本
${isFirst ? '5. 这是首次提问，请用2-3句话简要概括即可' : '5. 根据问题复杂度适当展开，但不超过' + maxLength + '字'}`
  }

  return `【系统设定】${systemConstraint}\n\n【用户问题】${userMessage}`
}

const sendAiChatMessage = async (message?: string, isFirst: boolean = false) => {
  const msg = message || aiChatInput.value.trim()
  if (!msg || aiChatLoading.value) return

  if (!userStore.isLoggedIn) {
    loginPromptVisible.value = true
    return
  }

  if (!aiConfig.value) {
    try {
      const res = await getAiModuleConfig('poetry_chat')
      aiConfig.value = res.data
    } catch {
      console.error('获取AI配置失败')
    }
  }

  aiChatMessages.value.push({
    role: 'user',
    content: msg,
    timestamp: Date.now()
  })
  aiChatInput.value = ''
  aiChatLoading.value = true
  scrollToAiChatBottom()

  try {
    const { chat } = await import('@/api/modules/ai')
    const prompt = buildAiPrompt(msg, isFirst)
    const res = await chat({ message: prompt, moduleCode: 'poetry_chat' })
    aiChatMessages.value.push({
      role: 'assistant',
      content: res.data.reply || '抱歉，暂时无法回答',
      timestamp: Date.now()
    })
  } catch {
    aiChatMessages.value.push({
      role: 'assistant',
      content: 'AI服务暂时不可用，请稍后重试',
      timestamp: Date.now()
    })
  } finally {
    aiChatLoading.value = false
    scrollToAiChatBottom()
  }
}

const handleAiChatKeydown = (e: Event | KeyboardEvent) => {
  if ((e as KeyboardEvent).key === 'Enter' && !(e as KeyboardEvent).shiftKey) {
    e.preventDefault()
    sendAiChatMessage()
  }
}

const scrollToAiChatBottom = () => {
  nextTick(() => {
    if (aiChatContainerRef.value) {
      aiChatContainerRef.value.scrollTop = aiChatContainerRef.value.scrollHeight
    }
  })
}

const toggleAiChat = () => {
  aiChatVisible.value = !aiChatVisible.value
  if (aiChatVisible.value && !aiChatInitialized) {
    aiChatInitialized = true
    nextTick(() => {
      sendAiChatMessage('请简要介绍一下这首诗', true)
    })
  }
}

const fetchDetail = async () => {
  if (!props.title) return
  loading.value = true
  detail.value = null
  try {
    const result = await getApihzPoetryDetail(props.title)
    if (result) {
      detail.value = result
    }
  } catch (e) {
    console.error('获取诗词详情失败:', e)
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  emit('update:visible', false)
}

watch(() => props.visible, (val) => {
  if (val && props.title) {
    fetchDetail()
    activeTab.value = 'translation'
    aiChatVisible.value = false
    aiChatInitialized = false
  }
})

onUnmounted(() => {
  aiChatMessages.value = []
  aiChatInitialized = false
})
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="visible" class="poetry-dialog-overlay" @click.self="handleClose">
        <div class="poetry-dialog">
          <div class="dialog-header">
            <div class="dialog-title-area">
              <el-icon class="dialog-back" @click="handleClose"><ArrowLeft /></el-icon>
              <h2 class="dialog-title">{{ title }}</h2>
              <span class="dialog-author" v-if="author">{{ author }}</span>
            </div>
            <el-icon class="dialog-close" @click="handleClose"><Close /></el-icon>
          </div>

          <div class="dialog-body" v-loading="loading">
            <template v-if="detail">
              <div class="poem-original">
                <div class="poem-text">
                  <p
                    v-for="(sentence, idx) in poemSentences"
                    :key="idx"
                    class="poem-sentence"
                  >{{ sentence }}</p>
                </div>
              </div>

              <div class="detail-tabs">
                <a
                  v-for="section in detailSections"
                  :key="section.key"
                  v-show="hasContent(section.fields)"
                  :class="['tab-item', { active: activeTab === section.key }]"
                  @click="activeTab = section.key"
                >
                  {{ section.label }}
                </a>
              </div>

              <div class="detail-content">
                <div
                  v-for="section in detailSections"
                  :key="section.key"
                  v-show="activeTab === section.key && hasContent(section.fields)"
                  class="content-section"
                >
                  <p class="content-text" style="white-space: pre-line;">{{ getContent(section.fields) }}</p>
                </div>
              </div>

              <div class="ai-assistant-section">
                <div class="ai-toggle-bar" @click="toggleAiChat">
                  <div class="ai-toggle-left">
                    <el-icon class="ai-toggle-icon"><MagicStick /></el-icon>
                    <span class="ai-toggle-label">AI诗词助手</span>
                    <span class="ai-toggle-hint">{{ aiChatVisible ? '收起' : '与AI聊聊这首诗' }}</span>
                  </div>
                  <el-icon class="ai-toggle-arrow">
                    <ArrowUp v-if="aiChatVisible" />
                    <ArrowDown v-else />
                  </el-icon>
                </div>

                <Transition name="ai-chat-slide">
                  <div v-if="aiChatVisible" class="ai-chat-area">
                    <div class="ai-chat-messages" ref="aiChatContainerRef">
                      <div
                        v-for="(msg, index) in aiChatMessages"
                        :key="index"
                        :class="['ai-chat-message', msg.role]"
                      >
                        <div v-if="msg.role === 'assistant'" class="ai-msg-avatar">
                          <span>AI</span>
                        </div>
                        <div class="ai-msg-body">
                          <div class="ai-msg-content">{{ msg.content }}</div>
                        </div>
                        <div v-if="msg.role === 'user'" class="user-msg-avatar">
                          <span>我</span>
                        </div>
                      </div>
                      <div v-if="aiChatLoading" class="ai-chat-message assistant">
                        <div class="ai-msg-avatar"><span>AI</span></div>
                        <div class="ai-msg-body">
                          <div class="ai-msg-content ai-msg-loading">
                            <span class="loading-dot"></span>
                            <span class="loading-dot"></span>
                            <span class="loading-dot"></span>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div class="ai-chat-input-area">
                      <el-input
                        v-model="aiChatInput"
                        type="textarea"
                        :rows="2"
                        :autosize="{ minRows: 1, maxRows: 4 }"
                        placeholder="输入问题，按Enter发送，Shift+Enter换行..."
                        @keydown="handleAiChatKeydown"
                        :disabled="aiChatLoading"
                        resize="none"
                      />
                      <el-button
                        class="ai-send-btn"
                        @click="sendAiChatMessage()"
                        :loading="aiChatLoading"
                        :disabled="!aiChatInput.trim()"
                      >
                        <el-icon><Promotion /></el-icon>
                      </el-button>
                    </div>
                  </div>
                </Transition>
              </div>

              <div class="resource-section">
                <h4 class="resource-title">相关资源</h4>
                <div class="resource-list">
                  <a
                    v-for="res in resources"
                    :key="res.name"
                    :href="res.url"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="resource-item"
                  >
                    <el-icon class="resource-icon"><Link /></el-icon>
                    <div class="resource-info">
                      <span class="resource-name">{{ res.name }}</span>
                      <span class="resource-desc">{{ res.desc }}</span>
                    </div>
                    <el-icon class="resource-arrow"><ArrowRight /></el-icon>
                  </a>
                </div>
              </div>
            </template>

            <el-empty v-else-if="!loading" description="暂未收录该诗词的详细赏析" />
          </div>
        </div>
      </div>
    </Transition>

    <LoginPrompt
      v-model:visible="loginPromptVisible"
      message="使用AI诗词助手需要先登录"
    />
  </Teleport>
</template>

<style scoped lang="scss">
@use '@/assets/styles/variables' as *;
@use '@/assets/styles/mixins' as *;

.poetry-dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(2px);
  z-index: 2000;
  @include flex-center;
}

.poetry-dialog {
  width: 680px;
  max-width: 92vw;
  max-height: 82vh;
  background: $background-color-light;
  border-radius: $border-radius-lg;
  box-shadow: $box-shadow-xl;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dialog-header {
  @include flex-between;
  padding: $spacing-lg $spacing-lg $spacing-md;
  border-bottom: 1px solid $border-color-light;
}

.dialog-title-area {
  display: flex;
  align-items: baseline;
  gap: $spacing-md;
}

.dialog-back {
  cursor: pointer;
  font-size: 20px;
  color: $text-color-secondary;
  padding: $spacing-xs;
  border-radius: $border-radius-sm;
  transition: all $transition-fast;

  &:hover {
    color: $primary-color;
    background: rgba($primary-color, 0.06);
  }
}

.dialog-title {
  font-size: $font-size-xxl;
  font-family: $font-family-title;
  color: $primary-color;
  margin: 0;
}

.dialog-author {
  font-size: $font-size-base;
  color: $text-color-light;
}

.dialog-close {
  cursor: pointer;
  font-size: 20px;
  color: $text-color-light;
  padding: $spacing-xs;
  border-radius: $border-radius-sm;
  transition: all $transition-fast;

  &:hover {
    color: $danger-color;
    background: rgba($danger-color, 0.06);
  }
}

.dialog-body {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-lg;
  @include scrollbar;
}

.poem-original {
  background: linear-gradient(135deg, rgba($primary-color, 0.03), rgba($secondary-color, 0.02));
  border-radius: $border-radius-md;
  padding: $spacing-lg $spacing-lg;
  margin-bottom: $spacing-lg;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: $spacing-md;
    right: $spacing-md;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba($primary-color, 0.2), transparent);
  }

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: $spacing-md;
    right: $spacing-md;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba($primary-color, 0.2), transparent);
  }
}

.poem-text {
  text-align: center;
}

.poem-sentence {
  font-size: $font-size-lg;
  color: $text-color;
  line-height: 2.2;
  letter-spacing: 2px;
  margin: 0;
  font-family: $font-family-title;
  overflow-wrap: break-word;
  word-break: break-all;
  max-width: 100%;
}

.detail-tabs {
  display: flex;
  gap: $spacing-xs;
  border-bottom: 1px solid $border-color-light;
  margin-bottom: $spacing-lg;
  overflow-x: auto;
}

.tab-item {
  padding: $spacing-sm $spacing-md;
  font-size: $font-size-sm;
  color: $text-color-secondary;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
  transition: all $transition-fast;

  &:hover {
    color: $primary-color;
  }

  &.active {
    color: $primary-color;
    border-bottom-color: $primary-color;
    font-weight: 600;
  }
}

.detail-content {
  min-height: 120px;
}

.content-section {
  animation: fadeIn 0.2s ease;
}

.content-text {
  font-size: $font-size-base;
  color: $text-color;
  line-height: 1.9;
  text-indent: 2em;
  margin: 0;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.ai-assistant-section {
  margin-top: $spacing-xl;
  border-top: 1px solid $border-color-light;
  padding-top: $spacing-lg;
}

.ai-toggle-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md $spacing-lg;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: $border-radius-md;
  cursor: pointer;
  transition: all $transition-base;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.25);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.35);
  }
}

.ai-toggle-left {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.ai-toggle-icon {
  font-size: 18px;
  color: #fff;
}

.ai-toggle-label {
  font-size: $font-size-base;
  font-weight: 600;
  color: #fff;
}

.ai-toggle-hint {
  font-size: $font-size-sm;
  color: rgba(255, 255, 255, 0.7);
  margin-left: $spacing-xs;
}

.ai-toggle-arrow {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
}

.ai-chat-slide-enter-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  max-height: 500px;
  overflow: hidden;
}

.ai-chat-slide-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.ai-chat-slide-enter-from,
.ai-chat-slide-leave-to {
  max-height: 0;
  opacity: 0;
  transform: translateY(-8px);
}

.ai-chat-area {
  margin-top: $spacing-md;
  border: 1px solid $border-color-light;
  border-radius: $border-radius-md;
  overflow: hidden;
  background: $background-color-light;
}

.ai-chat-messages {
  height: 320px;
  overflow-y: auto;
  padding: $spacing-md;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  @include scrollbar;
}

.ai-chat-message {
  display: flex;
  gap: $spacing-sm;
  max-width: 90%;
  animation: msgSlideIn 0.25s ease;

  &.user {
    align-self: flex-end;
    flex-direction: row;

    .ai-msg-body {
      align-items: flex-end;
    }

    .ai-msg-content {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      border-radius: $border-radius-md $border-radius-md $border-radius-sm $border-radius-md;
    }
  }

  &.assistant {
    align-self: flex-start;

    .ai-msg-content {
      background: #f4f5f7;
      color: $text-color;
      border-radius: $border-radius-md $border-radius-md $border-radius-md $border-radius-sm;
    }
  }
}

.ai-msg-avatar,
.user-msg-avatar {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $font-size-xs;
  font-weight: 600;
  margin-top: 2px;
}

.ai-msg-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.user-msg-avatar {
  background: $primary-color;
  color: #fff;
}

.ai-msg-body {
  display: flex;
  flex-direction: column;
}

.ai-msg-content {
  padding: $spacing-sm $spacing-md;
  font-size: $font-size-sm;
  line-height: 1.7;
  word-break: break-word;
  white-space: pre-line;
}

.ai-msg-loading {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: $spacing-sm $spacing-lg;
}

.loading-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: $text-color-light;
  animation: dotPulse 1.2s ease-in-out infinite;

  &:nth-child(2) { animation-delay: 0.2s; }
  &:nth-child(3) { animation-delay: 0.4s; }
}

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

@keyframes msgSlideIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.ai-chat-input-area {
  display: flex;
  align-items: flex-end;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  border-top: 1px solid $border-color-light;
  background: rgba($primary-color, 0.01);

  .el-textarea {
    flex: 1;

    @include el-comment-input;

    :deep(.el-textarea__inner) {
      border-radius: $comment-input-radius;
      padding: $comment-input-padding;
    }
  }
}

.ai-send-btn {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  padding: 0;
  border-radius: $border-radius-md;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: #fff;

  &:hover:not(:disabled) {
    opacity: 0.9;
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.resource-section {
  margin-top: $spacing-xl;
  padding-top: $spacing-lg;
  border-top: 1px solid $border-color-light;
}

.resource-title {
  font-size: $font-size-sm;
  font-weight: 600;
  color: $text-color-secondary;
  margin: 0 0 $spacing-md;
  letter-spacing: 1px;
}

.resource-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.resource-item {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-sm $spacing-md;
  border-radius: $border-radius-sm;
  text-decoration: none;
  transition: all $transition-fast;

  &:hover {
    background: rgba($primary-color, 0.04);

    .resource-name {
      color: $primary-color;
    }

    .resource-arrow {
      transform: translateX(2px);
      opacity: 1;
    }
  }
}

.resource-icon {
  font-size: 16px;
  color: $primary-color;
  flex-shrink: 0;
}

.resource-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.resource-name {
  font-size: $font-size-sm;
  color: $text-color;
  font-weight: 500;
  transition: color $transition-fast;
}

.resource-desc {
  font-size: $font-size-xs;
  color: $text-color-light;
}

.resource-arrow {
  font-size: 14px;
  color: $text-color-light;
  opacity: 0.5;
  transition: all $transition-fast;
}

.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.25s ease;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}
</style>
