import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import {
  startFeihua,
  beginFeihuaRound,
  playFeihua,
  giveUpFeihua,
  startChain,
  beginChainRound,
  playChain,
  giveUpChain,
  type GameStartResponse,
  type GamePlayResponse,
  type GameStartParams,
  type SentenceRef
} from '@/api/modules/game'

export type GameType = 'feihua' | 'chain'
export type GamePhase = 'idle' | 'prepare' | 'playing' | 'waiting' | 'settled'
export type GameOutcome = '' | 'win' | 'lose' | 'giveup'
export type InputType = 'text' | 'voice'
/**
 * 计时子状态（仅在 phase === playing 时有效）：
 * - monitoring：倒计时冻结，监测输入框是否在 5 秒内有变化；
 * - counting：30 秒倒计时逐秒递减；
 * - buffer：30 秒耗尽后的 5 秒缓冲提交窗口。
 */
export type TimerPhase = 'monitoring' | 'counting' | 'buffer'

/** 单回合答题总时长（秒），与后端口径一致 */
export const ROUND_SECONDS = 30
/** 停笔满 5 秒后倒计时继续（打字即暂停的监测窗口） */
export const MONITOR_SECONDS = 5
/** 30 秒耗尽后的缓冲提交窗口（秒） */
export const BUFFER_SECONDS = 5
/** 上报有效时长上限：30 + 5 = 35 秒，后端据此判超时 */
export const TOTAL_ACTIVE_SECONDS = ROUND_SECONDS + BUFFER_SECONDS

/**
 * 对局状态机：小窗口与游戏页共用同一份状态与对局流程，
 * 差异仅由容器形态（GameStage 的 size）控制。
 *
 * 计时规则（飞花令 / 诗词接龙共用）：
 * 1. 点「开始答题」→ 倒计时直接开始（30 秒逐秒递减）；
 * 2. 输入框一旦有变化 → 暂停倒计时并进入监测态（5 秒停笔监测），满 5 秒无变化则恢复递减；
 * 3. 30 秒耗尽 → 进入缓冲态，开启 5 秒缓冲提交窗口；
 * 4. 缓冲窗口耗尽仍无主动提交 → 自动提交当前内容，由后端按上报有效时长判超时；
 * 5. 任意时刻主动提交 → 终止全部计时。
 */
