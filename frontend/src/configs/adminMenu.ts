/**
 * 后台菜单单一数据源。
 * 侧边栏、面包屑、标签页三处都从这里取，避免改一处漏一处。
 */

export interface AdminMenuItem {
  path: string
  name: string
  icon: string
  label: string
}

export interface AdminMenuGroup {
  label: string
  icon: string
  /** 无子菜单时直接指向页面 */
  path?: string
  name?: string
  children?: AdminMenuItem[]
}

export const adminMenus: AdminMenuGroup[] = [
  {
    label: '数据与监控',
    icon: 'DataAnalysis',
    children: [
      { path: '/admin/dashboard', name: 'AdminDashboard', icon: 'Odometer', label: '控制台' },
      { path: '/admin/monitor', name: 'AdminMonitor', icon: 'Monitor', label: '系统监测' },
      { path: '/admin/user-analytics', name: 'AdminUserAnalytics', icon: 'TrendCharts', label: '用户分析' },
      { path: '/admin/recommend', name: 'AdminRecommend', icon: 'DataLine', label: '推荐与实验' }
    ]
  },
  {
    label: '用户管理',
    icon: 'User',
    children: [{ path: '/admin/users', name: 'AdminUsers', icon: 'User', label: '用户管理' }]
  },
  {
    label: '内容管理',
    icon: 'Document',
    children: [
      { path: '/admin/poems', name: 'AdminPoems', icon: 'Reading', label: '诗词管理' },
      { path: '/admin/categories', name: 'AdminCategories', icon: 'FolderOpened', label: '分类管理' },
      { path: '/admin/dynasties', name: 'AdminDynasties', icon: 'Clock', label: '朝代管理' },
      { path: '/admin/poets', name: 'AdminPoets', icon: 'EditPen', label: '诗人管理' },
      { path: '/admin/poet-featured', name: 'AdminPoetFeatured', icon: 'Star', label: '精选诗人管理' },
      { path: '/admin/home-navigation', name: 'AdminHomeNavigation', icon: 'Guide', label: '首页导航管理' },
      { path: '/admin/vision-articles', name: 'AdminVisionArticles', icon: 'Notebook', label: '诗话视野管理' },
      { path: '/admin/system-knowledge', name: 'AdminSystemKnowledge', icon: 'Collection', label: '系统知识库' }
    ]
  },
  {
    label: '社区管理',
    icon: 'ChatLineSquare',
    children: [
      { path: '/admin/forum-posts', name: 'AdminForumPosts', icon: 'ChatLineSquare', label: '帖子管理' },
      { path: '/admin/reports', name: 'AdminReports', icon: 'Warning', label: '举报管理' },
      { path: '/admin/repairs', name: 'AdminRepairs', icon: 'Tickets', label: '报修管理' }
    ]
  },
  {
    label: 'AI功能',
    icon: 'Cpu',
    children: [
      { path: '/admin/ai-models', name: 'AdminAiModels', icon: 'Cpu', label: 'AI模型管理' },
      { path: '/admin/ai-config', name: 'AdminAiConfig', icon: 'Setting', label: 'AI功能配置' }
    ]
  },
  {
    label: '审核管理',
    icon: 'Checked',
    children: [
      { path: '/admin/audit-pipeline', name: 'AuditPipeline', icon: 'DataLine', label: '审核流水线' },
      { path: '/admin/audit-records', name: 'AuditRecords', icon: 'Document', label: '审核记录' },
      { path: '/admin/poem-audit', name: 'PoemAudit', icon: 'Reading', label: '诗词审核' },
      { path: '/admin/poet-profiles', name: 'PoetProfiles', icon: 'User', label: '诗人认证审核' },
      { path: '/admin/poet-suggestions', name: 'AdminPoetSuggestions', icon: 'Checked', label: '诗人内容审核' },
      { path: '/admin/ai-contents', name: 'AdminAiContents', icon: 'MagicStick', label: 'AI内容审核' },
      { path: '/admin/comments', name: 'Comments', icon: 'ChatLineSquare', label: '评论管理' }
    ]
  },
  {
    label: '系统管理',
    icon: 'Setting',
    children: [
      { path: '/admin/logs', name: 'AdminLogs', icon: 'Document', label: '操作日志' },
      { path: '/admin/static-pages', name: 'AdminStaticPages', icon: 'Document', label: '静态页面管理' },
      { path: '/admin/files', name: 'AdminFiles', icon: 'FolderOpened', label: '文件管理' },
      { path: '/admin/cache', name: 'AdminCache', icon: 'Refresh', label: '缓存管理' }
    ]
  }
]

/**
 * 审核员专属菜单：不展示管理员的数据监控、用户、内容、社区、AI、系统管理等分类，
 * 只保留审核任务与内容审核入口，避免审核员误入管理页面。
 */
export const reviewerMenus: AdminMenuGroup[] = [
  {
    label: '审核任务',
    icon: 'DataLine',
    children: [
      { path: '/admin/audit-pipeline', name: 'AuditPipeline', icon: 'DataLine', label: '审核流水线' },
      { path: '/admin/audit-records', name: 'AuditRecords', icon: 'Document', label: '审核记录' }
    ]
  },
  {
    label: '内容审核',
    icon: 'Checked',
    children: [
      { path: '/admin/poem-audit', name: 'PoemAudit', icon: 'Reading', label: '诗词审核' },
      { path: '/admin/poet-profiles', name: 'PoetProfiles', icon: 'User', label: '诗人认证审核' },
      { path: '/admin/poet-suggestions', name: 'AdminPoetSuggestions', icon: 'Checked', label: '诗人内容审核' },
      { path: '/admin/ai-contents', name: 'AdminAiContents', icon: 'MagicStick', label: 'AI内容审核' },
      { path: '/admin/comments', name: 'Comments', icon: 'ChatLineSquare', label: '评论管理' },
      { path: '/admin/forum-posts', name: 'AdminForumPosts', icon: 'ChatDotRound', label: '帖子管理' },
      { path: '/admin/reports', name: 'AdminReports', icon: 'Warning', label: '举报处理' }
    ]
  }
]

const flat = new Map<string, { group: string; label: string; icon: string }>()

for (const group of adminMenus) {
  for (const item of group.children || []) {
    flat.set(item.path, { group: group.label, label: item.label, icon: item.icon })
  }
}

/** 按路径查菜单信息；未登记的路径返回 null，由调用方兜底用 route.meta.title */
export function findAdminMenu(path: string) {
  return flat.get(path) || null
}

/**
 * 同一页面通过 query 切换内部视图时，给标签和面包屑补一个语义后缀，
 * 例如 /admin/monitor?tab=security → 「系统监测 · 安全审计」。
 * 未登记的参数不加工，标签只记 fullPath，不额外造标签。
 */
const QUERY_LABELS: Record<string, Record<string, string>> = {
  '/admin/monitor': {
    availability: '可用性',
    ai: 'AI 链路',
    security: '安全审计',
    ops: '运维资源'
  },
  '/admin/ai-models': {
    rag: 'RAG 配置'
  }
}

export function queryPageSuffix(path: string, query: Record<string, unknown>): string {
  const map = QUERY_LABELS[path]
  if (!map) return ''
  const key = typeof query?.tab === 'string' ? query.tab : ''
  return (key && map[key]) || ''
}
