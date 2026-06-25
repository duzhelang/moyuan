<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRepairOrderDetail, getRepairComments, addRepairComment, closeRepairOrder, submitSatisfaction } from '@/api/modules/repair'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const orderId = Number(route.params.id)

const orderDetail = ref<any>(null)
const comments = ref<any[]>([])
const loading = ref(false)
const commentContent = ref('')
const submittingComment = ref(false)

const satisfactionScore = ref(0)
const satisfactionComment = ref('')
const submittingSatisfaction = ref(false)

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

const canClose = computed(() => {
  return orderDetail.value && (orderDetail.value.status === 0 || orderDetail.value.status === 1)
})

const canEvaluate = computed(() => {
  return orderDetail.value && orderDetail.value.status === 2 && !orderDetail.value.satisfaction
})

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await getRepairOrderDetail(orderId)
    orderDetail.value = res.data
  } catch (error) {
    console.error('获取报修详情失败', error)
  } finally {
    loading.value = false
  }
}

const fetchComments = async () => {
  try {
    const res = await getRepairComments(orderId)
    comments.value = res.data || []
  } catch (error) {
    console.error('获取评论列表失败', error)
  }
}

const handleSubmitComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入反馈内容')
    return
  }
  submittingComment.value = true
  try {
    await addRepairComment(orderId, { content: commentContent.value })
    ElMessage.success('反馈提交成功')
    commentContent.value = ''
    await fetchComments()
  } catch (error) {
    console.error('提交反馈失败', error)
  } finally {
    submittingComment.value = false
  }
}

const handleCloseOrder = async () => {
  try {
    await ElMessageBox.confirm('确定要关闭此工单吗？关闭后将无法继续操作。', '提示', { type: 'warning' })
    await closeRepairOrder(orderId)
    ElMessage.success('工单已关闭')
    await fetchDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('关闭工单失败', error)
    }
  }
}

const handleSubmitSatisfaction = async () => {
  if (satisfactionScore.value === 0) {
    ElMessage.warning('请选择评分')
    return
  }
  submittingSatisfaction.value = true
  try {
    await submitSatisfaction(orderId, {
      satisfaction: satisfactionScore.value,
      comment: satisfactionComment.value || undefined
    })
    ElMessage.success('评价提交成功')
    satisfactionScore.value = 0
    satisfactionComment.value = ''
    await fetchDetail()
  } catch (error) {
    console.error('提交评价失败', error)
  } finally {
    submittingSatisfaction.value = false
  }
}

const goBack = () => {
  router.go(-1)
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    userStore.logout()
    router.push('/')
    ElMessage.success('已退出登录')
  } catch {}
}

onMounted(async () => {
  await fetchDetail()
  await fetchComments()
})
</script>

<template>
  <div class="detail-container" v-loading="loading">
    <div class="detail-header">
      <div class="header-left">
        <el-button @click="goBack">
          <el-icon><Back /></el-icon>
          <span>上一页</span>
        </el-button>
        <h2 class="detail-title">报修详情</h2>
      </div>
      <div class="header-right">
        <el-button @click="router.push('/repair')">
          <el-icon><List /></el-icon>
          <span>报修列表</span>
        </el-button>
        <el-button @click="router.push('/contact')">
          <el-icon><ChatDotRound /></el-icon>
          <span>联系团队</span>
        </el-button>
        <el-button @click="router.push('/repair/create')">
          <el-icon><Plus /></el-icon>
          <span>新建报修</span>
        </el-button>
        <el-button type="danger" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出</span>
        </el-button>
      </div>
    </div>

    <template v-if="orderDetail">
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <span>工单信息</span>
            <el-tag :type="statusMap[orderDetail.status]?.type as any">
              {{ statusMap[orderDetail.status]?.text || '未知' }}
            </el-tag>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单编号">{{ orderDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ orderDetail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ orderDetail.title }}</el-descriptions-item>
          <el-descriptions-item label="类别">{{ categoryMap[orderDetail.category] || orderDetail.category }}</el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="priorityMap[orderDetail.priority]?.type as any" size="small">
              {{ priorityMap[orderDetail.priority]?.text || '未知' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2">
            <div class="description-content">{{ orderDetail.description }}</div>
          </el-descriptions-item>
          <el-descriptions-item v-if="orderDetail.resolveContent" label="解决方案" :span="2">
            <div class="resolve-content">{{ orderDetail.resolveContent }}</div>
          </el-descriptions-item>
          <el-descriptions-item v-if="orderDetail.images" label="相关图片" :span="2">
            <div class="image-list">
              <el-image
                v-for="(img, index) in orderDetail.images.split(',')"
                :key="index"
                :src="img"
                :preview-src-list="orderDetail.images.split(',')"
                fit="cover"
                class="preview-image"
              />
            </div>
          </el-descriptions-item>
        </el-descriptions>

        <div class="action-buttons" v-if="canClose">
          <el-button type="danger" @click="handleCloseOrder">关闭工单</el-button>
        </div>
      </el-card>

      <el-card class="comment-card">
        <template #header>
          <span>沟通记录</span>
        </template>

        <el-timeline>
          <el-timeline-item
            v-for="item in comments"
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
              <div v-if="item.images" class="comment-images">
                <el-image
                  v-for="(img, index) in item.images.split(',')"
                  :key="index"
                  :src="img"
                  :preview-src-list="item.images.split(',')"
                  fit="cover"
                  class="comment-image"
                />
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>

        <div v-if="comments.length === 0" class="no-comments">暂无沟通记录</div>
      </el-card>

      <el-card v-if="canEvaluate" class="evaluate-card">
        <template #header>
          <span>满意度评价</span>
        </template>

        <el-form label-position="top">
          <el-form-item label="评分">
            <el-rate v-model="satisfactionScore" show-text :texts="['很差', '较差', '一般', '满意', '非常满意']" />
          </el-form-item>
          <el-form-item label="评价内容（可选）">
            <el-input
              v-model="satisfactionComment"
              type="textarea"
              :rows="3"
              placeholder="请输入您的评价"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submittingSatisfaction" @click="handleSubmitSatisfaction">提交评价</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card v-if="orderDetail.status !== 3 && orderDetail.status !== 4" class="feedback-card">
        <template #header>
          <span>添加反馈</span>
        </template>

        <el-input
          v-model="commentContent"
          type="textarea"
          :rows="4"
          placeholder="请输入您的反馈内容"
        />
        <div class="feedback-actions">
          <el-button type="primary" :loading="submittingComment" @click="handleSubmitComment">提交反馈</el-button>
        </div>
      </el-card>
    </template>
  </div>
</template>

<style scoped lang="scss">
.detail-container {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.detail-header {
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

.detail-title {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.info-card,
.comment-card,
.evaluate-card,
.feedback-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.description-content,
.resolve-content {
  white-space: pre-wrap;
  line-height: 1.6;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-image {
  width: 100px;
  height: 100px;
  border-radius: 4px;
}

.action-buttons {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
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

.comment-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.comment-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
}

.no-comments {
  text-align: center;
  color: #909399;
  padding: 20px 0;
}

.feedback-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.evaluate-card,
.feedback-card {
  @include el-comment-input;
}
</style>