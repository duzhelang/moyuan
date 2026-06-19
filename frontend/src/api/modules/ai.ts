import request from '@/utils/request'

export interface ChatRequest {
  message: string
  model?: string
}

export interface ChatResponse {
  message: string
  reply: string
  model: string
}

export interface WritePoemResponse {
  poem: string
  model: string
  visionModel: string
}

export interface AnalyzeRequest {
  poem: string
  model?: string
}

export interface AnalyzeResponse {
  poem: string
  analysis: string
  model: string
}

export function chat(data: ChatRequest) {
  return request.post<ChatResponse>('/ai/chat', data)
}

export function writePoemFromImage(image: File, model: string = 'zhipu', visionModel?: string) {
  const formData = new FormData()
  formData.append('image', image)
  formData.append('model', model)
  if (visionModel) {
    formData.append('visionModel', visionModel)
  }
  return request.post<WritePoemResponse>('/ai/write-poem', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function analyzePoem(data: AnalyzeRequest) {
  return request.post<AnalyzeResponse>('/ai/analyze', data)
}

export interface CoupletRequest {
  upperCouplet: string
  model?: string
}

export interface CoupletResponse {
  upperCouplet: string
  result: string
  model: string
}

export function matchCouplet(data: CoupletRequest) {
  return request.post<CoupletResponse>('/ai/couplet', data)
}

export interface AiModuleConfig {
  id: number
  moduleCode: string
  moduleName: string
  modelId: number | null
  requireVision: number
  description: string
  promptTemplate: string
  maxLength: number
  responseStyle: string
  firstResponseLength: number
  enableMarkdown: number
}

export function getAiModuleConfig(moduleCode: string) {
  return request.get<AiModuleConfig>(`/ai/config/${moduleCode}`)
}

export function fillAiContent(data: { targetType: string; targetId: number; fieldName: string }) {
  return request.post<any>('/ai/fill-content', data)
}

export function previewAiContent(data: { targetType: string; targetId: number; fieldName: string }) {
  return request.post<{ content: string }>('/ai/preview', data)
}

export function submitForReview(data: { targetType: string; targetId: number; fieldName: string; content: string }) {
  return request.post<any>('/ai/submit-review', data)
}

export function getFillStatus(targetType: string, targetId: number) {
  return request.get<any[]>(`/ai/fill-status/${targetType}/${targetId}`)
}