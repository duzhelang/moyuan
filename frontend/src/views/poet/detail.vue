<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { Poet, Poem } from '@/types/model'
import { getPoetById, getPoetList } from '@/api/modules/poet'
import { getPoemsByPoet } from '@/api/modules/poem'
import { getAiModuleConfig, type AiModuleConfig, fillAiContent, getFillStatus, previewAiContent, submitForReview } from '@/api/modules/ai'
import { savePoetDraft } from '@/api/modules/poet-draft'
import { getExternalPoems, type PoemSearchResult } from '@/api/modules/external-poetry'
import { importExternalPoem, fixExternalPoems } from '@/api/modules/poem'
import PoetryDetailDialog from '@/components/business/PoetryDetailDialog.vue'
import LoginPrompt from '@/components/common/LoginPrompt.vue'
import { useUserStore } from '@/stores/user'
import { useParticles } from '@/composables/useParticles'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const particleCanvasRef = ref<HTMLCanvasElement | null>(null)
useParticles(particleCanvasRef, {
  count: 80,
  colors: ['#d4af87', '#f0e4d7', '#c9a06c', '#8b7355'],
  opacityRange: [0.3, 0.6],
  sizeRange: [2, 4]
})

const isAdmin = computed(() => userStore.userInfo?.role === 'admin')
const loginPromptVisible = ref(false)

const fillStatusMap = ref<Record<string, any>>({})
const fillingField = ref('')
const previewField = ref('')
const previewContent = ref('')
const submittingReview = ref(false)

const loading = ref(false)
const poet = ref<Poet | null>(null)
const poems = ref<Poem[]>([])
const poemTotal = ref(0)
const poemPage = ref(1)
const poemSize = ref(10)
const externalPoems = ref<PoemSearchResult[]>([])
const externalPoemsLoading = ref(false)
const fixingPoems = ref(false)

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
const isNearBottom = ref(false)

const isReading = ref(false)
const currentReadingId = ref<string | null>(null)
let speechUtterance: SpeechSynthesisUtterance | null = null

const editingSection = ref<string | null>(null)
const editText = ref('')
const editTitle = ref('')
const draftSaving = ref(false)

const isMusicPlaying = ref(false)
let audioElement: HTMLAudioElement | null = null

const poetryDialogVisible = ref(false)
const selectedPoemTitle = ref('')
const selectedPoemAuthor = ref('')

const suggestionDialogVisible = ref(false)
const suggestionSection = ref('')
const suggestionContent = ref('')
const suggestionSubmitting = ref(false)
const suggestionCategory = ref('other')

const isFavorited = ref(false)
const fontSizeLevel = ref(1)
const fontSizeMap: Record<number, string> = { 0: '14px', 1: '16px', 2: '18px', 3: '20px' }
const relatedPoets = ref<Poet[]>([])
const relatedPoetsLoading = ref(false)
const relatedPoetsDialogVisible = ref(false)
const relatedCollapsed = ref(true)
const featuresCollapsed = ref(true)
const aiFeatures = ref('')
const aiFeaturesLoading = ref(false)
const featuresDetailVisible = ref(false)
const poetNameFontSize = ref(38)
const relatedExpanded = ref(false)
const floatBtnsBottom = ref(80)

const chatDraggable = ref(false)
const chatPos = ref({ x: 0, y: 0 })
const chatDragging = ref(false)
const chatDragOffset = ref({ x: 0, y: 0 })

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}
const chatMessages = ref<ChatMessage[]>([])
const chatInput = ref('')
const chatLoading = ref(false)
const chatContainerRef = ref<HTMLElement | null>(null)
const enlargedMessageIndex = ref<number | null>(null)

const toggleMessageEnlarge = (index: number) => {
  enlargedMessageIndex.value = enlargedMessageIndex.value === index ? null : index
}
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

const sectionFieldNameMap: Record<string, string> = {
  'section-intro': 'biography',
  'section-life': 'life_story',
  'section-influence': 'influence',
  'section-evaluation': 'evaluation',
  'section-anecdotes': 'anecdotes',
}

const loadFillStatus = async () => {
  if (!poet.value?.id) return
  try {
    const res = await getFillStatus('poet', poet.value.id)
    if (res && Array.isArray(res)) {
      const map: Record<string, any> = {}
      const allFields = new Set<string>()
      let latestPending: any = null
      res.forEach((item: any) => {
        allFields.add(item.fieldName)
        if (item.status === 0) {
          map[item.fieldName] = item
          if (!latestPending || new Date(item.createTime) > new Date(latestPending.createTime)) {
            latestPending = item
          }
        } else if (item.status === 1) {
          map[item.fieldName] = { ...item, approved: true }
        } else if (item.status === 2) {
          map[item.fieldName] = { ...item, rejected: true }
        }
      })
      fillStatusMap.value = map
      allFields.forEach(f => submittedFields.value.add(f))
      
      if (latestPending && !previewField.value) {
        previewField.value = latestPending.fieldName
        previewContent.value = sanitizeAiContent(latestPending.content)
      }
    }
  } catch (e) {
    // 静默失败
  }
}

const sanitizeAiContent = (content: string): string => {
  if (!content) return ''
  return content
    .replace(/\\/g, '')
    .replace(/\*\*/g, '')
    .replace(/\*/g, '')
    .replace(/#{1,6}\s/g, '')
    .replace(/```[\s\S]*?```/g, '')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/^\s*[-*+]\s/gm, '')
    .replace(/^\s*\d+\.\s/gm, '')
    .replace(/[\x00-\x08\x0B\x0C\x0E-\x1F]/g, '')
    .trim()
}

const handleAiFill = async (fieldName: string) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (!poet.value?.id) return
  fillingField.value = fieldName
  try {
    if (!aiConfig.value) {
      await fetchAiConfig(false)
    }
    const res = await previewAiContent({
      targetType: 'poet',
      targetId: poet.value.id,
      fieldName,
      moduleCode: 'poet_chat'
    })
    const cleanedContent = sanitizeAiContent(res.data.content)
    previewField.value = fieldName
    previewContent.value = cleanedContent

    await submitForReview({
      targetType: 'poet',
      targetId: poet.value.id,
      fieldName,
      content: cleanedContent
    })
    submittedFields.value.add(fieldName)
    await loadFillStatus()
    ElMessage.success('AI内容已生成并保存，等待管理员审核')
  } catch (e: any) {
    ElMessage.error(e.message || 'AI生成失败')
  } finally {
    fillingField.value = ''
  }
}

