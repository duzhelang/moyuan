<script setup lang="ts">
import { ref, onMounted, onUnmounted, onActivated, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getForumPostList } from '@/api/modules/forum'
import { writePoemFromImage, analyzePoem, matchCouplet } from '@/api/modules/ai'
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

onMounted(async () => {
  loadAnimationConfig()
  showTime()
  clockTimer.value = setInterval(showTime, 1000)

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

    <img class="zhuang_s8" :src="asset('/img/YIJI_tuobiao_5.png')" alt="">
    <img class="zhuang_s10" :src="asset('/img/tuobiao_2.png')" alt="">
    <div class="forum_poem_layout">
      <ForumPreview :posts="hotPosts" />

      <div class="poem_selection_container">
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
