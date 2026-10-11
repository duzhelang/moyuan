<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { Dynasty, Category } from '@/types/model'
import { createPoem, searchPoems } from '@/api/modules/poem'
import { getDynastyList } from '@/api/modules/dynasty'
import { getCategoryList, createCategory } from '@/api/modules/category'
import { useFormDraftStore } from '@/stores/formDraft'
import { getCache, setCache } from '@/utils/storage'

const router = useRouter()
const formRef = ref<FormInstance>()
const formDraftStore = useFormDraftStore()

const form = ref({
  title: '',
  content: '',
  dynastyId: undefined as number | undefined,
  categoryId: undefined as number | string | undefined,
  source: '',
  isOriginal: 1 as number
})

const dynasties = ref<Dynasty[]>([])
const categories = ref<Category[]>([])
const submitting = ref(false)
const checkingDuplicate = ref(false)

const modernDynasty = computed(() => {
  return dynasties.value.find(d => d.name === '现代')
})

const isTransferring = computed(() => form.value.isOriginal === 0)

const rules: FormRules = {
  title: [
    { required: true, message: '请输入诗词标题', trigger: 'blur' },
    { max: 50, message: '标题长度不能超过50个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入诗词内容', trigger: 'blur' },
    { max: 2000, message: '内容长度不能超过2000个字符', trigger: 'blur' }
  ],
  source: [
    { 
      required: true, 
      message: '转载引用必须填写出处', 
      trigger: 'blur',
      validator: (rule: any, value: string, callback: any) => {
        if (form.value.isOriginal === 0 && !value?.trim()) {
          callback(new Error('转载引用必须填写出处'))
        } else {
          callback()
        }
      }
    },
    { max: 200, message: '出处长度不能超过200个字符', trigger: 'blur' }
  ]
}

const fetchOptions = async () => {
  try {
    const cachedDynasties = getCache<Dynasty[]>('dynasties')
    const cachedCategories = getCache<Category[]>('categories')
    if (cachedDynasties && cachedCategories) {
      dynasties.value = cachedDynasties
      categories.value = cachedCategories
      return
    }

    const [dynastyRes, categoryRes] = await Promise.all([
      getDynastyList(),
      getCategoryList()
    ])
    dynasties.value = dynastyRes.data
    categories.value = categoryRes.data
    setCache('dynasties', dynastyRes.data, 60 * 60 * 1000)
    setCache('categories', categoryRes.data, 60 * 60 * 1000)
  } catch (error) {
    console.error('获取选项失败:', error)
  }
}

const setDefaultDynasty = () => {
  if (form.value.isOriginal === 1 && modernDynasty.value) {
    form.value.dynastyId = modernDynasty.value.id
  } else if (form.value.isOriginal === 0) {
    form.value.dynastyId = undefined
    form.value.source = ''
  }
}

const handleOriginalChange = () => {
  setDefaultDynasty()
}

