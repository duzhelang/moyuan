<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getAdminStatsTrend, getSearchMetrics, getVisitTrend } from '@/api/modules/admin'
import type { SearchMetrics } from '@/api/modules/admin'
import UserTrendPanel from '@/components/admin/UserTrendPanel.vue'

const loading = ref(true)
const dates = ref<string[]>([])
const visits = ref<number[]>([])
const uniqueVisitors = ref<number[]>([])
const users = ref<number[]>([])
const posts = ref<number[]>([])
const search = ref<SearchMetrics | null>(null)

const last = (arr: number[]) => (arr.length ? arr[arr.length - 1] : 0)

const cards = computed(() => [
  { key: 'pv', label: '今日 PV', value: last(visits.value) },
  { key: 'uv', label: '今日 UV', value: last(uniqueVisitors.value) },
  {
    key: 'per',
    // PV 实为接口请求数、UV 为 IP 去重，UV 极小时比值没有意义
    label: '人均请求',
    value:
      last(uniqueVisitors.value) < 5
        ? '—'
        : (last(visits.value) / last(uniqueVisitors.value)).toFixed(1),
    unit: last(uniqueVisitors.value) < 5 ? '样本不足' : '次'
  },
  { key: 'new', label: '今日新增用户', value: last(users.value) }
])

/** 搜索指标：total 为 0 时视为还没积累到数据，不要显示 0% */
const searchReady = computed(() => !!search.value?.available && (search.value?.total || 0) > 0)

const searchCards = computed(() => {
  const s = search.value
  if (!searchReady.value || !s) {
    return [
      { key: 'total', label: '搜索总量', value: '—' },
      { key: 'rate', label: '无结果率', value: '—' },
      { key: 'empty', label: '无结果次数', value: '—' },
      { key: 'distinct', label: '去重关键词', value: '—' }
    ]
  }
  return [
    { key: 'total', label: '搜索总量', value: String(s.total) },
    { key: 'rate', label: '无结果率', value: (s.noResultRate * 100).toFixed(1) + '%' },
    { key: 'empty', label: '无结果次数', value: String(s.noResult) },
    { key: 'distinct', label: '去重关键词', value: String(s.distinctKeywords) }
  ]
})

const trendRows = computed(() => search.value?.trend || [])

const trendMax = computed(() =>
  Math.max(1, ...trendRows.value.map((r) => r.total))
)

