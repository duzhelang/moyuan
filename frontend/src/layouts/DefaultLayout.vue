<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'
import AppFooter from '@/components/common/AppFooter.vue'

const route = useRoute()
const showHeader = computed(() => !route.meta?.hideHeader)
</script>

<template>
  <div class="app-wrapper">
    <AppHeader v-if="showHeader" />
    <main class="app-main">
      <router-view />
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.app-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-main {
  flex: 1;
  width: 100%;
  max-width: 1512px;
  margin: 0 auto;
  padding: 0px 0 20px;
  min-height: calc(100vh - 55px);
}
</style>
