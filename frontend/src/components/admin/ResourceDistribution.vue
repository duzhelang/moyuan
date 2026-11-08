<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getResourceOverview } from '@/api/modules/admin'
import type {
  ResourceItem,
  ResourceOverview,
  ResourceStatusNode
} from '@/api/modules/admin'
import { readChartInk, readChartPalette, useAdminTheme } from '@/composables/useAdminTheme'

use([CanvasRenderer, PieChart, TitleComponent, TooltipComponent])

type DomainKey = 'content' | 'ai' | 'storage' | 'runtime'

const TABS: { key: DomainKey; label: string; note: string }[] = [
  { key: 'content', label: '内容资源', note: '内容主体占比' },
  { key: 'ai', label: 'AI 资源', note: '模型库提供方占比' },
  { key: 'storage', label: '存储资源', note: '文件类型占比' },
  { key: 'runtime', label: '运行资源', note: '项目运行状态' }
]

const GROUP_LABELS: Record<string, string> = {
  subject: '内容主体',
  structure: '结构细分',
  role: '用户角色',
  behavior: '社区行为',
  governance: '治理数据',
  model: '模型',
  module: '模块绑定',
  rag: 'RAG 资源',
  corpus: '语料资源',
  call: '近 24 小时',
  file: '文件',
  database: '数据库',
  cache: '缓存',
  jvm: 'JVM',
  mysql: 'MySQL',
  redis: 'Redis'
}

/** 可下钻条目：与后台管理页一一对应 */
const ROUTES: Record<string, { path: string; query?: Record<string, string> }> = {
  poem: { path: '/admin/poems' },
  poet: { path: '/admin/poets' },
  user: { path: '/admin/users' },
  post: { path: '/admin/forum-posts' },
  comment: { path: '/admin/comments' },
  category: { path: '/admin/categories' },
  dynasty: { path: '/admin/dynasties' },
  audit_backlog: { path: '/admin/audit-pipeline' },
  ai_generated: { path: '/admin/ai-contents' },
  report: { path: '/admin/reports' },
  repair: { path: '/admin/repairs' },
  model_total: { path: '/admin/ai-models' },
  model_enabled: { path: '/admin/ai-models' },
  rag_chunks: { path: '/admin/ai-models', query: { tab: 'rag' } },
  file_count: { path: '/admin/files' },
  file_size: { path: '/admin/files' }
}

const REFRESH_MS = 5 * 60 * 1000

const router = useRouter()
const { resolvedTheme } = useAdminTheme()
const palette = computed(() => {
  void resolvedTheme.value
  return readChartPalette()
})
const chartInk = computed(() => {
  void resolvedTheme.value
  return readChartInk()
})

const overview = ref<ResourceOverview | null>(null)
const loading = ref(true)
const loadError = ref('')
const updatedAt = ref('—')
const activeTab = ref<DomainKey>('content')

let timer: ReturnType<typeof setInterval> | undefined

