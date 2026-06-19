<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminComments, auditComment, deleteAdminComment } from '@/api/modules/admin'

const loading = ref(false)
const dataList = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const statusFilter = ref<number | null>(null)

const statusOptions = [
  { label: '全部', value: null },
  { label: '正常', value: 0 },
  { label: '隐藏', value: 1 },
]

const statusTagMap: Record<number, { type: string; label: string }> = {
  0: { type: 'success', label: '正常' },
  1: { type: 'warning', label: '隐藏' },
}

const targetTypeMap: Record<number, string> = {
  0: '诗词',
  1: '帖子',
  2: '文章',
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminComments({
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
    await ElMessageBox.confirm('确认通过该评论？', '审核确认', { type: 'warning' })
    await auditComment(row.id, 0)
    ElMessage.success('审核通过')
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
    await auditComment(row.id, 1, value || undefined)
    ElMessage.success('已隐藏')
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认删除该评论？删除后不可恢复。', '删除确认', { type: 'warning' })
    await deleteAdminComment(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div class="comments-page">
    <div class="page-header">
      <h2>评论管理</h2>
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
      <el-table-column prop="nickname" label="用户" width="100" />
      <el-table-column label="评论内容" min-width="250">
        <template #default="{ row }">
          <el-tooltip :content="row.content" placement="top" :show-after="500">
            <span class="content-preview">{{ (row.content || '').substring(0, 100) }}{{ (row.content || '').length > 100 ? '...' : '' }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="目标类型" width="80">
        <template #default="{ row }">
          {{ targetTypeMap[row.targetType] || '未知' }}
        </template>
      </el-table-column>
      <el-table-column prop="targetTitle" label="目标标题" width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="statusTagMap[row.status]?.type as any" size="small">
            {{ statusTagMap[row.status]?.label || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="评论时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 1">
            <el-button type="success" size="small" @click="handleApprove(row)">通过</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
          <template v-else>
            <el-button type="warning" size="small" @click="handleReject(row)">隐藏</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
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
.comments-page {
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
</style>
