import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  getPoemList,
  getPoemById,
  getModernPoems,
  createPoem,
  updatePoem,
  deletePoem,
  searchPoems,
  getPoemsByDynasty,
  getPoemsByCategory,
  getPoemsByPoet,
  likePoem,
  favoritePoem,
  getMyFavorites,
  getFeaturedPoems,
  getRandomPoem,
  getDailyPoem,
  globalSearch,
  getPoemRatings,
  ratePoem,
  requestAiRating,
  getAiRating,
  regenerateAiRating,
  getCurrentUserRating,
  importExternalPoem,
  fixExternalPoems
} from './poem'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('poem API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getPoemList', () => {
    it('调用GET /poems', async () => {
      const params = { pageNum: 1, pageSize: 10 }
      await getPoemList(params)
      expect(request.get).toHaveBeenCalledWith('/poems', { params })
    })
  })

  describe('getPoemById', () => {
    it('调用GET /poems/:id', async () => {
      await getPoemById(123)
      expect(request.get).toHaveBeenCalledWith('/poems/123')
    })
  })

  describe('getModernPoems', () => {
    it('调用GET /poems/modern/page', async () => {
      const params = { pageNum: 1, pageSize: 10, isOriginal: true }
      await getModernPoems(params)
      expect(request.get).toHaveBeenCalledWith('/poems/modern/page', { params })
    })

    it('无参数时调用', async () => {
      await getModernPoems()
      expect(request.get).toHaveBeenCalledWith('/poems/modern/page', { params: undefined })
    })
  })

  describe('createPoem', () => {
    it('调用POST /poems', async () => {
      const data = { title: '测试诗词', content: '测试内容' }
      await createPoem(data)
      expect(request.post).toHaveBeenCalledWith('/poems', data)
    })
  })

  describe('updatePoem', () => {
    it('调用PUT /admin/poems/:id', async () => {
      const data = { id: 123, title: '更新标题' }
      await updatePoem(data)
      expect(request.put).toHaveBeenCalledWith('/admin/poems/123', data)
    })
  })

  describe('deletePoem', () => {
    it('调用DELETE /admin/poems/:id', async () => {
      await deletePoem(123)
      expect(request.delete).toHaveBeenCalledWith('/admin/poems/123')
    })
  })

  describe('searchPoems', () => {
    it('调用GET /search', async () => {
      await searchPoems('测试')
      expect(request.get).toHaveBeenCalledWith('/search', { params: { keyword: '测试' } })
    })
  })

  describe('getPoemsByDynasty', () => {
    it('调用GET /poems 并传入dynastyId', async () => {
      await getPoemsByDynasty(1, { pageNum: 1, pageSize: 10 })
      expect(request.get).toHaveBeenCalledWith('/poems', {
        params: { pageNum: 1, pageSize: 10, dynastyId: 1 }
      })
    })
  })

  describe('getPoemsByCategory', () => {
    it('调用GET /poems 并传入categoryId', async () => {
      await getPoemsByCategory(2, { pageNum: 1, pageSize: 10 })
      expect(request.get).toHaveBeenCalledWith('/poems', {
        params: { pageNum: 1, pageSize: 10, categoryId: 2 }
      })
    })
  })

  describe('getPoemsByPoet', () => {
    it('调用GET /poems 并传入poetId', async () => {
      await getPoemsByPoet(3, { pageNum: 1, pageSize: 10 })
      expect(request.get).toHaveBeenCalledWith('/poems', {
        params: { pageNum: 1, pageSize: 10, poetId: 3 }
      })
    })
  })

  describe('likePoem', () => {
    it('调用POST /poems/:id/like', async () => {
      await likePoem(123)
      expect(request.post).toHaveBeenCalledWith('/poems/123/like')
    })
  })

  describe('favoritePoem', () => {
    it('调用POST /poems/:id/favorite', async () => {
      await favoritePoem(123)
      expect(request.post).toHaveBeenCalledWith('/poems/123/favorite')
    })
  })

  describe('getMyFavorites', () => {
    it('调用GET /poems/favorites', async () => {
      await getMyFavorites({ pageNum: 1, pageSize: 10 })
      expect(request.get).toHaveBeenCalledWith('/poems/favorites', {
        params: { pageNum: 1, pageSize: 10 }
      })
    })
  })

  describe('getFeaturedPoems', () => {
    it('调用GET /poems', async () => {
      await getFeaturedPoems({ pageSize: 5 })
      expect(request.get).toHaveBeenCalledWith('/poems', {
        params: { pageSize: 5, pageNum: 1 }
      })
    })
  })

  describe('getRandomPoem', () => {
    it('调用GET /poems/random', async () => {
      await getRandomPoem()
      expect(request.get).toHaveBeenCalledWith('/poems/random')
    })
  })

  describe('getDailyPoem', () => {
    it('调用GET /poems/daily', async () => {
      await getDailyPoem()
      expect(request.get).toHaveBeenCalledWith('/poems/daily')
    })
  })

  describe('globalSearch', () => {
    it('调用GET /search', async () => {
      await globalSearch('测试')
      expect(request.get).toHaveBeenCalledWith('/search', { params: { keyword: '测试' } })
    })
  })

  describe('getPoemRatings', () => {
    it('调用GET /poems/:id/ratings', async () => {
      await getPoemRatings(123)
      expect(request.get).toHaveBeenCalledWith('/poems/123/ratings')
    })
  })

  describe('ratePoem', () => {
    it('调用POST /poems/:id/ratings', async () => {
      await ratePoem(123, 4.5, '不错')
      expect(request.post).toHaveBeenCalledWith('/poems/123/ratings', null, {
        params: { score: 4.5, comment: '不错' }
      })
    })
  })

  describe('requestAiRating', () => {
    it('调用POST /poems/:id/ratings/ai', async () => {
      await requestAiRating(123)
      expect(request.post).toHaveBeenCalledWith('/poems/123/ratings/ai')
    })
  })

  describe('getAiRating', () => {
    it('调用GET /poems/:id/ratings/ai', async () => {
      await getAiRating(123)
      expect(request.get).toHaveBeenCalledWith('/poems/123/ratings/ai')
    })
  })

  describe('regenerateAiRating', () => {
    it('调用POST /poems/:id/ratings/ai/regenerate', async () => {
      await regenerateAiRating(123)
      expect(request.post).toHaveBeenCalledWith('/poems/123/ratings/ai/regenerate')
    })
  })

  describe('getCurrentUserRating', () => {
    it('调用GET /poems/:id/ratings/me', async () => {
      await getCurrentUserRating(123)
      expect(request.get).toHaveBeenCalledWith('/poems/123/ratings/me')
    })
  })

  describe('importExternalPoem', () => {
    it('调用POST /poems/import-external', async () => {
      const data = { title: '测试', content: '内容', author: '作者', dynasty: '唐' }
      await importExternalPoem(data)
      expect(request.post).toHaveBeenCalledWith('/poems/import-external', data)
    })
  })

  describe('fixExternalPoems', () => {
    it('调用POST /poems/fix-external-poems', async () => {
      await fixExternalPoems()
      expect(request.post).toHaveBeenCalledWith('/poems/fix-external-poems')
    })
  })
})