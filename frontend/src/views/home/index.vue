<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getForumPostList } from '@/api/modules/forum'
import { writePoemFromImage, analyzePoem } from '@/api/modules/ai'
import { getRandomPoetFeatured } from '@/api/modules/poetFeatured'
import type { PoetFeatured } from '@/api/modules/poetFeatured'
import { getFeaturedVisionArticles } from '@/api/modules/visionArticle'
import { ElMessage } from 'element-plus'
import type { ForumPost, VisionArticle } from '@/types/model'
import { useUserStore } from '@/stores/user'
import PoetCard from '@/components/business/PoetCard.vue'
import HomeNavigation from '@/components/business/HomeNavigation.vue'
import QrCodeLink from '@/components/common/QrCodeLink.vue'

const router = useRouter()
const userStore = useUserStore()

const hotPosts = ref<ForumPost[]>([])

const aiModel = ref('zhipu')
const visionModel = ref('glm-4.6v-flash')

const imageFile = ref<File | null>(null)
const imagePreview = ref('')
const poemResult = ref('')
const poemLoading = ref(false)

const analyzeInput = ref('')
const analyzeResult = ref('')
const analyzeLoading = ref(false)

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

const poetsData = ref<PoetFeatured[]>([])
const poetRefreshTimer = ref<ReturnType<typeof setInterval> | null>(null)
const visionArticles = ref<VisionArticle[]>([])

