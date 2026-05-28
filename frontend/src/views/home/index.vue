<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePoemStore } from '@/stores/poem'
import type { Poem, Poet } from '@/types/model'

const router = useRouter()
const poemStore = usePoemStore()

const currentSlide = ref(1)
const maxSlides = 6
const loading = ref(false)

const poets = ref<Poet[]>([
  {
    id: 1,
    name: '李清照',
    dynastyName: '宋代',
    description: '（1084年3月13日—1155年），号易安居士，齐州章丘（今山东省济南市章丘区）人。宋代婉约派代表词人。',
    avatar: '/images/cd_suolue (10).jpg',
    createdAt: ''
  },
  {
    id: 2,
    name: '杜牧',
    dynastyName: '唐代',
    description: '（803年—852年），字牧之，京兆万年（今陕西省西安市）人。杜佑之孙，唐朝文学家、诗人。',
    avatar: '/images/cd_suolue (11).jpg',
    createdAt: ''
  },
  {
    id: 3,
    name: '苏轼',
    dynastyName: '宋代',
    description: '（1037年—1101年），字子瞻，又字和仲，号铁冠道人、东坡居士，世称苏东坡、苏仙、坡仙。',
    avatar: '/images/cd_suolue (12).jpg',
    createdAt: ''
  },
  {
    id: 4,
    name: '上官婉儿',
    dynastyName: '唐代',
    description: '（664 年 —710 年）是唐代一位极具影响力的女官和诗人。与蔡文姬、李清照、卓文君并称中国古代四大才女。',
    avatar: '/images/cd_suolue (9).jpg',
    createdAt: ''
  }
])

const poems = ref<Poem[]>([
  {
    id: 1,
    title: '黄鹤楼',
    content: '昔人已乘黄鹤去，此地空余黄鹤楼。',
    source: '崔颢',
    dynastyName: '唐代',
    viewCount: 0,
    likeCount: 0,
    favoriteCount: 0,
    status: 1,
    isFeatured: 1,
    createdAt: '',
    updatedAt: ''
  },
  {
    id: 2,
    title: '春宫怨',
    content: '早被婵娟误，欲妆临镜慵。',
    source: '杜荀鹤',
    dynastyName: '唐代',
    viewCount: 0,
    likeCount: 0,
    favoriteCount: 0,
    status: 1,
    isFeatured: 1,
    createdAt: '',
    updatedAt: ''
  },
  {
    id: 3,
    title: '秋日赴阙题潼关驿楼',
    content: '红叶晚萧萧，长亭酒一瓢。',
    source: '许浑',
    dynastyName: '唐代',
    viewCount: 0,
    likeCount: 0,
    favoriteCount: 0,
    status: 1,
    isFeatured: 1,
    createdAt: '',
    updatedAt: ''
  },
  {
    id: 4,
    title: '次北固山下',
    content: '客路青山外，行舟绿水前。',
    source: '王湾',
    dynastyName: '唐代',
    viewCount: 0,
    likeCount: 0,
    favoriteCount: 0,
    status: 1,
    isFeatured: 1,
    createdAt: '',
    updatedAt: ''
  }
])

const forumPosts = ref([
  {
    id: 1,
    title: '愿如花已落千行',
    content: '愿如花已落千行，未闻花语浅别殇。\n幽僻心境漫过少，唯有诗语解锁缰。\n金甲未脱抬眼望，怒剑难收向疆场。\n笑祝功成人与事，再鼓一旬又何妨。',
    author: '常平逼王',
    date: '2019年6月24日 04:45',
    avatar: '/images/微信二维.jpg'
  },
  {
    id: 2,
    title: '西江月.未语',
    content: '华灯初照白路，夜来树影凄楚，俳徊只与月相伴，茫然一片迷雾。\n离人兀自心扬，未语难避哀伤，只若通明过后，真心但化霓裳。',
    author: '常平逼王',
    date: '2018年9月1日 11:01',
    avatar: '/images/微信二维.jpg'
  }
])

const imgLoopShow = (id: number) => {
  currentSlide.value = id
}

const goToPoemDetail = (id: number) => {
  router.push(`/poem/${id}`)
}

const goToForum = () => {
  router.push('/forum')
}

onMounted(() => {
  setInterval(() => {
    currentSlide.value = currentSlide.value >= maxSlides ? 1 : currentSlide.value + 1
  }, 3000)
})
</script>

