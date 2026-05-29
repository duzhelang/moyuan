<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getFeaturedPoems, getRandomPoem } from '@/api/modules/poem'
import { getPoetList } from '@/api/modules/poet'
import { getDynastyList } from '@/api/modules/dynasty'
import { getForumPostList } from '@/api/modules/forum'
import { chat, writePoemFromImage, analyzePoem } from '@/api/modules/ai'
import { ElMessage } from 'element-plus'
import type { Poem, Poet, Dynasty, ForumPost } from '@/types/model'

const router = useRouter()

const searchKeyword = ref('')
const featuredPoems = ref<Poem[]>([])
const dailyPoem = ref<Poem | null>(null)
const poets = ref<Poet[]>([])
const dynasties = ref<Dynasty[]>([])
const hotPosts = ref<ForumPost[]>([])
const loading = ref(true)

const aiDialogVisible = ref(false)
const aiMessage = ref('')
const aiModel = ref('zhipu')
const aiLoading = ref(false)
const chatHistory = ref<Array<{ role: string; content: string }>>([])
const chatContainerRef = ref<HTMLElement | null>(null)

const imageFile = ref<File | null>(null)
const imagePreview = ref('')
const poemResult = ref('')
const poemLoading = ref(false)

const analyzeInput = ref('')
const analyzeResult = ref('')
const analyzeLoading = ref(false)

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/search', query: { keyword: searchKeyword.value } })
  }
}

const getFirstTwoLines = (content: string) => {
  const lines = content.split('\n').filter(Boolean)
  return lines.slice(0, 2).join('\n')
}

const openAiDialog = () => {
  aiDialogVisible.value = true
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainerRef.value) {
      chatContainerRef.value.scrollTop = chatContainerRef.value.scrollHeight
    }
  })
}

const handleAiChat = async () => {
  if (!aiMessage.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }

  const userMessage = aiMessage.value
  chatHistory.value.push({ role: 'user', content: userMessage })
  aiMessage.value = ''
  aiLoading.value = true
  scrollToBottom()

  try {
    const res = await chat({ message: userMessage, model: aiModel.value })
    chatHistory.value.push({ role: 'assistant', content: res.data.reply })
  } catch (error) {
    ElMessage.error('AI问答失败，请稍后重试')
    chatHistory.value.push({ role: 'assistant', content: '抱歉，AI服务暂时不可用，请稍后重试。' })
  } finally {
    aiLoading.value = false
    scrollToBottom()
  }
}

const clearChatHistory = () => {
  chatHistory.value = []
}

const beforeImageUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}

const handleImageChange = (file: any) => {
  if (file.raw) {
    imageFile.value = file.raw
    const reader = new FileReader()
    reader.onload = (e) => {
      imagePreview.value = e.target?.result as string
    }
    reader.readAsDataURL(file.raw)
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
    const res = await writePoemFromImage(imageFile.value, aiModel.value)
    poemResult.value = res.data.poem
  } catch (error) {
    ElMessage.error('看图写诗失败，请稍后重试')
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
  } catch (error) {
    ElMessage.error('智能分析失败，请稍后重试')
    analyzeResult.value = '抱歉，AI服务暂时不可用，请稍后重试。'
  } finally {
    analyzeLoading.value = false
  }
}

