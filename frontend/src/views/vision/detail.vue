<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, User, Clock, View, Star, HomeFilled,
  Share, DocumentCopy, ArrowUp, Reading,
  Collection, Promotion, MoreFilled, Expand, Fold,
  Microphone, VideoPause, Edit, Close,
  Headset, Top
} from '@element-plus/icons-vue'
import type { VisionArticle } from '@/types/model'
import { getVisionArticleById, likeVisionArticle, getVisionArticleList } from '@/api/modules/visionArticle'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const article = ref<VisionArticle | null>(null)
const articleId = computed(() => Number(route.params.id))
const fontSize = ref(16)
const readProgress = ref(0)
const showBackToTop = ref(false)
const relatedArticles = ref<VisionArticle[]>([])
const prevArticle = ref<VisionArticle | null>(null)
const nextArticle = ref<VisionArticle | null>(null)
const tocItems = ref<{ id: string; text: string; level: number }[]>([])
const sidebarCollapsed = ref(false)
const activeSection = ref('')
const isReading = ref(false)
let speechUtterance: SpeechSynthesisUtterance | null = null
const editingSection = ref(false)
const editText = ref('')

const isMusicPlaying = ref(false)
let audioElement: HTMLAudioElement | null = null

const readTime = computed(() => {
  if (!article.value?.content) return '3 分钟'
  const wordCount = article.value.content.replace(/<[^>]*>/g, '').length
  const minutes = Math.ceil(wordCount / 500)
  return `${minutes} 分钟`
})

const formattedContent = computed(() => {
  if (!article.value?.content) return ''
  let content = article.value.content
  // 先转义 HTML 实体，防止 v-html 将内容中的 < > & 等误解析为 HTML 标签
  content = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  // 处理 Windows (\r\n) 和 Unix (\n) 两种换行符
  content = content.replace(/\r?\n/g, '<br/>')
  // 如果内容包含 HTML 标题标签，提取目录结构（内容中已有 &lt;h 标签则不会匹配到）
  content = content.replace(/<h([1-6])[^>]*>(.*?)<\/h[1-6]>/gi, (match, level, text) => {
    const id = `heading-${text.replace(/\s+/g, '-').toLowerCase()}`
    return `<h${level} id="${id}" class="article-heading">${text}</h${level}>`
  })
  return content
})

const fetchArticle = async () => {
  loading.value = true
  try {
    const res = await getVisionArticleById(articleId.value)
    article.value = res.data
    await nextTick()
    extractToc()
    fetchRelatedArticles()
    fetchPrevNextArticles()
  } catch (error) {
    ElMessage.error('获取文章详情失败')
    router.push('/vision')
  } finally {
    loading.value = false
  }
}

const extractToc = () => {
  if (!article.value?.content) return
  const headingRegex = /<h([1-6])[^>]*>(.*?)<\/h[1-6]>/gi
  const items: { id: string; text: string; level: number }[] = []
  let match
  while ((match = headingRegex.exec(article.value.content)) !== null) {
    const level = parseInt(match[1])
    const text = match[2].replace(/<[^>]*>/g, '')
    const id = `heading-${text.replace(/\s+/g, '-').toLowerCase()}`
    items.push({ id, text, level })
  }
  tocItems.value = items
}

const fetchRelatedArticles = async () => {
  try {
    const res = await getVisionArticleList({
      pageNum: 1,
      pageSize: 4,
      category: article.value?.category
    })
    relatedArticles.value = (res.data.list || []).filter(a => a.id !== articleId.value).slice(0, 3)
  } catch (error) {
    console.error('获取相关文章失败:', error)
  }
}

const fetchPrevNextArticles = async () => {
  try {
    const res = await getVisionArticleList({ pageNum: 1, pageSize: 100 })
    const articles = res.data.list || []
    const currentIndex = articles.findIndex(a => a.id === articleId.value)
    if (currentIndex > 0) {
      prevArticle.value = articles[currentIndex - 1]
    }
    if (currentIndex < articles.length - 1) {
      nextArticle.value = articles[currentIndex + 1]
    }
  } catch (error) {
    console.error('获取前后文章失败:', error)
  }
}

const handleLike = async () => {
  try {
    await likeVisionArticle(articleId.value)
    ElMessage.success('点赞成功')
    if (article.value) {
      article.value.likeCount++
    }
  } catch (error) {
    ElMessage.error('点赞失败')
  }
}

const handleBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/vision')
  }
}

