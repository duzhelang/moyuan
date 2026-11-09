import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const chatMock = vi.fn()
const chatStreamMock = vi.fn()
const getAiModuleConfigMock = vi.fn()
const getSystemKnowledgeListMock = vi.fn()
const ragAvailableMock = vi.fn()
const askRagStreamMock = vi.fn()

vi.mock('@/api/modules/ai', () => ({
  chat: (...args: unknown[]) => chatMock(...args),
  chatStream: (...args: unknown[]) => chatStreamMock(...args),
  getAiModuleConfig: (...args: unknown[]) => getAiModuleConfigMock(...args)
}))

vi.mock('@/api/modules/rag', () => ({
  ragAvailable: (...args: unknown[]) => ragAvailableMock(...args),
  askRag: vi.fn(),
  askRagStream: (...args: unknown[]) => askRagStreamMock(...args)
}))

vi.mock('@/api/modules/systemKnowledge', () => ({
  getSystemKnowledgeList: (...args: unknown[]) => getSystemKnowledgeListMock(...args)
}))

vi.mock('@/api/modules/user', () => ({
  login: vi.fn(),
  register: vi.fn(),
  getUserInfo: vi.fn().mockResolvedValue({ data: { id: 1, username: 'tester', role: 'user' } }),
  updateUser: vi.fn(),
  updatePassword: vi.fn(),
  logout: vi.fn(),
  refreshToken: vi.fn()
}))

import { useAiChatStore } from './aiChat'

const KNOWLEDGE_ROW = {
  id: 1,
  code: 'play.feihualing',
  title: '飞花令',
  category: 'play',
  keywords: '飞花令,行令,飞花',
  aliases: '飞花',
  body: '飞花令是本站的诗词对战玩法。',
  steps: '进入飞花令页面\n选择模式\n输入含令字的诗句',
  faq: JSON.stringify([{ q: '飞花令怎么玩', a: '进入飞花令页面即可开始对战', aliases: ['飞花令在哪玩'] }]),
  route: '/game/feihua'
}

function bootstrap() {
  localStorage.clear()
  sessionStorage.clear()
  localStorage.setItem('token', 'fake-token')
  setActivePinia(createPinia())
  chatMock.mockReset().mockResolvedValue({ data: { reply: '模拟回答' } })
  chatStreamMock.mockReset().mockImplementation(
    async (_payload: unknown, handlers?: { onDelta?: (delta: string) => void }) => {
      handlers?.onDelta?.('模拟回答')
      return '模拟回答'
    }
  )
  getAiModuleConfigMock.mockReset().mockResolvedValue({ data: { moduleCode: 'chat', maxLength: 200, firstResponseLength: 100, promptTemplate: '' } })
  getSystemKnowledgeListMock.mockReset().mockResolvedValue({ data: [KNOWLEDGE_ROW] })
  // 默认关闭 RAG，保持原有「前端检索 + system_chat」链路的断言
  ragAvailableMock.mockReset().mockResolvedValue(false)
  askRagStreamMock.mockReset()
  return useAiChatStore()
}

