<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getAdminStats, getAdminStatsTrend } from '@/api/modules/admin'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'

use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const stats = ref<Record<string, number>>({})
const loading = ref(true)
const trendData = ref<{ dates: string[]; poems: number[]; posts: number[]; users: number[] }>({
  dates: [],
  poems: [],
  posts: [],
  users: []
})
const trendLoading = ref(true)

const statCards = [
  { key: 'userCount', label: '用户总数', icon: 'User', color: '#1890ff' },
  { key: 'poemCount', label: '诗词总数', icon: 'Reading', color: '#52c41a' },
  { key: 'categoryCount', label: '分类总数', icon: 'FolderOpened', color: '#faad14' },
  { key: 'dynastyCount', label: '朝代总数', icon: 'Clock', color: '#722ed1' },
  { key: 'poetCount', label: '诗人总数', icon: 'EditPen', color: '#eb2f96' },
  { key: 'postCount', label: '帖子总数', icon: 'ChatLineSquare', color: '#13c2c2' }
]

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
})
</script>

<template>
  <div class="dashboard">
    <h2 class="page-title">控制台</h2>

    <el-row :gutter="20">
      <el-col v-for="card in statCards" :key="card.key" :xs="12" :sm="8" :md="6" :lg="4">
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

  .chart-row {
    margin-top: 8px;
  }

  .chart-card {
    margin-bottom: 20px;
    border-radius: 8px;
  }
}
</style>
