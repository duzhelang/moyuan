<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getAdminRepairs, getAdminRepairDetail, updateRepairStatus, assignRepairOrder, addRepairInternalComment, getRepairStats, getAdminRepairComments } from '@/api/modules/admin'

const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const stats = ref<Record<string, number>>({})

const searchForm = reactive({
  keyword: '',
  status: null as number | null,
  category: '',
  priority: null as number | null
})

const statusOptions = [
  { value: null, label: '全部' },
  { value: 0, label: '待处理' },
  { value: 1, label: '处理中' },
  { value: 2, label: '已解决' },
  { value: 3, label: '已关闭' },
  { value: 4, label: '已驳回' }
]

const categoryOptions = [
  { value: '', label: '全部' },
  { value: 'system', label: '系统故障' },
  { value: 'function', label: '功能异常' },
  { value: 'ui', label: '界面问题' },
  { value: 'data', label: '数据问题' },
  { value: 'other', label: '其他' }
]

const priorityOptions = [
  { value: null, label: '全部' },
  { value: 1, label: '低' },
  { value: 2, label: '中' },
  { value: 3, label: '高' },
  { value: 4, label: '紧急' }
]

const categoryMap: Record<string, string> = {
  system: '系统故障',
  function: '功能异常',
  ui: '界面问题',
  data: '数据问题',
  other: '其他'
}

const priorityMap: Record<number, { text: string; type: string }> = {
  1: { text: '低', type: 'info' },
  2: { text: '中', type: '' },
  3: { text: '高', type: 'warning' },
  4: { text: '紧急', type: 'danger' }
}

const statusMap: Record<number, { text: string; type: string }> = {
  0: { text: '待处理', type: 'info' },
  1: { text: '处理中', type: 'warning' },
  2: { text: '已解决', type: 'success' },
  3: { text: '已关闭', type: 'danger' },
  4: { text: '已驳回', type: 'danger' }
}

const detailDialogVisible = ref(false)
const currentDetail = ref<any>(null)
const detailComments = ref<any[]>([])
const detailLoading = ref(false)

const statusUpdateDialogVisible = ref(false)
const statusUpdateForm = reactive({
  status: 0,
  resolveContent: ''
})

const assignDialogVisible = ref(false)
const assignForm = reactive({
  assigneeId: null as number | null
})

