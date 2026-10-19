/**
 * 全站 AI 小窗的全局会话 store
 * - 跨路由不销毁，位置/会话按「路由画像 + 助理」分区持久化到 localStorage
 * - send() 先做轻量意图分流，再决定走诗词链路还是系统知识库链路
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { useUserStore } from './user'
import { chat as chatApi, getAiModuleConfig } from '@/api/modules/ai'
import type { AiModuleConfig } from '@/api/modules/ai'
import { getSystemKnowledgeList } from '@/api/modules/systemKnowledge'
import {
  resolveProfile,
  resolvePosition,
  renderPresetText,
  type AiContext,
  type AiPageProfile
} from '@/configs/ai-chat/pageProfiles'
import type { AssistantId } from '@/configs/ai-chat/assistants'
import { getAssistant } from '@/configs/ai-chat/assistants'
import { buildPoetryPrompt, buildSystemPrompt } from '@/utils/ai-prompt'
import {
  KNOWLEDGE_FALLBACK,
  buildKnowledgeContext,
  detectIntent,
  matchFaq,
  parseEntries,
  pickCategoryEntries,
  searchEntries,
  type KnowledgeEntry,
  type RawKnowledgeEntry
} from '@/utils/systemKnowledgeSearch'
import { collectAllowedRoutes } from '@/utils/renderAiLinks'

const STORAGE_KEY = 'moyuan:aiChat'
/** 每个会话分区最多保留的消息条数 */
const MAX_MESSAGES_PER_KEY = 50
/** 需要保留页面上下文的画像键 */
const CONTEXT_PROFILE_KEYS = ['poet:detail', 'poem:detail', 'poem:list']

export interface AiMessage {
  role: 'user' | 'assistant'
  content: string
  ts: number
  /** 该消息所属助理，用于展示头像 */
  assistant?: AssistantId
  /** 系统提示行（例如「已为你切换到…」） */
  note?: string
}

interface PersistShape {
  posByProfile: Record<string, { x: number; y: number }>
  messagesByKey: Record<string, AiMessage[]>
  context: AiContext | null
  presetSentByKey: Record<string, boolean>
  activeAssistantMap: Record<string, AssistantId>
}