const checkDuplicate = async (): Promise<boolean> => {
  if (!form.value.title.trim() && !form.value.content.trim()) {
    return false
  }

  checkingDuplicate.value = true
  try {
    const keyword = form.value.title.trim() || form.value.content.trim().substring(0, 20)
    const res = await searchPoems(keyword)
    const poems = res.data || []
    
    const titleDuplicate = poems.find(p => 
      p.title === form.value.title.trim()
    )
    
    if (titleDuplicate) {
      const contentSimilar = form.value.content.trim() && 
        poems.some(p => p.content && p.content.replace(/\s/g, '') === form.value.content.trim().replace(/\s/g, ''))
      
      if (contentSimilar) {
        try {
          await ElMessageBox.confirm(
            '检测到已存在相同标题和内容的诗词，是否继续发布？',
            '重复内容提醒',
            {
              confirmButtonText: '继续发布',
              cancelButtonText: '取消',
              type: 'warning'
            }
          )
          return false
        } catch {
          return true
        }
      } else {
        ElMessage.warning('已存在相同标题的诗词，请确认是否为新作品')
        return false
      }
    }
    
    return false
  } catch (error) {
    return false
  } finally {
    checkingDuplicate.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      const isDuplicate = await checkDuplicate()
      if (isDuplicate) return
      
      submitting.value = true
      try {
        let categoryId = form.value.categoryId
        
        if (typeof categoryId === 'string' && categoryId.trim()) {
          const existingCategory = categories.value.find(c => c.name === categoryId.trim())
          if (existingCategory) {
            categoryId = existingCategory.id
          } else {
            const newCategory = await createCategory({ name: categoryId.trim() })
            categoryId = newCategory.data.id
            categories.value.push(newCategory.data)
          }
        }
        
        const submitData = {
          title: form.value.title,
          content: form.value.content,
          dynastyId: form.value.isOriginal === 1 ? (form.value.dynastyId || modernDynasty.value?.id) : form.value.dynastyId,
          categoryId: typeof categoryId === 'number' ? categoryId : undefined,
          source: form.value.source || undefined,
          isOriginal: form.value.isOriginal
        }
        
        const res = await createPoem(submitData)
        ElMessage.success('发布成功')
        formDraftStore.deleteDraft('poem')
        router.push(`/poem/${res.data.id}`)
      } catch (error) {
        ElMessage.error('发布失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleReset = () => {
  form.value = {
    title: '',
    content: '',
    dynastyId: undefined,
    categoryId: undefined,
    source: '',
    isOriginal: 1
  }
  formDraftStore.deleteDraft('poem')
}

const loadDraft = () => {
  const draft = formDraftStore.loadDraft('poem')
  if (draft) {
    form.value = draft.data as any
    ElMessage.info('已恢复未保存的草稿')
  }
}

const saveDraft = () => {
  if (form.value.title || form.value.content) {
    formDraftStore.saveDraft('poem', form.value)
  }
}

onMounted(() => {
  fetchOptions().then(() => {
    setDefaultDynasty()
  })
  loadDraft()
})

onUnmounted(() => {
  saveDraft()
})
</script>

<template>
  <div class="poem-create-page ink-bg ink-bg--4">
    <div class="container page-container">
      <div class="page-header">
        <div class="header-content">
          <h1 class="page-title">发布新诗</h1>
          <p class="page-subtitle">分享您的诗词创作，传承中华诗词文化，与诗词爱好者交流心得</p>
        </div>
        <el-button @click="router.push('/poem')">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>
      
      <div class="create-layout">
        <el-card class="create-card">
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            class="create-form"
          >
            <div class="main-input-section">
              <el-form-item prop="title" class="title-form-item">
                <template #label>
                  <div class="form-label">
                    <el-icon><EditPen /></el-icon>
                    <span>诗词标题</span>
                  </div>
                </template>
                <el-input
                  v-model="form.title"
                  placeholder="请输入诗词标题，如《静夜思》《登鹳雀楼》"
                  maxlength="50"
                  show-word-limit
                  size="large"
                />
              </el-form-item>
              
              <el-form-item prop="content" class="content-form-item">
                <template #label>
                  <div class="form-label">
                    <el-icon><Document /></el-icon>
                    <span>诗词内容</span>
                  </div>
                </template>
                <el-input
                  v-model="form.content"
                  type="textarea"
                  :rows="12"
                  placeholder="请输入诗词内容，每行一句，系统会自动保留换行格式"
                  maxlength="2000"
                  show-word-limit
                />
              </el-form-item>
            </div>
            
            <el-form-item label="原创标识" prop="isOriginal" class="original-form-item">
              <template #label>
                <div class="form-label">
                  <el-icon><Medal /></el-icon>
                  <span>原创标识</span>
                </div>
              </template>
              <el-radio-group v-model="form.isOriginal" @change="handleOriginalChange">
                <el-radio :value="1">原创作品</el-radio>
                <el-radio :value="0">转载引用</el-radio>
              </el-radio-group>
              <div class="original-tip">
                <template v-if="form.isOriginal === 1">
                  <el-icon><CircleCheck /></el-icon>
                  <span>标记为原创的作品将展示原创标识，发布后需通过审核（默认朝代：现代）</span>
                </template>
                <template v-else>
                  <el-icon><Warning /></el-icon>
                  <span>转载引用请务必填写出处信息，尊重原作者版权</span>
                </template>
              </div>
            </el-form-item>
            
            <div class="form-row" v-if="isTransferring">
              <el-form-item label="朝代" class="form-item-half">
                <template #label>
                  <div class="form-label">
                    <el-icon><Location /></el-icon>
                    <span>朝代</span>
                  </div>
                </template>
                <el-select
                  v-model="form.dynastyId"
                  placeholder="请选择朝代"
                  clearable
                >
                  <el-option
                    v-for="dynasty in dynasties"
                    :key="dynasty.id"
                    :label="dynasty.name"
                    :value="dynasty.id"
                  />
                </el-select>
              </el-form-item>
              
              <el-form-item label="出处" prop="source" class="form-item-half">
                <template #label>
                  <div class="form-label">
                    <el-icon><Link /></el-icon>
                    <span>出处</span>
                  </div>
                </template>
                <el-input
                  v-model="form.source"
                  placeholder="请输入出处，如《唐诗三百首》《全宋词》等"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
            </div>
            
            <el-form-item label="分类" class="category-form-item">
              <template #label>
                <div class="form-label">
                  <el-icon><Folder /></el-icon>
                  <span>分类</span>
                </div>
              </template>
              <el-select
                v-model="form.categoryId"
                placeholder="请选择或输入分类（可选）"
                clearable
                filterable
                allow-create
                default-first-option
              >
                <el-option
                  v-for="category in categories"
                  :key="category.id"
                  :label="category.name"
                  :value="category.id"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item class="form-actions">
              <el-button
                type="primary"
                :loading="submitting || checkingDuplicate"
                @click="handleSubmit"
              >
                <el-icon><Promotion /></el-icon>
                {{ checkingDuplicate ? '查重中...' : '发布诗词' }}
              </el-button>
              <el-button @click="handleReset">
                <el-icon><RefreshLeft /></el-icon>
                重置
              </el-button>
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
              <li>请确保诗词内容为原创或已获得授权</li>
              <li>每行一句，系统会自动保留换行格式</li>
              <li>可以选择已有分类或输入新的分类名称</li>
              <li>原创作品默认归类为现代，发布后将进入审核流程</li>
              <li>转载引用需注明原作者和出处，尊重知识产权</li>
              <li>系统会自动检测重复内容，避免重复发布</li>
            </ul>
          </el-card>
          
          <el-card class="writing-guide-card">
            <template #header>
              <div class="writing-guide-header">
                <el-icon><Reading /></el-icon>
                <span>创作指南</span>
              </div>
            </template>
            <div class="writing-guide-content">
              <div class="guide-item">
                <div class="guide-icon">
                  <el-icon><Edit /></el-icon>
                </div>
                <div class="guide-text">
                  <h4>标题规范</h4>
                  <p>使用诗词原名或自拟标题，简洁明了</p>
                </div>
              </div>
              <div class="guide-item">
                <div class="guide-icon">
                  <el-icon><Document /></el-icon>
                </div>
                <div class="guide-text">
                  <h4>内容格式</h4>
                  <p>每行一句，保持诗词的韵律和节奏</p>
                </div>
              </div>
              <div class="guide-item">
                <div class="guide-icon">
                  <el-icon><Folder /></el-icon>
                </div>
                <div class="guide-text">
                  <h4>分类选择</h4>
                  <p>可选择已有分类或输入新的分类名称</p>
                </div>
              </div>
              <div class="guide-item">
                <div class="guide-icon">
                  <el-icon><Medal /></el-icon>
                </div>
                <div class="guide-text">
                  <h4>原创标识</h4>
                  <p>原创作品展示专属标识，转载需注明出处</p>
                </div>
              </div>
              <div class="guide-item">
                <div class="guide-icon">
                  <el-icon><Search /></el-icon>
                </div>
                <div class="guide-text">
                  <h4>查重机制</h4>
                  <p>发布前自动检测重复，避免内容冲突</p>
                </div>
              </div>
            </div>
          </el-card>
          
          <el-card class="examples-card">
            <template #header>
              <div class="examples-header">
                <el-icon><Star /></el-icon>
                <span>优秀示例</span>
              </div>
            </template>
            <div class="examples-content">
              <div class="example-item">
                <div class="example-title">《静夜思》</div>
                <div class="example-author">李白 · 唐代</div>
                <div class="example-content">
                  床前明月光，<br>
                  疑是地上霜。<br>
                  举头望明月，<br>
                  低头思故乡。
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.poem-create-page {
  padding: 75px 0 $spacing-xl;
  min-height: 60vh;
}

.page-container {
  padding-left: $spacing-xxl;
  padding-right: $spacing-xxl;
  
  @include responsive(md) {
    padding-left: $spacing-lg;
    padding-right: $spacing-lg;
  }
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
  box-shadow: $box-shadow;
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

.create-form {
  max-width: 100%;
}

.main-input-section {
  background-color: rgba($primary-color, 0.02);
  border: 1px solid rgba($primary-color, 0.08);
  border-radius: $border-radius-md;
  padding: $spacing-lg;
  margin-bottom: $spacing-lg;
}

.title-form-item {
  margin-bottom: $spacing-lg;
  
  :deep(.el-input__inner) {
    font-size: $font-size-xl;
    font-weight: 600;
    padding: $spacing-md $spacing-lg;
  }
}

.content-form-item {
  margin-bottom: 0;
  
  :deep(.el-textarea__inner) {
    font-size: $font-size-lg;
    line-height: 2;
    padding: $spacing-md $spacing-lg;
    min-height: 300px;
  }
}

.original-form-item {
  margin-bottom: $spacing-lg;
  padding: $spacing-md;
  background-color: rgba($background-color, 0.5);
  border-radius: $border-radius-sm;
}

.category-form-item {
  margin-bottom: $spacing-lg;
  
  :deep(.el-select) {
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

.form-row {
  display: flex;
  gap: $spacing-lg;
  margin-bottom: $spacing-lg;
  
  @include responsive(md) {
    flex-direction: column;
    gap: 0;
  }
}

.form-item-half {
  flex: 1;
}

.form-actions {
  margin-top: $spacing-xl;
  padding-top: $spacing-lg;
  border-top: 1px solid $border-color-light;
  
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

.original-tip {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
  font-size: $font-size-sm;
  color: $text-color-secondary;
  line-height: 1.5;
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

.writing-guide-card {
  border-radius: $border-radius-md;
  
  .writing-guide-header {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: $font-size-lg;
    color: $primary-color;
  }
}

.writing-guide-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.guide-item {
  display: flex;
  gap: $spacing-md;
  
  .guide-icon {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background-color: rgba($primary-color, 0.1);
    display: flex;
    align-items: center;
    justify-content: center;
    color: $primary-color;
    flex-shrink: 0;
  }
  
  .guide-text {
    flex: 1;
    
    h4 {
      font-size: $font-size-base;
      font-weight: 600;
      color: $text-color;
      margin: 0 0 $spacing-xs 0;
    }
    
    p {
      font-size: $font-size-sm;
      color: $text-color-secondary;
      line-height: 1.5;
      margin: 0;
    }
  }
}

.examples-card {
  border-radius: $border-radius-md;
  
  .examples-header {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: $font-size-lg;
    color: $primary-color;
  }
}

.examples-content {
  .example-item {
    padding: $spacing-md;
    background-color: rgba($primary-color, 0.05);
    border-radius: $border-radius-sm;
    border-left: 3px solid $primary-color;
    
    .example-title {
      font-size: $font-size-lg;
      font-weight: 600;
      color: $text-color;
      margin-bottom: $spacing-xs;
    }
    
    .example-author {
      font-size: $font-size-sm;
      color: $text-color-secondary;
      margin-bottom: $spacing-md;
    }
    
    .example-content {
      font-size: $font-size-base;
      color: $text-color;
      line-height: 1.8;
      font-family: $font-family-title;
    }
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

:deep(.el-textarea__inner) {
  font-size: $font-size-lg;
  line-height: 1.8;
}

:deep(.el-radio-group) {
  display: flex;
  gap: $spacing-lg;
}

:deep(.el-radio) {
  margin-right: 0;
}

/* ============================================================
   视觉统一优化（宣纸 + 铜墨风格）
   复用规范见 .trae/documents/墨渊-视觉风格与可复用细节要点.md
   仅样式层调整，不改动模板结构与交互逻辑
   ============================================================ */

/* 页面标题：左对齐 + 渐变装饰线（覆盖全局 h1 的 flex 居中） */
.page-title {
  display: block;
  text-align: left;
  position: relative;
  letter-spacing: 2px;
  margin-bottom: $spacing-md;
  padding-bottom: $spacing-sm;

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

/* 卡片统一：宣纸底 + 铜色细边 + 顶部渐变装饰条 */
.create-card,
.tips-card,
.writing-guide-card,
.examples-card {
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
  :deep(.el-card__body) {
    padding: $spacing-xl;
  }
}

/* 主输入区：宣纸暖色底块 */
.main-input-section {
  background: linear-gradient(180deg, rgba($primary-50, 0.55), rgba($surface-primary, 0.9));
  border: 1px solid rgba($border-color-bronze, 0.4);
  border-radius: $border-radius-lg;
}

/* 原创标识：暖色底块 + 铜色描边 */
.original-form-item {
  background-color: rgba($primary-50, 0.6);
  border: 1px solid rgba($border-color-bronze, 0.35);
  border-radius: $border-radius-md;
}

/* 表单标签图标徽章 */
.form-label .el-icon {
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

/* 操作区：铜色分隔线 */
.form-actions {
  border-top-color: rgba($border-color-bronze, 0.35);
}

/* 侧栏卡片头部：暖色渐变 + 铜色分隔线 */
.tips-card,
.writing-guide-card,
.examples-card {
  :deep(.el-card__header) {
    padding: $spacing-md $spacing-lg;
    background: linear-gradient(180deg, rgba($primary-50, 0.8), rgba($surface-primary, 0.2));
    border-bottom: 1px solid rgba($border-color-bronze, 0.35);
  }

  :deep(.el-card__body) {
    padding: $spacing-lg;
  }
}

/* 侧栏区块标题：左侧渐变竖条 */
.tips-header,
.writing-guide-header,
.examples-header {
  position: relative;
  padding-left: $spacing-sm;
  font-weight: 600;

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

/* 发布提示列表：自定义铜色圆点 */
.tips-list {
  padding-left: 0;
  list-style: none;

  li {
    position: relative;
    padding-left: $spacing-md;
    transition: color $transition-fast;

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

    &:hover {
      color: $text-color;
    }
  }
}

/* 创作指南：圆形图标改淡铜底 + 铜色描边 */
.guide-item .guide-icon {
  background-color: $tag-bg-light;
  box-shadow: inset 0 0 0 1px rgba($border-color-bronze, 0.5);
}

/* 优秀示例：暖色块 + 左色条 + 悬停位移 */
.examples-content .example-item {
  background: linear-gradient(135deg, rgba($primary-50, 0.85), rgba($surface-primary, 0.9));
  border-left: 3px solid rgba($secondary-color, 0.55);
  border-radius: $border-radius-md;
  transition: all $transition-fast;

  &:hover {
    border-left-color: $primary-color;
    transform: translateX(2px);
  }
}

/* 输入框 / 下拉框：铜色细边 + 主题聚焦光环 */
/* 注意：伪类必须写在 :deep() 括号内，Sass 会把 &:hover 提到 :deep() 外面导致选择器失效 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
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

/* 文本域：铜色描边 + 主题聚焦光环（去掉 EP 默认灰内阴影，避免双边框） */
:deep(.el-textarea__inner) {
  font-family: $font-family-input;
  color: $text-color;
  background-color: $background-color-light;
  border: 1px solid rgba($border-color-bronze, 0.7);
  border-radius: $border-radius-md;
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

/* 原创标识单选：替换 Element Plus 默认蓝 */
:deep(.el-radio__inner:hover) {
  border-color: $primary-color;
}

:deep(.el-radio__input.is-checked .el-radio__inner) {
  background: $primary-color;
  border-color: $primary-color;
}

:deep(.el-radio__input.is-checked + .el-radio__label) {
  color: $primary-color;
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

/* 次要按钮（返回列表 / 重置）：铜色描边幽灵按钮 */
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
