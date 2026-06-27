<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePoemStore } from '@/stores/poem'
import { useUserStore } from '@/stores/user'
import { usePreferencesStore } from '@/stores/preferences'
import type { Dynasty, Category, Poem, Poet } from '@/types/model'
import { getDynastyList } from '@/api/modules/dynasty'
import { getCategoryList } from '@/api/modules/category'
import { likePoem, favoritePoem, getModernPoems } from '@/api/modules/poem'
import { getPoetList } from '@/api/modules/poet'
import { getCache, setCache } from '@/utils/storage'
import { searchApihzPoems, getApihzPoetryDetail } from '@/api/modules/external-poetry'
import type { PoemSearchResult } from '@/api/modules/external-poetry'
import { searchPoetryByTitle, searchPoetryByAuthor, getRandomPoetry, type PoetryDbPoem } from '@/api/modules/external-poetry'
import { getAiModuleConfig, chat, type AiModuleConfig } from '@/api/modules/ai'
import { smartSearch, getSearchSuggestions, getHotSearches, getSearchHistory, clearSearchHistory } from '@/api/modules/search'
import type { SmartSearchResult } from '@/api/modules/search'
import { getCachedContent as getPoemCachedContent, saveCachedContent as savePoemCachedContent } from '@/api/modules/poem-content'
import LoginPrompt from '@/components/common/LoginPrompt.vue'
import { useParticles } from '@/composables/useParticles'

const router = useRouter()
const route = useRoute()
const poemStore = usePoemStore()
const userStore = useUserStore()
const preferencesStore = usePreferencesStore()

const particleCanvasRef = ref<HTMLCanvasElement | null>(null)
useParticles(particleCanvasRef, {
  count: 60,
  colors: ['#d4af87', '#f0e4d7', '#c9a06c'],
  opacityRange: [0.15, 0.3],
  sizeRange: [1, 2.5]
})

const bannerCanvasRef = ref<HTMLCanvasElement | null>(null)
useParticles(bannerCanvasRef, {
  count: 40,
  colors: ['#d4af87', '#f0e4d7'],
  opacityRange: [0.25, 0.4],
  sizeRange: [1, 2.5]
})

const loading = ref(false)
const dynasties = ref<Dynasty[]>([])
const categories = ref<Category[]>([])
const poets = ref<Poet[]>([])

const activeTab = ref<'classical' | 'modern' | 'foreign'>('classical')

const poemImages = [
  '/img/lt_jx_2.jpg',
  '/img/lt_jx_3.jpg',
  '/img/lt_jx_4.jpg'
]

const filters = ref({
  dynastyId: undefined as number | undefined,
  categoryId: undefined as number | undefined,
  poetId: undefined as number | undefined,
  keyword: '',
  sortBy: 'latest' as 'latest' | 'popular' | 'likes'
})

const modernFilters = ref({
  isOriginal: undefined as boolean | undefined,
  hasCertifiedPoet: undefined as boolean | undefined,
  sortBy: 'latest' as 'latest' | 'popular' | 'likes'
})

interface ForeignPoem {
  id: string
  title: string
  content: string
  author: string
  linecount: string
  source: 'foreign'
  originalLines: string[]
  chineseContent?: string
}

const foreignFilters = ref({
  keyword: '',
  author: ''
})
const foreignPoemList = ref<ForeignPoem[]>([])
const foreignTotal = ref(0)
const foreignLoading = ref(false)

const currentPage = ref(1)
const pageSize = ref(preferencesStore.userPreferences.pageSize || 10)

const poems = computed(() => {
  if (activeTab.value === 'classical') {
    return poemStore.poemList
  }
  if (activeTab.value === 'modern') {
    return modernPoemList.value
  }
  return foreignPoemList.value
})
const total = computed(() => {
  if (activeTab.value === 'classical') {
    return poemStore.total
  }
  if (activeTab.value === 'modern') {
    return modernTotal.value
  }
  return foreignTotal.value
})

const modernPoemList = ref<Poem[]>([])
const modernTotal = ref(0)

const externalPoemResults = ref<PoemSearchResult[]>([])
const externalSearching = ref(false)
const selectedExternalPoem = ref<PoemSearchResult | null>(null)
const externalPoemDetail = ref<any>(null)
const externalDetailLoading = ref(false)
const externalActiveTab = ref('translation')

const poemHistory = ref<PoemSearchResult[]>([])
const hoveredLineIdx = ref<number | null>(null)
const explainedLineIdxs = ref<Set<number>>(new Set())
const lineExplanations = ref<Map<number, string>>(new Map())
const lineExplaining = ref<Set<number>>(new Set())

const searchSuggestions = ref<string[]>([])
const hotSearches = ref<string[]>([])
const searchHistoryList = ref<string[]>([])
const showSearchPanel = ref(false)
const searchLevel = ref<string>('')
const searchMessage = ref('')

const annotationContent = ref('')
const appreciationContent = ref('')
const backgroundContent = ref('')
const annotationLoading = ref(false)
const appreciationLoading = ref(false)
const backgroundLoading = ref(false)

const authorInfoExpanded = ref(false)
const authorInfoContent = ref('')
const authorInfoLoading = ref(false)

const backgroundKnowledge = ref({
  dynastyPoetry: { content: '', loading: false, loaded: false },
  literarySchool: { content: '', loading: false, loaded: false },
  historicalContext: { content: '', loading: false, loaded: false }
})
const activeKnowledgePanels = ref<string[]>([])
const expandedKnowledgePanels = ref<string[]>([])

const contentCache = ref<Map<string, string>>(new Map())

const activePanel = ref<'annotation' | 'appreciation' | 'background' | null>(null)
const showEnlargedContent = ref(false)

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}

const aiChatVisible = ref(false)
const aiChatMessages = ref<ChatMessage[]>([])
const aiChatInput = ref('')
const aiChatLoading = ref(false)
const aiChatContainerRef = ref<HTMLElement | null>(null)
const aiConfig = ref<AiModuleConfig | null>(null)
const loginPromptVisible = ref(false)
let aiChatInitialized = false

const resources = [
  { name: '古诗文网', url: 'https://www.gushiwen.org', desc: '经典古诗文鉴赏' },
  { name: '中华典藏', url: 'https://www.zhonghuadiancang.com', desc: '中华古籍全文检索' },
  { name: '中国哲学书电子化计划', url: 'https://ctext.org', desc: '先秦至清代文献' },
]

const sortOptions = [
  { value: 'latest', label: '最新发布' },
  { value: 'popular', label: '最多浏览' },
  { value: 'likes', label: '最多点赞' }
]

const fetchPoems = async () => {
  loading.value = true
  externalPoemResults.value = []
  searchLevel.value = ''
  searchMessage.value = ''
  
  try {
    if (activeTab.value === 'classical') {
      const result: SmartSearchResult = await smartSearch({
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        dynastyId: filters.value.dynastyId,
        categoryId: filters.value.categoryId,
        poetId: filters.value.poetId,
        keyword: filters.value.keyword,
        sortBy: filters.value.sortBy
      })

      poemStore.poemList = result.list
      poemStore.total = result.total
      searchLevel.value = result.searchLevel
      searchMessage.value = result.message

      if (result.searchLevel === 'fuzzy' || result.searchLevel === 'pinyin') {
        ElMessage.info(result.message)
      }

      if (result.suggestExternal && filters.value.keyword) {
        const cacheKey = `external_search_keyword_${filters.value.keyword}`
        const cachedResults = getCache<PoemSearchResult[]>(cacheKey)
        
        if (cachedResults && cachedResults.length > 0) {
          externalPoemResults.value = cachedResults
          ElMessage.info('已加载缓存的搜索结果')
          if (externalPoemResults.value.length > 0 && externalPoemResults.value.length <= 2) {
            await openPoemDetail(externalPoemResults.value[0])
          }
        } else {
          try {
            await ElMessageBox.confirm(
              '本地未找到相关诗词，是否进行全网搜索？（搜索时间较长）',
              '提示',
              {
                confirmButtonText: '全网搜索',
                cancelButtonText: '取消',
                type: 'info'
              }
            )
            externalSearching.value = true
            externalPoemResults.value = await searchApihzPoems(filters.value.keyword, 20)
            
            if (externalPoemResults.value.length > 0) {
              setCache(cacheKey, externalPoemResults.value, 24 * 60 * 60 * 1000)
            }
            
            if (externalPoemResults.value.length > 0 && externalPoemResults.value.length <= 2) {
              await openPoemDetail(externalPoemResults.value[0])
            }
          } catch {
            // 用户取消
          } finally {
            externalSearching.value = false
          }
        }
      } else if (result.suggestExternal && filters.value.categoryId) {
        const category = categories.value.find(c => c.id === filters.value.categoryId)
        if (category) {
          const cacheKey = `external_search_category_${filters.value.categoryId}`
          const cachedResults = getCache<PoemSearchResult[]>(cacheKey)
          
          if (cachedResults && cachedResults.length > 0) {
            externalPoemResults.value = cachedResults
            ElMessage.info('已加载缓存的搜索结果')
          } else {
            try {
              await ElMessageBox.confirm(
                `本地没有"${category.name}"分类的诗词，是否进行全网搜索？（搜索时间较长）`,
                '提示',
                {
                  confirmButtonText: '全网搜索',
                  cancelButtonText: '取消',
                  type: 'info'
                }
              )
              externalSearching.value = true
              externalPoemResults.value = await searchApihzPoems(category.name, 20)
              
              if (externalPoemResults.value.length > 0) {
                setCache(cacheKey, externalPoemResults.value, 24 * 60 * 60 * 1000)
              }
            } catch {
              // 用户取消
            } finally {
              externalSearching.value = false
            }
          }
        }
      }
    } else if (activeTab.value === 'modern') {
      const res = await getModernPoems({
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        isOriginal: modernFilters.value.isOriginal,
        hasCertifiedPoet: modernFilters.value.hasCertifiedPoet,
        sortBy: modernFilters.value.sortBy
      })
      modernPoemList.value = res.data.list
      modernTotal.value = res.data.total
    } else if (activeTab.value === 'foreign') {
      await fetchForeignPoems()
    }
  } catch (error) {
    ElMessage.error('获取诗词列表失败')
  } finally {
    loading.value = false
  }
}

const fetchForeignPoems = async () => {
  foreignLoading.value = true
  try {
    let results: PoetryDbPoem[] = []
    if (foreignFilters.value.keyword) {
      results = await searchPoetryByTitle(foreignFilters.value.keyword)
    } else if (foreignFilters.value.author) {
      results = await searchPoetryByAuthor(foreignFilters.value.author)
    } else {
      results = await getRandomPoetry(20)
    }
    foreignPoemList.value = results.map((poem: PoetryDbPoem, index: number) => ({
      id: `foreign_${index}`,
      title: poem.title,
      content: poem.lines?.join('\n') || '',
      author: poem.author,
      linecount: poem.linecount,
      source: 'foreign' as const,
      originalLines: poem.lines || []
    }))
    foreignTotal.value = foreignPoemList.value.length
  } catch (error) {
    ElMessage.error('获取外文诗词失败')
  } finally {
    foreignLoading.value = false
  }
}

const openPoemDetail = async (poem: PoemSearchResult) => {
  if (poem.source === 'local' && poem.id) {
    router.push(`/poem/${poem.id}`)
  } else if (poem.source === 'foreign') {
    // 外文诗歌直接使用 PoetryDB 返回的英文原文
    openForeignPoemDetail(poem)
  } else {
    selectedExternalPoem.value = poem
    externalDetailLoading.value = true
    externalActiveTab.value = 'translation'
    explainedLineIdxs.value = new Set()
    lineExplanations.value = new Map()
    lineExplaining.value = new Set()
    aiChatMessages.value = []
    aiChatInitialized = false
    annotationContent.value = ''
    appreciationContent.value = ''
    backgroundContent.value = ''
    activePanel.value = null
    showEnlargedContent.value = false

    const exists = poemHistory.value.find(h => h.title === poem.title)
    if (!exists) {
      poemHistory.value.push(poem)
      if (poemHistory.value.length > 10) {
        poemHistory.value.shift()
      }
    }

    setCache(`external_poem_${poem.title}`, poem, 24 * 60 * 60 * 1000)
    syncFiltersToUrl()

    try {
      const result = await getApihzPoetryDetail(poem.title)
      externalPoemDetail.value = result
    } catch (e) {
      console.error('获取诗词详情失败:', e)
      externalPoemDetail.value = null
    } finally {
      externalDetailLoading.value = false
    }
  }
}

const goBackInHistory = () => {
  if (poemHistory.value.length > 1) {
    poemHistory.value.pop()
    const prevPoem = poemHistory.value[poemHistory.value.length - 1]
    openPoemDetail(prevPoem)
  }
}

const ensureAiConfig = async () => {
  if (!aiConfig.value) {
    try {
      const res = await getAiModuleConfig('poetry_chat')
      aiConfig.value = res.data
    } catch {
      console.error('获取AI配置失败')
    }
  }
}



