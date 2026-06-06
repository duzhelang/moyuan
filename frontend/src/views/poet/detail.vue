<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { Poet, Poem } from '@/types/model'
import { getPoetById } from '@/api/modules/poet'
import { getPoemsByPoet } from '@/api/modules/poem'
import PoetryDetailDialog from '@/components/business/PoetryDetailDialog.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const poet = ref<Poet | null>(null)
const poems = ref<Poem[]>([])
const poemTotal = ref(0)
const poemPage = ref(1)
const poemSize = ref(10)

const poetId = computed(() => Number(route.params.id))

const navSections = [
  { id: 'section-intro', label: '简　　介', icon: 'Document' },
  { id: 'section-life', label: '人物生平', icon: 'User' },
  { id: 'section-influence', label: '主要影响', icon: 'TrendCharts' },
  { id: 'section-evaluation', label: '历史评价', icon: 'ChatDotRound' },
  { id: 'section-anecdotes', label: '轶事典故', icon: 'Reading' },
  { id: 'section-poems', label: '诗词作品', icon: 'Notebook' },
]

const activeSection = ref('section-intro')
const sidebarCollapsed = ref(false)

const isReading = ref(false)
const currentReadingId = ref<string | null>(null)
let speechUtterance: SpeechSynthesisUtterance | null = null

const editingSection = ref<string | null>(null)
const editText = ref('')
const editTitle = ref('')

const isMusicPlaying = ref(false)
let audioElement: HTMLAudioElement | null = null

const poetryDialogVisible = ref(false)
const selectedPoemTitle = ref('')
const selectedPoemAuthor = ref('')

const openPoetryDetail = (poem: Poem) => {
  selectedPoemTitle.value = poem.title
  selectedPoemAuthor.value = poet.value?.name || ''
  poetryDialogVisible.value = true
}

const sectionContentMap: Record<string, { title: string; field: keyof Poet }> = {
  'section-intro': { title: '简　　介', field: 'biography' },
  'section-life': { title: '人物生平', field: 'lifeStory' },
  'section-influence': { title: '主要影响', field: 'influence' },
  'section-evaluation': { title: '历史评价', field: 'evaluation' },
  'section-anecdotes': { title: '轶事典故', field: 'anecdotes' },
}

const getSectionContent = (sectionId: string): string => {
  const config = sectionContentMap[sectionId]
  if (!config || !poet.value) return ''
  return (poet.value[config.field] as string) || ''
}

const hasSectionContent = (sectionId: string): boolean => {
  return !!getSectionContent(sectionId)
}

const handleScroll = () => {
  const scrollTop = window.scrollY + 120
  for (let i = navSections.length - 1; i >= 0; i--) {
    const el = document.getElementById(navSections[i].id)
    if (el && el.offsetTop <= scrollTop) {
      activeSection.value = navSections[i].id
      break
    }
  }
}

