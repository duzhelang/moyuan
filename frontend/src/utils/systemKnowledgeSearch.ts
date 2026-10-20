/**
 * 系统知识库的本地检索层（纯前端计算，零额外依赖）
 * 职责：条目解析 → 关键词打分检索 Top-K → FAQ 精确直答 → 组装注入给 system_chat 模块的资料文本
 */
import { KNOWLEDGE_SYNONYMS } from '@/configs/ai-chat/synonyms'

/** 后端返回的原始条目 */
export interface RawKnowledgeEntry {
  id: number
  code: string
  title: string
  category: string
  keywords?: string | null
  aliases?: string | null
  body: string
  steps?: string | null
  faq?: string | null
  route?: string | null
  updateTime?: string | null
}

export interface KnowledgeFaq {
  q: string
  a: string
  aliases?: string[]
}

/** 规范化后的条目 */
export interface KnowledgeEntry {
  id: number
  code: string
  title: string
  category: string
  keywords: string[]
  aliases: string[]
  body: string
  steps: string[]
  faq: KnowledgeFaq[]
  route?: string
  /** 最后更新时间，用于生成问答缓存版本号 */
  updatedAt?: string
}

/** 命中 FAQ 的直答结果 */
export interface FaqHit {
  answer: string
  route?: string
  entryTitle: string
}

/** 答案兜底：知识库未命中时的可控答复 */
export const KNOWLEDGE_FALLBACK = '我暂时没有这方面信息，换个说法再问我一次试试～'

function splitCsv(text?: string | null): string[] {
  if (!text) return []
  return text
    .split(/[,，、;；]/)
    .map(item => item.trim())
    .filter(Boolean)
}

function splitLines(text?: string | null): string[] {
  if (!text) return []
  return text
    .split(/\r?\n/)
    .map(item => item.trim())
    .filter(Boolean)
}

function parseFaq(text?: string | null): KnowledgeFaq[] {
  if (!text) return []
  try {
    const parsed = JSON.parse(text)
    if (!Array.isArray(parsed)) return []
    return parsed
      .filter(item => item && typeof item.q === 'string' && typeof item.a === 'string')
      .map(item => ({
        q: item.q as string,
        a: item.a as string,
        aliases: Array.isArray(item.aliases) ? (item.aliases as string[]) : []
      }))
  } catch {
    return []
  }
}

/** 把后端条目转换为可检索结构 */
export function parseEntries(list: RawKnowledgeEntry[]): KnowledgeEntry[] {
  return (list || []).map(item => ({
    id: item.id,
    code: item.code,
    title: item.title,
    category: item.category,
    keywords: splitCsv(item.keywords),
    aliases: splitCsv(item.aliases),
    body: item.body || '',
    steps: splitLines(item.steps),
    faq: parseFaq(item.faq),
    route: item.route || undefined,
    updatedAt: item.updateTime || undefined
  }))
}