const generateAnnotation = async () => {
  if (!userStore.isLoggedIn) {
    loginPromptVisible.value = true
    return
  }

  activePanel.value = 'annotation'
  showEnlargedContent.value = true

  const poemTitle = selectedExternalPoem.value?.title || ''
  const poemAuthor = selectedExternalPoem.value?.author || ''
  const poemContent = poemSentences.value.join('\n')

  const cached = await getCachedContent(poemTitle, poemAuthor, 'annotation')
  if (cached) {
    annotationContent.value = cached
    return
  }

  annotationLoading.value = true
  annotationContent.value = ''

  try {
    await ensureAiConfig()
    const prompt = `【系统设定】你是一个文化渊博的大学者，精通古今中外诗歌知识。请为这首诗提供详细的译文注释。

当前诗词：《${poemTitle}》${poemAuthor ? ' - ' + poemAuthor : ''}
诗词内容：
${poemContent}

回答要求：
1. 逐句翻译，用现代汉语解释每句诗的含义
2. 对关键词语进行注释，解释其在古代的含义
3. 语言简洁易懂，适合普通读者阅读
4. 不要使用markdown格式，直接输出纯文本

【用户问题】请为这首诗提供译文注释`

    const { chat } = await import('@/api/modules/ai')
    const res = await chat({ message: prompt, moduleCode: 'poetry_chat' })
    annotationContent.value = res.data.reply || '抱歉，暂时无法生成注释'
    await saveCachedContent(poemTitle, poemAuthor, 'annotation', annotationContent.value, 'ai')
  } catch {
    annotationContent.value = 'AI服务暂时不可用，请稍后重试'
  } finally {
    annotationLoading.value = false
  }
}

const generateAppreciation = async () => {
  if (!userStore.isLoggedIn) {
    loginPromptVisible.value = true
    return
  }

  activePanel.value = 'appreciation'
  showEnlargedContent.value = true

  const poemTitle = selectedExternalPoem.value?.title || ''
  const poemAuthor = selectedExternalPoem.value?.author || ''
  const poemContent = poemSentences.value.join('\n')

  const cached = await getCachedContent(poemTitle, poemAuthor, 'appreciation')
  if (cached) {
    appreciationContent.value = cached
    return
  }

  appreciationLoading.value = true
  appreciationContent.value = ''

  try {
    await ensureAiConfig()
    const prompt = `【系统设定】你是一个文化渊博的大学者，精通古今中外诗歌知识。请提供这首诗的历代名人点评和历史评价。

当前诗词：《${poemTitle}》${poemAuthor ? ' - ' + poemAuthor : ''}
诗词内容：
${poemContent}

回答要求：
1. 列举历代名家对此诗的点评和评价（如钟嵘、严羽、王国维等诗论家的评语）
2. 引用真实的古籍文献中的评价原文
3. 说明此诗在文学史上的地位和影响
4. 如果是名篇，引用经典评论（如《诗品》《沧浪诗话》《人间词话》等）
5. 不要使用markdown格式，直接输出纯文本，不要用**加粗等符号

【用户问题】请提供这首诗的历代名人点评和历史评价`

    const { chat } = await import('@/api/modules/ai')
    const res = await chat({ message: prompt, moduleCode: 'poetry_chat' })
    appreciationContent.value = res.data.reply || '抱歉，暂时无法生成鉴赏'
    await saveCachedContent(poemTitle, poemAuthor, 'appreciation', appreciationContent.value, 'ai')
  } catch {
    appreciationContent.value = 'AI服务暂时不可用，请稍后重试'
  } finally {
    appreciationLoading.value = false
  }
}

const generateBackground = async () => {
  if (!userStore.isLoggedIn) {
    loginPromptVisible.value = true
    return
  }

  const poemTitle = selectedExternalPoem.value?.title || ''
  const poemAuthor = selectedExternalPoem.value?.author || ''
  const poemDynasty = selectedExternalPoem.value?.dynasty || ''

  const cached = await getCachedContent(poemTitle, poemAuthor, 'background')
  if (cached) {
    backgroundContent.value = cached
    return
  }

  backgroundLoading.value = true
  backgroundContent.value = ''

  try {
    await ensureAiConfig()
    const prompt = `【系统设定】你是一个文化渊博的大学者，精通古今中外诗歌知识。请介绍这首诗的创作背景。

当前诗词：《${poemTitle}》${poemAuthor ? ' - ' + poemAuthor : ''}${poemDynasty ? ' 【' + poemDynasty + '】' : ''}

回答要求：
1. 介绍诗人的生平和时代背景
2. 说明这首诗的创作缘由和历史背景
3. 解释诗歌与诗人经历的关系
4. 语言简洁易懂，适合普通读者阅读
5. 不要使用markdown格式，直接输出纯文本

【用户问题】请介绍这首诗的创作背景`

    const { chat } = await import('@/api/modules/ai')
    const res = await chat({ message: prompt, moduleCode: 'poetry_chat' })
    backgroundContent.value = res.data.reply || '抱歉，暂时无法生成创作背景'
    await saveCachedContent(poemTitle, poemAuthor, 'background', backgroundContent.value, 'ai')
  } catch {
    backgroundContent.value = 'AI服务暂时不可用，请稍后重试'
  } finally {
    backgroundLoading.value = false
  }
}

const explainLine = async (idx: number) => {
  if (!userStore.isLoggedIn) {
    loginPromptVisible.value = true
    return
  }

  if (explainedLineIdxs.value.has(idx)) {
    explainedLineIdxs.value.delete(idx)
    explainedLineIdxs.value = new Set(explainedLineIdxs.value)
    return
  }

  explainedLineIdxs.value.add(idx)
  explainedLineIdxs.value = new Set(explainedLineIdxs.value)

  if (lineExplanations.value.has(idx) || lineExplaining.value.has(idx)) return

  const sentence = poemSentences.value[idx]
  const poemTitle = selectedExternalPoem.value?.title || ''
  const poemAuthor = selectedExternalPoem.value?.author || ''

  lineExplaining.value.add(idx)
  lineExplaining.value = new Set(lineExplaining.value)

  try {
    if (!aiConfig.value) {
      const res = await getAiModuleConfig('poetry_chat')
      aiConfig.value = res.data
    }

    const prompt = `【系统设定】你是一个文化渊博的大学者，精通古今中外诗歌知识。请用简洁易懂的语言解释这句诗的含义、意象和情感。

当前诗词：《${poemTitle}》${poemAuthor ? ' - ' + poemAuthor : ''}
需要解释的诗句：${sentence}

回答要求：
1. 语言简洁精炼，不超过150字
2. 解释诗句的字面意思和深层含义
3. 分析其中的意象和修辞手法
4. 说明表达的情感或思想
5. 直接输出纯文本，不要使用任何markdown符号（如**、##、- 列表等）

【用户问题】请解释这句诗`

    const { chat } = await import('@/api/modules/ai')
    const res = await chat({ message: prompt, moduleCode: 'poetry_chat' })
    lineExplanations.value.set(idx, res.data.reply || '抱歉，暂时无法解析')
    lineExplanations.value = new Map(lineExplanations.value)
  } catch {
    lineExplanations.value.set(idx, 'AI服务暂时不可用，请稍后重试')
    lineExplanations.value = new Map(lineExplanations.value)
  } finally {
    lineExplaining.value.delete(idx)
    lineExplaining.value = new Set(lineExplaining.value)
  }
}

const closeLineExplanation = (idx: number) => {
  explainedLineIdxs.value.delete(idx)
  explainedLineIdxs.value = new Set(explainedLineIdxs.value)
}

const formatContent = (content: string): string[] => {
  if (!content) return []
  return content
    .split(/\n+/)
    .map(p => p.trim())
    .filter(p => p.length > 0)
}

const copyContent = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

const getCacheKey = (poemTitle: string, poetName: string, contentType: string) => {
  return `${poemTitle}_${poetName}_${contentType}`
}

const getCachedContent = async (poemTitle: string, poetName: string, contentType: string): Promise<string> => {
  const key = getCacheKey(poemTitle, poetName, contentType)
  if (contentCache.value.has(key)) {
    return contentCache.value.get(key)!
  }
  try {
    const res = await getPoemCachedContent(poemTitle, poetName, contentType)
    if (res.data?.content) {
      contentCache.value.set(key, res.data.content)
      return res.data.content
    }
  } catch {}
  return ''
}

const saveCachedContent = async (poemTitle: string, poetName: string, contentType: string, content: string, source: string) => {
  const key = getCacheKey(poemTitle, poetName, contentType)
  contentCache.value.set(key, content)
  try {
    await savePoemCachedContent({ poemTitle, poetName, contentType, content, source })
  } catch (e) {
    console.error('保存缓存失败:', e)
  }
}

const toggleAuthorInfo = async () => {
  authorInfoExpanded.value = !authorInfoExpanded.value
  if (authorInfoExpanded.value && !authorInfoContent.value) {
    await loadAuthorInfo()
  }
}

const loadAuthorInfo = async () => {
  const poemTitle = selectedExternalPoem.value?.title || ''
  const poemAuthor = selectedExternalPoem.value?.author || ''
  if (!poemAuthor) return

  authorInfoLoading.value = true
  try {
    const cached = await getCachedContent(poemTitle, poemAuthor, 'author_info')
    if (cached) {
      authorInfoContent.value = cached
      return
    }

    await ensureAiConfig()
    const prompt = `【系统设定】你是一个文化渊博的大学者，精通古今中外诗歌知识。请介绍这位诗人的生平和文学成就。

诗人姓名：${poemAuthor}
${selectedExternalPoem.value?.dynasty ? '朝代：' + selectedExternalPoem.value.dynasty : ''}

回答要求：
1. 介绍诗人的生平经历
2. 说明诗人的文学风格和代表作品
3. 语言简洁易懂，适合普通读者阅读
4. 不要使用markdown格式，直接输出纯文本

【用户问题】请介绍这位诗人`

    const { chat } = await import('@/api/modules/ai')
    const res = await chat({ message: prompt, moduleCode: 'poetry_chat' })
    authorInfoContent.value = res.data.reply || '抱歉，暂时无法生成诗人介绍'
    await saveCachedContent(poemTitle, poemAuthor, 'author_info', authorInfoContent.value, 'ai')
  } catch {
    authorInfoContent.value = 'AI服务暂时不可用，请稍后重试'
  } finally {
    authorInfoLoading.value = false
  }
}

const toggleKnowledgePanel = async (panel: string) => {
  const index = activeKnowledgePanels.value.indexOf(panel)
  if (index > -1) {
    activeKnowledgePanels.value.splice(index, 1)
  } else {
    activeKnowledgePanels.value.push(panel)
    await loadKnowledgeContent(panel)
  }
}

const loadKnowledgeContent = async (panel: string) => {
  const poemTitle = selectedExternalPoem.value?.title || ''
  const poemAuthor = selectedExternalPoem.value?.author || ''
  const poemDynasty = selectedExternalPoem.value?.dynasty || ''

  const panelData = backgroundKnowledge.value[panel as keyof typeof backgroundKnowledge.value]
  if (panelData.loaded) return

  panelData.loading = true
  try {
    const contentType = `knowledge_${panel}`
    const cached = await getCachedContent(poemTitle, poemAuthor, contentType)
    if (cached) {
      panelData.content = cached
      panelData.loaded = true
      return
    }

    await ensureAiConfig()
    let prompt = ''
    if (panel === 'dynastyPoetry') {
      prompt = `【系统设定】你是一个文化渊博的大学者，精通古今中外诗歌知识。请介绍${poemDynasty}朝代的诗歌特点。

回答要求：
1. 介绍该朝代诗歌的主要特点和风格
2. 列举代表诗人和作品
3. 语言简洁易懂
4. 不要使用markdown格式，直接输出纯文本

【用户问题】请介绍${poemDynasty}朝代的诗歌特点`
    } else if (panel === 'literarySchool') {
      prompt = `【系统设定】你是一个文化渊博的大学者，精通古今中外诗歌知识。请介绍与《${poemTitle}》相关的文学流派。

当前诗词：《${poemTitle}》${poemAuthor ? ' - ' + poemAuthor : ''}

回答要求：
1. 分析这首诗属于哪个文学流派
2. 介绍该流派的特点和代表人物
3. 语言简洁易懂
4. 不要使用markdown格式，直接输出纯文本

【用户问题】请介绍与这首诗相关的文学流派`
    } else if (panel === 'historicalContext') {
      prompt = `【系统设定】你是一个文化渊博的大学者，精通古今中外诗歌知识。请介绍《${poemTitle}》的历史文化背景。

当前诗词：《${poemTitle}》${poemAuthor ? ' - ' + poemAuthor : ''}${poemDynasty ? ' 【' + poemDynasty + '】' : ''}

回答要求：
1. 介绍这首诗创作时的历史背景
2. 说明当时的社会文化环境
3. 语言简洁易懂
4. 不要使用markdown格式，直接输出纯文本

【用户问题】请介绍这首诗的历史文化背景`
    }

    const { chat } = await import('@/api/modules/ai')
    const res = await chat({ message: prompt, moduleCode: 'poetry_chat' })
    panelData.content = res.data.reply || '抱歉，暂时无法生成内容'
    panelData.loaded = true
    await saveCachedContent(poemTitle, poemAuthor, contentType, panelData.content, 'ai')
  } catch {
    panelData.content = 'AI服务暂时不可用，请稍后重试'
  } finally {
    panelData.loading = false
  }
}

