<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { searchPoemsWithRecommend, getRecommendedPoems } from '@/api/modules/external-poetry'
import type { PoemSearchResult } from '@/api/modules/external-poetry'
import PoetryDetailDialog from '@/components/business/PoetryDetailDialog.vue'
import ancientPoemsData from '@/data/home-ancient-poems.json'

const router = useRouter()

interface PoemCard {
  date: string
  title?: string
  content: string
  author: string
  image?: string
}

const ancientPoems: PoemCard[] = ancientPoemsData

const asset = (path: string) => path

const poemSearchKeyword = ref('')
const poemSearchResults = ref<PoemSearchResult[]>([])
const poemSearchLoading = ref(false)
const poemSearchMode = ref<'default' | 'search' | 'recommend'>('default')
const recommendedPoems = ref<PoemSearchResult[]>([])

const handlePoemSearch = async () => {
  if (!poemSearchKeyword.value.trim()) {
    poemSearchMode.value = 'default'
    poemSearchResults.value = []
    return
  }
  poemSearchLoading.value = true
  poemSearchMode.value = 'search'
  try {
    poemSearchResults.value = await searchPoemsWithRecommend(poemSearchKeyword.value.trim())
  } catch (error) {
    console.error('搜索诗词失败:', error)
    ElMessage.error('搜索失败，请稍后重试')
  } finally {
    poemSearchLoading.value = false
  }
}

const fetchRecommendedPoems = async () => {
  try {
    recommendedPoems.value = await getRecommendedPoems(6)
  } catch (error) {
    console.error('获取推荐诗词失败:', error)
  }
}

const showRecommendedPoems = async () => {
  poemSearchMode.value = 'recommend'
  if (recommendedPoems.value.length === 0) {
    await fetchRecommendedPoems()
  }
}

const showDefaultPoems = () => {
  poemSearchMode.value = 'default'
  poemSearchKeyword.value = ''
  poemSearchResults.value = []
}

const clearPoemSearch = () => {
  poemSearchKeyword.value = ''
  poemSearchResults.value = []
  poemSearchMode.value = 'default'
}

const poetryDialogVisible = ref(false)
const poetryDialogTitle = ref('')
const poetryDialogAuthor = ref('')

const openPoemDetail = (poem: PoemSearchResult) => {
  if (poem.source === 'local' && poem.id) {
    router.push(`/poem/${poem.id}`)
  } else {
    poetryDialogTitle.value = poem.title
    poetryDialogAuthor.value = poem.author || ''
    poetryDialogVisible.value = true
  }
}
</script>

