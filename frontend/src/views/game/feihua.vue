<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useGameStore } from '@/stores/game'
import { searchPoems } from '@/api/modules/poem'
import type { SentenceRef } from '@/api/modules/game'
import GameStage from '@/components/game/GameStage.vue'
import MarkTimer from '@/components/game/MarkTimer.vue'
import SealKeyword from '@/components/game/SealKeyword.vue'
import PoemSlip from '@/components/game/PoemSlip.vue'
import VerdictBanner from '@/components/game/VerdictBanner.vue'
import ResultScroll from '@/components/game/ResultScroll.vue'
import VoiceInputButton from '@/components/game/VoiceInputButton.vue'
import GuestBadge from '@/components/game/GuestBadge.vue'

const router = useRouter()
const store = useGameStore()

// ===== 开始前配置 =====
const mode = ref<'ancient' | 'normal' | 'super'>('normal')
const level = ref<'easy' | 'medium' | 'hard'>('easy')
const superType = ref<'char' | 'topic'>('char')
const keywordInput = ref('花')
const startError = ref(false)

const modeOptions = [
  { value: 'ancient', label: '古代严格', desc: '令字须在第 N 个字位' },
  { value: 'normal', label: '诗词大会版', desc: '诗句含令字即可' },
  { value: 'super', label: '超级飞花令', desc: '多字同含 / 主题贴合' }
] as const
const levelOptions = [
  { value: 'easy', label: '初级', desc: '累计 10 句' },
  { value: 'medium', label: '进阶', desc: '累计 20 句' },
  { value: 'hard', label: '究极', desc: 'AI 无句可接即胜' }
] as const

const keywordsPayload = computed<string[]>(() => {
  const raw = keywordInput.value.trim()
  if (!raw) return []
  if (mode.value === 'super' && superType.value === 'char') return Array.from(raw)
  return [raw]
})

// ===== 准备阶段过场（3 秒，不计时） =====
const prepLeft = ref(3)
let prepTimer: ReturnType<typeof setInterval> | null = null

function runPrepare() {
  prepLeft.value = 3
  if (prepTimer) clearInterval(prepTimer)
  prepTimer = setInterval(() => {
    prepLeft.value--
    if (prepLeft.value <= 0) {
      if (prepTimer) clearInterval(prepTimer)
      prepTimer = null
    }
  }, 1000)
}

const prepareReady = computed(() => prepLeft.value <= 0)
const targetLabel = computed(() => (store.winTargetCount === null ? '无上限' : `${store.winTargetCount} 句`))
const inputPlaceholder = computed(() => {
  if (store.isAncient) return `令字「${store.keywords[0] || ''}」须落在第 ${store.roundNo} 个字位`
  if (store.superType === 'topic') return `写一句贴合「${store.keywords[0] || ''}」主题的古诗`
  const kw = store.keywords.map((k) => `「${k}」`).join('')
  return `写一句${kw ? `含 ${kw} 的` : ''}古诗`
})

const modeLabel = computed(() => modeOptions.find((m) => m.value === mode.value)?.label || '')
const levelLabel = computed(() => levelOptions.find((l) => l.value === level.value)?.label || '')

const historySlips = computed(() => store.historySentences.map((sentence) => ({ sentence })))

// ===== 交互 =====
async function handleStart() {
  if (!keywordsPayload.value.length) {
    ElMessage.warning('请先输入令字')
    return
  }
  startError.value = false
  try {
    await store.startGame('feihua', {
      mode: mode.value,
      level: level.value,
      superType: mode.value === 'super' ? superType.value : undefined,
      keywords: keywordsPayload.value,
      inputType: 'text'
    })
    runPrepare()
  } catch {
    // 请求错误已由统一拦截器提示
    startError.value = true
  }
}

async function handleBegin() {
  try {
    await store.beginRound()
  } catch {
    // 请求错误已由统一拦截器提示
  }
}

async function handleSubmit(inputType: 'text' | 'voice' = 'text') {
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
  void handleSubmit('voice')
}

async function handleGiveUp() {
  await store.giveUp()
}

function handleRestart() {
  store.reset()
}

