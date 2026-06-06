<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadProps, UploadFile } from 'element-plus'
import { getMyFavorites, favoritePoem } from '@/api/modules/poem'
import { getMyPosts, getUserStats } from '@/api/modules/user'
import { getHistory, clearHistory } from '@/api/modules/history'
import type { Poem, ForumPost } from '@/types/model'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const activeTab = ref('profile')
const avatarUploading = ref(false)
const showPasswordDialog = ref(false)
const showDeviceDialog = ref(false)
const showSecurityLogDialog = ref(false)

const userStats = ref({
  favoriteCount: 0,
  postCount: 0,
  historyCount: 0,
  likeCount: 0
})

const settings = reactive({
  systemNotification: true,
  businessNotification: true,
  emailNotification: false,
  smsNotification: false,
  theme: 'light',
  sidebarMode: 'expanded',
  fontSize: 14,
  language: 'zh'
})

const privacy = reactive({
  modelOptimization: true,
  statisticalAnalysis: true
})

const mockDevices = ref([
  { id: 1, name: 'Windows PC - Chrome', ip: '192.168.1.100', location: '上海市', lastLogin: '2024-01-15 10:30:00' },
  { id: 2, name: 'iPhone 15 - Safari', ip: '192.168.1.101', location: '上海市', lastLogin: '2024-01-14 18:45:00' }
])

const mockSecurityLogs = ref([
  { id: 1, content: '密码修改成功', time: '2024-01-15 10:30:00' },
  { id: 2, content: '登录成功', time: '2024-01-14 18:45:00' },
  { id: 3, content: '登录成功', time: '2024-01-13 09:15:00' }
])

const uploadHeaders = computed(() => ({
  Authorization: userStore.token ? `Bearer ${userStore.token}` : ''
}))

const uploadData = computed(() => {
  const data: Record<string, any> = { fileType: 'avatar' }
  if (userStore.userInfo?.id) {
    data.relatedId = userStore.userInfo.id
  }
  return data
})

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

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
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

const fetchUserStats = async () => {
  try {
    const res = await getUserStats()
    userStats.value = res.data
  } catch (error) {
    console.error('获取用户统计信息失败')
  }
}

const handleAvatarSuccess: UploadProps['onSuccess'] = async (response) => {
  try {
    if (response.code === 200) {
      await userStore.updateUser({ avatar: response.data.url })
      ElMessage.success('头像更新成功')
    } else {
      ElMessage.error(response.message || '头像上传失败')
    }
  } catch (error) {
    ElMessage.error('头像更新失败')
  } finally {
    avatarUploading.value = false
  }
}

const handleAvatarError = () => {
  ElMessage.error('头像上传失败，请检查网络后重试')
  avatarUploading.value = false
}

const beforeAvatarUpload = (file: UploadFile) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size! / 1024 / 1024 < 2

  if (!isJpgOrPng) {
    ElMessage.error('头像只能是 JPG/PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB!')
    return false
  }
  avatarUploading.value = true
  return true
}

const handleResetProfile = () => {
  ElMessageBox.confirm('确定要重置所有修改吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    initForm()
    ElMessage.success('已重置')
  }).catch(() => {})
}

const handleSaveSettings = () => {
  ElMessage.success('设置已保存')
}

const handleResetSettings = () => {
  ElMessageBox.confirm('确定要重置为默认设置吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    settings.systemNotification = true
    settings.businessNotification = true
    settings.emailNotification = false
    settings.smsNotification = false
    settings.theme = 'light'
    settings.sidebarMode = 'expanded'
    settings.fontSize = 14
    settings.language = 'zh'
    ElMessage.success('已重置为默认设置')
  }).catch(() => {})
}

