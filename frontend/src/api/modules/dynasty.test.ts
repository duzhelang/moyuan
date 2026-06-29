import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  getDynastyList,
  getDynastyById,
  createDynasty,
  updateDynasty,
  deleteDynasty
} from './dynasty'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('dynasty API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getDynastyList', () => {
    it('调用GET /dynasties', async () => {
      await getDynastyList()
      expect(request.get).toHaveBeenCalledWith('/dynasties')
    })
  })

  describe('getDynastyById', () => {
    it('调用GET /dynasties/:id', async () => {
      await getDynastyById(1)
      expect(request.get).toHaveBeenCalledWith('/dynasties/1')
    })
  })

  describe('createDynasty', () => {
    it('调用POST /dynasties', async () => {
      const data = { name: '唐朝' }
      await createDynasty(data)
      expect(request.post).toHaveBeenCalledWith('/dynasties', data)
    })
  })

  describe('updateDynasty', () => {
    it('调用PUT /dynasties/:id', async () => {
      const data = { name: '宋朝' }
      await updateDynasty(1, data)
      expect(request.put).toHaveBeenCalledWith('/dynasties/1', data)
    })
  })

  describe('deleteDynasty', () => {
    it('调用DELETE /dynasties/:id', async () => {
      await deleteDynasty(1)
      expect(request.delete).toHaveBeenCalledWith('/dynasties/1')
    })
  })
})