const handleShare = async () => {
  const shareData = {
    title: article.value?.title || '诗话视野',
    text: article.value?.summary || '来自墨渊的诗话视野文章',
    url: window.location.href
  }
  try {
    if (navigator.share) {
      await navigator.share(shareData)
    } else {
      await navigator.clipboard.writeText(window.location.href)
      ElMessage.success('链接已复制到剪贴板')
    }
  } catch (error) {
    console.error('分享失败:', error)
  }
}

const increaseFontSize = () => {
  if (fontSize.value < 24) {
    fontSize.value += 2
  }
}

const decreaseFontSize = () => {
  if (fontSize.value > 12) {
    fontSize.value -= 2
  }
}

const scrollToHeading = (id: string) => {
  const element = document.getElementById(id)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleScroll = () => {
  const scrollTop = window.scrollY
  const docHeight = document.documentElement.scrollHeight - window.innerHeight
  readProgress.value = docHeight > 0 ? Math.round((scrollTop / docHeight) * 100) : 0
  showBackToTop.value = scrollTop > 300

  const adjustedScrollTop = scrollTop + 120
  for (let i = tocItems.value.length - 1; i >= 0; i--) {
    const el = document.getElementById(tocItems.value[i].id)
    if (el && el.offsetTop <= adjustedScrollTop) {
      activeSection.value = tocItems.value[i].id
      break
    }
  }
}

const goToArticle = (id: number) => {
  router.push(`/vision/${id}`)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const readText = () => {
  if (isReading.value) {
    stopReading()
    return
  }

  const content = article.value?.content
  if (!content) {
    ElMessage.info('暂无可朗读的内容')
    return
  }

  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = content
  const text = tempDiv.innerText

  speechUtterance = new SpeechSynthesisUtterance(text)
  speechUtterance.lang = 'zh-CN'
  speechUtterance.rate = 0.9
  speechUtterance.onend = () => {
    isReading.value = false
  }
  speechUtterance.onerror = () => {
    isReading.value = false
  }
  window.speechSynthesis.speak(speechUtterance)
  isReading.value = true
}

const stopReading = () => {
  window.speechSynthesis.cancel()
  isReading.value = false
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

const openEdit = () => {
  editText.value = article.value?.content || ''
  editingSection.value = true
}

const closeEdit = () => {
  editingSection.value = false
  editText.value = ''
}

watch(() => route.params.id, (newId) => {
  if (newId) {
    fetchArticle()
  }
})

onMounted(() => {
  fetchArticle()
  window.addEventListener('scroll', handleScroll)
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
  <div class="vision-detail-page" v-loading="loading">
    <div class="reading-progress">
      <div class="progress-bar" :style="{ width: readProgress + '%' }"></div>
    </div>

    <aside class="vision-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <el-icon class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
          <component :is="sidebarCollapsed ? 'Expand' : 'Fold'" />
        </el-icon>
        <span v-show="!sidebarCollapsed" class="sidebar-title">文章目录</span>
      </div>
      <nav v-show="!sidebarCollapsed" class="sidebar-nav">
        <a
          v-for="item in tocItems"
          :key="item.id"
          :class="['nav-item', { active: activeSection === item.id }]"
          @click="scrollToHeading(item.id)"
        >
          <span>{{ item.text }}</span>
        </a>
      </nav>
    </aside>

    <main class="vision-main" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <div class="container" v-if="article">
      <div class="article-header">
        <div class="header-top">
          <el-button @click="handleBack" class="back-button" text>
            <el-icon><ArrowLeft /></el-icon>
            返回列表
          </el-button>
          <div class="header-actions">
            <el-button-group>
              <el-button @click="decreaseFontSize" text :disabled="fontSize <= 12">
                <span class="font-size-btn">A-</span>
              </el-button>
              <el-button @click="increaseFontSize" text :disabled="fontSize >= 24">
                <span class="font-size-btn">A+</span>
              </el-button>
            </el-button-group>
            <el-button @click="handleShare" text>
              <el-icon><Share /></el-icon>
              分享
            </el-button>
            <el-button @click="readText" text>
              <el-icon><VideoPause v-if="isReading" /><Microphone v-else /></el-icon>
              {{ isReading ? '停止朗读' : '朗读' }}
            </el-button>
            <el-button @click="openEdit" text>
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
          </div>
        </div>
      </div>

      <div class="article-layout">
        <div class="article-main">
          <el-card class="article-card" :body-style="{ padding: '40px' }">
            <div class="article-title-section">
              <el-tag v-if="article.category" class="article-category" type="warning" effect="plain">
                {{ article.category }}
              </el-tag>
              <h1 class="article-title">{{ article.title }}</h1>
              <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>
              <div class="article-meta">
                <span v-if="article.author" class="meta-author">
                  <el-icon><User /></el-icon>
                  {{ article.author }}
                </span>
                <span class="meta-time">
                  <el-icon><Clock /></el-icon>
                  {{ article.createTime }}
                </span>
                <span class="meta-view">
                  <el-icon><View /></el-icon>
                  {{ article.viewCount }} 次浏览
                </span>
                <span class="meta-read-time">
                  <el-icon><Reading /></el-icon>
                  约 {{ readTime }} 阅读
                </span>
              </div>
            </div>

            <div v-if="article.coverImage" class="article-cover">
              <img :src="article.coverImage" :alt="article.title" />
            </div>

            <div class="article-content-section">
              <div
                class="article-content"
                :style="{ fontSize: fontSize + 'px' }"
                v-html="formattedContent"
              ></div>
            </div>

            <div class="article-actions">
              <el-button type="primary" @click="handleLike" size="large">
                <el-icon><Star /></el-icon>
                点赞 ({{ article.likeCount }})
              </el-button>
              <el-button @click="handleShare" size="large">
                <el-icon><Share /></el-icon>
                分享文章
              </el-button>
            </div>
          </el-card>

          <div class="prev-next-section" v-if="prevArticle || nextArticle">
            <div class="prev-next-card" v-if="prevArticle" @click="goToArticle(prevArticle.id)">
              <div class="prev-next-label">
                <el-icon><ArrowLeft /></el-icon>
                上一篇
              </div>
              <div class="prev-next-title">{{ prevArticle.title }}</div>
            </div>
            <div class="prev-next-card next" v-if="nextArticle" @click="goToArticle(nextArticle.id)">
              <div class="prev-next-label">
                下一篇
                <el-icon><ArrowLeft style="transform: rotate(180deg)" /></el-icon>
              </div>
              <div class="prev-next-title">{{ nextArticle.title }}</div>
            </div>
          </div>

          <div class="related-section" v-if="relatedArticles.length > 0">
            <h3 class="related-title">
              <el-icon><Collection /></el-icon>
              相关推荐
            </h3>
            <div class="related-list">
              <el-card
                v-for="item in relatedArticles"
                :key="item.id"
                class="related-card"
                shadow="hover"
                @click="goToArticle(item.id)"
              >
                <div class="related-info">
                  <el-tag v-if="item.category" size="small" type="warning" effect="plain">
                    {{ item.category }}
                  </el-tag>
                  <h4 class="related-card-title">{{ item.title }}</h4>
                  <p v-if="item.summary" class="related-card-summary">{{ item.summary }}</p>
                  <div class="related-meta">
                    <span v-if="item.author">{{ item.author }}</span>
                    <span>{{ item.viewCount }} 阅读</span>
                  </div>
                </div>
              </el-card>
            </div>
          </div>

          <div class="back-to-home">
            <el-button @click="router.push('/vision')" type="primary" plain>
              <el-icon><MoreFilled /></el-icon>
              查看全部文章
            </el-button>
            <el-button @click="router.push('/')" plain>
              <el-icon><HomeFilled /></el-icon>
              返回首页
            </el-button>
          </div>
        </div>
      </div>

      <el-back-top :right="40" :bottom="40" />
    </div>
    </main>

    <div class="right-toolbar">
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
              <span>编辑文章内容</span>
              <el-icon class="edit-close" @click="closeEdit"><Close /></el-icon>
            </div>
            <div class="edit-panel-body">
              <el-input
                v-model="editText"
                type="textarea"
                :rows="15"
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
  </div>
</template>

<style scoped lang="scss">
.reading-progress {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #8b4513, #d2691e);
  transition: width 0.1s ease;
}

.vision-detail-page {
  padding: 20px 0 60px;
  min-height: 100vh;
  background: linear-gradient(180deg, #f5f0e8 0%, #e8e0d0 100%);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.article-header {
  margin-bottom: 20px;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.back-button {
  font-size: 14px;
  color: #8b4513;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.font-size-btn {
  font-weight: 600;
  font-size: 14px;
}

.article-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.article-main {
  flex: 1;
  min-width: 0;
}

.article-card {
  margin-bottom: 24px;
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.article-title-section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 2px solid #f0e6d6;
}

.article-category {
  margin-bottom: 16px;
  font-size: 13px;
}

.article-title {
  font-size: 32px;
  color: #2c1810;
  margin-bottom: 16px;
  line-height: 1.3;
  font-weight: 700;
}

.article-summary {
  font-size: 16px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 20px;
  padding: 16px;
  background: #f9f5f0;
  border-radius: 8px;
  border-left: 4px solid #d2691e;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.meta-author,
.meta-time,
.meta-view,
.meta-read-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #888;
}

.meta-author {
  font-weight: 600;
  color: #8b4513;
}

.article-cover {
  margin-bottom: 32px;

  img {
    width: 100%;
    max-height: 500px;
    object-fit: cover;
    border-radius: 12px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  }
}

.article-content-section {
  margin-bottom: 40px;
}

.article-content {
  line-height: 1.9;
  color: #333;
  font-family: "PingFang SC", "Microsoft YaHei", "STSong", serif;

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4),
  :deep(h5),
  :deep(h6) {
    color: #2c1810;
    margin: 32px 0 16px;
    font-weight: 600;
  }

  :deep(h2) {
    font-size: 1.5em;
    padding-bottom: 8px;
    border-bottom: 2px solid #f0e6d6;
  }

  :deep(p) {
    margin-bottom: 16px;
    text-indent: 2em;
  }

  :deep(blockquote) {
    margin: 20px 0;
    padding: 16px 20px;
    background: #f9f5f0;
    border-left: 4px solid #d2691e;
    border-radius: 0 8px 8px 0;
    font-style: italic;
    color: #555;
  }

  :deep(img) {
    max-width: 100%;
    height: auto;
    border-radius: 8px;
    margin: 16px 0;
  }
}

.article-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 32px;
  border-top: 2px solid #f0e6d6;
}

.prev-next-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 32px;
}

.prev-next-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  }

  &.next {
    text-align: right;
  }
}

.prev-next-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;

  .next & {
    justify-content: flex-end;
  }
}

.prev-next-title {
  font-size: 15px;
  color: #333;
  font-weight: 500;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.related-section {
  margin-bottom: 32px;
}

.related-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  color: #2c1810;
  margin-bottom: 20px;
  font-weight: 600;
}

.related-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.related-card {
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.3s ease;
  border: none;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  }
}