const fetchRandomPoets = async () => {
  try {
    const res = await getRandomPoetFeatured(4)
    const unique: PoetFeatured[] = []
    const seen = new Set<number>()
    for (const item of res.data) {
      const key = item.poetId ?? item.id
      if (!seen.has(key)) {
        seen.add(key)
        unique.push(item)
      }
    }
    poetsData.value = unique.slice(0, 4)
  } catch {
    console.error('获取精选诗人失败')
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
    imageFile.value = file
    const reader = new FileReader()
    reader.onload = (e) => {
      imagePreview.value = e.target?.result as string
    }
    reader.readAsDataURL(file)
  }
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



const handleLogout = async () => {
  try {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } catch {
    ElMessage.error('退出失败')
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
  poetRefreshTimer.value = setInterval(fetchRandomPoets, 30000)

  try {
    const postRes = await getForumPostList({ pageNum: 1, pageSize: 5 }).catch(() => ({ data: { list: [] as ForumPost[] } }))
    hotPosts.value = postRes.data?.list || []
  } catch {
    console.error('首页数据加载失败')
  }

  try {
    const visionRes = await getFeaturedVisionArticles({ pageNum: 1, pageSize: 6 }).catch(() => ({ data: { list: [] as VisionArticle[] } }))
    visionArticles.value = visionRes.data?.list || []
  } catch {
    console.error('诗话视野数据加载失败')
  }
})

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
        <router-link to="/user/login" class="top-link top-link-login">登录</router-link>
        <span class="top-divider">|</span>
        <router-link to="/user/register" class="top-link top-link-register">注册</router-link>
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
        <div class="toptub1"><a href="#luntan" title="交流论坛"><img src="/img/top_tubiao4.png"></a></div>
        <div class="toptub2" title="菜单"><a href="#sy_fljs"><img src="/img/top_tubiao2.png"></a></div>
        <div class="toptub3" title="搜索"><a href="javascript:void(0)"><img src="/img/top_tubiao1.png"></a></div>
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
    <div :class="['daohang', { fixed: isNavFixed }]">
      <ul>
        <li><a href="/forum" target="_blank">进入论坛</a></li>
        <li>古体诗词
          <ul>
            <li>怀古诗</li>
            <li>咏物感怀</li>
            <li>边塞风云</li>
            <li>山水田园</li>
            <li>赠友送别</li>
            <li>闲适隐逸</li>
            <li>···</li>
          </ul>
        </li>
        <li>现代诗歌
          <ul>
            <li>人生派</li>
            <li>七月派</li>
            <li>朦胧派</li>
            <li>现实主义诗派</li>
            <li>新时代派</li>
            <li>流萤诗派</li>
            <li>···</li>
          </ul>
        </li>
        <li>外国诗歌
          <ul>
            <li>俄罗斯</li>
            <li>法国</li>
            <li>英国</li>
            <li>日本</li>
          </ul>
        </li>
        <li>当代青年
          <ul>
            <li>九刀</li>
            <li>初芒</li>
            <li>精工庞华</li>
            <li>清幽雅丽</li>
          </ul>
        </li>
        <li>关于我们
          <ul>
            <li>查看详情</li>
            <li>联系我们</li>
            <li>错误指正</li>
          </ul>
        </li>
        <li id="xdl_kaiguan">个人中心
          <div class="dl_icon_wrapper">
            <img class="dl_icon dl_icon_default" src="/img/dl_tb1.png" alt="">
            <img class="dl_icon dl_icon_hover" src="/img/dl_tb2.png" alt="">
          </div>
          <div id="sy_xdlchuang" class="sy_xdlchuang">
            <template v-if="userStore.isLoggedIn">
              <div class="user-menu-container">
                <div class="user-menu-header">
                  <el-avatar :src="userStore.avatar" :size="40">
                    {{ userStore.username?.charAt(0)?.toUpperCase() }}
                  </el-avatar>
                  <span class="user-menu-name">{{ userStore.userInfo?.nickname || userStore.username }}</span>
                </div>
                <div class="user-menu-items">
                  <div class="user-menu-item" @click="router.push('/user/profile')">
                    <el-icon><User /></el-icon>
                    <span>个人中心</span>
                  </div>
                  <div class="user-menu-item" @click="router.push('/user/profile?tab=favorites')">
                    <el-icon><Star /></el-icon>
                    <span>我的收藏</span>
                  </div>
                  <div class="user-menu-item" @click="router.push('/user/profile?tab=history')">
                    <el-icon><Clock /></el-icon>
                    <span>浏览历史</span>
                  </div>
                  <div v-if="userStore.userInfo?.role === 'admin'" class="user-menu-item admin-item" @click="router.push('/admin/dashboard')">
                    <el-icon><Setting /></el-icon>
                    <span>后台管理</span>
                  </div>
                  <div class="user-menu-item logout-item" @click="handleLogout">
                    <el-icon><SwitchButton /></el-icon>
                    <span>退出登录</span>
                  </div>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="login-container">
                <h2 class="login-title">登&nbsp;&nbsp;&nbsp;&nbsp;录</h2>
                <div class="lieb">
                  <label for="username" class="dl_ts">用户名:</label>
                  <input type="text" id="username" class="input-field" placeholder="请输入用户名">
                </div>
                <div class="lieb">
                  <label for="password" class="dl_ts">密&nbsp;&nbsp;码:</label>
                  <input type="password" id="password" class="input-field" placeholder="请输入密码" required>
                </div>
                <button type="button" class="login-button" @click="router.push('/user/login')">登录</button>
                <div class="forgot-password">
                  <a class="fuzhu" href="/user/login">忘记密码?</a>
                </div>
                <div class="register-link">
                  <a class="fuzhu" href="/user/register">还没有账号?注册</a>
                </div>
              </div>
            </template>
          </div>
        </li>
      </ul>
    </div>
    <div :class="['sousuo', { fixed: isNavFixed }]">
      <form id="sousuo0" @submit.prevent="router.push('/search')">
        <input type="text" placeholder="输入搜索内容" name="search">
        <button type="submit">搜索</button>
      </form>
    </div>

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
      <img class="zhuang_s11" :src="asset('/img/lt_bg6.jpg')" alt="">
      <img class="zhuang_s3" :src="asset('/img/canci.png')" alt="">
      <img class="zhuang_s4" :src="asset('/img/gn_ (2).png')" alt="">
      <img class="zhuang_s4_top" :src="asset('/img/gn_ (2).png')" alt="">
    </div>

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

      <HomeNavigation type="works" title="作品" />

      <div class="cd_fenge"></div>

      <HomeNavigation type="genres" title="流派" />

      <HomeNavigation type="dynasties" title="朝代" />

      <div class="cd_fenge"></div>
      <h2>展风拓潮</h2>
      <img class="zhuang_s6" :src="asset('/img/zichu (4).png')" alt="">
      <img class="zhuang_s7" :src="asset('/img/tuobiao (4).png')" alt="">
    </div>

    <hr>
    <div class="z_center">
      <h2 class="ai_title">趣味AI</h2>
      <div class="z_left">
        <div class="ai-card ai-card-poem">
          <div class="ai-card-header">
            <h2>看图写诗</h2>
            <p>上传图片，AI为你赋诗一首</p>
          </div>
          <div class="ai-card-body">
            <div class="ai-upload-area" @click="imageUploadRef?.click()">
              <img
                v-if="imagePreview"
                :src="imagePreview"
                alt=""
                class="ai-preview-img"
              >
              <div v-else class="ai-upload-placeholder">
                <span class="ai-upload-icon">+</span>
                <span>点击上传图片</span>
              </div>
            </div>
            <input
              ref="imageUploadRef"
              type="file"
              accept="image/*"
              style="display: none;"
              @change="handleImageUpload"
            >
            <button class="ai-btn ai-btn-primary" @click="handleWritePoem" :disabled="poemLoading">
              {{ poemLoading ? 'AI赋诗中...' : '开始赋诗' }}
            </button>
            <div v-if="poemResult" class="ai-result-box">
              <p class="ai-result-text">{{ poemResult }}</p>
            </div>
          </div>
        </div>
        <div class="ai-card ai-card-analyze">
          <div class="ai-card-header">
            <h2>智能分析</h2>
            <p>输入诗句，AI为你深度解析</p>
          </div>
          <div class="ai-card-body">
            <textarea
              class="ai-textarea"
              v-model="analyzeInput"
              placeholder="例如：床前明月光，疑是地上霜。"
              rows="4"
            ></textarea>
            <button class="ai-btn ai-btn-primary" @click="handleAnalyzePoem" :disabled="analyzeLoading">
              {{ analyzeLoading ? 'AI解析中...' : '智能解析' }}
            </button>
            <div v-if="analyzeResult" class="ai-result-box">
              <p class="ai-result-text">{{ analyzeResult }}</p>
            </div>
          </div>
        </div>
      </div>
      <div class="z_right">
        <h4>诗话视野</h4>
        <hr>
        <template v-if="visionArticles.length > 0">
          <div
            v-for="(article, index) in visionArticles"
            :key="article.id"
            class="vision-article-item"
          >
            <p @click="router.push(`/vision/${article.id}`)" class="vision-article-title">
              ---{{ index + 1 }}. "{{ article.title }}"
            </p>
            <a class="shsy_gdxq" @click.prevent="router.push(`/vision/${article.id}`)">&lt;更多详情&gt;</a>
          </div>
        </template>
        <template v-else>
          <p>暂无文章内容</p>
        </template>
        <hr>
        <a class="shsy_gdxq" @click.prevent="router.push('/vision')">&lt;查看全部&gt;</a>
      </div>
    </div>

    <img class="zhuang_s" :src="asset('/img/gn_ (2).png')" alt="">
    <img class="zhuang_s6" :src="asset('/img/zichu (4).png')" alt="">
    <img class="zhuang_s8" :src="asset('/img/YIJI_tuobiao (5).png')" alt="">
    <img class="zhuang_s9" :src="asset('/img/wz_tubiao (2).png')" alt="">
    <img class="zhuang_s10" :src="asset('/img/tuobiao (2).png')" alt="">
    <h1>诗汇论坛</h1>
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
        <marquee behavior="scroll" direction="left" scrollamount="5">
          论坛是我们心灵的栖息地，在诗词的韵律中找到共鸣，在分享与探索中共同成长！
        </marquee>
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
      <div class="luntan_jxh">
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
}