const getKnowledgeSummary = (content: string, maxLength: number = 100) => {
  if (!content) return ''
  if (content.length <= maxLength) return content
  return content.substring(0, maxLength) + '...'
}

const isKnowledgeExpanded = (panel: string) => {
  return expandedKnowledgePanels.value.includes(panel)
}

const toggleKnowledgeExpand = (panel: string) => {
  const index = expandedKnowledgePanels.value.indexOf(panel)
  if (index > -1) {
    expandedKnowledgePanels.value.splice(index, 1)
  } else {
    expandedKnowledgePanels.value.push(panel)
  }
}

const buildAiPrompt = (userMessage: string, isFirst: boolean = false) => {
  const poemTitle = selectedExternalPoem.value?.title || '该诗词'
  const poemAuthor = selectedExternalPoem.value?.author || ''
  const poemContent = poemSentences.value.join('')

  const poemContext = `\n\n当前诗词：《${poemTitle}》${poemAuthor ? ' - ' + poemAuthor : ''}\n诗词内容：${poemContent}`

  let systemConstraint = ''
  if (aiConfig.value?.promptTemplate) {
    const maxLength = isFirst ? (aiConfig.value.firstResponseLength || 120) : (aiConfig.value.maxLength || 300)
    const styleHint = isFirst
      ? `这是首次提问，请用2-3句话简要概括即可，不超过${aiConfig.value.firstResponseLength || 120}字`
      : `根据问题复杂度适当展开，但不超过${maxLength}字`

    systemConstraint = aiConfig.value.promptTemplate
      .replace(/\{poetName\}/g, poemAuthor)
      .replace(/\{poemTitle\}/g, poemTitle)
      .replace(/\{poemContent\}/g, poemContent)
      .replace(/\{maxLength\}/g, String(maxLength))
      .replace(/\{styleHint\}/g, styleHint)

    if (!systemConstraint.includes(poemTitle)) {
      systemConstraint += poemContext
    }
  } else {
    const maxLength = isFirst ? 120 : 300
    systemConstraint = `你是一个文化渊博的大学者，精通古今中外诗歌知识。正在为用户讲解一首古诗。
当前诗词：《${poemTitle}》${poemAuthor ? ' - ' + poemAuthor : ''}
诗词内容：${poemContent}

回答要求：
1. 语言简洁精炼，避免冗余
2. 使用通俗易懂的中文
3. 重点突出，条理清晰
4. 不要使用markdown格式，直接输出纯文本
${isFirst ? '5. 这是首次提问，请用2-3句话简要概括即可' : '5. 根据问题复杂度适当展开，但不超过' + maxLength + '字'}`
  }

  return `【系统设定】${systemConstraint}\n\n【用户问题】${userMessage}`
}

const sendAiChatMessage = async (message?: string, isFirst: boolean = false) => {
  const msg = message || aiChatInput.value.trim()
  if (!msg || aiChatLoading.value) return

  if (!userStore.isLoggedIn) {
    loginPromptVisible.value = true
    return
  }

  if (!aiConfig.value) {
    try {
      const res = await getAiModuleConfig('poetry_chat')
      aiConfig.value = res.data
    } catch {
      console.error('获取AI配置失败')
    }
  }

  aiChatMessages.value.push({
    role: 'user',
    content: msg,
    timestamp: Date.now()
  })
  aiChatInput.value = ''
  aiChatLoading.value = true
  scrollToAiChatBottom()

  try {
    const { chat } = await import('@/api/modules/ai')
    const prompt = buildAiPrompt(msg, isFirst)
    const res = await chat({ message: prompt, moduleCode: 'poetry_chat' })
    aiChatMessages.value.push({
      role: 'assistant',
      content: res.data.reply || '抱歉，暂时无法回答',
      timestamp: Date.now()
    })
  } catch {
    aiChatMessages.value.push({
      role: 'assistant',
      content: 'AI服务暂时不可用，请稍后重试',
      timestamp: Date.now()
    })
  } finally {
    aiChatLoading.value = false
    scrollToAiChatBottom()
  }
}

const handleAiChatKeydown = (e: Event | KeyboardEvent) => {
  if ((e as KeyboardEvent).key === 'Enter' && !(e as KeyboardEvent).shiftKey) {
    e.preventDefault()
    sendAiChatMessage()
  }
}

const scrollToAiChatBottom = () => {
  nextTick(() => {
    if (aiChatContainerRef.value) {
      aiChatContainerRef.value.scrollTop = aiChatContainerRef.value.scrollHeight
    }
  })
}

const toggleAiChat = () => {
  aiChatVisible.value = !aiChatVisible.value
  if (aiChatVisible.value && !aiChatInitialized) {
    aiChatInitialized = true
    nextTick(() => {
      sendAiChatMessage('请简要介绍一下这首诗', true)
    })
  }
}

const hasContent = (fields: string[]): boolean => {
  if (!externalPoemDetail.value) return false
  return fields.some(f => {
    const val = externalPoemDetail.value?.[f]
    return val && typeof val === 'string' && val.trim().length > 0
  })
}

const getContent = (fields: string[]): string => {
  if (!externalPoemDetail.value) return ''
  return fields
    .map(f => externalPoemDetail.value?.[f])
    .filter(v => v && typeof v === 'string' && v.trim().length > 0)
    .join('\n\n')
}

const parsePoemContent = (content: string): string[] => {
  if (!content) return []
  const cleaned = content
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n')
    .replace(/<\/div>/gi, '\n')
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&emsp;/g, '　')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')

  const lines = cleaned.split(/\n/).filter(line => line.trim())
  const sentences: string[] = []
  for (const line of lines) {
    const parts = line.split(/(?<=[。！？；!?;])/).filter(s => s.trim())
    if (parts.length === 0 && line.trim()) {
      sentences.push(line.trim())
    } else {
      sentences.push(...parts.map(s => s.trim()).filter(Boolean))
    }
  }
  return sentences
}

const poemSentences = computed(() => {
  if (!externalPoemDetail.value?.content) return []
  return parsePoemContent(externalPoemDetail.value.content)
})

const fetchFilters = async () => {
  try {
    const cachedDynasties = getCache<Dynasty[]>('dynasties')
    const cachedCategories = getCache<Category[]>('categories')
    const cachedPoets = getCache<Poet[]>('poets')

    if (cachedDynasties && cachedCategories && cachedPoets) {
      dynasties.value = cachedDynasties
      categories.value = cachedCategories
      poets.value = cachedPoets
      return
    }

    const [dynastyRes, categoryRes, poetRes] = await Promise.all([
      getDynastyList(),
      getCategoryList(),
      getPoetList({ pageSize: 200 })
    ])
    dynasties.value = dynastyRes.data
    categories.value = categoryRes.data
    poets.value = poetRes.data.list

    setCache('dynasties', dynastyRes.data, 60 * 60 * 1000)
    setCache('categories', categoryRes.data, 60 * 60 * 1000)
    setCache('poets', poetRes.data.list, 60 * 60 * 1000)
  } catch (error) {
    ElMessage.error('获取筛选条件失败')
  }
}

const handleTabChange = (tab: string | number | boolean | undefined) => {
  activeTab.value = tab as 'classical' | 'modern' | 'foreign'
  currentPage.value = 1
  syncFiltersToUrl()
  fetchPoems()
}

const handleFilterChange = () => {
  currentPage.value = 1
  syncFiltersToUrl()
  fetchPoems()
}

const clearFilters = () => {
  if (activeTab.value === 'classical') {
    filters.value = {
      dynastyId: undefined,
      categoryId: undefined,
      poetId: undefined,
      keyword: '',
      sortBy: 'latest'
    }
  } else if (activeTab.value === 'modern') {
    modernFilters.value = {
      isOriginal: undefined,
      hasCertifiedPoet: undefined,
      sortBy: 'latest'
    }
  } else {
    foreignFilters.value = {
      keyword: '',
      author: ''
    }
  }
  currentPage.value = 1
  syncFiltersToUrl()
  fetchPoems()
}

const syncFiltersToUrl = () => {
  const query: Record<string, string> = {}
  if (activeTab.value === 'modern') {
    query.tab = 'modern'
  } else if (activeTab.value === 'foreign') {
    query.tab = 'foreign'
  }
  if (activeTab.value === 'classical') {
    if (filters.value.dynastyId) query.dynastyId = String(filters.value.dynastyId)
    if (filters.value.categoryId) query.categoryId = String(filters.value.categoryId)
    if (filters.value.poetId) query.poetId = String(filters.value.poetId)
    if (filters.value.keyword) query.keyword = filters.value.keyword
    if (filters.value.sortBy !== 'latest') query.sortBy = filters.value.sortBy
  } else if (activeTab.value === 'modern') {
    if (modernFilters.value.isOriginal !== undefined) query.isOriginal = String(modernFilters.value.isOriginal)
    if (modernFilters.value.hasCertifiedPoet !== undefined) query.hasCertifiedPoet = String(modernFilters.value.hasCertifiedPoet)
    if (modernFilters.value.sortBy !== 'latest') query.sortBy = modernFilters.value.sortBy
  } else {
    if (foreignFilters.value.keyword) query.keyword = foreignFilters.value.keyword
    if (foreignFilters.value.author) query.author = foreignFilters.value.author
  }
  if (currentPage.value > 1) query.page = String(currentPage.value)
  if (selectedExternalPoem.value) query.poemTitle = selectedExternalPoem.value.title
  router.replace({ query })
}

const hasActiveFilters = computed(() => {
  if (activeTab.value === 'classical') {
    return filters.value.dynastyId || filters.value.categoryId || filters.value.poetId || filters.value.keyword || filters.value.sortBy !== 'latest'
  }
  if (activeTab.value === 'modern') {
    return modernFilters.value.isOriginal !== undefined || modernFilters.value.hasCertifiedPoet !== undefined || modernFilters.value.sortBy !== 'latest'
  }
  return foreignFilters.value.keyword || foreignFilters.value.author
})

const handlePageChange = (page: number) => {
  currentPage.value = page
  syncFiltersToUrl()
  fetchPoems()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  preferencesStore.setPageSize(size)
  fetchPoems()
}

const goToDetail = (id: number | string) => {
  if (typeof id === 'string' && id.startsWith('foreign_')) {
    // 外文诗词，打开详情面板
    const poem = foreignPoemList.value.find(p => p.id === id)
    if (poem) {
      openForeignPoemDetail(poem)
    }
  } else {
    router.push(`/poem/${id}`)
  }
}

const showForeignOriginal = ref(false)
const foreignOriginalLines = ref<string[]>([])
const foreignTranslating = ref(false)

const translateEnglishPoem = async (poem: any): Promise<string> => {
  const englishText = poem.originalLines?.join('\n') || poem.content || ''
  const prompt = `请将以下英文诗歌翻译成中文，要求译文优美、保留诗意和韵律感，仅返回中文译文即可，不要添加任何额外说明。\n\n原文标题：${poem.title}\n作者：${poem.author}\n\n原文：\n${englishText}`
  try {
    const res = await chat({ message: prompt })
    return res.data.reply || ''
  } catch (e) {
    console.error('AI翻译失败:', e)
    return ''
  }
}

const openForeignPoemDetail = async (poem: any) => {
  externalDetailLoading.value = true
  selectedExternalPoem.value = {
    title: poem.title,
    content: poem.content,
    author: poem.author,
    source: 'external'
  }

  // 优先通过接口盒子API搜索中文版
  let chineseText = poem.chineseContent || ''
  if (!chineseText) {
    try {
      const apiResult = await getApihzPoetryDetail(poem.title)
      if (apiResult && apiResult.content) {
        chineseText = apiResult.content
      }
    } catch (e) {
      // 接口盒子没有结果，走AI翻译
    }
  }

  // 接口盒子没有找到，用AI翻译
  if (!chineseText) {
    foreignTranslating.value = true
    chineseText = await translateEnglishPoem(poem)
    foreignTranslating.value = false
  }

  // 保存中文翻译到诗歌对象
  if (chineseText) {
    poem.chineseContent = chineseText
    poem.content = chineseText
  }

  externalPoemDetail.value = {
    title: poem.title,
    content: chineseText || poem.content,
    author: poem.author
  }
  externalDetailLoading.value = false
  externalActiveTab.value = 'translation'
  showEnlargedContent.value = false
  showForeignOriginal.value = false
  foreignOriginalLines.value = poem.originalLines || []
}

const handleLike = async (poem: any, event: Event) => {
  event.stopPropagation()
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再点赞')
    return
  }
  try {
    await likePoem(poem.id)
    poem.isLiked = !poem.isLiked
    poem.likeCount = poem.isLiked ? poem.likeCount + 1 : poem.likeCount - 1
    ElMessage.success(poem.isLiked ? '点赞成功' : '已取消点赞')
  } catch (error) {
    ElMessage.error('操作失败，请稍后重试')
  }
}