const load = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getResourceOverview()
    overview.value = res.data
    updatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } catch (error) {
    console.error('获取资源分布失败', error)
    if (!overview.value) loadError.value = '资源数据获取失败，请确认后端已重启'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
  timer = setInterval(load, REFRESH_MS)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const activeNote = computed(() => TABS.find((t) => t.key === activeTab.value)?.note || '')

/** 当前域的降级错误 */
const domainError = computed(() => {
  const data = overview.value
  if (!data) return ''
  return (data[activeTab.value] as { error?: string })?.error || ''
})

/** 环图数据：内容域只看主体，AI 域看提供方，存储域看文件类型 */
const ringItems = computed<ResourceItem[]>(() => {
  const data = overview.value
  if (!data) return []
  if (activeTab.value === 'content') {
    return data.content.items.filter((item) => item.group === 'subject')
  }
  if (activeTab.value === 'ai') return data.ai.ring || []
  if (activeTab.value === 'storage') return data.storage.ring || []
  return []
})

const ringTotal = computed(() => ringItems.value.reduce((sum, item) => sum + (item.value || 0), 0))

/** 明细行按后端给的 group 顺序分组 */
const rowGroups = computed(() => {
  const data = overview.value
  if (!data) return []
  let items: ResourceItem[] = []
  if (activeTab.value === 'content') items = data.content.items
  else if (activeTab.value === 'ai') items = data.ai.items
  else if (activeTab.value === 'storage') items = data.storage.items
  else items = data.runtime.items

  const groups: { key: string; label: string; items: ResourceItem[] }[] = []
  for (const item of items) {
    const key = item.group || 'other'
    let group = groups.find((g) => g.key === key)
    if (!group) {
      group = { key, label: GROUP_LABELS[key] || '其他', items: [] }
      groups.push(group)
    }
    group.items.push(item)
  }
  return groups
})

/** 明细行条形的基准值：主体/字节展示真实占比，其余按「同分组 + 同单位」最大值 */
const baseFor = (item: ResourceItem) => {
  const data = overview.value
  if (!data) return 1
  if (activeTab.value === 'content' && item.group === 'subject' && item.unit === 'count') {
    return Math.max(1, ringTotal.value)
  }
  if (activeTab.value === 'storage' && item.unit === 'bytes') {
    const total = data.storage.items
      .filter((i) => i.unit === 'bytes')
      .reduce((sum, i) => sum + i.value, 0)
    return Math.max(1, total)
  }
  const sameGroupUnit = (activeTab.value === 'content'
    ? data.content.items
    : data[activeTab.value === 'ai' ? 'ai' : 'storage'].items
  ).filter((i) => i.group === item.group && i.unit === item.unit)
  return Math.max(1, ...sameGroupUnit.map((i) => i.value))
}

const showPercent = (item: ResourceItem) => {
  if (activeTab.value === 'content') {
    return item.group === 'subject' && item.unit === 'count'
  }
  return activeTab.value === 'storage' && item.unit === 'bytes'
}

/**
 * 是否画进度条：
 * - 内容主体、存储字节展示真实占比；
 * - 其余只在同分组、同单位存在多个可比项时画相对条；
 * - 单值分组不画，避免出现「全是 100%」的无意义横线。
 */
const showBar = (item: ResourceItem) => {
  if (activeTab.value === 'runtime') return false
  if (showPercent(item)) return true
  const data = overview.value
  if (!data) return false
  const items = activeTab.value === 'content'
    ? data.content.items
    : activeTab.value === 'ai'
      ? data.ai.items
      : data.storage.items
  return items.filter((i) => i.group === item.group && i.unit === item.unit).length > 1
}

const barPercent = (item: ResourceItem) => Math.min(100, (item.value / baseFor(item)) * 100)
const sharePercent = (item: ResourceItem) => (item.value / baseFor(item)) * 100

const formatCount = (value: number) => {
  if (!Number.isFinite(value)) return '0'
  if (value >= 100_000_000) return `${(value / 100_000_000).toFixed(2)} 亿`
  if (value >= 10_000) return `${(value / 10_000).toFixed(1)} 万`
  return value.toLocaleString('zh-CN')
}

const formatBytes = (value: number) => {
  if (!Number.isFinite(value) || value <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = value
  let index = 0
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024
    index += 1
  }
  return `${size >= 100 ? size.toFixed(0) : size.toFixed(1)} ${units[index]}`
}

const formatDuration = (seconds: number) => {
  if (!Number.isFinite(seconds) || seconds <= 0) return '—'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (days > 0) return `${days} 天 ${hours} 小时`
  if (hours > 0) return `${hours} 小时 ${minutes} 分`
  return `${minutes} 分钟`
}

const formatValue = (item: ResourceItem) => {
  if (item.unit === 'bytes') return formatBytes(item.value)
  if (item.unit === 'percent') return `${item.value.toFixed(1)}%`
  if (item.unit === 'seconds') return formatDuration(item.value)
  return formatCount(item.value)
}

/** 语料文件名转可读路径：local__docs__api__auth.md → docs/api/auth.md */
const prettyCorpusName = (name: string) => {
  if (!name.startsWith('local__')) return name
  return name
    .replace(/^local__user__/, '投放/')
    .replace(/^local__/, '')
    .replace(/__/g, '/')
}

/** 生成语料排前面，本地文档随后，保证有限的展示高度内先看到核心来源 */
const corpusFiles = computed(() => {
  const files = (overview.value?.storage.rag.corpusDetails || []).map((file) => ({
    ...file,
    shortName: prettyCorpusName(file.name),
    generated: !file.name.startsWith('local__')
  }))
  return [...files.filter((file) => file.generated), ...files.filter((file) => !file.generated)]
})

const corpusLocalCount = computed(() => corpusFiles.value.filter((file) => !file.generated).length)

const ringOption = computed(() => {
  const paletteColors = palette.value
  const items = ringItems.value
  const total = ringTotal.value
  const data = items.map((item, index) => ({
    name: item.label,
    value: item.value,
    itemStyle: { color: paletteColors[index % paletteColors.length] }
  }))
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: chartInk.value.surface,
      borderColor: chartInk.value.border,
      textStyle: { color: chartInk.value.regular, fontSize: 12 },
      formatter: (params: { dataIndex: number; percent: number }) => {
        const item = items[params.dataIndex]
        if (!item) return ''
        return `${item.label}<br/>${formatValue(item)} · ${params.percent}%`
      }
    },
    title: {
      text: total > 0 ? formatCount(total) : '—',
      subtext: activeNote.value,
      left: 'center',
      top: '40%',
      textStyle: {
        color: chartInk.value.text,
        fontSize: 17,
        fontWeight: 600,
        fontFamily: 'var(--adm-font-serif)'
      },
      subtextStyle: { color: chartInk.value.muted, fontSize: 10, lineHeight: 18 }
    },
    series: [
      {
        type: 'pie',
        radius: ['47%', '72%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: chartInk.value.surface,
          borderWidth: 2
        },
        label: { show: false },
        emphasis: {
          label: {
            show: true,
            fontSize: 12,
            fontWeight: 'bold',
            color: chartInk.value.text,
            formatter: '{b}\n{d}%'
          }
        },
        labelLine: { show: false },
        data
      }
    ]
  }
})

