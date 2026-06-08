<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePoemStore } from '@/stores/poem'
import { useUserStore } from '@/stores/user'
import { usePreferencesStore } from '@/stores/preferences'
import type { Dynasty, Category, Poem, Poet } from '@/types/model'
import { getDynastyList } from '@/api/modules/dynasty'
import { getCategoryList } from '@/api/modules/category'
import { likePoem, favoritePoem, getModernPoems } from '@/api/modules/poem'
import { getPoetList } from '@/api/modules/poet'
import { getCache, setCache } from '@/utils/storage'

const router = useRouter()
const route = useRoute()
const poemStore = usePoemStore()
const userStore = useUserStore()
const preferencesStore = usePreferencesStore()

const loading = ref(false)
const dynasties = ref<Dynasty[]>([])
const categories = ref<Category[]>([])
const poets = ref<Poet[]>([])

const activeTab = ref<'classical' | 'modern'>('classical')

const filters = ref({
  dynastyId: undefined as number | undefined,
  categoryId: undefined as number | undefined,
  poetId: undefined as number | undefined,
  keyword: '',
  sortBy: 'latest' as 'latest' | 'popular' | 'likes'
})

const modernFilters = ref({
  isOriginal: undefined as boolean | undefined,
  hasCertifiedPoet: undefined as boolean | undefined,
  sortBy: 'latest' as 'latest' | 'popular' | 'likes'
})

const currentPage = ref(1)
const pageSize = ref(preferencesStore.userPreferences.pageSize || 10)

const poems = computed(() => {
  if (activeTab.value === 'classical') {
    return poemStore.poemList
  }
  return modernPoemList.value
})
const total = computed(() => {
  if (activeTab.value === 'classical') {
    return poemStore.total
  }
  return modernTotal.value
})

const modernPoemList = ref<Poem[]>([])
const modernTotal = ref(0)

const sortOptions = [
  { value: 'latest', label: '最新发布' },
  { value: 'popular', label: '最多浏览' },
  { value: 'likes', label: '最多点赞' }
]

const fetchPoems = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'classical') {
      await poemStore.fetchPoemList({
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        dynastyId: filters.value.dynastyId,
        categoryId: filters.value.categoryId,
        poetId: filters.value.poetId,
        keyword: filters.value.keyword,
        sortBy: filters.value.sortBy
      })
    } else {
      const res = await getModernPoems({
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        isOriginal: modernFilters.value.isOriginal,
        hasCertifiedPoet: modernFilters.value.hasCertifiedPoet,
        sortBy: modernFilters.value.sortBy
      })
      modernPoemList.value = res.data.list
      modernTotal.value = res.data.total
    }
  } catch (error) {
    ElMessage.error('获取诗词列表失败')
  } finally {
    loading.value = false
  }
}

const fetchFilters = async () => {
  try {
    const cachedDynasties = getCache<Dynasty[]>('dynasties')
    const cachedCategories = getCache<Category[]>('categories')
    const cachedPoets = getCache<Poet[]>('poets')

    if (cachedDynasties && cachedCategories && cachedPoets) {
      dynasties.value = cachedDynasties
      categories.value = cachedCategories
      poets.value = cachedPoets
      return
    }

    const [dynastyRes, categoryRes, poetRes] = await Promise.all([
      getDynastyList(),
      getCategoryList(),
      getPoetList({ pageSize: 200 })
    ])
    dynasties.value = dynastyRes.data
    categories.value = categoryRes.data
    poets.value = poetRes.data.list

    setCache('dynasties', dynastyRes.data, 60 * 60 * 1000)
    setCache('categories', categoryRes.data, 60 * 60 * 1000)
    setCache('poets', poetRes.data.list, 60 * 60 * 1000)
  } catch (error) {
    ElMessage.error('获取筛选条件失败')
  }
}

const handleTabChange = (tab: 'classical' | 'modern') => {
  activeTab.value = tab
  currentPage.value = 1
  syncFiltersToUrl()
  fetchPoems()
}

const handleFilterChange = () => {
  currentPage.value = 1
  syncFiltersToUrl()
  fetchPoems()
}

