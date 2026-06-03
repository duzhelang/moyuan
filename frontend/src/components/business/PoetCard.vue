<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

interface PoetData {
  id: number
  poetId?: number | null
  name: string
  dynasty?: string
  description: string
  biography: string
  imageUrl: string
}

const PLACEHOLDER_IMAGE = `data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='250' height='240' viewBox='0 0 250 240'><rect width='250' height='240' fill='%23e3c4a5'/><text x='125' y='110' font-family='KaiTi,STKaiti,serif' font-size='28' fill='%238b6f47' text-anchor='middle'>墨渊</text><text x='125' y='145' font-family='KaiTi,STKaiti,serif' font-size='16' fill='%238b6f47' text-anchor='middle'>诗韵悠长</text></svg>`

const props = defineProps<{
  poet: PoetData
}>()

const router = useRouter()
const isFlipped = ref(false)
const imageFailed = ref(false)

const imageSrc = computed(() => {
  if (imageFailed.value || !props.poet.imageUrl) return PLACEHOLDER_IMAGE
  return props.poet.imageUrl
})

const onImageError = () => {
  imageFailed.value = true
}

const dynastyMap: Record<string, string> = {
  '唐': 'tang', '宋': 'song', '元': 'yuan', '明': 'ming', '清': 'qing',
  '先秦': 'pre-qin', '汉': 'han', '魏晋': 'wei-jin', '南北朝': 'nan-bei',
  '隋': 'sui', '五代十国': 'five-dynasties', '近代': 'modern'
}

const dynastyClass = computed(() => {
  return dynastyMap[props.poet.dynasty ?? ''] ?? 'other'
})

const truncatedBiography = computed(() => {
  const bio = props.poet.biography || props.poet.description || ''
  return bio.length > 120 ? bio.slice(0, 120) + '...' : bio
})

const handleClick = () => {
  if (props.poet.poetId) {
    router.push(`/poet/${props.poet.poetId}`)
  }
}
</script>

<template>
  <div
    class="poet-flip-card"
    @mouseenter="isFlipped = true"
    @mouseleave="isFlipped = false"
    @click="handleClick"
  >
    <div class="poet-flip-inner" :class="{ flipped: isFlipped }">
      <div class="poet-flip-front">
        <img :src="imageSrc" :alt="poet.name" class="poet-flip-img" @error="onImageError">
        <div class="poet-flip-img-overlay"></div>
        <div class="poet-flip-img-name">
          <span v-if="poet.dynasty" :class="['poet-flip-img-dynasty', `dynasty-${dynastyClass}`]">{{ poet.dynasty }}</span>
          <span class="poet-flip-img-text">{{ poet.name }}</span>
        </div>
        <div class="poet-flip-front-info">
          <p v-if="poet.description" class="poet-flip-brief">{{ poet.description }}</p>
        </div>
      </div>
      <div class="poet-flip-back">
        <div class="poet-flip-back-header">
          <span class="poet-flip-back-name">{{ poet.name }}</span>
          <span v-if="poet.dynasty" class="poet-flip-back-dynasty">{{ poet.dynasty }}</span>
        </div>
        <div class="poet-flip-back-divider"></div>
        <p class="poet-flip-back-bio">{{ truncatedBiography }}</p>
        <span class="poet-flip-cta">点击查看详情 →</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.poet-flip-card {
  width: 250px;
  height: 310px;
  perspective: 800px;
  cursor: pointer;
  margin: 0 12px;
  flex-shrink: 0;
  animation: poetCardEnter 0.6s cubic-bezier(0.23, 1, 0.32, 1) both;
}

.poet-flip-card:nth-child(2) { animation-delay: 0.08s; }
.poet-flip-card:nth-child(3) { animation-delay: 0.16s; }
.poet-flip-card:nth-child(4) { animation-delay: 0.24s; }
.poet-flip-card:nth-child(5) { animation-delay: 0.32s; }

