/**
 * 全局 AI 小窗的「路由画像」
 * 每个页面通过画像声明：默认位置、尺寸、主题、模块码、预设问题、默认助理、入口按钮形态。
 * 小窗本体只有一个组件，页面差异全部由画像驱动。
 */
import type { AssistantId } from './assistants'

/** 锚点：相对视口定位 */
export type AiAnchor = 'left-top' | 'right-top' | 'left-bottom' | 'right-bottom'

/** 传给小窗的页面上下文 */
export interface AiContext {
  /** 上下文类型：诗人 / 诗词 / 无 */
  type: 'poet' | 'poem' | 'none'
  /** 本地库主键（可选） */
  id?: number
  /** 诗人名 或 诗词标题 */
  name?: string
  /** 诗词作者 */
  author?: string
  /** 诗词正文（多句用换行分隔） */
  content?: string
  /** 诗词所属诗人 */
  poetName?: string
}

/** 页面上的小窗唤起入口（FAB）配置 */
export interface AiEntryConfig {
  /** 是否显示入口 */
  show: boolean
  anchor: AiAnchor
  dx: number
  dy: number
  /** 形态：圆形图标 / 胶囊带文字 */
  shape: 'circle' | 'pill'
  label?: string
}

export interface AiPageProfile {
  /** 画像键，作为消息分区与位置记忆的 key */
  key: string
  anchor: AiAnchor
  offset: { dx: number; dy: number }
  size: { w: number; h: number }
  /** 主题色 CSS 变量，实现「每页不同外观」 */
  theme: Record<string, string>
  /** 诗词类问答使用的 AI 模块编码 */
  moduleCode: string
  /** 进入页面即展开小窗（复刻原「常驻面板」页） */
  autoOpen: boolean
  /** 进入页面时强制收起（页内自带折叠面板的页面） */
  collapseOnEnter?: boolean
  /** 页内嵌入渲染：由页面注册 dock 容器承载窗口，不使用右下浮窗与入口 */
  dock?: boolean
  /** 唤起入口 */
  entry: AiEntryConfig
  /** 欢迎语 */
  greeting?: string
  /** 该页原有的预设问题，1:1 迁移 */
  presetQuestions: Array<{ text: string; assistant?: AssistantId }>
  /** auto=首个自动发送；chips=渲染为可点快捷芯片 */
  presetMode: 'auto' | 'chips'
  /** 进入该页默认使用的助理 */
  defaultAssistant: AssistantId
  /** 该页面不启用全局小窗（登录页、后台等） */
  disabled?: boolean
}

/** 墨渊棕金色系默认主题 */
const DEFAULT_THEME: Record<string, string> = {
  '--ai-accent': '#8b4513',
  '--ai-accent-2': '#d2691e',
  '--ai-bg': 'rgba(255, 253, 247, 0.97)',
  '--ai-border': 'rgba(184, 134, 11, 0.45)',
  '--ai-header-text': '#fff8ec'
}

/** 诗云/诗宝阁等页面可用的青绿主题（示例能力，按需扩展） */
const JADE_THEME: Record<string, string> = {
  ...DEFAULT_THEME,
  '--ai-accent': '#2f7d70',
  '--ai-accent-2': '#4fb3a0',
  '--ai-border': 'rgba(79, 179, 160, 0.45)'
}

/** 寻章摘句页紫色折叠面板主题：与「墨渊问诗」卡片同色系，展开后与卡片融为一体 */
const PURPLE_THEME: Record<string, string> = {
  '--ai-accent': '#667eea',
  '--ai-accent-2': '#764ba2',
  '--ai-bg': '#faf8ff',
  '--ai-border': 'rgba(118, 75, 162, 0.28)',
  '--ai-header-text': '#ffffff'
}

/** 无原入口的通用兜底画像 */
const GENERAL_PROFILE: AiPageProfile = {
  key: 'general',
  anchor: 'right-bottom',
  offset: { dx: 90, dy: 90 },
  size: { w: 380, h: 500 },
  theme: DEFAULT_THEME,
  moduleCode: 'poetry_chat',
  autoOpen: false,
  entry: {
    show: true,
    anchor: 'right-bottom',
    dx: 30,
    dy: 96,
    shape: 'circle'
  },
  greeting: '我是墨渊的小助理，可以问我诗词，也可以问我本站怎么用',
  presetQuestions: [
    { text: '飞花令怎么玩', assistant: 'system' },
    { text: '寻章摘句能做什么', assistant: 'system' },
    { text: '诗词接龙怎么参与', assistant: 'system' }
  ],
  presetMode: 'chips',
  defaultAssistant: 'system'
}

