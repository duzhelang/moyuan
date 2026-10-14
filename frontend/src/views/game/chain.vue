<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useGameStore } from '@/stores/game'
import { searchPoems } from '@/api/modules/poem'
import { getDynastyList } from '@/api/modules/dynasty'
import type { Dynasty } from '@/types/model'
import type { SentenceRef } from '@/api/modules/game'
import GameStage from '@/components/game/GameStage.vue'
import MarkTimer from '@/components/game/MarkTimer.vue'
import ChainTrack from '@/components/game/ChainTrack.vue'
import type { ChainNode } from '@/components/game/ChainTrack.vue'
import VerdictBanner from '@/components/game/VerdictBanner.vue'
import ResultScroll from '@/components/game/ResultScroll.vue'
import VoiceInputButton from '@/components/game/VoiceInputButton.vue'
import GuestBadge from '@/components/game/GuestBadge.vue'

const router = useRouter()
const store = useGameStore()

const startError = ref(false)

// ===== 选择范围（朝代，默认不限 = 中国古典诗词） =====
const dynasties = ref<Dynasty[]>([])
const selectedDynastyId = ref<number | null>(null)

/** 配置阶段展示的范围文案 */
const selectedScopeLabel = computed(() => {
  if (selectedDynastyId.value == null) return '中国古典诗词'
  return dynasties.value.find((d) => d.id === selectedDynastyId.value)?.name || '中国古典诗词'
})

/** 对局中展示的范围文案（以后端回传为准，为空即不限朝代） */
const gameScopeLabel = computed(() => store.dynastyName || '中国古典诗词')

async function loadDynasties() {
  try {
    const res = await getDynastyList()
    dynasties.value = res.data || []
  } catch {
    // 朝代列表加载失败不阻断开局，范围退回默认「中国古典诗词」
  }
}

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

const startRef = computed<SentenceRef | null>(() => store.startSentence)
const startLastChar = computed(() => {
  const s = startRef.value?.sentence || ''
  const chars = Array.from(s)
  return chars[chars.length - 1] || ''
})

const inputPlaceholder = computed(() =>
  startLastChar.value ? `以「${startLastChar.value}」起，接一句古诗` : '接一句古诗'
)

/** 接龙链节点：历史按 AI/用户交替，链尾补上当前待接句 */
const chainNodes = computed<ChainNode[]>(() => {
  const list: ChainNode[] = store.historySentences.map((sentence, idx) => ({
    sentence,
    source: idx % 2 === 0 ? 'ai' : 'user'
  }))
  const last = store.lastSentence
  if (last && list[list.length - 1]?.sentence !== last.sentence) {
    list.push({ sentence: last.sentence, title: last.title, author: last.author, source: 'ai' })
  }
  return list
})

const resultSentences = computed(() => store.historySentences.map((sentence) => ({ sentence })))

async function handleStart() {
  startError.value = false
  try {
    await store.startGame('chain', {
      inputType: 'text',
      dynastyId: selectedDynastyId.value ?? undefined
    })
    runPrepare()
  } catch {
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

onMounted(() => {
  void loadDynasties()
})

onUnmounted(() => {
  if (prepTimer) clearInterval(prepTimer)
})
</script>

<template>
  <div class="game-page game-page--chain">
    <div class="game-topbar">
      <button class="game-back" type="button" @click="router.push('/')">‹ 返回</button>
      <h1 class="game-title">诗 词 接 龙</h1>
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
        <h3 class="config-label">选择范围</h3>
        <div class="config-options">
          <button
            class="config-chip"
            :class="{ active: selectedDynastyId === null }"
            type="button"
            @click="selectedDynastyId = null"
          >
            <span class="chip-label">中国古典诗词</span>
            <span class="chip-desc">不限朝代（默认）</span>
          </button>
          <button
            v-for="d in dynasties"
            :key="d.id"
            class="config-chip"
            :class="{ active: selectedDynastyId === d.id }"
            type="button"
            @click="selectedDynastyId = d.id"
          >
            <span class="chip-label">{{ d.name }}</span>
            <span class="chip-desc">仅限{{ d.name }}</span>
          </button>
        </div>
      </div>

      <div class="config-block">
        <h3 class="config-label">玩法说明</h3>
        <p class="config-desc">
          以 AI 起句的末字为首字，接出真实古典诗词原句；人机交替接续，AI 无句可接即你获胜。
          当前范围为「{{ selectedScopeLabel }}」，你与 AI 的诗句都须落在该范围内。
          语言输入支持文本与语音（语音录制上限 20 秒）。
        </p>
      </div>
      <div class="config-actions">
        <button class="game-primary-btn" type="button" :disabled="store.loading" @click="handleStart">开始挑战</button>
      </div>
      <p v-if="startError" class="config-error">开局失败，请稍后重试</p>
    </div>

    <!-- 准备阶段：起句卷轴展开入场 + 3 秒过场（不计时） -->
    <div v-else-if="store.phase === 'prepare'" class="game-prepare">
      <div class="chain-scroll">
        <div class="chain-scroll-roll chain-scroll-roll--top" />
        <div class="chain-scroll-body">
          <p class="prep-label">AI 起句</p>
          <p class="chain-start-sentence">{{ startRef?.sentence }}</p>
          <p v-if="startRef?.author || startRef?.title" class="chain-start-meta">
            <span v-if="startRef?.author">{{ startRef?.author }}</span>
            <span v-if="startRef?.author && startRef?.title"> · </span>
            <span v-if="startRef?.title">《{{ startRef?.title }}》</span>
          </p>
          <p v-if="startLastChar" class="prep-rule">请以末字「{{ startLastChar }}」接起</p>
          <p class="prep-rule">当前范围：{{ gameScopeLabel }}</p>
        </div>
        <div class="chain-scroll-roll chain-scroll-roll--bottom" />
      </div>
      <button class="game-primary-btn" type="button" :disabled="!prepareReady || store.submitting" @click="handleBegin">
        <span v-if="store.submitting" class="btn-spinner" />
        {{ prepareReady ? (store.submitting ? '研墨中…' : '开始接龙') : `过场 ${prepLeft} 秒` }}
      </button>
    </div>

    <!-- 对局中 -->
    <GameStage
      v-else-if="store.phase === 'playing' || store.phase === 'waiting'"
      size="full"
      :phase="store.phase"
      game-type="chain"
    >
      <template #head>
        <span class="stage-kicker">第 {{ store.roundNo }} 回合</span>
        <span class="stage-meta">范围 {{ gameScopeLabel }}</span>
        <span class="stage-meta">链长 {{ store.historyCount }}</span>
        <span class="stage-meta">重试 {{ store.retryLeft + 1 }} 次</span>
      </template>
      <template #timer>
        <MarkTimer
          :remain-seconds="store.timerRemain"
          :total="store.timerTotal"
          :paused="store.timerPaused"
          :hint="store.timerHint"
        />
      </template>
      <template #board>
        <ChainTrack :nodes="chainNodes" :waiting="store.timerPaused" @detail="openSentence" />
      </template>
      <template #loading>
        <div v-if="store.timerPaused" class="game-inking">
          <span class="inking-ring" />
          <span>AI 正在研墨…</span>
        </div>
      </template>
      <template #aside>
        <div class="poet-shadow" :class="{ 'is-thinking': store.timerPaused }">
          <span class="poet-head" />
          <span class="poet-body" />
          <span class="poet-label">{{ store.timerPaused ? '诗人研墨中…' : '诗人应句' }}</span>
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
        :sentences="resultSentences"
        sentences-clickable
        game-type="chain"
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
