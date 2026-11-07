<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { findAdminMenu, queryPageSuffix } from '@/configs/adminMenu'

const route = useRoute()
const router = useRouter()

const current = computed(() => {
  const menu = findAdminMenu(route.path)
  const base = menu?.label || (route.meta?.title as string) || '未命名页面'
  // 带 ?tab= 的页面补上语义后缀，和标签栏保持一致
  const suffix = queryPageSuffix(route.path, route.query as Record<string, unknown>)
  return {
    group: menu?.group || '',
    label: suffix ? `${base} · ${suffix}` : base
  }
})

const isHome = computed(() => route.path === '/admin/dashboard')

const goHome = () => {
  if (!isHome.value) router.push('/admin/dashboard')
}
</script>

<template>
  <div class="admin-crumb">
    <span class="crumb-item home" :class="{ plain: isHome }" @click="goHome">
      <el-icon><HomeFilled /></el-icon>
      后台首页
    </span>

    <template v-if="!isHome">
      <el-icon class="crumb-sep"><ArrowRight /></el-icon>
      <span class="crumb-item group">{{ current.group }}</span>
      <el-icon class="crumb-sep"><ArrowRight /></el-icon>
      <span class="crumb-item last">{{ current.label }}</span>
    </template>
  </div>
</template>

<style scoped lang="scss">
.admin-crumb {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 20px;
  background: var(--adm-surface);
  border-bottom: 1px solid var(--adm-border-soft);
  font-size: 12px;
  transition: background-color 0.25s;
}

.crumb-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--adm-text-muted);
  white-space: nowrap;

  &.home {
    cursor: pointer;

    &:hover {
      color: var(--adm-primary);
    }

    &.plain {
      cursor: default;
      color: var(--adm-text-2);

      &:hover {
        color: var(--adm-text-2);
      }
    }
  }

  &.group {
    color: var(--adm-text-muted);
  }

  &.last {
    color: var(--adm-text);
    font-weight: 500;
  }
}

.crumb-sep {
  font-size: 10px;
  color: var(--adm-text-faint);
}
</style>
