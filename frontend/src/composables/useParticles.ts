import { ref, onMounted, onUnmounted, type Ref } from 'vue'

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  size: number
  opacity: number
  color: string
}

interface ParticleOptions {
  count?: number
  colors?: string[]
  opacityRange?: [number, number]
  sizeRange?: [number, number]
  connectionDistance?: number
}

export function useParticles(canvasRef: Ref<HTMLCanvasElement | null>, options: ParticleOptions = {}) {
  const {
    count = 80,
    colors = ['#d4af87', '#f0e4d7', '#c9a06c'],
    opacityRange = [0.25, 0.45],
    sizeRange = [1.5, 3],
    connectionDistance = 150
  } = options

  const animationId = ref<number>(0)
  let particles: Particle[] = []

  const createParticles = (canvas: HTMLCanvasElement): Particle[] => {
    const result: Particle[] = []
    for (let i = 0; i < count; i++) {
      result.push({
        x: Math.random() * canvas.width,
        y: Math.random() * canvas.height,
        vx: (Math.random() - 0.5) * 0.5,
        vy: (Math.random() - 0.5) * 0.5,
        size: Math.random() * sizeRange[1] + sizeRange[0],
        opacity: Math.random() * (opacityRange[1] - opacityRange[0]) + opacityRange[0],
        color: colors[Math.floor(Math.random() * colors.length)]
      })
    }
    return result
  }

  const updateParticles = (canvas: HTMLCanvasElement) => {
    particles.forEach(p => {
      p.x += p.vx
      p.y += p.vy

      if (p.x < 0 || p.x > canvas.width) p.vx *= -1
      if (p.y < 0 || p.y > canvas.height) p.vy *= -1

      p.x = Math.max(0, Math.min(canvas.width, p.x))
      p.y = Math.max(0, Math.min(canvas.height, p.y))
    })
  }

  const drawParticles = (ctx: CanvasRenderingContext2D) => {
    ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height)

    particles.forEach(p => {
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
      ctx.fillStyle = p.color
      ctx.globalAlpha = p.opacity
      ctx.fill()
      ctx.globalAlpha = 1
    })

    const avgOpacity = particles.reduce((sum, p) => sum + p.opacity, 0) / particles.length

    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const distance = Math.sqrt(dx * dx + dy * dy)

        if (distance < connectionDistance) {
          ctx.beginPath()
          ctx.strokeStyle = particles[i].color
          ctx.globalAlpha = avgOpacity * 0.5 * (1 - distance / connectionDistance)
          ctx.lineWidth = 1
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.stroke()
          ctx.globalAlpha = 1
        }
      }
    }
  }

  const startAnimation = (canvas: HTMLCanvasElement) => {
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const animate = () => {
      updateParticles(canvas)
      drawParticles(ctx)
      animationId.value = requestAnimationFrame(animate)
    }
    animate()
  }

  const initCanvas = () => {
    const canvas = canvasRef.value
    if (!canvas) return

    const container = canvas.parentElement
    if (!container) return

    canvas.width = container.offsetWidth
    canvas.height = container.offsetHeight

    particles = createParticles(canvas)
    startAnimation(canvas)
  }

  const handleResize = () => {
    const canvas = canvasRef.value
    if (!canvas) return

    const container = canvas.parentElement
    if (!container) return

    canvas.width = container.offsetWidth
    canvas.height = container.offsetHeight
  }

  onMounted(() => {
    setTimeout(() => {
      initCanvas()
    }, 100)
    window.addEventListener('resize', handleResize)
  })

  onUnmounted(() => {
    if (animationId.value) {
      cancelAnimationFrame(animationId.value)
    }
    window.removeEventListener('resize', handleResize)
  })

  return {
    canvasRef
  }
}