.top .logo_img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

.logo-txt {
  font-size: 26px;
  color: #fff;
  margin-left: 8px;
  font-weight: 600;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
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

.daohang {
  width: 100%;
  height: 58px;
  margin: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  z-index: 50;
}

.daohang ul {
  list-style: none;
  display: flex;
  font-weight: 600;
  background-color: rgba(100, 100, 100, 0.75);
  font-size: 28px;
  border-radius: 7px;
  margin: 0 auto;
  padding: 0;
}

.daohang > ul > li {
  float: left;
  width: 180px;
  height: 38px;
  line-height: 38px;
  text-align: center;
  border: 1px solid gainsboro;
  border-radius: 5px;
  color: wheat;
  position: relative;
  cursor: pointer;
  list-style: none;
}

.daohang > ul > li > a {
  color: wheat;
  text-decoration: none;
}

.daohang > ul > li > a:hover {
  color: yellow;
}

.daohang > ul > li > ul {
  display: none;
  position: absolute;
  top: 38px;
  left: -1px;
  z-index: 51;
  font-size: 20px;
  background: rgba(80, 80, 80, 0.85);
  width: 170px;
  border: 1px solid gainsboro;
  border-radius: 7px;
  padding: 0;
  list-style: none;
}

.daohang > ul > li:hover > ul {
  display: block;
}

.daohang > ul > li > ul > li {
  width: 100%;
  height: 35px;
  line-height: 35px;
  text-align: center;
  color: wheat;
  cursor: pointer;
  border-bottom: 1px solid rgba(200, 200, 200, 0.2);
  list-style: none;
}

.daohang > ul > li > ul > li:last-child {
  border-bottom: none;
}

.daohang > ul > li > ul > li:hover {
  color: yellow;
  font-size: 22px;
  background-color: rgba(100, 100, 100, 0.5);
}

.daohang.fixed {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  width: 100%;
  background: url('/img/dt_0.0.jpg');
  background-size: cover;
  background-repeat: no-repeat;
  background-position: 0px -320px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, .2);
  z-index: 61;
}

