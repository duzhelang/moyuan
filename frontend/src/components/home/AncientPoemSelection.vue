<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { searchPoemsWithRecommend } from '@/api/modules/external-poetry'
import type { PoemSearchResult } from '@/api/modules/external-poetry'
import type { StrategyKey } from '@/api/modules/recommend'
import { useUserStore } from '@/stores/user'
import { useRecommendStrategy } from '@/composables/useRecommendStrategy'
import RecommendStrategySwitch from '@/components/recommend/RecommendStrategySwitch.vue'
import RecommendPanel from '@/components/recommend/RecommendPanel.vue'
import RecommendCompareDialog from '@/components/recommend/RecommendCompareDialog.vue'
import PoetryDetailDialog from '@/components/business/PoetryDetailDialog.vue'
import { usePoemCardNavigation } from '@/composables/usePoemCardNavigation'
import ancientPoemsData from '@/data/home-ancient-poems.json'

const userStore = useUserStore()
const route = useRoute()

// 卡片点击导航：本地命中跳 /poem/:id，未命中弹窗兜底（同时阻止冒泡，避免触发面板焦点切换）
const {
  poetryDialogVisible,
  poetryDialogTitle,
  poetryDialogAuthor,
  openPoemDetail,
  openStaticPoemDetail
} = usePoemCardNavigation()

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

// ==================== 推荐策略切换（附录 A） ====================

const {
  strategy: currentStrategy,
  availableStrategies,
  options: strategyOptions,
  items: recommendedPoems,
  loading: recommendLoading,
  error: recommendError,
  fallbackNotice,
  load: loadRecommendations,
  switchTo: switchStrategy,
  retry: retryRecommendation,
  backToRule: backToRuleStrategy,
  loadModels
} = useRecommendStrategy({ topK: 6, persist: true })

const showRecommendedPoems = async () => {
  poemSearchMode.value = 'recommend'
  await loadModels()
  if (recommendedPoems.value.length === 0 && !recommendLoading.value) {
    await loadRecommendations()
  }
}

// 「为您推荐」推荐依据：登录用户走用户画像匹配 + 兴趣标签建模，未登录回退全站热度排序
const recommendBasis = computed(() =>
  userStore.isLoggedIn ? '用户画像匹配 + 兴趣标签建模' : '全站热度排序（浏览量 · 点赞量）'
)

// 工具栏左侧依据文案随策略变化
const toolbarBasis = computed(() => {
  const option = strategyOptions.value.find(o => o.key === currentStrategy.value)
  return option ? `推荐依据：${option.hint}` : `推荐依据：${recommendBasis.value}`
})

const canCompare = computed(
  () => userStore.isLoggedIn && availableStrategies.value.length >= 2
)

// 对比弹窗：默认左「规则」右「融合」，右列可在可用策略中切换
const compareVisible = ref(false)
const compareLeft = ref<StrategyKey>('rule')
const compareRight = ref<StrategyKey>('fusion')

const openCompare = () => {
  if (!canCompare.value) return
  compareLeft.value = 'rule'
  const preferFusion = availableStrategies.value.includes('fusion')
  const firstModelStrategy = availableStrategies.value.find(key => key !== 'rule')
  compareRight.value = preferFusion
    ? 'fusion'
    : firstModelStrategy ?? 'rule'
  compareVisible.value = true
}

const isValidStrategyParam = (value: unknown): value is StrategyKey =>
  typeof value === 'string' && availableStrategies.value.includes(value as StrategyKey)

// URL 直链（附录 A.8.3）：?strategy=fusion / ?compare=1&left=rule&right=fusion
// 参数非法或策略不可用时回落规则，不报错；对比条件不满足时忽略
onMounted(async () => {
  const strategyParam = typeof route.query.strategy === 'string' ? route.query.strategy : ''
  const compareParam = route.query.compare === '1'
  if (!strategyParam && !compareParam) return

  poemSearchMode.value = 'recommend'
  await loadModels(true)

  if (strategyParam && isValidStrategyParam(strategyParam)) {
    if (strategyParam !== currentStrategy.value) {
      await switchStrategy(strategyParam)
    } else if (recommendedPoems.value.length === 0 && !recommendLoading.value) {
      await loadRecommendations()
    }
  } else {
    // 参数缺失/非法回落规则推荐
    if (recommendedPoems.value.length === 0 && !recommendLoading.value) {
      await loadRecommendations()
    }
  }

  if (compareParam && canCompare.value) {
    compareLeft.value = isValidStrategyParam(route.query.left) ? route.query.left : 'rule'
    compareRight.value = isValidStrategyParam(route.query.right)
      ? route.query.right
      : availableStrategies.value.includes('fusion')
        ? 'fusion'
        : availableStrategies.value.find(key => key !== 'rule') ?? 'rule'
    compareVisible.value = true
  }
})

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
</script>

