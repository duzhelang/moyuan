import { ref } from 'vue'
import type { ForumPost, Comment } from '@/types/model'
import { getForumPostList, getForumPostById, getComments, createComment } from '@/api/modules/forum'
import type { CommentCreateRequest } from '@/types/api'

export function useForum() {
  const posts = ref<ForumPost[]>([])
  const currentPost = ref<ForumPost | null>(null)
  const comments = ref<Comment[]>([])
  const loading = ref(false)
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(10)
  const commentTotal = ref(0)

  async function fetchPostList(params: { pageNum?: number; pageSize?: number; keyword?: string }) {
    loading.value = true
    try {
      const response = await getForumPostList(params)
      posts.value = response.data.list
      total.value = response.data.total
      currentPage.value = response.data.pageNum
      pageSize.value = response.data.pageSize
    } catch (error) {
      console.error('获取帖子列表失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function fetchPostDetail(id: number) {
    loading.value = true
    try {
      const response = await getForumPostById(id)
      currentPost.value = response.data
    } catch (error) {
      console.error('获取帖子详情失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function fetchComments(targetId: number, targetType: number, params?: { pageNum?: number; pageSize?: number }) {
    try {
      const response = await getComments(targetId, targetType, params)
      comments.value = response.data.list
      commentTotal.value = response.data.total
    } catch (error) {
      console.error('获取评论失败:', error)
      throw error
    }
  }

  async function submitComment(data: CommentCreateRequest) {
    try {
      await createComment(data)
    } catch (error) {
      console.error('提交评论失败:', error)
      throw error
    }
  }

  return {
    posts,
    currentPost,
    comments,
    loading,
    total,
    currentPage,
    pageSize,
    commentTotal,
    fetchPostList,
    fetchPostDetail,
    fetchComments,
    submitComment
  }
}