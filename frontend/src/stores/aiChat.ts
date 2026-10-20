/**
 * 全站 AI 小窗的全局会话 store
 * - 跨路由不销毁，位置/会话按「路由画像 + 助理」分区持久化到 localStorage
 * - send() 先做轻量意图分流，再决定走诗词链路还是系统知识库链路
 */
import { defineStore } from 'pinia'
import { computed, ref, shallowRef } from 'vue'
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
/** 网页用法助理问答缓存（会话级） */
const ANSWER_CACHE_KEY = 'moyuan:aiChatAnswers'
/** 缓存有效期：30 分钟 */
const ANSWER_CACHE_TTL = 30 * 60 * 1000
/** 缓存条目上限，超出后按时间淘汰 */
const ANSWER_CACHE_LIMIT = 100
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
  /** 用户在页面上手动调整过的尺寸 */
  sizeByProfile: Record<string, { w: number; h: number }>
  messagesByKey: Record<string, AiMessage[]>
  context: AiContext | null
  presetSentByKey: Record<string, boolean>
  activeAssistantMap: Record<string, AssistantId>
  manualMap: Record<string, boolean>
}

/** 等待用户确认助理切换的越界提问 */
export interface PendingSwitch {
  from: AssistantId
  to: AssistantId
  text: string
}

interface AnswerCacheItem {
  answer: string
  ts: number
  version: string
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
  const sizeByProfile = ref<Record<string, { w: number; h: number }>>({})
  const presetSentByKey = ref<Record<string, boolean>>({})
  const loadingByKey = ref<Record<string, boolean>>({})
  /** 越界提问：等用户确认是否切换助理 */
  const pendingSwitch = ref<PendingSwitch | null>(null)
  /** 本次进入该路由后是否被用户手动关闭 */
  const closedByUser = ref(false)
  /** 页内嵌入容器：由页面 onMounted 注册，窗口 Teleport 到该元素内渲染 */
  const dockEl = shallowRef<HTMLElement | null>(null)
  /** 容器当前是否真实可见（窄屏下右栏会被隐藏，此时窗口回退为浮窗） */
  const dockUsable = ref(false)
  /** 卡内展开后拖动脱离卡片：页面据此隐藏承载卡片，窗口转为浮窗 */
  const detached = ref(false)
  /** 视口尺寸变化计数，用于重算默认位置 */
  const viewportTick = ref(0)
  /** 会话代次：关闭小窗（清空会话）时递增，用于丢弃已作废的异步回答 */
  const sessionSeq = ref(0)

  const knowledgeEntries = ref<KnowledgeEntry[]>([])
  const knowledgeLoaded = ref(false)
  /** 知识库请求进行中，避免重复并发拉取 */
  const knowledgeLoading = ref(false)
  const allowedRoutes = ref<string[]>([])
  const configCache = ref<Record<string, AiModuleConfig | null>>({})

  /* ---------------- 计算属性 ---------------- */

  const currentKey = computed(() => profile.value.key)

