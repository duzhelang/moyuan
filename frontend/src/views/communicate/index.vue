<script setup lang="ts">
import { ref, onMounted, onActivated } from 'vue'
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
const likedPostIds = ref<Set<number>>(new Set())

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
    if (likedPostIds.value.has(post.id)) {
      likedPostIds.value.delete(post.id)
      post.likeCount = Math.max(0, post.likeCount - 1)
    } else {
      likedPostIds.value.add(post.id)
      post.likeCount++
    }
  } catch {
    ElMessage.error('操作失败')
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
    await createComment({ content, targetId: postId, targetType: 2 })
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

onActivated(() => {
  currentPage.value = 1
  fetchPosts()
})
</script>

<template>
  <div class="communicate-page">
    <div class="star-bg"></div>

    <div class="communicate-container">
      <div class="page-nav-bar">
        <button class="nav-btn" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </button>
        <span class="nav-sep">/</span>
        <button class="nav-btn" @click="router.push('/')">
          <el-icon><HomeFilled /></el-icon>
          首页
        </button>
        <span class="nav-sep">/</span>
        <button class="nav-btn" @click="router.push('/forum')">
          诗汇论坛
        </button>
        <span class="nav-sep">/</span>
        <span class="nav-current">交流广场</span>
      </div>

      <div class="page-header-bar">
        <h1 class="page-title">交流广场</h1>
        <p class="page-subtitle">分享你的诗词感悟，与同好交流心得</p>
      </div>

      <div class="post-compose-card fade-slide-up" v-if="userStore.isLoggedIn">
        <div class="compose-trigger" @click="showPostForm = !showPostForm">
          <el-avatar :src="userStore.avatar" :size="42">
            {{ userStore.username?.charAt(0)?.toUpperCase() }}
          </el-avatar>
          <span class="compose-placeholder">分享你的想法...</span>
          <div class="compose-actions">
            <el-icon class="compose-icon" :class="{ 'rotate-icon': showPostForm }"><Edit /></el-icon>
          </div>
        </div>

        <transition name="expand">
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
        </transition>
      </div>

      <div class="post-compose-card login-hint" v-else @click="goToLogin">
        <el-avatar :size="42" class="login-avatar">
          <el-icon><User /></el-icon>
        </el-avatar>
        <span class="compose-placeholder">登录后即可发表动态...</span>
      </div>

      <div class="feed-list" v-loading="loading">
        <div
          v-for="(post, index) in posts"
          :key="post.id"
          class="feed-card fade-slide-up"
          :style="{ animationDelay: `${index * 0.06}s` }"
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
/* ==================== 入场动画 ==================== */
@keyframes fadeSlideUp {
  from {
    opacity: 0;
    transform: translateY(24px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes headerFadeIn {
  from {
    opacity: 0;
    transform: translateY(-16px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes twinkle {
  0% { opacity: 0.5; }
  50% { opacity: 1; }
  100% { opacity: 0.6; }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

@keyframes shimmer {
  0% { background-position: -200% center; }
  100% { background-position: 200% center; }
}

@keyframes gentlePulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(100, 160, 240, 0); }
  50% { box-shadow: 0 0 20px 2px rgba(100, 160, 240, 0.08); }
}

.fade-slide-up {
  animation: fadeSlideUp 0.5s ease-out both;
}

/* ==================== 页面基础 ==================== */
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
    animation: twinkle 4s ease-in-out infinite;
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
    animation: twinkle 5s ease-in-out infinite reverse;
  }
}

/* ==================== 容器 ==================== */
.communicate-container {
  position: relative;
  z-index: 1;
  max-width: 680px;
  margin: 0 auto;
  padding: 30px 16px 60px;
}

/* ==================== 页头 ==================== */
.page-nav-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  animation: headerFadeIn 0.6s ease-out;

  .nav-btn {
    background: none;
    border: none;
    color: rgba(180, 195, 220, 0.6);
    font-size: 13px;
    cursor: pointer;
    padding: 4px 8px;
    border-radius: 6px;
    transition: all 0.25s ease;
    display: flex;
    align-items: center;
    gap: 4px;
    font-family: "PingFang SC", "Microsoft YaHei", sans-serif;

    &:hover {
      color: #e8d5b7;
      background: rgba(100, 150, 220, 0.08);
    }
  }

  .nav-sep {
    color: rgba(150, 165, 190, 0.3);
    font-size: 12px;
  }

  .nav-current {
    color: rgba(200, 210, 230, 0.5);
    font-size: 13px;
    font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
  }
}

.page-header-bar {
  text-align: center;
  margin-bottom: 30px;
  animation: headerFadeIn 0.6s ease-out;
  position: relative;

  &::after {
    content: '';
    display: block;
    width: 80px;
    height: 2px;
    background: linear-gradient(90deg, transparent, #d4af87, transparent);
    margin: 16px auto 0;
    border-radius: 2px;
  }
}

.page-title {
  font-size: 30px;
  font-weight: 700;
  color: #e8d5b7;
  font-family: cursive;
  margin: 0 0 8px;
  text-shadow: 0 2px 12px rgba(232, 213, 183, 0.35);
  letter-spacing: 2px;
}

.page-subtitle {
  font-size: 14px;
  color: rgba(200, 210, 225, 0.7);
  margin: 0;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

/* ==================== 发布卡片 ==================== */
.post-compose-card {
  background: linear-gradient(135deg, rgba(20, 38, 72, 0.75), rgba(16, 30, 58, 0.85));
  border: 1px solid rgba(100, 150, 220, 0.15);
  border-radius: 16px;
  padding: 18px;
  margin-bottom: 24px;
  backdrop-filter: blur(16px);
  transition: all 0.35s ease;
  overflow: hidden;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;

  &:hover {
    border-color: rgba(100, 150, 220, 0.3);
    box-shadow: 0 6px 30px rgba(0, 0, 0, 0.25), 0 0 40px rgba(80, 140, 220, 0.06);
  }

  &.login-hint {
    cursor: pointer;

    &:hover {
      border-color: rgba(100, 150, 220, 0.35);
      background: linear-gradient(135deg, rgba(25, 45, 80, 0.85), rgba(18, 35, 65, 0.9));
      transform: translateY(-2px);
      box-shadow: 0 8px 25px rgba(0, 0, 0, 0.3);
    }
  }
}

/* ==================== 触发栏 ==================== */
.compose-trigger {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 4px 0;
  transition: all 0.2s ease;

  &:hover {
    .compose-placeholder {
      color: rgba(200, 210, 230, 0.7);
    }
  }
}

.compose-placeholder {
  flex: 1;
  font-size: 14px;
  color: rgba(180, 190, 210, 0.55);
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
  transition: color 0.25s ease;
}

.compose-icon {
  font-size: 18px;
  color: rgba(180, 190, 210, 0.4);
  transition: transform 0.35s ease, color 0.25s ease;
}

.rotate-icon {
  transform: rotate(45deg);
  color: rgba(120, 170, 240, 0.7);
}

.login-avatar {
  background: rgba(60, 80, 120, 0.5);
  color: rgba(180, 190, 210, 0.5);
}

/* ==================== 发布表单 ==================== */
.compose-form {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(100, 140, 200, 0.1);
  overflow: hidden;
  box-sizing: border-box;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  position: relative;

  :deep(.el-input) {
    display: block;
    width: 100%;
    max-width: 100%;
    min-width: 0;
  }

  :deep(.el-input__prefix) {
    display: none !important;
  }

  :deep(.el-input__wrapper) {
    background: rgba(12, 22, 45, 0.7);
    box-shadow: none;
    border: 1px solid rgba(100, 140, 200, 0.15);
    border-radius: 10px;
    padding: 0 14px;
    width: 100% !important;
    max-width: 100% !important;
    box-sizing: border-box !important;
    transition: border-color 0.3s ease, box-shadow 0.3s ease;

    &:hover {
      border-color: rgba(100, 140, 200, 0.28);
    }

    &:focus-within {
      border-color: rgba(100, 160, 240, 0.4);
      box-shadow: 0 0 12px rgba(80, 140, 220, 0.1);
    }
  }

  :deep(.el-input__inner) {
    background: transparent;
    border: none;
    box-shadow: none;
    color: rgba(220, 230, 245, 0.92);
    font-size: 14px;
    line-height: 1.6;
    width: 100%;
    max-width: 100%;

    &::placeholder {
      color: rgba(160, 175, 200, 0.45);
    }
  }

  :deep(.el-textarea) {
    display: block !important;
    width: 100% !important;
    max-width: 100% !important;
    min-width: 0 !important;
    position: relative;
    overflow: hidden;
  }

  :deep(.el-textarea__inner) {
    background: rgba(12, 22, 45, 0.7);
    box-shadow: none;
    border: 1px solid rgba(100, 140, 200, 0.15);
    border-radius: 10px;
    color: rgba(220, 230, 245, 0.92);
    font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
    font-size: 14px;
    line-height: 1.7;
    resize: none;
    width: 100% !important;
    max-width: 100% !important;
    min-width: 0 !important;
    box-sizing: border-box !important;
    display: block;
    margin: 0 !important;
    overflow: hidden;
    transition: border-color 0.3s ease, box-shadow 0.3s ease;

    &::placeholder {
      color: rgba(160, 175, 200, 0.45);
    }

    &:hover {
      border-color: rgba(100, 140, 200, 0.28);
    }

    &:focus {
      border-color: rgba(100, 160, 240, 0.4);
      box-shadow: 0 0 12px rgba(80, 140, 220, 0.1);
    }
  }

  :deep(.el-input__count-inner) {
    background: transparent;
    color: rgba(160, 175, 195, 0.55);
    font-size: 12px;
  }
}

.compose-title-input {
  margin-bottom: 12px;
  width: 100%;
}

.compose-content-input {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;

  :deep(.el-textarea) {
    width: 100% !important;
    max-width: 100% !important;
    min-width: 0 !important;
  }
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
  border-radius: 10px;
  overflow: hidden;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.05);
  }

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
  transition: background 0.2s ease;

  &:hover {
    background: rgba(220, 60, 60, 0.8);
  }
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
  background: rgba(50, 75, 115, 0.35);
  border: 1px solid rgba(100, 140, 200, 0.18);
  border-radius: 10px;
  color: rgba(180, 195, 215, 0.75);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;

  &:hover {
    background: rgba(60, 90, 140, 0.45);
    border-color: rgba(100, 150, 220, 0.3);
    color: rgba(210, 220, 240, 0.9);
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
  }
}

.compose-submit-btn {
  background: linear-gradient(135deg, rgba(65, 125, 200, 0.85), rgba(45, 95, 175, 0.95));
  border: none;
  color: #fff;
  padding: 9px 28px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  letter-spacing: 1px;

  &:hover {
    background: linear-gradient(135deg, rgba(75, 140, 215, 0.95), rgba(55, 110, 195, 1));
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(50, 100, 180, 0.4);
  }

  &:active {
    transform: translateY(0);
  }
}

/* ==================== 展开/折叠过渡 ==================== */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  max-height: 400px;
  opacity: 1;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
  margin-top: 0;
  padding-top: 0;
  border-top-width: 0;
}

/* ==================== 动态列表 ==================== */
.feed-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 200px;
}

.feed-card {
  background: linear-gradient(145deg, rgba(18, 34, 64, 0.8), rgba(14, 28, 54, 0.9));
  border: 1px solid rgba(80, 125, 185, 0.14);
  border-radius: 16px;
  padding: 22px;
  backdrop-filter: blur(12px);
  transition: all 0.35s ease;
  animation: gentlePulse 6s ease-in-out infinite;
  animation-delay: inherit;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;

  &:hover {
    border-color: rgba(80, 125, 185, 0.3);
    box-shadow: 0 6px 28px rgba(0, 0, 0, 0.25), 0 0 30px rgba(80, 140, 220, 0.05);
    transform: translateY(-2px);
    animation-play-state: paused;
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
  border: 2px solid rgba(100, 160, 240, 0.22);
  background: rgba(40, 70, 120, 0.55);
  color: rgba(200, 215, 235, 0.85);
  transition: border-color 0.3s ease;

  &:hover {
    border-color: rgba(100, 170, 250, 0.4);
  }
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
  text-shadow: 0 1px 6px rgba(232, 200, 122, 0.15);
}

.feed-time {
  font-size: 12px;
  color: rgba(160, 175, 200, 0.55);
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.feed-body {
  cursor: pointer;
  margin-bottom: 14px;
  padding: 4px 0;
  border-radius: 8px;
  transition: background 0.2s ease;

  &:hover {
    background: rgba(80, 130, 200, 0.04);
  }
}

.feed-title {
  font-size: 17px;
  font-weight: 600;
  color: rgba(225, 235, 255, 0.95);
  margin: 0 0 8px;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
  line-height: 1.4;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
}

.feed-content {
  font-size: 14px;
  color: rgba(205, 215, 240, 0.8);
  line-height: 1.75;
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
  border-radius: 10px;
  color: rgba(160, 180, 210, 0.6);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;

  &:hover {
    background: rgba(80, 125, 200, 0.12);
    color: rgba(190, 210, 240, 0.95);
    transform: translateY(-1px);
  }

  &:active {
    transform: scale(0.95);
  }

  .el-icon {
    font-size: 16px;
    transition: transform 0.25s ease;
  }

  &:hover .el-icon {
    transform: scale(1.15);
  }
}

/* ==================== 评论区 ==================== */
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
    border-radius: 10px;
    justify-content: center;
    transition: background 0.25s ease;

    span {
      font-size: 13px;
      color: rgba(160, 180, 210, 0.5);
      font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
    }

    &:hover {
      background: rgba(40, 65, 110, 0.45);
    }
  }
}

.comment-user-avatar {
  flex-shrink: 0;
  background: rgba(40, 70, 120, 0.55);
  color: rgba(200, 215, 235, 0.8);
}

.comment-input {
  @include comment-input-dark;
}

.comment-submit-btn {
  padding: 6px 18px;
  background: rgba(65, 125, 200, 0.65);
  border: none;
  border-radius: 18px;
  color: rgba(235, 242, 255, 0.92);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;

  &:hover {
    background: rgba(75, 140, 215, 0.85);
    transform: translateY(-1px);
    box-shadow: 0 3px 12px rgba(50, 100, 180, 0.3);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
  }
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  background: rgba(14, 24, 50, 0.55);
  border-radius: 12px;
  border: 1px solid rgba(80, 120, 180, 0.07);
  transition: background 0.2s ease;

  &:hover {
    background: rgba(18, 30, 58, 0.65);
  }
}

.comment-avatar {
  flex-shrink: 0;
  background: rgba(50, 80, 130, 0.5);
  color: rgba(200, 215, 235, 0.7);
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
  color: rgba(185, 205, 235, 0.85);
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.comment-time {
  font-size: 11px;
  color: rgba(140, 155, 185, 0.45);
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.comment-text {
  font-size: 13px;
  color: rgba(205, 215, 240, 0.75);
  line-height: 1.65;
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
  border-radius: 6px;
  color: rgba(155, 170, 195, 0.5);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    color: rgba(190, 210, 240, 0.85);
    background: rgba(80, 125, 200, 0.1);
  }

  .el-icon {
    font-size: 13px;
  }
}

.comment-empty {
  text-align: center;
  padding: 16px;
  color: rgba(155, 170, 200, 0.45);
  font-size: 13px;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: rgba(165, 180, 210, 0.55);

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.45;
    animation: float 3s ease-in-out infinite;
  }

  p {
    margin: 0 0 8px;
    font-size: 16px;
    font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
  }

  .empty-hint {
    font-size: 13px;
    color: rgba(155, 170, 200, 0.38);
  }
}

/* ==================== 分页 ==================== */
.pagination-bar {
  margin-top: 30px;
  display: flex;
  justify-content: center;
  animation: fadeSlideUp 0.5s ease-out 0.3s both;

  :deep(.el-pagination) {
    --el-pagination-bg-color: rgba(20, 35, 65, 0.7);
    --el-pagination-text-color: rgba(185, 200, 225, 0.75);
    --el-pagination-button-bg-color: rgba(20, 35, 65, 0.7);
    --el-pagination-button-color: rgba(185, 200, 225, 0.75);
    --el-pagination-hover-color: rgba(100, 165, 245, 0.85);
  }

  :deep(.el-pager li) {
    background: rgba(20, 35, 65, 0.7);
    color: rgba(185, 200, 225, 0.75);
    border-radius: 8px;
    margin: 0 3px;
    transition: all 0.25s ease;

    &:hover {
      color: rgba(210, 225, 250, 0.95);
    }

    &.is-active {
      background: rgba(65, 130, 205, 0.75);
      color: #fff;
      box-shadow: 0 2px 10px rgba(50, 100, 180, 0.3);
    }
  }
}
</style>


