<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminForumPosts, updateForumPostStatus, deleteAdminForumPost } from '@/api/modules/admin'

const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminForumPosts({ page: page.value, size: pageSize.value, keyword: keyword.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取帖子列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleStatusChange = async (row: any, status: number) => {
  try {
    await updateForumPostStatus(row.id, status)
    ElMessage.success('状态更新成功')
    fetchData()
  } catch (error) {
    console.error('更新状态失败', error)
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该帖子吗？', '提示', { type: 'warning' })
    await deleteAdminForumPost(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') console.error('删除失败', error)
  }
}

const handleSearch = () => {
  page.value = 1
  fetchData()
}

const getStatusTag = (status: number) => {
  switch (status) {
    case 0: return { type: 'info' as const, text: '草稿' }
    case 1: return { type: 'success' as const, text: '已发布' }
    case 2: return { type: 'warning' as const, text: '已关闭' }
    default: return { type: 'info' as const, text: '未知' }
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">帖子管理</h2>

    <el-card class="search-card">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-input v-model="keyword" placeholder="搜索帖子标题/内容" clearable @keyup.enter="handleSearch" />
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status).type" size="small">{{ getStatusTag(row.status).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="60">
          <template #default="{ row }">
            <el-tag v-if="row.isTop" type="warning" size="small">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="60" />
        <el-table-column prop="likeCount" label="点赞" width="60" />
        <el-table-column prop="commentCount" label="评论" width="60" />
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" v-if="row.status === 1" @click="handleStatusChange(row, 2)">关闭</el-button>
            <el-button size="small" v-if="row.status === 2" @click="handleStatusChange(row, 1)">发布</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-title { font-size: 24px; margin-bottom: 20px; color: #333; }
.search-card { margin-bottom: 16px; }
.table-card { border-radius: 8px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
