<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'

const props = withDefaults(defineProps<{
  disabled?: boolean
  size?: 'full' | 'mini'
}>(), {
  disabled: false,
  size: 'full'
})

const emit = defineEmits<{
  (e: 'result', text: string): void
  (e: 'error'): void
}>()

/** 语音录制上限（秒），全程计入答题 30 秒 */
const VOICE_LIMIT_SECONDS = 20

const SpeechRec: any =
  (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
/** 能力探测：不支持则隐藏入口 */
const supported = computed(() => !!SpeechRec)

const recording = ref(false)
const elapsed = ref(0)
let recognition: any = null
let limitTimer: ReturnType<typeof setInterval> | null = null

function clearTimer() {
  if (limitTimer) {
    clearInterval(limitTimer)
    limitTimer = null
  }
}

function stop() {
  clearTimer()
  if (recognition) {
    try {
      recognition.stop()
    } catch {
      // 忽略重复停止
    }
  }
}

function start() {
  if (!supported.value || props.disabled || recording.value) return
  try {
    recognition = new SpeechRec()
  } catch {
    emit('error')
    return
  }
  recognition.lang = 'zh-CN'
  recognition.interimResults = false
  recognition.maxAlternatives = 1
  recognition.onresult = (event: any) => {
    const text: string = event?.results?.[0]?.[0]?.transcript || ''
    if (text.trim()) emit('result', text.trim())
    else emit('error')
  }
  recognition.onerror = () => emit('error')
  recognition.onend = () => {
    recording.value = false
    clearTimer()
  }

  recording.value = true
  elapsed.value = 0
  recognition.start()
  // 录制满 20 秒自动结束并提交
  limitTimer = setInterval(() => {
    elapsed.value++
    if (elapsed.value >= VOICE_LIMIT_SECONDS) stop()
  }, 1000)
}

function toggle() {
  if (recording.value) stop()
  else start()
}

onUnmounted(() => stop())
</script>

<template>
  <button
    v-if="supported"
    class="voice-btn"
    :class="[`is-${size}`, { 'is-recording': recording }]"
    type="button"
    :disabled="disabled"
    @click="toggle"
  >
    <span class="voice-icon">🎙</span>
    <span v-if="recording" class="voice-text">{{ elapsed }}s / {{ VOICE_LIMIT_SECONDS }}s</span>
    <span v-else class="voice-text">语音吟诗</span>
  </button>
</template>

<style scoped lang="scss">
.voice-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: $border-radius-md;
  border: 1px solid $border-color-bronze;
  background: rgba($surface-primary, 0.95);
  color: $primary-700;
  cursor: pointer;
  font-size: $font-size-base;
  transition: all $transition-base;
}

.voice-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.voice-btn.is-recording {
  border-color: $accent-color-vermilion;
  color: $accent-color-vermilion;
  background: rgba($accent-color-vermilion, 0.08);
}

.voice-btn.is-recording .voice-icon {
  animation: voicePulse 1.2s ease-in-out infinite;
}

.voice-btn.is-mini {
  padding: 6px 10px;
  font-size: $font-size-sm;
}

@keyframes voicePulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.15);
    opacity: 0.7;
  }
}
</style>
