<script setup lang="ts">
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<template>
  <router-view v-slot="{ Component }">
    <keep-alive :include="appStore.cachedViews">
      <component :is="Component" />
    </keep-alive>
  </router-view>
</template>

<style lang="scss">
#app {
  width: 100%;
  min-height: 100vh;
  font-family: $font-family-base;
  color: $text-color;
  background-color: $background-color;
}
</style>