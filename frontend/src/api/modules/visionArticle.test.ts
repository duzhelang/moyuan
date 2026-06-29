import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  getVisionArticleList,
  getFeaturedVisionArticles,
  getVisionArticleById,
  likeVisionArticle
} from './visionArticle'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

describe('visionArticle API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getVisionArticleList', () => {
    it('调用GET /vision/articles', async () => {
      const params = { pageNum: 1, pageSize: 10, category: '诗词赏析' }
      await getVisionArticleList(params)
      expect(request.get).toHaveBeenCalledWith('/vision/articles', { params })
    })

    it('无参数时调用', async () => {
      await getVisionArticleList()
      expect(request.get).toHaveBeenCalledWith('/vision/articles', { params: undefined })
    })
  })

  describe('getFeaturedVisionArticles', () => {
    it('调用GET /vision/articles/featured', async () => {
      const params = { pageNum: 1, pageSize: 5 }
      await getFeaturedVisionArticles(params)
      expect(request.get).toHaveBeenCalledWith('/vision/articles/featured', { params })
    })

    it('无参数时调用', async () => {
      await getFeaturedVisionArticles()
      expect(request.get).toHaveBeenCalledWith('/vision/articles/featured', { params: undefined })
    })
  })

  describe('getVisionArticleById', () => {
    it('调用GET /vision/articles/:id', async () => {
      await getVisionArticleById(1)
      expect(request.get).toHaveBeenCalledWith('/vision/articles/1')
    })
  })

  describe('likeVisionArticle', () => {
    it('调用POST /vision/articles/:id/like', async () => {
      await likeVisionArticle(1)
      expect(request.post).toHaveBeenCalledWith('/vision/articles/1/like')
    })
  })
})