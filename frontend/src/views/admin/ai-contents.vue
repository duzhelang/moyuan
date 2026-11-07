<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminAiContents, approveAiContent, rejectAiContent, getAuditStats } from '@/api/modules/admin'
import AuditBatchBar from '@/components/admin/AuditBatchBar.vue'
import type { BatchBarAction } from '@/types/admin-audit'
import { useBatchAudit, askBatchReason, confirmBatch } from '@/composables/useBatchAudit'
import { useAuditCursor } from '@/composables/useAuditCursor'

const loading = ref(false)
const dataList = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const statusFilter = ref<number | ''>(0)
const tableRef = ref<any>(null)
const pendingStat = ref(0)

const statusOptions = [
  { label: '待审核', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已拒绝', value: 2 },
  { label: '全部', value: '' as const }
]

const fieldNameMap: Record<string, string> = {
  translation: '译文',
  appreciation: '赏析',
  background: '创作背景',
  biography: '生平简介',
  life_story: '人物生平',
  influence: '主要影响',
  evaluation: '历史评价',
  anecdotes: '轶事典故'
}

const targetTypeMap: Record<string, string> = {
  poem: '诗词',
  poet: '诗人'
}

const statusTagMap: Record<number, { type: string; label: string }> = {
  0: { type: 'warning', label: '待审核' },
  1: { type: 'success', label: '已通过' },
  2: { type: 'danger', label: '已拒绝' }
}

const formatTime = (v?: string | null) => (v ? String(v).replace('T', ' ').slice(0, 16) : '-')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminAiContents({
      page: page.value,
      size: size.value,
      status: statusFilter.value === '' ? undefined : Number(statusFilter.value)
    })
    dataList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res = await getAuditStats()
    pendingStat.value = res.data.aiContents || 0
  } catch {
    pendingStat.value = 0
  }
}

/* ---------------- 批量审核 ---------------- */

const batch = useBatchAudit<any>({
  handler: (id, action, reason) =>
    action === 'approve' ? approveAiContent(id) : rejectAiContent(id, reason),
  labelOf: (row) => `${targetTypeMap[row.targetType] || ''} · ${row.targetName || ''}`,
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

const pendingInSelection = computed(() => batch.rows.value.filter((r: any) => r.status === 0).length)

const onBatchAction = async (key: string) => {
  const count = batch.count.value
  if (!count) return
  if (key === 'approve') {
    if (!(await confirmBatch('approve', count))) return
    batch.run('approve', undefined, (row: any) => row.status === 0)
    return
  }
  const reason = await askBatchReason('reject', count)
  if (reason === null) return
  batch.run('reject', reason, (row: any) => row.status === 0)
}

const retryFailed = async () => {
  const ids = batch.failedIds.value
  tableRef.value?.clearSelection()
  batch.clearFailures()
  await nextTick()
  dataList.value.forEach((row) => {
    if (ids.has(row.id)) tableRef.value?.toggleRowSelection(row, true)
  })
  ElMessage.info(`已重新勾选 ${ids.size} 条失败记录`)
}

/* ---------------- 单条 ---------------- */

const handleApprove = async (row: any) => {
  try {
    await approveAiContent(row.id)
    ElMessage.success('已通过，内容已写入正式数据')
    fetchData()
    fetchStats()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const handleReject = async (row: any) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝该内容', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '拒绝原因',
      inputValidator: (v: string) => (v && v.trim().length >= 2 ? true : '请填写至少 2 个字的理由')
    })
    await rejectAiContent(row.id, value)
    ElMessage.success('已拒绝')
    fetchData()
    fetchStats()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '操作失败')
  }
}

/* ---------------- 键盘游标 ---------------- */

const cursor = useAuditCursor<any>({
  rows: computed(() => dataList.value),
  tableRef,
  onApprove: (row) => handleApprove(row),
  onReject: (row) => handleReject(row),
  onToggle: (row) => tableRef.value?.toggleRowSelection(row),
  onClear: () => tableRef.value?.clearSelection(),
  onSelectAll: () => tableRef.value?.toggleAllSelection()
})

const onFilterChange = () => {
  page.value = 1
  tableRef.value?.clearSelection()
  cursor.reset()
  fetchData()
}

onMounted(() => {
  fetchData()
  fetchStats()
})
</script>

<template>
  <div class="ai-contents-page">
    <div class="page-header">
      <div class="title-row">
        <h2>AI 内容审核</h2>
        <el-tag v-if="pendingStat" type="warning" effect="plain" size="small">
          待审 {{ pendingStat }}
        </el-tag>
      </div>
      <div class="filter-bar">
        <el-radio-group v-model="statusFilter" @change="onFilterChange">
          <el-radio-button v-for="opt in statusOptions" :key="String(opt.value)" :value="opt.value">
            {{ opt.label }}
          </el-radio-button>
        </el-radio-group>
      </div>
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
      :total="dataList.length"
      @action="onBatchAction"
      @clear="tableRef?.clearSelection()"
      @select-all="tableRef?.toggleAllSelection()"
      @retry-failed="retryFailed"
    />

    <el-table
      ref="tableRef"
      :data="dataList"
      v-loading="loading"
      row-key="id"
      stripe
      :row-class-name="cursor.rowClassName"
      @selection-change="batch.setSelection"
    >
      <el-table-column type="selection" width="46" reserve-selection />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="目标" width="80">
        <template #default="{ row }">
          {{ targetTypeMap[row.targetType] || row.targetType }}
        </template>
      </el-table-column>
      <el-table-column prop="targetName" label="目标名称" min-width="120" show-overflow-tooltip />
      <el-table-column label="字段" width="100">
        <template #default="{ row }">
          {{ fieldNameMap[row.fieldName] || row.fieldName }}
        </template>
      </el-table-column>
      <el-table-column label="AI 生成内容" min-width="320">
        <template #default="{ row }">
          <el-tooltip :content="row.content" placement="top" :show-after="400">
            <span class="content-preview">{{ row.content }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTagMap[row.status]?.type as any" size="small">
            {{ statusTagMap[row.status]?.label || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="150">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button link type="success" @click="handleApprove(row)">通过</el-button>
            <el-button link type="danger" @click="handleReject(row)">拒绝</el-button>
          </template>
          <span v-else class="muted">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="fetchData"
        @size-change="onFilterChange"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.ai-contents-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;

  h2 {
    margin: 0;
    font-size: 20px;
  }
}

.filter-bar {
  display: flex;
  align-items: center;
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
}

.muted {
  color: var(--adm-text-faint);
  font-size: 13px;
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

:deep(.is-cursor) {
  background: color-mix(in srgb, var(--adm-primary) 10%, transparent) !important;
}
</style>
