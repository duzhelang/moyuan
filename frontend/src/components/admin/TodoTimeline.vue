<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { TodoItem } from '@/types/admin-dashboard'

const props = withDefaults(defineProps<{ items?: TodoItem[] }>(), {
  items: () => []
})

const router = useRouter()

const activeKind = ref<'audit' | 'alert'>('audit')

const groups = computed(() => {
  const audit = props.items.filter((i) => (i.kind || 'audit') === 'audit')
  const alert = props.items.filter((i) => i.kind === 'alert')
  return [
    { key: 'audit' as const, label: '审核积压', items: audit },
    { key: 'alert' as const, label: '异常处置', items: alert }
  ].filter((g) => g.items.length > 0)
})

const list = computed(() =>
  props.items.filter((i) => (i.kind || 'audit') === activeKind.value)
)
const allTotal = computed(() => props.items.reduce((sum, i) => sum + i.count, 0))
const total = computed(() => list.value.reduce((sum, i) => sum + i.count, 0))
const groupCount = (items: TodoItem[]) => items.reduce((sum, i) => sum + i.count, 0)

const hasAlert = computed(() =>
  list.value.some((i) => i.status === 'down' || i.status === 'warn')
)

/** 进度条以 10 条为满格基线，积压越多条越长 */
const maxCount = computed(() => Math.max(10, ...list.value.map((i) => i.count)))

/** 顶部构成条：按各待办数量占比铺出整条色带 */
const segments = computed(() =>
  list.value.map((item, index) => ({
    key: item.key,
    count: item.count,
    pct: total.value ? (item.count / total.value) * 100 : 0,
    color: tone(index)
  }))
)

const tone = (index: number) => `var(--adm-chart-${(index % 6) + 1})`
const barOf = (count: number) => Math.min(100, (count / maxCount.value) * 100)

const go = (item: TodoItem) => {
  if (item.link) router.push(item.link)
}
</script>

<template>
  <section class="todo-panel">
    <span class="todo-watermark" aria-hidden="true">{{ total }}</span>

    <div class="tp-head">
      <div class="tp-title-box">
        <span class="tp-title">运营待办</span>
        <span class="tp-note">
          {{ activeKind === 'audit' ? '审核积压 · 点击直达处理页' : '异常处置 · 举报 / 报修 / AI / 安全' }}
        </span>
      </div>
      <div class="tp-total">
        <b>{{ total }}</b>
        <span>{{ activeKind === 'audit' ? '待审核' : '待处置' }}</span>
      </div>
    </div>

    <div class="tp-tabs" role="tablist">
      <button
        v-for="group in groups"
        :key="group.key"
        type="button"
        class="tp-tab"
        :class="{ active: activeKind === group.key }"
        role="tab"
        :aria-selected="activeKind === group.key"
        @click="activeKind = group.key"
      >
        {{ group.label }}
        <b>{{ groupCount(group.items) }}</b>
      </button>
      <span class="tp-all">全部 {{ allTotal }}</span>
    </div>

    <div v-if="segments.length" class="tp-stack" aria-hidden="true">
      <i
        v-for="seg in segments"
        :key="seg.key"
        :style="{ width: seg.pct + '%', background: seg.color }"
      ></i>
    </div>

    <div class="tp-grid">
      <div
        v-for="(item, index) in list"
        :key="item.key"
        class="tp-tile"
        :class="[item.status, { linkable: !!item.link }]"
        :title="item.note || ''"
        @click="go(item)"
      >
        <div class="tile-head">
          <span class="tile-label">{{ item.label }}</span>
          <span class="tile-arrow">→</span>
        </div>
        <div class="tile-num" :style="{ color: item.count ? tone(index) : undefined }">
          {{ item.count }}
        </div>
        <div class="tile-bar">
          <i :style="{ width: barOf(item.count) + '%', background: tone(index) }"></i>
        </div>
        <span class="tile-tag">{{ item.status === 'ok' ? '正常' : '需处理' }}</span>
      </div>
    </div>

    <div v-if="!list.length" class="tp-empty">暂无待办数据</div>

    <div class="tp-foot">
      <span>共 6 类审核事项</span>
      <b v-if="hasAlert" class="foot-alert">存在积压，建议优先处理</b>
      <b v-else class="foot-ok">全部在正常水位</b>
    </div>
  </section>
</template>

<style scoped lang="scss">
.todo-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 262px;
  padding: 13px 16px 12px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 10px;
  overflow: hidden;
  background:
    linear-gradient(150deg, color-mix(in srgb, var(--adm-primary) 10%, transparent), transparent 54%),
    var(--adm-surface);
  transition: border-color 0.2s, box-shadow 0.2s;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, var(--adm-primary), var(--adm-chart-2), transparent 78%);
    opacity: 0.8;
  }

  &:hover {
    border-color: var(--adm-border);
    box-shadow: var(--adm-shadow);
  }
}

