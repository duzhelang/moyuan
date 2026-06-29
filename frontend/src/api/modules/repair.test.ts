import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  createRepairOrder,
  getMyRepairOrders,
  getRepairOrderDetail,
  closeRepairOrder,
  submitSatisfaction,
  addRepairComment,
  getRepairComments
} from './repair'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn()
  }
}))

describe('repair API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('createRepairOrder', () => {
    it('调用POST /repair/orders', async () => {
      const data = { title: '测试报修', description: '描述', category: '功能异常', priority: 1 }
      await createRepairOrder(data)
      expect(request.post).toHaveBeenCalledWith('/repair/orders', data)
    })
  })

  describe('getMyRepairOrders', () => {
    it('调用GET /repair/orders', async () => {
      const params = { pageNum: 1, pageSize: 10, status: 0 }
      await getMyRepairOrders(params)
      expect(request.get).toHaveBeenCalledWith('/repair/orders', { params })
    })

    it('status为null时调用', async () => {
      const params = { pageNum: 1, pageSize: 10, status: null }
      await getMyRepairOrders(params)
      expect(request.get).toHaveBeenCalledWith('/repair/orders', { params })
    })
  })

  describe('getRepairOrderDetail', () => {
    it('调用GET /repair/orders/:id', async () => {
      await getRepairOrderDetail(123)
      expect(request.get).toHaveBeenCalledWith('/repair/orders/123')
    })
  })

  describe('closeRepairOrder', () => {
    it('调用PUT /repair/orders/:id/close', async () => {
      await closeRepairOrder(123)
      expect(request.put).toHaveBeenCalledWith('/repair/orders/123/close')
    })
  })

  describe('submitSatisfaction', () => {
    it('调用PUT /repair/orders/:id/satisfaction', async () => {
      const data = { satisfaction: 5, comment: '很好' }
      await submitSatisfaction(123, data)
      expect(request.put).toHaveBeenCalledWith('/repair/orders/123/satisfaction', data)
    })
  })

  describe('addRepairComment', () => {
    it('调用POST /repair/orders/:id/comments', async () => {
      const data = { content: '评论内容', images: 'img1.jpg', isInternal: 0 }
      await addRepairComment(123, data)
      expect(request.post).toHaveBeenCalledWith('/repair/orders/123/comments', data)
    })
  })

  describe('getRepairComments', () => {
    it('调用GET /repair/orders/:id/comments', async () => {
      await getRepairComments(123)
      expect(request.get).toHaveBeenCalledWith('/repair/orders/123/comments')
    })
  })
})