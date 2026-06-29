import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  savePoetDraft,
  getAdminPoetDrafts,
  getAdminPoetDraftById,
  reviewPoetDraft
} from './poet-draft'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn()
  }
}))

describe('poet-draft API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('savePoetDraft', () => {
    it('调用POST /poet-drafts', async () => {
      const data = { poetId: 1, section: 'biography', content: '草稿内容' }
      await savePoetDraft(data)
      expect(request.post).toHaveBeenCalledWith('/poet-drafts', data)
    })
  })

  describe('getAdminPoetDrafts', () => {
    it('调用GET /admin/poet-drafts', async () => {
      const params = { pageNum: 1, pageSize: 10, poetId: 1, status: 0 }
      await getAdminPoetDrafts(params)
      expect(request.get).toHaveBeenCalledWith('/admin/poet-drafts', { params })
    })
  })

  describe('getAdminPoetDraftById', () => {
    it('调用GET /admin/poet-drafts/:id', async () => {
      await getAdminPoetDraftById(1)
      expect(request.get).toHaveBeenCalledWith('/admin/poet-drafts/1')
    })
  })

  describe('reviewPoetDraft', () => {
    it('调用PUT /admin/poet-drafts/:id/review', async () => {
      await reviewPoetDraft(1, 1, '审核通过')
      expect(request.put).toHaveBeenCalledWith('/admin/poet-drafts/1/review', null, {
        params: { status: 1, reviewComment: '审核通过' }
      })
    })

    it('不带审核意见调用', async () => {
      await reviewPoetDraft(1, 2)
      expect(request.put).toHaveBeenCalledWith('/admin/poet-drafts/1/review', null, {
        params: { status: 2, reviewComment: undefined }
      })
    })
  })
})