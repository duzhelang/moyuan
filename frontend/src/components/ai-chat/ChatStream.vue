<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import type { ChatMessage } from '@/composables/useAiChat'
import type { AiPersona } from '@/configs/ai-chat/personas'
import type { AiSkill } from '@/configs/ai-chat/skills'

const props = defineProps<{
  messages: ChatMessage[]
  loading: boolean
  persona: AiPersona
  recommendedSkills: AiSkill[]
}>()

const emit = defineEmits<{
  pickSkill: [id: string]
}>()

// ===== 结果解析（复用首页现有解析逻辑） =====
interface MeterLine {
  text: string
  tone: string
  rhyme: boolean
  note: string
}
interface MeterResult {
  valid: boolean
  format: string
  rhymeGroup: string
  lines: MeterLine[]
  summary: string
}

/** 从 AI 返回中提取首个 JSON 对象（剥离 ``` 围栏） */
function extractJson<T>(raw: string): T | null {
  if (!raw) return null
  let text = raw.trim()
  if (text.startsWith('```')) {
    const nl = text.indexOf('\n')
    if (nl >= 0) text = text.slice(nl + 1)
    const fence = text.lastIndexOf('```')
    if (fence >= 0) text = text.slice(0, fence)
  }
  const start = text.indexOf('{')
  const end = text.lastIndexOf('}')
  if (start < 0 || end <= start) return null
  try {
    return JSON.parse(text.slice(start, end + 1)) as T
  } catch {
    return null
  }
}

function parseMeter(raw: string): MeterResult | null {
  const p = extractJson<MeterResult>(raw)
  if (p && Array.isArray(p.lines) && p.lines.length > 0) {
    return {
      valid: !!p.valid,
      format: p.format || '',
      rhymeGroup: p.rhymeGroup || '',
      lines: p.lines,
      summary: p.summary || ''
    }
  }
  return null
}

function parseAcrostic(raw: string): { head: string; rest: string }[] {
  return raw
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .slice(0, 8)
    .map((line) => {
      const chars = Array.from(line)
      return { head: chars[0] ?? '', rest: chars.slice(1).join('') }
    })
}

type Rendered =
  | { kind: 'text'; id: string; role: 'user' | 'assistant'; content: string }
  | { kind: 'acrostic'; id: string; content: string; lines: { head: string; rest: string }[] }
  | {
    kind: 'tone'
    id: string
    content: string
    meter: MeterResult
    toneLines: { line: MeterLine; chars: string[] }[]
  }

const rendered = computed<Rendered[]>(() =>
  props.messages.map((m): Rendered => {
    if (m.role === 'assistant' && m.skillId === 'tone') {
      const meter = parseMeter(m.content)
      if (meter) {
        return {
          kind: 'tone',
          id: m.id,
          content: m.content,
          meter,
          toneLines: meter.lines.map((line) => ({
            line,
            chars: Array.from(line.tone || '').filter((c) => c === '平' || c === '仄')
          }))
        }
      }
    }
    if (m.role === 'assistant' && m.skillId === 'acrostic') {
      const lines = parseAcrostic(m.content)
      if (lines.length) return { kind: 'acrostic', id: m.id, content: m.content, lines }
    }
    return { kind: 'text', id: m.id, role: m.role, content: m.content }
  })
)

// ===== 自动滚动到底部 =====
const streamRef = ref<HTMLElement | null>(null)

const scrollToBottom = () => {
  nextTick(() => {
    const el = streamRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

watch(() => [props.messages.length, props.loading] as const, scrollToBottom)
onMounted(scrollToBottom)
</script>

<template>
  <div ref="streamRef" class="chat-stream">
    <div v-if="!messages.length && !loading" class="chat-empty">
      <div class="chat-empty-icon">{{ persona.icon }}</div>
      <p class="chat-empty-greeting">{{ persona.greeting }}</p>
      <div v-if="recommendedSkills.length" class="chat-empty-actions">
        <button
          v-for="s in recommendedSkills"
          :key="s.id"
          class="chat-empty-chip"
          @click="emit('pickSkill', s.id)"
        >
          <span>{{ s.icon }}</span>{{ s.name }}
        </button>
      </div>
    </div>

    <div v-else class="chat-list">
      <div
        v-for="m in rendered"
        :key="m.id"
        class="chat-row"
        :class="m.kind === 'text' ? m.role : 'assistant'"
      >
        <div class="chat-bubble" :class="{ 'is-assistant': m.kind !== 'text' || m.role === 'assistant' }">
          <template v-if="m.kind === 'text'">
            <p class="chat-text">{{ m.content }}</p>
          </template>

          <template v-else-if="m.kind === 'acrostic'">
            <div class="acrostic-lines">
              <div v-for="(line, idx) in m.lines" :key="idx" class="acrostic-line">
                <span class="acrostic-head">{{ line.head }}</span><span class="acrostic-rest">{{ line.rest }}</span>
              </div>
            </div>
          </template>

          <template v-else>
            <div class="meter-head">
              <span class="meter-badge" :class="m.meter.valid ? 'is-valid' : 'is-invalid'">
                {{ m.meter.valid ? '基本合律' : '存在不合律处' }}
              </span>
              <span v-if="m.meter.format" class="meter-format">{{ m.meter.format }}</span>
              <span v-if="m.meter.rhymeGroup" class="meter-rhyme">押：{{ m.meter.rhymeGroup }}</span>
            </div>
            <div class="meter-lines">
              <div v-for="(item, idx) in m.toneLines" :key="idx" class="meter-line">
                <div class="meter-line-text">{{ item.line.text }}</div>
                <div class="meter-line-tone">
                  <span
                    v-for="(t, ti) in item.chars"
                    :key="ti"
                    class="meter-tone"
                    :class="t === '平' ? 'is-ping' : 'is-ze'"
                  >{{ t }}</span>
                  <span v-if="item.line.rhyme" class="meter-rhyme-tag">押韵</span>
                </div>
                <div v-if="item.line.note" class="meter-line-note">{{ item.line.note }}</div>
              </div>
            </div>
            <div v-if="m.meter.summary" class="meter-summary">{{ m.meter.summary }}</div>
          </template>
        </div>
      </div>

      <div v-if="loading" class="chat-row assistant">
        <div class="chat-bubble is-assistant chat-loading">
          <span class="chat-loading-spinner"></span>
          <span class="chat-loading-text">墨渊正在提笔…</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.chat-stream {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 24px;
}

/* 空态 */
.chat-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  text-align: center;
}

.chat-empty-icon {
  font-size: 44px;
  line-height: 1;
}

.chat-empty-greeting {
  max-width: 520px;
  margin: 0;
  font-size: 15px;
  line-height: 1.9;
  color: #6b5b45;
}

.chat-empty-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 4px;
}

.chat-empty-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 16px;
  border: 1px dashed #b34a31;
  border-radius: 999px;
  background: rgba(179, 74, 49, 0.05);
  color: #b34a31;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.chat-empty-chip:hover {
  background: rgba(179, 74, 49, 0.12);
}

