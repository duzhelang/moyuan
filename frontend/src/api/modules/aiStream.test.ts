import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { chatStream } from './ai'

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

  it('限流等 JSON 响应应抛出后端 message', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(
      JSON.stringify({ code: '4291', message: 'AI 请求过于频繁，请稍后再试' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    ))

    await expect(chatStream({ messages: [{ role: 'user', content: 'hi' }] }))
      .rejects.toThrow('AI 请求过于频繁')
  })
})
