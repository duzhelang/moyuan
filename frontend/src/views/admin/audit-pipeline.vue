<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addReviewer,
  assignPendingTickets,
  claimTicket,
  deleteAuditRule,
  deleteReviewer,
  getAuditBoard,
  getAuditRules,
  getAuditReviewers,
  getReviewerCandidates,
  returnTicket,
  saveAuditRule,
  scanAuditTickets,
  transferTicket,
  updateReviewer
} from '@/api/modules/audit-pipeline'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

/** 管理员 = 调度台（分派/监督/可协审）；审核员 = 个人工作台（只处理自己的工单） */
const isAdmin = computed(() => userStore.userInfo?.role === 'admin')

const loading = ref(false)
const acting = ref(false)
const board = ref<any>({ stats: {}, columns: {}, reviewers: [], currentReviewerId: null })
const autoRefresh = ref(true)
let timer: number | null = null

const stats = computed(() => board.value.stats || {})
const columns = computed(() => board.value.columns || {})
const reviewers = computed<any[]>(() => board.value.reviewers || [])

const adminColumnMeta = [
  { key: 'pending', title: '待分派', hint: '未命中规则，可指派或协审' },
  { key: 'others', title: '审核员处理中', hint: '全组在办，可协审接手、改派或退回' },
  { key: 'mine', title: '我的协审', hint: '管理员参与审核的工单' },
  { key: 'done', title: '今日完成', hint: '全组最近归档' }
]

const reviewerColumnMeta = [
  { key: 'pending', title: '待认领', hint: '尚未分派，可主动认领' },
  { key: 'mine', title: '我的待审', hint: '已分派给我' },
  { key: 'done', title: '今日完成', hint: '我最近归档' }
]

const boardColumns = computed(() => (isAdmin.value ? adminColumnMeta : reviewerColumnMeta))

const toggleAutoRefresh = (enabled: string | number | boolean) => {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
  if (Boolean(enabled)) {
    timer = window.setInterval(() => fetchBoard(true), 30000)
  }
}

const bizMeta: Record<string, { label: string; color: string }> = {
  poem: { label: '诗词', color: 'var(--adm-chart-1)' },
  comment: { label: '评论', color: 'var(--adm-chart-2)' },
  ai_content: { label: 'AI 内容', color: 'var(--adm-chart-3)' },
  poet_profile: { label: '诗人认证', color: 'var(--adm-chart-4)' },
  poet_draft: { label: '正文修订', color: 'var(--adm-chart-5)' },
  poet_suggestion: { label: '内容建议', color: 'var(--adm-chart-6)' }
}

const contentLabel = (row: any) => {
  if (row.contentType === 'poem_ancient') return '古体'
  if (row.contentType === 'poem_modern') return '现代诗'
  return ''
}

