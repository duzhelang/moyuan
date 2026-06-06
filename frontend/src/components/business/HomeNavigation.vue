<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHomeNavigationList } from '@/api/modules/admin'

interface NavigationItem {
  id: number
  type: string
  title: string
  subtitle?: string
  imageUrl: string
  linkId: number
  linkType: string
  sortOrder: number
}

const props = defineProps<{
  type: 'works' | 'genres' | 'dynasties'
  title: string
  animation?: 'wave' | 'fade' | 'slide'
}>()

const VISIBLE_COUNT = 4
const WAVE_DELAY = 350
const PLACEHOLDER_IMAGE = `data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='290' height='150' viewBox='0 0 290 150'><rect width='290' height='150' fill='%23e3c4a5'/><text x='145' y='82' font-family='KaiTi,STKaiti,serif' font-size='32' fill='%238b6f47' text-anchor='middle' dominant-baseline='middle'>墨渊</text></svg>`

const animationType = computed(() => props.animation || (props.type === 'genres' ? 'fade' : 'wave'))
const cycleInterval = props.type === 'dynasties' ? 10000 : props.type === 'genres' ? 8000 : 6000

const router = useRouter()
const allItems = ref<NavigationItem[]>([])
const visibleItems = ref<NavigationItem[]>([])
const breathing = ref([false, false, false, false])
const fading = ref([false, false, false, false])
const sliding = ref([false, false, false, false])
const switchTimer = ref<ReturnType<typeof setInterval> | null>(null)
const isHovering = ref(false)
const isSwitching = ref(false)
const failedImageIds = ref<Record<number, boolean>>({})
const isFastMode = ref(false)
const recentlyShownIds = ref<Set<number>>(new Set())

const getImageSrc = (item: NavigationItem): string => {
  if (failedImageIds.value[item.id]) return PLACEHOLDER_IMAGE
  if (!item.imageUrl) return PLACEHOLDER_IMAGE
  return `/img/${item.imageUrl}`
}

const handleImageError = (item: NavigationItem) => {
  failedImageIds.value = { ...failedImageIds.value, [item.id]: true }
}

const dedupItems = (items: NavigationItem[]): NavigationItem[] => {
  const seen = new Set<number>()
  return items.filter(item => {
    const key = item.linkId ?? item.id
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

const fetchItems = async () => {
  try {
    const res = await getHomeNavigationList({ type: props.type })
    allItems.value = dedupItems(res.data || [])
    pickRandomItems()
  } catch (error) {
    console.error(`获取${props.title}数据失败:`, error)
    allItems.value = []
    visibleItems.value = []
  }
}

const shuffleArray = <T,>(arr: T[]): T[] => {
  const copy = [...arr]
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[copy[i], copy[j]] = [copy[j], copy[i]]
  }
  return copy
}

const pickRandomItems = () => {
  if (allItems.value.length <= VISIBLE_COUNT) {
    visibleItems.value = [...allItems.value]
    return
  }
  const shuffled = shuffleArray(allItems.value)
  visibleItems.value = shuffled.slice(0, VISIBLE_COUNT)
  recentlyShownIds.value = new Set(visibleItems.value.map(i => i.id))
}

const getNextItem = (currentIndex: number): NavigationItem => {
  const currentVisibleIds = new Set(visibleItems.value.map(v => v.id))
  const excludeIds = new Set([...currentVisibleIds, ...recentlyShownIds.value])

  const freshCandidates = allItems.value.filter(item => !excludeIds.has(item.id))
  if (freshCandidates.length > 0) {
    const chosen = freshCandidates[Math.floor(Math.random() * freshCandidates.length)]
    recentlyShownIds.value = new Set([...recentlyShownIds.value, chosen.id])
    return chosen
  }

  const visibleOnlyCandidates = allItems.value.filter(item => !currentVisibleIds.has(item.id))
  if (visibleOnlyCandidates.length > 0) {
    recentlyShownIds.value = new Set()
    const chosen = visibleOnlyCandidates[Math.floor(Math.random() * visibleOnlyCandidates.length)]
    recentlyShownIds.value = new Set([chosen.id])
    return chosen
  }

  return visibleItems.value[currentIndex]
}

const waveSwitch = (direction: 'left' | 'right' | 'random' = 'random', fast = false) => {
  if (isSwitching.value) return
  isSwitching.value = true

  const waveDelay = fast ? 120 : WAVE_DELAY
  const breathIn = fast ? 150 : 350
  const breathOut = fast ? 150 : 300

  let order: number[]
  if (direction === 'left') {
    order = [3, 2, 1, 0]
  } else if (direction === 'right') {
    order = [0, 1, 2, 3]
  } else {
    order = [0, 1, 2, 3]
    for (let i = order.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1))
      ;[order[i], order[j]] = [order[j], order[i]]
    }
  }

  order.forEach((idx, step) => {
    setTimeout(() => {
      breathing.value[idx] = true
      setTimeout(() => {
        const newItem = getNextItem(idx)
        visibleItems.value[idx] = newItem
        setTimeout(() => {
          breathing.value[idx] = false
          if (step === order.length - 1) {
            isSwitching.value = false
          }
        }, breathOut)
      }, breathIn)
    }, step * waveDelay)
  })
}