.sousuo.fixed {
  display: flex;
  position: fixed;
  top: 50px;
  left: 0;
  right: 0;
  width: 100%;
  z-index: 50;
  padding: 0 10%;
}

#xdl_kaiguan {
  position: relative;
  cursor: pointer;
}

.dl_icon_wrapper {
  position: absolute;
  top: 40%;
  right: 5px;
  transform: translateY(-50%);
  width: 28px;
  height: 28px;
  cursor: pointer;
}

.dl_icon {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  transition: all 0.3s ease;
}

.dl_icon_default {
  opacity: 1;
  filter: drop-shadow(0 0 0 transparent);
}

.dl_icon_hover {
  opacity: 0;
  filter: drop-shadow(0 0 4px rgba(255, 215, 0, 0.8)) brightness(1.3) contrast(1.2);
}

#xdl_kaiguan:hover .dl_icon_default {
  opacity: 0;
  transform: scale(0.9);
}

#xdl_kaiguan:hover .dl_icon_hover {
  opacity: 1;
  transform: scale(1.1);
}

.sy_xdlchuang {
  position: absolute;
  right: 0;
  top: 38px;
  z-index: 51;
  display: none;
  cursor: pointer;
}

#xdl_kaiguan:hover .sy_xdlchuang {
  display: block;
}

.login-container {
  background: url('/img/dt_0.0.jpg') no-repeat -155px 0 / cover;
  width: 366px;
  height: 220px;
  background-color: #f4f4f4;
  border: 1px solid #ccc;
  border-radius: 50px;
  border-top-left-radius: 15px;
  border-bottom-right-radius: 15px;
  padding: 10px 20px;
  box-sizing: border-box;
}

.login-title {
  text-align: center;
  font-size: 28px;
  font-weight: 600;
  color: #000;
  margin: 2px 0;
}

.lieb {
  display: flex;
  align-items: center;
  color: #000;
  margin: 5px 0;
}

.dl_ts {
  font-size: 14px;
  width: 60px;
  text-align: right;
  margin-right: 8px;
}

.input-field {
  flex: 1;
  height: 30px;
  background-color: #ffffff;
  color: #000;
  border-radius: 10px;
  border: 1px solid #ccc;
  padding: 0 10px;
  font-size: 14px;
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
  letter-spacing: 0.5px;
  line-height: 1.5;
}