const handleFavorite = async (poem: any, event: Event) => {
  event.stopPropagation()
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再收藏')
    return
  }
  try {
    await favoritePoem(poem.id)
    poem.isFavorited = !poem.isFavorited
    poem.favoriteCount = poem.isFavorited ? poem.favoriteCount + 1 : poem.favoriteCount - 1
    ElMessage.success(poem.isFavorited ? '收藏成功' : '已取消收藏')
  } catch (error) {
    ElMessage.error('操作失败，请稍后重试')
  }
}

const getFormattedVerses = (content: string) => {
  if (!content) return []
  const lines = content.split(/[。！？；\n]+/).filter(line => line.trim())
  return lines.slice(0, 3).map(line => line.trim())
}

onMounted(() => {
  const query = route.query
  if (query.tab === 'modern') {
    activeTab.value = 'modern'
  } else if (query.tab === 'foreign') {
    activeTab.value = 'foreign'
  }
  if (query.dynastyId) {
    filters.value.dynastyId = Number(query.dynastyId)
  }
  if (query.categoryId) {
    filters.value.categoryId = Number(query.categoryId)
  }
  if (query.poetId) {
    filters.value.poetId = Number(query.poetId)
  }
  if (query.keyword) {
    if (activeTab.value === 'foreign') {
      foreignFilters.value.keyword = String(query.keyword)
    } else {
      filters.value.keyword = String(query.keyword)
    }
  }
  if (query.author) {
    foreignFilters.value.author = String(query.author)
  }
  if (query.page) {
    const page = Number(query.page)
    if (!isNaN(page) && page > 0) {
      currentPage.value = page
    }
  }
  if (query.sortBy) {
    const sortBy = String(query.sortBy) as 'latest' | 'popular' | 'likes'
    if (activeTab.value === 'classical') {
      filters.value.sortBy = sortBy
    } else {
      modernFilters.value.sortBy = sortBy
    }
  }
  if (query.isOriginal !== undefined) {
    modernFilters.value.isOriginal = query.isOriginal === 'true'
  }
  if (query.hasCertifiedPoet !== undefined) {
    modernFilters.value.hasCertifiedPoet = query.hasCertifiedPoet === 'true'
  }
  fetchFilters()
  fetchPoems()
  initSearchData()

  window.addEventListener('scroll', handleSearchScroll, true)
  window.addEventListener('resize', handleSearchScroll)

  if (query.poemTitle) {
    const poemTitle = String(query.poemTitle)
    const cachedPoem = getCache<PoemSearchResult>(`external_poem_${poemTitle}`)
    if (cachedPoem) {
      openPoemDetail(cachedPoem)
    }
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleSearchScroll, true)
  window.removeEventListener('resize', handleSearchScroll)
})

const initSearchData = async () => {
  try {
    const [hot, history] = await Promise.all([
      getHotSearches(8),
      getSearchHistory(10)
    ])
    hotSearches.value = hot
    searchHistoryList.value = history
  } catch (error) {
    console.error('初始化搜索数据失败:', error)
  }
}

const handleSearchInput = async (value: string) => {
  if (value.trim()) {
    try {
      searchSuggestions.value = await getSearchSuggestions(value.trim(), 10)
    } catch {
      searchSuggestions.value = []
    }
  } else {
    searchSuggestions.value = []
  }
}

let searchScrollTimer: ReturnType<typeof setTimeout> | null = null
const handleSearchScroll = () => {
  if (showSearchPanel.value) {
    if (searchScrollTimer) clearTimeout(searchScrollTimer)
    searchScrollTimer = setTimeout(() => updateSearchPanelPosition(), 100)
  }
}

// 搜索面板与输入框的间距（px）
const SEARCH_PANEL_GAP = 4

const handleSearchFocus = () => {
  updateSearchPanelPosition()
  showSearchPanel.value = true
}

const handleSearchBlur = () => {
  setTimeout(() => {
    showSearchPanel.value = false
  }, 200)
}

const searchPanelStyle = ref<Record<string, string>>({})
const searchInputRef = ref<HTMLElement | null>(null)

const updateSearchPanelPosition = () => {
  if (!searchInputRef.value) return
  const rect = searchInputRef.value.getBoundingClientRect()
  searchPanelStyle.value = {
    position: 'fixed',
    top: (rect.bottom + SEARCH_PANEL_GAP) + 'px',
    left: rect.left + 'px',
    width: rect.width + 'px'
  }
}

const selectSuggestion = (suggestion: string) => {
  filters.value.keyword = suggestion
  showSearchPanel.value = false
  handleFilterChange()
}

const selectHotSearch = (keyword: string) => {
  filters.value.keyword = keyword
  showSearchPanel.value = false
  handleFilterChange()
}

const selectHistory = (keyword: string) => {
  filters.value.keyword = keyword
  showSearchPanel.value = false
  handleFilterChange()
}

const handleClearHistory = async () => {
  try {
    await clearSearchHistory()
    searchHistoryList.value = []
    ElMessage.success('搜索历史已清除')
  } catch {
    ElMessage.error('清除失败')
  }
}
</script>