export const useAiChatStore = defineStore('aiChat', () => {
  const userStore = useUserStore()

  const isOpen = ref(false)
  const minimized = ref(false)
  const loginPromptVisible = ref(false)
  const context = ref<AiContext | null>(null)
  const profile = ref<AiPageProfile>(resolveProfile('/'))
  const currentPath = ref('/')
  const activeAssistantMap = ref<Record<string, AssistantId>>({})
  const manualMap = ref<Record<string, boolean>>({})
  const messagesByKey = ref<Record<string, AiMessage[]>>({})
  const posByProfile = ref<Record<string, { x: number; y: number }>>({})
  const presetSentByKey = ref<Record<string, boolean>>({})
  const loadingByKey = ref<Record<string, boolean>>({})
  /** 本次进入该路由后是否被用户手动关闭 */
  const closedByUser = ref(false)
  /** 视口尺寸变化计数，用于重算默认位置 */
  const viewportTick = ref(0)

  const knowledgeEntries = ref<KnowledgeEntry[]>([])
  const knowledgeLoaded = ref(false)
  const allowedRoutes = ref<string[]>([])
  const configCache = ref<Record<string, AiModuleConfig | null>>({})

  /* ---------------- 计算属性 ---------------- */

  const currentKey = computed(() => profile.value.key)

  const activeAssistant = computed<AssistantId>(
    () => activeAssistantMap.value[currentKey.value] || profile.value.defaultAssistant
  )

  /** 消息分区键：同一页面的两个助理互不串台 */
  const currentMessageKey = computed(() => `${currentKey.value}#${activeAssistant.value}`)

  const messages = computed<AiMessage[]>(() => messagesByKey.value[currentMessageKey.value] || [])

  const loading = computed(() => !!loadingByKey.value[currentMessageKey.value])

  const position = computed(() => {
    void viewportTick.value
    const saved = posByProfile.value[currentKey.value]
    if (saved) return saved
    const viewport = { w: window.innerWidth, h: window.innerHeight }
    return resolvePosition(profile.value, profile.value.size, viewport)
  })

  /** 当前是否命中白名单路由可渲染跳转链接 */
  const renderableRoutes = computed(() => allowedRoutes.value)

  /** 胶囊/芯片形态的预设问题（带上下文变量渲染） */
  const presetChips = computed(() =>
    profile.value.presetQuestions.map(item => ({
      text: renderPresetText(item.text, context.value),
      assistant: item.assistant || profile.value.defaultAssistant
    }))
  )

  const greeting = computed(() => profile.value.greeting || '')

  /* ---------------- 持久化 ---------------- */

  function persist() {
    try {
      const trimmed: Record<string, AiMessage[]> = {}
      Object.entries(messagesByKey.value).forEach(([key, list]) => {
        trimmed[key] = list.slice(-MAX_MESSAGES_PER_KEY)
      })
      const payload: PersistShape = {
        posByProfile: posByProfile.value,
        messagesByKey: trimmed,
        context: context.value,
        presetSentByKey: presetSentByKey.value,
        activeAssistantMap: activeAssistantMap.value
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
    } catch (e) {
      console.warn('AI 小窗会话持久化失败', e)
    }
  }

  function load() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (!raw) return
      const data = JSON.parse(raw) as Partial<PersistShape>
      posByProfile.value = data.posByProfile || {}
      messagesByKey.value = data.messagesByKey || {}
      context.value = data.context || null
      presetSentByKey.value = data.presetSentByKey || {}
      activeAssistantMap.value = data.activeAssistantMap || {}
    } catch (e) {
      console.warn('AI 小窗会话恢复失败', e)
    }
  }

  /* ---------------- 路由与可见性 ---------------- */

  /** 路由变化时同步画像；跨页不销毁，已在 poet:detail 等页复刻「常驻面板」 */
  function syncRoute(path: string) {
    currentPath.value = path
    profile.value = resolveProfile(path)
    closedByUser.value = false
    loginPromptVisible.value = false

    // 只有带上下文的页面保留上下文，其余页面（首页/论坛等）清空，避免误带上一页的诗人/诗词
    if (!CONTEXT_PROFILE_KEYS.includes(profile.value.key)) {
      context.value = null
    }

    if (profile.value.disabled) {
      isOpen.value = false
      return
    }
    if (profile.value.autoOpen && !isOpen.value) {
      isOpen.value = true
      minimized.value = false
      ensurePreset()
    }
  }

  function open() {
    if (profile.value.disabled) return
    if (!userStore.isLoggedIn) {
      loginPromptVisible.value = true
      return
    }
    isOpen.value = true
    minimized.value = false
    closedByUser.value = false
    ensurePreset()
  }

  function close() {
    isOpen.value = false
    minimized.value = false
    closedByUser.value = true
  }

  function toggle() {
    if (isOpen.value && !minimized.value) {
      close()
    } else {
      open()
    }
  }

  function minimize() {
    minimized.value = true
  }

  function restore() {
    minimized.value = false
  }

  function closeLoginPrompt() {
    loginPromptVisible.value = false
  }

  /* ---------------- 上下文与助理 ---------------- */

  function setContext(ctx: AiContext | null) {
    context.value = ctx
    persist()
  }

  function clearContext() {
    setContext(null)
  }

  function applyAssistant(id: AssistantId, manual: boolean) {
    activeAssistantMap.value[currentKey.value] = id
    if (manual) manualMap.value[currentKey.value] = true
  }

  /** 手动点击头像切换助理（手动选择优先于意图分流） */
  function setAssistant(id: AssistantId) {
    if (activeAssistant.value === id) return
    applyAssistant(id, true)
    persist()
  }

  function setPos(key: string, pos: { x: number; y: number }) {
    posByProfile.value[key] = pos
    persist()
  }

  /** 重置当前页面位置为画像默认值 */
  function resetPos() {
    delete posByProfile.value[currentKey.value]
    viewportTick.value++
    persist()
  }

  function refreshViewport() {
    viewportTick.value++
  }

  /* ---------------- 预设问题 ---------------- */

  function ensurePreset() {
    const p = profile.value
    if (!p.presetQuestions.length || p.presetMode !== 'auto') return
    if (!userStore.isLoggedIn) return
    if (p.autoOpen && !isOpen.value) return

    const first = p.presetQuestions[0]
    if (first.assistant && first.assistant !== activeAssistant.value) {
      applyAssistant(first.assistant, false)
    }
    const key = currentMessageKey.value
    if (presetSentByKey.value[key]) return
    if ((messagesByKey.value[key] || []).length > 0) {
      presetSentByKey.value[key] = true
      persist()
      return
    }
    presetSentByKey.value[key] = true
    persist()
    send(renderPresetText(first.text, context.value))
  }

  /* ---------------- 消息 ---------------- */

  function pushMessage(key: string, message: AiMessage) {
    const list = messagesByKey.value[key] || []
    list.push(message)
    messagesByKey.value[key] = list.slice(-MAX_MESSAGES_PER_KEY)
  }

  function pushAssistant(text: string, assistant: AssistantId = activeAssistant.value, note?: string) {
    pushMessage(currentMessageKey.value, {
      role: 'assistant',
      content: text,
      ts: Date.now(),
      assistant,
      note
    })
  }

  function clearCurrentMessages() {
    messagesByKey.value[currentMessageKey.value] = []
    presetSentByKey.value[currentMessageKey.value] = false
    persist()
  }

  /* ---------------- 知识库与模块配置 ---------------- */

  async function loadKnowledge(force = false) {
    if (knowledgeLoaded.value && !force) return
    try {
      const res = await getSystemKnowledgeList()
      const entries = parseEntries((res.data || []) as RawKnowledgeEntry[])
      knowledgeEntries.value = entries
      allowedRoutes.value = collectAllowedRoutes(entries.map(entry => entry.route))
    } catch (e) {
      console.warn('系统知识库加载失败', e)
      if (force) {
        knowledgeEntries.value = []
        allowedRoutes.value = []
      }
    } finally {
      knowledgeLoaded.value = true
    }
  }

  async function loadModuleConfig(moduleCode: string): Promise<AiModuleConfig | null> {
    if (moduleCode in configCache.value) return configCache.value[moduleCode]
    try {
      const res = await getAiModuleConfig(moduleCode)
      configCache.value[moduleCode] = res.data
    } catch {
      configCache.value[moduleCode] = null
    }
    return configCache.value[moduleCode]
  }

  /* ---------------- 两条回答链路 ---------------- */

  /** 系统用法链路：FAQ 直答 → 检索注入 → 兜底不给结论 */
  async function answerBySystem(text: string): Promise<string> {
    await loadKnowledge()
    const hit = matchFaq(text, knowledgeEntries.value)
    if (hit) {
      return hit.route ? `${hit.answer}\n\n[[${hit.route}]]` : hit.answer
    }

    const picked = searchEntries(text, knowledgeEntries.value, 3)
    const material = picked.length ? picked : pickCategoryEntries(text, knowledgeEntries.value, 6)
    const materialText = buildKnowledgeContext(material, allowedRoutes.value)
    if (!materialText) return KNOWLEDGE_FALLBACK

    const config = await loadModuleConfig('system_chat')
    const prompt = buildSystemPrompt(text, config, materialText)
    const res = await chatApi({ message: prompt, moduleCode: 'system_chat' })
    return res.data?.reply || KNOWLEDGE_FALLBACK
  }

  /** 诗词链路：按页面画像的模块码与上下文拼装 prompt */
  async function answerByPoetry(text: string): Promise<string> {
    const moduleCode = profile.value.moduleCode
    const config = await loadModuleConfig(moduleCode)
    const key = currentMessageKey.value
    const userCount = (messagesByKey.value[key] || []).filter(item => item.role === 'user').length
    const isFirst = userCount <= 1
    const prompt = buildPoetryPrompt(text, context.value, config, isFirst)
    const res = await chatApi({ message: prompt, moduleCode })
    return res.data?.reply || '抱歉，暂时无法回答'
  }

  /* ---------------- 发送 ---------------- */

  async function send(rawText: string) {
    const text = (rawText || '').trim()
    if (!text) return
    if (!userStore.isLoggedIn) {
      loginPromptVisible.value = true
      return
    }

    let assistant = activeAssistant.value
    const intent = detectIntent(text)
    let note: string | undefined

    if (intent && intent !== assistant) {
      // 手动选择或当前默认助理与提问意图不符：直接切换，并给出可见提示
      applyAssistant(intent, false)
      note = `已为你切换到${getAssistant(intent).name}`
      assistant = intent
    }

    const key = currentMessageKey.value
    if (loadingByKey.value[key]) return

    pushMessage(key, { role: 'user', content: text, ts: Date.now(), assistant })
    loadingByKey.value[key] = true
    persist()

    try {
      const reply = assistant === 'system' ? await answerBySystem(text) : await answerByPoetry(text)
      pushAssistant(reply, assistant, note)
    } catch (e) {
      console.warn('AI 小窗回答失败', e)
      pushAssistant('AI 服务暂时不可用，请稍后重试', assistant)
    } finally {
      loadingByKey.value[key] = false
      persist()
    }
  }

  /** 点击预设芯片：显式指定助理并直接提问 */
  async function askWith(text: string, assistant: AssistantId) {
    if (activeAssistant.value !== assistant) {
      applyAssistant(assistant, true)
    }
    await send(text)
  }

  load()

  return {
    isOpen,
    minimized,
    loginPromptVisible,
    context,
    profile,
    currentKey,
    currentMessageKey,
    activeAssistant,
    messages,
    loading,
    position,
    presetChips,
    greeting,
    renderableRoutes,
    knowledgeEntries,
    syncRoute,
    open,
    close,
    toggle,
    minimize,
    restore,
    closeLoginPrompt,
    setContext,
    clearContext,
    setAssistant,
    setPos,
    resetPos,
    refreshViewport,
    ensurePreset,
    clearCurrentMessages,
    loadKnowledge,
    send,
    askWith
  }
})