const formatDuration = (ms: number) => {
  const minutes = Math.floor(ms / 60000)
  if (minutes < 60) return `${minutes}m`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h${minutes % 60 ? ' ' + (minutes % 60) + 'm' : ''}`
  return `${Math.floor(hours / 24)}d`
}

/** 剩余时间 / 超时；已归档不显示 */
const slaText = (row: any) => {
  if (!row.deadlineAt || row.status === 'approved' || row.status === 'rejected') return ''
  const left = new Date(row.deadlineAt).getTime() - Date.now()
  if (left < 0) return `超时 ${formatDuration(-left)}`
  return `剩 ${formatDuration(left)}`
}

const fetchBoard = async (silent = false) => {
  if (!silent) loading.value = true
  try {
    const res = await getAuditBoard()
    board.value = res.data
  } catch (e: any) {
    if (!silent) ElMessage.error(e?.message || '看板加载失败')
  } finally {
    loading.value = false
  }
}

/* ---------------- 建单与分派 ---------------- */

const handleScan = async () => {
  acting.value = true
  try {
    const res = await scanAuditTickets()
    const created = res.data?.createdTotal ?? 0
    const assigned = res.data?.assigned ?? 0
    ElMessage.success(`新建工单 ${created} 条，分派 ${assigned} 条`)
    await fetchBoard()
  } catch (e: any) {
    ElMessage.error(e?.message || '扫描失败')
  } finally {
    acting.value = false
  }
}

const handleAssignPending = async () => {
  acting.value = true
  try {
    const res = await assignPendingTickets()
    ElMessage.success(`已分派 ${res.data ?? 0} 条`)
    await fetchBoard()
  } catch (e: any) {
    ElMessage.error(e?.message || '分派失败')
  } finally {
    acting.value = false
  }
}

/* ---------------- 工单流转 ---------------- */

const handleClaim = async (row: any) => {
  try {
    await claimTicket(row.id)
    ElMessage.success(isAdmin.value ? '已协审认领' : '已认领')
    await fetchBoard()
  } catch (e: any) {
    ElMessage.error(e?.message || '认领失败')
  }
}

const handleReturn = async (row: any) => {
  try {
    await ElMessageBox.confirm('退回后工单回到待分派池，等待重新分配。', '退回工单', {
      type: 'warning',
      confirmButtonText: '退回',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await returnTicket(row.id)
    ElMessage.success('已退回待分派')
    await fetchBoard()
  } catch (e: any) {
    ElMessage.error(e?.message || '退回失败')
  }
}

const transferVisible = ref(false)
const transferTarget = ref<any>(null)
const transferTo = ref<number | null>(null)
const transferTitle = computed(() => (isAdmin.value ? '指派 / 改派工单' : '转交工单'))

const openTransfer = (row: any) => {
  transferTarget.value = row
  transferTo.value = null
  transferVisible.value = true
}

const submitTransfer = async () => {
  if (!transferTarget.value || !transferTo.value) {
    ElMessage.warning('请选择接收人')
    return
  }
  try {
    await transferTicket(transferTarget.value.id, transferTo.value)
    ElMessage.success('已转交')
    transferVisible.value = false
    await fetchBoard()
  } catch (e: any) {
    ElMessage.error(e?.message || '转交失败')
  }
}

/** 去对应的审核页处理（弱管控：审核仍在原页面完成，结果自动回写工单） */
const routeOf: Record<string, string> = {
  poem: '/admin/poem-audit',
  comment: '/admin/comments',
  ai_content: '/admin/ai-contents',
  poet_profile: '/admin/poet-profiles',
  poet_draft: '/admin/poet-suggestions',
  poet_suggestion: '/admin/poet-suggestions'
}

const goReview = (row: any) => {
  const path = routeOf[row.bizType]
  if (path) router.push(path)
}

/* ---------------- 规则配置 ---------------- */

const rulesVisible = ref(false)
const rules = ref<any[]>([])
const rulesLoading = ref(false)

const scopeOptions = [
  { label: '按作者', value: 'author', priority: 10 },
  { label: '按朝代', value: 'dynasty', priority: 20 },
  { label: '按内容类型', value: 'content_type', priority: 30 },
  { label: '全局兜底', value: 'global', priority: 90 }
]

const contentTypeOptions = [
  { label: '古体诗', value: 'poem_ancient' },
  { label: '现代诗', value: 'poem_modern' },
  { label: '评论', value: 'comment' },
  { label: 'AI 内容', value: 'ai_content' },
  { label: '诗人认证', value: 'poet_profile' },
  { label: '正文修订', value: 'poet_draft' },
  { label: '内容建议', value: 'poet_suggestion' }
]

const scopeLabel = (scopeType: string) =>
  scopeOptions.find((s) => s.value === scopeType)?.label || scopeType

const ruleForm = reactive({
  id: undefined as number | undefined,
  scopeType: 'content_type',
  scopeValue: '',
  reviewerId: undefined as number | undefined,
  priority: 30,
  remark: ''
})

const resetRuleForm = () => {
  ruleForm.id = undefined
  ruleForm.scopeType = 'content_type'
  ruleForm.scopeValue = ''
  ruleForm.reviewerId = undefined
  ruleForm.priority = 30
  ruleForm.remark = ''
}

const loadRules = async () => {
  rulesLoading.value = true
  try {
    const [ruleRes, reviewerRes] = await Promise.all([getAuditRules(), getAuditReviewers()])
    rules.value = ruleRes.data || []
    if (reviewerRes.data?.length) {
      board.value.reviewers = reviewerRes.data
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '规则加载失败')
  } finally {
    rulesLoading.value = false
  }
}

const openRules = async () => {
  rulesVisible.value = true
  resetRuleForm()
  await loadRules()
}

const onScopeChange = () => {
  const opt = scopeOptions.find((s) => s.value === ruleForm.scopeType)
  ruleForm.priority = opt?.priority ?? 100
  ruleForm.scopeValue = ''
  if (ruleForm.scopeType === 'global') ruleForm.scopeValue = ''
}

const editRule = (row: any) => {
  ruleForm.id = row.id
  ruleForm.scopeType = row.scopeType
  ruleForm.scopeValue = row.scopeValue || ''
  ruleForm.reviewerId = row.reviewerId
  ruleForm.priority = row.priority
  ruleForm.remark = row.remark || ''
}

const submitRule = async () => {
  if (!ruleForm.reviewerId) {
    ElMessage.warning('请选择审核员')
    return
  }
  if (ruleForm.scopeType !== 'global' && !String(ruleForm.scopeValue || '').trim()) {
    ElMessage.warning('请填写匹配值')
    return
  }
  try {
    await saveAuditRule({
      id: ruleForm.id,
      scopeType: ruleForm.scopeType,
      scopeValue: ruleForm.scopeType === 'global' ? null : String(ruleForm.scopeValue).trim(),
      reviewerId: ruleForm.reviewerId,
      priority: ruleForm.priority,
      enabled: 1,
      remark: ruleForm.remark || undefined
    })
    ElMessage.success(ruleForm.id ? '规则已更新' : '规则已新增')
    resetRuleForm()
    await loadRules()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  }
}

const removeRule = async (row: any) => {
  try {
    await ElMessageBox.confirm(`删除规则「${scopeLabel(row.scopeType)} ${row.scopeValue || ''}」？`, '删除规则', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteAuditRule(row.id)
    ElMessage.success('已删除')
    await loadRules()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

/* ---------------- 审核员团队（仅管理员） ---------------- */

const reviewersVisible = ref(false)
const reviewerTeamLoading = ref(false)
const candidates = ref<any[]>([])
const reviewerFormVisible = ref(false)
const specialtyOptions = contentTypeOptions
const specialtyLabel = (value: string) =>
  specialtyOptions.find((o) => o.value === value)?.label || value
const reviewerForm = reactive({
  id: null as number | null,
  userId: null as number | null,
  displayName: '',
  specialties: [] as string[],
  maxConcurrent: 30,
  status: 1
})

const resetReviewerForm = () => {
  reviewerForm.id = null
  reviewerForm.userId = null
  reviewerForm.displayName = ''
  reviewerForm.specialties = []
  reviewerForm.maxConcurrent = 30
  reviewerForm.status = 1
}

const loadReviewerTeam = async () => {
  reviewerTeamLoading.value = true
  try {
    const [listRes, candidateRes] = await Promise.all([getAuditReviewers(), getReviewerCandidates()])
    board.value.reviewers = listRes.data || []
    candidates.value = candidateRes.data || []
  } catch (e: any) {
    ElMessage.error(e?.message || '审核员列表加载失败')
  } finally {
    reviewerTeamLoading.value = false
  }
}

const openReviewerTeam = async () => {
  reviewersVisible.value = true
  await loadReviewerTeam()
}

const openAddReviewer = () => {
  resetReviewerForm()
  reviewerFormVisible.value = true
}

const openEditReviewer = (row: any) => {
  reviewerForm.id = row.id
  reviewerForm.userId = row.userId
  reviewerForm.displayName = row.displayName || ''
  reviewerForm.specialties = (row.specialties || '').split(',').filter(Boolean)
  reviewerForm.maxConcurrent = row.maxConcurrent || 30
  reviewerForm.status = row.status ?? 1
  reviewerFormVisible.value = true
}

const submitReviewer = async () => {
  if (!reviewerForm.id && !reviewerForm.userId) {
    ElMessage.warning('请选择审核员账号')
    return
  }
  const payload = {
    displayName: reviewerForm.displayName || undefined,
    specialties: reviewerForm.specialties.join(',') || undefined,
    maxConcurrent: reviewerForm.maxConcurrent,
    status: reviewerForm.status
  }
  try {
    if (reviewerForm.id) {
      await updateReviewer(reviewerForm.id, payload)
      ElMessage.success('审核员已更新')
    } else {
      await addReviewer({ userId: reviewerForm.userId as number, ...payload })
      ElMessage.success('审核员已登记')
    }
    reviewerFormVisible.value = false
    await loadReviewerTeam()
    await fetchBoard()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  }
}

const toggleReviewer = async (row: any) => {
  try {
    await updateReviewer(row.id, { status: row.status === 1 ? 0 : 1 })
    ElMessage.success(row.status === 1 ? '已停用' : '已启用')
    await loadReviewerTeam()
    await fetchBoard()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const removeReviewer = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `删除审核员「${row.displayName}」？其名下分派规则也会一并清理。`,
      '删除审核员',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteReviewer(row.id)
    ElMessage.success('已删除')
    await loadReviewerTeam()
    await fetchBoard()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

/* ---------------- 生命周期 ---------------- */

onMounted(async () => {
  await fetchBoard()
  if (autoRefresh.value) {
    timer = window.setInterval(() => fetchBoard(true), 30000)
  }
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <div class="pipeline-page" v-loading="loading">
    <div class="page-header">
      <div class="title-row">
        <h2>{{ isAdmin ? '审核流水线 · 调度台' : '我的审核工作台' }}</h2>
        <el-tag v-if="stats.overdue" type="danger" effect="plain" size="small">
          {{ stats.overdue }} 单超时
        </el-tag>
        <el-tag v-else-if="stats.total" type="success" effect="plain" size="small">无超时</el-tag>
      </div>
      <div class="actions">
        <el-checkbox v-model="autoRefresh" size="small" @change="toggleAutoRefresh">
          30s 自动刷新
        </el-checkbox>
        <template v-if="isAdmin">
          <el-button size="small" :loading="acting" @click="handleScan">扫描建单</el-button>
          <el-button size="small" type="primary" :loading="acting" @click="handleAssignPending">
            一键分派
          </el-button>
          <el-button size="small" @click="openRules">规则配置</el-button>
          <el-button size="small" @click="openReviewerTeam">审核员管理</el-button>
        </template>
        <el-button v-else size="small" :loading="loading" @click="fetchBoard()">刷新</el-button>
      </div>
    </div>

    <div class="role-note">
      <template v-if="isAdmin">
        <strong>管理员 · 调度台</strong>：审核员的上级，负责建单、分派、改派、规则配置与审核员团队管理；可协审任意未归档工单，但不进入自动派单池。
      </template>
      <template v-else>
        <strong>审核员 · 工作台</strong>：只处理分派给你或你主动认领的工单；在对应审核页提交结果后，工单状态会自动回写。
      </template>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <span class="stat-label">{{ isAdmin ? '待分派' : '待认领' }}</span>
        <span class="stat-value">{{ stats.pending || 0 }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">{{ isAdmin ? '在办工单' : '我的在办' }}</span>
        <span class="stat-value accent">{{ stats.active || 0 }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">今日完成</span>
        <span class="stat-value success">{{ stats.doneToday || 0 }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">平均处理</span>
        <span class="stat-value">{{ stats.avgMinutes || 0 }}<em>分</em></span>
      </div>
    </div>

    <div class="board" :class="{ 'board-reviewer': !isAdmin }">
      <div v-for="col in boardColumns" :key="col.key" class="column">
        <div class="column-head">
          <span class="column-title" :class="{ mine: col.key === 'mine' }">{{ col.title }}</span>
          <span class="column-count">{{ (columns[col.key] || []).length }}</span>
        </div>
        <div class="column-hint">{{ col.hint }}</div>

        <div class="cards">
          <div
            v-for="row in columns[col.key] || []"
            :key="row.id"
            class="ticket"
            :class="{ overdue: row.overdue, mine: col.key === 'mine' }"
          >
            <div class="ticket-tags">
              <span
                class="tag"
                :style="{
                  background: `color-mix(in srgb, ${bizMeta[row.bizType]?.color || 'var(--adm-text-muted)'} 10%, transparent)`,
                  color: bizMeta[row.bizType]?.color || 'var(--adm-text-muted)'
                }"
              >
                {{ bizMeta[row.bizType]?.label || row.bizType }}
              </span>
              <span v-if="contentLabel(row)" class="tag ghost">{{ contentLabel(row) }}</span>
              <span v-if="slaText(row)" class="tag" :class="row.overdue ? 'danger' : 'warn'">
                {{ slaText(row) }}
              </span>
              <span v-if="row.priority >= 1" class="tag danger">加急</span>
            </div>

            <div class="ticket-title">{{ row.title || '（无标题）' }}</div>
            <div class="ticket-meta">
              <span v-if="row.authorName">{{ row.authorName }}</span>
              <span v-if="row.dynastyName">· {{ row.dynastyName }}</span>
              <span v-if="row.reviewerName" class="assignee">→ {{ row.reviewerName }}</span>
            </div>
            <div v-if="row.assignNote && col.key !== 'done'" class="ticket-note">{{ row.assignNote }}</div>
            <div v-if="col.key === 'done'" class="ticket-note">
              {{ row.status === 'approved' ? '已通过' : '已拒绝' }}
              <template v-if="row.result"> · {{ row.result }}</template>
            </div>

            <div v-if="col.key !== 'done'" class="ticket-actions">
              <template v-if="isAdmin">
                <el-button v-if="col.key === 'pending'" link type="primary" size="small" @click="openTransfer(row)">
                  指派
                </el-button>
                <el-button v-if="col.key === 'pending' || col.key === 'others'" link size="small" @click="handleClaim(row)">协审</el-button>
                <el-button v-if="col.key === 'mine'" link type="primary" size="small" @click="goReview(row)">
                  去审核
                </el-button>
                <el-button v-if="col.key === 'mine' || col.key === 'others'" link size="small" @click="openTransfer(row)">
                  改派
                </el-button>
                <el-button v-if="col.key === 'mine' || col.key === 'others'" link type="danger" size="small" @click="handleReturn(row)">
                  退回
                </el-button>
              </template>
              <template v-else>
                <el-button v-if="col.key === 'pending'" link type="primary" size="small" @click="handleClaim(row)">
                  认领
                </el-button>
                <el-button v-if="col.key === 'mine'" link type="primary" size="small" @click="goReview(row)">
                  去审核
                </el-button>
                <el-button v-if="col.key === 'mine'" link size="small" @click="openTransfer(row)">转交</el-button>
                <el-button v-if="col.key === 'mine'" link type="danger" size="small" @click="handleReturn(row)">
                  退回
                </el-button>
              </template>
            </div>
          </div>

          <div v-if="!(columns[col.key] || []).length" class="empty">
            {{ col.key === 'done' ? '今天还没有完成记录' : '暂无工单' }}
          </div>
        </div>
      </div>
    </div>

    <div v-if="isAdmin" class="load-panel">
      <div class="panel-head">
        <span>审核员负载</span>
        <span class="panel-note">在手 / 上限 · 超时单会标红</span>
      </div>
      <div class="load-list">
        <div v-for="r in reviewers" :key="r.id" class="load-item">
          <div class="load-top">
            <span class="load-name">{{ r.displayName }}</span>
            <span class="load-num" :class="{ danger: r.overdue > 0 }">
              {{ r.onHand }}<em>/{{ r.maxConcurrent }}</em>
            </span>
          </div>
          <div class="load-track">
            <div
              class="load-fill"
              :class="{ danger: r.overdue > 0, warn: r.onHand / r.maxConcurrent > 0.7 }"
              :style="{ width: Math.min(100, Math.round((r.onHand / Math.max(1, r.maxConcurrent)) * 100)) + '%' }"
            ></div>
          </div>
          <div class="load-meta">
            <span>今日完成 {{ r.doneToday }}</span>
            <span v-if="r.overdue" class="danger">超时 {{ r.overdue }}</span>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="transferVisible" :title="transferTitle" width="460px">
      <div v-if="transferTarget" class="transfer-body">
        <p class="transfer-title">{{ transferTarget.title }}</p>
        <el-select v-model="transferTo" placeholder="选择接收人" style="width: 100%">
          <el-option
            v-for="r in reviewers.filter((x) => x.status === 1)"
            :key="r.id"
            :label="`${r.displayName}（在手 ${r.onHand}/${r.maxConcurrent}）`"
            :value="r.id"
          />
        </el-select>
      </div>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTransfer">
          {{ isAdmin ? '确认指派' : '确认转交' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rulesVisible" title="分派规则" width="900px">
      <div class="rules-tip">
        按优先级从小到大匹配：作者 → 朝代 → 内容类型 → 全局兜底。命中且未超上限才分派，
        都未命中时按「擅长该类型 + 在手最少」自动选人。管理员为调度角色，不参与自动派单。
      </div>

      <el-table :data="rules" v-loading="rulesLoading" size="small" stripe>
        <el-table-column label="范围" width="110">
          <template #default="{ row }">{{ scopeLabel(row.scopeType) }}</template>
        </el-table-column>
        <el-table-column label="匹配值" min-width="140">
          <template #default="{ row }">
            <span v-if="row.scopeType === 'global'" class="muted">—</span>
            <span v-else>{{ row.scopeValue }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reviewerName" label="分派给" width="120" />
        <el-table-column prop="priority" label="优先级" width="80" />
        <el-table-column prop="remark" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="editRule(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="removeRule(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider content-position="left">{{ ruleForm.id ? '编辑规则' : '新增规则' }}</el-divider>

      <el-form :inline="true" class="rule-form">
        <el-form-item label="范围">
          <el-select v-model="ruleForm.scopeType" style="width: 130px" @change="onScopeChange">
            <el-option v-for="s in scopeOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="ruleForm.scopeType === 'content_type'" label="类型">
          <el-select v-model="ruleForm.scopeValue" style="width: 130px" placeholder="选择类型">
            <el-option v-for="c in contentTypeOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>

        <el-form-item v-else-if="ruleForm.scopeType !== 'global'" label="匹配值">
          <el-input
            v-model="ruleForm.scopeValue"
            style="width: 160px"
            :placeholder="ruleForm.scopeType === 'author' ? '作者 id，如 79' : '朝代名，如 唐'"
          />
        </el-form-item>

        <el-form-item label="分派给">
          <el-select v-model="ruleForm.reviewerId" style="width: 150px" placeholder="选择审核员">
            <el-option
              v-for="r in reviewers.filter((x) => x.status === 1)"
              :key="r.id"
              :label="r.displayName"
              :value="r.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="优先级">
          <el-input-number v-model="ruleForm.priority" :min="1" :max="999" size="small" />
        </el-form-item>

        <el-form-item label="说明">
          <el-input v-model="ruleForm.remark" style="width: 180px" placeholder="选填" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="small" @click="submitRule">
            {{ ruleForm.id ? '保存修改' : '新增' }}
          </el-button>
          <el-button v-if="ruleForm.id" size="small" @click="resetRuleForm">取消编辑</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <el-dialog v-model="reviewersVisible" title="审核员管理" width="900px">
      <div class="rules-tip">
        管理员是审核员的上级：在此登记审核员、设置工位名、擅长类型与并发上限，并控制启停。
        只有「在岗 + 未超上限 + 角色为审核员」的账号会进入自动派单池；管理员协审不占自动派单名额。
      </div>
      <div class="team-toolbar">
        <span class="muted">共 {{ reviewers.length }} 名审核员</span>
        <el-button type="primary" size="small" @click="openAddReviewer">登记审核员</el-button>
      </div>
      <el-table :data="reviewers" v-loading="reviewerTeamLoading" size="small" stripe>
        <el-table-column label="工位名" min-width="130">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditReviewer(row)">{{ row.displayName }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column label="擅长类型" min-width="220">
          <template #default="{ row }">
            <span v-if="!row.specialties" class="muted">全部</span>
            <el-tag v-for="s in String(row.specialties || '').split(',').filter(Boolean)" :key="s" size="small" effect="plain" class="specialty-tag">
              {{ specialtyLabel(s) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="在手" width="90">
          <template #default="{ row }">
            <span :class="{ danger: row.overdue > 0 }">{{ row.onHand }}/{{ row.maxConcurrent }}</span>
          </template>
        </el-table-column>
        <el-table-column label="今日完成" width="90">
          <template #default="{ row }">{{ row.doneToday }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="plain">
              {{ row.status === 1 ? '在岗' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditReviewer(row)">编辑</el-button>
            <el-button link size="small" @click="toggleReviewer(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" size="small" @click="removeReviewer(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog
      v-model="reviewerFormVisible"
      :title="reviewerForm.id ? '编辑审核员' : '登记审核员'"
      width="520px"
      append-to-body
    >
      <el-form label-width="90px">
        <el-form-item v-if="!reviewerForm.id" label="账号">
          <el-select v-model="reviewerForm.userId" placeholder="选择角色为审核员的账号" style="width: 100%">
            <el-option
              v-for="c in candidates"
              :key="c.userId"
              :label="`${c.nickname || c.username}（${c.username}）`"
              :value="c.userId"
            />
          </el-select>
          <div class="form-hint">没有候选账号？请先到「用户管理」将用户角色设为审核员。</div>
        </el-form-item>
        <el-form-item label="工位名">
          <el-input v-model="reviewerForm.displayName" placeholder="展示在分派列表中的名字，留空取昵称" />
        </el-form-item>
        <el-form-item label="擅长类型">
          <el-select v-model="reviewerForm.specialties" multiple placeholder="不选表示全部类型" style="width: 100%">
            <el-option v-for="o in specialtyOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="并发上限">
          <el-input-number v-model="reviewerForm.maxConcurrent" :min="1" :max="999" />
          <span class="form-hint inline">超过上限后不再自动分派</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="reviewerForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="在岗"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewerFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewerTeamLoading" @click="submitReviewer">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.pipeline-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  padding: 12px 14px;
  background: var(--adm-surface-muted);
  border-radius: 6px;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: var(--adm-text-muted);
}

.stat-value {
  font-size: 22px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;

  em {
    margin-left: 2px;
    font-size: 12px;
    font-style: normal;
    font-weight: 400;
    color: var(--adm-text-muted);
  }

  &.accent {
    color: var(--adm-primary);
  }

  &.success {
    color: var(--adm-ok);
  }
}

.board {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  align-items: start;
}

.column {
  background: var(--adm-surface-muted);
  border-radius: 6px;
  padding: 10px;
}

.column-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.column-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--adm-text-2);

  &.mine {
    color: var(--adm-primary);
  }
}

.column-count {
  font-size: 12px;
  color: var(--adm-text-muted);
}

.column-hint {
  margin: 2px 0 8px;
  font-size: 11px;
  color: var(--adm-text-faint);
}

.cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ticket {
  padding: 10px;
  background: var(--adm-surface);
  border: 1px solid var(--adm-border-soft);
  border-left: 2px solid var(--adm-border);
  border-radius: 4px;

  &.mine {
    border-left-color: var(--adm-primary);
  }

  &.overdue {
    border-left-color: var(--adm-down);
  }
}

.ticket-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}

.tag {
  padding: 0 5px;
  border-radius: 2px;
  font-size: 11px;
  line-height: 17px;

  &.ghost {
    background: var(--adm-surface-muted);
    color: var(--adm-text-muted);
  }

  &.warn {
    background: color-mix(in srgb, var(--adm-warn) 12%, transparent);
    color: var(--adm-warn);
  }

  &.danger {
    background: color-mix(in srgb, var(--adm-down) 12%, transparent);
    color: var(--adm-down);
  }
}

.ticket-title {
  margin-bottom: 3px;
  font-size: 13px;
  font-weight: 500;
  color: var(--adm-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-meta {
  display: flex;
  gap: 3px;
  font-size: 11px;
  color: var(--adm-text-muted);
}

.assignee {
  margin-left: auto;
  color: var(--adm-text-2);
}

.ticket-note {
  margin-top: 4px;
  font-size: 11px;
  color: var(--adm-text-faint);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-actions {
  display: flex;
  gap: 8px;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed var(--adm-border-soft);
}

.empty {
  padding: 18px 0;
  text-align: center;
  font-size: 12px;
  color: var(--adm-text-faint);
}

.load-panel {
  margin-top: 16px;
  padding: 14px 16px;
  background: var(--adm-surface);
  border: 1px solid var(--adm-border-soft);
  border-radius: 6px;
}

.panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 500;
}

.panel-note {
  font-size: 11px;
  font-weight: 400;
  color: var(--adm-text-faint);
}

.load-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px 20px;
}

.load-top {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--adm-text-2);
}

.load-num {
  font-variant-numeric: tabular-nums;

  em {
    font-style: normal;
    color: var(--adm-text-faint);
  }

  &.danger {
    color: var(--adm-down);
  }
}

.load-track {
  height: 4px;
  margin: 5px 0 4px;
  background: var(--adm-bg);
  border-radius: 2px;
  overflow: hidden;
}

.load-fill {
  height: 100%;
  background: var(--adm-primary);
  transition: width 0.2s;

  &.warn {
    background: var(--adm-warn);
  }

  &.danger {
    background: var(--adm-down);
  }
}

.load-meta {
  display: flex;
  gap: 10px;
  font-size: 11px;
  color: var(--adm-text-muted);

  .danger {
    color: var(--adm-down);
  }
}

.transfer-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.transfer-title {
  margin: 0;
  font-size: 13px;
  color: var(--adm-text);
}

.rules-tip {
  margin-bottom: 12px;
  padding: 8px 12px;
  background: var(--adm-surface-muted);
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--adm-text-muted);
}

.rule-form {
  margin-top: 4px;
}

.muted {
  color: var(--adm-text-faint);
}

.role-note {
  margin-bottom: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid var(--adm-border);
  background: color-mix(in srgb, var(--adm-chart-1) 7%, var(--adm-surface));
  color: var(--adm-text-muted);
  font-size: 13px;
  line-height: 1.6;

  strong {
    color: var(--adm-text);
  }
}

.board-reviewer {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.team-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.form-hint {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--adm-text-faint);

  &.inline {
    margin: 0 0 0 8px;
  }
}

.specialty-tag {
  margin: 0 4px 2px 0;
}

@media (max-width: 1400px) {
  .board,
  .board-reviewer {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
