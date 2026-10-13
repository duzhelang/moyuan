<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted, onActivated, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getForumPostList } from '@/api/modules/forum'
import { writePoemFromImage, analyzePoem, matchCouplet, ocrImage, chat } from '@/api/modules/ai'
import { queryRhymeByCharacter, queryRhymeByGroup } from '@/api/modules/rhyme'
import type { RhymeItem } from '@/api/modules/rhyme'
import { getRandomPoetFeatured } from '@/api/modules/poetFeatured'
import type { PoetFeatured } from '@/api/modules/poetFeatured'
import { getVisionArticleList } from '@/api/modules/visionArticle'
import { ElMessage } from 'element-plus'
import type { ForumPost, VisionArticle } from '@/types/model'
import { useUserStore } from '@/stores/user'
import PoetCard from '@/components/business/PoetCard.vue'
import HomeNavigation from '@/components/business/HomeNavigation.vue'
import DailyPoetry from '@/components/business/DailyPoetry.vue'
import QrCodeLink from '@/components/common/QrCodeLink.vue'
import HomeNavBar from '@/components/business/HomeNavBar.vue'
import { HomeCarousel, ForumPreview, AncientPoemSelection, ContemporaryPoems } from '@/components/home'
import homePoetryData from '@/data/home-poetry-library.json'

const NAV_ANIMATION_KEY = 'home_nav_animation_config'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const hotPosts = ref<ForumPost[]>([])

const animationConfig = ref<Record<string, string>>({
  works: 'wave',
  genres: 'fade',
  dynasties: 'slide'
})

const loadAnimationConfig = () => {
  try {
    const saved = localStorage.getItem(NAV_ANIMATION_KEY)
    if (saved) animationConfig.value = { ...animationConfig.value, ...JSON.parse(saved) }
  } catch {}
}

const navBarRef = ref<InstanceType<typeof HomeNavBar> | null>(null)

const aiModel = ref('zhipu')
const visionModel = ref('glm-4.6v-flash')

const activeAiTab = ref<'poem' | 'analyze' | 'explore'>('poem')

const imageFile = ref<File | null>(null)
const imagePreview = ref('')
const poemResult = ref('')
const poemLoading = ref(false)

const analyzeInput = ref('')
const analyzeResult = ref('')
const analyzeLoading = ref(false)

const activeExtraPanel = ref<'rhyme' | 'couplet' | 'ocr' | 'acrostic' | 'meter' | 'feihua' | null>(null)

// 「诗境 AI」下方功能卡横向滚动容器（配合左右箭头切换）
const extraScrollRef = ref<HTMLElement | null>(null)

/** 横向滚动功能卡：dir 为 -1 向左、1 向右 */
const scrollExtra = (dir: number) => {
  extraScrollRef.value?.scrollBy({ left: dir * 430, behavior: 'smooth' })
}

/** 征集有趣功能：跳转到发布新帖，向社区征集想法 */
const handleCollectFeature = () => {
  router.push('/forum/create')
}

const ocrFile = ref<File | null>(null)
const ocrPreview = ref('')
const ocrResult = ref('')
const ocrLoading = ref(false)
const ocrUploadRef = ref<HTMLInputElement | null>(null)

const processOcrFile = (file: File) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请上传图片文件')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过10MB')
    return
  }
  ocrFile.value = file
  const reader = new FileReader()
  reader.onload = (e) => { ocrPreview.value = e.target?.result as string }
  reader.readAsDataURL(file)
}

const handleOcrUpload = (e: Event) => {
  const input = e.target as HTMLInputElement
  if (input.files && input.files.length > 0) {
    processOcrFile(input.files[0])
  }
}

const handleOcrDragOver = (e: DragEvent) => {
  (e.currentTarget as HTMLElement)?.classList.add('ai-drag-over')
}

const handleOcrDragLeave = (e: DragEvent) => {
  (e.currentTarget as HTMLElement)?.classList.remove('ai-drag-over')
}

const handleOcrDrop = (e: DragEvent) => {
  (e.currentTarget as HTMLElement)?.classList.remove('ai-drag-over')
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    processOcrFile(files[0])
  }
}

const clearOcrImage = () => {
  ocrFile.value = null
  ocrPreview.value = ''
  ocrResult.value = ''
  if (ocrUploadRef.value) ocrUploadRef.value.value = ''
}

const handleOcrRecognize = async () => {
  if (!ocrFile.value) {
    ElMessage.warning('请先上传图片')
    return
  }
  ocrLoading.value = true
  ocrResult.value = ''
  try {
    const res = await ocrImage(ocrFile.value, aiModel.value, visionModel.value, 'ocr')
    ocrResult.value = res.data.text
  } catch {
    ocrResult.value = '抱歉，AI服务暂时不可用，请稍后重试。'
  } finally {
    ocrLoading.value = false
  }
}

const rhymeInput = ref('')
const rhymeResults = ref<RhymeItem[]>([])
const rhymeLoading = ref(false)
const rhymeSearchType = ref<'character' | 'group'>('character')

const handleRhymeQuery = async () => {
  if (!rhymeInput.value.trim()) {
    ElMessage.warning('请输入要查询的汉字或韵部')
    return
  }
  rhymeLoading.value = true
  rhymeResults.value = []
  try {
    const res = rhymeSearchType.value === 'character'
      ? await queryRhymeByCharacter(rhymeInput.value.trim())
      : await queryRhymeByGroup(rhymeInput.value.trim())
    rhymeResults.value = res.data
    if (res.data.length === 0) {
      ElMessage.info('未找到匹配的韵脚数据')
    }
  } catch {
    ElMessage.error('查询失败，请稍后重试')
  } finally {
    rhymeLoading.value = false
  }
}

const coupletInput = ref('')
const coupletResult = ref('')
const coupletLoading = ref(false)

const exampleCouplets = homePoetryData.exampleCouplets

interface PoetryItem {
  title: string
  author: string
  poem: string
  info: string
}

const poetryLibrary: PoetryItem[] = homePoetryData.poetryLibrary
const analyzeExamples = homePoetryData.analyzeExamples

const explorePoem = ref('')
const exploreInfo = ref('')

const loadRandomPoem = () => {
  const pick = poetryLibrary[Math.floor(Math.random() * poetryLibrary.length)]
  explorePoem.value = pick.poem
  exploreInfo.value = `《${pick.title}》— ${pick.info}`
}

loadRandomPoem()

const handleCoupletMatch = async (upperCouplet?: string) => {
  const input = upperCouplet || coupletInput.value.trim()
  if (!input) {
    ElMessage.warning('请输入上联')
    return
  }
  coupletInput.value = input
  coupletLoading.value = true
  coupletResult.value = ''
  try {
    const res = await matchCouplet({ upperCouplet: input, moduleCode: 'couplet' })
    coupletResult.value = res.data.result
  } catch {
    coupletResult.value = '抱歉，AI服务暂时不可用，请稍后重试。'
  } finally {
    coupletLoading.value = false
  }
}