const submittedFields = ref<Set<string>>(new Set())

const handleSubmitReview = async () => {
  if (!poet.value?.id || !previewField.value || !previewContent.value) return
  submittingReview.value = true
  try {
    await submitForReview({
      targetType: 'poet',
      targetId: poet.value.id,
      fieldName: previewField.value,
      content: previewContent.value
    })
    ElMessage.success('已提交审核，内容将显示为AI参考，待管理员审核后正式生效')
    submittedFields.value.add(previewField.value)
    await loadFillStatus()
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    submittingReview.value = false
  }
}

const handleCancelPreview = () => {
  previewField.value = ''
  previewContent.value = ''
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

const formatSectionContent = (content: string): string => {
  if (!content) return ''
  let text = content
    .replace(/&emsp;&emsp;/g, '\n')
    .replace(/&emsp;/g, '\n')
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
  
  const lines = text.split('\n')
  const result: string[] = []
  
  for (let i = 0; i < lines.length; i++) {
    const trimmed = lines[i].trim()
    if (!trimmed) {
      result.push('')
      continue
    }
    
    if (/^[\u4e00-\u9fa5]{4}$/.test(trimmed)) {
      result.push(`<div class="section-subtitle">${trimmed}</div>`)
      continue
    }
    
    const match = trimmed.match(/^(.*?[。！？])([\u4e00-\u9fa5]{4})\s*$/)
    if (match && match[1] && match[2]) {
      const prevText = match[1].trim()
      const subtitle = match[2]
      if (prevText) {
        result.push(`<p>\u3000\u3000${prevText}</p>`)
      }
      result.push(`<div class="section-subtitle">${subtitle}</div>`)
      continue
    }
    
    const startMatch = trimmed.match(/^([\u4e00-\u9fa5]{4})[：:，,、\s\u3000]/)
    if (startMatch) {
      result.push(`<div class="section-subtitle">${startMatch[1]}</div>`)
      const rest = trimmed.substring(4).replace(/^[\s\u3000]+/, '').trim()
      if (rest) {
        result.push(`<p>\u3000\u3000${rest}</p>`)
      }
      continue
    }
    
    result.push(`<p>\u3000\u3000${trimmed}</p>`)
  }
  
  return result.join('\n')
}

const formatContent = (content: string): string => {
  return content
    .replace(/&emsp;&emsp;/g, '\n')
    .replace(/&emsp;/g, '\n')
    .split('\n')
    .map(line => line.trim() ? '\u3000\u3000' + line.trim() : '')
    .join('\n')
}

const formatFeaturesContent = (content: string): string => {
  if (!content) return ''
  let text = content
    .replace(/&emsp;&emsp;/g, '\n')
    .replace(/&emsp;/g, '\n')
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
  
  const lines = text.split('\n')
  const formattedLines: string[] = []
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim()
    if (!line) {
      if (formattedLines.length > 0 && formattedLines[formattedLines.length - 1] !== '') {
        formattedLines.push('')
      }
      continue
    }
    
    const fourCharTitleMatch = line.match(/^([\u4e00-\u9fa5]{4})[：:，,]/)
    if (fourCharTitleMatch) {
      if (formattedLines.length > 0 && formattedLines[formattedLines.length - 1] !== '') {
        formattedLines.push('')
      }
      formattedLines.push(line)
      formattedLines.push('')
    } else {
      formattedLines.push('\u3000\u3000' + line)
    }
  }
  
  while (formattedLines.length > 0 && formattedLines[formattedLines.length - 1] === '') {
    formattedLines.pop()
  }
  
  return formattedLines.join('\n')
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
  
  const footer = document.querySelector('.dibu') as HTMLElement
  if (footer) {
    const footerRect = footer.getBoundingClientRect()
    const windowHeight = window.innerHeight
    if (footerRect.top < windowHeight) {
      const overlap = windowHeight - footerRect.top + 20
      floatBtnsBottom.value = Math.max(20, overlap)
    } else {
      floatBtnsBottom.value = 80
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
    adjustPoetNameFontSize()
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
    if (res.data.total < 5 && poet.value?.name) {
      fetchExternalPoems()
    }
  } catch (error) {
    console.error('获取诗人作品失败:', error)
  }
}

const fetchExternalPoems = async () => {
  if (!poet.value?.name) return
  externalPoemsLoading.value = true
  try {
    const needCount = 10 - (poemTotal.value || 0)
    if (needCount <= 0) return
    
    const results = await getExternalPoems(poet.value.name, needCount)
    
    if (results.length > 0) {
      externalPoems.value = results
      for (const poem of results) {
        try {
          await importExternalPoem({
            title: poem.title,
            content: poem.content || '',
            author: poem.author || poet.value.name,
            dynasty: poem.dynasty || poet.value.dynastyName || ''
          })
        } catch (e) {
          console.error('导入诗词失败:', poem.title, e)
        }
      }
      
      const res = await getPoemsByPoet(poetId.value, {
        pageNum: 1,
        pageSize: 10
      })
      poems.value = res.data.list
      poemTotal.value = res.data.total
      if (res.data.total > 0) {
        externalPoems.value = []
      }
    }
  } catch (error) {
    console.error('获取外部诗词失败:', error)
  } finally {
    externalPoemsLoading.value = false
  }
}

const openExternalPoemDetail = async (poem: PoemSearchResult) => {
  try {
    const res = await importExternalPoem({
      title: poem.title,
      content: poem.content,
      author: poem.author,
      dynasty: poem.dynasty
    })
    router.push(`/poem/${res.data.id}`)
  } catch (error) {
    ElMessage.error('导入诗词失败，请稍后重试')
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

const handleFixExternalPoems = async () => {
  fixingPoems.value = true
  try {
    const res = await fixExternalPoems()
    ElMessage.success(`修复完成：共 ${res.data.total} 首，已修复 ${res.data.fixed} 首`)
    fetchPoems()
  } catch (error) {
    ElMessage.error('修复失败，请稍后重试')
  } finally {
    fixingPoems.value = false
  }
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

const saveDraft = async () => {
  if (!editText.value.trim()) {
    ElMessage.warning('内容不能为空')
    return
  }
  const sectionField = sectionContentMap[editingSection.value!]?.field
  if (!sectionField) return
  draftSaving.value = true
  try {
    await savePoetDraft({
      poetId: poetId.value,
      section: sectionField,
      content: editText.value.trim()
    })
    ElMessage.success('草稿已保存，等待审核后生效')
    closeEdit()
  } catch {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    draftSaving.value = false
  }
}

const openSuggestion = (sectionId: string) => {
  suggestionSection.value = sectionId
  suggestionContent.value = ''
  const sectionMap: Record<string, string> = {
    'section-intro': 'biography',
    'section-life': 'life_story',
    'section-influence': 'influence',
    'section-evaluation': 'evaluation',
    'section-anecdotes': 'anecdotes'
  }
  suggestionCategory.value = sectionMap[sectionId] || 'other'
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
      content: suggestionContent.value.trim(),
      category: suggestionCategory.value
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

const fetchAiConfig = async (showPrompt: boolean = false) => {
  if (!userStore.isLoggedIn) {
    if (showPrompt) {
      loginPromptVisible.value = true
    }
    return
  }
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

  if (!userStore.isLoggedIn) {
    loginPromptVisible.value = true
    return
  }

  if (!aiConfig.value) {
    await fetchAiConfig(false)
  }

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
    const res = await chat({ message: prompt, moduleCode: 'poet_chat' })
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
    saveChatCache()
  }
}

const adjustPoetNameFontSize = () => {
  nextTick(() => {
    if (!poet.value?.name) return
    
    const sidebarHeader = document.querySelector('.sidebar-header')
    const collapseBtn = document.querySelector('.collapse-btn')
    const sidebarTitle = document.querySelector('.sidebar-title') as HTMLElement
    
    if (!sidebarHeader || !collapseBtn || !sidebarTitle) return
    
    const headerWidth = sidebarHeader.clientWidth
    const btnWidth = collapseBtn.clientWidth
    const gap = 8
    const padding = 32
    const availableWidth = headerWidth - btnWidth - gap - padding
    
    if (availableWidth <= 0) return
    
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    
    const fontFamily = "'KaiTi', 'STKaiti', serif"
    const text = poet.value.name
    
    let fontSize = 38
    const minFontSize = 16
    
    while (fontSize >= minFontSize) {
      ctx.font = `${fontSize}px ${fontFamily}`
      const textWidth = ctx.measureText(text).width
      
      if (textWidth <= availableWidth) {
        break
      }
      
      fontSize -= 2
    }
    
    poetNameFontSize.value = Math.max(fontSize, minFontSize)
  })
}

const CHAT_CACHE_KEY = 'poet_chat_cache'
const CHAT_CACHE_DURATION = 60 * 60 * 1000

const getChatCacheKey = () => {
  return `${CHAT_CACHE_KEY}_${poetId.value}`
}

const loadChatCache = () => {
  try {
    const cacheKey = getChatCacheKey()
    const cached = localStorage.getItem(cacheKey)
    if (!cached) return false
    
    const data = JSON.parse(cached)
    const now = Date.now()
    
    if (now - data.timestamp > CHAT_CACHE_DURATION) {
      localStorage.removeItem(cacheKey)
      return false
    }
    
    if (data.messages && data.messages.length > 0) {
      chatMessages.value = data.messages
      return true
    }
    return false
  } catch {
    return false
  }
}

const saveChatCache = () => {
  try {
    const cacheKey = getChatCacheKey()
    const data = {
      messages: chatMessages.value,
      timestamp: Date.now()
    }
    localStorage.setItem(cacheKey, JSON.stringify(data))
  } catch (error) {
    console.error('保存聊天缓存失败:', error)
  }
}

const chatStarted = ref(false)

const initAiChat = () => {
  if (!userStore.isLoggedIn) return
  if (chatMessages.value.length === 0) {
    const hasCache = loadChatCache()
    if (hasCache) {
      chatStarted.value = true
    }
  }
}

const startAiChat = () => {
  if (!userStore.isLoggedIn) {
    loginPromptVisible.value = true
    return
  }
  chatStarted.value = true
  sendChatMessage(`${poet.value?.name}是谁`, true)
}

const handleGenerateAiFeatures = () => {
  if (!userStore.isLoggedIn) {
    loginPromptVisible.value = true
    return
  }
  generateAiFeatures()
}

const generateAiFeatures = async () => {
  if (!userStore.isLoggedIn) return
  if (!poet.value || aiFeatures.value || aiFeaturesLoading.value) return
  
  aiFeaturesLoading.value = true
  try {
    const { chat } = await import('@/api/modules/ai')
    const prompt = `【系统设定】你是一个古典诗词文化助手。请用简洁的语言概括诗人${poet.value.name}的诗词特点，不超过60字。要求：1.语言精炼 2.突出主要特点 3.不要使用markdown格式

【用户问题】请概括${poet.value.name}的诗词特点`
    
    const res = await chat({ message: prompt, moduleCode: 'poet_chat' })
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

const handleChatKeydown = (e: Event | KeyboardEvent) => {
  if ((e as KeyboardEvent).key === 'Enter' && !(e as KeyboardEvent).shiftKey) {
    e.preventDefault()
    sendChatMessage()
  }
}

const startChatDrag = (e: MouseEvent) => {
  const panel = document.querySelector('.panel-ai-chat') as HTMLElement
  if (!panel) return

  if (!chatDraggable.value) {
    const rect = panel.getBoundingClientRect()
    chatPos.value = { x: rect.left, y: rect.top }
    chatDraggable.value = true
  }

  chatDragging.value = true
  chatDragOffset.value = {
    x: e.clientX - chatPos.value.x,
    y: e.clientY - chatPos.value.y
  }
  document.addEventListener('mousemove', onChatDrag)
  document.addEventListener('mouseup', stopChatDrag)
}

const onChatDrag = (e: MouseEvent) => {
  if (!chatDragging.value) return
  const panel = document.querySelector('.panel-ai-chat') as HTMLElement
  const newX = e.clientX - chatDragOffset.value.x
  const newY = e.clientY - chatDragOffset.value.y
  const panelWidth = panel ? panel.offsetWidth : 400
  const panelHeight = panel ? panel.offsetHeight : 420
  const maxX = window.innerWidth - panelWidth
  const maxY = window.innerHeight - panelHeight
  chatPos.value = {
    x: Math.max(0, Math.min(newX, maxX)),
    y: Math.max(0, Math.min(newY, maxY))
  }
}

const stopChatDrag = () => {
  chatDragging.value = false
  document.removeEventListener('mousemove', onChatDrag)
  document.removeEventListener('mouseup', stopChatDrag)
}

watch(() => route.params.id, (newId) => {
  if (newId) {
    aiFeatures.value = ''
    fetchPoet()
    fetchPoems()
    nextTick(() => handleScroll())
  }
})

onMounted(async () => {
  changeFontSize(fontSizeLevel.value)
  await fetchPoet()
  loadFillStatus()
  fetchPoems()
  checkFavoriteStatus()
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('resize', adjustPoetNameFontSize)
  nextTick(() => handleScroll())
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', adjustPoetNameFontSize)
  stopChatDrag()
  stopReading()
  if (audioElement) {
    audioElement.pause()
    audioElement = null
  }
})
</script>

<template>
  <div class="poet-detail-page" v-loading="loading">
    <canvas ref="particleCanvasRef" class="particle-bg"></canvas>
    <aside class="poet-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <el-tooltip :content="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'" placement="right">
          <el-icon class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed; adjustPoetNameFontSize()">
            <component :is="sidebarCollapsed ? 'Expand' : 'Fold'" />
          </el-icon>
        </el-tooltip>
        <span v-show="!sidebarCollapsed" class="sidebar-title" :style="{ fontSize: poetNameFontSize + 'px' }">{{ poet?.name || '诗人' }}</span>
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
        <div class="poet-header-left">
          <el-tooltip content="返回诗人列表" placement="bottom">
            <el-button @click="router.push('/poet')" class="back-button" plain>
              <el-icon><ArrowLeft /></el-icon>
              返回列表
            </el-button>
          </el-tooltip>
        </div>
        <div class="poet-header-right">
          <div class="font-size-inline">
            <el-tooltip content="缩小字号" placement="top">
              <span class="fs-btn" :class="{ disabled: fontSizeLevel <= 0 }" @click="fontSizeLevel > 0 && changeFontSize(fontSizeLevel - 1)">
                <el-icon><Remove /></el-icon>
              </span>
            </el-tooltip>
            <span class="fs-label">{{ fontSizeMap[fontSizeLevel] }}</span>
            <el-tooltip content="放大字号" placement="top">
              <span class="fs-btn" :class="{ disabled: fontSizeLevel >= 3 }" @click="fontSizeLevel < 3 && changeFontSize(fontSizeLevel + 1)">
                <el-icon><Plus /></el-icon>
              </span>
            </el-tooltip>
          </div>
        </div>
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
          <p class="brief-text" :style="{ fontSize: fontSizeMap[fontSizeLevel] }">{{ formatContent(poet.biography) }}</p>
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
              <el-tooltip :content="isReading && currentReadingId === section.id ? '停止朗读' : '朗读该段落'" placement="top">
                <el-button
                  size="small"
                  :type="isReading && currentReadingId === section.id ? 'danger' : 'primary'"
                  text
                  @click="readText(section.id)"
                >
                  <el-icon><VideoPause v-if="isReading && currentReadingId === section.id" /><Microphone v-else /></el-icon>
                  {{ isReading && currentReadingId === section.id ? '停止朗读' : '朗读' }}
                </el-button>
              </el-tooltip>
              <el-tooltip content="提交修正意见" placement="top">
                <el-button
                  size="small"
                  type="warning"
                  text
                  @click="openSuggestion(section.id)"
                >
                  <el-icon><ChatLineSquare /></el-icon>
                  修正意见
                </el-button>
              </el-tooltip>
            </div>
          </div>
          <div class="section-divider"></div>
          <div class="section-body">
            <div v-if="previewField === sectionFieldNameMap[section.id] && previewContent" class="ai-preview">
              <div class="ai-hint">
                <el-icon><InfoFilled /></el-icon>
                <span>AI生成内容预览</span>
              </div>
              <p class="preview-text">{{ previewContent }}</p>
              <div class="preview-actions">
                <el-button type="success" size="small" :loading="submittingReview" @click="handleSubmitReview()">
                  <el-icon><Check /></el-icon>
                  提交审核
                </el-button>
                <el-button type="info" size="small" @click="handleCancelPreview()">
                  <el-icon><Close /></el-icon>
                  取消
                </el-button>
              </div>
            </div>
            <div v-else-if="submittedFields.has(sectionFieldNameMap[section.id]) && !hasSectionContent(section.id)" class="ai-submitted">
              <div class="ai-hint">
                <el-icon><InfoFilled /></el-icon>
                <span>AI内容已提交，等待管理员审核</span>
              </div>
            </div>
            <div v-else-if="hasSectionContent(section.id)" class="section-text" v-html="formatSectionContent(getSectionContent(section.id))"></div>
            <div v-else>
              <el-empty description="暂未收录" :image-size="60" />
              <div class="ai-fill-actions">
                <template v-if="fillStatusMap[sectionFieldNameMap[section.id]]">
                  <el-button 
                    v-if="fillStatusMap[sectionFieldNameMap[section.id]].approved" 
                    type="success" 
                    size="small" 
                    plain 
                    disabled
                  >
                    <el-icon><CircleCheck /></el-icon>
                    已审核通过
                  </el-button>
                  <el-button 
                    v-else-if="fillStatusMap[sectionFieldNameMap[section.id]].rejected" 
                    type="danger" 
                    size="small" 
                    plain 
                    disabled
                  >
                    <el-icon><CircleClose /></el-icon>
                    审核未通过
                  </el-button>
                  <el-button 
                    v-else 
                    type="success" 
                    size="small" 
                    plain 
                    disabled
                  >
                    <el-icon><Clock /></el-icon>
                    已提交待审核
                  </el-button>
                </template>
                <el-button 
                  v-else 
                  type="warning" 
                  size="small" 
                  plain 
                  :loading="fillingField === sectionFieldNameMap[section.id]" 
                  @click="handleAiFill(sectionFieldNameMap[section.id])"
                >
                  <el-icon><MagicStick /></el-icon>
                  AI填充
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div id="section-poems" class="content-section">
          <div class="section-header">
            <h2 class="section-title">
              <el-icon><Notebook /></el-icon>
              诗词作品
            </h2>
            <div class="section-actions">
              <el-tooltip content="修复外部诗词的诗人信息" placement="top">
                <el-button
                  size="small"
                  type="warning"
                  text
                  :loading="fixingPoems"
                  @click="handleFixExternalPoems"
                >
                  <el-icon><Refresh /></el-icon>
                  修复诗词
                </el-button>
              </el-tooltip>
              <el-tooltip :content="isReading && currentReadingId === 'section-poems' ? '停止朗读' : '朗读全部诗词'" placement="top">
                <el-button
                  size="small"
                  :type="isReading && currentReadingId === 'section-poems' ? 'danger' : 'primary'"
                  text
                  @click="readText('section-poems')"
                >
                  <el-icon><VideoPause v-if="isReading && currentReadingId === 'section-poems'" /><Microphone v-else /></el-icon>
                  {{ isReading && currentReadingId === 'section-poems' ? '停止朗读' : '朗读' }}
                </el-button>
              </el-tooltip>
            </div>
          </div>
          <div class="section-divider"></div>
          <div class="section-body">
            <template v-if="poems.length > 0">
              <div class="poems-list">
                <div
                  v-for="poem in poems"
                  :key="poem.id"
                  class="poem-item"
                  @click="openPoetryDetail(poem)"
                >
                  <span class="poem-link-title">{{ poem.title }}</span>
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
            </template>
            <template v-else-if="externalPoemsLoading">
              <div class="external-loading">
                <el-icon class="loading-icon"><Loading /></el-icon>
                <span>正在从古诗词库检索作品...</span>
              </div>
            </template>
            <template v-else-if="externalPoems.length > 0">
              <div class="external-poems-header">
                <el-icon><Connection /></el-icon>
                <span>以下作品来自古诗词库</span>
              </div>
              <div class="external-poems-links">
                <a
                  v-for="(poem, index) in externalPoems"
                  :key="index"
                  class="external-poem-link"
                  @click.prevent="openExternalPoemDetail(poem)"
                >
                  {{ poem.title }}
                </a>
              </div>
            </template>
            <el-empty v-else description="暂无作品" :image-size="60" />
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
          <div v-else class="related-poets-grid">
            <div
              v-for="rp in relatedPoets.slice(0, relatedExpanded ? relatedPoets.length : 3)"
              :key="rp.id"
              class="related-poet-card"
              @click="goToRelatedPoet(rp.id)"
            >
              <el-avatar :src="rp.avatar" :size="48">{{ rp.name?.charAt(0) }}</el-avatar>
              <div class="poet-card-info">
                <div class="poet-card-name">{{ rp.name }}</div>
                <div class="poet-card-meta">{{ rp.dynastyName }}</div>
              </div>
            </div>
          </div>
          <div v-if="relatedPoets.length > 3" class="related-more-btn">
            <el-button text type="primary" @click="relatedExpanded = !relatedExpanded">
              {{ relatedExpanded ? '收起' : '查看更多' }}
              <el-icon><ArrowDown v-if="!relatedExpanded" /><ArrowUp v-else /></el-icon>
            </el-button>
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
            <p class="features-preview">{{ formatContent(poet.influence).substring(0, 120) }}{{ poet.influence.length > 120 ? '...' : '' }}</p>
            <div class="features-detail-btn" v-if="poet.influence.length > 120">
              <el-button text type="primary" size="small" @click="featuresDetailVisible = true">
                展开详情
                <el-icon><ArrowDown /></el-icon>
              </el-button>
            </div>
          </div>
          <div v-else-if="aiFeatures" class="poem-features">
            <p class="features-preview">{{ aiFeatures }}</p>
          </div>
          <div v-else-if="!aiFeaturesLoading" class="poem-features-generate">
            <el-tooltip content="使用AI分析诗人创作风格特点" placement="top">
              <el-button type="primary" size="small" @click="handleGenerateAiFeatures">
                <el-icon><MagicStick /></el-icon>
                AI生成特点
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </div>

      <div
        class="floating-panel panel-ai-chat"
        :class="{ 'chat-dragging': chatDragging }"
        :style="chatDraggable ? { position: 'fixed', left: chatPos.x + 'px', top: chatPos.y + 'px', zIndex: 1000 } : {}"
      >
        <div class="panel-header chat-drag-handle" @mousedown="startChatDrag">
          <div class="header-left">
            <el-icon><MagicStick /></el-icon>
            <span>AI对话</span>
          </div>
          <el-tooltip :content="chatDraggable ? '固定位置' : '拖拽移动'" placement="top">
            <el-icon class="drag-toggle" @click.stop="chatDraggable = !chatDraggable">
              <Rank v-if="chatDraggable" /><MoreFilled v-else />
            </el-icon>
          </el-tooltip>
        </div>
        <div class="ai-chat-messages" ref="chatContainerRef">
          <template v-if="chatStarted || chatMessages.length > 0">
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
                <div 
                  class="message-content" 
                  :class="{ 'enlarged': enlargedMessageIndex === index }"
                  @click="toggleMessageEnlarge(index)"
                >{{ msg.content }}</div>
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
          </template>
          <div v-else class="chat-welcome">
            <div class="welcome-icon">
              <el-icon><MagicStick /></el-icon>
            </div>
            <p class="welcome-text">与AI助手对话，了解{{ poet?.name || '诗人' }}的故事</p>
            <el-button type="primary" @click="startAiChat" :loading="chatLoading">
              <el-icon><Promotion /></el-icon>
              开始对话
            </el-button>
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
          <el-tooltip content="发送消息" placement="top">
            <el-button type="primary" @click="sendChatMessage()" :loading="chatLoading" :disabled="!chatInput.trim()">
              <el-icon><Promotion /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="fade">
        <div v-if="editingSection" class="edit-overlay" @click.self="closeEdit">
          <div class="edit-panel">
            <div class="edit-panel-header">
              <span>编辑 - {{ editTitle }}</span>
              <el-tooltip content="关闭编辑" placement="left">
                <el-icon class="edit-close" @click="closeEdit"><Close /></el-icon>
              </el-tooltip>
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
              <span class="edit-tip">提示：编辑内容将保存为草稿，审核通过后生效</span>
              <div>
                <el-button @click="closeEdit">关闭</el-button>
                <el-button type="primary" :loading="draftSaving" @click="saveDraft">保存草稿</el-button>
              </div>
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
      v-model="featuresDetailVisible"
      :title="poet?.name + ' - 诗词特点详情'"
      width="680px"
      :close-on-click-modal="true"
      class="features-detail-dialog"
    >
      <div class="features-detail-content" v-if="poet?.influence">
        <div 
          v-for="(paragraph, index) in formatFeaturesContent(poet.influence).split('\n')" 
          :key="index"
          class="features-paragraph"
        >
          <template v-if="paragraph && /^[\u4e00-\u9fa5]{4}[：:，,]/.test(paragraph.trim())">
            <h4 class="features-subtitle">{{ paragraph.match(/^([\u4e00-\u9fa5]{4})/)?.[1] || '' }}{{ paragraph.substring(4) }}</h4>
          </template>
          <template v-else-if="paragraph">
            <p>{{ paragraph }}</p>
          </template>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      v-model="suggestionDialogVisible"
      title="提交修正意见"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="suggestion-form">
        <p class="suggestion-tip">如果您发现内容有误或有补充建议，请在此提交修正意见，管理员审核后会进行修改。</p>
        <el-form label-width="80px">
          <el-form-item label="意见分类">
            <el-select v-model="suggestionCategory" placeholder="请选择分类" style="width: 100%">
              <el-option label="简介内容" value="biography" />
              <el-option label="人物生平" value="life_story" />
              <el-option label="主要影响" value="influence" />
              <el-option label="历史评价" value="evaluation" />
              <el-option label="轶事典故" value="anecdotes" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="详细内容">
            <el-input
              v-model="suggestionContent"
              type="textarea"
              :rows="6"
              placeholder="请输入您的修正意见..."
              maxlength="1000"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="suggestionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="suggestionSubmitting" @click="submitSuggestion">提交</el-button>
      </template>
    </el-dialog>

    <LoginPrompt
      v-model:visible="loginPromptVisible"
      message="AI对话功能需要登录后才能使用"
    />
  </div>

  <div class="side-float-btns" :style="{ bottom: floatBtnsBottom + 'px' }">
    <div class="side-float-btn" @click="toggleMusic" :title="isMusicPlaying ? '暂停音乐' : '播放音乐'">
      <el-icon><Headset /></el-icon>
    </div>
    <div class="side-float-btn" @click="scrollToTop" title="返回顶部">
      <el-icon><Top /></el-icon>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use 'sass:color';

.poet-detail-page {
  display: flex;
  min-height: calc(100vh - 60px);
  position: relative;
  padding-top: 55px;
}

.particle-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

.poet-sidebar {
  position: sticky;
  top: 55px;
  height: calc(100vh - 55px);
  overflow-y: auto;
  width: 180px;
  flex-shrink: 0;
  align-self: flex-start;
  border-right: 1px solid $border-color;
  z-index: 99;
  display: flex;
  flex-direction: column;
  transition: width $transition-base;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
    background: url('/img/dts_1.jpg') no-repeat -60px 0 / cover;
    z-index: -1;
    pointer-events: none;
  }

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
  font-size: 22px;
  color: $text-color-secondary;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: all 0.2s ease;

  &:hover {
    color: $primary-color;
    background-color: rgba(0, 0, 0, 0.05);
  }
}

.sidebar-title {
  font-family: $font-family-title;
  color: $primary-color;
  white-space: nowrap;
  padding: $spacing-xs $spacing-sm;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15) 0%, rgba(255, 255, 255, 0.05) 100%);
  backdrop-filter: blur(1px);
  -webkit-backdrop-filter: blur(1px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
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
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  text-decoration: none;
  white-space: nowrap;
  position: relative;
  transform: translateX(0);

  .el-icon {
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }

  &:hover {
    color: #409eff;
    background: rgba(64, 158, 255, 0.06);
    transform: translateX(4px);

    .el-icon {
      transform: scale(1.15);
    }
  }

  &.active {
    color: #409eff;
    background: rgba(64, 158, 255, 0.15);
    font-weight: 600;
    position: relative;
    transform: translateX(4px);

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 20px;
      background: linear-gradient(to bottom, #409eff, rgba(64, 158, 255, 0.4));
      border-radius: 0 2px 2px 0;
      box-shadow: 0 0 6px rgba(64, 158, 255, 0.3);
    }

    &::after {
      content: '';
      position: absolute;
      right: 12px;
      top: 50%;
      transform: translateY(-50%);
      width: 4px;
      height: 4px;
      background: #409eff;
      border-radius: 50%;
      box-shadow: 0 0 4px rgba(64, 158, 255, 0.4);
    }
  }
}

.poet-main {
  flex: 1;
  min-width: 0;
  padding: $spacing-xl;
  position: relative;
  z-index: 1;

  @include responsive(md) {
    padding: $spacing-md;
  }
}

.poet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-lg;
  padding-top: 10px;

  .poet-header-right {
    margin-right: -30px;
  }
}

.back-button {
  font-family: $font-family-input;
}

.font-size-inline {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid $border-color;
  border-radius: 20px;
  padding: 4px 12px;
  box-shadow: $box-shadow;

  .fs-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    cursor: pointer;
    color: $text-color-secondary;
    transition: all $transition-fast;

    &:hover:not(.disabled) {
      color: $primary-color;
      background: rgba($primary-color, 0.1);
    }

    &.disabled {
      opacity: 0.3;
      cursor: not-allowed;
    }

    .el-icon {
      font-size: 16px;
    }
  }

  .fs-label {
    font-size: $font-size-sm;
    font-weight: 500;
    color: $text-color;
    min-width: 36px;
    text-align: center;
    font-family: $font-family-input;
  }
}

.poet-content {
  max-width: 860px;
}

.poet-hero {
  position: relative;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
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

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(
      135deg,
      rgba($primary-color, 0.25) 0%,
      rgba(0, 0, 0, 0.05) 40%,
      rgba($secondary-color, 0.18) 100%
    );
    pointer-events: none;
  }
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
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
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
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
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
  font-size: $font-size-xxl;
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
  margin: $spacing-sm 0 $spacing-lg;
  position: relative;
  background: linear-gradient(
    to right,
    $primary-color 0%,
    rgba($primary-color, 0.3) 20%,
    rgba($primary-color, 0.08) 50%,
    transparent 80%
  );

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 6px;
    height: 6px;
    background: $primary-color;
    border-radius: 50%;
    box-shadow: 0 0 6px rgba($primary-color, 0.4);
  }

  &::after {
    content: '';
    position: absolute;
    left: 18px;
    top: 50%;
    transform: translateY(-50%);
    width: 3px;
    height: 3px;
    background: rgba($primary-color, 0.5);
    border-radius: 50%;
  }
}

