<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const currentTime = ref('')
let timeTimer: ReturnType<typeof setInterval> | null = null

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timeTimer) clearInterval(timeTimer)
})

const goHome = () => {
  router.push('/')
}
</script>

<template>
  <header class="auth-header">
    <div class="auth-header-content">
      <div class="auth-header-left">
        <router-link to="/" class="logo-link">
          <img src="/img/tubiao (1).jpg" alt="墨渊" class="logo-img" />
          <span class="logo-text">墨渊</span>
        </router-link>
      </div>

      <div class="auth-header-center">
        <div class="time-display">
          {{ currentTime }}
        </div>
      </div>

      <div class="auth-header-right">
        <div class="contact-item">
          <span class="contact-text">了解我们</span>
          <div class="qr-popup">
            <img src="/img/微信二维.jpg" alt="了解我们" />
          </div>
        </div>
        <div class="contact-item">
          <span class="contact-text">联系我们</span>
          <div class="qr-popup">
            <img src="/img/微信二维.jpg" alt="联系我们" />
          </div>
        </div>
        <div class="divider"></div>
        <span class="home-link" @click="goHome">
          <el-icon class="home-icon"><HomeFilled /></el-icon>
          返回首页
        </span>
      </div>
    </div>
  </header>
</template>

<style scoped lang="scss">
.auth-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 55px;
  background: url('/img/dt_3.jpg') no-repeat 0px -320px / cover;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.auth-header-content {
  width: 100%;
  max-width: 1512px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 30px;
}

.auth-header-left {
  display: flex;
  align-items: center;
}

.logo-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  gap: 8px;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.02);
  }
}

.logo-img {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.logo-text {
  font-size: 30px;
  font-weight: bold;
  color: #2c2c2c;
  font-family: cursive;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.5);
}

.auth-header-center {
  display: flex;
  align-items: center;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.time-display {
  color: #fff;
  background-color: rgba(0, 0, 0, 0.2);
  padding: 4px 14px;
  font-size: 13px;
  border-radius: 15px;
  letter-spacing: 0.5px;
}

.auth-header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.contact-item {
  position: relative;
  cursor: pointer;
  padding: 5px 0;
}

.contact-text {
  color: #f5f5f5;
  font-size: 17px;
  font-family: cursive;
  transition: all 0.2s ease;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);

  .contact-item:hover & {
    color: #fff;
    font-size: 18px;
  }
}

.qr-popup {
  display: none;
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-top: 12px;
  background: rgba(60, 60, 60, 0.9);
  border-radius: 10px;
  padding: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);

  &::before {
    content: '';
    position: absolute;
    top: -6px;
    left: 50%;
    transform: translateX(-50%);
    width: 0;
    height: 0;
    border-left: 6px solid transparent;
    border-right: 6px solid transparent;
    border-bottom: 6px solid rgba(60, 60, 60, 0.9);
  }

  .contact-item:hover & {
    display: block;
    animation: qrFadeIn 0.25s ease;
  }

  img {
    width: 110px;
    height: 110px;
    border-radius: 6px;
    display: block;
  }
}

@keyframes qrFadeIn {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

.divider {
  width: 1px;
  height: 20px;
  background-color: rgba(255, 255, 255, 0.3);
}

.home-link {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #f0e6d3;
  font-size: 15px;
  font-family: cursive;
  cursor: pointer;
  transition: all 0.2s ease;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);

  &:hover {
    color: #ffd700;
    text-shadow: 0 0 8px rgba(255, 215, 0, 0.5);
  }
}

.home-icon {
  font-size: 16px;
}
</style>
