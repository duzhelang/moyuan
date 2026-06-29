import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  getStaticPageByPageKey,
  getAdminStaticPages,
  getAdminStaticPageById,
  updateAdminStaticPage
} from './static-page'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn()
  }
}))

describe('static-page API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getStaticPageByPageKey', () => {
    it('调用GET /static-pages/:pageKey', async () => {
      await getStaticPageByPageKey('terms')
      expect(request.get).toHaveBeenCalledWith('/static-pages/terms')
    })
  })

  describe('getAdminStaticPages', () => {
    it('调用GET /admin/static-pages', async () => {
      await getAdminStaticPages()
      expect(request.get).toHaveBeenCalledWith('/admin/static-pages')
    })
  })

  describe('getAdminStaticPageById', () => {
    it('调用GET /admin/static-pages/:id', async () => {
      await getAdminStaticPageById(1)
      expect(request.get).toHaveBeenCalledWith('/admin/static-pages/1')
    })
  })

  describe('updateAdminStaticPage', () => {
    it('调用PUT /admin/static-pages/:id', async () => {
      const data = { title: '更新标题', content: '更新内容' }
      await updateAdminStaticPage(1, data)
      expect(request.put).toHaveBeenCalledWith('/admin/static-pages/1', data)
    })
  })
})