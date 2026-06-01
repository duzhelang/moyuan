<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getRandomPoem } from '@/api/modules/poem'
import type { Poem } from '@/types/model'

const router = useRouter()
const route = useRoute()

const canvas = ref<HTMLCanvasElement | null>(null)
const eyeOffsetX = ref(0)
const eyeOffsetY = ref(0)
const mouthPath = ref('M35 85 Q60 65 85 85')
const idleTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const isPermissionIssue = ref(false)
const randomPoem = ref<Poem | null>(null)

const handleMouseMove = (e: MouseEvent) => {
  eyeOffsetX.value = (e.clientX / window.innerWidth - 0.5) * 10
  eyeOffsetY.value = (e.clientY / window.innerHeight - 0.5) * 10
  resetIdle()
}

const sigh = () => {
  mouthPath.value = 'M38 90 Q60 55 83 90'
  setTimeout(() => {
    mouthPath.value = 'M35 85 Q60 65 85 85'
  }, 1200)
}

const resetIdle = () => {
  if (idleTimer.value) clearTimeout(idleTimer.value)
  idleTimer.value = setTimeout(sigh, 2000)
}

const goBack = () => {
  mouthPath.value = 'M35 80 Q60 100 85 80'
  setTimeout(() => router.back(), 300)
}

const goHome = () => {
  mouthPath.value = 'M35 80 Q60 100 85 80'
  setTimeout(() => router.push('/'), 300)
}

const goPoem = () => {
  if (randomPoem.value) {
    router.push(`/poem/${randomPoem.value.id}`)
  }
}

const fetchRandomPoem = async () => {
  try {
    const res = await getRandomPoem()
    randomPoem.value = res.data
  } catch {
    randomPoem.value = null
  }
}

const initParticles = () => {
  const c = canvas.value
  if (!c) return
  const ctx = c.getContext('2d')
  if (!ctx) return

  c.width = window.innerWidth
  c.height = window.innerHeight

  const particles = Array.from({ length: 60 }, () => ({
    x: Math.random() * c.width,
    y: Math.random() * c.height,
    r: Math.random() * 2,
    dx: Math.random() - 0.5,
    dy: Math.random() - 0.5
  }))

  let animId = 0
  const draw = () => {
    ctx.clearRect(0, 0, c.width, c.height)
    particles.forEach(p => {
      p.x += p.dx
      p.y += p.dy
      if (p.x < 0 || p.x > c.width) p.dx *= -1
      if (p.y < 0 || p.y > c.height) p.dy *= -1
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = 'rgba(255,255,255,0.45)'
      ctx.fill()
    })
    animId = requestAnimationFrame(draw)
  }
  draw()
  return () => cancelAnimationFrame(animId)
}

let cancelAnim: (() => void) | undefined

onMounted(() => {
  isPermissionIssue.value = route.query.type === 'permission'
  resetIdle()
  cancelAnim = initParticles()
  fetchRandomPoem()
})

onUnmounted(() => {
  if (idleTimer.value) clearTimeout(idleTimer.value)
  cancelAnim?.()
})
</script>

<template>
  <div class="error-page" @mousemove="handleMouseMove">
    <canvas ref="canvas" class="particle-bg"></canvas>

    <div class="error-content">
      <div class="face breathe">
        <svg viewBox="0 0 120 120">
          <circle cx="60" cy="60" r="55" stroke="#fff" stroke-width="10" fill="transparent"/>
          <circle
            class="eye"
            :cx="35 + eyeOffsetX"
            :cy="45 + eyeOffsetY"
            r="10"
            fill="#fff"
          />
          <circle
            class="eye"
            :cx="85 + eyeOffsetX"
            :cy="45 + eyeOffsetY"
            r="10"
            fill="#fff"
          />
          <path
            :d="mouthPath"
            stroke="#fff"
            stroke-width="10"
            fill="none"
            stroke-linecap="round"
            style="transition: d 0.3s ease;"
          />
        </svg>
      </div>

      <h1>{{ isPermissionIssue ? '403' : '404' }}</h1>
      <p v-if="isPermissionIssue">权限不足<br/>您没有权限访问该页面，请联系管理员分配权限</p>
      <p v-else>页面未找到<br/>您访问的页面不存在或发生了其他错误</p>

      <div class="actions">
        <button @click="goBack">返回上一页</button>
        <button @click="goHome">返回首页</button>
      </div>

      <div v-if="randomPoem" class="poem-card" @click="goPoem">
        <p class="poem-content">{{ randomPoem.content.split('\n').slice(0, 4).join('\n') }}</p>
        <p class="poem-info">
          —— {{ randomPoem.poetName ? randomPoem.poetName + ' · ' : '' }}{{ randomPoem.title }}
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.error-page {
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #8B4513, #D2691E);
  position: relative;
}

.particle-bg {
  position: absolute;
  width: 100%;
  height: 100%;
}

.error-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #fff;
  top: 50%;
  transform: translateY(-50%);
  padding: 0 20px;
}

.face {
  width: 120px;
  margin: 0 auto 20px;
  transition: transform 0.3s;
}

.breathe {
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

h1 {
  font-family: 'KaiTi', 'STKaiti', serif;
  font-size: 72px;
  margin: 0 0 10px 0;
}

p {
  margin-bottom: 20px;
  opacity: 0.85;
  line-height: 1.6;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
}

.actions {
  margin-bottom: 30px;
}

.actions button {
  margin: 0 10px;
  padding: 10px 24px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  cursor: pointer;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 14px;
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
}

.actions button:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.05);
}

.poem-card {
  max-width: 480px;
  margin: 0 auto;
  padding: 20px 28px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  cursor: pointer;
  transition: all 0.3s ease;
}

.poem-card:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.poem-content {
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 16px;
  line-height: 2;
  color: rgba(255, 255, 255, 0.9);
  white-space: pre-line;
  margin-bottom: 12px;
}

.poem-info {
  font-family: 'KaiTi', 'STKaiti', serif;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  text-align: right;
  margin-bottom: 0;
}

@media (max-width: 600px) {
  h1 {
    font-size: 48px;
  }

  .poem-card {
    margin: 0 10px;
  }
}
</style>