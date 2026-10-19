/**
 * 全局 AI 小窗的两个轻量助理定义
 * - 诗词助理：只回答诗词内容类问题（诗人、诗句、赏析）
 * - 网页用法助理：只回答本系统用法类问题（玩法、入口、规则），走 system_chat 模块与系统知识库
 */

export type AssistantId = 'poetry' | 'system'

export interface AiAssistant {
  /** 助理标识 */
  id: AssistantId
  /** 显示名称 */
  name: string
  /** 可爱的内联 SVG 头像（data URI，零资源依赖） */
  avatar: string
  /** 鼠标悬浮提示文案 */
  tip: string
  /** 绑定的 AI 模块编码；为 null 表示沿用当前页面画像的模块码 */
  moduleCode: string | null
}

function toDataUri(svg: string): string {
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg.trim())}`
}

/** 诗词助理头像：小诗仙（发冠 + 眉心朱砂 + 毛笔） */
const POETRY_AVATAR = toDataUri(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
  <defs>
    <linearGradient id="pg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#fbe3b5"/>
      <stop offset="1" stop-color="#e0a75e"/>
    </linearGradient>
  </defs>
  <circle cx="32" cy="32" r="32" fill="url(#pg)"/>
  <path d="M15 31c0-11 7.6-18 17-18s17 7 17 18" fill="#4a3222"/>
  <ellipse cx="32" cy="37" rx="15" ry="13.5" fill="#ffe8d4"/>
  <path d="M17.5 31h29" stroke="#4a3222" stroke-width="2.4" stroke-linecap="round"/>
  <circle cx="26" cy="38" r="2" fill="#3b2a1c"/>
  <circle cx="38" cy="38" r="2" fill="#3b2a1c"/>
  <circle cx="21.5" cy="43" r="2.6" fill="#f4a0a0" opacity="0.75"/>
  <circle cx="42.5" cy="43" r="2.6" fill="#f4a0a0" opacity="0.75"/>
  <path d="M29 44.5q3 2.8 6 0" stroke="#8a4b3c" stroke-width="1.8" fill="none" stroke-linecap="round"/>
  <circle cx="32" cy="26.5" r="2" fill="#d94f4f"/>
  <path d="M45.5 25.5l7-7" stroke="#7b5b3a" stroke-width="3" stroke-linecap="round"/>
</svg>
`)

/** 网页用法助理头像：小问号（青色系 + 问号徽标） */
const SYSTEM_AVATAR = toDataUri(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
  <defs>
    <linearGradient id="sg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#b6ecdc"/>
      <stop offset="1" stop-color="#4fb3a0"/>
    </linearGradient>
  </defs>
  <circle cx="32" cy="32" r="32" fill="url(#sg)"/>
  <path d="M17.5 30q14.5-14 29 0" fill="#2f7d70"/>
  <ellipse cx="32" cy="37.5" rx="15" ry="13.5" fill="#eafaf5"/>
  <circle cx="26" cy="38.5" r="2" fill="#28564e"/>
  <circle cx="38" cy="38.5" r="2" fill="#28564e"/>
  <circle cx="21.5" cy="43.5" r="2.6" fill="#7fd6c4" opacity="0.85"/>
  <circle cx="42.5" cy="43.5" r="2.6" fill="#7fd6c4" opacity="0.85"/>
  <path d="M29 45q3 2.6 6 0" stroke="#3f7f74" stroke-width="1.8" fill="none" stroke-linecap="round"/>
  <circle cx="48" cy="17" r="9.5" fill="#ffffff" opacity="0.94"/>
  <text x="48" y="22" text-anchor="middle" font-size="14" font-weight="bold" fill="#2f7d70" font-family="Arial, sans-serif">?</text>
</svg>
`)

export const AI_ASSISTANTS: AiAssistant[] = [
  {
    id: 'poetry',
    name: '诗词助理',
    avatar: POETRY_AVATAR,
    tip: '问我诗词：诗人、诗句、赏析',
    moduleCode: null
  },
  {
    id: 'system',
    name: '网页用法助理',
    avatar: SYSTEM_AVATAR,
    tip: '问我用法：玩法、入口、怎么用',
    moduleCode: 'system_chat'
  }
]

export const AI_ASSISTANT_MAP: Record<AssistantId, AiAssistant> = AI_ASSISTANTS.reduce(
  (acc, item) => {
    acc[item.id] = item
    return acc
  },
  {} as Record<AssistantId, AiAssistant>
)

export function getAssistant(id: AssistantId): AiAssistant {
  return AI_ASSISTANT_MAP[id] || AI_ASSISTANT_MAP.poetry
}
