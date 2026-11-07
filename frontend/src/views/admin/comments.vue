<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminComments, auditComment, deleteAdminComment } from '@/api/modules/admin'
import AuditBatchBar from '@/components/admin/AuditBatchBar.vue'
import type { BatchBarAction } from '@/types/admin-audit'
import { useBatchAudit, askBatchReason, confirmBatch } from '@/composables/useBatchAudit'
import { useAuditCursor } from '@/composables/useAuditCursor'

const loading = ref(false)
const dataList = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const statusFilter = ref<number | ''>('')
const tableRef = ref<any>(null)

const statusOptions = [
  { label: '全部', value: '' as const },
  { label: '正常', value: 1 },
  { label: '已隐藏', value: 0 }
]

const statusTagMap: Record<number, { type: string; label: string }> = {
  1: { type: 'success', label: '正常' },
  0: { type: 'warning', label: '已隐藏' }
}

// 后端约定：1 = 诗词，2 = 帖子（poem/detail.vue、forum/detail.vue 提交时用的就是这个）
const targetTypeMap: Record<number, string> = {
  1: '诗词',
  2: '帖子'
}

const formatTime = (v?: string | null) => (v ? String(v).replace('T', ' ').slice(0, 16) : '-')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminComments({
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

/* ---------------- 批量操作 ---------------- */

const batch = useBatchAudit<any>({
  handler: async (id, action, reason) => {
    if (action === 'delete') return deleteAdminComment(id)
    return auditComment(id, action === 'approve' ? 1 : 0, reason)
  },
  labelOf: (row) => (row.content || '').slice(0, 20),
  actionLabel: { approve: '恢复', reject: '隐藏', delete: '删除' },
  onFinished: () => fetchData()
})

const batchActions: BatchBarAction[] = [
  { key: 'approve', label: '批量恢复', type: 'success' },
  { key: 'reject', label: '批量隐藏', type: 'warning', plain: true },
  { key: 'delete', label: '批量删除', type: 'danger', plain: true }
]

const onBatchAction = async (key: string) => {
  const count = batch.count.value
  if (!count) return
  if (key === 'delete') {
    if (!(await confirmBatch('delete', count))) return
    batch.run('delete')
    return
  }
  if (key === 'approve') {
    batch.run('approve', undefined, (row: any) => row.status !== 1)
    return
  }
  const reason = await askBatchReason('reject', count)
  if (reason === null) return
  batch.run('reject', reason, (row: any) => row.status !== 0)
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

/* ---------------- 单条操作 ---------------- */

const handleApprove = async (row: any) => {
  try {
    await auditComment(row.id, 1)
    ElMessage.success('已恢复显示')
    fetchData()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const handleReject = async (row: any) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入隐藏原因', '隐藏该评论', {
      confirmButtonText: '确认隐藏',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '隐藏原因（内部留档）',
      inputValidator: (v: string) => (v && v.trim().length >= 2 ? true : '请填写至少 2 个字的理由')
    })
    await auditComment(row.id, 0, value)
    ElMessage.success('已隐藏')
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '操作失败')
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认删除该评论？删除后不可恢复。', '删除确认', { type: 'error' })
    await deleteAdminComment(row.id)
    ElMessage.success('删除成功')
    fetchData()
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

onMounted(fetchData)
</script>

<template>
  <div class="comments-page">
    <div class="page-header">
      <div class="title-row">
        <h2>评论管理</h2>
        <el-tag type="info" effect="plain" size="small">共 {{ total }} 条</el-tag>
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
      <el-table-column prop="nickname" label="用户" width="110" />
      <el-table-column label="评论内容" min-width="280">
        <template #default="{ row }">
          <el-tooltip :content="row.content" placement="top" :show-after="400">
            <span class="content-preview">{{ row.content }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="目标" width="80">
        <template #default="{ row }">
          {{ targetTypeMap[row.targetType] || '未知' }}
        </template>
      </el-table-column>
      <el-table-column prop="targetTitle" label="所在内容" width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTagMap[row.status]?.type as any" size="small">
            {{ statusTagMap[row.status]?.label || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="评论时间" width="150">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 1" link type="warning" @click="handleReject(row)">隐藏</el-button>
          <el-button v-else link type="success" @click="handleApprove(row)">恢复</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
.comments-page {
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

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

:deep(.is-cursor) {
  background: color-mix(in srgb, var(--adm-primary) 10%, transparent) !important;
}
</style>