const nodeTone = (node: ResourceStatusNode) => {
  if (node.status === 'up') return 'ok'
  if (node.status === 'warn') return 'warn'
  return 'down'
}

const goTo = (item: ResourceItem) => {
  const route = ROUTES[item.key]
  if (!route) return
  router.push({ path: route.path, query: route.query })
}
</script>

<template>
  <section v-loading="loading" class="panel resource-panel">
    <div class="res-head">
      <div class="res-title-wrap">
        <span class="res-title">资源分布</span>
        <span class="res-note">内容 / AI / 存储 / 运行 · 5 分钟自动刷新</span>
      </div>
      <span class="res-updated">更新于 {{ updatedAt }}</span>
    </div>

    <div class="res-tabs" role="tablist">
      <button
        v-for="tab in TABS"
        :key="tab.key"
        class="res-tab"
        :class="{ active: activeTab === tab.key }"
        role="tab"
        :aria-selected="activeTab === tab.key"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <div
      v-if="activeTab === 'ai' && overview && !domainError"
      class="domain-tip"
    >
      默认模型 {{ overview.ai.defaultModel }} · RAG {{ overview.ai.rag.ready ? '已就绪' : '未就绪' }}
      · Ollama {{ overview.ai.rag.ollama ? '在线' : '离线' }}
    </div>

    <div
      v-if="activeTab === 'storage' && overview && !domainError"
      class="vector-card"
    >
      <div class="vector-head">
        <span class="vector-title">
          <i class="vector-led" :class="{ ready: overview.storage.rag.ready }"></i>
          RAG 向量库
        </span>
        <span class="vector-badge">
          {{ overview.storage.rag.ready ? '已就绪' : '未就绪' }}
          · {{ overview.storage.rag.indexDim }} 维
        </span>
      </div>
      <div class="vector-facts">
        <span>嵌入模型 <b>{{ overview.storage.rag.embedLabel || overview.storage.rag.embed || '—' }}</b></span>
        <span>文本块 <b>{{ overview.storage.rag.chunks }}</b></span>
        <span>语料字数 <b>{{ formatCount(overview.storage.rag.corpusWords) }}</b></span>
        <span>索引文件 <b>{{ overview.storage.rag.indexFiles }}</b></span>
        <span>索引体积 <b>{{ formatBytes(overview.storage.rag.indexBytes) }}</b></span>
        <span>构建时间 <b>{{ overview.storage.rag.builtAt || '—' }}</b></span>
      </div>
      <div v-if="overview.storage.rag.indexDetails?.length" class="index-files">
        <span class="corpus-label">索引构成</span>
        <span
          v-for="file in overview.storage.rag.indexDetails"
          :key="file.name"
          class="index-chip"
        >
          {{ file.name }} · {{ formatBytes(file.bytes) }}
        </span>
      </div>
      <div v-if="corpusFiles.length" class="corpus-files">
        <div class="corpus-head">
          <span class="corpus-label">
            语料来源（{{ corpusFiles.length }} 个文件：生成 {{ corpusFiles.length - corpusLocalCount }} · 本地 {{ corpusLocalCount }}）
          </span>
          <span class="corpus-hint">列表可滚动</span>
        </div>
        <div class="corpus-list">
          <div
            v-for="file in corpusFiles"
            :key="file.name"
            class="corpus-row"
            :title="file.name"
          >
            <span class="corpus-name">{{ file.shortName }}</span>
            <span class="corpus-meta">{{ file.chunks }} 块 · {{ formatBytes(file.bytes) }}</span>
          </div>
        </div>
      </div>
      <p v-else class="corpus-empty">
        未读取到语料文件；请先运行 corpus_export.py 并重建索引
      </p>
    </div>

    <div v-if="domainError || loadError" class="res-empty">{{ domainError || loadError }}</div>

    <template v-else-if="!overview">
      <div class="res-empty">{{ loading ? '资源数据加载中…' : '暂无资源数据' }}</div>
    </template>

    <template v-else-if="activeTab !== 'runtime'">
      <div class="res-body">
        <div class="ring-wrap">
          <v-chart v-if="ringItems.length" class="ring-chart" :option="ringOption" autoresize />
          <div v-else class="ring-empty">暂无分布数据</div>
        </div>

        <div class="res-rows">
          <template v-for="group in rowGroups" :key="group.key">
            <div class="group-label">{{ group.label }}</div>
            <div
              v-for="(item, index) in group.items"
              :key="item.key"
              class="res-row"
              :class="{ clickable: !!ROUTES[item.key] }"
              @click="goTo(item)"
            >
              <span
                class="row-dot"
                :style="{ background: palette[index % palette.length] }"
              ></span>
              <span class="row-name" :title="item.label">{{ item.label }}</span>
              <span class="row-bar" :class="{ flat: !showBar(item) }">
                <i :style="{ width: barPercent(item) + '%', background: palette[index % palette.length] }"></i>
              </span>
              <span class="row-val">{{ formatValue(item) }}</span>
              <span v-if="showPercent(item)" class="row-pct">{{ sharePercent(item).toFixed(1) }}%</span>
            </div>
          </template>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="runtime-body">
        <div class="gauge-grid">
          <div v-for="gauge in overview.runtime.gauges" :key="gauge.key" class="gauge-card">
            <div class="gauge-head">
              <span>{{ gauge.label }}</span>
              <span class="gauge-status" :class="gauge.status">{{ gauge.status === 'up' ? '正常' : '偏高' }}</span>
            </div>
            <div class="gauge-value">
              {{ gauge.unit === 'bytes' ? formatBytes(gauge.used) : formatCount(gauge.used) }}
              <em>/ {{ gauge.unit === 'bytes' ? formatBytes(gauge.total) : formatCount(gauge.total) }}</em>
            </div>
            <div class="gauge-bar">
              <i
                :style="{
                  width: Math.min(100, gauge.total ? (gauge.used / gauge.total) * 100 : 0) + '%',
                  background: gauge.status === 'warn' ? 'var(--adm-warn)' : 'var(--adm-primary)'
                }"
              ></i>
            </div>
          </div>

          <div class="node-grid">
            <div v-for="node in overview.runtime.nodes" :key="node.key" class="node-card">
              <span class="node-dot" :class="nodeTone(node)"></span>
              <div class="node-info">
                <span class="node-name">{{ node.label }}</span>
                <span class="node-msg">{{ node.message }}</span>
              </div>
              <span v-if="node.latencyMs !== null && node.latencyMs > 0" class="node-latency">{{ node.latencyMs }} ms</span>
            </div>
          </div>
        </div>

        <div class="res-rows runtime-rows">
          <template v-for="group in rowGroups" :key="group.key">
            <div class="group-label">{{ group.label }}</div>
            <div v-for="item in group.items" :key="item.key" class="res-row">
              <span class="row-name wide">{{ item.label }}</span>
              <span class="row-val">{{ formatValue(item) }}</span>
            </div>
          </template>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped lang="scss">