const clearFilters = () => {
  if (activeTab.value === 'classical') {
    filters.value = {
      dynastyId: undefined,
      categoryId: undefined,
      poetId: undefined,
      keyword: '',
      sortBy: 'latest'
    }
  } else {
    modernFilters.value = {
      isOriginal: undefined,
      hasCertifiedPoet: undefined,
      sortBy: 'latest'
    }
  }
  currentPage.value = 1
  syncFiltersToUrl()
  fetchPoems()
}

const syncFiltersToUrl = () => {
  const query: Record<string, string> = {}
  if (activeTab.value === 'modern') {
    query.tab = 'modern'
  }
  if (activeTab.value === 'classical') {
    if (filters.value.dynastyId) query.dynastyId = String(filters.value.dynastyId)
    if (filters.value.categoryId) query.categoryId = String(filters.value.categoryId)
    if (filters.value.poetId) query.poetId = String(filters.value.poetId)
    if (filters.value.keyword) query.keyword = filters.value.keyword
    if (filters.value.sortBy !== 'latest') query.sortBy = filters.value.sortBy
  } else {
    if (modernFilters.value.isOriginal !== undefined) query.isOriginal = String(modernFilters.value.isOriginal)
    if (modernFilters.value.hasCertifiedPoet !== undefined) query.hasCertifiedPoet = String(modernFilters.value.hasCertifiedPoet)
    if (modernFilters.value.sortBy !== 'latest') query.sortBy = modernFilters.value.sortBy
  }
  router.replace({ query })
}

const hasActiveFilters = computed(() => {
  if (activeTab.value === 'classical') {
    return filters.value.dynastyId || filters.value.categoryId || filters.value.poetId || filters.value.keyword || filters.value.sortBy !== 'latest'
  }
  return modernFilters.value.isOriginal !== undefined || modernFilters.value.hasCertifiedPoet !== undefined || modernFilters.value.sortBy !== 'latest'
})

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchPoems()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  preferencesStore.setPageSize(size)
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
    ElMessage.error('操作失败，请稍后重试')
  }
}

const getFormattedVerses = (content: string) => {
  if (!content) return []
  const lines = content.split(/[。！？；\n]+/).filter(line => line.trim())
  return lines.slice(0, 3).map(line => line.trim())
}

onMounted(() => {
  const query = route.query
  if (query.tab === 'modern') {
    activeTab.value = 'modern'
  }
  if (query.dynastyId) {
    filters.value.dynastyId = Number(query.dynastyId)
  }
  if (query.categoryId) {
    filters.value.categoryId = Number(query.categoryId)
  }
  if (query.poetId) {
    filters.value.poetId = Number(query.poetId)
  }
  if (query.keyword) {
    filters.value.keyword = String(query.keyword)
  }
  if (query.sortBy) {
    const sortBy = String(query.sortBy) as 'latest' | 'popular' | 'likes'
    if (activeTab.value === 'classical') {
      filters.value.sortBy = sortBy
    } else {
      modernFilters.value.sortBy = sortBy
    }
  }
  if (query.isOriginal !== undefined) {
    modernFilters.value.isOriginal = query.isOriginal === 'true'
  }
  if (query.hasCertifiedPoet !== undefined) {
    modernFilters.value.hasCertifiedPoet = query.hasCertifiedPoet === 'true'
  }
  fetchFilters()
  fetchPoems()
})
</script>

