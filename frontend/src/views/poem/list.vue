<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePoemStore } from '@/stores/poem'
import { useUserStore } from '@/stores/user'
import type { Dynasty, Category } from '@/types/model'
import { getDynastyList } from '@/api/modules/dynasty'
import { getCategoryList } from '@/api/modules/category'
import { likePoem, favoritePoem } from '@/api/modules/poem'

const router = useRouter()
const route = useRoute()
const poemStore = usePoemStore()
const userStore = useUserStore()

const loading = ref(false)
const dynasties = ref<Dynasty[]>([])
const categories = ref<Category[]>([])

const filters = ref({
  dynastyId: undefined as number | undefined,
  categoryId: undefined as number | undefined,
  keyword: '',
  sortBy: 'latest' as 'latest' | 'popular' | 'likes'
})

const currentPage = ref(1)
const pageSize = ref(10)

const poems = computed(() => poemStore.poemList)
const total = computed(() => poemStore.total)

const sortOptions = [
  { value: 'latest', label: '最新发布' },
  { value: 'popular', label: '最多浏览' },
  { value: 'likes', label: '最多点赞' }
]

const fetchPoems = async () => {
  loading.value = true
  try {
    await poemStore.fetchPoemList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      dynastyId: filters.value.dynastyId,
      categoryId: filters.value.categoryId,
      keyword: filters.value.keyword,
      sortBy: filters.value.sortBy
    })
  } catch (error) {
    console.error('获取诗词列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchFilters = async () => {
  try {
    const [dynastyRes, categoryRes] = await Promise.all([
      getDynastyList(),
      getCategoryList()
    ])
    dynasties.value = dynastyRes.data
    categories.value = categoryRes.data
  } catch (error) {
    console.error('获取筛选条件失败:', error)
  }
}

const handleFilterChange = () => {
  currentPage.value = 1
  fetchPoems()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchPoems()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  fetchPoems()
}

const goToDetail = (id: number) => {
  router.push(`/poem/${id}`)
}

const handleLike = async (poem: any, event: Event) => {
  event.stopPropagation()
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再点赞')
    return
  }
  try {
    await likePoem(poem.id)
    poem.isLiked = !poem.isLiked
    poem.likeCount = poem.isLiked ? poem.likeCount + 1 : poem.likeCount - 1
    ElMessage.success(poem.isLiked ? '点赞成功' : '已取消点赞')
  } catch (error) {
    console.error('点赞操作失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  }
}

const handleFavorite = async (poem: any, event: Event) => {
  event.stopPropagation()
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再收藏')
    return
  }
  try {
    await favoritePoem(poem.id)
    poem.isFavorited = !poem.isFavorited
    poem.favoriteCount = poem.isFavorited ? poem.favoriteCount + 1 : poem.favoriteCount - 1
    ElMessage.success(poem.isFavorited ? '收藏成功' : '已取消收藏')
  } catch (error) {
    console.error('收藏操作失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  }
}

onMounted(() => {
  const query = route.query
  if (query.dynastyId) {
    filters.value.dynastyId = Number(query.dynastyId)
  }
  if (query.categoryId) {
    filters.value.categoryId = Number(query.categoryId)
  }
  if (query.keyword) {
    filters.value.keyword = String(query.keyword)
  }
  fetchFilters()
  fetchPoems()
})
</script>

<template>
  <div class="poem-list-page">
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">诗词鉴赏</h1>
          <p class="page-subtitle">品读千年韵律，感悟诗词之美</p>
        </div>
        <el-button
          v-if="userStore.isLoggedIn"
          type="primary"
          @click="router.push('/poem/create')"
        >
          <el-icon><Edit /></el-icon>
          发布新诗
        </el-button>
      </div>
      
      <div class="filter-section">
        <el-card class="filter-card">
          <div class="filter-row">
            <div class="filter-item">
              <label>朝代：</label>
              <el-select
                v-model="filters.dynastyId"
                placeholder="全部朝代"
                clearable
                @change="handleFilterChange"
              >
                <el-option
                  v-for="dynasty in dynasties"
                  :key="dynasty.id"
                  :label="dynasty.name"
                  :value="dynasty.id"
                />
              </el-select>
            </div>
            
            <div class="filter-item">
              <label>分类：</label>
              <el-select
                v-model="filters.categoryId"
                placeholder="全部分类"
                clearable
                @change="handleFilterChange"
              >
                <el-option
                  v-for="category in categories"
                  :key="category.id"
                  :label="category.name"
                  :value="category.id"
                />
              </el-select>
            </div>
            
            <div class="filter-item">
              <label>排序：</label>
              <el-select
                v-model="filters.sortBy"
                @change="handleFilterChange"
              >
                <el-option
                  v-for="option in sortOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </div>
            
            <div class="filter-item">
              <label>搜索：</label>
              <el-input
                v-model="filters.keyword"
                placeholder="搜索诗词..."
                clearable
                @keyup.enter="handleFilterChange"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>
            
            <el-button type="primary" @click="handleFilterChange">
              搜索
            </el-button>
          </div>
        </el-card>
      </div>
      
      <div class="poem-grid" v-loading="loading">
        <el-empty v-if="!loading && poems.length === 0" description="暂无诗词" />
        
        <div
          v-for="poem in poems"
          :key="poem.id"
          class="poem-card"
          @click="goToDetail(poem.id)"
        >
          <div class="poem-image">
            <img src="/images/h6.jpg" :alt="poem.title" />
            <div class="featured-badge" v-if="poem.isFeatured">
              <el-icon><Star /></el-icon>
              精选
            </div>
          </div>
          <div class="poem-info">
            <div class="poem-header">
              <h3 class="poem-title">{{ poem.title }}</h3>
              <div class="poem-tags">
                <span class="tag dynasty-tag" v-if="poem.dynastyName">{{ poem.dynastyName }}</span>
                <span class="tag category-tag" v-if="poem.categoryName">{{ poem.categoryName }}</span>
              </div>
            </div>
            <p class="poem-author">
              <span v-if="poem.poetName">{{ poem.poetName }}</span>
            </p>
            <p class="poem-content">{{ poem.content }}</p>
            <div class="poem-footer">
              <div class="poem-meta">
                <span class="meta-item">
                  <el-icon><View /></el-icon>
                  {{ poem.viewCount }}
                </span>
                <span class="meta-item">
                  <el-icon><ChatDotRound /></el-icon>
                  {{ poem.likeCount }}
                </span>
              </div>
              <div class="poem-actions">
                <el-button
                  class="action-btn like-btn"
                  :class="{ 'is-liked': poem.isLiked }"
                  :icon="poem.isLiked ? 'StarFilled' : 'Star'"
                  @click="handleLike(poem, $event)"
                  link
                >
                  {{ poem.isLiked ? '已赞' : '点赞' }}
                </el-button>
                <el-button
                  class="action-btn favorite-btn"
                  :class="{ 'is-favorited': poem.isFavorited }"
                  :icon="poem.isFavorited ? 'FolderChecked' : 'Folder'"
                  @click="handleFavorite(poem, $event)"
                  link
                >
                  {{ poem.isFavorited ? '已藏' : '收藏' }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="pagination-section" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use 'sass:color';

.poem-list-page {
  padding: $spacing-xl 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xl;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.page-title {
  font-size: $font-size-title;
  color: $primary-color;
  font-family: $font-family-title;
}

.page-subtitle {
  font-size: $font-size-sm;
  color: $text-color-light;
  font-family: $font-family-base;
  margin: 0;
}

.filter-section {
  margin-bottom: $spacing-xl;
}

.filter-card {
  border-radius: $border-radius-md;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-lg;
  align-items: center;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  
  label {
    font-size: $font-size-base;
    color: $text-color;
    white-space: nowrap;
  }
  
  .el-select,
  .el-input {
    width: 180px;
  }

  :deep(.el-input__inner) {
    font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
    letter-spacing: 0.5px;
    line-height: 1.5;
  }
}

.poem-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-lg;
  min-height: 400px;
  
  @include responsive(lg) {
    grid-template-columns: repeat(2, 1fr);
  }
  
  @include responsive(md) {
    grid-template-columns: 1fr;
  }
}

.poem-card {
  @include card;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  gap: $spacing-lg;
  position: relative;
  
  @include responsive(sm) {
    flex-direction: column;
  }
}

.poem-image {
  width: 200px;
  height: 150px;
  flex-shrink: 0;
  border-radius: $border-radius-sm;
  overflow: hidden;
  position: relative;
  
  @include responsive(sm) {
    width: 100%;
    height: 180px;
  }
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform $transition-slow;
  }
}

.featured-badge {
  position: absolute;
  top: $spacing-sm;
  left: $spacing-sm;
  background: linear-gradient(135deg, #FFD700, #FFA500);
  color: white;
  padding: $spacing-xs $spacing-sm;
  border-radius: $border-radius-sm;
  font-size: $font-size-xs;
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-weight: bold;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.poem-card:hover .poem-image img {
  transform: scale(1.1);
}

.poem-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.poem-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: $spacing-xs;
  gap: $spacing-sm;
}

.poem-title {
  font-size: $font-size-xl;
  color: $text-color;
  font-family: $font-family-title;
  flex: 1;
  min-width: 0;
  @include text-ellipsis;
}

.poem-tags {
  display: flex;
  gap: $spacing-xs;
  flex-shrink: 0;
}

.tag {
  padding: $spacing-xs $spacing-sm;
  border-radius: $border-radius-sm;
  font-size: $font-size-xs;
  font-weight: 500;
  white-space: nowrap;
}

.dynasty-tag {
  background-color: rgba($primary-color, 0.1);
  color: $primary-color;
}

.category-tag {
  background-color: rgba($accent-color, 0.1);
  color: $accent-color;
}

.poem-author {
  font-size: $font-size-sm;
  color: $text-color-secondary;
  margin-bottom: $spacing-sm;
  font-family: $font-family-base;
}

.poem-content {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: $line-height-loose;
  @include text-clamp(2);
  flex: 1;
  font-family: $font-family-base;
}

.poem-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: $spacing-sm;
  padding-top: $spacing-sm;
  border-top: 1px solid $border-color-light;
  
  @include responsive(sm) {
    flex-direction: column;
    align-items: flex-start;
    gap: $spacing-sm;
  }
}

.poem-meta {
  display: flex;
  gap: $spacing-md;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-light;
}

.poem-actions {
  display: flex;
  gap: $spacing-sm;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-secondary;
  transition: all $transition-fast;
  padding: $spacing-xs $spacing-sm;
  border-radius: $border-radius-sm;
  
  &:hover {
    background-color: $background-color;
  }
}

.like-btn {
  &.is-liked {
    color: $warning-color;
    
    &:hover {
      color: darken($warning-color, 10%);
    }
  }
}

.favorite-btn {
  &.is-favorited {
    color: $danger-color;
    
    &:hover {
      color: color.adjust($danger-color, $lightness: -10%);
    }
  }
}

.pagination-section {
  margin-top: $spacing-xl;
  display: flex;
  justify-content: center;
}
</style>