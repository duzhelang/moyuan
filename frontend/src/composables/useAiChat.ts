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

export function useAiChat() {
  const personas = PERSONAS
  const allSkills = SKILLS

  const roleId = ref<string>(PERSONAS[0].id)
  const messages = ref<ChatMessage[]>(loadMessages(roleId.value))
  const input = ref('')
  const skillId = ref<string | null>(null)
  const loading = ref(false)

  const persona = computed<AiPersona>(() => getPersonaById(roleId.value))
  const skills = computed<AiSkill[]>(() => getSkillsByRole(roleId.value))
  const activeSkill = computed<AiSkill | null>(
    () => skills.value.find((s) => s.id === skillId.value) ?? null
  )

  /** 切换角色：保存当前会话并恢复目标角色会话 */
  const switchRole = (id: string) => {
    if (id === roleId.value) return
    saveMessages(roleId.value, messages.value)
    roleId.value = id
    messages.value = loadMessages(id)
    skillId.value = null
    input.value = ''
  }

  /** 清空当前角色的历史与上下文 */
  const newSession = () => {
    messages.value = []
    saveMessages(roleId.value, [])
    skillId.value = null
    input.value = ''
  }

  /** 选中/取消能力 chip */
  const toggleSkill = (id: string) => {
    skillId.value = skillId.value === id ? null : id
  }

  /** 清除当前能力 */
  const clearSkill = () => {
    skillId.value = null
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

  /** 发送消息（阶段一非流式，整段一次返回） */
  const send = async () => {
    const raw = input.value.trim()
    if (!raw || loading.value) return

    const skill = activeSkill.value
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
    // 方法
    switchRole,
    newSession,
    toggleSkill,
    clearSkill,
    send
  }
}
