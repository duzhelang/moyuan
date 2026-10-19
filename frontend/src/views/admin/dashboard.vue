<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getAdminStats, getAdminStatsTrend, getVisitStats, getVisitTrend } from '@/api/modules/admin'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'

use([CanvasRenderer, LineChart, PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const stats = ref<Record<string, any>>({})
const loading = ref(true)
const trendData = ref<{ dates: string[]; poems: number[]; posts: number[]; users: number[] }>({
  dates: [],
  poems: [],
  posts: [],
  users: []
})
const trendLoading = ref(true)

const visitStats = ref({
  todayVisits: 0,
  weekVisits: 0,
  monthVisits: 0,
  totalVisits: 0
})
const visitLoading = ref(true)
const visitTrendData = ref<{ dates: string[]; visits: number[]; uniqueVisitors: number[] }>({
  dates: [],
  visits: [],
  uniqueVisitors: []
})

const statCards = [
  { key: 'userCount', label: '用户总数', tipTitle: '用户总数构成', icon: 'User', color: '#1890ff' },
  { key: 'poemCount', label: '诗词总数', tipTitle: '诗词总数构成', icon: 'Reading', color: '#52c41a' },
  { key: 'categoryCount', label: '分类总数', tipTitle: '分类总数构成', icon: 'FolderOpened', color: '#faad14' },
  // 朝代总量为固定的朝代枚举，无可拆维度，不提供悬浮说明
  { key: 'dynastyCount', label: '朝代总数', tipTitle: '', icon: 'Clock', color: '#722ed1' },
  { key: 'poetCount', label: '诗人总数', tipTitle: '诗人总数构成', icon: 'EditPen', color: '#eb2f96' },
  { key: 'postCount', label: '帖子总数', tipTitle: '帖子总数构成', icon: 'ChatLineSquare', color: '#13c2c2' }
]

const visitCards = [
  { key: 'todayVisits', label: '今日访问', icon: 'View', color: '#1890ff', tip: '今日 00:00 起记录的接口访问次数，按每次请求逐次计数，含游客访问' },
  { key: 'weekVisits', label: '本周访问', icon: 'DataLine', color: '#52c41a', tip: '近 7 天（含今日）累计接口访问次数，非自然周' },
  { key: 'monthVisits', label: '本月访问', icon: 'Calendar', color: '#faad14', tip: '近 30 天（含今日）累计接口访问次数，非自然月' },
  { key: 'totalVisits', label: '总访问量', icon: 'TrendCharts', color: '#722ed1', tip: '自系统记录访问日志以来的累计接口访问次数' }
]

// 各卡片悬浮说明明细（来自后端 /admin/stats 的 details 字段，按 key 对应）
const statDetails = computed<Record<string, any>>(() => stats.value.details || {})

const lineChartOption = computed(() => ({
  title: { text: '近7天增长趋势', left: 'center' },
  tooltip: { trigger: 'axis' },
  legend: { data: ['用户新增', '诗词新增', '帖子新增'], bottom: 0 },
  grid: { left: '3%', right: '4%', bottom: '12%', top: '15%', containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: trendData.value.dates },
  yAxis: { type: 'value' },
  series: [
    { name: '用户新增', type: 'line', smooth: true, data: trendData.value.users, color: '#1890ff' },
    { name: '诗词新增', type: 'line', smooth: true, data: trendData.value.poems, color: '#52c41a' },
    { name: '帖子新增', type: 'line', smooth: true, data: trendData.value.posts, color: '#13c2c2' }
  ]
}))

const pieChartOption = computed(() => {
  const pieData = [
    { name: '用户', value: stats.value.userCount || 0 },
    { name: '诗词', value: stats.value.poemCount || 0 },
    { name: '帖子', value: stats.value.postCount || 0 },
    { name: '分类', value: stats.value.categoryCount || 0 },
    { name: '朝代', value: stats.value.dynastyCount || 0 },
    { name: '诗人', value: stats.value.poetCount || 0 }
  ]
  return {
    title: { text: '资源分布', left: 'center' },
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'horizontal', bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        labelLine: { show: false },
        data: pieData
      }
    ]
  }
})

const visitChartOption = computed(() => ({
  title: { text: '近7天访问趋势', left: 'center' },
  tooltip: { trigger: 'axis' },
  legend: { data: ['访问量', '独立访客'], bottom: 0 },
  grid: { left: '3%', right: '4%', bottom: '12%', top: '15%', containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: visitTrendData.value.dates },
  yAxis: { type: 'value' },
  series: [
    { name: '访问量', type: 'bar', data: visitTrendData.value.visits, color: '#1890ff', barWidth: '40%' },
    { name: '独立访客', type: 'line', smooth: true, data: visitTrendData.value.uniqueVisitors, color: '#52c41a' }
  ]
}))

onMounted(async () => {
  try {
    const res = await getAdminStats()
    stats.value = res.data
  } catch (error) {
    console.error('获取统计数据失败', error)
  } finally {
    loading.value = false
  }

  try {
    const res = await getAdminStatsTrend()
    trendData.value = res.data
  } catch (error) {
    console.error('获取趋势数据失败', error)
  } finally {
    trendLoading.value = false
  }

  try {
    const [visitRes, trendRes] = await Promise.all([
      getVisitStats(),
      getVisitTrend(7)
    ])
    visitStats.value = visitRes.data
    visitTrendData.value = trendRes.data
  } catch (error) {
    console.error('获取访问统计数据失败', error)
  } finally {
    visitLoading.value = false
  }
})
</script>

