<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePoemStore } from '@/stores/poem'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { Comment, PoemRatingsData } from '@/types/model'
import { likePoem, favoritePoem, getPoemRatings, ratePoem, requestAiRating } from '@/api/modules/poem'
import { getComments, createComment, likeComment } from '@/api/modules/forum'
import { addHistory } from '@/api/modules/history'

const route = useRoute()
const router = useRouter()
const poemStore = usePoemStore()
const userStore = useUserStore()

const loading = ref(false)
const poem = computed(() => poemStore.currentPoem)
const comments = ref<Comment[]>([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentSize = ref(10)

const newComment = ref('')
const submittingComment = ref(false)

const poemId = computed(() => Number(route.params.id))

const ratingsData = ref<PoemRatingsData | null>(null)
const userScore = ref(0)
const userComment = ref('')
const submittingRating = ref(false)
const requestingAi = ref(false)
const aiPollTimer = ref<ReturnType<typeof setInterval> | null>(null)

const fetchPoem = async () => {
  loading.value = true
  try {
    await poemStore.fetchPoemDetail(poemId.value)
    addHistory(route.params.id as unknown as number, 1).catch(() => {})
  } catch (error) {
    ElMessage.error('获取诗词详情失败')
    router.push('/poem')
  } finally {
    loading.value = false
  }
}

const fetchComments = async () => {
  try {
    const res = await getComments(poemId.value, 1, {
      pageNum: commentPage.value,
      pageSize: commentSize.value
    })
    comments.value = res.data.list
    commentTotal.value = res.data.total
  } catch (error) {
    console.error('获取评论失败:', error)
  }
}

const fetchRatings = async () => {
  try {
    const res = await getPoemRatings(poemId.value)
    ratingsData.value = res.data
  } catch (error) {
    ElMessage.error('获取评分失败')
  }
}

const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await likePoem(poemId.value)
    if (poem.value) {
      const liked = !poem.value.isLiked
      poem.value.isLiked = liked
      poem.value.likeCount += liked ? 1 : -1
      ElMessage.success(liked ? '点赞成功' : '已取消点赞')
    }
  } catch (error) {
    ElMessage.error('点赞失败')
  }
}

const handleFavorite = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await favoritePoem(poemId.value)
    if (poem.value) {
      const favorited = !poem.value.isFavorited
      poem.value.isFavorited = favorited
      poem.value.favoriteCount += favorited ? 1 : -1
      ElMessage.success(favorited ? '收藏成功' : '已取消收藏')
    }
  } catch (error) {
    ElMessage.error('收藏失败')
  }
}

const handleSubmitRating = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再评分')
    return
  }
  if (userScore.value === 0) {
    ElMessage.warning('请选择评分')
    return
  }
  submittingRating.value = true
  try {
    await ratePoem(poemId.value, userScore.value, userComment.value || undefined)
    ElMessage.success('评分成功')
    userScore.value = 0
    userComment.value = ''
    fetchRatings()
  } catch (error) {
    ElMessage.error('评分失败')
  } finally {
    submittingRating.value = false
  }
}

const pollAiRating = () => {
  let attempts = 0
  const maxAttempts = 10
  aiPollTimer.value = setInterval(async () => {
    attempts++
    await fetchRatings()
    if (ratingsData.value?.aiRating || attempts >= maxAttempts) {
      if (aiPollTimer.value) {
        clearInterval(aiPollTimer.value)
        aiPollTimer.value = null
      }
      if (attempts >= maxAttempts && !ratingsData.value?.aiRating) {
        ElMessage.warning('AI评分处理中，请稍后刷新查看')
      }
    }
  }, 1500)
}

onUnmounted(() => {
  if (aiPollTimer.value) {
    clearInterval(aiPollTimer.value)
    aiPollTimer.value = null
  }
})

const handleRequestAiRating = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  requestingAi.value = true
  try {
    await requestAiRating(poemId.value)
    ElMessage.success('AI评分请求已提交')
    pollAiRating()
  } catch (error) {
    ElMessage.error('AI评分请求失败')
  } finally {
    requestingAi.value = false
  }
}

