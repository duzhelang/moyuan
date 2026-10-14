<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminFiles, getFileStorageStats, cleanOrphanFiles, deleteAdminFile } from '@/api/modules/admin'

const loading = ref(false)
const dataList = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const fileTypeFilter = ref<string>('')
const keyword = ref('')

const stats = ref<any>({ totalCount: 0, totalSize: 0, missingCount: 0, typeStats: [] })
const cleaning = ref(false)

const fileTypeOptions = [
  'avatar', 'poem', 'user_poem', 'forum', 'vision',
  'ai_generated', 'config', 'export', 'temp', 'backup', 'audit', 'watermark', 'cache'
]

const relatedTypeMap: Record<string, string> = {
  user: '用户',
  poem: '诗词',
  forum: '帖子',
  vision: '诗话视野',
  article: '文章',
}

const formatSize = (bytes: number) => {
  if (!bytes || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let index = 0
  let value = bytes
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index++
  }
  return `${value.toFixed(value >= 10 || index === 0 ? 0 : 1)} ${units[index]}`
}

const fetchStats = async () => {
  try {
    const res = await getFileStorageStats()
    stats.value = res.data || { totalCount: 0, totalSize: 0, missingCount: 0, typeStats: [] }
  } catch (e) {
    ElMessage.error('获取存储统计失败')
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminFiles({
      page: page.value,
      size: size.value,
      fileType: fileTypeFilter.value || undefined,
      keyword: keyword.value || undefined,
    })
    dataList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('获取文件列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchData()
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认删除文件「${row.originalName || row.fileKey}」？将同时删除物理文件。`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteAdminFile(row.id)
    ElMessage.success('已删除')
    fetchData()
    fetchStats()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

const handleCleanOrphans = async () => {
  try {
    await ElMessageBox.confirm('将扫描物理上传目录，删除磁盘存在但数据库无记录的孤儿文件。确认执行？', '清理确认', { type: 'warning' })
  } catch {
    return
  }
  cleaning.value = true
  try {
    const res = await cleanOrphanFiles()
    const result = res.data || {}
    ElMessage.success(`清理完成：删除 ${result.deletedCount || 0} 个文件，释放 ${formatSize(result.freedSize || 0)}`)
    fetchData()
    fetchStats()
  } catch (e: any) {
    ElMessage.error(e?.message || '清理失败')
  } finally {
    cleaning.value = false
  }
}

onMounted(() => {
  fetchStats()
  fetchData()
})
</script>

<template>
  <div class="files-page">
    <div class="page-header">
      <h2>文件管理</h2>
      <el-button type="warning" :loading="cleaning" @click="handleCleanOrphans">清理孤儿文件</el-button>
    </div>

    <div class="stats-row">
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">总文件数</div>
        <div class="stat-value">{{ stats.totalCount }}</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">总占用</div>
        <div class="stat-value">{{ formatSize(stats.totalSize) }}</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">物理文件缺失</div>
        <div class="stat-value" :class="{ warn: stats.missingCount > 0 }">{{ stats.missingCount }}</div>
      </el-card>
      <el-card shadow="never" class="stat-card type-card">
        <div class="stat-label">各类型占用</div>
        <div class="type-list">
          <span v-for="item in stats.typeStats" :key="item.fileType" class="type-item">
            {{ item.fileType }}：{{ item.count }} 个 / {{ formatSize(item.size) }}
          </span>
          <span v-if="!stats.typeStats || stats.typeStats.length === 0" class="type-empty">暂无数据</span>
        </div>
      </el-card>
    </div>

    <div class="filter-bar">
      <el-select v-model="fileTypeFilter" placeholder="全部类型" clearable style="width: 160px" @change="handleSearch">
        <el-option v-for="t in fileTypeOptions" :key="t" :label="t" :value="t" />
      </el-select>
      <el-input v-model="keyword" placeholder="按文件名 / fileKey 搜索" clearable style="width: 260px" @keyup.enter="handleSearch" />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <el-table :data="dataList" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="originalName" label="文件名" min-width="160" show-overflow-tooltip />
      <el-table-column prop="fileKey" label="fileKey" min-width="200" show-overflow-tooltip />
      <el-table-column prop="fileType" label="类型" width="110" />
      <el-table-column prop="mimeType" label="MIME" width="130" show-overflow-tooltip />
      <el-table-column label="大小" width="100">
        <template #default="{ row }">{{ formatSize(row.size) }}</template>
      </el-table-column>
      <el-table-column label="关联" width="120">
        <template #default="{ row }">
          <span v-if="row.relatedType">{{ relatedTypeMap[row.relatedType] || row.relatedType }}#{{ row.relatedId }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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
.files-page {
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
.stats-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.stat-card {
  width: 220px;
  .stat-label {
    font-size: 13px;
    color: #909399;
    margin-bottom: 8px;
  }
  .stat-value {
    font-size: 22px;
    font-weight: 600;
    color: #303133;
    &.warn {
      color: #e6a23c;
    }
  }
}
.type-card {
  width: 480px;
  .type-list {
    display: flex;
    flex-wrap: wrap;
    gap: 4px 12px;
    font-size: 13px;
    color: #606266;
    max-height: 72px;
    overflow-y: auto;
  }
  .type-empty {
    font-size: 13px;
    color: #c0c4cc;
  }
}
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.pagination-bar {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
