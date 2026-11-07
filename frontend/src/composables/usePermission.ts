import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import type { Role, PermissionConfig } from '@/utils/permission'
import {
  ROLE_LABELS,
  resolveRole,
  hasRole,
  hasAnyRole,
  hasPermission,
  hasAnyPermission,
  hasAllPermissions,
  checkPermission
} from '@/utils/permission'

export function usePermission() {
  const userStore = useUserStore()

  const currentRole = computed<Role>(() => resolveRole())
  const roleName = computed(() => ROLE_LABELS[currentRole.value])
  const isAdmin = computed(() => currentRole.value === 'admin')
  const isReviewer = computed(() => currentRole.value === 'reviewer')
  const isPoet = computed(() => currentRole.value === 'poet')
  const isGuest = computed(() => currentRole.value === 'guest')
  const isLoggedIn = computed(() => userStore.isLoggedIn)

  function checkRole(role: Role): boolean {
    return hasRole(role)
  }

  function checkAnyRole(roles: Role[]): boolean {
    return hasAnyRole(roles)
  }

  function checkHasPermission(permission: string): boolean {
    return hasPermission(permission)
  }

  function checkAnyPermission(permissions: string[]): boolean {
    return hasAnyPermission(permissions)
  }

  function checkAllPermissions(permissions: string[]): boolean {
    return hasAllPermissions(permissions)
  }

  function check(config: PermissionConfig): boolean {
    return checkPermission(config)
  }

  return {
    currentRole,
    roleName,
    isAdmin,
    isReviewer,
    isPoet,
    isGuest,
    isLoggedIn,
    checkRole,
    checkAnyRole,
    checkHasPermission,
    checkAnyPermission,
    checkAllPermissions,
    check
  }
}