<template>
  <div class="poem_selection_left">
    <h2 class="luntan_section_title">古诗词推选</h2>
    <div class="luntan_jx">
      <div class="luntan_jx_title">
        <span @click="showDefaultPoems" :class="{ active: poemSearchMode === 'default' }">历史的印痕</span>
        <span @click="showRecommendedPoems" :class="{ active: poemSearchMode === 'recommend' }" class="recommend_btn">为您推荐</span>
      </div>
      <div class="poem_search_bar">
        <input
          v-model="poemSearchKeyword"
          class="poem_search_input"
          placeholder="搜索古诗词..."
          @keyup.enter="handlePoemSearch"
        />
        <button class="poem_search_btn" @click="handlePoemSearch" :disabled="poemSearchLoading">
          {{ poemSearchLoading ? '搜索中...' : '搜索' }}
        </button>
        <button v-if="poemSearchMode === 'search'" class="poem_search_clear" @click="clearPoemSearch">清除</button>
      </div>
      <div class="luntan_jx_scroll">
        <template v-if="poemSearchMode === 'default'">
          <div
            v-for="(poem, idx) in ancientPoems"
            :key="'ancient-' + idx"
            class="poem_card"
          >
            <div class="poem_card_img">
              <img :src="asset(poem.image ?? '')" :alt="poem.title || '诗词配图'">
              <div class="poem_card_img_overlay"></div>
            </div>
            <div class="poem_card_body">
              <div v-if="poem.title" class="poem_title">{{ poem.title }}</div>
              <div class="poem_meta">【{{ poem.date }}】{{ poem.author }}</div>
              <div class="poem_lines">
                <div v-for="(line, li) in poem.content.split('\n')" :key="li" class="poem_line">{{ line }}</div>
              </div>
            </div>
          </div>
        </template>

        <template v-if="poemSearchMode === 'search'">
          <div v-if="poemSearchLoading" class="poem_search_status">
            正在搜索...
          </div>
          <div v-else-if="poemSearchResults.length === 0" class="poem_search_status">
            未找到相关诗词
          </div>
          <template v-else>
            <div
              v-for="(poem, idx) in poemSearchResults"
              :key="'search-' + idx"
              class="poem_card poem_search_card"
              @click="openPoemDetail(poem)"
            >
              <div class="poem_card_body">
                <div class="poem_title">{{ poem.title }}</div>
                <div class="poem_meta">
                  <span v-if="poem.dynasty">【{{ poem.dynasty }}】</span>
                  <span v-if="poem.author">{{ poem.author }}</span>
                  <span v-if="poem.source === 'external'" class="poem_source_tag">古诗库</span>
                </div>
                <div class="poem_lines">
                  <div v-for="(line, li) in poem.content.split('\n')" :key="li" class="poem_line">{{ line }}</div>
                </div>
                <div v-if="poem.recommendReason" class="poem_recommend_reason">
                  <span class="recommend_icon">&#128161;</span> {{ poem.recommendReason }}
                </div>
              </div>
            </div>
          </template>
        </template>

        <template v-if="poemSearchMode === 'recommend'">
          <div v-if="recommendedPoems.length === 0" class="poem_search_status">
            暂无推荐，多浏览诗词会更精准哦~
          </div>
          <template v-else>
            <div
              v-for="(poem, idx) in recommendedPoems"
              :key="'recommend-' + idx"
              class="poem_card poem_recommend_card"
              @click="openPoemDetail(poem)"
            >
              <div class="poem_card_body">
                <div class="poem_title">{{ poem.title }}</div>
                <div class="poem_meta">
                  <span v-if="poem.dynasty">【{{ poem.dynasty }}】</span>
                  <span v-if="poem.author">{{ poem.author }}</span>
                </div>
                <div class="poem_lines">
                  <div v-for="(line, li) in poem.content.split('\n')" :key="li" class="poem_line">{{ line }}</div>
                </div>
                <div v-if="poem.recommendReason" class="poem_recommend_reason">
                  <span class="recommend_icon">&#128161;</span> {{ poem.recommendReason }}
                </div>
              </div>
            </div>
          </template>
        </template>
      </div>
    </div>
  </div>

  <PoetryDetailDialog
    v-model:visible="poetryDialogVisible"
    :title="poetryDialogTitle"
    :author="poetryDialogAuthor"
  />
</template>

<style scoped>
.poem_selection_left {
  flex: 1;
  min-width: 0;
  overflow: visible;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.luntan_section_title {
  font-family: cursive;
  text-align: center;
  font-size: 28px;
  color: #333;
  margin: 0 0 15px;
  padding: 0;
}

.luntan_jx {
  position: relative;
  width: 100%;
  border-radius: 16px;
  margin: 0;
  padding: 20px;
  box-sizing: border-box;
  overflow: visible;
  min-height: 500px;
  background: linear-gradient(135deg, rgba(139, 69, 19, 0.08), rgba(210, 105, 30, 0.12));
  border: 1px solid rgba(139, 69, 19, 0.15);
}

.luntan_jx_title {
  font-size: 36px;
  font-weight: bold;
  background: linear-gradient(to right, #ff7e5f, #feb47b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 16px;
  font-family: cursive;
  letter-spacing: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.luntan_jx_title span {
  cursor: pointer;
  transition: all 0.3s;
  -webkit-text-fill-color: initial;
  background-clip: initial;
  background: none;
  border-bottom: 2px solid transparent;
  padding-bottom: 4px;
}

.luntan_jx_title span:hover {
  opacity: 0.8;
  border-bottom-color: rgba(255, 126, 95, 0.5);
}

.luntan_jx_title span.active {
  border-bottom-color: #ff7e5f;
}

.luntan_jx_title .recommend_btn {
  font-size: 20px;
  color: #667eea;
}

.poem_search_bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.poem_search_input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid rgba(139, 69, 19, 0.2);
  border-radius: 8px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.8);
  transition: border-color 0.3s, box-shadow 0.3s;
}

.poem_search_input:focus {
  outline: none;
  border-color: #ff7e5f;
  box-shadow: 0 0 0 3px rgba(255, 126, 95, 0.1);
}

.poem_search_input::placeholder {
  color: #999;
}

.poem_search_btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #ff7e5f, #feb47b);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  white-space: nowrap;
}

