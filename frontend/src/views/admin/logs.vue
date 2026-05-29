<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminLogs } from '@/api/modules/admin'

const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const dateRange = ref<[string, string] | null>(null)
const loading = ref(false)

const methodTagType = (method: string) => {
  const map: Record<string, string> = {
    GET: 'info',
    POST: 'success',
    PUT: 'warning',
    DELETE: 'danger'
  }
  return map[method] || 'info'
}

const fetchData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: pageSize.value
    }
    if (keyword.value) params.keyword = keyword.value
    if (dateRange.value) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }
    const res = await getAdminLogs(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取操作日志失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchData()
}

const handleReset = () => {
  keyword.value = ''
  dateRange.value = null
  page.value = 1
  fetchData()
}

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">操作日志</h2>

    <el-card class="search-card">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-input v-model="keyword" placeholder="搜索操作描述/URI/用户名" clearable @keyup.enter="handleSearch" />
        </el-col>
        <el-col :span="10">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <div v-if="row.params" class="expand-item">
                <span class="expand-label">请求参数：</span>
                <pre class="expand-value">{{ row.params }}</pre>
              </div>
              <div v-if="row.result" class="expand-item">
                <span class="expand-label">返回结果：</span>
                <pre class="expand-value">{{ row.result }}</pre>
              </div>
              <div v-if="row.error" class="expand-item">
                <span class="expand-label">错误信息：</span>
                <pre class="expand-value error-text">{{ row.error }}</pre>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="请求方法" width="100">
          <template #default="{ row }">
            <el-tag :type="methodTagType(row.method)" size="small">{{ row.method }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uri" label="URI" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operation" label="操作描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="username" label="用户名" width="100" />
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">
            <span :style="{ color: row.duration > 1000 ? '#f56c6c' : '#666' }">{{ row.duration }}ms</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.error ? 'danger' : 'success'" size="small">{{ row.error ? '失败' : '成功' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="170" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchData"
          @size-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-title {
  font-size: 24px;
  margin-bottom: 20px;
  color: #333;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  border-radius: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.expand-content {
  padding: 12px 48px;
}

.expand-item {
  margin-bottom: 12px;
}

.expand-label {
  font-weight: 600;
  color: #333;
}

.expand-value {
  margin: 4px 0 0;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #666;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
}

.error-text {
  color: #f56c6c;
}
</style>