<template>
  <div class="poem-list-page">
    <canvas ref="particleCanvasRef" class="particle-bg"></canvas>
    <div class="container">
      <div class="page-nav">
        <el-button text @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-divider direction="vertical" />
        <el-button text @click="router.push('/')">
          <el-icon><HomeFilled /></el-icon>
          首页
        </el-button>
        <el-divider direction="vertical" />
        <span class="current-page">诗词鉴赏</span>
      </div>

      <div class="header-banner">
        <canvas ref="bannerCanvasRef" class="banner-particles"></canvas>
        <div class="page-header">
          <h1 class="page-title">诗词鉴赏</h1>
          <p class="page-subtitle">品读千年韵律，感悟诗词之美</p>
          <div class="header-actions">
            <el-button @click="router.push('/poet')">
              <el-icon><User /></el-icon>
              诗人风采
            </el-button>
            <el-button
              v-if="userStore.isLoggedIn"
              type="primary"
              @click="router.push('/poem/create')"
            >
              <el-icon><Edit /></el-icon>
              发布新诗
            </el-button>
          </div>
        </div>

        <div class="poem-tabs">
          <el-radio-group v-model="activeTab" @change="handleTabChange">
            <el-radio-button value="classical">古籍诗文</el-radio-button>
            <el-radio-button value="modern">现代创作</el-radio-button>
            <el-radio-button value="foreign">外文诗词</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div class="filter-section" v-if="activeTab === 'classical'">
        <el-card class="filter-card">
          <div class="filter-row">
            <div class="filter-item">
              <label>朝代：</label>
              <el-select
                v-model="filters.dynastyId"
                placeholder="全部朝代"
                clearable
                @change="handleFilterChange"
              >
                <el-option
                  v-for="dynasty in dynasties"
                  :key="dynasty.id"
                  :label="dynasty.name"
                  :value="dynasty.id"
                />
              </el-select>
            </div>

            <div class="filter-item">
              <label>分类：</label>
              <el-select
                v-model="filters.categoryId"
                placeholder="全部分类"
                clearable
                @change="handleFilterChange"
              >
                <el-option
                  v-for="category in categories"
                  :key="category.id"
                  :label="category.name"
                  :value="category.id"
                />
              </el-select>
            </div>

            <div class="filter-item">
              <label>诗人：</label>
              <el-select
                v-model="filters.poetId"
                placeholder="全部诗人"
                clearable
                filterable
                @change="handleFilterChange"
              >
                <el-option
                  v-for="poet in poets"
                  :key="poet.id"
                  :label="poet.name"
                  :value="poet.id"
                />
              </el-select>
            </div>

            <div class="filter-item">
              <label>排序：</label>
              <el-select
                v-model="filters.sortBy"
                @change="handleFilterChange"
              >
                <el-option
                  v-for="option in sortOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </div>

            <div class="filter-item search-item">
              <label>搜索：</label>
              <!-- 搜索面板定位参照：使用 div 而非 el-input，以便获取完整容器尺寸 -->
              <div class="search-input-wrapper" ref="searchInputRef">
                <el-input
                  v-model="filters.keyword"
                  placeholder="搜索诗词..."
                  clearable
                  @keyup.enter="handleFilterChange"
                  @input="handleSearchInput"
                  @focus="handleSearchFocus"
                  @blur="handleSearchBlur"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                
                <div v-if="showSearchPanel && (searchSuggestions.length > 0 || hotSearches.length > 0 || searchHistoryList.length > 0)" class="search-panel" :style="searchPanelStyle">
                  <div v-if="searchSuggestions.length > 0" class="panel-section">
                    <div class="panel-title">搜索建议</div>
                    <div v-for="item in searchSuggestions" :key="item" class="panel-item" @mousedown.prevent="selectSuggestion(item)">
                      <el-icon><Search /></el-icon>
                      <span>{{ item }}</span>
                    </div>
                  </div>
                  
                  <div v-else-if="searchHistoryList.length > 0" class="panel-section">
                    <div class="panel-title">
                      <span>搜索历史</span>
                      <el-button text size="small" @mousedown.prevent="handleClearHistory">清除</el-button>
                    </div>
                    <div v-for="item in searchHistoryList" :key="item" class="panel-item" @mousedown.prevent="selectHistory(item)">
                      <el-icon><Clock /></el-icon>
                      <span>{{ item }}</span>
                    </div>
                  </div>
                  
                  <div v-if="hotSearches.length > 0" class="panel-section">
                    <div class="panel-title">热门搜索</div>
                    <div class="hot-tags">
                      <el-tag v-for="item in hotSearches" :key="item" class="hot-tag" @mousedown.prevent="selectHotSearch(item)">
                        {{ item }}
                      </el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <el-button type="primary" @click="handleFilterChange">
              搜索
            </el-button>

            <el-button v-if="hasActiveFilters" @click="clearFilters">
              清除筛选
            </el-button>
          </div>
        </el-card>
      </div>

      <div class="filter-section" v-else-if="activeTab === 'modern'">
        <el-card class="filter-card">
          <div class="filter-row">
            <div class="filter-item">
              <label>类型：</label>
              <el-select
                v-model="modernFilters.isOriginal"
                placeholder="全部作品"
                clearable
                @change="handleFilterChange"
              >
                <el-option label="全部作品" :value="''" />
                <el-option label="原创作品" :value="true" />
                <el-option label="非原创" :value="false" />
              </el-select>
            </div>

            <div class="filter-item">
              <label>诗人：</label>
              <el-select
                v-model="modernFilters.hasCertifiedPoet"
                placeholder="全部诗人"
                clearable
                @change="handleFilterChange"
              >
                <el-option label="全部诗人" :value="''" />
                <el-option label="认证诗人" :value="true" />
              </el-select>
            </div>

            <div class="filter-item">
              <label>排序：</label>
              <el-select
                v-model="modernFilters.sortBy"
                @change="handleFilterChange"
              >
                <el-option
                  v-for="option in sortOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </div>

            <el-button v-if="hasActiveFilters" @click="clearFilters">
              清除筛选
            </el-button>
          </div>
        </el-card>
      </div>

      <div class="filter-section" v-else-if="activeTab === 'foreign'">
        <el-card class="filter-card">
          <div class="filter-row">
            <div class="filter-item search-item">
              <label>标题搜索：</label>
              <el-input
                v-model="foreignFilters.keyword"
                placeholder="搜索英文诗词标题..."
                clearable
                @keyup.enter="handleFilterChange"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>

            <div class="filter-item search-item">
              <label>作者搜索：</label>
              <el-input
                v-model="foreignFilters.author"
                placeholder="搜索作者..."
                clearable
                @keyup.enter="handleFilterChange"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>

            <el-button type="primary" @click="handleFilterChange">
              搜索
            </el-button>

            <el-button @click="fetchForeignPoems">
              随机获取
            </el-button>

            <el-button v-if="hasActiveFilters" @click="clearFilters">
              清除筛选
            </el-button>
          </div>
        </el-card>
      </div>

      <div class="result-stats" v-if="total > 0 && !selectedExternalPoem">
        <span v-if="activeTab === 'foreign'">共找到 <strong>{{ total }}</strong> 首外文诗词</span>
        <span v-else>共找到 <strong>{{ total }}</strong> 首诗词</span>
        <el-tag v-if="searchLevel === 'fuzzy'" type="warning" size="small" class="search-level-tag">模糊匹配</el-tag>
        <el-tag v-else-if="searchLevel === 'pinyin'" type="info" size="small" class="search-level-tag">拼音匹配</el-tag>
      </div>

      <div class="poem-grid" v-loading="loading || foreignLoading" v-if="(loading || foreignLoading || poems.length > 0 || (!externalSearching && externalPoemResults.length === 0)) && !selectedExternalPoem">
        <el-empty v-if="!loading && !foreignLoading && !externalSearching && poems.length === 0 && externalPoemResults.length === 0" description="暂无诗词" />

        <div
          v-for="(poem, index) in poems"
          :key="poem.id"
          class="poem-card"
          @click="goToDetail(poem.id)"
        >
          <div class="poem-image">
            <img :src="poemImages[index % poemImages.length]" :alt="poem.title" />
            <div class="featured-badge" v-if="poem.isFeatured">
              <el-icon><Star /></el-icon>
              精选
            </div>
            <div class="original-badge" v-if="poem.isOriginal">
              <el-icon><EditPen /></el-icon>
              原创
            </div>
            <div class="foreign-badge" v-if="poem.source === 'foreign'">
              <el-icon><Reading /></el-icon>
              外文
            </div>
          </div>
          <div class="poem-info">
            <div class="poem-header">
              <h3 class="poem-title">{{ poem.title }}</h3>
              <div class="poem-tags">
                <span class="tag dynasty-tag" v-if="poem.dynastyName">{{ poem.dynastyName }}</span>
                <span class="tag category-tag" v-if="poem.categoryName">{{ poem.categoryName }}</span>
                <span class="tag original-tag" v-if="poem.isOriginal">原创</span>
              </div>
            </div>
            <p class="poem-author">
              <span v-if="poem.poetName">{{ poem.poetName }}</span>
              <span v-else-if="poem.author">{{ poem.author }}</span>
            </p>
            <div class="poem-verses">
              <p v-for="(line, index) in getFormattedVerses(poem.chineseContent || poem.content)" :key="index" class="verse-line">
                {{ line }}
              </p>
            </div>
            <div class="poem-footer">
              <div class="poem-meta">
                <span class="meta-item">
                  <el-icon><View /></el-icon>
                  {{ poem.viewCount }}
                </span>
                <span class="meta-item">
                  <el-icon><ChatDotRound /></el-icon>
                  {{ poem.likeCount }}
                </span>
                <span class="meta-item rating-meta" v-if="poem.avgScore">
                  <el-icon><Trophy /></el-icon>
                  {{ poem.avgScore.toFixed(1) }}
                </span>
              </div>
              <div class="poem-actions">
                <el-button
                  class="action-btn like-btn"
                  :class="{ 'is-liked': poem.isLiked }"
                  :icon="poem.isLiked ? 'StarFilled' : 'Star'"
                  @click="handleLike(poem, $event)"
                  link
                >
                  {{ poem.isLiked ? '已赞' : '点赞' }}
                </el-button>
                <el-button
                  class="action-btn favorite-btn"
                  :class="{ 'is-favorited': poem.isFavorited }"
                  :icon="poem.isFavorited ? 'FolderChecked' : 'Folder'"
                  @click="handleFavorite(poem, $event)"
                  link
                >
                  {{ poem.isFavorited ? '已藏' : '收藏' }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-section" v-if="total > 0 && !selectedExternalPoem">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>

      <div class="external-results-section" v-if="externalSearching || externalPoemResults.length > 0 || selectedExternalPoem">
        <div class="section-header">
          <div class="section-title-area">
            <el-button v-if="poemHistory.length > 0" text class="back-btn" @click="goBackInHistory">
              <el-icon><ArrowLeft /></el-icon>
              返回上一篇
            </el-button>
            <el-button v-if="selectedExternalPoem && externalPoemResults.length === 0" text class="back-btn" @click="selectedExternalPoem = null; externalPoemDetail = null">
              <el-icon><ArrowLeft /></el-icon>
              返回诗词列表
            </el-button>
            <h1 class="section-title">寻章摘句</h1>
            <p class="section-subtitle">品读千年韵律，感悟诗词之美</p>
          </div>
          <div class="poem-history-bar" v-if="poemHistory.length > 0">
            <span class="history-label">浏览历史：</span>
            <div class="history-items">
              <span
                v-for="(item, idx) in poemHistory.slice(-5)"
                :key="idx"
                :class="['history-item', { 'is-current': selectedExternalPoem?.title === item.title }]"
                @click="openPoemDetail(item)"
              >
                {{ item.title }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="externalSearching" class="external-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          正在搜索古诗词库...
        </div>

        <div v-else class="external-content-layout">
          <div class="external-poem-selector">
            <div
              v-for="(poem, idx) in externalPoemResults"
              :key="'ext-' + idx"
              :class="['poem-select-item', { 'is-active': selectedExternalPoem?.title === poem.title }]"
              @click="openPoemDetail(poem)"
            >
              <span class="poem-select-title">{{ poem.title }}</span>
              <span class="poem-select-author" v-if="poem.author">{{ poem.author }}</span>
            </div>
          </div>

          <div class="external-main-content">
            <div class="poem-detail-layout" v-if="selectedExternalPoem" v-loading="externalDetailLoading">
              <template v-if="externalPoemDetail">
                <div class="poem-left-panel">
                <div class="poem-header-section">
                  <div class="poem-header-info">
                    <h2 class="poem-main-title">{{ selectedExternalPoem.title }}</h2>
                    <div class="poem-meta">
                      <span
                        v-if="selectedExternalPoem.author"
                        class="author-link"
                        @click="toggleAuthorInfo"
                      >
                        {{ selectedExternalPoem.author }}
                        <el-icon class="expand-icon" :class="{ 'is-expanded': authorInfoExpanded }">
                          <ArrowDown />
                        </el-icon>
                      </span>
                      <span v-if="selectedExternalPoem.dynasty">【{{ selectedExternalPoem.dynasty }}】</span>
                    </div>
                    <Transition name="expand">
                      <div v-if="authorInfoExpanded" class="author-info-expand">
                        <div v-if="authorInfoLoading" class="author-info-loading">
                          <el-icon class="is-loading"><Loading /></el-icon>
                          正在加载诗人介绍...
                        </div>
                        <div v-else class="author-info-content">{{ authorInfoContent }}</div>
                      </div>
                    </Transition>
                  </div>

                  <div class="action-toolbar">
                    <el-button
                      @click="generateAnnotation"
                      :loading="annotationLoading"
                      :class="['toolbar-btn', { 'is-active': activePanel === 'annotation' }]"
                    >
                      <el-icon><EditPen /></el-icon>
                      <span>译文注释</span>
                    </el-button>
                    <el-button
                      @click="generateAppreciation"
                      :loading="appreciationLoading"
                      :class="['toolbar-btn', { 'is-active': activePanel === 'appreciation' }]"
                    >
                      <el-icon><Star /></el-icon>
                      <span>名人点评</span>
                    </el-button>
                    <el-button
                      @click="generateBackground"
                      :loading="backgroundLoading"
                      :class="['toolbar-btn', { 'is-active': activePanel === 'background' }]"
                    >
                      <el-icon><Document /></el-icon>
                      <span>创作背景</span>
                    </el-button>
                    <el-button
                      v-if="foreignOriginalLines.length > 0"
                      @click="showForeignOriginal = !showForeignOriginal"
                      :class="['toolbar-btn', { 'is-active': showForeignOriginal }]"
                    >
                      <el-icon><Document /></el-icon>
                      <span>{{ showForeignOriginal ? '隐藏原文' : '外文原文' }}</span>
                    </el-button>
                  </div>

                  <Transition name="panel-expand-left">
                    <div class="poem-flyout-panel" v-if="activePanel">
                      <div class="flyout-connector"></div>
                      <div class="flyout-inner">
                        <div class="flyout-header">
                          <h3 v-if="activePanel === 'annotation'">译文注释</h3>
                          <h3 v-else-if="activePanel === 'appreciation'">名人点评</h3>
                          <h3 v-else-if="activePanel === 'background'">创作背景</h3>
                          <el-button text @click="activePanel = null">
                            <el-icon><Close /></el-icon>
                          </el-button>
                        </div>
                        <div class="flyout-content">
                          <div v-if="activePanel === 'annotation'">
                            <div v-if="annotationLoading" class="panel-loading">
                              <div class="search-loading-anim">
                                <el-icon><Search /></el-icon>
                                <span class="search-dot"></span>
                                <span class="search-dot"></span>
                                <span class="search-dot"></span>
                              </div>
                              <span>搜索中</span>
                            </div>
                            <div v-else-if="annotationContent">
                              <el-button text size="small" @click="copyContent(annotationContent)" class="copy-btn copy-btn-top">
                                <el-icon><CopyDocument /></el-icon> 复制
                              </el-button>
                              <div v-for="(para, idx) in formatContent(annotationContent)" :key="idx" class="content-paragraph">
                                <p>{{ para }}</p>
                              </div>
                            </div>
                          </div>
                          <div v-else-if="activePanel === 'appreciation'">
                            <div v-if="appreciationLoading" class="panel-loading">
                              <div class="search-loading-anim">
                                <el-icon><Search /></el-icon>
                                <span class="search-dot"></span>
                                <span class="search-dot"></span>
                                <span class="search-dot"></span>
                              </div>
                              <span>搜索中</span>
                            </div>
                            <div v-else-if="appreciationContent">
                              <el-button text size="small" @click="copyContent(appreciationContent)" class="copy-btn copy-btn-top">
                                <el-icon><CopyDocument /></el-icon> 复制
                              </el-button>
                              <div v-for="(para, idx) in formatContent(appreciationContent)" :key="idx" class="content-paragraph">
                                <p>{{ para }}</p>
                              </div>
                            </div>
                          </div>
                          <div v-else-if="activePanel === 'background'">
                            <div v-if="backgroundLoading" class="panel-loading">
                              <div class="search-loading-anim">
                                <el-icon><Search /></el-icon>
                                <span class="search-dot"></span>
                                <span class="search-dot"></span>
                                <span class="search-dot"></span>
                              </div>
                              <span>搜索中</span>
                            </div>
                            <div v-else-if="backgroundContent">
                              <el-button text size="small" @click="copyContent(backgroundContent)" class="copy-btn copy-btn-top">
                                <el-icon><CopyDocument /></el-icon> 复制
                              </el-button>
                              <p style="white-space: pre-line;">{{ backgroundContent }}</p>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </Transition>
                </div>

                <div class="poem-body-layout">
                  <div class="poem-content-section">
                    <Transition name="enlarged-content">
                      <div v-if="showEnlargedContent && activePanel" class="enlarged-content-panel">
                        <div class="enlarged-header">
                          <h3 v-if="activePanel === 'annotation'">译文注释</h3>
                          <h3 v-else-if="activePanel === 'appreciation'">名人点评</h3>
                          <h3 v-else-if="activePanel === 'background'">创作背景</h3>
                          <el-button text @click="showEnlargedContent = false">
                            <el-icon><Close /></el-icon>
                          </el-button>
                        </div>
                        <div class="enlarged-body">
                          <div v-if="activePanel === 'annotation'">
                            <div v-if="annotationLoading" class="enlarged-loading">
                              <el-icon class="is-loading"><Loading /></el-icon>
                              正在生成译文注释...
                            </div>
                            <div v-else-if="annotationContent" class="enlarged-text">
                              <div v-for="(para, idx) in formatContent(annotationContent)" :key="idx" class="enlarged-paragraph">
                                {{ para }}
                              </div>
                            </div>
                          </div>
                          <div v-else-if="activePanel === 'appreciation'">
                            <div v-if="appreciationLoading" class="enlarged-loading">
                              <el-icon class="is-loading"><Loading /></el-icon>
                              正在生成名人点评...
                            </div>
                            <div v-else-if="appreciationContent" class="enlarged-text">
                              <div v-for="(para, idx) in formatContent(appreciationContent)" :key="idx" class="enlarged-paragraph">
                                {{ para }}
                              </div>
                            </div>
                          </div>
                          <div v-else-if="activePanel === 'background'">
                            <div v-if="backgroundLoading" class="enlarged-loading">
                              <el-icon class="is-loading"><Loading /></el-icon>
                              正在生成创作背景...
                            </div>
                            <div v-else-if="backgroundContent" class="enlarged-text">
                              {{ backgroundContent }}
                            </div>
                          </div>
                        </div>
                      </div>
                    </Transition>
                    <div class="poem-verses-panel">
                      <template v-for="(sentence, idx) in poemSentences" :key="idx">
                        <div
                          :class="['verse-line-item', { 'is-explained': explainedLineIdxs.has(idx) }]"
                          @mouseenter="hoveredLineIdx = idx"
                          @mouseleave="hoveredLineIdx = null"
                          @click="explainLine(idx)"
                        >
                          <span class="verse-number">{{ idx + 1 }}</span>
                          <span class="verse-text">{{ sentence }}</span>
                          <el-icon class="verse-explain-icon" v-show="hoveredLineIdx === idx">
                            <MagicStick />
                          </el-icon>
                        </div>
                        <Transition name="expand-line">
                          <div class="ai-line-explain" v-if="explainedLineIdxs.has(idx)">
                            <div class="ai-explain-header">
                              <el-icon><MagicStick /></el-icon>
                              <span>AI解析</span>
                              <el-icon class="ai-explain-close" @click.stop="closeLineExplanation(idx)">
                                <Close />
                              </el-icon>
                            </div>
                            <div v-if="lineExplaining.has(idx)" class="ai-explain-loading">
                              <div class="search-loading-anim">
                                <el-icon><Search /></el-icon>
                                <span class="search-dot"></span>
                                <span class="search-dot"></span>
                                <span class="search-dot"></span>
                              </div>
                              <span>搜索中</span>
                            </div>
                            <div v-else class="ai-explain-content">{{ lineExplanations.get(idx) }}</div>
                          </div>
                        </Transition>
                      </template>
                    </div>
                    <Transition name="expand-line">
                      <div v-if="showForeignOriginal && foreignOriginalLines.length > 0" class="foreign-original-panel">
                        <div class="foreign-original-header">
                          <el-icon><Document /></el-icon>
                          <span>外文原文</span>
                        </div>
                        <div class="foreign-original-body">
                          <div v-for="(line, idx) in foreignOriginalLines" :key="idx" class="foreign-original-line">
                            <span class="foreign-line-number">{{ idx + 1 }}</span>
                            <span class="foreign-line-text">{{ line }}</span>
                          </div>
                        </div>
                      </div>
                    </Transition>
                  </div>
                </div>
                </div>

                <div class="poem-right-panel">
                  <div class="background-knowledge-card">
                    <h4 class="info-card-title">背景知识</h4>
                    <div class="knowledge-panels">
                      <div class="knowledge-panel">
                        <div class="knowledge-panel-header" @click="toggleKnowledgePanel('dynastyPoetry')">
                          <span>朝代诗歌特点</span>
                          <el-icon :class="{ 'is-expanded': activeKnowledgePanels.includes('dynastyPoetry') }">
                            <ArrowRight />
                          </el-icon>
                        </div>
                        <Transition name="slide-left">
                          <div v-if="activeKnowledgePanels.includes('dynastyPoetry')" class="knowledge-panel-content">
                            <div v-if="backgroundKnowledge.dynastyPoetry.loading" class="knowledge-loading">
                              <el-icon class="is-loading"><Loading /></el-icon>
                              正在加载...
                            </div>
                            <div v-else class="knowledge-text">
                              <div v-if="isKnowledgeExpanded('dynastyPoetry')">
                                {{ backgroundKnowledge.dynastyPoetry.content }}
                              </div>
                              <div v-else>
                                {{ getKnowledgeSummary(backgroundKnowledge.dynastyPoetry.content) }}
                              </div>
                              <el-button 
                                v-if="backgroundKnowledge.dynastyPoetry.content && backgroundKnowledge.dynastyPoetry.content.length > 100"
                                text 
                                size="small" 
                                class="knowledge-expand-btn"
                                @click.stop="toggleKnowledgeExpand('dynastyPoetry')"
                              >
                                {{ isKnowledgeExpanded('dynastyPoetry') ? '收起' : '更多' }}
                              </el-button>
                            </div>
                          </div>
                        </Transition>
                      </div>

                      <div class="knowledge-panel">
                        <div class="knowledge-panel-header" @click="toggleKnowledgePanel('literarySchool')">
                          <span>相关文学流派</span>
                          <el-icon :class="{ 'is-expanded': activeKnowledgePanels.includes('literarySchool') }">
                            <ArrowRight />
                          </el-icon>
                        </div>
                        <Transition name="slide-left">
                          <div v-if="activeKnowledgePanels.includes('literarySchool')" class="knowledge-panel-content">
                            <div v-if="backgroundKnowledge.literarySchool.loading" class="knowledge-loading">
                              <el-icon class="is-loading"><Loading /></el-icon>
                              正在加载...
                            </div>
                            <div v-else class="knowledge-text">
                              <div v-if="isKnowledgeExpanded('literarySchool')">
                                {{ backgroundKnowledge.literarySchool.content }}
                              </div>
                              <div v-else>
                                {{ getKnowledgeSummary(backgroundKnowledge.literarySchool.content) }}
                              </div>
                              <el-button 
                                v-if="backgroundKnowledge.literarySchool.content && backgroundKnowledge.literarySchool.content.length > 100"
                                text 
                                size="small" 
                                class="knowledge-expand-btn"
                                @click.stop="toggleKnowledgeExpand('literarySchool')"
                              >
                                {{ isKnowledgeExpanded('literarySchool') ? '收起' : '更多' }}
                              </el-button>
                            </div>
                          </div>
                        </Transition>
                      </div>

                      <div class="knowledge-panel">
                        <div class="knowledge-panel-header" @click="toggleKnowledgePanel('historicalContext')">
                          <span>历史文化背景</span>
                          <el-icon :class="{ 'is-expanded': activeKnowledgePanels.includes('historicalContext') }">
                            <ArrowRight />
                          </el-icon>
                        </div>
                        <Transition name="slide-left">
                          <div v-if="activeKnowledgePanels.includes('historicalContext')" class="knowledge-panel-content">
                            <div v-if="backgroundKnowledge.historicalContext.loading" class="knowledge-loading">
                              <el-icon class="is-loading"><Loading /></el-icon>
                              正在加载...
                            </div>
                            <div v-else class="knowledge-text">
                              <div v-if="isKnowledgeExpanded('historicalContext')">
                                {{ backgroundKnowledge.historicalContext.content }}
                              </div>
                              <div v-else>
                                {{ getKnowledgeSummary(backgroundKnowledge.historicalContext.content) }}
                              </div>
                              <el-button 
                                v-if="backgroundKnowledge.historicalContext.content && backgroundKnowledge.historicalContext.content.length > 100"
                                text 
                                size="small" 
                                class="knowledge-expand-btn"
                                @click.stop="toggleKnowledgeExpand('historicalContext')"
                              >
                                {{ isKnowledgeExpanded('historicalContext') ? '收起' : '更多' }}
                              </el-button>
                            </div>
                          </div>
                        </Transition>
                      </div>
                    </div>
                  </div>

                  <div class="info-card annotation-card" v-if="annotationContent">
                    <div class="info-card-header">
                      <div class="info-card-icon">
                        <el-icon><EditPen /></el-icon>
                      </div>
                      <h4 class="info-card-title">译文注释</h4>
                      <el-button text size="small" @click="copyContent(annotationContent)" class="copy-btn">
                        <el-icon><CopyDocument /></el-icon>
                      </el-button>
                    </div>
                    <div class="info-card-content">
                      <div v-for="(para, idx) in formatContent(annotationContent)" :key="idx" class="content-paragraph">
                        <p>{{ para }}</p>
                      </div>
                    </div>
                  </div>

                  <div class="info-card appreciation-card" v-if="appreciationContent">
                    <div class="info-card-header">
                      <div class="info-card-icon appreciation-icon">
                        <el-icon><Star /></el-icon>
                      </div>
                      <h4 class="info-card-title">名人点评</h4>
                      <el-button text size="small" @click="copyContent(appreciationContent)" class="copy-btn">
                        <el-icon><CopyDocument /></el-icon>
                      </el-button>
                    </div>
                    <div class="info-card-content">
                      <div v-for="(para, idx) in formatContent(appreciationContent)" :key="idx" class="content-paragraph">
                        <p>{{ para }}</p>
                      </div>
                    </div>
                  </div>

                  <div class="info-card" v-if="backgroundContent">
                    <h4 class="info-card-title">创作背景</h4>
                    <div class="info-card-content">
                      <p style="white-space: pre-line;">{{ backgroundContent }}</p>
                    </div>
                  </div>

                  <div class="info-card" v-if="hasContent(['jj', 'yj', 'xzsf', 'dj', 'jx'])">
                    <h4 class="info-card-title">深度解读</h4>
                    <div class="info-card-content">
                      <p style="white-space: pre-line;">{{ getContent(['jj', 'yj', 'xzsf', 'dj', 'jx']) }}</p>
                    </div>
                  </div>

                  <div class="info-card" v-if="hasContent(['pj'])">
                    <h4 class="info-card-title">评价</h4>
                    <div class="info-card-content">
                      <p style="white-space: pre-line;">{{ getContent(['pj']) }}</p>
                    </div>
                  </div>

                  <div class="ai-chat-card">
                    <div class="ai-chat-header" @click="toggleAiChat">
                      <div class="ai-chat-header-left">
                        <el-icon><MagicStick /></el-icon>
                        <span>AI诗词助手</span>
                      </div>
                      <el-icon>
                        <ArrowUp v-if="aiChatVisible" />
                        <ArrowDown v-else />
                      </el-icon>
                    </div>

                    <Transition name="ai-chat-slide">
                      <div v-if="aiChatVisible" class="ai-chat-body">
                        <div class="ai-chat-messages" ref="aiChatContainerRef">
                          <div
                            v-for="(msg, index) in aiChatMessages"
                            :key="index"
                            :class="['ai-message', msg.role]"
                          >
                            <div class="ai-message-avatar" v-if="msg.role === 'assistant'">AI</div>
                            <div class="ai-message-content">{{ msg.content }}</div>
                            <div class="ai-message-avatar" v-if="msg.role === 'user'">我</div>
                          </div>
                          <div v-if="aiChatLoading" class="ai-message assistant">
                            <div class="ai-message-avatar">AI</div>
                            <div class="ai-message-content ai-loading">
                              <span class="loading-dot"></span>
                              <span class="loading-dot"></span>
                              <span class="loading-dot"></span>
                            </div>
                          </div>
                        </div>
                        <div class="ai-chat-input">
                          <el-input
                            v-model="aiChatInput"
                            type="textarea"
                            :rows="2"
                            :autosize="{ minRows: 1, maxRows: 3 }"
                            placeholder="输入问题，按Enter发送..."
                            @keydown="handleAiChatKeydown"
                            :disabled="aiChatLoading"
                            resize="none"
                          />
                          <el-button
                            class="ai-send-btn"
                            @click="sendAiChatMessage()"
                            :loading="aiChatLoading"
                            :disabled="!aiChatInput.trim()"
                          >
                            <el-icon><Promotion /></el-icon>
                          </el-button>
                        </div>
                      </div>
                    </Transition>
                  </div>

                  <div class="resource-card">
                    <h4 class="resource-card-title">相关资源</h4>
                    <div class="resource-list">
                      <a
                        v-for="res in resources"
                        :key="res.name"
                        :href="res.url"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="resource-item"
                      >
                        <el-icon><Link /></el-icon>
                        <span>{{ res.name }}</span>
                      </a>
                    </div>
                  </div>
                </div>
              </template>

              <el-empty v-else-if="!externalDetailLoading" description="暂未收录该诗词的详细赏析" />
            </div>

            <div class="poem-detail-layout placeholder" v-else>
              <div class="placeholder-content">
                <el-icon class="placeholder-icon"><Document /></el-icon>
                <p class="placeholder-text">点击上方诗词查看详细赏析</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <LoginPrompt
        v-model:visible="loginPromptVisible"
        message="使用AI诗词助手需要先登录"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
@use 'sass:color';

.poem-list-page {
  padding: $spacing-xl 0;
  position: relative;
  min-height: 100vh;
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

.container {
  position: relative;
  z-index: 1;
}

.page-nav {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;

  .el-button {
    color: $text-color-secondary;

    &:hover {
      color: $primary-color;
    }
  }

  .current-page {
    font-size: $font-size-sm;
    color: $text-color-light;
  }
}

.header-banner {
  position: relative;
  overflow: hidden;
  border-radius: $border-radius-lg;
  margin-bottom: $spacing-xl;

  .banner-particles {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
    pointer-events: none;
  }

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, #5a3e2b 0%, #6b4c36 50%, #5a3e2b 100%);
    z-index: 0;
  }

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: url('/img/dt_3.jpg') no-repeat center / cover;
    opacity: 0.15;
    z-index: 1;
  }

  > * {
    position: relative;
    z-index: 2;
  }
}

.page-header {
  text-align: center;
  padding: 40px 20px 30px;
}

.header-actions {
  margin-top: $spacing-md;
  display: flex;
  justify-content: center;
  gap: $spacing-sm;
}

.page-title {
  text-align: center;
  font-size: $font-size-title;
  color: #f8f0e6;
  font-family: $font-family-title;
  margin-bottom: $spacing-xs;
}

.page-subtitle {
  font-size: $font-size-sm;
  color: #d9c7b2;
  font-family: $font-family-base;
  margin: 0;
}

.poem-tabs {
  text-align: center;
  margin-bottom: $spacing-lg;
}

.filter-section {
  margin-bottom: $spacing-xl;
  background: url('/img/dt_5.jpg') no-repeat center / cover;
  border-radius: $border-radius-md;
}

.filter-card {
  border-radius: $border-radius-md;
  background: transparent;
  border: none;
  box-shadow: none;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-lg;
  align-items: center;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;

  label {
    font-size: $font-size-base;
    color: $text-color;
    white-space: nowrap;
  }

  .el-select,
  .el-input {
    width: 180px;
  }

  :deep(.el-input__inner) {
    font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
    letter-spacing: 0.5px;
    line-height: 1.5;
  }
}

.search-item {
  position: relative;
  flex: 1;
  min-width: 200px;
  
  label {
    font-size: $font-size-base;
    color: $text-color;
    white-space: nowrap;
  }
  
  .el-input {
    width: 100%;
  }
  
  :deep(.el-input__inner) {
    font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
    letter-spacing: 0.5px;
    line-height: 1.5;
    
    &::placeholder {
      color: $text-color-light;
      font-size: $font-size-base;
    }
  }
}

.search-input-wrapper {
  position: relative;
  width: 100%;
  
  .el-input {
    width: 100%;
  }
}

.search-panel {
  z-index: 9999;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  max-height: 400px;
  overflow-y: auto;
  @include scrollbar;
}

.panel-section {
  padding: $spacing-sm 0;
  
  &:not(:last-child) {
    border-bottom: 1px solid $border-color-light;
  }
}

.panel-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 30px;
  line-height: 30px;
  font-size: 12px;
  color: #909399;
}