const handleSubmitComment = async () => {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  submittingComment.value = true
  try {
    await createComment({
      content: newComment.value,
      targetId: poemId.value,
      targetType: 1
    })
    ElMessage.success('评论成功')
    newComment.value = ''
    fetchComments()
  } catch (error) {
    ElMessage.error('评论失败')
  } finally {
    submittingComment.value = false
  }
}

const handleCommentLike = async (comment: Comment) => {
  try {
    await likeComment(comment.id)
    comment.likeCount++
    ElMessage.success('点赞成功')
  } catch (error) {
    ElMessage.error('点赞失败')
  }
}

const handleCommentPageChange = (page: number) => {
  commentPage.value = page
  fetchComments()
}

onMounted(() => {
  fetchPoem()
  fetchComments()
  fetchRatings()
})
</script>

<template>
  <div class="poem-detail-page" v-loading="loading">
    <div class="container" v-if="poem">
      <div class="poem-header">
        <el-button @click="router.push('/poem')" class="back-button">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>
      
      <el-card class="poem-card">
        <div class="poem-title-section">
          <h1 class="poem-title">{{ poem.title }}</h1>
          <div class="poem-meta">
            <span v-if="poem.dynastyName" class="meta-item">
              <el-icon><Calendar /></el-icon>
              {{ poem.dynastyName }}
            </span>
            <span v-if="poem.poetName" class="meta-item">
              <el-icon><User /></el-icon>
              {{ poem.poetName }}
            </span>
            <span v-if="poem.categoryName" class="meta-item">
              <el-icon><Collection /></el-icon>
              {{ poem.categoryName }}
            </span>
          </div>
        </div>
        
        <div class="poem-content-section">
          <div class="poem-text">
            <p>{{ poem.content }}</p>
          </div>
          
          <div class="poem-source" v-if="poem.source">
            <p>出处：{{ poem.source }}</p>
          </div>
        </div>
        
        <div class="poem-actions">
          <el-button :type="poem.isLiked ? 'danger' : 'primary'" @click="handleLike">
            <el-icon><Star /></el-icon>
            {{ poem.isLiked ? '已点赞' : '点赞' }} ({{ poem.likeCount }})
          </el-button>
          <el-button :type="poem.isFavorited ? 'danger' : 'warning'" @click="handleFavorite">
            <el-icon><CollectionTag /></el-icon>
            {{ poem.isFavorited ? '已收藏' : '收藏' }} ({{ poem.favoriteCount }})
          </el-button>
          <span class="view-count">
            <el-icon><View /></el-icon>
            {{ poem.viewCount }} 次浏览
          </span>
        </div>
      </el-card>

      <div class="rating-section" v-if="ratingsData">
        <h2 class="section-title">评分区域</h2>

        <div class="rating-overview">
          <div class="rating-score">
            <span class="score-value">{{ ratingsData.averageScore.toFixed(1) }}</span>
            <span class="score-label">综合评分</span>
          </div>
          <div class="rating-count">
            <span>{{ ratingsData.ratingCount }} 人评分</span>
          </div>
        </div>

        <div class="ai-rating-card" v-if="ratingsData.aiRating">
          <el-card>
            <template #header>
              <div class="ai-rating-header">
                <span class="ai-label">
                  <el-icon><Cpu /></el-icon>
                  AI 评分
                </span>
                <span class="ai-model">{{ ratingsData.aiRating.aiModel || '未知模型' }}</span>
              </div>
            </template>
            <div class="ai-rating-content">
              <div class="ai-score">
                <span class="score-value">{{ ratingsData.aiRating.score.toFixed(1) }}</span>
              </div>
              <div class="ai-analysis" v-if="ratingsData.aiRating.aiAnalysis">
                <p>{{ ratingsData.aiRating.aiAnalysis }}</p>
              </div>
            </div>
          </el-card>
        </div>

        <div class="request-ai-section" v-else>
          <el-button
            type="primary"
            :loading="requestingAi"
            @click="handleRequestAiRating"
          >
            <el-icon><Cpu /></el-icon>
            请求 AI 评分
          </el-button>
        </div>

        <div class="user-rating-section">
          <h3 class="sub-title">用户评分</h3>
          <div class="rating-form">
            <div class="score-select">
              <label>评分：</label>
              <el-rate
                v-model="userScore"
                :max="5"
                show-score
                score-template="{value} 分"
              />
            </div>
            <div class="comment-input">
              <el-input
                v-model="userComment"
                type="textarea"
                :rows="3"
                placeholder="写下你的评价（可选）..."
                maxlength="200"
                show-word-limit
              />
            </div>
            <el-button
              type="primary"
              :loading="submittingRating"
              @click="handleSubmitRating"
              :disabled="userScore === 0"
            >
              提交评分
            </el-button>
          </div>
        </div>

        <div class="user-ratings-list" v-if="ratingsData.userRatings.length > 0">
          <h3 class="sub-title">用户评价</h3>
          <div
            v-for="rating in ratingsData.userRatings"
            :key="rating.id"
            class="rating-item"
          >
            <div class="rating-item-header">
              <el-avatar :src="rating.avatar" :size="32">
                {{ rating.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span class="rating-username">{{ rating.username }}</span>
              <el-rate :model-value="rating.score" disabled :max="5" />
              <span class="rating-time">{{ rating.createTime }}</span>
            </div>
            <div class="rating-comment" v-if="rating.comment">
              <p>{{ rating.comment }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="comment-section">
        <h2 class="section-title">评论区</h2>
        
        <div class="comment-form">
          <el-input
            v-model="newComment"
            type="textarea"
            :rows="4"
            placeholder="写下你的评论..."
            maxlength="500"
            show-word-limit
          />
          <el-button
            type="primary"
            :loading="submittingComment"
            @click="handleSubmitComment"
            class="submit-button"
          >
            发表评论
          </el-button>
        </div>
        
        <div class="comment-list">
          <div v-if="comments.length === 0" class="empty-comments">
            <el-empty description="暂无评论，快来发表第一条评论吧" />
          </div>
          
          <div
            v-for="comment in comments"
            :key="comment.id"
            class="comment-item"
          >
            <div class="comment-header">
              <el-avatar :src="comment.avatar" :size="40">
                {{ comment.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <div class="comment-meta">
                <span class="comment-author">{{ comment.username }}</span>
                <span class="comment-time">{{ comment.createTime }}</span>
              </div>
            </div>
            <div class="comment-content">
              <p>{{ comment.content }}</p>
            </div>
            <div class="comment-actions">
              <el-button text size="small" @click="handleCommentLike(comment)">
                <el-icon><Star /></el-icon>
                {{ comment.likeCount }}
              </el-button>
              <el-button text size="small">
                <el-icon><ChatDotRound /></el-icon>
                回复
              </el-button>
            </div>
          </div>
        </div>
        
        <div class="comment-pagination" v-if="commentTotal > commentSize">
          <el-pagination
            v-model:current-page="commentPage"
            :page-size="commentSize"
            :total="commentTotal"
            layout="prev, pager, next"
            @current-change="handleCommentPageChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.poem-detail-page {
  padding: $spacing-xl 0;
  min-height: 60vh;
}

.poem-header {
  margin-bottom: $spacing-lg;
}

.back-button {
  margin-bottom: $spacing-md;
}

.poem-card {
  margin-bottom: $spacing-xl;
  border-radius: $border-radius-md;
}

.poem-title-section {
  text-align: center;
  margin-bottom: $spacing-xl;
  padding-bottom: $spacing-xl;
  border-bottom: 1px solid $border-color;
}

.poem-title {
  font-size: $font-size-hero;
  color: $primary-color;
  font-family: $font-family-title;
  margin-bottom: $spacing-md;
}

.poem-meta {
  display: flex;
  justify-content: center;
  gap: $spacing-lg;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-base;
  color: $text-color-secondary;
}

.poem-content-section {
  margin-bottom: $spacing-xl;
}

.poem-text {
  text-align: center;
  margin-bottom: $spacing-lg;
  
  p {
    font-size: $font-size-xl;
    line-height: 2;
    color: $text-color;
    white-space: pre-line;
  }
}

.poem-source {
  text-align: center;
  
  p {
    font-size: $font-size-sm;
    color: $text-color-secondary;
    font-style: italic;
  }
}

.poem-actions {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding-top: $spacing-xl;
  border-top: 1px solid $border-color;
}

.view-count {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-secondary;
  margin-left: auto;
}

.comment-section {
  margin-top: $spacing-xl;
}

.section-title {
  font-size: $font-size-xl;
  color: $text-color;
  margin-bottom: $spacing-lg;
  padding-bottom: $spacing-sm;
  border-bottom: 2px solid $primary-color;
  display: inline-block;
}

.comment-form {
  margin-bottom: $spacing-xl;

  :deep(.el-textarea__inner) {
    font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
    letter-spacing: 0.5px;
    line-height: 1.6;
  }
}

.submit-button {
  margin-top: $spacing-md;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.empty-comments {
  padding: $spacing-xxl 0;
}

.comment-item {
  @include card;
  padding: $spacing-lg;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.comment-meta {
  display: flex;
  flex-direction: column;
}

.comment-author {
  font-size: $font-size-base;
  color: $text-color;
  font-weight: 600;
}

.comment-time {
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.comment-content {
  margin-bottom: $spacing-md;
  
  p {
    font-size: $font-size-base;
    color: $text-color;
    line-height: $line-height-loose;
  }
}

.comment-actions {
  display: flex;
  gap: $spacing-sm;
}

.comment-pagination {
  margin-top: $spacing-xl;
  display: flex;
  justify-content: center;
}

.rating-section {
  margin-top: $spacing-xl;
  padding: $spacing-lg;
  background: $background-color-light;
  border-radius: $border-radius-md;
  box-shadow: $box-shadow;
}

.rating-overview {
  display: flex;
  align-items: center;
  gap: $spacing-xl;
  margin-bottom: $spacing-xl;
  padding: $spacing-lg;
  background: linear-gradient(135deg, rgba($primary-color, 0.05), rgba($accent-color, 0.05));
  border-radius: $border-radius-md;
}

.rating-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-xs;
}

.score-value {
  font-size: $font-size-hero;
  font-weight: bold;
  color: $warning-color;
  font-family: $font-family-title;
}

.score-label {
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.rating-count {
  font-size: $font-size-base;
  color: $text-color-secondary;
}

.ai-rating-card {
  margin-bottom: $spacing-xl;

  .ai-rating-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .ai-label {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
    font-weight: 600;
    color: $primary-color;
  }

  .ai-model {
    font-size: $font-size-sm;
    color: $text-color-light;
  }

  .ai-rating-content {
    display: flex;
    gap: $spacing-xl;
    align-items: flex-start;
  }

  .ai-score {
    flex-shrink: 0;

    .score-value {
      font-size: $font-size-xxl;
      color: $primary-color;
    }
  }

  .ai-analysis {
    flex: 1;

    p {
      font-size: $font-size-base;
      color: $text-color;
      line-height: $line-height-loose;
      margin: 0;
    }
  }
}

.request-ai-section {
  margin-bottom: $spacing-xl;
  text-align: center;
  padding: $spacing-lg;
  background: $background-color;
  border-radius: $border-radius-md;
}

.user-rating-section {
  margin-bottom: $spacing-xl;

  .sub-title {
    font-size: $font-size-lg;
    color: $text-color;
    margin-bottom: $spacing-md;
    font-family: $font-family-title;
  }
}

.rating-form {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;

  .score-select {
    display: flex;
    align-items: center;
    gap: $spacing-md;

    label {
      font-size: $font-size-base;
      color: $text-color;
      white-space: nowrap;
    }
  }

  .comment-input {
    :deep(.el-textarea__inner) {
      font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
      letter-spacing: 0.5px;
      line-height: 1.6;
    }
  }
}

.user-ratings-list {
  .sub-title {
    font-size: $font-size-lg;
    color: $text-color;
    margin-bottom: $spacing-md;
    font-family: $font-family-title;
  }
}

.rating-item {
  padding: $spacing-md;
  border-bottom: 1px solid $border-color-light;

  &:last-child {
    border-bottom: none;
  }
}

.rating-item-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-sm;
}

.rating-username {
  font-size: $font-size-base;
  color: $text-color;
  font-weight: 600;
}

.rating-time {
  font-size: $font-size-sm;
  color: $text-color-light;
  margin-left: auto;
}

.rating-comment {
  p {
    font-size: $font-size-base;
    color: $text-color-secondary;
    line-height: $line-height-loose;
    margin: 0;
  }
}
</style>