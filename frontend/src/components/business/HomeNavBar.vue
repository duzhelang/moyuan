<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { HomeFilled, Document, UserFilled, Reading, ChatDotRound, Notebook, EditPen, Search } from '@element-plus/icons-vue'
import LoginDropdown from './LoginDropdown.vue'

const props = defineProps<{
  isNavFixed: boolean
}>()

const emit = defineEmits<{
  search: [payload: { query: string; type: 'internal' | 'external' }]
}>()

const router = useRouter()
const userStore = useUserStore()

const searchQuery = ref('')
const searchType = ref<'internal' | 'external'>('internal')
const searchInputRef = ref<HTMLInputElement | null>(null)

const handleSearch = () => {
  const q = searchQuery.value.trim()
  if (!q) return
  emit('search', { query: q, type: searchType.value })
}

defineExpose({ searchInputRef })
</script>

<template>
  <div>
    <div :class="['daohang', { fixed: isNavFixed }]">
      <ul>
        <li @click="router.push('/')">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </li>
        <li @click="router.push('/poem')">
          <el-icon><Document /></el-icon>
          <span>诗词库</span>
          <ul>
            <li @click.stop="router.push('/poem')">全部诗词</li>
            <li @click.stop="router.push({ path: '/poem', query: { tab: 'classical' } })">古体诗词</li>
            <li @click.stop="router.push({ path: '/poem', query: { tab: 'modern' } })">现代诗歌</li>
            <li @click.stop="router.push({ path: '/poem', query: { categoryId: 3 } })">外国诗歌</li>
          </ul>
        </li>
        <li @click="router.push('/poet')">
          <el-icon><UserFilled /></el-icon>
          <span>诗人馆</span>
          <ul>
            <li @click.stop="router.push('/poet')">诗人风采</li>
            <li @click.stop="router.push('/poet')">朝代浏览</li>
            <li @click.stop="router.push('/poet')">流派探索</li>
          </ul>
        </li>
        <li @click="router.push('/vision')">
          <el-icon><Reading /></el-icon>
          <span>诗话视野</span>
        </li>
        <li @click="router.push('/communicate')">
          <el-icon><ChatDotRound /></el-icon>
          <span>交流广场</span>
        </li>
        <li @click="router.push('/forum')">
          <el-icon><Notebook /></el-icon>
          <span>诗汇论坛</span>
        </li>
        <li
          v-if="userStore.isLoggedIn && userStore.userInfo"
          class="nav-publish-btn"
          @click="router.push('/poem/create')"
        >
          <el-icon><EditPen /></el-icon>
          <span>发布</span>
        </li>
        <LoginDropdown />
      </ul>
    </div>
    <div :class="['search-section', { fixed: isNavFixed }]">
      <div class="search-wrapper">
        <form class="search-form" @submit.prevent="handleSearch">
          <input
            ref="searchInputRef"
            type="text"
            :placeholder="searchType === 'internal' ? '搜索诗词、诗人、文章...' : '搜索全网诗词相关内容...'"
            v-model="searchQuery"
            @keydown.enter="handleSearch"
          >
        </form>
        <div class="search-tabs">
          <button
            :class="['search-tab', { active: searchType === 'internal' }]"
            @click="searchType = 'internal'"
          >
            站内
          </button>
          <button
            :class="['search-tab', { active: searchType === 'external' }]"
            @click="searchType = 'external'"
          >
            全网
          </button>
        </div>
        <button type="button" class="search-submit-btn" @click="handleSearch">
          <el-icon><Search /></el-icon>
          <span>搜索</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.daohang {
  width: 80%;
  height: 58px;
  margin: 0 auto;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  position: relative;
  z-index: 50;
}

.daohang ul {
  list-style: none;
  display: flex;
  font-weight: 600;
  background-color: rgba(100, 100, 100, 0.75);
  font-size: 15px;
  border-radius: 7px;
  margin: 0 auto;
  padding: 0;
}

