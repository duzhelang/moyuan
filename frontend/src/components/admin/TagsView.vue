<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminTabs } from '@/composables/useAdminTabs'
import type { AdminTab } from '@/composables/useAdminTabs'

const route = useRoute()
const router = useRouter()
const { tabs, removeTab, closeOthers, closeAll } = useAdminTabs()

const scroller = ref<HTMLElement | null>(null)

const isActive = (tab: AdminTab) => tab.path === route.path

/** 关闭单个标签：关的是当前页就跳到左边相邻页，没有就回控制台 */
const close = (tab: AdminTab) => {
  if (tab.affix) return
  const removedAt = removeTab(tab.path)
  if (removedAt < 0) return
  if (isActive(tab)) {
    const fallback = tabs.value[removedAt - 1] || tabs.value[removedAt] || tabs.value[0]
    router.push(fallback?.fullPath || fallback?.path || '/admin/dashboard')
  }
}

const handleCloseOthers = (tab: AdminTab) => {
  closeOthers(tab.path)
  if (!isActive(tab)) router.push(tab.fullPath || tab.path)
}

const handleCloseAll = () => {
  closeAll()
  if (route.path !== '/admin/dashboard') router.push('/admin/dashboard')
}

const handleCommand = (command: string) => {
  if (command === 'all') {
    handleCloseAll()
    return
  }
  const current = tabs.value.find((t) => isActive(t)) || tabs.value[0]
  if (current) handleCloseOthers(current)
}

/** 当前标签滚进可视区 */
const scrollActiveIntoView = () => {
  nextTick(() => {
    const el = scroller.value?.querySelector('.tag.is-active') as HTMLElement | null
    el?.scrollIntoView({ block: 'nearest', inline: 'nearest' })
  })
}

watch(() => route.path, scrollActiveIntoView)
onMounted(scrollActiveIntoView)
</script>

<template>
  <div class="tags-view">
    <div ref="scroller" class="tags-scroll">
      <div
        v-for="tab in tabs"
        :key="tab.path"
        class="tag"
        :class="{ 'is-active': isActive(tab), 'is-affix': tab.affix }"
        @click="router.push(tab.fullPath || tab.path)"
      >
        <i class="tag-dot"></i>
        <span class="tag-label">{{ tab.label }}</span>
        <el-icon v-if="!tab.affix" class="tag-close" @click.stop="close(tab)">
          <Close />
        </el-icon>
      </div>
    </div>

    <el-dropdown trigger="click" @command="handleCommand">
      <span class="tags-more">
        <el-icon><MoreFilled /></el-icon>
      </span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="others">关闭其他</el-dropdown-item>
          <el-dropdown-item command="all">关闭全部</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<style scoped lang="scss">
.tags-view {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 16px 0 20px;
  background: var(--adm-surface);
  border-bottom: 1px solid var(--adm-border-soft);
  transition: background-color 0.25s;
}

.tags-scroll {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 26px;
  padding: 0 9px;
  flex-shrink: 0;
  border: 1px solid var(--adm-border-soft);
  border-radius: 4px;
  background: var(--adm-surface-muted);
  font-size: 12px;
  color: var(--adm-text-muted);
  cursor: pointer;
  user-select: none;
  transition: all 0.15s;

  &:hover {
    color: var(--adm-primary);
    border-color: var(--adm-border);
    background: var(--adm-primary-soft);
  }

  .tag-dot {
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--adm-text-faint);
  }

  .tag-label {
    max-width: 168px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .tag-close {
    font-size: 11px;
    color: var(--adm-text-faint);
    border-radius: 50%;
    transition: all 0.15s;

    &:hover {
      color: var(--adm-on-primary);
      background: var(--adm-down);
    }
  }

  &.is-active {
    color: var(--adm-on-primary);
    background: var(--adm-primary);
    border-color: var(--adm-primary);

    .tag-dot {
      background: var(--adm-on-primary);
    }

    .tag-close {
      color: var(--adm-on-primary);
      opacity: 0.72;

      &:hover {
        color: var(--adm-on-primary);
        background: rgba(255, 255, 255, 0.25);
        opacity: 1;
      }
    }
  }
}

.tags-more {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  border-radius: 4px;
  color: var(--adm-text-muted);
  cursor: pointer;
  outline: none;

  &:hover {
    color: var(--adm-primary);
    background: var(--adm-primary-soft);
  }
}
</style>