/** 各页面专属画像 */
const PAGE_PROFILES: Array<{ match: RegExp; profile: AiPageProfile }> = [
  {
    // 诗人详情页：窗口常驻嵌入右侧栏「诗词特点」下方的空区（随右栏滚动）
    match: /^\/poet\/\d+$/,
    profile: {
      key: 'poet:detail',
      anchor: 'right-top',
      offset: { dx: 16, dy: 63 },
      size: { w: 368, h: 420 },
      theme: DEFAULT_THEME,
      moduleCode: 'poet_chat',
      autoOpen: true,
      dock: true,
      entry: { show: false, anchor: 'right-bottom', dx: 30, dy: 96, shape: 'circle' },
      greeting: '与 AI 助手对话，了解这位诗人的故事',
      presetQuestions: [{ text: '${poetName}是谁', assistant: 'poetry' }],
      presetMode: 'auto',
      defaultAssistant: 'poetry'
    }
  },
  {
    // 诗词详情页：原右下浮钮 + 滑出窗
    match: /^\/poem\/\d+$/,
    profile: {
      key: 'poem:detail',
      anchor: 'right-bottom',
      offset: { dx: 90, dy: 90 },
      size: { w: 380, h: 500 },
      theme: DEFAULT_THEME,
      moduleCode: 'chat',
      autoOpen: false,
      entry: { show: true, anchor: 'right-bottom', dx: 30, dy: 96, shape: 'circle' },
      greeting: '向 AI 提问，读懂这首诗词',
      presetQuestions: [{ text: '请简要介绍一下这首诗', assistant: 'poetry' }],
      presetMode: 'auto',
      defaultAssistant: 'poetry'
    }
  },
  {
    // 寻章摘句页：AI 面板嵌入右侧栏「背景知识」卡片下方，紫色折叠区默认收起
    match: /^\/poem$/,
    profile: {
      key: 'poem:list',
      anchor: 'right-bottom',
      offset: { dx: 40, dy: 96 },
      size: { w: 380, h: 420 },
      theme: PURPLE_THEME,
      moduleCode: 'poetry_chat',
      autoOpen: false,
      collapseOnEnter: true,
      dock: true,
      entry: { show: false, anchor: 'right-bottom', dx: 30, dy: 96, shape: 'pill', label: 'AI 助手' },
      greeting: '可以问我这首诗的译文、赏析与背景',
      presetQuestions: [{ text: '请简要介绍一下这首诗', assistant: 'poetry' }],
      presetMode: 'chips',
      defaultAssistant: 'poetry'
    }
  },
  {
    // 诗宝阁（独立对话页）：不重复挂入口，仅保留全局小窗能力
    match: /^\/ai-chat$/,
    profile: {
      ...GENERAL_PROFILE,
      key: 'ai-chat',
      theme: JADE_THEME,
      entry: { show: false, anchor: 'right-bottom', dx: 30, dy: 96, shape: 'circle' },
      presetMode: 'chips'
    }
  }
]

/** 不启用全局小窗的路径前缀（登录/注册/后台） */
const DISABLED_PREFIXES = ['/user/login', '/user/register', '/admin']

function isDisabledPath(path: string): boolean {
  return DISABLED_PREFIXES.some(prefix => path.startsWith(prefix))
}

/** 按当前路由路径解析画像（忽略 query/hash，避免带筛选参数时落回通用画像） */
export function resolveProfile(path: string): AiPageProfile {
  const cleanPath = path.split('?')[0].split('#')[0]
  if (isDisabledPath(cleanPath)) {
    return { ...GENERAL_PROFILE, key: 'disabled', disabled: true, entry: { ...GENERAL_PROFILE.entry, show: false } }
  }
  const hit = PAGE_PROFILES.find(item => item.match.test(cleanPath))
  return hit ? hit.profile : GENERAL_PROFILE
}

/** 渲染预设问题中的变量模板（如 ${poetName}） */
export function renderPresetText(text: string, ctx: AiContext | null): string {
  if (!text.includes('${')) return text
  const poetName = ctx?.poetName || (ctx?.type === 'poet' ? ctx?.name : '') || ctx?.author || ''
  const poemTitle = ctx?.type === 'poem' ? (ctx?.name || '') : ''
  return text
    .replace(/\$\{poetName\}/g, poetName || '这位诗人')
    .replace(/\$\{poemTitle\}/g, poemTitle || '这首诗')
}

/** 按锚点与偏移计算小窗左上角坐标（随视口自适应） */
export function resolvePosition(
  profile: AiPageProfile,
  size: { w: number; h: number },
  viewport: { w: number; h: number }
): { x: number; y: number } {
  const { anchor, offset } = profile
  let x = 0
  let y = 0
  if (anchor === 'left-top') {
    x = offset.dx
    y = offset.dy
  } else if (anchor === 'right-top') {
    x = viewport.w - size.w - offset.dx
    y = offset.dy
  } else if (anchor === 'left-bottom') {
    x = offset.dx
    y = viewport.h - size.h - offset.dy
  } else {
    x = viewport.w - size.w - offset.dx
    y = viewport.h - size.h - offset.dy
  }
  const minX = Math.max(8, Math.min(x, viewport.w - size.w - 8))
  const minY = Math.max(8, Math.min(y, viewport.h - size.h - 8))
  return { x: minX, y: minY }
}
