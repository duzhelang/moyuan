import { useUserStore } from '@/stores/user'

/**
 * 平台五类角色：
 * - admin    管理员：审核员的上级，负责审核调度、团队与规则管理，可协审
 * - reviewer 审核员：只处理分派给自己或主动认领的工单
 * - poet     认证诗人：通过诗人认证的普通用户，可维护诗人档案与投稿
 * - user     普通用户：浏览、互动、投稿、提交报修与建议
 * - guest    游客：未登录，仅浏览公开内容
 */
export type Role = 'admin' | 'reviewer' | 'poet' | 'user' | 'guest'

export const ROLE_LABELS: Record<Role, string> = {
  admin: '管理员',
  reviewer: '审核员',
  poet: '认证诗人',
  user: '普通用户',
  guest: '游客'
}

export interface PermissionConfig {
  roles?: Role[]
  permissions?: string[]
  requireAll?: boolean
}

const baseUserPermissions = [
  'poem:read',
  'poet:read',
  'forum:read',
  'forum:create',
  'forum:edit:own',
  'comment:create',
  'comment:edit:own',
  'user:profile:edit:own',
  'suggestion:submit',
  'repair:submit'
]

const rolePermissions: Record<Role, string[]> = {
  admin: [
    ...baseUserPermissions,
    'user:manage',
    'poem:manage',
    'poet:manage',
    'dynasty:manage',
    'category:manage',
    'forum:manage',
    'vision:manage',
    'system:manage',
    'audit:board',
    'audit:review',
    'audit:assign',
    'audit:team',
    'audit:rule',
    'repair:manage'
  ],
  reviewer: [
    ...baseUserPermissions,
    'audit:board',
    'audit:review'
  ],
  poet: [
    ...baseUserPermissions,
    'poem:submit',
    'poet:profile:manage'
  ],
  user: baseUserPermissions,
  guest: [
    'poem:read',
    'poet:read',
    'forum:read'
  ]
}

/** 结合诗人认证状态解析当前有效角色 */
export function resolveRole(): Role {
  const userStore = useUserStore()
  const user = userStore.userInfo
  if (!user) {
    return 'guest'
  }
  const role = (user.role || 'user') as Role
  if (role === 'admin' || role === 'reviewer' || role === 'poet') {
    return role
  }
  return user.poetVerified === 1 ? 'poet' : 'user'
}

export function roleLabel(role?: string): string {
  if (!role) {
    return ROLE_LABELS.guest
  }
  return ROLE_LABELS[role as Role] || role
}

export function hasRole(role: Role): boolean {
  return resolveRole() === role
}

export function hasAnyRole(roles: Role[]): boolean {
  const current = resolveRole()
  return roles.includes(current)
}

export function hasPermission(permission: string): boolean {
  const permissions = rolePermissions[resolveRole()] || []
  return permissions.includes(permission)
}

export function hasAnyPermission(permissions: string[]): boolean {
  return permissions.some(permission => hasPermission(permission))
}

export function hasAllPermissions(permissions: string[]): boolean {
  return permissions.every(permission => hasPermission(permission))
}

export function checkPermission(config: PermissionConfig): boolean {
  const { roles, permissions, requireAll = false } = config

  if (roles && roles.length > 0) {
    if (!hasAnyRole(roles)) return false
  }

  if (permissions && permissions.length > 0) {
    if (requireAll) {
      return hasAllPermissions(permissions)
    }
    return hasAnyPermission(permissions)
  }

  return true
}