.related-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.related-card-title {
  font-size: 16px;
  color: #333;
  margin: 0;
  font-weight: 600;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.related-card-summary {
  font-size: 13px;
  color: #888;
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.related-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.back-to-home {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px 0;
}

.right-toolbar {
  position: fixed;
  right: 20px;
  bottom: 120px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 100;

  @media (max-width: 768px) {
    right: 10px;
    bottom: 80px;
  }
}

.toolbar-item {
  width: 48px;
  height: 48px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #666;

  &:hover {
    color: #8b4513;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
    transform: translateY(-2px);
  }

  .toolbar-label {
    font-size: 10px;
    margin-top: 1px;
    line-height: 1;
  }
}

@media (max-width: 768px) {
  .article-layout {
    flex-direction: column;
  }

  .article-title {
    font-size: 24px;
  }

  .prev-next-section {
    grid-template-columns: 1fr;
  }

  .related-list {
    grid-template-columns: 1fr;
  }

  .header-top {
    flex-direction: column;
    align-items: flex-start;
  }
}

.vision-sidebar {
  position: fixed;
  left: 0;
  top: 60px;
  bottom: 0;
  width: 200px;
  background: #f5f0e8;
  border-right: 1px solid #e0d6c6;
  z-index: 100;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  overflow: hidden;

  &.collapsed {
    width: 48px;
  }

  @media (max-width: 768px) {
    display: none;
  }
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid #e0d6c6;
  min-height: 48px;
}

.collapse-btn {
  cursor: pointer;
  font-size: 18px;
  color: #8b4513;
  flex-shrink: 0;

  &:hover {
    color: #d2691e;
  }
}

.sidebar-title {
  font-size: 48px;
  color: #8b4513;
  white-space: nowrap;
}

.sidebar-nav {
  flex: 1;
  padding: 8px 0;
  overflow-y: auto;
}

.nav-item {
  display: block;
  padding: 8px 12px;
  margin: 4px 8px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #666;
  transition: all 0.2s ease;
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  &:hover {
    color: #8b4513;
    background: rgba(139, 69, 19, 0.06);
  }

  &.active {
    color: #8b4513;
    background: rgba(139, 69, 19, 0.1);
    font-weight: 600;
  }
}

.vision-main {
  flex: 1;
  margin-left: 200px;
  transition: margin-left 0.3s ease;

  &.sidebar-collapsed {
    margin-left: 48px;
  }

  @media (max-width: 768px) {
    margin-left: 0 !important;
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
  width: 700px;
  max-width: 90vw;
  max-height: 80vh;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.edit-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #eee;
  font-size: 18px;
  color: #333;
}

.edit-close {
  cursor: pointer;
  font-size: 18px;
  color: #999;

  &:hover {
    color: #e74c3c;
  }
}

.edit-panel-body {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.edit-panel-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-top: 1px solid #eee;
}

.edit-tip {
  font-size: 13px;
  color: #999;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
