<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Back, User, ChatDotRound, SwitchButton, View, Delete } from '@element-plus/icons-vue'
import { getMyRepairOrders, closeRepairOrder } from '@/api/modules/repair'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const statusFilter = ref<number | null>(null)
const loading = ref(false)

const statusOptions = [
  { value: null, label: '全部' },
  { value: 0, label: '待处理' },
  { value: 1, label: '处理中' },
  { value: 2, label: '已解决' },
  { value: 3, label: '已关闭' },
  { value: 4, label: '已驳回' }
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

const stats = computed(() => {
  const pending = tableData.value.filter(item => item.status === 0).length
  const processing = tableData.value.filter(item => item.status === 1).length
  const resolved = tableData.value.filter(item => item.status === 2).length
  return { pending, processing, resolved, total: tableData.value.length }
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMyRepairOrders({
      pageNum: page.value,
      pageSize: pageSize.value,
      status: statusFilter.value
    })
    tableData.value = res.data.list
    total.value = res.data.total
  } catch (error) {
    console.error('获取报修列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleStatusChange = () => {
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

const goToDetail = (id: number) => {
  router.push(`/repair/${id}`)
}

const goToCreate = () => {
  router.push('/repair/create')
}

const goToProfile = () => {
  router.push('/user/profile')
}

const goToContact = () => {
  router.push('/contact')
}

const goBack = () => {
  router.go(-1)
}

const handleCloseOrder = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要关闭此工单吗？关闭后将无法继续操作。', '提示', { type: 'warning' })
    await closeRepairOrder(id)
    ElMessage.success('工单已关闭')
    await fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('关闭工单失败', error)
    }
  }
}

const canClose = (status: number) => {
  return status === 0 || status === 1
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    userStore.logout()
    router.push('/')
    ElMessage.success('已退出登录')
  } catch {}
}

onMounted(fetchData)
</script>

<template>
  <div class="repair-container">
    <div class="repair-header">
      <div class="header-left">
        <el-button @click="goBack">
          <el-icon><Back /></el-icon>
          <span>上一页</span>
        </el-button>
        <h2 class="repair-title">我的报修</h2>
      </div>
      <div class="header-right">
        <el-button @click="goToProfile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-button>
        <el-button @click="goToContact">
          <el-icon><ChatDotRound /></el-icon>
          <span>联系团队</span>
        </el-button>
        <el-button type="primary" :icon="Plus" @click="goToCreate">提交报修</el-button>
        <el-button type="danger" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出</span>
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总工单数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-pending" shadow="hover">
          <div class="stat-value">{{ stats.pending }}</div>
          <div class="stat-label">待处理</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-processing" shadow="hover">
          <div class="stat-value">{{ stats.processing }}</div>
          <div class="stat-label">处理中</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-resolved" shadow="hover">
          <div class="stat-value">{{ stats.resolved }}</div>
          <div class="stat-label">已解决</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="filter-card">
      <el-row :gutter="16" align="middle">
        <el-col :span="6">
          <el-select
            v-model="statusFilter"
            placeholder="选择状态"
            clearable
            @change="handleStatusChange"
          >
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-col>
        <el-col :span="18" class="filter-actions">
          <el-button type="primary" :icon="Plus" @click="goToCreate">提交新报修</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="工单编号" width="100" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
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
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="goToDetail(row.id)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <el-button
              v-if="canClose(row.status)"
              size="small"
              type="danger"
              link
              @click="handleCloseOrder(row.id)"
            >
              <el-icon><Delete /></el-icon>
              关闭
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无报修记录">
        <el-button type="primary" @click="goToCreate">提交报修</el-button>
      </el-empty>

      <div class="pagination-wrapper" v-if="total > 0">
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
  </div>
</template>

<style scoped lang="scss">
.repair-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.repair-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.repair-title {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
  }
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

.filter-card {
  margin-bottom: 16px;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
}

.table-card {
  border-radius: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
