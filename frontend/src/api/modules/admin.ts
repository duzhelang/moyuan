import request from '@/utils/request'
import type { User } from '@/types/model'

export function getUserList(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<{ records: User[]; total: number }>('/admin/users', { params })
}

export function getUserById(id: number) {
  return request.get<User>(`/admin/users/${id}`)
}

export function createUser(data: { username: string; password: string; email?: string; nickname?: string; role?: string; phone?: string }) {
  return request.post<User>('/admin/users', data)
}

export function updateUser(id: number, data: Record<string, any>) {
  return request.put<User>(`/admin/users/${id}`, data)
}

export function deleteUser(id: number) {
  return request.delete<void>(`/admin/users/${id}`)
}

export function getAdminPoems(params: { page?: number; size?: number; keyword?: string; auditStatus?: number; sort?: string }) {
  return request.get<{ records: any[]; total: number }>('/admin/poems', { params })
}

export function createAdminPoem(data: any) {
  return request.post<any>('/admin/poems', data)
}

export function updateAdminPoem(id: number, data: any) {
  return request.put<any>(`/admin/poems/${id}`, data)
}

export function deleteAdminPoem(id: number) {
  return request.delete<void>(`/admin/poems/${id}`)
}

export function getAdminCategories(params: { page?: number; size?: number }) {
  return request.get<{ records: any[]; total: number }>('/admin/categories', { params })
}

export function createAdminCategory(data: any) {
  return request.post<any>('/admin/categories', data)
}

export function updateAdminCategory(id: number, data: any) {
  return request.put<any>(`/admin/categories/${id}`, data)
}

export function deleteAdminCategory(id: number) {
  return request.delete<void>(`/admin/categories/${id}`)
}

export function getAdminDynasties(params: { page?: number; size?: number }) {
  return request.get<{ records: any[]; total: number }>('/admin/dynasties', { params })
}

export function createAdminDynasty(data: any) {
  return request.post<any>('/admin/dynasties', data)
}

export function updateAdminDynasty(id: number, data: any) {
  return request.put<any>(`/admin/dynasties/${id}`, data)
}

export function deleteAdminDynasty(id: number) {
  return request.delete<void>(`/admin/dynasties/${id}`)
}

export function getAdminPoets(params: { page?: number; size?: number; keyword?: string; sort?: string }) {
  return request.get<{ records: any[]; total: number }>('/admin/poets', { params })
}

export function createAdminPoet(data: any) {
  return request.post<any>('/admin/poets', data)
}

export function updateAdminPoet(id: number, data: any) {
  return request.put<any>(`/admin/poets/${id}`, data)
}

export function deleteAdminPoet(id: number) {
  return request.delete<void>(`/admin/poets/${id}`)
}

/** 刷新诗人头像：按最新规则回填缺失/地区错配的预设头像，保留真实肖像 */
export function refreshPoetAvatars() {
  return request.post<number>('/admin/poets/refresh-avatars')
}

export function getAdminForumPosts(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<{ records: any[]; total: number }>('/admin/forum-posts', { params })
}

export function updateForumPostStatus(id: number, status: number) {
  return request.put<void>(`/admin/forum-posts/${id}/status`, { status })
}

export function deleteAdminForumPost(id: number) {
  return request.delete<void>(`/admin/forum-posts/${id}`)
}

export function getAdminStats() {
  return request.get<Record<string, number>>('/admin/stats')
}

/** 用户结构与活跃留存（角色分布 / 7d·30d 活跃 / 回访率） */
export interface UserStructureRole {
  key: string
  label: string
  value: number
}

export interface UserStructure {
  totalUsers: number
  newUsers7d: number
  roles: UserStructureRole[]
  active7d: number
  active30d: number
  eligible7d: number
  eligible30d: number
  retention7d: number
  retention30d: number
  silent30d: number
}

export function getUserStructure() {
  return request.get<UserStructure>('/admin/stats/user-structure')
}

/** 用户活跃热力图：星期 × 小时，值为访问请求数，users 为去重登录用户 */
export interface UserHeatmapCell {
  weekday: number
  hour: number
  requests: number
  users: number
}

export interface UserHeatmap {
  days: number
  available: boolean
  message?: string
  maxRequests: number
  cells: UserHeatmapCell[]
}

