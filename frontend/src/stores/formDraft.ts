import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { FormDraft } from '@/types/storage'
import { getFormDraft, setFormDraft, removeFormDraft } from '@/utils/storage'

export const useFormDraftStore = defineStore('formDraft', () => {
  const drafts = ref<Record<string, FormDraft | null>>({
    poem: getFormDraft('poem'),
    forum: getFormDraft('forum'),
    comment: getFormDraft('comment')
  })

  function saveDraft(type: 'poem' | 'forum' | 'comment', data: Record<string, any>) {
    setFormDraft(type, data)
    drafts.value[type] = getFormDraft(type)
  }

  function loadDraft(type: 'poem' | 'forum' | 'comment'): FormDraft | null {
    return getFormDraft(type)
  }

  function deleteDraft(type: 'poem' | 'forum' | 'comment') {
    removeFormDraft(type)
    drafts.value[type] = null
  }

  function hasDraft(type: 'poem' | 'forum' | 'comment'): boolean {
    return drafts.value[type] !== null
  }

  return {
    drafts,
    saveDraft,
    loadDraft,
    deleteDraft,
    hasDraft
  }
})