// ===== 藏头诗 =====
const acrosticInput = ref('')
const acrosticLength = ref<5 | 7>(7)
const acrosticResult = ref('')
const acrosticLoading = ref(false)
const acrosticExamples = ['墨渊', '风花雪月', '春江花月', '山河无恙']

// 解析 AI 返回的诗句：逐行取出，首字作为藏头字高亮
const acrosticLines = computed(() =>
  acrosticResult.value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .slice(0, 8)
    .map((line) => {
      const chars = Array.from(line)
      return { head: chars[0] ?? '', rest: chars.slice(1).join('') }
    })
)

const handleAcrosticGenerate = async (chars?: string) => {
  const input = (chars ?? acrosticInput.value).trim()
  if (!input) {
    ElMessage.warning('请输入藏头字')
    return
  }
  const heads = Array.from(input).slice(0, 8)
  if (heads.length < 2) {
    ElMessage.warning('藏头字至少 2 个')
    return
  }
  acrosticInput.value = heads.join('')
  acrosticLoading.value = true
  acrosticResult.value = ''
  try {
    const prompt = `请创作一首藏头诗，藏头字依次为「${heads.join('')}」。\n`
      + '要求：\n'
      + `1. 共 ${heads.length} 句，每句第一个字依次为上述藏头字，顺序不可变；\n`
      + `2. 体裁为${acrosticLength.value === 7 ? '七言' : '五言'}，每句 ${acrosticLength.value} 个字；\n`
      + '3. 押平声韵且一韵到底，内容连贯、意境统一、语言典雅；\n'
      + `4. 直接输出诗句，每句一行，共 ${heads.length} 行，不要标题、不要解释、不要任何多余符号。`
    const res = await chat({ message: prompt, moduleCode: 'chat' })
    acrosticResult.value = res.data.reply || ''
    if (!acrosticResult.value.trim()) {
      ElMessage.info('AI 未返回内容，请重试')
    }
  } catch {
    ElMessage.error('生成失败，请稍后重试')
  } finally {
    acrosticLoading.value = false
  }
}

// ===== 平仄格律校验 =====
interface MeterLine {
  text: string
  tone: string
  rhyme: boolean
  note: string
}
interface MeterResult {
  valid: boolean
  format: string
  rhymeGroup: string
  lines: MeterLine[]
  summary: string
}

const meterInput = ref('')
const meterLoading = ref(false)
const meterResult = ref<MeterResult | null>(null)
const meterRaw = ref('')

// 从 AI 返回中提取首个 JSON 对象（剥离 ``` 围栏）
function extractJson<T>(raw: string): T | null {
  if (!raw) return null
  let text = raw.trim()
  if (text.startsWith('```')) {
    const nl = text.indexOf('\n')
    if (nl >= 0) text = text.slice(nl + 1)
    const fence = text.lastIndexOf('```')
    if (fence >= 0) text = text.slice(0, fence)
  }
  const start = text.indexOf('{')
  const end = text.lastIndexOf('}')
  if (start < 0 || end <= start) return null
  try {
    return JSON.parse(text.slice(start, end + 1)) as T
  } catch {
    return null
  }
}

// 逐句拆出平仄字（仅保留 平/仄）
const meterRenderLines = computed(() =>
  (meterResult.value?.lines ?? []).map((line) => ({
    ...line,
    toneChars: Array.from(line.tone || '').filter((c) => c === '平' || c === '仄')
  }))
)

const handleMeterCheck = async () => {
  const input = meterInput.value.trim()
  if (!input) {
    ElMessage.warning('请输入要校验的诗句')
    return
  }
  meterLoading.value = true
  meterResult.value = null
  meterRaw.value = ''
  try {
    const prompt = '你是精通《平水韵》与近体诗格律的专家。请校验下面这首诗的平仄格律与押韵情况。\n'
      + `待校验诗作：\n${input}\n\n`
      + '要求：只输出如下严格 JSON，不要任何解释文字、不要 markdown 代码块标记：\n'
      + '{"valid":true或false,"format":"体裁，如 七言绝句","rhymeGroup":"押韵所属平水韵韵部，如 下平八庚；不押韵填 无",'
      + '"lines":[{"text":"该句原文（含标点）","tone":"逐字平仄，仅含 平/仄 两种字符并忽略标点，如 平仄平平仄仄平","rhyme":true或false,"note":"该句简评，若不合律请指出问题"}],'
      + '"summary":"总体格律评价，1-2 句"}'
    const res = await chat({ message: prompt, moduleCode: 'chat' })
    const raw = res.data.reply || ''
    const parsed = extractJson<MeterResult>(raw)
    if (parsed && Array.isArray(parsed.lines) && parsed.lines.length > 0) {
      meterResult.value = {
        valid: !!parsed.valid,
        format: parsed.format || '',
        rhymeGroup: parsed.rhymeGroup || '',
        lines: parsed.lines,
        summary: parsed.summary || ''
      }
    } else if (raw.trim()) {
      // 解析失败时兜底展示原始文本
      meterRaw.value = raw.trim()
    } else {
      ElMessage.info('AI 未返回内容，请重试')
    }
  } catch {
    ElMessage.error('校验失败，请稍后重试')
  } finally {
    meterLoading.value = false
  }
}

// ===== 飞花令 / 诗词接龙 =====
const feihuaMode = ref<'keyword' | 'chain'>('keyword')
const feihuaInput = ref('')
const feihuaLoading = ref(false)
const feihuaResult = ref('')
const feihuaLines = computed(() =>
  feihuaResult.value.split('\n').map((line) => line.trim()).filter(Boolean)
)
const feihuaBtnText = computed(() => (feihuaMode.value === 'keyword' ? '玩飞花令' : '接下一句'))

const handleFeihua = async () => {
  const input = feihuaInput.value.trim()
  if (!input) {
    ElMessage.warning(feihuaMode.value === 'keyword' ? '请输入令字' : '请输入上一句')
    return
  }
  feihuaLoading.value = true
  feihuaResult.value = ''
  try {
    const prompt = feihuaMode.value === 'keyword'
      ? `请以「${input}」为令字，列出 10 句含有该字的古诗词名句。\n要求：只引用确有其篇的真实名句，严禁杜撰；每句一行，格式为「诗句 —— 作者《篇名》」，不要编号、不要解释、不要多余符号。`
      : `请以古诗词名句接龙：上一句为「${input}」。请以该句最后一个汉字为首字，接出下一句古诗词名句。\n要求：只引用确有其篇的真实名句，严禁杜撰；共输出 2 行，第 1 行为接龙句（首字须与上一句末字相同），第 2 行为「—— 作者《篇名》」，不要其他解释。`
    const res = await chat({ message: prompt, moduleCode: 'chat' })
    feihuaResult.value = res.data.reply || ''
    if (!feihuaResult.value.trim()) {
      ElMessage.info('AI 未返回内容，请重试')
    }
  } catch {
    ElMessage.error('查询失败，请稍后重试')
  } finally {
    feihuaLoading.value = false
  }
}

