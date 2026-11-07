/** 管理后台仪表盘：链路 / 体检 / 待办 的数据结构 */

export type StatusLevel = 'ok' | 'warn' | 'down'

export interface ChainMetric {
  label: string
  value: string
}

/** 系统链路节点 */
export interface ChainNode {
  key: string
  name: string
  status: StatusLevel
  /** 延迟展示值，未接入时用 '—' */
  latency: string
  metrics: ChainMetric[]
  /** 数据来源：true=接口真实数据，false=占位待接入 */
  live: boolean
  /** 节点职责说明，用于悬浮窗底部解释「这个节点是干什么的」 */
  desc?: string
}

/** 首页体检条指标 */
export interface MetricItem {
  key: string
  label: string
  value: string
  status: StatusLevel
  live: boolean
}

/** 运营待办节点 */
export interface TodoItem {
  key: string
  label: string
  count: number
  status: StatusLevel
  hint?: string
  /** 点击下钻的业务页路由 */
  link?: string
}
