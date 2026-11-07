<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminPoems, auditPoem, getAuditStats } from '@/api/modules/admin'
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
const keyword = ref('')
const tableRef = ref<any>(null)
const pendingStat = ref(0)
/** 紧凑模式：一屏能多看一倍的行，审积压时用 */
const dense = ref(false)

const statusOptions = [
  { label: '待审核', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已拒绝', value: 2 },
  { label: '全部', value: '' as const }
]

const statusTagMap: Record<number, { type: string; label: string }> = {
  0: { type: 'warning', label: '待审核' },
  1: { type: 'success', label: '已通过' },
  2: { type: 'danger', label: '已拒绝' }
}

const formatTime = (v?: string | null) => (v ? String(v).replace('T', ' ').slice(0, 16) : '-')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminPoems({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      auditStatus: statusFilter.value === '' ? undefined : Number(statusFilter.value)
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
    pendingStat.value = res.data.poems || 0
  } catch {
    pendingStat.value = 0
  }
}

/* ---------------- 批量审核 ---------------- */

const batch = useBatchAudit<any>({
  handler: (id, action, reason) => auditPoem(id, action === 'approve' ? 1 : 2, reason),
  labelOf: (row) => row.title || `#${row.id}`,
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

const pendingInSelection = computed(() => batch.rows.value.filter((r: any) => r.auditStatus === 0).length)

const onBatchAction = async (key: string) => {
  const count = batch.count.value
  if (!count) return
  if (key === 'approve') {
    if (!(await confirmBatch('approve', count))) return
    // 只有待审核的能通过，其余状态自动跳过
    batch.run('approve', undefined, (row: any) => row.auditStatus === 0)
    return
  }
  const reason = await askBatchReason('reject', count)
  if (reason === null) return
  batch.run('reject', reason)
}

/** 把失败的几条重新勾上，方便直接重试 */
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

/* ---------------- 单条审核 ---------------- */

const handleApprove = async (row: any) => {
  try {
    await auditPoem(row.id, 1)
    ElMessage.success('已通过')
    fetchData()
    fetchStats()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const handleReject = async (row: any) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝该诗词', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '拒绝原因（会显示给作者）',
      inputValidator: (v: string) => (v && v.trim().length >= 2 ? true : '请填写至少 2 个字的理由')
    })
    await auditPoem(row.id, 2, value)
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
  <div class="poem-audit-page">
    <div class="page-header">
      <div class="title-row">
        <h2>诗词审核</h2>
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
        <el-input
          v-model="keyword"
          placeholder="搜索标题 / 作者"
          clearable
          style="width: 200px"
          @keyup.enter="onFilterChange"
          @clear="onFilterChange"
        >
          <template #append>
            <el-button @click="onFilterChange">搜索</el-button>
          </template>
        </el-input>
        <el-radio-group v-model="dense" size="small">
          <el-radio-button :value="false">舒适</el-radio-button>
          <el-radio-button :value="true">紧凑</el-radio-button>
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
      :size="dense ? 'small' : 'default'"
      :row-class-name="cursor.rowClassName"
      @selection-change="batch.setSelection"
    >
      <el-table-column type="selection" width="46" reserve-selection />
      <el-table-column type="expand" width="46">
        <template #default="{ row }">
          <div class="poem-preview">
            <div class="poem-text">{{ row.content }}</div>
            <div v-if="row.translation" class="poem-block">
              <span class="block-label">译文</span>
              <p>{{ row.translation }}</p>
            </div>
            <div v-if="row.appreciation" class="poem-block">
              <span class="block-label">赏析</span>
              <p>{{ row.appreciation }}</p>
            </div>
            <div v-if="row.background" class="poem-block">
              <span class="block-label">创作背景</span>
              <p>{{ row.background }}</p>
            </div>
            <div class="poem-meta">
              <span>作者：{{ row.poetName || row.author || '-' }}</span>
              <span>朝代：{{ row.dynastyName || '-' }}</span>
              <span>分类：{{ row.categoryName || '-' }}</span>
              <span>来源：{{ row.source || '-' }}</span>
              <span>原创：{{ row.isOriginal === 1 ? '是' : '否' }}</span>
              <span v-if="row.auditReason">审核备注：{{ row.auditReason }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="poetName" label="作者" width="100" />
      <el-table-column prop="dynastyName" label="朝代" width="80" />
      <el-table-column prop="poemType" label="类型" width="80">
        <template #default="{ row }">
          {{ row.poemType === 'modern' ? '现代诗' : '古体' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTagMap[row.auditStatus]?.type as any" size="small">
            {{ statusTagMap[row.auditStatus]?.label || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="150">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="原创" width="70">
        <template #default="{ row }">
          <el-tag v-if="row.isOriginal === 1" type="success" size="small" effect="plain">原创</el-tag>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <template v-if="row.auditStatus === 0">
            <el-button link type="success" @click="handleApprove(row)">通过</el-button>
            <el-button link type="danger" @click="handleReject(row)">拒绝</el-button>
          </template>
          <template v-else-if="row.auditStatus === 1">
            <el-button link type="warning" @click="handleReject(row)">撤销</el-button>
          </template>
          <template v-else>
            <el-button link type="success" @click="handleApprove(row)">改为通过</el-button>
          </template>
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
.poem-audit-page {
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

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

:deep(.is-cursor) {
  background: color-mix(in srgb, var(--adm-primary) 10%, transparent) !important;
}

.muted {
  color: var(--adm-text-faint);
}

/* 展开行：审核时能直接读正文，不用再跳到前台 */
.poem-preview {
  padding: 4px 12px 8px 48px;
}

.poem-text {
  margin-bottom: 12px;
  padding: 12px 16px;
  border-left: 3px solid var(--adm-border);
  background: var(--adm-surface-muted);
  border-radius: 0 4px 4px 0;
  font-family: var(--font-serif, 'Songti SC', serif);
  font-size: 14px;
  line-height: 1.9;
  color: var(--adm-text);
  white-space: pre-wrap;
}

.poem-block {
  margin-bottom: 10px;

  .block-label {
    display: inline-block;
    margin-bottom: 2px;
    padding: 0 6px;
    border-radius: 3px;
    background: var(--adm-bg);
    font-size: 12px;
    line-height: 18px;
    color: var(--adm-text-muted);
  }

  p {
    margin: 4px 0 0;
    font-size: 13px;
    line-height: 1.8;
    color: var(--adm-text-2);
    white-space: pre-wrap;
  }
}

.poem-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 18px;
  padding-top: 8px;
  border-top: 1px dashed var(--adm-border-soft);
  font-size: 12px;
  color: var(--adm-text-muted);
}
</style>