.panel-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  height: 34px;
  line-height: 34px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: all 0.2s;
  
  &:hover {
    background: #f5f7fa;
    color: #409eff;
  }
  
  .el-icon {
    font-size: 14px;
    color: #c0c4cc;
  }
}

.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
  padding: $spacing-xs $spacing-md $spacing-sm;
}

.hot-tag {
  cursor: pointer;
  transition: all $transition-fast;
  
  &:hover {
    color: $primary-color;
    border-color: $primary-color;
    background: rgba($primary-color, 0.04);
  }
}

.result-stats {
  margin-bottom: $spacing-md;
  font-size: $font-size-sm;
  color: $text-color-secondary;
  display: flex;
  align-items: center;
  gap: 8px;

  strong {
    color: $primary-color;
    font-weight: 600;
  }
  
  .search-level-tag {
    margin-left: 4px;
  }
}

.poem-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-lg;
  min-height: 400px;
  max-height: calc(100vh - 300px);
  overflow-y: auto;
  padding-right: $spacing-sm;
  @include scrollbar;

  @include responsive(lg) {
    grid-template-columns: repeat(2, 1fr);
  }

  @include responsive(md) {
    grid-template-columns: 1fr;
    max-height: none;
  }
}

.poem-card {
  @include card;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  gap: $spacing-lg;
  position: relative;
  border-left: 3px solid $primary-color;
  transition: all $transition-base;

  &:hover {
    border-left-color: color.adjust($primary-color, $lightness: -10%);
    transform: translateY(-2px);
  }

  @include responsive(sm) {
    flex-direction: column;
    border-left: none;
    border-top: 3px solid $primary-color;

    &:hover {
      border-top-color: color.adjust($primary-color, $lightness: -10%);
    }
  }
}