onMounted(async () => {
  try {
    const [featuredRes, poetRes, dynastyRes, postRes, dailyRes] = await Promise.all([
      getFeaturedPoems({ pageSize: 6 }).catch(() => ({ data: { list: [] as Poem[] } })),
      getPoetList({ pageSize: 6 }).catch(() => ({ data: { list: [] as Poet[] } })),
      getDynastyList().catch(() => ({ data: [] as Dynasty[] })),
      getForumPostList({ pageNum: 1, pageSize: 5 }).catch(() => ({ data: { list: [] as ForumPost[] } })),
      getRandomPoem().catch(() => ({ data: null as Poem | null }))
    ])
    featuredPoems.value = featuredRes.data?.list || []
    poets.value = poetRes.data?.list || []
    dynasties.value = dynastyRes.data || []
    hotPosts.value = postRes.data?.list || []
    dailyPoem.value = dailyRes.data || null
  } catch (error) {
    console.error('首页数据加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">墨渊 · 古诗词鉴赏平台</h1>
        <p class="hero-subtitle">品读千年诗词，感受文化之美</p>
        <div class="hero-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索诗词、诗人、帖子..."
            size="large"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
        <div v-if="dailyPoem" class="daily-card" @click="router.push(`/poem/${dailyPoem.id}`)">
          <p class="daily-content">{{ getFirstTwoLines(dailyPoem.content) }}</p>
          <span class="daily-meta">— {{ dailyPoem.poetName }}《{{ dailyPoem.title }}》</span>
        </div>
      </div>
    </section>

    <section class="section featured-section">
      <div class="container">
        <h2 class="section-title">精选诗词</h2>
        <div v-loading="loading" class="poem-grid">
          <div
            v-for="poem in featuredPoems"
            :key="poem.id"
            class="poem-card"
            @click="router.push(`/poem/${poem.id}`)"
          >
            <h3 class="poem-card-title">{{ poem.title }}</h3>
            <div class="poem-card-meta">
              <span v-if="poem.poetName">{{ poem.poetName }}</span>
              <span v-if="poem.dynastyName">{{ poem.dynastyName }}</span>
            </div>
            <p class="poem-card-content">{{ getFirstTwoLines(poem.content) }}</p>
          </div>
        </div>
        <el-empty v-if="!loading && featuredPoems.length === 0" description="暂无推荐诗词" />
      </div>
    </section>

    <section class="section poet-section">
      <div class="container">
        <div class="section-header">
          <h2 class="section-title">诗人风采</h2>
          <router-link to="/poet" class="view-more">查看更多</router-link>
        </div>
        <div v-loading="loading" class="poet-grid">
          <div
            v-for="poet in poets"
            :key="poet.id"
            class="poet-card"
            @click="router.push(`/poet/${poet.id}`)"
          >
            <el-avatar :src="poet.avatar" :size="64">
              {{ poet.name?.charAt(0) }}
            </el-avatar>
            <h3 class="poet-name">{{ poet.name }}</h3>
            <span v-if="poet.dynastyName" class="poet-dynasty">{{ poet.dynastyName }}</span>
            <p v-if="poet.description" class="poet-desc">{{ poet.description }}</p>
          </div>
        </div>
        <el-empty v-if="!loading && poets.length === 0" description="暂无诗人信息" />
      </div>
    </section>

    <section class="section dynasty-section">
      <div class="container">
        <h2 class="section-title">按朝代浏览</h2>
        <div v-loading="loading" class="dynasty-tags">
          <el-tag
            v-for="dynasty in dynasties"
            :key="dynasty.id"
            size="large"
            class="dynasty-tag"
            @click="router.push({ path: '/poem', query: { dynastyId: String(dynasty.id) } })"
          >
            {{ dynasty.name }}
          </el-tag>
        </div>
        <el-empty v-if="!loading && dynasties.length === 0" description="暂无朝代信息" />
      </div>
    </section>

    <section class="section forum-section">
      <div class="container">
        <div class="section-header">
          <h2 class="section-title">论坛热议</h2>
          <router-link to="/forum" class="view-more">查看更多</router-link>
        </div>
        <div v-loading="loading" class="forum-list">
          <div
            v-for="post in hotPosts"
            :key="post.id"
            class="forum-item"
            @click="router.push(`/forum/${post.id}`)"
          >
            <h4 class="forum-item-title">{{ post.title }}</h4>
            <div class="forum-item-meta">
              <span class="meta-item">
                <el-icon><View /></el-icon>
                {{ post.viewCount }}
              </span>
              <span class="meta-item">
                <el-icon><ChatDotRound /></el-icon>
                {{ post.commentCount }}
              </span>
            </div>
          </div>
        </div>
        <el-empty v-if="!loading && hotPosts.length === 0" description="暂无帖子" />
      </div>
    </section>

    <section class="section ai-section">
      <div class="container">
        <h2 class="section-title">展风拓潮</h2>
        <p class="section-subtitle">趣味AI</p>
        <div class="ai-features">
          <div class="ai-feature-card">
            <div class="ai-feature-header">
              <el-icon size="32" color="#409eff"><Picture /></el-icon>
              <h3>看图写诗</h3>
            </div>
            <p class="ai-feature-desc">上传图片，AI为您创作古诗词</p>
            <div class="ai-feature-content">
              <div class="upload-area">
                <el-upload
                  class="image-uploader"
                  :show-file-list="false"
                  :before-upload="beforeImageUpload"
                  :on-change="handleImageChange"
                  accept="image/*"
                >
                  <div v-if="imagePreview" class="image-preview">
                    <img :src="imagePreview" alt="预览" />
                  </div>
                  <div v-else class="upload-placeholder">
                    <el-icon size="48" color="#c0c4cc"><Plus /></el-icon>
                    <p>点击上传图片</p>
                  </div>
                </el-upload>
              </div>
              <div class="poem-result">
                <el-button
                  type="primary"
                  :loading="poemLoading"
                  :disabled="!imageFile"
                  @click="handleWritePoem"
                >
                  生成诗词
                </el-button>
                <div v-if="poemResult" class="poem-output">
                  <h4>AI生成的诗词：</h4>
                  <p class="poem-text">{{ poemResult }}</p>
                </div>
              </div>
            </div>
          </div>

          <div class="ai-feature-card">
            <div class="ai-feature-header">
              <el-icon size="32" color="#67c23a"><DataAnalysis /></el-icon>
              <h3>智能分析</h3>
            </div>
            <p class="ai-feature-desc">输入诗句，AI为您深度解析</p>
            <div class="ai-feature-content">
              <el-input
                v-model="analyzeInput"
                type="textarea"
                :rows="4"
                placeholder="例如：床前明月光,疑是地上霜..."
                resize="none"
              />
              <el-button
                type="success"
                :loading="analyzeLoading"
                :disabled="!analyzeInput.trim()"
                @click="handleAnalyzePoem"
                style="margin-top: 16px"
              >
                智能分析
              </el-button>
              <div v-if="analyzeResult" class="analyze-output">
                <h4>AI分析结果：</h4>
                <p class="analyze-text">{{ analyzeResult }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <footer class="home-footer">
      <p>© 2024 墨渊 · 古诗词鉴赏平台</p>
    </footer>

    <el-button
      class="ai-float-btn"
      type="primary"
      circle
      size="large"
      @click="openAiDialog"
    >
      <el-icon size="24"><ChatDotRound /></el-icon>
    </el-button>

    <el-dialog
      v-model="aiDialogVisible"
      title="AI 诗词问答"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="ai-chat-container">
        <div class="ai-chat-header">
          <el-select v-model="aiModel" size="small">
            <el-option-group label="原生API">
              <el-option label="智谱AI" value="zhipu" />
              <el-option label="DeepSeek" value="deepseek" />
              <el-option label="Kimi" value="kimi" />
              <el-option label="MiMo" value="mimo" />
            </el-option-group>
            <el-option-group label="NVIDIA NIM">
              <el-option label="GLM-5" value="glm-5" />
              <el-option label="GLM-5.1" value="glm-5.1" />
              <el-option label="DeepSeek-V4" value="deepseek-v4" />
              <el-option label="Kimi-K2.5" value="kimi-k2.5" />
              <el-option label="Kimi-K2.6" value="kimi-k2.6" />
              <el-option label="MiniMax-M2.5" value="minimax-m2.5" />
              <el-option label="Qwen3.5" value="qwen3.5" />
              <el-option label="Llama 4 Scout" value="llama-4-scout" />
              <el-option label="Llama 4 Maverick" value="llama-4-maverick" />
            </el-option-group>
          </el-select>
          <el-button size="small" @click="clearChatHistory">清空记录</el-button>
        </div>

        <div ref="chatContainerRef" class="ai-chat-messages">
          <div v-if="chatHistory.length === 0" class="ai-chat-empty">
            <el-icon size="48" color="#c0c4cc"><ChatDotRound /></el-icon>
            <p>开始与AI对话吧！</p>
            <p class="ai-chat-hint">可以询问关于诗词、诗人、朝代等问题</p>
          </div>
          <div
            v-for="(msg, index) in chatHistory"
            :key="index"
            :class="['ai-message', msg.role === 'user' ? 'ai-message-user' : 'ai-message-assistant']"
          >
            <div class="ai-message-content">{{ msg.content }}</div>
          </div>
          <div v-if="aiLoading" class="ai-message ai-message-assistant">
            <div class="ai-message-content ai-loading">
              <span class="dot">·</span>
              <span class="dot">·</span>
              <span class="dot">·</span>
            </div>
          </div>
        </div>

        <div class="ai-chat-input">
          <el-input
            v-model="aiMessage"
            placeholder="输入你的问题..."
            :disabled="aiLoading"
            @keyup.enter="handleAiChat"
          >
            <template #append>
              <el-button :loading="aiLoading" @click="handleAiChat">
                发送
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.hero-section {
  background: linear-gradient(135deg, $primary-color 0%, $secondary-color 100%);
  padding: $spacing-xxl * 2 $spacing-lg;
  text-align: center;
  color: #fff;
}

.hero-content {
  @include flex-column;
  align-items: center;
  gap: $spacing-lg;
}

.hero-title {
  font-size: 40px;
  font-family: $font-family-title;
  font-weight: 700;
  letter-spacing: 2px;
}

.hero-subtitle {
  font-size: $font-size-xl;
  opacity: 0.9;
}

.hero-search {
  width: 100%;
  max-width: 560px;
  margin-top: $spacing-md;
}

.daily-card {
  margin-top: $spacing-xl;
  padding: $spacing-lg;
  background: rgba(255, 255, 255, 0.15);
  border-radius: $border-radius-md;
  cursor: pointer;
  max-width: 560px;
  width: 100%;
  backdrop-filter: blur(4px);
  transition: background $transition-base;

  &:hover {
    background: rgba(255, 255, 255, 0.25);
  }
}

.daily-content {
  font-size: $font-size-xl;
  font-family: $font-family-title;
  line-height: $line-height-loose;
  white-space: pre-line;
  margin-bottom: $spacing-sm;
}

.daily-meta {
  font-size: $font-size-sm;
  opacity: 0.85;
}

.section {
  padding: $spacing-xxl * 1.5 0;
}

.section-title {
  font-size: $font-size-title;
  color: $primary-color;
  font-family: $font-family-title;
  margin-bottom: $spacing-xl;
  text-align: center;
}

.section-header {
  @include flex-between;
  margin-bottom: $spacing-xl;

  .section-title {
    margin-bottom: 0;
    text-align: left;
  }
}

.view-more {
  font-size: $font-size-base;
  color: $primary-color;

  &:hover {
    text-decoration: underline;
  }
}

.poem-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $spacing-lg;

  @include responsive(lg) {
    grid-template-columns: repeat(2, 1fr);
  }

  @include responsive(md) {
    grid-template-columns: 1fr;
  }
}

.poem-card {
  @include card;
  cursor: pointer;
}

.poem-card-title {
  font-size: $font-size-xl;
  color: $text-color;
  font-family: $font-family-title;
  margin-bottom: $spacing-sm;
}

.poem-card-meta {
  display: flex;
  gap: $spacing-md;
  margin-bottom: $spacing-md;

  span {
    font-size: $font-size-sm;
    color: $text-color-secondary;
  }
}

.poem-card-content {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: $line-height-loose;
  white-space: pre-line;
  @include text-clamp(2);
}

.poet-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $spacing-lg;

  @include responsive(lg) {
    grid-template-columns: repeat(2, 1fr);
  }

  @include responsive(md) {
    grid-template-columns: 1fr;
  }
}

.poet-card {
  @include card;
  @include flex-column;
  align-items: center;
  text-align: center;
  cursor: pointer;
  gap: $spacing-sm;
}

.poet-name {
  font-size: $font-size-lg;
  color: $text-color;
  font-family: $font-family-title;
}

.poet-dynasty {
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.poet-desc {
  font-size: $font-size-sm;
  color: $text-color-light;
  line-height: $line-height-base;
  @include text-clamp(2);
}

.dynasty-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-md;
  justify-content: center;
}

.dynasty-tag {
  cursor: pointer;
  font-size: $font-size-lg;
  padding: $spacing-md $spacing-xl;
  transition: all $transition-base;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $box-shadow-md;
  }
}

.forum-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.forum-item {
  @include card;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}

.forum-item-title {
  font-size: $font-size-lg;
  color: $text-color;
  flex: 1;
  @include text-ellipsis;
}

.forum-item-meta {
  display: flex;
  gap: $spacing-lg;
  flex-shrink: 0;
  margin-left: $spacing-lg;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.home-footer {
  text-align: center;
  padding: $spacing-xl;
  color: $text-color-light;
  font-size: $font-size-sm;
  border-top: 1px solid $border-color;
}

.ai-section {
  background-color: #f5f7fa;
}

.section-subtitle {
  text-align: center;
  font-size: $font-size-lg;
  color: $text-color-secondary;
  margin-top: -$spacing-md;
  margin-bottom: $spacing-xl;
}

.ai-features {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-xl;

  @include responsive(md) {
    grid-template-columns: 1fr;
  }
}

.ai-feature-card {
  @include card;
  padding: $spacing-xl;
}

.ai-feature-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-md;

  h3 {
    font-size: $font-size-xl;
    color: $text-color;
    font-family: $font-family-title;
  }
}

.ai-feature-desc {
  font-size: $font-size-base;
  color: $text-color-secondary;
  margin-bottom: $spacing-lg;
}

.ai-feature-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.upload-area {
  display: flex;
  justify-content: center;
}

.image-uploader {
  width: 100%;
  max-width: 300px;
}

.upload-placeholder {
  width: 100%;
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2px dashed $border-color;
  border-radius: $border-radius-md;
  cursor: pointer;
  transition: border-color $transition-base;

  &:hover {
    border-color: $primary-color;
  }

  p {
    margin-top: $spacing-sm;
    color: $text-color-secondary;
  }
}

.image-preview {
  width: 100%;
  height: 200px;
  border-radius: $border-radius-md;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.poem-result,
.analyze-output {
  margin-top: $spacing-md;
  padding: $spacing-md;
  background-color: #f9f9f9;
  border-radius: $border-radius-md;

  h4 {
    font-size: $font-size-base;
    color: $text-color;
    margin-bottom: $spacing-sm;
  }
}

.poem-text,
.analyze-text {
  font-size: $font-size-base;
  color: $text-color;
  line-height: $line-height-loose;
  white-space: pre-line;
}

.ai-float-btn {
  position: fixed;
  right: 30px;
  bottom: 30px;
  z-index: 1000;
  width: 56px;
  height: 56px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;

  &:hover {
    transform: scale(1.1);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
  }
}

.ai-chat-container {
  display: flex;
  flex-direction: column;
  height: 500px;
}

.ai-chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md;
  border-bottom: 1px solid $border-color;
}

.ai-chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-md;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.ai-chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: $text-color-secondary;

  p {
    margin-top: $spacing-sm;
    font-size: $font-size-lg;
  }

  .ai-chat-hint {
    font-size: $font-size-sm;
    color: $text-color-light;
  }
}

.ai-message {
  display: flex;
  margin-bottom: $spacing-sm;

  &.ai-message-user {
    justify-content: flex-end;

    .ai-message-content {
      background-color: $primary-color;
      color: #fff;
      border-radius: 12px 12px 0 12px;
    }
  }

  &.ai-message-assistant {
    justify-content: flex-start;

    .ai-message-content {
      background-color: #f4f4f5;
      color: $text-color;
      border-radius: 12px 12px 12px 0;
    }
  }
}

.ai-message-content {
  max-width: 80%;
  padding: $spacing-md $spacing-lg;
  line-height: 1.5;
  word-break: break-word;
}

.ai-loading {
  .dot {
    animation: dot-blink 1.4s infinite both;
    font-size: 24px;
    line-height: 1;

    &:nth-child(2) {
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes dot-blink {
  0%, 80%, 100% {
    opacity: 0;
  }
  40% {
    opacity: 1;
  }
}

.ai-chat-input {
  padding: $spacing-md;
  border-top: 1px solid $border-color;
}
</style>