export function getUserHeatmap(days = 7) {
  return request.get<UserHeatmap>('/admin/stats/user-heatmap', {
    params: { days },
    silent: true
  })
}

export function getAdminStatsTrend(days = 7) {
  return request.get<{
    dates: string[]
    poems: number[]
    posts: number[]
    users: number[]
  }>('/admin/stats/trend', { params: { days } })
}

/** 热门诗词：收藏 × 3 + 浏览，按近 N 天真实互动排序 */
export interface HotPoem {
  id: number
  title: string
  favorites: number
  views: number
  score: number
}

export function getHotPoems(days = 30, limit = 5) {
  return request.get<HotPoem[]>('/admin/stats/hot-poems', {
    params: { days, limit },
    silent: true
  })
}

export function getVisitStats() {
  return request.get<{
    todayVisits: number
    weekVisits: number
    monthVisits: number
    totalVisits: number
  }>('/admin/stats/visits')
}

export function getVisitTrend(days?: number) {
  return request.get<{
    dates: string[]
    visits: number[]
    uniqueVisitors: number[]
  }>('/admin/stats/visits/trend', { params: { days } })
}

/** 链路节点健康检查（管理端） */
export interface SystemHealthNode {
  key: string
  name: string
  status: 'ok' | 'warn' | 'down'
  latencyMs: number | null
  message?: string
  metrics: { label: string; value: string }[]
}

export interface SystemHealth {
  checkedAt: string
  nodes: SystemHealthNode[]
}

export function getSystemHealth() {
  return request.get<SystemHealth>('/admin/system/health', { silent: true })
}

/** 资源分布总览（内容 / AI / 存储 / 运行四域，后端 5 分钟缓存） */
export interface ResourceItem {
  key: string
  label: string
  value: number
  unit: 'count' | 'bytes' | 'percent' | 'seconds'
  group?: string
}

export interface ResourceGauge {
  key: string
  label: string
  used: number
  total: number
  unit: 'bytes' | 'count'
  status: 'up' | 'warn' | 'down'
}

export interface ResourceStatusNode {
  key: string
  label: string
  status: 'up' | 'warn' | 'down'
  latencyMs: number | null
  message: string
}

export interface ResourceCorpusFile {
  name: string
  bytes: number
  chunks: number
}

export interface ResourceIndexFile {
  name: string
  bytes: number
}

export interface ResourceRag {
  ready: boolean
  status: 'up' | 'warn' | 'down'
  message: string
  chunks: number
  corpusFiles: number
  corpusBytes: number
  corpusWords: number
  corpusDetails: ResourceCorpusFile[]
  indexDetails: ResourceIndexFile[]
  indexBytes: number
  indexFiles: number
  indexDim: number
  builtAt: string
  embed: string
  embedLabel: string
  generation: string
  ollama: boolean
  latencyMs: number | null
}

export interface ResourceOverview {
  generatedAt: string
  content: { items: ResourceItem[]; error?: string }
  ai: {
    ring: ResourceItem[]
    items: ResourceItem[]
    rag: ResourceRag
    defaultModel: string
    calls: { total24h: number; failed24h: number; failRate: number }
    error?: string
  }
  storage: {
    ring: ResourceItem[]
    items: ResourceItem[]
    rag: ResourceRag
    mysqlLatencyMs: number
    redisLatencyMs: number
    error?: string
  }
  runtime: {
    gauges: ResourceGauge[]
    nodes: ResourceStatusNode[]
    items: ResourceItem[]
    error?: string
  }
}

export function getResourceOverview() {
  return request.get<ResourceOverview>('/admin/system/resource-overview', { silent: true })
}

/** 接口调用指标（错误率 / P95 / 慢接口） */
export interface SystemMetricsSlowApi {
  path: string
  count: number
  avgMs: number
  maxMs: number
}

export interface SystemMetricsHourlyPoint {
  label: string
  count: number
  errorCount: number
  avgMs: number
}

export interface SystemMetricsStatusBucket {
  name: string
  count: number
}

export interface SystemMetrics {
  windowHours: number
  available: boolean
  message?: string
  total: number
  errorCount: number
  errorRate: number
  avgMs: number
  p95Ms: number | null
  slowTop: SystemMetricsSlowApi[]
  hourly: SystemMetricsHourlyPoint[]
  statusBuckets: SystemMetricsStatusBucket[]
}

