<script setup lang="ts">
import AiChatWindow from '@/components/common/AiChatWindow.vue'
import AiChatFab from '@/components/common/AiChatFab.vue'
</script>

<template>
  <router-view />
  <!-- 全站 AI 小窗：挂在路由视图之外，跨路由不销毁 -->
  <AiChatFab />
  <AiChatWindow />
</template>

<style>
html, body {
  margin: 0;
  padding: 0;
  border: 0;
  width: 100%;
  min-height: 100vh;
}

body {
  overflow-x: clip;
}

#app {
  width: 100%;
  min-height: 100vh;
}
</style>