const fadeSwitch = (direction: 'left' | 'right' | 'random' = 'random', fast = false) => {
  if (isSwitching.value) return
  isSwitching.value = true

  const stepDelay = fast ? 80 : 200
  const fadeTime = fast ? 150 : 400

  const order = direction === 'left' ? [3, 2, 1, 0] : direction === 'right' ? [0, 1, 2, 3] : [0, 1, 2, 3]

  order.forEach((idx, step) => {
    setTimeout(() => {
      fading.value[idx] = true
      setTimeout(() => {
        const newItem = getNextItem(idx)
        visibleItems.value[idx] = newItem
        setTimeout(() => {
          fading.value[idx] = false
          if (step === order.length - 1) {
            isSwitching.value = false
          }
        }, fadeTime)
      }, fadeTime)
    }, step * stepDelay)
  })
}

const slideSwitch = (direction: 'left' | 'right' | 'random' = 'random', fast = false) => {
  if (isSwitching.value) return
  isSwitching.value = true

  const stepDelay = fast ? 100 : 250
  const slideTime = fast ? 200 : 500

  const order = direction === 'left' ? [3, 2, 1, 0] : direction === 'right' ? [0, 1, 2, 3] : [0, 1, 2, 3]

  order.forEach((idx, step) => {
    setTimeout(() => {
      sliding.value[idx] = true
      setTimeout(() => {
        const newItem = getNextItem(idx)
        visibleItems.value[idx] = newItem
        setTimeout(() => {
          sliding.value[idx] = false
          if (step === order.length - 1) {
            isSwitching.value = false
          }
        }, slideTime)
      }, slideTime)
    }, step * stepDelay)
  })
}

const switchCards = (direction: 'left' | 'right' | 'random' = 'random', fast = false) => {
  if (fast) isFastMode.value = true
  if (animationType.value === 'fade') {
    fadeSwitch(direction, fast)
  } else if (animationType.value === 'slide') {
    slideSwitch(direction, fast)
  } else {
    waveSwitch(direction, fast)
  }
  if (fast) {
    setTimeout(() => { isFastMode.value = false }, 1200)
  }
}

const handleArrowClick = (direction: 'left' | 'right') => {
  switchCards(direction, true)
}

const handleMouseEnter = () => {
  isHovering.value = true
}

const handleMouseLeave = () => {
  isHovering.value = false
}

const handleClick = (item: NavigationItem) => {
  if (item.linkType === 'poem') {
    router.push(`/poem/${item.linkId}`)
  } else if (item.linkType === 'poet') {
    router.push(`/poet/${item.linkId}`)
  } else if (item.linkType === 'category') {
    router.push({ path: '/poem', query: { categoryId: String(item.linkId) } })
  } else if (item.linkType === 'dynasty') {
    router.push({ path: '/poem', query: { dynastyId: String(item.linkId) } })
  }
}

const startAutoSwitch = () => {
  switchTimer.value = setInterval(() => {
    if (!isHovering.value) {
      switchCards()
    }
  }, cycleInterval)
}

onMounted(() => {
  fetchItems()
  setTimeout(startAutoSwitch, 3000)
})

onUnmounted(() => {
  if (switchTimer.value) {
    clearInterval(switchTimer.value)
    switchTimer.value = null
  }
})
</script>

<template>
  <div class="h6" @mouseenter="handleMouseEnter" @mouseleave="handleMouseLeave">
    <h1 class="nav-title">{{ title }}</h1>
    <div class="scrollableDiv">
      <button class="arrow-btn arrow-left" @click.stop="handleArrowClick('left')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <button class="arrow-btn arrow-right" @click.stop="handleArrowClick('right')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 6 15 12 9 18"/></svg>
      </button>
      <div class="nav-grid" :class="{ 'fast-mode': isFastMode }">
        <template v-for="(item, idx) in visibleItems" :key="(item?.id ?? idx) + '-' + idx">
        <div
          v-if="item"
          class="nav-card"
          :class="{ breathing: breathing[idx], fading: fading[idx], sliding: sliding[idx] }"
          @click="handleClick(item)"
        >
          <img :src="getImageSrc(item)" :alt="item.title" @error="handleImageError(item)">
          <li>{{ item.title }}</li>
          <p v-if="item.subtitle">{{ item.subtitle }}</p>
        </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.h6 {
  position: relative;
  height: auto !important;
  width: 100% !important;
  margin: 15px auto 0 !important;
  display: flex;
  align-items: flex-start;
}

