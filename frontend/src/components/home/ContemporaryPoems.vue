<template>
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
                  :class="{ active: poemLikeStates[poemKey(idx)] }"
                  @click="handlePoemLike(poemKey(idx))"
                >
                  <span class="action_icon">&#10084;</span>
                  <span class="action_count">{{ poemLikeCounts[poemKey(idx)] || '' }}</span>
                </button>
                <button
                  class="contemporary_action_btn"
                  :class="{ active: poemFavoriteStates[poemKey(idx)] }"
                  @click="handlePoemFavorite(poemKey(idx))"
                >
                  <span class="action_icon">&#9733;</span>
                </button>
                <button
                  class="contemporary_action_btn"
                  @click="togglePoemComments(poemKey(idx), 2)"
                >
                  <span class="action_icon">&#128172;</span>
                  <span class="action_count">{{ commentTotals[poemKey(idx)] || '' }}</span>
                </button>
              </div>
            </div>
          </div>
          <div class="contemporary_comment_section" v-if="expandedPoemIdx === poemKey(idx)">
            <div class="contemporary_comment_input_bar" v-if="userStore.isLoggedIn">
              <input
                v-model="commentInputs[poemKey(idx)]"
                class="contemporary_comment_input"
                placeholder="写下你的想法..."
                @keyup.enter="handleSubmitPoemComment(poemKey(idx), 2)"
              />
              <button
                class="contemporary_comment_submit"
                @click="handleSubmitPoemComment(poemKey(idx), 2)"
                :disabled="submittingComment[poemKey(idx)]"
              >
                {{ submittingComment[poemKey(idx)] ? '...' : '发送' }}
              </button>
            </div>
            <div class="contemporary_comment_login" v-else @click="router.push('/user/login')">
              <span>登录后参与讨论</span>
            </div>
            <div class="contemporary_comment_list" v-if="poemComments[poemKey(idx)]?.length">
              <div
                v-for="comment in poemComments[poemKey(idx)]"
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
            <div class="contemporary_comment_empty" v-else-if="commentTotals[poemKey(idx)] === 0">
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
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { likePoem, favoritePoem } from '@/api/modules/poem'
import { getComments, createComment, likeComment } from '@/api/modules/forum'
import type { Comment } from '@/types/model'
import contemporaryPoemsData from '@/data/home-contemporary-poems.json'

interface PoemCard {
  date: string
  title?: string
  content: string
  author: string
  image?: string
}

const router = useRouter()
const userStore = useUserStore()

const contemporaryPoems: PoemCard[] = contemporaryPoemsData

const asset = (path: string) => path

const expandedPoemIdx = ref<string | null>(null)
const poemComments = ref<Record<string, Comment[]>>({})
const commentInputs = ref<Record<string, string>>({})
const submittingComment = ref<Record<string, boolean>>({})
const commentTotals = ref<Record<string, number>>({})

const poemLikeStates = ref<Record<string, boolean>>({})
const poemFavoriteStates = ref<Record<string, boolean>>({})
const poemLikeCounts = ref<Record<string, number>>({})
const poemFavoriteCounts = ref<Record<string, number>>({})

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

const poemKey = (idx: number) => String((contemporaryPage.value - 1) * contemporaryPageSize + idx)

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
</script>

<style scoped>
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
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
  letter-spacing: 0.5px;
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