.daohang > ul > li {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 160px;
  height: 38px;
  line-height: 38px;
  text-align: center;
  border: 1px solid gainsboro;
  border-radius: 5px;
  color: wheat;
  position: relative;
  cursor: pointer;
  list-style: none;
  transition: all 0.3s ease;
  font-size: 15px;
}

.daohang > ul > li:hover {
  color: yellow;
}

.daohang > ul > li > .el-icon {
  font-size: 18px;
}

.daohang > ul > li > ul {
  display: none;
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  z-index: 51;
  font-size: 14px;
  background: rgba(80, 80, 80, 0.85);
  min-width: 160px;
  border: 1px solid gainsboro;
  border-radius: 7px;
  padding: 2px 0;
  margin-top: -3px;
  list-style: none;
  height: auto;
}

.daohang > ul > li:hover > ul {
  display: block;
}

.daohang > ul > li > ul > li {
  width: 100%;
  height: 35px;
  line-height: 35px;
  text-align: center;
  color: wheat;
  cursor: pointer;
  border-bottom: 1px solid rgba(200, 200, 200, 0.2);
  list-style: none;
}

.daohang > ul > li > ul > li:last-child {
  border-bottom: none;
}

.daohang > ul > li > ul > li:hover {
  color: yellow;
  background-color: rgba(100, 100, 100, 0.5);
}

.nav-publish-btn {
  background: rgba(70, 130, 200, 0.6) !important;
  color: #fff !important;
}

.nav-publish-btn:hover {
  background: rgba(80, 145, 220, 0.8) !important;
}

.daohang.fixed {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  width: 100%;
  background: url('/img/dt_0.0.jpg');
  background-size: cover;
  background-repeat: no-repeat;
  background-position: 0px -320px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, .2);
  z-index: 61;
}

.search-section {
  width: 80%;
  margin: 0 auto;
  padding: 12px 0;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  z-index: 49;
}

.search-section.fixed {
  position: fixed;
  top: 50px;
  left: 0;
  right: 0;
  width: 100%;
  z-index: 50;
  background: rgba(245, 240, 232, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 8px 0;
}

.search-wrapper {
  display: flex;
  align-items: stretch;
  gap: 0;
  width: 65%;
  max-width: 900px;
  height: 44px;
  border-radius: 22px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(139, 69, 19, 0.12);
  transition: box-shadow 0.3s ease;
}

.search-wrapper:hover {
  box-shadow: 0 4px 18px rgba(139, 69, 19, 0.2);
}

.search-tabs {
  display: flex;
  flex-direction: column;
  gap: 0;
  height: 100%;
}

.search-tab {
  padding: 0 14px;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-left: 1px solid rgba(200, 184, 160, 0.5);
  background: #f0e8d8;
  color: #8B6914;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
  letter-spacing: 1px;
}

.search-tab:first-child {
  border-left: 1px solid rgba(200, 184, 160, 0.5);
  border-bottom: 1px solid rgba(200, 184, 160, 0.3);
}

.search-tab:last-child {
  border-top: 1px solid rgba(200, 184, 160, 0.3);
}

.search-tab:hover {
  background: #e8dcc8;
  color: #6B4513;
}

.search-tab.active {
  background: #8B4513;
  color: #fff;
}

.search-form {
  display: flex;
  flex: 1;
  height: 44px;
  border: none;
  overflow: hidden;
}

.search-form input {
  flex: 1;
  height: 44px;
  padding: 0 18px;
  border: none;
  background: #faf6f0;
  font-size: 14px;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
  color: #333;
  outline: none;
  transition: background 0.25s ease;
}

.search-form input:focus {
  background: #fff;
}

.search-form input::placeholder {
  color: #a09585;
  font-style: normal;
}

.search-submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 0 22px;
  height: 44px;
  background: linear-gradient(135deg, #8B4513, #A0522D);
  border: none;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
  letter-spacing: 1px;
}

.search-submit-btn:hover {
  background: linear-gradient(135deg, #A0522D, #8B4513);
}

.search-submit-btn:active {
  transform: scale(0.97);
}
</style>
