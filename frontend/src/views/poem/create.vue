<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { Dynasty, Category } from '@/types/model'
import { createPoem } from '@/api/modules/poem'
import { getDynastyList } from '@/api/modules/dynasty'
import { getCategoryList } from '@/api/modules/category'
import { useFormDraftStore } from '@/stores/formDraft'
import { getCache, setCache } from '@/utils/storage'

const router = useRouter()
const formRef = ref<FormInstance>()
const formDraftStore = useFormDraftStore()

const form = ref({
  title: '',
  content: '',
  dynastyId: undefined as number | undefined,
  categoryId: undefined as number | undefined,
  source: '',
  isOriginal: 1 as number
})

const dynasties = ref<Dynasty[]>([])
const categories = ref<Category[]>([])
const submitting = ref(false)

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

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const res = await createPoem({
          title: form.value.title,
          content: form.value.content,
          dynastyId: form.value.dynastyId,
          categoryId: form.value.categoryId,
          source: form.value.source || undefined,
          isOriginal: form.value.isOriginal
        })
        ElMessage.success('发布成功')
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
  fetchOptions()
  loadDraft()
})

onUnmounted(() => {
  saveDraft()
})
</script>

<template>
  <div class="poem-create-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">发布新诗</h1>
        <el-button @click="router.push('/poem')">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>
      
      <el-card class="create-card">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="create-form"
        >
          <el-form-item label="诗词标题" prop="title">
            <el-input
              v-model="form.title"
              placeholder="请输入诗词标题"
              maxlength="50"
              show-word-limit
            />
          </el-form-item>
          
          <el-form-item label="诗词内容" prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="10"
              placeholder="请输入诗词内容，每行一句"
              maxlength="2000"
              show-word-limit
            />
          </el-form-item>
          
          <div class="form-row">
            <el-form-item label="朝代" class="form-item-half">
              <el-select
                v-model="form.dynastyId"
                placeholder="请选择朝代（可选）"
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
            
            <el-form-item label="分类" class="form-item-half">
              <el-select
                v-model="form.categoryId"
                placeholder="请选择分类（可选）"
                clearable
              >
                <el-option
                  v-for="category in categories"
                  :key="category.id"
                  :label="category.name"
                  :value="category.id"
                />
              </el-select>
            </el-form-item>
          </div>
          
          <el-form-item label="出处" prop="source">
            <el-input
              v-model="form.source"
              placeholder="请输入出处（可选）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
          
          <el-form-item label="原创标识" prop="isOriginal">
            <el-radio-group v-model="form.isOriginal">
              <el-radio :value="1">原创作品</el-radio>
              <el-radio :value="0">转载引用</el-radio>
            </el-radio-group>
            <div class="original-tip">
              <template v-if="form.isOriginal === 1">
                标记为原创的作品将展示原创标识，发布后需通过审核
              </template>
              <template v-else>
                转载引用请务必填写出处信息，尊重原作者版权
              </template>
            </div>
          </el-form-item>
          
          <el-form-item class="form-actions">
            <el-button
              type="primary"
              :loading="submitting"
              @click="handleSubmit"
            >
              <el-icon><Edit /></el-icon>
              发布诗词
            </el-button>
            <el-button @click="handleReset">
              <el-icon><RefreshLeft /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
      
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
          <li>选择合适的朝代和分类有助于其他用户发现您的作品</li>
          <li>填写出处可以增加诗词的可信度</li>
          <li>原创作品发布后将进入审核流程，审核通过后展示原创标识</li>
          <li>转载引用需注明原作者和出处，尊重知识产权</li>
        </ul>
      </el-card>
    </div>
  </div>
</template>

<style scoped lang="scss">
.poem-create-page {
  padding: $spacing-xl 0;
  min-height: 60vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xl;
}

.page-title {
  font-size: $font-size-title;
  color: $primary-color;
  font-family: $font-family-title;
}

.create-card {
  margin-bottom: $spacing-xl;
  border-radius: $border-radius-md;
}

.create-form {
  max-width: 800px;
}

.form-row {
  display: flex;
  gap: $spacing-lg;
  
  @include responsive(md) {
    flex-direction: column;
    gap: 0;
  }
}

.form-item-half {
  flex: 1;
}

.form-actions {
  margin-top: $spacing-lg;
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

.original-tip {
  margin-top: $spacing-sm;
  font-size: $font-size-sm;
  color: $text-color-secondary;
  line-height: 1.5;
}

.tips-list {
  margin: 0;
  padding-left: $spacing-lg;
  
  li {
    font-size: $font-size-base;
    color: $text-color-secondary;
    line-height: $line-height-loose;
  }
}
</style>