.resource-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 380px;
  padding-bottom: 8px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 10px;
  background: var(--adm-surface);
  overflow: hidden;
}

.res-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 13px 18px 6px;
}

.res-title-wrap {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
  flex-wrap: wrap;
}

.res-title {
  position: relative;
  padding-left: 10px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--adm-text);

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    width: 3px;
    height: 12px;
    transform: translateY(-50%);
    border-radius: 2px;
    background: linear-gradient(180deg, var(--adm-primary), transparent);
  }
}

.res-note,
.res-updated {
  font-size: 11px;
  letter-spacing: 0.5px;
  color: var(--adm-text-faint);
  white-space: nowrap;
}

.res-updated {
  margin-left: auto;
  font-variant-numeric: tabular-nums;
}

.res-tabs {
  display: flex;
  gap: 6px;
  padding: 0 18px 8px;
  flex-wrap: wrap;
}

.res-tab {
  padding: 4px 12px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 13px;
  background: transparent;
  color: var(--adm-text-2);
  font-size: 11.5px;
  letter-spacing: 0.5px;
  cursor: pointer;
  transition: all 0.18s;

  &:hover {
    color: var(--adm-primary);
    border-color: var(--adm-primary);
  }

  &.active {
    color: var(--adm-on-primary);
    background: var(--adm-primary);
    border-color: var(--adm-primary);
  }
}

