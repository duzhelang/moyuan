<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { ForumPost, Comment } from '@/types/model'
import { getForumPostList, likeForumPost, getComments, createComment, likeComment } from '@/api/modules/forum'
import { uploadFile } from '@/api/modules/file'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const posts = ref<ForumPost[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const expandedPostId = ref<number | null>(null)
const postComments = ref<Record<number, Comment[]>>({})
const commentInputs = ref<Record<number, string>>({})
const submittingComment = ref<Record<number, boolean>>({})
const commentTotals = ref<Record<number, number>>({})

const showPostForm = ref(false)
const newPostTitle = ref('')
const newPostContent = ref('')
const newPostImages = ref<string[]>([])
const uploading = ref(false)
const submittingPost = ref(false)

const imageUploadRef = ref<HTMLInputElement | null>(null)

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - d.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  const diffHour = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  if (diffHour < 24) return `${diffHour}小时前`
  if (diffDay < 7) return `${diffDay}天前`
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

const fetchPosts = async () => {
  loading.value = true
  try {
    const res = await getForumPostList({
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    posts.value = res.data.list
    total.value = res.data.total
  } catch {
    ElMessage.error('获取动态失败')
  } finally {
    loading.value = false
  }
}

const toggleComments = async (postId: number) => {
  if (expandedPostId.value === postId) {
    expandedPostId.value = null
    return
  }
  expandedPostId.value = postId
  if (!postComments.value[postId]) {
    try {
      const res = await getComments(postId, 2, { pageNum: 1, pageSize: 20 })
      postComments.value[postId] = res.data.list
      commentTotals.value[postId] = res.data.total
    } catch {
      ElMessage.error('获取评论失败')
    }
  }
}

const handleLike = async (post: ForumPost) => {
  try {
    await likeForumPost(post.id)
    post.likeCount++
    ElMessage.success('点赞成功')
  } catch {
    ElMessage.error('点赞失败')
  }
}

const handleSubmitComment = async (postId: number) => {
  const content = commentInputs.value[postId]
  if (!content?.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  submittingComment.value[postId] = true
  try {
    await createComment({ content, postId, targetType: 2 })
    ElMessage.success('评论成功')
    commentInputs.value[postId] = ''
    const res = await getComments(postId, 2, { pageNum: 1, pageSize: 20 })
    postComments.value[postId] = res.data.list
    commentTotals.value[postId] = res.data.total
    const post = posts.value.find(p => p.id === postId)
    if (post) post.commentCount++
  } catch {
    ElMessage.error('评论失败')
  } finally {
    submittingComment.value[postId] = false
  }
}

const handleCommentLike = async (comment: Comment) => {
  try {
    await likeComment(comment.id)
    comment.likeCount++
  } catch {
    ElMessage.error('点赞失败')
  }
}

const handleImageUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (!files) return
  uploading.value = true
  try {
    for (let i = 0; i < files.length; i++) {
      const res = await uploadFile(files[i])
      newPostImages.value.push(res.data.url)
    }
  } catch {
    ElMessage.error('图片上传失败')
  } finally {
    uploading.value = false
    if (imageUploadRef.value) imageUploadRef.value.value = ''
  }
}

const removeImage = (index: number) => {
  newPostImages.value.splice(index, 1)
}

const handleSubmitPost = async () => {
  if (!newPostTitle.value.trim() || !newPostContent.value.trim()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  submittingPost.value = true
  try {
    let content = newPostContent.value
    if (newPostImages.value.length > 0) {
      content += '\n' + newPostImages.value.map(url => `[img]${url}[/img]`).join('\n')
    }
    const { createForumPost } = await import('@/api/modules/forum')
    await createForumPost({ title: newPostTitle.value, content })
    ElMessage.success('发布成功')
    showPostForm.value = false
    newPostTitle.value = ''
    newPostContent.value = ''
    newPostImages.value = []
    currentPage.value = 1
    fetchPosts()
  } catch {
    ElMessage.error('发布失败')
  } finally {
    submittingPost.value = false
  }
}

const goToDetail = (id: number) => {
  router.push(`/forum/${id}`)
}

const goToLogin = () => {
  router.push('/user/login')
}

onMounted(() => {
  fetchPosts()
})
</script>

<template>
  <div class="communicate-page">
    <div class="star-bg"></div>

    <div class="communicate-container">
      <div class="page-header-bar">
        <h1 class="page-title">交流广场</h1>
        <p class="page-subtitle">分享你的诗词感悟，与同好交流心得</p>
      </div>

      <div class="post-compose-card" v-if="userStore.isLoggedIn">
        <div class="compose-trigger" @click="showPostForm = !showPostForm">
          <el-avatar :src="userStore.avatar" :size="42">
            {{ userStore.username?.charAt(0)?.toUpperCase() }}
          </el-avatar>
          <span class="compose-placeholder">分享你的想法...</span>
          <div class="compose-actions">
            <el-icon class="compose-icon"><Edit /></el-icon>
          </div>
        </div>

        <div class="compose-form" v-show="showPostForm">
          <el-input
            v-model="newPostTitle"
            placeholder="标题"
            maxlength="50"
            class="compose-title-input"
          />
          <el-input
            v-model="newPostContent"
            type="textarea"
            :rows="4"
            placeholder="写下你想分享的内容..."
            maxlength="1000"
            show-word-limit
            class="compose-content-input"
          />

          <div class="compose-images" v-if="newPostImages.length > 0">
            <div
              v-for="(img, idx) in newPostImages"
              :key="idx"
              class="compose-image-item"
            >
              <img :src="img" alt="">
              <span class="compose-image-remove" @click="removeImage(idx)">×</span>
            </div>
          </div>

          <div class="compose-footer">
            <div class="compose-tools">
              <input
                ref="imageUploadRef"
                type="file"
                accept="image/*"
                multiple
                style="display: none;"
                @change="handleImageUpload"
              >
              <button class="tool-btn" @click="imageUploadRef?.click()" :disabled="uploading">
                <el-icon><Picture /></el-icon>
                <span>{{ uploading ? '上传中...' : '图片' }}</span>
              </button>
            </div>
            <el-button
              type="primary"
              :loading="submittingPost"
              @click="handleSubmitPost"
              class="compose-submit-btn"
            >
              发布
            </el-button>
          </div>
        </div>
      </div>

      <div class="post-compose-card login-hint" v-else @click="goToLogin">
        <el-avatar :size="42" class="login-avatar">
          <el-icon><User /></el-icon>
        </el-avatar>
        <span class="compose-placeholder">登录后即可发表动态...</span>
      </div>

      <div class="feed-list" v-loading="loading">
        <div
          v-for="post in posts"
          :key="post.id"
          class="feed-card"
        >
          <div class="feed-header">
            <el-avatar :src="post.avatar" :size="44" class="feed-avatar">
              {{ post.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <div class="feed-user-info">
              <span class="feed-username">{{ post.username }}</span>
              <span class="feed-time">{{ formatDate(post.createTime) }}</span>
            </div>
          </div>

          <div class="feed-body" @click="goToDetail(post.id)">
            <h3 class="feed-title" v-if="post.title">{{ post.title }}</h3>
            <p class="feed-content">{{ post.content }}</p>
          </div>

          <div class="feed-actions">
            <button class="action-btn" @click="handleLike(post)">
              <el-icon><Star /></el-icon>
              <span>{{ post.likeCount || '赞' }}</span>
            </button>
            <button class="action-btn" @click="toggleComments(post.id)">
              <el-icon><ChatDotRound /></el-icon>
              <span>{{ post.commentCount || '评论' }}</span>
            </button>
            <button class="action-btn">
              <el-icon><View /></el-icon>
              <span>{{ post.viewCount }}</span>
            </button>
          </div>

          <div class="comment-section" v-if="expandedPostId === post.id">
            <div class="comment-input-bar" v-if="userStore.isLoggedIn">
              <el-avatar :src="userStore.avatar" :size="32" class="comment-user-avatar">
                {{ userStore.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <input
                v-model="commentInputs[post.id]"
                class="comment-input"
                placeholder="写评论..."
                @keyup.enter="handleSubmitComment(post.id)"
              >
              <button
                class="comment-submit-btn"
                @click="handleSubmitComment(post.id)"
                :disabled="submittingComment[post.id]"
              >
                发送
              </button>
            </div>
            <div class="comment-input-bar login-comment-hint" v-else @click="goToLogin">
              <span>登录后参与评论</span>
            </div>

            <div class="comment-list" v-if="postComments[post.id]?.length">
              <div
                v-for="comment in postComments[post.id]"
                :key="comment.id"
                class="comment-item"
              >
                <el-avatar :src="comment.avatar" :size="30" class="comment-avatar">
                  {{ comment.username?.charAt(0)?.toUpperCase() }}
                </el-avatar>
                <div class="comment-body">
                  <div class="comment-meta">
                    <span class="comment-author">{{ comment.username }}</span>
                    <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
                  </div>
                  <p class="comment-text">{{ comment.content }}</p>
                  <div class="comment-actions">
                    <button class="comment-action-btn" @click="handleCommentLike(comment)">
                      <el-icon><Star /></el-icon>
                      {{ comment.likeCount || '' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div class="comment-empty" v-else-if="commentTotals[post.id] === 0">
              <span>暂无评论，快来抢沙发吧</span>
            </div>
          </div>
        </div>

        <div class="empty-state" v-if="!loading && posts.length === 0">
          <el-icon class="empty-icon"><ChatDotRound /></el-icon>
          <p>暂无动态</p>
          <p class="empty-hint">快来发布第一条动态吧</p>
        </div>
      </div>

      <div class="pagination-bar" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="fetchPosts"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.communicate-page {
  min-height: 100vh;
  position: relative;
  background: #0a1628;
  overflow: hidden;
}

.star-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(15, 32, 65, 0.9) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 20%, rgba(20, 40, 80, 0.8) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 80%, rgba(10, 25, 55, 0.85) 0%, transparent 55%),
    linear-gradient(180deg, #060d1f 0%, #0c1a35 30%, #0f2040 60%, #0a1628 100%);
  z-index: 0;

  &::before,
  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
  }

  &::before {
    background-image:
      radial-gradient(1px 1px at 10% 15%, rgba(255, 255, 255, 0.6), transparent),
      radial-gradient(1px 1px at 25% 35%, rgba(255, 255, 255, 0.4), transparent),
      radial-gradient(1.5px 1.5px at 40% 10%, rgba(255, 255, 255, 0.7), transparent),
      radial-gradient(1px 1px at 55% 45%, rgba(255, 255, 255, 0.3), transparent),
      radial-gradient(1px 1px at 70% 20%, rgba(255, 255, 255, 0.5), transparent),
      radial-gradient(1.5px 1.5px at 85% 55%, rgba(255, 255, 255, 0.6), transparent),
      radial-gradient(1px 1px at 15% 60%, rgba(255, 255, 255, 0.35), transparent),
      radial-gradient(1px 1px at 30% 80%, rgba(255, 255, 255, 0.45), transparent),
      radial-gradient(1.5px 1.5px at 50% 70%, rgba(255, 255, 255, 0.55), transparent),
      radial-gradient(1px 1px at 65% 90%, rgba(255, 255, 255, 0.3), transparent),
      radial-gradient(1px 1px at 80% 75%, rgba(255, 255, 255, 0.4), transparent),
      radial-gradient(1px 1px at 95% 40%, rgba(255, 255, 255, 0.5), transparent),
      radial-gradient(1px 1px at 5% 95%, rgba(255, 255, 255, 0.35), transparent),
      radial-gradient(1px 1px at 45% 5%, rgba(255, 255, 255, 0.6), transparent),
      radial-gradient(1px 1px at 75% 60%, rgba(255, 255, 255, 0.25), transparent);
    animation: twinkle 4s ease-in-out infinite alternate;
  }

  &::after {
    background-image:
      radial-gradient(1px 1px at 12% 25%, rgba(255, 255, 255, 0.3), transparent),
      radial-gradient(1.5px 1.5px at 28% 45%, rgba(255, 255, 255, 0.5), transparent),
      radial-gradient(1px 1px at 42% 65%, rgba(255, 255, 255, 0.25), transparent),
      radial-gradient(1px 1px at 58% 15%, rgba(255, 255, 255, 0.4), transparent),
      radial-gradient(1px 1px at 72% 85%, rgba(255, 255, 255, 0.35), transparent),
      radial-gradient(1px 1px at 88% 35%, rgba(255, 255, 255, 0.45), transparent),
      radial-gradient(1px 1px at 35% 55%, rgba(255, 255, 255, 0.3), transparent),
      radial-gradient(1.5px 1.5px at 62% 72%, rgba(255, 255, 255, 0.5), transparent),
      radial-gradient(1px 1px at 8% 48%, rgba(255, 255, 255, 0.2), transparent),
      radial-gradient(1px 1px at 92% 8%, rgba(255, 255, 255, 0.4), transparent);
    animation: twinkle 5s ease-in-out infinite alternate-reverse;
  }
}

@keyframes twinkle {
  0% { opacity: 0.6; }
  100% { opacity: 1; }
}

.communicate-container {
  position: relative;
  z-index: 1;
  max-width: 680px;
  margin: 0 auto;
  padding: 30px 16px 60px;
}

.page-header-bar {
  text-align: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #e8d5b7;
  font-family: cursive;
  margin: 0 0 8px;
  text-shadow: 0 2px 10px rgba(232, 213, 183, 0.3);
}

.page-subtitle {
  font-size: 14px;
  color: rgba(200, 200, 210, 0.6);
  margin: 0;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.post-compose-card {
  background: rgba(20, 35, 65, 0.7);
  border: 1px solid rgba(100, 140, 200, 0.15);
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 24px;
  backdrop-filter: blur(12px);
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(100, 140, 200, 0.25);
  }

  &.login-hint {
    cursor: pointer;

    &:hover {
      border-color: rgba(100, 140, 200, 0.35);
      background: rgba(25, 42, 75, 0.8);
    }
  }
}

.compose-trigger {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.compose-placeholder {
  flex: 1;
  font-size: 14px;
  color: rgba(180, 185, 200, 0.5);
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.compose-icon {
  font-size: 18px;
  color: rgba(180, 185, 200, 0.4);
}

.login-avatar {
  background: rgba(60, 80, 120, 0.5);
  color: rgba(180, 185, 200, 0.5);
}

.compose-form {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(100, 140, 200, 0.1);

  :deep(.el-input__inner),
  :deep(.el-textarea__inner) {
    background: rgba(15, 25, 50, 0.6);
    border: 1px solid rgba(100, 140, 200, 0.12);
    color: rgba(220, 225, 240, 0.9);
    font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
    border-radius: 8px;

    &::placeholder {
      color: rgba(160, 170, 190, 0.4);
    }

    &:focus {
      border-color: rgba(100, 160, 240, 0.3);
    }
  }

  :deep(.el-textarea__inner) {
    resize: none;
  }

  :deep(.el-input__count-inner) {
    background: transparent;
    color: rgba(160, 170, 190, 0.5);
  }
}

.compose-title-input {
  margin-bottom: 12px;
}

.compose-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.compose-image-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.compose-image-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
  line-height: 1;
}

.compose-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.compose-tools {
  display: flex;
  gap: 8px;
}

.tool-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: rgba(60, 80, 120, 0.3);
  border: 1px solid rgba(100, 140, 200, 0.15);
  border-radius: 8px;
  color: rgba(180, 190, 210, 0.7);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;

  &:hover {
    background: rgba(60, 80, 120, 0.5);
    border-color: rgba(100, 140, 200, 0.3);
    color: rgba(200, 210, 230, 0.9);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.compose-submit-btn {
  background: linear-gradient(135deg, rgba(70, 130, 200, 0.8), rgba(50, 100, 180, 0.9));
  border: none;
  color: #fff;
  padding: 8px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: linear-gradient(135deg, rgba(80, 145, 220, 0.9), rgba(60, 115, 200, 1));
    transform: translateY(-1px);
    box-shadow: 0 4px 15px rgba(50, 100, 180, 0.4);
  }
}

.feed-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 200px;
}

.feed-card {
  background: rgba(18, 32, 60, 0.75);
  border: 1px solid rgba(80, 120, 180, 0.12);
  border-radius: 14px;
  padding: 20px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(80, 120, 180, 0.25);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
  }
}

.feed-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.feed-avatar {
  flex-shrink: 0;
  border: 2px solid rgba(100, 160, 240, 0.2);
  background: rgba(40, 70, 120, 0.5);
  color: rgba(200, 210, 230, 0.8);
}

.feed-user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.feed-username {
  font-size: 15px;
  font-weight: 600;
  color: #e8c87a;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.feed-time {
  font-size: 12px;
  color: rgba(160, 170, 190, 0.5);
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.feed-body {
  cursor: pointer;
  margin-bottom: 14px;
}

.feed-title {
  font-size: 17px;
  font-weight: 600;
  color: rgba(220, 230, 250, 0.95);
  margin: 0 0 8px;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
  line-height: 1.4;
}

.feed-content {
  font-size: 14px;
  color: rgba(200, 210, 230, 0.75);
  line-height: 1.7;
  margin: 0;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
  white-space: pre-line;
  word-break: break-word;
}

.feed-actions {
  display: flex;
  gap: 4px;
  padding-top: 12px;
  border-top: 1px solid rgba(80, 120, 180, 0.08);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: rgba(160, 175, 200, 0.6);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;

  &:hover {
    background: rgba(80, 120, 180, 0.1);
    color: rgba(180, 200, 230, 0.9);
  }

  .el-icon {
    font-size: 16px;
  }
}

.comment-section {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(80, 120, 180, 0.08);
}

.comment-input-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;

  &.login-comment-hint {
    cursor: pointer;
    padding: 10px 14px;
    background: rgba(40, 60, 100, 0.3);
    border-radius: 8px;
    justify-content: center;

    span {
      font-size: 13px;
      color: rgba(160, 175, 200, 0.5);
      font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
    }

    &:hover {
      background: rgba(40, 60, 100, 0.5);
    }
  }
}

.comment-user-avatar {
  flex-shrink: 0;
  background: rgba(40, 70, 120, 0.5);
  color: rgba(200, 210, 230, 0.8);
}

.comment-input {
  flex: 1;
  height: 36px;
  padding: 0 14px;
  background: rgba(15, 25, 50, 0.6);
  border: 1px solid rgba(100, 140, 200, 0.12);
  border-radius: 18px;
  color: rgba(220, 225, 240, 0.9);
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s ease;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;

  &::placeholder {
    color: rgba(160, 170, 190, 0.4);
  }

  &:focus {
    border-color: rgba(100, 160, 240, 0.3);
  }
}

.comment-submit-btn {
  padding: 6px 16px;
  background: rgba(70, 130, 200, 0.6);
  border: none;
  border-radius: 16px;
  color: rgba(230, 240, 255, 0.9);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;

  &:hover {
    background: rgba(80, 145, 220, 0.8);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(15, 25, 48, 0.5);
  border-radius: 10px;
  border: 1px solid rgba(80, 120, 180, 0.06);
}

.comment-avatar {
  flex-shrink: 0;
  background: rgba(50, 80, 130, 0.5);
  color: rgba(200, 210, 230, 0.7);
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-author {
  font-size: 13px;
  font-weight: 500;
  color: rgba(180, 200, 230, 0.8);
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.comment-time {
  font-size: 11px;
  color: rgba(140, 155, 180, 0.4);
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.comment-text {
  font-size: 13px;
  color: rgba(200, 210, 230, 0.7);
  line-height: 1.6;
  margin: 0;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.comment-action-btn {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 2px 6px;
  background: transparent;
  border: none;
  border-radius: 4px;
  color: rgba(150, 165, 190, 0.5);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    color: rgba(180, 200, 230, 0.8);
    background: rgba(80, 120, 180, 0.1);
  }

  .el-icon {
    font-size: 13px;
  }
}

.comment-empty {
  text-align: center;
  padding: 16px;
  color: rgba(150, 165, 190, 0.4);
  font-size: 13px;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: rgba(160, 175, 200, 0.5);

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.4;
  }

  p {
    margin: 0 0 8px;
    font-size: 16px;
    font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
  }

  .empty-hint {
    font-size: 13px;
    color: rgba(150, 165, 190, 0.35);
  }
}

.pagination-bar {
  margin-top: 30px;
  display: flex;
  justify-content: center;

  :deep(.el-pagination) {
    --el-pagination-bg-color: rgba(20, 35, 65, 0.7);
    --el-pagination-text-color: rgba(180, 195, 220, 0.7);
    --el-pagination-button-bg-color: rgba(20, 35, 65, 0.7);
    --el-pagination-button-color: rgba(180, 195, 220, 0.7);
    --el-pagination-hover-color: rgba(100, 160, 240, 0.8);
  }

  :deep(.el-pager li) {
    background: rgba(20, 35, 65, 0.7);
    color: rgba(180, 195, 220, 0.7);
    border-radius: 6px;
    margin: 0 3px;

    &.is-active {
      background: rgba(70, 130, 200, 0.7);
      color: #fff;
    }
  }
}
</style>
