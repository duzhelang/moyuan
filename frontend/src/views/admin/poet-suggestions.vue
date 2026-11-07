<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getAdminPoetDrafts, reviewPoetDraft, type PoetDraft } from '@/api/modules/poet-draft'
import { getAuditStats } from '@/api/modules/admin'
import AuditBatchBar from '@/components/admin/AuditBatchBar.vue'
import type { BatchBarAction } from '@/types/admin-audit'
import { useBatchAudit, askBatchReason, confirmBatch } from '@/composables/useBatchAudit'
import { useAuditCursor } from '@/composables/useAuditCursor'

const tab = ref<'drafts' | 'suggestions'>('drafts')
const drafts = ref<PoetDraft[]>([])
const suggestions = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const statusFilter = ref<number | undefined>(undefined)
const loading = ref(false)
const reviewDialogVisible = ref(false)
const reviewTarget = ref<any>(null)
const reviewType = ref<'draft' | 'suggestion'>('draft')
const reviewStatus = ref(1)
const reviewComment = ref('')
const tableRef = ref<any>(null)
const draftStat = ref(0)
const suggestionStat = ref(0)

const sectionLabelMap: Record<string, string> = {
  biography: '简介',
  introduction: '简介',
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

// 草稿用数字状态（0/1/2），建议用字符串状态（pending/approved/rejected），统一成数字再渲染
const suggestionStatusToNum: Record<string, number> = { pending: 0, approved: 1, rejected: 2 }
const statusOf = (row: any): number =>
  tab.value === 'drafts' ? Number(row.status) : (suggestionStatusToNum[row.status] ?? 0)

const formatTime = (v?: string | null) => (v ? String(v).replace('T', ' ').slice(0, 19) : '-')

// 诗人 ID 不好认，换成名字（按需拉取，当前页最多 10 个）
const poetNameMap = ref<Record<number, string>>({})
const ensurePoetNames = async (rows: any[]) => {
  const ids = [...new Set(rows.map((r) => Number(r.poetId)).filter((id) => !!id))]
  await Promise.all(
    ids
      .filter((id) => !poetNameMap.value[id])
      .map(async (id) => {
        try {
          const res = await request.get<any>(`/poets/${id}`)
          if (res.data?.name) poetNameMap.value[id] = res.data.name
        } catch {
          // 诗人不存在时退回显示 ID，不影响列表
        }
      })
  )
}

const fetchDrafts = async () => {
  loading.value = true
  try {
    const res = await getAdminPoetDrafts({ pageNum: page.value, pageSize: pageSize.value, status: statusFilter.value })
    drafts.value = res.data.records
    total.value = res.data.total
    ensurePoetNames(drafts.value)
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
    ensurePoetNames(suggestions.value)
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

const fetchStats = async () => {
  try {
    const res = await getAuditStats()
    draftStat.value = res.data.poetDrafts || 0
    suggestionStat.value = res.data.poetSuggestions || 0
  } catch {
    draftStat.value = 0
    suggestionStat.value = 0
  }
}

/* ---------------- 批量审核 ---------------- */

// 两个 tab 走两套接口：草稿 1/2，建议 approved/rejected
const batch = useBatchAudit<any>({
  handler: async (id, action, reason) => {
    if (tab.value === 'drafts') {
      return reviewPoetDraft(id, action === 'approve' ? 1 : 2, reason)
    }
    const { reviewPoetSuggestion } = await import('@/api/modules/admin')
    return reviewPoetSuggestion(id, action === 'approve' ? 'approved' : 'rejected', reason)
  },
  labelOf: (row) => `${sectionLabelMap[row.section] || row.section || ''} · #${row.poetId}`,
  actionLabel: { approve: '通过', reject: '拒绝', delete: '删除' },
  onFinished: () => {
    fetchData()
    fetchStats()
  }
})

const batchActions: BatchBarAction[] = [
  { key: 'approve', label: '批量通过', type: 'success' },
  { key: 'reject', label: '批量拒绝', type: 'danger', plain: true }
]

const pendingInSelection = computed(() => batch.rows.value.filter((r: any) => statusOf(r) === 0).length)

const onBatchAction = async (key: string) => {
  const count = batch.count.value
  if (!count) return
  if (key === 'approve') {
    if (!(await confirmBatch('approve', count))) return
    batch.run('approve', undefined, (row: any) => statusOf(row) === 0)
    return
  }
  const reason = await askBatchReason('reject', count)
  if (reason === null) return
  batch.run('reject', reason, (row: any) => statusOf(row) === 0)
}

const retryFailed = async () => {
  const ids = batch.failedIds.value
  tableRef.value?.clearSelection()
  batch.clearFailures()
  await nextTick()
  const rows = tab.value === 'drafts' ? drafts.value : suggestions.value
  rows.forEach((row: any) => {
    if (ids.has(row.id)) tableRef.value?.toggleRowSelection(row, true)
  })
  ElMessage.info(`已重新勾选 ${ids.size} 条失败记录`)
}

const cursor = useAuditCursor<any>({
  rows: computed(() => (tab.value === 'drafts' ? drafts.value : suggestions.value)),
  tableRef,
  onApprove: (row) => openReview(row, tab.value === 'drafts' ? 'draft' : 'suggestion'),
  onReject: (row) => openReview(row, tab.value === 'drafts' ? 'draft' : 'suggestion'),
  onToggle: (row) => tableRef.value?.toggleRowSelection(row),
  onClear: () => tableRef.value?.clearSelection(),
  onSelectAll: () => tableRef.value?.toggleAllSelection(),
  enabled: computed(() => !reviewDialogVisible.value)
})

const handleTabChange = () => {
  page.value = 1
  statusFilter.value = undefined
  batch.clearSelection()
  tableRef.value?.clearSelection()
  cursor.reset()
  fetchData()
}

/** 批量执行中不让切 tab，否则 handler 会按新 tab 的接口去审旧 tab 的数据 */
const beforeTabLeave = () => {
  if (batch.running.value) {
    ElMessage.warning('批量审核进行中，请稍候再切换')
    return false
  }
  return true
}

onMounted(() => {
  fetchData()
  fetchStats()
})
</script>

<template>
  <div class="poet-suggestions-page">
    <div class="page-header">
      <h2>诗人内容审核</h2>
      <p class="page-desc">
        审核用户对「诗人详情页」正文的修订：在诗人详情页提交的整段修订进入「草稿审核」，单纯留意见建议的进入「用户建议」；审核通过才会写回诗人主档，拒绝不影响现有内容。
      </p>
    </div>

    <el-tabs v-model="tab" :before-leave="beforeTabLeave" @tab-change="handleTabChange">
      <el-tab-pane name="drafts">
        <template #label>
          草稿审核
          <el-tag v-if="draftStat" type="warning" effect="plain" size="small" class="tab-badge">{{ draftStat }}</el-tag>
        </template>
      </el-tab-pane>
      <el-tab-pane name="suggestions">
        <template #label>
          用户建议
          <el-tag v-if="suggestionStat" type="warning" effect="plain" size="small" class="tab-badge">{{ suggestionStat }}</el-tag>
        </template>
      </el-tab-pane>
    </el-tabs>

    <div class="filter-bar">
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="fetchData" style="width: 140px">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已拒绝" :value="2" />
      </el-select>
    </div>

    <AuditBatchBar
      :count="batch.count.value"
      :pending-count="pendingInSelection"
      :running="batch.running.value"
      :done="batch.done.value"
      :queued="batch.queued.value"
      :percent="batch.percent.value"
      :failed-count="batch.failedCount.value"
      :actions="batchActions"
      :total="(tab === 'drafts' ? drafts : suggestions).length"
      @action="onBatchAction"
      @clear="tableRef?.clearSelection()"
      @select-all="tableRef?.toggleAllSelection()"
      @retry-failed="retryFailed"
    />

    <el-table
      ref="tableRef"
      :data="tab === 'drafts' ? drafts : suggestions"
      v-loading="loading"
      row-key="id"
      stripe
      :row-class-name="cursor.rowClassName"
      @selection-change="batch.setSelection"
    >
      <template #empty>
        <div class="empty-block">
          <p class="empty-title">
            {{ tab === 'drafts' ? '暂无待审核的诗家正文修订' : '暂无用户提交的内容建议' }}
          </p>
          <p class="empty-hint">
            这页不会自动产生数据：用户在诗人详情页点「修订」提交整段改写，就在「草稿审核」里等审；
            点「意见建议」只留一句话，就进「用户建议」。审核通过后才写回诗人的简介 / 人物生平 / 主要影响 / 历史评价 / 轶事典故。
          </p>
        </div>
      </template>
      <el-table-column type="selection" width="46" reserve-selection />
      <el-table-column label="诗人" min-width="130">
        <template #default="{ row }">
          <span class="poet-name">{{ poetNameMap[row.poetId] || '未知诗人' }}</span>
          <span class="poet-id">#{{ row.poetId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="提交人" width="90">
        <template #default="{ row }">
          <span class="poet-id">#{{ tab === 'drafts' ? row.editorId : row.userId }}</span>
        </template>
      </el-table-column>
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
      <el-table-column label="内容摘要" min-width="240">
        <template #default="{ row }">
          <el-tooltip :content="row.content" placement="top" :show-after="400">
            <span class="content-preview">{{ row.content }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="(statusLabelMap[statusOf(row)]?.type as any) || 'info'" size="small">
            {{ statusLabelMap[statusOf(row)]?.text || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="160">
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="审核备注" min-width="150">
        <template #default="{ row }">
          <el-tooltip v-if="row.reviewComment" :content="row.reviewComment" placement="top" :show-after="400">
            <span class="content-preview one-line">{{ row.reviewComment }}</span>
          </el-tooltip>
          <span v-else class="poet-id">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="statusOf(row) === 0" type="primary" size="small" @click="openReview(row, tab === 'drafts' ? 'draft' : 'suggestion')">
            审核
          </el-button>
          <span v-else class="reviewed-text">已审核</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleTabChange"
      />
    </div>

    <el-dialog v-model="reviewDialogVisible" title="审核" width="500px">
      <div v-if="reviewTarget" class="review-detail">
        <p>
          <strong>诗人：</strong>{{ poetNameMap[reviewTarget.poetId] || '未知诗人' }}
          <span class="poet-id">#{{ reviewTarget.poetId }}</span>
        </p>
        <p><strong>板块：</strong>{{ sectionLabelMap[reviewTarget.section] || reviewTarget.section }}</p>
        <div v-if="reviewTarget.originalContent" class="text-block">
          <span class="text-label">原文</span>
          <div class="text-body muted">{{ reviewTarget.originalContent }}</div>
        </div>
        <div class="text-block">
          <span class="text-label">提交内容</span>
          <div class="text-body">{{ reviewTarget.content }}</div>
        </div>
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

.tab-badge {
  margin-left: 6px;
  height: 18px;
  padding: 0 6px;
  font-variant-numeric: tabular-nums;
}

:deep(.is-cursor) {
  background: color-mix(in srgb, var(--adm-primary) 10%, transparent) !important;
}

.page-header {
  margin-bottom: 20px;

  h2 {
    font-size: 20px;
    color: var(--adm-text);
    margin: 0 0 8px;
  }
}

.page-desc {
  margin: 0;
  max-width: 900px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--adm-text-muted);
}

.empty-block {
  padding: 30px 60px 40px;
  text-align: center;
}

.empty-title {
  margin: 0 0 8px;
  font-size: 15px;
  color: var(--adm-text-2);
}

.empty-hint {
  margin: 0 auto;
  max-width: 640px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--adm-text-faint);
}

.filter-bar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
}

.content-preview {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
  color: var(--adm-text-2);
  font-size: 13px;
  line-height: 20px;
  cursor: pointer;
  white-space: normal;

  &.one-line {
    -webkit-line-clamp: 1;
    line-clamp: 1;
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.poet-name {
  color: var(--adm-text);
}

.poet-id {
  margin-left: 4px;
  color: var(--adm-text-faint);
  font-size: 12px;
}

.reviewed-text {
  color: var(--adm-text-muted);
  font-size: 13px;
}

// 审核弹窗里的原文 / 提交内容对照：整段可读，避免硬截断后看不出改了什么
.text-block {
  margin-bottom: 12px;
}

.text-label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--adm-text-muted);
}

.text-body {
  max-height: 150px;
  overflow-y: auto;
  padding: 8px 10px;
  border-radius: 4px;
  background: var(--adm-surface-muted);
  font-size: 13px;
  line-height: 1.7;
  color: var(--adm-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.text-body.muted {
  background: var(--adm-surface-muted);
  color: var(--adm-text-muted);
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
