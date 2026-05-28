<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePoemStore } from '@/stores/poem'
import { ElMessage } from 'element-plus'
import type { Poem, Comment } from '@/types/model'
import { likePoem, favoritePoem } from '@/api/modules/poem'
import { getComments, createComment } from '@/api/modules/forum'

const route = useRoute()
const router = useRouter()
const poemStore = usePoemStore()

const loading = ref(false)
const poem = computed(() => poemStore.currentPoem)
const comments = ref<Comment[]>([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentSize = ref(10)

const newComment = ref('')
const submittingComment = ref(false)

const poemId = computed(() => Number(route.params.id))

const fetchPoem = async () => {
  loading.value = true
  try {
    await poemStore.fetchPoemDetail(poemId.value)
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
      page: commentPage.value,
      size: commentSize.value
    })
    comments.value = res.data.records
    commentTotal.value = res.data.total
  } catch (error) {
    console.error('获取评论失败:', error)
  }
}

const handleLike = async () => {
  try {
    await likePoem(poemId.value)
    ElMessage.success('点赞成功')
    if (poem.value) {
      poem.value.likeCount++
    }
  } catch (error) {
    ElMessage.error('点赞失败')
  }
}

const handleFavorite = async () => {
  try {
    await favoritePoem(poemId.value)
    ElMessage.success('收藏成功')
    if (poem.value) {
      poem.value.favoriteCount++
    }
  } catch (error) {
    ElMessage.error('收藏失败')
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

const handleCommentPageChange = (page: number) => {
  commentPage.value = page
  fetchComments()
}

onMounted(() => {
  fetchPoem()
  fetchComments()
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
          <el-button type="primary" @click="handleLike">
            <el-icon><Star /></el-icon>
            点赞 ({{ poem.likeCount }})
          </el-button>
          <el-button type="warning" @click="handleFavorite">
            <el-icon><CollectionTag /></el-icon>
            收藏 ({{ poem.favoriteCount }})
          </el-button>
          <span class="view-count">
            <el-icon><View /></el-icon>
            {{ poem.viewCount }} 次浏览
          </span>
        </div>
      </el-card>
      
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
                <span class="comment-time">{{ comment.createdAt }}</span>
              </div>
            </div>
            <div class="comment-content">
              <p>{{ comment.content }}</p>
            </div>
            <div class="comment-actions">
              <el-button text size="small">
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
</style>