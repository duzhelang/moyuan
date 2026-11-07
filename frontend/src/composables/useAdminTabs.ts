import { ref } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'
import { findAdminMenu, queryPageSuffix } from '@/configs/adminMenu'

export interface AdminTab {
  path: string
  /** 完整路径（含 query），切回标签时用它还原页面内定位 */
  fullPath: string
  label: string
  group: string
  /** 固定标签，不可关闭（控制台） */
  affix?: boolean
}

const STORAGE_KEY = 'admin:tabs:v1'
const AFFIX_TAB: AdminTab = {
  path: '/admin/dashboard',
  fullPath: '/admin/dashboard',
  label: '控制台',
  group: '数据与监控',
  affix: true
}

const MAX_TABS = 12

/** 旧版本存的是没有 fullPath 的数据，读出时补上，避免标签点不动 */
function normalize(tab: Partial<AdminTab>): AdminTab | null {
  if (!tab || typeof tab.path !== 'string') return null
  return {
    path: tab.path,
    fullPath: typeof tab.fullPath === 'string' ? tab.fullPath : tab.path,
    label: tab.label || tab.path,
    group: tab.group || '',
    affix: tab.affix
  }
}

function load(): AdminTab[] {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return [{ ...AFFIX_TAB }]
    const list = (JSON.parse(raw) as Partial<AdminTab>[])
      .map(normalize)
      .filter((t): t is AdminTab => !!t)
    if (!list.some((t) => t.path === AFFIX_TAB.path)) list.unshift({ ...AFFIX_TAB })
    return list.slice(0, MAX_TABS)
  } catch {
    return [{ ...AFFIX_TAB }]
  }
}

function persist(list: AdminTab[]) {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(list))
  } catch {
    /* 隐私模式下不可写，忽略即可 */
  }
}

/** 模块级单例：标签栏与布局组件共享同一份状态 */
const tabs = ref<AdminTab[]>(load())

/** 页面基础名 + query 语义后缀（如「系统监测 · 安全审计」） */
function buildLabel(route: RouteLocationNormalized): string {
  const menu = findAdminMenu(route.path)
  const base =
    menu?.label || (route.meta?.title as string) || route.path.split('/').pop() || '未命名'
  const suffix = queryPageSuffix(route.path, route.query as Record<string, unknown>)
  return suffix ? `${base} · ${suffix}` : base
}

export function useAdminTabs() {
  /**
   * 登记标签。
   * 同一页面只保留一个标签（避免 ?tab= 不同的同类页面堆满标签栏），
   * 但会记住最后一次访问的 fullPath，切回来时还原到对应视图。
   */
  const addTab = (route: RouteLocationNormalized) => {
    if (!route.path.startsWith('/admin') || route.path === '/admin') return

    const label = buildLabel(route)
    const fullPath = route.fullPath
    const index = tabs.value.findIndex((t) => t.path === route.path)

    // 已存在：只在 query 变化时更新定位与标签名
    if (index >= 0) {
      const existing = tabs.value[index]
      if (existing.fullPath === fullPath && existing.label === label) return
      const next = [...tabs.value]
      next[index] = { ...existing, fullPath, label }
      tabs.value = next
      persist(next)
      return
    }

    const menu = findAdminMenu(route.path)
    const next = [...tabs.value, { path: route.path, fullPath, label, group: menu?.group || '' }]

    // 超出上限时从第二个开始丢（第一个是固定的控制台）
    while (next.length > MAX_TABS) {
      const dropIndex = next.findIndex((t) => !t.affix)
      if (dropIndex < 0) break
      next.splice(dropIndex, 1)
    }

    tabs.value = next
    persist(next)
  }

  /** 关闭标签，返回被关闭标签原来的下标，供调用方决定跳到哪个相邻页 */
  const removeTab = (path: string) => {
    const index = tabs.value.findIndex((t) => t.path === path)
    if (index < 0 || tabs.value[index].affix) return -1
    const next = tabs.value.filter((t) => t.path !== path)
    tabs.value = next
    persist(next)
    return index
  }

  const closeOthers = (path: string) => {
    const next = tabs.value.filter((t) => t.affix || t.path === path)
    tabs.value = next
    persist(next)
  }

  const closeAll = () => {
    const next = tabs.value.filter((t) => t.affix)
    tabs.value = next
    persist(next)
  }

  /** 移除已不存在的路由（例如权限变化），避免死标签 */
  const prune = (exists: (path: string) => boolean) => {
    const next = tabs.value.filter((t) => t.affix || exists(t.path))
    if (next.length !== tabs.value.length) {
      tabs.value = next
      persist(next)
    }
  }

  return { tabs, addTab, removeTab, closeOthers, closeAll, prune }
}
