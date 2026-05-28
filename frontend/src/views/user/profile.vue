<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const activeTab = ref('profile')

const form = reactive({
  nickname: '',
  email: '',
  phone: '',
  gender: 0,
  birthday: '',
  bio: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordFormRef = ref<FormInstance>()

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const profileRules: FormRules = {
  nickname: [
    { max: 20, message: '昵称长度不能超过20个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
}

const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const initForm = () => {
  if (userStore.userInfo) {
    form.nickname = userStore.userInfo.nickname || ''
    form.email = userStore.userInfo.email || ''
    form.phone = userStore.userInfo.phone || ''
    form.gender = userStore.userInfo.gender || 0
    form.birthday = userStore.userInfo.birthday || ''
    form.bio = userStore.userInfo.bio || ''
  }
}

const handleUpdateProfile = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.updateUser({ ...form })
        ElMessage.success('更新成功')
      } catch (error) {
        ElMessage.error('更新失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const handleUpdatePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.updatePassword({ ...passwordForm })
        ElMessage.success('密码修改成功')
        passwordForm.oldPassword = ''
        passwordForm.newPassword = ''
        passwordForm.confirmPassword = ''
      } catch (error) {
        ElMessage.error('密码修改失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const handleLogout = () => {
  userStore.logout()
  router.push('/')
  ElMessage.success('已退出登录')
}

onMounted(() => {
  initForm()
})
</script>

<template>
  <div class="profile-page">
    <div class="container">
      <h1 class="page-title">个人中心</h1>
      
      <div class="profile-content">
        <div class="profile-sidebar">
          <div class="user-card">
            <el-avatar :src="userStore.avatar" :size="80">
              {{ userStore.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <h3 class="username">{{ userStore.username }}</h3>
            <p class="user-bio">{{ form.bio || '这个人很懒，什么都没留下' }}</p>
          </div>
          
          <el-menu
            :default-active="activeTab"
            class="profile-menu"
            @select="(key: string) => activeTab = key"
          >
            <el-menu-item index="profile">
              <el-icon><User /></el-icon>
              <span>个人信息</span>
            </el-menu-item>
            <el-menu-item index="password">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </el-menu-item>
            <el-menu-item index="favorites">
              <el-icon><Star /></el-icon>
              <span>我的收藏</span>
            </el-menu-item>
            <el-menu-item index="posts">
              <el-icon><Document /></el-icon>
              <span>我的帖子</span>
            </el-menu-item>
          </el-menu>
          
          <el-button
            type="danger"
            class="logout-button"
            @click="handleLogout"
          >
            退出登录
          </el-button>
        </div>
        
        <div class="profile-main">
          <el-card v-if="activeTab === 'profile'" class="profile-card">
            <template #header>
              <h3>个人信息</h3>
            </template>
            
            <el-form
              ref="formRef"
              :model="form"
              :rules="profileRules"
              label-width="100px"
              class="profile-form"
            >
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="form.nickname" placeholder="请输入昵称" />
              </el-form-item>
              
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" placeholder="请输入邮箱" />
              </el-form-item>
              
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="form.phone" placeholder="请输入手机号" />
              </el-form-item>
              
              <el-form-item label="性别">
                <el-radio-group v-model="form.gender">
                  <el-radio :label="0">未知</el-radio>
                  <el-radio :label="1">男</el-radio>
                  <el-radio :label="2">女</el-radio>
                </el-radio-group>
              </el-form-item>
              
              <el-form-item label="生日">
                <el-date-picker
                  v-model="form.birthday"
                  type="date"
                  placeholder="选择生日"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
              
              <el-form-item label="个人简介">
                <el-input
                  v-model="form.bio"
                  type="textarea"
                  :rows="4"
                  placeholder="介绍一下自己..."
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
              
              <el-form-item>
                <el-button
                  type="primary"
                  :loading="loading"
                  @click="handleUpdateProfile"
                >
                  保存修改
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
          
          <el-card v-if="activeTab === 'password'" class="profile-card">
            <template #header>
              <h3>修改密码</h3>
            </template>
            
            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="100px"
              class="password-form"
            >
              <el-form-item label="原密码" prop="oldPassword">
                <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  placeholder="请输入原密码"
                  show-password
                />
              </el-form-item>
              
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="请输入新密码"
                  show-password
                />
              </el-form-item>
              
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  show-password
                />
              </el-form-item>
              
              <el-form-item>
                <el-button
                  type="primary"
                  :loading="loading"
                  @click="handleUpdatePassword"
                >
                  修改密码
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
          
          <el-card v-if="activeTab === 'favorites'" class="profile-card">
            <template #header>
              <h3>我的收藏</h3>
            </template>
            <el-empty description="暂无收藏" />
          </el-card>
          
          <el-card v-if="activeTab === 'posts'" class="profile-card">
            <template #header>
              <h3>我的帖子</h3>
            </template>
            <el-empty description="暂无帖子" />
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.profile-page {
  padding: $spacing-xl 0;
}

.page-title {
  font-size: $font-size-title;
  color: $primary-color;
  font-family: $font-family-title;
  margin-bottom: $spacing-xl;
}

.profile-content {
  display: flex;
  gap: $spacing-xl;
  
  @include responsive(md) {
    flex-direction: column;
  }
}

.profile-sidebar {
  width: 280px;
  flex-shrink: 0;
  
  @include responsive(md) {
    width: 100%;
  }
}

.user-card {
  @include card;
  text-align: center;
  margin-bottom: $spacing-lg;
}

.username {
  font-size: $font-size-xl;
  color: $text-color;
  margin-top: $spacing-md;
  margin-bottom: $spacing-xs;
}

.user-bio {
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.profile-menu {
  border-right: none;
  margin-bottom: $spacing-lg;
}

.logout-button {
  width: 100%;
}

.profile-main {
  flex: 1;
}

.profile-card {
  border-radius: $border-radius-md;
}

.profile-form,
.password-form {
  max-width: 500px;
}
</style>