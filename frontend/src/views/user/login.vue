<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import AuthHeader from '@/components/common/AuthHeader.vue'
import { getItem, setItem, removeItem } from '@/utils/storage'
import { useParticles } from '@/composables/useParticles'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const particleCanvasRef = ref<HTMLCanvasElement | null>(null)
useParticles(particleCanvasRef, {
  count: 50,
  colors: ['#d4af87', '#f0e4d7', '#c9a06c'],
  opacityRange: [0.2, 0.4],
  sizeRange: [1.5, 3]
})

const loading = ref(false)
const rememberMe = ref(false)
const showPassword = ref(false)

const form = reactive({
  username: '',
  password: ''
})

onMounted(() => {
  const savedAccount = getItem<{ username: string; password: string }>('remembered_account')
  if (savedAccount) {
    form.username = savedAccount.username
    form.password = savedAccount.password
    rememberMe.value = true
  }
})

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password
    })
    if (rememberMe.value) {
      setItem('remembered_account', { username: form.username, password: form.password })
    } else {
      removeItem('remembered_account')
    }
    const redirect = route.query.redirect as string
    if (userStore.userInfo?.role === 'admin' && !redirect) {
      router.push('/admin/dashboard')
    } else {
      router.push(redirect || '/')
    }
  } catch (error) {
    ElMessage.error('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

const goToRegister = () => {
  router.push('/user/register')
}

const togglePassword = () => {
  showPassword.value = !showPassword.value
}

const fillAdminUsername = () => {
  form.username = userStore.lastAdminUsername
}
</script>

<template>
  <div class="dl_container">
    <canvas ref="particleCanvasRef" class="particle-bg"></canvas>
    <AuthHeader />

    <div class="body">
      <div class="dl_tubiao">
        <img src="/img/qyc.png" class="dl_zhuang_s1"/>
        <img src="/img/010.png" class="dl_zhuang_s2"/>
        <img src="/img/fy_tubiao (5).png" class="dl_zhuang_s3"/>
        <img src="/img/tuobiao (3).png" class="dl_zhuang_s4"/>
        <img src="/img/fy_tubiao (10).png" class="dl_zhuang_s5"/>
      </div>

      <div class="login-card">
        <div class="card-content">
          <div class="user-panel">
            <img src="/img/00.jpg" class="avatar" alt="用户头像">
          </div>

          <div class="divider"></div>
          <div class="login-form-panel">
            <h1 class="login-title">
              <router-link to="/user/login" class="login-title-link">登&nbsp;&nbsp;&nbsp;&nbsp;录</router-link>
            </h1>
            <form @submit.prevent="handleLogin" class="login-form">
              <div class="form-group">
                <label for="username" class="form-label">用户名:</label>
                <input type="text" id="username" class="form-input" placeholder="请输入注册手机号" v-model="form.username">
              </div>
              <div class="form-group">
                <label for="password" class="form-label">密&nbsp;&nbsp;码:</label>
                <div class="password-wrapper">
                  <input :type="showPassword ? 'text' : 'password'" id="password" class="form-input" placeholder="请输入密码" required v-model="form.password">
                  <span class="toggle-password" @click="togglePassword">
                    {{ showPassword ? '🙈' : '👁' }}
                  </span>
                </div>
              </div>
              <div class="remember-group">
                <label class="remember-label">
                  <input type="checkbox" v-model="rememberMe" class="remember-checkbox">
                  <span class="remember-text">记住密码</span>
                </label>
              </div>
              <div class="submit-group">
                <button class="login-button" type="submit" :disabled="loading">
                  {{ loading ? '登录中...' : '登录' }}
                </button>
              </div>
            </form>
            <div class="form-footer">
              <div v-if="userStore.lastAdminUsername" class="admin-quick-entry">
                <el-tag 
                  type="warning" 
                  effect="plain" 
                  class="admin-tag"
                  @click="fillAdminUsername"
                >
                  管理员快捷登录
                </el-tag>
              </div>
              <div class="register-link">
                <a class="register-text" href="javascript:void(0)" @click="goToRegister">还没有账号？立即注册</a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="dlzc_ditu_div"></div>
  </div>
</template>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.dl_container {
  height: 100vh;
  background: url('/img/dt_0.0.jpg') no-repeat -155px 0 / cover;
  position: relative;
  font-family: cursive;
  overflow-x: hidden;
  overflow-y: auto;
}

.particle-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

.dl_container::-webkit-scrollbar {
  width: 8px;
}

.dl_container::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
}

.dl_container::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
}

