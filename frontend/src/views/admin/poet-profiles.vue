<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminPoetProfiles, verifyPoetProfile, getAuditStats } from '@/api/modules/admin'
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
const pendingStat = ref(0)

// verifiedStatus：0-未认证，1-已认证，2-认证中，3-认证失败
// 说明：Element Plus 2.6+ 用 value 作为选项值（label 仅作文案，3.0 将不再兼作值），
// 且 value 不接受 null，所以「全部」用空串占位，请求时再转成 undefined。
const statusOptions = [
  { label: '全部', value: '' as const },
  { label: '未认证', value: 0 },
  { label: '认证中', value: 2 },
  { label: '已认证', value: 1 },
  { label: '认证失败', value: 3 },
]

const statusTagMap: Record<number, { type: string; label: string }> = {
  0: { type: 'info', label: '未认证' },
  1: { type: 'success', label: '已认证' },
  2: { type: 'warning', label: '认证中' },
  3: { type: 'danger', label: '认证失败' },
}

// 审核弹窗
const dialogVisible = ref(false)
const submitting = ref(false)
const currentRow = ref<any>(null)
const reviewResult = ref<number>(1)
const reviewReason = ref('')

const formatTime = (v?: string | null) => (v ? String(v).replace('T', ' ').slice(0, 19) : '-')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminPoetProfiles({
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

const onMountedHook = () => {
  fetchData()
  fetchStats()
}

const fetchStats = async () => {
  try {
    const res = await getAuditStats()
    pendingStat.value = res.data.poetProfiles || 0
  } catch {
    pendingStat.value = 0
  }
}

const openReview = (row: any, preset: number = 1) => {
  currentRow.value = row
  reviewResult.value = preset
  reviewReason.value = ''
  dialogVisible.value = true
}

// 列表内直接通过（已认证的行不展示此入口）
const quickApprove = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确认通过「${row.penName || '该申请人'}」的诗人认证？`,
      '认证审核',
      { type: 'warning', confirmButtonText: '确认通过', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await verifyPoetProfile(row.id, 1)
    ElMessage.success('已通过认证')
    await fetchData()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const submitReview = async () => {
  if (!currentRow.value) return
  if (reviewResult.value === 3 && !reviewReason.value.trim()) {
    ElMessage.warning('拒绝或撤销时请填写备注说明原因')
    return
  }
  submitting.value = true
  try {
    await verifyPoetProfile(currentRow.value.id, reviewResult.value, reviewReason.value || undefined)
    ElMessage.success(reviewResult.value === 1 ? '已通过认证' : '已拒绝认证')
    dialogVisible.value = false
    await fetchData()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

/* ---------------- 批量审核 ---------------- */

const batch = useBatchAudit<any>({
  handler: (id, action, reason) => verifyPoetProfile(id, action === 'approve' ? 1 : 3, reason),
  labelOf: (row) => row.penName || `#${row.id}`,
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

const pendingInSelection = computed(
  () => batch.rows.value.filter((r: any) => r.verifiedStatus !== 1).length
)

const onBatchAction = async (key: string) => {
  const count = batch.count.value
  if (!count) return
  if (key === 'approve') {
    if (!(await confirmBatch('approve', count))) return
    batch.run('approve', undefined, (row: any) => row.verifiedStatus !== 1)
    return
  }
  const reason = await askBatchReason('reject', count)
  if (reason === null) return
  batch.run('reject', reason, (row: any) => row.verifiedStatus !== 3)
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

const cursor = useAuditCursor<any>({
  rows: computed(() => dataList.value),
  tableRef,
  // A 键走「快速通过」，R 键走「填理由拒绝」的弹窗
  onApprove: (row) => quickApprove(row),
  onReject: (row) => openReview(row, 3),
  onToggle: (row) => tableRef.value?.toggleRowSelection(row),
  onClear: () => tableRef.value?.clearSelection(),
  onSelectAll: () => tableRef.value?.toggleAllSelection(),
  enabled: computed(() => !dialogVisible.value)
})

const onFilterChange = () => {
  page.value = 1
  tableRef.value?.clearSelection()
  cursor.reset()
  fetchData()
}

onMounted(onMountedHook)
</script>

<template>
  <div class="poet-profiles-page">
    <div class="page-header">
      <div class="title-row">
        <h2>诗人认证审核</h2>
        <el-tag v-if="pendingStat" type="warning" effect="plain" size="small">待审 {{ pendingStat }}</el-tag>
      </div>
      <div class="filter-bar">
        <el-radio-group v-model="statusFilter" @change="onFilterChange">
          <el-radio-button v-for="opt in statusOptions" :key="opt.label" :value="opt.value">
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
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column prop="penName" label="笔名" min-width="120" show-overflow-tooltip />
      <el-table-column prop="realName" label="真实姓名" min-width="110">
        <template #default="{ row }">
          <span>{{ row.realName || '未填写' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="specialty" label="擅长领域" min-width="140" show-overflow-tooltip />
      <el-table-column label="简介" min-width="260">
        <template #default="{ row }">
          <el-tooltip v-if="row.introduction" :content="row.introduction" placement="top" :show-after="400">
            <span class="intro-text">{{ row.introduction }}</span>
          </el-tooltip>
          <span v-else class="empty-text">未填写</span>
        </template>
      </el-table-column>
      <el-table-column label="认证状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagMap[row.verifiedStatus]?.type as any" size="small">
            {{ statusTagMap[row.verifiedStatus]?.label || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="160">
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="认证时间" width="160">
        <template #default="{ row }">
          {{ formatTime(row.verifiedTime) }}
        </template>
      </el-table-column>
      <el-table-column label="审核备注" min-width="160">
        <template #default="{ row }">
          <el-tooltip v-if="row.verifiedReason" :content="row.verifiedReason" placement="top" :show-after="500">
            <span class="ellipsis-text">{{ row.verifiedReason }}</span>
          </el-tooltip>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.verifiedStatus !== 1" link type="success" @click="quickApprove(row)">通过</el-button>
          <el-button v-if="row.verifiedStatus === 1" link type="danger" @click="openReview(row, 3)">撤销认证</el-button>
          <el-button v-else link type="danger" @click="openReview(row, 3)">拒绝</el-button>
          <el-button link type="primary" @click="openReview(row)">详情</el-button>
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

    <el-dialog v-model="dialogVisible" :title="reviewResult === 3 ? '拒绝 / 撤销认证' : '诗人认证审核'" width="520px">
      <el-form label-width="90px" v-if="currentRow">
        <el-form-item label="笔名">
          <span>{{ currentRow.penName || '-' }}</span>
        </el-form-item>
        <el-form-item label="真实姓名">
          <span>{{ currentRow.realName || '-' }}</span>
        </el-form-item>
        <el-form-item label="审核结果">
          <el-radio-group v-model="reviewResult">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="3">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="reviewReason"
            type="textarea"
            :rows="3"
            :placeholder="reviewResult === 3 ? '请填写拒绝 / 撤销原因（必填）' : '可填写备注（选填）'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReview">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.poet-profiles-page {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
  h2 {
    margin: 0;
    font-size: 20px;
  }
}
.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
:deep(.is-cursor) {
  background: color-mix(in srgb, var(--adm-primary) 10%, transparent) !important;
}
.pagination-bar {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.ellipsis-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

// 简介保留两行，超出才吸断，比单行吸断更好读
.intro-text {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
  line-height: 20px;
  color: var(--el-text-color-regular);
  white-space: normal;
}

.empty-text {
  color: var(--el-text-color-placeholder);
}
</style>
