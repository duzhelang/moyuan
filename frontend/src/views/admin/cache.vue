<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clearPoetCache, clearAllCache } from '@/api/modules/admin'

const clearingPoets = ref(false)
const clearingAll = ref(false)

const handleClearPoets = async () => {
  try {
    await ElMessageBox.confirm('确认清除诗人相关缓存？清除后下次访问将重新查询数据库。', '操作确认', { type: 'warning' })
  } catch {
    return
  }
  clearingPoets.value = true
  try {
    const res = await clearPoetCache()
    ElMessage.success(res.data || '诗人缓存已清除')
  } catch (e: any) {
    ElMessage.error(e?.message || '清除失败')
  } finally {
    clearingPoets.value = false
  }
}

const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm('确认清除全部缓存（诗人 + 诗词）？清除后下次访问将重新查询数据库。', '操作确认', { type: 'warning' })
  } catch {
    return
  }
  clearingAll.value = true
  try {
    const res = await clearAllCache()
    ElMessage.success(res.data || '所有缓存已清除')
  } catch (e: any) {
    ElMessage.error(e?.message || '清除失败')
  } finally {
    clearingAll.value = false
  }
}
</script>

<template>
  <div class="cache-page">
    <div class="page-header">
      <h2>缓存管理</h2>
    </div>

    <el-alert
      title="缓存用于加速诗人、诗词等数据的读取。当数据更新后前台未及时刷新时，可手动清除缓存使下次访问重新从数据库加载。"
      type="info"
      :closable="false"
      show-icon
      class="tip-alert"
    />

    <div class="cache-cards">
      <el-card shadow="never" class="cache-card">
        <div class="card-title">清除诗人缓存</div>
        <div class="card-desc">清除诗人列表、诗人详情等缓存数据。</div>
        <el-button
          type="warning"
          :loading="clearingPoets"
          @click="handleClearPoets"
        >
          清除诗人缓存
        </el-button>
      </el-card>

      <el-card shadow="never" class="cache-card">
        <div class="card-title">清除全部缓存</div>
        <div class="card-desc">清除诗人与诗词等全部业务缓存，操作影响范围更大。</div>
        <el-button
          type="danger"
          :loading="clearingAll"
          @click="handleClearAll"
        >
          清除全部缓存
        </el-button>
      </el-card>
    </div>
  </div>
</template>

<style scoped lang="scss">
.cache-page {
  padding: 20px;
}
.page-header {
  margin-bottom: 20px;
  h2 {
    margin: 0;
    font-size: 20px;
  }
}
.tip-alert {
  margin-bottom: 20px;
}
.cache-cards {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}
.cache-card {
  width: 320px;
  .card-title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 8px;
  }
  .card-desc {
    font-size: 13px;
    color: #909399;
    margin-bottom: 16px;
    min-height: 36px;
  }
}
</style>