.res-body {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 14px;
  flex: 1;
  min-height: 0;
}

.ring-wrap {
  width: 168px;
  height: 190px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
}

.ring-chart {
  width: 168px;
  height: 190px;
}

.ring-empty {
  font-size: 12px;
  color: var(--adm-text-faint);
}

.res-rows {
  flex: 1;
  min-width: 0;
  max-height: 320px;
  overflow-y: auto;
  padding-right: 2px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  column-gap: 20px;
  align-content: start;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--adm-border-soft);
    border-radius: 2px;
  }
}

.group-label {
  grid-column: 1 / -1;
  margin: 6px 0 2px;
  font-size: 10.5px;
  letter-spacing: 1px;
  color: var(--adm-text-faint);
  text-transform: uppercase;

  &:first-child {
    margin-top: 0;
  }
}

.res-row {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 4px 3px;
  font-size: 12px;
  border-bottom: 1px dashed var(--adm-border-soft);
  border-radius: 3px;

  &:last-child {
    border-bottom: none;
  }

  &.clickable {
    cursor: pointer;

    &:hover {
      background: color-mix(in srgb, var(--adm-primary) 7%, transparent);
    }
  }
}

.row-dot {
  width: 6px;
  height: 6px;
  border-radius: 2px;
  flex-shrink: 0;
}

.row-name {
  width: 96px;
  color: var(--adm-text-2);
  flex-shrink: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &.wide {
    width: 112px;
  }
}

.row-bar {
  flex: 1 1 60px;
  max-width: 220px;
  height: 4px;
  border-radius: 2px;
  background: var(--adm-surface-muted);
  overflow: hidden;
  min-width: 36px;

  &.flat {
    background: transparent;

    i {
      display: none;
    }
  }

  i {
    display: block;
    height: 100%;
    border-radius: 2px;
    opacity: 0.72;
  }
}