.dl_container::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.5);
}

.body {
  width: 100%;
  max-width: 1512px;
  margin: 0 auto;
  padding-top: 55px;
  padding-bottom: 80px;
  min-height: calc(100vh - 55px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.dl_tubiao {
  height: 0;
  position: relative;
}

.dl_tubiao img {
  position: fixed;
  pointer-events: none;
}

.dl_zhuang_s1 {
  position: fixed;
  top: 75px;
  z-index: 1;
  left: 150px;
  width: 1200px;
  height: 400px;
}

.dl_zhuang_s2 {
  position: fixed;
  top: 35px;
  z-index: 1;
  left: -20px;
  width: 300px;
  height: 200px;
}

.dl_zhuang_s3 {
  position: fixed;
  width: 200px;
  z-index: 1;
  height: 300px;
  top: 70px;
  right: 20px;
}

.dl_zhuang_s4 {
  position: fixed;
  bottom: 250px;
  z-index: 1;
  right: -50px;
  width: 503px;
  height: 300px;
}

.dl_zhuang_s5 {
  position: fixed;
  bottom: 70px;
  z-index: 1;
  left: 20px;
  width: 200px;
  height: 300px;
}

.login-card {
  margin: 20px 10px;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  z-index: 50;
}

.card-content {
  backdrop-filter: blur(1.5px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.2);
  width: 600px;
  height: 400px;
  border-radius: 27px;
  background-color: rgba(227,196,165,0.7);
  background-size: cover;
  background-repeat: no-repeat;
  background-position: 0px 0px;
  display: flex;
  align-items: center;
  overflow: hidden;
}

.user-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 35%;
  height: 100%;
  box-sizing: border-box;
  padding: 20px;
  gap: 20px;
}

.user-panel .avatar {
  display: block;
  border-radius: 50%;
  width: 90px;
  height: 90px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

.login-title-link {
  color: inherit;
  text-decoration: none;
  cursor: pointer;
  transition: opacity 0.2s;
}

.login-title-link:hover {
  opacity: 0.8;
}

.divider {
  width: 1px;
  height: 70%;
  background-color: #e0e0e0;
}

.login-form-panel {
  width: 60%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100%;
}

.login-title {
  text-shadow: 8px 2px 3px rgba(254, 254, 254, 0.4);
  color: #333;
  font-size: 50px;
  font-weight: 600;
  margin: 0 auto 25px;
  display: flex;
  justify-content: center;
}

.form-input {
  width: 250px;
  height: 33px;
  border-radius: 10px;
  border: none;
  background-color: rgba(255, 255, 255, 0.5);
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
  letter-spacing: 0.5px;
  line-height: 1.5;
  padding: 0 5px;
  font-size: 16px;
  outline: none;
}

.password-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.password-wrapper .form-input {
  padding-right: 35px;
}

.toggle-password {
  position: absolute;
  right: 8px;
  cursor: pointer;
  font-size: 18px;
  user-select: none;
}

.form-group {
  height: 50px;
  font-size: 20px;
  display: flex;
  align-items: center;
}

.form-group .form-label {
  width: 70px;
  text-align: right;
  margin-right: 10px;
  font-size: 20px;
  font-family: cursive;
}

.remember-group {
  height: 30px;
  padding-left: 80px;
  display: flex;
  align-items: center;
}

.remember-label {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 6px;
}

.remember-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #007BFF;
}

.remember-text {
  font-size: 14px;
  font-family: cursive;
  color: #555;
  user-select: none;
}

.submit-group {
  padding-left: 80px;
}

.login-button {
  display: flex;
  width: 200px;
  height: 43px;
  font-size: 25px;
  padding: 5px;
  border-radius: 5px;
  background-color: #007BFF;
  color: white;
  border: none;
  cursor: pointer;
  justify-content: center;
  align-items: center;
  font-family: cursive;
}

.login-button:hover {
  background-color: #0056b3;
}

.login-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.form-footer {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.admin-quick-entry {
  text-align: left;
}

.admin-tag {
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.admin-tag:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(230, 162, 60, 0.3);
}

.register-link {
  font-size: 13px;
  text-align: right;
}

.register-link .register-text {
  color: #55aaff;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.2s;
}

.register-link .register-text:hover {
  color: #007BFF;
  text-decoration: underline;
}

.dlzc_ditu_div {
  position: fixed;
  z-index: 50;
  bottom: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: 60px;
  background: url('/img/dt_1.jpg') no-repeat 0px -360px / cover;
}
</style>
