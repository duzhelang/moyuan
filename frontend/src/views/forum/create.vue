<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { Category } from '@/types/model'
import type { ForumPostCreateRequest } from '@/types/api'
import { createForumPost } from '@/api/modules/forum'
import { getCategoryList } from '@/api/modules/category'
import { useFormDraftStore } from '@/stores/formDraft'
import { getCache, setCache } from '@/utils/storage'

const router = useRouter()
const formDraftStore = useFormDraftStore()

const form = ref<ForumPostCreateRequest>({
  title: '',
  content: '',
  category: ''
})
const categories = ref<Category[]>([])
const submitting = ref(false)

const fetchCategories = async () => {
  try {
    const cachedCategories = getCache<Category[]>('categories')
    if (cachedCategories) {
      categories.value = cachedCategories
      return
    }

    const res = await getCategoryList()
    categories.value = res.data
    setCache('categories', res.data, 60 * 60 * 1000)
  } catch (error) {
    ElMessage.error('获取分类列表失败')
  }
}

const loadDraft = () => {
  const draft = formDraftStore.loadDraft('forum')
  if (draft) {
    form.value = draft.data as ForumPostCreateRequest
    ElMessage.info('已恢复未保存的草稿')
  }
}

const saveDraft = () => {
  if (form.value.title || form.value.content) {
    formDraftStore.saveDraft('forum', form.value)
  }
}

const handleSubmit = async () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!form.value.content.trim()) {
    ElMessage.warning('请输入内容')
    return
  }

  submitting.value = true
  try {
    const res = await createForumPost(form.value)
    ElMessage.success('发帖成功')
    formDraftStore.deleteDraft('forum')
    router.push(`/forum/${res.data.id}`)
  } catch (error) {
    ElMessage.error('发帖失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchCategories()
  loadDraft()
})

onUnmounted(() => {
  saveDraft()
})
</script>

<template>
  <div class="forum-create-page">
    <div class="container">
      <div class="page-header">
        <div class="header-content">
          <h1 class="page-title">发布新帖</h1>
          <p class="page-subtitle">分享您的诗词见解、创作心得或提出问题，与社区成员交流讨论</p>
        </div>
        <el-button @click="router.push('/forum')">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>
      
      <div class="create-layout">
        <el-card class="create-card">
          <el-form :model="form" label-position="top">
            <el-form-item label="标题">
              <template #label>
                <div class="form-label">
                  <el-icon><EditPen /></el-icon>
                  <span>标题</span>
                </div>
              </template>
              <el-input
                v-model="form.title"
                placeholder="请输入帖子标题，简明扼要地描述您的主题"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="分类">
              <template #label>
                <div class="form-label">
                  <el-icon><Folder /></el-icon>
                  <span>分类</span>
                </div>
              </template>
              <el-select v-model="form.category" placeholder="请选择分类" clearable>
                <el-option
                  v-for="cat in categories"
                  :key="cat.id"
                  :label="cat.name"
                  :value="cat.name"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="内容">
              <template #label>
                <div class="form-label">
                  <el-icon><Document /></el-icon>
                  <span>内容</span>
                </div>
              </template>
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="12"
                placeholder="请输入帖子内容，支持详细描述您的想法、问题或分享内容"
                maxlength="5000"
                show-word-limit
              />
            </el-form-item>
            <el-form-item class="form-actions">
              <el-button
                type="primary"
                :loading="submitting"
                @click="handleSubmit"
              >
                <el-icon><Promotion /></el-icon>
                发布帖子
              </el-button>
              <el-button @click="router.push('/forum')">取消</el-button>
              <div class="draft-tip">
                <el-icon><Memo /></el-icon>
                <span>草稿会自动保存，下次访问时恢复</span>
              </div>
            </el-form-item>
          </el-form>
        </el-card>
        
        <div class="side-panel">
          <el-card class="tips-card">
            <template #header>
              <div class="tips-header">
                <el-icon><InfoFilled /></el-icon>
                <span>发布提示</span>
              </div>
            </template>
            <ul class="tips-list">
              <li>标题应简洁明了，能够概括帖子主题</li>
              <li>选择合适的分类有助于其他用户发现您的帖子</li>
              <li>内容详细、有条理的帖子更容易获得回复</li>
              <li>可以分享诗词赏析、创作心得、学习方法等</li>
              <li>提出问题时，请尽量提供背景信息和具体描述</li>
              <li>尊重他人观点，保持友好交流的氛围</li>
            </ul>
          </el-card>
          
          <el-card class="category-guide-card">
            <template #header>
              <div class="category-guide-header">
                <el-icon><Collection /></el-icon>
                <span>分类指南</span>
              </div>
            </template>
            <div class="category-guide-content">
              <div class="category-item">
                <span class="category-name">诗词赏析</span>
                <span class="category-desc">分享对经典诗词的理解和感悟</span>
              </div>
              <div class="category-item">
                <span class="category-name">创作交流</span>
                <span class="category-desc">展示原创作品，交流创作经验</span>
              </div>
              <div class="category-item">
                <span class="category-name">学习讨论</span>
                <span class="category-desc">探讨诗词学习方法和技巧</span>
              </div>
              <div class="category-item">
                <span class="category-name">问题求助</span>
                <span class="category-desc">提出疑问，寻求社区帮助</span>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.forum-create-page {
  padding: $spacing-xl 0;
  min-height: 60vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: $spacing-xl;
}

.header-content {
  flex: 1;
}

.page-title {
  font-size: $font-size-title;
  color: $primary-color;
  font-family: $font-family-title;
  margin-bottom: $spacing-sm;
}

.page-subtitle {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: 1.6;
  margin: 0;
}

.create-layout {
  display: flex;
  gap: $spacing-xl;
  
  @include responsive(lg) {
    flex-direction: column;
  }
}

.create-card {
  flex: 1;
  border-radius: $border-radius-md;
}

.side-panel {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
  
  @include responsive(lg) {
    width: 100%;
  }
}

.form-label {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  font-size: $font-size-lg;
  font-weight: 600;
  color: $text-color;
}

.form-actions {
  margin-top: $spacing-lg;
  
  .el-form-item__content {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: $spacing-md;
  }
}

.draft-tip {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-secondary;
  margin-left: $spacing-md;
}

.tips-card {
  border-radius: $border-radius-md;
  
  .tips-header {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: $font-size-lg;
    color: $primary-color;
  }
}

.tips-list {
  margin: 0;
  padding-left: $spacing-lg;
  
  li {
    font-size: $font-size-base;
    color: $text-color-secondary;
    line-height: $line-height-loose;
    margin-bottom: $spacing-sm;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
}

.category-guide-card {
  border-radius: $border-radius-md;
  
  .category-guide-header {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: $font-size-lg;
    color: $primary-color;
  }
}

.category-guide-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.category-item {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
  
  .category-name {
    font-size: $font-size-base;
    font-weight: 600;
    color: $text-color;
  }
  
  .category-desc {
    font-size: $font-size-sm;
    color: $text-color-secondary;
    line-height: 1.5;
  }
}

:deep(.el-form-item__label) {
  font-size: $font-size-lg;
  font-weight: 600;
  color: $text-color;
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
  letter-spacing: 0.5px;
  line-height: 1.5;
}
</style>
