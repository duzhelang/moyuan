<script setup lang="ts">
defineProps<{
  text: string
  qrImage: string
  alt?: string
}>()
</script>

<template>
  <div class="qr-code-link">
    <span class="qr-text">{{ text }}</span>
    <div class="qr-popup">
      <img :src="qrImage" :alt="alt || text" />
    </div>
  </div>
</template>

<style scoped>
.qr-code-link {
  position: relative;
  cursor: pointer;
}

.qr-text {
  color: whitesmoke;
  font-size: 18px;
  padding: 4px 12px;
  transition: all 0.3s ease;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.qr-popup {
  display: none;
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  padding: 8px;
  background: rgba(100, 100, 100, 0.6);
  border-radius: 12px;
  z-index: 100;
}

.qr-popup img {
  width: 120px;
  height: 120px;
  border-radius: 8px;
}

.qr-code-link:hover .qr-text {
  font-size: 19px;
  text-shadow: 0 0 10px white;
}

.qr-code-link:hover .qr-popup {
  display: block;
}
</style>