/** 归一化：去空白与常见标点、转小写 */
function normalize(text: string): string {
  return text
    .toLowerCase()
    .replace(/[\s，。？！、,.?!:：;；"'“”‘’()（）【】[\]<>《》~～·\-—_]/g, '')
}

/** 结合同义词表扩展提问关键词 */
function expandQuery(query: string): string[] {
  const normalized = normalize(query)
  const tokens = new Set<string>([normalized])
  Object.entries(KNOWLEDGE_SYNONYMS).forEach(([canonical, variants]) => {
    const hit = normalized.includes(normalize(canonical)) || variants.some(v => normalized.includes(normalize(v)))
    if (hit) {
      tokens.add(normalize(canonical))
      variants.forEach(v => tokens.add(normalize(v)))
    }
  })
  return Array.from(tokens).filter(Boolean)
}

/** 提问与条目的相关性打分 */
function scoreEntry(tokens: string[], entry: KnowledgeEntry): number {
  let score = 0
  const title = normalize(entry.title)
  const body = normalize(entry.body)

  tokens.forEach(token => {
    if (!token) return
    if (title.includes(token)) score += 6
    if (entry.keywords.some(k => normalize(k).includes(token) || token.includes(normalize(k)))) score += 4
    if (entry.aliases.some(a => normalize(a).includes(token) || token.includes(normalize(a)))) score += 4
    if (body.includes(token)) score += 1
  })

  // 中文 bigram 覆盖率补充（提问较长时的长尾命中）
  tokens.forEach(token => {
    if (token.length < 4) return
    let hit = 0
    for (let i = 0; i + 2 <= token.length; i++) {
      if (body.includes(token.slice(i, i + 2))) hit++
    }
    score += Math.min(hit * 0.3, 2)
  })

  return score
}

const MATCH_THRESHOLD = 4

/** 关键词检索 Top-K；无有效命中返回空数组 */
export function searchEntries(query: string, entries: KnowledgeEntry[], topK = 3): KnowledgeEntry[] {
  if (!query || !entries.length) return []
  const tokens = expandQuery(query)
  const scored = entries
    .map(entry => ({ entry, score: scoreEntry(tokens, entry) }))
    .filter(item => item.score >= MATCH_THRESHOLD)
    .sort((a, b) => b.score - a.score)
  return scored.slice(0, topK).map(item => item.entry)
}

/**
 * 无有效命中时的兜底：取得分最高的一整类条目，避免把全量资料塞进 prompt 造成 token 膨胀
 */
export function pickCategoryEntries(query: string, entries: KnowledgeEntry[], limit = 6): KnowledgeEntry[] {
  if (!entries.length) return []
  const tokens = expandQuery(query)
  const byCategory = new Map<string, { score: number; items: KnowledgeEntry[] }>()
  entries.forEach(entry => {
    const bucket = byCategory.get(entry.category) || { score: 0, items: [] }
    bucket.score += scoreEntry(tokens, entry)
    bucket.items.push(entry)
    byCategory.set(entry.category, bucket)
  })
  const best = Array.from(byCategory.values()).sort((a, b) => b.score - a.score)[0]
  return best ? best.items.slice(0, limit) : entries.slice(0, limit)
}

/** FAQ 精确直答：命中返回答案，未命中返回 null */
export function matchFaq(query: string, entries: KnowledgeEntry[]): FaqHit | null {
  const normalized = normalize(query)
  if (normalized.length < 2) return null

  let best: { hit: FaqHit; weight: number } | null = null
  entries.forEach(entry => {
    entry.faq.forEach(faq => {
      const candidates = [faq.q, ...(faq.aliases || [])]
      candidates.forEach(candidate => {
        const normCandidate = normalize(candidate)
        if (normCandidate.length < 3) return
        if (!normalized.includes(normCandidate)) return
        const weight = normCandidate.length
        if (!best || weight > best.weight) {
          best = { hit: { answer: faq.a, route: entry.route, entryTitle: entry.title }, weight }
        }
      })
    })
  })
  return best ? (best as { hit: FaqHit }).hit : null
}

/** 组装注入给模型的【系统资料】文本 */
export function buildKnowledgeContext(entries: KnowledgeEntry[], allowedRoutes: string[]): string {
  if (!entries.length) return ''
  const blocks = entries.map(entry => {
    const lines: string[] = []
    lines.push(`【${entry.title}】（分类：${entry.category}）`)
    lines.push(entry.body)
    if (entry.steps.length) {
      lines.push('步骤：')
      entry.steps.forEach((step, index) => lines.push(`${index + 1}. ${step}`))
    }
    const route = entry.route && allowedRoutes.includes(entry.route) ? entry.route : ''
    lines.push(`关联入口：${route || '暂无独立页面入口'}`)
    return lines.join('\n')
  })
  return blocks.join('\n\n')
}

/**
 * 轻量意图分流：判断提问属于「系统用法类」还是「诗词内容类」
 * 返回 null 表示无法判定
 */
const SYSTEM_HINTS = [
  '怎么用', '怎么玩', '在哪里', '在哪', '哪一页', '入口', '规则', '玩法', '怎么进入', '如何进入',
  '怎么做', '怎么操作', '教程', '帮助', '如何使用', '怎么参加', '怎么参与', '能做什么', '有什么功能',
  '需要登录', '要登录', '怎么注册', '怎么登录', '在哪里找', '在哪找', '按钮在哪', '什么意思这个页面'
]

const CONTENT_HINTS = [
  '这首诗', '这句', '这一句', '诗人', '作者', '赏析', '鉴赏', '翻译', '译文', '注释', '出处', '背景',
  '平仄', '对仗', '押韵', '意象', '含义', '意思', '评价', '写一首', '藏头', '接下一句', '下半句', '上联', '下联'
]

export function detectIntent(query: string): 'system' | 'poetry' | null {
  const normalized = normalize(query)
  if (!normalized) return null
  const systemHit = SYSTEM_HINTS.some(hint => normalized.includes(normalize(hint)))
  const contentHit = CONTENT_HINTS.some(hint => normalized.includes(normalize(hint)))
  if (systemHit && !contentHit) return 'system'
  if (contentHit && !systemHit) return 'poetry'
  return null
}
