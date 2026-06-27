<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { getItem, setItem, removeItem } from '@/utils/storage'
import { User, Star, Clock, Setting, SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const showLoginDropdown = ref(false)
const loginDropdownTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const isMouseInDropdownArea = ref(false)
const isLoginInputFocused = ref(false)
const rememberMe = ref(false)
const quickLoginForm = ref({
  username: '',
  password: ''
})

const handleLoginDropdownEnter = () => {
  isMouseInDropdownArea.value = true
  if (loginDropdownTimer.value) {
    clearTimeout(loginDropdownTimer.value)
    loginDropdownTimer.value = null
  }
  showLoginDropdown.value = true
}

const handleLoginDropdownLeave = () => {
  isMouseInDropdownArea.value = false
  loginDropdownTimer.value = setTimeout(() => {
    if (!isMouseInDropdownArea.value && !isLoginInputFocused.value) {
      showLoginDropdown.value = false
    }
    loginDropdownTimer.value = null
  }, 400)
}

const handleDropdownInteractionStart = () => {
  if (loginDropdownTimer.value) {
    clearTimeout(loginDropdownTimer.value)
    loginDropdownTimer.value = null
  }
}

const handleLoginInputFocus = () => {
  isLoginInputFocused.value = true
  if (loginDropdownTimer.value) {
    clearTimeout(loginDropdownTimer.value)
    loginDropdownTimer.value = null
  }
}

const handleLoginInputBlur = () => {
  isLoginInputFocused.value = false
  setTimeout(() => {
    if (!isMouseInDropdownArea.value && !isLoginInputFocused.value) {
      showLoginDropdown.value = false
    }
  }, 300)
}

const handleLoginClick = () => {
  if (userStore.isLoggedIn && userStore.userInfo) {
    router.push('/user/profile')
  } else {
    router.push('/user/login')
  }
}

const handleQuickLogin = async () => {
  if (!quickLoginForm.value.username || !quickLoginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  try {
    await userStore.login({
      username: quickLoginForm.value.username,
      password: quickLoginForm.value.password
    })
    ElMessage.success('登录成功')
    if (rememberMe.value) {
      setItem('remembered_account', {
        username: quickLoginForm.value.username,
        password: quickLoginForm.value.password
      }, 30 * 24 * 60 * 60 * 1000)
    } else {
      removeItem('remembered_account')
    }
    showLoginDropdown.value = false
    quickLoginForm.value = { username: '', password: '' }
  } catch (error: any) {
    ElMessage.error(error.message || '登录失败，请检查用户名和密码')
  }
}

const handleLogout = () => {
  try {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } catch {
    ElMessage.error('退出失败')
  }
}

onMounted(() => {
  const savedAccount = getItem<{ username: string; password: string }>('remembered_account')
  if (savedAccount) {
    quickLoginForm.value.username = savedAccount.username
    quickLoginForm.value.password = savedAccount.password
    rememberMe.value = true
  }
})
</script>

<template>
  <li
    id="xdl_kaiguan"
    @mouseenter="handleLoginDropdownEnter"
    @mouseleave="handleLoginDropdownLeave"
  >
    <el-icon><User /></el-icon>
    <span @click="handleLoginClick">{{ (userStore.isLoggedIn && userStore.userInfo) ? '我的' : '登录' }}</span>
    <div class="dl_icon_wrapper">
      <img class="dl_icon dl_icon_default" src="/img/dl_tb1.png" alt="">
      <img class="dl_icon dl_icon_hover" src="/img/dl_tb2.png" alt="">
    </div>
    <div
      id="sy_xdlchuang"
      class="sy_xdlchuang"
      v-show="showLoginDropdown"
      @mouseenter="handleLoginDropdownEnter"
      @mousedown="handleDropdownInteractionStart"
    >
      <template v-if="userStore.isLoggedIn && userStore.userInfo">
        <div class="user-menu-container" @mouseenter="handleLoginDropdownEnter" @click.stop>
          <div class="user-menu-header">
            <el-avatar :src="userStore.avatar" :size="40">
              {{ userStore.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <span class="user-menu-name">{{ userStore.userInfo?.nickname || userStore.username }}</span>
          </div>
          <div class="user-menu-items">
            <div class="user-menu-item" @click.stop="router.push('/user/profile')">
              <el-icon><User /></el-icon>
              <span>个人中心</span>
            </div>
            <div class="user-menu-item" @click.stop="router.push('/user/profile?tab=favorites')">
              <el-icon><Star /></el-icon>
              <span>我的收藏</span>
            </div>
            <div class="user-menu-item" @click.stop="router.push('/user/profile?tab=history')">
              <el-icon><Clock /></el-icon>
              <span>浏览历史</span>
            </div>
            <div v-if="userStore.userInfo?.role === 'admin'" class="user-menu-item admin-item" @click.stop="router.push('/admin/dashboard')">
              <el-icon><Setting /></el-icon>
              <span>后台管理</span>
            </div>
            <div class="user-menu-item logout-item" @click.stop="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </div>
          </div>
        </div>
      </template>
      <template v-else>
        <div class="login-container" @click.stop @mouseenter="handleLoginDropdownEnter">
          <h2 class="login-title">登&nbsp;&nbsp;&nbsp;&nbsp;录</h2>
          <div class="lieb">
            <label for="ld-username" class="dl_ts">用户名:</label>
            <input type="text" id="ld-username" v-model="quickLoginForm.username" class="input-field" placeholder="请输入用户名" autocomplete="off" @click.stop @focus="handleLoginInputFocus" @blur="handleLoginInputBlur">
          </div>
          <div class="lieb">
            <label for="ld-password" class="dl_ts">密&nbsp;&nbsp;码:</label>
            <input type="password" id="ld-password" v-model="quickLoginForm.password" class="input-field" placeholder="请输入密码" autocomplete="off" required @click.stop @focus="handleLoginInputFocus" @blur="handleLoginInputBlur">
          </div>
          <div class="remember-me-container">
            <label class="remember-me-label" @click.stop>
              <input type="checkbox" v-model="rememberMe" class="remember-me-checkbox" @click.stop>
              <span class="remember-me-text">记住密码</span>
            </label>
          </div>
          <button type="button" class="login-button" @click.stop="handleQuickLogin">登录</button>
          <div class="login-links">
            <div class="forgot-password">
              <a class="fuzhu" href="/user/login" @click.stop>忘记密码?</a>
            </div>
            <div class="register-link">
              <a class="fuzhu" href="/user/register" @click.stop>还没有账号?注册</a>
            </div>
          </div>
        </div>
      </template>
    </div>
  </li>
</template>

<style scoped>
#xdl_kaiguan {
  position: relative;
  cursor: pointer;
}

.dl_icon_wrapper {
  position: absolute;
  top: 40%;
  right: 5px;
  transform: translateY(-50%);
  width: 28px;
  height: 28px;
  cursor: pointer;
}

.dl_icon {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  transition: all 0.3s ease;
}

.dl_icon_default {
  opacity: 1;
  filter: drop-shadow(0 0 0 transparent);
}

.dl_icon_hover {
  opacity: 0;
  filter: drop-shadow(0 0 4px rgba(255, 215, 0, 0.8)) brightness(1.3) contrast(1.2);
}

#xdl_kaiguan:hover .dl_icon_default {
  opacity: 0;
  transform: scale(0.9);
}

#xdl_kaiguan:hover .dl_icon_hover {
  opacity: 1;
  transform: scale(1.1);
}

.sy_xdlchuang {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  z-index: 51;
  cursor: pointer;
  padding-top: 3px !important;
  margin-top: -4px !important;
  width: auto !important;
}

.login-container {
  background: url('/img/dt_0_0.jpg') no-repeat -155px 0 / cover;
  width: 366px;
  min-height: 240px;
  background-color: #f4f4f4;
  border: 1px solid #ccc;
  border-radius: 50px;
  border-top-left-radius: 15px;
  border-bottom-right-radius: 15px;
  padding: 10px 20px 15px;
  box-sizing: border-box;
}

.login-title {
  text-align: center;
  font-size: 28px;
  font-weight: 600;
  color: #000;
  margin: 2px 0;
}

.lieb {
  display: flex;
  align-items: center;
  color: #000;
  margin: 5px 0;
}

.dl_ts {
  font-size: 14px;
  width: 60px;
  text-align: right;
  margin-right: 8px;
}

.input-field {
  flex: 1;
  height: 30px;
  background-color: #ffffff;
  color: #000;
  border-radius: 10px;
  border: 1px solid #ccc;
  padding: 0 10px;
  font-size: 14px;
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
  letter-spacing: 0.5px;
  line-height: 1.5;
}

.login-button {
  width: 100px;
  height: 32px;
  background-color: #007BFF;
  color: white;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  font-size: 14px;
  margin-top: 8px;
}

.remember-me-container {
  display: flex;
  align-items: center;
  margin-top: 6px;
  padding-left: 68px;
}

.remember-me-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.remember-me-checkbox {
  width: 14px;
  height: 14px;
  cursor: pointer;
  accent-color: #007BFF;
}

.remember-me-text {
  font-size: 13px;
  color: #666;
}

.login-button:hover {
  background-color: #0056b3;
}

.login-links {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 6px;
  gap: 12px;
  width: 100%;
  box-sizing: border-box;
  padding: 0 15px;
}

.forgot-password .fuzhu,
.register-link .fuzhu {
  background-color: rgba(204, 204, 204, 0.7);
  padding: 3px 8px;
  border-radius: 4px;
  text-shadow: 0 0 10px white;
  color: #55aaff;
  text-decoration: none;
  white-space: nowrap;
  font-size: 12px;
  display: inline-block;
}

.forgot-password .fuzhu:hover,
.register-link .fuzhu:hover {
  color: #007BFF;
}

.user-menu-container {
  background: url('/img/dt_0_0.jpg') no-repeat -155px 0 / cover;
  width: 160px;
  background-color: #f4f4f4;
  border: 1px solid #ccc;
  border-radius: 15px;
  border-top-left-radius: 15px;
  border-bottom-right-radius: 15px;
  padding: 12px;
  box-sizing: border-box;
}

.user-menu-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  margin-bottom: 8px;
}

.user-menu-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-menu-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #333;
  font-size: 14px;
}

.user-menu-item:hover {
  background-color: rgba(0, 123, 255, 0.1);
  color: #007BFF;
}

.user-menu-item .el-icon {
  font-size: 16px;
}

.admin-item {
  color: #e6a23c;
  margin-top: 4px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  padding-top: 12px;
}

.admin-item:hover {
  background-color: rgba(230, 162, 60, 0.1);
  color: #e6a23c;
}

.logout-item {
  color: #f56c6c;
  margin-top: 4px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  padding-top: 12px;
}
</style>
