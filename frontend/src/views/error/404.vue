<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="error-page">
    <div class="error-container">
      <div class="error-content">
        <h1 class="error-code">404</h1>
        <h2 class="error-title">页面未找到</h2>
        <p class="error-desc">
          抱歉，您访问的页面不存在或已被移除
        </p>
        <div class="error-actions">
          <el-button type="primary" size="large" @click="goHome">
            <el-icon><HomeFilled /></el-icon>
            返回首页
          </el-button>
          <el-button size="large" @click="goBack">
            <el-icon><Back /></el-icon>
            返回上页
          </el-button>
        </div>
      </div>
      
      <div class="error-decoration">
        <div class="decoration-circle"></div>
        <div class="decoration-line"></div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.error-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, $primary-color 0%, $secondary-color 100%);
  padding: $spacing-lg;
}

.error-container {
  text-align: center;
  position: relative;
}

.error-content {
  background: $background-color-light;
  border-radius: $border-radius-xl;
  box-shadow: $box-shadow-xl;
  padding: $spacing-xxl * 2;
  position: relative;
  z-index: 1;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  color: $primary-color;
  font-family: $font-family-title;
  line-height: 1;
  margin-bottom: $spacing-md;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

.error-title {
  font-size: $font-size-title;
  color: $text-color;
  margin-bottom: $spacing-md;
}

.error-desc {
  font-size: $font-size-lg;
  color: $text-color-secondary;
  margin-bottom: $spacing-xl;
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
}

.error-actions {
  display: flex;
  justify-content: center;
  gap: $spacing-md;
}

.error-decoration {
  position: absolute;
  top: -50px;
  right: -50px;
  width: 200px;
  height: 200px;
  opacity: 0.1;
}

.decoration-circle {
  width: 100%;
  height: 100%;
  border: 4px solid $primary-color;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.decoration-line {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100px;
  height: 4px;
  background-color: $primary-color;
  transform: translate(-50%, -50%) rotate(45deg);
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 0.1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.2;
  }
  100% {
    transform: scale(1);
    opacity: 0.1;
  }
}
</style>