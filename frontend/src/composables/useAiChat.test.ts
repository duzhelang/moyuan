import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useAiChat } from './useAiChat'
import {
  chat,
  chatStream,
  clearChatSession,
  getChatSession,
  saveChatSession,
  toneCheck
} from '@/api/modules/ai'

vi.mock('@/api/modules/ai', () => ({
  chat: vi.fn(),
  chatStream: vi.fn(),
  toneCheck: vi.fn(),
  getChatSession: vi.fn(),
  saveChatSession: vi.fn(),
  clearChatSession: vi.fn()
}))

vi.mock('@/api/modules/poem', () => ({
  searchPoems: vi.fn()
}))

const mockedGetChatSession = vi.mocked(getChatSession)
const mockedSaveChatSession = vi.mocked(saveChatSession)
const mockedClearChatSession = vi.mocked(clearChatSession)
const mockedChatStream = vi.mocked(chatStream)
const mockedToneCheck = vi.mocked(toneCheck)

describe('useAiChat', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    mockedGetChatSession.mockResolvedValue({ data: [] } as never)
    mockedSaveChatSession.mockResolvedValue({ data: undefined } as never)
    mockedClearChatSession.mockResolvedValue({ data: undefined } as never)
  })

  it('发送自由对话时应携带角色 system 并流式追加回复', async () => {
    mockedChatStream.mockImplementation(async (_payload, handlers) => {
      handlers?.onDelta?.('明月')
      return '明月'
    })

    const chatState = useAiChat()
    chatState.input.value = '你好'
    await chatState.send()

    expect(mockedChatStream).toHaveBeenCalledTimes(1)
    const payload = mockedChatStream.mock.calls[0][0]
    expect(payload.messages[0].role).toBe('system')
    expect(payload.messages.at(-1)).toEqual({ role: 'user', content: '你好' })

    expect(chatState.messages.value.map((message) => message.role)).toEqual(['user', 'assistant'])
    expect(chatState.messages.value.at(-1)?.content).toBe('明月')
  })

  it('第二次发送应把上一轮 user/assistant 带入多轮上下文', async () => {
    mockedChatStream.mockResolvedValue('第一轮回复')

    const chatState = useAiChat()
    chatState.input.value = '第一问'
    await chatState.send()
    chatState.input.value = '第二问'
    await chatState.send()

    const secondPayload = mockedChatStream.mock.calls[1][0]
    expect(secondPayload.messages.map((message) => message.content)).toEqual([
      expect.stringContaining('诗词'),
      '第一问',
      '第一轮回复',
      '第二问'
    ])
  })

  it('平仄能力应走本地引擎并生成 tone 可解析的 JSON', async () => {
    mockedToneCheck.mockResolvedValue({
      data: {
        passed: false,
        genre: '七言绝句',
        issues: ['第2句末字非平声'],
        lines: [{ text: '两个黄鹂鸣翠柳', pattern: '仄仄平平平仄仄', issues: [] }]
      }
    } as never)

    const chatState = useAiChat()
    chatState.switchRole('judge')
    chatState.toggleSkill('tone')
    chatState.input.value = '两个黄鹂鸣翠柳'
    await chatState.send()

    expect(mockedToneCheck).toHaveBeenCalledWith('两个黄鹂鸣翠柳')
    expect(mockedChatStream).not.toHaveBeenCalled()

    const reply = chatState.messages.value.at(-1)
    expect(reply?.skillId).toBe('tone')
    const parsed = JSON.parse(reply?.content || '{}')
    expect(parsed.valid).toBe(false)
    expect(parsed.format).toBe('七言绝句')
    expect(parsed.lines[0].tone).toBe('仄仄平平平仄仄')
  })
})
