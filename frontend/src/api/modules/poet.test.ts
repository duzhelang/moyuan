import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import { getPoetList, getPoetById, submitPoetSuggestion } from './poet'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

describe('poet API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getPoetList', () => {
    it('调用GET /poets', async () => {
      const params = { pageNum: 1, pageSize: 10, dynastyId: 1 }
      await getPoetList(params)
      expect(request.get).toHaveBeenCalledWith('/poets', { params })
    })

    it('无参数时调用', async () => {
      await getPoetList()
      expect(request.get).toHaveBeenCalledWith('/poets', { params: undefined })
    })
  })

  describe('getPoetById', () => {
    it('调用GET /poets/:id', async () => {
      await getPoetById(123)
      expect(request.get).toHaveBeenCalledWith('/poets/123')
    })
  })

  describe('submitPoetSuggestion', () => {
    it('调用POST /poet-suggestions', async () => {
      const data = { poetId: 1, section: 'biography', content: '建议内容', category: '纠错' }
      await submitPoetSuggestion(data)
      expect(request.post).toHaveBeenCalledWith('/poet-suggestions', data)
    })
  })
})