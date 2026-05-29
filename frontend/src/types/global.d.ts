declare global {
  interface Window {
    $message: any
    $dialog: any
    $loading: any
  }
}

export {}

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    keepAlive?: boolean
    hidden?: boolean
    icon?: string
    roles?: string[]
    requiresAdmin?: boolean
  }
}