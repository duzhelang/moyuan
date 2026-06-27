<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { ForumPost } from '@/types/model'
import { getForumPostList } from '@/api/modules/forum'
import { useParticles } from '@/composables/useParticles'

const router = useRouter()

const particleCanvasRef = ref<HTMLCanvasElement | null>(null)
useParticles(particleCanvasRef, {
  count: 60,
  colors: ['#d4af87', '#f0e4d7', '#c9a06c'],
  opacityRange: [0.15, 0.3],
  sizeRange: [1, 2.5]
})

const loading = ref(false)
const posts = ref<ForumPost[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const fetchPosts = async () => {
  loading.value = true
  try {
    const res = await getForumPostList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: keyword.value
    })
    posts.value = res.data.list
    total.value = res.data.total
  } catch (error) {
    ElMessage.error('获取帖子列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchPosts()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchPosts()
}

const goToDetail = (id: number) => {
  router.push(`/forum/${id}`)
}

const goToCreate = () => {
  router.push('/forum/create')
}

onMounted(() => {
  fetchPosts()
})
</script>

<template>
  <div class="forum-list-page">
    <canvas ref="particleCanvasRef" class="particle-bg"></canvas>
    <div class="container">
      <div class="page-nav">
        <el-button text @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-divider direction="vertical" />
        <el-button text @click="router.push('/')">
          <el-icon><HomeFilled /></el-icon>
          首页
        </el-button>
        <el-divider direction="vertical" />
        <span class="current-page">诗汇论坛</span>
      </div>

      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">诗汇论坛</h1>
          <p class="page-subtitle">以诗会友，畅谈古今</p>
        </div>
        <div class="header-right">
          <el-button @click="router.push('/communicate')">
            <el-icon><ChatDotSquare /></el-icon>
            交流广场
          </el-button>
          <el-button type="primary" @click="goToCreate">
            <el-icon><Edit /></el-icon>
            发帖
          </el-button>
        </div>
      </div>

      <div class="decorative-quote">
        <p class="quote-text">"诗者，志之所之也。在心为志，发言为诗。"</p>
        <span class="quote-source">——《毛诗序》</span>
      </div>
      
      <div class="search-section">
        <el-input
          v-model="keyword"
          placeholder="搜索帖子..."
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
      
      <div class="post-list" v-loading="loading">
        <el-empty v-if="!loading && posts.length === 0" description="暂无帖子" />
        
        <div
          v-for="post in posts"
          :key="post.id"
          class="post-card"
          @click="goToDetail(post.id)"
        >
          <div class="post-header">
            <el-avatar :src="post.avatar" :size="48">
              {{ post.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <div class="post-meta">
              <h3 class="post-title">{{ post.title }}</h3>
              <div class="post-info">
                <span class="post-author">{{ post.username }}</span>
                <span class="post-time">{{ post.createTime }}</span>
              </div>
            </div>
          </div>
          
          <div class="post-content">
            <p>{{ post.content }}</p>
          </div>
          
          <div class="post-footer">
            <span class="meta-item">
              <el-icon><View /></el-icon>
              {{ post.viewCount }}
            </span>
            <span class="meta-item">
              <el-icon><Star /></el-icon>
              {{ post.likeCount }}
            </span>
            <span class="meta-item">
              <el-icon><ChatDotRound /></el-icon>
              {{ post.commentCount }}
            </span>
          </div>
        </div>
      </div>
      
      <div class="pagination-section" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.forum-list-page {
  padding: $spacing-xl 0 $spacing-xxl;
  width: 100%;
  min-height: 80vh;
  position: relative;
}

.particle-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

.container {
  position: relative;
  z-index: 1;
}

.page-nav {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;

  .el-button {
    color: $text-color-secondary;
    transition: color $transition-fast;

    &:hover {
      color: $primary-color;
    }
  }

  .current-page {
    font-size: $font-size-sm;
    color: $text-color-light;
  }
}

.page-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: $spacing-xl;
  padding-bottom: $spacing-lg;
  border-bottom: 1px solid $border-color-light;
}

.header-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-xs;
}

.page-title {
  font-size: $font-size-hero;
  color: $primary-color;
  font-family: $font-family-title;
  letter-spacing: 2px;
  position: relative;
  text-align: center;

  &::after {
    content: '';
    position: absolute;
    bottom: -8px;
    left: 50%;
    transform: translateX(-50%);
    width: 80px;
    height: 3px;
    background: linear-gradient(90deg, transparent, $primary-color, $accent-color, $primary-color, transparent);
    border-radius: 2px;
    box-shadow: 0 1px 4px rgba($primary-color, 0.3);
  }
}

.decorative-quote {
  text-align: center;
  padding: 20px 40px;
  margin-bottom: $spacing-xl;
  position: relative;

  &::before,
  &::after {
    content: '';
    display: block;
    width: 60px;
    height: 1px;
    background: linear-gradient(90deg, transparent, $primary-color, transparent);
    margin: 0 auto 12px;
  }

  &::after {
    margin: 12px auto 0;
  }
}

.quote-text {
  font-size: 16px;
  color: $text-color-secondary;
  font-style: italic;
  letter-spacing: 2px;
  line-height: 1.8;
  margin: 0;
}

.quote-source {
  font-size: 13px;
  color: $text-color-light;
}

.page-subtitle {
  font-size: $font-size-base;
  color: $text-color-light;
  font-family: $font-family-base;
  margin: 0;
  margin-top: $spacing-sm;
}

.header-right {
  display: flex;
  gap: $spacing-sm;
  margin-top: $spacing-md;

  .el-button {
    border-radius: $border-radius-md;
    padding: 10px 20px;
    font-weight: 500;
    transition: all $transition-fast;

    &:hover {
      transform: translateY(-1px);
      box-shadow: $box-shadow;
    }
  }
}

.search-section {
  margin-bottom: $spacing-xl;

  :deep(.el-input) {
    border-radius: $border-radius-lg;
    overflow: hidden;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
    transition: box-shadow $transition-fast;

    &:focus-within {
      box-shadow: 0 2px 16px rgba(139, 69, 19, 0.12);
    }
  }

  :deep(.el-input__wrapper) {
    padding: 8px 16px;
    border-radius: $border-radius-lg;
  }

  :deep(.el-input__inner) {
    font-family: $font-family-input;
    letter-spacing: 0.5px;
    line-height: 1.6;
    font-size: $font-size-base;
  }

  :deep(.el-input-group__append) {
    background-color: $primary-color;
    border-color: $primary-color;
    border-radius: 0 $border-radius-lg $border-radius-lg 0;
    padding: 0 20px;

    .el-button {
      color: #fff;
      
      &:hover {
        color: #fff;
        opacity: 0.9;
      }
    }
  }
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  min-height: 400px;
}

.post-card {
  @include card;
  cursor: pointer;
  border-radius: $border-radius-lg;
  padding: $spacing-lg $spacing-xl;
  border: 1px solid transparent;
  transition: all $transition-base;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $box-shadow-lg;
    border-color: $border-color-light;

    .post-title {
      color: $primary-color;
    }
  }
}

