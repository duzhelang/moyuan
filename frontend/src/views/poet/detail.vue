<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { Poet, Poem } from '@/types/model'
import { getPoetById, getPoetList } from '@/api/modules/poet'
import { getPoemsByPoet } from '@/api/modules/poem'
import { getAiModuleConfig, type AiModuleConfig } from '@/api/modules/ai'
import PoetryDetailDialog from '@/components/business/PoetryDetailDialog.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isAdmin = computed(() => userStore.userInfo?.role === 'admin')

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

const suggestionDialogVisible = ref(false)
const suggestionSection = ref('')
const suggestionContent = ref('')
const suggestionSubmitting = ref(false)

const isFavorited = ref(false)
const fontSizeLevel = ref(1)
const fontSizeMap: Record<number, string> = { 0: '14px', 1: '16px', 2: '18px', 3: '20px' }
const fontSizeDialogVisible = ref(false)
const relatedPoets = ref<Poet[]>([])
const relatedPoetsLoading = ref(false)
const relatedCollapsed = ref(true)
const featuresCollapsed = ref(true)
const aiFeatures = ref('')
const aiFeaturesLoading = ref(false)

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}
const chatMessages = ref<ChatMessage[]>([])
const chatInput = ref('')
const chatLoading = ref(false)
const chatContainerRef = ref<HTMLElement | null>(null)
const aiConfig = ref<AiModuleConfig | null>(null)

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
  const content = (poet.value[config.field] as string) || ''
  return content
    .replace(/&emsp;&emsp;/g, '\n')
    .replace(/&emsp;/g, '\n')
    .split('\n')
    .map(line => line.trim() ? '\u3000\u3000' + line.trim() : '')
    .join('\n')
}

