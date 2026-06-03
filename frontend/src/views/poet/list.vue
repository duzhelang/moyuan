<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { Poet, Dynasty } from '@/types/model'
import { getPoetList } from '@/api/modules/poet'
import { getDynastyList } from '@/api/modules/dynasty'

const router = useRouter()

const loading = ref(false)
const poets = ref<Poet[]>([])
const total = ref(0)
const dynasties = ref<Dynasty[]>([])

const filters = ref({
  dynastyId: undefined as number | undefined,
  keyword: ''
})

const currentPage = ref(1)
const pageSize = ref(10)

const fetchPoets = async () => {
  loading.value = true
  try {
    const res = await getPoetList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      dynastyId: filters.value.dynastyId,
      keyword: filters.value.keyword
    })
    poets.value = res.data.list
    total.value = res.data.total
  } catch (error) {
    console.error('获取诗人列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchDynasties = async () => {
  try {
    const res = await getDynastyList()
    dynasties.value = res.data
  } catch (error) {
    console.error('获取朝代列表失败:', error)
  }
}

const handleFilterChange = () => {
  currentPage.value = 1
  fetchPoets()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchPoets()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  fetchPoets()
}

const goToDetail = (id: number) => {
  router.push(`/poet/${id}`)
}

onMounted(() => {
  fetchDynasties()
  fetchPoets()
})
</script>

<template>
  <div class="poet-list-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">诗人风采</h1>
        <p class="page-subtitle">穿越千年时光，领略文人墨客的风采与情怀</p>
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
              <label>搜索：</label>
              <el-input
                v-model="filters.keyword"
                placeholder="搜索诗人..."
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

      <div class="poet-grid" v-loading="loading">
        <el-empty v-if="!loading && poets.length === 0" description="暂无诗人" />

        <div
          v-for="poet in poets"
          :key="poet.id"
          class="poet-card"
          @click="goToDetail(poet.id)"
        >
          <el-avatar :src="poet.avatar" :size="80">
            {{ poet.name?.charAt(0) }}
          </el-avatar>
          <div class="poet-info">
            <h3 class="poet-name">{{ poet.name }}</h3>
            <div class="poet-alias" v-if="poet.courtesyName || poet.pseudonym">
              <span v-if="poet.courtesyName">字：{{ poet.courtesyName }}</span>
              <span v-if="poet.pseudonym">号：{{ poet.pseudonym }}</span>
            </div>
            <div class="poet-meta">
              <span v-if="poet.dynastyName">{{ poet.dynastyName }}</span>
              <span v-if="poet.birthYear">{{ poet.birthYear }}年 - {{ poet.deathYear ? poet.deathYear + '年' : '至今' }}</span>
              <span v-if="poet.birthplace">{{ poet.birthplace }}</span>
            </div>
            <p class="poet-desc" v-if="poet.description">{{ poet.description }}</p>
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
.poet-list-page {
  padding: $spacing-xl 0;
}

.page-header {
  text-align: center;
  margin-bottom: $spacing-xl;
}

.page-title {
  text-align: center;
  font-size: $font-size-title;
  color: $primary-color;
  font-family: $font-family-title;
  margin-bottom: $spacing-xs;
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
}

.poet-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-lg;
  min-height: 400px;

  @include responsive(md) {
    grid-template-columns: 1fr;
  }
}

.poet-card {
  @include card;
  cursor: pointer;
  display: flex;
  gap: $spacing-lg;
  padding: $spacing-lg;
  transition: box-shadow $transition-slow;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  }
}

.poet-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.poet-name {
  font-size: $font-size-xl;
  color: $text-color;
  margin-bottom: $spacing-xs;
}

.poet-alias {
  display: flex;
  gap: $spacing-lg;
  font-size: $font-size-sm;
  color: $text-color-secondary;
  margin-bottom: $spacing-sm;
}

.poet-meta {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-md;
  font-size: $font-size-sm;
  color: $text-color-light;
  margin-bottom: $spacing-sm;
}

.poet-desc {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: $line-height-loose;
  @include text-clamp(2);
  flex: 1;
}

.pagination-section {
  margin-top: $spacing-xl;
  display: flex;
  justify-content: center;
}
</style>
