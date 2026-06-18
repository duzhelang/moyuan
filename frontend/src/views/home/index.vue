<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, onActivated, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getForumPostList, getComments, createComment, likeComment } from '@/api/modules/forum'
import { likePoem, favoritePoem } from '@/api/modules/poem'
import { writePoemFromImage, analyzePoem, matchCouplet } from '@/api/modules/ai'
import { queryRhymeByCharacter, queryRhymeByGroup } from '@/api/modules/rhyme'
import type { RhymeItem } from '@/api/modules/rhyme'
import { getRandomPoetFeatured } from '@/api/modules/poetFeatured'
import type { PoetFeatured } from '@/api/modules/poetFeatured'
import { getVisionArticleList } from '@/api/modules/visionArticle'
import { searchPoemsWithRecommend, getRecommendedPoems } from '@/api/modules/external-poetry'
import type { PoemSearchResult } from '@/api/modules/external-poetry'
import { ElMessage } from 'element-plus'
import type { ForumPost, VisionArticle, Comment } from '@/types/model'
import { useUserStore } from '@/stores/user'
import PoetCard from '@/components/business/PoetCard.vue'
import HomeNavigation from '@/components/business/HomeNavigation.vue'
import DailyPoetry from '@/components/business/DailyPoetry.vue'
import QrCodeLink from '@/components/common/QrCodeLink.vue'
import PoetryDetailDialog from '@/components/business/PoetryDetailDialog.vue'
import HomeNavBar from '@/components/business/HomeNavBar.vue'

const NAV_ANIMATION_KEY = 'home_nav_animation_config'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const hotPosts = ref<ForumPost[]>([])

const expandedPoemIdx = ref<string | null>(null)
const poemComments = ref<Record<string, Comment[]>>({})
const commentInputs = ref<Record<string, string>>({})
const submittingComment = ref<Record<string, boolean>>({})
const commentTotals = ref<Record<string, number>>({})

const poemLikeStates = ref<Record<string, boolean>>({})
const poemFavoriteStates = ref<Record<string, boolean>>({})
const poemLikeCounts = ref<Record<string, number>>({})
const poemFavoriteCounts = ref<Record<string, number>>({})

const handlePoemLike = async (poemIdx: string) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/user/login')
    return
  }
  try {
    await likePoem(Number(poemIdx))
    poemLikeStates.value[poemIdx] = !poemLikeStates.value[poemIdx]
    poemLikeCounts.value[poemIdx] = (poemLikeCounts.value[poemIdx] || 0) + (poemLikeStates.value[poemIdx] ? 1 : -1)
    ElMessage.success(poemLikeStates.value[poemIdx] ? '点赞成功' : '已取消点赞')
  } catch {
    ElMessage.error('操作失败')
  }
}

const handlePoemFavorite = async (poemIdx: string) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/user/login')
    return
  }
  try {
    await favoritePoem(Number(poemIdx))
    poemFavoriteStates.value[poemIdx] = !poemFavoriteStates.value[poemIdx]
    poemFavoriteCounts.value[poemIdx] = (poemFavoriteCounts.value[poemIdx] || 0) + (poemFavoriteStates.value[poemIdx] ? 1 : -1)
    ElMessage.success(poemFavoriteStates.value[poemIdx] ? '收藏成功' : '已取消收藏')
  } catch {
    ElMessage.error('操作失败')
  }
}

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

const analyzeExamples = [
  { text: '床前明月光，疑是地上霜。', label: '🌙 静夜思' },
  { text: '大漠孤烟直，长河落日圆。', label: '🏜️ 使至塞上' },
  { text: '落霞与孤鹜齐飞，秋水共长天一色。', label: '🕊️ 滕王阁序' },
  { text: '人生若只如初见，何事秋风悲画扇。', label: '🍂 纳兰词' }
]

interface PoetryItem {
  title: string
  author: string
  poem: string
  info: string
}

const poetryLibrary: PoetryItem[] = [
  { title: '静夜思', author: '李白', poem: '床前明月光，\n疑是地上霜。\n举头望明月，\n低头思故乡。', info: '唐·李白 — 语言清新朴素，意蕴深远，是思乡诗中的千古绝唱。' },
  { title: '春晓', author: '孟浩然', poem: '春眠不觉晓，\n处处闻啼鸟。\n夜来风雨声，\n花落知多少。', info: '唐·孟浩然 — 以听觉写春景，惜春之情溢于言表。' },
  { title: '登鹳雀楼', author: '王之涣', poem: '白日依山尽，\n黄河入海流。\n欲穷千里目，\n更上一层楼。', info: '唐·王之涣 — 气势磅礴，蕴含积极向上的哲理。' },
  { title: '江雪', author: '柳宗元', poem: '千山鸟飞绝，\n万径人踪灭。\n孤舟蓑笠翁，\n独钓寒江雪。', info: '唐·柳宗元 — 以极简笔墨营造孤绝意境，遗世独立。' },
  { title: '相思', author: '王维', poem: '红豆生南国，\n春来发几枝。\n愿君多采撷，\n此物最相思。', info: '唐·王维 — 借红豆寄寓深情，语浅而情浓。' },
  { title: '枫桥夜泊', author: '张继', poem: '月落乌啼霜满天，\n江枫渔火对愁眠。\n姑苏城外寒山寺，\n夜半钟声到客船。', info: '唐·张继 — 声色交织，愁绪弥漫，千古传诵的名篇。' }
]

const explorePoem = ref('')
const exploreInfo = ref('')

const loadRandomPoem = () => {
  const pick = poetryLibrary[Math.floor(Math.random() * poetryLibrary.length)]
  explorePoem.value = pick.poem
  exploreInfo.value = `《${pick.title}》— ${pick.info}`
}

loadRandomPoem()

const activeExtraPanel = ref<'rhyme' | 'couplet' | null>(null)

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

const exampleCouplets = ['春风得意马蹄疾', '海内存知己', '大漠孤烟直', '明月松间照']

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
    const res = await matchCouplet({ upperCouplet: input })
    coupletResult.value = res.data.result
  } catch {
    coupletResult.value = '抱歉，AI服务暂时不可用，请稍后重试。'
  } finally {
    coupletLoading.value = false
  }
}

const currentSlide = ref(1)
const maxSlides = 6
const currentShiwen = ref(1)
const maxShiwen = 7
const showRightTools = ref(false)
const isNavFixed = ref(false)
const ltTimer = ref<ReturnType<typeof setInterval> | null>(null)
const shiwenTimer = ref<ReturnType<typeof setInterval> | null>(null)
const clockTimer = ref<ReturnType<typeof setInterval> | null>(null)
const currentTime = ref('')

const imageUploadRef = ref<HTMLInputElement | null>(null)

const asset = (path: string) => path

const POET_VISIBLE_COUNT = 4
const PLACEHOLDER_POET: PoetFeatured = {
  id: -1, poetId: null, name: '敬请期待', dynasty: '',
  description: '更多诗人正在收录中...', biography: '',
  imageUrl: '', sortOrder: 0, status: 1
}

const poetsData = ref<PoetFeatured[]>([])
const poetPool = ref<PoetFeatured[]>([])
const displayedPoetIds = ref<Set<number>>(new Set())
const poetRefreshTimer = ref<ReturnType<typeof setInterval> | null>(null)
const visionArticles = ref<VisionArticle[]>([])
const visionCurrentPage = ref(1)
const visionPageSize = ref(5)
const visionTotal = ref(0)

interface PoemCard {
  date: string
  title?: string
  content: string
  author: string
  image?: string
}

const ancientPoems: PoemCard[] = [
  { date: '唐代', title: '静夜思', content: '床前明月光，疑是地上霜。\n举头望明月，低头思故乡。', author: '李白', image: '/img/lt_jx (1).jpg' },
  { date: '唐代', title: '登鹳雀楼', content: '白日依山尽，黄河入海流。\n欲穷千里目，更上一层楼。', author: '王之涣', image: '/img/lt_jx (2).jpg' },
  { date: '唐代', title: '春晓', content: '春眠不觉晓，处处闻啼鸟。\n夜来风雨声，花落知多少。', author: '孟浩然', image: '/img/lt_jx (3).jpg' },
  { date: '唐代', title: '悯农', content: '锄禾日当午，汗滴禾下土。\n谁知盘中餐，粒粒皆辛苦。', author: '李绅', image: '/img/lt_jx (4).jpg' },
  { date: '唐代', title: '望庐山瀑布', content: '日照香炉生紫烟，遥看瀑布挂前川。\n飞流直下三千尺，疑是银河落九天。', author: '李白', image: '/img/lt_jx (1).jpg' },
  { date: '宋代', title: '水调歌头', content: '明月几时有？把酒问青天。\n不知天上宫阙，今夕是何年。', author: '苏轼', image: '/img/lt_jx (2).jpg' },
  { date: '唐代', title: '黄鹤楼送孟浩然之广陵', content: '故人西辞黄鹤楼，烟花三月下扬州。\n孤帆远影碧空尽，唯见长江天际流。', author: '李白', image: '/img/lt_jx (3).jpg' },
  { date: '唐代', title: '相思', content: '红豆生南国，春来发几枝。\n愿君多采撷，此物最相思。', author: '王维', image: '/img/lt_jx (4).jpg' },
  { date: '宋代', title: '念奴娇·赤壁怀古', content: '大江东去，浪淘尽，千古风流人物。\n故垒西边，人道是，三国周郎赤壁。', author: '苏轼', image: '/img/lt_jx (1).jpg' },
  { date: '唐代', title: '将进酒', content: '君不见，黄河之水天上来，奔流到海不复回。\n君不见，高堂明镜悲白发，朝如青丝暮成雪。', author: '李白', image: '/img/lt_jx (2).jpg' }
]

