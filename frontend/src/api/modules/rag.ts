/**
 * 墨渊知识问答服务（RAG）接口封装
 * 服务地址由 Vite 代理 /rag → http://127.0.0.1:8010
 */
export interface RagEmbeddingModel {
  key: string
  label: string
  provider: 'huggingface' | 'ollama'
  dim: number
  status: 'ready' | 'reserved'
  note: string
  ready: boolean
  chunks: number
  index_dim: number
  message: string
}

export interface RagGenerationModel {
  key: string
  label: string
  model: string
  status: 'ready' | 'reserved'
  note: string
}

export interface RagConfig {
  embed: string
  generation: string
  rerank: boolean
  top_k: number
}

export interface RagModelsPayload {
  config: RagConfig
  embeddings: RagEmbeddingModel[]
  generations: RagGenerationModel[]
}

export interface RagHealth {
  status: string
  embed: string
  generation: string
  index_ready: boolean
  chunks: number
  ollama: boolean
}

async function ragRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/rag${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init
  })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(body.detail || `知识问答服务请求失败（${response.status}）`)
  }
  return body as T
}

export function getRagModels() {
  return ragRequest<RagModelsPayload>('/config')
}

export function updateRagConfig(patch: Partial<RagConfig>) {
  return ragRequest<{ config: RagConfig; message: string }>('/config', {
    method: 'PUT',
    body: JSON.stringify(patch)
  })
}

export function getRagHealth() {
  return ragRequest<RagHealth>('/health')
}

/* -------------------- 小窗问答调用 -------------------- */

export interface RagSource {
  file: string
  section: string
  vector: number
  bm25: number
  rerank: number
  neighbor: boolean
}

export interface RagAskResponse {
  answer: string
  refused: boolean
  sources: RagSource[]
  embed: string
  generation: string
  elapsed: number
}

/** 同步问答（保留给非流式调用方；小窗已改用 askRagStream） */
export function askRag(question: string) {
  return ragRequest<RagAskResponse>('/ask', {
    method: 'POST',
    body: JSON.stringify({ question })
  })
}

/* -------------------- 流式问答（SSE） -------------------- */

export interface RagStreamMeta {
  sources: RagSource[]
  embed: string
  generation: string
  refused: boolean
}

export interface RagStreamHandlers {
  onMeta?: (meta: RagStreamMeta) => void
  onDelta?: (text: string) => void
}

export interface RagStreamResult {
  answer: string
  refused: boolean
  sources: RagSource[]
  embed: string
  generation: string
  elapsed: number
}

/**
 * SSE 流式问答
 * - 必须用 fetch + ReadableStream：Axios 不支持流式，EventSource 不支持 POST
 * - 后端事件：meta（来源/模型/是否拒答）→ delta（增量文本）→ done（耗时）；异常为 error
 */
export async function askRagStream(
  question: string,
  handlers: RagStreamHandlers = {}
): Promise<RagStreamResult> {
  const response = await fetch('/rag/ask/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify({ question })
  })

  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.detail || `知识问答服务请求失败（${response.status}）`)
  }
  if (!response.body) {
    throw new Error('当前环境不支持流式响应')
  }

  const result: RagStreamResult = {
    answer: '',
    refused: false,
    sources: [],
    embed: '',
    generation: '',
    elapsed: 0
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const handleFrame = (frame: string) => {
    let event = 'message'
    const dataLines: string[] = []
    frame.split('\n').forEach(line => {
      if (line.startsWith('event:')) {
        event = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trim())
      }
    })
    if (!dataLines.length) return

    let payload: Record<string, unknown>
    try {
      payload = JSON.parse(dataLines.join('\n')) as Record<string, unknown>
    } catch {
      return
    }

    if (event === 'meta') {
      result.sources = (payload.sources as RagSource[]) || []
      result.embed = (payload.embed as string) || ''
      result.generation = (payload.generation as string) || ''
      result.refused = !!payload.refused
      handlers.onMeta?.({
        sources: result.sources,
        embed: result.embed,
        generation: result.generation,
        refused: result.refused
      })
    } else if (event === 'delta') {
      const text = (payload.text as string) || ''
      if (text) {
        result.answer += text
        handlers.onDelta?.(text)
      }
    } else if (event === 'done') {
      result.elapsed = (payload.elapsed as number) || 0
    } else if (event === 'error') {
      throw new Error((payload.message as string) || '生成失败')
    }
  }

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let index = buffer.indexOf('\n\n')
      while (index !== -1) {
        handleFrame(buffer.slice(0, index))
        buffer = buffer.slice(index + 2)
        index = buffer.indexOf('\n\n')
      }
    }
    buffer += decoder.decode()
    if (buffer.trim()) handleFrame(buffer)
  } catch (e) {
    void reader.cancel()
    throw e
  }

  void reader.cancel()
  return result
}

/** 健康探测缓存，避免每次提问都请求服务 */
const HEALTH_TTL = 30 * 1000
let healthCache: { ok: boolean; ts: number } | null = null

export async function ragAvailable(force = false): Promise<boolean> {
  if (!force && healthCache && Date.now() - healthCache.ts < HEALTH_TTL) {
    return healthCache.ok
  }
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), 3000)
  try {
    const response = await fetch('/rag/health', { signal: controller.signal })
    const body = await response.json().catch(() => ({}))
    const ok = response.ok && body.status === 'ok' && body.index_ready !== false
    healthCache = { ok, ts: Date.now() }
    return ok
  } catch {
    healthCache = { ok: false, ts: Date.now() }
    return false
  } finally {
    clearTimeout(timer)
  }
}