<template>
  <div class="dashboard">
    <h2 class="page-title">墨里数章，一屏尽览</h2>

    <el-row :gutter="20">
      <el-col v-for="card in statCards" :key="card.key" :xs="12" :sm="8" :md="6" :lg="4">
        <el-tooltip
          :disabled="!statDetails[card.key]"
          placement="bottom"
          effect="light"
          popper-class="poem-stat-popper"
          :show-after="120"
        >
          <template #content>
            <div class="psd">
              <div class="psd-bar"></div>
              <div class="psd-title">{{ card.tipTitle }}</div>
              <div v-for="g in (statDetails[card.key]?.groups || [])" :key="g.title" class="psd-sec">
                <div class="psd-slabel">{{ g.title }}</div>
                <div v-for="r in g.rows" :key="r.name" class="psd-row">
                  <i class="psd-dot" :style="{ background: r.color }"></i>
                  <span class="psd-name">{{ r.name }}</span>
                  <b class="psd-num">{{ r.value }}</b>
                </div>
                <div v-if="g.sub" class="psd-sub">{{ g.sub }}</div>
              </div>
              <div v-if="statDetails[card.key]?.tip" class="psd-tip">{{ statDetails[card.key].tip }}</div>
            </div>
          </template>
          <el-card class="stat-card" shadow="hover" :body-style="{ padding: '20px' }">
            <div class="stat-content">
              <div class="stat-icon" :style="{ backgroundColor: card.color + '20', color: card.color }">
                <el-icon :size="24"><component :is="card.icon" /></el-icon>
              </div>
              <div class="stat-info">
                <p class="stat-label">{{ card.label }}</p>
                <p class="stat-value">{{ loading ? '...' : (stats[card.key] || 0) }}</p>
              </div>
            </div>
          </el-card>
        </el-tooltip>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="visit-row">
      <el-col v-for="card in visitCards" :key="card.key" :xs="12" :sm="6">
        <el-tooltip
          placement="bottom"
          effect="light"
          popper-class="poem-stat-popper"
          :show-after="120"
        >
          <template #content>
            <div class="psd">
              <div class="psd-bar"></div>
              <div class="psd-title">{{ card.label }} · 统计口径</div>
              <div class="psd-tip">{{ card.tip }}</div>
            </div>
          </template>
          <el-card class="stat-card" shadow="hover" :body-style="{ padding: '20px' }">
            <div class="stat-content">
              <div class="stat-icon" :style="{ backgroundColor: card.color + '20', color: card.color }">
                <el-icon :size="24"><component :is="card.icon" /></el-icon>
              </div>
              <div class="stat-info">
                <p class="stat-label">{{ card.label }}</p>
                <p class="stat-value">{{ visitLoading ? '...' : (visitStats[card.key as keyof typeof visitStats] || 0) }}</p>
              </div>
            </div>
          </el-card>
        </el-tooltip>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card v-loading="trendLoading" class="chart-card" shadow="hover">
          <v-chart :option="lineChartOption" style="height: 400px" autoresize />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card v-loading="loading" class="chart-card" shadow="hover">
          <v-chart :option="pieChartOption" style="height: 400px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24">
        <el-card v-loading="visitLoading" class="chart-card" shadow="hover">
          <v-chart :option="visitChartOption" style="height: 400px" autoresize />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  .page-title {
    font-size: 24px;
    margin-bottom: 20px;
    color: #333;
  }

  .stat-card {
    margin-bottom: 20px;
    border-radius: 8px;
  }

  .stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .stat-icon {
    width: 56px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 12px;
  }

  .stat-label {
    font-size: 14px;
    color: #999;
    margin: 0 0 4px;
  }

  .stat-value {
    font-size: 28px;
    font-weight: 600;
    color: #333;
    margin: 0;
  }

  .visit-row {
    margin-top: 8px;
  }

  .chart-row {
    margin-top: 8px;
  }

  .chart-card {
    margin-bottom: 20px;
    border-radius: 8px;
  }
}
</style>

<style lang="scss">
/* 诗词总数悬浮说明面板：popper 会被 teleport 到 body，故使用全局样式 */
.poem-stat-popper.el-popper {
  padding: 0;
  background: linear-gradient(160deg, #fdfaf3 0%, #f6ecda 100%);
  border: 1px solid rgba(150, 105, 50, 0.35);
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(60, 40, 20, 0.25);
  overflow: hidden;
}

.poem-stat-popper.el-popper .el-popper__arrow::before {
  background: #fdfaf3;
  border: 1px solid rgba(150, 105, 50, 0.35);
}

.poem-stat-popper .psd {
  position: relative;
  min-width: 248px;
  padding: 16px 16px 14px;
}

.poem-stat-popper .psd-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #c9a45c, #8a5a2b, #c9a45c);
}

.poem-stat-popper .psd-title {
  font-size: 14px;
  font-weight: 700;
  color: #5a3d24;
  letter-spacing: 1px;
  margin-bottom: 10px;
}

.poem-stat-popper .psd-sec {
  margin-bottom: 10px;
}

.poem-stat-popper .psd-slabel {
  font-size: 12px;
  color: #a08050;
  margin-bottom: 6px;
  padding-left: 8px;
  border-left: 3px solid #c9a45c;
}

.poem-stat-popper .psd-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #6b5236;
  line-height: 1.9;
}

.poem-stat-popper .psd-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.poem-stat-popper .psd-name {
  flex: 1;
}

.poem-stat-popper .psd-num {
  color: #8a5a2b;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.poem-stat-popper .psd-sub {
  font-size: 12px;
  color: #9a8058;
  padding-left: 16px;
  margin-top: 2px;
}

.poem-stat-popper .psd-tip {
  margin-top: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: #7a5a30;
  background: rgba(201, 164, 92, 0.14);
  border: 1px dashed rgba(150, 105, 50, 0.35);
}
</style>
