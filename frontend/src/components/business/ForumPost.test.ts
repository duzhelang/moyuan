import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ForumPost from './ForumPost.vue'

const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockPush
  })
}))

vi.mock('element-plus', () => ({
  ElAvatar: {
    template: '<slot />',
    props: ['src', 'size']
  },
  ElIcon: {
    template: '<slot />'
  }
}))

describe('ForumPost', () => {
  const defaultPost = {
    id: 1,
    title: '讨论：李白的诗风特点',
    content: '李白是唐代著名诗人，他的诗风豪放飘逸，想象丰富，语言流转自然。',
    username: '诗词爱好者',
    avatar: '/img/avatars/user1.jpg',
    createTime: '2024-01-15 10:30',
    viewCount: 200,
    likeCount: 30,
    commentCount: 15
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('渲染帖子标题', () => {
    const wrapper = mount(ForumPost, {
      props: { post: defaultPost }
    })
    
    expect(wrapper.find('.post-title').text()).toBe('讨论：李白的诗风特点')
  })

  it('渲染帖子内容', () => {
    const wrapper = mount(ForumPost, {
      props: { post: defaultPost }
    })
    
    expect(wrapper.find('.post-content p').text()).toContain('李白是唐代著名诗人')
  })

  it('渲染用户名', () => {
    const wrapper = mount(ForumPost, {
      props: { post: defaultPost }
    })
    
    expect(wrapper.find('.post-author').text()).toBe('诗词爱好者')
  })

  it('渲染发布时间', () => {
    const wrapper = mount(ForumPost, {
      props: { post: defaultPost }
    })
    
    expect(wrapper.find('.post-time').text()).toBe('2024-01-15 10:30')
  })

  it('点击跳转到详情页', () => {
    const wrapper = mount(ForumPost, {
      props: { post: defaultPost }
    })
    
    wrapper.find('.forum-post').trigger('click')
    
    expect(mockPush).toHaveBeenCalledWith('/forum/1')
  })

  it('显示浏览量', () => {
    const wrapper = mount(ForumPost, {
      props: { post: defaultPost }
    })
    
    expect(wrapper.find('.post-footer').text()).toContain('200')
  })

  it('显示点赞数', () => {
    const wrapper = mount(ForumPost, {
      props: { post: defaultPost }
    })
    
    expect(wrapper.find('.post-footer').text()).toContain('30')
  })

  it('显示评论数', () => {
    const wrapper = mount(ForumPost, {
      props: { post: defaultPost }
    })
    
    expect(wrapper.find('.post-footer').text()).toContain('15')
  })

  it('显示用户头像首字母', () => {
    const post = { ...defaultPost, avatar: '' }
    const wrapper = mount(ForumPost, {
      props: { post }
    })
    
    expect(wrapper.find('.el-avatar').text()).toBe('诗')
  })

  it('无用户名时不显示首字母', () => {
    const post = { ...defaultPost, username: '' }
    const wrapper = mount(ForumPost, {
      props: { post }
    })
    
    expect(wrapper.find('.el-avatar').text()).toBe('')
  })
})
