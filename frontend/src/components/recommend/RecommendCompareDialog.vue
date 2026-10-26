<script setup lang="ts">
// 策略并排对比弹窗（答辩用，附录 A.3.5 / A.8.2）：
// 内部两个 useRecommendStrategy 实例分别取左右两列，底部展示 Top10 重合率，不改变首页布局
import { computed, ref, watch } from 'vue'
import { MODEL_STRATEGY_LABELS } from '@/api/modules/recommend'
import type { ModelStrategyKey, StrategyKey } from '@/api/modules/recommend'
import { useRecommendStrategy, type UseRecommendStrategyReturn } from '@/composables/useRecommendStrategy'
import { useRecommendStrategyStore } from '@/stores/recommendStrategy'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    left: StrategyKey
    right: StrategyKey
    topK?: number
  }>(),
  { topK: 10 }
)

const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()

const store = useRecommendStrategyStore()

function useColumn(initial: StrategyKey): UseRecommendStrategyReturn {
  return useRecommendStrategy({ defaultStrategy: initial, topK: props.topK, persist: false })
}

const leftColumn = useColumn(props.left)
const rightColumn = useColumn(props.right)

watch(
  () => props.modelValue,
  async open => {
    if (!open) return
    // 每次打开按当前 props 对齐两列（URL 直链/重复打开可能带不同 left/right；
    // 列实例在 setup 时固化过初始值），同策略则刷新数据
    const syncColumn = async (column: UseRecommendStrategyReturn, target: StrategyKey) => {
      if (column.strategy.value !== target) {
        await column.switchTo(target)
      } else {
        await column.load()
      }
    }
    await Promise.all([
      syncColumn(leftColumn, props.left),
      syncColumn(rightColumn, props.right)
    ])
  }
)

// 右列可在可用策略中切换（默认右列为模型策略时允许换策略对比）
const selectableStrategies = computed<StrategyKey[]>(() => store.availableStrategies)

function labelFor(key: StrategyKey) {
  return key === 'rule' ? '规则' : MODEL_STRATEGY_LABELS[key as ModelStrategyKey]
}

function versionFor(ctrl: UseRecommendStrategyReturn) {
  const key = ctrl.strategy.value
  if (key === 'rule') return '现有规则推荐'
  const model = store.models.find(m => m.strategy === key)
  return model ? `模型版本 ${model.version}` : ''
}

/** 重合率 = 交集数量 / min(左列表长度, 右列表长度)；任一列为空显示 — */
const overlapPercent = computed<number | null>(() => {
  const l = leftColumn.items.value
  const r = rightColumn.items.value
  if (l.length === 0 || r.length === 0) return null
  const rIds = new Set(r.map(item => item.id))
  const inter = l.filter(item => rIds.has(item.id)).length
  return Math.round((inter / Math.min(l.length, r.length)) * 100)
})

