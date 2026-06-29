import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import PoemCard from './PoemCard.vue'

const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockPush
  })
}))

vi.mock('element-plus', () => ({
  ElIcon: {
    template: '<slot />'
  }
}))

describe('PoemCard', () => {
  const defaultPoem = {
    id: 1,
    title: '静夜思',
    content: '床前明月光，疑是地上霜。举头望明月，低头思故乡。',
    poetName: '李白',
    dynastyName: '唐',
    viewCount: 100,
    likeCount: 50,
    avgScore: 4.5,
    isOriginal: false
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('渲染诗词标题', () => {
    const wrapper = mount(PoemCard, {
      props: { poem: defaultPoem }
    })
    
    expect(wrapper.find('.poem-title').text()).toBe('静夜思')
  })

  it('点击跳转到详情页', () => {
    const wrapper = mount(PoemCard, {
      props: { poem: defaultPoem }
    })
    
    wrapper.find('.poem-card').trigger('click')
    
    expect(mockPush).toHaveBeenCalledWith('/poem/1')
  })

  it('显示作者信息', () => {
    const wrapper = mount(PoemCard, {
      props: { poem: defaultPoem, showAuthor: true }
    })
    
    expect(wrapper.find('.poem-author').text()).toContain('唐')
    expect(wrapper.find('.poem-author').text()).toContain('李白')
  })

  it('不显示作者信息', () => {
    const wrapper = mount(PoemCard, {
      props: { poem: defaultPoem, showAuthor: false }
    })
    
    expect(wrapper.find('.poem-author').exists()).toBe(false)
  })

  it('显示原创标签', () => {
    const poem = { ...defaultPoem, isOriginal: true }
    const wrapper = mount(PoemCard, {
      props: { poem }
    })
    
    expect(wrapper.find('.original-tag').exists()).toBe(true)
  })

  it('不显示原创标签', () => {
    const wrapper = mount(PoemCard, {
      props: { poem: defaultPoem }
    })
    
    expect(wrapper.find('.original-tag').exists()).toBe(false)
  })

  it('显示浏览量', () => {
    const wrapper = mount(PoemCard, {
      props: { poem: defaultPoem }
    })
    
    expect(wrapper.find('.poem-meta').text()).toContain('100')
  })

  it('显示点赞数', () => {
    const wrapper = mount(PoemCard, {
      props: { poem: defaultPoem }
    })
    
    expect(wrapper.find('.poem-meta').text()).toContain('50')
  })

  it('显示评分', () => {
    const wrapper = mount(PoemCard, {
      props: { poem: defaultPoem }
    })
    
    expect(wrapper.find('.rating-meta').text()).toContain('4.5')
  })

  it('无评分时不显示评分', () => {
    const poem = { ...defaultPoem, avgScore: undefined }
    const wrapper = mount(PoemCard, {
      props: { poem }
    })
    
    expect(wrapper.find('.rating-meta').exists()).toBe(false)
  })

  it('格式化诗词内容', () => {
    const wrapper = mount(PoemCard, {
      props: { poem: defaultPoem }
    })
    
    const verses = wrapper.findAll('.verse-line')
    expect(verses.length).toBeGreaterThan(0)
    expect(verses.length).toBeLessThanOrEqual(4)
  })

  it('无内容时显示空', () => {
    const poem = { ...defaultPoem, content: '' }
    const wrapper = mount(PoemCard, {
      props: { poem }
    })
    
    expect(wrapper.findAll('.verse-line')).toHaveLength(0)
  })
})
