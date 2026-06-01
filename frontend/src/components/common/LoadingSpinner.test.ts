import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import LoadingSpinner from './LoadingSpinner.vue'

describe('LoadingSpinner', () => {
  it('loading为false时不渲染', () => {
    const wrapper = mount(LoadingSpinner, {
      props: {
        loading: false
      }
    })
    expect(wrapper.find('.loading-spinner').exists()).toBe(false)
  })

  it('loading为true时渲染', () => {
    const wrapper = mount(LoadingSpinner, {
      props: {
        loading: true
      }
    })
    expect(wrapper.find('.loading-spinner').exists()).toBe(true)
    expect(wrapper.find('.spinner').exists()).toBe(true)
    expect(wrapper.find('.bounce1').exists()).toBe(true)
    expect(wrapper.find('.bounce2').exists()).toBe(true)
    expect(wrapper.find('.bounce3').exists()).toBe(true)
  })

  it('默认不显示文本', () => {
    const wrapper = mount(LoadingSpinner, {
      props: {
        loading: true
      }
    })
    expect(wrapper.find('.loading-text').exists()).toBe(false)
  })

  it('显示文本', () => {
    const wrapper = mount(LoadingSpinner, {
      props: {
        loading: true,
        text: '加载中...'
      }
    })
    expect(wrapper.find('.loading-text').exists()).toBe(true)
    expect(wrapper.find('.loading-text').text()).toBe('加载中...')
  })

  it('默认loading为true', () => {
    const wrapper = mount(LoadingSpinner)
    expect(wrapper.find('.loading-spinner').exists()).toBe(true)
  })

  it('空文本不显示', () => {
    const wrapper = mount(LoadingSpinner, {
      props: {
        loading: true,
        text: ''
      }
    })
    expect(wrapper.find('.loading-text').exists()).toBe(false)
  })
})