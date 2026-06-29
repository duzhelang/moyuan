import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import { addHistory, getHistory, clearHistory } from './history'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn()
  }
}))

describe('history API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('addHistory', () => {
    it('调用POST /history', async () => {
      await addHistory(123, 1)
      expect(request.post).toHaveBeenCalledWith('/history', null, {
        params: { targetId: 123, targetType: 1 }
      })
    })
  })

  describe('getHistory', () => {
    it('调用GET /history', async () => {
      const params = { targetType: 1, pageNum: 1, pageSize: 10 }
      await getHistory(params)
      expect(request.get).toHaveBeenCalledWith('/history', { params })
    })

    it('无参数时调用', async () => {
      await getHistory()
      expect(request.get).toHaveBeenCalledWith('/history', { params: undefined })
    })
  })

  describe('clearHistory', () => {
    it('调用DELETE /history 带targetType', async () => {
      await clearHistory(1)
      expect(request.delete).toHaveBeenCalledWith('/history', {
        params: { targetType: 1 }
      })
    })

    it('调用DELETE /history 不带targetType', async () => {
      await clearHistory()
      expect(request.delete).toHaveBeenCalledWith('/history', {
        params: {}
      })
    })
  })
})