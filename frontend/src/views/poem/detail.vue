<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePoemStore } from '@/stores/poem'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { Comment, PoemRatingsData } from '@/types/model'
import { likePoem, favoritePoem, getPoemRatings, ratePoem, requestAiRating } from '@/api/modules/poem'
import { getComments, createComment, likeComment } from '@/api/modules/forum'
import { addHistory } from '@/api/modules/history'
import { getAiModuleConfig, type AiModuleConfig } from '@/api/modules/ai'
import LoginPrompt from '@/components/common/LoginPrompt.vue'

const route = useRoute()
const router = useRouter()
const poemStore = usePoemStore()
const userStore = useUserStore()

const loading = ref(false)
const poem = computed(() => poemStore.currentPoem)
const comments = ref<Comment[]>([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentSize = ref(10)

const newComment = ref('')
const submittingComment = ref(false)

const poemId = computed(() => Number(route.params.id))

const ratingsData = ref<PoemRatingsData | null>(null)
const userScore = ref(0)
const userComment = ref('')
const submittingRating = ref(false)
const requestingAi = ref(false)
const aiPollTimer = ref<ReturnType<typeof setInterval> | null>(null)

const loginPromptVisible = ref(false)

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

const poemSentences = computed(() => {
  if (!poem.value?.content) return []
  return poem.value.content
    .split(/[。！？；\n]+/)
    .map((s: string) => s.trim())
    .filter((s: string) => s.length > 0)
})

const fetchPoem = async () => {
  loading.value = true
  try {
    await poemStore.fetchPoemDetail(poemId.value)
    addHistory(route.params.id as unknown as number, 1).catch(() => {})
  } catch (error) {
    ElMessage.error('获取诗词详情失败')
    router.push('/poem')
  } finally {
    loading.value = false
  }
}

const fetchComments = async () => {
  try {
    const res = await getComments(poemId.value, 1, {
      pageNum: commentPage.value,
      pageSize: commentSize.value
    })
    comments.value = res.data.list
    commentTotal.value = res.data.total
  } catch (error) {
    console.error('获取评论失败:', error)
  }
}

const fetchRatings = async () => {
  try {
    const res = await getPoemRatings(poemId.value)
    ratingsData.value = res.data
  } catch (error) {
    ElMessage.error('获取评分失败')
  }
}

const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await likePoem(poemId.value)
    if (poem.value) {
      const liked = !poem.value.isLiked
      poem.value.isLiked = liked
      poem.value.likeCount += liked ? 1 : -1
      ElMessage.success(liked ? '点赞成功' : '已取消点赞')
    }
  } catch (error) {
    ElMessage.error('点赞失败')
  }
}

const handleFavorite = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await favoritePoem(poemId.value)
    if (poem.value) {
      const favorited = !poem.value.isFavorited
      poem.value.isFavorited = favorited
      poem.value.favoriteCount += favorited ? 1 : -1
      ElMessage.success(favorited ? '收藏成功' : '已取消收藏')
    }
  } catch (error) {
    ElMessage.error('收藏失败')
  }
}

const handleSubmitRating = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再评分')
    return
  }
  if (userScore.value === 0) {
    ElMessage.warning('请选择评分')
    return
  }
  submittingRating.value = true
  try {
    await ratePoem(poemId.value, userScore.value, userComment.value || undefined)
    ElMessage.success('评分成功')
    userScore.value = 0
    userComment.value = ''
    fetchRatings()
  } catch (error) {
    ElMessage.error('评分失败')
  } finally {
    submittingRating.value = false
  }
}

const pollAiRating = () => {
  let attempts = 0
  const maxAttempts = 10
  aiPollTimer.value = setInterval(async () => {
    attempts++
    await fetchRatings()
    if (ratingsData.value?.aiRating || attempts >= maxAttempts) {
      if (aiPollTimer.value) {
        clearInterval(aiPollTimer.value)
        aiPollTimer.value = null
      }
      if (attempts >= maxAttempts && !ratingsData.value?.aiRating) {
        ElMessage.warning('AI评分处理中，请稍后刷新查看')
      }
    }
  }, 1500)
}

