import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  chat,
  writePoemFromImage,
  analyzePoem,
  matchCouplet,
  getAiModuleConfig,
  ocrImage,
  fillAiContent,
  previewAiContent,
  submitForReview,
  getFillStatus
} from './ai'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

describe('ai API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('chat', () => {
    it('调用POST /ai/chat', async () => {
      const data = { message: '测试消息', model: 'zhipu', moduleCode: 'test' }
      await chat(data)
      expect(request.post).toHaveBeenCalledWith('/ai/chat', data)
    })
  })

  describe('writePoemFromImage', () => {
    it('调用POST /ai/write-poem', async () => {
      const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' })
      await writePoemFromImage(file, 'zhipu', 'glm-4v', 'test')
      expect(request.post).toHaveBeenCalledWith('/ai/write-poem', expect.any(FormData), {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    })

    it('使用默认参数', async () => {
      const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' })
      await writePoemFromImage(file)
      expect(request.post).toHaveBeenCalledWith('/ai/write-poem', expect.any(FormData), {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    })
  })

  describe('analyzePoem', () => {
    it('调用POST /ai/analyze', async () => {
      const data = { poem: '测试诗词', model: 'zhipu', moduleCode: 'test' }
      await analyzePoem(data)
      expect(request.post).toHaveBeenCalledWith('/ai/analyze', data)
    })
  })

  describe('matchCouplet', () => {
    it('调用POST /ai/couplet', async () => {
      const data = { upperCouplet: '上联', model: 'zhipu', moduleCode: 'test' }
      await matchCouplet(data)
      expect(request.post).toHaveBeenCalledWith('/ai/couplet', data)
    })
  })

  describe('getAiModuleConfig', () => {
    it('调用GET /ai/config/:moduleCode', async () => {
      await getAiModuleConfig('test')
      expect(request.get).toHaveBeenCalledWith('/ai/config/test')
    })
  })

  describe('ocrImage', () => {
    it('调用POST /ai/ocr', async () => {
      const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' })
      await ocrImage(file, 'zhipu', 'glm-4v', 'test')
      expect(request.post).toHaveBeenCalledWith('/ai/ocr', expect.any(FormData), {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    })

    it('使用默认参数', async () => {
      const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' })
      await ocrImage(file)
      expect(request.post).toHaveBeenCalledWith('/ai/ocr', expect.any(FormData), {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    })
  })

  describe('fillAiContent', () => {
    it('调用POST /ai/fill-content', async () => {
      const data = { targetType: 'poem', targetId: 1, fieldName: 'translation', moduleCode: 'test' }
      await fillAiContent(data)
      expect(request.post).toHaveBeenCalledWith('/ai/fill-content', data)
    })
  })

  describe('previewAiContent', () => {
    it('调用POST /ai/preview', async () => {
      const data = { targetType: 'poem', targetId: 1, fieldName: 'translation', moduleCode: 'test' }
      await previewAiContent(data)
      expect(request.post).toHaveBeenCalledWith('/ai/preview', data)
    })
  })

  describe('submitForReview', () => {
    it('调用POST /ai/submit-review', async () => {
      const data = { targetType: 'poem', targetId: 1, fieldName: 'translation', content: '内容' }
      await submitForReview(data)
      expect(request.post).toHaveBeenCalledWith('/ai/submit-review', data)
    })
  })

  describe('getFillStatus', () => {
    it('调用GET /ai/fill-status/:targetType/:targetId', async () => {
      await getFillStatus('poem', 1)
      expect(request.get).toHaveBeenCalledWith('/ai/fill-status/poem/1')
    })
  })
})