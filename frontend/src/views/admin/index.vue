<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const adminMenus = [
  { path: '/admin/dashboard', name: 'dashboard', icon: 'DataAnalysis', label: '控制台' },
  {
    label: '用户管理',
    icon: 'User',
    children: [
      { path: '/admin/users', name: 'AdminUsers', icon: 'User', label: '用户管理' }
    ]
  },
  {
    label: '内容管理',
    icon: 'Document',
    children: [
      { path: '/admin/poems', name: 'AdminPoems', icon: 'Reading', label: '诗词管理' },
      { path: '/admin/categories', name: 'AdminCategories', icon: 'FolderOpened', label: '分类管理' },
      { path: '/admin/dynasties', name: 'AdminDynasties', icon: 'Clock', label: '朝代管理' },
      { path: '/admin/poets', name: 'AdminPoets', icon: 'EditPen', label: '诗人管理' },
      { path: '/admin/poet-featured', name: 'AdminPoetFeatured', icon: 'Star', label: '精选诗人管理' },
      { path: '/admin/home-navigation', name: 'AdminHomeNavigation', icon: 'Guide', label: '首页导航管理' }
    ]
  },
  {
    label: '社区管理',
    icon: 'ChatLineSquare',
    children: [
      { path: '/admin/forum-posts', name: 'AdminForumPosts', icon: 'ChatLineSquare', label: '帖子管理' }
    ]
  },
  {
    label: 'AI功能',
    icon: 'Cpu',
    children: [
      { path: '/admin/ai-models', name: 'AdminAiModels', icon: 'Cpu', label: 'AI模型管理' }
    ]
  },
  {
    label: '审核管理',
    icon: 'Checked',
    children: [
      { path: '/admin/poem-audit', name: 'PoemAudit', icon: 'Reading', label: '诗词审核' },
      { path: '/admin/poet-profiles', name: 'PoetProfiles', icon: 'User', label: '诗人认证审核' },
      { path: '/admin/poet-suggestions', name: 'AdminPoetSuggestions', icon: 'Checked', label: '诗人内容审核' },
      { path: '/admin/ai-contents', name: 'AdminAiContents', icon: 'MagicStick', label: 'AI内容审核' }
    ]
  },
  {
    label: '评论管理',
    icon: 'ChatDotSquare',
    children: [
      { path: '/admin/comments', name: 'Comments', icon: 'ChatLineSquare', label: '评论管理' }
    ]
  },
  {
    label: '系统管理',
    icon: 'Setting',
    children: [
      { path: '/admin/logs', name: 'AdminLogs', icon: 'Document', label: '操作日志' },
      { path: '/admin/static-pages', name: 'AdminStaticPages', icon: 'Document', label: '静态页面管理' }
    ]
  }
]

const handleLogout = () => {
  userStore.logout()
  router.push('/')
}

const goHome = () => {
  router.push('/')
}
</script>

<template>
  <div class="admin-layout">
    <el-container style="height: 100vh">
      <el-aside width="220px" class="admin-sidebar">
        <div class="sidebar-header">
          <h2 class="sidebar-title">墨渊后台</h2>
        </div>
        <el-menu
          :default-active="route.path"
          router
          class="sidebar-menu"
          background-color="#001529"
          text-color="#ffffff99"
          active-text-color="#ffffff"
        >
          <template v-for="menu in adminMenus" :key="menu.path || menu.label">
            <el-menu-item v-if="!menu.children" :index="menu.path">
              <el-icon><component :is="menu.icon" /></el-icon>
              <span>{{ menu.label }}</span>
            </el-menu-item>
            <el-sub-menu v-else :index="menu.label">
              <template #title>
                <el-icon><component :is="menu.icon" /></el-icon>
                <span>{{ menu.label }}</span>
              </template>
              <el-menu-item
                v-for="child in menu.children"
                :key="child.path"
                :index="child.path"
              >
                <el-icon><component :is="child.icon" /></el-icon>
                <span>{{ child.label }}</span>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="admin-header">
          <div class="header-left">
            <span class="welcome-text">欢迎回来，{{ userStore.username }}</span>
          </div>
          <div class="header-right">
            <el-button text @click="goHome">
              <el-icon><HomeFilled /></el-icon>
              返回前台
            </el-button>
            <el-dropdown>
              <span class="user-info">
                <el-avatar :size="32" :src="userStore.avatar">
                  {{ userStore.username?.charAt(0)?.toUpperCase() }}
                </el-avatar>
                <span class="username">{{ userStore.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/user/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.userInfo?.role === 'admin'" @click="router.push('/admin/dashboard')">后台管理</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <el-main class="admin-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style scoped lang="scss">
.admin-layout {
  height: 100vh;
}

.admin-sidebar {
  background-color: #001529;
  overflow-y: auto;
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-title {
  color: #ffffff;
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.sidebar-menu {
  border-right: none;
  
  :deep(.el-sub-menu) {
    .el-sub-menu__title {
      height: 56px;
      line-height: 56px;
      
      &:hover {
        background-color: rgba(255, 255, 255, 0.1) !important;
      }
    }
    
    .el-menu {
      background-color: #000c17 !important;
      
      .el-menu-item {
        height: 50px;
        line-height: 50px;
        padding-left: 56px !important;
        
        &:hover {
          background-color: rgba(255, 255, 255, 0.1) !important;
        }
        
        &.is-active {
          background-color: #1890ff !important;
        }
      }
    }
  }
  
  :deep(.el-menu-item) {
    height: 56px;
    line-height: 56px;
    
    &:hover {
      background-color: rgba(255, 255, 255, 0.1) !important;
    }
    
    &.is-active {
      background-color: #1890ff !important;
    }
  }
}

.admin-header {
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
}

.welcome-text {
  font-size: 14px;
  color: #666;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #333;
}

.admin-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
