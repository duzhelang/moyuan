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

/** 同步问答（小窗接入第一版，后续可切 SSE 流式） */
export function askRag(question: string) {
  return ragRequest<RagAskResponse>('/ask', {
    method: 'POST',
    body: JSON.stringify({ question })
  })
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
