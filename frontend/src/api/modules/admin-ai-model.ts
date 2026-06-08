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

export interface AiModuleConfig {
  id?: number
  moduleCode: string
  moduleName: string
  modelId: number | null
  requireVision: number
  description?: string
  promptTemplate?: string
  maxLength?: number
  responseStyle?: string
  firstResponseLength?: number
  enableMarkdown?: number
}

export function createModel(data: AiModelConfig) {
  return request.post('/admin/ai-models', data)
}

export function getAllModels() {
  return request.get('/admin/ai-models')
}

export function getModelById(id: number) {
  return request.get(`/admin/ai-models/${id}`)
}

export function updateModel(id: number, data: AiModelConfig) {
  return request.put(`/admin/ai-models/${id}`, data)
}

export function deleteModel(id: number) {
  return request.delete(`/admin/ai-models/${id}`)
}

export function toggleModel(id: number) {
  return request.post(`/admin/ai-models/${id}/toggle`)
}

export function setDefaultModel(id: number) {
  return request.post(`/admin/ai-models/${id}/set-default`)
}

export function getProviders() {
  return request.get('/admin/ai-models/providers')
}

export function testConnection(id: number) {
  return request.post(`/admin/ai-models/${id}/test`)
}

export function getAllModuleConfigs() {
  return request.get('/admin/ai-models/modules')
}

export function updateModuleConfig(moduleCode: string, data: Partial<AiModuleConfig>) {
  return request.put(`/admin/ai-models/modules/${moduleCode}`, data)
}

export function getModelsForModule(moduleCode: string) {
  return request.get(`/admin/ai-models/modules/${moduleCode}/models`)
}