const formatContent = (content: string): string => {
  return content
    .replace(/&emsp;&emsp;/g, '\n')
    .replace(/&emsp;/g, '\n')
    .split('\n')
    .map(line => line.trim() ? '\u3000\u3000' + line.trim() : '')
    .join('\n')
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
    fetchAiConfig()
    if (res.data.dynastyId) {
      fetchRelatedPoets(res.data.dynastyId)
    }
    initAiChat()
    if (!res.data.influence) {
      generateAiFeatures()
    }
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

const openSuggestion = (sectionId: string) => {
  suggestionSection.value = sectionId
  suggestionContent.value = ''
  suggestionDialogVisible.value = true
}

const submitSuggestion = async () => {
  if (!suggestionContent.value.trim()) {
    ElMessage.warning('请输入修正意见')
    return
  }
  suggestionSubmitting.value = true
  try {
    const { submitPoetSuggestion } = await import('@/api/modules/poet')
    await submitPoetSuggestion({
      poetId: poetId.value,
      section: suggestionSection.value,
      content: suggestionContent.value.trim()
    })
    ElMessage.success('修正意见已提交，感谢您的反馈！')
    suggestionDialogVisible.value = false
    suggestionContent.value = ''
  } catch {
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    suggestionSubmitting.value = false
  }
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

const checkFavoriteStatus = () => {
  if (!poetId.value) return
  const favorites = JSON.parse(localStorage.getItem('poet_favorites') || '[]')
  isFavorited.value = favorites.includes(poetId.value)
}

const toggleFavorite = () => {
  const favorites = JSON.parse(localStorage.getItem('poet_favorites') || '[]')
  if (isFavorited.value) {
    const index = favorites.indexOf(poetId.value)
    if (index > -1) favorites.splice(index, 1)
    isFavorited.value = false
    ElMessage.success('已取消收藏')
  } else {
    favorites.push(poetId.value)
    isFavorited.value = true
    ElMessage.success('已收藏诗人')
  }
  localStorage.setItem('poet_favorites', JSON.stringify(favorites))
}

const handleShare = async () => {
  const shareData = {
    title: `${poet.value?.name} - 古今诗话`,
    text: `来看看${poet.value?.dynastyName}诗人${poet.value?.name}的诗词作品`,
    url: window.location.href
  }
  try {
    if (navigator.share) {
      await navigator.share(shareData)
    } else {
      await navigator.clipboard.writeText(window.location.href)
      ElMessage.success('链接已复制到剪贴板')
    }
  } catch {
    await navigator.clipboard.writeText(window.location.href)
    ElMessage.success('链接已复制到剪贴板')
  }
}

const readAllText = () => {
  if (isReading.value) {
    stopReading()
    return
  }
  const sections = navSections.slice(0, 5)
  let allText = ''
  sections.forEach(section => {
    const content = getSectionContent(section.id)
    if (content) {
      allText += section.label.replace(/\s/g, '') + '。' + content + '\n'
    }
  })
  if (poems.value.length > 0) {
    allText += '诗词作品。' + poems.value.map(p => `${p.title}。${p.content}`).join('\n')
  }
  if (!allText) {
    ElMessage.info('暂无可朗读的内容')
    return
  }
  speechUtterance = new SpeechSynthesisUtterance(allText)
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
  currentReadingId.value = 'all'
}

const changeFontSize = (level: number) => {
  fontSizeLevel.value = level
  document.documentElement.style.setProperty('--poet-content-font-size', fontSizeMap[level])
}

const fetchRelatedPoets = async (dynastyId?: number) => {
  if (!dynastyId) return
  relatedPoetsLoading.value = true
  try {
    const res = await getPoetList({ dynastyId, pageSize: 10 })
    relatedPoets.value = (res.data.list || []).filter(p => p.id !== poetId.value).slice(0, 6)
  } catch {
    ElMessage.error('获取相关诗人失败')
  } finally {
    relatedPoetsLoading.value = false
  }
}

const goToRelatedPoet = (id: number) => {
  relatedPoetsDialogVisible.value = false
  router.push(`/poet/${id}`)
}

const scrollToChatBottom = () => {
  nextTick(() => {
    if (chatContainerRef.value) {
      chatContainerRef.value.scrollTop = chatContainerRef.value.scrollHeight
    }
  })
}

const fetchAiConfig = async () => {
  try {
    const res = await getAiModuleConfig('poet_chat')
    aiConfig.value = res.data
  } catch {
    console.error('获取AI配置失败')
  }
}

const buildPrompt = (userMessage: string, isFirst: boolean = false) => {
  const poetName = poet.value?.name || '该诗人'
  const config = aiConfig.value

  let systemConstraint = ''
  if (config?.promptTemplate) {
    const maxLength = isFirst ? (config.firstResponseLength || 80) : (config.maxLength || 150)
    const styleHint = isFirst
      ? `这是首次提问，请用2-3句话简要概括即可，不超过${config.firstResponseLength || 80}字`
      : `根据问题复杂度适当展开，但不超过${maxLength}字`

    systemConstraint = config.promptTemplate
      .replace(/\{poetName\}/g, poetName)
      .replace(/\{maxLength\}/g, String(maxLength))
      .replace(/\{styleHint\}/g, styleHint)
  } else {
    systemConstraint = `你是一个古典诗词文化助手，正在为用户介绍诗人${poetName}。
回答要求：
1. 语言简洁精炼，避免冗余
2. 使用通俗易懂的中文
3. 重点突出，条理清晰
4. 不要使用markdown格式，直接输出纯文本
${isFirst ? '5. 这是首次提问，请用2-3句话简要概括即可' : '5. 根据问题复杂度适当展开，但不超过150字'}`
  }

  return `【系统设定】${systemConstraint}\n\n【用户问题】${userMessage}`
}

const sendChatMessage = async (message?: string, isFirst: boolean = false) => {
  const msg = message || chatInput.value.trim()
  if (!msg || chatLoading.value) return

  chatMessages.value.push({
    role: 'user',
    content: msg,
    timestamp: Date.now()
  })
  chatInput.value = ''
  chatLoading.value = true
  scrollToChatBottom()

  try {
    const { chat } = await import('@/api/modules/ai')
    const prompt = buildPrompt(msg, isFirst)
    const res = await chat({ message: prompt })
    chatMessages.value.push({
      role: 'assistant',
      content: res.data.reply || '抱歉，暂时无法回答',
      timestamp: Date.now()
    })
  } catch {
    chatMessages.value.push({
      role: 'assistant',
      content: 'AI服务暂时不可用，请稍后重试',
      timestamp: Date.now()
    })
  } finally {
    chatLoading.value = false
    scrollToChatBottom()
  }
}

const initAiChat = () => {
  if (poet.value && chatMessages.value.length === 0) {
    sendChatMessage(`${poet.value.name}是谁`, true)
  }
}

const generateAiFeatures = async () => {
  if (!poet.value || aiFeatures.value || aiFeaturesLoading.value) return
  
  aiFeaturesLoading.value = true
  try {
    const { chat } = await import('@/api/modules/ai')
    const prompt = `【系统设定】你是一个古典诗词文化助手。请用简洁的语言概括诗人${poet.value.name}的诗词特点，不超过60字。要求：1.语言精炼 2.突出主要特点 3.不要使用markdown格式

【用户问题】请概括${poet.value.name}的诗词特点`
    
    const res = await chat({ message: prompt })
    const reply = res.data.reply || ''
    aiFeatures.value = reply.length > 100 ? reply.substring(0, 100) + '...' : reply
  } catch (error) {
    console.error('生成诗词特点失败:', error)
  } finally {
    aiFeaturesLoading.value = false
  }
}

const readChatMessage = (text: string, id: string) => {
  if (isReading.value && currentReadingId.value === id) {
    stopReading()
    return
  }
  if (isReading.value) stopReading()

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
  currentReadingId.value = id
}

const handleChatKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendChatMessage()
  }
}

