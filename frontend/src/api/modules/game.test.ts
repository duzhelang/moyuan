import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  startFeihua,
  beginFeihuaRound,
  playFeihua,
  giveUpFeihua,
  getFeihuaState,
  startChain,
  beginChainRound,
  playChain,
  giveUpChain,
  getChainState
} from './game'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

describe('game API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('飞花令', () => {
    it('startFeihua 调用 POST /game/feihua/start', () => {
      const params = { mode: 'ancient', level: 'easy', keywords: ['花'], inputType: 'text' }
      startFeihua(params)
      expect(request.post).toHaveBeenCalledWith('/game/feihua/start', params)
    })

    it('beginFeihuaRound 调用 POST /game/feihua/round/begin', () => {
      beginFeihuaRound('fh_1')
      expect(request.post).toHaveBeenCalledWith('/game/feihua/round/begin', { gameId: 'fh_1' })
    })

    it('playFeihua 调用 POST /game/feihua/play', () => {
      const params = { gameId: 'fh_1', userSentence: '花近高楼伤客心', inputType: 'text' }
      playFeihua(params)
      expect(request.post).toHaveBeenCalledWith('/game/feihua/play', params)
    })

    it('giveUpFeihua 调用 POST /game/feihua/giveup', () => {
      giveUpFeihua('fh_1')
      expect(request.post).toHaveBeenCalledWith('/game/feihua/giveup', { gameId: 'fh_1' })
    })

    it('getFeihuaState 调用 GET /game/feihua/state', () => {
      getFeihuaState('fh_1')
      expect(request.get).toHaveBeenCalledWith('/game/feihua/state', { params: { gameId: 'fh_1' } })
    })
  })

  describe('诗词接龙', () => {
    it('startChain 调用 POST /game/chain/start', () => {
      const params = { inputType: 'text' }
      startChain(params)
      expect(request.post).toHaveBeenCalledWith('/game/chain/start', params)
    })

    it('beginChainRound 调用 POST /game/chain/round/begin', () => {
      beginChainRound('ch_1')
      expect(request.post).toHaveBeenCalledWith('/game/chain/round/begin', { gameId: 'ch_1' })
    })

    it('playChain 调用 POST /game/chain/play', () => {
      const params = { gameId: 'ch_1', userSentence: '月上柳梢头', inputType: 'voice' }
      playChain(params)
      expect(request.post).toHaveBeenCalledWith('/game/chain/play', params)
    })

    it('giveUpChain 调用 POST /game/chain/giveup', () => {
      giveUpChain('ch_1')
      expect(request.post).toHaveBeenCalledWith('/game/chain/giveup', { gameId: 'ch_1' })
    })

    it('getChainState 调用 GET /game/chain/state', () => {
      getChainState('ch_1')
      expect(request.get).toHaveBeenCalledWith('/game/chain/state', { params: { gameId: 'ch_1' } })
    })
  })
})