/** 背景大数字：给卡片一个「量级」的视觉锚点 */
.todo-watermark {
  position: absolute;
  right: 12px;
  bottom: -12px;
  font-family: var(--adm-font-serif);
  font-size: 88px;
  font-weight: 700;
  line-height: 1;
  color: color-mix(in srgb, var(--adm-primary) 7%, transparent);
  pointer-events: none;
  user-select: none;
}

.tp-head {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.tp-title-box {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.tp-title {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--adm-text);
}

.tp-note {
  font-size: 11px;
  color: var(--adm-text-faint);
}

.tp-total {
  display: flex;
  align-items: baseline;
  gap: 4px;
  flex-shrink: 0;
  padding: 2px 9px;
  border-radius: 20px;
  background: color-mix(in srgb, var(--adm-primary) 12%, transparent);
  border: 1px solid var(--adm-border-soft);

  b {
    font-family: var(--adm-font-serif);
    font-size: 19px;
    font-weight: 700;
    color: var(--adm-primary);
    font-variant-numeric: tabular-nums;
  }

  span {
    font-size: 11px;
    color: var(--adm-text-muted);
  }
}

.tp-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.tp-tab {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 12px;
  background: transparent;
  color: var(--adm-text-muted);
  font-size: 11px;
  cursor: pointer;
  transition: all 0.18s;

  b {
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }

  &:hover {
    color: var(--adm-primary);
    border-color: var(--adm-primary);
  }

  &.active {
    color: var(--adm-on-primary);
    background: var(--adm-primary);
    border-color: var(--adm-primary);
  }
}

.tp-all {
  margin-left: auto;
  font-size: 10.5px;
  color: var(--adm-text-faint);
  font-variant-numeric: tabular-nums;
}

/** 构成条：一眼看出积压集中在哪一类 */
.tp-stack {
  position: relative;
  display: flex;
  height: 6px;
  border-radius: 3px;
  overflow: hidden;
  background: var(--adm-surface-muted);
  margin-bottom: 10px;

  i {
    display: block;
    height: 100%;
    opacity: 0.85;

    & + i {
      margin-left: 2px;
    }
  }
}

.tp-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-auto-rows: 1fr;
  gap: 8px;
  flex: 1;
  min-height: 0;

  @media (max-width: 700px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.tp-tile {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 9px 10px 8px;
  border: 1px solid var(--adm-border-soft);
  border-radius: 8px;
  background: var(--adm-surface-muted);
  transition: transform 0.16s, border-color 0.16s, background 0.16s;

  &.linkable {
    cursor: pointer;
  }

  &.linkable:hover {
    transform: translateY(-2px);
    border-color: var(--adm-border);
    background: var(--adm-surface);

    .tile-arrow {
      opacity: 1;
      transform: translateX(0);
    }
  }

  &.warn {
    border-color: color-mix(in srgb, var(--adm-warn) 36%, var(--adm-border-soft));
    background: color-mix(in srgb, var(--adm-warn) 8%, var(--adm-surface-muted));
  }

  &.down {
    border-color: color-mix(in srgb, var(--adm-down) 40%, var(--adm-border-soft));
  }
}

.tile-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.tile-label {
  font-size: 12px;
  color: var(--adm-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tile-arrow {
  font-size: 12px;
  color: var(--adm-primary);
  opacity: 0;
  transform: translateX(-3px);
  transition: opacity 0.16s, transform 0.16s;
  flex-shrink: 0;
}

.tile-num {
  font-family: var(--adm-font-serif);
  font-size: 27px;
  font-weight: 700;
  line-height: 1.05;
  color: var(--adm-text);
  font-variant-numeric: tabular-nums;
}

.tile-bar {
  height: 3px;
  border-radius: 2px;
  background: color-mix(in srgb, var(--adm-text) 6%, transparent);
  overflow: hidden;

  i {
    display: block;
    height: 100%;
    border-radius: 2px;
    opacity: 0.65;
  }
}

.tile-tag {
  align-self: flex-start;
  font-size: 10px;
  line-height: 1.6;
  padding: 0 6px;
  border-radius: 8px;
  color: var(--adm-text-faint);
  background: color-mix(in srgb, var(--adm-text) 5%, transparent);

  .warn & {
    color: var(--adm-warn);
    background: color-mix(in srgb, var(--adm-warn) 12%, transparent);
  }

  .down & {
    color: var(--adm-down);
    background: color-mix(in srgb, var(--adm-down) 12%, transparent);
  }
}

.tp-empty {
  margin: auto;
  font-size: 12px;
  color: var(--adm-text-faint);
}

.tp-foot {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 9px;
  padding-top: 8px;
  border-top: 1px solid var(--adm-border-soft);
  font-size: 11px;
  color: var(--adm-text-faint);
}

.foot-alert {
  color: var(--adm-warn);
  font-weight: 500;
}

.foot-ok {
  color: var(--adm-ok);
  font-weight: 500;
}
</style>