/** 把每日序列归一化成 SVG 折线坐标，宽度固定 640，高度 120 */
const trendPoints = (key: 'total' | 'noResult') => {
  const rows = trendRows.value
  if (rows.length === 0) return ''
  // 只有一天数据时落在最右端（对应今天），polyline 单点画不出线，由模板补圆心
  if (rows.length === 1) return `620,${(120 - (rows[0][key] / trendMax.value) * 110).toFixed(1)}`
  return rows
    .map((row, i) => {
      const x = (i / (rows.length - 1)) * 640
      const y = 120 - (row[key] / trendMax.value) * 110
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
}

const dateLabel = (d: string) => d.slice(5)

onMounted(async () => {
  try {
    const [trendRes, visitRes, searchRes] = await Promise.all([
      getAdminStatsTrend(),
      getVisitTrend(7),
      getSearchMetrics(7)
    ])
    const trend = trendRes.data
    const visit = visitRes.data
    dates.value = visit?.dates || []
    visits.value = visit?.visits || []
    uniqueVisitors.value = visit?.uniqueVisitors || []
    users.value = trend?.users || []
    posts.value = trend?.posts || []
    search.value = searchRes.data || null
  } catch (error) {
    console.error('获取用户分析数据失败', error)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading" class="analytics-page">
    <div class="page-head">
      <h2 class="page-title">用户分析</h2>
      <p class="page-desc">
        访问、增长与搜索行为的近期走势。搜索数据来自 `search_log` 埋点，无结果率越高说明内容覆盖越不足。
      </p>
    </div>

    <el-row :gutter="20" class="stat-row">
      <el-col v-for="c in cards" :key="c.key" :xs="12" :sm="6">
        <el-card shadow="never" class="stat-card" :body-style="{ padding: '16px 18px' }">
          <p class="stat-label">{{ c.label }}</p>
          <p class="stat-value">
            {{ c.value }}<span v-if="c.unit" class="stat-unit">{{ c.unit }}</span>
          </p>
        </el-card>
      </el-col>
    </el-row>

    <UserTrendPanel
      :dates="dates"
      :visits="visits"
      :unique-visitors="uniqueVisitors"
      :users="users"
      :posts="posts"
    />

    <el-card shadow="never" class="search-card">
      <div class="card-head">
        <span class="card-title">搜索行为 · 近 7 天</span>
        <el-tag v-if="!searchReady" size="small" type="info" effect="plain">暂无埋点数据</el-tag>
      </div>

      <el-row v-if="searchReady" :gutter="20" class="search-metrics">
        <el-col v-for="c in searchCards" :key="c.key" :xs="12" :sm="6">
          <div class="mini">
            <span class="mini-label">{{ c.label }}</span>
            <span class="mini-value">{{ c.value }}</span>
          </div>
        </el-col>
      </el-row>
      <div v-else class="search-empty">
        <span class="empty-badge">埋点已上线</span>
        <p class="empty-text">
          近 7 天还没有搜索记录。用户在诗词页发起搜索后，搜索量、无结果率与关键词榜单会自动出现在这里。
        </p>
      </div>

      <div v-if="trendRows.length" class="trend-wrap">
        <div class="trend-legend">
          <span><i class="dot dot-total" />搜索总量</span>
          <span><i class="dot dot-empty" />其中无结果</span>
          <span class="trend-range">{{ trendRows[0].date }} ~ {{ trendRows[trendRows.length - 1].date }}</span>
        </div>
        <svg class="trend-svg" viewBox="0 0 640 120" preserveAspectRatio="none">
          <line x1="0" y1="119" x2="640" y2="119" stroke="#EBEEF5" stroke-width="1" />
          <polyline
            :points="trendPoints('total')"
            fill="none"
            stroke="#1890ff"
            stroke-width="2"
            stroke-linejoin="round"
            vector-effect="non-scaling-stroke"
          />
          <polyline
            :points="trendPoints('noResult')"
            fill="none"
            stroke="#faad14"
            stroke-width="1.5"
            stroke-dasharray="4 3"
            stroke-linejoin="round"
            vector-effect="non-scaling-stroke"
          />
          <!-- 只有一天数据时 polyline 画不出线，改用端点标记，避免看起来是空白图 -->
          <circle
            v-if="trendRows.length === 1"
            :cx="620"
            :cy="120 - ((trendRows[0].total || 0) / trendMax) * 110"
            r="4"
            fill="#1890ff"
          />
        </svg>
        <div class="trend-axis">
          <span v-for="row in trendRows" :key="row.date">{{ dateLabel(row.date) }}</span>
        </div>
      </div>

      <el-row v-if="searchReady" :gutter="20" class="kw-row">
        <el-col :xs="24" :sm="12">
          <div class="kw-title">
            <span>搜不到的关键词 TOP</span>
            <em v-if="search?.available === false" class="kw-hint">{{ search?.message }}</em>
          </div>
          <el-table
            :data="search?.topNoResult || []"
            size="small"
            :show-header="true"
            empty-text="暂无搜不到的关键词"
            max-height="280"
          >
            <el-table-column prop="keyword" label="关键词" min-width="120" show-overflow-tooltip />
            <el-table-column prop="count" label="次数" width="70" align="right" />
          </el-table>
        </el-col>
        <el-col :xs="24" :sm="12">
          <div class="kw-title"><span>热搜关键词 TOP</span></div>
          <el-table
            :data="search?.topKeywords || []"
            size="small"
            empty-text="暂无搜索记录"
            max-height="280"
          >
            <el-table-column prop="keyword" label="关键词" min-width="120" show-overflow-tooltip />
            <el-table-column prop="count" label="次数" width="70" align="right" />
            <el-table-column prop="avgResult" label="平均命中" width="90" align="right" />
          </el-table>
        </el-col>
      </el-row>

      <p class="search-note">
        无结果率 = 命中数为 0 的搜索次数 ÷ 搜索总次数。匿名搜索同样计入，但不带用户标识。
      </p>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-head {
  margin-bottom: 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--adm-text);
  margin: 0 0 6px;
}

.page-desc {
  font-size: 13px;
  color: var(--adm-text-muted);
  margin: 0;
  max-width: 860px;
  line-height: 1.7;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  margin-bottom: 20px;
}

.stat-label {
  font-size: 13px;
  color: var(--adm-text-muted);
  margin: 0 0 6px;
}

.stat-value {
  font-size: 26px;
  font-weight: 600;
  color: var(--adm-text);
  margin: 0;
  font-variant-numeric: tabular-nums;
}

.stat-unit {
  font-size: 13px;
  font-weight: 400;
  color: var(--adm-text-muted);
  margin-left: 3px;
}

.search-card {
  border-radius: 8px;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--adm-text);
}

.search-metrics {
  margin-bottom: 8px;
}

.mini {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  background: var(--adm-surface-muted);
  border-radius: 6px;
  margin-bottom: 12px;
}

.search-empty {
  padding: 22px 0 18px;
  text-align: center;
  margin-bottom: 8px;
}

.empty-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--adm-chart-1) 12%, transparent);
  color: var(--adm-primary);
  font-size: 11px;
  margin-bottom: 8px;
}

.empty-text {
  margin: 0 auto;
  max-width: 460px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--adm-text-faint);
}

.mini-label {
  font-size: 12px;
  color: var(--adm-text-muted);
}

.mini-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--adm-text);
  font-variant-numeric: tabular-nums;
}

.trend-wrap {
  margin: 8px 0 20px;
}

.trend-legend {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: var(--adm-text-muted);
  margin-bottom: 8px;
}

.dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 5px;
}

.dot-total {
  background: var(--adm-primary);
}

.dot-empty {
  background: var(--adm-warn);
}

.trend-range {
  margin-left: auto;
  font-variant-numeric: tabular-nums;
}

.trend-svg {
  width: 100%;
  height: 120px;
  display: block;
}

.trend-axis {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--adm-text-faint);
  margin-top: 4px;
  font-variant-numeric: tabular-nums;
}

.kw-row {
  margin-top: 4px;
}

.kw-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--adm-text);
  margin-bottom: 8px;
}

.kw-hint {
  font-style: normal;
  font-size: 12px;
  font-weight: 400;
  color: var(--adm-warn);
}

.search-note {
  font-size: 12px;
  color: var(--adm-text-muted);
  margin: 12px 0 0;
  line-height: 1.7;
}
</style>
