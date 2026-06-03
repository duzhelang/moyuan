<script setup lang="ts">
import { ref } from 'vue'
import { searchPoetryByTitle, searchPoetryByAuthor, getRandomPoetry } from '@/api/modules/external-poetry'
import type { PoetryDbPoem } from '@/api/modules/external-poetry'

const searchQuery = ref('')
const searchType = ref<'title' | 'author'>('title')
const searchResults = ref<PoetryDbPoem[]>([])
const loading = ref(false)
const error = ref('')
const selectedPoem = ref<PoetryDbPoem | null>(null)

const handleSearch = async () => {
  if (!searchQuery.value.trim()) {
    error.value = '请输入搜索内容'
    return
  }

  loading.value = true
  error.value = ''
  searchResults.value = []
  selectedPoem.value = null

  try {
    let results: PoetryDbPoem[] = []
    if (searchType.value === 'title') {
      results = await searchPoetryByTitle(searchQuery.value)
    } else {
      results = await searchPoetryByAuthor(searchQuery.value)
    }

    if (results.length === 0) {
      error.value = '未找到相关诗词'
    } else {
      searchResults.value = results
    }
  } catch {
    error.value = '搜索失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const handleRandom = async () => {
  loading.value = true
  error.value = ''
  searchResults.value = []
  selectedPoem.value = null

  try {
    const results = await getRandomPoetry(5)
    searchResults.value = results
  } catch {
    error.value = '获取随机诗词失败'
  } finally {
    loading.value = false
  }
}

const selectPoem = (poem: PoetryDbPoem) => {
  selectedPoem.value = poem
}

const closeDetail = () => {
  selectedPoem.value = null
}
</script>

<template>
  <div class="poetry-search">
    <div class="search-header">
      <h2>英文诗词搜索</h2>
      <p class="search-subtitle">PoetryDB · 探索经典英文诗歌</p>
    </div>

    <div class="search-form">
      <div class="search-type-tabs">
        <button
          :class="['type-tab', { active: searchType === 'title' }]"
          @click="searchType = 'title'"
        >
          按标题搜索
        </button>
        <button
          :class="['type-tab', { active: searchType === 'author' }]"
          @click="searchType = 'author'"
        >
          按作者搜索
        </button>
      </div>
      <div class="search-input-group">
        <input
          v-model="searchQuery"
          :placeholder="searchType === 'title' ? '例如: Shall I compare' : '例如: Shakespeare'"
          @keyup.enter="handleSearch"
          class="search-input"
        />
        <button @click="handleSearch" :disabled="loading" class="search-btn">
          {{ loading ? '搜索中...' : '搜索' }}
        </button>
      </div>
      <button @click="handleRandom" :disabled="loading" class="random-btn">
        随机推荐
      </button>
    </div>

    <div v-if="error" class="error-message">
      {{ error }}
    </div>

    <div v-if="searchResults.length > 0" class="results-container">
      <div class="results-header">
        <span>找到 {{ searchResults.length }} 首诗词</span>
      </div>
      <div class="results-list">
        <div
          v-for="(poem, index) in searchResults"
          :key="index"
          class="poem-item"
          @click="selectPoem(poem)"
        >
          <div class="poem-item-header">
            <h3 class="poem-item-title">{{ poem.title }}</h3>
            <span class="poem-item-author">{{ poem.author }}</span>
          </div>
          <div class="poem-item-preview">
            {{ poem.lines.slice(0, 2).join(' / ') }}...
          </div>
          <div class="poem-item-meta">
            <span>{{ poem.linecount }} 行</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="selectedPoem" class="poem-detail-overlay" @click.self="closeDetail">
      <div class="poem-detail">
        <button class="close-btn" @click="closeDetail">×</button>
        <h2 class="detail-title">{{ selectedPoem.title }}</h2>
        <p class="detail-author">{{ selectedPoem.author }}</p>
        <div class="detail-lines">
          <p v-for="(line, i) in selectedPoem.lines" :key="i" class="detail-line">
            {{ line }}
          </p>
        </div>
        <div class="detail-meta">
          <span>共 {{ selectedPoem.linecount }} 行</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.poetry-search {
  background: linear-gradient(135deg, rgba(85, 107, 47, 0.12), rgba(107, 142, 35, 0.18));
  border-radius: 16px;
  padding: 24px;
  margin: 20px auto;
  max-width: 800px;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(85, 107, 47, 0.2);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.search-header {
  text-align: center;
  margin-bottom: 24px;
}

.search-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #2E5916;
  margin: 0 0 8px;
  font-family: cursive;
}

.search-subtitle {
  font-size: 14px;
  color: #556B2F;
  margin: 0;
  letter-spacing: 2px;
}

.search-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 20px;
}

