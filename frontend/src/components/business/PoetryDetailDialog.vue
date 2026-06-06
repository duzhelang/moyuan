<script setup lang="ts">
import { ref, watch } from 'vue'
import type { ApihzPoetryItem } from '@/api/modules/external-poetry'
import { getApihzPoetryDetail } from '@/api/modules/external-poetry'

const props = defineProps<{
  visible: boolean
  title: string
  author: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const loading = ref(false)
const detail = ref<ApihzPoetryItem | null>(null)
const activeTab = ref('translation')

const detailSections = [
  { key: 'translation', label: '译文注释', fields: ['ywjzsy', 'ywjzse'] },
  { key: 'background', label: '创作背景', fields: ['czbj'] },
  { key: 'appreciation', label: '鉴赏赏析', fields: ['jsy', 'jse', 'sxy', 'sxe'] },
  { key: 'analysis', label: '深度解读', fields: ['jj', 'yj', 'xzsf', 'dj', 'jx'] },
  { key: 'evaluation', label: '评价', fields: ['pj'] },
]

const hasContent = (fields: string[]): boolean => {
  if (!detail.value) return false
  return fields.some(f => {
    const val = detail.value?.[f as keyof ApihzPoetryItem]
    return val && typeof val === 'string' && val.trim().length > 0
  })
}

const getContent = (fields: string[]): string => {
  if (!detail.value) return ''
  return fields
    .map(f => detail.value?.[f as keyof ApihzPoetryItem])
    .filter(v => v && typeof v === 'string' && v.trim().length > 0)
    .join('\n\n')
}

const fetchDetail = async () => {
  if (!props.title) return
  loading.value = true
  detail.value = null
  try {
    const result = await getApihzPoetryDetail(props.title)
    if (result) {
      detail.value = result
    }
  } catch (e) {
    console.error('获取诗词详情失败:', e)
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  emit('update:visible', false)
}

watch(() => props.visible, (val) => {
  if (val && props.title) {
    fetchDetail()
    activeTab.value = 'translation'
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="visible" class="poetry-dialog-overlay" @click.self="handleClose">
        <div class="poetry-dialog">
          <div class="dialog-header">
            <div class="dialog-title-area">
              <h2 class="dialog-title">{{ title }}</h2>
              <span class="dialog-author" v-if="author">{{ author }}</span>
            </div>
            <el-icon class="dialog-close" @click="handleClose"><Close /></el-icon>
          </div>

          <div class="dialog-body" v-loading="loading">
            <template v-if="detail">
              <div class="poem-original">
                <p class="original-text">{{ detail.content }}</p>
              </div>

              <div class="detail-tabs">
                <a
                  v-for="section in detailSections"
                  :key="section.key"
                  v-show="hasContent(section.fields)"
                  :class="['tab-item', { active: activeTab === section.key }]"
                  @click="activeTab = section.key"
                >
                  {{ section.label }}
                </a>
              </div>

              <div class="detail-content">
                <div
                  v-for="section in detailSections"
                  :key="section.key"
                  v-show="activeTab === section.key && hasContent(section.fields)"
                  class="content-section"
                >
                  <p class="content-text" style="white-space: pre-line;">{{ getContent(section.fields) }}</p>
                </div>
              </div>
            </template>

            <el-empty v-else-if="!loading" description="暂未收录该诗词的详细赏析" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped lang="scss">
.poetry-dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.poetry-dialog {
  width: 680px;
  max-width: 92vw;
  max-height: 82vh;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #e8e8e8;
}

.dialog-title-area {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.dialog-title {
  font-size: 20px;
  font-family: 'KaiTi', 'STKaiti', serif;
  color: #8B4513;
  margin: 0;
}

.dialog-author {
  font-size: 14px;
  color: #999;
}

.dialog-close {
  cursor: pointer;
  font-size: 20px;
  color: #999;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;

  &:hover {
    color: #F56C6C;
    background: rgba(245, 108, 108, 0.08);
  }
}

.dialog-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

.poem-original {
  background: linear-gradient(135deg, rgba(139, 69, 19, 0.04), rgba(210, 105, 30, 0.03));
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 20px;
  border-left: 3px solid #8B4513;
}

.original-text {
  font-size: 15px;
  color: #333;
  line-height: 2;
  margin: 0;
  font-family: 'KaiTi', 'STKaiti', serif;
}

.detail-tabs {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid #e8e8e8;
  margin-bottom: 16px;
  overflow-x: auto;
}

.tab-item {
  padding: 8px 14px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
  transition: all 0.2s;

  &:hover {
    color: #8B4513;
  }

  &.active {
    color: #8B4513;
    border-bottom-color: #8B4513;
    font-weight: 600;
  }
}

.detail-content {
  min-height: 120px;
}

.content-section {
  animation: fadeIn 0.2s ease;
}

.content-text {
  font-size: 14px;
  color: #333;
  line-height: 1.9;
  text-indent: 2em;
  margin: 0;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.25s ease;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}
</style>
