<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import {
  getAdminReports,
  getReportStats,
  handleReport,
  type UserReport
} from '@/api/modules/report'

const router = useRouter()

const loading = ref(false)
const tableData = ref<UserReport[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const stats = ref<Record<string, number>>({})

const searchForm = reactive({
  keyword: '',
  status: '',
  targetType: '',
  aiFlag: null as number | null
})

const statusOptions = [
  { value: 'pending', label: '待处理' },
  { value: 'processing', label: '处理中' },
  { value: 'accepted', label: '已采纳' },
  { value: 'rejected', label: '已驳回' }
]

const targetTypeOptions = [
  { value: 'post', label: '帖子' },
  { value: 'comment', label: '评论' },
  { value: 'poem', label: '诗词' },
  { value: 'user', label: '用户' }
]

const reasonMap: Record<string, string> = {
  spam: '垃圾广告 / 引流',
  abuse: '辱骂攻击',
  porn: '色情低俗',
  plagiarism: '抄袭 / 洗稿',
  illegal: '违法违规',
  ai_auto: 'AI 风控',
  other: '其他违规'
}

const statusMap: Record<string, { text: string; type: string }> = {
  pending: { text: '待处理', type: 'info' },
  processing: { text: '处理中', type: 'warning' },
  accepted: { text: '已采纳', type: 'success' },
  rejected: { text: '已驳回', type: 'danger' }
}

const targetTypeMap: Record<string, string> = {
  post: '帖子',
  comment: '评论',
  poem: '诗词',
  user: '用户'
}

const riskMap: Record<string, { text: string; type: string }> = {
  low: { text: '低风险', type: 'info' },
  medium: { text: '中风险', type: 'warning' },
  high: { text: '高风险', type: 'danger' }
}

const formatTime = (value?: string | null) => (value ? String(value).replace('T', ' ').slice(0, 16) : '-')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminReports({
      page: page.value,
      size: pageSize.value,
      status: searchForm.status || undefined,
      targetType: searchForm.targetType || undefined,
      aiFlag: searchForm.aiFlag,
      keyword: searchForm.keyword || undefined
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('获取举报列表失败')
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res = await getReportStats()
    stats.value = res.data || {}
  } catch {
    stats.value = {}
  }
}

const handleSearch = () => {
  page.value = 1
  fetchData()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = ''
  searchForm.targetType = ''
  searchForm.aiFlag = null
  page.value = 1
  fetchData()
}

const openTarget = (row: UserReport) => {
  switch (row.targetType) {
    case 'post':
      router.push(`/forum/${row.targetId}`)
      break
    case 'poem':
      router.push(`/poem/${row.targetId}`)
      break
    case 'user':
      router.push(`/user/${row.targetId}/homepage`)
      break
    default:
      ElMessage.info(`评论内容：请在对应帖子/诗词下查看（ID ${row.targetId}）`)
  }
}

/** 举报详情弹窗：列表行已包含全部字段，直接展示，不再单独请求 */
const detailVisible = ref(false)
const detailRow = ref<UserReport | null>(null)

const openDetail = (row: UserReport) => {
  detailRow.value = row
  detailVisible.value = true
}

/** 详情里直接进入处理流程，避免来回找行 */
const openHandleFromDetail = () => {
  if (!detailRow.value) return
  detailVisible.value = false
  openHandle(detailRow.value)
}

const dialogVisible = ref(false)
const submitting = ref(false)
const current = ref<UserReport | null>(null)
const handleForm = reactive<{
  status: string
  action: 'none' | 'hide' | 'delete'
  handleNote: string
}>({
  status: 'accepted',
  action: 'none',
  handleNote: ''
})

const actionOptions = [
  { value: 'none', label: '仅记录结论' },
  { value: 'hide', label: '隐藏内容' },
  { value: 'delete', label: '删除内容' }
]

const actionDisabled = () => {
  if (handleForm.status !== 'accepted') return true
  return !current.value || !['post', 'comment'].includes(current.value.targetType)
}

const openHandle = (row: UserReport) => {
  current.value = row
  handleForm.status = row.status === 'pending' ? 'accepted' : row.status
  handleForm.action = 'none'
  handleForm.handleNote = row.handleNote || ''
  dialogVisible.value = true
}

const submitHandle = async () => {
  if (!current.value) return
  if (handleForm.status === 'accepted' && handleForm.action === 'none' && !handleForm.handleNote.trim()) {
    ElMessage.warning('采纳举报时请填写处理说明，或选择隐藏/删除内容')
    return
  }
  submitting.value = true
  try {
    await handleReport(current.value.id, {
      status: handleForm.status,
      action: actionDisabled() ? 'none' : handleForm.action,
      handleNote: handleForm.handleNote.trim() || undefined
    })
    ElMessage.success('处理完成，已通知相关用户')
    dialogVisible.value = false
    fetchData()
    fetchStats()
  } catch {
    ElMessage.error('处理失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchData()
  fetchStats()
})
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">举报管理</h2>
    <p class="page-desc">用户对用户/内容的违规举报。系统报错请前往「报修管理」，AI 自动风控生成的举报单带 AI 标记。</p>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value">{{ stats.total || 0 }}</div>
          <div class="stat-label">举报总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-pending" shadow="hover">
          <div class="stat-value">{{ stats.pending || 0 }}</div>
          <div class="stat-label">待处理</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-processing" shadow="hover">
          <div class="stat-value">{{ stats.aiPending || 0 }}</div>
          <div class="stat-label">AI 待复核</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-resolved" shadow="hover">
          <div class="stat-value">{{ stats.accepted || 0 }}</div>
          <div class="stat-label">已采纳</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="search-card">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索举报说明/AI 判定/处理说明"
            clearable
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
          />
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.status" placeholder="状态" clearable @change="handleSearch">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.targetType" placeholder="对象类型" clearable @change="handleSearch">
            <el-option v-for="item in targetTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.aiFlag" placeholder="来源" clearable @change="handleSearch">
            <el-option :value="0" label="用户举报" />
            <el-option :value="1" label="AI 风控" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column label="举报对象" width="130">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTarget(row)">
              {{ targetTypeMap[row.targetType] || row.targetType }} #{{ row.targetId }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="原因" width="130">
          <template #default="{ row }">
            <span>{{ reasonMap[row.reasonType] || row.reasonType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="举报说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.aiFlag === 1" type="danger" size="small">AI 风控</el-tag>
            <el-tag v-else size="small">用户举报</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="AI 风险" width="100">
          <template #default="{ row }">
            <el-tooltip v-if="row.aiRiskLevel" :content="row.aiReason || '无说明'" placement="top">
              <el-tag :type="riskMap[row.aiRiskLevel]?.type as any" size="small">
                {{ riskMap[row.aiRiskLevel]?.text || row.aiRiskLevel }}
              </el-tag>
            </el-tooltip>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type as any" size="small">
              {{ statusMap[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="举报人" width="100">
          <template #default="{ row }">
            <span>{{ row.reporterId === 0 ? '系统/AI' : `用户 ${row.reporterId}` }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="warning" @click="openHandle(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
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

    <el-dialog v-model="detailVisible" title="举报详情" width="680px" top="6vh">
      <template v-if="detailRow">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="编号">{{ detailRow.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusMap[detailRow.status]?.type as any" size="small">
              {{ statusMap[detailRow.status]?.text || detailRow.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="举报对象">
            <el-button link type="primary" @click="openTarget(detailRow)">
              {{ targetTypeMap[detailRow.targetType] || detailRow.targetType }} #{{ detailRow.targetId }}
            </el-button>
          </el-descriptions-item>
          <el-descriptions-item label="来源">
            <el-tag v-if="detailRow.aiFlag === 1" type="danger" size="small">AI 风控</el-tag>
            <el-tag v-else size="small">用户举报</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="举报人">
            {{ detailRow.reporterId === 0 ? '系统 / AI' : `用户 #${detailRow.reporterId}` }}
          </el-descriptions-item>
          <el-descriptions-item label="举报时间">
            {{ formatTime(detailRow.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="原因类型">
            {{ reasonMap[detailRow.reasonType] || detailRow.reasonType }}
          </el-descriptions-item>
          <el-descriptions-item label="AI 风险">
            <el-tooltip
              v-if="detailRow.aiRiskLevel"
              :content="detailRow.aiReason || '无说明'"
              placement="top"
            >
              <el-tag :type="riskMap[detailRow.aiRiskLevel]?.type as any" size="small">
                {{ riskMap[detailRow.aiRiskLevel]?.text || detailRow.aiRiskLevel }}
              </el-tag>
            </el-tooltip>
            <span v-else>—</span>
          </el-descriptions-item>
          <el-descriptions-item label="举报说明" :span="2">
            <div class="detail-text">{{ detailRow.reason || '未填写' }}</div>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailRow.evidence" label="证据材料" :span="2">
            <div class="detail-text">{{ detailRow.evidence }}</div>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailRow.aiReason" label="AI 判定" :span="2">
            <div class="detail-text">{{ detailRow.aiReason }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="处理人">
            {{ detailRow.handlerId ? `管理员 #${detailRow.handlerId}` : '未处理' }}
          </el-descriptions-item>
          <el-descriptions-item label="处理时间">
            {{ formatTime(detailRow.handledAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="处理说明" :span="2">
            <div class="detail-text">{{ detailRow.handleNote || '—' }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="openHandleFromDetail">去处理</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="处理举报" width="520px">
      <div v-if="current" class="handle-target">
        举报对象：{{ targetTypeMap[current.targetType] || current.targetType }} #{{ current.targetId }}
      </div>
      <el-form label-width="90px">
        <el-form-item label="处理结论" required>
          <el-radio-group v-model="handleForm.status">
            <el-radio value="processing">处理中</el-radio>
            <el-radio value="accepted">举报成立</el-radio>
            <el-radio value="rejected">举报不成立</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容处理">
          <el-select v-model="handleForm.action" :disabled="actionDisabled()" style="width: 100%">
            <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <div v-if="actionDisabled()" class="form-tip">
            仅「举报成立」且对象为帖子/评论时可隐藏或删除内容
          </div>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input
            v-model="handleForm.handleNote"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="会通过站内通知告知举报人与被举报人"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 20px;
}

.page-title {
  margin: 0 0 6px;
  font-size: 22px;
}

.page-desc {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.stat-label {
  margin-top: 4px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.stat-pending .stat-value {
  color: var(--el-color-warning);
}

.stat-processing .stat-value {
  color: var(--el-color-danger);
}

.stat-resolved .stat-value {
  color: var(--el-color-success);
}

.search-card {
  margin-bottom: 16px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.handle-target {
  margin-bottom: 14px;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  font-size: 13px;
}

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.detail-text {
  white-space: pre-wrap;
  line-height: 1.7;
}
</style>
