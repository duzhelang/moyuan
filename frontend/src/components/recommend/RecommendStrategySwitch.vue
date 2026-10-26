<script setup lang="ts">
// 推荐策略切换纯控件：只负责标签渲染、键盘切换与焦点管理，不取数（附录 A.3.1）
import { ref } from 'vue'
import type { StrategyKey, StrategyOption } from '@/api/modules/recommend'

const props = defineProps<{
  modelValue: StrategyKey
  options: StrategyOption[]
  disabled?: boolean
  size?: 'default' | 'small'
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', key: StrategyKey): void
  (e: 'change', payload: { from: StrategyKey; to: StrategyKey }): void
}>()

const tabRefs = ref<Record<string, HTMLElement | null>>({})

function setTabRef(key: StrategyKey, el: unknown) {
  tabRefs.value[key] = (el as HTMLElement) ?? null
}

function select(option: StrategyOption) {
  if (props.disabled || option.key === props.modelValue) return
  emit('change', { from: props.modelValue, to: option.key })
  emit('update:modelValue', option.key)
  tabRefs.value[option.key]?.focus()
}

function selectIndex(index: number) {
  const option = props.options[index]
  if (option) select(option)
}

function currentIndex(option: StrategyOption) {
  return props.options.findIndex(o => o.key === option.key)
}

function onKeydown(event: KeyboardEvent, option: StrategyOption) {
  const len = props.options.length
  if (len === 0) return
  switch (event.key) {
    case 'ArrowRight':
    case 'ArrowDown':
      event.preventDefault()
      selectIndex((currentIndex(option) + 1) % len)
      break
    case 'ArrowLeft':
    case 'ArrowUp':
      event.preventDefault()
      selectIndex((currentIndex(option) - 1 + len) % len)
      break
    case 'Home':
      event.preventDefault()
      selectIndex(0)
      break
    case 'End':
      event.preventDefault()
      selectIndex(len - 1)
      break
  }
}
</script>

<template>
  <div
    class="rs_switch"
    :class="[size === 'small' ? 'rs_small' : 'rs_default', { rs_disabled: disabled }]"
    role="tablist"
    aria-label="推荐策略切换"
  >
    <el-tooltip
      v-for="option in options"
      :key="option.key"
      :content="option.detail ? `${option.hint} · ${option.detail}` : option.hint"
      placement="bottom"
      :show-after="120"
      popper-class="moyuan-poem-tip"
    >
      <button
        :ref="el => setTabRef(option.key, el)"
        type="button"
        role="tab"
        class="rs_tab"
        :class="{ active: option.key === modelValue }"
        :aria-selected="option.key === modelValue"
        :tabindex="option.key === modelValue ? 0 : -1"
        :disabled="disabled"
        @click="select(option)"
        @keydown="onKeydown($event, option)"
      >
        <span class="rs_label">{{ option.label }}</span>
        <span v-if="option.metric" class="rs_metric">{{ option.metric }}</span>
      </button>
    </el-tooltip>
  </div>
</template>

<style scoped>
.rs_switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px;
  background: rgba(255, 252, 245, 0.6);
  border: 1px solid rgba(138, 90, 43, 0.25);
  border-radius: 8px;
  max-width: 100%;
}

.rs_tab {
  height: 30px;
  padding: 0 12px;
  font-size: 13px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #6b4d2e;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.18s ease;
  display: inline-flex;
  align-items: center;
  font-family: inherit;
}

.rs_tab:hover:not(:disabled) {
  background: rgba(138, 90, 43, 0.08);
}

.rs_tab.active {
  background: #8a5a2b;
  color: #f7efe0;
  box-shadow: 0 2px 6px rgba(138, 90, 43, 0.25);
}

.rs_tab:focus-visible {
  outline: 3px solid rgba(212, 175, 55, 0.55);
  outline-offset: 1px;
}

.rs_tab:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.rs_metric {
  font-size: 11px;
  margin-left: 4px;
  color: #7a5c37;
}

.rs_tab.active .rs_metric {
  color: rgba(247, 239, 224, 0.88);
}

/* 小尺寸：<768px 移动端 */
.rs_small .rs_tab {
  height: 28px;
  padding: 0 10px;
  font-size: 12px;
}

.rs_small .rs_metric {
  font-size: 10px;
}

.rs_disabled {
  opacity: 0.45;
}
</style>