.nav-title {
  font-family: cursive;
  font-size: 50px;
  font-weight: 600;
  color: #333;
  margin: 8px 0 0 10px;
  display: block !important;
  width: auto !important;
  text-align: left !important;
  writing-mode: vertical-rl;
  text-orientation: mixed;
  white-space: nowrap;
  flex-shrink: 0;
}

.scrollableDiv {
  position: relative;
  overflow: hidden;
  flex: 1;
  min-width: 0;
}

.arrow-btn {
  position: absolute;
  top: 0;
  z-index: 31;
  cursor: pointer;
  border: none;
  background: linear-gradient(to right, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.4));
  backdrop-filter: blur(4px);
  width: 36px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
  color: #666;
}

.scrollableDiv:hover .arrow-btn {
  opacity: 1;
}

.arrow-left {
  left: 0;
  border-radius: 8px 0 0 8px;
  background: linear-gradient(to right, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0));
}

.arrow-right {
  right: 0;
  border-radius: 0 8px 8px 0;
  background: linear-gradient(to left, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0));
}

.arrow-btn:hover {
  color: #222;
  background: linear-gradient(to right, rgba(255, 255, 255, 1), rgba(255, 255, 255, 0.3));
}

.arrow-left:hover {
  width: 44px;
  background: linear-gradient(to right, rgba(255, 255, 255, 1), rgba(255, 255, 255, 0));
}

.arrow-right:hover {
  width: 44px;
  background: linear-gradient(to left, rgba(255, 255, 255, 1), rgba(255, 255, 255, 0));
}

.arrow-btn:active {
  transform: scale(0.97);
}

.arrow-btn svg {
  width: 22px;
  height: 22px;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.15));
}

.nav-grid {
  display: grid !important;
  grid-template-columns: repeat(4, 1fr);
  gap: 5px;
  padding: 0 35px;
  flex-direction: row !important;
  flex-wrap: nowrap !important;
  align-items: center !important;
  justify-content: center !important;
  border-radius: 0 !important;
  margin: 0 !important;
}

.nav-card {
  width: 295px;
  min-width: 295px;
  height: 200px;
  text-align: center;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.4s ease;
  transform-origin: center bottom;
}

.fast-mode .nav-card {
  transition: transform 0.15s ease, opacity 0.15s ease;
}

.fast-mode .nav-card.sliding {
  transition: transform 0.15s ease, opacity 0.15s ease;
}

.nav-grid > .nav-card {
  display: block !important;
  flex-direction: initial !important;
  align-items: initial !important;
  justify-content: initial !important;
  flex-wrap: initial !important;
}

.nav-card.breathing {
  transform: scale(0.88);
  opacity: 0.35;
}

.nav-card:hover {
  transform: scale(1.05);
}

.nav-card.breathing:hover {
  transform: scale(0.88);
}

.nav-card.fading {
  opacity: 0;
  transform: scale(0.95);
}

.nav-card.fading:hover {
  transform: scale(0.95);
}

.nav-card.sliding {
  opacity: 0;
  transform: translateX(60px) scale(0.9);
}

.nav-card.sliding:hover {
  transform: translateX(60px) scale(0.9);
}

.nav-card img {
  border-radius: 5% !important;
  width: 290px !important;
  height: 150px !important;
  object-fit: cover !important;
  display: block !important;
  transition: linear 0.3s;
}

.fast-mode .nav-card img {
  transition: linear 0.15s;
}

.nav-card:hover img {
  transform: scale(1.05);
  border-radius: 20% !important;
}

.nav-card.breathing img {
  border-radius: 50% !important;
  transform: scale(0.9);
  filter: blur(1px) brightness(0.9);
}

.nav-card.fading img {
  filter: blur(2px) brightness(0.8);
  transform: scale(0.95);
}

.nav-card li {
  display: block;
  text-align: center;
  list-style: none;
  font-size: 16px;
  font-weight: 500;
  margin-top: 8px;
  transition: color 0.3s ease;
}

.nav-card:hover li {
  color: #409eff;
}

.nav-card p {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
  transition: color 0.3s ease;
}

.nav-card:hover p {
  color: #333;
}
</style>