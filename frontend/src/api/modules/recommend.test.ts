import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import { recommendApi, clampTopK, normalizeRecommendItems } from './recommend'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

describe('recommend API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('get', () => {
    it('调用GET /recommend 并夹紧 topK', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: { items: [] } })
      await recommendApi.get({ strategy: 'fusion', topK: 99 })
      expect(request.get).toHaveBeenCalledWith('/recommend', expect.objectContaining({
        params: { strategy: 'fusion', topK: 20 },
        silent: true,
        skipCanceledToast: true
      }))
    })

    it('透传 AbortSignal', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: { items: [] } })
      const controller = new AbortController()
      await recommendApi.get({ strategy: 'era' }, controller.signal)
      expect(request.get).toHaveBeenCalledWith('/recommend', expect.objectContaining({
        signal: controller.signal
      }))
    })
  })

  describe('models', () => {
    it('调用GET /recommend/models（静默）', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: { models: [], availableStrategies: ['rule'] } })
      await recommendApi.models()
      expect(request.get).toHaveBeenCalledWith('/recommend/models', expect.objectContaining({
        silent: true
      }))
    })
  })

  describe('rule', () => {
    it('调用GET /search/poems/recommended', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: [] })
      await recommendApi.rule(6)
      expect(request.get).toHaveBeenCalledWith('/search/poems/recommended', expect.objectContaining({
        params: { limit: 6 }
      }))
    })
  })

  describe('recordEvent', () => {
    it('调用POST /recommend/events 且失败静默', async () => {
      vi.mocked(request.post).mockResolvedValue({ data: null })
      await recommendApi.recordEvent({ page: 'home', from: 'rule', to: 'fusion' })
      expect(request.post).toHaveBeenCalledWith(
        '/recommend/events',
        { page: 'home', from: 'rule', to: 'fusion' },
        { silent: true }
      )
    })

    it('上报失败不抛出', async () => {
      vi.mocked(request.post).mockRejectedValue(new Error('network'))
      await expect(
        recommendApi.recordEvent({ page: 'home', from: 'rule', to: 'global' })
      ).resolves.toBeUndefined()
    })
  })
})

describe('clampTopK', () => {
  it('夹紧到 1..20', () => {
    expect(clampTopK(0)).toBe(1)
    expect(clampTopK(99)).toBe(20)
    expect(clampTopK(6)).toBe(6)
  })

  it('非法值回落默认', () => {
    expect(clampTopK(undefined, 6)).toBe(6)
    expect(clampTopK(NaN, 10)).toBe(10)
  })
})

describe('normalizeRecommendItems', () => {
  it('过滤无 id 项并补齐字段', () => {
    const items = normalizeRecommendItems([
      { id: 1, title: '静夜思', content: '床前明月光', author: '李白', dynasty: '唐', source: 'local' },
      { title: '无id', content: '' }
    ])
    expect(items).toHaveLength(1)
    expect(items[0]).toMatchObject({ id: 1, title: '静夜思', source: 'local' })
  })

  it('非数组输入返回空数组', () => {
    expect(normalizeRecommendItems(null)).toEqual([])
    expect(normalizeRecommendItems(undefined)).toEqual([])
  })
})
