<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { ForumPost } from '@/types/model'
import { getForumPostList } from '@/api/modules/forum'

const router = useRouter()

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
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">诗汇论坛</h1>
        <el-button type="primary" @click="goToCreate">
          <el-icon><Edit /></el-icon>
          发帖
        </el-button>
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
  padding: $spacing-xl 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xl;
}

.page-title {
  font-size: $font-size-title;
  color: $primary-color;
  font-family: $font-family-title;
}

.search-section {
  margin-bottom: $spacing-xl;

  :deep(.el-input__inner) {
    font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
    letter-spacing: 0.5px;
    line-height: 1.5;
  }
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
  min-height: 400px;
}

.post-card {
  @include card;
  cursor: pointer;
}

.post-header {
  display: flex;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.post-meta {
  flex: 1;
}

.post-title {
  font-size: $font-size-xl;
  color: $text-color;
  margin-bottom: $spacing-xs;
}

.post-info {
  display: flex;
  gap: $spacing-md;
}

.post-author {
  font-size: $font-size-sm;
  color: $primary-color;
  font-weight: 600;
}

.post-time {
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.post-content {
  margin-bottom: $spacing-md;
  
  p {
    font-size: $font-size-base;
    color: $text-color-secondary;
    line-height: $line-height-loose;
    @include text-clamp(3);
  }
}

.post-footer {
  display: flex;
  gap: $spacing-lg;
  padding-top: $spacing-md;
  border-top: 1px solid $border-color;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.pagination-section {
  margin-top: $spacing-xl;
  display: flex;
  justify-content: center;
}
</style>