describe('aiChat store', () => {
  beforeEach(() => {
    bootstrap()
  })

  it('按路由解析画像：诗人详情页为右侧常驻且默认诗词助理', () => {
    const store = useAiChatStore()
    store.syncRoute('/poet/12')

    expect(store.profile.key).toBe('poet:detail')
    expect(store.profile.moduleCode).toBe('poet_chat')
    expect(store.profile.defaultAssistant).toBe('poetry')
    expect(store.profile.autoOpen).toBe(true)
    expect(store.isOpen).toBe(true)
  })

  it('后台与登录页不启用全局小窗', () => {
    const store = useAiChatStore()
    store.syncRoute('/admin/dashboard')

    expect(store.profile.disabled).toBe(true)
    expect(store.isOpen).toBe(false)
    expect(store.profile.entry.show).toBe(false)
  })

  it('切换助理后消息分区隔离（key 带助理后缀）', () => {
    const store = useAiChatStore()
    store.syncRoute('/poem/9')
    const poetryKey = store.currentMessageKey

    store.setAssistant('system')

    expect(store.activeAssistant).toBe('system')
    expect(store.currentMessageKey).toBe(`${store.currentKey}#system`)
    expect(store.currentMessageKey).not.toBe(poetryKey)
  })

  it('系统用法提问命中 FAQ 时直接回答，不调用模型', async () => {
    const store = useAiChatStore()
    store.syncRoute('/poem/9')
    await store.send('飞花令怎么玩')

    expect(chatMock).not.toHaveBeenCalled()
    const last = store.messages[store.messages.length - 1]
    expect(last.content).toContain('进入飞花令页面即可开始对战')
    expect(last.content).toContain('[[/game/feihua]]')
    expect(last.note).toContain('网页用法助理')
  })

  it('系统用法提问未命中 FAQ 时注入资料并走 system_chat 模块', async () => {
    const store = useAiChatStore()
    store.syncRoute('/poem/9')
    store.setAssistant('system')
    await store.send('本站还有哪些玩法')

    expect(chatMock).toHaveBeenCalledTimes(1)
    const payload = chatMock.mock.calls[0][0] as { message: string; moduleCode: string }
    expect(payload.moduleCode).toBe('system_chat')
    expect(payload.message).toContain('【系统资料】')
  })

  it('RAG 服务可用时走 SSE 流式回答并附带来源，不再回退前端检索', async () => {
    ragAvailableMock.mockResolvedValue(true)
    askRagStreamMock.mockImplementation(
      async (_question: string, handlers: { onDelta?: (text: string) => void }) => {
        handlers.onDelta?.('平台有')
        handlers.onDelta?.('两种评分方式')
        return {
          answer: '平台有两种评分方式',
          refused: false,
          sources: [
            {
              file: 'moyuan_business_modules.md',
              section: '16. 诗词评分模块',
              vector: 1,
              bm25: 1,
              rerank: 1,
              neighbor: false
            }
          ],
          embed: 'bge-small',
          generation: 'qwen3.5:4b',
          elapsed: 1
        }
      }
    )

    const store = useAiChatStore()
    store.syncRoute('/poem/9')
    store.setAssistant('system')
    await store.send('平台有哪两种评分方式')

    expect(chatMock).not.toHaveBeenCalled()
    const last = store.messages[store.messages.length - 1]
    expect(last.content).toContain('平台有两种评分方式')
    // 流式占位被最终答案覆盖，来源追加在末尾
    expect(last.content).toContain('moyuan_business_modules.md·16. 诗词评分模块')
    expect(store.messages.filter(item => item.role === 'assistant').length).toBe(2)
  })

  it('RAG 拒答时回退原有检索链路', async () => {
    ragAvailableMock.mockResolvedValue(true)
    askRagStreamMock.mockResolvedValue({
      answer: '知识库未收录该问题，请换一种问法。',
      refused: true,
      sources: [],
      embed: 'bge-small',
      generation: 'qwen3.5:4b',
      elapsed: 0
    })

    const store = useAiChatStore()
    store.syncRoute('/poem/9')
    store.setAssistant('system')
    await store.send('本站还有哪些玩法')

    expect(chatMock).toHaveBeenCalledTimes(1)
    const last = store.messages[store.messages.length - 1]
    expect(last.content).toBe('模拟回答')
  })

  it('诗词内容提问走流式接口与当前页面画像的模块码', async () => {
    const store = useAiChatStore()
    store.syncRoute('/poem/9')
    store.setContext({ type: 'poem', id: 9, name: '静夜思', author: '李白', content: '床前明月光' })
    await store.send('这首诗的作者是谁')

    expect(chatStreamMock).toHaveBeenCalledTimes(1)
    const payload = chatStreamMock.mock.calls[0][0] as {
      messages: Array<{ role: string; content: string }>
      moduleCode: string
    }
    expect(payload.moduleCode).toBe('chat')
    // 系统设定以 system 角色下发，用户问题单独作为 user 消息
    expect(payload.messages[0].role).toBe('system')
    expect(payload.messages[0].content).toContain('静夜思')
    expect(payload.messages[1]).toEqual({ role: 'user', content: '这首诗的作者是谁' })
    expect(store.messages[0].content).toBe('这首诗的作者是谁')
  })

  it('停止生成会中断流式请求并保留已输出内容', async () => {
    chatStreamMock.mockImplementation(
      (_payload: unknown, handlers?: { onDelta?: (delta: string) => void; signal?: AbortSignal }) => {
        handlers?.onDelta?.('已输出的部分')
        return new Promise<string>((_resolve, reject) => {
          handlers?.signal?.addEventListener('abort', () => reject(new Error('aborted')))
        })
      }
    )

    const store = useAiChatStore()
    store.syncRoute('/poem/9')
    const task = store.send('这首诗的作者是谁')
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(store.loading).toBe(true)
    store.stop()

    expect(store.loading).toBe(false)
    const handlers = chatStreamMock.mock.calls[0][1] as { signal: AbortSignal }
    expect(handlers.signal.aborted).toBe(true)

    await task
    const last = store.messages[store.messages.length - 1]
    expect(last.role).toBe('assistant')
    expect(last.content).toBe('已输出的部分')
  })

  it('诗词详情页首次打开自动发送预设问题', async () => {
    const store = useAiChatStore()
    store.syncRoute('/poem/9')
    store.open()

    expect(store.messages[0].content).toBe('请简要介绍一下这首诗')
    // 预设问题是异步发出的：需等待「取模块配置 → 调模型」整条链路落定
    await new Promise(resolve => setTimeout(resolve, 0))
    expect(chatStreamMock).toHaveBeenCalled()
  })

  it('未登录时提示登录且不发起请求', async () => {
    localStorage.clear()
    setActivePinia(createPinia())
    const store = useAiChatStore()
    store.syncRoute('/poem/9')
    await store.send('飞花令怎么玩')

    expect(store.loginPromptVisible).toBe(true)
    expect(chatMock).not.toHaveBeenCalled()
    expect(chatStreamMock).not.toHaveBeenCalled()
  })

  it('诗人详情页的预设问题等上下文注入后再发送', async () => {
    const store = useAiChatStore()
    store.syncRoute('/poet/12')

    expect(store.messages.length).toBe(0)

    store.setContext({ type: 'poet', id: 12, name: '李白' })
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(store.messages[0].content).toBe('李白是谁')
  })

  it('寻章摘句页使用芯片预设，不自动发送', async () => {
    const store = useAiChatStore()
    store.syncRoute('/poem')
    store.open()

    expect(store.presetChips.length).toBeGreaterThan(0)
    expect(store.messages.length).toBe(0)
  })

  it('手动选过助理后，越界提问先等待用户确认，不直接被切走', async () => {
    const store = useAiChatStore()
    store.syncRoute('/poem/9')
    store.setAssistant('system')

    await store.send('这首诗的作者是谁')

    expect(store.activeAssistant).toBe('system')
    expect(store.pendingSwitch?.to).toBe('poetry')
    // 切换助理时只插入能力介绍，越界提问本身未发送
    expect(store.messages.filter(item => item.role === 'user').length).toBe(0)
    expect(store.messages.filter(item => item.role === 'assistant').length).toBe(1)
    expect(chatMock).not.toHaveBeenCalled()
    expect(chatStreamMock).not.toHaveBeenCalled()

    store.confirmSwitchAndSend()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(store.activeAssistant).toBe('poetry')
    expect(store.pendingSwitch).toBeNull()
    expect(store.messages[0].content).toBe('这首诗的作者是谁')
    expect(chatStreamMock).toHaveBeenCalled()
  })

  it('网页用法助理同一问题二次提问命中缓存，不重复调用模型', async () => {
    const store = useAiChatStore()
    store.syncRoute('/poem/9')
    store.setAssistant('system')

    await store.send('站内有哪些可以逛的地方')
    await store.send('站内有哪些可以逛的地方')

    expect(chatMock).toHaveBeenCalledTimes(1)
    // 切换时插入的能力介绍 + 两次提问的两次回答
    expect(store.messages.filter(item => item.role === 'assistant').length).toBe(3)
  })

  it('最小化保留当前会话，恢复后消息仍在', async () => {
    const store = useAiChatStore()
    store.syncRoute('/poem')
    store.send('请简要介绍一下这首诗')
    await new Promise(resolve => setTimeout(resolve, 0))

    const count = store.messages.length
    store.minimize()
    expect(store.minimized).toBe(true)

    store.restore()
    expect(store.minimized).toBe(false)
    expect(store.messages.length).toBe(count)
  })

  it('关闭小窗清空当前页面会话，重新打开为全新对话', async () => {
    const store = useAiChatStore()
    store.syncRoute('/poem')
    store.send('请简要介绍一下这首诗')
    await new Promise(resolve => setTimeout(resolve, 0))
    expect(store.messages.length).toBeGreaterThan(0)

    store.close()
    expect(store.messages.length).toBe(0)

    store.open()
    expect(store.isOpen).toBe(true)
    expect(store.messages.length).toBe(0)
  })
})