.poem-image {
  width: 180px;
  height: 140px;
  flex-shrink: 0;
  border-radius: $border-radius-sm;
  overflow: hidden;
  position: relative;

  @include responsive(sm) {
    width: 100%;
    height: 160px;
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform $transition-slow;
  }
}

.featured-badge {
  position: absolute;
  top: 4px;
  left: 4px;
  background: linear-gradient(135deg, #FFD700, #FFA500);
  color: white;
  padding: 1px 4px;
  border-radius: $border-radius-sm;
  font-size: 9px;
  display: flex;
  align-items: center;
  gap: 2px;
  font-weight: bold;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  z-index: 2;
  pointer-events: none;
}

.original-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: linear-gradient(135deg, #67C23A, #85ce61);
  color: white;
  padding: 2px 6px;
  border-radius: $border-radius-sm;
  font-size: 10px;
  display: flex;
  align-items: center;
  gap: 2px;
  font-weight: bold;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  z-index: 1;
  pointer-events: none;
}

.foreign-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  color: white;
  padding: 2px 6px;
  border-radius: $border-radius-sm;
  font-size: 10px;
  display: flex;
  align-items: center;
  gap: 2px;
  font-weight: bold;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  z-index: 1;
  pointer-events: none;
}

.poem-card:hover .poem-image img {
  transform: scale(1.1);
}

.poem-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding-right: $spacing-lg;
}

.poem-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: $spacing-xs;
  gap: $spacing-sm;
}

.poem-title {
  font-size: $font-size-xl;
  color: $text-color;
  font-family: $font-family-title;
  flex: 1;
  min-width: 0;
  @include text-ellipsis;
}

.poem-tags {
  display: flex;
  gap: $spacing-xs;
  flex-shrink: 0;
}

.tag {
  padding: $spacing-xs $spacing-sm;
  border-radius: $border-radius-sm;
  font-size: $font-size-xs;
  font-weight: 500;
  white-space: nowrap;
}

.dynasty-tag {
  background-color: rgba($primary-color, 0.1);
  color: $primary-color;
}

.category-tag {
  background-color: rgba($accent-color, 0.1);
  color: $accent-color;
}

.original-tag {
  background-color: rgba($success-color, 0.1);
  color: $success-color;
}

.poem-author {
  font-size: $font-size-sm;
  color: $text-color-secondary;
  margin-bottom: $spacing-sm;
  font-family: $font-family-base;
}

.poem-verses {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
  min-height: 60px;
}

.verse-line {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: 1.6;
  margin: 0;
  font-family: $font-family-base;
  white-space: normal;
  word-break: break-all;
}

.poem-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: $spacing-sm;
  padding-top: $spacing-sm;
  border-top: 1px solid $border-color-light;

  @include responsive(sm) {
    flex-direction: column;
    align-items: flex-start;
    gap: $spacing-sm;
  }
}

.poem-meta {
  display: flex;
  gap: $spacing-md;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-light;
}

.rating-meta {
  color: $warning-color;
  font-weight: 600;
}

.poem-actions {
  display: flex;
  gap: $spacing-sm;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-secondary;
  transition: all $transition-fast;
  padding: $spacing-xs $spacing-sm;
  border-radius: $border-radius-sm;

  &:hover {
    background-color: $background-color;
  }
}

.like-btn {
  &.is-liked {
    color: $warning-color;

    &:hover {
      color: color.adjust($warning-color, $lightness: -10%);
    }
  }
}

.favorite-btn {
  &.is-favorited {
    color: $danger-color;

    &:hover {
      color: color.adjust($danger-color, $lightness: -10%);
    }
  }
}

.pagination-section {
  margin-top: $spacing-xl;
  display: flex;
  justify-content: center;
}

.card-corner {
  position: absolute;
  top: 0;
  right: 0;
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, transparent 50%, rgba($primary-color, 0.05) 50%);
  pointer-events: none;
}

.external-results-section {
  margin-top: $spacing-xl;
  padding: $spacing-xl;
  background: linear-gradient(135deg, #f5efe6 0%, #e8ddd0 40%, #f0e6d8 70%, #f5efe6 100%);
  border-radius: $border-radius-md;
  box-shadow: $box-shadow;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: $spacing-lg;
  flex-wrap: wrap;
  gap: $spacing-md;
}

.section-title-area {
  text-align: center;
  flex: 1;
}

.back-btn {
  color: $text-color-secondary;
  margin-bottom: $spacing-sm;

  &:hover {
    color: $primary-color;
  }
}

.section-title {
  font-size: 36px;
  font-family: $font-family-title;
  color: $primary-color;
  margin: 0 0 $spacing-xs;
  letter-spacing: 4px;
}

.section-subtitle {
  font-size: $font-size-base;
  color: $text-color-light;
  margin: 0;
  letter-spacing: 2px;
}

.poem-history-bar {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.history-label {
  white-space: nowrap;
}

.history-items {
  display: flex;
  gap: $spacing-xs;
}

.history-item {
  padding: $spacing-xs $spacing-sm;
  background: rgba($primary-color, 0.06);
  border-radius: $border-radius-sm;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: rgba($primary-color, 0.12);
  }

  &.is-current {
    background: $primary-color;
    color: #fff;
  }
}

.external-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  padding: $spacing-xxl 0;
  color: $text-color-secondary;
  font-size: $font-size-base;
}

.external-content-layout {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.external-poem-selector {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
  padding-bottom: $spacing-lg;
  border-bottom: 1px solid $border-color-light;
}

.poem-select-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-sm $spacing-md;
  background: rgba($primary-color, 0.04);
  border-radius: $border-radius-sm;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: rgba($primary-color, 0.08);
  }

  &.is-active {
    background: $primary-color;
    color: #fff;

    .poem-select-title,
    .poem-select-author {
      color: #fff;
    }
  }
}

.poem-select-title {
  font-size: $font-size-sm;
  font-weight: 500;
  color: $text-color;
}

.poem-select-author {
  font-size: $font-size-xs;
  color: $text-color-light;
}

.external-main-content {
  min-width: 0;
}

.poem-detail-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: $spacing-xl;
  min-height: 400px;
  align-items: start;

  &.placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.poem-left-panel {
  grid-column: 1;
  grid-row: 1 / 3;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: $background-color-light;
  border-radius: $border-radius-md;
  border: 1px solid $border-color-light;
  padding: $spacing-xl;
}

.poem-header-info {
  text-align: center;
  margin-bottom: $spacing-xl;
  padding-bottom: $spacing-lg;
  border-bottom: 1px solid $border-color-light;
}

.poem-main-title {
  font-size: 28px;
  font-family: $font-family-title;
  color: $primary-color;
  margin: 0 0 $spacing-sm;
}

.poem-meta {
  font-size: $font-size-base;
  color: $text-color-secondary;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: $spacing-sm;
}

.author-link {
  color: $primary-color;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  border-bottom: 1px dashed $primary-color;
  transition: all $transition-fast;

  &:hover {
    color: color.adjust($primary-color, $lightness: -10%);
    border-bottom-color: color.adjust($primary-color, $lightness: -10%);
  }

  .expand-icon {
    transition: transform $transition-fast;

    &.is-expanded {
      transform: rotate(180deg);
    }
  }
}

.author-info-expand {
  margin-top: $spacing-md;
  padding: $spacing-md;
  background: rgba($primary-color, 0.05);
  border-radius: $border-radius-sm;
  border-left: 3px solid $primary-color;
}

.author-info-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  color: $text-color-light;
  font-size: $font-size-sm;
}

.author-info-content {
  font-size: $font-size-sm;
  color: $text-color-secondary;
  line-height: 1.8;
}

.expand-enter-active,
.expand-leave-active {
  transition: all $transition-base;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
  margin-top: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 500px;
}

.slide-left-enter-active,
.slide-left-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.slide-left-enter-from,
.slide-left-leave-to {
  opacity: 0;
  transform: translateX(-30px);
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.slide-left-enter-to,
.slide-left-leave-from {
  opacity: 1;
  transform: translateX(0);
  max-height: 500px;
}

.action-toolbar {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;
  padding: $spacing-md 0;
  border-top: 1px solid $border-color-light;
  border-bottom: 1px solid $border-color-light;
}

.toolbar-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-xs;
  background: transparent;
  border: 1px solid $border-color;
  color: $text-color-secondary;
  transition: all $transition-fast;

  &:hover:not(:disabled) {
    background: rgba($primary-color, 0.06);
    border-color: $primary-color;
    color: $primary-color;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .el-icon {
    font-size: $font-size-lg;
  }
}

.poem-header-section {
  margin-bottom: $spacing-lg;
  text-align: center;
  position: relative;
  z-index: 10;
}

.poem-body-layout {
  display: flex;
  gap: $spacing-lg;
  justify-content: center;
}

.poem-content-section {
  text-align: center;
  max-width: 700px;
  margin: 0 auto;
  flex: 0 1 700px;
}

.poem-flyout-panel {
  position: absolute;
  top: 100%;
  right: 0;
  width: calc(100% + 20px);
  z-index: 20;
  margin-top: $spacing-sm;
}

.flyout-connector {
  position: absolute;
  top: 0;
  right: 40px;
  width: 12px;
  height: 12px;
  background: $background-color-light;
  border: 1px solid $border-color-light;
  border-bottom: none;
  border-right: none;
  transform: translateY(-50%) rotate(45deg);
}

.flyout-inner {
  background: $background-color-light;
  border-radius: $border-radius-md;
  border: 1px solid $border-color-light;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.flyout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md $spacing-lg;
  border-bottom: 1px solid $border-color-light;
  background: linear-gradient(135deg, rgba($primary-color, 0.04), rgba($primary-color, 0.01));

  h3 {
    font-size: $font-size-lg;
    font-weight: 600;
    color: $text-color;
    margin: 0;
  }
}

.flyout-content {
  padding: $spacing-lg;
  max-height: 400px;
  overflow-y: auto;
  @include scrollbar;
}

.panel-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $spacing-md;
  padding: $spacing-xxl;
  color: $text-color-secondary;
}

.search-loading-anim {
  display: flex;
  align-items: center;
  gap: 3px;

  .el-icon {
    font-size: $font-size-xl;
    color: $primary-color;
    animation: searchPulse 1.5s ease-in-out infinite;
  }
}

.search-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: $primary-color;
  animation: searchDotBounce 1.4s ease-in-out infinite;

  &:nth-child(2) { animation-delay: 0.2s; }
  &:nth-child(3) { animation-delay: 0.4s; }
}

@keyframes searchPulse {
  0%, 100% { opacity: 0.5; transform: scale(0.95); }
  50% { opacity: 1; transform: scale(1.1); }
}

@keyframes searchDotBounce {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.7); }
  40% { opacity: 1; transform: scale(1.2); }
}