const internalCommentDialogVisible = ref(false)
const internalCommentForm = reactive({
  content: '',
  images: ''
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminRepairs({
      page: page.value,
      size: pageSize.value,
      status: searchForm.status,
      category: searchForm.category || undefined,
      priority: searchForm.priority,
      keyword: searchForm.keyword || undefined
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取报修列表失败', error)
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res = await getRepairStats()
    stats.value = res.data
  } catch (error) {
    console.error('获取统计数据失败', error)
  }
}

const handleSearch = () => {
  page.value = 1
  fetchData()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = null
  searchForm.category = ''
  searchForm.priority = null
  page.value = 1
  fetchData()
}

const handlePageChange = (newPage: number) => {
  page.value = newPage
  fetchData()
}

const handleSizeChange = (newSize: number) => {
  pageSize.value = newSize
  page.value = 1
  fetchData()
}

const handleViewDetail = async (row: any) => {
  detailLoading.value = true
  detailDialogVisible.value = true
  try {
    const [detailRes, commentsRes] = await Promise.all([
      getAdminRepairDetail(row.id),
      getAdminRepairComments(row.id)
    ])
    currentDetail.value = detailRes.data
    detailComments.value = commentsRes.data || []
  } catch (error) {
    console.error('获取详情失败', error)
  } finally {
    detailLoading.value = false
  }
}

const handleOpenStatusUpdate = (row: any) => {
  currentDetail.value = row
  statusUpdateForm.status = row.status
  statusUpdateForm.resolveContent = row.resolveContent || ''
  statusUpdateDialogVisible.value = true
}

const handleStatusUpdate = async () => {
  try {
    await updateRepairStatus(currentDetail.value.id, {
      status: statusUpdateForm.status,
      resolveContent: statusUpdateForm.resolveContent || undefined
    })
    ElMessage.success('状态更新成功')
    statusUpdateDialogVisible.value = false
    fetchData()
    fetchStats()
  } catch (error) {
    console.error('更新状态失败', error)
  }
}

const handleOpenAssign = (row: any) => {
  currentDetail.value = row
  assignForm.assigneeId = row.assigneeId || null
  assignDialogVisible.value = true
}

const handleAssign = async () => {
  if (!assignForm.assigneeId) {
    ElMessage.warning('请输入处理人ID')
    return
  }
  try {
    await assignRepairOrder(currentDetail.value.id, assignForm.assigneeId)
    ElMessage.success('分配成功')
    assignDialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('分配失败', error)
  }
}

const handleOpenInternalComment = (row: any) => {
  currentDetail.value = row
  internalCommentForm.content = ''
  internalCommentForm.images = ''
  internalCommentDialogVisible.value = true
}

const handleInternalComment = async () => {
  if (!internalCommentForm.content.trim()) {
    ElMessage.warning('请输入备注内容')
    return
  }
  try {
    await addRepairInternalComment(currentDetail.value.id, {
      content: internalCommentForm.content,
      images: internalCommentForm.images || undefined
    })
    ElMessage.success('备注添加成功')
    internalCommentDialogVisible.value = false
    if (detailDialogVisible.value) {
      handleViewDetail(currentDetail.value)
    }
  } catch (error) {
    console.error('添加备注失败', error)
  }
}

onMounted(() => {
  fetchData()
  fetchStats()
})
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">报修管理</h2>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value">{{ stats.total || 0 }}</div>
          <div class="stat-label">总工单数</div>
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
          <div class="stat-value">{{ stats.processing || 0 }}</div>
          <div class="stat-label">处理中</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-resolved" shadow="hover">
          <div class="stat-value">{{ stats.resolved || 0 }}</div>
          <div class="stat-label">已解决</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="search-card">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索标题/描述"
            clearable
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
          />
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.status" placeholder="状态" clearable @change="handleSearch">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.category" placeholder="类别" clearable @change="handleSearch">
            <el-option
              v-for="item in categoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.priority" placeholder="优先级" clearable @change="handleSearch">
            <el-option
              v-for="item in priorityOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
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
        <el-table-column prop="id" label="工单编号" width="100" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="username" label="提交人" width="100" />
        <el-table-column label="类别" width="100">
          <template #default="{ row }">
            <span>{{ categoryMap[row.category] || row.category }}</span>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="80">
          <template #default="{ row }">
            <el-tag :type="priorityMap[row.priority]?.type as any" size="small">
              {{ priorityMap[row.priority]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type as any" size="small">
              {{ statusMap[row.status]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="处理人" width="100">
          <template #default="{ row }">
            <span>{{ row.assigneeName || '未分配' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleViewDetail(row)">详情</el-button>
            <el-button size="small" type="success" link @click="handleOpenStatusUpdate(row)">更新状态</el-button>
            <el-button size="small" type="warning" link @click="handleOpenAssign(row)">分配</el-button>
            <el-button size="small" type="info" link @click="handleOpenInternalComment(row)">备注</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailDialogVisible" title="工单详情" width="800px" top="5vh">
      <div v-loading="detailLoading">
        <template v-if="currentDetail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="工单编号">{{ currentDetail.id }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentDetail.createTime }}</el-descriptions-item>
            <el-descriptions-item label="标题" :span="2">{{ currentDetail.title }}</el-descriptions-item>
            <el-descriptions-item label="提交人">{{ currentDetail.username }}</el-descriptions-item>
            <el-descriptions-item label="处理人">{{ currentDetail.assigneeName || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="类别">{{ categoryMap[currentDetail.category] || currentDetail.category }}</el-descriptions-item>
            <el-descriptions-item label="优先级">
              <el-tag :type="priorityMap[currentDetail.priority]?.type as any" size="small">
                {{ priorityMap[currentDetail.priority]?.text || '未知' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusMap[currentDetail.status]?.type as any">
                {{ statusMap[currentDetail.status]?.text || '未知' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="满意度" v-if="currentDetail.satisfaction">
              <el-rate v-model="currentDetail.satisfaction" disabled />
            </el-descriptions-item>
            <el-descriptions-item label="问题描述" :span="2">
              <div class="description-content">{{ currentDetail.description }}</div>
            </el-descriptions-item>
            <el-descriptions-item v-if="currentDetail.resolveContent" label="解决方案" :span="2">
              <div class="resolve-content">{{ currentDetail.resolveContent }}</div>
            </el-descriptions-item>
          </el-descriptions>

          <el-divider>沟通记录</el-divider>

          <el-timeline>
            <el-timeline-item
              v-for="item in detailComments"
              :key="item.id"
              :timestamp="item.createTime"
              :type="item.isInternal ? 'warning' : 'primary'"
              placement="top"
            >
              <el-card class="comment-item" :class="{ 'internal-comment': item.isInternal }">
                <div class="comment-header">
                  <span class="comment-user">{{ item.username || '管理员' }}</span>
                  <el-tag v-if="item.isInternal" type="warning" size="small">内部备注</el-tag>
                </div>
                <div class="comment-content">{{ item.content }}</div>
              </el-card>
            </el-timeline-item>
          </el-timeline>

          <div v-if="detailComments.length === 0" class="no-comments">暂无沟通记录</div>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="statusUpdateDialogVisible" title="更新状态" width="500px">
      <el-form label-width="80px">
        <el-form-item label="状态">
          <el-select v-model="statusUpdateForm.status" style="width: 100%">
            <el-option
              v-for="item in statusOptions.filter(i => i.value !== null)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="解决方案" v-if="statusUpdateForm.status === 2">
          <el-input
            v-model="statusUpdateForm.resolveContent"
            type="textarea"
            :rows="4"
            placeholder="请输入解决方案"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusUpdateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStatusUpdate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialogVisible" title="分配处理人" width="400px">
      <el-form label-width="80px">
        <el-form-item label="处理人ID">
          <el-input-number v-model="assignForm.assigneeId" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssign">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="internalCommentDialogVisible" title="添加内部备注" width="500px">
      <el-form label-width="80px">
        <el-form-item label="备注内容">
          <el-input
            v-model="internalCommentForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入内部备注"
          />
        </el-form-item>
        <el-form-item label="图片（可选）">
          <el-input v-model="internalCommentForm.images" placeholder="请输入图片URL，多个用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="internalCommentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleInternalComment">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 0;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 16px;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
  border-radius: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.stat-pending .stat-value {
  color: #e6a23c;
}

.stat-processing .stat-value {
  color: #409eff;
}

.stat-resolved .stat-value {
  color: #67c23a;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  margin-bottom: 16px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.description-content,
.resolve-content {
  white-space: pre-wrap;
  line-height: 1.6;
}

.comment-item {
  margin-bottom: 0;
}

.internal-comment {
  background-color: #fdf6ec;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-user {
  font-weight: 600;
  color: #333;
}

.comment-content {
  line-height: 1.6;
  white-space: pre-wrap;
}

.no-comments {
  text-align: center;
  color: #909399;
  padding: 20px 0;
}
</style>