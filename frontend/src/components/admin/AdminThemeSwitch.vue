<script setup lang="ts">
import { computed } from 'vue'
import { useAdminTheme } from '@/composables/useAdminTheme'
import type { AdminTheme } from '@/composables/useAdminTheme'

const { theme, viewMode, resolvedTheme, setTheme, setViewMode } = useAdminTheme()

const themeOptions: {
  value: AdminTheme
  label: string
  desc: string
  dots: string[]
}[] = [
  { value: 'auto', label: '双境自动', desc: '运营浅色 · 评测深色', dots: ['#f3eee4', '#141c24'] },
  { value: 'paper', label: '宣纸墨韵', desc: '浅色文人书房', dots: ['#f3eee4', '#8a5a2b'] },
  { value: 'night', label: '夜墨金线', desc: '深色墨夜 · 金线', dots: ['#0d141b', '#c9a45c'] },
  { value: 'ink', label: '砚石墨白', desc: '中性工业浅色', dots: ['#f2f3f5', '#3a3f45'] }
]

const currentLabel = computed(
  () => themeOptions.find((item) => item.value === theme.value)?.label || '双境自动'
)

const themes: { value: 'ops' | 'eval'; label: string }[] = [
  { value: 'ops', label: '运营视图' },
  { value: 'eval', label: '评测视图' }
]
</script>

<template>
  <el-popover placement="bottom-end" :width="264" trigger="click" popper-class="admin-theme-popper">
    <template #reference>
      <button class="theme-trigger" type="button" :title="`当前主题：${currentLabel}`">
        <span class="theme-dots">
          <i v-for="(dot, i) in themeOptions.find((o) => o.value === theme)?.dots" :key="i" :style="{ background: dot }"></i>
        </span>
        <span class="theme-label">{{ currentLabel }}</span>
        <el-icon class="theme-arrow"><ArrowDown /></el-icon>
      </button>
    </template>

    <div class="theme-panel">
      <p class="panel-title">外观主题</p>
      <button
        v-for="item in themeOptions"
        :key="item.value"
        type="button"
        class="theme-item"
        :class="{ active: theme === item.value }"
        @click="setTheme(item.value)"
      >
        <span class="theme-dots">
          <i v-for="(dot, i) in item.dots" :key="i" :style="{ background: dot }"></i>
        </span>
        <span class="theme-text">
          <b>{{ item.label }}</b>
          <em>{{ item.desc }}</em>
        </span>
        <el-icon v-if="theme === item.value" class="check"><Check /></el-icon>
      </button>

      <div class="panel-sep"></div>

      <p class="panel-title">视图模式</p>
      <div class="view-switch">
        <button
          v-for="item in themes"
          :key="item.value"
          type="button"
          class="view-btn"
          :class="{ active: viewMode === item.value }"
          @click="setViewMode(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
      <p class="panel-hint">
        双境自动下，运营视图用宣纸浅色，评测视图用夜墨深色；当前解析为
        {{ resolvedTheme === 'night' ? '夜墨金线' : resolvedTheme === 'ink' ? '砚石墨白' : '宣纸墨韵' }}。
      </p>
    </div>
  </el-popover>
</template>

<style scoped lang="scss">
.theme-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 30px;
  padding: 0 10px;
  border-radius: 15px;
  border: 1px solid var(--adm-border-soft);
  background: var(--adm-surface);
  color: var(--adm-text-2);
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;

  &:hover {
    border-color: var(--adm-border);
    background: var(--adm-surface-muted);
  }
}

.theme-dots {
  display: inline-flex;
  align-items: center;
  gap: 2px;

  i {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    display: block;
    box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.12);
  }
}

.theme-label {
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.theme-arrow {
  font-size: 12px;
  color: var(--adm-text-faint);
}

.theme-panel {
  .panel-title {
    margin: 0 0 8px;
    font-size: 11px;
    letter-spacing: 1px;
    color: var(--adm-text-faint);
  }
}

.theme-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s;

  &:hover {
    background: var(--adm-surface-muted);
  }

  &.active {
    background: var(--adm-primary-soft);
  }

  .theme-text {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;

    b {
      font-size: 13px;
      font-weight: 500;
      color: var(--adm-text);
    }

    em {
      font-style: normal;
      font-size: 11px;
      color: var(--adm-text-muted);
    }
  }

  .check {
    font-size: 14px;
    color: var(--adm-primary);
  }
}

.panel-sep {
  height: 1px;
  margin: 10px 0;
  background: var(--adm-border-soft);
}

.view-switch {
  display: flex;
  gap: 6px;
  padding: 3px;
  border-radius: 8px;
  background: var(--adm-surface-muted);
}

.view-btn {
  flex: 1;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--adm-text-muted);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;

  &.active {
    background: var(--adm-surface);
    color: var(--adm-primary);
    box-shadow: var(--adm-shadow);
    font-weight: 500;
  }
}

.panel-hint {
  margin: 8px 0 0;
  font-size: 11px;
  line-height: 1.6;
  color: var(--adm-text-faint);
}
</style>