// 历史的印痕搜索相关状态
const poemSearchKeyword = ref('')
const poemSearchResults = ref<PoemSearchResult[]>([])
const poemSearchLoading = ref(false)
const poemSearchMode = ref<'default' | 'search' | 'recommend'>('default')
const recommendedPoems = ref<PoemSearchResult[]>([])

// 搜索诗词
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

// 获取推荐诗词
const fetchRecommendedPoems = async () => {
  try {
    recommendedPoems.value = await getRecommendedPoems(6)
  } catch (error) {
    console.error('获取推荐诗词失败:', error)
  }
}

// 切换到推荐模式
const showRecommendedPoems = async () => {
  poemSearchMode.value = 'recommend'
  if (recommendedPoems.value.length === 0) {
    await fetchRecommendedPoems()
  }
}

// 切换回默认模式
const showDefaultPoems = () => {
  poemSearchMode.value = 'default'
  poemSearchKeyword.value = ''
  poemSearchResults.value = []
}

// 清空搜索
const clearPoemSearch = () => {
  poemSearchKeyword.value = ''
  poemSearchResults.value = []
  poemSearchMode.value = 'default'
}

// 诗词鉴赏弹窗
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

const contemporaryPoems: PoemCard[] = [
  { date: '2019年6月24日', content: '愿如花已落千行，未闻花语浅别殇。幽僻心境漫过少，唯有诗语解锁缰。金甲未脱抬眼望，怒剑难收向疆场。笑祝功成人与事，再鼓一旬又何妨。', author: '常平逼王', image: '/img/lt_jx (4).jpg' },
  { date: '2019年6月24日', content: '愿如花已落千行，未闻花语浅别殇。幽僻心境漫过少，唯有诗语解锁缰。金甲未脱抬眼望，怒剑难收向疆场。笑祝功成人与事，再鼓一旬又何妨。', author: '常平逼王', image: '/img/lt_jx (1).jpg' },
  { date: '2018年9月1日', title: '西江月.未语', content: '华灯初照白路，夜来树影凄楚，俳徊只与月相伴，茫然一片迷雾。离人兀自心扬，未语难避哀伤，只若通明过后，真心但化霓裳。', author: '常平逼王', image: '/img/lt_jx (2).jpg' },
  { date: '2018年9月24日', title: '忆江南.哀', content: '秋风寒，落红自凋残。未识伊人愁几许，伊人兀自笑颜开。吾心不胜哀！', author: '常平逼王', image: '/img/lt_jx (3).jpg' },
  { date: '2022年4月5日', content: '东风吹起白蝴蝶，散入云幕尽青烟。但觉离魂归来少，春日暖暖艳阳天。松柏长青久可立，香印再燃意更坚。思情尽付潇湘处，绵远流长年复年。', author: '常平逼王', image: '/img/lt_jx (4).jpg' },
  { date: '2018年4月22日', title: '清平乐.梦怀', content: '雨落窗外，淅声渐消潺，一帘凝愁入梦来，若雾浸湿枕畔。经年离愁之初，再会此情更苦，最是万千惆怅，与白共登天目！', author: '常平逼王', image: '/img/lt_jx (4).jpg' },
  { date: '2018年5月20日', title: '乌夜啼.叹城府', content: '淡雾兀遮明眸，更添愁，独倚玉阑不觉泪空流。人情恶，谁易错，自长酌，一心明月向沟成污浊。', author: '常平逼王', image: '/img/lt_jx (4).jpg' },
  { date: '2019年2月4日', content: '新桃映红把福迎，银烛高照万家兴。火尾连排上清夜，笑语欢声动耳惊。奈何遣词功夫浅，此夜此景休论明。唯愿持酒歌一曲，与君同乐一身轻', author: '常平逼王', image: '/img/lt_jx (4).jpg' },
  { date: '2020年1月25日', content: '回首一九已成空，展望二零佳意浓。世事岂敢轻年少，天意哪可断前程。唐猊又着战意显，三尺重出徙侣从。烂柯沉木枉悲夫，夫且若怡也可容。', author: '常平逼王', image: '/img/lt_jx (4).jpg' },
  { date: '2021年4月3日', content: '沉影迷叠千层障，乱云归处是它乡。酒酣仍识昔日客，心迷难辨眼前芳。纵把凡锦比仙缎，不需经年多思量。一笑即随羊角去，九风还作万华芳。', author: '常平逼王', image: '/img/lt_jx (4).jpg' },
  { date: '2023年1月4日', title: '一剪梅.无题', content: '一别三秋未招摇，山也迢迢，水也昭昭。何人再添新衣袍，笑意盈绕，喜上眉梢。忆昔花开岁月好，蜂字飘摇，蝶字舞蹈。而今方知云未晓，风又飘飘，雨又萧萧。', author: '常平逼王', image: '/img/lt_jx (4).jpg' }
]

const contemporaryPage = ref(1)
const contemporaryPageSize = 3
const contemporaryTotal = computed(() => contemporaryPoems.length)
const paginatedContemporaryPoems = computed(() => {
  const start = (contemporaryPage.value - 1) * contemporaryPageSize
  return contemporaryPoems.slice(start, start + contemporaryPageSize)
})
const handleContemporaryPageChange = (page: number) => {
  contemporaryPage.value = page
}

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

const updateImage = () => {
  const img = document.getElementById('ad_scroll') as HTMLImageElement
  if (img) img.src = `/img/lb_ (${currentSlide.value}).jpg`
  for (let i = 1; i <= maxSlides; i++) {
    const el = document.getElementById('li' + i)
    if (el) el.style.color = currentSlide.value === i ? 'rgb(249,255,0)' : 'black'
  }
}

const nextSlide = () => {
  currentSlide.value = currentSlide.value >= maxSlides ? 1 : currentSlide.value + 1
  updateImage()
}

const prevSlide = () => {
  currentSlide.value = currentSlide.value <= 1 ? maxSlides : currentSlide.value - 1
  updateImage()
}

const goToSlide = (index: number) => {
  currentSlide.value = index
  updateImage()
}

const updateShiwen = () => {
  const img = document.getElementById('ad_scroll_shiwen') as HTMLImageElement
  if (img) img.src = `/img/lb_shiwen (${currentShiwen.value}).png`
  for (let i = 1; i <= maxShiwen; i++) {
    const el = document.getElementById('l' + i)
    if (el) el.style.backgroundColor = currentShiwen.value === i ? 'rgb(249,255,0)' : '#bfbcbc'
  }
}

