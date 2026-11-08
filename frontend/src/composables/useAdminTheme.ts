import { computed, ref, watch } from 'vue'

/** 后台可选主题：三套实际主题 + 双境自动 */
export type AdminTheme = 'auto' | 'paper' | 'night' | 'ink'
export type AdminResolvedTheme = 'paper' | 'night' | 'ink'
/** 视图模式：运营视图走浅色，评测视图走深色 */
export type AdminViewMode = 'ops' | 'eval'

const THEME_KEY = 'moyuan_admin_theme'
const VIEW_KEY = 'moyuan_admin_view'

/** 隐私模式或禁用存储时 localStorage 可能抛错，统一兜底 */
const safeGet = (key: string): string | null => {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

const safeSet = (key: string, value: string) => {
  try {
    localStorage.setItem(key, value)
  } catch {
    // 存储不可用时仅本次会话生效
  }
}

const readTheme = (): AdminTheme => {
  const saved = safeGet(THEME_KEY)
  return saved === 'paper' || saved === 'night' || saved === 'ink' || saved === 'auto'
    ? saved
    : 'auto'
}

const readView = (): AdminViewMode => {
  const saved = safeGet(VIEW_KEY)
  return saved === 'eval' ? 'eval' : 'ops'
}

const theme = ref<AdminTheme>(readTheme())
const viewMode = ref<AdminViewMode>(readView())

/** 双境自动：评测视图深色，其余浅色宣纸 */
const resolvedTheme = computed<AdminResolvedTheme>(() => {
  if (theme.value !== 'auto') return theme.value
  return viewMode.value === 'eval' ? 'night' : 'paper'
})

/** 写入 body，弹层（teleport）与 Element 组件都能继承 */
const applyThemeAttr = () => {
  document.body.dataset.adminTheme = resolvedTheme.value
}

const persist = () => {
  safeSet(THEME_KEY, theme.value)
  safeSet(VIEW_KEY, viewMode.value)
}

watch([theme, viewMode], () => {
  persist()
  applyThemeAttr()
})

export const useAdminTheme = () => {
  const setTheme = (value: AdminTheme) => {
    theme.value = value
  }

  const setViewMode = (value: AdminViewMode) => {
    viewMode.value = value
  }

  /** 进入后台布局时启用主题作用域 */
  const activate = () => {
    document.body.classList.add('admin-theme-active')
    applyThemeAttr()
  }

  /** 离开后台时移除，避免影响前台页面 */
  const deactivate = () => {
    document.body.classList.remove('admin-theme-active')
    delete document.body.dataset.adminTheme
  }

  return {
    theme,
    viewMode,
    resolvedTheme,
    setTheme,
    setViewMode,
    activate,
    deactivate
  }
}

/** 应用启动时预先写入主题；直接刷新后台地址时同步启用作用域，避免首帧闪白 */
export const initAdminTheme = () => {
  if (window.location.pathname.startsWith('/admin')) {
    document.body.classList.add('admin-theme-active')
  }
  applyThemeAttr()
}

/** 图表取色：统一从 CSS 变量读取，切换主题后可重新绘制 */
export const readChartPalette = (): string[] => {
  const styles = getComputedStyle(document.body)
  return [1, 2, 3, 4, 5, 6].map((i) =>
    styles.getPropertyValue(`--adm-chart-${i}`).trim()
  )
}

/**
 * 统一图表主题：把轴、网格、图例、tooltip、渐变集中到一处，
 * 三个后台页面不再各自复制一份配置，换主题时只需改这里。
 */
export const readChartTheme = () => {
  const palette = readChartPalette()
  const ink = readChartInk()
  return {
    palette,
    ink,
    tooltip: {
      trigger: 'axis' as const,
      backgroundColor: ink.surface,
      borderColor: ink.border,
      borderWidth: 1,
      padding: [8, 12] as [number, number],
      textStyle: { color: ink.regular, fontSize: 12 }
    },
    legend: {
      top: 0,
      right: 0,
      itemWidth: 10,
      itemHeight: 6,
      itemGap: 12,
      textStyle: { color: ink.muted, fontSize: 11 }
    },
    grid: { left: 2, right: 8, bottom: 2, top: 30, containLabel: true },
    categoryAxis: {
      axisLine: { lineStyle: { color: ink.border } },
      axisTick: { show: false },
      axisLabel: { color: ink.muted, fontSize: 11 },
      splitLine: { show: false }
    },
    valueAxis: {
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: ink.muted, fontSize: 11 },
      splitLine: { lineStyle: { color: ink.grid, type: 'dashed' as const } }
    },
    /** 面积渐变：8 位十六进制叠加透明度，ECharts 可直接识别 */
    areaGradient: (hex: string) => ({
      color: {
        type: 'linear',
        x: 0,
        y: 0,
        x2: 0,
        y2: 1,
        colorStops: [
          { offset: 0, color: `${hex}2e` },
          { offset: 1, color: `${hex}05` }
        ]
      }
    }),
    /** 柱状渐变：上浓下淡 */
    barGradient: (hex: string) => ({
      type: 'linear',
      x: 0,
      y: 0,
      x2: 0,
      y2: 1,
      colorStops: [
        { offset: 0, color: hex },
        { offset: 1, color: `${hex}2e` }
      ]
    })
  }
}

/**
 * 图表文字/线条取色：坐标轴刻度、网格、图例、tooltip 背景。
 * 这些必须走主题 token，否则深色主题下刻度会糊在背景里。
 */
export const readChartInk = () => {
  const styles = getComputedStyle(document.body)
  const read = (name: string, fallback: string) =>
    styles.getPropertyValue(name).trim() || fallback
  return {
    text: read('--adm-text', '#2b241c'),
    regular: read('--adm-text-2', '#4a3f32'),
    muted: read('--adm-text-muted', '#909399'),
    faint: read('--adm-text-faint', '#c0c4cc'),
    border: read('--adm-border-soft', 'rgba(140, 140, 140, 0.16)'),
    surface: read('--adm-surface', '#ffffff'),
    grid: read('--adm-grid-line', 'rgba(140, 140, 140, 0.12)')
  }
}
