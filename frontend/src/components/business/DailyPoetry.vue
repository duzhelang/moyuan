<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDailyPoetry } from '@/api/modules/external-poetry'
import type { JinrishiciResponse } from '@/api/modules/external-poetry'

const router = useRouter()
const dailyPoetry = ref<JinrishiciResponse['data'] | null>(null)
const loading = ref(true)
const error = ref(false)

const fetchDailyPoetry = async () => {
  loading.value = true
  error.value = false
  try {
    const data = await getDailyPoetry()
    dailyPoetry.value = data
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

const goToPoemSearch = () => {
  if (dailyPoetry.value?.origin?.title) {
    router.push({ path: '/poem', query: { keyword: dailyPoetry.value.origin.title } })
  }
}

const goToAuthorSearch = () => {
  if (dailyPoetry.value?.origin?.author) {
    router.push({ path: '/poem', query: { keyword: dailyPoetry.value.origin.author } })
  }
}

const goToContentSearch = () => {
  if (dailyPoetry.value?.origin?.title) {
    router.push({ path: '/poem', query: { keyword: dailyPoetry.value.origin.title } })
  }
}

onMounted(() => {
  fetchDailyPoetry()
})
</script>

<template>
  <div class="daily-poetry">
    <div class="daily-poetry-header">
      <h2>每日诗词</h2>
      <p class="daily-poetry-subtitle">今日推荐 · 品味经典</p>
    </div>
    <div v-if="loading" class="daily-poetry-loading">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>
    <div v-else-if="error" class="daily-poetry-error">
      <p>加载失败，点击重试</p>
      <button @click="fetchDailyPoetry" class="retry-btn">重新加载</button>
    </div>
    <div v-else-if="dailyPoetry" class="daily-poetry-content">
      <div class="poetry-text" @click="goToContentSearch" title="点击查看整首诗">
        <p class="poetry-line">{{ dailyPoetry.content }}</p>
      </div>
      <div class="poetry-info">
        <span class="poetry-title" @click.stop="goToPoemSearch" title="点击查看相关诗词">{{ dailyPoetry.origin.title }}</span>
        <span class="poetry-author" @click.stop="goToAuthorSearch" title="点击查看该诗人作品">〔{{ dailyPoetry.origin.dynasty }}〕{{ dailyPoetry.origin.author }}</span>
      </div>
      <div v-if="dailyPoetry.origin.translate && dailyPoetry.origin.translate.length > 0" class="poetry-translate">
        <p class="translate-label">译文：</p>
        <p class="translate-text">{{ dailyPoetry.origin.translate[0] }}</p>
      </div>
      <div v-if="dailyPoetry.matchTags && dailyPoetry.matchTags.length > 0" class="poetry-tags">
        <span v-for="tag in dailyPoetry.matchTags" :key="tag" class="tag">{{ tag }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.daily-poetry {
  background: linear-gradient(135deg, rgba(139, 69, 19, 0.12), rgba(210, 105, 30, 0.18));
  border-radius: 16px;
  padding: 24px;
  margin: 20px auto;
  max-width: 600px;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(139, 69, 19, 0.2);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.daily-poetry-header {
  text-align: center;
  margin-bottom: 20px;
}

.daily-poetry-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #4A2C1A;
  margin: 0 0 8px;
  font-family: cursive;
}

.daily-poetry-subtitle {
  font-size: 14px;
  color: #8B6914;
  margin: 0;
  letter-spacing: 2px;
}

.daily-poetry-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  gap: 12px;
  color: #666;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid rgba(139, 69, 19, 0.2);
  border-top-color: #8B4513;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.daily-poetry-error {
  text-align: center;
  padding: 30px 0;
  color: #999;
}

.retry-btn {
  margin-top: 12px;
  padding: 8px 20px;
  background: linear-gradient(135deg, #8B4513, #A0522D);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.retry-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(139, 69, 19, 0.3);
}

.daily-poetry-content {
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.poetry-text {
  text-align: center;
  margin-bottom: 20px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.poetry-text:hover {
  background: rgba(255, 255, 255, 0.6);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(139, 69, 19, 0.15);
}

.poetry-line {
  font-size: 22px;
  color: #4A2C1A;
  line-height: 1.8;
  margin: 0;
  font-family: "STKaiti", "KaiTi", cursive;
  letter-spacing: 2px;
}

.poetry-info {
  text-align: center;
  margin-bottom: 16px;
}

.poetry-title {
  display: block;
  font-size: 16px;
  color: #8B4513;
  font-weight: 600;
  margin-bottom: 4px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.poetry-title:hover {
  color: #A0522D;
  text-decoration: underline;
}

.poetry-author {
  display: block;
  font-size: 14px;
  color: #666;
  font-style: italic;
  cursor: pointer;
  transition: color 0.2s ease;
}

.poetry-author:hover {
  color: #8B4513;
  text-decoration: underline;
}

.poetry-translate {
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  border-left: 3px solid #8B4513;
}

.translate-label {
  font-size: 13px;
  color: #8B6914;
  margin: 0 0 6px;
  font-weight: 500;
}

.translate-text {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
  margin: 0;
}

.poetry-tags {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.tag {
  padding: 4px 12px;
  background: rgba(139, 69, 19, 0.1);
  color: #8B4513;
  border-radius: 20px;
  font-size: 12px;
  border: 1px solid rgba(139, 69, 19, 0.2);
}
</style>