.toolbar-btn {
  &.is-active {
    background: $primary-color;
    color: #fff;
    border-color: $primary-color;
  }
}

.panel-expand-left-enter-active {
  transition: all 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

.panel-expand-left-leave-active {
  transition: all 0.25s cubic-bezier(0.55, 0, 1, 0.45);
}

.panel-expand-left-enter-from,
.panel-expand-left-leave-to {
  opacity: 0;
  transform: translateX(30px) scale(0.97);
}

.panel-expand-left-enter-to,
.panel-expand-left-leave-from {
  opacity: 1;
  transform: translateX(0) scale(1);
}

.poem-verses-panel {
  margin-bottom: $spacing-xl;
  display: inline-block;
  text-align: left;
  max-width: 100%;
}

.verse-line-item {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-md $spacing-lg;
  border-radius: $border-radius-sm;
  cursor: pointer;
  transition: all $transition-fast;
  position: relative;

  &:hover {
    background: rgba($primary-color, 0.06);
  }

  &.is-explained {
    background: rgba($primary-color, 0.1);
    border-left: 3px solid $primary-color;
  }
}

.verse-number {
  font-size: $font-size-xs;
  color: $text-color-light;
  width: 20px;
  text-align: center;
  flex-shrink: 0;
}

.verse-text {
  font-size: $font-size-xxl;
  font-family: $font-family-title;
  color: $text-color;
  line-height: 2;
  letter-spacing: 2px;
  flex: 1;
}

.verse-explain-icon {
  color: $primary-color;
  font-size: $font-size-lg;
  flex-shrink: 0;
}

.ai-line-explain {
  background: linear-gradient(135deg, rgba(#667eea, 0.05), rgba(#764ba2, 0.05));
  border-radius: $border-radius-md;
  padding: $spacing-lg;
  border-left: 3px solid #667eea;
}

.ai-explain-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
  font-size: $font-size-sm;
  font-weight: 600;
  color: #667eea;
}

.ai-explain-close {
  margin-left: auto;
  cursor: pointer;
  font-size: $font-size-base;
  color: $text-color-light;
  transition: color $transition-fast;

  &:hover {
    color: $danger-color;
  }
}

.ai-explain-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  padding: $spacing-md 0;
  color: $text-color-light;
  font-size: $font-size-sm;
}

.ai-explain-content {
  font-size: $font-size-base;
  color: $text-color;
  line-height: 1.8;
  white-space: pre-line;
}

.foreign-original-panel {
  margin-top: $spacing-lg;
  background: linear-gradient(135deg, rgba(#667eea, 0.05), rgba(#764ba2, 0.05));
  border-radius: $border-radius-md;
  padding: $spacing-lg;
  border-left: 3px solid #667eea;
}

.foreign-original-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
  color: #667eea;
  font-weight: 600;
  font-size: $font-size-base;
}

.foreign-original-body {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.foreign-original-line {
  display: flex;
  align-items: baseline;
  gap: $spacing-md;
  padding: $spacing-xs 0;
}

.foreign-line-number {
  min-width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(#667eea, 0.1);
  color: #667eea;
  font-size: $font-size-xs;
  font-weight: 600;
  flex-shrink: 0;
}

.foreign-line-text {
  font-size: $font-size-base;
  color: $text-color;
  line-height: 1.8;
  font-style: italic;
  letter-spacing: 0.5px;
}

.expand-line-enter-active,
.expand-line-leave-active {
  transition: all $transition-base;
  overflow: hidden;
}

.expand-line-enter-from,
.expand-line-leave-to {
  opacity: 0;
  max-height: 0;
  margin-top: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.expand-line-enter-to,
.expand-line-leave-from {
  opacity: 1;
  max-height: 300px;
}

.enlarged-content-panel {
  background: linear-gradient(135deg, #f8f4ee 0%, #f0e8d8 100%);
  border-radius: $border-radius-md;
  border: 2px solid $primary-color;
  margin-bottom: $spacing-xl;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba($primary-color, 0.15);
}

.enlarged-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md $spacing-lg;
  background: linear-gradient(135deg, $primary-color, color.adjust($primary-color, $lightness: -10%));
  color: #fff;

  h3 {
    font-size: $font-size-lg;
    font-weight: 600;
    margin: 0;
    font-family: $font-family-title;
  }

  .el-button {
    color: #fff;
    &:hover {
      opacity: 0.8;
    }
  }
}

.enlarged-body {
  padding: $spacing-lg;
  max-height: 400px;
  overflow-y: auto;
  @include scrollbar;
}

.enlarged-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  padding: $spacing-xxl;
  color: $text-color-secondary;
}

.enlarged-text {
  font-size: $font-size-xl;
  line-height: 2;
  color: $text-color;
  font-family: $font-family-title;
  letter-spacing: 1px;
}

.enlarged-paragraph {
  margin-bottom: $spacing-md;
  text-indent: 2em;
  &:last-child {
    margin-bottom: 0;
  }
}

.enlarged-content-enter-active {
  transition: all 0.4s cubic-bezier(0.22, 1, 0.36, 1);
}

.enlarged-content-leave-active {
  transition: all 0.3s cubic-bezier(0.55, 0, 1, 0.45);
}

.enlarged-content-enter-from,
.enlarged-content-leave-to {
  opacity: 0;
  transform: translateY(-20px) scale(0.97);
  max-height: 0;
  margin-bottom: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.enlarged-content-enter-to,
.enlarged-content-leave-from {
  opacity: 1;
  transform: translateY(0) scale(1);
  max-height: 500px;
}

.copy-btn-top {
  margin-bottom: $spacing-md;
  color: $text-color-secondary;

  &:hover {
    color: $primary-color;
  }
}

.action-buttons-card {
  background: $background-color-light;
  border-radius: $border-radius-md;
  border: 1px solid $border-color-light;
  padding: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;

  .action-btn {
    width: 100%;
    justify-content: flex-start;
  }
}

.poem-right-panel {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  overflow-y: auto;
  padding-right: $spacing-sm;
  grid-column: 2;
  grid-row: 1 / 3;
  @include scrollbar;
}

.info-card {
  background: $background-color-light;
  border-radius: $border-radius-md;
  border: 1px solid $border-color-light;
  padding: $spacing-lg;
  transition: all $transition-fast;

  &:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }
}

.info-card-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
  padding-bottom: $spacing-sm;
  border-bottom: 1px solid $border-color-light;
}

.info-card-icon {
  width: 32px;
  height: 32px;
  border-radius: $border-radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8d5b7 0%, #d4af87 100%);
  color: #5a3e2b;
  font-size: $font-size-lg;

  &.appreciation-icon {
    background: linear-gradient(135deg, #f0e4d7 0%, #e8d5b7 100%);
    color: #8b6914;
  }
}

.info-card-title {
  font-size: $font-size-base;
  font-weight: 600;
  color: $text-color;
  margin: 0;
  flex: 1;
}

.copy-btn {
  color: $text-color-secondary;
  &:hover {
    color: $primary-color;
  }
}

.info-card-content {
  font-size: $font-size-sm;
  color: $text-color-secondary;
  line-height: 1.8;

  p {
    margin: 0;
  }
}

.content-paragraph {
  margin-bottom: $spacing-sm;
  padding-left: $spacing-sm;
  border-left: 2px solid transparent;
  transition: all $transition-fast;

  &:hover {
    border-left-color: $primary-color;
    background: rgba($primary-color, 0.02);
  }

  &:last-child {
    margin-bottom: 0;
  }

  p {
    text-indent: 2em;
  }
}

.annotation-card {
  border-left: 3px solid #d4af87;
}

.appreciation-card {
  border-left: 3px solid #e8d5b7;
}

.background-knowledge-card {
  background: $background-color-light;
  border-radius: $border-radius-md;
  border: 1px solid $border-color-light;
  padding: $spacing-lg;
}

.knowledge-panels {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.knowledge-panel {
  border: 1px solid $border-color-light;
  border-radius: $border-radius-sm;
  overflow: hidden;
  position: relative;
  min-height: 42px;
}

.knowledge-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md;
  background: rgba($primary-color, 0.03);
  cursor: pointer;
  transition: all $transition-fast;
  font-size: $font-size-sm;
  font-weight: 500;
  color: $text-color;

  &:hover {
    background: rgba($primary-color, 0.06);
  }

  .el-icon {
    transition: transform $transition-fast;

    &.is-expanded {
      transform: rotate(90deg);
    }
  }
}

.knowledge-panel-content {
  padding: $spacing-md;
  background: $background-color-light;
  transform-origin: left center;
}

.knowledge-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  color: $text-color-light;
  font-size: $font-size-sm;
  padding: $spacing-md 0;
}

.knowledge-text {
  font-size: $font-size-sm;
  color: $text-color-secondary;
  line-height: 1.8;
}

.knowledge-expand-btn {
  display: block;
  margin-top: $spacing-sm;
  color: $primary-color;
  font-size: $font-size-sm;
  padding: 0;
  &:hover {
    color: color.adjust($primary-color, $lightness: -10%);
  }
}

.info-empty {
  font-size: $font-size-sm;
  color: $text-color-light;
  margin: 0;
  font-style: italic;
}

.ai-chat-card {
  background: $background-color-light;
  border-radius: $border-radius-md;
  border: 1px solid $border-color-light;
}

.ai-chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md $spacing-lg;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    opacity: 0.9;
  }
}

.ai-chat-header-left {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  color: #fff;
  font-weight: 600;
}

.ai-chat-slide-enter-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  max-height: 500px;
  overflow: hidden;
}

.ai-chat-slide-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.ai-chat-slide-enter-from,
.ai-chat-slide-leave-to {
  max-height: 0;
  opacity: 0;
}

.ai-chat-body {
  border-top: 1px solid $border-color-light;
}

.ai-chat-messages {
  height: 250px;
  overflow-y: auto;
  padding: $spacing-md;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  @include scrollbar;
}

.ai-message {
  display: flex;
  gap: $spacing-sm;
  max-width: 90%;

  &.user {
    align-self: flex-end;
    flex-direction: row;

    .ai-message-content {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      border-radius: $border-radius-md $border-radius-md $border-radius-sm $border-radius-md;
    }
  }

  &.assistant {
    align-self: flex-start;

    .ai-message-content {
      background: #f4f5f7;
      color: $text-color;
      border-radius: $border-radius-md $border-radius-md $border-radius-md $border-radius-sm;
    }
  }
}

.ai-message-avatar {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $font-size-xs;
  font-weight: 600;
  margin-top: 2px;

  .assistant & {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
  }

  .user & {
    background: $primary-color;
    color: #fff;
  }
}

.ai-message-content {
  padding: $spacing-sm $spacing-md;
  font-size: $font-size-sm;
  line-height: 1.7;
  word-break: break-word;
  white-space: pre-line;
}

.ai-loading {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: $spacing-sm $spacing-lg;
}

.loading-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: $text-color-light;
  animation: dotPulse 1.2s ease-in-out infinite;

  &:nth-child(2) { animation-delay: 0.2s; }
  &:nth-child(3) { animation-delay: 0.4s; }
}

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

.ai-chat-input {
  display: flex;
  align-items: flex-end;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  border-top: 1px solid $border-color-light;
  background: rgba($primary-color, 0.01);

  .el-textarea {
    flex: 1;

    @include el-comment-input;

    :deep(.el-textarea__inner) {
      border-radius: $comment-input-radius;
      padding: $comment-input-padding;
    }
  }
}

.ai-send-btn {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  padding: 0;
  border-radius: $border-radius-md;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: #fff;

  &:hover:not(:disabled) {
    opacity: 0.9;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.resource-card {
  background: $background-color-light;
  border-radius: $border-radius-md;
  border: 1px solid $border-color-light;
  padding: $spacing-lg;
}

.resource-card-title {
  font-size: $font-size-sm;
  font-weight: 600;
  color: $text-color-secondary;
  margin: 0 0 $spacing-md;
}

.resource-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.resource-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm;
  border-radius: $border-radius-sm;
  text-decoration: none;
  font-size: $font-size-sm;
  color: $text-color;
  transition: all $transition-fast;

  &:hover {
    background: rgba($primary-color, 0.04);
    color: $primary-color;
  }
}

.placeholder-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 300px;
  color: $text-color-light;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: $spacing-md;
  opacity: 0.5;
}

.placeholder-text {
  font-size: $font-size-base;
  margin: 0;
}

@media (max-width: 768px) {
  .poem-detail-layout {
    grid-template-columns: 1fr;
  }

  .poem-left-panel {
    grid-column: auto;
    grid-row: auto;
  }

  .poem-right-panel {
    grid-column: auto;
    grid-row: auto;
  }

  .external-poem-selector {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }

  .action-toolbar {
    flex-direction: column;
  }

  .toolbar-btn {
    width: 100%;
  }

  .knowledge-panels {
    width: 100%;
  }

  .background-knowledge-card {
    width: 100%;
  }


  .poem-body-layout {
    flex-direction: column;
  }

  .poem-flyout-panel {
    width: calc(100% + $spacing-lg);
    right: -#{$spacing-sm};
  }
}

</style>