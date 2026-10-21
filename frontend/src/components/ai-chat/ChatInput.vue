<script setup lang="ts">
import type { AiSkill } from '@/configs/ai-chat/skills'

const props = defineProps<{
  modelValue: string
  loading: boolean
  placeholder: string
  activeSkill: AiSkill | null
  /** 完整对局页路径；为空表示当前能力没有独立对局页 */
  gamePath?: string
  /** 完整对局入口文案 */
  gameLabel?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  send: []
  clearSkill: []
  openGame: []
}>()

const onInput = (e: Event) => {
  emit('update:modelValue', (e.target as HTMLTextAreaElement).value)
}

// Enter 发送、Shift+Enter 换行；输入法组合期间不发送
const onKeydown = (e: KeyboardEvent) => {
  if (e.key !== 'Enter' || e.shiftKey || e.isComposing) return
  e.preventDefault()
  emit('send')
}

const onSend = () => {
  if (!props.modelValue.trim() || props.loading) return
  emit('send')
}
</script>

<template>
  <div class="chat-input-wrap">
    <div v-if="activeSkill" class="chat-active-skill">
      <span class="chat-active-skill-text">{{ activeSkill.icon }} 已选能力：{{ activeSkill.name }}</span>
      <button
        v-if="gamePath"
        class="chat-active-skill-link"
        @click="emit('openGame')"
      >{{ gameLabel || '完整对局' }} →</button>
      <button class="chat-active-skill-clear" title="取消能力" @click="emit('clearSkill')">&times;</button>
    </div>
    <div class="chat-input-row">
      <textarea
        class="chat-textarea"
        rows="2"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="loading"
        @input="onInput"
        @keydown="onKeydown"
      ></textarea>
      <button
        class="chat-send-btn"
        :class="{ loading }"
        :disabled="loading || !modelValue.trim()"
        @click="onSend"
      >
        <span v-if="loading" class="chat-send-spinner"></span>
        <span>{{ loading ? '生成中' : '发送' }}</span>
      </button>
    </div>
    <div class="chat-input-hint">Enter 发送 / Shift+Enter 换行</div>
  </div>
</template>

<style scoped lang="scss">
.chat-input-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-active-skill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  align-self: flex-start;
  padding: 4px 10px;
  border: 1px solid #b34a31;
  border-radius: 999px;
  background: #fdf6ee;
  font-size: 12px;
  color: #b34a31;
}

.chat-active-skill-link {
  padding: 0 2px;
  border: none;
  background: transparent;
  color: #8b4513;
  font-size: 12px;
  line-height: 1;
  text-decoration: underline;
  text-underline-offset: 3px;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.2s ease;
}

.chat-active-skill-link:hover {
  color: #b34a31;
}

.chat-active-skill-clear {
  border: none;
  background: transparent;
  color: #b34a31;
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
  padding: 0;
}

.chat-input-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.chat-textarea {
  flex: 1;
  min-height: 56px;
  max-height: 140px;
  padding: 10px 14px;
  border: 1px solid #d4c5a9;
  border-radius: 10px;
  background: #fffcf7;
  color: #333;
  font-size: 14px;
  line-height: 1.6;
  font-family: inherit;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.chat-textarea:focus {
  border-color: #b34a31;
  box-shadow: 0 0 0 3px rgba(139, 69, 19, 0.12);
}

.chat-textarea::placeholder {
  color: #a09585;
}

.chat-textarea:disabled {
  background: #f5efe6;
  cursor: not-allowed;
}

.chat-send-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 96px;
  height: 56px;
  flex-shrink: 0;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #8b4513, #a0522d);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
  cursor: pointer;
  transition: opacity 0.2s ease, transform 0.2s ease;
  font-family: inherit;
}

.chat-send-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #a0522d, #8b4513);
}

.chat-send-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.chat-send-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.chat-send-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: chat-spin 0.8s linear infinite;
}

@keyframes chat-spin {
  to {
    transform: rotate(360deg);
  }
}

.chat-input-hint {
  font-size: 12px;
  color: #a09585;
  text-align: right;
}
</style>
