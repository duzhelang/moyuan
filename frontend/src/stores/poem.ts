import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Poem, PoemListParams } from '@/types/model'
import { getPoemList, getPoemById, getModernPoems } from '@/api/modules/poem'

export const usePoemStore = defineStore('poem', () => {
  const poemList = ref<Poem[]>([])
  const currentPoem = ref<Poem | null>(null)
  const modernPoems = ref<Poem[]>([])
  const loading = ref(false)
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(10)

  async function fetchPoemList(params: PoemListParams) {
    loading.value = true
    try {
      const response = await getPoemList(params)
      poemList.value = response.data.list
      total.value = response.data.total
      currentPage.value = response.data.pageNum
      pageSize.value = response.data.pageSize
    } catch (error) {
      console.error('获取诗词列表失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function fetchPoemDetail(id: number) {
    loading.value = true
    try {
      const response = await getPoemById(id)
      const data = response.data
      if (data && typeof data === 'object' && 'poem' in data) {
        const poem = (data as any).poem
        poem.isLiked = (data as any).isLiked ?? false
        poem.isFavorited = (data as any).isFavorited ?? false
        currentPoem.value = poem
      } else {
        currentPoem.value = data as any
      }
    } catch (error) {
      console.error('获取诗词详情失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function fetchModernPoems() {
    loading.value = true
    try {
      const response = await getModernPoems()
      modernPoems.value = response.data.list
    } catch (error) {
      console.error('获取现代诗词失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  function setCurrentPoem(poem: Poem | null) {
    currentPoem.value = poem
  }

  function clearCurrentPoem() {
    currentPoem.value = null
  }

  return {
    poemList,
    currentPoem,
    modernPoems,
    loading,
    total,
    currentPage,
    pageSize,
    fetchPoemList,
    fetchPoemDetail,
    fetchModernPoems,
    setCurrentPoem,
    clearCurrentPoem
  }
})