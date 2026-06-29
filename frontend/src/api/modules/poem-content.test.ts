import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import { getCachedContent, saveCachedContent } from './poem-content'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

describe('poem-content API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getCachedContent', () => {
    it('调用GET /poem-content/cache', async () => {
      await getCachedContent('静夜思', '李白', 'translation')
      expect(request.get).toHaveBeenCalledWith('/poem-content/cache', {
        params: { poemTitle: '静夜思', poetName: '李白', contentType: 'translation' }
      })
    })
  })

  describe('saveCachedContent', () => {
    it('调用POST /poem-content/cache', async () => {
      const data = {
        poemTitle: '静夜思',
        poetName: '李白',
        contentType: 'translation',
        content: '译文内容',
        source: 'ai'
      }
      await saveCachedContent(data)
      expect(request.post).toHaveBeenCalledWith('/poem-content/cache', data)
    })
  })
})