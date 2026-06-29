import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  getForumPostList,
  getForumPostById,
  createForumPost,
  updateForumPost,
  deleteForumPost,
  likeForumPost,
  getComments,
  createComment,
  deleteComment,
  likeComment
} from './forum'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('forum API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getForumPostList', () => {
    it('调用GET /forum/posts', async () => {
      const params = { pageNum: 1, pageSize: 10, keyword: '测试' }
      await getForumPostList(params)
      expect(request.get).toHaveBeenCalledWith('/forum/posts', { params })
    })
  })

  describe('getForumPostById', () => {
    it('调用GET /forum/posts/:id', async () => {
      await getForumPostById(123)
      expect(request.get).toHaveBeenCalledWith('/forum/posts/123')
    })
  })

  describe('createForumPost', () => {
    it('调用POST /forum/posts', async () => {
      const data = { title: '测试帖子', content: '内容' }
      await createForumPost(data)
      expect(request.post).toHaveBeenCalledWith('/forum/posts', data)
    })
  })

  describe('updateForumPost', () => {
    it('调用PUT /forum/posts/:id', async () => {
      const data = { id: 123, title: '更新标题' }
      await updateForumPost(data)
      expect(request.put).toHaveBeenCalledWith('/forum/posts/123', data)
    })
  })

  describe('deleteForumPost', () => {
    it('调用DELETE /forum/posts/:id', async () => {
      await deleteForumPost(123)
      expect(request.delete).toHaveBeenCalledWith('/forum/posts/123')
    })
  })

  describe('likeForumPost', () => {
    it('调用POST /forum/posts/:id/like', async () => {
      await likeForumPost(123)
      expect(request.post).toHaveBeenCalledWith('/forum/posts/123/like')
    })
  })

  describe('getComments', () => {
    it('调用GET /forum/comments', async () => {
      await getComments(123, 1, { pageNum: 1, pageSize: 10 })
      expect(request.get).toHaveBeenCalledWith('/forum/comments', {
        params: { targetId: 123, targetType: 1, pageNum: 1, pageSize: 10 }
      })
    })

    it('无分页参数时调用', async () => {
      await getComments(123, 1)
      expect(request.get).toHaveBeenCalledWith('/forum/comments', {
        params: { targetId: 123, targetType: 1 }
      })
    })
  })

  describe('createComment', () => {
    it('调用POST /forum/comments', async () => {
      const data = { targetId: 123, targetType: 1, content: '评论内容' }
      await createComment(data)
      expect(request.post).toHaveBeenCalledWith('/forum/comments', data)
    })
  })

  describe('deleteComment', () => {
    it('调用DELETE /forum/comments/:id', async () => {
      await deleteComment(123)
      expect(request.delete).toHaveBeenCalledWith('/forum/comments/123')
    })
  })

  describe('likeComment', () => {
    it('调用POST /forum/comments/:id/like', async () => {
      await likeComment(123)
      expect(request.post).toHaveBeenCalledWith('/forum/comments/123/like')
    })
  })
})