@keyframes poetCardEnter {
  from {
    opacity: 0;
    transform: translateY(24px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.poet-flip-inner {
  width: 100%;
  height: 100%;
  position: relative;
  transform-style: preserve-3d;
  transition: transform 0.7s cubic-bezier(0.4, 0, 0.2, 1);
}

.poet-flip-inner.flipped {
  transform: rotateY(180deg);
}

.poet-flip-front,
.poet-flip-back {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  border-radius: 12px;
  overflow: hidden;
}

.poet-flip-front {
  display: flex;
  flex-direction: column;
  background: #1a1a1a;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  transition: box-shadow 0.4s ease;
}

.poet-flip-card:hover .poet-flip-front {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.25);
}

.poet-flip-img {
  width: 100%;
  height: 240px;
  object-fit: cover;
  display: block;
  flex-shrink: 0;
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.poet-flip-card:hover .poet-flip-img {
  transform: scale(1.05);
}

.poet-flip-img-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 240px;
  background: linear-gradient(
    to bottom,
    transparent 55%,
    rgba(0, 0, 0, 0.3) 80%,
    rgba(0, 0, 0, 0.55) 100%
  );
  pointer-events: none;
}

.poet-flip-img-name {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 2px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  z-index: 2;
  pointer-events: none;
}

.poet-flip-img-dynasty {
  display: inline-block;
  padding: 1px 7px;
  border-radius: 20px;
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.5px;
  line-height: 1.5;
  flex-shrink: 0;
}

.poet-flip-img-name .dynasty-tang { background: rgba(210, 140, 80, 0.8); color: #fff; }
.poet-flip-img-name .dynasty-song { background: rgba(60, 160, 60, 0.75); color: #fff; }
.poet-flip-img-name .dynasty-yuan { background: rgba(200, 60, 60, 0.75); color: #fff; }
.poet-flip-img-name .dynasty-ming { background: rgba(160, 30, 30, 0.75); color: #fff; }
.poet-flip-img-name .dynasty-qing { background: rgba(80, 140, 200, 0.75); color: #fff; }
.poet-flip-img-name .dynasty-pre-qin { background: rgba(140, 140, 30, 0.75); color: #fff; }
.poet-flip-img-name .dynasty-han { background: rgba(200, 150, 30, 0.75); color: #fff; }
.poet-flip-img-name .dynasty-wei-jin { background: rgba(120, 100, 220, 0.75); color: #fff; }
.poet-flip-img-name .dynasty-nan-bei { background: rgba(20, 140, 140, 0.75); color: #fff; }
.poet-flip-img-name .dynasty-sui { background: rgba(220, 120, 50, 0.75); color: #fff; }
.poet-flip-img-name .dynasty-five-dynasties { background: rgba(140, 30, 140, 0.75); color: #fff; }
.poet-flip-img-name .dynasty-modern { background: rgba(90, 20, 150, 0.75); color: #fff; }
.poet-flip-img-name .dynasty-other { background: rgba(110, 110, 110, 0.75); color: #fff; }

.poet-flip-img-text {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  font-family: cursive;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.6);
  line-height: 1.2;
}

.poet-flip-front-info {
  flex: 1;
  padding: 8px 14px;
  display: flex;
  align-items: center;
  position: relative;
  z-index: 1;
  background: linear-gradient(135deg, rgba(227, 196, 165, 0.6), rgba(210, 180, 145, 0.4));
}

.poet-flip-brief {
  margin: 0;
  font-size: 12px;
  color: #555;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  text-overflow: ellipsis;
}

.poet-flip-back {
  transform: rotateY(180deg);
  display: flex;
  flex-direction: column;
  padding: 24px 20px;
  background: linear-gradient(145deg, #f5efe6, #e8ddd0);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.poet-flip-back-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 8px;
}

.poet-flip-back-name {
  font-size: 22px;
  font-weight: 700;
  color: #3a2a1a;
  font-family: cursive;
}

.poet-flip-back-dynasty {
  font-size: 13px;
  color: #8B6914;
  font-weight: 500;
}

.poet-flip-back-divider {
  width: 40px;
  height: 2px;
  background: linear-gradient(to right, #8B4513, transparent);
  margin-bottom: 14px;
}

.poet-flip-back-bio {
  margin: 0;
  font-size: 13px;
  color: #5a4a3a;
  line-height: 1.7;
  flex: 1;
  overflow: hidden;
}

.poet-flip-cta {
  margin-top: 12px;
  font-size: 13px;
  color: #8B4513;
  font-weight: 600;
  text-align: right;
  transition: color 0.3s ease;
}

.poet-flip-card:hover .poet-flip-cta {
  color: #A0522D;
}
</style>
