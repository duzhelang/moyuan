<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getNotifications,
  getUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
  type UserNotification
} from '@/api/modules/notification'

const router = useRouter()

const loading = ref(false)
const list = ref<UserNotification[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const unreadOnly = ref(false)
const unreadCount = ref(0)

const typeMap: Record<string, { label: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
  audit_result: { label: '审核结果', type: 'success' },
  report_result: { label: '举报结果', type: 'warning' },
  system: { label: '系统通知', type: 'info' }
}

const formatTime = (value?: string | null) => (value ? String(value).replace('T', ' ').slice(0, 16) : '-')

const emptyText = computed(() => (unreadOnly.value ? '暂无未读通知' : '暂无通知'))

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getNotifications({
      pageNum: page.value,
      pageSize: size.value,
      unreadOnly: unreadOnly.value || undefined
    })
    list.value = res.data.list || []
    total.value = res.data.total || 0
    await loadUnreadCount()
  } catch {
    ElMessage.error('获取通知失败')
  } finally {
    loading.value = false
  }
}

const loadUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data?.count || 0
  } catch {
    unreadCount.value = 0
  }
}

const openNotification = async (item: UserNotification) => {
  if (item.isRead === 0) {
    try {
      await markNotificationRead(item.id)
      item.isRead = 1
      await loadUnreadCount()
    } catch {
      // 标记失败不阻断跳转
    }
  }
  if (item.link) {
    router.push(item.link)
  }
}

const handleMarkAll = async () => {
  try {
    await markAllNotificationsRead()
    ElMessage.success('已全部标记为已读')
    fetchList()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleFilter = (value: boolean) => {
  unreadOnly.value = value
  page.value = 1
  fetchList()
}

onMounted(fetchList)
</script>

<template>
  <div class="notification-page">
    <div class="page-inner">
      <div class="page-head">
        <div>
          <h2 class="page-title">站内通知</h2>
          <p class="page-desc">审核结果、举报处理结果都会在这里通知你</p>
        </div>
        <el-button :disabled="list.length === 0 || unreadCount === 0" @click="handleMarkAll">全部已读</el-button>
      </div>

      <div class="filter-bar">
        <el-radio-group :model-value="unreadOnly" @change="handleFilter($event as boolean)">
          <el-radio-button :value="false">全部</el-radio-button>
          <el-radio-button :value="true">未读</el-radio-button>
        </el-radio-group>
      </div>

      <div v-loading="loading" class="notification-list">
        <el-empty v-if="!loading && list.length === 0" :description="emptyText" />

        <div
          v-for="item in list"
          :key="item.id"
          class="notification-item"
          :class="{ unread: item.isRead === 0, clickable: !!item.link }"
          @click="openNotification(item)"
        >
          <div class="item-main">
            <div class="item-head">
              <el-tag :type="typeMap[item.type]?.type || 'info'" size="small">
                {{ typeMap[item.type]?.label || '通知' }}
              </el-tag>
              <span class="item-title">{{ item.title }}</span>
              <span v-if="item.isRead === 0" class="unread-dot"></span>
            </div>
            <p v-if="item.content" class="item-content">{{ item.content }}</p>
            <span class="item-time">{{ formatTime(item.createTime) }}</span>
          </div>
          <el-icon v-if="item.link" class="item-arrow"><ArrowRight /></el-icon>
        </div>
      </div>

      <div v-if="total > size" class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.notification-page {
  min-height: 78vh;
  padding: 90px 0 40px;
  background: linear-gradient(180deg, #f7f2ea 0%, #fdfbf7 100%);
}

.page-inner {
  width: 100%;
  max-width: 860px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  color: #3d3024;
}

.page-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: #9a8877;
}

.filter-bar {
  margin-bottom: 14px;
}

.notification-list {
  min-height: 200px;
}

.notification-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  margin-bottom: 12px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #efe5d8;
  transition: box-shadow 0.2s ease, transform 0.2s ease;

  &.clickable {
    cursor: pointer;
  }

  &.clickable:hover {
    box-shadow: 0 8px 20px rgba(120, 96, 70, 0.12);
    transform: translateY(-1px);
  }

  &.unread {
    border-left: 3px solid #c9a06c;
    background: #fffdf9;
  }
}

.item-main {
  flex: 1;
  min-width: 0;
}

.item-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.item-title {
  font-size: 15px;
  font-weight: 600;
  color: #3d3024;
}

.unread-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #d4583f;
}

.item-content {
  margin: 8px 0 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #6f6154;
  word-break: break-word;
}

.item-time {
  font-size: 12px;
  color: #a99a8a;
}

.item-arrow {
  color: #c2b3a3;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
