/**
 * 「诗宝阁」AI 对话页 —— 会话状态与请求组合式函数
 *
 * 本版能力：
 * - SSE 流式输出（POST /api/ai/chat/stream），支持中断；
 * - 多轮上下文：system（角色设定）+ 最近若干轮历史 + 本轮任务；
 * - 服务端会话：按「用户 + 角色」存 Redis，localStorage 仅作兜底；
 * - 平仄校验走本地确定性引擎（/api/ai/tone-check），失败再回退大模型；
 * - 飞花令保留对话内轻量对局。
 */
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  chat,
  chatStream,
  clearChatSession,
  getChatSession,
  saveChatSession,
  toneCheck
} from '@/api/modules/ai'
import type { ChatSessionMessage, ChatStreamMessage } from '@/api/modules/ai'
import { searchPoems } from '@/api/modules/poem'
import { PERSONAS, getPersonaById } from '@/configs/ai-chat/personas'
import type { AiPersona } from '@/configs/ai-chat/personas'
import { SKILLS, getSkillsByRole } from '@/configs/ai-chat/skills'
import type { AiSkill } from '@/configs/ai-chat/skills'

/** 单条对话消息 */
export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  /** 消息原始文本 */
  content: string
  /** 触发该回复的能力 id（用于特殊渲染），自由对话为空 */
  skillId?: string
  time: number
}

/** localStorage 会话键前缀（服务端不可用时的兜底） */
const STORAGE_PREFIX = 'moyuan_ai_chat_'
/** 最多保留的消息条数（约 20 轮） */
const MAX_HISTORY = 40
/** 单条消息内容长度上限，防止 localStorage 膨胀 */
const MAX_CONTENT_LEN = 4000
/** 参与多轮上下文的最近消息条数 */
const CONTEXT_MESSAGE_LIMIT = 12
/** 参与多轮上下文的单条消息最大字符数 */
const CONTEXT_MESSAGE_CHARS = 2000
/** 参与多轮上下文的合计字符上限 */
const CONTEXT_TOTAL_CHARS = 8000

/** 生成简易唯一 id */
function genId(): string {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
}

/** 读取指定角色的本地会话 */
function loadMessages(roleId: string): ChatMessage[] {
  try {
    const raw = localStorage.getItem(STORAGE_PREFIX + roleId)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) return (parsed as ChatMessage[]).slice(-MAX_HISTORY)
  } catch {
    // 本地数据损坏时静默忽略，按空会话处理
  }
  return []
}

/** 持久化指定角色的会话（localStorage 兜底） */
function saveMessages(roleId: string, msgs: ChatMessage[]): void {
  try {
    const trimmed = msgs.slice(-MAX_HISTORY).map((m) => ({
      ...m,
      content: m.content.slice(0, MAX_CONTENT_LEN)
    }))
    localStorage.setItem(STORAGE_PREFIX + roleId, JSON.stringify(trimmed))
  } catch {
    // 存储写入失败（如超额）时静默忽略，不影响当前会话
  }
}

/** 飞花令有效诗句最少汉字数（与游戏模块口径一致） */
const FEIHUA_MIN_HANZI = 4

/** 飞花令对局状态（仅存于当前会话，不落库） */
interface FeihuaGame {
  /** 令字（单字） */
  keyword: string
  /** 已使用诗句（去标点后的汉字串） */
  used: string[]
  /** 已行回合数 */
  rounds: number
}

/** 只保留汉字，用于含字判定与判重 */
function onlyHanzi(text: string): string {
  return Array.from(text)
    .filter((char) => /[\u4e00-\u9fa5]/.test(char))
    .join('')
}

/** 从 AI 返回中提取首个 JSON 对象（剥离 ``` 围栏） */
function extractJson<T>(raw: string): T | null {
  if (!raw) return null
  let text = raw.trim()
  if (text.startsWith('```')) {
    const nl = text.indexOf('\n')
    if (nl >= 0) text = text.slice(nl + 1)
    const fence = text.lastIndexOf('```')
    if (fence >= 0) text = text.slice(0, fence)
  }
  const start = text.indexOf('{')
  const end = text.lastIndexOf('}')
  if (start < 0 || end <= start) return null
  try {
    return JSON.parse(text.slice(start, end + 1)) as T
  } catch {
    return null
  }
}

/** 从「诗句 —— 作者《篇名》」中取出诗句部分并归一为汉字串 */
function aiLineHanzi(reply: string): string {
  const first = reply.split('\n').map((line) => line.trim()).find(Boolean) || ''
  const sentence = first.split(/——|—|--/)[0] || ''
  return onlyHanzi(sentence.replace(/^诗句[:：]?/, ''))
}

