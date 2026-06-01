<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { usePoemStore } from '@/stores/poem'
import type { Poem, Dynasty, Category } from '@/types/model'
import { getDynastyList } from '@/api/modules/dynasty'
import { getCategoryList } from '@/api/modules/category'

const router = useRouter()
const poemStore = usePoemStore()

const loading = ref(false)
const dynasties = ref<Dynasty[]>([])
const categories = ref<Category[]>([])

const filters = ref({
  dynastyId: undefined as number | undefined,
  categoryId: undefined as number | undefined,
  keyword: ''
})

const currentPage = ref(1)
const pageSize = ref(10)

const poems = computed(() => poemStore.poemList)
const total = computed(() => poemStore.total)

const fetchPoems = async () => {
  loading.value = true
  try {
    await poemStore.fetchPoemList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      dynastyId: filters.value.dynastyId,
      categoryId: filters.value.categoryId,
      keyword: filters.value.keyword
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

onMounted(() => {
  fetchFilters()
  fetchPoems()
})
</script>

<template>
  <div class="poem-list-page">
    <div class="container">
      <h1 class="page-title">诗词鉴赏</h1>
      
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
          </div>
          <div class="poem-info">
            <h3 class="poem-title">{{ poem.title }}</h3>
            <p class="poem-author">
              <span v-if="poem.dynastyName">{{ poem.dynastyName }}：</span>
              <span v-if="poem.poetName">{{ poem.poetName }}</span>
            </p>
            <p class="poem-content">{{ poem.content }}</p>
            <div class="poem-meta">
              <span class="meta-item">
                <el-icon><View /></el-icon>
                {{ poem.viewCount }}
              </span>
              <span class="meta-item">
                <el-icon><Star /></el-icon>
                {{ poem.likeCount }}
              </span>
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
}

.page-title {
  text-align: center;
  font-size: $font-size-title;
  color: $primary-color;
  font-family: $font-family-title;
  margin-bottom: $spacing-xl;
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
}

.poem-image {
  width: 200px;
  height: 150px;
  flex-shrink: 0;
  border-radius: $border-radius-sm;
  overflow: hidden;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform $transition-slow;
  }
}

.poem-card:hover .poem-image img {
  transform: scale(1.1);
}

.poem-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.poem-title {
  font-size: $font-size-xl;
  color: $text-color;
  margin-bottom: $spacing-xs;
}

.poem-author {
  font-size: $font-size-sm;
  color: $text-color-secondary;
  margin-bottom: $spacing-sm;
}

.poem-content {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: $line-height-loose;
  @include text-clamp(2);
  flex: 1;
}

.poem-meta {
  display: flex;
  gap: $spacing-md;
  margin-top: $spacing-sm;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-light;
}

.pagination-section {
  margin-top: $spacing-xl;
  display: flex;
  justify-content: center;
}
</style>