import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import { uploadFile, deleteFile, getFileInfo } from './file'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn()
  }
}))

describe('file API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('uploadFile', () => {
    it('调用POST /files/upload', async () => {
      const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' })
      await uploadFile(file, 'avatar', 1, 'user')
      expect(request.post).toHaveBeenCalledWith('/files/upload', expect.any(FormData), {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    })

    it('不带可选参数调用', async () => {
      const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' })
      await uploadFile(file)
      expect(request.post).toHaveBeenCalledWith('/files/upload', expect.any(FormData), {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    })
  })

  describe('deleteFile', () => {
    it('调用DELETE /files/:fileKey', async () => {
      await deleteFile('test-key')
      expect(request.delete).toHaveBeenCalledWith('/files/test-key')
    })
  })

  describe('getFileInfo', () => {
    it('调用GET /files/:fileKey', async () => {
      await getFileInfo('test-key')
      expect(request.get).toHaveBeenCalledWith('/files/test-key')
    })
  })
})