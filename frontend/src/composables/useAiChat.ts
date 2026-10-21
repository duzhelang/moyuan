/**
 * 「诗宝阁」AI 对话页 —— 会话状态与请求组合式函数
 *
 * 阶段一 MVP：
 * - 复用现有非流式接口 POST /api/ai/chat（moduleCode 必传 'chat'，否则回退智谱默认模型会 429）；
 * - 角色 system prompt 以「角色设定」前缀注入 user 消息；
 * - 会话按 roleId 分存 localStorage，切换角色各自恢复；
 * - 历史仅用于展示，不回传模型（多轮上下文属阶段二）。
 */
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { chat } from '@/api/modules/ai'
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

/** localStorage 会话键前缀 */
const STORAGE_PREFIX = 'moyuan_ai_chat_'
/** 最多保留的消息条数（约 20 轮） */
const MAX_HISTORY = 40
/** 单条消息内容长度上限，防止 localStorage 膨胀 */
const MAX_CONTENT_LEN = 4000

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

/** 持久化指定角色的会话 */
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

  /** 切换角色：保存当前会话并恢复目标角色会话 */
  const switchRole = (id: string) => {
    if (id === roleId.value) return
    saveMessages(roleId.value, messages.value)
    roleId.value = id
    messages.value = loadMessages(id)
    skillId.value = null
    resetFeihua()
    input.value = ''
  }

  /** 清空当前角色的历史与上下文 */
  const newSession = () => {
    messages.value = []
    saveMessages(roleId.value, [])
    skillId.value = null
    resetFeihua()
    input.value = ''
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

  /** 发送消息（阶段一非流式，整段一次返回） */
  const send = async () => {
    const raw = input.value.trim()
    if (!raw || loading.value) return

    const skill = activeSkill.value
    // 飞花令走独立对局流程：设令字、逐句判定、AI 接句
    if (skill?.id === 'feihua') {
      await sendFeihua(raw, skill)
      return
    }

    if (skill?.validate) {
      const err = skill.validate(raw)
      if (err) {
        ElMessage.warning(err)
        return
      }
    }

    pushMessage({ id: genId(), role: 'user', content: raw, skillId: skill?.id, time: Date.now() })
    input.value = ''
    loading.value = true
    try {
      const res = await chat({ message: buildFullPrompt(raw, skill), moduleCode: 'chat' })
      const reply = (res.data?.reply || '').trim()
      if (!reply) {
        ElMessage.info('AI 未返回内容，请重试')
      } else {
        pushMessage({ id: genId(), role: 'assistant', content: reply, skillId: skill?.id, time: Date.now() })
      }
    } catch {
      ElMessage.error('生成失败，请稍后重试')
    } finally {
      loading.value = false
    }
  }

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
    send
  }
}