<template>
  <div class="poem_selection_left">
    <div class="panel-content">
      <h2 class="luntan_section_title">古诗词推选</h2>
      <div class="luntan_jx">
        <div class="luntan_jx_title">
          <el-tooltip content="基于编辑精选经典名篇语料库" placement="bottom" :show-after="80" popper-class="moyuan-poem-tip">
            <span @click="showDefaultPoems" :class="{ active: poemSearchMode === 'default' }">历史的印痕</span>
          </el-tooltip>
          <el-tooltip :content="recommendBasis" placement="bottom" :show-after="80" popper-class="moyuan-poem-tip">
            <span @click="showRecommendedPoems" :class="{ active: poemSearchMode === 'recommend' }" class="recommend_btn">为您推荐</span>
          </el-tooltip>
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
        <!-- 推荐策略工具栏：只在「为您推荐」页签显示（附录 A.1.2 / A.5.1） -->
        <div v-if="poemSearchMode === 'recommend'" class="recommend_toolbar">
          <span class="recommend_basis">{{ toolbarBasis }}</span>
          <div class="recommend_toolbar_right">
            <RecommendStrategySwitch
              :model-value="currentStrategy"
              :options="strategyOptions"
              @update:model-value="switchStrategy"
            />
            <button
              v-if="canCompare"
              type="button"
              class="recommend_compare_btn"
              @click="openCompare"
            >
              对比
            </button>
          </div>
        </div>
        <!-- 后端缓存未命中/版本下线的轻提示：淡入，不弹 toast、不遮挡卡片 -->
        <transition name="recommend-notice">
          <div v-if="fallbackNotice" class="recommend_fallback_notice">{{ fallbackNotice }}</div>
        </transition>
        <div class="luntan_jx_scroll">
          <template v-if="poemSearchMode === 'default'">
            <div
              v-for="(poem, idx) in ancientPoems"
              :key="'ancient-' + idx"
              class="poem_card"
              @click.stop="openStaticPoemDetail(poem.title || '', poem.author, poem.content)"
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
                @click.stop="openPoemDetail(poem)"
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
            <RecommendPanel
              :items="recommendedPoems"
              :loading="recommendLoading"
              :error="recommendError"
              :is-rule-strategy="currentStrategy === 'rule'"
              :top-k="6"
              @retry="retryRecommendation"
              @switch-rule="backToRuleStrategy"
              @open="openPoemDetail"
            />
          </template>
        </div>
      </div>
    </div>
    <button type="button" class="panel-spine" aria-label="展开「古诗词推选」">
      <span class="panel-spine-title">古诗词推选</span>
      <span class="panel-spine-icon">❯</span>
    </button>
  </div>

  <PoetryDetailDialog
    v-model:visible="poetryDialogVisible"
    :title="poetryDialogTitle"
    :author="poetryDialogAuthor"
  />

  <RecommendCompareDialog
    v-model="compareVisible"
    :left="compareLeft"
    :right="compareRight"
    :top-k="10"
  />
</template>

<style scoped>
/* 面板骨架布局（.poem_selection_left / .panel-content / .panel-spine）统一在
   home-styles.scss 中维护，避免两处样式互相覆盖。此处只保留内容层内部样式。 */

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
  overflow: hidden;
  display: flex;
  flex-direction: column;
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
  align-content: flex-start;
  gap: 16px;
  flex: 1;
  min-height: 0;
  max-height: none;
  overflow-y: auto;
}

/* 窄屏下两栏改为上下堆叠，此时给滚动区固定高度以保留滚动条 */
@media (max-width: 768px) {
  .luntan_jx_scroll {
    flex: none;
    max-height: 462px;
  }
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
  cursor: pointer;
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

/* ==================== 推荐策略工具栏（附录 A.5.1） ==================== */

.recommend_toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin: 0 0 8px;
}

.recommend_basis {
  font-size: 12px;
  color: #7a5c37;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recommend_toolbar_right {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  /* 标签不换行；空间不足时容器横向滚动，不挤压卡片区 */
  overflow-x: auto;
  scrollbar-width: none;
}

.recommend_toolbar_right::-webkit-scrollbar {
  display: none;
}

.recommend_compare_btn {
  flex-shrink: 0;
  height: 30px;
  padding: 0 12px;
  font-size: 13px;
  color: #6b4d2e;
  background: rgba(255, 252, 245, 0.6);
  border: 1px solid rgba(138, 90, 43, 0.25);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.18s ease;
  font-family: inherit;
}

.recommend_compare_btn:hover {
  background: rgba(138, 90, 43, 0.08);
  border-color: rgba(138, 90, 43, 0.45);
}

.recommend_fallback_notice {
  font-size: 12px;
  color: #7a5c37;
  background: rgba(138, 90, 43, 0.08);
  border: 1px solid rgba(138, 90, 43, 0.2);
  border-radius: 6px;
  padding: 4px 10px;
  margin: 0 0 8px;
}

.recommend-notice-enter-active {
  transition: opacity 0.2s ease;
}

.recommend-notice-enter-from {
  opacity: 0;
}

.recommend-notice-leave-active {
  transition: opacity 0.15s ease;
}

.recommend-notice-leave-to {
  opacity: 0;
}

/* 移动端（<768px）：依据与标签分两行，标签横向滚动、高度 28px 字号 12px（附录 A.5.3） */
@media (max-width: 768px) {
  .recommend_toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .recommend_toolbar_right {
    padding: 0 4px;
  }

  .recommend_toolbar_right :deep(.rs_tab) {
    height: 28px;
    padding: 0 10px;
    font-size: 12px;
  }

  .recommend_toolbar_right :deep(.rs_metric) {
    font-size: 10px;
  }
}

</style>

<style>
/* Element Plus 悬浮提示：墨渊铜金质感 */
.moyuan-poem-tip.el-popper {
  background: linear-gradient(135deg, #8B4513, #a2591f) !important;
  border: 1px solid rgba(255, 248, 238, 0.28) !important;
  color: #fff8ee !important;
  font-family: "Noto Serif SC", "STSong", "SimSun", serif !important;
  font-size: 13px !important;
  letter-spacing: 2px;
  border-radius: 8px !important;
  box-shadow: 0 8px 20px rgba(139, 69, 19, 0.3) !important;
  padding: 7px 14px !important;
}

.moyuan-poem-tip.el-popper .el-popper__arrow::before {
  background: #8B4513 !important;
  border: 1px solid rgba(255, 248, 238, 0.28) !important;
}
</style>