function isAbortError(error: unknown): boolean {
  return typeof error === 'object' && error !== null
    && (error as { name?: string }).name === 'AbortError'
}

/**
 * 是否属于「重试也没用」的拒绝：限流、额度用尽、功能关闭、参数非法、未登录。
 * 这类错误再回退一次非流式请求只会二次扣配额，并掩盖真实原因。
 */
function isNonRetryable(error: unknown): boolean {
  if (typeof error !== 'object' || error === null) return false
  const { status, code } = error as { status?: number; code?: string }
  if (status === 400 || status === 401 || status === 403 || status === 429) return true
  return ['400', '401', '403', '4290', '4291', '4292', '4293'].includes(String(code || ''))
}

export function useAiChat() {
  const personas = PERSONAS
  const allSkills = SKILLS

  const roleId = ref<string>(PERSONAS[0].id)
  const messages = ref<ChatMessage[]>(loadMessages(roleId.value))
  const input = ref('')
  const skillId = ref<string | null>(null)
  const loading = ref(false)
  /** 飞花令对局状态：空令字表示尚未开局 */
  const feihuaGame = ref<FeihuaGame>({ keyword: '', used: [], rounds: 0 })
  /** 当前流式请求的中断句柄 */
  const abortController = ref<AbortController | null>(null)
  /** 会话切换序号，用于丢弃过期的服务端拉取结果 */
  let sessionSeq = 0

  const persona = computed<AiPersona>(() => getPersonaById(roleId.value))
  const skills = computed<AiSkill[]>(() => getSkillsByRole(roleId.value))
  const activeSkill = computed<AiSkill | null>(
    () => skills.value.find((s) => s.id === skillId.value) ?? null
  )
  const feihuaKeyword = computed(() => feihuaGame.value.keyword)

  /** 重置飞花令对局 */
  const resetFeihua = () => {
    feihuaGame.value = { keyword: '', used: [], rounds: 0 }
  }

  /** 把当前会话同步到服务端（失败静默，localStorage 仍是兜底） */
  const persistServer = (targetRoleId = roleId.value) => {
    const snapshot: ChatSessionMessage[] = messages.value.map((message) => ({
      id: message.id,
      role: message.role,
      content: message.content,
      skillId: message.skillId,
      time: message.time
    }))
    void saveChatSession(targetRoleId, snapshot).catch(() => {
      // 服务端不可用时保持本地会话，不影响后续对话
    })
  }

  /** 从服务端恢复会话；服务端为空时把本地会话反向同步一次 */
  const syncFromServer = async (targetRoleId: string) => {
    const seq = ++sessionSeq
    try {
      const res = await getChatSession(targetRoleId)
      if (seq !== sessionSeq || roleId.value !== targetRoleId) return
      const remote = Array.isArray(res.data) ? res.data : []
      if (!remote.length) {
        if (messages.value.length) persistServer(targetRoleId)
        return
      }
      messages.value = remote.slice(-MAX_HISTORY).map((message) => ({
        id: message.id || genId(),
        role: message.role,
        content: message.content,
        skillId: message.skillId,
        time: message.time || Date.now()
      }))
      saveMessages(targetRoleId, messages.value)
    } catch {
      // 拉取失败时保留本地会话
    }
  }

  /** 切换角色：保存当前会话并恢复目标角色会话 */
  const switchRole = (id: string) => {
    if (id === roleId.value) return
    saveMessages(roleId.value, messages.value)
    sessionSeq++
    roleId.value = id
    messages.value = loadMessages(id)
    skillId.value = null
    resetFeihua()
    input.value = ''
    void syncFromServer(id)
  }

  /** 清空当前角色的历史与上下文（本地 + 服务端） */
  const newSession = () => {
    sessionSeq++
    messages.value = []
    saveMessages(roleId.value, [])
    skillId.value = null
    resetFeihua()
    input.value = ''
    void clearChatSession(roleId.value).catch(() => {
      // 服务端清理失败不影响本地新会话
    })
  }

  /** 选中/取消能力 chip；切换能力时结束当前飞花令对局 */
  const toggleSkill = (id: string) => {
    skillId.value = skillId.value === id ? null : id
    resetFeihua()
  }

  /** 清除当前能力 */
  const clearSkill = () => {
    skillId.value = null
    resetFeihua()
  }

  /** 追加消息并持久化 */
  const pushMessage = (msg: ChatMessage) => {
    let next = [...messages.value, msg]
    if (next.length > MAX_HISTORY) next = next.slice(-MAX_HISTORY)
    messages.value = next
    saveMessages(roleId.value, next)
  }

  /** 流式增量：替换数组元素触发视图更新 */
  const appendToMessage = (id: string, delta: string) => {
    const index = messages.value.findIndex((message) => message.id === id)
    if (index < 0) return
    const current = messages.value[index]
    messages.value[index] = { ...current, content: current.content + delta }
  }

  /** 用最终结果覆盖流式占位消息 */
  const setMessageContent = (id: string, content: string) => {
    const index = messages.value.findIndex((message) => message.id === id)
    if (index < 0) return
    messages.value[index] = { ...messages.value[index], content }
  }

  const removeMessage = (id: string) => {
    messages.value = messages.value.filter((message) => message.id !== id)
  }

  /** 拼装最终 user 消息：角色设定 + （能力任务模板 | 用户输入） */
  const buildFullPrompt = (raw: string, skill: AiSkill | null): string => {
    const parts: string[] = [`【角色设定】${persona.value.systemPrompt}`]
    if (skill) {
      parts.push(`【本次任务】${skill.buildPrompt(raw)}`)
    } else {
      parts.push(`【用户输入】${raw}`)
    }
    return parts.join('\n\n')
  }

  /** 多轮上下文：system + 最近历史 + 本轮任务；跳过 tone 的 JSON 结果 */
  const buildStreamMessages = (raw: string, skill: AiSkill | null): ChatStreamMessage[] => {
    const history: ChatStreamMessage[] = []
    let totalChars = 0
    const candidates = messages.value.slice(-CONTEXT_MESSAGE_LIMIT)
    for (let i = candidates.length - 1; i >= 0; i--) {
      const message = candidates[i]
      if (message.role === 'assistant' && message.skillId === 'tone') continue
      const content = message.content.slice(0, CONTEXT_MESSAGE_CHARS)
      if (!content.trim()) continue
      if (totalChars + content.length > CONTEXT_TOTAL_CHARS) break
      history.unshift({ role: message.role, content })
      totalChars += content.length
    }
    return [
      { role: 'system', content: persona.value.systemPrompt },
      ...history,
      { role: 'user', content: skill ? skill.buildPrompt(raw) : raw }
    ]
  }

  /** 推送一条飞花令判定消息 */
  const pushFeihuaVerdict = (skill: AiSkill, content: string) => {
    pushMessage({ id: genId(), role: 'assistant', content, skillId: skill.id, time: Date.now() })
  }

  /** 本地诗库校验：优先整句检索，未命中再用前四字/前两字检索后比对 */
  const existsInLibrary = async (raw: string, normalized: string): Promise<boolean> => {
    const keywords = Array.from(new Set([
      raw,
      normalized.slice(0, 4),
      normalized.slice(0, 2)
    ])).filter(Boolean)
    for (const keyword of keywords) {
      try {
        const res = await searchPoems(keyword)
        const list = res.data || []
        if (list.some((poem) => onlyHanzi(poem.content || '').includes(normalized))) return true
      } catch {
        // 单个关键词检索失败时继续尝试下一个
      }
    }
    return false
  }

  /** 诗库未收录时，交给 AI 复核是否为真实古诗词原句 */
  const judgeRealByAi = async (sentence: string): Promise<boolean> => {
    const prompt = '请判定下面这句是否为中国古典诗词中真实存在的原句（排除现代人拟作、网络杜撰、篡改拼接）。\n'
      + `句子：「${sentence}」\n\n`
      + '只输出如下严格 JSON，不要任何解释文字、不要 markdown 代码块标记：\n'
      + '{"real":true或false,"reason":"不超过20字的原因"}'
    const res = await chat({ message: prompt, moduleCode: 'chat' })
    const parsed = extractJson<{ real?: boolean }>(res.data?.reply || '')
    return parsed?.real === true
  }

  /** AI 接句：返回「诗句 —— 作者《篇名》」一行 */
  const generateFeihuaReply = async (keyword: string, used: string[]): Promise<string> => {
    const prompt = '你是诗友，正在与用户玩飞花令。\n'
      + `令字：「${keyword}」\n`
      + `已用诗句：${used.length ? used.join('；') : '无'}\n`
      + '请接一句真实存在的中国古典诗词原句，要求：\n'
      + `1. 句中必须含有「${keyword}」字；\n`
      + '2. 不得与已用诗句重复；\n'
      + '3. 只输出一行，格式为「诗句 —— 作者《篇名》」，不要编号、解释或多余文字。'
    const res = await chat({
      message: `【角色设定】${persona.value.systemPrompt}\n\n【本次任务】${prompt}`,
      moduleCode: 'chat'
    })
    return (res.data?.reply || '').trim()
  }

  /** 飞花令对局：设令字 → 判定用户诗句 → AI 接句 */
  const sendFeihua = async (raw: string, skill: AiSkill) => {
    // 阶段一：尚未开局，本条输入作为令字
    if (!feihuaGame.value.keyword) {
      if (!/^[\u4e00-\u9fa5]$/.test(raw)) {
        ElMessage.warning('飞花令先定一个令字吧，比如「花」。')
        return
      }
      feihuaGame.value = { keyword: raw, used: [], rounds: 0 }
      pushMessage({ id: genId(), role: 'user', content: raw, skillId: skill.id, time: Date.now() })
      pushFeihuaVerdict(
        skill,
        `好，令字就定「${raw}」。\n你先来一句含「${raw}」的古诗词原句，咱们一句一句对下去。\n（取消能力或新建会话可重开一局；想玩计时闯关，可点「完整对局」入口）`
      )
      input.value = ''
      return
    }

    const keyword = feihuaGame.value.keyword
    pushMessage({ id: genId(), role: 'user', content: raw, skillId: skill.id, time: Date.now() })
    input.value = ''
    loading.value = true
    try {
      const normalized = onlyHanzi(raw)
      // 本地硬规则：最短长度 → 含令字 → 判重
      if (normalized.length < FEIHUA_MIN_HANZI) {
        pushFeihuaVerdict(skill, '这一句有点短，完整地念一句诗如何？')
        return
      }
      if (!normalized.includes(keyword)) {
        pushFeihuaVerdict(skill, `这一句里好像没有「${keyword}」字，再想一句含「${keyword}」的诗吧。`)
        return
      }
      if (feihuaGame.value.used.includes(normalized)) {
        pushFeihuaVerdict(skill, '这句刚才已经对过啦，换一句没出现过的吧。')
        return
      }

      // 真实性：本地诗库优先，未收录再交 AI 复核
      const inLibrary = await existsInLibrary(raw, normalized)
      if (!inLibrary && !(await judgeRealByAi(raw))) {
        pushFeihuaVerdict(skill, '我一时没在诗库里找到这一句，换一句大家耳熟能详的名句吧。')
        return
      }

      feihuaGame.value.used.push(normalized)
      feihuaGame.value.rounds += 1

      let reply = ''
      try {
        reply = await generateFeihuaReply(keyword, feihuaGame.value.used)
      } catch {
        // 生成失败按「AI 无句可接」处理，不影响已通过的判定
      }

      const aiLine = aiLineHanzi(reply)
      let content = `好句！这一句含着「${keyword}」，也没重复。`
      if (reply && aiLine.length >= FEIHUA_MIN_HANZI && aiLine.includes(keyword)
        && !feihuaGame.value.used.includes(aiLine)) {
        feihuaGame.value.used.push(aiLine)
        content += `\n\n我接一句：\n${reply}\n\n已对 ${feihuaGame.value.rounds} 回合。`
      } else {
        content += `\n\n这一回合我接不上了，算你赢。\n\n已对 ${feihuaGame.value.rounds} 回合。`
      }
      pushFeihuaVerdict(skill, content)
    } catch {
      pushFeihuaVerdict(skill, '我这边一时没缓过神来，稍后再试一句吧。')
    } finally {
      loading.value = false
    }
  }

  /** 平仄校验：优先本地确定性引擎，失败回退大模型 */
  const sendTone = async (raw: string, skill: AiSkill) => {
    pushMessage({ id: genId(), role: 'user', content: raw, skillId: skill.id, time: Date.now() })
    input.value = ''
    loading.value = true
    try {
      const res = await toneCheck(raw)
      const data = res.data
      if (!data || !Array.isArray(data.lines) || data.lines.length === 0) {
        throw new Error('EMPTY_TONE_RESULT')
      }
      const issues = data.issues || []
      const content = JSON.stringify({
        valid: !!data.passed,
        format: data.genre || '',
        rhymeGroup: '',
        lines: data.lines.map((line) => ({
          text: line.text,
          tone: line.pattern || '',
          rhyme: false,
          note: (line.issues || []).join('；')
        })),
        summary: issues.length ? issues.join('；') : '格律校验通过'
      })
      pushMessage({ id: genId(), role: 'assistant', content, skillId: 'tone', time: Date.now() })
    } catch (error) {
      if (isNonRetryable(error)) {
        // 限流/未登录：请求层已提示，且回退大模型同样会被拦住，直接结束
        return
      }
      // 本地引擎不可用（接口异常/结果为空）时回退原 LLM 路径，保持能力可用
      try {
        const res = await chat({ message: buildFullPrompt(raw, skill), moduleCode: 'chat' })
        const reply = (res.data?.reply || '').trim()
        if (reply) {
          pushMessage({ id: genId(), role: 'assistant', content: reply, skillId: 'tone', time: Date.now() })
        } else {
          ElMessage.info('AI 未返回内容，请重试')
        }
      } catch (fallbackError) {
        if (!isNonRetryable(fallbackError)) {
          ElMessage.error('校验失败，请稍后重试')
        }
      }
    } finally {
      loading.value = false
      persistServer()
    }
  }

  /** 停止生成：保留已输出的部分内容 */
  const stop = () => {
    abortController.value?.abort()
    abortController.value = null
    loading.value = false
    saveMessages(roleId.value, messages.value)
    persistServer()
  }

  /** 发送消息（SSE 流式 + 多轮；失败回退非流式单轮） */
  const send = async () => {
    const raw = input.value.trim()
    if (!raw || loading.value) return

    const skill = activeSkill.value
    // 飞花令走独立对局流程：设令字、逐句判定、AI 接句
    if (skill?.id === 'feihua') {
      await sendFeihua(raw, skill)
      persistServer()
      return
    }
    // 平仄校验走本地确定性引擎
    if (skill?.id === 'tone') {
      await sendTone(raw, skill)
      return
    }

    if (skill?.validate) {
      const err = skill.validate(raw)
      if (err) {
        ElMessage.warning(err)
        return
      }
    }

    const streamMessages = buildStreamMessages(raw, skill)
    pushMessage({ id: genId(), role: 'user', content: raw, skillId: skill?.id, time: Date.now() })
    input.value = ''
    loading.value = true

    const controller = new AbortController()
    abortController.value = controller
    const assistantId = genId()
    let placeholderAdded = false
    const ensurePlaceholder = () => {
      if (placeholderAdded) return
      pushMessage({ id: assistantId, role: 'assistant', content: '', skillId: skill?.id, time: Date.now() })
      placeholderAdded = true
    }

    try {
      const reply = await chatStream(
        { messages: streamMessages, moduleCode: 'chat' },
        {
          signal: controller.signal,
          onDelta: (delta) => {
            ensurePlaceholder()
            appendToMessage(assistantId, delta)
          }
        }
      )
      if (!reply.trim()) {
        if (placeholderAdded) removeMessage(assistantId)
        ElMessage.info('AI 未返回内容，请重试')
      } else {
        ensurePlaceholder()
        setMessageContent(assistantId, reply)
      }
    } catch (error) {
      if (isAbortError(error)) {
        // 用户主动停止：保留已生成的部分内容
      } else if (isNonRetryable(error)) {
        // 限流/额度/功能关闭：重试只会再消耗一次配额，直接给出后端原因
        if (placeholderAdded && !messages.value.find((m) => m.id === assistantId)?.content?.trim()) {
          removeMessage(assistantId)
        }
        ElMessage.error((error as Error)?.message || '请求被拒绝，请稍后重试')
      } else {
        // 网络/流中断等可重试失败：回退非流式单轮，至少保证有回复
        try {
          const res = await chat({ message: buildFullPrompt(raw, skill), moduleCode: 'chat' })
          const reply = (res.data?.reply || '').trim()
          if (!reply) {
            if (placeholderAdded) removeMessage(assistantId)
            ElMessage.info('AI 未返回内容，请重试')
          } else {
            ensurePlaceholder()
            setMessageContent(assistantId, reply)
          }
        } catch (fallbackError) {
          // 业务类失败已由请求层提示过一次，这里只兜真正的网络/未知异常
          if (!isNonRetryable(fallbackError)) {
            if (placeholderAdded) {
              ElMessage.error('生成中断，请重试')
            } else {
              ElMessage.error('生成失败，请稍后重试')
            }
          }
        }
      }
    } finally {
      // 已被「停止」或新请求接管时，不覆盖新一轮的状态
      if (abortController.value === controller) {
        loading.value = false
        abortController.value = null
        // 流式过程中为避免频繁写盘，只在结束时统一落本地 + 服务端
        saveMessages(roleId.value, messages.value)
        persistServer()
      }
    }
  }

  // 首次进入：本地会话先展示，再异步与服务端合并
  void syncFromServer(roleId.value)

  return {
    // 常量
    personas,
    allSkills,
    // 状态
    roleId,
    persona,
    skills,
    skillId,
    activeSkill,
    messages,
    input,
    loading,
    /** 飞花令当前令字；空串表示尚未开局 */
    feihuaKeyword,
    // 方法
    switchRole,
    newSession,
    toggleSkill,
    clearSkill,
    send,
    stop
  }
}
