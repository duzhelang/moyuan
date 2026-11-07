<script setup lang="ts">
import { computed, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { adminMenus, reviewerMenus } from '@/configs/adminMenu'
import { useAdminTabs } from '@/composables/useAdminTabs'
import { useAdminTheme } from '@/composables/useAdminTheme'
import AdminBreadcrumb from '@/components/admin/AdminBreadcrumb.vue'
import TagsView from '@/components/admin/TagsView.vue'
import AdminThemeSwitch from '@/components/admin/AdminThemeSwitch.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { addTab, closeAll, tabs } = useAdminTabs()
const { activate, deactivate } = useAdminTheme()

/** 管理员看到全部后台菜单；审核员使用精简的审核工作台菜单 */
const isReviewer = computed(() => userStore.userInfo?.role === 'reviewer')
// 审核员菜单已按角色单独定义，过滤仅作为后备保护
const REVIEWER_ALLOWED = new Set([
  '/admin/audit-pipeline',
  '/admin/audit-records',
  '/admin/poem-audit',
  '/admin/poet-profiles',
  '/admin/poet-suggestions',
  '/admin/ai-contents',
  '/admin/comments',
  '/admin/forum-posts',
  '/admin/reports'
])

const visibleMenus = computed(() => {
  if (userStore.userInfo?.role !== 'reviewer') {
    return adminMenus
  }
  return reviewerMenus
    .map((group) => ({
      ...group,
      children: (group.children || []).filter((item) =>
        item.path ? REVIEWER_ALLOWED.has(item.path) : false
      )
    }))
    .filter((group) => group.children.length > 0)
})

// 在父级 setup 阶段立即生效，保证子页面读取 CSS 变量时主题已就绪
activate()

onUnmounted(() => {
  deactivate()
})

// 每次进入后台页面都登记标签；同一页面只换 query 时也要更新定位，所以监听 fullPath
watch(
  () => route.fullPath,
  () => addTab(route),
  { immediate: true }
)

const handleLogout = () => {
  closeAll()
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
          <h2 class="sidebar-title">{{ isReviewer ? '墨渊 · 审核工作台' : '墨渊后台' }}</h2>
        </div>
        <el-menu
          :default-active="route.path"
          router
          class="sidebar-menu"
        >
          <template v-for="menu in visibleMenus" :key="menu.label">
            <el-menu-item v-if="!menu.children" :index="menu.path || ''">
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
            <AdminThemeSwitch />
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

        <AdminBreadcrumb />
        <!-- 只有控制台一个标签时不显示标签栏，避免首屏多占一行 -->
        <TagsView v-if="tabs.length > 1" />

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
  background: var(--adm-bg);
}

.admin-sidebar {
  background-color: var(--adm-sidebar-bg);
  overflow-y: auto;
  transition: background-color 0.25s;
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--adm-border-soft);
}

.sidebar-title {
  color: var(--adm-sidebar-text);
  font-family: var(--adm-font-serif);
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 2px;
  margin: 0;
}

.sidebar-menu {
  border-right: none;
  background-color: transparent;

  :deep(.el-sub-menu) {
    .el-sub-menu__title {
      height: 56px;
      line-height: 56px;
      color: var(--adm-sidebar-text);

      &:hover {
        background-color: var(--adm-sidebar-active-bg) !important;
      }
    }

    .el-menu {
      background-color: var(--adm-sidebar-bg) !important;

      .el-menu-item {
        height: 50px;
        line-height: 50px;
        padding-left: 56px !important;
        color: var(--adm-sidebar-text);

        &:hover {
          background-color: var(--adm-sidebar-active-bg) !important;
        }

        &.is-active {
          background-color: var(--adm-sidebar-active-bg) !important;
          color: var(--adm-primary) !important;
        }
      }
    }
  }

  :deep(.el-menu-item) {
    height: 56px;
    line-height: 56px;
    color: var(--adm-sidebar-text);

    &:hover {
      background-color: var(--adm-sidebar-active-bg) !important;
    }

    &.is-active {
      background-color: var(--adm-sidebar-active-bg) !important;
      color: var(--adm-primary) !important;
    }
  }
}

.admin-header {
  background-color: var(--adm-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid var(--adm-border-soft);
  box-shadow: none;
  z-index: 10;
  transition: background-color 0.25s;
}

.header-left {
  display: flex;
  align-items: center;
}

.welcome-text {
  font-size: 14px;
  color: var(--adm-text-muted);
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
  color: var(--adm-text-2);
}

.admin-main {
  background-color: var(--adm-bg);
  background-image: var(--adm-page-gradient);
  padding: 20px;
  overflow-y: auto;
  transition: background-color 0.25s;
}
</style>
