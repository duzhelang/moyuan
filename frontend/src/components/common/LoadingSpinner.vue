<script setup lang="ts">
defineProps<{
  loading?: boolean
  text?: string
}>()
</script>

<template>
  <div class="loading-spinner" v-if="loading">
    <div class="spinner">
      <div class="bounce1"></div>
      <div class="bounce2"></div>
      <div class="bounce3"></div>
    </div>
    <p class="loading-text" v-if="text">{{ text }}</p>
  </div>
</template>

<style scoped lang="scss">
.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-xl;
}

.spinner {
  display: flex;
  gap: $spacing-sm;
}

.spinner > div {
  width: 12px;
  height: 12px;
  background-color: $primary-color;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.bounce1 {
  animation-delay: -0.32s;
}

.bounce2 {
  animation-delay: -0.16s;
}

.loading-text {
  margin-top: $spacing-md;
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}
</style>