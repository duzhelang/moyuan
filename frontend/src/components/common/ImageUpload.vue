<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { UploadProps, UploadFile } from 'element-plus'
import { uploadFile } from '@/api/modules/file'

const props = withDefaults(defineProps<{
  modelValue?: string
  maxSize?: number
}>(), {
  modelValue: '',
  maxSize: 5
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const imageUrl = ref(props.modelValue)
const uploading = ref(false)

watch(() => props.modelValue, (val) => {
  imageUrl.value = val
})

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp'].includes(file.type)
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLtMax = file.size / 1024 / 1024 < props.maxSize
  if (!isLtMax) {
    ElMessage.error(`图片大小不能超过 ${props.maxSize}MB`)
    return false
  }
  return true
}

const customUpload = async (options: any) => {
  uploading.value = true
  try {
    const res = await uploadFile(options.file)
    imageUrl.value = res.data.url
    emit('update:modelValue', res.data.url)
    ElMessage.success('上传成功')
    options.onSuccess(res.data)
  } catch (error) {
    ElMessage.error('上传失败')
    options.onError(error)
  } finally {
    uploading.value = false
  }
}

const handleRemove = () => {
  imageUrl.value = ''
  emit('update:modelValue', '')
}
</script>

<template>
  <div class="image-upload">
    <el-upload
      v-if="!imageUrl"
      class="upload-area"
      drag
      :show-file-list="false"
      :before-upload="beforeUpload"
      :http-request="customUpload"
      :disabled="uploading"
    >
      <div class="upload-content" v-loading="uploading">
        <el-icon class="upload-icon"><Plus /></el-icon>
        <div class="upload-text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="upload-tip">支持 jpg/png/gif/webp，最大 {{ maxSize }}MB</div>
      </div>
    </el-upload>

    <div v-else class="preview-area">
      <img :src="imageUrl" class="preview-image" />
      <div class="preview-actions">
        <el-button type="danger" size="small" @click="handleRemove">
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.image-upload {
  width: 100%;
}

.upload-area {
  width: 100%;

  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
    padding: $spacing-xl;
  }
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
  min-height: 120px;
  justify-content: center;
}

.upload-icon {
  font-size: 40px;
  color: $text-color-secondary;
}

.upload-text {
  font-size: $font-size-base;
  color: $text-color-secondary;

  em {
    color: $primary-color;
    font-style: normal;
  }
}

.upload-tip {
  font-size: $font-size-sm;
  color: $text-color-light;
}

.preview-area {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
  padding: $spacing-md;
  border: 1px solid $border-color;
  border-radius: $border-radius-md;
}

.preview-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: $border-radius-sm;
}

.preview-actions {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}
</style>
