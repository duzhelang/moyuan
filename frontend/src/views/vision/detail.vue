<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, User, Clock, View, Star, HomeFilled } from '@element-plus/icons-vue'
import type { VisionArticle } from '@/types/model'
import { getVisionArticleById, likeVisionArticle } from '@/api/modules/visionArticle'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const article = ref<VisionArticle | null>(null)
const articleId = computed(() => Number(route.params.id))

const fetchArticle = async () => {
  loading.value = true
  try {
    const res = await getVisionArticleById(articleId.value)
    article.value = res.data
  } catch (error) {
    ElMessage.error('获取文章详情失败')
    router.push('/')
  } finally {
    loading.value = false
  }
}

const handleLike = async () => {
  try {
    await likeVisionArticle(articleId.value)
    ElMessage.success('点赞成功')
    if (article.value) {
      article.value.likeCount++
    }
  } catch (error) {
    ElMessage.error('点赞失败')
  }
}

const handleBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

onMounted(() => {
  fetchArticle()
})
</script>

<template>
  <div class="vision-detail-page" v-loading="loading">
    <div class="container" v-if="article">
      <div class="article-header">
        <el-button @click="handleBack" class="back-button">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
      </div>

      <el-card class="article-card">
        <div class="article-title-section">
          <el-tag v-if="article.category" class="article-category" type="warning" effect="plain">
            {{ article.category }}
          </el-tag>
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-meta">
            <span v-if="article.author" class="meta-author">
              <el-icon><User /></el-icon>
              {{ article.author }}
            </span>
            <span class="meta-time">
              <el-icon><Clock /></el-icon>
              {{ article.createTime }}
            </span>
            <span class="meta-view">
              <el-icon><View /></el-icon>
              {{ article.viewCount }} 次浏览
            </span>
          </div>
        </div>

        <div v-if="article.coverImage" class="article-cover">
          <img :src="article.coverImage" :alt="article.title" />
        </div>

        <div class="article-content-section">
          <div class="article-content" v-html="article.content.replace(/\n/g, '<br/>')"></div>
        </div>

        <div class="article-actions">
          <el-button type="primary" @click="handleLike">
            <el-icon><Star /></el-icon>
            点赞 ({{ article.likeCount }})
          </el-button>
        </div>
      </el-card>

      <div class="back-to-home">
        <el-button @click="router.push('/')" type="default" plain>
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.vision-detail-page {
  padding: $spacing-xl 0;
  min-height: 60vh;
}

.article-header {
  margin-bottom: $spacing-lg;
}

.back-button {
  margin-bottom: $spacing-md;
}

.article-card {
  margin-bottom: $spacing-xl;
  border-radius: $border-radius-md;
}

.article-title-section {
  margin-bottom: $spacing-xl;
  padding-bottom: $spacing-xl;
  border-bottom: 1px solid $border-color;
}

.article-category {
  margin-bottom: $spacing-md;
}

.article-title {
  font-size: $font-size-hero;
  color: $text-color;
  margin-bottom: $spacing-lg;
  line-height: 1.4;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
  flex-wrap: wrap;
}

.meta-author,
.meta-time,
.meta-view {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.meta-author {
  font-weight: 600;
  color: $primary-color;
}

.article-cover {
  margin-bottom: $spacing-xl;

  img {
    width: 100%;
    max-height: 400px;
    object-fit: cover;
    border-radius: $border-radius-md;
  }
}

.article-content-section {
  margin-bottom: $spacing-xl;
}

.article-content {
  font-size: $font-size-lg;
  line-height: $line-height-loose;
  color: $text-color;
}

.article-actions {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding-top: $spacing-xl;
  border-top: 1px solid $border-color;
}

.back-to-home {
  text-align: center;
  padding: $spacing-lg 0;
}
</style>