const showRightTools = ref(false)
const isNavFixed = ref(false)
const clockTimer = ref<ReturnType<typeof setInterval> | null>(null)
const currentTime = ref('')

const imageUploadRef = ref<HTMLInputElement | null>(null)

const asset = (path: string) => path

const POET_VISIBLE_COUNT = 4
const PLACEHOLDER_POET: PoetFeatured = {
  id: -1, poetId: null, name: '敬请期待', dynasty: '',
  description: '更多诗人正在收录中...', biography: '',
  imageUrl: '', sortOrder: 0, status: 1, isShow: 0
}

const poetsData = ref<PoetFeatured[]>([])
const poetPool = ref<PoetFeatured[]>([])
const displayedPoetIds = ref<Set<number>>(new Set())
const poetRefreshTimer = ref<ReturnType<typeof setInterval> | null>(null)
// 鼠标悬浮在“著名诗人”行时暂停定时轮换
const isPoetHovering = ref(false)
const visionArticles = ref<VisionArticle[]>([])
const visionCurrentPage = ref(1)
const visionPageSize = ref(5)
const visionTotal = ref(0)

const shuffleArray = <T,>(arr: T[]): T[] => {
  const copy = [...arr]
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[copy[i], copy[j]] = [copy[j], copy[i]]
  }
  return copy
}

const dedupPoets = (items: PoetFeatured[]): PoetFeatured[] => {
  const seen = new Set<number>()
  return items.filter(item => {
    const key = item.poetId ?? item.id
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

const selectCoordinatedPoets = (pool: PoetFeatured[], excludeIds: Set<number>): PoetFeatured[] => {
  const candidates = pool.filter(p => !excludeIds.has(p.poetId ?? p.id))
  if (candidates.length >= POET_VISIBLE_COUNT) {
    return shuffleArray(candidates).slice(0, POET_VISIBLE_COUNT)
  }
  const fallback = shuffleArray(pool)
  const result: PoetFeatured[] = [...candidates]
  for (const item of fallback) {
    if (result.length >= POET_VISIBLE_COUNT) break
    const key = item.poetId ?? item.id
    if (!result.some(r => (r.poetId ?? r.id) === key)) {
      result.push(item)
    }
  }
  while (result.length < POET_VISIBLE_COUNT) {
    result.push({ ...PLACEHOLDER_POET, id: -1 - result.length })
  }
  return result
}

const refreshPoetsDisplay = () => {
  const selected = selectCoordinatedPoets(poetPool.value, displayedPoetIds.value)
  poetsData.value = selected
  displayedPoetIds.value = new Set(selected.map(p => p.poetId ?? p.id))
}

const fetchRandomPoets = async () => {
  try {
    const res = await getRandomPoetFeatured(8)
    poetPool.value = dedupPoets(res.data || [])
    refreshPoetsDisplay()
  } catch {
    console.error('获取精选诗人失败')
    if (poetsData.value.length === 0) {
      poetsData.value = Array.from({ length: POET_VISIBLE_COUNT }, (_, i) => ({ ...PLACEHOLDER_POET, id: -1 - i }))
    }
  }
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const scrollToSearch = () => {
  const el = document.querySelector('.search-wrapper') as HTMLElement
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    setTimeout(() => { navBarRef.value?.searchInputRef?.focus() }, 500)
  }
}

const openMusic = () => {
  const audio = document.getElementById('myAudio') as HTMLAudioElement
  if (audio) {
    if (audio.paused) audio.play()
    else audio.pause()
  }
}

const handleImageUpload = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    processImageFile(file)
  }
}

const processImageFile = (file: File) => {
  if (!file.type.match(/image\/(jpeg|png|webp|gif)/)) {
    ElMessage.warning('请上传 JPG / PNG / GIF / WebP 格式的图片')
    return
  }
  imageFile.value = file
  const reader = new FileReader()
  reader.onload = (e) => {
    imagePreview.value = e.target?.result as string
  }
  reader.readAsDataURL(file)
}

const handleDragOver = (e: DragEvent) => {
  (e.currentTarget as HTMLElement)?.classList.add('ai-drag-over')
}

const handleDragLeave = (e: DragEvent) => {
  (e.currentTarget as HTMLElement)?.classList.remove('ai-drag-over')
}

const handleDrop = (e: DragEvent) => {
  (e.currentTarget as HTMLElement)?.classList.remove('ai-drag-over')
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    processImageFile(files[0])
  }
}

const clearImage = () => {
  imageFile.value = null
  imagePreview.value = ''
  poemResult.value = ''
  if (imageUploadRef.value) imageUploadRef.value.value = ''
}

const handleWritePoem = async () => {
  if (!imageFile.value) {
    ElMessage.warning('请先上传图片')
    return
  }
  poemLoading.value = true
  poemResult.value = ''
  try {
    console.log('开始调用看图写诗, model:', aiModel.value, 'visionModel:', visionModel.value)
    const res = await writePoemFromImage(imageFile.value, aiModel.value, visionModel.value, 'write_poem')
    console.log('看图写诗响应:', res)
    poemResult.value = res.data.poem
  } catch (error) {
    console.error('看图写诗失败:', error)
    poemResult.value = '抱歉，AI服务暂时不可用，请稍后重试。'
  } finally {
    poemLoading.value = false
  }
}

const handleAnalyzePoem = async () => {
  if (!analyzeInput.value.trim()) {
    ElMessage.warning('请输入要分析的诗句')
    return
  }
  analyzeLoading.value = true
  analyzeResult.value = ''
  try {
    const res = await analyzePoem({ poem: analyzeInput.value, model: aiModel.value, moduleCode: 'analyze' })
    analyzeResult.value = res.data.analysis
  } catch {
    analyzeResult.value = '抱歉，AI服务暂时不可用，请稍后重试。'
  } finally {
    analyzeLoading.value = false
  }
}



const handleLogout = () => {
  try {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } catch {
    ElMessage.error('退出失败')
  }
}

const handleUserCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/user/profile')
      break
    case 'favorites':
      router.push('/user/profile?tab=favorites')
      break
    case 'history':
      router.push('/user/profile?tab=history')
      break
    case 'admin':
      router.push('/admin/dashboard')
      break
    case 'logout':
      handleLogout()
      break
  }
}

const onNavSearch = (payload: { query: string; type: 'internal' | 'external' }) => {
  if (!payload.query) {
    ElMessage.warning('请输入搜索内容')
    return
  }
  if (payload.type === 'internal') {
    const el = document.querySelector('.poem_selection_container') as HTMLElement
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  } else {
    window.open(`https://www.baidu.com/s?wd=${encodeURIComponent(payload.query + ' 诗词')}`, '_blank')
  }
}

const handleScroll = () => {
  showRightTools.value = window.scrollY > 300
  isNavFixed.value = window.scrollY > 650
}

const showTime = () => {
  currentTime.value = new Date().toLocaleString()
}

/* ==================== 底部双框焦点显示 ==================== */
type PanelSide = 'left' | 'right'

