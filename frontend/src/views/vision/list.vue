<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { View, Star, ArrowLeft, HomeFilled } from '@element-plus/icons-vue'
import type { VisionArticle } from '@/types/model'
import { getVisionArticleList } from '@/api/modules/visionArticle'

const router = useRouter()

const loading = ref(false)
const articles = ref<VisionArticle[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const category = ref('')

const categories = ['全部', '诗词鉴赏', '文化探索', '诗人故事', '诗词历史']

const fetchArticles = async () => {
  loading.value = true
  try {
    const params: { pageNum: number; pageSize: number; category?: string } = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (category.value && category.value !== '全部') {
      params.category = category.value
    }
    const res = await getVisionArticleList(params)
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
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleCategoryChange = (cat: string) => {
  category.value = cat
  pageNum.value = 1
  fetchArticles()
}

const goToDetail = (id: number) => {
  router.push(`/vision/${id}`)
}

const goHome = () => {
  router.push('/')
}

onMounted(() => {
  fetchArticles()
})
</script>

<template>
  <div class="vision-list-page" v-loading="loading">
    <div class="page-nav">
      <div class="nav-left">
        <el-button text @click="goHome">
          <el-icon><HomeFilled /></el-icon>
          首页
        </el-button>
        <el-divider direction="vertical" />
        <span class="current-page">诗话视野</span>
      </div>
    </div>

    <div class="container">
      <div class="page-header">
        <h1>诗话视野</h1>
        <p class="page-subtitle">探索诗词文化的无限魅力</p>
      </div>

      <div class="category-filter">
        <el-tag
          v-for="cat in categories"
          :key="cat"
          :type="category === cat || (cat === '全部' && !category) ? '' : 'info'"
          :effect="category === cat || (cat === '全部' && !category) ? 'dark' : 'plain'"
          class="category-tag"
          @click="handleCategoryChange(cat)"
        >
          {{ cat }}
        </el-tag>
      </div>

      <div class="article-list" v-if="articles.length > 0">
        <el-card
          v-for="article in articles"
          :key="article.id"
          class="article-item"
          shadow="hover"
          @click="goToDetail(article.id)"
        >
          <div class="article-content">
            <div v-if="article.coverImage" class="article-cover">
              <img :src="article.coverImage" :alt="article.title" />
            </div>
            <div class="article-info">
              <div class="article-header">
                <el-tag v-if="article.category" size="small" type="warning" effect="plain">
                  {{ article.category }}
                </el-tag>
              </div>
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
          </div>
        </el-card>
      </div>

      <el-empty v-else description="暂无文章" />

      <div class="pagination-wrapper" v-if="total > 0">
        <div class="pagination-info">
          共 <span class="total-num">{{ total }}</span> 篇文章
        </div>
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, jumper"
          :pager-count="7"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.vision-list-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f5f0e8 0%, #e8e0d0 100%);
}

.page-nav {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  padding: 12px 24px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e0d6c6;
  position: sticky;
  top: 0;
  z-index: 100;

  .nav-left {
    display: flex;
    align-items: center;
    gap: 8px;

    .el-button {
      color: #8b4513;
      font-size: 14px;

      &:hover {
        color: #d2691e;
      }
    }

    .current-page {
      color: #2c1810;
      font-weight: 600;
      font-size: 14px;
    }
  }
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 60px;
}

.page-header {
  text-align: center;
  padding: 40px 0 24px;

  h1 {
    font-size: 36px;
    color: #2c1810;
    margin-bottom: 8px;
    font-weight: 700;
  }
}

.page-subtitle {
  font-size: 16px;
  color: #666;
}

.category-filter {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 32px;
  flex-wrap: wrap;
}

.category-tag {
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  padding: 8px 20px;

  &:hover {
    transform: translateY(-2px);
  }
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.article-item {
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.3s ease;
  border: none;
  overflow: hidden;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 40px rgba(139, 69, 19, 0.15);
  }
}

.article-content {
  display: flex;
  gap: 24px;
}

.article-cover {
  flex-shrink: 0;
  width: 240px;
  height: 160px;
  border-radius: 8px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.3s ease;
  }

  .article-item:hover & img {
    transform: scale(1.05);
  }
}

.article-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.article-header {
  margin-bottom: 8px;
}

.article-title {
  font-size: 20px;
  color: #2c1810;
  margin: 0 0 8px;
  font-weight: 600;
  line-height: 1.4;
}

.article-summary {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-author {
  font-size: 13px;
  color: #8b4513;
  font-weight: 600;
}

.meta-time,
.meta-view,
.meta-like {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #888;
}

.pagination-wrapper {
  margin-top: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.pagination-info {
  font-size: 14px;
  color: #666;

  .total-num {
    color: #8b4513;
    font-weight: 600;
  }
}

@media (max-width: 768px) {
  .article-content {
    flex-direction: column;
  }

  .article-cover {
    width: 100%;
    height: 200px;
  }

  .article-title {
    font-size: 18px;
  }
}
</style>
