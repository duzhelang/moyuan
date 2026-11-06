import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { chatStream } from './ai'
import { refreshSessionForFetch } from '@/utils/request'

// SSE 走 fetch，不经过 axios 拦截器，这里隔离请求层以验证 401 重放逻辑
vi.mock('@/utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
  refreshSessionForFetch: vi.fn()
}))

const mockedRefresh = vi.mocked(refreshSessionForFetch)

function sseResponse(frames: string[]): Response {
  const encoder = new TextEncoder()
  const stream = new ReadableStream({
    start(controller) {
      for (const frame of frames) controller.enqueue(encoder.encode(frame))
      controller.close()
    }
  })
  return new Response(stream, {
    status: 200,
    headers: { 'Content-Type': 'text/event-stream' }
  })
}

describe('chatStream', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
    mockedRefresh.mockReset()
    mockedRefresh.mockResolvedValue(false)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('应拼接 delta 并回调增量', async () => {
    vi.mocked(fetch).mockResolvedValue(sseResponse([
      'data:{"delta":"明"}\n\n',
      'data:{"delta":"月"}\n\n',
      'data:[DONE]\n\n'
    ]))
    const deltas: string[] = []

    const reply = await chatStream(
      { messages: [{ role: 'user', content: 'hi' }] },
      { onDelta: (delta) => deltas.push(delta) }
    )

    expect(reply).toBe('明月')
    expect(deltas).toEqual(['明', '月'])
  })

  it('应把 error 事件抛给调用方', async () => {
    vi.mocked(fetch).mockResolvedValue(sseResponse([
      'data:{"error":"上游失败"}\n\n'
    ]))

    await expect(chatStream({ messages: [{ role: 'user', content: 'hi' }] }))
      .rejects.toThrow('上游失败')
  })

  it('限流等 JSON 响应应抛出后端 message 与业务码', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(
      JSON.stringify({ code: '4291', message: 'AI 请求过于频繁，请稍后再试' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    ))

    let error: (Error & { code?: string }) | undefined
    try {
      await chatStream({ messages: [{ role: 'user', content: 'hi' }] })
    } catch (e) {
      error = e as Error & { code?: string }
    }
    expect(error?.message).toContain('AI 请求过于频繁')
    // 调用方据此判断「重试无意义」，不再回退非流式请求
    expect(error?.code).toBe('4291')
  })

  it('401 时应刷新令牌并重放一次', async () => {
    mockedRefresh.mockResolvedValue(true)
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response('', { status: 401 }))
      .mockResolvedValueOnce(sseResponse(['data:{"delta":"好"}\n\n', 'data:[DONE]\n\n']))

    const reply = await chatStream({ messages: [{ role: 'user', content: 'hi' }] })

    expect(mockedRefresh).toHaveBeenCalledTimes(1)
    expect(fetch).toHaveBeenCalledTimes(2)
    expect(reply).toBe('好')
  })

  it('刷新失败时不再重放，直接抛出错误', async () => {
    mockedRefresh.mockResolvedValue(false)
    vi.mocked(fetch).mockResolvedValue(new Response(
      JSON.stringify({ code: '401', message: '未登录或登录已过期' }),
      { status: 401, headers: { 'Content-Type': 'application/json' } }
    ))

    await expect(chatStream({ messages: [{ role: 'user', content: 'hi' }] }))
      .rejects.toThrow('未登录或登录已过期')
    expect(fetch).toHaveBeenCalledTimes(1)
  })
})
