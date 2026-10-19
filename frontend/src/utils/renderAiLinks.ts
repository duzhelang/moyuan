/**
 * 把 AI 回答中的 [[/route]] 标记解析为「可点击跳转」片段
 * 出于安全考虑，只有命中白名单（系统知识库中真实存在的 route）才会渲染成链接
 */

export interface AiSegment {
  type: 'text' | 'link'
  text: string
  /** 仅 type === 'link' 时存在 */
  route?: string
}

const LINK_PATTERN = /\[\[\s*(\/[^\][\s]*)\s*\]\]/g

/** 校验路由是否合法且在白名单内 */
export function isAllowedRoute(route: string, allowedRoutes: string[]): boolean {
  if (!route || !route.startsWith('/')) return false
  if (!allowedRoutes.length) return false
  return allowedRoutes.some(allowed => allowed === route)
}

/**
 * 解析回答文本
 * @param content AI 回答原文
 * @param allowedRoutes 白名单路由集合（通常取知识库条目的 route 汇总）
 */
export function renderAiLinks(content: string, allowedRoutes: string[] = []): AiSegment[] {
  if (!content) return []
  const segments: AiSegment[] = []
  let lastIndex = 0
  LINK_PATTERN.lastIndex = 0
  let match: RegExpExecArray | null

  while ((match = LINK_PATTERN.exec(content)) !== null) {
    const [raw, route] = match
    if (match.index > lastIndex) {
      segments.push({ type: 'text', text: content.slice(lastIndex, match.index) })
    }
    if (isAllowedRoute(route, allowedRoutes)) {
      segments.push({ type: 'link', text: '点击去体验', route })
    } else {
      // 非法路由不渲染成链接，原样丢弃标记
      segments.push({ type: 'text', text: route })
    }
    lastIndex = match.index + raw.length
  }

  if (lastIndex < content.length) {
    segments.push({ type: 'text', text: content.slice(lastIndex) })
  }
  return segments.filter(seg => seg.text !== '')
}

/** 收集知识库白名单路由（支持 /poem/:id 这类占位路由跳过） */
export function collectAllowedRoutes(routes: Array<string | null | undefined>): string[] {
  const set = new Set<string>()
  routes.forEach(route => {
    if (!route) return
    const clean = route.trim()
    if (!clean.startsWith('/')) return
    if (clean.includes(':')) return
    set.add(clean)
  })
  return Array.from(set)
}
