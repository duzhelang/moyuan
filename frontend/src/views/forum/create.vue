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
  <div class="forum-create-page ink-bg ink-bg--3">
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
  /* 顶部预留固定头部（AppHeader 高 55px）空间，避免右上角「返回列表」按钮被遮挡 */
  padding: 75px 0 $spacing-xl;
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
  /* 覆盖全局 h1 的 flex 居中，避免标题与左对齐的副标题错位 */
  display: block;
  text-align: left;
  position: relative;
  font-size: $font-size-title;
  color: $primary-color;
  font-family: $font-family-title;
  letter-spacing: 2px;
  margin-bottom: $spacing-md;
  padding-bottom: $spacing-sm;

  /* 标题下方渐变装饰线 */
  &::after {
    content: '';
    position: absolute;
    left: 0;
    bottom: 0;
    width: 72px;
    height: 3px;
    border-radius: 2px;
    background: linear-gradient(90deg, $primary-color, $accent-color, transparent);
    box-shadow: 0 1px 4px rgba($primary-color, 0.25);
  }
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

/* 卡片统一：宣纸底色 + 铜色细边 + 顶部渐变装饰条 */
.create-card,
.tips-card,
.category-guide-card {
  position: relative;
  overflow: hidden;
  background: $surface-primary;
  border: 1px solid rgba($border-color-bronze, 0.45);
  border-radius: $border-radius-lg;
  box-shadow: 0 2px 10px rgba($primary-color, 0.06);
  transition: box-shadow $transition-base, border-color $transition-base;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(
      90deg,
      transparent,
      $primary-300 20%,
      $primary-500 50%,
      $primary-300 80%,
      transparent
    );
  }

  &:hover {
    box-shadow: 0 6px 18px rgba($primary-color, 0.12);
    border-color: rgba($border-color-bronze, 0.75);
  }
}

.create-card {
  flex: 1;

  :deep(.el-card__body) {
    padding: $spacing-xl;
  }
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

  /* 图标徽章 */
  .el-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 26px;
    height: 26px;
    border-radius: $border-radius-md;
    background: $tag-bg-light;
    color: $primary-color;
    font-size: 15px;
    box-shadow: inset 0 0 0 1px rgba($border-color-bronze, 0.5);
  }
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
  :deep(.el-card__header) {
    padding: $spacing-md $spacing-lg;
    background: linear-gradient(180deg, rgba($primary-50, 0.8), rgba($surface-primary, 0.2));
    border-bottom: 1px solid rgba($border-color-bronze, 0.35);
  }

  :deep(.el-card__body) {
    padding: $spacing-lg;
  }
}

.tips-header,
.category-guide-header {
  position: relative;
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding-left: $spacing-sm;
  font-size: $font-size-lg;
  font-weight: 600;
  color: $primary-color;

  /* 左侧装饰竖条 */
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 3px;
    height: 16px;
    border-radius: 2px;
    background: linear-gradient(180deg, $secondary-color, $primary-color);
  }
}

.tips-list {
  margin: 0;
  padding: 0;
  list-style: none;
  
  li {
    position: relative;
    padding-left: $spacing-md;
    font-size: $font-size-base;
    color: $text-color-secondary;
    line-height: $line-height-loose;
    margin-bottom: $spacing-sm;
    transition: color $transition-fast;

    /* 自制铜色圆点标记 */
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 10px;
      width: 5px;
      height: 5px;
      border-radius: 50%;
      background: $accent-color;
      box-shadow: 0 0 0 3px rgba($accent-color, 0.15);
    }
    
    &:last-child {
      margin-bottom: 0;
    }

    &:hover {
      color: $text-color;
    }
  }
}

.category-guide-card {
  :deep(.el-card__header) {
    padding: $spacing-md $spacing-lg;
    background: linear-gradient(180deg, rgba($primary-50, 0.8), rgba($surface-primary, 0.2));
    border-bottom: 1px solid rgba($border-color-bronze, 0.35);
  }

  :deep(.el-card__body) {
    padding: $spacing-lg;
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
  padding: $spacing-sm $spacing-md;
  border-radius: $border-radius-md;
  background: rgba($primary-50, 0.7);
  border-left: 3px solid rgba($secondary-color, 0.55);
  transition: all $transition-fast;

  &:hover {
    background: rgba($primary-100, 0.75);
    border-left-color: $primary-color;
    transform: translateX(2px);
  }
  
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

/* 输入框 / 下拉框：铜色细边 + 更大圆角 + 主题聚焦光环 */
/* 注意：伪类必须写在 :deep() 括号内，Sass 会把 &:hover 提到 :deep() 外面导致选择器失效 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  padding: 6px 12px;
  border-radius: $border-radius-md;
  background-color: $background-color-light;
  box-shadow: 0 0 0 1px rgba($border-color-bronze, 0.7) inset;
  transition: box-shadow $transition-fast;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px $accent-color inset;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px $primary-color inset, $input-focus-ring;
}

:deep(.el-input__inner) {
  font-family: $font-family-input;
  letter-spacing: 0.5px;
  line-height: 1.5;
}

/* 文本域：宣纸质感 + 主题聚焦光环 */
:deep(.el-textarea__inner) {
  padding: 12px 14px;
  font-family: $font-family-input;
  font-size: $font-size-base;
  line-height: 1.8;
  letter-spacing: 0.5px;
  color: $text-color;
  background-color: $background-color-light;
  border: 1px solid rgba($border-color-bronze, 0.7);
  border-radius: $border-radius-md;
  /* 覆盖 Element Plus 默认的灰色内阴影，避免与铜色描边形成双边框 */
  box-shadow: none;
  transition: all $transition-fast;
}

:deep(.el-textarea__inner::placeholder) {
  color: $text-color-light;
}

:deep(.el-textarea__inner:hover) {
  border-color: $accent-color;
  box-shadow: none;
}

:deep(.el-textarea__inner:focus) {
  border-color: $primary-color;
  box-shadow: $input-focus-ring;
}

/* 主按钮：棕金渐变，替代 Element Plus 默认蓝 */
:deep(.el-button--primary) {
  --el-button-bg-color: #{$primary-color};
  --el-button-border-color: #{$primary-color};
  --el-button-hover-bg-color: #{$primary-600};
  --el-button-hover-border-color: #{$primary-600};
  --el-button-active-bg-color: #{$primary-700};
  --el-button-active-border-color: #{$primary-700};

  height: 40px;
  padding: 0 $spacing-lg;
  border-radius: $border-radius-md;
  font-size: $font-size-base;
  font-weight: 600;
  letter-spacing: 1px;
  background-image: linear-gradient(135deg, $primary-400, $primary-color);
  border-color: $primary-color;
  box-shadow: 0 2px 8px rgba($primary-color, 0.25);
}

:deep(.el-button--primary:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba($primary-color, 0.3);
}

/* 次要按钮（返回列表 / 取消）：铜色描边幽灵按钮 */
:deep(.el-button:not(.el-button--primary)) {
  height: 40px;
  padding: 0 $spacing-md;
  border-radius: $border-radius-md;
  color: $primary-color;
  background-color: $background-color-light;
  border-color: rgba($primary-color, 0.5);
}

:deep(.el-button:not(.el-button--primary):hover),
:deep(.el-button:not(.el-button--primary):focus) {
  color: $primary-color;
  background-color: $primary-50;
  border-color: $primary-color;
}
</style>
