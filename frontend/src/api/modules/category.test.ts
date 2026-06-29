import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  getCategoryList,
  getCategoryById,
  createCategory,
  updateCategory,
  deleteCategory
} from './category'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('category API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getCategoryList', () => {
    it('调用GET /categories', async () => {
      await getCategoryList()
      expect(request.get).toHaveBeenCalledWith('/categories')
    })
  })

  describe('getCategoryById', () => {
    it('调用GET /categories/:id', async () => {
      await getCategoryById(1)
      expect(request.get).toHaveBeenCalledWith('/categories/1')
    })
  })

  describe('createCategory', () => {
    it('调用POST /categories', async () => {
      const data = { name: '古体诗' }
      await createCategory(data)
      expect(request.post).toHaveBeenCalledWith('/categories', data)
    })
  })

  describe('updateCategory', () => {
    it('调用PUT /categories/:id', async () => {
      const data = { name: '近体诗' }
      await updateCategory(1, data)
      expect(request.put).toHaveBeenCalledWith('/categories/1', data)
    })
  })

  describe('deleteCategory', () => {
    it('调用DELETE /categories/:id', async () => {
      await deleteCategory(1)
      expect(request.delete).toHaveBeenCalledWith('/categories/1')
    })
  })
})