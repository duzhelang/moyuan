import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import PoetCard from './PoetCard.vue'

const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockPush
  })
}))

describe('PoetCard', () => {
  const defaultPoet = {
    id: 1,
    poetId: 100,
    name: '李白',
    dynasty: '唐',
    description: '唐代著名诗人',
    biography: '李白（701年—762年），字太白，号青莲居士，唐朝浪漫主义诗人',
    imageUrl: '/img/poets/libai.jpg'
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('渲染诗人名称', () => {
    const wrapper = mount(PoetCard, {
      props: { poet: defaultPoet }
    })
    
    expect(wrapper.find('.poet-flip-img-text').text()).toBe('李白')
  })

  it('渲染朝代', () => {
    const wrapper = mount(PoetCard, {
      props: { poet: defaultPoet }
    })
    
    expect(wrapper.find('.poet-flip-img-dynasty').text()).toBe('唐')
  })

  it('点击跳转到诗人详情', () => {
    const wrapper = mount(PoetCard, {
      props: { poet: defaultPoet }
    })
    
    wrapper.find('.poet-flip-card').trigger('click')
    
    expect(mockPush).toHaveBeenCalledWith('/poet/100')
  })

  it('无poetId时跳转到搜索', () => {
    const poet = { ...defaultPoet, poetId: null }
    const wrapper = mount(PoetCard, {
      props: { poet }
    })
    
    wrapper.find('.poet-flip-card').trigger('click')
    
    expect(mockPush).toHaveBeenCalledWith({ path: '/poem', query: { keyword: '李白' } })
  })

  it('占位符诗人不可点击', () => {
    const poet = { ...defaultPoet, id: -1, name: '敬请期待' }
    const wrapper = mount(PoetCard, {
      props: { poet }
    })
    
    wrapper.find('.poet-flip-card').trigger('click')
    
    expect(mockPush).not.toHaveBeenCalled()
  })

  it('鼠标悬停翻转', async () => {
    const wrapper = mount(PoetCard, {
      props: { poet: defaultPoet }
    })
    
    await wrapper.find('.poet-flip-card').trigger('mouseenter')
    
    expect(wrapper.find('.poet-flip-inner').classes()).toContain('flipped')
  })

  it('鼠标离开恢复', async () => {
    const wrapper = mount(PoetCard, {
      props: { poet: defaultPoet }
    })
    
    await wrapper.find('.poet-flip-card').trigger('mouseenter')
    await wrapper.find('.poet-flip-card').trigger('mouseleave')
    
    expect(wrapper.find('.poet-flip-inner').classes()).not.toContain('flipped')
  })

  it('显示简介', () => {
    const wrapper = mount(PoetCard, {
      props: { poet: defaultPoet }
    })
    
    expect(wrapper.find('.poet-flip-brief').text()).toBe('唐代著名诗人')
  })

  it('显示传记（截断）', () => {
    const wrapper = mount(PoetCard, {
      props: { poet: defaultPoet }
    })
    
    const bio = wrapper.find('.poet-flip-back-bio').text()
    expect(bio).toContain('李白')
    expect(bio.length).toBeLessThanOrEqual(123)
  })

  it('现代诗人标签', () => {
    const poet = { ...defaultPoet, dynasty: '现代' }
    const wrapper = mount(PoetCard, {
      props: { poet }
    })
    
    expect(wrapper.find('.poet-flip-img-modern-tag').exists()).toBe(true)
  })

  it('图片加载失败显示占位图', async () => {
    const wrapper = mount(PoetCard, {
      props: { poet: defaultPoet }
    })
    
    await wrapper.find('.poet-flip-img').trigger('error')
    
    const img = wrapper.find('.poet-flip-img')
    expect(img.attributes('src')).toContain('data:image/svg+xml')
  })

  it('无图片显示占位图', () => {
    const poet = { ...defaultPoet, imageUrl: '' }
    const wrapper = mount(PoetCard, {
      props: { poet }
    })
    
    const img = wrapper.find('.poet-flip-img')
    expect(img.attributes('src')).toContain('data:image/svg+xml')
  })
})