<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="carousel-container">
        <div class="carousel">
          <div class="carousel-slides">
            <div 
              v-for="i in maxSlides" 
              :key="i"
              class="carousel-slide"
              :class="{ active: currentSlide === i }"
            >
              <img :src="`/images/lb_ (${i}).jpg`" :alt="`轮播图 ${i}`" />
            </div>
          </div>
          
          <div class="carousel-indicators">
            <button
              v-for="i in maxSlides"
              :key="i"
              class="indicator"
              :class="{ active: currentSlide === i }"
              @click="imgLoopShow(i)"
            >
              {{ i }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <section class="poets-section">
      <div class="container">
        <h2 class="section-title">著名诗人</h2>
        <div class="poets-grid">
          <div 
            v-for="poet in poets" 
            :key="poet.id"
            class="poet-card"
          >
            <div class="poet-avatar">
              <img :src="poet.avatar" :alt="poet.name" />
            </div>
            <div class="poet-info">
              <h3 class="poet-name">{{ poet.name }}</h3>
              <p class="poet-dynasty">{{ poet.dynastyName }}</p>
              <p class="poet-desc">{{ poet.description }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="poems-section">
      <div class="container">
        <h2 class="section-title">经典作品</h2>
        <div class="poems-grid">
          <div 
            v-for="poem in poems" 
            :key="poem.id"
            class="poem-card"
            @click="goToPoemDetail(poem.id)"
          >
            <div class="poem-image">
              <img :src="`/images/h6${poem.id > 1 ? '.' + poem.id : ''}.jpg`" :alt="poem.title" />
            </div>
            <div class="poem-content">
              <h3 class="poem-title">{{ poem.title }}</h3>
              <p class="poem-author">{{ poem.dynastyName }}：{{ poem.source }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="forum-section">
      <div class="container">
        <h2 class="section-title">诗汇论坛</h2>
        <div class="forum-posts">
          <div 
            v-for="post in forumPosts" 
            :key="post.id"
            class="forum-post"
          >
            <div class="post-header">
              <img :src="post.avatar" :alt="post.author" class="post-avatar" />
              <div class="post-meta">
                <span class="post-title">{{ post.title }}</span>
                <span class="post-date">{{ post.date }}</span>
              </div>
            </div>
            <div class="post-content">
              <p>{{ post.content }}</p>
            </div>
          </div>
        </div>
        <div class="forum-action">
          <el-button type="primary" @click="goToForum">
            查看更多
          </el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.home-page {
  padding-bottom: $spacing-xxl;
}

.hero-section {
  margin-bottom: $spacing-xxl;
}

.carousel-container {
  @include container;
}

.carousel {
  position: relative;
  overflow: hidden;
  border-radius: $border-radius-lg;
  box-shadow: $box-shadow-lg;
}

.carousel-slides {
  position: relative;
  height: 400px;
}

.carousel-slide {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  transition: opacity $transition-slow;
  
  &.active {
    opacity: 1;
  }
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.carousel-indicators {
  position: absolute;
  bottom: $spacing-md;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: $spacing-sm;
}

.indicator {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.5);
  border: none;
  color: #fff;
  font-size: $font-size-sm;
  cursor: pointer;
  transition: all $transition-fast;
  
  &.active {
    background-color: $primary-color;
  }
  
  &:hover {
    background-color: rgba(255, 255, 255, 0.8);
  }
}

.section-title {
  text-align: center;
  font-size: $font-size-title;
  color: $primary-color;
  margin-bottom: $spacing-xl;
  font-family: $font-family-title;
}

.poets-section {
  margin-bottom: $spacing-xxl;
}

.poets-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-lg;
  
  @include responsive(lg) {
    grid-template-columns: repeat(2, 1fr);
  }
  
  @include responsive(sm) {
    grid-template-columns: 1fr;
  }
}

.poet-card {
  @include card;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: $spacing-xl;
}

.poet-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  margin-bottom: $spacing-md;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.poet-name {
  font-size: $font-size-xl;
  color: $primary-color;
  margin-bottom: $spacing-xs;
}

.poet-dynasty {
  font-size: $font-size-sm;
  color: $text-color-secondary;
  margin-bottom: $spacing-sm;
}

.poet-desc {
  font-size: $font-size-sm;
  color: $text-color-secondary;
  line-height: $line-height-loose;
  @include text-clamp(3);
}

.poems-section {
  margin-bottom: $spacing-xxl;
}

.poems-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-lg;
  
  @include responsive(lg) {
    grid-template-columns: repeat(2, 1fr);
  }
  
  @include responsive(sm) {
    grid-template-columns: 1fr;
  }
}

.poem-card {
  @include card;
  cursor: pointer;
  overflow: hidden;
  padding: 0;
}

.poem-image {
  height: 200px;
  overflow: hidden;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform $transition-slow;
  }
}

.poem-card:hover .poem-image img {
  transform: scale(1.1);
}

.poem-content {
  padding: $spacing-md;
}

.poem-title {
  font-size: $font-size-lg;
  color: $text-color;
  margin-bottom: $spacing-xs;
}

.poem-author {
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.forum-section {
  margin-bottom: $spacing-xxl;
}

.forum-posts {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.forum-post {
  @include card;
}

.post-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.post-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.post-meta {
  display: flex;
  flex-direction: column;
}

.post-title {
  font-size: $font-size-lg;
  color: $text-color;
  font-weight: 600;
}

.post-date {
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.post-content {
  p {
    font-size: $font-size-base;
    color: $text-color-secondary;
    line-height: $line-height-loose;
    white-space: pre-line;
  }
}

.forum-action {
  text-align: center;
  margin-top: $spacing-xl;
}
</style>