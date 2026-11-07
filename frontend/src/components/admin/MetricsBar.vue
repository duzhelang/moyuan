<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { MetricItem } from '@/types/admin-dashboard'

defineProps<{ items: MetricItem[] }>()

const router = useRouter()

/** 未接通的指标不再显示成「—」，统一标注采样状态，避免整行看起来像坏掉 */
const displayValue = (item: MetricItem) => (item.live ? item.value : '待采样')
</script>

<template>
  <div class="metrics-bar">
    <div class="bar-head">
      <span class="bar-label">今日体检</span>
      <span class="bar-hint">异常项自动标色</span>
    </div>

    <div class="bar-grid">
      <div
        v-for="item in items"
        :key="item.key"
        class="cell"
        :class="{ ghost: !item.live }"
        :title="item.live ? '' : '埋点已上线，等待首批真实数据'"
      >
        <span class="cell-label">
          <em class="dot" :class="[item.live ? item.status : 'ghost']"></em>
          {{ item.label }}
        </span>
        <span class="cell-value" :class="[item.live ? item.status : 'ghost']">
          {{ displayValue(item) }}
        </span>
      </div>
    </div>

    <span class="bar-more" @click="router.push('/admin/monitor')">全部监控 →</span>
  </div>
</template>

<style scoped lang="scss">
.metrics-bar {
  display: flex;
  align-items: stretch;
  gap: 18px;
  padding: 12px 18px;
  margin-bottom: 16px;
  background: var(--adm-surface);
  border: 1px solid var(--adm-border-soft);
  border-radius: 8px;
}

.bar-head {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 3px;
  padding-right: 18px;
  border-right: 1px solid var(--adm-border-soft);
  flex-shrink: 0;
}

.bar-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--adm-text);
}

.bar-hint {
  font-size: 11px;
  color: var(--adm-text-faint);
}

.bar-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(96px, 1fr));
  gap: 4px;
  flex: 1;
  min-width: 0;

  @media (max-width: 1400px) {
    grid-template-columns: repeat(3, minmax(96px, 1fr));
    row-gap: 12px;
  }
}

.cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 0 10px;
  border-left: 1px solid var(--adm-surface-muted);

  &:first-child {
    border-left: none;
  }
}

.cell-label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--adm-text-muted);
  white-space: nowrap;

  .dot {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: var(--adm-ok);
    flex-shrink: 0;

    &.warn {
      background: var(--adm-warn);
    }

    &.down {
      background: var(--adm-down);
    }

    &.ghost {
      background: var(--adm-border);
    }
  }
}

.cell-value {
  font-size: 15px;
  font-weight: 500;
  color: var(--adm-text);
  font-variant-numeric: tabular-nums;
  line-height: 1.2;

  &.warn {
    color: var(--adm-warn);
  }

  &.down {
    color: var(--adm-down);
  }

  &.ghost {
    font-size: 12px;
    font-weight: 400;
    color: var(--adm-text-faint);
  }
}

.bar-more {
  display: flex;
  align-items: center;
  margin-left: auto;
  padding-left: 14px;
  border-left: 1px solid var(--adm-border-soft);
  font-size: 12px;
  color: var(--adm-primary);
  cursor: pointer;
  flex-shrink: 0;

  &:hover {
    opacity: 0.8;
  }
}
</style>
