import request from '@/utils/request'

export interface ChatRequest {
  message: string
  model?: string
  moduleCode?: string
}

export interface ChatResponse {
  message: string
  reply: string
  model: string
}

export interface PingZeChar {
  char: string
  pingze: '平' | '仄' | '中' | string
  pinyin: string
}

export interface PingZeLine {
  text: string
  pattern: string
  chars: PingZeChar[]
  issues: string[]
}

export interface ToneCheckResult {
  passed: boolean
  genre: string
  issues: string[]
  lines: PingZeLine[]
}

export interface WritePoemResponse {
  poem: string
  model: string
  visionModel: string
  /** 生成后的格律校验结果 */
  check?: ToneCheckResult
  /** 是否因格律不合格触发了重写 */
  rewritten?: boolean
}

export interface AnalyzeRequest {
  poem: string
  model?: string
  moduleCode?: string
}

export interface AnalyzeResponse {
  poem: string
  analysis: string
  model: string
}

export function chat(data: ChatRequest) {
  return request.post<ChatResponse>('/ai/chat', data)
}

export function writePoemFromImage(image: File, model: string = 'zhipu', visionModel?: string, moduleCode?: string) {
  const formData = new FormData()
  formData.append('image', image)
  formData.append('model', model)
  if (visionModel) {
    formData.append('visionModel', visionModel)
  }
  if (moduleCode) {
    formData.append('moduleCode', moduleCode)
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
  moduleCode?: string
}

export interface CoupletResponse {
  upperCouplet: string
  result: string
  model: string
  /** 对联格律校验结果 */
  check?: { passed: boolean; issues: string[]; lower?: string }
  /** 是否因格律不合格触发了重写 */
  rewritten?: boolean
}

export function matchCouplet(data: CoupletRequest) {
  return request.post<CoupletResponse>('/ai/couplet', data)
}

/** 平仄标注与格律校验 */
export function toneCheck(text: string) {
  return request.post<ToneCheckResult>('/ai/tone-check', { text })
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

export interface OcrResponse {
  text: string
  model: string
  visionModel: string
}

export function ocrImage(image: File, model: string = 'zhipu', visionModel?: string, moduleCode?: string) {
  const formData = new FormData()
  formData.append('image', image)
  formData.append('model', model)
  if (visionModel) {
    formData.append('visionModel', visionModel)
  }
  if (moduleCode) {
    formData.append('moduleCode', moduleCode)
  }
  return request.post<OcrResponse>('/ai/ocr', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function fillAiContent(data: { targetType: string; targetId: number; fieldName: string; moduleCode?: string }) {
  return request.post<any>('/ai/fill-content', data)
}

export function previewAiContent(data: { targetType: string; targetId: number; fieldName: string; moduleCode?: string }) {
  return request.post<{ content: string }>('/ai/preview', data)
}

export function submitForReview(data: { targetType: string; targetId: number; fieldName: string; content: string }) {
  return request.post<any>('/ai/submit-review', data)
}

export function getFillStatus(targetType: string, targetId: number) {
  return request.get<any[]>(`/ai/fill-status/${targetType}/${targetId}`)
}

/* ==================== 诗宝阁：流式对话与会话 ==================== */

export interface ChatStreamMessage {
  role: 'system' | 'user' | 'assistant'
  content: string
}

export interface ChatStreamPayload {
  messages: ChatStreamMessage[]
  moduleCode?: string
  temperature?: number
  maxTokens?: number
}

export interface ChatStreamHandlers {
  /** 增量文本回调 */
  onDelta?: (delta: string) => void
  /** 用于「停止生成」 */
  signal?: AbortSignal
}

/**
 * SSE 流式问答。
 * <p>
 * Axios 不支持读取流，EventSource 不支持 POST，因此使用 fetch + ReadableStream。
 * 后端事件格式：data:{"delta":"..."} → data:[DONE]；异常为 data:{"error":"..."}。
 */
export async function chatStream(
  payload: ChatStreamPayload,
  handlers: ChatStreamHandlers = {}
): Promise<string> {
  const base = import.meta.env.VITE_API_BASE_URL || '/api'
  const token = localStorage.getItem('token') || ''
  const response = await fetch(`${base}/ai/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(payload),
    signal: handlers.signal
  })

  if (!response.ok) {
    const text = await response.text().catch(() => '')
    throw new Error(text || `AI 流式请求失败（${response.status}）`)
  }
  // 限流/参数错误时后端会返回普通 JSON（HTTP 200），需把 message 抛给调用方
  const contentType = response.headers.get('content-type') || ''
  if (!contentType.includes('text/event-stream')) {
    const text = await response.text().catch(() => '')
    let message = ''
    try {
      const body = JSON.parse(text) as { message?: string }
      message = body?.message || ''
    } catch {
      // 非 JSON 响应按原始文本兜底
    }
    throw new Error(message || `AI 服务异常（${response.status}）`)
  }
  if (!response.body) {
    throw new Error('当前浏览器不支持流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let full = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')

    let frameEnd = buffer.indexOf('\n\n')
    while (frameEnd >= 0) {
      const frame = buffer.slice(0, frameEnd)
      buffer = buffer.slice(frameEnd + 2)
      for (const line of frame.split('\n')) {
        if (!line.startsWith('data:')) continue
        const data = line.slice(5).trim()
        if (!data) continue
        if (data === '[DONE]') return full
        let event: { delta?: string; error?: string }
        try {
          event = JSON.parse(data)
        } catch {
          continue
        }
        if (event.error) throw new Error(event.error)
        if (typeof event.delta === 'string') {
          full += event.delta
          handlers.onDelta?.(event.delta)
        }
      }
      frameEnd = buffer.indexOf('\n\n')
    }
  }

  return full
}

/** 服务端会话消息（与 ChatMessage 字段保持一致） */
export interface ChatSessionMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  skillId?: string
  time: number
}

export function getChatSession(roleId: string) {
  return request.get<ChatSessionMessage[]>(`/ai/chat/sessions/${roleId}`)
}

export function saveChatSession(roleId: string, messages: ChatSessionMessage[]) {
  return request.put<void>(`/ai/chat/sessions/${roleId}`, { messages })
}

export function clearChatSession(roleId: string) {
  return request.delete<void>(`/ai/chat/sessions/${roleId}`)
}