.login-button {
  width: 100px;
  height: 32px;
  background-color: #007BFF;
  color: white;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  font-size: 14px;
  margin-top: 8px;
}

.login-button:hover {
  background-color: #0056b3;
}

.forgot-password {
  text-align: center;
  margin-top: 3px;
}

.forgot-password a {
  background-color: rgba(204, 204, 204, 0.7);
  text-shadow: 0 0 10px white;
  color: #55aaff;
  text-decoration: none;
  font-size: 14px;
}

.forgot-password a:hover {
  color: #007BFF;
}

.register-link {
  text-align: right;
  margin-top: 2px;
}

.register-link a {
  background-color: rgba(204, 204, 204, 0.7);
  text-shadow: 0 0 10px white;
  color: #55aaff;
  text-decoration: none;
  font-size: 14px;
}

.register-link a:hover {
  color: #007BFF;
}

.user-menu-container {
  background: url('/img/dt_0.0.jpg') no-repeat -155px 0 / cover;
  width: 220px;
  background-color: #f4f4f4;
  border: 1px solid #ccc;
  border-radius: 15px;
  border-top-left-radius: 15px;
  border-bottom-right-radius: 15px;
  padding: 15px;
  box-sizing: border-box;
}

.user-menu-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  margin-bottom: 8px;
}

.user-menu-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-menu-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #333;
  font-size: 14px;
}

.user-menu-item:hover {
  background-color: rgba(0, 123, 255, 0.1);
  color: #007BFF;
}

.user-menu-item .el-icon {
  font-size: 16px;
}

.admin-item {
  color: #e6a23c;
  margin-top: 4px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  padding-top: 12px;
}

.admin-item:hover {
  background-color: rgba(230, 162, 60, 0.1);
  color: #e6a23c;
}

.logout-item {
  color: #f56c6c;
  margin-top: 4px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  padding-top: 12px;
}

.logout-item:hover {
  background-color: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}