const handleExportData = (format: 'pdf' | 'excel') => {
  ElMessage.success(`正在导出 ${format.toUpperCase()} 文件...`)
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

const favorites = ref<Poem[]>([])
const favoritesTotal = ref(0)
const favoritesPage = ref(1)
const favoritesSize = ref(10)
const favoritesLoading = ref(false)

const fetchFavorites = async () => {
  favoritesLoading.value = true
  try {
    const res = await getMyFavorites({
      pageNum: favoritesPage.value,
      pageSize: favoritesSize.value
    })
    favorites.value = res.data.list
    favoritesTotal.value = res.data.total
  } catch (error) {
    console.error('获取收藏列表失败')
  } finally {
    favoritesLoading.value = false
  }
}

const handleCancelFavorite = async (poem: Poem) => {
  try {
    await favoritePoem(poem.id)
    ElMessage.success('已取消收藏')
    fetchFavorites()
  } catch (error) {
    ElMessage.error('取消收藏失败')
  }
}

const handleFavoritesPageChange = (page: number) => {
  favoritesPage.value = page
  fetchFavorites()
}

const myPosts = ref<ForumPost[]>([])
const myPostsTotal = ref(0)
const myPostsPage = ref(1)
const myPostsSize = ref(10)
const myPostsLoading = ref(false)

const fetchMyPosts = async () => {
  myPostsLoading.value = true
  try {
    const res = await getMyPosts({
      pageNum: myPostsPage.value,
      pageSize: myPostsSize.value
    })
    myPosts.value = res.data.list
    myPostsTotal.value = res.data.total
  } catch (error) {
    console.error('获取帖子列表失败')
  } finally {
    myPostsLoading.value = false
  }
}

const handleMyPostsPageChange = (page: number) => {
  myPostsPage.value = page
  fetchMyPosts()
}

const historyList = ref<any[]>([])
const historyTotal = ref(0)
const historyPage = ref(1)
const historySize = ref(20)
const historyLoading = ref(false)

const fetchHistory = async () => {
  historyLoading.value = true
  try {
    const res = await getHistory({ pageNum: historyPage.value, pageSize: historySize.value })
    historyList.value = res.data.list
    historyTotal.value = res.data.total
  } catch (error) {
    console.error('获取浏览历史失败')
  } finally {
    historyLoading.value = false
  }
}

const handleClearHistory = async () => {
  try {
    await clearHistory()
    ElMessage.success('已清空浏览历史')
    historyList.value = []
    historyTotal.value = 0
  } catch (error) {
    ElMessage.error('清空失败')
  }
}

const handleHistoryPageChange = (page: number) => {
  historyPage.value = page
  fetchHistory()
}

watch(activeTab, (val) => {
  if (val === 'favorites') fetchFavorites()
  if (val === 'posts') fetchMyPosts()
  if (val === 'history') fetchHistory()
})

const handleTabChange = (key: string) => {
  activeTab.value = key
}

onMounted(() => {
  initForm()
  fetchUserStats()
  
  const tab = router.currentRoute.value.query.tab as string
  if (tab && ['profile', 'security', 'favorites', 'posts', 'history', 'settings', 'privacy', 'admin'].includes(tab)) {
    activeTab.value = tab
  }
})
</script>

<template>
  <div class="profile-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">个人中心</h1>
        <div class="page-nav-buttons">
          <el-button @click="router.push('/')">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-button>
          <el-button @click="router.push('/poems')">
            <el-icon><Reading /></el-icon>
            <span>诗词库</span>
          </el-button>
          <el-button @click="router.push('/forum')">
            <el-icon><ChatDotSquare /></el-icon>
            <span>论坛</span>
          </el-button>
          <el-button @click="router.push('/ai-chat')">
            <el-icon><ChatLineSquare /></el-icon>
            <span>AI对话</span>
          </el-button>
        </div>
      </div>
      
      <div class="profile-content">
        <div class="profile-sidebar">
          <div class="user-card">
            <el-upload
              class="avatar-uploader"
              action="/api/files/upload"
              :headers="uploadHeaders"
              :data="uploadData"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :on-error="handleAvatarError"
              :before-upload="beforeAvatarUpload"
              accept="image/jpeg,image/png"
            >
              <el-avatar :src="userStore.avatar" :size="100" class="user-avatar-large" v-loading="avatarUploading">
                {{ userStore.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <div class="avatar-overlay">
                <el-icon><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </el-upload>
            <h3 class="username">{{ userStore.userInfo?.nickname || userStore.username }}</h3>
            <p class="user-role">
              <el-tag :type="userStore.userInfo?.role === 'admin' ? 'danger' : 'primary'" size="small">
                {{ userStore.userInfo?.role === 'admin' ? '管理员' : '普通用户' }}
              </el-tag>
            </p>
            <p class="user-bio">{{ form.bio || '这个人很懒，什么都没留下' }}</p>

            <el-button
              v-if="userStore.userInfo?.role === 'admin'"
              type="danger"
              class="admin-entry-btn"
              @click="router.push('/admin')"
            >
              <el-icon><Setting /></el-icon>
              <span>进入后台管理</span>
            </el-button>

            <div class="user-stats">
              <div class="stat-item" @click="activeTab = 'favorites'">
                <span class="stat-number">{{ userStats.favoriteCount }}</span>
                <span class="stat-label">收藏</span>
              </div>
              <div class="stat-item" @click="activeTab = 'posts'">
                <span class="stat-number">{{ userStats.postCount }}</span>
                <span class="stat-label">帖子</span>
              </div>
              <div class="stat-item" @click="activeTab = 'history'">
                <span class="stat-number">{{ userStats.historyCount }}</span>
                <span class="stat-label">浏览</span>
              </div>
            </div>
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
            <el-menu-item index="security">
              <el-icon><Lock /></el-icon>
              <span>账号安全</span>
            </el-menu-item>
            <el-menu-item index="favorites">
              <el-icon><Star /></el-icon>
              <span>我的收藏</span>
            </el-menu-item>
            <el-menu-item index="posts">
              <el-icon><Document /></el-icon>
              <span>我的帖子</span>
            </el-menu-item>
            <el-menu-item index="history">
              <el-icon><Clock /></el-icon>
              <span>浏览历史</span>
            </el-menu-item>
            <el-menu-item index="settings">
              <el-icon><Setting /></el-icon>
              <span>系统偏好</span>
            </el-menu-item>
            <el-menu-item index="privacy">
              <el-icon><Key /></el-icon>
              <span>数据隐私</span>
            </el-menu-item>
            <el-menu-item index="admin" v-if="userStore.userInfo?.role === 'admin'">
              <el-icon><Tools /></el-icon>
              <span>系统管理</span>
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
              label-width="80px"
              class="profile-form"
            >
              <el-form-item label="用户名">
                <el-input :value="userStore.username" disabled />
              </el-form-item>
              
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
                  <el-radio :value="0">未知</el-radio>
                  <el-radio :value="1">男</el-radio>
                  <el-radio :value="2">女</el-radio>
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
              
              <el-form-item label="注册时间">
                <el-input :value="userStore.userInfo?.createTime" disabled />
              </el-form-item>
              
              <el-form-item>
                <el-button
                  type="primary"
                  :loading="loading"
                  @click="handleUpdateProfile"
                >
                  保存修改
                </el-button>
                <el-button @click="handleResetProfile">重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>
          
          <el-card v-if="activeTab === 'security'" class="profile-card">
            <template #header>
              <h3>账号安全中心</h3>
            </template>
            
            <div class="security-section">
              <div class="security-item">
                <div class="security-info">
                  <h4>登录密码</h4>
                  <p>定期修改密码可以保护账号安全</p>
                </div>
                <el-button type="primary" @click="showPasswordDialog = true">修改密码</el-button>
              </div>
              
              <div class="security-item">
                <div class="security-info">
                  <h4>绑定手机号</h4>
                  <p>已绑定：{{ userStore.userInfo?.phone || '未绑定' }}</p>
                </div>
                <el-button disabled>开发中</el-button>
              </div>
              
              <div class="security-item">
                <div class="security-info">
                  <h4>绑定邮箱</h4>
                  <p>已绑定：{{ userStore.userInfo?.email || '未绑定' }}</p>
                </div>
                <el-button disabled>开发中</el-button>
              </div>
              
              <div class="security-item">
                <div class="security-info">
                  <h4>登录设备管理</h4>
                  <p>查看和管理登录过的设备</p>
                </div>
                <el-button @click="showDeviceDialog = true">查看设备</el-button>
              </div>
              
              <div class="security-item">
                <div class="security-info">
                  <h4>安全日志</h4>
                  <p>查看账号安全相关操作记录</p>
                </div>
                <el-button @click="showSecurityLogDialog = true">查看日志</el-button>
              </div>
            </div>
          </el-card>
          
          <el-card v-if="activeTab === 'favorites'" class="profile-card">
            <template #header>
              <h3>我的收藏</h3>
            </template>

            <div v-loading="favoritesLoading" class="favorites-list">
              <el-empty v-if="!favoritesLoading && favorites.length === 0" description="暂无收藏" />

              <div
                v-for="poem in favorites"
                :key="poem.id"
                class="favorite-card"
                @click="router.push(`/poem/${poem.id}`)"
              >
                <div class="favorite-header">
                  <h4 class="favorite-title">{{ poem.title }}</h4>
                  <el-button
                    type="danger"
                    size="small"
                    plain
                    @click.stop="handleCancelFavorite(poem)"
                  >
                    取消收藏
                  </el-button>
                </div>
                <div class="favorite-meta">
                  <span v-if="poem.poetName">{{ poem.poetName }}</span>
                  <span v-if="poem.dynastyName">{{ poem.dynastyName }}</span>
                </div>
                <p v-if="poem.content" class="favorite-content">{{ poem.content }}</p>
              </div>
            </div>

            <div v-if="favoritesTotal > 0" class="pagination-section">
              <el-pagination
                v-model:current-page="favoritesPage"
                :page-size="favoritesSize"
                :total="favoritesTotal"
                layout="prev, pager, next"
                @current-change="handleFavoritesPageChange"
              />
            </div>
          </el-card>

          <el-card v-if="activeTab === 'posts'" class="profile-card">
            <template #header>
              <h3>我的帖子</h3>
            </template>

            <div v-loading="myPostsLoading" class="posts-list">
              <el-empty v-if="!myPostsLoading && myPosts.length === 0" description="暂无帖子" />

              <div
                v-for="post in myPosts"
                :key="post.id"
                class="post-card"
                @click="router.push(`/forum/${post.id}`)"
              >
                <h4 class="post-title">{{ post.title }}</h4>
                <p class="post-content">{{ post.content }}</p>
                <div class="post-footer">
                  <span class="meta-item">
                    <el-icon><View /></el-icon>
                    {{ post.viewCount }}
                  </span>
                  <span class="meta-item">
                    <el-icon><Star /></el-icon>
                    {{ post.likeCount }}
                  </span>
                  <span class="meta-item">
                    <el-icon><ChatDotRound /></el-icon>
                    {{ post.commentCount }}
                  </span>
                  <span class="meta-item post-time">{{ post.createTime }}</span>
                </div>
              </div>
            </div>

            <div v-if="myPostsTotal > 0" class="pagination-section">
              <el-pagination
                v-model:current-page="myPostsPage"
                :page-size="myPostsSize"
                :total="myPostsTotal"
                layout="prev, pager, next"
                @current-change="handleMyPostsPageChange"
              />
            </div>
          </el-card>

          <el-card v-if="activeTab === 'history'" class="profile-card">
            <template #header>
              <div class="card-header">
                <h3>浏览历史</h3>
                <el-button type="danger" text @click="handleClearHistory">清空历史</el-button>
              </div>
            </template>
            <div v-loading="historyLoading">
              <div v-if="historyList.length" class="history-list">
                <div
                  v-for="item in historyList"
                  :key="item.id"
                  class="history-item"
                  @click="router.push(item.targetType === 1 ? `/poem/${item.targetId}` : `/forum/${item.targetId}`)"
                >
                  <div class="history-info">
                    <el-tag :type="item.targetType === 1 ? 'primary' : 'success'" size="small">
                      {{ item.targetType === 1 ? '诗词' : '帖子' }}
                    </el-tag>
                    <span class="history-id">ID: {{ item.targetId }}</span>
                  </div>
                  <span class="history-time">{{ item.createdAt }}</span>
                </div>
              </div>
              <el-empty v-else description="暂无浏览记录" />
              <div class="pagination-section" v-if="historyTotal > historySize">
                <el-pagination
                  background
                  layout="prev, pager, next"
                  :total="historyTotal"
                  :page-size="historySize"
                  :current-page="historyPage"
                  @current-change="handleHistoryPageChange"
                />
              </div>
            </div>
          </el-card>

          <el-card v-if="activeTab === 'settings'" class="profile-card">
            <template #header>
              <h3>系统偏好设置</h3>
            </template>
            
            <div class="settings-section">
              <div class="settings-group">
                <h4>消息通知设置</h4>
                <div class="setting-item">
                  <span>系统通知</span>
                  <el-switch v-model="settings.systemNotification" />
                </div>
                <div class="setting-item">
                  <span>业务通知</span>
                  <el-switch v-model="settings.businessNotification" />
                </div>
                <div class="setting-item">
                  <span>邮件通知</span>
                  <el-switch v-model="settings.emailNotification" />
                </div>
                <div class="setting-item">
                  <span>短信通知</span>
                  <el-switch v-model="settings.smsNotification" />
                </div>
              </div>
              
              <div class="settings-group">
                <h4>界面主题设置</h4>
                <div class="setting-item">
                  <span>主题模式</span>
                  <el-radio-group v-model="settings.theme">
                    <el-radio-button label="light">浅色</el-radio-button>
                    <el-radio-button label="dark">深色</el-radio-button>
                    <el-radio-button label="system">跟随系统</el-radio-button>
                  </el-radio-group>
                </div>
                <div class="setting-item">
                  <span>侧边栏模式</span>
                  <el-radio-group v-model="settings.sidebarMode">
                    <el-radio-button label="expanded">展开</el-radio-button>
                    <el-radio-button label="collapsed">收起</el-radio-button>
                  </el-radio-group>
                </div>
                <div class="setting-item">
                  <span>字体大小</span>
                  <el-slider v-model="settings.fontSize" :min="12" :max="20" :step="1" show-input />
                </div>
              </div>
              
              <div class="settings-group">
                <h4>语言设置</h4>
                <div class="setting-item">
                  <span>界面语言</span>
                  <el-select v-model="settings.language" placeholder="选择语言">
                    <el-option label="中文" value="zh" />
                    <el-option label="English" value="en" />
                  </el-select>
                </div>
              </div>
              
              <div class="settings-actions">
                <el-button type="primary" @click="handleSaveSettings">保存设置</el-button>
                <el-button @click="handleResetSettings">重置默认</el-button>
              </div>
            </div>
          </el-card>

          <el-card v-if="activeTab === 'privacy'" class="profile-card">
            <template #header>
              <h3>数据与隐私管理</h3>
            </template>
            
            <div class="privacy-section">
              <div class="privacy-item">
                <div class="privacy-info">
                  <h4>个人数据导出</h4>
                  <p>导出您的个人数据，包括收藏、帖子、浏览记录等</p>
                </div>
                <div class="privacy-actions">
                  <el-button @click="handleExportData('pdf')">导出 PDF</el-button>
                  <el-button @click="handleExportData('excel')">导出 Excel</el-button>
                </div>
              </div>
              
              <div class="privacy-item">
                <div class="privacy-info">
                  <h4>数据使用授权</h4>
                  <p>允许平台使用匿名数据改进服务</p>
                </div>
                <div class="privacy-actions">
                  <div class="setting-item">
                    <span>模型优化</span>
                    <el-switch v-model="privacy.modelOptimization" />
                  </div>
                  <div class="setting-item">
                    <span>统计分析</span>
                    <el-switch v-model="privacy.statisticalAnalysis" />
                  </div>
                </div>
              </div>
              
              <div class="privacy-item">
                <div class="privacy-info">
                  <h4>隐私政策</h4>
                  <p>查看平台隐私政策</p>
                </div>
                <el-button disabled>开发中</el-button>
              </div>
              
              <div class="privacy-item">
                <div class="privacy-info">
                  <h4>账号注销</h4>
                  <p>注销账号将删除所有数据，此操作不可恢复</p>
                </div>
                <el-button type="danger" disabled>暂未开放</el-button>
              </div>
            </div>
          </el-card>

          <el-card v-if="activeTab === 'admin' && userStore.userInfo?.role === 'admin'" class="profile-card">
            <template #header>
              <h3>系统快捷操作</h3>
            </template>
            
            <div class="admin-section">
              <div class="admin-item" @click="router.push('/admin/users')">
                <el-icon><User /></el-icon>
                <span>用户管理</span>
              </div>
              <div class="admin-item" @click="router.push('/admin/roles')">
                <el-icon><UserFilled /></el-icon>
                <span>角色管理</span>
              </div>
              <div class="admin-item" @click="router.push('/admin/menus')">
                <el-icon><Menu /></el-icon>
                <span>菜单管理</span>
              </div>
              <div class="admin-item" @click="router.push('/admin/models')">
                <el-icon><Cpu /></el-icon>
                <span>模型管理</span>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>
    
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="400px">
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
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
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleUpdatePassword">确定</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="showDeviceDialog" title="登录设备管理" width="600px">
      <div class="device-list">
        <div v-for="device in mockDevices" :key="device.id" class="device-item">
          <div class="device-info">
            <el-icon :size="24"><Monitor /></el-icon>
            <div>
              <h4>{{ device.name }}</h4>
              <p>{{ device.ip }} · {{ device.location }}</p>
              <p class="device-time">最后登录：{{ device.lastLogin }}</p>
            </div>
          </div>
          <el-button type="danger" text size="small">下线</el-button>
        </div>
      </div>
    </el-dialog>
    
    <el-dialog v-model="showSecurityLogDialog" title="安全日志" width="600px">
      <div class="security-log-list">
        <div v-for="log in mockSecurityLogs" :key="log.id" class="log-item">
          <div class="log-info">
            <el-icon :size="16"><InfoFilled /></el-icon>
            <span>{{ log.content }}</span>
          </div>
          <span class="log-time">{{ log.time }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.profile-page {
  padding: $spacing-xl $spacing-xl;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xl;
  
  @include responsive(sm) {
    flex-direction: column;
    gap: $spacing-md;
    align-items: flex-start;
  }
}

.page-title {
  font-size: $font-size-title;
  color: $primary-color;
  font-family: $font-family-title;
  margin: 0;
}

.page-nav-buttons {
  display: flex;
  gap: $spacing-sm;
  
  @include responsive(sm) {
    flex-wrap: wrap;
  }
  
  .el-button {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
  }
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

.user-avatar-large {
  border: 4px solid $primary-color;
  box-shadow: $box-shadow-lg;
  margin-bottom: $spacing-md;
}

.username {
  font-size: $font-size-xl;
  color: $text-color;
  margin-top: $spacing-md;
  margin-bottom: $spacing-xs;
  font-weight: 600;
}

.user-bio {
  font-size: $font-size-sm;
  color: $text-color-secondary;
  margin-bottom: $spacing-lg;
}

.admin-entry-btn {
  width: 100%;
  margin-bottom: $spacing-lg;
  font-weight: 500;

  .el-icon {
    margin-right: $spacing-xs;
  }
}

.user-stats {
  display: flex;
  justify-content: space-around;
  padding-top: $spacing-lg;
  border-top: 1px solid $border-color;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  padding: $spacing-sm;
  border-radius: $border-radius-md;
  transition: all $transition-base;
  
  &:hover {
    background-color: rgba($primary-color, 0.05);
  }
}

.stat-number {
  font-size: $font-size-xxl;
  font-weight: 600;
  color: $primary-color;
}

.stat-label {
  font-size: $font-size-xs;
  color: $text-color-secondary;
  margin-top: $spacing-xs;
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
  max-width: 100%;

  :deep(.el-form-item__content) {
    flex: 1;
    min-width: 0;
  }

  :deep(.el-input),
  :deep(.el-textarea),
  :deep(.el-date-editor) {
    width: 100% !important;
  }

  :deep(.el-input__inner),
  :deep(.el-textarea__inner) {
    font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
    letter-spacing: 0.5px;
    line-height: 1.5;
  }
}

.favorites-list,
.posts-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
  min-height: 200px;
}

.favorite-card {
  @include card;
  cursor: pointer;
}

.favorite-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-sm;
}

.favorite-title {
  font-size: $font-size-xl;
  color: $text-color;
}

.favorite-meta {
  display: flex;
  gap: $spacing-md;
  margin-bottom: $spacing-sm;

  span {
    font-size: $font-size-sm;
    color: $text-color-secondary;
  }
}

.favorite-content {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: $line-height-loose;
  @include text-clamp(2);
}

.post-card {
  @include card;
  cursor: pointer;
}

.post-title {
  font-size: $font-size-xl;
  color: $text-color;
  margin-bottom: $spacing-sm;
}

.post-content {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: $line-height-loose;
  @include text-clamp(2);
  margin-bottom: $spacing-md;
}

.post-footer {
  display: flex;
  gap: $spacing-lg;
  padding-top: $spacing-md;
  border-top: 1px solid $border-color;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.post-time {
  margin-left: auto;
}

.pagination-section {
  margin-top: $spacing-xl;
  display: flex;
  justify-content: center;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  h3 {
    margin: 0;
    font-size: $font-size-xl;
    color: $text-color;
  }
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md;
  border: 1px solid $border-color;
  border-radius: $border-radius-sm;
  cursor: pointer;
  transition: all $transition-base;

  &:hover {
    border-color: $primary-color;
    background-color: rgba($primary-color, 0.02);
  }
}

.history-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.history-id {
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.history-time {
  font-size: $font-size-sm;
  color: $text-color-light;
}

.user-role {
  margin: $spacing-xs 0;
}

.avatar-uploader {
  position: relative;
  display: inline-block;
  cursor: pointer;
  
  .avatar-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
    border-radius: 50%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity $transition-base;
    color: #fff;
    font-size: $font-size-sm;
    
    .el-icon {
      font-size: 20px;
      margin-bottom: $spacing-xs;
    }
  }
  
  &:hover .avatar-overlay {
    opacity: 1;
  }
}

.security-section {
  .security-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-lg 0;
    border-bottom: 1px solid $border-color;
    
    &:last-child {
      border-bottom: none;
    }
    
    .security-info {
      h4 {
        margin: 0 0 $spacing-xs;
        font-size: $font-size-base;
        color: $text-color;
      }
      
      p {
        margin: 0;
        font-size: $font-size-sm;
        color: $text-color-secondary;
      }
    }
  }
}

.settings-section {
  .settings-group {
    margin-bottom: $spacing-xl;
    
    h4 {
      font-size: $font-size-lg;
      color: $text-color;
      margin-bottom: $spacing-lg;
      padding-bottom: $spacing-sm;
      border-bottom: 1px solid $border-color;
    }
  }
  
  .setting-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-md 0;
    
    span {
      font-size: $font-size-base;
      color: $text-color;
    }
  }
  
  .settings-actions {
    margin-top: $spacing-xl;
    display: flex;
    gap: $spacing-md;
  }
}

.privacy-section {
  .privacy-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-lg 0;
    border-bottom: 1px solid $border-color;
    
    &:last-child {
      border-bottom: none;
    }
    
    .privacy-info {
      flex: 1;
      
      h4 {
        margin: 0 0 $spacing-xs;
        font-size: $font-size-base;
        color: $text-color;
      }
      
      p {
        margin: 0;
        font-size: $font-size-sm;
        color: $text-color-secondary;
      }
    }
    
    .privacy-actions {
      display: flex;
      gap: $spacing-md;
      align-items: center;
      
      .setting-item {
        display: flex;
        align-items: center;
        gap: $spacing-sm;
        
        span {
          font-size: $font-size-sm;
          color: $text-color-secondary;
        }
      }
    }
  }
}

.admin-section {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-lg;
  
  .admin-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: $spacing-xl;
    border: 1px solid $border-color;
    border-radius: $border-radius-md;
    cursor: pointer;
    transition: all $transition-base;
    
    &:hover {
      border-color: $primary-color;
      background-color: rgba($primary-color, 0.02);
    }
    
    .el-icon {
      font-size: 32px;
      color: $primary-color;
      margin-bottom: $spacing-md;
    }
    
    span {
      font-size: $font-size-base;
      color: $text-color;
    }
  }
}

.device-list {
  .device-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-lg 0;
    border-bottom: 1px solid $border-color;
    
    &:last-child {
      border-bottom: none;
    }
    
    .device-info {
      display: flex;
      align-items: center;
      gap: $spacing-md;
      
      .el-icon {
        color: $primary-color;
      }
      
      h4 {
        margin: 0 0 $spacing-xs;
        font-size: $font-size-base;
        color: $text-color;
      }
      
      p {
        margin: 0;
        font-size: $font-size-sm;
        color: $text-color-secondary;
      }
      
      .device-time {
        font-size: $font-size-xs;
        color: $text-color-light;
      }
    }
  }
}

.security-log-list {
  .log-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-md 0;
    border-bottom: 1px solid $border-color;
    
    &:last-child {
      border-bottom: none;
    }
    
    .log-info {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      
      .el-icon {
        color: $text-color-secondary;
      }
      
      span {
        font-size: $font-size-base;
        color: $text-color;
      }
    }
    
    .log-time {
      font-size: $font-size-sm;
      color: $text-color-secondary;
    }
  }
}
</style>