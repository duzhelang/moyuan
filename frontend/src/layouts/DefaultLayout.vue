<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const searchKeyword = ref('')

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/poem', query: { keyword: searchKeyword.value } })
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/')
}
</script>

<template>
  <div class="default-layout">
    <header class="layout-header">
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
            placeholder="搜索诗词..."
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
    
    <main class="layout-main">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <keep-alive :include="appStore.cachedViews">
            <component :is="Component" />
          </keep-alive>
        </transition>
      </router-view>
    </main>
    
    <footer class="layout-footer">
      <div class="footer-container">
        <div class="footer-links">
          <h4>联系我们</h4>
          <ul>
            <li>事务联络</li>
            <li>媒体采访</li>
            <li>人才应聘</li>
          </ul>
        </div>
        
        <div class="footer-links">
          <h4>友情链接</h4>
          <ul>
            <li>教育部</li>
            <li>江苏省教育厅</li>
            <li>中国传媒大学</li>
          </ul>
        </div>
        
        <div class="footer-contact">
          <h4>联系方式</h4>
          <p>电话：(025)86179990</p>
          <p>地址：江苏省南京市江宁区弘景大道3666号</p>
          <p>邮编：211172</p>
        </div>
      </div>
      
      <div class="footer-copyright">
        <p>&copy; {{ new Date().getFullYear() }} 古今诗话——墨渊 版权所有</p>
      </div>
    </footer>
  </div>
</template>

<style scoped lang="scss">
.default-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-header {
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

.layout-main {
  flex: 1;
  padding: $spacing-xl 0;
}

.layout-footer {
  background-color: $background-color-dark;
  color: #fff;
  padding: $spacing-xl 0 $spacing-lg;
}

.footer-container {
  @include container;
  display: flex;
  gap: $spacing-xxl;
  margin-bottom: $spacing-xl;
}

.footer-links {
  h4 {
    font-size: $font-size-lg;
    margin-bottom: $spacing-md;
    color: #fff;
  }
  
  ul {
    li {
      margin-bottom: $spacing-sm;
      color: rgba(255, 255, 255, 0.7);
      font-size: $font-size-sm;
    }
  }
}

.footer-contact {
  h4 {
    font-size: $font-size-lg;
    margin-bottom: $spacing-md;
    color: #fff;
  }
  
  p {
    margin-bottom: $spacing-sm;
    color: rgba(255, 255, 255, 0.7);
    font-size: $font-size-sm;
  }
}

.footer-copyright {
  @include container;
  text-align: center;
  padding-top: $spacing-lg;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  
  p {
    color: rgba(255, 255, 255, 0.5);
    font-size: $font-size-sm;
  }
}
</style>