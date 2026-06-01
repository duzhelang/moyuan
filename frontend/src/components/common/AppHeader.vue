<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const currentTime = ref('')

const asset = (path: string) => path

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

let timeTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
  if (userStore.isLoggedIn && !userStore.userInfo) {
    userStore.fetchUserInfo()
  }
})

onUnmounted(() => {
  if (timeTimer) clearInterval(timeTimer)
})

const handleLogout = async () => {
  try {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } catch {
    ElMessage.error('退出失败')
  }
}

const goToProfile = () => {
  router.push('/user/profile')
}

const goToLogin = () => {
  router.push('/user/login')
}

const goToRegister = () => {
  router.push('/user/register')
}

const handleUserCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/user/profile')
      break
    case 'favorites':
      router.push('/user/profile?tab=favorites')
      break
    case 'posts':
      router.push('/user/profile?tab=posts')
      break
    case 'history':
      router.push('/user/profile?tab=history')
      break
    case 'admin':
      router.push('/admin/dashboard')
      break
    case 'logout':
      handleLogout()
      break
  }
}
</script>

<template>
  <div class="header">
    <div class="header-content">
      <div class="header-left">
        <router-link to="/" class="logo-link">
          <img :src="asset('/img/tubiao (1).jpg')" alt="墨渊" class="logo-img">
          <span class="logo-text">墨渊</span>
        </router-link>
      </div>
      
      <div class="header-center">
        <div class="time-display" title="现在时间">
          {{ currentTime }}
        </div>
      </div>
      
      <div class="header-right">
        <div class="contact-item">
          <span class="contact-text">了解我们</span>
          <div class="qr-popup">
            <img :src="asset('/img/微信二维.jpg')" alt="了解我们">
          </div>
        </div>
        <div class="contact-item">
          <span class="contact-text">联系我们</span>
          <div class="qr-popup">
            <img :src="asset('/img/微信二维.jpg')" alt="联系我们">
          </div>
        </div>
        <div class="divider"></div>
        <div class="user-actions">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown trigger="click" @command="handleUserCommand">
              <div class="user-info">
                <el-avatar 
                  :src="userStore.avatar" 
                  :size="32"
                  class="user-avatar"
                >
                  {{ userStore.username?.charAt(0)?.toUpperCase() }}
                </el-avatar>
                <span class="user-nickname">
                  {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}
                </span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="favorites">
                    <el-icon><Star /></el-icon>
                    我的收藏
                  </el-dropdown-item>
                  <el-dropdown-item command="posts">
                    <el-icon><Document /></el-icon>
                    我的帖子
                  </el-dropdown-item>
                  <el-dropdown-item command="history">
                    <el-icon><Clock /></el-icon>
                    浏览历史
                  </el-dropdown-item>
                  <el-dropdown-item v-if="userStore.userInfo?.role === 'admin'" divided command="admin">
                    <el-icon><Setting /></el-icon>
                    后台管理
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" link @click="goToLogin" class="login-btn">登录</el-button>
            <span class="separator">|</span>
            <el-button type="primary" link @click="goToRegister" class="login-btn">注册</el-button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.header {
  position: relative;
  z-index: 100;
  width: 100%;
  height: 55px;
  background: url('/img/dt_3.jpg') no-repeat 0px -320px / cover;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.header-content {
  width: 100%;
  max-width: 1512px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 30px;
}

.header-left {
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

.header-center {
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
  backdrop-filter: blur(4px);
}

.header-right {
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
  backdrop-filter: blur(10px);
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
    animation: fadeIn 0.2s ease;
  }
  
  img {
    width: 110px;
    height: 110px;
    border-radius: 6px;
    display: block;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-5px);
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

.user-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-family: cursive;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 20px;
  transition: all 0.2s ease;
  
  &:hover {
    background-color: rgba(255, 255, 255, 0.1);
  }
}

.user-avatar {
  border: 2px solid rgba(255, 255, 255, 0.3);
  transition: all 0.2s ease;
  
  &:hover {
    border-color: #ffd700;
    box-shadow: 0 0 8px rgba(255, 215, 0, 0.5);
  }
}

.user-nickname {
  color: #f0e6d3;
  font-size: 14px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);
}

.login-btn {
  color: #f0e6d3 !important;
  font-size: 15px;
  font-family: cursive;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);
  transition: all 0.2s ease;
  
  &:hover {
    color: #ffd700 !important;
    text-shadow: 0 0 8px rgba(255, 215, 0, 0.5);
  }
}

.separator {
  color: rgba(255, 255, 255, 0.4);
  font-size: 14px;
}
</style>