watch(() => route.params.id, (newId) => {
  if (newId) {
    aiFeatures.value = ''
    fetchPoet()
    fetchPoems()
    nextTick(() => handleScroll())
  }
})

onMounted(() => {
  fetchPoet()
  fetchPoems()
  checkFavoriteStatus()
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
          <p class="brief-text">{{ formatContent(poet.biography) }}</p>
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
              <el-button
                v-if="isAdmin"
                size="small"
                type="info"
                text
                @click="openEdit(section.id)"
              >
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-button
                v-else
                size="small"
                type="warning"
                text
                @click="openSuggestion(section.id)"
              >
                <el-icon><ChatLineSquare /></el-icon>
                修正意见
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
                <p class="poem-content">{{ formatContent(poem.content) }}</p>
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

    <div class="right-floating-panels">
      <div class="floating-panel panel-actions">
        <div class="header-actions">
          <el-icon :class="{ 'action-active': isFavorited }" @click="toggleFavorite" :title="isFavorited ? '取消收藏' : '收藏诗人'">
            <StarFilled v-if="isFavorited" /><Star v-else />
          </el-icon>
          <el-icon @click="handleShare" title="分享诗人"><Share /></el-icon>
          <el-icon :class="{ 'action-active': isReading && currentReadingId === 'all' }" @click="readAllText" :title="isReading && currentReadingId === 'all' ? '停止朗读' : '朗读全文'">
            <VideoPause v-if="isReading && currentReadingId === 'all'" /><Microphone v-else />
          </el-icon>
          <el-icon @click="fontSizeDialogVisible = true" title="字号调节"><FontSize /></el-icon>
          <el-icon @click="handleScreenshot" title="截图"><Camera /></el-icon>
        </div>
      </div>

      <div class="floating-panel panel-related" :class="{ collapsed: relatedCollapsed }">
        <div class="panel-header" @click="relatedCollapsed = !relatedCollapsed">
          <div class="header-left">
            <el-icon><User /></el-icon>
            <span>相关诗人</span>
          </div>
          <el-icon class="collapse-icon">
            <ArrowUp v-if="!relatedCollapsed" /><ArrowDown v-else />
          </el-icon>
        </div>
        <div class="panel-body" v-show="!relatedCollapsed" v-loading="relatedPoetsLoading">
          <el-empty v-if="!relatedPoetsLoading && relatedPoets.length === 0" description="暂无相关诗人" :image-size="40" />
          <div v-else class="related-poets-list">
            <div
              v-for="rp in relatedPoets"
              :key="rp.id"
              class="related-poet-item"
              @click="goToRelatedPoet(rp.id)"
            >
              <el-avatar :src="rp.avatar" :size="32">{{ rp.name?.charAt(0) }}</el-avatar>
              <div class="poet-info">
                <div class="poet-name">{{ rp.name }}</div>
                <div class="poet-meta">{{ rp.dynastyName }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="floating-panel panel-features" :class="{ collapsed: featuresCollapsed }">
        <div class="panel-header" @click="featuresCollapsed = !featuresCollapsed">
          <div class="header-left">
            <el-icon><Document /></el-icon>
            <span>诗词特点</span>
          </div>
          <el-icon class="collapse-icon">
            <ArrowUp v-if="!featuresCollapsed" /><ArrowDown v-else />
          </el-icon>
        </div>
        <div class="panel-body" v-show="!featuresCollapsed" v-loading="aiFeaturesLoading">
          <div v-if="poet?.influence" class="poem-features">
            <p>{{ formatContent(poet.influence) }}</p>
          </div>
          <div v-else-if="aiFeatures" class="poem-features">
            <p>{{ aiFeatures }}</p>
          </div>
          <div v-else-if="!aiFeaturesLoading" class="poem-features-generate">
            <el-button type="primary" size="small" @click="generateAiFeatures">
              <el-icon><MagicStick /></el-icon>
              AI生成特点
            </el-button>
          </div>
        </div>
      </div>

      <div class="floating-panel panel-ai-chat">
        <div class="panel-header">
          <el-icon><MagicStick /></el-icon>
          <span>AI对话</span>
        </div>
        <div class="ai-chat-messages" ref="chatContainerRef">
          <div
            v-for="(msg, index) in chatMessages"
            :key="index"
            :class="['chat-message', msg.role]"
          >
            <div class="message-avatar">
              <el-avatar :size="28" v-if="msg.role === 'user'">我</el-avatar>
              <el-avatar :size="28" v-else class="ai-avatar">AI</el-avatar>
            </div>
            <div class="message-body">
              <div class="message-content">{{ msg.content }}</div>
              <div v-if="msg.role === 'assistant'" class="message-actions">
                <el-icon
                  :class="{ 'speaking': currentReadingId === `chat-${index}` }"
                  @click="readChatMessage(msg.content, `chat-${index}`)"
                  title="朗读"
                >
                  <VideoPause v-if="currentReadingId === `chat-${index}`" /><Headset v-else />
                </el-icon>
              </div>
            </div>
          </div>
          <div v-if="chatLoading" class="chat-message assistant">
            <div class="message-avatar">
              <el-avatar :size="28" class="ai-avatar">AI</el-avatar>
            </div>
            <div class="message-body">
              <div class="message-content loading">思考中...</div>
            </div>
          </div>
        </div>
        <div class="ai-chat-input">
          <el-input
            v-model="chatInput"
            type="textarea"
            :rows="2"
            placeholder="输入问题，按Enter发送..."
            @keydown="handleChatKeydown"
            :disabled="chatLoading"
          />
          <el-button type="primary" @click="sendChatMessage()" :loading="chatLoading" :disabled="!chatInput.trim()">
            <el-icon><Promotion /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <div class="side-float-btns">
      <div class="side-float-btn" @click="toggleMusic" :title="isMusicPlaying ? '暂停音乐' : '播放音乐'">
        <el-icon><Headset /></el-icon>
      </div>
      <div class="side-float-btn" @click="scrollToTop" title="返回顶部">
        <el-icon><Top /></el-icon>
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

    <el-dialog
      v-model="suggestionDialogVisible"
      title="提交修正意见"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="suggestion-form">
        <p class="suggestion-tip">如果您发现内容有误或有补充建议，请在此提交修正意见，管理员审核后会进行修改。</p>
        <el-input
          v-model="suggestionContent"
          type="textarea"
          :rows="6"
          placeholder="请输入您的修正意见..."
          maxlength="1000"
          show-word-limit
        />
      </div>
      <template #footer>
        <el-button @click="suggestionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="suggestionSubmitting" @click="submitSuggestion">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="fontSizeDialogVisible"
      title="字号调节"
      width="360px"
    >
      <div class="font-size-panel">
        <div class="font-size-options">
          <div
            v-for="(size, level) in fontSizeMap"
            :key="level"
            :class="['size-option', { active: fontSizeLevel === Number(level) }]"
            @click="changeFontSize(Number(level))"
          >
            <span :style="{ fontSize: size }">文</span>
            <span class="size-label">{{ size }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
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
  top: 55px;
  bottom: 0;
  width: 180px;
  background: url('/img/dts_1.jpg') no-repeat -60px 0 / cover;
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
  font-size: 48px;
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
  font-size: 20px;
  color: $text-color-secondary;
  transition: all $transition-fast;
  text-decoration: none;
  white-space: nowrap;

  &:hover {
    color: #409eff;
    background: rgba(64, 158, 255, 0.06);
  }

  &.active {
    color: #409eff;
    background: rgba(64, 158, 255, 0.15);
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
      background: #409eff;
      border-radius: 0 2px 2px 0;
    }
  }
}

.poet-main {
  flex: 1;
  margin-left: 180px;
  margin-right: 300px;
  padding: $spacing-lg $spacing-xl;
  transition: margin-left $transition-base;

  &.sidebar-collapsed {
    margin-left: 48px;
  }

  @include responsive(lg) {
    margin-right: 0;
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
  background: url('/img/dt_4.jpg') no-repeat 0 -160px / cover;
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
  padding-top: calc($spacing-xl + 30px);

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
  white-space: pre-line;
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
  font-size: var(--poet-content-font-size, $font-size-base);
  color: $text-color;
  line-height: $line-height-loose;
  white-space: pre-line;
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
  white-space: pre-line;
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

.right-floating-panels {
  position: fixed;
  right: 40px;
  top: 55px;
  bottom: 100px;
  width: 340px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  overflow-y: auto;
  padding: $spacing-xs 0;
  @include scrollbar;

  @include responsive(lg) {
    display: none;
  }
}

.floating-panel {
  border-radius: $border-radius-md;
  box-shadow: $box-shadow;
  transition: box-shadow $transition-fast;

  &:hover {
    box-shadow: $box-shadow-md;
  }

  &.panel-actions {
    background: $background-color-light;
    border: 1px solid $border-color;
    padding: $spacing-md;

    .header-actions {
      display: flex;
      gap: $spacing-md;
      justify-content: center;
      flex-wrap: wrap;
      padding: $spacing-xs 0;

      .el-icon {
        font-size: 28px;
        width: 48px;
        height: 48px;
        cursor: pointer;
        color: $text-color-secondary;
        transition: all $transition-fast;
        padding: 10px;
        border-radius: $border-radius-md;
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(0, 0, 0, 0.02);
        border: 1px solid transparent;

        svg {
          width: 28px;
          height: 28px;
        }

        &:hover {
          color: $primary-color;
          background: rgba($primary-color, 0.1);
          border-color: rgba($primary-color, 0.2);
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba($primary-color, 0.15);
        }

        &:active {
          transform: translateY(0);
        }

        &.action-active {
          color: #f5a623;
          background: rgba(#f5a623, 0.1);
          border-color: rgba(#f5a623, 0.2);
        }
      }
    }
  }

  &.panel-related {
    background: $background-color-light;
    border: 1px solid $border-color;
  }

  &.panel-features {
    background: $background-color-light;
    border: 1px solid $border-color;
  }

  &.panel-ai-chat {
    background: $background-color-light;
    border: 1px solid $border-color;
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 400px;

    .panel-header {
      background: linear-gradient(135deg, rgba(#667eea, 0.08) 0%, rgba(#764ba2, 0.08) 100%);
      cursor: default;
      padding: $spacing-md;

      .header-left {
        .el-icon {
          font-size: 18px;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
        }

        span {
          font-size: $font-size-base;
          font-weight: 600;
        }
      }
    }
  }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  font-size: $font-size-sm;
  font-weight: 600;
  color: $text-color;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  cursor: pointer;
  user-select: none;
  transition: all $transition-fast;

  &:hover {
    background: rgba(0, 0, 0, 0.03);
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: $spacing-sm;

    .el-icon {
      font-size: 16px;
      color: $primary-color;
    }
  }

  .collapse-icon {
    font-size: 14px;
    color: $text-color-secondary;
    transition: transform $transition-fast;
  }
}

.panel-body {
  padding: $spacing-md;
}

.floating-panel.collapsed {
  .panel-header {
    border-bottom: none;
  }
}

.related-poets-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.related-poet-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  border-radius: $border-radius-sm;
  cursor: pointer;
  transition: all $transition-fast;
  border: 1px solid transparent;

  &:hover {
    background: rgba($primary-color, 0.05);
    border-color: rgba($primary-color, 0.1);
    transform: translateX(4px);
  }

  .poet-info {
    flex: 1;
    min-width: 0;
  }

  .poet-name {
    font-size: $font-size-sm;
    font-weight: 600;
    color: $text-color;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .poet-meta {
    font-size: 12px;
    color: $text-color-secondary;
    margin-top: 2px;
  }
}

.side-float-btns {
  position: fixed;
  right: 400px;
  bottom: 40px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 99;

  @include responsive(lg) {
    display: none;
  }

  .side-float-btn {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: $background-color-light;
    border: 1px solid $border-color;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    box-shadow: $box-shadow;
    transition: all $transition-fast;

    .el-icon {
      font-size: 18px;
      color: $text-color-secondary;
    }

    &:hover {
      border-color: $primary-color;
      box-shadow: $box-shadow-md;

      .el-icon {
        color: $primary-color;
      }
    }
  }
}

.poem-features {
  p {
    font-size: $font-size-sm;
    color: $text-color;
    line-height: 1.7;
    margin: 0;
    text-align: justify;
  }
}

.poem-features-generate {
  display: flex;
  justify-content: center;
  padding: $spacing-sm 0;
}

.panel-ai-chat .ai-chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-md;
  min-height: 200px;
  @include scrollbar;
}

.chat-message {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
  align-items: flex-start;
  animation: fadeIn 0.3s ease;

  &.user {
    flex-direction: row-reverse;

    .message-content {
      background: linear-gradient(135deg, $primary-color 0%, darken($primary-color, 10%) 100%);
      color: white;
      border-radius: 18px 18px 4px 18px;
    }
  }

  &.assistant {
    .message-content {
      background: #f4f4f5;
      color: $text-color;
      border-radius: 18px 18px 18px 4px;
    }
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-avatar {
  flex-shrink: 0;

  .ai-avatar {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    font-size: 11px;
    font-weight: 600;
    box-shadow: 0 2px 8px rgba(#667eea, 0.3);
  }
}

.message-body {
  flex: 1;
  min-width: 0;
}

.message-content {
  font-size: 13px;
  line-height: 1.6;
  padding: 10px 14px;
  word-break: break-word;

  &.loading {
    color: $text-color-secondary;
    font-style: italic;
  }
}

.message-actions {
  display: flex;
  gap: $spacing-sm;
  margin-top: $spacing-xs;
  padding-left: $spacing-sm;

  .el-icon {
    font-size: 14px;
    cursor: pointer;
    color: $text-color-light;
    transition: color $transition-fast;

    &:hover {
      color: $primary-color;
    }

    &.speaking {
      color: $primary-color;
    }
  }
}

.panel-ai-chat .ai-chat-input {
  padding: $spacing-md;
  border-top: 1px solid $border-color;
  display: flex;
  gap: $spacing-sm;
  align-items: flex-end;
  background: linear-gradient(to bottom, #fafafa, #f5f5f5);

  .el-textarea {
    flex: 1;

    :deep(.el-textarea__inner) {
      border-radius: 20px;
      padding: 10px 16px;
      resize: none;
      border-color: $border-color;
      font-size: $font-size-sm;
      line-height: 1.5;
      transition: all $transition-fast;
      background: white;

      &::placeholder {
        font-size: $font-size-sm;
        color: $text-color-light;
      }

      &:focus {
        border-color: $primary-color;
        box-shadow: 0 0 0 3px rgba($primary-color, 0.15);
      }
    }
  }

  .el-button {
    border-radius: 20px;
    padding: 10px 24px;
    font-weight: 500;
    background: linear-gradient(135deg, $primary-color 0%, darken($primary-color, 10%) 100%);
    border: none;
    box-shadow: 0 2px 8px rgba($primary-color, 0.3);

    &:hover {
      box-shadow: 0 4px 12px rgba($primary-color, 0.4);
    }
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

.suggestion-form {
  .suggestion-tip {
    font-size: $font-size-sm;
    color: $text-color-secondary;
    margin-bottom: $spacing-md;
    line-height: 1.6;
  }
}

.font-size-panel {
  padding: $spacing-md 0;
}

.font-size-options {
  display: flex;
  justify-content: space-around;
  gap: $spacing-md;
}

.size-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  border: 2px solid $border-color;
  border-radius: $border-radius-md;
  cursor: pointer;
  transition: all $transition-fast;
  flex: 1;

  &:hover {
    border-color: $primary-color;
    background: rgba($primary-color, 0.05);
  }

  &.active {
    border-color: $primary-color;
    background: rgba($primary-color, 0.1);
    color: $primary-color;
  }

  .size-label {
    font-size: $font-size-sm;
    color: $text-color-secondary;
  }
}

</style>
