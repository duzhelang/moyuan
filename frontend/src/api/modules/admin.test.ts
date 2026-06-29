import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  getUserList,
  getUserById,
  createUser,
  updateUser,
  deleteUser,
  getAdminPoems,
  createAdminPoem,
  updateAdminPoem,
  deleteAdminPoem,
  getAdminCategories,
  createAdminCategory,
  updateAdminCategory,
  deleteAdminCategory,
  getAdminDynasties,
  createAdminDynasty,
  updateAdminDynasty,
  deleteAdminDynasty,
  getAdminPoets,
  createAdminPoet,
  updateAdminPoet,
  deleteAdminPoet,
  getAdminForumPosts,
  updateForumPostStatus,
  deleteAdminForumPost,
  getAdminStats,
  getAdminStatsTrend,
  getVisitStats,
  getVisitTrend,
  getAdminLogs,
  getAdminPoetFeatured,
  getAdminPoetFeaturedById,
  createAdminPoetFeatured,
  updateAdminPoetFeatured,
  deleteAdminPoetFeatured,
  getHomeNavigationList,
  getAdminHomeNavigation,
  createHomeNavigation,
  updateHomeNavigation,
  deleteHomeNavigation,
  getAdminPoetSuggestions,
  reviewPoetSuggestion,
  getAdminAiContents,
  approveAiContent,
  rejectAiContent,
  auditPoem,
  getAdminPoetProfiles,
  verifyPoetProfile,
  getAdminComments,
  auditComment,
  deleteAdminComment,
  getAuditStats,
  getAdminRepairs,
  getAdminRepairDetail,
  updateRepairStatus,
  assignRepairOrder,
  addRepairInternalComment,
  getAdminRepairComments,
  getRepairStats
} from './admin'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('admin API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('用户管理', () => {
    it('getUserList 调用GET /admin/users', async () => {
      const params = { page: 1, size: 10, keyword: 'test' }
      await getUserList(params)
      expect(request.get).toHaveBeenCalledWith('/admin/users', { params })
    })

    it('getUserById 调用GET /admin/users/:id', async () => {
      await getUserById(1)
      expect(request.get).toHaveBeenCalledWith('/admin/users/1')
    })

    it('createUser 调用POST /admin/users', async () => {
      const data = { username: 'test', password: '123456', email: 'test@test.com' }
      await createUser(data)
      expect(request.post).toHaveBeenCalledWith('/admin/users', data)
    })

    it('updateUser 调用PUT /admin/users/:id', async () => {
      const data = { nickname: '新昵称' }
      await updateUser(1, data)
      expect(request.put).toHaveBeenCalledWith('/admin/users/1', data)
    })

    it('deleteUser 调用DELETE /admin/users/:id', async () => {
      await deleteUser(1)
      expect(request.delete).toHaveBeenCalledWith('/admin/users/1')
    })
  })

  describe('诗词管理', () => {
    it('getAdminPoems 调用GET /admin/poems', async () => {
      const params = { page: 1, size: 10, keyword: 'test' }
      await getAdminPoems(params)
      expect(request.get).toHaveBeenCalledWith('/admin/poems', { params })
    })

    it('createAdminPoem 调用POST /admin/poems', async () => {
      const data = { title: '测试诗词', content: '内容' }
      await createAdminPoem(data)
      expect(request.post).toHaveBeenCalledWith('/admin/poems', data)
    })

    it('updateAdminPoem 调用PUT /admin/poems/:id', async () => {
      const data = { title: '更新标题' }
      await updateAdminPoem(1, data)
      expect(request.put).toHaveBeenCalledWith('/admin/poems/1', data)
    })

    it('deleteAdminPoem 调用DELETE /admin/poems/:id', async () => {
      await deleteAdminPoem(1)
      expect(request.delete).toHaveBeenCalledWith('/admin/poems/1')
    })
  })

  describe('分类管理', () => {
    it('getAdminCategories 调用GET /admin/categories', async () => {
      const params = { page: 1, size: 10 }
      await getAdminCategories(params)
      expect(request.get).toHaveBeenCalledWith('/admin/categories', { params })
    })

    it('createAdminCategory 调用POST /admin/categories', async () => {
      const data = { name: '古体诗' }
      await createAdminCategory(data)
      expect(request.post).toHaveBeenCalledWith('/admin/categories', data)
    })

    it('updateAdminCategory 调用PUT /admin/categories/:id', async () => {
      const data = { name: '近体诗' }
      await updateAdminCategory(1, data)
      expect(request.put).toHaveBeenCalledWith('/admin/categories/1', data)
    })

    it('deleteAdminCategory 调用DELETE /admin/categories/:id', async () => {
      await deleteAdminCategory(1)
      expect(request.delete).toHaveBeenCalledWith('/admin/categories/1')
    })
  })

  describe('朝代管理', () => {
    it('getAdminDynasties 调用GET /admin/dynasties', async () => {
      const params = { page: 1, size: 10 }
      await getAdminDynasties(params)
      expect(request.get).toHaveBeenCalledWith('/admin/dynasties', { params })
    })

    it('createAdminDynasty 调用POST /admin/dynasties', async () => {
      const data = { name: '唐朝' }
      await createAdminDynasty(data)
      expect(request.post).toHaveBeenCalledWith('/admin/dynasties', data)
    })

    it('updateAdminDynasty 调用PUT /admin/dynasties/:id', async () => {
      const data = { name: '宋朝' }
      await updateAdminDynasty(1, data)
      expect(request.put).toHaveBeenCalledWith('/admin/dynasties/1', data)
    })

    it('deleteAdminDynasty 调用DELETE /admin/dynasties/:id', async () => {
      await deleteAdminDynasty(1)
      expect(request.delete).toHaveBeenCalledWith('/admin/dynasties/1')
    })
  })

  describe('诗人管理', () => {
    it('getAdminPoets 调用GET /admin/poets', async () => {
      const params = { page: 1, size: 10 }
      await getAdminPoets(params)
      expect(request.get).toHaveBeenCalledWith('/admin/poets', { params })
    })

    it('createAdminPoet 调用POST /admin/poets', async () => {
      const data = { name: '李白', dynastyId: 6 }
      await createAdminPoet(data)
      expect(request.post).toHaveBeenCalledWith('/admin/poets', data)
    })

    it('updateAdminPoet 调用PUT /admin/poets/:id', async () => {
      const data = { name: '杜甫' }
      await updateAdminPoet(1, data)
      expect(request.put).toHaveBeenCalledWith('/admin/poets/1', data)
    })

    it('deleteAdminPoet 调用DELETE /admin/poets/:id', async () => {
      await deleteAdminPoet(1)
      expect(request.delete).toHaveBeenCalledWith('/admin/poets/1')
    })
  })

  describe('帖子管理', () => {
    it('getAdminForumPosts 调用GET /admin/forum-posts', async () => {
      const params = { page: 1, size: 10, keyword: 'test' }
      await getAdminForumPosts(params)
      expect(request.get).toHaveBeenCalledWith('/admin/forum-posts', { params })
    })

    it('updateForumPostStatus 调用PUT /admin/forum-posts/:id/status', async () => {
      await updateForumPostStatus(1, 1)
      expect(request.put).toHaveBeenCalledWith('/admin/forum-posts/1/status', { status: 1 })
    })

    it('deleteAdminForumPost 调用DELETE /admin/forum-posts/:id', async () => {
      await deleteAdminForumPost(1)
      expect(request.delete).toHaveBeenCalledWith('/admin/forum-posts/1')
    })
  })

  describe('统计数据', () => {
    it('getAdminStats 调用GET /admin/stats', async () => {
      await getAdminStats()
      expect(request.get).toHaveBeenCalledWith('/admin/stats')
    })

    it('getAdminStatsTrend 调用GET /admin/stats/trend', async () => {
      await getAdminStatsTrend()
      expect(request.get).toHaveBeenCalledWith('/admin/stats/trend')
    })

    it('getVisitStats 调用GET /admin/stats/visits', async () => {
      await getVisitStats()
      expect(request.get).toHaveBeenCalledWith('/admin/stats/visits')
    })

    it('getVisitTrend 调用GET /admin/stats/visits/trend', async () => {
      await getVisitTrend(7)
      expect(request.get).toHaveBeenCalledWith('/admin/stats/visits/trend', { params: { days: 7 } })
    })
  })

  describe('操作日志', () => {
    it('getAdminLogs 调用GET /admin/logs', async () => {
      const params = { page: 1, size: 10, keyword: 'test' }
      await getAdminLogs(params)
      expect(request.get).toHaveBeenCalledWith('/admin/logs', { params })
    })
  })

  describe('精选诗人管理', () => {
    it('getAdminPoetFeatured 调用GET /admin/poet-featured', async () => {
      const params = { page: 1, size: 10 }
      await getAdminPoetFeatured(params)
      expect(request.get).toHaveBeenCalledWith('/admin/poet-featured', { params })
    })

    it('getAdminPoetFeaturedById 调用GET /admin/poet-featured/:id', async () => {
      await getAdminPoetFeaturedById(1)
      expect(request.get).toHaveBeenCalledWith('/admin/poet-featured/1')
    })

    it('createAdminPoetFeatured 调用POST /admin/poet-featured', async () => {
      const data = { poetId: 1, sortOrder: 1 }
      await createAdminPoetFeatured(data)
      expect(request.post).toHaveBeenCalledWith('/admin/poet-featured', data)
    })

    it('updateAdminPoetFeatured 调用PUT /admin/poet-featured/:id', async () => {
      const data = { sortOrder: 2 }
      await updateAdminPoetFeatured(1, data)
      expect(request.put).toHaveBeenCalledWith('/admin/poet-featured/1', data)
    })

    it('deleteAdminPoetFeatured 调用DELETE /admin/poet-featured/:id', async () => {
      await deleteAdminPoetFeatured(1)
      expect(request.delete).toHaveBeenCalledWith('/admin/poet-featured/1')
    })
  })

  describe('首页导航管理', () => {
    it('getHomeNavigationList 调用GET /home-navigation', async () => {
      await getHomeNavigationList({ type: '作品' })
      expect(request.get).toHaveBeenCalledWith('/home-navigation', { params: { type: '作品' } })
    })

    it('getAdminHomeNavigation 调用GET /admin/home-navigation/manage', async () => {
      const params = { page: 1, size: 10, type: '作品' }
      await getAdminHomeNavigation(params)
      expect(request.get).toHaveBeenCalledWith('/admin/home-navigation/manage', { params })
    })

    it('createHomeNavigation 调用POST /admin/home-navigation', async () => {
      const data = { name: '测试', type: '作品' }
      await createHomeNavigation(data)
      expect(request.post).toHaveBeenCalledWith('/admin/home-navigation', data)
    })

    it('updateHomeNavigation 调用PUT /admin/home-navigation/:id', async () => {
      const data = { name: '更新' }
      await updateHomeNavigation(1, data)
      expect(request.put).toHaveBeenCalledWith('/admin/home-navigation/1', data)
    })

    it('deleteHomeNavigation 调用DELETE /admin/home-navigation/:id', async () => {
      await deleteHomeNavigation(1)
      expect(request.delete).toHaveBeenCalledWith('/admin/home-navigation/1')
    })
  })

  describe('诗人建议管理', () => {
    it('getAdminPoetSuggestions 调用GET /admin/poet-suggestions', async () => {
      const params = { pageNum: 1, pageSize: 10, status: '0' }
      await getAdminPoetSuggestions(params)
      expect(request.get).toHaveBeenCalledWith('/admin/poet-suggestions', { params })
    })

    it('reviewPoetSuggestion 调用PUT /admin/poet-suggestions/:id/review', async () => {
      await reviewPoetSuggestion(1, 'approved', '审核通过')
      expect(request.put).toHaveBeenCalledWith('/admin/poet-suggestions/1/review', null, {
        params: { status: 'approved', reviewComment: '审核通过' }
      })
    })
  })

  describe('AI内容审核', () => {
    it('getAdminAiContents 调用GET /admin/ai-contents', async () => {
      const params = { page: 1, size: 10, status: 0 }
      await getAdminAiContents(params)
      expect(request.get).toHaveBeenCalledWith('/admin/ai-contents', { params })
    })

    it('approveAiContent 调用PUT /admin/ai-contents/:id/approve', async () => {
      await approveAiContent(1, '审核通过')
      expect(request.put).toHaveBeenCalledWith('/admin/ai-contents/1/approve', { reviewComment: '审核通过' })
    })

    it('rejectAiContent 调用PUT /admin/ai-contents/:id/reject', async () => {
      await rejectAiContent(1, '审核拒绝')
      expect(request.put).toHaveBeenCalledWith('/admin/ai-contents/1/reject', { reviewComment: '审核拒绝' })
    })
  })

  describe('诗词审核', () => {
    it('auditPoem 调用PUT /admin/poems/:id/audit', async () => {
      await auditPoem(1, 1, '审核通过')
      expect(request.put).toHaveBeenCalledWith('/admin/poems/1/audit', { status: 1, reason: '审核通过' })
    })
  })

  describe('诗人认证管理', () => {
    it('getAdminPoetProfiles 调用GET /admin/poet-profiles', async () => {
      const params = { page: 1, size: 10, status: 0 }
      await getAdminPoetProfiles(params)
      expect(request.get).toHaveBeenCalledWith('/admin/poet-profiles', { params })
    })

    it('verifyPoetProfile 调用PUT /admin/poet-profiles/:id/verify', async () => {
      await verifyPoetProfile(1, 1, '认证通过')
      expect(request.put).toHaveBeenCalledWith('/admin/poet-profiles/1/verify', { status: 1, reason: '认证通过' })
    })
  })

  describe('评论管理', () => {
    it('getAdminComments 调用GET /admin/comments', async () => {
      const params = { page: 1, size: 10, status: 0 }
      await getAdminComments(params)
      expect(request.get).toHaveBeenCalledWith('/admin/comments', { params })
    })

    it('auditComment 调用PUT /admin/comments/:id/audit', async () => {
      await auditComment(1, 1, '审核通过')
      expect(request.put).toHaveBeenCalledWith('/admin/comments/1/audit', { status: 1, reason: '审核通过' })
    })

    it('deleteAdminComment 调用DELETE /admin/comments/:id', async () => {
      await deleteAdminComment(1)
      expect(request.delete).toHaveBeenCalledWith('/admin/comments/1')
    })
  })

  describe('审核统计', () => {
    it('getAuditStats 调用GET /admin/audit/stats', async () => {
      await getAuditStats()
      expect(request.get).toHaveBeenCalledWith('/admin/audit/stats')
    })
  })

  describe('报修管理', () => {
    it('getAdminRepairs 调用GET /admin/repairs', async () => {
      const params = { page: 1, size: 10, status: 0, category: '功能异常', priority: 1, keyword: 'test' }
      await getAdminRepairs(params)
      expect(request.get).toHaveBeenCalledWith('/admin/repairs', { params })
    })

    it('getAdminRepairDetail 调用GET /admin/repairs/:id', async () => {
      await getAdminRepairDetail(1)
      expect(request.get).toHaveBeenCalledWith('/admin/repairs/1')
    })

    it('updateRepairStatus 调用PUT /admin/repairs/:id/status', async () => {
      const data = { status: 1, resolveContent: '处理中' }
      await updateRepairStatus(1, data)
      expect(request.put).toHaveBeenCalledWith('/admin/repairs/1/status', data)
    })

    it('assignRepairOrder 调用PUT /admin/repairs/:id/assign', async () => {
      await assignRepairOrder(1, 2)
      expect(request.put).toHaveBeenCalledWith('/admin/repairs/1/assign', { assigneeId: 2 })
    })

    it('addRepairInternalComment 调用POST /admin/repairs/:id/comments', async () => {
      const data = { content: '内部备注' }
      await addRepairInternalComment(1, data)
      expect(request.post).toHaveBeenCalledWith('/admin/repairs/1/comments', data)
    })

    it('getAdminRepairComments 调用GET /admin/repairs/:id/comments', async () => {
      await getAdminRepairComments(1)
      expect(request.get).toHaveBeenCalledWith('/admin/repairs/1/comments')
    })

    it('getRepairStats 调用GET /admin/repairs/stats', async () => {
      await getRepairStats()
      expect(request.get).toHaveBeenCalledWith('/admin/repairs/stats')
    })
  })
})