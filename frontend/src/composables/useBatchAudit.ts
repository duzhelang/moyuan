import { computed, ref, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export type BatchAction = 'approve' | 'reject' | 'delete'

export interface BatchFailure {
  id: number
  label: string
  message: string
}

interface BatchAuditOptions<T> {
  /** 单条执行器：内部会并发调用，页面传各自现有的单条审核接口即可 */
  handler: (id: number, action: BatchAction, reason?: string) => Promise<unknown>
  /** 并发度：后端是普通 REST，4 条并发足够快又不会打满连接池 */
  concurrency?: number
  /** 失败明细里展示的标题 */
  labelOf?: (row: T) => string
  /** 整批结束后的回调（通常用来刷新列表） */
  onFinished?: (payload: { action: BatchAction; succeeded: number; failures: BatchFailure[] }) => void
  /** 批量动作的中文名，用于提示语 */
  actionLabel?: Record<BatchAction, string>
}

/**
 * 审核页面的批量执行器。
 *
 * 设计取舍：不新增后端批量接口，前端用「并发池 + 单条接口」实现。
 * 好处是刷新页面即生效、不依赖后端重启，几十到几百条的积压量完全够用；
 * 每条独立成败，部分失败不会让整批回滚，失败项会保持选中方便直接重试。
 */
export function useBatchAudit<T extends { id: number }>(options: BatchAuditOptions<T>) {
  const { handler, concurrency = 4, labelOf, onFinished } = options
  const actionLabel = options.actionLabel || { approve: '通过', reject: '拒绝', delete: '删除' }

  const rows = ref<T[]>([]) as Ref<T[]>
  const running = ref(false)
  const done = ref(0)
  const queued = ref(0)
  const failures = ref<BatchFailure[]>([])

  const count = computed(() => rows.value.length)
  const percent = computed(() => (queued.value ? Math.floor((done.value / queued.value) * 100) : 0))
  const succeeded = computed(() => Math.max(0, done.value - failures.value.length))
  const failedCount = computed(() => failures.value.length)

  function setSelection(list: T[]) {
    // 执行过程中表格会刷新并触发 selection-change，这里必须忽略，否则选中态会被清空
    if (running.value) return
    rows.value = list
  }

  function clearSelection() {
    rows.value = []
  }

  async function runPool(items: T[], worker: (row: T) => Promise<void>) {
    let cursor = 0
    const size = Math.max(1, Math.min(concurrency, items.length))
    await Promise.all(
      Array.from({ length: size }, async () => {
        while (cursor < items.length) {
          const row = items[cursor]
          cursor += 1
          await worker(row)
        }
      })
    )
  }

  /**
   * @param filter 过滤出真正要处理的行（例如批量通过时跳过已通过的），
   *               被跳过的条数会单独提示，不计入成功也不计入失败。
   */
  async function run(action: BatchAction, reason?: string, filter?: (row: T) => boolean) {
    if (running.value || !rows.value.length) return
    const targets = filter ? rows.value.filter(filter) : [...rows.value]
    const skipped = rows.value.length - targets.length
    if (!targets.length) {
      ElMessage.warning(`选中的 ${skipped} 条当前状态不支持「${actionLabel[action]}」`)
      return
    }

    running.value = true
    failures.value = []
    done.value = 0
    queued.value = targets.length

    await runPool(targets, async (row) => {
      try {
        await handler(row.id, action, reason)
      } catch (e: any) {
        failures.value.push({
          id: row.id,
          label: labelOf ? labelOf(row) : `#${row.id}`,
          message: e?.message || '请求失败'
        })
      } finally {
        done.value += 1
      }
    })

    running.value = false

    const ok = targets.length - failures.value.length
    const parts = [`${actionLabel[action]}成功 ${ok} 条`]
    if (failures.value.length) parts.push(`失败 ${failures.value.length} 条`)
    if (skipped) parts.push(`跳过 ${skipped} 条（状态不符）`)
    const text = parts.join('，')

    if (failures.value.length) {
      ElMessage.warning(text)
    } else {
      ElMessage.success(text)
    }

    onFinished?.({ action, succeeded: ok, failures: failures.value })

    // 选中一律清空：列表刷新后行对象会换掉，残留旧引用反而会误导。
    // 失败明细保留在 failures 里，页面可用 failedIds 重新勾选后重试。
    rows.value = []
  }

  /** 失败项 id 集合，供页面「仅留失败项」重新勾选 */
  const failedIds = computed(() => new Set(failures.value.map((f) => f.id)))

  function clearFailures() {
    failures.value = []
  }

  return {
    rows,
    count,
    running,
    done,
    queued,
    percent,
    succeeded,
    failures,
    failedIds,
    failedCount,
    setSelection,
    clearSelection,
    clearFailures,
    run
  }
}

/** 批量拒绝时用的常用理由，减少重复输入 */
export const REJECT_PRESETS = [
  '内容不符合本站规范',
  '疑似非原创或来源不明',
  '格式与字段要求不符',
  '与已有条目重复',
  '含敏感或不适宜内容'
]

/**
 * 批量拒绝的统一理由输入。
 * 用 prompt 而不是自建弹窗：Element Plus 的 prompt 支持校验与预设输入，
 * 少一层组件状态，且行为与单条拒绝保持一致。
 */
export async function askBatchReason(action: BatchAction, count: number): Promise<string | null> {
  const verb = action === 'delete' ? '删除' : '拒绝'
  try {
    const { value } = await ElMessageBox.prompt(
      `将对选中的 ${count} 条统一${verb}，理由会写入每一条的审核备注。`,
      `批量${verb}`,
      {
        confirmButtonText: `确认${verb}`,
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: `请填写${verb}理由（必填）`,
        inputValidator: (v: string) => (v && v.trim().length >= 2 ? true : '请填写至少 2 个字的理由'),
        type: 'warning',
        // 记住上次输入，批量审同一批内容时不用反复敲
        inputValue: sessionStorage.getItem('audit:lastReason') || ''
      }
    )
    sessionStorage.setItem('audit:lastReason', value)
    return value
  } catch {
    return null
  }
}

/** 批量通过前的确认，避免误点一次性过掉几十条 */
export async function confirmBatch(action: BatchAction, count: number): Promise<boolean> {
  const verb = action === 'delete' ? '删除' : '通过'
  try {
    await ElMessageBox.confirm(
      action === 'delete'
        ? `将永久删除选中的 ${count} 条，删除后不可恢复。`
        : `将把选中的 ${count} 条一次性${verb}。`,
      `批量${verb}`,
      {
        confirmButtonText: `确认${verb}`,
        cancelButtonText: '取消',
        type: action === 'delete' ? 'error' : 'warning'
      }
    )
    return true
  } catch {
    return false
  }
}