// 悬停预览延迟：需在同一面板停留 3 秒才触发焦点放大，避免鼠标扫过时频繁抖动
const HOVER_PREVIEW_DELAY = 3000
// 面板最小可用宽度，窄屏堆叠时焦点功能关闭
const PANEL_FOCUS_MIN_WIDTH = 769

const lockedSide = ref<PanelSide | null>(null)
const hoverSide = ref<PanelSide | null>(null)
// 锁定优先于悬停预览：点击锁定后焦点固定，悬停不再改变布局；未锁定时悬停才起预览作用
const activeSide = computed<PanelSide | null>(() => lockedSide.value ?? hoverSide.value)

const panelFocusClass = computed(() => {
  if (activeSide.value === 'left') return 'focus-left'
  if (activeSide.value === 'right') return 'focus-right'
  return ''
})

const isPanelFocusEnabled = () => window.innerWidth >= PANEL_FOCUS_MIN_WIDTH

let hoverTimer: ReturnType<typeof setTimeout> | null = null

const clearHoverTimer = () => {
  if (hoverTimer) {
    clearTimeout(hoverTimer)
    hoverTimer = null
  }
}

// 命中判定：根据事件目标所属面板推断是哪一侧
const resolvePanelSide = (target: EventTarget | null): PanelSide | null => {
  const el = target as HTMLElement | null
  if (!el || typeof el.closest !== 'function') return null
  if (el.closest('.poem_selection_left')) return 'left'
  if (el.closest('.poem_selection_right')) return 'right'
  return null
}

const handlePanelOver = (e: MouseEvent) => {
  // 已锁定焦点时不再响应悬停，保证点击后焦点固定
  if (!isPanelFocusEnabled() || lockedSide.value) return
  const side = resolvePanelSide(e.target)
  if (side === hoverSide.value) return
  clearHoverTimer()
  if (!side) {
    hoverSide.value = null
    return
  }
  hoverTimer = setTimeout(() => { hoverSide.value = side }, HOVER_PREVIEW_DELAY)
}

const handlePanelLeave = () => {
  clearHoverTimer()
  hoverSide.value = null
}

// 点击切换焦点：默认双列，点击某一侧放大该侧，再次点击同侧恢复双列
const togglePanelFocus = (side: PanelSide) => {
  if (!isPanelFocusEnabled()) return
  lockedSide.value = lockedSide.value === side ? null : side
  hoverSide.value = null
  clearHoverTimer()
}

// 点击面板内交互元素（按钮/输入框/分页）时不触发焦点切换，避免误触；
// 收起骨架条自身是 button，需排除在外，点击它就是展开该侧
const INTERACTIVE_SELECTOR = 'button:not(.panel-spine), a, input, textarea, select, .el-pagination'

const handlePanelClick = (e: MouseEvent) => {
  const el = e.target as HTMLElement | null
  if (el && typeof el.closest === 'function' && el.closest(INTERACTIVE_SELECTOR)) return
  const side = resolvePanelSide(e.target)
  if (side) togglePanelFocus(side)
}

const handlePanelKeydown = (e: KeyboardEvent) => {
  const tag = (e.target as HTMLElement | null)?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA') return
  if (e.key === 'Escape') {
    lockedSide.value = null
    hoverSide.value = null
    return
  }
  if (!isPanelFocusEnabled()) return
  if (e.key === 'ArrowLeft') {
    lockedSide.value = 'left'
    hoverSide.value = null
  } else if (e.key === 'ArrowRight') {
    lockedSide.value = 'right'
    hoverSide.value = null
  }
}

// 面板高度：以右侧「当代·精选」的自然内容高度为定值，避免宽度变化时高度抖动
const PANEL_TRANSITION_MS = 620
const panelHeight = ref(0)
const panelStyle = computed(() =>
  panelHeight.value ? { '--poem-panel-h': `${panelHeight.value}px` } : {}
)

const measurePanelHeight = () => {
  // 仅在等宽态测量：收起态的窄宽度会让卡片重排，测出的高度没有参考价值
  if (activeSide.value !== null || !isPanelFocusEnabled()) return
  // 等宽态下容器是 height:auto，高度由右侧「当代·精选」的内容自然撑起
  // （左侧面板 contain:size，不参与撑高），直接读容器高度即得定值，无需对面板做样式手术
  const container = document.querySelector('.poem_selection_container') as HTMLElement | null
  if (!container) return
  const h = Math.round(container.getBoundingClientRect().height)
  if (h > 240) panelHeight.value = h
}

let panelResizeTimer: ReturnType<typeof setTimeout> | null = null
const schedulePanelMeasure = (delay = 0) => {
  if (panelResizeTimer) clearTimeout(panelResizeTimer)
  panelResizeTimer = setTimeout(() => {
    panelResizeTimer = null
    measurePanelHeight()
  }, delay)
}

const handlePanelResize = () => schedulePanelMeasure(200)

// 释放焦点后宽度还在过渡，等动画结束再按等宽态重新测量
watch(activeSide, (side) => {
  if (side === null) schedulePanelMeasure(PANEL_TRANSITION_MS + 80)
})

onMounted(async () => {
  loadAnimationConfig()
  showTime()
  clockTimer.value = setInterval(showTime, 1000)

  window.addEventListener('scroll', handleScroll)

  fetchRandomPoets()
  poetRefreshTimer.value = setInterval(() => {
    if (!isPoetHovering.value) {
      fetchRandomPoets()
    }
  }, 20000)

  try {
    const postRes = await getForumPostList({ pageNum: 1, pageSize: 5 }).catch(() => ({ data: { list: [] as ForumPost[] } }))
    hotPosts.value = postRes.data?.list || []
  } catch {
    console.error('首页数据加载失败')
  }

  fetchVisionArticles()

  // 底部双框：先在等宽态测量面板高度（默认双列，不恢复历史焦点状态）
  await nextTick()
  measurePanelHeight()
  // 图片/字体加载后可能引起一次重排，延迟补测一次
  schedulePanelMeasure(400)
  window.addEventListener('keydown', handlePanelKeydown)
  window.addEventListener('resize', handlePanelResize)
})

const fetchVisionArticles = async () => {
  try {
    const visionRes = await getVisionArticleList({
      pageNum: visionCurrentPage.value,
      pageSize: visionPageSize.value
    }).catch(() => ({ data: { list: [] as VisionArticle[], total: 0 } }))
    visionArticles.value = visionRes.data?.list || []
    visionTotal.value = visionRes.data?.total || 0
  } catch {
    console.error('诗话视野数据加载失败')
  }
}

const handleVisionPageChange = (page: number) => {
  visionCurrentPage.value = page
  fetchVisionArticles()
}

onUnmounted(() => {
  if (poetRefreshTimer.value) clearInterval(poetRefreshTimer.value)
  if (clockTimer.value) {
    clearInterval(clockTimer.value)
    clockTimer.value = null
  }
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('keydown', handlePanelKeydown)
  window.removeEventListener('resize', handlePanelResize)
  clearHoverTimer()
  if (panelResizeTimer) {
    clearTimeout(panelResizeTimer)
    panelResizeTimer = null
  }
})

