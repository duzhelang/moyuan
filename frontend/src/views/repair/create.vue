<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadProps, UploadFile, UploadRawFile } from 'element-plus'
import { Back, List, ChatDotRound, SwitchButton, Plus, Delete } from '@element-plus/icons-vue'
import { createRepairOrder } from '@/api/modules/repair'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const fileList = ref<UploadFile[]>([])
const pasteAreaRef = ref<HTMLDivElement>()

const form = reactive({
  title: '',
  description: '',
  category: '',
  priority: 2,
  images: [] as string[]
})

const categoryOptions = [
  { value: 'system', label: '系统故障' },
  { value: 'function', label: '功能异常' },
  { value: 'ui', label: '界面问题' },
  { value: 'data', label: '数据问题' },
  { value: 'other', label: '其他' }
]

const priorityOptions = [
  { value: 1, label: '低' },
  { value: 2, label: '中' },
  { value: 3, label: '高' },
  { value: 4, label: '紧急' }
]

const rules: FormRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入问题描述', trigger: 'blur' },
    { min: 10, max: 2000, message: '描述长度在 10 到 2000 个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择问题类别', trigger: 'change' }
  ]
}

const uploadHeaders = computed(() => ({
  Authorization: userStore.token ? `Bearer ${userStore.token}` : ''
}))

const uploadAction = '/api/file/upload/image'

const handleUploadSuccess = (response: any, file: UploadFile) => {
  if (response.code === 200) {
    form.images.push(response.data.url)
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error('图片上传失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('图片上传失败')
}

const handleRemoveFile = (file: UploadFile) => {
  const index = fileList.value.indexOf(file)
  if (index > -1) {
    fileList.value.splice(index, 1)
    form.images.splice(index, 1)
  }
}

const handlePaste = (event: ClipboardEvent) => {
  const items = event.clipboardData?.items
  if (!items) return

  for (let i = 0; i < items.length; i++) {
    if (items[i].type.indexOf('image') !== -1) {
      event.preventDefault()
      const file = items[i].getAsFile()
      if (file) {
        const reader = new FileReader()
        reader.onload = (e) => {
          const base64 = e.target?.result as string
          form.images.push(base64)
          ElMessage.success('图片已粘贴')
        }
        reader.readAsDataURL(file)
      }
      break
    }
  }
}

const handleRemoveImage = (index: number) => {
  form.images.splice(index, 1)
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const res = await createRepairOrder({
          title: form.title,
          description: form.description,
          category: form.category,
          priority: form.priority,
          images: form.images.length > 0 ? form.images.join(',') : undefined
        })
        ElMessage.success('报修提交成功')
        if (res.data?.id) {
          router.push(`/repair/${res.data.id}`)
        } else {
          router.push('/repair')
        }
      } catch (error) {
        console.error('提交报修失败', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleReset = () => {
  formRef.value?.resetFields()
  form.images = []
  fileList.value = []
}

const handleCancel = () => {
  router.push('/repair')
}

const goToRepairList = () => {
  router.push('/repair')
}

const goToContact = () => {
  router.push('/contact')
}

const goBack = () => {
  router.go(-1)
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    userStore.logout()
    router.push('/')
    ElMessage.success('已退出登录')
  } catch {}
}
</script>

<template>
  <div class="create-container">
    <div class="create-header">
      <div class="header-left">
        <el-button @click="goBack">
          <el-icon><Back /></el-icon>
          <span>上一页</span>
        </el-button>
        <h2 class="create-title">提交报修</h2>
      </div>
      <div class="header-right">
        <el-button @click="goToRepairList">
          <el-icon><List /></el-icon>
          <span>报修列表</span>
        </el-button>
        <el-button @click="goToContact">
          <el-icon><ChatDotRound /></el-icon>
          <span>联系团队</span>
        </el-button>
        <el-button type="danger" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出</span>
        </el-button>
      </div>
    </div>

    <el-card class="form-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="top"
      >
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入问题标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="问题描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请详细描述您遇到的问题"
            :rows="6"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="问题类别" prop="category">
              <el-select v-model="form.category" placeholder="请选择问题类别" style="width: 100%">
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-radio-group v-model="form.priority">
                <el-radio-button
                  v-for="item in priorityOptions"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="相关图片（可选）">
          <div class="image-upload-area">
            <el-upload
              v-model:file-list="fileList"
              :action="uploadAction"
              :headers="uploadHeaders"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :on-remove="handleRemoveFile"
              list-type="picture-card"
              :limit="5"
              accept="image/*"
            >
              <el-icon><Plus /></el-icon>
              <template #tip>
                <div class="upload-tip">支持上传图片，最多5张</div>
              </template>
            </el-upload>
          </div>

          <div class="paste-area" ref="pasteAreaRef" @paste="handlePaste">
            <el-icon><Plus /></el-icon>
            <span>在此处粘贴图片 (Ctrl+V)</span>
          </div>

          <div v-if="form.images.length > 0" class="image-preview-list">
            <div v-for="(img, index) in form.images" :key="index" class="image-preview-item">
              <el-image :src="img" fit="cover" class="preview-img" />
              <el-button
                class="remove-btn"
                type="danger"
                :icon="Delete"
                circle
                size="small"
                @click="handleRemoveImage(index)"
              />
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交报修</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.create-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.create-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.create-title {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.form-card {
  border-radius: 8px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.image-upload-area {
  margin-bottom: 16px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.paste-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 200px;
  height: 120px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #909399;
  font-size: 14px;
  gap: 8px;

  &:hover {
    border-color: #409eff;
    color: #409eff;
  }
}

.image-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}

.image-preview-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
}

.preview-img {
  width: 100%;
  height: 100%;
}

.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
}
</style>
