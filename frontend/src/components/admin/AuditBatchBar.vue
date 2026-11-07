<script setup lang="ts">
import { computed } from 'vue'
import type { BatchBarAction } from '@/types/admin-audit'

const props = withDefaults(
  defineProps<{
    count: number
    pendingCount?: number
    running?: boolean
    done?: number
    queued?: number
    percent?: number
    failedCount?: number
    actions?: BatchBarAction[]
    total?: number
    showShortcuts?: boolean
  }>(),
  {
    pendingCount: undefined,
    running: false,
    done: 0,
    queued: 0,
    percent: 0,
    failedCount: 0,
    actions: () => [],
    total: 0,
    showShortcuts: true
  }
)

const emit = defineEmits<{
  (e: 'action', key: string): void
  (e: 'clear'): void
  (e: 'select-all'): void
  (e: 'retry-failed'): void
}>()

const hasSelection = computed(() => props.count > 0)
const progressText = computed(() => `${props.done}/${props.queued}`)
</script>

<template>
  <div class="audit-bar" :class="{ active: hasSelection, busy: running }">
    <div class="left">
      <template v-if="hasSelection">
        <span class="badge">{{ count }}</span>
        <span class="summary">
          已选 <b>{{ count }}</b> 条
          <template v-if="pendingCount !== undefined && pendingCount !== count">
            <em>· 其中 {{ pendingCount }} 条可审</em>
          </template>
        </span>
        <el-button link type="primary" size="small" :disabled="running" @click="emit('clear')">
          清空
        </el-button>
        <el-button
          v-if="failedCount"
          link
          type="danger"
          size="small"
          :disabled="running"
          @click="emit('retry-failed')"
        >
          {{ failedCount }} 条失败，仅留失败项
        </el-button>
      </template>
      <template v-else>
        <span class="idle-text">
          勾选左侧复选框可批量处理<template v-if="total">，本页共 {{ total }} 条</template>
        </span>
        <el-button link type="primary" size="small" :disabled="running" @click="emit('select-all')">
          全选本页
        </el-button>
      </template>
    </div>

    <div class="right">
      <div v-if="running" class="progress">
        <div class="track">
          <div class="fill" :style="{ width: percent + '%' }"></div>
        </div>
        <span class="progress-text">{{ progressText }}</span>
      </div>

      <el-button
        v-for="action in actions"
        :key="action.key"
        size="small"
        :type="action.type || 'default'"
        :plain="action.plain"
        :disabled="!hasSelection || running"
        @click="emit('action', action.key)"
      >
        {{ action.label }}
      </el-button>

      <span v-if="showShortcuts" class="shortcuts">
        <kbd>J</kbd><kbd>K</kbd> 移动
        <kbd>X</kbd> 勾选
        <kbd>A</kbd> 通过
        <kbd>R</kbd> 拒绝
      </span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.audit-bar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 46px;
  margin-bottom: 12px;
  padding: 8px 14px;
  background: var(--adm-surface);
  border: 1px solid var(--adm-border);
  border-left: 3px solid var(--adm-border);
  border-radius: 6px;
  transition: border-color 0.15s, background-color 0.15s;

  &.active {
    border-left-color: var(--adm-primary);
    background: color-mix(in srgb, var(--adm-primary) 8%, transparent);
  }
}

.left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 11px;
  background: var(--adm-primary);
  color: var(--adm-on-primary);
  font-size: 12px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}

.summary {
  font-size: 13px;
  color: var(--adm-text-2);

  b {
    color: var(--adm-text);
    font-weight: 500;
    font-variant-numeric: tabular-nums;
  }

  em {
    font-style: normal;
    color: var(--adm-text-muted);
  }
}

.idle-text {
  font-size: 13px;
  color: var(--adm-text-muted);
}

.right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

.track {
  width: 90px;
  height: 4px;
  border-radius: 2px;
  background: var(--adm-border-soft);
  overflow: hidden;
}

.fill {
  height: 100%;
  background: var(--adm-primary);
  transition: width 0.2s linear;
}

.progress-text {
  font-size: 12px;
  color: var(--adm-text-2);
  font-variant-numeric: tabular-nums;
}

.shortcuts {
  margin-left: 4px;
  padding-left: 10px;
  border-left: 1px solid var(--adm-border-soft);
  font-size: 12px;
  color: var(--adm-text-faint);
  white-space: nowrap;

  kbd {
    display: inline-block;
    min-width: 16px;
    margin-right: 2px;
    padding: 0 4px;
    border: 1px solid var(--adm-border);
    border-bottom-width: 2px;
    border-radius: 3px;
    background: var(--adm-surface-muted);
    font-family: inherit;
    font-size: 11px;
    line-height: 15px;
    text-align: center;
    color: var(--adm-text-muted);
  }
}

@media (max-width: 1200px) {
  .shortcuts {
    display: none;
  }
}
</style>