export function getSystemMetrics(hours = 24) {
  return request.get<SystemMetrics>('/admin/system/metrics', {
    params: { hours },
    silent: true
  })
}

export interface SearchKeywordRow {
  keyword: string
  count: number
  avgResult: number
}

export interface SearchDailyRow {
  date: string
  total: number
  noResult: number
}

export interface SearchMetrics {
  windowDays: number
  available: boolean
  message?: string
  total: number
  noResult: number
  noResultRate: number
  distinctKeywords: number
  topKeywords: SearchKeywordRow[]
  topNoResult: SearchKeywordRow[]
  trend: SearchDailyRow[]
}

export function getSearchMetrics(days = 7) {
  return request.get<SearchMetrics>('/admin/system/search-metrics', {
    params: { days },
    silent: true
  })
}

export interface AiEndpointRow {
  endpoint: string
  count: number
  errorCount: number
  avgMs: number
}

export interface AiModelRow {
  model: string
  count: number
  errorCount: number
}

export interface AiMetricsHourlyPoint {
  label: string
  count: number
  errorCount: number
  avgMs: number
}

export interface AiMetrics {
  windowHours: number
  available: boolean
  message?: string
  total: number
  errorCount: number
  errorRate: number
  avgMs: number
  p95Ms: number | null
  ttftAvgMs: number | null
  streamCount: number
  byEndpoint: AiEndpointRow[]
  byModel: AiModelRow[]
  hourly: AiMetricsHourlyPoint[]
}

export function getAiMetrics(hours = 24) {
  return request.get<AiMetrics>('/admin/system/ai-metrics', {
    params: { hours },
    silent: true
  })
}

export interface SecurityTypeRow {
  type: string
  count: number
}

export interface SecurityIpRow {
  ip: string
  count: number
}

export interface SecurityHourlyPoint {
  label: string
  count: number
  loginFail: number
}

export interface SecurityMetrics {
  windowHours: number
  available: boolean
  message?: string
  loginFail: number
  forbidden: number
  rateLimit: number
  total: number
  distinctIp: number
  highRiskOps: number | null
  byType: SecurityTypeRow[]
  topIps: SecurityIpRow[]
  hourly: SecurityHourlyPoint[]
}

export function getSecurityMetrics(hours = 24) {
  return request.get<SecurityMetrics>('/admin/system/security-metrics', {
    params: { hours },
    silent: true
  })
}

export interface RecentEvent {
  time: string
  source: 'ai' | 'security' | 'api'
  title: string
  detail: string
}

export function getRecentEvents(limit = 12) {
  return request.get<RecentEvent[]>('/admin/system/recent-events', {
    params: { limit },
    silent: true
  })
}

export function getAdminLogs(params?: { page?: number; size?: number; keyword?: string; startTime?: string; endTime?: string }) {
  return request.get<{ records: any[]; total: number }>('/admin/logs', { params })
}

export function getAdminPoetFeatured(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<{ records: any[]; total: number }>('/admin/poet-featured', { params })
}

export function getAdminPoetFeaturedById(id: number) {
  return request.get<any>(`/admin/poet-featured/${id}`)
}

export function createAdminPoetFeatured(data: any) {
  return request.post<any>('/admin/poet-featured', data)
}

export function updateAdminPoetFeatured(id: number, data: any) {
  return request.put<any>(`/admin/poet-featured/${id}`, data)
}

export function deleteAdminPoetFeatured(id: number) {
  return request.delete<void>(`/admin/poet-featured/${id}`)
}

export function updateAdminPoetFeaturedShow(id: number, isShow: number) {
  return request.put<void>(`/admin/poet-featured/${id}/show`, { isShow })
}

export function batchShowAdminPoetFeatured(ids: number[], isShow: number) {
  return request.put<void>('/admin/poet-featured/batch-show', { ids, isShow })
}

export function getHomeNavigationList(params?: { type?: string }) {
  return request.get<any[]>('/home-navigation', { params })
}

export function getAdminHomeNavigation(params: { page?: number; size?: number; type?: string }) {
  return request.get<{ records: any[]; total: number }>('/admin/home-navigation/manage', { params })
}

