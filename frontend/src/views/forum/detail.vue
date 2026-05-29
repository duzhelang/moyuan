<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { ForumPost, Comment } from '@/types/model'
import { getForumPostById, likeForumPost, getComments, createComment, likeComment } from '@/api/modules/forum'
import { addHistory } from '@/api/modules/history'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const post = ref<ForumPost | null>(null)
const comments = ref<Comment[]>([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentSize = ref(10)

const newComment = ref('')
const submittingComment = ref(false)

const postId = computed(() => Number(route.params.id))

const fetchPost = async () => {
  loading.value = true
  try {
    const res = await getForumPostById(postId.value)
    post.value = res.data
    addHistory(route.params.id as unknown as number, 2).catch(() => {})
  } catch (error) {
    ElMessage.error('获取帖子详情失败')
    router.push('/forum')
  } finally {
    loading.value = false
  }
}

const fetchComments = async () => {
  try {
    const res = await getComments(postId.value, 2, {
      pageNum: commentPage.value,
      pageSize: commentSize.value
    })
    comments.value = res.data.list
    commentTotal.value = res.data.total
  } catch (error) {
    console.error('获取评论失败:', error)
  }
}

const handleLike = async () => {
  try {
    await likeForumPost(postId.value)
    ElMessage.success('点赞成功')
    if (post.value) {
      post.value.likeCount++
    }
  } catch (error) {
    ElMessage.error('点赞失败')
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
      targetId: postId.value,
      targetType: 2
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
  fetchPost()
  fetchComments()
})
</script>

<template>
  <div class="forum-detail-page" v-loading="loading">
    <div class="container" v-if="post">
      <div class="post-header">
        <el-button @click="router.push('/forum')" class="back-button">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>
      
      <el-card class="post-card">
        <div class="post-title-section">
          <h1 class="post-title">{{ post.title }}</h1>
          <div class="post-meta">
            <el-avatar :src="post.avatar" :size="48">
              {{ post.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <div class="meta-info">
              <span class="meta-author">{{ post.username }}</span>
              <span class="meta-time">{{ post.createTime }}</span>
            </div>
          </div>
        </div>
        
        <div class="post-content-section">
          <p>{{ post.content }}</p>
        </div>
        
        <div class="post-actions">
          <el-button type="primary" @click="handleLike">
            <el-icon><Star /></el-icon>
            点赞 ({{ post.likeCount }})
          </el-button>
          <span class="view-count">
            <el-icon><View /></el-icon>
            {{ post.viewCount }} 次浏览
          </span>
          <span class="comment-count">
            <el-icon><ChatDotRound /></el-icon>
            {{ post.commentCount }} 条评论
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
.forum-detail-page {
  padding: $spacing-xl 0;
  min-height: 60vh;
}

.post-header {
  margin-bottom: $spacing-lg;
}

.back-button {
  margin-bottom: $spacing-md;
}

.post-card {
  margin-bottom: $spacing-xl;
  border-radius: $border-radius-md;
}

.post-title-section {
  margin-bottom: $spacing-xl;
  padding-bottom: $spacing-xl;
  border-bottom: 1px solid $border-color;
}

.post-title {
  font-size: $font-size-hero;
  color: $text-color;
  margin-bottom: $spacing-lg;
}

.post-meta {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.meta-info {
  display: flex;
  flex-direction: column;
}

.meta-author {
  font-size: $font-size-lg;
  color: $primary-color;
  font-weight: 600;
}

.meta-time {
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.post-content-section {
  margin-bottom: $spacing-xl;
  
  p {
    font-size: $font-size-lg;
    line-height: $line-height-loose;
    color: $text-color;
    white-space: pre-line;
  }
}

.post-actions {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding-top: $spacing-xl;
  border-top: 1px solid $border-color;
}

.view-count,
.comment-count {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.comment-count {
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