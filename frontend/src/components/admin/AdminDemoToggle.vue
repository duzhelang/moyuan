<script setup lang="ts">
import { useAdminDemo } from '@/composables/useAdminDemo'

/**
 * 演示数据开关：只影响前端展示，不写入后端。
 * 控制台与用户分析共享同一个状态。
 */
const { demoMode, toggleDemo } = useAdminDemo()
</script>

<template>
  <button
    type="button"
    class="demo-toggle"
    :class="{ active: demoMode }"
    :title="demoMode ? '恢复接口返回的真实数据' : '仅在前端注入演示数据，不会影响后端真实数据'"
    @click="toggleDemo"
  >
    <el-icon :size="14"><MagicStick /></el-icon>
    <span>{{ demoMode ? '恢复真实数据' : '注入演示数据' }}</span>
  </button>
</template>

<style scoped lang="scss">
.demo-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 11px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 12px;
  background: var(--adm-surface-muted);
  color: var(--adm-text-muted);
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.18s ease;
  white-space: nowrap;

  &:hover {
    color: var(--adm-primary);
    border-color: color-mix(in srgb, var(--adm-primary) 36%, var(--adm-border-soft));
  }

  &.active {
    color: var(--adm-on-primary);
    background: var(--adm-primary);
    border-color: var(--adm-primary);
  }

  span {
    line-height: 1;
  }
}
</style>