.sousuo {
  width: 80%;
  height: 50px;
  margin: 0 auto;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.sousuo form {
  display: flex;
  gap: 0;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.sousuo form:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
  transform: translateY(-1px);
}

.sousuo input[type="text"] {
  width: 240px;
  height: 40px;
  background-color: rgba(20, 20, 25, 0.85);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-right: none;
  border-radius: 20px 0 0 20px;
  padding: 0 16px;
  font-size: 14px;
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
  letter-spacing: 0.5px;
  line-height: 1.5;
  outline: none;
  transition: all 0.3s ease;
}

.sousuo input[type="text"]::placeholder {
  color: rgba(255, 255, 255, 0.4);
  font-style: italic;
}

.sousuo input[type="text"]:focus {
  background-color: rgba(30, 30, 35, 0.9);
  border-color: rgba(255, 255, 255, 0.2);
}

.sousuo button {
  width: 70px;
  height: 40px;
  background-color: rgba(100, 90, 75, 0.7);
  color: #f5f0e8;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 0 20px 20px 0;
  cursor: pointer;
  font-size: 14px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", sans-serif;
  font-weight: 500;
  transition: all 0.3s ease;
}

.sousuo button:hover {
  background-color: rgba(139, 115, 85, 0.8);
  color: #fff;
}

.sousuo button:active {
  transform: scale(0.95);
}

.flip-box .l2 img {
  border-radius: 1%;
  width: 250px;
  height: 240px;
  display: block;
  transition: linear 0.3s;
}

.h6_li .l1 {
  width: 295px;
  height: 200px;
  text-align: center;
  overflow: hidden;
}

.h6_li .l1 img {
  border-radius: 5%;
  width: 290px;
  height: 150px;
  display: block;
  transition: linear 0.3s;
}

.h6_li .l1 img:hover {
  transform: scale(1.05);
  border-radius: 20%;
}

.h6_li .l1 li {
  display: block;
  text-align: center;
}

.h6_li .l1 p {
  font-size: 18px;
  position: relative;
  left: 90px;
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

.luntan_jx img,
.luntan_ddjx img {
  width: 200px;
  height: 150px;
  margin: 10px 15px;
  border: 2px solid gainsboro;
  border-radius: 7px;
}

.jingxuan_pl img {
  width: 100px;
  height: 100px;
}

.zhuang_s1 {
  width: 136px;
  height: 235px;
  position: absolute;
  top: 165px;
  left: 0;
  z-index: 20;
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
  z-index: 1;
  transform: rotate(60deg);
  opacity: 0.7;
  filter: drop-shadow(5px 10px 15px rgba(0, 0, 0, 0.15));
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
  top: -172px;
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
  left: 28.4%;
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
  top: 4080px;
  left: 82.6%;
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
  z-index: -1;
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

.luntan {
  display: flex;
  flex-wrap: wrap;
  flex-direction: column;
  position: relative;
  width: 75%;
  margin: 0 auto;
  background-color: rgba(80, 80, 80, 0.1);
  border-radius: 15px;
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
  border: 1px solid #ccc;
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
  text-align: center;
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

.z_center {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 30px;
  padding: 20px 0;
  position: relative;
}

.ai_title {
  width: 100%;
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  color: #4A2C1A;
  margin: 0 0 20px;
  position: static;
  height: auto;
}

.z_right {
  overflow-y: visible;
  height: auto;
  flex-shrink: 0;
  width: 500px;
}

.z_left {
  overflow-y: visible;
  height: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 500px;
}

.z_left > .ai-card {
  min-width: 0;
  width: 100%;
}

.ai-card {
  border-radius: 16px;
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.ai-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
}

.ai-card-poem {
  background: linear-gradient(145deg, rgba(139, 69, 19, 0.15), rgba(210, 105, 30, 0.25));
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.ai-card-analyze {
  background: linear-gradient(145deg, rgba(85, 107, 47, 0.15), rgba(107, 142, 35, 0.25));
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.ai-card-header {
  padding: 20px 24px 12px;
  text-align: center;
}

.ai-card-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: #4A2C1A;
  margin: 0 0 6px;
}

.ai-card-header p {
  font-size: 13px;
  color: #555;
  margin: 0;
}

.ai-card-body {
  padding: 12px 24px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.ai-upload-area {
  width: 100%;
  height: 180px;
  border: 2px dashed rgba(139, 69, 19, 0.3);
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: border-color 0.3s ease, background-color 0.3s ease;
  background-color: rgba(255, 255, 255, 0.15);
}

.ai-upload-area:hover {
  border-color: rgba(139, 69, 19, 0.6);
  background-color: rgba(255, 255, 255, 0.25);
}

.ai-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 10px;
}

.ai-upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #666;
  font-size: 14px;
}

.ai-upload-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: rgba(139, 69, 19, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #8B4513;
}

.ai-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid rgba(139, 69, 19, 0.2);
  border-radius: 10px;
  background-color: rgba(255, 255, 255, 0.2);
  color: #333;
  font-size: 15px;
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
  letter-spacing: 0.5px;
  line-height: 1.6;
  resize: vertical;
  outline: none;
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
  box-sizing: border-box;
}

.ai-textarea:focus {
  border-color: rgba(139, 69, 19, 0.5);
  box-shadow: 0 0 0 3px rgba(139, 69, 19, 0.1);
}

.ai-textarea::placeholder {
  color: #999;
  font-size: 14px;
}

.ai-btn {
  width: 100%;
  padding: 10px 20px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  font-family: cursive;
  transition: all 0.3s ease;
}

.ai-btn-primary {
  background: linear-gradient(135deg, #8B4513, #A0522D);
  color: white;
}

.ai-btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(139, 69, 19, 0.4);
}

.ai-btn:disabled {
  background: #c3c3c3;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.ai-result-box {
  width: 100%;
  padding: 14px 16px;
  background-color: rgba(255, 255, 255, 0.3);
  border-radius: 10px;
  border-left: 4px solid #8B4513;
  animation: aiFadeIn 0.4s ease;
}

.ai-result-text {
  margin: 0;
  font-size: 14px;
  color: #333;
  line-height: 1.8;
  font-family: cursive;
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
</style>