const scrollToSection = (sectionId: string) => {
  const el = document.getElementById(sectionId)
  if (el) {
    const offset = 80
    const top = el.getBoundingClientRect().top + window.scrollY - offset
    window.scrollTo({ top, behavior: 'smooth' })
    activeSection.value = sectionId
  }
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const fetchPoet = async () => {
  loading.value = true
  try {
    const res = await getPoetById(poetId.value)
    poet.value = res.data
  } catch {
    ElMessage.error('获取诗人详情失败')
    router.push('/poet')
  } finally {
    loading.value = false
  }
}

const fetchPoems = async () => {
  try {
    const res = await getPoemsByPoet(poetId.value, {
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
  nextTick(() => {
    const el = document.getElementById('section-poems')
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

const goToPoemDetail = (id: number) => {
  router.push(`/poem/${id}`)
}

const readText = (sectionId: string) => {
  if (isReading.value && currentReadingId.value === sectionId) {
    stopReading()
    return
  }
  if (isReading.value) stopReading()

  let text = ''
  if (sectionId === 'section-poems') {
    text = poems.value.map(p => `${p.title}。${p.content}`).join('\n')
  } else {
    const el = document.querySelector(`#${sectionId} .section-body`)
    if (el) text = (el as HTMLElement).innerText
  }
  if (!text) {
    ElMessage.info('暂无可朗读的内容')
    return
  }

  speechUtterance = new SpeechSynthesisUtterance(text)
  speechUtterance.lang = 'zh-CN'
  speechUtterance.rate = 0.9
  speechUtterance.onend = () => {
    isReading.value = false
    currentReadingId.value = null
  }
  speechUtterance.onerror = () => {
    isReading.value = false
    currentReadingId.value = null
  }
  window.speechSynthesis.speak(speechUtterance)
  isReading.value = true
  currentReadingId.value = sectionId
}

const stopReading = () => {
  window.speechSynthesis.cancel()
  isReading.value = false
  currentReadingId.value = null
}

const openEdit = (sectionId: string) => {
  const config = sectionContentMap[sectionId]
  if (!config) return
  editTitle.value = config.title
  editText.value = getSectionContent(sectionId)
  editingSection.value = sectionId
}

const closeEdit = () => {
  editingSection.value = null
  editText.value = ''
  editTitle.value = ''
}

const toggleMusic = () => {
  if (!audioElement) {
    audioElement = new Audio('/js/music.mp3')
    audioElement.loop = true
    audioElement.volume = 0.5
  }
  if (isMusicPlaying.value) {
    audioElement.pause()
    isMusicPlaying.value = false
  } else {
    audioElement.play().catch(() => {
      ElMessage.warning('音乐播放失败，请检查音频文件')
    })
    isMusicPlaying.value = true
  }
}

const handleScreenshot = () => {
  ElMessage.info('截图功能开发中')
}

watch(() => route.params.id, (newId) => {
  if (newId) {
    fetchPoet()
    fetchPoems()
    nextTick(() => handleScroll())
  }
})

onMounted(() => {
  fetchPoet()
  fetchPoems()
  window.addEventListener('scroll', handleScroll, { passive: true })
  nextTick(() => handleScroll())
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  stopReading()
  if (audioElement) {
    audioElement.pause()
    audioElement = null
  }
})
</script>

<template>
  <div class="poet-detail-page" v-loading="loading">
    <aside class="poet-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <el-icon class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
          <component :is="sidebarCollapsed ? 'Expand' : 'Fold'" />
        </el-icon>
        <span v-show="!sidebarCollapsed" class="sidebar-title">{{ poet?.name || '诗人' }}</span>
      </div>
      <nav v-show="!sidebarCollapsed" class="sidebar-nav">
        <a
          v-for="section in navSections"
          :key="section.id"
          :class="['nav-item', { active: activeSection === section.id }]"
          @click="scrollToSection(section.id)"
        >
          <el-icon><component :is="section.icon" /></el-icon>
          <span>{{ section.label }}</span>
        </a>
      </nav>
    </aside>

    <main class="poet-main" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <div class="poet-header">
        <el-button @click="router.push('/poet')" class="back-button" plain>
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>

      <div v-if="poet" class="poet-content">
        <div class="poet-hero">
          <div class="poet-hero-bg">
            <div class="hero-decoration deco-1"></div>
            <div class="hero-decoration deco-2"></div>
          </div>
          <div class="poet-hero-content">
            <el-avatar :src="poet.avatar" :size="120" class="poet-avatar">
              {{ poet.name?.charAt(0) }}
            </el-avatar>
            <div class="poet-basic">
              <h1 class="poet-name">
                {{ poet.name }}
                <el-tag v-if="poet.poetType === 'modern'" type="success" size="small" class="modern-tag">现代诗人</el-tag>
              </h1>
              <div class="poet-alias" v-if="poet.courtesyName || poet.pseudonym">
                <span v-if="poet.courtesyName" class="alias-tag">字：{{ poet.courtesyName }}</span>
                <span v-if="poet.pseudonym" class="alias-tag">号：{{ poet.pseudonym }}</span>
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
        </div>

        <div class="poet-brief" v-if="poet.biography">
          <p class="brief-text">{{ poet.biography }}</p>
        </div>

        <div
          v-for="section in navSections.slice(0, 5)"
          :key="section.id"
          :id="section.id"
          class="content-section"
        >
          <div class="section-header">
            <h2 class="section-title">
              <el-icon><component :is="section.icon" /></el-icon>
              {{ section.label }}
            </h2>
            <div class="section-actions">
              <el-button
                size="small"
                :type="isReading && currentReadingId === section.id ? 'danger' : 'primary'"
                text
                @click="readText(section.id)"
              >
                <el-icon><VideoPause v-if="isReading && currentReadingId === section.id" /><Microphone v-else /></el-icon>
                {{ isReading && currentReadingId === section.id ? '停止朗读' : '朗读' }}
              </el-button>
              <el-button size="small" type="info" text @click="openEdit(section.id)">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
            </div>
          </div>
          <div class="section-divider"></div>
          <div class="section-body">
            <p v-if="hasSectionContent(section.id)" class="section-text">{{ getSectionContent(section.id) }}</p>
            <el-empty v-else description="暂未收录" :image-size="60" />
          </div>
        </div>

        <div id="section-poems" class="content-section">
          <div class="section-header">
            <h2 class="section-title">
              <el-icon><Notebook /></el-icon>
              诗词作品
            </h2>
            <div class="section-actions">
              <el-button
                size="small"
                :type="isReading && currentReadingId === 'section-poems' ? 'danger' : 'primary'"
                text
                @click="readText('section-poems')"
              >
                <el-icon><VideoPause v-if="isReading && currentReadingId === 'section-poems'" /><Microphone v-else /></el-icon>
                {{ isReading && currentReadingId === 'section-poems' ? '停止朗读' : '朗读' }}
              </el-button>
            </div>
          </div>
          <div class="section-divider"></div>
          <div class="section-body">
            <el-empty v-if="poems.length === 0" description="暂无作品" :image-size="60" />
            <div v-else class="poems-list">
              <div
                v-for="poem in poems"
                :key="poem.id"
                class="poem-item"
                @click="openPoetryDetail(poem)"
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
    </main>

    <div class="right-toolbar">
      <div class="toolbar-item" @click="handleScreenshot" title="截图">
        <el-icon :size="20"><Camera /></el-icon>
        <span class="toolbar-label">截图</span>
      </div>
      <div class="toolbar-item" @click="toggleMusic" :title="isMusicPlaying ? '暂停音乐' : '播放音乐'">
        <el-icon :size="20"><Headset /></el-icon>
        <span class="toolbar-label">音乐</span>
      </div>
      <div class="toolbar-item" @click="scrollToTop" title="返回顶部">
        <el-icon :size="20"><Top /></el-icon>
        <span class="toolbar-label">顶部</span>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="fade">
        <div v-if="editingSection" class="edit-overlay" @click.self="closeEdit">
          <div class="edit-panel">
            <div class="edit-panel-header">
              <span>编辑 - {{ editTitle }}</span>
              <el-icon class="edit-close" @click="closeEdit"><Close /></el-icon>
            </div>
            <div class="edit-panel-body">
              <el-input
                v-model="editText"
                type="textarea"
                :rows="12"
                placeholder="输入内容..."
              />
            </div>
            <div class="edit-panel-footer">
              <span class="edit-tip">提示：此编辑仅在当前会话生效</span>
              <el-button @click="closeEdit">关闭</el-button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <PoetryDetailDialog
      v-model:visible="poetryDialogVisible"
      :title="selectedPoemTitle"
      :author="selectedPoemAuthor"
    />
  </div>
</template>

<style scoped lang="scss">
.poet-detail-page {
  display: flex;
  min-height: calc(100vh - 60px);
  position: relative;
}

.poet-sidebar {
  position: fixed;
  left: 0;
  top: 60px;
  bottom: 0;
  width: 180px;
  background: $background-color-light;
  border-right: 1px solid $border-color;
  z-index: 100;
  display: flex;
  flex-direction: column;
  transition: width $transition-base;
  overflow: hidden;

  &.collapsed {
    width: 48px;
  }

  @include responsive(md) {
    display: none;
  }
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  border-bottom: 1px solid $border-color;
  min-height: 48px;
}

.collapse-btn {
  cursor: pointer;
  font-size: 18px;
  color: $text-color-secondary;
  flex-shrink: 0;

  &:hover {
    color: $primary-color;
  }
}

.sidebar-title {
  font-size: $font-size-base;
  font-family: $font-family-title;
  color: $primary-color;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-nav {
  flex: 1;
  padding: $spacing-sm 0;
  overflow-y: auto;
  @include scrollbar;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  margin: $spacing-xs $spacing-sm;
  border-radius: $border-radius-sm;
  cursor: pointer;
  font-size: $font-size-base;
  color: $text-color-secondary;
  transition: all $transition-fast;
  text-decoration: none;
  white-space: nowrap;

  &:hover {
    color: $primary-color;
    background: rgba($primary-color, 0.06);
  }

  &.active {
    color: $primary-color;
    background: rgba($primary-color, 0.1);
    font-weight: 600;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 16px;
      background: $primary-color;
      border-radius: 0 2px 2px 0;
    }
  }
}

.poet-main {
  flex: 1;
  margin-left: 180px;
  padding: $spacing-lg $spacing-xl;
  transition: margin-left $transition-base;

  &.sidebar-collapsed {
    margin-left: 48px;
  }

  @include responsive(md) {
    margin-left: 0 !important;
    padding: $spacing-md;
  }
}

.poet-header {
  margin-bottom: $spacing-lg;
}

.back-button {
  font-family: $font-family-input;
}

.poet-content {
  max-width: 860px;
}

.poet-hero {
  position: relative;
  background: $background-color-light;
  border-radius: $border-radius-md;
  box-shadow: $box-shadow;
  overflow: hidden;
  margin-bottom: $spacing-lg;
}

.poet-hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 120px;
  background: linear-gradient(135deg, rgba($primary-color, 0.08), rgba($secondary-color, 0.05));
  overflow: hidden;
}

.hero-decoration {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;

  &.deco-1 {
    width: 200px;
    height: 200px;
    background: $primary-color;
    top: -80px;
    right: -40px;
  }

  &.deco-2 {
    width: 120px;
    height: 120px;
    background: $secondary-color;
    bottom: -40px;
    left: 60px;
  }
}

.poet-hero-content {
  position: relative;
  z-index: 1;
  display: flex;
  gap: $spacing-xl;
  align-items: flex-start;
  padding: $spacing-xl;

  @include responsive(sm) {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
}

.poet-avatar {
  flex-shrink: 0;
  border: 3px solid $background-color-light;
  box-shadow: $box-shadow-md;
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

.modern-tag {
  margin-left: $spacing-sm;
  vertical-align: middle;
}

.poet-alias {
  display: flex;
  gap: $spacing-md;
  margin-bottom: $spacing-md;

  @include responsive(sm) {
    justify-content: center;
  }
}

.alias-tag {
  display: inline-flex;
  align-items: center;
  padding: $spacing-xs $spacing-sm;
  background: rgba($primary-color, 0.06);
  border-radius: $border-radius-sm;
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.poet-meta {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-lg;

  @include responsive(sm) {
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

.poet-brief {
  background: $background-color-light;
  border-radius: $border-radius-md;
  box-shadow: $box-shadow;
  padding: $spacing-lg;
  margin-bottom: $spacing-lg;
  border-left: 3px solid $primary-color;
}

.brief-text {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: $line-height-loose;
  margin: 0;
  text-indent: 2em;
}

.content-section {
  background: $background-color-light;
  border-radius: $border-radius-md;
  box-shadow: $box-shadow;
  padding: $spacing-lg;
  margin-bottom: $spacing-lg;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: $font-size-xl;
  color: $text-color;
  font-family: $font-family-title;
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.section-actions {
  display: flex;
  gap: $spacing-xs;
}

.section-divider {
  height: 2px;
  background: linear-gradient(to right, $primary-color, transparent);
  margin: $spacing-sm 0 $spacing-lg;
}

.section-body {
  min-height: 80px;
}

.section-text {
  font-size: $font-size-base;
  color: $text-color;
  line-height: $line-height-loose;
  white-space: pre-line;
  text-indent: 2em;
}

.poems-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.poem-item {
  padding: $spacing-lg;
  border: 1px solid $border-color-light;
  border-radius: $border-radius-md;
  cursor: pointer;
  transition: all $transition-base;

  &:hover {
    border-color: $primary-color;
    box-shadow: $box-shadow-md;
  }
}

.poem-title {
  font-size: $font-size-xl;
  color: $text-color;
  font-family: $font-family-title;
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

.right-toolbar {
  position: fixed;
  right: 20px;
  bottom: 120px;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  z-index: 100;

  @include responsive(md) {
    right: 10px;
    bottom: 80px;
  }
}

.toolbar-item {
  width: 48px;
  height: 48px;
  background: $background-color-light;
  border-radius: 50%;
  box-shadow: $box-shadow-md;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all $transition-fast;
  color: $text-color-secondary;

  &:hover {
    color: $primary-color;
    box-shadow: $box-shadow-lg;
    transform: translateY(-2px);
  }

  .toolbar-label {
    font-size: 10px;
    margin-top: 1px;
    line-height: 1;
  }
}

.edit-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.edit-panel {
  width: 560px;
  max-width: 90vw;
  max-height: 80vh;
  background: $background-color-light;
  border-radius: $border-radius-lg;
  box-shadow: $box-shadow-xl;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.edit-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md $spacing-lg;
  border-bottom: 1px solid $border-color;
  font-size: $font-size-lg;
  font-family: $font-family-title;
  color: $text-color;
}

.edit-close {
  cursor: pointer;
  font-size: 18px;
  color: $text-color-light;

  &:hover {
    color: $danger-color;
  }
}

.edit-panel-body {
  flex: 1;
  padding: $spacing-lg;
  overflow-y: auto;
}

.edit-panel-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md $spacing-lg;
  border-top: 1px solid $border-color;
}

.edit-tip {
  font-size: $font-size-sm;
  color: $text-color-light;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity $transition-base;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
