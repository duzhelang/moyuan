<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { View, Star } from '@element-plus/icons-vue'
import type { VisionArticle } from '@/types/model'
import { getVisionArticleList } from '@/api/modules/visionArticle'

const router = useRouter()

const loading = ref(false)
const articles = ref<VisionArticle[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const fetchArticles = async () => {
  loading.value = true
  try {
    const res = await getVisionArticleList({ pageNum: pageNum.value, pageSize: pageSize.value })
    articles.value = res.data.list
    total.value = res.data.total
  } catch (error) {
    console.error('获取文章列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  pageNum.value = page
  fetchArticles()
}

const goToDetail = (id: number) => {
  router.push(`/vision/${id}`)
}

onMounted(() => {
  fetchArticles()
})
</script>

<template>
  <div class="vision-list-page" v-loading="loading">
    <div class="container">
      <div class="page-header">
        <h1>诗话视野</h1>
        <p class="page-subtitle">探索诗词文化的无限魅力</p>
      </div>

      <div class="article-list" v-if="articles.length > 0">
        <el-card
          v-for="article in articles"
          :key="article.id"
          class="article-item"
          shadow="hover"
          @click="goToDetail(article.id)"
        >
          <div class="article-info">
            <el-tag v-if="article.category" size="small" type="warning" effect="plain">
              {{ article.category }}
            </el-tag>
            <h3 class="article-title">{{ article.title }}</h3>
            <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>
            <div class="article-meta">
              <span v-if="article.author" class="meta-author">{{ article.author }}</span>
              <span class="meta-time">{{ article.createTime }}</span>
              <span class="meta-view">
                <el-icon><View /></el-icon>
                {{ article.viewCount }}
              </span>
              <span class="meta-like">
                <el-icon><Star /></el-icon>
                {{ article.likeCount }}
              </span>
            </div>
          </div>
        </el-card>
      </div>

      <el-empty v-else description="暂无文章" />

      <div class="pagination" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="pageNum"
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
.vision-list-page {
  padding: $spacing-xl 0;
  min-height: 60vh;
}

.page-header {
  text-align: center;
  margin-bottom: $spacing-xxl;

  h1 {
    font-size: $font-size-hero;
    color: $text-color;
    margin-bottom: $spacing-sm;
  }
}

.page-subtitle {
  font-size: $font-size-lg;
  color: $text-color-secondary;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.article-item {
  cursor: pointer;
  border-radius: $border-radius-md;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
  }
}

.article-info {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.article-title {
  font-size: $font-size-xl;
  color: $text-color;
  margin: $spacing-xs 0;
}

.article-summary {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: $line-height-normal;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-top: $spacing-sm;
}

.meta-author {
  font-size: $font-size-sm;
  color: $primary-color;
  font-weight: 600;
}

.meta-time,
.meta-view,
.meta-like {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.pagination {
  margin-top: $spacing-xxl;
  display: flex;
  justify-content: center;
}
</style>