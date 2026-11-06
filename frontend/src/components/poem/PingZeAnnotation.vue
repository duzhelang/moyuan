<script setup lang="ts">
import { computed } from 'vue'

export interface PingZeChar {
  char: string
  pingze: '平' | '仄' | '中' | string
  pinyin: string
}

export interface PingZeLine {
  text: string
  pattern: string
  chars: PingZeChar[]
  issues: string[]
}

const props = withDefaults(defineProps<{
  lines: PingZeLine[]
  issues?: string[]
  passed?: boolean
  genre?: string
  showPinyin?: boolean
}>(), {
  issues: () => [],
  passed: true,
  genre: '',
  showPinyin: false
})

/** 问题行集合，用于整行标红 */
const issueLineIndexes = computed(() =>
  new Set(props.lines.map((line, index) => (line.issues?.length ? index : -1)).filter(i => i >= 0))
)
</script>

<template>
  <div class="pingze-annotation">
    <div v-if="genre || lines.length" class="pingze-head">
      <el-tag v-if="genre" size="small" effect="plain" class="genre-tag">{{ genre }}</el-tag>
      <el-tag :type="passed ? 'success' : 'danger'" size="small" effect="light">
        {{ passed ? '格律校验通过' : '存在格律问题' }}
      </el-tag>
    </div>

    <div class="pingze-lines">
      <div
        v-for="(line, index) in lines"
        :key="index"
        class="pingze-line"
        :class="{ 'has-issue': issueLineIndexes.has(index) }"
      >
        <div class="chars">
          <span
            v-for="(charItem, charIndex) in line.chars"
            :key="charIndex"
            class="char-item"
            :class="`tone-${charItem.pingze}`"
            :title="charItem.pinyin ? `${charItem.pinyin}（${charItem.pingze}）` : charItem.pingze"
          >
            <span v-if="showPinyin" class="pinyin-mark">{{ charItem.pinyin }}</span>
            <span class="tone-mark">{{ charItem.pingze }}</span>
            <span class="char-text">{{ charItem.char }}</span>
          </span>
        </div>
        <p v-if="line.issues && line.issues.length" class="line-issue">
          {{ line.issues.join('；') }}
        </p>
      </div>
    </div>

    <ul v-if="issues && issues.length" class="pingze-issues">
      <li v-for="(issue, index) in issues" :key="index">{{ issue }}</li>
    </ul>
  </div>
</template>

<style scoped>
.pingze-annotation {
  width: 100%;
}

.pingze-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.genre-tag {
  color: #8b5a2b;
  border-color: rgba(139, 90, 43, 0.4);
}

.pingze-lines {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pingze-line {
  padding: 6px 8px;
  border-radius: 6px;
  background: var(--pingze-line-bg, rgba(255, 255, 255, 0.02));
}

.pingze-line.has-issue {
  background: rgba(198, 40, 40, 0.08);
  box-shadow: inset 0 0 0 1px rgba(198, 40, 40, 0.25);
}

.chars {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
}

.char-item {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  min-width: 26px;
}

.tone-mark {
  font-size: 11px;
  line-height: 1;
  margin-bottom: 2px;
  opacity: 0.85;
}

.pinyin-mark {
  font-size: 9px;
  line-height: 1;
  color: #9a8c7c;
  margin-bottom: 1px;
}

.char-text {
  font-size: 18px;
  line-height: 1.3;
  color: var(--pingze-char-color, #e8dcc8);
  font-family: 'KaiTi', 'STKaiti', serif;
}

.tone-平 .tone-mark {
  color: #4caf50;
}

.tone-仄 .tone-mark {
  color: #ef5350;
}

.tone-中 .tone-mark {
  color: #9e9e9e;
}

.line-issue {
  margin: 6px 0 0;
  font-size: 12px;
  color: #ef5350;
}

.pingze-issues {
  margin: 10px 0 0;
  padding-left: 18px;
  font-size: 12px;
  color: #ef5350;
}

.pingze-issues li {
  line-height: 1.7;
}
</style>
