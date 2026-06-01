<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getMyFavorites, favoritePoem } from '@/api/modules/poem'
import { getMyPosts, getUserStats } from '@/api/modules/user'
import { getHistory, clearHistory } from '@/api/modules/history'
import type { Poem, ForumPost, UserHistory } from '@/types/model'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const activeTab = ref('profile')

const userStats = ref({
  favoriteCount: 0,
  postCount: 0,
  historyCount: 0,
  likeCount: 0
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

const fetchUserStats = async () => {
  try {
    const res = await getUserStats()
    userStats.value = res.data
  } catch (error) {
    console.error('获取用户统计信息失败')
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

onMounted(() => {
  initForm()
  fetchUserStats()
  
  const tab = router.currentRoute.value.query.tab as string
  if (tab && ['profile', 'password', 'favorites', 'posts', 'history'].includes(tab)) {
    activeTab.value = tab
  }
})
</script>

<template>
  <div class="profile-page">
    <div class="container">
      <h1 class="page-title">个人中心</h1>
      
      <div class="profile-content">
        <div class="profile-sidebar">
          <div class="user-card">
            <el-avatar :src="userStore.avatar" :size="100" class="user-avatar-large">
              {{ userStore.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <h3 class="username">{{ userStore.userInfo?.nickname || userStore.username }}</h3>
            <p class="user-bio">{{ form.bio || '这个人很懒，什么都没留下' }}</p>
            
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
            <el-menu-item index="history">
              <el-icon><Clock /></el-icon>
              <span>浏览历史</span>
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
  max-width: 500px;

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
</style>