const goToShiwen = (index: number) => {
  currentShiwen.value = index
  updateShiwen()
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

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const togglePoemComments = async (poemIdx: string, targetType: number) => {
  if (expandedPoemIdx.value === poemIdx) {
    expandedPoemIdx.value = null
    return
  }
  expandedPoemIdx.value = poemIdx
  if (!poemComments.value[poemIdx]) {
    try {
      const res = await getComments(Number(poemIdx), targetType, { pageNum: 1, pageSize: 20 })
      poemComments.value[poemIdx] = res.data.list
      commentTotals.value[poemIdx] = res.data.total
    } catch {
      ElMessage.error('获取评论失败')
    }
  }
}

const handleSubmitPoemComment = async (poemIdx: string, targetType: number) => {
  const content = commentInputs.value[poemIdx]
  if (!content?.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  submittingComment.value[poemIdx] = true
  try {
    await createComment({ content, targetId: Number(poemIdx), targetType })
    ElMessage.success('评论成功')
    commentInputs.value[poemIdx] = ''
    const res = await getComments(Number(poemIdx), targetType, { pageNum: 1, pageSize: 20 })
    poemComments.value[poemIdx] = res.data.list
    commentTotals.value[poemIdx] = res.data.total
  } catch {
    ElMessage.error('评论失败')
  } finally {
    submittingComment.value[poemIdx] = false
  }
}

const handlePoemCommentLike = async (comment: Comment) => {
  try {
    await likeComment(comment.id)
    comment.likeCount++
  } catch {
    ElMessage.error('点赞失败')
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
  if (!file.type.match(/image\/(jpeg|png|webp)/)) {
    ElMessage.warning('请上传 JPG / PNG / WebP 格式的图片')
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
    const res = await writePoemFromImage(imageFile.value, aiModel.value, visionModel.value)
    poemResult.value = res.data.poem
  } catch {
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
    const res = await analyzePoem({ poem: analyzeInput.value, model: aiModel.value })
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
    poemSearchKeyword.value = payload.query
    handlePoemSearch()
    const el = document.querySelector('.poem_selection_left') as HTMLElement
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

onMounted(async () => {
  loadAnimationConfig()
  showTime()
  clockTimer.value = setInterval(showTime, 1000)
  ltTimer.value = setInterval(nextSlide, 4000)
  shiwenTimer.value = setInterval(() => {
    currentShiwen.value = currentShiwen.value >= maxShiwen ? 1 : currentShiwen.value + 1
    updateShiwen()
  }, 6000)
  updateImage()
  updateShiwen()

  window.addEventListener('scroll', handleScroll)

  fetchRandomPoets()
  poetRefreshTimer.value = setInterval(fetchRandomPoets, 20000)

  try {
    const postRes = await getForumPostList({ pageNum: 1, pageSize: 5 }).catch(() => ({ data: { list: [] as ForumPost[] } }))
    hotPosts.value = postRes.data?.list || []
  } catch {
    console.error('首页数据加载失败')
  }

  fetchVisionArticles()
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
  if (ltTimer.value) clearInterval(ltTimer.value)
  if (shiwenTimer.value) clearInterval(shiwenTimer.value)
  if (poetRefreshTimer.value) clearInterval(poetRefreshTimer.value)
  if (clockTimer.value) {
    clearInterval(clockTimer.value)
    clockTimer.value = null
  }
  window.removeEventListener('scroll', handleScroll)
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
          <img class="logo_img" src="/img/tubiao (1).jpg" alt="">
          <span class="logo-txt">墨渊</span>
        </a>
      </div>
      <div class="top_txt">
        <div class="shijian" title="现在时间">{{ currentTime }}</div>
        <QrCodeLink text="了解我们" qrImage="/img/微信二维.jpg" alt="扫一扫查看" />
        <QrCodeLink text="联系我们" qrImage="/img/微信二维.jpg" alt="扫一扫联系" />
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
          <img :src="asset('/img/jianzu (5).png')" alt="">
        </div>
      </div>
    </div>
    <audio id="myAudio" :src="asset('/js/music.mp3')" preload="auto"></audio>

    <div class="lbye">
      <div class="sy_top_daohang_biao">
        <div class="toptub1"><a href="#luntan" title="交流论坛"><img src="/img/top_tubiao4.png" alt="交流论坛"></a></div>
        <div class="toptub2" title="诗词库"><a href="/poem" @click.prevent="router.push('/poem')"><img src="/img/top_tubiao2.png" alt="诗词库"></a></div>
        <div class="toptub3" title="搜索" @click="scrollToSearch"><a href="javascript:void(0)"><img src="/img/top_tubiao1.png" alt="搜索"></a></div>
        <div class="toptub4" :title="(userStore.isLoggedIn && userStore.userInfo) ? '个人中心' : '去登录'" @click="(userStore.isLoggedIn && userStore.userInfo) ? router.push('/user/profile') : router.push('/user/login')"><a href="javascript:void(0)"><img src="/img/top_tubiao3.png" :alt="(userStore.isLoggedIn && userStore.userInfo) ? '个人中心' : '去登录'"></a></div>
      </div>
      <div id="lb_jt_z" class="lb_jiantou_left" @click="prevSlide">
        <img :src="asset('/img/jianzu (3).png')" alt="">
      </div>
      <div id="lb_jt_y" class="lb_jiantou_right" @click="nextSlide">
        <img :src="asset('/img/jianzu (4).png')" alt="">
      </div>
      <img class="lb_zhutu" :src="asset('/img/lb_ (' + currentSlide + ').jpg')" id="ad_scroll" alt="">
      <ul>
        <li
          v-for="i in maxSlides"
          :key="i"
          :id="'li' + i"
          :style="{ color: currentSlide === i ? 'rgb(249,255,0)' : '' }"
          @mouseenter="goToSlide(i)"
        >{{ i }}</li>
      </ul>
      <img class="zhuang_s4_1" :src="asset('/img/gn_ (2).png')" alt="">
    </div>

    <hr class="hr">
    <hr>
    <HomeNavBar ref="navBarRef" :is-nav-fixed="isNavFixed" @search="onNavSearch" />

    <div class="shiwen-wrapper">
      <div class="lbye_shiwen">
        <img class="lb_zhushi" :src="asset('/img/lb_shiwen (' + currentShiwen + ').png')" id="ad_scroll_shiwen" alt="">
        <ul>
          <li
            v-for="i in maxShiwen"
            :key="i"
            class="xubiao"
            :id="'l' + i"
            :style="{ backgroundColor: currentShiwen === i ? 'rgb(249,255,0)' : '' }"
            @mouseenter="goToShiwen(i)"
          ></li>
        </ul>
      </div>
      <img class="zhuang_s1" :src="asset('/img/gn_3.png')" alt="">
      <img class="zhuang_s2" :src="asset('/img/fy_tubiao (6).png')" alt="">
      <img class="zhuang_s5" :src="asset('/img/zichu(1).png')" alt="">
      <img class="zhuang_s7" :src="asset('/img/tuobiao (4).png')" alt="">
      <img class="zhuang_s3" :src="asset('/img/canci.png')" alt="">
      <img class="zhuang_s4" :src="asset('/img/gn_ (2).png')" alt="">
      <img class="zhuang_s4_top" :src="asset('/img/gn_ (2).png')" alt="">
      <img class="zhuang_s9" :src="asset('/img/wz_tubiao (2).png')" alt="">
    </div>

    <hr>
    <DailyPoetry />
    <hr>
    <h1 class="sy_fljx">分类精赏</h1>

    <div class="z_top">
      <div class="h4">
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
      <img class="zhuang_s6" :src="asset('/img/zichu (4).png')" alt="">
      <img class="zhuang_s7" :src="asset('/img/tuobiao (4).png')" alt="">
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
            <p class="ai-upload-hint">支持 JPG / PNG / WebP，AI 将为您赋诗一首</p>
            <input ref="imageUploadRef" type="file" accept="image/jpeg,image/png,image/webp" style="display:none" @change="handleImageUpload">
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

      <div class="ai-extra-features">
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
            <button class="ai-btn-primary" :disabled="rhymeLoading" @click="handleRhymeQuery">
              {{ rhymeLoading ? '查询中...' : '查询' }}
            </button>
          </div>
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

    <img class="zhuang_s8" :src="asset('/img/YIJI_tuobiao (5).png')" alt="">
    <img class="zhuang_s10" :src="asset('/img/tuobiao (2).png')" alt="">
    <div class="forum_poem_layout">
      <div class="luntan" id="luntan">
        <div class="luntan_title">
          <h1>论 坛</h1>
          <h2>沟通 | 分享 | 探索 | 成长</h2>
        </div>
        <div class="luntan_ss">
          <div class="sy_ss_0_lt">
            <input type="text" class="sy_ss_lt" placeholder="搜索你感兴趣的内容">
            <div class="sy_ss_tp_lt">
              <img :src="asset('/img/tuobiao (4).png')" alt="">
            </div>
          </div>
        </div>
        <div class="luntan_gundong">
          <div class="marquee-content">
            论坛是我们心灵的栖息地，在诗词的韵律中找到共鸣，在分享与探索中共同成长！
          </div>
        </div>
        <div class="luntan_anniu">
          <ul>
            <li><router-link to="/forum">首页</router-link></li>
            <li><router-link to="/forum">全部</router-link></li>
            <li><router-link to="/forum">热帖</router-link></li>
            <li><router-link to="/forum">精华</router-link></li>
            <li><router-link to="/forum">畅言</router-link></li>
          </ul>
        </div>
        <div class="luntan_pin_layout">
          <div class="luntan_pin_top">
            <div class="jxh_left">
              <div class="jxh_left_title">
                <span>精选板块</span>
              </div>
              <div class="jxh_left_nr">
                <div class="jxh_left_nr0">
                  <router-link to="/forum">
                    <img :src="asset('/img/lt_jx (1).jpg')" alt="">
                  </router-link>
                </div>
                <div class="jxh_left_nr1">
                  <router-link to="/forum">
                    <span class="jxh_left_nr1_title">古诗词鉴赏</span>
                  </router-link>
                  <span class="jxh_left_nr1_d">古诗词鉴赏是一个深邃而美妙的领域，它带领我们穿越时光的长河，品味那些流传千年的文字之美。</span>
                </div>
              </div>
              <div class="jxh_left_nr">
                <div class="jxh_left_nr0">
                  <router-link to="/forum">
                    <img :src="asset('/img/lt_jx (2).jpg')" alt="">
                  </router-link>
                </div>
                <div class="jxh_left_nr1">
                  <router-link to="/forum">
                    <span class="jxh_left_nr1_title">古诗词分享</span>
                  </router-link>
                  <span class="jxh_left_nr1_d">诗词分享是一座桥梁，连接着古今的心灵，让我们共同品味诗词之美，传承文化之韵。</span>
                </div>
              </div>
              <div class="jxh_left_nr">
                <div class="jxh_left_nr0">
                  <router-link to="/forum">
                    <img :src="asset('/img/lt_jx (3).jpg')" alt="">
                  </router-link>
                </div>
                <div class="jxh_left_nr1">
                  <router-link to="/forum">
                    <span class="jxh_left_nr1_title">新星闪耀</span>
                  </router-link>
                  <span class="jxh_left_nr1_d">新一代青年笔下流淌着千年的韵律，以古风新韵续写着不朽的篇章。</span>
                </div>
              </div>
            </div>
            <div class="jxh_right">
              <div class="jxh_right_title">
                <span>畅言寓所</span>
              </div>
              <div
                v-for="post in hotPosts.slice(0, 3)"
                :key="post.id"
                class="jxh_right_nr"
                @click="router.push(`/forum/${post.id}`)"
              >
                <span class="jxh_right_nr_title">{{ post.title }}</span>
                <span class="jxh_right_nr_d">{{ post.username }} · {{ post.viewCount }}阅读</span>
              </div>
              <div v-if="hotPosts.length === 0" class="jxh_right_nr">
                <span class="jxh_right_nr_title">欢迎来到论坛交流</span>
                <span class="jxh_right_nr_d">暂无帖子</span>
              </div>
              <router-link to="/forum" class="jxh_right_anniu">
                <span>查看更多</span>
              </router-link>
            </div>
          </div>
        </div>
      </div>

      <div class="poem_selection_container">
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
              <!-- 默认模式：显示静态古诗词 -->
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

              <!-- 搜索模式：显示搜索结果 -->
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

              <!-- 推荐模式：显示协同过滤推荐 -->
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

        <div class="poem_selection_right">
          <h2 class="luntan_section_title">当代·精选</h2>
          <div class="luntan_ddjx contemporary_container">
            <div class="luntan_jx_title contemporary_title">时代画像</div>
            <div class="contemporary_scroll">
              <div
                v-for="(poem, idx) in paginatedContemporaryPoems"
                :key="'contemporary-' + ((contemporaryPage - 1) * contemporaryPageSize + idx)"
                class="contemporary_card"
              >
                <div class="contemporary_card_img">
                  <img :src="asset(poem.image ?? '')" :alt="poem.title || '诗词配图'">
                </div>
                <div class="contemporary_card_body">
                  <div class="contemporary_card_date">{{ poem.date }}</div>
                  <div class="contemporary_card_content">
                    <div class="contemporary_card_title">{{ poem.title || '&nbsp;' }}</div>
                    <p>{{ poem.content }}</p>
                  </div>
                  <div class="contemporary_card_footer">
                    <span class="contemporary_card_author">{{ poem.author }}</span>
                    <div class="contemporary_card_actions">
                      <button
                        class="contemporary_action_btn"
                        :class="{ active: poemLikeStates[String((contemporaryPage - 1) * contemporaryPageSize + idx)] }"
                        @click="handlePoemLike(String((contemporaryPage - 1) * contemporaryPageSize + idx))"
                      >
                        <span class="action_icon">&#10084;</span>
                        <span class="action_count">{{ poemLikeCounts[String((contemporaryPage - 1) * contemporaryPageSize + idx)] || '' }}</span>
                      </button>
                      <button
                        class="contemporary_action_btn"
                        :class="{ active: poemFavoriteStates[String((contemporaryPage - 1) * contemporaryPageSize + idx)] }"
                        @click="handlePoemFavorite(String((contemporaryPage - 1) * contemporaryPageSize + idx))"
                      >
                        <span class="action_icon">&#9733;</span>
                      </button>
                      <button
                        class="contemporary_action_btn"
                        @click="togglePoemComments('contemporary-' + ((contemporaryPage - 1) * contemporaryPageSize + idx), 2)"
                      >
                        <span class="action_icon">&#128172;</span>
                        <span class="action_count">{{ commentTotals['contemporary-' + ((contemporaryPage - 1) * contemporaryPageSize + idx)] || '' }}</span>
                      </button>
                    </div>
                  </div>
                </div>
                <div class="contemporary_comment_section" v-if="expandedPoemIdx === 'contemporary-' + ((contemporaryPage - 1) * contemporaryPageSize + idx)">
                  <div class="contemporary_comment_input_bar" v-if="userStore.isLoggedIn">
                    <input
                      v-model="commentInputs['contemporary-' + ((contemporaryPage - 1) * contemporaryPageSize + idx)]"
                      class="contemporary_comment_input"
                      placeholder="写下你的想法..."
                      @keyup.enter="handleSubmitPoemComment('contemporary-' + ((contemporaryPage - 1) * contemporaryPageSize + idx), 2)"
                    />
                    <button
                      class="contemporary_comment_submit"
                      @click="handleSubmitPoemComment('contemporary-' + ((contemporaryPage - 1) * contemporaryPageSize + idx), 2)"
                      :disabled="submittingComment['contemporary-' + ((contemporaryPage - 1) * contemporaryPageSize + idx)]"
                    >
                      {{ submittingComment['contemporary-' + ((contemporaryPage - 1) * contemporaryPageSize + idx)] ? '...' : '发送' }}
                    </button>
                  </div>
                  <div class="contemporary_comment_login" v-else @click="router.push('/user/login')">
                    <span>登录后参与讨论</span>
                  </div>
                  <div class="contemporary_comment_list" v-if="poemComments['contemporary-' + ((contemporaryPage - 1) * contemporaryPageSize + idx)]?.length">
                    <div
                      v-for="comment in poemComments['contemporary-' + ((contemporaryPage - 1) * contemporaryPageSize + idx)]"
                      :key="comment.id"
                      class="contemporary_comment_item"
                    >
                      <div class="contemporary_comment_author">{{ comment.username }}</div>
                      <div class="contemporary_comment_text">{{ comment.content }}</div>
                      <div class="contemporary_comment_footer">
                        <span class="contemporary_comment_time">{{ formatDate(comment.createTime) }}</span>
                        <button class="contemporary_comment_like" @click="handlePoemCommentLike(comment)">
                          &#128077; {{ comment.likeCount || '' }}
                        </button>
                      </div>
                    </div>
                  </div>
                  <div class="contemporary_comment_empty" v-else-if="commentTotals['contemporary-' + ((contemporaryPage - 1) * contemporaryPageSize + idx)] === 0">
                    <span>暂无评论，来发表第一条评论吧</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="contemporary_pagination" v-if="contemporaryTotal > contemporaryPageSize">
              <el-pagination
                size="small"
                layout="prev, pager, next"
                :total="contemporaryTotal"
                :page-size="contemporaryPageSize"
                :current-page="contemporaryPage"
                @current-change="handleContemporaryPageChange"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <PoetryDetailDialog
    v-model:visible="poetryDialogVisible"
    :title="poetryDialogTitle"
    :author="poetryDialogAuthor"
  />
</template>

<style>
.left-pic, .right-pic, .right-1, .left-1, .left-2, .left-3, .right-3,
.left-4, .right-4, .right-2, .right-5, .right-6, .left-5, .right-7,
.right-8, .right-9 {
  display: none;
}

.home-page {
  max-width: 1512.8px;
  width: 100%;
  margin: 0 auto;
  position: relative;
  box-sizing: border-box;
}

.home-page h1,
.home-page h2,
.home-page h4 {
  font-family: cursive;
}

.top {
  width: 100%;
  height: 50px;
  background: url('/img/dt_3.jpg') no-repeat 0px -325px / cover;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.top .logo {
  display: flex;
  align-items: center;
  margin-left: 10px;
}

.top .logo a {
  display: flex;
  align-items: center;
  text-decoration: none;
  transition: transform 0.2s ease;
}

.top .logo a:hover {
  transform: scale(1.03);
}

.top .logo_img {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transition: all 0.2s ease;
}

.top .logo a:hover .logo_img {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  transform: scale(1.06);
}

.logo-txt {
  font-size: 30px;
  color: #2c2c2c;
  margin-left: 8px;
  font-weight: bold;
  font-family: cursive;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.5);
  transition: all 0.2s ease;
}

.top .logo a:hover .logo-txt {
  color: rgba(255, 255, 255, 0.85);
  text-shadow: 0 0 10px rgba(255, 255, 255, 0.4), 1px 1px 3px rgba(0, 0, 0, 0.3);
}

.top_txt {
  display: flex;
  align-items: center;
  margin-right: 20px;
  gap: 12px;
}

.shijian {
  color: #fff;
  background-color: rgba(200, 200, 200, 0.3);
  padding: 4px 14px;
  border-radius: 4px;
  font-size: 14px;
  letter-spacing: 0.5px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.top-link {
  color: #fff !important;
  font-size: 18px;
  font-weight: 600;
  text-decoration: none;
  padding: 4px 14px;
  border-radius: 4px;
  transition: all 0.3s ease;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
  cursor: pointer;
}

.top-link:hover {
  color: #ffd700 !important;
  background-color: rgba(255, 255, 255, 0.15);
  text-shadow: 0 0 8px rgba(255, 215, 0, 0.5);
}

.top-divider {
  color: rgba(255, 255, 255, 0.5);
  font-size: 16px;
  user-select: none;
}

.top-user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.top-user-info:hover {
  background-color: rgba(255, 255, 255, 0.15);
}

.top-user-name {
  color: #fff;
  font-size: 14px;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

html {
  overflow-x: hidden;
}
.home-page .lbye {
  margin: 0 auto;
  width: 80%;
  height: 620px;
  position: relative;
  top: 5px;
  margin-bottom: 10px;
}

.lbye .lb_zhutu {
  width: 100%;
  height: 600px;
  display: block;
  border: 1px solid gainsboro;
  border-radius: 20px;
  position: relative;
  z-index: 30;
  transition: opacity 0.5s ease;
}

.lbye .lb_jiantou_left,
.lbye .lb_jiantou_right {
  background-color: rgba(100, 100, 100, 0.5);
  width: 30px;
  height: 600px;
  position: absolute;
  z-index: 31;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20px;
  cursor: pointer;
  top: 0;
}

.lbye .lb_jiantou_left {
  left: 0;
}

.lbye .lb_jiantou_right {
  right: 0;
}

.lbye .lb_jiantou_left:hover,
.lbye .lb_jiantou_right:hover {
  background-color: rgba(220, 220, 220, 0.6);
  width: 50px;
}

.lbye .lb_jiantou_left img,
.lbye .lb_jiantou_right img {
  width: 30px;
  height: 30px;
}

.lbye ul {
  position: absolute;
  z-index: 35;
  bottom: 30px;
  left: 250px;
  list-style: none;
  display: flex;
}

.lbye ul li {
  cursor: pointer;
  list-style: none;
  float: left;
  background-color: #bfbcbc;
  text-align: center;
  width: 21px;
  margin-right: 10px;
  font-size: 12px;
  border-radius: 50%;
}

.shiwen-wrapper {
  margin: 0 auto;
  width: 100%;
  position: relative;
  height: 420px;
  overflow: visible;
}

.lbye_shiwen {
  margin: 0 auto;
  width: 80%;
  height: 400px;
  position: relative;
  top: 5px;
  overflow: hidden;
}

.lbye_shiwen .lb_zhushi {
  width: 88%;
  height: 380px;
  border: 0px solid gainsboro;
  border-radius: 20px;
  position: relative;
  left: 40px;
  z-index: 30;
  transition: opacity 0.5s ease;
}

.lbye_shiwen ul {
  position: absolute;
  z-index: 35;
  bottom: 30px;
  left: 250px;
  list-style: none;
  display: flex;
}

.lbye_shiwen ul li.xubiao {
  cursor: pointer;
  list-style: none;
  float: left;
  background-color: #bfbcbc;
  text-align: center;
  width: 41px;
  height: 10px;
  margin-right: 10px;
  font-size: 12px;
  border-radius: 50px;
}









.nav-card img {
  border-radius: 5%;
  width: 100%;
  height: 150px;
  object-fit: cover;
  display: block;
  transition: all 0.3s ease;
}

.nav-card img:hover {
  transform: scale(1.05);
  border-radius: 20%;
}



.zhuang_s1 {
  width: 136px;
  height: 235px;
  position: absolute;
  top: 165px;
  left: 60px;
  z-index: 22;
}

.zhuang_s2 {
  width: 200px;
  height: 495px;
  position: absolute;
  top: -100px;
  left: 0;
  z-index: 20;
}

.zhuang_s3 {
  width: 320px;
  height: 900px;
  position: absolute;
  top: 520px;
  right: 30px;
  left: auto;
  z-index: 0;
  transform: rotate(60deg);
  opacity: 0.5;
  filter: drop-shadow(5px 10px 15px rgba(0, 0, 0, 0.15));
  pointer-events: none;
}

.zhuang_s4 {
  width: 100%;
  height: 50px;
  position: absolute;
  top: 370px;
  left: 0;
  z-index: 25;
}

.zhuang_s4_top {
  width: 100%;
  height: 50px;
  position: absolute;
  top: -185px;
  left: 0;
  z-index: 25;
}

.zhuang_s4_1 {
  width: 100%;
  height: 50px;
  position: absolute;
  top: 370px;
  left: 0;
}

.zhuang_s5 {
  width: 332px;
  height: 495px;
  position: absolute;
  top: -85px;
  right: 0;
  left: auto;
  z-index: 21;
}

.zhuang_s6 {
  width: 430px;
  height: 350px;
  position: absolute;
  top: 2600px;
  left: 58%;
  z-index: 1;
  opacity: 0.6;
}

.zhuang_s7 {
  width: 410px;
  height: 306px;
  position: absolute;
  top: 2290px;
  left: 74.4%;
  z-index: 1;
  opacity: 0.6;
}

.zhuang_s8 {
  width: 510px;
  height: 606px;
  position: absolute;
  top: 3400px;
  left: 0;
}

.zhuang_s9 {
  width: 210px;
  height: 406px;
  position: absolute;
  top: 2700px;
  left: 82.6%;
  z-index: 1;
  opacity: 0.6;
  pointer-events: none;
}

.zhuang_s10 {
  width: 510px;
  height: 356px;
  position: absolute;
  top: 4400px;
  left: 5px;
}

.zhuang_s {
  position: relative;
  width: 1500px;
  height: 50px;
  z-index: 20;
}

.zhuang_s11 {
  width: 580px;
  height: 500px;
  position: absolute;
  top: 1891px;
  left: 51.1%;
  z-index: 0;
}

.sy_top_daohang_biao {
  height: 0;
  display: flex;
  position: absolute;
  top: 50px;
  right: 150px;
  z-index: 40;
}

.sy_top_daohang_biao img {
  width: 30px;
  height: 30px;
  margin: 0 10px;
  background-color: rgba(150, 150, 150, 0.6);
  border-radius: 50%;
}

.sy_top_daohang_biao img:hover {
  background-color: rgba(220, 220, 220, 0.8);
}

.youce_fangbiao img {
  width: 50px;
  height: 50px;
  border-radius: 50%;
}

.dibu .d_left img {
  width: 300px;
  height: 300px;
  opacity: 0.5;
}

.luntan_jl .jingxuan_zt img {
  width: 85px;
  height: 85px;
}

#duihua_kuang_yang {
  width: 100%;
  display: block;
  height: 200px;
}

.forum_poem_layout {
  display: flex;
  flex-wrap: wrap;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  margin: 0 auto;
  position: relative;
}

.luntan {
  display: flex;
  flex-wrap: wrap;
  flex-direction: column;
  position: relative;
  width: 60%;
  margin: 0 auto;
  background-color: rgba(80, 80, 80, 0.1);
  border-radius: 15px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  overflow: visible;
  z-index: 1;
}

.luntan_title {
  width: 100%;
  text-align: center;
  padding: 10px 0;
}

.luntan_title h1 {
  font-size: 40px;
  color: #333;
  margin: 10px 0 5px;
}

.luntan_title h2 {
  font-size: 18px;
  color: #666;
  margin: 0;
}

.luntan_ss {
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 10px 0;
}

.sy_ss_0_lt {
  display: flex;
  align-items: center;
  width: 400px;
}

.sy_ss_lt {
  flex: 1;
  height: 30px;
  padding: 0 15px;
  border-radius: 20px 0 0 20px;
  font-size: 14px;
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
  letter-spacing: 0.5px;
  line-height: 1.5;
  background-color: rgba(100, 100, 100, 0.5);
  color: #fff;
  border: none;
}

.sy_ss_lt::-webkit-input-placeholder {
  color: rgba(250, 250, 250, 0.8);
  font-size: 14px;
}

.sy_ss_tp_lt {
  width: 35px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #4CAF50;
  border-radius: 0 20px 20px 0;
  cursor: pointer;
}

.sy_ss_tp_lt img {
  width: 20px;
  height: 20px;
}

.luntan_gundong {
  width: 100%;
  font-size: 18px;
  color: yellow;
  background-color: rgba(100, 100, 100, 0.5);
  padding: 5px 0;
  overflow: hidden;
  white-space: nowrap;
}

.marquee-content {
  display: inline-block;
  animation: marquee-scroll 15s linear infinite;
}

@keyframes marquee-scroll {
  0% {
    transform: translateX(100%);
  }
  100% {
    transform: translateX(-100%);
  }
}

.luntan_anniu {
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 10px 0;
}

.luntan_anniu ul {
  display: flex;
  gap: 10px;
}

.luntan_anniu ul li {
  list-style: none;
}

.luntan_anniu ul li a {
  display: block;
  padding: 8px 20px;
  background-color: rgba(100, 100, 100, 0.75);
  color: wheat;
  border-radius: 5px;
  font-size: 16px;
  text-decoration: none;
  transition: all 0.3s;
}

.luntan_anniu ul li a:hover {
  background-color: rgba(220, 220, 220, 0.8);
  color: #333;
}

.luntan_jxh {
  display: flex;
  width: 100%;
  padding: 15px;
  gap: 20px;
}

.luntan_pin_layout {
  display: flex;
  flex-direction: column;
  width: 100%;
  gap: 20px;
}

.luntan_pin_top {
  display: flex;
  width: 100%;
  padding: 15px;
  gap: 20px;
}

.poem_selection_container {
  display: flex;
  width: 90%;
  margin: 30px auto 0;
  gap: 30px;
  padding: 0;
  box-sizing: border-box;
  overflow: visible;
  z-index: 1;
  justify-content: center;
}

.poem_selection_left {
  flex: 1;
  min-width: 0;
  overflow: visible;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.poem_selection_right {
  flex: 1;
  min-width: 0;
  overflow: visible;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.jxh_left {
  flex: 2;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.jxh_left_title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  padding-bottom: 8px;
  border-bottom: 2px solid #ccc;
}

.jxh_left_nr {
  display: flex;
  gap: 15px;
  padding: 10px;
  border: 1px solid gainsboro;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
}

.jxh_left_nr:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.jxh_left_nr0 img {
  width: 200px;
  height: 150px;
  border-radius: 7px;
  object-fit: cover;
}

.jxh_left_nr1 {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.jxh_left_nr1_title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.jxh_left_nr1_d {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  overflow: hidden;
}

.jxh_right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.jxh_right_title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  padding-bottom: 8px;
  border-bottom: 2px solid #ccc;
}

.jxh_right_nr {
  display: flex;
  flex-direction: column;
  padding: 10px;
  border: 1px solid gainsboro;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.jxh_right_nr:hover {
  background-color: rgba(227, 196, 165, 0.3);
}

.jxh_right_nr_title {
  font-size: 15px;
  color: #333;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.jxh_right_nr_d {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.jxh_right_anniu {
  display: flex;
  justify-content: center;
  margin-top: 10px;
}

.jxh_right_anniu span {
  padding: 8px 20px;
  background-color: #007BFF;
  color: white;
  border-radius: 5px;
  text-decoration: none;
  font-size: 14px;
  transition: background-color 0.3s;
}

.jxh_right_anniu span:hover {
  background-color: #0056b3;
}

.z_right_container {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 30px;
  padding: 20px 0;
  position: relative;
}

.z_right {
  overflow-y: auto;
  height: 600px;
  flex-shrink: 0;
  width: 500px;
  background-image: url('/img/lt_bg6.jpg');
  background-size: 100% 100%;
  background-position: center;
  background-repeat: no-repeat;
  border-radius: 0;
  padding: 20px;
  box-sizing: border-box;
  position: relative;
  z-index: 2;
}

.vision-header {
  position: sticky;
  top: -20px;
  z-index: 10;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0.95) 80%, rgba(255, 255, 255, 0));
  padding: 0 0 12px;
  margin: -20px -20px 12px;
  padding-top: 20px;
  padding-bottom: 16px;
}

.vision-header h4 {
  font-size: 18px;
  font-weight: 700;
  color: #2c1810;
  text-align: center;
  margin: 0;
  padding-bottom: 10px;
  border-bottom: 2px solid rgba(139, 69, 19, 0.3);
  letter-spacing: 4px;
  padding-left: 4px;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.5);
}

.z_right::-webkit-scrollbar {
  width: 8px;
}

.z_right::-webkit-scrollbar-track {
  background: rgba(139, 69, 19, 0.1);
  border-radius: 4px;
}

.z_right::-webkit-scrollbar-thumb {
  background: rgba(139, 69, 19, 0.4);
  border-radius: 4px;
}

.z_right::-webkit-scrollbar-thumb:hover {
  background: rgba(139, 69, 19, 0.6);
}

.z_right::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.7);
  border-radius: 0;
  z-index: -1;
}

.vision-item {
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.2s ease;
  border-radius: 6px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(5px);
}

.vision-item:hover {
  background: rgba(255, 255, 255, 0.6);
  transform: translateX(4px);
}

.vision-title {
  font-size: 14px;
  color: #2c1810;
  font-weight: 600;
  margin: 0 0 6px;
  line-height: 1.5;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.5);
}

.vision-summary {
  font-size: 12px;
  color: #4a3728;
  margin: 0 0 6px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.vision-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 11px;
  color: #6b5744;
  margin: 6px 0 4px;
}

.vision-author {
  color: #8b4513;
  font-weight: 600;
}

.vision-category {
  color: #556b2f;
  background: rgba(85, 107, 47, 0.15);
  padding: 2px 8px;
  border-radius: 3px;
  font-weight: 500;
}

.vision-views,
.vision-likes {
  color: #7a6b5d;
}

.vision-pagination {
  display: flex;
  justify-content: center;
  margin: 12px 0;
}

.vision-pagination .el-pagination {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #4a3728;
  --el-pagination-button-bg-color: rgba(255, 255, 255, 0.4);
  --el-pagination-button-color: #4a3728;
  --el-pagination-hover-color: #8b4513;
}

.ai-container {
  width: 100%;
  max-width: 880px;
  margin: 0 auto;
  padding: 30px 28px 24px;
  position: relative;
  z-index: 10;
  border-radius: 20px;
  box-shadow: 0 4px 30px rgba(44, 36, 22, 0.08), 0 20px 60px rgba(44, 36, 22, 0.16);
  border: 1.5px solid #d9cebf;
  background-image:
    linear-gradient(180deg, rgba(253, 250, 243, 0.55) 0%, rgba(245, 240, 232, 0.55) 100%),
    linear-gradient(0deg, transparent 24%, rgba(180, 160, 130, 0.03) 25%, rgba(180, 160, 130, 0.03) 26%, transparent 27%, transparent 74%, rgba(180, 160, 130, 0.03) 75%, rgba(180, 160, 130, 0.03) 76%, transparent 77%),
    linear-gradient(90deg, transparent 24%, rgba(180, 160, 130, 0.02) 25%, rgba(180, 160, 130, 0.02) 26%, transparent 27%, transparent 74%, rgba(180, 160, 130, 0.02) 75%, rgba(180, 160, 130, 0.02) 76%, transparent 77%);
  background-size: 100% 100%, 60px 60px, 60px 60px;
  backdrop-filter: blur(8px);
}

.ai-container::before {
  content: '';
  position: absolute;
  top: 14px;
  left: 50%;
  transform: translateX(-50%);
  width: 70%;
  height: 1px;
  background: linear-gradient(90deg, transparent, #8b7355, #8b7355, transparent);
  opacity: 0.3;
  border-radius: 1px;
}

.ai-header {
  text-align: center;
  margin-bottom: 24px;
  position: relative;
}

.ai-header-icon {
  display: inline-block;
  width: 52px;
  height: 52px;
  background: linear-gradient(135deg, #fdfaf3 0%, #f5ede0 100%);
  border-radius: 50%;
  border: 2px solid #d9cebf;
  box-shadow: 0 3px 14px rgba(44, 36, 22, 0.08);
  margin-bottom: 8px;
  line-height: 52px;
  font-size: 28px;
  position: relative;
}

.ai-header-icon::after {
  content: '';
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  border: 1px dashed #d9cebf;
  opacity: 0.5;
  animation: aiRotateDashed 20s linear infinite;
}

@keyframes aiRotateDashed {
  to { transform: rotate(360deg); }
}

.ai-header-title {
  font-family: 'STKaiti', 'KaiTi', 'Ma Shan Zheng', 'Noto Serif SC', serif;
  font-size: 2em;
  color: #2c2416;
  letter-spacing: 0.06em;
  font-weight: 400;
  margin: 4px 0 0;
  position: relative;
  display: inline-block;
}

.ai-header-title::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  width: 60%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #b8860b, transparent);
  border-radius: 2px;
}

.ai-seal {
  display: inline-block;
  width: 36px;
  height: 36px;
  border: 2px solid #c0392b;
  border-radius: 6px;
  color: #c0392b;
  font-family: 'STKaiti', 'KaiTi', 'Ma Shan Zheng', 'Noto Serif SC', serif;
  font-size: 0.7em;
  line-height: 32px;
  text-align: center;
  transform: rotate(-6deg);
  margin-left: 8px;
  opacity: 0.75;
  vertical-align: middle;
  position: relative;
  top: -2px;
}

.ai-subtitle {
  display: block;
  font-size: 0.85em;
  color: #8b7355;
  letter-spacing: 0.08em;
  margin-top: 8px;
  font-family: 'STSong', 'Noto Serif SC', 'Songti SC', 'SimSun', 'PingFang SC', 'Microsoft YaHei', serif;
}

.ai-tab-nav {
  display: flex;
  gap: 6px;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 20px;
  position: relative;
  z-index: 2;
}

.ai-tab-btn {
  padding: 10px 22px;
  border-radius: 30px;
  border: 1.5px solid #d9cebf;
  background: #fdfaf3;
  cursor: pointer;
  font-family: 'STSong', 'Noto Serif SC', 'Songti SC', 'SimSun', 'PingFang SC', 'Microsoft YaHei', serif;
  font-size: 0.95em;
  color: #5c4a3a;
  letter-spacing: 0.04em;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  white-space: nowrap;
  user-select: none;
}

.ai-tab-btn:hover {
  border-color: #b8860b;
  color: #2c2416;
  box-shadow: 0 3px 14px rgba(44, 36, 22, 0.08);
  transform: translateY(-1px);
}

.ai-tab-btn.active {
  background: linear-gradient(135deg, #3a2f20 0%, #2c2416 100%);
  color: #f5ecd7;
  border-color: transparent;
  box-shadow: 0 4px 18px rgba(44, 36, 22, 0.3);
  font-weight: 500;
  letter-spacing: 0.05em;
}

.ai-tab-icon {
  margin-right: 4px;
  font-size: 1.05em;
}

.ai-panels {
  position: relative;
  min-height: 200px;
}

.ai-panel {
  animation: aiFadeSlideIn 0.45s ease;
}

@keyframes aiFadeSlideIn {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.ai-upload-zone {
  border: 2px dashed #d9cebf;
  border-radius: 20px;
  padding: 36px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  background: linear-gradient(180deg, rgba(245, 240, 230, 0.5) 0%, rgba(253, 250, 243, 0.3) 100%);
  overflow: hidden;
}

.ai-upload-zone:hover {
  border-color: #b8860b;
  background: linear-gradient(180deg, rgba(250, 245, 235, 0.7) 0%, rgba(255, 252, 245, 0.5) 100%);
  box-shadow: 0 6px 28px rgba(44, 36, 22, 0.08);
  transform: translateY(-2px);
}

.ai-upload-zone.ai-drag-over {
  border-color: #d4a745;
  background: rgba(210, 180, 140, 0.1);
  box-shadow: 0 0 0 8px rgba(210, 180, 140, 0.06);
}

.ai-upload-emoji {
  font-size: 52px;
  display: block;
  margin-bottom: 10px;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.ai-upload-zone:hover .ai-upload-emoji {
  transform: scale(1.08);
}

.ai-upload-text {
  font-family: 'STKaiti', 'KaiTi', 'Ma Shan Zheng', 'Noto Serif SC', serif;
  font-size: 1.2em;
  color: #5c4a3a;
  letter-spacing: 0.05em;
  margin: 0;
}

.ai-upload-hint {
  font-size: 0.85em;
  color: #8b7355;
  margin-top: 6px;
}

.ai-preview-area {
  margin-top: 16px;
  text-align: center;
  animation: aiFadeSlideIn 0.4s ease;
}

.ai-preview-area img {
  max-width: 100%;
  max-height: 280px;
  border-radius: 14px;
  box-shadow: 0 6px 24px rgba(44, 36, 22, 0.16);
  border: 3px solid #d9cebf;
  object-fit: contain;
  background: #faf7f0;
}

.ai-preview-actions {
  margin-top: 14px;
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
}

.ai-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 11px 24px;
  border-radius: 30px;
  cursor: pointer;
  font-family: 'STSong', 'Noto Serif SC', 'Songti SC', 'SimSun', 'PingFang SC', 'Microsoft YaHei', serif;
  font-size: 0.95em;
  letter-spacing: 0.04em;
  border: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  white-space: nowrap;
  background: linear-gradient(135deg, #3a2f20 0%, #2c2416 100%);
  color: #f5ecd7;
  box-shadow: 0 4px 16px rgba(44, 36, 22, 0.25);
}

.ai-btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 7px 22px rgba(44, 36, 22, 0.35);
  background: linear-gradient(135deg, #4a3d2a 0%, #3a2f20 100%);
}

.ai-btn-primary:disabled {
  background: #c3c3c3;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.ai-btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 11px 24px;
  border-radius: 30px;
  cursor: pointer;
  font-family: 'STSong', 'Noto Serif SC', 'Songti SC', 'SimSun', 'PingFang SC', 'Microsoft YaHei', serif;
  font-size: 0.95em;
  letter-spacing: 0.04em;
  border: 1.5px solid #d9cebf;
  background: transparent;
  color: #5c4a3a;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  white-space: nowrap;
}

.ai-btn-outline:hover {
  border-color: #b8860b;
  color: #2c2416;
  background: rgba(210, 180, 140, 0.06);
}

.ai-analyze-wrap {
  position: relative;
  background: #fdfaf3;
  border-radius: 14px;
  border: 1.5px solid #d9cebf;
  padding: 6px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 10px rgba(44, 36, 22, 0.08);
}

.ai-analyze-wrap:focus-within {
  border-color: #b8860b;
  box-shadow: 0 4px 20px rgba(184, 134, 11, 0.12);
}

.ai-analyze-textarea {
  width: 100%;
  min-height: 120px;
  border: none;
  outline: none;
  resize: vertical;
  font-family: 'STKaiti', 'KaiTi', 'Noto Serif SC', 'STSong', 'SimSun', 'Songti SC', serif;
  font-size: 1.1em;
  padding: 14px 16px;
  background: transparent;
  color: #2c2416;
  letter-spacing: 0.04em;
  line-height: 1.8;
  border-radius: 8px;
  box-sizing: border-box;
}

.ai-analyze-textarea::placeholder {
  color: #c4b8a8;
  font-style: italic;
  letter-spacing: 0.03em;
}

.ai-example-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
  padding: 0 4px;
}

.ai-example-tag {
  font-size: 0.82em;
  padding: 6px 13px;
  border-radius: 20px;
  background: #f7f3ea;
  border: 1px solid #d9cebf;
  cursor: pointer;
  color: #8b7355;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
  font-family: 'STKaiti', 'KaiTi', 'Noto Serif SC', 'STSong', 'SimSun', 'Songti SC', serif;
}

.ai-example-tag:hover {
  background: #f0e8d5;
  border-color: #b8860b;
  color: #2c2416;
}

.ai-result-card {
  margin-top: 18px;
  background: linear-gradient(180deg, #fdfaf5 0%, #f9f4e8 100%);
  border-radius: 14px;
  border: 1px solid #d9cebf;
  padding: 20px 22px;
  animation: aiFadeSlideIn 0.5s ease;
  position: relative;
  box-shadow: 0 3px 16px rgba(44, 36, 22, 0.08);
}

.ai-poem-content {
  font-family: 'STKaiti', 'KaiTi', 'Noto Serif SC', 'STSong', 'SimSun', 'Songti SC', serif;
  font-size: 1.25em;
  line-height: 2;
  letter-spacing: 0.05em;
  color: #2c2416;
  text-align: center;
  white-space: pre-line;
  position: relative;
  padding: 0 10px;
}

.ai-poem-content::before,
.ai-poem-content::after {
  font-size: 1.6em;
  color: #b8860b;
  opacity: 0.6;
  font-family: 'STKaiti', 'KaiTi', 'Ma Shan Zheng', 'Noto Serif SC', serif;
  position: absolute;
}

.ai-poem-content::before {
  content: '「';
  left: -4px;
  top: -6px;
}

.ai-poem-content::after {
  content: '」';
  right: -4px;
  bottom: -6px;
}

.ai-analysis-label {
  display: inline-block;
  font-size: 0.8em;
  color: #b8860b;
  letter-spacing: 0.06em;
  margin-bottom: 6px;
  font-weight: 500;
}

.ai-analysis-text {
  font-family: 'STSong', 'Noto Serif SC', 'Songti SC', 'SimSun', 'PingFang SC', 'Microsoft YaHei', serif;
  font-size: 0.95em;
  color: #5c4a3a;
  line-height: 1.9;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dotted #d9cebf;
  letter-spacing: 0.03em;
}

.ai-explore-hint {
  text-align: center;
  color: #8b7355;
  font-family: 'STKaiti', 'KaiTi', 'Ma Shan Zheng', 'Noto Serif SC', serif;
  font-size: 1.05em;
  letter-spacing: 0.05em;
  margin: 0 0 14px;
}

.ai-explore-card {
  background: linear-gradient(180deg, #fdfaf5 0%, #f9f4e8 100%);
}

.ai-explore-info {
  text-align: center;
  margin-top: 12px;
}

.ai-extra-features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px dotted #d9cebf;
}

.ai-extra-card {
  background: #fdfaf5;
  border-radius: 14px;
  border: 1px solid #d9cebf;
  padding: 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
}

.ai-extra-card:hover {
  border-color: #b8860b;
  box-shadow: 0 4px 18px rgba(44, 36, 22, 0.08);
  transform: translateY(-2px);
}

.ai-extra-icon {
  font-size: 2em;
  margin-bottom: 6px;
}

.ai-extra-title {
  font-family: 'STKaiti', 'KaiTi', 'Ma Shan Zheng', 'Noto Serif SC', serif;
  font-size: 1em;
  color: #2c2416;
  letter-spacing: 0.04em;
}

.ai-extra-desc {
  font-size: 0.78em;
  color: #8b7355;
  margin-top: 3px;
}

.ai-extra-card.active {
  border-color: #b8860b;
  background: linear-gradient(135deg, #fdfaf5, #f5eed8);
  box-shadow: 0 4px 18px rgba(44, 36, 22, 0.12);
}

.ai-extra-panel {
  margin-top: 16px;
  background: #fdfaf5;
  border: 1px solid #d9cebf;
  border-radius: 14px;
  padding: 20px;
  animation: aiFadeIn 0.3s ease;
}

.ai-extra-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.ai-extra-panel-title {
  font-family: 'STKaiti', 'KaiTi', 'Ma Shan Zheng', 'Noto Serif SC', serif;
  font-size: 1.1em;
  color: #2c2416;
  letter-spacing: 0.04em;
}

.ai-extra-panel-close {
  background: none;
  border: none;
  font-size: 1.4em;
  color: #8b7355;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}

.ai-extra-panel-close:hover {
  color: #2c2416;
}

.rhyme-search-bar {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rhyme-search-type {
  display: flex;
  gap: 8px;
}

.rhyme-type-btn {
  background: none;
  border: 1px solid #d9cebf;
  border-radius: 8px;
  padding: 6px 14px;
  font-size: 0.85em;
  color: #5a4a3a;
  cursor: pointer;
  transition: all 0.2s;
}

.rhyme-type-btn.active {
  background: #b8860b;
  color: #fff;
  border-color: #b8860b;
}

.rhyme-search-input {
  display: flex;
  gap: 8px;
}

.rhyme-search-input input {
  flex: 1;
  border: 1px solid #d9cebf;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 0.9em;
  background: #fff;
  color: #2c2416;
  outline: none;
}

.rhyme-search-input input:focus {
  border-color: #b8860b;
}

.rhyme-results {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dotted #d9cebf;
}

.rhyme-result-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
}

.rhyme-result-char {
  font-family: 'STKaiti', 'KaiTi', 'Ma Shan Zheng', 'Noto Serif SC', serif;
  font-size: 2em;
  color: #b8860b;
  line-height: 1;
}

.rhyme-result-info {
  font-size: 0.9em;
  color: #5a4a3a;
}

.rhyme-result-info strong {
  color: #b8860b;
}

.rhyme-same-title {
  font-size: 0.85em;
  color: #8b7355;
  margin-bottom: 8px;
}

.rhyme-char-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.rhyme-char-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  font-family: 'STKaiti', 'KaiTi', 'Ma Shan Zheng', 'Noto Serif SC', serif;
  font-size: 1em;
  cursor: default;
  transition: all 0.2s;
}

.rhyme-char-tag.平声 {
  background: #e8f5e9;
  color: #2e7d32;
  border: 1px solid #c8e6c9;
}

.rhyme-char-tag.上声,
.rhyme-char-tag.去声,
.rhyme-char-tag.入声 {
  background: #fce4ec;
  color: #c62828;
  border: 1px solid #f8bbd0;
}

.couplet-example-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.couplet-example-label {
  font-size: 0.82em;
  color: #8b7355;
}

.couplet-example-tag {
  background: none;
  border: 1px solid #d9cebf;
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 0.8em;
  color: #5a4a3a;
  cursor: pointer;
  transition: all 0.2s;
}

.couplet-example-tag:hover {
  border-color: #b8860b;
  color: #b8860b;
}

.couplet-input-area {
  display: flex;
  gap: 8px;
}

.couplet-input-area input {
  flex: 1;
  border: 1px solid #d9cebf;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 0.9em;
  background: #fff;
  color: #2c2416;
  outline: none;
}

.couplet-input-area input:focus {
  border-color: #b8860b;
}

.couplet-result-card {
  margin-top: 14px;
}

.couplet-display {
  text-align: center;
  padding: 8px 0;
}

.couplet-upper {
  font-family: 'STKaiti', 'KaiTi', 'Ma Shan Zheng', 'Noto Serif SC', serif;
  font-size: 1.05em;
  color: #2c2416;
  letter-spacing: 0.08em;
}

.couplet-divider {
  color: #d9cebf;
  margin: 8px 0;
  font-size: 0.9em;
}

.couplet-lower {
  font-family: 'STKaiti', 'KaiTi', 'Ma Shan Zheng', 'Noto Serif SC', serif;
  font-size: 1.05em;
  color: #b8860b;
  letter-spacing: 0.08em;
  white-space: pre-wrap;
  text-align: left;
  line-height: 1.8;
}

@keyframes aiFadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.vision-article-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.vision-article-title {
  cursor: pointer;
  transition: color 0.3s ease;
}

.vision-article-title:hover {
  color: #007BFF;
}

.luntan_section_title {
  font-family: cursive;
  text-align: center;
  font-size: 28px;
  color: #333;
  margin: 0 0 15px;
  padding: 0;
}

.luntan_jx,
.luntan_ddjx {
  position: relative;
  width: 100%;
  border-radius: 16px;
  margin: 0;
  padding: 20px;
  box-sizing: border-box;
  overflow: visible;
  min-height: 500px;
}

.luntan_jx {
  background: linear-gradient(135deg, rgba(139, 69, 19, 0.08), rgba(210, 105, 30, 0.12));
  border: 1px solid rgba(139, 69, 19, 0.15);
}

.luntan_ddjx {
  background: linear-gradient(135deg, rgba(85, 107, 47, 0.08), rgba(107, 142, 35, 0.12));
  border: 1px solid rgba(85, 107, 47, 0.15);
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

.poem_search_status {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
}

.poem_search_card,
.poem_recommend_card {
  flex-direction: column;
}

.poem_search_card .poem_card_body,
.poem_recommend_card .poem_card_body {
  padding: 16px;
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

.poem_card_dynasty {
  position: absolute;
  top: 10px;
  left: 10px;
  background: linear-gradient(135deg, rgba(139, 69, 19, 0.85), rgba(160, 82, 45, 0.9));
  color: #ffe4c4;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 20px;
  letter-spacing: 2px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(4px);
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

.poem_author {
  font-size: 13px;
  color: #a0724e;
  margin-bottom: 16px;
  letter-spacing: 3px;
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

.jingxuan_pl {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.jingxuan_pl p {
  margin: 0;
  font-size: 14px;
  color: #444;
  line-height: 1.6;
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
  word-wrap: break-word;
  overflow-wrap: break-word;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
  line-clamp: 4;
}

.luntan_ddjx .poem_date {
  font-size: 13px;
  color: #888;
  display: block;
  text-align: right;
}

.luntan_ddjx .poem_title {
  font-size: 17px;
  font-weight: 600;
  color: #8B4513;
  display: block;
  margin: 6px 0;
  letter-spacing: 1px;
}

.luntan_ddjx .poem_author {
  font-size: 14px;
  color: #666;
  display: block;
  text-align: right;
  margin-top: 8px;
  font-style: italic;
}

.luntan_ddjx .poem_card img {
  width: 120px;
  height: 90px;
  border-radius: 8px;
  object-fit: cover;
  border: 2px solid rgba(200, 200, 200, 0.4);
  flex-shrink: 0;
  transition: transform 0.3s ease;
}

.luntan_ddjx .poem_card:hover img {
  transform: scale(1.03);
}

.poem_comment_trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.2s ease;
}

.poem_comment_trigger:hover {
  opacity: 1;
}

.poem_comment_icon {
  font-size: 14px;
}

.poem_comment_count {
  font-size: 12px;
  color: #666;
}

.poem_comment_section {
  width: 100%;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  padding: 12px;
  margin-top: 8px;
  box-sizing: border-box;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.poem_comment_input_bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.poem_comment_input_bar.poem_comment_login_hint {
  justify-content: center;
  padding: 8px;
  color: #999;
  cursor: pointer;
  font-size: 13px;
}

.poem_comment_input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
}

.poem_comment_input:focus {
  border-color: #66afe9;
}

.poem_comment_submit_btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.poem_comment_submit_btn:hover {
  opacity: 0.9;
}

.poem_comment_submit_btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.poem_comment_list {
  max-height: 200px;
  overflow-y: auto;
}

.poem_comment_list::-webkit-scrollbar {
  width: 4px;
}

.poem_comment_list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 2px;
}

.poem_comment_item {
  padding: 8px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.poem_comment_item:last-child {
  border-bottom: none;
}

.poem_comment_author {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.poem_comment_text {
  font-size: 13px;
  color: #555;
  line-height: 1.5;
}

.poem_comment_footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}

.poem_comment_time {
  font-size: 11px;
  color: #999;
}

.poem_comment_like_btn {
  background: none;
  border: none;
  font-size: 12px;
  color: #999;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.2s;
}

.poem_comment_like_btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #666;
}

.poem_comment_empty {
  text-align: center;
  padding: 16px;
  color: #999;
  font-size: 13px;
}

.contemporary_container {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%) !important;
  border: 1px solid #dee2e6 !important;
}

.contemporary_title {
  background: linear-gradient(to right, #495057, #6c757d) !important;
  -webkit-background-clip: text !important;
  -webkit-text-fill-color: transparent !important;
  background-clip: text !important;
}

.contemporary_scroll {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 5px;
}

.contemporary_pagination {
  display: flex;
  justify-content: center;
  padding: 12px 0 8px;
}

.contemporary_card {
  display: flex;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e9ecef;
  transition: all 0.3s ease;
  position: relative;
}

.contemporary_card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.contemporary_card_img {
  width: 120px;
  height: 120px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 10px 0 0 10px;
}

.contemporary_card_img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.contemporary_card:hover .contemporary_card_img img {
  transform: scale(1.05);
}

.contemporary_card_body {
  flex: 1;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  min-width: 0;
  position: relative;
}

.contemporary_card_date {
  position: absolute;
  top: 8px;
  right: 12px;
  font-size: 12px;
  color: #868e96;
}

.contemporary_card_content {
  flex: 1;
  margin-bottom: 8px;
}

.contemporary_card_content p {
  font-size: 14px;
  color: #495057;
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
  line-clamp: 4;
  overflow: hidden;
}

.contemporary_card_title {
  font-size: 15px;
  font-weight: 600;
  color: #212529;
  margin-bottom: 6px;
}

.contemporary_card_footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid #f1f3f5;
}

.contemporary_card_author {
  font-size: 12px;
  color: #6c757d;
  font-style: italic;
}

.contemporary_card_actions {
  display: flex;
  gap: 6px;
}

.contemporary_action_btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 12px;
  color: #6c757d;
}

.contemporary_action_btn:hover {
  background: #e9ecef;
  color: #495057;
}

.contemporary_action_btn.active {
  background: #e94560;
  border-color: transparent;
  color: #fff;
}

.action_icon {
  font-size: 12px;
}

.action_count {
  font-size: 11px;
}

.contemporary_comment_section {
  padding: 12px 16px;
  background: #f8f9fa;
  border-top: 1px solid #e9ecef;
}

.contemporary_comment_input_bar {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.contemporary_comment_input {
  flex: 1;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 16px;
  font-size: 13px;
  color: #212529;
  outline: none;
  transition: all 0.2s;
}

.contemporary_comment_input::placeholder {
  color: #adb5bd;
}

.contemporary_comment_input:focus {
  border-color: #868e96;
  box-shadow: 0 0 0 2px rgba(134, 142, 150, 0.1);
}

.contemporary_comment_submit {
  padding: 8px 14px;
  background: #495057;
  border: none;
  border-radius: 16px;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.contemporary_comment_submit:hover {
  background: #343a40;
}

.contemporary_comment_submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.contemporary_comment_login {
  text-align: center;
  padding: 8px;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  color: #495057;
  font-size: 12px;
  border: 1px solid #e9ecef;
}

.contemporary_comment_login:hover {
  background: #f8f9fa;
}

.contemporary_comment_list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.contemporary_comment_item {
  background: #fff;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.contemporary_comment_author {
  font-size: 12px;
  font-weight: 600;
  color: #495057;
  margin-bottom: 4px;
}

.contemporary_comment_text {
  font-size: 12px;
  color: #212529;
  line-height: 1.5;
  margin-bottom: 4px;
}

.contemporary_comment_footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.contemporary_comment_time {
  font-size: 11px;
  color: #adb5bd;
}

.contemporary_comment_like {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: #6c757d;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.2s;
}

.contemporary_comment_like:hover {
  background: #f1f3f5;
}

.contemporary_comment_empty {
  text-align: center;
  padding: 10px;
  color: #adb5bd;
  font-size: 12px;
}

</style>
