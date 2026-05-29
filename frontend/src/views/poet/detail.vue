<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { Poet, Poem } from '@/types/model'
import { getPoetById } from '@/api/modules/poet'
import { getPoemsByPoet } from '@/api/modules/poem'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const poet = ref<Poet | null>(null)
const poems = ref<Poem[]>([])
const poemTotal = ref(0)
const poemPage = ref(1)
const poemSize = ref(10)

const poetId = Number(route.params.id)

const fetchPoet = async () => {
  loading.value = true
  try {
    const res = await getPoetById(poetId)
    poet.value = res.data
  } catch (error) {
    ElMessage.error('获取诗人详情失败')
    router.push('/poet')
  } finally {
    loading.value = false
  }
}

const fetchPoems = async () => {
  try {
    const res = await getPoemsByPoet(poetId, {
      pageNum: poemPage.value,
      pageSize: poemSize.value
    })
    poems.value = res.data.list
    poemTotal.value = res.data.total
  } catch (error) {
    console.error('获取诗人作品失败:', error)
  }
}

const handlePageChange = (page: number) => {
  poemPage.value = page
  fetchPoems()
}

const goToPoemDetail = (id: number) => {
  router.push(`/poem/${id}`)
}

onMounted(() => {
  fetchPoet()
  fetchPoems()
})
</script>

<template>
  <div class="poet-detail-page" v-loading="loading">
    <div class="container" v-if="poet">
      <div class="poet-header">
        <el-button @click="router.push('/poet')" class="back-button">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>

      <el-card class="poet-card">
        <div class="poet-profile">
          <el-avatar :src="poet.avatar" :size="120" class="poet-avatar">
            {{ poet.name?.charAt(0) }}
          </el-avatar>
          <div class="poet-basic">
            <h1 class="poet-name">{{ poet.name }}</h1>
            <div class="poet-alias" v-if="poet.courtesyName || poet.pseudonym">
              <span v-if="poet.courtesyName">字：{{ poet.courtesyName }}</span>
              <span v-if="poet.pseudonym">号：{{ poet.pseudonym }}</span>
            </div>
            <div class="poet-meta">
              <span v-if="poet.dynastyName" class="meta-item">
                <el-icon><Calendar /></el-icon>
                {{ poet.dynastyName }}
              </span>
              <span v-if="poet.birthYear" class="meta-item">
                <el-icon><Timer /></el-icon>
                {{ poet.birthYear }}年 - {{ poet.deathYear ? poet.deathYear + '年' : '至今' }}
              </span>
              <span v-if="poet.birthplace" class="meta-item">
                <el-icon><Location /></el-icon>
                {{ poet.birthplace }}
              </span>
            </div>
          </div>
        </div>

        <div class="poet-biography" v-if="poet.description">
          <h2 class="section-title">生平简介</h2>
          <p class="biography-text">{{ poet.description }}</p>
        </div>
      </el-card>

      <div class="poems-section">
        <h2 class="section-title">诗词作品</h2>

        <div class="poems-list">
          <el-empty v-if="poems.length === 0" description="暂无作品" />

          <div
            v-for="poem in poems"
            :key="poem.id"
            class="poem-item"
            @click="goToPoemDetail(poem.id)"
          >
            <h3 class="poem-title">{{ poem.title }}</h3>
            <p class="poem-content">{{ poem.content }}</p>
            <div class="poem-footer">
              <div class="poem-source">
                <span v-if="poem.dynastyName">{{ poem.dynastyName }}</span>
                <span v-if="poem.poetName"> · {{ poem.poetName }}</span>
              </div>
              <div class="poem-stats">
                <span class="stat-item">
                  <el-icon><View /></el-icon>
                  {{ poem.viewCount }}
                </span>
                <span class="stat-item">
                  <el-icon><Star /></el-icon>
                  {{ poem.likeCount }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="poem-pagination" v-if="poemTotal > poemSize">
          <el-pagination
            v-model:current-page="poemPage"
            :page-size="poemSize"
            :total="poemTotal"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.poet-detail-page {
  padding: $spacing-xl 0;
  min-height: 60vh;
}

.poet-header {
  margin-bottom: $spacing-lg;
}

.back-button {
  margin-bottom: $spacing-md;
}

.poet-card {
  margin-bottom: $spacing-xl;
  border-radius: $border-radius-md;
}

.poet-profile {
  display: flex;
  gap: $spacing-xl;
  align-items: flex-start;
  margin-bottom: $spacing-xl;
  padding-bottom: $spacing-xl;
  border-bottom: 1px solid $border-color;

  @include responsive(md) {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
}

.poet-avatar {
  flex-shrink: 0;
}

.poet-basic {
  flex: 1;
}

.poet-name {
  font-size: $font-size-hero;
  color: $primary-color;
  font-family: $font-family-title;
  margin-bottom: $spacing-sm;
}

.poet-alias {
  display: flex;
  gap: $spacing-lg;
  font-size: $font-size-base;
  color: $text-color-secondary;
  margin-bottom: $spacing-md;

  @include responsive(md) {
    justify-content: center;
  }
}

.poet-meta {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-lg;

  @include responsive(md) {
    justify-content: center;
  }
}

.meta-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-base;
  color: $text-color-secondary;
}

.poet-biography {
  margin-top: $spacing-md;
}

.section-title {
  font-size: $font-size-xl;
  color: $text-color;
  margin-bottom: $spacing-lg;
  padding-bottom: $spacing-sm;
  border-bottom: 2px solid $primary-color;
  display: inline-block;
}

.biography-text {
  font-size: $font-size-base;
  color: $text-color;
  line-height: $line-height-loose;
  white-space: pre-line;
}

.poems-section {
  margin-top: $spacing-xl;
}

.poems-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.poem-item {
  @include card;
  cursor: pointer;
  padding: $spacing-lg;
  transition: box-shadow $transition-slow;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  }
}

.poem-title {
  font-size: $font-size-xl;
  color: $text-color;
  margin-bottom: $spacing-sm;
}

.poem-content {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: $line-height-loose;
  @include text-clamp(3);
  margin-bottom: $spacing-md;
}

.poem-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.poem-source {
  font-size: $font-size-sm;
  color: $text-color-light;
}

.poem-stats {
  display: flex;
  gap: $spacing-md;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-light;
}

.poem-pagination {
  margin-top: $spacing-xl;
  display: flex;
  justify-content: center;
}
</style>