  /** 当前页面小窗尺寸：优先用户手动调整过的值，否则取画像默认值 */
  const currentSize = computed(() => sizeByProfile.value[currentKey.value] || profile.value.size)

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
    return resolvePosition(profile.value, currentSize.value, viewport)
  })

  /** 当前是否命中白名单路由可渲染跳转链接 */
  const renderableRoutes = computed(() => allowedRoutes.value)

  /** 路由 → 知识条目标题，用于生成可读的跳转文案 */
  const routeTitles = computed<Record<string, string>>(() => {
    const map: Record<string, string> = {}
    knowledgeEntries.value.forEach(entry => {
      if (entry.route && allowedRoutes.value.includes(entry.route)) {
        map[entry.route] = entry.title
      }
    })
    return map
  })

  /** 知识库版本：取所有条目的最新更新时间，知识变更后缓存自动失效 */
  const knowledgeVersion = computed(() => {
    const times = knowledgeEntries.value
      .map(entry => entry.updatedAt || '')
      .filter(Boolean)
      .sort()
    return times.length ? times[times.length - 1] : 'v0'
  })

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
        sizeByProfile: sizeByProfile.value,
        messagesByKey: trimmed,
        context: context.value,
        presetSentByKey: presetSentByKey.value,
        activeAssistantMap: activeAssistantMap.value,
        manualMap: manualMap.value
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
      sizeByProfile.value = data.sizeByProfile || {}
      messagesByKey.value = data.messagesByKey || {}
      context.value = data.context || null
      presetSentByKey.value = data.presetSentByKey || {}
      activeAssistantMap.value = data.activeAssistantMap || {}
      manualMap.value = data.manualMap || {}
    } catch (e) {
      console.warn('AI 小窗会话恢复失败', e)
    }
  }

  /* ---------------- 路由与可见性 ---------------- */

  /** 路由变化时同步画像；跨页不销毁，已在 poet:detail 等页复刻「常驻面板」 */
  function syncRoute(path: string) {
    const nextProfile = resolveProfile(path)
    currentPath.value = path
    // 同一画像内仅查询参数变化（如筛选、分页）：保留当前展开状态与会话，避免面板被意外收起
    if (nextProfile.key === profile.value.key && !nextProfile.disabled) {
      profile.value = nextProfile
      return
    }
    profile.value = nextProfile
    closedByUser.value = false
    detached.value = false
    loginPromptVisible.value = false
    pendingSwitch.value = null

    // 只有带上下文的页面保留上下文，其余页面（首页/论坛等）清空，避免误带上一页的诗人/诗词
    if (!CONTEXT_PROFILE_KEYS.includes(profile.value.key)) {
      context.value = null
    }

    if (profile.value.disabled) {
      isOpen.value = false
      return
    }
    if (profile.value.autoOpen) {
      // 常驻展开的页面：即使上一页最小化过，进入本页也要恢复完整面板
      isOpen.value = true
      minimized.value = false
      ensurePreset()
    } else if (profile.value.collapseOnEnter) {
      // 页内自带折叠面板的页面：默认收起，保留既有会话
      isOpen.value = false
      minimized.value = false
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
    // 打开时补一次知识库预热：启动阶段网络异常导致的加载失败可在这次重试
    void loadKnowledge()
    ensurePreset()
  }

  function close() {
    isOpen.value = false
    minimized.value = false
    closedByUser.value = true
    detached.value = false
    // 关闭即结束本次会话：清空当前页面两个助理的消息，保留页面上下文
    sessionSeq.value++
    clearProfileMessages()
  }

  function toggle() {
    if (isOpen.value && !minimized.value) {
      close()
    } else {
      open()
    }
  }

  /** 页内折叠面板收起：仅隐藏窗口，保留会话与页面上下文（区别于 close 的关闭清空） */
  function hide() {
    isOpen.value = false
    minimized.value = false
    detached.value = false
  }

  /** 拖动脱离 / 复位回卡片：页面据此隐藏或恢复紫色卡片 */
  function setDetached(value: boolean) {
    detached.value = value
  }

  /** 页面注册/注销页内嵌入容器，窗口据此决定 Teleport 目标 */
  function registerDock(el: HTMLElement | null) {
    dockEl.value = el
    if (!el) dockUsable.value = false
  }

  /** 窗口组件测量后回写容器可用性，供入口按钮决定是否展示 */
  function setDockUsable(value: boolean) {
    dockUsable.value = value
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
    // 上下文注入后再触发预设问题（诗人详情页首屏加载完诗人信息才具备提问条件）
    if (isOpen.value) ensurePreset()
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
    pendingSwitch.value = null
    if (id === activeAssistant.value) return
    // 首次切到该助理时，主动说明它能做什么，避免用户不知道该怎么问
    const targetKey = `${currentKey.value}#${id}`
    const hasHistory = (messagesByKey.value[targetKey] || []).length > 0
    applyAssistant(id, true)
    if (!hasHistory) {
      pushMessage(targetKey, {
        role: 'assistant',
        content: getAssistant(id).intro,
        ts: Date.now(),
        assistant: id
      })
    }
    persist()
  }

  function setPos(key: string, pos: { x: number; y: number }) {
    posByProfile.value[key] = pos
    persist()
  }

  /** 记录用户拖拽调整后的尺寸 */
  function setSize(key: string, size: { w: number; h: number }) {
    sizeByProfile.value[key] = size
    persist()
  }

  /** 恢复当前页面的默认布局（位置与尺寸） */
  function resetPos() {
    delete posByProfile.value[currentKey.value]
    delete sizeByProfile.value[currentKey.value]
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
    // 依赖页面上下文的预设问题（如「${poetName}是谁」）需等上下文注入后再触发
    if (first.text.includes('${') && !(context.value?.name || '').trim()) return

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
    void send(renderPresetText(first.text, context.value), { skipIntent: true })
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

  /** 关闭小窗时清空当前页面上两个助理的全部会话（其它页面互不影响） */
  function clearProfileMessages() {
    const prefix = `${currentKey.value}#`
    Object.keys(messagesByKey.value).forEach(key => {
      if (key.startsWith(prefix)) {
        delete messagesByKey.value[key]
        delete presetSentByKey.value[key]
        delete loadingByKey.value[key]
      }
    })
    pendingSwitch.value = null
    persist()
  }

  /* ---------------- 知识库与模块配置 ---------------- */

  async function loadKnowledge(force = false) {
    if (knowledgeLoading.value) return
    if (knowledgeLoaded.value && !force) return
    knowledgeLoading.value = true
    try {
      const res = await getSystemKnowledgeList()
      const entries = parseEntries((res.data || []) as RawKnowledgeEntry[])
      knowledgeEntries.value = entries
      allowedRoutes.value = collectAllowedRoutes(entries.map(entry => entry.route))
      knowledgeLoaded.value = true
    } catch (e) {
      // 失败不标记已加载，下次调用可重试，避免一次网络异常导致跳转链接长期失效
      console.warn('系统知识库加载失败', e)
      knowledgeLoaded.value = false
    } finally {
      knowledgeLoading.value = false
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

  /* ---------------- 问答缓存（网页用法助理） ---------------- */

  /** 归一化问题文本，避免空格与标点差异导致缓存未命中 */
  function normalizeQuestion(text: string): string {
    return text
      .toLowerCase()
      .replace(/[\s，。？！、,.?!:：;；"'“”‘’()（）【】[\]<>《》~～·\-—_]/g, '')
  }

  function cacheKeyOf(question: string, assistant: AssistantId, version: string): string {
    return `${version}|${assistant}|${normalizeQuestion(question)}`
  }

  /** 读取缓存；版本不一致或超时视为未命中并顺手清理 */
  function readAnswerCache(question: string, assistant: AssistantId, version: string): string | null {
    try {
      const raw = sessionStorage.getItem(ANSWER_CACHE_KEY)
      if (!raw) return null
      const all = JSON.parse(raw) as Record<string, AnswerCacheItem>
      const key = cacheKeyOf(question, assistant, version)
      const item = all[key]
      if (!item) return null
      if (item.version !== version || Date.now() - item.ts > ANSWER_CACHE_TTL) {
        delete all[key]
        sessionStorage.setItem(ANSWER_CACHE_KEY, JSON.stringify(all))
        return null
      }
      return item.answer
    } catch {
      return null
    }
  }

  /** 写入缓存，按过期时间与数量上限清理旧条目 */
  function writeAnswerCache(question: string, assistant: AssistantId, version: string, answer: string) {
    if (!answer) return
    try {
      const raw = sessionStorage.getItem(ANSWER_CACHE_KEY)
      const all = raw ? (JSON.parse(raw) as Record<string, AnswerCacheItem>) : {}
      Object.keys(all).forEach(key => {
        if (all[key].version !== version || Date.now() - all[key].ts > ANSWER_CACHE_TTL) {
          delete all[key]
        }
      })
      all[cacheKeyOf(question, assistant, version)] = { answer, ts: Date.now(), version }
      const kept = Object.entries(all)
        .sort((a, b) => b[1].ts - a[1].ts)
        .slice(0, ANSWER_CACHE_LIMIT)
      sessionStorage.setItem(ANSWER_CACHE_KEY, JSON.stringify(Object.fromEntries(kept)))
    } catch (e) {
      console.warn('AI 小窗问答缓存写入失败', e)
    }
  }

  /* ---------------- 两条回答链路 ---------------- */

  /** 系统用法链路：缓存 → FAQ 直答 → 检索注入 → 兜底不给结论 */
  async function answerBySystem(text: string): Promise<string> {
    await loadKnowledge()
    const version = knowledgeVersion.value
    const cached = readAnswerCache(text, 'system', version)
    if (cached) return cached

    const hit = matchFaq(text, knowledgeEntries.value)
    if (hit) {
      const answer = hit.route ? `${hit.answer}\n\n[[${hit.route}]]` : hit.answer
      writeAnswerCache(text, 'system', version, answer)
      return answer
    }

    const picked = searchEntries(text, knowledgeEntries.value, 3)
    const material = picked.length ? picked : pickCategoryEntries(text, knowledgeEntries.value, 6)
    const materialText = buildKnowledgeContext(material, allowedRoutes.value)
    if (!materialText) {
      writeAnswerCache(text, 'system', version, KNOWLEDGE_FALLBACK)
      return KNOWLEDGE_FALLBACK
    }

    const config = await loadModuleConfig('system_chat')
    const prompt = buildSystemPrompt(text, config, materialText)
    const res = await chatApi({ message: prompt, moduleCode: 'system_chat' })
    const reply = res.data?.reply || KNOWLEDGE_FALLBACK
    writeAnswerCache(text, 'system', version, reply)
    return reply
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

  async function send(rawText: string, options: { skipIntent?: boolean } = {}) {
    const text = (rawText || '').trim()
    if (!text) return
    if (!userStore.isLoggedIn) {
      loginPromptVisible.value = true
      return
    }

    let assistant = activeAssistant.value
    let note: string | undefined

    if (!options.skipIntent) {
      const intent = detectIntent(text)
      if (intent && intent !== assistant) {
        // 用户手动选过助理：不强制切换，先询问是否切换
        if (manualMap.value[currentKey.value] === true) {
          pendingSwitch.value = { from: assistant, to: intent, text }
          return
        }
        applyAssistant(intent, false)
        note = `已为你切换到${getAssistant(intent).name}`
        assistant = intent
      }
    }
    pendingSwitch.value = null

    const key = currentMessageKey.value
    if (loadingByKey.value[key]) return

    pushMessage(key, { role: 'user', content: text, ts: Date.now(), assistant })
    loadingByKey.value[key] = true
    persist()

    const seq = sessionSeq.value
    try {
      const reply = assistant === 'system' ? await answerBySystem(text) : await answerByPoetry(text)
      // 期间用户关闭了小窗（会话已清空），丢弃这条过期回答
      if (seq === sessionSeq.value) pushAssistant(reply, assistant, note)
    } catch (e) {
      console.warn('AI 小窗回答失败', e)
      if (seq === sessionSeq.value) pushAssistant('AI 服务暂时不可用，请稍后重试', assistant)
    } finally {
      loadingByKey.value[key] = false
      persist()
    }
  }

  /** 确认切换到目标助理并发送等待中的提问 */
  function confirmSwitchAndSend() {
    const pending = pendingSwitch.value
    if (!pending) return
    pendingSwitch.value = null
    applyAssistant(pending.to, true)
    persist()
    void send(pending.text, { skipIntent: true })
  }

  /** 保持当前助理并发送等待中的提问 */
  function keepAssistantAndSend() {
    const pending = pendingSwitch.value
    if (!pending) return
    pendingSwitch.value = null
    void send(pending.text, { skipIntent: true })
  }

  function dismissSwitch() {
    pendingSwitch.value = null
  }

  /** 点击预设芯片：显式指定助理并直接提问 */
  async function askWith(text: string, assistant: AssistantId) {
    if (activeAssistant.value !== assistant) {
      applyAssistant(assistant, true)
    }
    pendingSwitch.value = null
    await send(text, { skipIntent: true })
  }

  load()

  return {
    isOpen,
    minimized,
    closedByUser,
    dockEl,
    dockUsable,
    detached,
    loginPromptVisible,
    context,
    profile,
    currentKey,
    currentMessageKey,
    activeAssistant,
    messages,
    loading,
    position,
    currentSize,
    presetChips,
    greeting,
    renderableRoutes,
    routeTitles,
    knowledgeEntries,
    pendingSwitch,
    syncRoute,
    open,
    close,
    toggle,
    hide,
    registerDock,
    setDetached,
    setDockUsable,
    minimize,
    restore,
    closeLoginPrompt,
    setContext,
    clearContext,
    setAssistant,
    setPos,
    setSize,
    resetPos,
    refreshViewport,
    ensurePreset,
    clearCurrentMessages,
    loadKnowledge,
    send,
    askWith,
    confirmSwitchAndSend,
    keepAssistantAndSend,
    dismissSwitch
  }
})
