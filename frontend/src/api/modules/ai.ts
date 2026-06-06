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