// 重合率数字 300ms 计数动画
const overlapDisplay = ref(0)
let overlapRaf = 0
let overlapSettle = 0
watch(overlapPercent, target => {
  cancelAnimationFrame(overlapRaf)
  clearTimeout(overlapSettle)
  if (target == null) {
    overlapDisplay.value = 0
    return
  }
  const from = overlapDisplay.value
  const start = performance.now()
  const duration = 300
  const step = (now: number) => {
    const t = Math.min(1, (now - start) / duration)
    overlapDisplay.value = Math.round(from + (target - from) * t)
    if (t < 1) overlapRaf = requestAnimationFrame(step)
  }
  overlapRaf = requestAnimationFrame(step)
  // 兜底：后台标签页 RAF 被暂停时，计数也能收敛到真实重合率
  overlapSettle = window.setTimeout(() => {
    overlapDisplay.value = target
  }, duration + 60)
})
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    class="rcc_dialog"
    width="min(1080px, 92vw)"
    align-center
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template #header>
      <span class="rcc_title">推荐策略对比</span>
    </template>

    <div class="rcc_body">
      <div class="rcc_columns">
        <!-- 左列 -->
        <section class="rcc_column">
          <header class="rcc_column_head">
            <div class="rcc_column_name">
              <el-select
                v-if="selectableStrategies.length > 1"
                :model-value="leftColumn.strategy.value"
                size="small"
                class="rcc_select"
                @update:model-value="leftColumn.switchTo($event as StrategyKey)"
              >
                <el-option v-for="key in selectableStrategies" :key="key" :value="key" :label="labelFor(key)" />
              </el-select>
              <span v-else class="rcc_strategy_label">{{ labelFor(leftColumn.strategy.value) }}</span>
              <span v-if="leftColumn.fallbackUsed.value" class="rcc_fallback_tag">已回退规则</span>
            </div>
            <div class="rcc_column_version">{{ versionFor(leftColumn) }}</div>
          </header>
          <div class="rcc_column_list">
            <div v-if="leftColumn.loading.value" class="rcc_loading">加载中...</div>
            <div v-else-if="leftColumn.error.value" class="rcc_loading">加载失败</div>
            <div v-else-if="leftColumn.items.value.length === 0" class="rcc_loading">暂无推荐</div>
            <div
              v-for="poem in leftColumn.items.value"
              :key="'cmp-l-' + poem.id"
              class="rcc_item"
            >
              <span class="rcc_item_title">{{ poem.title }}</span>
              <span class="rcc_item_meta">{{ poem.dynasty }}{{ poem.author ? ' · ' + poem.author : '' }}</span>
            </div>
          </div>
        </section>

        <!-- 右列 -->
        <section class="rcc_column">
          <header class="rcc_column_head">
            <div class="rcc_column_name">
              <el-select
                v-if="selectableStrategies.length > 1"
                :model-value="rightColumn.strategy.value"
                size="small"
                class="rcc_select"
                @update:model-value="rightColumn.switchTo($event as StrategyKey)"
              >
                <el-option v-for="key in selectableStrategies" :key="key" :value="key" :label="labelFor(key)" />
              </el-select>
              <span v-else class="rcc_strategy_label">{{ labelFor(rightColumn.strategy.value) }}</span>
              <span v-if="rightColumn.fallbackUsed.value" class="rcc_fallback_tag">已回退规则</span>
            </div>
            <div class="rcc_column_version">{{ versionFor(rightColumn) }}</div>
          </header>
          <div class="rcc_column_list">
            <div v-if="rightColumn.loading.value" class="rcc_loading">加载中...</div>
            <div v-else-if="rightColumn.error.value" class="rcc_loading">加载失败</div>
            <div v-else-if="rightColumn.items.value.length === 0" class="rcc_loading">暂无推荐</div>
            <div
              v-for="poem in rightColumn.items.value"
              :key="'cmp-r-' + poem.id"
              class="rcc_item"
            >
              <span class="rcc_item_title">{{ poem.title }}</span>
              <span class="rcc_item_meta">{{ poem.dynasty }}{{ poem.author ? ' · ' + poem.author : '' }}</span>
            </div>
          </div>
        </section>
      </div>
    </div>

    <template #footer>
      <div class="rcc_footer">
        <span>
          Top{{ topK }} 重合率
          <strong class="rcc_overlap">
            {{ overlapPercent == null ? '—' : overlapDisplay + '%' }}
          </strong>
          · 两列各 {{ rightColumn.items.value.length || leftColumn.items.value.length }} 首
        </span>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.rcc_title {
  font-family: "Noto Serif SC", "STSong", "SimSun", serif;
  font-size: 18px;
  font-weight: 700;
  color: #8b4513;
  letter-spacing: 2px;
}

.rcc_columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.rcc_column {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border: 1px solid rgba(138, 90, 43, 0.25);
  border-radius: 10px;
  background: rgba(255, 252, 245, 0.6);
  overflow: hidden;
}

.rcc_column_head {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(138, 90, 43, 0.15);
  background: rgba(138, 90, 43, 0.06);
}

.rcc_column_name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rcc_strategy_label {
  font-size: 14px;
  font-weight: 700;
  color: #6b4d2e;
}

.rcc_select {
  width: 110px;
}

.rcc_fallback_tag {
  font-size: 11px;
  color: #8a5a2b;
  border: 1px solid rgba(138, 90, 43, 0.4);
  border-radius: 4px;
  padding: 1px 6px;
  background: rgba(138, 90, 43, 0.08);
}

.rcc_column_version {
  margin-top: 4px;
  font-size: 12px;
  color: #7a5c37;
}

.rcc_column_list {
  max-height: 52vh;
  overflow-y: auto;
  padding: 8px 12px;
}

.rcc_loading {
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 24px 0;
}

.rcc_item {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  padding: 7px 4px;
  border-bottom: 1px dashed rgba(138, 90, 43, 0.15);
  font-size: 13px;
  color: #4a3826;
}

.rcc_item:last-child {
  border-bottom: none;
}

.rcc_item_title {
  font-family: "Noto Serif SC", "STSong", "SimSun", serif;
  letter-spacing: 1px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rcc_item_meta {
  flex-shrink: 0;
  font-size: 12px;
  color: #a0724e;
}

.rcc_footer {
  text-align: left;
  font-size: 13px;
  color: #6b4d2e;
}

.rcc_overlap {
  color: #8a5a2b;
  font-size: 16px;
}

/* <768px 上下堆叠 */
@media (max-width: 768px) {
  .rcc_columns {
    grid-template-columns: 1fr;
  }

  .rcc_column_list {
    max-height: 36vh;
  }
}
</style>

<style>
/* el-dialog 渲染于组件树内（未 append-to-body），此处用全局选择器定制墨渊棕金底 */
.rcc_dialog.el-dialog {
  max-width: 92vw;
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(160deg, #fffdf7 0%, #faf3e4 100%);
  border: 1px solid rgba(138, 90, 43, 0.3);
  border-radius: 14px;
  overflow: hidden;
}

.rcc_dialog .el-dialog__body {
  overflow-y: auto;
  padding-top: 8px;
}

.rcc_dialog .el-dialog__footer {
  border-top: 1px solid rgba(138, 90, 43, 0.15);
  padding-top: 10px;
}
</style>
