<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminPoetProfiles, verifyPoetProfile } from '@/api/modules/admin'

const loading = ref(false)
const dataList = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const statusFilter = ref<number | null>(null)

const statusOptions = [
  { label: '全部', value: null },
  { label: '未认证', value: 0 },
  { label: '认证中', value: 1 },
  { label: '已认证', value: 2 },
  { label: '认证失败', value: 3 },
]

const statusTagMap: Record<number, { type: string; label: string }> = {
  0: { type: 'info', label: '未认证' },
  1: { type: 'warning', label: '认证中' },
  2: { type: 'success', label: '已认证' },
  3: { type: 'danger', label: '认证失败' },
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminPoetProfiles({
      page: page.value,
      size: size.value,
      status: statusFilter.value
    })
    dataList.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleApprove = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认通过该诗人认证申请？', '审核确认', { type: 'warning' })
    await verifyPoetProfile(row.id, 2)
    ElMessage.success('认证通过')
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const handleReject = async (row: any) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因（可选）', '审核拒绝', {
      inputPlaceholder: '拒绝原因',
      type: 'warning',
    })
    await verifyPoetProfile(row.id, 3, value || undefined)
    ElMessage.success('已拒绝')
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div class="poet-profiles-page">
    <div class="page-header">
      <h2>诗人认证审核</h2>
      <div class="filter-bar">
        <el-radio-group v-model="statusFilter" @change="fetchData">
          <el-radio-button v-for="opt in statusOptions" :key="opt.value" :label="opt.value">
            {{ opt.label }}
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <el-table :data="dataList" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="nickname" label="用户昵称" width="120" />
      <el-table-column prop="penName" label="笔名" width="120" />
      <el-table-column prop="genre" label="擅长体裁" width="120" />
      <el-table-column label="简介" min-width="200">
        <template #default="{ row }">
          <el-tooltip :content="row.introduction" placement="top" :show-after="500">
            <span class="content-preview">{{ (row.introduction || '').substring(0, 80) }}{{ (row.introduction || '').length > 80 ? '...' : '' }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagMap[row.status]?.type as any" size="small">
            {{ statusTagMap[row.status]?.label || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 1">
            <el-button type="success" size="small" @click="handleApprove(row)">通过</el-button>
            <el-button type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
          </template>
          <span v-else-if="row.status === 2" class="text-success">已认证</span>
          <span v-else-if="row.status === 3" class="text-danger">已拒绝</span>
          <span v-else class="text-muted">未申请</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchData"
        @size-change="fetchData"
      />
    </div>
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
  margin-bottom: 20px;
  h2 {
    margin: 0;
    font-size: 20px;
  }
}
.content-preview {
  color: #666;
  font-size: 13px;
  cursor: pointer;
}
.pagination-bar {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.text-success {
  color: #67c23a;
  font-size: 13px;
}
.text-danger {
  color: #f56c6c;
  font-size: 13px;
}
.text-muted {
  color: #999;
  font-size: 13px;
}
</style>