<template>
  <div class="poem-list-page">
    <div class="page-decoration">
      <div class="decoration-left">
        <img src="/img/lb_shiwen (1).png" alt="" class="deco-img" />
      </div>
      <div class="decoration-right">
        <img src="/img/lb_shiwen (2).png" alt="" class="deco-img" />
      </div>
    </div>

    <div class="container">
      <div class="page-nav">
        <el-button text @click="router.push('/')">
          <el-icon><HomeFilled /></el-icon>
          首页
        </el-button>
        <el-divider direction="vertical" />
        <span class="current-page">诗词鉴赏</span>
      </div>

      <div class="page-header">
        <div class="header-content">
          <div class="header-left">
            <h1 class="page-title">诗词鉴赏</h1>
            <p class="page-subtitle">品读千年韵律，感悟诗词之美</p>
            <div class="title-decoration">
              <span class="deco-line"></span>
              <span class="deco-icon">墨</span>
              <span class="deco-line"></span>
            </div>
          </div>
          <div class="header-right">
            <el-button @click="router.push('/poet')">
              <el-icon><User /></el-icon>
              诗人风采
            </el-button>
            <el-button
              v-if="userStore.isLoggedIn"
              type="primary"
              @click="router.push('/poem/create')"
            >
              <el-icon><Edit /></el-icon>
              发布新诗
            </el-button>
          </div>
        </div>
      </div>

      <div class="tab-section">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="古籍诗文" name="classical" />
          <el-tab-pane label="现代创作" name="modern" />
        </el-tabs>
      </div>

      <div class="filter-section" v-if="activeTab === 'classical'">
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
              <label>诗人：</label>
              <el-select
                v-model="filters.poetId"
                placeholder="全部诗人"
                clearable
                filterable
                @change="handleFilterChange"
              >
                <el-option
                  v-for="poet in poets"
                  :key="poet.id"
                  :label="poet.name"
                  :value="poet.id"
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

            <el-button v-if="hasActiveFilters" @click="clearFilters">
              清除筛选
            </el-button>
          </div>
        </el-card>
      </div>

      <div class="filter-section" v-else>
        <el-card class="filter-card">
          <div class="filter-row">
            <div class="filter-item">
              <label>类型：</label>
              <el-select
                v-model="modernFilters.isOriginal"
                placeholder="全部作品"
                clearable
                @change="handleFilterChange"
              >
                <el-option label="全部作品" :value="undefined" />
                <el-option label="原创作品" :value="true" />
                <el-option label="非原创" :value="false" />
              </el-select>
            </div>

            <div class="filter-item">
              <label>诗人：</label>
              <el-select
                v-model="modernFilters.hasCertifiedPoet"
                placeholder="全部诗人"
                clearable
                @change="handleFilterChange"
              >
                <el-option label="全部诗人" :value="undefined" />
                <el-option label="认证诗人" :value="true" />
              </el-select>
            </div>

            <div class="filter-item">
              <label>排序：</label>
              <el-select
                v-model="modernFilters.sortBy"
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

            <el-button v-if="hasActiveFilters" @click="clearFilters">
              清除筛选
            </el-button>
          </div>
        </el-card>
      </div>

      <div class="result-stats" v-if="total > 0">
        <span>共找到 <strong>{{ total }}</strong> 首诗词</span>
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
            <div class="original-badge" v-if="poem.isOriginal">
              <el-icon><EditPen /></el-icon>
              原创
            </div>
          </div>
          <div class="poem-info">
            <div class="poem-header">
              <h3 class="poem-title">{{ poem.title }}</h3>
              <div class="poem-tags">
                <span class="tag dynasty-tag" v-if="poem.dynastyName">{{ poem.dynastyName }}</span>
                <span class="tag category-tag" v-if="poem.categoryName">{{ poem.categoryName }}</span>
                <span class="tag original-tag" v-if="poem.isOriginal">原创</span>
              </div>
            </div>
            <p class="poem-author">
              <span v-if="poem.poetName">{{ poem.poetName }}</span>
            </p>
            <div class="poem-verses">
              <p v-for="(line, index) in getFormattedVerses(poem.content)" :key="index" class="verse-line">
                {{ line }}
              </p>
            </div>
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
                <span class="meta-item rating-meta" v-if="poem.averageScore">
                  <el-icon><Trophy /></el-icon>
                  {{ poem.averageScore.toFixed(1) }}
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
.poem-list-page {
  padding: $spacing-xl 0;
  position: relative;
  min-height: 100vh;
  overflow-x: hidden;
}

.page-decoration {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
}

.decoration-left {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0.15;

  .deco-img {
    width: 200px;
    height: auto;
  }
}

.decoration-right {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0.15;

  .deco-img {
    width: 200px;
    height: auto;
  }
}

.page-nav {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;
  position: relative;
  z-index: 1;

  .el-button {
    color: $text-color-secondary;

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
  margin-bottom: $spacing-xl;
  position: relative;
  z-index: 1;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, rgba($primary-color, 0.03), rgba($accent-color, 0.03));
  border-radius: $border-radius-lg;
  padding: $spacing-xl;
  border: 1px solid rgba($primary-color, 0.1);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  align-items: center;
}

.header-right {
  display: flex;
  gap: $spacing-sm;
}

