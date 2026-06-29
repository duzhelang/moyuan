import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  createModel,
  getAllModels,
  getModelById,
  updateModel,
  deleteModel,
  toggleModel,
  setDefaultModel,
  getProviders,
  testConnection,
  getAllModuleConfigs,
  updateModuleConfig,
  getModelsForModule
} from './admin-ai-model'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('admin-ai-model API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('createModel', () => {
    it('调用POST /admin/ai-models', async () => {
      const data = { name: 'test', displayName: '测试', provider: 'zhipu', modelType: 'text' as const, apiUrl: 'http://test.com', apiKey: 'key', modelId: 'model1' }
      await createModel(data)
      expect(request.post).toHaveBeenCalledWith('/admin/ai-models', data)
    })
  })

  describe('getAllModels', () => {
    it('调用GET /admin/ai-models', async () => {
      await getAllModels()
      expect(request.get).toHaveBeenCalledWith('/admin/ai-models')
    })
  })

  describe('getModelById', () => {
    it('调用GET /admin/ai-models/:id', async () => {
      await getModelById(1)
      expect(request.get).toHaveBeenCalledWith('/admin/ai-models/1')
    })
  })

  describe('updateModel', () => {
    it('调用PUT /admin/ai-models/:id', async () => {
      const data = { name: 'test', displayName: '测试', provider: 'zhipu', modelType: 'text' as const, apiUrl: 'http://test.com', apiKey: 'key', modelId: 'model1' }
      await updateModel(1, data)
      expect(request.put).toHaveBeenCalledWith('/admin/ai-models/1', data)
    })
  })

  describe('deleteModel', () => {
    it('调用DELETE /admin/ai-models/:id', async () => {
      await deleteModel(1)
      expect(request.delete).toHaveBeenCalledWith('/admin/ai-models/1')
    })
  })

  describe('toggleModel', () => {
    it('调用POST /admin/ai-models/:id/toggle', async () => {
      await toggleModel(1)
      expect(request.post).toHaveBeenCalledWith('/admin/ai-models/1/toggle')
    })
  })

  describe('setDefaultModel', () => {
    it('调用POST /admin/ai-models/:id/set-default', async () => {
      await setDefaultModel(1)
      expect(request.post).toHaveBeenCalledWith('/admin/ai-models/1/set-default')
    })
  })

  describe('getProviders', () => {
    it('调用GET /admin/ai-models/providers', async () => {
      await getProviders()
      expect(request.get).toHaveBeenCalledWith('/admin/ai-models/providers')
    })
  })

  describe('testConnection', () => {
    it('调用POST /admin/ai-models/:id/test', async () => {
      await testConnection(1)
      expect(request.post).toHaveBeenCalledWith('/admin/ai-models/1/test')
    })
  })

  describe('getAllModuleConfigs', () => {
    it('调用GET /admin/ai-models/modules', async () => {
      await getAllModuleConfigs()
      expect(request.get).toHaveBeenCalledWith('/admin/ai-models/modules')
    })
  })

  describe('updateModuleConfig', () => {
    it('调用PUT /admin/ai-models/modules/:moduleCode', async () => {
      const data = { moduleName: '测试模块', modelId: 1 }
      await updateModuleConfig('test', data)
      expect(request.put).toHaveBeenCalledWith('/admin/ai-models/modules/test', data)
    })
  })

  describe('getModelsForModule', () => {
    it('调用GET /admin/ai-models/modules/:moduleCode/models', async () => {
      await getModelsForModule('test')
      expect(request.get).toHaveBeenCalledWith('/admin/ai-models/modules/test/models')
    })
  })
})