// 首页左右面板（古诗词推选 / 当代·精选）诗词卡片跳转导航公共逻辑
// 规则：本地库命中则跳转详情页 /poem/:id，本地未命中则回退到 PoetryDetailDialog 弹窗展示外部详情
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { searchPoemsWithRecommend, type PoemSearchResult } from '@/api/modules/external-poetry'

export function usePoemCardNavigation() {
  const router = useRouter()

  // 弹窗形式的诗词详情（外部诗源兜底）
  const poetryDialogVisible = ref(false)
  const poetryDialogTitle = ref('')
  const poetryDialogAuthor = ref('')

  const openPoemDialog = (title: string, author = '') => {
    poetryDialogTitle.value = title
    poetryDialogAuthor.value = author
    poetryDialogVisible.value = true
  }

  // 搜索/推荐结果卡片：本地记录跳详情页，外部诗词弹窗展示
  const openPoemDetail = (poem: PoemSearchResult) => {
    if (poem.source === 'local' && poem.id) {
      router.push(`/poem/${poem.id}`)
    } else {
      openPoemDialog(poem.title, poem.author || '')
    }
  }

  // 静态卡片（无 id）：按标题（无标题时取正文前 12 字）搜索本地库，
  // 命中后跳转对应详情页，未命中回退弹窗兜底
  const openStaticPoemDetail = async (title: string, author = '', content = '') => {
    const keyword = (title || content.slice(0, 12)).trim()
    try {
      const results = await searchPoemsWithRecommend(keyword, 1, 5)
      const local = results.find(r => r.source === 'local' && r.id)
      if (local) {
        router.push(`/poem/${local.id}`)
        return
      }
    } catch {
      // 搜索失败不影响兜底弹窗
      console.error('按标题查找本地诗词失败:', keyword)
    }
    openPoemDialog(title, author)
  }

  return {
    poetryDialogVisible,
    poetryDialogTitle,
    poetryDialogAuthor,
    openPoemDetail,
    openStaticPoemDetail
  }
}
