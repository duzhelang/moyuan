<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

withDefaults(defineProps<{ size?: number }>(), {
  size: 32
})

const router = useRouter()
const userStore = useUserStore()

const handleLogout = async () => {
  try {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } catch {
    ElMessage.error('退出失败')
  }
}

const handleUserCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/user/profile')
      break
    case 'favorites':
      router.push('/user/profile?tab=favorites')
      break
    case 'posts':
      router.push('/user/profile?tab=posts')
      break
    case 'history':
      router.push('/user/profile?tab=history')
      break
    case 'repair':
      router.push('/repair')
      break
    case 'createPoem':
      router.push('/poem/create')
      break
    case 'admin':
      router.push('/admin/dashboard')
      break
    case 'logout':
      handleLogout()
      break
  }
}
</script>

<template>
  <el-dropdown trigger="click" teleported @command="handleUserCommand">
    <div class="user-info">
      <el-avatar :src="userStore.avatar" :size="size" class="user-avatar">
        {{ userStore.username?.charAt(0)?.toUpperCase() }}
      </el-avatar>
      <span class="user-nickname">
        {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}
      </span>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="profile">
          <el-icon><User /></el-icon>
          个人中心
        </el-dropdown-item>
        <el-dropdown-item command="favorites">
          <el-icon><Star /></el-icon>
          我的收藏
        </el-dropdown-item>
        <el-dropdown-item command="posts">
          <el-icon><Document /></el-icon>
          我的帖子
        </el-dropdown-item>
        <el-dropdown-item command="history">
          <el-icon><Clock /></el-icon>
          浏览历史
        </el-dropdown-item>
        <el-dropdown-item command="repair">
          <el-icon><Tickets /></el-icon>
          报修中心
        </el-dropdown-item>
        <el-dropdown-item command="createPoem">
          <el-icon><Edit /></el-icon>
          发布新诗
        </el-dropdown-item>
        <el-dropdown-item v-if="userStore.userInfo?.role === 'admin'" divided command="admin">
          <el-icon><Setting /></el-icon>
          后台管理
        </el-dropdown-item>
        <el-dropdown-item divided command="logout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped lang="scss">
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 20px;
  transition: all 0.3s ease;

  &:hover {
    background-color: rgba(255, 255, 255, 0.2);
    transform: scale(1.02);
  }
}

.user-avatar {
  position: relative;
  border: 2px solid rgba(255, 215, 0, 0.55);
  background: linear-gradient(135deg, rgba(255, 215, 0, 0.2), rgba(120, 80, 20, 0.28));
  box-shadow:
    0 0 10px rgba(255, 215, 0, 0.45),
    0 0 22px rgba(255, 215, 0, 0.22),
    inset 0 0 6px rgba(255, 215, 0, 0.25);
  transition: all 0.3s ease;

  :deep(img) {
    opacity: 0.95;
  }

  /* 底纹：中心柔光 + 金色斜向网纹，覆盖在头像上增强质感 */
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: 50%;
    pointer-events: none;
    background:
      radial-gradient(circle at 30% 25%, rgba(255, 255, 255, 0.18), rgba(0, 0, 0, 0.18) 72%),
      repeating-linear-gradient(45deg, rgba(255, 215, 0, 0.14) 0 3px, rgba(255, 215, 0, 0.03) 3px 6px);
  }

  &:hover {
    border-color: #ffd700;
    box-shadow:
      0 0 16px rgba(255, 215, 0, 0.85),
      0 0 30px rgba(255, 215, 0, 0.5),
      inset 0 0 8px rgba(255, 215, 0, 0.35);
    transform: scale(1.05);
  }
}

.user-nickname {
  color: #ffd700;
  font-size: 14px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-shadow: 0 0 6px rgba(255, 215, 0, 0.45), 1px 1px 2px rgba(0, 0, 0, 0.4);
}
</style>