onUnmounted(() => {
  if (aiPollTimer.value) {
    clearInterval(aiPollTimer.value)
    aiPollTimer.value = null
  }
})

const handleRequestAiRating = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  requestingAi.value = true
  try {
    await requestAiRating(poemId.value)
    ElMessage.success('AI评分请求已提交')
    pollAiRating()
  } catch (error) {
    ElMessage.error('AI评分请求失败')
  } finally {
    requestingAi.value = false
  }
}

const handleSubmitComment = async () => {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  submittingComment.value = true
  try {
    await createComment({
      content: newComment.value,
      targetId: poemId.value,
      targetType: 1
    })
    ElMessage.success('评论成功')
    newComment.value = ''
    fetchComments()
  } catch (error) {
    ElMessage.error('评论失败')
  } finally {
    submittingComment.value = false
  }
}

const handleCommentLike = async (comment: Comment) => {
  try {
    await likeComment(comment.id)
    comment.likeCount++
    ElMessage.success('点赞成功')
  } catch (error) {
    ElMessage.error('点赞失败')
  }
}

const handleCommentPageChange = (page: number) => {
  commentPage.value = page
  fetchComments()
}

const openExternalLink = (url: string) => {
  window.open(url, '_blank', 'noopener,noreferrer')
}

const scrollToAiChatBottom = () => {
  nextTick(() => {
    if (aiChatContainerRef.value) {
      aiChatContainerRef.value.scrollTop = aiChatContainerRef.value.scrollHeight
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
    const res = await getAiModuleConfig('chat')
    aiConfig.value = res.data
  } catch {
    console.error('获取AI配置失败')
  }
}

const buildAiPrompt = (userMessage: string, isFirst: boolean = false) => {
  const poemTitle = poem.value?.title || '这首诗'
  const poemContent = poem.value?.content || ''
  const config = aiConfig.value

  let systemConstraint = ''
  if (config?.promptTemplate) {
    const maxLength = isFirst ? (config.firstResponseLength || 80) : (config.maxLength || 200)
    const styleHint = isFirst
      ? `这是首次提问，请用2-3句话简要概括即可，不超过${config.firstResponseLength || 80}字`
      : `根据问题复杂度适当展开，但不超过${maxLength}字`

    systemConstraint = config.promptTemplate
      .replace(/\{poetName\}/g, poem.value?.poetName || '')
      .replace(/\{poemTitle\}/g, poemTitle)
      .replace(/\{poemContent\}/g, poemContent)
      .replace(/\{maxLength\}/g, String(maxLength))
      .replace(/\{styleHint\}/g, styleHint)

    if (!systemConstraint.includes(poemTitle)) {
      systemConstraint += `\n\n当前诗词：《${poemTitle}》\n诗词内容：${poemContent}`
    }
  } else {
    systemConstraint = `你是一位文化渊博的大学者，精通古今中外诗歌知识。你正在为用户赏析诗歌《${poemTitle}》。

诗词内容：
${poemContent}

回答要求：
1. 语言简洁精炼，避免冗余
2. 使用通俗易懂的中文
3. 重点突出，条理清晰
4. 不要使用markdown格式，直接输出纯文本
${isFirst ? '5. 这是首次提问，请用2-3句话简要概括即可' : '5. 根据问题复杂度适当展开，但不超过200字'}`
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
    await fetchAiConfig(false)
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
    const res = await chat({ message: prompt })
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

const toggleAiChat = () => {
  aiChatVisible.value = !aiChatVisible.value
  if (aiChatVisible.value && userStore.isLoggedIn && aiChatMessages.value.length === 0) {
    fetchAiConfig()
    sendAiChatMessage('请简要介绍一下这首诗', true)
  }
}

onMounted(() => {
  fetchPoem()
  fetchComments()
  fetchRatings()
})
</script>

<template>
  <div class="poem-detail-page" v-loading="loading">
    <div class="page-decoration">
      <div class="decoration-left">
        <img src="/img/lb_shiwen (1).png" alt="" class="deco-img" />
      </div>
      <div class="decoration-right">
        <img src="/img/lb_shiwen (2).png" alt="" class="deco-img" />
      </div>
    </div>

    <div class="container" v-if="poem">
      <div class="poem-header">
        <el-button @click="router.push('/poem')" class="back-button">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>
      
      <el-card class="poem-card">
        <div class="poem-title-section">
          <div class="title-decoration">
            <span class="deco-line"></span>
            <span class="deco-dot"></span>
            <span class="deco-line"></span>
          </div>
          <h1 class="poem-title">{{ poem.title }}</h1>
          <div class="title-underline"></div>
          <div class="poem-meta">
            <span v-if="poem.dynastyName" class="meta-tag">
              <el-icon><Calendar /></el-icon>
              {{ poem.dynastyName }}
            </span>
            <span v-if="poem.poetName" class="meta-tag">
              <el-icon><User /></el-icon>
              {{ poem.poetName }}
            </span>
            <span v-if="poem.categoryName" class="meta-tag">
              <el-icon><Collection /></el-icon>
              {{ poem.categoryName }}
            </span>
          </div>
        </div>
        
        <div class="poem-content-section">
          <div class="poem-text">
            <p v-for="(sentence, index) in poemSentences" :key="index" class="poem-sentence">
              {{ sentence }}
            </p>
          </div>
          
          <div class="poem-source" v-if="poem.source">
            <p>出处：{{ poem.source }}</p>
          </div>
        </div>
        
        <div class="poem-actions">
          <el-button :type="poem.isLiked ? 'danger' : 'primary'" @click="handleLike">
            <el-icon><Star /></el-icon>
            {{ poem.isLiked ? '已点赞' : '点赞' }} ({{ poem.likeCount }})
          </el-button>
          <el-button :type="poem.isFavorited ? 'danger' : 'warning'" @click="handleFavorite">
            <el-icon><CollectionTag /></el-icon>
            {{ poem.isFavorited ? '已收藏' : '收藏' }} ({{ poem.favoriteCount }})
          </el-button>
          <span class="view-count">
            <el-icon><View /></el-icon>
            {{ poem.viewCount }} 次浏览
          </span>
        </div>
      </el-card>

      <div class="poem-info-section">
        <div class="info-grid">
          <div class="poet-brief-card info-card">
            <div class="info-card-header">
              <el-icon><User /></el-icon>
              <h3>诗人简介</h3>
            </div>
            <div class="info-card-body">
              <div v-if="poem.poetId" class="poet-brief-content">
                <div class="poet-brief-meta">
                  <el-avatar :size="48">{{ poem.poetName?.charAt(0) }}</el-avatar>
                  <div class="poet-brief-info">
                    <span class="poet-brief-name">{{ poem.poetName }}</span>
                    <span class="poet-brief-dynasty" v-if="poem.dynastyName">{{ poem.dynastyName }}</span>
                  </div>
                </div>
                <router-link :to="`/poet/${poem.poetId}`" class="poet-link">
                  了解更多诗人信息 →
                </router-link>
              </div>
              <div v-else class="info-empty">
                <p>暂无诗人信息</p>
              </div>
            </div>
          </div>

          <div class="poem-background-card info-card">
            <div class="info-card-header">
              <el-icon><Document /></el-icon>
              <h3>创作背景</h3>
            </div>
            <div class="info-card-body">
              <div v-if="poem.background" class="info-content">
                <p>{{ poem.background }}</p>
              </div>
              <div v-else class="info-empty">
                <p>暂无创作背景信息</p>
                <el-button type="primary" size="small" plain @click="sendAiChatMessage('请介绍一下这首诗的创作背景')">
                  <el-icon><MagicStick /></el-icon>
                  请求AI分析
                </el-button>
              </div>
            </div>
          </div>

          <div class="poem-appreciation-card info-card">
            <div class="info-card-header">
              <el-icon><Reading /></el-icon>
              <h3>作品赏析</h3>
            </div>
            <div class="info-card-body">
              <div v-if="poem.appreciation" class="info-content">
                <p>{{ poem.appreciation }}</p>
              </div>
              <div v-else-if="poem.translation" class="info-content">
                <p>{{ poem.translation }}</p>
              </div>
              <div v-else class="info-empty">
                <p>暂无赏析内容</p>
                <el-button type="primary" size="small" plain @click="sendAiChatMessage('请对这首诗进行翻译赏析')">
                  <el-icon><MagicStick /></el-icon>
                  请求AI翻译赏析
                </el-button>
              </div>
            </div>
          </div>

          <div class="poem-resources-card info-card">
            <div class="info-card-header">
              <el-icon><Link /></el-icon>
              <h3>相关资源</h3>
            </div>
            <div class="info-card-body">
              <div class="resource-list">
                <div class="resource-item" @click="openExternalLink('https://www.gushiwen.org/')">
                  <el-icon><Document /></el-icon>
                  <span>古诗文网</span>
                  <el-icon class="link-icon"><Top /></el-icon>
                </div>
                <div class="resource-item" @click="openExternalLink('https://www.zhonghuadiancang.com/')">
                  <el-icon><Document /></el-icon>
                  <span>中华典藏</span>
                  <el-icon class="link-icon"><Top /></el-icon>
                </div>
                <div class="resource-item" @click="openExternalLink('https://ctext.org/')">
                  <el-icon><Document /></el-icon>
                  <span>中国哲学书电子化计划</span>
                  <el-icon class="link-icon"><Top /></el-icon>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="rating-section" v-if="ratingsData">
        <h2 class="section-title">评分区域</h2>

        <div class="rating-overview">
          <div class="rating-score">
            <span class="score-value">{{ (ratingsData.averageScore || 0).toFixed(1) }}</span>
            <span class="score-label">综合评分</span>
          </div>
          <div class="rating-count">
            <span>{{ ratingsData.ratingCount }} 人评分</span>
          </div>
        </div>

        <div class="ai-rating-card" v-if="ratingsData.aiRating">
          <el-card>
            <template #header>
              <div class="ai-rating-header">
                <span class="ai-label">
                  <el-icon><Cpu /></el-icon>
                  AI 评分
                </span>
                <span class="ai-model">{{ ratingsData.aiRating.aiModel || '未知模型' }}</span>
              </div>
            </template>
            <div class="ai-rating-content">
              <div class="ai-score">
                <span class="score-value">{{ (ratingsData.aiRating.score || 0).toFixed(1) }}</span>
              </div>
              <div class="ai-analysis" v-if="ratingsData.aiRating.aiAnalysis">
                <p>{{ ratingsData.aiRating.aiAnalysis }}</p>
              </div>
            </div>
          </el-card>
        </div>

        <div class="request-ai-section" v-else>
          <el-button
            type="primary"
            :loading="requestingAi"
            @click="handleRequestAiRating"
          >
            <el-icon><Cpu /></el-icon>
            请求 AI 评分
          </el-button>
        </div>

        <div class="user-rating-section">
          <h3 class="sub-title">用户评分</h3>
          <div class="rating-form">
            <div class="score-select">
              <label>评分：</label>
              <el-rate
                v-model="userScore"
                :max="5"
                show-score
                score-template="{value} 分"
              />
            </div>
            <div class="comment-input">
              <el-input
                v-model="userComment"
                type="textarea"
                :rows="3"
                placeholder="写下你的评价（可选）..."
                maxlength="200"
                show-word-limit
              />
            </div>
            <el-button
              type="primary"
              :loading="submittingRating"
              @click="handleSubmitRating"
              :disabled="userScore === 0"
            >
              提交评分
            </el-button>
          </div>
        </div>

        <div class="user-ratings-list" v-if="ratingsData.userRatings.length > 0">
          <h3 class="sub-title">用户评价</h3>
          <div
            v-for="rating in ratingsData.userRatings"
            :key="rating.id"
            class="rating-item"
          >
            <div class="rating-item-header">
              <el-avatar :src="rating.avatar" :size="32">
                {{ rating.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span class="rating-username">{{ rating.username }}</span>
              <el-rate :model-value="rating.score" disabled :max="5" />
              <span class="rating-time">{{ rating.createTime }}</span>
            </div>
            <div class="rating-comment" v-if="rating.comment">
              <p>{{ rating.comment }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="comment-section">
        <h2 class="section-title">评论区</h2>
        
        <div class="comment-form">
          <el-input
            v-model="newComment"
            type="textarea"
            :rows="4"
            placeholder="写下你的评论..."
            maxlength="500"
            show-word-limit
          />
          <el-button
            type="primary"
            :loading="submittingComment"
            @click="handleSubmitComment"
            class="submit-button"
          >
            发表评论
          </el-button>
        </div>
        
        <div class="comment-list">
          <div v-if="comments.length === 0" class="empty-comments">
            <el-empty description="暂无评论，快来发表第一条评论吧" />
          </div>
          
          <div
            v-for="comment in comments"
            :key="comment.id"
            class="comment-item"
          >
            <div class="comment-header">
              <el-avatar :src="comment.avatar" :size="40">
                {{ comment.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <div class="comment-meta">
                <span class="comment-author">{{ comment.username }}</span>
                <span class="comment-time">{{ comment.createTime }}</span>
              </div>
            </div>
            <div class="comment-content">
              <p>{{ comment.content }}</p>
            </div>
            <div class="comment-actions">
              <el-button text size="small" @click="handleCommentLike(comment)">
                <el-icon><Star /></el-icon>
                {{ comment.likeCount }}
              </el-button>
              <el-button text size="small">
                <el-icon><ChatDotRound /></el-icon>
                回复
              </el-button>
            </div>
          </div>
        </div>
        
        <div class="comment-pagination" v-if="commentTotal > commentSize">
          <el-pagination
            v-model:current-page="commentPage"
            :page-size="commentSize"
            :total="commentTotal"
            layout="prev, pager, next"
            @current-change="handleCommentPageChange"
          />
        </div>
      </div>
    </div>

    <div class="ai-float-btn" @click="toggleAiChat" :class="{ active: aiChatVisible }">
      <el-icon :size="24"><MagicStick /></el-icon>
    </div>

    <Teleport to="body">
      <Transition name="ai-chat-slide">
        <div v-if="aiChatVisible" class="ai-chat-dialog">
          <div class="ai-chat-header">
            <div class="ai-chat-title">
              <el-icon><MagicStick /></el-icon>
              <span>AI诗词助手</span>
            </div>
            <el-icon class="ai-chat-close" @click="aiChatVisible = false"><Close /></el-icon>
          </div>
          <div class="ai-chat-messages" ref="aiChatContainerRef">
            <div
              v-for="(msg, index) in aiChatMessages"
              :key="index"
              :class="['chat-message', msg.role]"
            >
              <div class="message-avatar">
                <el-avatar :size="32" v-if="msg.role === 'user'">我</el-avatar>
                <el-avatar :size="32" v-else class="ai-avatar">AI</el-avatar>
              </div>
              <div class="message-body">
                <div class="message-content">{{ msg.content }}</div>
              </div>
            </div>
            <div v-if="aiChatLoading" class="chat-message assistant">
              <div class="message-avatar">
                <el-avatar :size="32" class="ai-avatar">AI</el-avatar>
              </div>
              <div class="message-body">
                <div class="message-content loading">思考中...</div>
              </div>
            </div>
          </div>
          <div class="ai-chat-input">
            <el-input
              v-model="aiChatInput"
              type="textarea"
              :rows="2"
              placeholder="输入问题，按Enter发送..."
              @keydown="handleAiChatKeydown"
              :disabled="aiChatLoading"
            />
            <el-button
              type="primary"
              @click="sendAiChatMessage()"
              :loading="aiChatLoading"
              :disabled="!aiChatInput.trim()"
            >
              <el-icon><Promotion /></el-icon>
            </el-button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <LoginPrompt
      v-model:visible="loginPromptVisible"
      message="AI对话功能需要登录后才能使用"
    />
  </div>
</template>

<style scoped lang="scss">
@use 'sass:color';

.poem-detail-page {
  padding: 70px 0 $spacing-xl;
  min-height: 60vh;
  position: relative;
  overflow-x: hidden;
}

.page-decoration {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
}

.decoration-left {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0.12;

  .deco-img {
    width: 180px;
    height: auto;
  }
}

.decoration-right {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0.12;

  .deco-img {
    width: 180px;
    height: auto;
  }
}

.container {
  position: relative;
  z-index: 1;
}

.poem-header {
  margin-bottom: $spacing-lg;
}

.back-button {
  margin-bottom: $spacing-md;
}

.poem-card {
  margin-bottom: $spacing-xl;
  border-radius: $border-radius-md;
  padding: $spacing-xl;
}

.poem-title-section {
  text-align: center;
  margin-bottom: $spacing-xl;
  padding-bottom: $spacing-xl;
}

.title-decoration {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;

  .deco-line {
    width: 60px;
    height: 1px;
    background: linear-gradient(to right, transparent, $primary-color, transparent);
  }

  .deco-dot {
    width: 6px;
    height: 6px;
    background: $primary-color;
    border-radius: 50%;
  }
}

.poem-title {
  font-size: $font-size-hero;
  color: $primary-color;
  font-family: $font-family-title;
  margin-bottom: $spacing-md;
  letter-spacing: 4px;
}

.title-underline {
  width: 80px;
  height: 2px;
  background: linear-gradient(to right, transparent, $primary-color, transparent);
  margin: $spacing-md auto $spacing-lg;
}

.poem-meta {
  display: flex;
  justify-content: center;
  gap: $spacing-md;
  flex-wrap: wrap;
}

.meta-tag {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-md;
  background: rgba($primary-color, 0.06);
  border: 1px solid rgba($primary-color, 0.15);
  border-radius: 20px;
  font-size: $font-size-sm;
  color: $text-color-secondary;
  transition: all $transition-fast;

  &:hover {
    background: rgba($primary-color, 0.1);
    border-color: rgba($primary-color, 0.25);
  }
}

.poem-content-section {
  margin-bottom: $spacing-xl;
  padding: $spacing-xl 0;
}

.poem-text {
  text-align: center;
  margin-bottom: $spacing-lg;
  
  .poem-sentence {
    font-size: $font-size-xl;
    line-height: 2.2;
    letter-spacing: 2px;
    color: $text-color;
    font-family: $font-family-title;
    margin-bottom: $spacing-xs;
  }
}

.poem-source {
  text-align: center;
  margin-top: $spacing-xl;
  padding-top: $spacing-lg;
  border-top: 1px dashed $border-color-light;
  
  p {
    font-size: $font-size-sm;
    color: $text-color-light;
    font-style: italic;
    margin: 0;
  }
}

.poem-actions {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding-top: $spacing-xl;
  border-top: 1px solid $border-color;
}

.poem-info-section {
  margin-top: $spacing-xl;
  margin-bottom: $spacing-xl;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-lg;

  @include responsive(md) {
    grid-template-columns: 1fr;
  }
}

.info-card {
  background: $background-color-light;
  border-radius: $border-radius-md;
  box-shadow: $box-shadow;
  overflow: hidden;
  transition: all $transition-fast;

  &:hover {
    box-shadow: $box-shadow-md;
    transform: translateY(-2px);
  }
}

.info-card-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md $spacing-lg;
  background: linear-gradient(135deg, rgba($primary-color, 0.05), rgba($primary-color, 0.02));
  border-bottom: 1px solid $border-color-light;

  .el-icon {
    color: $primary-color;
    font-size: 18px;
  }

  h3 {
    font-size: $font-size-base;
    font-weight: 600;
    color: $text-color;
    margin: 0;
    font-family: $font-family-title;
  }
}

.info-card-body {
  padding: $spacing-lg;
}

.poet-brief-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.poet-brief-meta {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.poet-brief-info {
  display: flex;
  flex-direction: column;
}

.poet-brief-name {
  font-size: $font-size-lg;
  font-weight: 600;
  color: $text-color;
}

.poet-brief-dynasty {
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.poet-link {
  display: inline-flex;
  align-items: center;
  color: $primary-color;
  font-size: $font-size-sm;
  text-decoration: none;
  transition: color $transition-fast;

  &:hover {
    color: color.adjust($primary-color, $lightness: -10%);
    text-decoration: underline;
  }
}

.info-content {
  p {
    font-size: $font-size-base;
    color: $text-color;
    line-height: $line-height-loose;
    margin: 0;
    white-space: pre-line;
  }
}

.info-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-md 0;

  p {
    font-size: $font-size-sm;
    color: $text-color-light;
    margin: 0;
  }
}

.resource-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.resource-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  border-radius: $border-radius-sm;
  cursor: pointer;
  transition: all $transition-fast;
  border: 1px solid transparent;

  .el-icon {
    color: $primary-color;
    font-size: 16px;
  }

  span {
    flex: 1;
    font-size: $font-size-sm;
    color: $text-color;
  }

  .link-icon {
    color: $text-color-light;
    font-size: 14px;
    transform: rotate(45deg);
  }

  &:hover {
    background: rgba($primary-color, 0.05);
    border-color: rgba($primary-color, 0.1);
  }
}

.view-count {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-secondary;
  margin-left: auto;
}

.comment-section {
  margin-top: $spacing-xl;
  background: $background-color-light;
  border-radius: $border-radius-md;
  padding: $spacing-xl;
  box-shadow: $box-shadow;
}

.section-title {
  font-size: $font-size-xl;
  color: $primary-color;
  margin-bottom: $spacing-lg;
  padding-bottom: $spacing-sm;
  border-bottom: 2px solid $primary-color;
  display: inline-block;
  font-family: $font-family-title;
}

.comment-form {
  margin-bottom: $spacing-xl;
  padding: $spacing-lg;
  background: $background-color;
  border-radius: $border-radius-md;
  position: relative;
  z-index: 2;

  :deep(.el-textarea) {
    width: 100%;
  }

  :deep(.el-textarea__inner) {
    font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
    color: #333;
    letter-spacing: 0.5px;
    line-height: 1.6;
    resize: vertical;
    min-height: 80px;
    padding: 12px;
    background-color: #fff;
    border: 1px solid $border-color;
    border-radius: $border-radius-sm;
    transition: border-color 0.2s, box-shadow 0.2s;

    &::placeholder {
      color: #999;
    }

    &:focus {
      border-color: $primary-color;
      box-shadow: 0 0 0 2px rgba($primary-color, 0.15);
      outline: none;
    }
  }
}

.submit-button {
  margin-top: $spacing-md;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.empty-comments {
  padding: $spacing-xxl 0;
}

.comment-item {
  @include card;
  padding: $spacing-lg;
  border-left: 3px solid transparent;
  transition: all $transition-fast;

  &:hover {
    border-left-color: $primary-color;
  }
}

.comment-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.comment-meta {
  display: flex;
  flex-direction: column;
}

.comment-author {
  font-size: $font-size-base;
  color: $text-color;
  font-weight: 600;
}

.comment-time {
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.comment-content {
  margin-bottom: $spacing-md;
  
  p {
    font-size: $font-size-base;
    color: $text-color;
    line-height: $line-height-loose;
  }
}

.comment-actions {
  display: flex;
  gap: $spacing-sm;
}

.comment-pagination {
  margin-top: $spacing-xl;
  display: flex;
  justify-content: center;
}

.rating-section {
  margin-top: $spacing-xl;
  padding: $spacing-xl;
  background: $background-color-light;
  border-radius: $border-radius-md;
  box-shadow: $box-shadow;
}

.rating-overview {
  display: flex;
  align-items: center;
  gap: $spacing-xl;
  margin-bottom: $spacing-xl;
  padding: $spacing-lg;
  background: linear-gradient(135deg, rgba($primary-color, 0.05), rgba($accent-color, 0.05));
  border-radius: $border-radius-md;
}

.rating-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-xs;
}

.score-value {
  font-size: $font-size-hero;
  font-weight: bold;
  color: $warning-color;
  font-family: $font-family-title;
}

.score-label {
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.rating-count {
  font-size: $font-size-base;
  color: $text-color-secondary;
}

.ai-rating-card {
  margin-bottom: $spacing-xl;

  .ai-rating-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .ai-label {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
    font-weight: 600;
    color: $primary-color;
  }

  .ai-model {
    font-size: $font-size-sm;
    color: $text-color-light;
  }

  .ai-rating-content {
    display: flex;
    gap: $spacing-xl;
    align-items: flex-start;
  }

  .ai-score {
    flex-shrink: 0;

    .score-value {
      font-size: $font-size-xxl;
      color: $primary-color;
    }
  }

  .ai-analysis {
    flex: 1;

    p {
      font-size: $font-size-base;
      color: $text-color;
      line-height: $line-height-loose;
      margin: 0;
    }
  }
}

.request-ai-section {
  margin-bottom: $spacing-xl;
  text-align: center;
  padding: $spacing-lg;
  background: $background-color;
  border-radius: $border-radius-md;
}

.user-rating-section {
  margin-bottom: $spacing-xl;

  .sub-title {
    font-size: $font-size-lg;
    color: $text-color;
    margin-bottom: $spacing-md;
    font-family: $font-family-title;
  }
}

.rating-form {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;

  .score-select {
    display: flex;
    align-items: center;
    gap: $spacing-md;

    label {
      font-size: $font-size-base;
      color: $text-color;
      white-space: nowrap;
    }
  }

  .comment-input {
    :deep(.el-textarea__inner) {
      font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
      color: #333;
      letter-spacing: 0.5px;
      line-height: 1.6;
      resize: vertical;
      min-height: 60px;
      padding: 12px;
      background-color: #fff;
      border: 1px solid $border-color;
      border-radius: $border-radius-sm;
      transition: border-color 0.2s, box-shadow 0.2s;

      &::placeholder {
        color: #999;
      }

      &:focus {
        border-color: $primary-color;
        box-shadow: 0 0 0 2px rgba($primary-color, 0.15);
        outline: none;
      }
    }
  }
}

.user-ratings-list {
  .sub-title {
    font-size: $font-size-lg;
    color: $text-color;
    margin-bottom: $spacing-md;
    font-family: $font-family-title;
  }
}

.rating-item {
  padding: $spacing-md;
  border-bottom: 1px solid $border-color-light;

  &:last-child {
    border-bottom: none;
  }
}

.rating-item-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-sm;
}

.rating-username {
  font-size: $font-size-base;
  color: $text-color;
  font-weight: 600;
}

.rating-time {
  font-size: $font-size-sm;
  color: $text-color-light;
  margin-left: auto;
}

.rating-comment {
  p {
    font-size: $font-size-base;
    color: $text-color-secondary;
    line-height: $line-height-loose;
    margin: 0;
  }
}

.ai-float-btn {
  position: fixed;
  right: 30px;
  bottom: 30px;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1000;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: all $transition-fast;

  .el-icon {
    color: white;
    font-size: 24px;
  }

  &:hover {
    transform: scale(1.1);
    box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
  }

  &.active {
    background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
  }
}

.ai-chat-dialog {
  position: fixed;
  right: 90px;
  bottom: 90px;
  width: 380px;
  height: 500px;
  background: $background-color-light;
  border-radius: $border-radius-lg;
  box-shadow: $box-shadow-xl;
  display: flex;
  flex-direction: column;
  z-index: 1001;
  overflow: hidden;
}

.ai-chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md $spacing-lg;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;

  .ai-chat-title {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: $font-size-base;
    font-weight: 600;
  }

  .ai-chat-close {
    cursor: pointer;
    font-size: 18px;
    opacity: 0.8;
    transition: opacity $transition-fast;

    &:hover {
      opacity: 1;
    }
  }
}

.ai-chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-md;
  @include scrollbar;

  .chat-message {
    display: flex;
    gap: $spacing-sm;
    margin-bottom: $spacing-md;
    align-items: flex-start;
    animation: fadeIn 0.3s ease;

    &.user {
      flex-direction: row-reverse;

      .message-content {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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

  .message-avatar {
    flex-shrink: 0;

    .ai-avatar {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      font-size: 11px;
      font-weight: 600;
      box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
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

.ai-chat-input {
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
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);

    &:hover {
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
    }
  }
}

.ai-chat-slide-enter-active,
.ai-chat-slide-leave-active {
  transition: all 0.3s ease;
}

.ai-chat-slide-enter-from,
.ai-chat-slide-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}
</style>