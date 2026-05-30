import request from '@/utils/request'

export interface AiModelConfig {
  id?: number
  name: string
  displayName: string
  provider: string
  modelType: 'text' | 'vision' | 'both'
  apiUrl: string
  apiKey: string
  modelId: string
  visionModelId?: string
  maxTokens?: number
  extraConfig?: string
  isEnabled?: number
  isDefault?: number
  sortOrder?: number
}

export function createModel(data: AiModelConfig) {
  return request.post('/api/admin/ai-models', data)
}

export function getAllModels() {
  return request.get('/api/admin/ai-models')
}

export function getModelById(id: number) {
  return request.get(`/api/admin/ai-models/${id}`)
}

export function updateModel(id: number, data: AiModelConfig) {
  return request.put(`/api/admin/ai-models/${id}`, data)
}

export function deleteModel(id: number) {
  return request.delete(`/api/admin/ai-models/${id}`)
}

export function toggleModel(id: number) {
  return request.post(`/api/admin/ai-models/${id}/toggle`)
}

export function setDefaultModel(id: number) {
  return request.post(`/api/admin/ai-models/${id}/set-default`)
}

export function getProviders() {
  return request.get('/api/admin/ai-models/providers')
}

export function testConnection(id: number) {
  return request.post(`/api/admin/ai-models/${id}/test`)
}