.post-header {
  display: flex;
  gap: $spacing-md;
  margin-bottom: $spacing-md;

  .el-avatar {
    flex-shrink: 0;
    border: 2px solid $background-color;
    box-shadow: $box-shadow;
  }
}

.post-meta {
  flex: 1;
  min-width: 0;
}

.post-title {
  font-size: $font-size-xl;
  color: $text-color;
  margin-bottom: $spacing-xs;
  font-weight: 600;
  transition: color $transition-fast;
  @include text-ellipsis;
}

.post-info {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.post-author {
  font-size: $font-size-sm;
  color: $primary-color;
  font-weight: 600;
}

.post-time {
  font-size: $font-size-sm;
  color: $text-color-light;

  &::before {
    content: '·';
    margin-right: $spacing-sm;
    color: $border-color;
  }
}

.post-content {
  margin-bottom: $spacing-md;
  padding: 0 $spacing-xs;
  
  p {
    font-size: $font-size-base;
    color: $text-color-secondary;
    line-height: $line-height-loose;
    @include text-clamp(2);
  }
}

.post-footer {
  display: flex;
  gap: $spacing-xl;
  padding-top: $spacing-md;
  border-top: 1px solid $border-color-light;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: $font-size-sm;
  color: $text-color-light;
  transition: color $transition-fast;

  .el-icon {
    font-size: 16px;
  }

  &:hover {
    color: $primary-color;
  }
}

.pagination-section {
  margin-top: $spacing-xxl;
  display: flex;
  justify-content: center;

  :deep(.el-pagination) {
    --el-pagination-hover-color: #{$primary-color};
  }
}

@include responsive(md) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: $spacing-md;
  }

  .header-right {
    width: 100%;

    .el-button {
      flex: 1;
    }
  }

  .post-card {
    padding: $spacing-md;
  }

  .post-footer {
    gap: $spacing-md;
    flex-wrap: wrap;
  }
}

@include responsive(sm) {
  .forum-list-page {
    padding: $spacing-md 0;
  }

  .page-title {
    font-size: $font-size-title;
  }

  .page-subtitle {
    font-size: $font-size-sm;
  }

  .post-header {
    gap: $spacing-sm;
  }

  .post-title {
    font-size: $font-size-lg;
  }
}
</style>