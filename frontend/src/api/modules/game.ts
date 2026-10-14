import request from '@/utils/request'

/** 诗句引用（候选/接句/起句通用） */
export interface SentenceRef {
  sentence: string
  title: string
  author: string
  matchDesc: string
}

/** 开局响应（飞花令 / 接龙共用，接龙额外携带 AI 起句） */
export interface GameStartResponse {
  gameId: string
  mode: string
  level: string
  keywords: string[]
  winTargetCount: number | null
  startSentence?: SentenceRef
  isGuest: boolean
  /** 接龙：已成功接龙句数 */
  chainCount?: number
  /** 接龙：选择范围朝代 ID；为空表示不限朝代（中国古典诗词） */
  dynastyId?: number | null
  /** 接龙：选择范围朝代名称；为空表示不限朝代 */
  dynastyName?: string | null
}

/** 作答/回合响应 */
export interface GamePlayResponse {
  isValid: boolean
  roundNo: number
  checkMsg: string
  errorReason: string
  /** 飞花令：AI 候选诗句 */
  nextReferenceList?: SentenceRef[]
  /** 接龙：AI 接句（作为下一轮待接句） */
  nextSentence?: SentenceRef
  historySentences: string[]
  historyCount: number
  winTargetCount: number | null
  isGameOver: boolean
  gameResult: '' | 'win' | 'lose' | 'giveup'
  remainSeconds: number
  retryLeft: number
  isGuest: boolean
  /** 接龙选择范围朝代 ID；为空表示不限朝代（中国古典诗词） */
  dynastyId?: number | null
  /** 接龙选择范围朝代名称；为空表示不限朝代 */
  dynastyName?: string | null
}

/** 开局入参 */
export interface GameStartParams {
  mode?: string
  level?: string
  superType?: string
  keywords?: string[]
  inputType?: string
  /** 接龙选择范围朝代 ID；不传表示不限朝代（中国古典诗词） */
  dynastyId?: number
}

/** 作答入参 */
export interface GamePlayParams {
  gameId: string
  userSentence: string
  inputType: string
  /**
   * 前端上报的「已消耗有效答题秒数」：打字期间倒计时暂停，仅统计实际走表时长，
   * 含 30 秒答题 + 5 秒缓冲共 35 秒上限；后端据此判超时（未上报则回退服务端墙钟）。
   */
  activeSeconds?: number
}

// ==================== 飞花令 ====================
export const startFeihua = (data: GameStartParams) =>
  request.post<GameStartResponse>('/game/feihua/start', data)

export const beginFeihuaRound = (gameId: string) =>
  request.post<GamePlayResponse>('/game/feihua/round/begin', { gameId })

export const playFeihua = (data: GamePlayParams) =>
  request.post<GamePlayResponse>('/game/feihua/play', data)

export const giveUpFeihua = (gameId: string) =>
  request.post<void>('/game/feihua/giveup', { gameId })

export const getFeihuaState = (gameId: string) =>
  request.get<GamePlayResponse>('/game/feihua/state', { params: { gameId } })

// ==================== 诗词接龙 ====================
export const startChain = (data: GameStartParams) =>
  request.post<GameStartResponse>('/game/chain/start', data)

export const beginChainRound = (gameId: string) =>
  request.post<GamePlayResponse>('/game/chain/round/begin', { gameId })

export const playChain = (data: GamePlayParams) =>
  request.post<GamePlayResponse>('/game/chain/play', data)

export const giveUpChain = (gameId: string) =>
  request.post<void>('/game/chain/giveup', { gameId })

export const getChainState = (gameId: string) =>
  request.get<GamePlayResponse>('/game/chain/state', { params: { gameId } })