.section-body {
  min-height: 80px;
}

.ai-fill-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.ai-preview {
  background: linear-gradient(135deg, rgba($primary-color, 0.05), rgba($primary-color, 0.02));
  border: 1px solid rgba($primary-color, 0.2);
  border-radius: $border-radius-md;
  padding: $spacing-lg;
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 4px;
    height: 100%;
    background: linear-gradient(to bottom, $primary-color, rgba($primary-color, 0.5));
  }
  
  .ai-hint {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    margin-bottom: $spacing-md;
    padding: $spacing-sm $spacing-md;
    background: rgba($primary-color, 0.1);
    border-radius: $border-radius-sm;
    color: $primary-color;
    font-size: $font-size-sm;
    font-family: $font-family-input;
    border: 1px solid rgba($primary-color, 0.2);
    
    .el-icon {
      font-size: 16px;
      animation: pulse 2s infinite;
    }
  }
  
  .preview-text {
    margin: 0 0 $spacing-md 0;
    line-height: $line-height-loose;
    white-space: pre-line;
    color: $text-color;
    font-family: $font-family-base;
    font-size: var(--poet-content-font-size, $font-size-base);
    text-indent: 2em;
  }
  
  .preview-actions {
    display: flex;
    gap: $spacing-sm;
    justify-content: flex-end;
    padding-top: $spacing-md;
    border-top: 1px dashed rgba($primary-color, 0.3);
  }
}