export function createHomeNavigation(data: any) {
  return request.post<any>('/admin/home-navigation', data)
}

export function updateHomeNavigation(id: number, data: any) {
  return request.put<any>(`/admin/home-navigation/${id}`, data)
}

export function deleteHomeNavigation(id: number) {
  return request.delete<void>(`/admin/home-navigation/${id}`)
}

export function getAdminPoetSuggestions(params: { pageNum?: number; pageSize?: number; status?: string }) {
  return request.get<{ records: any[]; total: number }>('/admin/poet-suggestions', { params })
}

export function reviewPoetSuggestion(id: number, status: string, reviewComment?: string) {
  return request.put(`/admin/poet-suggestions/${id}/review`, null, {
    params: { status, reviewComment }
  })
}

export function getAdminAiContents(params: { page?: number; size?: number; status?: number | null }) {
  return request.get<{ records: any[]; total: number }>('/admin/ai-contents', { params })
}

export function approveAiContent(id: number, reviewComment?: string) {
  return request.put<void>(`/admin/ai-contents/${id}/approve`, { reviewComment })
}

export function rejectAiContent(id: number, reviewComment?: string) {
  return request.put<void>(`/admin/ai-contents/${id}/reject`, { reviewComment })
}

export function auditPoem(id: number, status: number, reason?: string) {
  return request.put<void>(`/admin/poems/${id}/audit`, { status, reason })
}

export function getAdminPoetProfiles(params: { page?: number; size?: number; status?: number }) {
  return request.get<{ records: any[]; total: number }>('/admin/poet-profiles', { params })
}

export function verifyPoetProfile(id: number, status: number, reason?: string) {
  return request.put<void>(`/admin/poet-profiles/${id}/verify`, { status, reason })
}

export function getAdminComments(params: { page?: number; size?: number; status?: number | null }) {
  return request.get<{ records: any[]; total: number }>('/admin/comments', { params })
}

export function auditComment(id: number, status: number, reason?: string) {
  return request.put<void>(`/admin/comments/${id}/audit`, { status, reason })
}

export function deleteAdminComment(id: number) {
  return request.delete<void>(`/admin/comments/${id}`)
}

export function getAuditStats() {
  return request.get<Record<string, number>>('/admin/audit/stats')
}

export function getAdminRepairs(params: { page?: number; size?: number; status?: number | null; category?: string; priority?: number | null; keyword?: string }) {
  // 后端报修分页返回 list（举报接口返回 records），两者都声明以兼容历史字段
  return request.get<{ list?: any[]; records?: any[]; total: number; pageNum?: number; pageSize?: number }>(
    '/admin/repairs',
    { params }
  )
}

export function getAdminRepairDetail(id: number) {
  return request.get<any>(`/admin/repairs/${id}`)
}

export function updateRepairStatus(id: number, data: { status: number; resolveContent?: string }) {
  return request.put<void>(`/admin/repairs/${id}/status`, data)
}

export function assignRepairOrder(id: number, assigneeId: number) {
  return request.put<void>(`/admin/repairs/${id}/assign`, { assigneeId })
}

export function addRepairInternalComment(id: number, data: { content: string; images?: string }) {
  return request.post<any>(`/admin/repairs/${id}/comments`, data)
}

export function getAdminRepairComments(id: number) {
  return request.get<any[]>(`/admin/repairs/${id}/comments`)
}

export function getRepairStats() {
  return request.get<Record<string, number>>('/admin/repairs/stats')
}

export function clearPoetCache() {
  return request.post<string>('/admin/cache/clear-poets')
}

export function clearAllCache() {
  return request.post<string>('/admin/cache/clear-all')
}

export function getAdminFiles(params: { page?: number; size?: number; fileType?: string; keyword?: string; status?: number | null }) {
  return request.get<{ records: any[]; total: number }>('/admin/files', { params })
}

export function getFileStorageStats() {
  return request.get<any>('/admin/files/stats')
}

export function cleanOrphanFiles() {
  return request.post<any>('/admin/files/clean-orphans')
}

export function deleteAdminFile(id: number) {
  return request.delete<void>(`/admin/files/${id}`)
}
