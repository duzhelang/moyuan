import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useFormDraftStore } from './formDraft'

vi.mock('@/utils/storage', () => ({
  getFormDraft: vi.fn(),
  setFormDraft: vi.fn(),
  removeFormDraft: vi.fn()
}))

describe('useFormDraftStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('初始化草稿状态', () => {
    const { getFormDraft } = require('@/utils/storage')
    getFormDraft.mockReturnValue(null)
    
    const store = useFormDraftStore()
    
    expect(store.drafts.poem).toBeNull()
    expect(store.drafts.forum).toBeNull()
    expect(store.drafts.comment).toBeNull()
  })

  describe('saveDraft', () => {
    it('保存诗词草稿', () => {
      const store = useFormDraftStore()
      const { setFormDraft, getFormDraft } = require('@/utils/storage')
      
      const draftData = { title: '静夜思', content: '床前明月光' }
      const savedDraft = { ...draftData, updatedAt: Date.now() }
      
      getFormDraft.mockReturnValue(savedDraft)
      
      store.saveDraft('poem', draftData)
      
      expect(setFormDraft).toHaveBeenCalledWith('poem', draftData)
      expect(store.drafts.poem).toEqual(savedDraft)
    })

    it('保存论坛草稿', () => {
      const store = useFormDraftStore()
      const { setFormDraft } = require('@/utils/storage')
      
      const draftData = { title: '讨论帖', content: '内容' }
      store.saveDraft('forum', draftData)
      
      expect(setFormDraft).toHaveBeenCalledWith('forum', draftData)
    })

    it('保存评论草稿', () => {
      const store = useFormDraftStore()
      const { setFormDraft } = require('@/utils/storage')
      
      const draftData = { content: '评论内容' }
      store.saveDraft('comment', draftData)
      
      expect(setFormDraft).toHaveBeenCalledWith('comment', draftData)
    })
  })

  describe('loadDraft', () => {
    it('加载存在的草稿', () => {
      const store = useFormDraftStore()
      const { getFormDraft } = require('@/utils/storage')
      
      const draft = { title: '草稿', content: '内容', updatedAt: Date.now() }
      getFormDraft.mockReturnValue(draft)
      
      const result = store.loadDraft('poem')
      
      expect(getFormDraft).toHaveBeenCalledWith('poem')
      expect(result).toEqual(draft)
    })

    it('加载不存在的草稿返回null', () => {
      const store = useFormDraftStore()
      const { getFormDraft } = require('@/utils/storage')
      
      getFormDraft.mockReturnValue(null)
      
      const result = store.loadDraft('forum')
      
      expect(result).toBeNull()
    })
  })

  describe('deleteDraft', () => {
    it('删除草稿', () => {
      const store = useFormDraftStore()
      const { removeFormDraft, getFormDraft } = require('@/utils/storage')
      
      getFormDraft.mockReturnValue({ title: 'test' })
      store.saveDraft('poem', { title: 'test' })
      
      store.deleteDraft('poem')
      
      expect(removeFormDraft).toHaveBeenCalledWith('poem')
      expect(store.drafts.poem).toBeNull()
    })
  })

  describe('hasDraft', () => {
    it('有草稿时返回true', () => {
      const store = useFormDraftStore()
      const { getFormDraft } = require('@/utils/storage')
      
      getFormDraft.mockReturnValue({ title: 'test' })
      store.saveDraft('poem', { title: 'test' })
      
      expect(store.hasDraft('poem')).toBe(true)
    })

    it('无草稿时返回false', () => {
      const store = useFormDraftStore()
      const { getFormDraft } = require('@/utils/storage')
      
      getFormDraft.mockReturnValue(null)
      
      expect(store.hasDraft('poem')).toBe(false)
    })
  })
})
