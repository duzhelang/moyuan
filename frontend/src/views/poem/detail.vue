<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePoemStore } from '@/stores/poem'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { Comment, PoemRatingsData } from '@/types/model'
import { likePoem, favoritePoem, getPoemRatings, ratePoem, requestAiRating, regenerateAiRating, getCurrentUserRating } from '@/api/modules/poem'
import { getComments, createComment, likeComment } from '@/api/modules/forum'
import { addHistory } from '@/api/modules/history'
import { getAiModuleConfig, type AiModuleConfig, fillAiContent, getFillStatus, previewAiContent, submitForReview } from '@/api/modules/ai'
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
const regeneratingAi = ref(false)
const aiExpanded = ref(false)
const aiPollTimer = ref<ReturnType<typeof setInterval> | null>(null)
const hasRated = ref(false)
const currentRating = ref<any>(null)
const reRateDialogVisible = ref(false)

const loginPromptVisible = ref(false)

const fillStatusMap = ref<Record<string, any>>({})
const fillingField = ref('')
const previewField = ref('')
const previewContent = ref('')
const submittingReview = ref(false)

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

const aiAnalysisParagraphs = computed(() => {
  const text = ratingsData.value?.aiRating?.aiAnalysis
  if (!text) return []
  return text.split(/\n\n+/).map((p: string) => p.trim()).filter((p: string) => p.length > 0)
})

const isSectionHeader = (text: string): boolean => {
  return /^[\u4e00-\u9fa5]+[、．.]|^评分[：:]/.test(text)
}

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

const checkUserRating = async () => {
  if (!userStore.isLoggedIn) {
    hasRated.value = false
    currentRating.value = null
    return
  }
  try {
    const res = await getCurrentUserRating(poemId.value)
    if (res.data) {
      hasRated.value = true
      currentRating.value = res.data
      userScore.value = res.data.score
      userComment.value = res.data.comment || ''
    } else {
      hasRated.value = false
      currentRating.value = null
      userScore.value = 0
      userComment.value = ''
    }
  } catch (error) {
    console.error('获取用户评分失败:', error)
  }
}

const loadFillStatus = async () => {
  if (!poem.value?.id) return
  try {
    const res = await getFillStatus('poem', poem.value.id)
    if (res && Array.isArray(res)) {
      const map: Record<string, any> = {}
      res.forEach((item: any) => {
        if (item.status === 0) map[item.fieldName] = item
      })
      fillStatusMap.value = map
    }
  } catch (e) {
    // 静默失败
  }
}

const handleAiFill = async (fieldName: string) => {
  if (!userStore.isLoggedIn) {
    loginPromptVisible.value = true
    return
  }
  if (!poem.value?.id) return
  fillingField.value = fieldName
  try {
    const res = await previewAiContent({ targetType: 'poem', targetId: poem.value.id, fieldName, moduleCode: 'chat' })
    previewField.value = fieldName
    previewContent.value = res.data.content
  } catch (e: any) {
    ElMessage.error(e.message || 'AI生成失败')
  } finally {
    fillingField.value = ''
  }
}

const submittedFields = ref<Set<string>>(new Set())

