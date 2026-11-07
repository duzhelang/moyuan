import { onBeforeUnmount, ref, type ComputedRef, type Ref } from 'vue'

interface AuditCursorOptions<T> {
  rows: ComputedRef<T[]> | Ref<T[]>
  tableRef: Ref<any>
  /** A 键：通过当前行 */
  onApprove: (row: T) => void
  /** R 键：拒绝当前行 */
  onReject: (row: T) => void
  /** X 键：勾选 / 取消当前行 */
  onToggle: (row: T) => void
  /** Esc：清空选中 */
  onClear?: () => void
  /** Ctrl+A：全选本页 */
  onSelectAll?: () => void
  /** 弹窗打开时通常要临时停用，避免和输入框抢按键 */
  enabled?: Ref<boolean>
}

/**
 * 键盘游标：审核积压时的主操作方式。
 *   J / ↓ 下一条     K / ↑ 上一条
 *   X 勾选           A 通过        R 拒绝
 *   Esc 清空         Ctrl+A 全选本页
 * 输入框、文本域、下拉里敲字不会被拦截。
 */
export function useAuditCursor<T>(options: AuditCursorOptions<T>) {
  const { rows, tableRef, onApprove, onReject, onToggle, onClear, onSelectAll, enabled } = options
  const activeIndex = ref(-1)

  const rowClassName = ({ rowIndex }: { rowIndex: number }) =>
    rowIndex === activeIndex.value ? 'is-cursor' : ''

  function current(): T | null {
    if (activeIndex.value < 0) return null
    return rows.value[activeIndex.value] || null
  }

  function scrollToActive() {
    const el = tableRef.value?.$el as HTMLElement | undefined
    if (!el) return
    const trs = el.querySelectorAll('.el-table__body-wrapper tbody tr')
    const tr = trs[activeIndex.value] as HTMLElement | undefined
    tr?.scrollIntoView({ block: 'nearest' })
  }

  function move(step: number) {
    const max = rows.value.length - 1
    if (max < 0) return
    const next = activeIndex.value + step
    activeIndex.value = next < 0 ? 0 : next > max ? max : next
    scrollToActive()
  }

  function reset() {
    activeIndex.value = -1
  }

  function isTyping(target: EventTarget | null) {
    const el = target as HTMLElement | null
    if (!el) return false
    const tag = el.tagName
    return (
      tag === 'INPUT' ||
      tag === 'TEXTAREA' ||
      tag === 'SELECT' ||
      el.isContentEditable === true ||
      !!el.closest('.el-select, .el-input, .el-textarea, .el-dialog')
    )
  }

  function onKeydown(event: KeyboardEvent) {
    if (enabled && !enabled.value) return
    if (isTyping(event.target)) return
    if (event.altKey || event.metaKey) return

    const key = event.key

    if ((event.ctrlKey || event.metaKey) && key.toLowerCase() === 'a') {
      if (onSelectAll) {
        event.preventDefault()
        onSelectAll()
      }
      return
    }
    if (event.ctrlKey) return

    switch (key.toLowerCase()) {
      case 'j':
        event.preventDefault()
        move(1)
        break
      case 'arrowdown':
        event.preventDefault()
        move(1)
        break
      case 'k':
        event.preventDefault()
        move(-1)
        break
      case 'arrowup':
        event.preventDefault()
        move(-1)
        break
      case 'x': {
        const row = current()
        if (row) {
          event.preventDefault()
          onToggle(row)
        }
        break
      }
      case 'a': {
        const row = current()
        if (row) {
          event.preventDefault()
          onApprove(row)
        }
        break
      }
      case 'r': {
        const row = current()
        if (row) {
          event.preventDefault()
          onReject(row)
        }
        break
      }
      case 'escape':
        if (onClear) {
          onClear()
          reset()
        }
        break
      default:
        break
    }
  }

  window.addEventListener('keydown', onKeydown)
  onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))

  return { activeIndex, rowClassName, move, reset, current }
}
