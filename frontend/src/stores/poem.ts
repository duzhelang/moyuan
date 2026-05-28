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
      poemList.value = response.data.records
      total.value = response.data.total
      currentPage.value = response.data.current
      pageSize.value = response.data.size
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
      currentPoem.value = response.data
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
      modernPoems.value = response.data
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