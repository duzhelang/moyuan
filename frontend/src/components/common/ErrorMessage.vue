<script setup lang="ts">
defineProps<{
  message?: string
  showRetry?: boolean
}>()

const emit = defineEmits<{
  retry: []
}>()
</script>

<template>
  <div class="error-message">
    <el-icon class="error-icon"><WarningFilled /></el-icon>
    <p class="error-text">{{ message || '加载失败，请稍后重试' }}</p>
    <el-button v-if="showRetry" type="primary" @click="emit('retry')">
      重新加载
    </el-button>
  </div>
</template>

<style scoped lang="scss">
.error-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-xxl;
}

.error-icon {
  font-size: 48px;
  color: $danger-color;
  margin-bottom: $spacing-md;
}

.error-text {
  font-size: $font-size-base;
  color: $text-color-secondary;
  margin-bottom: $spacing-lg;
  text-align: center;
}
</style>