/* 对话流 */
.chat-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-row {
  display: flex;
}

.chat-row.user {
  justify-content: flex-end;
}

.chat-row.assistant {
  justify-content: flex-start;
}

.chat-bubble {
  max-width: 78%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.8;
  word-break: break-word;
  white-space: pre-wrap;
}

/* 用户气泡 */
.chat-row.user .chat-bubble {
  background: linear-gradient(135deg, #8b4513, #a0522d);
  color: #fff;
  border-radius: 12px 12px 2px 12px;
  box-shadow: 0 2px 8px rgba(139, 69, 19, 0.2);
}

/* AI 气泡：铜边宣纸底 + 顶部 3px 渐变装饰条 */
.chat-bubble.is-assistant {
  position: relative;
  background: #fffcf7;
  border: 1px solid #d4c5a9;
  border-radius: 2px 12px 12px 12px;
  color: #4a3520;
  box-shadow: 0 2px 8px rgba(139, 69, 19, 0.08);
  overflow: hidden;
}

.chat-bubble.is-assistant::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #8b4513, #b34a31, #c9a063);
}

.chat-text {
  margin: 0;
}

/* 加载态 */
.chat-loading {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.chat-loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(139, 69, 19, 0.25);
  border-top-color: #8b4513;
  border-radius: 50%;
  animation: chat-stream-spin 0.8s linear infinite;
}

@keyframes chat-stream-spin {
  to {
    transform: rotate(360deg);
  }
}

.chat-loading-text {
  color: #8b6914;
  font-size: 14px;
}

/* 藏头诗 */
.acrostic-lines {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-family: 'FangSong', 'STFangsong', 'Noto Serif SC', 'SimSun', serif;
  font-size: 20px;
  letter-spacing: 3px;
}

.acrostic-head {
  color: #b34a31;
  font-weight: 700;
}

.acrostic-rest {
  color: #4a3520;
}

/* 平仄校验 */
.meter-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.meter-badge {
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.meter-badge.is-valid {
  background: #8b6914;
}

.meter-badge.is-invalid {
  background: #b34a31;
}

.meter-format {
  font-size: 13px;
  color: #6b4513;
}

.meter-rhyme {
  font-size: 13px;
  color: #8b6914;
}

.meter-lines {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.meter-line {
  padding-bottom: 8px;
  border-bottom: 1px dashed rgba(212, 197, 169, 0.7);
}

.meter-line:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.meter-line-text {
  font-family: 'FangSong', 'STFangsong', 'Noto Serif SC', 'SimSun', serif;
  font-size: 16px;
  color: #4a3520;
  margin-bottom: 4px;
}

.meter-line-tone {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.meter-tone {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.meter-tone.is-ping {
  background: rgba(139, 69, 19, 0.12);
  color: #6b4513;
}

.meter-tone.is-ze {
  background: rgba(179, 74, 49, 0.12);
  color: #b34a31;
}

.meter-rhyme-tag {
  margin-left: 6px;
  padding: 1px 8px;
  border-radius: 999px;
  background: rgba(201, 160, 99, 0.25);
  color: #8b6914;
  font-size: 11px;
}

.meter-line-note {
  margin-top: 4px;
  font-size: 12px;
  color: #8a7a63;
}

.meter-summary {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid rgba(212, 197, 169, 0.7);
  font-size: 13px;
  color: #6b4513;
}

@media (max-width: 768px) {
  .chat-stream {
    padding: 14px;
  }

  .chat-bubble {
    max-width: 90%;
  }
}
</style>
