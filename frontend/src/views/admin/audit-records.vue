<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditRecords, getAuditReviewers } from '@/api/modules/audit-pipeline'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.userInfo?.role === 'admin')

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const stats = ref<any>({ total: 0, approved: 0, rejected: 0, passRate: 0, avgMinutes: 0 })
const reviewers = ref<any[]>([])

const filters = reactive({
  bizType: '',
  outcome: '',
  reviewerId: null as number | null,
  dateRange: [] as string[],
  keyword: ''
})
const page = ref(1)
const pageSize = ref(10)

const bizTypeOptions = [
  { value: 'poem', label: '诗词' },
  { value: 'comment', label: '评论' },
  { value: 'ai_content', label: 'AI内容' },
  { value: 'poet_profile', label: '诗人认证' },
  { value: 'poet_draft', label: '诗人档案草稿' },
  { value: 'poet_suggestion', label: '诗人内容建议' }
]

const bizTypeLabel = (value: string) =>
  bizTypeOptions.find((o) => o.value === value)?.label || value || '-'

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAuditRecords({
      page: page.value,
      size: pageSize.value,
      bizType: filters.bizType || undefined,
      outcome: filters.outcome || undefined,
      reviewerId: isAdmin.value ? filters.reviewerId || undefined : undefined,
      startDate: filters.dateRange?.[0] || undefined,
      endDate: filters.dateRange?.[1] || undefined,
      keyword: filters.keyword || undefined
    })
    records.value = res.data.records || []
    total.value = res.data.total || 0
    stats.value = res.data.stats || stats.value
  } catch (error: any) {
    ElMessage.error(error?.message || '审核记录加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchData()
}

const handleReset = () => {
  filters.bizType = ''
  filters.outcome = ''
  filters.reviewerId = null
  filters.dateRange = []
  filters.keyword = ''
  handleSearch()
}

const loadReviewers = async () => {
  if (!isAdmin.value) return
  try {
    const res = await getAuditReviewers()
    reviewers.value = res.data || []
  } catch {
    reviewers.value = []
  }
}

onMounted(() => {
  fetchData()
  loadReviewers()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">{{ isAdmin ? '审核记录' : '我的审核记录' }}</h2>
        <p class="page-tip">
          {{ isAdmin ? '查看全组审核结果，可按审核员、类型、日期筛选。' : '仅展示本人已完成的审核记录和绩效统计。' }}
        </p>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <span class="stat-label">已审总数</span>
        <span class="stat-value">{{ stats.total || 0 }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">通过</span>
        <span class="stat-value success">{{ stats.approved || 0 }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">驳回</span>
        <span class="stat-value danger">{{ stats.rejected || 0 }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">通过率</span>
        <span class="stat-value accent">{{ stats.passRate || 0 }}<em>%</em></span>
      </div>
      <div class="stat-card">
        <span class="stat-label">平均耗时</span>
        <span class="stat-value">{{ stats.avgMinutes || 0 }}<em>分</em></span>
      </div>
    </div>

    <el-card class="filter-card">
      <el-row :gutter="12">
        <el-col :span="4">
          <el-select v-model="filters.bizType" placeholder="全部类型" clearable style="width: 100%">
            <el-option v-for="o in bizTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="filters.outcome" placeholder="全部结果" clearable style="width: 100%">
            <el-option label="通过" value="approved" />
            <el-option label="驳回" value="rejected" />
          </el-select>
        </el-col>
        <el-col v-if="isAdmin" :span="4">
          <el-select v-model="filters.reviewerId" placeholder="全部审核员" clearable style="width: 100%">
            <el-option v-for="r in reviewers" :key="r.id" :label="r.displayName" :value="r.id" />
          </el-select>
        </el-col>
        <el-col :span="5">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 100%"
          />
        </el-col>
        <el-col :span="4">
          <el-input v-model="filters.keyword" placeholder="标题/作者关键词" clearable @keyup.enter="handleSearch" />
        </el-col>
        <el-col :span="3" class="filter-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card">
      <el-table :data="records" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="title" label="内容标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ bizTypeLabel(row.bizType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="authorName" label="作者" width="110" show-overflow-tooltip />
        <el-table-column v-if="isAdmin" prop="reviewerName" label="审核员" width="110">
          <template #default="{ row }">{{ row.reviewerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'approved' ? 'success' : 'danger'" size="small">
              {{ row.status === 'approved' ? '通过' : '驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="result" label="审核意见" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.result || '-' }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="80">
          <template #default="{ row }">{{ row.durationMinutes || 0 }} 分</template>
        </el-table-column>
        <el-table-column prop="finishedAt" label="完成时间" width="170" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchData"
          @size-change="handleSearch"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 0;
}

.page-header {
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  color: var(--adm-text);
}

.page-tip {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--adm-text-muted);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.stat-card {
  padding: 14px 16px;
  border: 1px solid var(--adm-border);
  border-radius: 10px;
  background: var(--adm-surface);
}

.stat-label {
  display: block;
  font-size: 12px;
  color: var(--adm-text-muted);
}

.stat-value {
  display: block;
  margin-top: 6px;
  font-size: 24px;
  font-weight: 600;
  color: var(--adm-text);

  em {
    margin-left: 2px;
    font-size: 12px;
    font-style: normal;
    color: var(--adm-text-muted);
  }

  &.accent { color: var(--adm-chart-1); }
  &.success { color: #3f9c5a; }
  &.danger { color: #c05a5a; }
}

.filter-card {
  margin-bottom: 14px;
  border-radius: 8px;
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.table-card {
  border-radius: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