const handleSubmitReview = async () => {
  if (!poem.value?.id || !previewField.value || !previewContent.value) return
  submittingReview.value = true
  try {
    await submitForReview({
      targetType: 'poem',
      targetId: poem.value.id,
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

const handleContribute = () => {
  if (!userStore.isLoggedIn) {
    loginPromptVisible.value = true
    return
  }
  ElMessage.info('内容补充功能正在开发中，敬请期待')
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
    hasRated.value = true
    fetchRatings()
    checkUserRating()
  } catch (error) {
    ElMessage.error('评分失败')
  } finally {
    submittingRating.value = false
  }
}

const handleReRate = () => {
  reRateDialogVisible.value = true
}

const confirmReRate = () => {
  reRateDialogVisible.value = false
  hasRated.value = false
  userScore.value = 0
  userComment.value = ''
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
        ElMessage.warning('AI评价处理中，请稍后刷新查看')
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
    ElMessage.success('AI评价请求已提交')
    pollAiRating()
  } catch (error) {
    ElMessage.error('AI评价请求失败')
  } finally {
    requestingAi.value = false
  }
}

const handleRegenerateAiRating = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  regeneratingAi.value = true
  try {
    await regenerateAiRating(poemId.value)
    ElMessage.success('AI重新评价请求已提交')
    pollAiRating()
  } catch (error) {
    ElMessage.error('AI重新评价请求失败')
  } finally {
    regeneratingAi.value = false
  }
}

const toggleAiExpanded = () => {
  aiExpanded.value = !aiExpanded.value
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
    const res = await chat({ message: prompt, moduleCode: 'chat' })
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

onMounted(async () => {
  await fetchPoem()
  loadFillStatus()
  fetchComments()
  fetchRatings()
  checkUserRating()
})
</script>

<template>
  <div class="poem-detail-page" v-loading="loading">
    <div class="page-decoration">
      <div class="decoration-left">
        <img src="/img/lb_shiwen_1.png" alt="" class="deco-img" />
      </div>
      <div class="decoration-right">
        <img src="/img/lb_shiwen_2.png" alt="" class="deco-img" />
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
            <p>出处：{{ poem.source === 'external' ? '古诗词库' : poem.source }}</p>
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
                <p class="empty-hint">笔耕不辍，诗人简介待整理</p>
                <el-button type="primary" plain size="small" class="contribute-btn" @click="handleContribute">
                  <el-icon><Edit /></el-icon>
                  我来补充
                </el-button>
              </div>
            </div>
          </div>

          <div class="poem-background-card info-card">
            <div class="info-card-header">
              <el-icon><Document /></el-icon>
              <h3>创作背景</h3>
            </div>
            <div class="info-card-body">
              <div v-if="submittedFields.has('background') && previewContent" class="ai-submitted">
                <div class="ai-hint">
                  <el-icon><InfoFilled /></el-icon>
                  <span>以下为AI生成内容，正在等待后台管理员审核</span>
                </div>
                <p class="preview-text">{{ previewContent }}</p>
              </div>
              <div v-else-if="previewField === 'background' && previewContent" class="ai-preview">
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
              <div v-else-if="poem.background" class="info-content">
                <p>{{ poem.background }}</p>
              </div>
              <div v-else class="info-empty">
                <p class="empty-hint">创作背景尚待考证，欢迎补充</p>
                <div class="ai-fill-actions">
                  <el-button type="primary" size="small" plain @click="sendAiChatMessage('请介绍一下这首诗的创作背景')">
                    <el-icon><MagicStick /></el-icon>
                    请求AI分析
                  </el-button>
                  <el-button v-if="fillStatusMap['background']" type="success" size="small" plain disabled>
                    <el-icon><Clock /></el-icon>
                    已提交待审核
                  </el-button>
                  <el-button v-else type="warning" size="small" plain :loading="fillingField === 'background'" @click="handleAiFill('background')">
                    <el-icon><MagicStick /></el-icon>
                    AI填充
                  </el-button>
                  <el-button type="info" size="small" plain class="contribute-btn" @click="handleContribute">
                    <el-icon><Edit /></el-icon>
                    我来补充
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <div class="poem-appreciation-card info-card">
            <div class="info-card-header">
              <el-icon><Reading /></el-icon>
              <h3>作品赏析</h3>
            </div>
            <div class="info-card-body">
              <div v-if="submittedFields.has('appreciation') && previewContent" class="ai-submitted">
                <div class="ai-hint">
                  <el-icon><InfoFilled /></el-icon>
                  <span>以下为AI生成内容，正在等待后台管理员审核</span>
                </div>
                <p class="preview-text">{{ previewContent }}</p>
              </div>
              <div v-else-if="previewField === 'appreciation' && previewContent" class="ai-preview">
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
              <div v-else-if="poem.appreciation" class="info-content">
                <p>{{ poem.appreciation }}</p>
              </div>
              <div v-else-if="poem.translation" class="info-content">
                <p>{{ poem.translation }}</p>
              </div>
              <div v-else class="info-empty">
                <p class="empty-hint">赏析内容待整理，期待您的见解</p>
                <div class="ai-fill-actions">
                  <el-button type="primary" size="small" plain @click="sendAiChatMessage('请对这首诗进行翻译赏析')">
                    <el-icon><MagicStick /></el-icon>
                    请求AI翻译赏析
                  </el-button>
                  <el-button v-if="fillStatusMap['appreciation']" type="success" size="small" plain disabled>
                    <el-icon><Clock /></el-icon>
                    已提交待审核
                  </el-button>
                  <el-button v-else type="warning" size="small" plain :loading="fillingField === 'appreciation'" @click="handleAiFill('appreciation')">
                    <el-icon><MagicStick /></el-icon>
                    AI填充
                  </el-button>
                  <el-button type="info" size="small" plain class="contribute-btn" @click="handleContribute">
                    <el-icon><Edit /></el-icon>
                    我来补充
                  </el-button>
                </div>
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
          <template v-if="ratingsData.ratingCount > 0">
            <div class="rating-score">
              <span class="score-value">{{ (ratingsData.averageScore || 0).toFixed(1) }}</span>
              <span class="score-label">综合评分</span>
            </div>
            <div class="rating-count">
              <span>{{ ratingsData.ratingCount }} 人评分</span>
            </div>
          </template>
          <template v-else>
            <div class="rating-empty">
              <span class="empty-text">暂无评分</span>
              <span class="empty-hint">期待您的评价</span>
            </div>
          </template>
        </div>

        <div class="ai-rating-card" v-if="ratingsData.aiRating">
          <el-card>
            <template #header>
              <div class="ai-rating-header">
                <span class="ai-label">
                  <el-icon><Cpu /></el-icon>
                  AI 评价
                </span>
                <span class="ai-model">{{ ratingsData.aiRating.aiModel || '未知模型' }}</span>
              </div>
            </template>
            <div class="ai-rating-content">
              <div class="ai-score">
                <span class="score-value">{{ (ratingsData.aiRating.score || 0).toFixed(1) }}</span>
              </div>
              <div class="ai-analysis">
                <div v-if="!aiExpanded && ratingsData.aiRating.aiSummary" class="ai-summary">
                  <p v-for="(line, idx) in ratingsData.aiRating.aiSummary.split(/\n\n+/).filter((s: string) => s.trim())" :key="idx">{{ line.trim() }}</p>
                  <el-button type="primary" link @click="toggleAiExpanded">
                    查看详情
                    <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                </div>
                <div v-else-if="ratingsData.aiRating.aiAnalysis" class="ai-detail">
                  <template v-for="(para, idx) in aiAnalysisParagraphs" :key="idx">
                    <p v-if="isSectionHeader(para)" class="ai-section-header">{{ para }}</p>
                    <p v-else class="ai-paragraph">{{ para }}</p>
                  </template>
                  <el-button type="primary" link @click="toggleAiExpanded" v-if="ratingsData.aiRating.aiSummary">
                    收起详情
                    <el-icon class="el-icon--right"><ArrowUp /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
            <div class="ai-rating-actions">
              <el-button
                type="warning"
                size="small"
                :loading="regeneratingAi"
                @click="handleRegenerateAiRating"
              >
                <el-icon><Refresh /></el-icon>
                重新生成
              </el-button>
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
            请求 AI 评价
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
              @click="hasRated ? handleReRate() : handleSubmitRating()"
              :disabled="userScore === 0 && !hasRated"
            >
              {{ hasRated ? '重新评价' : '提交评分' }}
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

      <el-dialog v-model="reRateDialogVisible" title="修改评分" width="400px">
        <p>您已经对这首诗评过分了，确定要修改评分吗？</p>
        <template #footer>
          <el-button @click="reRateDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmReRate">确定修改</el-button>
        </template>
      </el-dialog>

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
  background-color: $background-color-page;
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
  max-width: 900px;
  margin: 0 auto;
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
  background: $background-color-paper;
  box-shadow: 0 2px 12px rgba($primary-color, 0.08);
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
    background: linear-gradient(to right, transparent, $accent-color-vermilion, transparent);
  }

  .deco-dot {
    width: 6px;
    height: 6px;
    background: $accent-color-vermilion;
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
  background: linear-gradient(to right, transparent, $accent-color-vermilion, transparent);
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
    font-size: $font-size-xxl;
    line-height: 2.4;
    letter-spacing: 3px;
    color: $text-color-ink;
    font-family: $font-family-title;
    margin-bottom: $spacing-xs;
    text-indent: 0;
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
  justify-content: center;
  gap: $spacing-lg;
  padding: $spacing-lg $spacing-xl;
  margin-top: $spacing-xl;
  border-top: 1px solid $border-color-bronze;
  border-bottom: 1px solid $border-color-bronze;
  background: rgba($border-color-bronze, 0.08);
  border-radius: $border-radius-sm;

  .el-button {
    font-family: $font-family-title;
    letter-spacing: 1px;
  }

  .view-count {
    font-family: $font-family-title;
    color: $text-color-secondary;
  }
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
  background: $background-color-paper;
  border-radius: $border-radius-md;
  box-shadow: 0 2px 8px rgba($primary-color, 0.06);
  overflow: hidden;
  transition: all $transition-fast;
  border: 1px solid rgba($border-color-bronze, 0.3);

  &:hover {
    box-shadow: 0 4px 12px rgba($primary-color, 0.1);
    transform: translateY(-2px);
  }
}

.info-card-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md $spacing-lg;
  background: linear-gradient(135deg, rgba($primary-color, 0.04), rgba($primary-color, 0.01));
  border-bottom: 1px solid rgba($border-color-bronze, 0.3);

  .el-icon {
    color: $primary-color;
    font-size: 18px;
  }

  h3 {
    font-size: $font-size-base;
    font-weight: 600;
    color: $text-color-ink;
    margin: 0;
    font-family: $font-family-title;
    letter-spacing: 2px;
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
  padding: $spacing-lg 0;

  p {
    font-size: $font-size-sm;
    color: $text-color-light;
    margin: 0;
  }

  .empty-hint {
    font-family: $font-family-title;
    font-size: $font-size-base;
    color: $text-color-light;
    letter-spacing: 1px;
    font-style: italic;
  }

  .contribute-btn {
    margin-top: $spacing-xs;
    border-color: $border-color-bronze;
    color: $primary-color;

    &:hover {
      background: rgba($primary-color, 0.05);
    }
  }
}

.ai-fill-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ai-preview {
  background: linear-gradient(135deg, rgba($primary-color, 0.03), rgba($primary-color, 0.01));
  border: 1px solid rgba($primary-color, 0.15);
  border-radius: $border-radius-md;
  padding: $spacing-lg;
  
  .preview-text {
    margin: 0 0 $spacing-md 0;
    line-height: $line-height-loose;
    white-space: pre-line;
    color: $text-color;
    font-family: $font-family-base;
    font-size: $font-size-base;
    text-indent: 2em;
  }
  
  .preview-actions {
    display: flex;
    gap: $spacing-sm;
    justify-content: flex-end;
    padding-top: $spacing-md;
    border-top: 1px dashed $border-color;
  }
}

.ai-submitted {
  background: linear-gradient(135deg, rgba($warning-color, 0.05), rgba($warning-color, 0.02));
  border: 1px dashed rgba($warning-color, 0.3);
  border-radius: $border-radius-md;
  padding: $spacing-lg;
  
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
    
    .el-icon {
      font-size: 16px;
    }
  }
  
  .preview-text {
    margin: 0;
    line-height: $line-height-loose;
    white-space: pre-line;
    color: $text-color-secondary;
    font-family: $font-family-base;
    font-size: $font-size-base;
    text-indent: 2em;
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

  @include el-comment-input;
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
  background: $background-color-paper;
  border-radius: $border-radius-md;
  box-shadow: 0 2px 8px rgba($primary-color, 0.06);
  border: 1px solid rgba($border-color-bronze, 0.3);
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

.rating-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-xs;
  width: 100%;

  .empty-text {
    font-family: $font-family-title;
    font-size: $font-size-lg;
    color: $text-color-light;
    letter-spacing: 2px;
  }

  .empty-hint {
    font-size: $font-size-sm;
    color: $text-color-light;
  }
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

    .ai-section-header {
      font-size: $font-size-base;
      color: $primary-color;
      font-weight: 600;
      line-height: $line-height-loose;
      margin: 0 0 $spacing-sm;
      text-indent: 0;
    }

    .ai-paragraph {
      font-size: $font-size-base;
      color: $text-color;
      line-height: $line-height-loose;
      margin: 0 0 $spacing-md;
      text-indent: 2em;

      &:last-child {
        margin-bottom: 0;
      }
    }

    p {
      font-size: $font-size-base;
      color: $text-color;
      line-height: $line-height-loose;
      margin: 0 0 $spacing-md;
      text-indent: 2em;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .ai-summary,
    .ai-detail {
      .el-button {
        margin-top: $spacing-sm;
      }
    }
  }

  .ai-rating-actions {
    margin-top: $spacing-md;
    padding-top: $spacing-md;
    border-top: 1px solid $border-color-light;
    display: flex;
    justify-content: flex-end;
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
    @include el-comment-input;

    :deep(.el-textarea__inner) {
      min-height: 60px;
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