<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminPoetDrafts, reviewPoetDraft, type PoetDraft } from '@/api/modules/poet-draft'

const tab = ref<'drafts' | 'suggestions'>('drafts')
const drafts = ref<PoetDraft[]>([])
const suggestions = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const statusFilter = ref<number | undefined>(undefined)
const loading = ref(false)
const reviewDialogVisible = ref(false)
const reviewTarget = ref<any>(null)
const reviewType = ref<'draft' | 'suggestion'>('draft')
const reviewStatus = ref(1)
const reviewComment = ref('')

const sectionLabelMap: Record<string, string> = {
  biography: '简介',
  life_story: '人物生平',
  influence: '主要影响',
  evaluation: '历史评价',
  anecdotes: '轶事典故',
  other: '其他'
}

const statusLabelMap: Record<number, { text: string; type: string }> = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已拒绝', type: 'danger' }
}

const fetchDrafts = async () => {
  loading.value = true
  try {
    const res = await getAdminPoetDrafts({ pageNum: page.value, pageSize: pageSize.value, status: statusFilter.value })
    drafts.value = res.data.records
    total.value = res.data.total
  } catch {
    ElMessage.error('获取草稿列表失败')
  } finally {
    loading.value = false
  }
}

const fetchSuggestions = async () => {
  loading.value = true
  try {
    const { getAdminPoetSuggestions } = await import('@/api/modules/admin')
    const statusMap: Record<number, string> = { 0: 'pending', 1: 'approved', 2: 'rejected' }
    const res = await getAdminPoetSuggestions({
      pageNum: page.value,
      pageSize: pageSize.value,
      status: statusFilter.value !== undefined ? statusMap[statusFilter.value] : undefined
    })
    suggestions.value = res.data.records
    total.value = res.data.total
  } catch {
    ElMessage.error('获取建议列表失败')
  } finally {
    loading.value = false
  }
}

const fetchData = () => {
  if (tab.value === 'drafts') fetchDrafts()
  else fetchSuggestions()
}

const openReview = (item: any, type: 'draft' | 'suggestion') => {
  reviewTarget.value = item
  reviewType.value = type
  reviewStatus.value = 1
  reviewComment.value = ''
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  try {
    if (reviewType.value === 'draft') {
      await reviewPoetDraft(reviewTarget.value.id, reviewStatus.value, reviewComment.value || undefined)
    } else {
      const { reviewPoetSuggestion } = await import('@/api/modules/admin')
      await reviewPoetSuggestion(reviewTarget.value.id, reviewStatus.value === 1 ? 'approved' : 'rejected', reviewComment.value || undefined)
    }
    ElMessage.success('审核完成')
    reviewDialogVisible.value = false
    fetchData()
  } catch {
    ElMessage.error('审核失败')
  }
}

const handlePageChange = (p: number) => {
  page.value = p
  fetchData()
}

const handleTabChange = () => {
  page.value = 1
  statusFilter.value = undefined
  fetchData()
}

onMounted(fetchData)
</script>

<template>
  <div class="poet-suggestions-page">
    <div class="page-header">
      <h2>诗人内容审核</h2>
    </div>

    <el-tabs v-model="tab" @tab-change="handleTabChange">
      <el-tab-pane label="草稿审核" name="drafts" />
      <el-tab-pane label="用户建议" name="suggestions" />
    </el-tabs>

    <div class="filter-bar">
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="fetchData" style="width: 140px">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已拒绝" :value="2" />
      </el-select>
    </div>

    <el-table :data="tab === 'drafts' ? drafts : suggestions" v-loading="loading" stripe>
      <el-table-column prop="poetId" label="诗人ID" width="80" />
      <el-table-column label="板块" width="120">
        <template #default="{ row }">
          {{ sectionLabelMap[row.section] || row.section }}
        </template>
      </el-table-column>
      <el-table-column v-if="tab === 'suggestions'" prop="category" label="分类" width="100">
        <template #default="{ row }">
          {{ sectionLabelMap[row.category] || row.category }}
        </template>
      </el-table-column>
      <el-table-column label="内容摘要" min-width="200">
        <template #default="{ row }">
          <el-tooltip :content="row.content" placement="top" :show-after="500">
            <span class="content-preview">{{ (row.content || '').substring(0, 80) }}{{ (row.content || '').length > 80 ? '...' : '' }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="(statusLabelMap[row.status]?.type as any) || 'info'" size="small">
            {{ statusLabelMap[row.status]?.text || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" width="180" />
      <el-table-column v-if="tab === 'suggestions'" prop="reviewComment" label="审核备注" width="150" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" type="primary" size="small" @click="openReview(row, tab === 'drafts' ? 'draft' : 'suggestion')">
            审核
          </el-button>
          <span v-else class="reviewed-text">已审核</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog v-model="reviewDialogVisible" title="审核" width="500px">
      <div v-if="reviewTarget" class="review-detail">
        <p><strong>诗人ID：</strong>{{ reviewTarget.poetId }}</p>
        <p><strong>板块：</strong>{{ sectionLabelMap[reviewTarget.section] || reviewTarget.section }}</p>
        <p v-if="reviewTarget.originalContent"><strong>原始内容：</strong>{{ reviewTarget.originalContent?.substring(0, 200) }}...</p>
        <p><strong>修改内容：</strong>{{ reviewTarget.content?.substring(0, 200) }}...</p>
      </div>
      <el-divider />
      <el-form label-width="80px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="reviewStatus">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="reviewComment" type="textarea" :rows="3" placeholder="可选填审核备注..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">确认审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.poet-suggestions-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;

  h2 {
    font-size: 20px;
    color: #333;
  }
}

.filter-bar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
}

.content-preview {
  color: #666;
  font-size: 13px;
  cursor: pointer;
}

.reviewed-text {
  color: #999;
  font-size: 13px;
}

.review-detail {
  p {
    margin-bottom: 8px;
    line-height: 1.6;
    font-size: 14px;
  }
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