.poem_search_btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 126, 95, 0.3);
}

.poem_search_btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.poem_search_clear {
  padding: 10px 16px;
  background: transparent;
  color: #999;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.poem_search_clear:hover {
  background: #f5f5f5;
  border-color: #ccc;
}

.luntan_jx_scroll {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  max-height: 450px;
  overflow-y: auto;
}

.luntan_jx_scroll::-webkit-scrollbar {
  width: 6px;
}

.luntan_jx_scroll::-webkit-scrollbar-thumb {
  background: rgba(139, 69, 19, 0.3);
  border-radius: 3px;
}

.luntan_jx_scroll::-webkit-scrollbar-track {
  background: transparent;
}

.poem_card {
  display: flex;
  align-items: stretch;
  gap: 0;
  background: linear-gradient(135deg, #fffbf5 0%, #fff8ee 50%, #fffbf5 100%);
  border-radius: 14px;
  border: 1px solid rgba(139, 69, 19, 0.15);
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
  position: relative;
  z-index: 1;
  box-shadow: 0 2px 12px rgba(139, 69, 19, 0.08);
}

.poem_card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 14px;
  border: 2px solid transparent;
  background: linear-gradient(135deg, rgba(139, 69, 19, 0.1), rgba(210, 105, 30, 0.08)) border-box;
  mask: linear-gradient(#fff, #fff) padding-box, linear-gradient(#fff, #fff);
  mask-composite: exclude;
  -webkit-mask-composite: xor;
  pointer-events: none;
}

.poem_card:hover {
  box-shadow: 0 12px 40px rgba(139, 69, 19, 0.18), 0 4px 12px rgba(0, 0, 0, 0.06);
  transform: translateY(-4px);
  background: linear-gradient(135deg, #fff 0%, #fff9f0 50%, #fff 100%);
  z-index: 10;
}

.poem_card_img {
  position: relative;
  width: 160px;
  min-height: 180px;
  flex-shrink: 0;
  overflow: hidden;
}

.poem_card_img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.poem_card:hover .poem_card_img img {
  transform: scale(1.08);
}

.poem_card_img_overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(139, 69, 19, 0.15), rgba(0, 0, 0, 0.25));
  transition: opacity 0.4s ease;
}

.poem_card:hover .poem_card_img_overlay {
  opacity: 0.6;
}

.poem_card_body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 20px;
  text-align: center;
  position: relative;
}

.poem_card_body::before {
  content: '';
  position: absolute;
  left: 0;
  top: 20%;
  bottom: 20%;
  width: 1px;
  background: linear-gradient(to bottom, transparent, rgba(139, 69, 19, 0.2), transparent);
}

.poem_card .poem_title {
  font-size: 20px;
  font-weight: 700;
  color: #8B4513;
  margin-bottom: 8px;
  letter-spacing: 3px;
  text-shadow: 0 1px 2px rgba(139, 69, 19, 0.1);
}

.poem_meta {
  font-size: 13px;
  color: #a0724e;
  margin-bottom: 16px;
  letter-spacing: 2px;
}

.poem_lines {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.poem_line {
  font-size: 16px;
  color: #333;
  letter-spacing: 2px;
  line-height: 2;
  font-family: "Noto Serif SC", "Source Han Serif SC", "STSong", "SimSun", serif;
}

.poem_search_card,
.poem_recommend_card {
  flex-direction: column;
}

.poem_search_card .poem_card_body,
.poem_recommend_card .poem_card_body {
  padding: 16px;
}

.poem_search_status {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
}

.poem_source_tag {
  display: inline-block;
  padding: 2px 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 4px;
  font-size: 12px;
  margin-left: 8px;
  vertical-align: middle;
}

.poem_recommend_reason {
  margin-top: 8px;
  padding: 6px 10px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08), rgba(118, 75, 162, 0.08));
  border-radius: 6px;
  font-size: 12px;
  color: #667eea;
}

.recommend_icon {
  margin-right: 4px;
}
</style>