/** 本地库诗句：解析 poemId 后跳详情，兜底跳诗词列表检索 */
async function openSentence(sentence: string) {
  try {
    const res = await searchPoems(sentence)
    const list = res.data || []
    const hit = list.find((poem) => poem.content?.includes(sentence)) || list[0]
    if (hit && hit.id) {
      router.push(`/poem/${hit.id}`)
      return
    }
  } catch {
    // 解析失败走列表兜底
  }
  router.push({ path: '/poem', query: { keyword: sentence } })
}

function openRef(ref: SentenceRef) {
  void openSentence(ref.sentence)
}

onUnmounted(() => {
  if (prepTimer) clearInterval(prepTimer)
})
</script>

<template>
  <div class="game-page game-page--feihua">
    <div class="game-topbar">
      <button class="game-back" type="button" @click="router.push('/')">‹ 返回</button>
      <h1 class="game-title">飞 花 令</h1>
      <GuestBadge v-if="store.isGuest" compact />
      <div class="game-topbar-actions">
        <button
          v-if="store.phase === 'playing' || store.phase === 'waiting'"
          class="game-quit"
          type="button"
          @click="handleGiveUp"
        >放弃本局</button>
      </div>
    </div>

    <!-- 07 整页加载：对局初始化未就绪 -->
    <div v-if="store.loading" class="game-full-loading">
      <span class="loading-ink" />
      <p class="loading-text">正在备好诗台…</p>
    </div>

    <!-- 配置阶段 -->
    <div v-else-if="store.phase === 'idle'" class="game-config">
      <div class="config-block">
        <h3 class="config-label">选择模式</h3>
        <div class="config-options">
          <button
            v-for="opt in modeOptions"
            :key="opt.value"
            class="config-chip"
            :class="{ active: mode === opt.value }"
            type="button"
            @click="mode = opt.value"
          >
            <span class="chip-label">{{ opt.label }}</span>
            <span class="chip-desc">{{ opt.desc }}</span>
          </button>
        </div>
      </div>

      <div v-if="mode === 'super'" class="config-block">
        <h3 class="config-label">超级飞花令类型</h3>
        <div class="config-options">
          <button class="config-chip" :class="{ active: superType === 'char' }" type="button" @click="superType = 'char'">
            <span class="chip-label">多字同含</span>
            <span class="chip-desc">同时含全部关键字</span>
          </button>
          <button class="config-chip" :class="{ active: superType === 'topic' }" type="button" @click="superType = 'topic'">
            <span class="chip-label">主题模式</span>
            <span class="chip-desc">贴合指定主题</span>
          </button>
        </div>
      </div>

      <div class="config-block">
        <h3 class="config-label">选择难度</h3>
        <div class="config-options">
          <button
            v-for="opt in levelOptions"
            :key="opt.value"
            class="config-chip"
            :class="{ active: level === opt.value }"
            type="button"
            @click="level = opt.value"
          >
            <span class="chip-label">{{ opt.label }}</span>
            <span class="chip-desc">{{ opt.desc }}</span>
          </button>
        </div>
      </div>

      <div class="config-block">
        <h3 class="config-label">{{ mode === 'super' && superType === 'topic' ? '输入主题' : '输入令字' }}</h3>
        <input
          v-model="keywordInput"
          class="config-input"
          :placeholder="mode === 'super' && superType === 'char' ? '如：山月（两个及以上汉字）' : '如：花'"
          maxlength="12"
        >
      </div>

      <div class="config-actions">
        <button class="game-primary-btn" type="button" :disabled="store.loading" @click="handleStart">开始挑战</button>
      </div>
      <p v-if="startError" class="config-error">开局失败，请稍后重试</p>
    </div>

    <!-- 准备阶段：规则说明 + 3 秒过场（不计时） -->
    <div v-else-if="store.phase === 'prepare'" class="game-prepare">
      <div class="prep-scroll">
        <h3 class="prep-title">{{ modeLabel }} · {{ levelLabel }}</h3>
        <p class="prep-rule">
          {{ store.isAncient ? `古代严格飞花令：第 ${store.roundNo} 回合，令字须出现在第 ${store.roundNo} 个汉字位置。` : '诗句须为真实古典诗词原句，且包含指定令字，不可重复。' }}
        </p>
        <p class="prep-rule">每回合 30 秒，含 2 次重试；累计达标或 AI 无句可接即胜。</p>
        <SealKeyword :keywords="store.keywords" :mode="store.mode" :round-no="store.roundNo" />
        <p class="prep-target">胜利目标：{{ targetLabel }}</p>
      </div>
      <button class="game-primary-btn" type="button" :disabled="!prepareReady || store.submitting" @click="handleBegin">
        <span v-if="store.submitting" class="btn-spinner" />
        {{ prepareReady ? (store.submitting ? '研墨中…' : '开始答题') : `过场 ${prepLeft} 秒` }}
      </button>
    </div>

    <!-- 对局中 -->
    <GameStage
      v-else-if="store.phase === 'playing' || store.phase === 'waiting'"
      size="full"
      :phase="store.phase"
      game-type="feihua"
    >
      <template #head>
        <span class="stage-kicker">第 {{ store.roundNo }} 回合</span>
        <span class="stage-meta">重试 {{ store.retryLeft + 1 }} 次</span>
        <span class="stage-meta">目标 {{ targetLabel }}</span>
      </template>
      <template #timer>
        <MarkTimer
          :remain-seconds="store.timerRemain"
          :total="store.timerTotal"
          :paused="store.timerPaused"
          :hint="store.timerHint"
        />
      </template>
      <template #keyword>
        <SealKeyword :keywords="store.keywords" :mode="store.mode" :round-no="store.roundNo" />
      </template>
      <template #loading>
        <div v-if="store.timerPaused" class="game-inking">
          <span class="inking-ring" />
          <span>AI 正在研墨…</span>
        </div>
      </template>
      <template #aside>
        <div class="stage-aside-inner">
          <h4 class="aside-title">已用诗句</h4>
          <div v-if="historySlips.length" class="aside-vertical">
            <PoemSlip
              v-for="(item, idx) in historySlips"
              :key="idx"
              :sentence="item.sentence"
              vertical
              clickable
              @detail="openSentence(item.sentence)"
            />
          </div>
          <p v-else class="aside-empty">尚无诗句</p>

          <template v-if="store.candidates.length">
            <h4 class="aside-title aside-title--pouch">锦囊 · 候选</h4>
            <div class="aside-pouch-list">
              <PoemSlip
                v-for="(ref, idx) in store.candidates"
                :key="idx"
                :sentence="ref.sentence"
                :title="ref.title"
                :author="ref.author"
                :matchDesc="ref.matchDesc"
                pouch
                clickable
                @detail="openRef(ref)"
              />
            </div>
          </template>
        </div>
      </template>
      <template #feedback>
        <VerdictBanner :variant="store.verdictVariant" :message="store.checkMsg" />
      </template>
      <template #input>
        <div class="game-input-row">
          <textarea
            v-model="store.inputDraft"
            class="game-textarea"
            :placeholder="inputPlaceholder"
            :disabled="store.phase !== 'playing'"
            rows="2"
            @keydown.enter.exact.prevent="handleSubmit('text')"
          ></textarea>
          <div class="game-input-side">
            <button
              class="game-primary-btn game-submit-btn"
              type="button"
              :disabled="store.submitting || store.phase !== 'playing'"
              @click="handleSubmit('text')"
            >
              <span v-if="store.submitting" class="btn-spinner" />
              {{ store.submitting ? '研墨中…' : '落笔' }}
            </button>
            <VoiceInputButton :disabled="store.phase !== 'playing'" @result="onVoiceResult" />
          </div>
        </div>
      </template>
    </GameStage>

    <!-- 结算 -->
    <div v-else-if="store.phase === 'settled'" class="game-result">
      <ResultScroll
        :result="store.gameResult"
        :sentences="historySlips"
        sentences-clickable
        game-type="feihua"
        :history-count="store.historyCount"
        :win-target-count="store.winTargetCount"
        :is-guest="store.isGuest"
        :reason="store.errorReason"
        @restart="handleRestart"
        @back="router.push('/')"
        @detail="openSentence"
      />
    </div>
  </div>
</template>

<style lang="scss">
@use './game-styles.scss' as *;
</style>
