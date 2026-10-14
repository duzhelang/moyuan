<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminPoetProfiles, verifyPoetProfile } from '@/api/modules/admin'

const loading = ref(false)
const dataList = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const statusFilter = ref<number | null>(null)

// verifiedStatus：0-未认证，1-已认证，2-认证中，3-认证失败
const statusOptions = [
  { label: '全部', value: null },
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

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminPoetProfiles({ page: page.value, size: size.value, status: statusFilter.value })
    dataList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const openReview = (row: any) => {
  currentRow.value = row
  reviewResult.value = 1
  reviewReason.value = ''
  dialogVisible.value = true
}

const submitReview = async () => {
  if (!currentRow.value) return
  if (reviewResult.value === 3 && !reviewReason.value.trim()) {
    ElMessage.warning('拒绝时请填写备注说明原因')
    return
  }
  submitting.value = true
  try {
    await verifyPoetProfile(currentRow.value.id, reviewResult.value, reviewReason.value || undefined)
    ElMessage.success(reviewResult.value === 1 ? '已通过认证' : '已拒绝认证')
    dialogVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    submitting.value = false
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
          <el-radio-button v-for="opt in statusOptions" :key="String(opt.value)" :label="opt.value">
            {{ opt.label }}
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <el-table :data="dataList" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column prop="penName" label="笔名" min-width="120" show-overflow-tooltip />
      <el-table-column prop="realName" label="真实姓名" min-width="120" show-overflow-tooltip />
      <el-table-column prop="specialty" label="擅长领域" min-width="140" show-overflow-tooltip />
      <el-table-column label="简介" min-width="200">
        <template #default="{ row }">
          <el-tooltip v-if="row.introduction" :content="row.introduction" placement="top" :show-after="500">
            <span class="ellipsis-text">{{ row.introduction }}</span>
          </el-tooltip>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="认证状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagMap[row.verifiedStatus]?.type as any" size="small">
            {{ statusTagMap[row.verifiedStatus]?.label || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" width="170" />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openReview(row)">审核</el-button>
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

    <el-dialog v-model="dialogVisible" title="诗人认证审核" width="520px">
      <el-form label-width="90px" v-if="currentRow">
        <el-form-item label="笔名">
          <span>{{ currentRow.penName || '-' }}</span>
        </el-form-item>
        <el-form-item label="真实姓名">
          <span>{{ currentRow.realName || '-' }}</span>
        </el-form-item>
        <el-form-item label="审核结果">
          <el-radio-group v-model="reviewResult">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="3">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="reviewReason"
            type="textarea"
            :rows="3"
            :placeholder="reviewResult === 3 ? '请填写拒绝原因（必填）' : '可填写备注（选填）'"
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
  margin-bottom: 20px;
  h2 {
    margin: 0;
    font-size: 20px;
  }
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
</style>
