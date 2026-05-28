<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: ''
})

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 16, message: '用户名长度在 4 到 16 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login({
          username: form.username,
          password: form.password
        })
        ElMessage.success('注册成功')
        router.push('/')
      } catch (error) {
        ElMessage.error('注册失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
  })
}

const goToLogin = () => {
  router.push('/user/login')
}
</script>

<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-card">
        <div class="register-header">
          <h1 class="register-title">用户注册</h1>
          <p class="register-subtitle">加入古今诗话，感受诗词之美</p>
        </div>
        
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="register-form"
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
            />
          </el-form-item>
          
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>
          
          <el-form-item label="邮箱" prop="email">
            <el-input
              v-model="form.email"
              placeholder="请输入邮箱地址"
              prefix-icon="Message"
              size="large"
            />
          </el-form-item>
          
          <el-form-item label="手机号（选填）" prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号码"
              prefix-icon="Phone"
              size="large"
            />
          </el-form-item>
          
          <div class="register-agreement">
            <el-checkbox>
              我已阅读并同意
              <a href="#">《用户协议》</a>
              和
              <a href="#">《隐私政策》</a>
            </el-checkbox>
          </div>
          
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="register-button"
              :loading="loading"
              @click="handleRegister"
            >
              注册
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="register-footer">
          <p class="login-link">
            已有账号？
            <a @click="goToLogin">立即登录</a>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, $primary-color 0%, $secondary-color 100%);
  padding: $spacing-lg;
}

.register-container {
  width: 100%;
  max-width: 450px;
}

.register-card {
  background: $background-color-light;
  border-radius: $border-radius-lg;
  box-shadow: $box-shadow-xl;
  padding: $spacing-xxl;
}

.register-header {
  text-align: center;
  margin-bottom: $spacing-xl;
}

.register-title {
  font-size: $font-size-title;
  color: $primary-color;
  font-family: $font-family-title;
  margin-bottom: $spacing-sm;
}

.register-subtitle {
  font-size: $font-size-base;
  color: $text-color-secondary;
}

.register-form {
  margin-bottom: $spacing-lg;
}

.register-agreement {
  margin-bottom: $spacing-lg;
  
  a {
    color: $primary-color;
    
    &:hover {
      color: darken($primary-color, 10%);
    }
  }
}

.register-button {
  width: 100%;
}

.register-footer {
  text-align: center;
}

.login-link {
  font-size: $font-size-base;
  color: $text-color-secondary;
  
  a {
    color: $primary-color;
    font-weight: 600;
    cursor: pointer;
    
    &:hover {
      color: darken($primary-color, 10%);
    }
  }
}
</style>