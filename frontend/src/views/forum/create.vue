<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { Category } from '@/types/model'
import type { ForumPostCreateRequest } from '@/types/api'
import { createForumPost } from '@/api/modules/forum'
import { getCategoryList } from '@/api/modules/category'

const router = useRouter()

const form = ref<ForumPostCreateRequest>({
  title: '',
  content: '',
  category: ''
})
const categories = ref<Category[]>([])
const submitting = ref(false)

const fetchCategories = async () => {
  try {
    const res = await getCategoryList()
    categories.value = res.data
  } catch (error) {
    ElMessage.error('获取分类列表失败')
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
    router.push(`/forum/${res.data.id}`)
  } catch (error) {
    ElMessage.error('发帖失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchCategories()
})
</script>

<template>
  <div class="forum-create-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">发布新帖</h1>
        <el-button @click="router.push('/forum')">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>
      <el-card class="create-card">
        <el-form :model="form" label-position="top">
          <el-form-item label="标题">
            <el-input
              v-model="form.title"
              placeholder="请输入帖子标题"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="分类">
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
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="12"
              placeholder="请输入帖子内容"
              maxlength="5000"
              show-word-limit
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="submitting"
              @click="handleSubmit"
            >
              发布
            </el-button>
            <el-button @click="router.push('/forum')">取消</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<style scoped lang="scss">
.forum-create-page {
  padding: $spacing-xl 0;
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
  border-radius: $border-radius-md;
}

:deep(.el-form-item__label) {
  font-size: $font-size-lg;
  font-weight: 600;
  color: $text-color;
}

:deep(.el-select) {
  width: 100%;
}
</style>
