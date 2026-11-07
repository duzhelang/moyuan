/** 审核工作台相关的共享类型 */

export interface BatchBarAction {
  key: string
  label: string
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  /** 高风险动作显示为描边红，配合二次确认 */
  plain?: boolean
}

/** 列表页状态筛选选项：全部用空串占位（Element Plus 的 value 不接受 null） */
export interface StatusOption {
  label: string
  value: number | ''
}
