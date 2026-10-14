<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

withDefaults(defineProps<{
  compact?: boolean
}>(), {
  compact: false
})

const route = useRoute()
const router = useRouter()

/** 文案取自需求 12.3 游客模式专属提示 */
const goLogin = () => {
  const redirect = encodeURIComponent(route.fullPath || '/')
  router.push(`/user/login?redirect=${redirect}`)
}
</script>

<template>
  <div class="guest-badge" :class="{ 'is-compact': compact }">
    <span class="badge-plate">
      <span class="badge-dot" />
      游客模式 · 战绩不会保存
    </span>
    <button class="badge-login" type="button" @click="goLogin">登录解锁完整对局</button>
  </div>
</template>

<style scoped lang="scss">
.guest-badge {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  flex-wrap: wrap;
}

.badge-plate {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border: 1px solid $border-color-bronze;
  border-radius: $border-radius-md;
  background: rgba($primary-50, 0.9);
  color: $primary-700;
  font-size: $font-size-sm;
  letter-spacing: 0.04em;
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $accent-color;
}

.badge-login {
  padding: 4px 12px;
  border: 1px solid $primary-400;
  border-radius: $border-radius-md;
  background: rgba($surface-primary, 0.95);
  color: $primary-600;
  font-size: $font-size-sm;
  cursor: pointer;
  transition: all $transition-base;
}

.badge-login:hover {
  background: $primary-500;
  border-color: $primary-500;
  color: #fff;
}

.guest-badge.is-compact {
  gap: $spacing-sm;
}

.guest-badge.is-compact .badge-plate {
  font-size: $font-size-xs;
  padding: 2px 8px;
}

.guest-badge.is-compact .badge-login {
  font-size: $font-size-xs;
  padding: 2px 8px;
}
</style>
