<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useGameStore } from '@/stores/game'
import { useUserStore } from '@/stores/user'
import GameStage from './GameStage.vue'
import MarkTimer from './MarkTimer.vue'
import SealKeyword from './SealKeyword.vue'
import PoemSlip from './PoemSlip.vue'
import VerdictBanner from './VerdictBanner.vue'
import ResultScroll from './ResultScroll.vue'
import VoiceInputButton from './VoiceInputButton.vue'
import GuestBadge from './GuestBadge.vue'
import type { ResultSentence } from './ResultScroll.vue'

const props = defineProps<{
  type: 'feihua' | 'chain'
}>()

const emit = defineEmits<{ (e: 'close'): void }>()

const router = useRouter()
const store = useGameStore()
const userStore = useUserStore()

const startFailed = ref(false)
const loggedInHint = ref(false)

const fullPath = computed(() => (props.type === 'feihua' ? '/game/feihua' : '/game/chain'))

const inputPlaceholder = computed(() => {
  if (props.type === 'chain') {
    const chars = store.lastSentence ? Array.from(store.lastSentence.sentence) : []
    const last = chars[chars.length - 1] || ''
    return last ? `以「${last}」起，接一句古诗` : '接一句古诗'
  }
  const keyword = store.keywords[0] || ''
  return keyword ? `写下含「${keyword}」的诗句` : '写下一句古诗'
})

/** 精简：仅展示最近 3 条诗笺 */
const recentSlips = computed<ResultSentence[]>(() => {
  if (props.type === 'feihua') {
    return store.historySentences.slice(-3).map((sentence) => ({ sentence }))
  }
  const base = store.historySentences.map((sentence) => ({ sentence }))
  if (store.lastSentence && base[base.length - 1]?.sentence !== store.lastSentence.sentence) {
    base.push({ sentence: store.lastSentence.sentence })
  }
  return base.slice(-3)
})

const resultSentences = computed<ResultSentence[]>(() =>
  store.historySentences.map((sentence) => ({ sentence }))
)

async function startMiniGame() {
  startFailed.value = false
  store.reset()
  try {
    if (props.type === 'feihua') {
      await store.startGame('feihua', { mode: 'normal', level: 'easy', keywords: ['花'], inputType: 'text' })
    } else {
      await store.startGame('chain', { inputType: 'text' })
    }
    await store.beginRound()
  } catch {
    // 请求错误已由统一拦截器提示
    startFailed.value = true
  }
}

async function submitAnswer(inputType: 'text' | 'voice' = 'text') {
  if (!store.inputDraft.trim()) {
    ElMessage.warning('请先写下诗句')
    return
  }
  try {
    await store.submit(inputType)
  } catch {
    // 请求错误已由统一拦截器提示
  }
}

function onVoiceResult(text: string) {
  store.inputDraft = text
  void submitAnswer('voice')
}

async function handleClose() {
  if (store.phase === 'playing' || store.phase === 'waiting') {
    await store.giveUp()
  }
  emit('close')
}

function goFullGame() {
  router.push(fullPath.value)
}

watch(
  () => userStore.isLoggedIn,
  (logged) => {
    if (logged && store.isGuest && (store.phase === 'playing' || store.phase === 'waiting')) {
      loggedInHint.value = true
    }
  }
)

onMounted(() => {
  void startMiniGame()
})

onUnmounted(() => {
  if (store.phase === 'playing' || store.phase === 'waiting') {
    void store.giveUp()
  }
})
</script>