.page-title {
  font-size: $font-size-title;
  color: $primary-color;
  font-family: $font-family-title;
  margin: 0;
}

.page-subtitle {
  font-size: $font-size-base;
  color: $text-color-secondary;
  font-family: $font-family-base;
  margin: 0;
}

.title-decoration {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-top: $spacing-xs;
}

.deco-line {
  width: 40px;
  height: 1px;
  background: linear-gradient(90deg, $primary-color, transparent);
}

.deco-icon {
  font-size: $font-size-xs;
  color: $primary-color;
  font-family: $font-family-title;
  opacity: 0.6;
}

.tab-section {
  margin-bottom: $spacing-lg;

  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__item) {
    font-size: $font-size-lg;
    font-family: $font-family-title;
    color: $text-color-secondary;

    &.is-active {
      color: $primary-color;
      font-weight: 600;
    }
  }

  :deep(.el-tabs__active-bar) {
    background-color: $primary-color;
  }
}

.filter-section {
  margin-bottom: $spacing-xl;
  position: relative;
  z-index: 1;
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

.result-stats {
  margin-bottom: $spacing-md;
  font-size: $font-size-sm;
  color: $text-color-secondary;
  position: relative;
  z-index: 1;

  strong {
    color: $primary-color;
    font-weight: 600;
  }
}

.poem-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-lg;
  min-height: 400px;
  position: relative;
  z-index: 1;
  max-height: calc(100vh - 300px);
  overflow-y: auto;
  padding-right: $spacing-sm;
  @include scrollbar;

  @include responsive(lg) {
    grid-template-columns: repeat(2, 1fr);
  }

  @include responsive(md) {
    grid-template-columns: 1fr;
    max-height: none;
  }
}

.poem-card {
  @include card;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  gap: $spacing-lg;
  position: relative;
  border-left: 3px solid $primary-color;
  transition: all $transition-base;

  &:hover {
    border-left-color: darken($primary-color, 10%);
    transform: translateY(-2px);
  }

  @include responsive(sm) {
    flex-direction: column;
    border-left: none;
    border-top: 3px solid $primary-color;

    &:hover {
      border-top-color: darken($primary-color, 10%);
    }
  }
}

.poem-image {
  width: 180px;
  height: 140px;
  flex-shrink: 0;
  border-radius: $border-radius-sm;
  overflow: hidden;
  position: relative;

  @include responsive(sm) {
    width: 100%;
    height: 160px;
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
  top: $spacing-xs;
  left: $spacing-xs;
  background: linear-gradient(135deg, #FFD700, #FFA500);
  color: white;
  padding: 2px 6px;
  border-radius: $border-radius-sm;
  font-size: 10px;
  display: flex;
  align-items: center;
  gap: 2px;
  font-weight: bold;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  z-index: 2;
}

.original-badge {
  position: absolute;
  top: $spacing-xs;
  right: $spacing-xs;
  background: linear-gradient(135deg, #67C23A, #85ce61);
  color: white;
  padding: 2px $spacing-xs;
  border-radius: $border-radius-sm;
  font-size: 10px;
  display: flex;
  align-items: center;
  gap: 2px;
  font-weight: bold;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  z-index: 1;
}

.poem-card:hover .poem-image img {
  transform: scale(1.1);
}

.poem-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding-right: $spacing-lg;
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

.original-tag {
  background-color: rgba($success-color, 0.1);
  color: $success-color;
}

.poem-author {
  font-size: $font-size-sm;
  color: $text-color-secondary;
  margin-bottom: $spacing-sm;
  font-family: $font-family-base;
}

.poem-verses {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
  min-height: 60px;
}

.verse-line {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: 1.6;
  margin: 0;
  font-family: $font-family-base;
  white-space: normal;
  word-break: break-all;
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

.rating-meta {
  color: $warning-color;
  font-weight: 600;
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
      color: darken($danger-color, 10%);
    }
  }
}

.pagination-section {
  margin-top: $spacing-xl;
  display: flex;
  justify-content: center;
  position: relative;
  z-index: 1;
}

.card-corner {
  position: absolute;
  top: 0;
  right: 0;
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, transparent 50%, rgba($primary-color, 0.05) 50%);
  pointer-events: none;
}
</style>