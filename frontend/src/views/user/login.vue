<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 16, message: '用户名长度在 4 到 16 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login({
          username: form.username,
          password: form.password
        })
        ElMessage.success('登录成功')
        const redirect = route.query.redirect as string
        router.push(redirect || '/')
      } catch (error) {
        ElMessage.error('登录失败，请检查用户名和密码')
      } finally {
        loading.value = false
      }
    }
  })
}

const goToRegister = () => {
  router.push('/user/register')
}
</script>

<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <h1 class="login-title">用户登录</h1>
          <p class="login-subtitle">欢迎回到古今诗话</p>
        </div>
        
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="login-form"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
            />
          </el-form-item>
          
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          
          <div class="login-options">
            <el-checkbox>记住密码</el-checkbox>
            <a href="#" class="forgot-password">忘记密码？</a>
          </div>
          
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-button"
              :loading="loading"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="login-footer">
          <p class="register-link">
            还没有账号？
            <a @click="goToRegister">立即注册</a>
          </p>
          
          <div class="third-party-login">
            <p class="third-party-title">其他登录方式</p>
            <div class="third-party-icons">
              <a href="#" class="third-party-icon">
                <img src="/images/wechat.png" alt="微信登录" />
              </a>
              <a href="#" class="third-party-icon">
                <img src="/images/qq.png" alt="QQ登录" />
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, $primary-color 0%, $secondary-color 100%);
  padding: $spacing-lg;
}

.login-container {
  width: 100%;
  max-width: 400px;
}

.login-card {
  background: $background-color-light;
  border-radius: $border-radius-lg;
  box-shadow: $box-shadow-xl;
  padding: $spacing-xxl;
}

.login-header {
  text-align: center;
  margin-bottom: $spacing-xl;
}

.login-title {
  font-size: $font-size-title;
  color: $primary-color;
  font-family: $font-family-title;
  margin-bottom: $spacing-sm;
}

.login-subtitle {
  font-size: $font-size-base;
  color: $text-color-secondary;
}

.login-form {
  margin-bottom: $spacing-lg;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-lg;
}

.forgot-password {
  font-size: $font-size-sm;
  color: $primary-color;
  
  &:hover {
    color: darken($primary-color, 10%);
  }
}

.login-button {
  width: 100%;
}

.login-footer {
  text-align: center;
}

.register-link {
  font-size: $font-size-base;
  color: $text-color-secondary;
  margin-bottom: $spacing-lg;
  
  a {
    color: $primary-color;
    font-weight: 600;
    cursor: pointer;
    
    &:hover {
      color: darken($primary-color, 10%);
    }
  }
}

.third-party-title {
  font-size: $font-size-sm;
  color: $text-color-light;
  margin-bottom: $spacing-md;
  position: relative;
  
  &::before,
  &::after {
    content: '';
    position: absolute;
    top: 50%;
    width: 30%;
    height: 1px;
    background-color: $border-color;
  }
  
  &::before {
    left: 0;
  }
  
  &::after {
    right: 0;
  }
}

.third-party-icons {
  display: flex;
  justify-content: center;
  gap: $spacing-lg;
}

.third-party-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  transition: transform $transition-fast;
  
  &:hover {
    transform: scale(1.1);
  }
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}
</style>