const handleTabParam = () => {
  const tabParam = route.query.tab as string
  if (tabParam && ['poem', 'analyze', 'explore'].includes(tabParam)) {
    activeAiTab.value = tabParam as 'poem' | 'analyze' | 'explore'
    setTimeout(() => {
      const aiSection = document.querySelector('.ai-container')
      if (aiSection) {
        aiSection.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
    }, 300)
  }
}

watch(() => route.query.tab, (newTab) => {
  if (newTab) {
    handleTabParam()
  }
}, { immediate: true })

onActivated(() => {
  handleTabParam()
})
</script>

<template>
  <div class="home-page">
    <div class="top" id="sy_top">
      <div class="logo">
        <a href="/">
          <img class="logo_img" src="/img/tubiao_1.jpg" alt="">
          <span class="logo-txt">墨渊</span>
        </a>
      </div>
      <div class="top_txt">
        <div class="shijian" title="现在时间">{{ currentTime }}</div>
        <QrCodeLink text="了解我们" qrImage="/img/wechat_qr.jpg" alt="扫一扫查看" />
        <QrCodeLink text="联系我们" qrImage="/img/wechat_qr.jpg" alt="扫一扫联系" />
        <template v-if="userStore.isLoggedIn && userStore.userInfo">
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="top-user-info">
              <el-avatar :src="userStore.avatar" :size="28">
                {{ userStore.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span class="top-user-name">{{ userStore.userInfo?.nickname || userStore.username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                <el-dropdown-item command="history">浏览历史</el-dropdown-item>
                <el-dropdown-item v-if="userStore.userInfo?.role === 'admin'" command="admin">后台管理</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/user/login" class="top-link top-link-login">登录</router-link>
          <span class="top-divider">|</span>
          <router-link to="/user/register" class="top-link top-link-register">注册</router-link>
        </template>
      </div>
    </div>
    <div v-show="showRightTools" class="youce_fangbiao">
      <div class="youce_fangbiao_s">
        <div class="you" @click="openMusic">
          <img :src="asset('/img/ce_tubiao2.png')" alt="">
        </div>
        <div class="you">
          <img :src="asset('/img/ce_tubiao3.png')" alt="">
        </div>
      </div>
      <div class="youce_fangbiao_x">
        <div class="you" @click="scrollToTop">
          <img :src="asset('/img/jianzu_5.png')" alt="">
        </div>
      </div>
    </div>
    <audio id="myAudio" :src="asset('/js/music.mp3')" preload="auto"></audio>

    <div class="carousel-wrapper">
      <div class="sy_top_daohang_biao">
        <div class="toptub1"><a href="#luntan" title="交流论坛"><img src="/img/top_tubiao4.png" alt="交流论坛"></a></div>
        <div class="toptub2" title="诗词库"><a href="/poem" @click.prevent="router.push('/poem')"><img src="/img/top_tubiao2.png" alt="诗词库"></a></div>
        <div class="toptub3" title="搜索" @click="scrollToSearch"><a href="javascript:void(0)"><img src="/img/top_tubiao1.png" alt="搜索"></a></div>
        <div class="toptub4" :title="(userStore.isLoggedIn && userStore.userInfo) ? '个人中心' : '去登录'" @click="(userStore.isLoggedIn && userStore.userInfo) ? router.push('/user/profile') : router.push('/user/login')"><a href="javascript:void(0)"><img src="/img/top_tubiao3.png" :alt="(userStore.isLoggedIn && userStore.userInfo) ? '个人中心' : '去登录'"></a></div>
      </div>
      <HomeCarousel type="main" />
    </div>

    <hr class="hr">
    <hr>
    <HomeNavBar ref="navBarRef" :is-nav-fixed="isNavFixed" @search="onNavSearch" />

    <div class="shiwen-wrapper">
      <HomeCarousel type="shiwen" />
      <img class="zhuang_s1" :src="asset('/img/gn_3.png')" alt="">
      <img class="zhuang_s2" :src="asset('/img/fy_tubiao_6.png')" alt="">
      <img class="zhuang_s5" :src="asset('/img/zichu_1.png')" alt="">
      <img class="zhuang_s7" :src="asset('/img/tuobiao_4.png')" alt="">
      <img class="zhuang_s3" :src="asset('/img/canci.png')" alt="">
      <img class="zhuang_s4" :src="asset('/img/gn_2.png')" alt="">
      <img class="zhuang_s4_top" :src="asset('/img/gn_2.png')" alt="">
      <img class="zhuang_s9" :src="asset('/img/wz_tubiao_2.png')" alt="">
    </div>

    <hr>
    <DailyPoetry />
    <div class="hr-image-wrapper">
      <hr>
      <img class="zhuang_s4_1" :src="asset('/img/gn_2.png')" alt="">
    </div>
    <h1 class="sy_fljx">分类精赏</h1>

    <div class="z_top">
      <div class="h4" @mouseenter="isPoetHovering = true" @mouseleave="isPoetHovering = false">
        <h1>著名诗人</h1>
        <PoetCard
          v-for="poetItem in poetsData"
          :key="poetItem.poetId ?? poetItem.id"
          :poet="poetItem"
        />
      </div>

      <div class="cd_fenge"></div>

      <HomeNavigation type="works" title="作品" :animation="animationConfig.works as 'wave' | 'fade' | 'slide'" />

      <div class="cd_fenge"></div>

      <HomeNavigation type="genres" title="流派" :animation="animationConfig.genres as 'wave' | 'fade' | 'slide'" />

      <HomeNavigation type="dynasties" title="朝代" :animation="animationConfig.dynasties as 'wave' | 'fade' | 'slide'" />

      <div class="cd_fenge"></div>
      <h2>展风拓潮</h2>
      <img class="zhuang_s6" :src="asset('/img/zichu_4.png')" alt="">
      <img class="zhuang_s7" :src="asset('/img/tuobiao_4.png')" alt="">
    </div>

    <hr>
    <div class="ai-container">
      <header class="ai-header">
        <div class="ai-header-icon">🖋️</div>
        <h2 class="ai-header-title">诗境 AI</h2>
        <span class="ai-seal">雅</span>
        <span class="ai-subtitle">· 古诗词智能创作与品鉴 ·</span>
      </header>

      <nav class="ai-tab-nav">
        <button
          class="ai-tab-btn"
          :class="{ active: activeAiTab === 'poem' }"
          @click="activeAiTab = 'poem'"
        >
          <span class="ai-tab-icon">🖼️</span> 看图写诗
        </button>
        <button
          class="ai-tab-btn"
          :class="{ active: activeAiTab === 'analyze' }"
          @click="activeAiTab = 'analyze'"
        >
          <span class="ai-tab-icon">🔍</span> 智能分析
        </button>
        <button
          class="ai-tab-btn"
          :class="{ active: activeAiTab === 'explore' }"
          @click="activeAiTab = 'explore'"
        >
          <span class="ai-tab-icon">📜</span> 诗海拾贝
        </button>
      </nav>

      <div class="ai-panels">
        <div v-show="activeAiTab === 'poem'" class="ai-panel">
          <div class="ai-upload-zone" @click="imageUploadRef?.click()" @dragover.prevent="handleDragOver" @dragleave="handleDragLeave" @drop.prevent="handleDrop">
            <span class="ai-upload-emoji">🏔️</span>
            <p class="ai-upload-text">点击上传图片，或拖拽至此处</p>
            <p class="ai-upload-hint">支持 JPG / PNG / GIF / WebP，单图 ≤32MB，AI 将为您赋诗一首</p>
            <input ref="imageUploadRef" type="file" accept="image/jpeg,image/png,image/gif,image/webp" style="display:none" @change="handleImageUpload">
          </div>
          <div v-if="imagePreview" class="ai-preview-area">
            <img :src="imagePreview" alt="预览图片">
            <div class="ai-preview-actions">
              <button class="ai-btn-primary" @click="handleWritePoem" :disabled="poemLoading">
                {{ poemLoading ? 'AI赋诗中...' : '✨ 开始赋诗' }}
              </button>
              <button class="ai-btn-outline" @click="clearImage">重新选择</button>
            </div>
          </div>
          <div v-if="poemResult" class="ai-result-card">
            <div class="ai-poem-content">{{ poemResult }}</div>
          </div>
        </div>

        <div v-show="activeAiTab === 'analyze'" class="ai-panel">
          <div class="ai-analyze-wrap">
            <textarea
              v-model="analyzeInput"
              class="ai-analyze-textarea"
              placeholder="在此输入诗句，如：床前明月光，疑是地上霜。"
            ></textarea>
          </div>
          <div class="ai-example-tags">
            <span
              v-for="ex in analyzeExamples"
              :key="ex.text"
              class="ai-example-tag"
              @click="analyzeInput = ex.text"
            >{{ ex.label }}</span>
          </div>
          <div style="margin-top:14px;text-align:right;">
            <button class="ai-btn-primary" @click="handleAnalyzePoem" :disabled="analyzeLoading">
              {{ analyzeLoading ? 'AI解析中...' : '🔍 智能解析' }}
            </button>
          </div>
          <div v-if="analyzeResult" class="ai-result-card">
            <div class="ai-analysis-label">📖 深度解析</div>
            <div class="ai-analysis-text">{{ analyzeResult }}</div>
          </div>
        </div>

        <div v-show="activeAiTab === 'explore'" class="ai-panel">
          <p class="ai-explore-hint">🌊 随机推荐一首经典诗词，感受千年文脉</p>
          <div class="ai-result-card ai-explore-card">
            <div class="ai-poem-content">{{ explorePoem }}</div>
            <div class="ai-analysis-text ai-explore-info">{{ exploreInfo }}</div>
          </div>
          <div style="text-align:center;margin-top:12px;">
            <button class="ai-btn-outline" @click="loadRandomPoem">🔄 换一首</button>
          </div>
        </div>
      </div>

      <div class="ai-extra-wrap">
        <button class="ai-extra-arrow ai-extra-arrow--left" type="button" aria-label="上一组功能" @click="scrollExtra(-1)">‹</button>
        <div ref="extraScrollRef" class="ai-extra-features">
          <div class="ai-extra-card" :class="{ active: activeExtraPanel === 'rhyme' }" @click="activeExtraPanel = activeExtraPanel === 'rhyme' ? null : 'rhyme'">
            <div class="ai-extra-icon">🎵</div>
            <div class="ai-extra-title">韵脚查询</div>
            <div class="ai-extra-desc">查询汉字所属韵部</div>
          </div>
          <div class="ai-extra-card" :class="{ active: activeExtraPanel === 'couplet' }" @click="activeExtraPanel = activeExtraPanel === 'couplet' ? null : 'couplet'">
            <div class="ai-extra-icon">🏮</div>
            <div class="ai-extra-title">AI 对对联</div>
            <div class="ai-extra-desc">上联出题，AI 对下联</div>
          </div>
          <div class="ai-extra-card" :class="{ active: activeExtraPanel === 'ocr' }" @click="activeExtraPanel = activeExtraPanel === 'ocr' ? null : 'ocr'">
            <div class="ai-extra-icon">📷</div>
            <div class="ai-extra-title">古籍识文</div>
            <div class="ai-extra-desc">AI识别图片中的古诗词文字</div>
          </div>
          <div class="ai-extra-card" :class="{ active: activeExtraPanel === 'acrostic' }" @click="activeExtraPanel = activeExtraPanel === 'acrostic' ? null : 'acrostic'">
            <div class="ai-extra-icon">🖋️</div>
            <div class="ai-extra-title">藏头诗</div>
            <div class="ai-extra-desc">输入藏头字，AI 作藏头诗</div>
          </div>
          <div class="ai-extra-card" :class="{ active: activeExtraPanel === 'meter' }" @click="activeExtraPanel = activeExtraPanel === 'meter' ? null : 'meter'">
            <div class="ai-extra-icon">🧭</div>
            <div class="ai-extra-title">平仄校验</div>
            <div class="ai-extra-desc">校验诗句平仄与押韵</div>
          </div>
          <div class="ai-extra-card" :class="{ active: activeExtraPanel === 'feihua' }" @click="activeExtraPanel = activeExtraPanel === 'feihua' ? null : 'feihua'">
            <div class="ai-extra-icon">🌺</div>
            <div class="ai-extra-title">飞花令</div>
            <div class="ai-extra-desc">行令对诗，诗词接龙</div>
          </div>
          <div class="ai-extra-card">
            <div class="ai-extra-icon">✒️</div>
            <div class="ai-extra-title">书法生成</div>
            <div class="ai-extra-desc">诗句转书法图片</div>
          </div>
          <div class="ai-extra-card">
            <div class="ai-extra-icon">📚</div>
            <div class="ai-extra-title">我的诗笺</div>
            <div class="ai-extra-desc">收藏喜爱的诗词</div>
          </div>
          <div class="ai-extra-card ai-extra-card--collect" @click="handleCollectFeature">
            <div class="ai-extra-icon">💡</div>
            <div class="ai-extra-title">征集有趣功能</div>
            <div class="ai-extra-desc">告诉我们你想玩的功能</div>
          </div>
        </div>
        <button class="ai-extra-arrow ai-extra-arrow--right" type="button" aria-label="下一组功能" @click="scrollExtra(1)">›</button>
      </div>

      <div v-if="activeExtraPanel === 'rhyme'" class="ai-extra-panel">
        <div class="ai-extra-panel-header">
          <span class="ai-extra-panel-title">🎵 韵脚查询</span>
          <button class="ai-extra-panel-close" @click="activeExtraPanel = null">&times;</button>
        </div>
        <div class="rhyme-search-bar">
          <div class="rhyme-search-type">
            <button class="rhyme-type-btn" :class="{ active: rhymeSearchType === 'character' }" @click="rhymeSearchType = 'character'">按汉字查</button>
            <button class="rhyme-type-btn" :class="{ active: rhymeSearchType === 'group' }" @click="rhymeSearchType = 'group'">按韵部查</button>
          </div>
          <div class="rhyme-search-input">
            <input
              v-model="rhymeInput"
              :placeholder="rhymeSearchType === 'character' ? '输入一个汉字，如：风' : '输入韵部名称，如：东'"
              maxlength="10"
              @keyup.enter="handleRhymeQuery"
            />
            <button class="ai-btn-primary rhyme-query-btn" :disabled="rhymeLoading" @click="handleRhymeQuery">
              <span v-if="rhymeLoading" class="rhyme-btn-spinner"></span>
              {{ rhymeLoading ? '查询中...' : '查询' }}
            </button>
          </div>
        </div>
        <div v-if="rhymeLoading" class="rhyme-loading">
          <span class="rhyme-spinner"></span>
          <span class="rhyme-loading-text">正在查询，请稍候…</span>
        </div>
        <div v-if="rhymeResults.length > 0" class="rhyme-results">
          <div class="rhyme-result-header">
            <span class="rhyme-result-char">{{ rhymeResults[0].character }}</span>
            <span class="rhyme-result-info">
              属于「<strong>{{ rhymeResults[0].rhymeGroup }}</strong>」韵 · {{ rhymeResults[0].toneType }} · {{ rhymeResults[0].rhymeCategory }}
            </span>
          </div>
          <div class="rhyme-same-group" v-if="rhymeSearchType === 'character'">
            <div class="rhyme-same-title">同韵字：</div>
            <div class="rhyme-char-grid">
              <span v-for="item in rhymeResults" :key="item.id" class="rhyme-char-tag" :class="item.toneType">{{ item.character }}</span>
            </div>
          </div>
          <div v-if="rhymeSearchType === 'group'" class="rhyme-char-grid">
            <span v-for="item in rhymeResults" :key="item.id" class="rhyme-char-tag" :class="item.toneType">{{ item.character }}</span>
          </div>
        </div>
      </div>

      <div v-if="activeExtraPanel === 'couplet'" class="ai-extra-panel">
        <div class="ai-extra-panel-header">
          <span class="ai-extra-panel-title">🏮 AI 对对联</span>
          <button class="ai-extra-panel-close" @click="activeExtraPanel = null">&times;</button>
        </div>
        <div class="couplet-example-tags">
          <span class="couplet-example-label">示例上联：</span>
          <button v-for="ex in exampleCouplets" :key="ex" class="couplet-example-tag" @click="handleCoupletMatch(ex)">{{ ex }}</button>
        </div>
        <div class="couplet-input-area">
          <input
            v-model="coupletInput"
            placeholder="请输入上联，如：春风得意马蹄疾"
            @keyup.enter="handleCoupletMatch()"
          />
          <button class="ai-btn-primary" :disabled="coupletLoading" @click="handleCoupletMatch()">
            {{ coupletLoading ? 'AI 思考中...' : '对下联' }}
          </button>
        </div>
        <div v-if="coupletResult" class="ai-result-card couplet-result-card">
          <div class="couplet-display">
            <div class="couplet-upper">上联：{{ coupletInput }}</div>
            <div class="couplet-divider">—</div>
            <div class="couplet-lower">{{ coupletResult }}</div>
          </div>
        </div>
      </div>

      <div v-if="activeExtraPanel === 'acrostic'" class="ai-extra-panel">
        <div class="ai-extra-panel-header">
          <span class="ai-extra-panel-title">🖋️ 藏头诗</span>
          <button class="ai-extra-panel-close" @click="activeExtraPanel = null">&times;</button>
        </div>
        <div class="couplet-example-tags">
          <span class="couplet-example-label">示例藏头字：</span>
          <button v-for="ex in acrosticExamples" :key="ex" class="couplet-example-tag" @click="handleAcrosticGenerate(ex)">{{ ex }}</button>
        </div>
        <div class="rhyme-search-type" style="margin-bottom:10px;">
          <button class="rhyme-type-btn" :class="{ active: acrosticLength === 5 }" @click="acrosticLength = 5">五言</button>
          <button class="rhyme-type-btn" :class="{ active: acrosticLength === 7 }" @click="acrosticLength = 7">七言</button>
        </div>
        <div class="couplet-input-area">
          <input
            v-model="acrosticInput"
            placeholder="输入 2-8 个藏头字，如：墨渊"
            maxlength="8"
            @keyup.enter="handleAcrosticGenerate()"
          />
          <button class="ai-btn-primary ai-tool-btn" :disabled="acrosticLoading" @click="handleAcrosticGenerate()">
            <span v-if="acrosticLoading" class="rhyme-btn-spinner"></span>
            {{ acrosticLoading ? '生成中...' : '生成藏头诗' }}
          </button>
        </div>
        <div v-if="acrosticLoading" class="rhyme-loading">
          <span class="rhyme-spinner"></span>
          <span class="rhyme-loading-text">AI 正在作诗，请稍候…</span>
        </div>
        <div v-else-if="acrosticResult" class="ai-result-card acrostic-result-card">
          <div class="acrostic-lines">
            <div v-for="(line, idx) in acrosticLines" :key="idx" class="acrostic-line">
              <span class="acrostic-head">{{ line.head }}</span><span class="acrostic-rest">{{ line.rest }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="activeExtraPanel === 'meter'" class="ai-extra-panel">
        <div class="ai-extra-panel-header">
          <span class="ai-extra-panel-title">🧭 平仄校验</span>
          <button class="ai-extra-panel-close" @click="activeExtraPanel = null">&times;</button>
        </div>
        <p class="ai-explore-hint" style="margin-bottom:12px;">粘贴一首绝句/律诗（每句一行），AI 校验平仄、押韵与格律</p>
        <textarea
          v-model="meterInput"
          class="meter-textarea"
          rows="4"
          placeholder="如：&#10;两个黄鹂鸣翠柳&#10;一行白鹭上青天"
        ></textarea>
        <div class="meter-actions">
          <button class="ai-btn-primary ai-tool-btn" :disabled="meterLoading" @click="handleMeterCheck">
            <span v-if="meterLoading" class="rhyme-btn-spinner"></span>
            {{ meterLoading ? '校验中...' : '校验格律' }}
          </button>
        </div>
        <div v-if="meterLoading" class="rhyme-loading">
          <span class="rhyme-spinner"></span>
          <span class="rhyme-loading-text">AI 正在校验格律，请稍候…</span>
        </div>
        <div v-else-if="meterResult" class="ai-result-card meter-result-card">
          <div class="meter-head">
            <span class="meter-badge" :class="meterResult.valid ? 'is-valid' : 'is-invalid'">
              {{ meterResult.valid ? '基本合律' : '存在不合律处' }}
            </span>
            <span v-if="meterResult.format" class="meter-format">{{ meterResult.format }}</span>
            <span v-if="meterResult.rhymeGroup" class="meter-rhyme">押：{{ meterResult.rhymeGroup }}</span>
          </div>
          <div class="meter-lines">
            <div v-for="(line, idx) in meterRenderLines" :key="idx" class="meter-line">
              <div class="meter-line-text">{{ line.text }}</div>
              <div class="meter-line-tone">
                <span
                  v-for="(t, ti) in line.toneChars"
                  :key="ti"
                  class="meter-tone"
                  :class="t === '平' ? 'is-ping' : 'is-ze'"
                >{{ t }}</span>
                <span v-if="line.rhyme" class="meter-rhyme-tag">押韵</span>
              </div>
              <div v-if="line.note" class="meter-line-note">{{ line.note }}</div>
            </div>
          </div>
          <div v-if="meterResult.summary" class="meter-summary">{{ meterResult.summary }}</div>
        </div>
        <div v-else-if="meterRaw" class="ai-result-card meter-raw">{{ meterRaw }}</div>
      </div>

      <div v-if="activeExtraPanel === 'feihua'" class="ai-extra-panel">
        <div class="ai-extra-panel-header">
          <span class="ai-extra-panel-title">🌺 飞花令 / 接龙</span>
          <button class="ai-extra-panel-close" @click="activeExtraPanel = null">&times;</button>
        </div>
        <div class="rhyme-search-type" style="margin-bottom:10px;">
          <button class="rhyme-type-btn" :class="{ active: feihuaMode === 'keyword' }" @click="feihuaMode = 'keyword'">飞花令</button>
          <button class="rhyme-type-btn" :class="{ active: feihuaMode === 'chain' }" @click="feihuaMode = 'chain'">诗词接龙</button>
        </div>
        <div class="couplet-input-area">
          <input
            v-model="feihuaInput"
            :placeholder="feihuaMode === 'keyword' ? '输入一个令字，如：月' : '输入上一句，如：举头望明月'"
            maxlength="30"
            @keyup.enter="handleFeihua"
          />
          <button class="ai-btn-primary ai-tool-btn" :disabled="feihuaLoading" @click="handleFeihua">
            <span v-if="feihuaLoading" class="rhyme-btn-spinner"></span>
            {{ feihuaLoading ? '思考中...' : feihuaBtnText }}
          </button>
        </div>
        <div v-if="feihuaLoading" class="rhyme-loading">
          <span class="rhyme-spinner"></span>
          <span class="rhyme-loading-text">AI 正在检索诗句，请稍候…</span>
        </div>
        <div v-else-if="feihuaLines.length" class="ai-result-card feihua-result-card">
          <div v-for="(line, idx) in feihuaLines" :key="idx" class="feihua-line">{{ line }}</div>
        </div>
      </div>

      <div v-if="activeExtraPanel === 'ocr'" class="ai-extra-panel">
        <div class="ai-extra-panel-header">
          <span class="ai-extra-panel-title">📷 古籍识文</span>
          <button class="ai-extra-panel-close" @click="activeExtraPanel = null">&times;</button>
        </div>
        <p class="ai-explore-hint" style="margin-bottom:12px;">上传古诗词图片、书法作品或碑帖，AI 将精准识别其中的文字</p>
        <div class="ai-upload-zone" style="margin-bottom:12px;" @click="ocrUploadRef?.click()" @dragover.prevent="handleOcrDragOver" @dragleave="handleOcrDragLeave" @drop.prevent="handleOcrDrop">
          <span class="ai-upload-emoji">📷</span>
          <p class="ai-upload-text">点击上传图片，或拖拽至此处</p>
          <p class="ai-upload-hint">支持 JPG / PNG / GIF / WebP，单图 ≤10MB</p>
          <input ref="ocrUploadRef" type="file" accept="image/jpeg,image/png,image/gif,image/webp" style="display:none" @change="handleOcrUpload">
        </div>
        <div v-if="ocrPreview" class="ai-preview-area" style="margin-bottom:12px;">
          <img :src="ocrPreview" alt="预览图片">
          <div class="ai-preview-actions">
            <button class="ai-btn-primary" @click="handleOcrRecognize" :disabled="ocrLoading">
              {{ ocrLoading ? 'AI识别中...' : '🔍 开始识别' }}
            </button>
            <button class="ai-btn-outline" @click="clearOcrImage">重新选择</button>
          </div>
        </div>
        <div v-if="ocrResult" class="ai-result-card">
          <div class="ai-analysis-label">📖 识别结果</div>
          <div class="ai-analysis-text" style="white-space:pre-wrap;">{{ ocrResult }}</div>
        </div>
      </div>
    </div>

    <div class="z_right_container">
      <div class="z_right">
        <div class="vision-header">
          <h4>诗话视野</h4>
        </div>
        <template v-for="(item, index) in visionArticles" :key="'va-' + item.id">
          <div class="vision-item" @click="router.push(`/vision/${item.id}`)">
            <p class="vision-title">---{{ (visionCurrentPage - 1) * visionPageSize + index + 1 }}. "{{ item.title }}"</p>
            <p v-if="item.summary" class="vision-summary">{{ item.summary }}</p>
            <div class="vision-meta">
              <span v-if="item.author" class="vision-author">{{ item.author }}</span>
              <span v-if="item.category" class="vision-category">{{ item.category }}</span>
              <span class="vision-views">{{ item.viewCount }} 阅读</span>
              <span class="vision-likes">{{ item.likeCount }} 点赞</span>
            </div>
            <a class="shsy_gdxq" @click.stop="router.push(`/vision/${item.id}`)">&lt;查看详情&gt;</a>
          </div>
        </template>
        <div class="vision-pagination" v-if="visionTotal > visionPageSize">
          <el-pagination
            size="small"
            layout="prev, pager, next"
            :total="visionTotal"
            :page-size="visionPageSize"
            :current-page="visionCurrentPage"
            @current-change="handleVisionPageChange"
          />
        </div>
        <hr>
        <el-link type="primary" class="shsy_gdxq" @click="router.push('/vision')">
          &lt;查看全部&gt;
        </el-link>
      </div>
    </div>

    <img class="zhuang_s8" :src="asset('/img/yiji_tuobiao_5.png')" alt="">
    <img class="zhuang_s10" :src="asset('/img/tuobiao_2.png')" alt="">
    <div class="forum_poem_layout">
      <ForumPreview :posts="hotPosts" />

      <div
        class="poem_selection_container"
        :class="panelFocusClass"
        :style="panelStyle"
        @mouseover="handlePanelOver"
        @mouseleave="handlePanelLeave"
        @click="handlePanelClick"
      >
        <AncientPoemSelection />

        <ContemporaryPoems />
      </div>
    </div>
  </div>


</template>

<style lang="scss">
@use './home-styles.scss' as *;
@use './home-responsive.scss' as *;

.carousel-wrapper {
  margin: 0 auto;
  width: 80%;
  height: 620px;
  position: relative;
  top: 5px;
  margin-bottom: 10px;
  border-radius: 20px;
  overflow: visible;
}

.carousel-wrapper .sy_top_daohang_biao {
  position: absolute;
  z-index: 10;
}
</style>