.ai-submitted {
  background: linear-gradient(135deg, rgba($warning-color, 0.06), rgba($warning-color, 0.02));
  border: 1px solid rgba($warning-color, 0.25);
  border-radius: $border-radius-md;
  padding: $spacing-lg;
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 4px;
    height: 100%;
    background: linear-gradient(to bottom, $warning-color, rgba($warning-color, 0.5));
  }
  
  .ai-hint {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    margin-bottom: $spacing-md;
    padding: $spacing-sm $spacing-md;
    background: rgba($warning-color, 0.1);
    border-radius: $border-radius-sm;
    color: $warning-color;
    font-size: $font-size-sm;
    font-family: $font-family-input;
    border: 1px solid rgba($warning-color, 0.2);
    
    .el-icon {
      font-size: 16px;
      animation: pulse 2s infinite;
    }
  }
  
  .preview-text {
    margin: 0;
    line-height: $line-height-loose;
    white-space: pre-line;
    color: $text-color-secondary;
    font-family: $font-family-base;
    font-size: var(--poet-content-font-size, $font-size-base);
    text-indent: 2em;
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

.section-text {
  font-size: var(--poet-content-font-size, $font-size-base);
  color: $text-color;
  line-height: $line-height-loose;

  :deep(p) {
    margin: 0 0 12px 0;
    text-indent: 2em;
  }

  :deep(.section-subtitle) {
    font-size: calc(var(--poet-content-font-size, #{$font-size-base}) + 2px);
    font-weight: 700;
    color: $primary-color;
    margin: 20px 0 8px 0;
    padding-left: 12px;
    border-left: 3px solid $primary-color;
    letter-spacing: 2px;
    text-indent: 0;

    &:first-child {
      margin-top: 0;
    }
  }
}

.poems-list {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: $spacing-sm;
}

.poem-item {
  padding: $spacing-sm;
  text-align: center;
  cursor: pointer;
  transition: all $transition-fast;
  border-radius: $border-radius-sm;
  overflow: hidden;
  min-width: 0;

  &:hover {
    background: rgba($primary-color, 0.05);

    .poem-link-title {
      color: $primary-color;
    }
  }
}

.poem-link-title {
  font-size: $font-size-base;
  color: $text-color;
  font-family: $font-family-title;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 6px 12px;
  border: 1px solid $border-color-light;
  border-radius: $border-radius-sm;
  
  &:hover {
    border-color: $primary-color;
  }
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

.external-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-xl 0;
  gap: $spacing-md;
  color: $text-color-secondary;

  .loading-icon {
    font-size: 24px;
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.external-poems-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  margin-bottom: $spacing-md;
  background: rgba($primary-color, 0.05);
  border-radius: $border-radius-sm;
  font-size: $font-size-sm;
  color: $text-color-secondary;

  .el-icon {
    color: $primary-color;
  }
}

.external-poems-links {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
  padding: $spacing-sm 0;
}

.external-poem-link {
  color: $primary-color;
  cursor: pointer;
  font-size: $font-size-base;
  padding: $spacing-xs $spacing-sm;
  border-radius: $border-radius-sm;
  transition: all $transition-fast;
  text-decoration: none;

  &:hover {
    background: rgba($primary-color, 0.1);
    text-decoration: underline;
  }
}

.right-floating-panels {
  position: sticky;
  top: 55px;
  width: 400px;
  flex-shrink: 0;
  align-self: flex-start;
  z-index: 1001;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  overflow: hidden;
  padding: $spacing-xs $spacing-md;
  max-height: calc(100vh - 55px);
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
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
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
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border: 1px solid $border-color;
  }

  &.panel-features {
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border: 1px solid $border-color;
  }

  &.panel-ai-chat {
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border: 1px solid $border-color;
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 420px;
    max-height: calc(100vh - 55px - 240px);

    .panel-header {
      background: linear-gradient(135deg, rgba(#667eea, 0.08) 0%, rgba(#764ba2, 0.08) 100%);
      cursor: default;
      padding: $spacing-md;

      .header-left {
        .el-icon {
          font-size: 18px;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          -webkit-background-clip: text;
          background-clip: text;
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
  right: 393px;
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
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border: 1px solid $border-color;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    box-shadow: $box-shadow;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: hidden;

    .el-icon {
      font-size: 18px;
      color: $text-color-secondary;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      position: relative;
      z-index: 1;
    }

    &::before {
      content: '';
      position: absolute;
      inset: 0;
      border-radius: 50%;
      background: radial-gradient(circle at center, rgba($primary-color, 0.12), transparent 70%);
      opacity: 0;
      transition: opacity 0.3s ease;
    }

    &:hover {
      border-color: $primary-color;
      box-shadow: $box-shadow-md, 0 0 12px rgba($primary-color, 0.15);
      transform: translateY(-2px) scale(1.05);

      &::before {
        opacity: 1;
      }

      .el-icon {
        color: $primary-color;
        transform: scale(1.1);
      }
    }

    &:active {
      transform: translateY(0) scale(0.98);
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
  padding: $spacing-sm $spacing-md;
  min-height: 150px;
  @include scrollbar;
}

.chat-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 200px;
  gap: $spacing-md;
  text-align: center;

  .welcome-icon {
    width: 60px;
    height: 60px;
    border-radius: 50%;
    background: linear-gradient(135deg, rgba(#667eea, 0.15) 0%, rgba(#764ba2, 0.15) 100%);
    display: flex;
    align-items: center;
    justify-content: center;

    .el-icon {
      font-size: 28px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      -webkit-background-clip: text;
      background-clip: text;
      -webkit-text-fill-color: transparent;
    }
  }

  .welcome-text {
    font-size: $font-size-sm;
    color: $text-color-secondary;
    margin: 0;
    line-height: 1.6;
  }

  .el-button {
    border-radius: 20px;
    padding: 10px 24px;
    font-weight: 500;
    background: linear-gradient(135deg, $primary-color 0%, color.adjust($primary-color, $lightness: -10%) 100%);
    border: none;
    box-shadow: 0 2px 8px rgba($primary-color, 0.3);

    &:hover {
      box-shadow: 0 4px 12px rgba($primary-color, 0.4);
    }
  }
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
      background: linear-gradient(135deg, $primary-color 0%, color.adjust($primary-color, $lightness: -10%) 100%);
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
  cursor: pointer;
  transition: all 0.3s ease;

  &.enlarged {
    font-size: 16px;
    line-height: 1.8;
    background: rgba($primary-color, 0.05);
    border-radius: 12px;
    padding: 14px 18px;
  }

  &.loading {
    color: $text-color-secondary;
    font-style: italic;
    cursor: default;
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

    @include el-comment-input;

    :deep(.el-textarea__inner) {
      border-radius: $comment-input-radius;
      padding: $comment-input-padding;
    }
  }

  .el-button {
    border-radius: 20px;
    padding: 10px 24px;
    font-weight: 500;
    background: linear-gradient(135deg, $primary-color 0%, color.adjust($primary-color, $lightness: -10%) 100%);
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
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
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

:deep(.features-detail-dialog) {
  .el-dialog__header {
    border-bottom: 1px solid $border-color;
    padding: $spacing-md $spacing-lg;
    margin: 0;
    
    .el-dialog__title {
      font-family: $font-family-title;
      font-size: $font-size-xl;
      color: $primary-color;
    }
  }
  
  .el-dialog__body {
    padding: 0;
  }
}

.suggestion-form {
  .suggestion-tip {
    font-size: $font-size-sm;
    color: $text-color-secondary;
    margin-bottom: $spacing-md;
    line-height: 1.6;
  }
}

.related-poets-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-sm;
}

.related-poet-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md $spacing-sm;
  border-radius: $border-radius-md;
  cursor: pointer;
  transition: all $transition-fast;
  border: 1px solid $border-color-light;
  background: rgba(255, 255, 255, 0.7);
  text-align: center;

  &:hover {
    border-color: $primary-color;
    background: rgba($primary-color, 0.03);
    transform: translateY(-2px);
    box-shadow: $box-shadow;
  }

  .poet-card-info {
    width: 100%;
    min-width: 0;
  }

  .poet-card-name {
    font-size: $font-size-sm;
    font-weight: 600;
    color: $text-color;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .poet-card-meta {
    font-size: $font-size-xs;
    color: $text-color-secondary;
    margin-top: 2px;
  }
}

.related-more-btn {
  display: flex;
  justify-content: center;
  margin-top: $spacing-sm;
  padding-top: $spacing-sm;
  border-top: 1px solid $border-color-light;
}

.features-preview {
  font-size: $font-size-sm;
  color: $text-color;
  line-height: 1.7;
  margin: 0;
  text-align: justify;
  white-space: pre-line;
}

.features-detail-btn {
  display: flex;
  justify-content: center;
  margin-top: $spacing-sm;
  padding-top: $spacing-sm;
  border-top: 1px solid $border-color-light;
}

.features-detail-content {
  padding: $spacing-md;
  max-height: 60vh;
  overflow-y: auto;
}

.features-paragraph {
  margin-bottom: $spacing-md;
  
  &:last-child {
    margin-bottom: 0;
  }
  
  p {
    font-size: $font-size-base;
    color: $text-color;
    line-height: 1.8;
    margin: 0 0 $spacing-md 0;
    text-align: justify;
  }
  
  .features-subtitle {
    font-size: $font-size-lg;
    font-weight: 600;
    color: $primary-color;
    margin: $spacing-lg 0 $spacing-sm 0;
    padding-bottom: $spacing-xs;
    border-bottom: 2px solid rgba($primary-color, 0.2);
    display: inline-block;
    
    &:first-child {
      margin-top: 0;
    }
  }
}

.panel-ai-chat {
  &.chat-dragging {
    opacity: 0.9;
    box-shadow: $box-shadow-lg;
  }

  .chat-drag-handle {
    cursor: move;
    user-select: none;

    &:hover {
      background: rgba(0, 0, 0, 0.04);
    }
  }

  .drag-toggle {
    font-size: 16px;
    color: $text-color-secondary;
    cursor: pointer;
    transition: all $transition-fast;
    padding: 4px;
    border-radius: $border-radius-sm;

    &:hover {
      color: $primary-color;
      background: rgba($primary-color, 0.1);
    }
  }
}

</style>
