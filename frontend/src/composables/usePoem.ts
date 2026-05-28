import { ref, computed } from 'vue'
import { usePoemStore } from '@/stores/poem'
import type { Poem, PoemListParams } from '@/types/model'

export function usePoem() {
  const poemStore = usePoemStore()

  const poems = computed(() => poemStore.poemList)
  const currentPoem = computed(() => poemStore.currentPoem)
  const loading = computed(() => poemStore.loading)
  const total = computed(() => poemStore.total)
  const currentPage = computed(() => poemStore.currentPage)
  const pageSize = computed(() => poemStore.pageSize)

  async function fetchPoemList(params: PoemListParams) {
    await poemStore.fetchPoemList(params)
  }

  async function fetchPoemDetail(id: number) {
    await poemStore.fetchPoemDetail(id)
  }

  async function fetchModernPoems() {
    await poemStore.fetchModernPoems()
  }

  function setCurrentPoem(poem: Poem | null) {
    poemStore.setCurrentPoem(poem)
  }

  function clearCurrentPoem() {
    poemStore.clearCurrentPoem()
  }

  return {
    poems,
    currentPoem,
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
}