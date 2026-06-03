import type { Directive, DirectiveBinding } from 'vue'
import { hasPermission, hasAnyPermission, hasAllPermissions } from '@/utils/permission'

interface PermissionBinding {
  permission?: string
  permissions?: string[]
  requireAll?: boolean
}

function checkElementPermission(binding: PermissionBinding): boolean {
  const { permission, permissions, requireAll = false } = binding

  if (permission) {
    return hasPermission(permission)
  }

  if (permissions && permissions.length > 0) {
    return requireAll ? hasAllPermissions(permissions) : hasAnyPermission(permissions)
  }

  return true
}

function handlePermission(el: HTMLElement, binding: DirectiveBinding<PermissionBinding | string>) {
  let config: PermissionBinding

  if (typeof binding.value === 'string') {
    config = { permission: binding.value }
  } else {
    config = binding.value || {}
  }

  const hasAccess = checkElementPermission(config)

  if (!hasAccess) {
    el.parentNode?.removeChild(el)
  }
}

export const permission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<PermissionBinding | string>) {
    handlePermission(el, binding)
  },
  updated(el: HTMLElement, binding: DirectiveBinding<PermissionBinding | string>) {
    handlePermission(el, binding)
  }
}

export const permissionOr: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string[]>) {
    const permissions = binding.value || []
    if (!hasAnyPermission(permissions)) {
      el.parentNode?.removeChild(el)
    }
  },
  updated(el: HTMLElement, binding: DirectiveBinding<string[]>) {
    const permissions = binding.value || []
    if (!hasAnyPermission(permissions)) {
      el.parentNode?.removeChild(el)
    }
  }
}

export const permissionAnd: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string[]>) {
    const permissions = binding.value || []
    if (!hasAllPermissions(permissions)) {
      el.parentNode?.removeChild(el)
    }
  },
  updated(el: HTMLElement, binding: DirectiveBinding<string[]>) {
    const permissions = binding.value || []
    if (!hasAllPermissions(permissions)) {
      el.parentNode?.removeChild(el)
    }
  }
}
