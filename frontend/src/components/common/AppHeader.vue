<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const searchKeyword = ref('')

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/search', query: { keyword: searchKeyword.value } })
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/')
}
</script>

<template>
  <header class="app-header">
    <div class="header-container">
      <div class="header-logo">
        <router-link to="/">
          <h1>古今诗话</h1>
        </router-link>
      </div>
      
      <nav class="header-nav">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/poem" class="nav-link">诗词</router-link>
        <router-link to="/forum" class="nav-link">论坛</router-link>
      </nav>
      
      <div class="header-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索诗词、诗人、帖子..."
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      
      <div class="header-actions">
        <template v-if="userStore.isLoggedIn">
          <el-dropdown>
            <span class="user-info">
              <el-avatar :src="userStore.avatar" :size="32">
                {{ userStore.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span class="username">{{ userStore.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/user/profile')">
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item v-if="userStore.userInfo?.role === 'admin'" @click="router.push('/admin/dashboard')">
                  后台管理
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" @click="router.push('/user/login')">
            登录
          </el-button>
          <el-button @click="router.push('/user/register')">
            注册
          </el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped lang="scss">
.app-header {
  background-color: $background-color-light;
  box-shadow: $box-shadow;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-container {
  @include container;
  @include flex-between;
  height: 60px;
}

.header-logo {
  h1 {
    font-size: $font-size-title;
    color: $primary-color;
    font-family: $font-family-title;
  }
}

.header-nav {
  display: flex;
  gap: $spacing-lg;
}

.nav-link {
  color: $text-color;
  font-size: $font-size-lg;
  padding: $spacing-sm 0;
  position: relative;
  
  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 0;
    height: 2px;
    background-color: $primary-color;
    transition: width $transition-base;
  }
  
  &:hover,
  &.router-link-active {
    color: $primary-color;
    
    &::after {
      width: 100%;
    }
  }
}

.header-search {
  width: 240px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.user-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  cursor: pointer;
}

.username {
  font-size: $font-size-base;
  color: $text-color;
}
</style>