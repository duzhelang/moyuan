<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { globalSearch } from '@/api/modules/poem'
import type { Poem, Poet, ForumPost } from '@/types/model'

const route = useRoute()
const router = useRouter()
const keyword = ref((route.query.keyword as string) || '')
const loading = ref(false)
const searched = ref(false)
const activeTab = ref('poems')
const results = ref<{ poems: Poem[]; poets: Poet[]; posts: ForumPost[] }>({
  poems: [],
  poets: [],
  posts: []
})

const hasResults = computed(() =>
  results.value.poems.length > 0 || results.value.poets.length > 0 || results.value.posts.length > 0
)

const doSearch = async () => {
  const q = keyword.value.trim()
  if (!q) return
  loading.value = true
  searched.value = true
  try {
    const res = await globalSearch(q)
    results.value = {
      poems: (res.data as any)?.poems || [],
      poets: (res.data as any)?.poets || [],
      posts: (res.data as any)?.posts || []
    }
    if (results.value.poems.length > 0) activeTab.value = 'poems'
    else if (results.value.poets.length > 0) activeTab.value = 'poets'
    else if (results.value.posts.length > 0) activeTab.value = 'posts'
    router.replace({ query: { keyword: q } })
  } catch (error) {
    console.error('搜索失败')
  } finally {
    loading.value = false
  }
}

const truncateContent = (content: string, maxLen = 80) => {
  if (!content) return ''
  return content.length > maxLen ? content.substring(0, maxLen) + '...' : content
}

watch(() => route.query.keyword, (val) => {
  if (val && typeof val === 'string' && val !== keyword.value) {
    keyword.value = val
    doSearch()
  }
})

onMounted(() => {
  if (keyword.value) doSearch()
})
</script>

<template>
  <div class="search-page">
    <div class="search-header">
      <h1 class="search-title">搜索结果</h1>
      <div class="search-input">
        <el-input
          v-model="keyword"
          placeholder="搜索诗词、诗人、帖子..."
          size="large"
          @keyup.enter="doSearch"
        >
          <template #append>
            <el-button @click="doSearch">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>
    </div>

    <div v-loading="loading" class="search-results">
      <template v-if="hasResults">
        <el-tabs v-model="activeTab">
          <el-tab-pane :label="`诗词 (${results.poems.length})`" name="poems">
            <div class="result-list">
              <div
                v-for="poem in results.poems"
                :key="poem.id"
                class="result-item poem-result"
                @click="router.push(`/poem/${poem.id}`)"
              >
                <h3 class="result-title">{{ poem.title }}</h3>
                <div class="result-meta">
                  <span v-if="poem.poetName">{{ poem.poetName }}</span>
                  <span v-if="poem.dynastyName">{{ poem.dynastyName }}</span>
                </div>
                <p class="result-content">{{ truncateContent(poem.content) }}</p>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="`诗人 (${results.poets.length})`" name="poets">
            <div class="result-list">
              <div
                v-for="poet in results.poets"
                :key="poet.id"
                class="result-item poet-result"
                @click="router.push(`/poet/${poet.id}`)"
              >
                <div class="poet-brief">
                  <el-avatar :src="poet.avatar" :size="48">{{ poet.name?.charAt(0) }}</el-avatar>
                  <div class="poet-info">
                    <h3 class="result-title">{{ poet.name }}</h3>
                    <span v-if="poet.dynastyName" class="result-meta">{{ poet.dynastyName }}</span>
                  </div>
                </div>
                <p v-if="poet.description" class="result-content">{{ truncateContent(poet.description) }}</p>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="`帖子 (${results.posts.length})`" name="posts">
            <div class="result-list">
              <div
                v-for="post in results.posts"
                :key="post.id"
                class="result-item post-result"
                @click="router.push(`/forum/${post.id}`)"
              >
                <h3 class="result-title">{{ post.title }}</h3>
                <p class="result-content">{{ truncateContent(post.content) }}</p>
                <div class="result-meta">
                  <span>
                    <el-icon><View /></el-icon>
                    {{ post.viewCount }}
                  </span>
                  <span>
                    <el-icon><ChatDotRound /></el-icon>
                    {{ post.commentCount }}
                  </span>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>

      <el-empty v-else-if="!loading && searched" description="未找到相关结果" />
      <el-empty v-else-if="!loading && !searched" description="请输入关键词搜索" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.search-page {
  padding: $spacing-xl;
  max-width: 900px;
  margin: 0 auto;
}

.search-header {
  text-align: center;
  margin-bottom: $spacing-xxl;
}

.search-title {
  font-size: $font-size-title;
  color: $primary-color;
  font-family: $font-family-title;
  margin-bottom: $spacing-lg;
}

.search-input {
  max-width: 600px;
  margin: 0 auto;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.result-item {
  @include card;
  cursor: pointer;
}

.result-title {
  font-size: $font-size-lg;
  color: $text-color;
  margin-bottom: $spacing-sm;
}

.result-meta {
  display: flex;
  gap: $spacing-md;
  font-size: $font-size-sm;
  color: $text-color-secondary;
  margin-bottom: $spacing-sm;
}

.result-content {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: $line-height-base;
}

.poet-brief {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-sm;
}

.poet-info {
  .result-title {
    margin-bottom: $spacing-xs;
  }

  .result-meta {
    margin-bottom: 0;
  }
}

.post-result .result-meta {
  margin-top: $spacing-sm;
  margin-bottom: 0;

  span {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
  }
}
</style>