.row-val {
  margin-left: auto;
  min-width: 56px;
  text-align: right;
  color: var(--adm-text);
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

.row-pct {
  width: 40px;
  text-align: right;
  color: var(--adm-text-faint);
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

.res-empty {
  flex: 1;
  display: grid;
  place-items: center;
  min-height: 200px;
  font-size: 12px;
  color: var(--adm-text-faint);
}

.domain-tip {
  padding: 0 18px 6px;
  font-size: 11px;
  letter-spacing: 0.3px;
  color: var(--adm-text-faint);
}

.vector-card {
  margin: 0 14px 8px;
  padding: 9px 12px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 8px;
  background: var(--adm-surface-muted);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.vector-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.vector-title {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  font-weight: 600;
  color: var(--adm-text);
}

.vector-led {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--adm-down);

  &.ready {
    background: var(--adm-ok);
  }
}

.vector-badge {
  font-size: 10.5px;
  color: var(--adm-text-faint);
  font-variant-numeric: tabular-nums;
}

.vector-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  font-size: 11px;
  color: var(--adm-text-muted);

  b {
    color: var(--adm-text);
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }
}

.index-files {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px 6px;
}

.index-chip {
  padding: 1px 7px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 9px;
  background: var(--adm-surface);
  font-size: 10px;
  color: var(--adm-text-2);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.corpus-files {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 0;
}

.corpus-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.corpus-label {
  font-size: 10.5px;
  color: var(--adm-text-faint);
  letter-spacing: 0.3px;
}

.corpus-hint {
  flex-shrink: 0;
  font-size: 10px;
  color: var(--adm-text-faint);
  opacity: 0.75;
  white-space: nowrap;
}

.corpus-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
  max-height: 112px;
  overflow-y: auto;
  padding-right: 2px;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--adm-border-soft);
    border-radius: 2px;
  }
}

.corpus-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
  padding: 2px 0;
  border-bottom: 1px dashed var(--adm-border-soft);
  font-size: 10.5px;
}

.corpus-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--adm-text-2);
}

.corpus-meta {
  flex-shrink: 0;
  color: var(--adm-text-faint);
  font-variant-numeric: tabular-nums;
}

.corpus-empty {
  margin: 0;
  font-size: 10.5px;
  color: var(--adm-warn);
}

.runtime-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 16px 4px;
  flex: 1;
}

.gauge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 10px;
}

.gauge-card {
  padding: 9px 11px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 8px;
  background: var(--adm-surface-muted);
}

.gauge-head {
  display: flex;
  justify-content: space-between;
  font-size: 11.5px;
  color: var(--adm-text-2);
}

.gauge-status {
  font-size: 10.5px;
  color: var(--adm-ok);

  &.warn {
    color: var(--adm-warn);
  }
}

.gauge-value {
  margin: 6px 0 5px;
  font-size: 15px;
  font-weight: 600;
  color: var(--adm-text);
  font-variant-numeric: tabular-nums;

  em {
    font-style: normal;
    font-size: 11px;
    font-weight: 400;
    color: var(--adm-text-faint);
  }
}

.gauge-bar {
  height: 4px;
  border-radius: 2px;
  background: var(--adm-surface);
  overflow: hidden;

  i {
    display: block;
    height: 100%;
    border-radius: 2px;
    opacity: 0.75;
  }
}

.node-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.node-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 7px;
}

.node-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;

  &.ok {
    background: var(--adm-ok);
  }

  &.warn {
    background: var(--adm-warn);
  }

  &.down {
    background: var(--adm-down);
  }
}

.node-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.node-name {
  font-size: 11.5px;
  color: var(--adm-text);
}

.node-msg {
  font-size: 10px;
  color: var(--adm-text-faint);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-latency {
  font-size: 10.5px;
  color: var(--adm-text-faint);
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

.runtime-rows {
  max-height: 150px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 14px;
  align-content: start;

  .group-label {
    grid-column: 1 / -1;
  }
}

@media (max-width: 640px) {
  .res-rows {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1200px) {
  .res-body {
    flex-direction: column;
    align-items: stretch;
  }

  .ring-wrap,
  .ring-chart {
    width: 100%;
    max-width: 320px;
    margin: 0 auto;
  }

  .res-rows {
    max-height: 260px;
  }

  .corpus-list {
    grid-template-columns: 1fr;
    max-height: 140px;
  }
}
</style>