<template>
  <div class="game-mini-panel">
    <div class="mini-head">
      <GuestBadge compact />
      <button class="mini-close" type="button" aria-label="关闭" @click="handleClose">×</button>
    </div>

    <div v-if="loggedInHint" class="mini-login-hint">
      <span>已登录，可前往完整对局页继续挑战</span>
      <button type="button" @click="goFullGame">前往完整对局</button>
    </div>

    <div v-if="startFailed" class="mini-error">
      <span>对局初始化失败，请稍后重试</span>
      <button type="button" @click="startMiniGame">重试</button>
    </div>

    <template v-else-if="store.phase === 'settled'">
      <ResultScroll
        :result="store.gameResult"
        :sentences="resultSentences"
        :game-type="type"
        :history-count="store.historyCount"
        :win-target-count="store.winTargetCount"
        :is-guest="store.isGuest"
        :reason="store.errorReason"
        @restart="startMiniGame"
        @back="handleClose"
      />
    </template>

    <GameStage v-else size="mini" :phase="store.phase" :game-type="type">
      <template #timer>
        <MarkTimer
          :remain-seconds="store.timerRemain"
          :total="store.timerTotal"
          size="mini"
          :paused="store.timerPaused"
          :hint="store.timerHint"
        />
      </template>
      <template v-if="type === 'feihua'" #keyword>
        <SealKeyword
          :keywords="store.keywords"
          :mode="store.mode"
          :round-no="store.roundNo"
          size="mini"
        />
      </template>
      <template #board>
        <div class="mini-slips">
          <PoemSlip
            v-for="(item, idx) in recentSlips"
            :key="idx"
            :sentence="item.sentence"
            :source="type === 'chain' ? (idx % 2 === 0 ? 'ai' : 'user') : ''"
          />
          <p v-if="!recentSlips.length" class="mini-empty">等待落笔…</p>
        </div>
      </template>
      <template #loading>
        <div v-if="store.timerPaused" class="mini-inking">
          <span class="inking-ring" />
          <span>AI 正在研墨…</span>
        </div>
      </template>
      <template #feedback>
        <VerdictBanner :variant="store.verdictVariant" :message="store.checkMsg" />
      </template>
      <template #input>
        <div class="mini-input-row">
          <input
            v-model="store.inputDraft"
            class="mini-input"
            :placeholder="inputPlaceholder"
            :disabled="store.phase !== 'playing'"
            @keyup.enter="submitAnswer('text')"
          >
          <button
            class="mini-submit"
            type="button"
            :disabled="store.submitting || store.phase !== 'playing'"
            @click="submitAnswer('text')"
          >
            <span v-if="store.submitting" class="mini-spinner" />
            {{ store.submitting ? '研墨中…' : '落笔' }}
          </button>
          <VoiceInputButton size="mini" :disabled="store.phase !== 'playing'" @result="onVoiceResult" />
        </div>
      </template>
    </GameStage>
  </div>
</template>

<style scoped lang="scss">
.game-mini-panel {
  position: relative;
}

.mini-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-md;
}

.mini-close {
  border: none;
  background: none;
  font-size: 22px;
  line-height: 1;
  color: $text-color-secondary;
  cursor: pointer;
}

.mini-close:hover {
  color: $primary-600;
}

.mini-login-hint,
.mini-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
  padding: $spacing-sm $spacing-md;
  border-radius: $border-radius-md;
  font-size: $font-size-sm;
}

.mini-login-hint {
  background: rgba($success-color, 0.1);
  border: 1px solid rgba($success-color, 0.35);
  color: darken($success-color, 20%);
}

.mini-error {
  background: rgba($accent-color-vermilion, 0.08);
  border: 1px solid rgba($accent-color-vermilion, 0.35);
  color: $accent-color-vermilion;
}

.mini-login-hint button,
.mini-error button {
  border: 1px solid currentColor;
  background: transparent;
  color: inherit;
  border-radius: $border-radius-sm;
  padding: 2px 10px;
  font-size: $font-size-xs;
  cursor: pointer;
}

.mini-slips {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.mini-empty {
  margin: 0;
  font-size: $font-size-sm;
  color: $text-color-light;
  text-align: center;
}

.mini-inking {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: $spacing-sm;
  color: $primary-600;
  font-family: $font-family-seal;
}

.inking-ring {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid rgba($primary-color, 0.25);
  border-top-color: $primary-500;
  animation: inkRotate 0.9s linear infinite;
}

.mini-input-row {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.mini-input {
  flex: 1;
  min-width: 0;
  padding: 8px 10px;
  border: none;
  border-bottom: 1px solid $border-color-bronze;
  background: transparent;
  font-family: $font-family-poetry;
  font-size: $font-size-base;
  color: $text-color-ink;
  outline: none;
  box-sizing: border-box;
}

.mini-input:focus {
  border-bottom-color: $primary-400;
}

.mini-input:disabled {
  opacity: 0.6;
}

.mini-submit {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 76px;
  justify-content: center;
  padding: 8px 14px;
  border: none;
  border-radius: $border-radius-md;
  background: $primary-500;
  color: #fff;
  cursor: pointer;
  font-size: $font-size-sm;
  transition: background $transition-base;
}

.mini-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.mini-spinner {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  animation: inkRotate 0.9s linear infinite;
}

@keyframes inkRotate {
  to {
    transform: rotate(360deg);
  }
}
</style>
