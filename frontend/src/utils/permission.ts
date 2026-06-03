import { useUserStore } from '@/stores/user'

export type Role = 'admin' | 'user' | 'guest'

export interface PermissionConfig {
  roles?: Role[]
  permissions?: string[]
  requireAll?: boolean
}

const rolePermissions: Record<Role, string[]> = {
  admin: [
    'user:manage',
    'poem:manage',
    'poet:manage',
    'dynasty:manage',
    'category:manage',
    'forum:manage',
    'vision:manage',
    'system:manage'
  ],
  user: [
    'poem:read',
    'poet:read',
    'forum:read',
    'forum:create',
    'forum:edit:own',
    'comment:create',
    'comment:edit:own',
    'user:profile:edit:own'
  ],
  guest: [
    'poem:read',
    'poet:read',
    'forum:read'
  ]
}

export function hasRole(role: Role): boolean {
  const userStore = useUserStore()
  if (!userStore.userInfo?.role) return false
  return userStore.userInfo.role === role
}

export function hasAnyRole(roles: Role[]): boolean {
  return roles.some(role => hasRole(role))
}

export function hasPermission(permission: string): boolean {
  const userStore = useUserStore()
  if (!userStore.userInfo?.role) return false

  const userRole = userStore.userInfo.role as Role
  const permissions = rolePermissions[userRole] || []

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