.search-type-tabs {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.type-tab {
  padding: 8px 20px;
  border: 1px solid rgba(85, 107, 47, 0.3);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.3);
  color: #556B2F;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.type-tab:hover {
  border-color: #556B2F;
  background: rgba(85, 107, 47, 0.1);
}

.type-tab.active {
  background: #556B2F;
  color: white;
  border-color: #556B2F;
}

.search-input-group {
  display: flex;
  gap: 12px;
}

.search-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid rgba(85, 107, 47, 0.3);
  border-radius: 10px;
  font-size: 15px;
  background: rgba(255, 255, 255, 0.5);
  outline: none;
  transition: border-color 0.3s ease;
}

.search-input:focus {
  border-color: #556B2F;
}

.search-input::placeholder {
  color: #999;
}

.search-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #556B2F, #6B8E23);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.search-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(85, 107, 47, 0.4);
}

.search-btn:disabled {
  background: #c3c3c3;
  cursor: not-allowed;
}

.random-btn {
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.4);
  color: #556B2F;
  border: 1px solid rgba(85, 107, 47, 0.3);
  border-radius: 10px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  align-self: center;
}

.random-btn:hover:not(:disabled) {
  background: rgba(85, 107, 47, 0.1);
  border-color: #556B2F;
}

.random-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-message {
  text-align: center;
  color: #e74c3c;
  padding: 12px;
  background: rgba(231, 76, 60, 0.1);
  border-radius: 8px;
  margin-bottom: 16px;
}

.results-container {
  margin-top: 20px;
}

.results-header {
  text-align: center;
  margin-bottom: 16px;
  color: #666;
  font-size: 14px;
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.poem-item {
  padding: 16px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  border: 1px solid rgba(85, 107, 47, 0.15);
  cursor: pointer;
  transition: all 0.3s ease;
}

.poem-item:hover {
  background: rgba(255, 255, 255, 0.7);
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.poem-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.poem-item-title {
  font-size: 16px;
  font-weight: 600;
  color: #2E5916;
  margin: 0;
}

.poem-item-author {
  font-size: 14px;
  color: #666;
  font-style: italic;
}

.poem-item-preview {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.poem-item-meta {
  font-size: 12px;
  color: #999;
  text-align: right;
}

.poem-detail-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(5px);
}

.poem-detail {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(245, 245, 245, 0.95));
  border-radius: 16px;
  padding: 32px;
  max-width: 600px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  position: relative;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s ease;
}

.close-btn:hover {
  background: rgba(0, 0, 0, 0.2);
}

.detail-title {
  font-size: 24px;
  font-weight: 700;
  color: #2E5916;
  margin: 0 0 8px;
  text-align: center;
  font-family: cursive;
}

.detail-author {
  font-size: 16px;
  color: #666;
  text-align: center;
  margin: 0 0 24px;
  font-style: italic;
}

.detail-lines {
  padding: 20px;
  background: rgba(85, 107, 47, 0.08);
  border-radius: 12px;
  margin-bottom: 20px;
}

.detail-line {
  font-size: 15px;
  color: #333;
  line-height: 2;
  margin: 0;
  text-align: center;
}

.detail-meta {
  text-align: center;
  color: #999;
  font-size: 14px;
}
</style>