export const useGameStore = defineStore('game', () => {
  const phase = ref<GamePhase>('idle')
  const gameId = ref('')
  const gameType = ref<GameType>('feihua')
  const mode = ref('')
  const level = ref('')
  const superType = ref('')
  const keywords = ref<string[]>([])
  /** 接龙选择范围：朝代 ID（null 表示不限朝代 / 中国古典诗词） */
  const dynastyId = ref<number | null>(null)
  /** 接龙选择范围：朝代名称（空表示不限朝代） */
  const dynastyName = ref('')
  const roundNo = ref(1)
  const remainSeconds = ref(ROUND_SECONDS)
  /** 计时子状态（监测 / 计时 / 缓冲） */
  const timerPhase = ref<TimerPhase>('monitoring')
  /** 监测态剩余秒数（停笔倒计时） */
  const monitorSeconds = ref(MONITOR_SECONDS)
  /** 缓冲态剩余秒数 */
  const bufferSeconds = ref(BUFFER_SECONDS)
  const retryLeft = ref(2)
  const historySentences = ref<string[]>([])
  const historyCount = ref(0)
  const winTargetCount = ref<number | null>(null)
  const isGuest = ref(false)
  const gameResult = ref<GameOutcome>('')
  const lastSentence = ref<SentenceRef | null>(null)
  const startSentence = ref<SentenceRef | null>(null)
  const candidates = ref<SentenceRef[]>([])
  const checkMsg = ref('')
  const errorReason = ref('')
  const lastValid = ref<boolean | null>(null)
  const isGameOver = ref(false)
  /** 开局/初始化整页加载态 */
  const loading = ref(false)
  /** 提交按钮加载态 */
  const submitting = ref(false)
  /** 输入草稿（小窗口与游戏页共用） */
  const inputDraft = ref('')

  /** 单一时钟心跳（1 秒），按当前子状态推动对应秒数 */
  let clockTimer: ReturnType<typeof setInterval> | null = null

  /** ancient 模式：令字须落在第 roundNo 个汉字位 */
  const isAncient = computed(() => gameType.value === 'feihua' && mode.value === 'ancient')
  /** 等待 AI 生成阶段：暂停倒计时展示 */
  const timerPaused = computed(() => phase.value === 'waiting')
  /** 计时环展示的秒数：缓冲态显示缓冲剩余，其余显示回合剩余 */
  const timerRemain = computed(() =>
    timerPhase.value === 'buffer' ? bufferSeconds.value : remainSeconds.value
  )
  /** 计时环分母：缓冲态 5 秒，其余 30 秒 */
  const timerTotal = computed(() => (timerPhase.value === 'buffer' ? BUFFER_SECONDS : ROUND_SECONDS))
  /** 计时环下方提示文案 */
  const timerHint = computed(() => {
    if (phase.value === 'waiting') return '研墨中'
    if (timerPhase.value === 'monitoring') return '停笔五秒后续时'
    if (timerPhase.value === 'buffer') return '缓冲时限'
    return ''
  })
  /** 裁判反馈变体；null 表示尚未作答 */
  const verdictVariant = computed<'correct' | 'repeat' | 'wrong' | 'none'>(() => {
    if (lastValid.value === null) return 'none'
    if (lastValid.value) return 'correct'
    return errorReason.value.includes('重复') ? 'repeat' : 'wrong'
  })

  function stopClock() {
    if (clockTimer) {
      clearInterval(clockTimer)
      clockTimer = null
    }
  }

  function startClock() {
    stopClock()
    clockTimer = setInterval(() => tick(), 1000)
  }

  /** 进入监测态：暂停倒计时，重新开始 5 秒停笔监测 */
  function enterMonitoring() {
    timerPhase.value = 'monitoring'
    monitorSeconds.value = MONITOR_SECONDS
  }

  /** 进入计时态：30 秒倒计时开始逐秒递减 */
  function enterCounting() {
    timerPhase.value = 'counting'
    monitorSeconds.value = MONITOR_SECONDS
  }

  /** 30 秒耗尽：进入 5 秒缓冲提交窗口 */
  function enterBuffer() {
    timerPhase.value = 'buffer'
    bufferSeconds.value = BUFFER_SECONDS
  }

  /** 单一时钟心跳：按当前子状态推动对应秒数 */
  function tick() {
    if (phase.value !== 'playing') return
    if (timerPhase.value === 'monitoring') {
      monitorSeconds.value--
      if (monitorSeconds.value <= 0) enterCounting()
      return
    }
    if (timerPhase.value === 'counting') {
      if (remainSeconds.value > 0) remainSeconds.value--
      if (remainSeconds.value <= 0) enterBuffer()
      return
    }
    bufferSeconds.value--
    if (bufferSeconds.value <= 0) expireBuffer()
  }

  /** 缓冲窗口耗尽仍无主动提交：自动提交，由后端按上报有效时长判超时 */
  function expireBuffer() {
    if (phase.value !== 'playing' || timerPhase.value !== 'buffer') return
    void play(inputDraft.value.trim(), 'text')
  }

  /** 已消耗的有效答题秒数（打字期间倒计时暂停，不计入），上报后端用于判超时 */
  function consumedActiveSeconds(): number {
    const used = ROUND_SECONDS - Math.max(0, remainSeconds.value)
    const buffered = timerPhase.value === 'buffer'
      ? BUFFER_SECONDS - Math.max(0, bufferSeconds.value)
      : 0
    return Math.max(0, used + buffered)
  }

  function reset() {
    stopClock()
    phase.value = 'idle'
    gameId.value = ''
    mode.value = ''
    level.value = ''
    superType.value = ''
    keywords.value = []
    dynastyId.value = null
    dynastyName.value = ''
    roundNo.value = 1
    remainSeconds.value = ROUND_SECONDS
    timerPhase.value = 'monitoring'
    monitorSeconds.value = MONITOR_SECONDS
    bufferSeconds.value = BUFFER_SECONDS
    retryLeft.value = 2
    historySentences.value = []
    historyCount.value = 0
    winTargetCount.value = null
    isGuest.value = false
    gameResult.value = ''
    lastSentence.value = null
    startSentence.value = null
    candidates.value = []
    checkMsg.value = ''
    errorReason.value = ''
    lastValid.value = null
    isGameOver.value = false
    loading.value = false
    submitting.value = false
    inputDraft.value = ''
  }

  /** 应用开局响应（进入准备阶段） */
  function applyStart(res: GameStartResponse) {
    gameId.value = res.gameId
    mode.value = res.mode || ''
    level.value = res.level || ''
    keywords.value = res.keywords || []
    dynastyId.value = res.dynastyId ?? null
    dynastyName.value = res.dynastyName || ''
    winTargetCount.value = res.winTargetCount ?? null
    isGuest.value = !!res.isGuest
    startSentence.value = res.startSentence || null
    if (gameType.value === 'chain') {
      lastSentence.value = res.startSentence || null
      historySentences.value = res.startSentence ? [res.startSentence.sentence] : []
      historyCount.value = res.chainCount ?? 0
    }
    roundNo.value = 1
    remainSeconds.value = ROUND_SECONDS
    timerPhase.value = 'monitoring'
    monitorSeconds.value = MONITOR_SECONDS
    bufferSeconds.value = BUFFER_SECONDS
    retryLeft.value = 2
    isGameOver.value = false
    gameResult.value = ''
    phase.value = 'prepare'
  }

  /** 应用作答/回合响应：以服务端 remainSeconds 为唯一校准来源 */
  function applyServerState(res: GamePlayResponse) {
    lastValid.value = res.isValid
    if (typeof res.roundNo === 'number') roundNo.value = res.roundNo
    checkMsg.value = res.checkMsg || ''
    errorReason.value = res.errorReason || ''
    if (res.nextReferenceList) candidates.value = res.nextReferenceList
    if (res.nextSentence) lastSentence.value = res.nextSentence
    if (res.historySentences) historySentences.value = res.historySentences
    if (typeof res.historyCount === 'number') historyCount.value = res.historyCount
    if (res.winTargetCount !== undefined) winTargetCount.value = res.winTargetCount ?? null
    isGameOver.value = !!res.isGameOver
    gameResult.value = (res.gameResult || '') as GameOutcome
    if (typeof res.remainSeconds === 'number') remainSeconds.value = res.remainSeconds
    if (typeof res.retryLeft === 'number') retryLeft.value = res.retryLeft
    isGuest.value = !!res.isGuest
    if (res.dynastyId !== undefined) dynastyId.value = res.dynastyId ?? null
    if (res.dynastyName !== undefined) dynastyName.value = res.dynastyName || ''
    if (isGameOver.value) {
      stopClock()
      phase.value = 'settled'
    }
  }

  /** 开局：创建对局并进入准备阶段 */
  async function startGame(type: GameType, params: GameStartParams = {}) {
    reset()
    gameType.value = type
    loading.value = true
    try {
      const res = type === 'feihua' ? await startFeihua(params) : await startChain(params)
      applyStart(res.data)
      return res.data
    } finally {
      loading.value = false
    }
  }

  /** 用户点「开始答题」：后端记录 roundStartTime，前端倒计时直接开始（输入时暂停并进入 5 秒监测） */
  async function beginRound() {
    if (!gameId.value) return null
    submitting.value = true
    try {
      const res = gameType.value === 'feihua'
        ? await beginFeihuaRound(gameId.value)
        : await beginChainRound(gameId.value)
      applyServerState(res.data)
      if (!isGameOver.value) {
        phase.value = 'playing'
        enterCounting()
        startClock()
      }
      return res.data
    } finally {
      submitting.value = false
    }
  }

  /** 提交作答：终止全部计时，等待 AI 期间暂停展示 */
  async function play(sentence: string, inputType: InputType = 'text') {
    if (!gameId.value || isGameOver.value) return null
    stopClock()
    const activeSeconds = consumedActiveSeconds()
    submitting.value = true
    phase.value = 'waiting'
    try {
      const data = { gameId: gameId.value, userSentence: sentence, inputType, activeSeconds }
      const res = gameType.value === 'feihua' ? await playFeihua(data) : await playChain(data)
      applyServerState(res.data)
      inputDraft.value = ''
      if (!isGameOver.value) {
        phase.value = 'playing'
        // 新一轮 / 重试：继续 30 秒倒计时（用户再次落笔时会自动暂停）
        enterCounting()
        startClock()
      }
      return res.data
    } catch (error) {
      // 提交失败：恢复答题态，避免卡在 AI 等待
      if (!isGameOver.value) {
        phase.value = 'playing'
        enterCounting()
        startClock()
      }
      throw error
    } finally {
      submitting.value = false
    }
  }

  /** 提交输入框内容（空输入不消耗重试，由调用方提示） */
  async function submit(inputType: InputType = 'text') {
    const sentence = inputDraft.value.trim()
    if (!sentence) return null
    return play(sentence, inputType)
  }

  /** 主动放弃本局 */
  async function giveUp() {
    if (!gameId.value) {
      phase.value = 'settled'
      return
    }
    const id = gameId.value
    try {
      if (gameType.value === 'feihua') await giveUpFeihua(id)
      else await giveUpChain(id)
    } catch {
      // 放弃请求失败不阻塞结算展示
    }
    stopClock()
    isGameOver.value = true
    gameResult.value = 'giveup'
    phase.value = 'settled'
  }

  // 输入变化：监测态重置 5 秒；计时态暂停倒计时并回到监测态（打字即暂停）；缓冲态为固定 5 秒窗口，不重置
  watch(inputDraft, () => {
    if (phase.value !== 'playing') return
    if (!inputDraft.value.trim()) return
    if (timerPhase.value === 'monitoring') {
      monitorSeconds.value = MONITOR_SECONDS
    } else if (timerPhase.value === 'counting') {
      enterMonitoring()
    }
  })

  return {
    phase,
    gameId,
    gameType,
    mode,
    level,
    superType,
    keywords,
    dynastyId,
    dynastyName,
    roundNo,
    remainSeconds,
    timerPhase,
    monitorSeconds,
    bufferSeconds,
    retryLeft,
    historySentences,
    historyCount,
    winTargetCount,
    isGuest,
    gameResult,
    lastSentence,
    startSentence,
    candidates,
    checkMsg,
    errorReason,
    lastValid,
    isGameOver,
    loading,
    submitting,
    inputDraft,
    isAncient,
    timerPaused,
    timerRemain,
    timerTotal,
    timerHint,
    verdictVariant,
    reset,
    applyStart,
    applyServerState,
    tick,
    startGame,
    beginRound,
    play,
    submit,
    giveUp
  }
})
