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
  padding: 4px 14px;
  border-radius: 4px;
  /* 不加底色框，仅靠文字阴影提升可读性 */
  transition: all 0.3s ease;
  text-shadow: 1px 1px 3px #000, 0 0 6px rgba(0, 0, 0, 0.8);
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
  /* 绝对定位元素默认按父级（触发文字）宽度收缩，需固定为内容宽度，避免二维码被压扁 */
  width: max-content;
}

.qr-popup img {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  object-fit: cover;
  /* 覆盖全局 img{max-width:100%}，否则图片宽度会被父级收缩宽度裁切导致非正方形 */
  max-width: none;
}

.qr-code-link:hover .qr-text {
  color: #ffd700;
  /* 悬停同样不加底色框，仅金色文字 + 光晕 */
  text-shadow: 0 0 8px rgba(255, 215, 0, 0.5);
}

.qr-code-link:hover .qr-popup {
  display: block;
}
</style>
