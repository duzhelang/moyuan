<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { getDynastyList } from '@/api/modules/dynasty'
import { getPoetList } from '@/api/modules/poet'
import { getPoemList } from '@/api/modules/poem'
import { ElMessage } from 'element-plus'
import { Search, Close } from '@element-plus/icons-vue'
import type { Poet, Poem } from '@/types/model'

const router = useRouter()

// ==================== 数据状态 ====================
const dynasties = ref<{ id: number; name: string }[]>([])
const poets = ref<Poet[]>([])
const allPoems = ref<Poem[]>([])
const loading = ref(true)
const selectedDynasty = ref('all')
const selectedPoet = ref<Poet | null>(null)
const poetPoems = ref<Poem[]>([])
const showPanel = ref(false)
const searchQuery = ref('')
const canvasRef = ref<HTMLDivElement | null>(null)

// 诗人位置缓存（诗人id → 3D坐标）
const poetPositionMap = new Map<number, THREE.Vector3>()

// ==================== 朝代颜色映射 ====================
const dynastyColorMap: Record<string, THREE.Color> = {
  '先秦': new THREE.Color('#b8860b'),
  '汉': new THREE.Color('#cd853f'),
  '魏晋': new THREE.Color('#daa520'),
  '南北朝': new THREE.Color('#d2691e'),
  '隋': new THREE.Color('#c97d3a'),
  '唐': new THREE.Color('#ffd700'),
  '宋': new THREE.Color('#ffa500'),
  '元': new THREE.Color('#ff8c00'),
  '明': new THREE.Color('#cd7f32'),
  '清': new THREE.Color('#b87333'),
  '近代': new THREE.Color('#e5a76e'),
  '现代': new THREE.Color('#f0c9a6')
}

const getDynastyColor = (dynasty?: string): THREE.Color => {
  if (!dynasty) return new THREE.Color('#cd853f')
  for (const [key, color] of Object.entries(dynastyColorMap)) {
    if (dynasty.includes(key)) return color.clone()
  }
  return new THREE.Color('#cd853f')
}

const dynastyIndexMap: Record<string, number> = {
  '先秦': 0, '汉': 1, '魏晋': 2, '南北朝': 3, '隋': 4,
  '唐': 5, '宋': 6, '元': 7, '明': 8, '清': 9,
  '近代': 10, '现代': 11
}

const getDynastyShell = (dynasty?: string): number => {
  if (!dynasty) return 6
  for (const [key, idx] of Object.entries(dynastyIndexMap)) {
    if (dynasty.includes(key)) return idx
  }
  return 6
}

// ==================== 诗体分类颜色 ====================
const categoryColorMap: Record<string, THREE.Color> = {
  '五言绝句': new THREE.Color('#6eb5ff'),
  '七言绝句': new THREE.Color('#7dffb3'),
  '五言律诗': new THREE.Color('#b88cff'),
  '七言律诗': new THREE.Color('#ff9f43'),
  '词': new THREE.Color('#ff6b9d'),
  '现代诗': new THREE.Color('#f0c9a6')
}

const getCategoryColor = (cat?: string): THREE.Color => {
  if (!cat) return new THREE.Color('#c8b896')
  for (const [key, color] of Object.entries(categoryColorMap)) {
    if (cat.includes(key)) return color.clone()
  }
  return new THREE.Color('#c8b896')
}

// ==================== Three.js 核心 ====================
let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let renderer: THREE.WebGLRenderer
let controls: OrbitControls
let raycaster: THREE.Raycaster
let mouse: THREE.Vector2
let poetMeshes: THREE.Mesh[] = []
let poetLabels: THREE.Sprite[] = []
let connectionLines: THREE.Line[] = []
let bgStars: THREE.Points
let poemCloud: THREE.Points | null = null
let poemGeometry: THREE.BufferGeometry | null = null
let clock: THREE.Clock
let animationId: number
let hoveredMesh: THREE.Mesh | null = null

const STAR_COUNT_BG = 8000
const SHELL_RADIUS_BASE = 25
const SHELL_RADIUS_STEP = 18

function initScene() {
  if (!canvasRef.value) return

  clock = new THREE.Clock()
  scene = new THREE.Scene()
  scene.fog = new THREE.FogExp2(0x050510, 0.003)

  camera = new THREE.PerspectiveCamera(
    60,
    canvasRef.value.clientWidth / canvasRef.value.clientHeight,
    0.1,
    2000
  )
  camera.position.set(0, 80, 200)

  renderer = new THREE.WebGLRenderer({
    antialias: true,
    alpha: false,
    powerPreference: 'high-performance'
  })
  renderer.setSize(canvasRef.value.clientWidth, canvasRef.value.clientHeight)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setClearColor(0x050510)
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.2
  canvasRef.value.appendChild(renderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.enablePan = true
  controls.panSpeed = 0.8
  controls.rotateSpeed = 0.5
  controls.zoomSpeed = 1.0
  controls.minDistance = 20
  controls.maxDistance = 600
  controls.target.set(0, 0, 0)

  raycaster = new THREE.Raycaster()
  raycaster.params.Points = { threshold: 2 }
  mouse = new THREE.Vector2(-999, -999)

  const ambientLight = new THREE.AmbientLight(0x333344, 0.5)
  scene.add(ambientLight)
}

function createBackgroundStars() {
  const positions = new Float32Array(STAR_COUNT_BG * 3)
  const colors = new Float32Array(STAR_COUNT_BG * 3)
  const sizes = new Float32Array(STAR_COUNT_BG)

  for (let i = 0; i < STAR_COUNT_BG; i++) {
    const r = 200 + Math.random() * 600
    const theta = Math.random() * Math.PI * 2
    const phi = Math.acos(2 * Math.random() - 1)
    positions[i * 3] = r * Math.sin(phi) * Math.cos(theta)
    positions[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta)
    positions[i * 3 + 2] = r * Math.cos(phi)

    const warmth = Math.random()
    colors[i * 3] = 0.8 + warmth * 0.2
    colors[i * 3 + 1] = 0.8 + warmth * 0.15
    colors[i * 3 + 2] = 0.9 + warmth * 0.1

    sizes[i] = Math.random() * 1.5 + 0.3
  }

  const geometry = new THREE.BufferGeometry()
  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3))
  geometry.setAttribute('size', new THREE.BufferAttribute(sizes, 1))

  const vertexShader = `
    attribute float size;
    varying vec3 vColor;
    void main() {
      vColor = color;
      vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);
      gl_PointSize = size * (200.0 / -mvPosition.z);
      gl_Position = projectionMatrix * mvPosition;
    }
  `
  const fragmentShader = `
    varying vec3 vColor;
    void main() {
      float d = length(gl_PointCoord - vec2(0.5));
      if (d > 0.5) discard;
      float alpha = smoothstep(0.5, 0.1, d);
      gl_FragColor = vec4(vColor, alpha * 0.7);
    }
  `

  const material = new THREE.ShaderMaterial({
    vertexShader,
    fragmentShader,
    vertexColors: true,
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending
  })

  bgStars = new THREE.Points(geometry, material)
  scene.add(bgStars)
}

function createPoetStars(poetData: Poet[]) {
  // 清理旧的
  poetMeshes.forEach(m => {
    scene.remove(m)
    m.geometry.dispose()
    ;(m.material as THREE.Material).dispose()
  })
  poetLabels.forEach(l => {
    scene.remove(l)
    l.material.dispose()
  })
  poetMeshes = []
  poetLabels = []
  poetPositionMap.clear()
  connectionLines.forEach(l => {
    scene.remove(l)
    l.geometry.dispose()
    ;(l.material as THREE.Material).dispose()
  })
  connectionLines = []

  const filtered = selectedDynasty.value === 'all'
    ? poetData
    : poetData.filter(p => p.dynastyName === selectedDynasty.value)

  const goldenAngle = Math.PI * (3 - Math.sqrt(5))

  filtered.forEach((poet, index) => {
    const shell = getDynastyShell(poet.dynastyName)
    const shellRadius = SHELL_RADIUS_BASE + shell * SHELL_RADIUS_STEP
    const ySpread = 12 + shell * 2

    const angle = index * goldenAngle
    const r = shellRadius + (Math.random() - 0.5) * 12
    const y = (Math.random() - 0.5) * ySpread

    const x = r * Math.cos(angle)
    const z = r * Math.sin(angle)

    const starSize = Math.max(0.6, Math.log(index + 3) * 0.5)
    const color = getDynastyColor(poet.dynastyName)

    // 发光球体
    const geometry = new THREE.SphereGeometry(starSize, 12, 12)
    const material = new THREE.MeshBasicMaterial({
      color: color,
      transparent: true,
      opacity: 0.95
    })
    const mesh = new THREE.Mesh(geometry, material)
    mesh.position.set(x, y, z)
    mesh.userData = { poet, index }
    scene.add(mesh)
    poetMeshes.push(mesh)
    poetPositionMap.set(poet.id, new THREE.Vector3(x, y, z))

    // 光晕
    const glowGeo = new THREE.SphereGeometry(starSize * 2.5, 12, 12)
    const glowMat = new THREE.MeshBasicMaterial({
      color: color,
      transparent: true,
      opacity: 0.12,
      depthWrite: false
    })
    const glowMesh = new THREE.Mesh(glowGeo, glowMat)
    mesh.add(glowMesh)

    // 文字标签
    const label = createTextSprite(poet.name, color)
    label.position.set(x, y + starSize + 2.5, z)
    label.userData = { poetId: poet.id }
    label.visible = false
    scene.add(label)
    poetLabels.push(label)
  })

  // 创建赠诗连线（相邻同朝代诗人）
  createConnectionLines(filtered)
}

function createTextSprite(text: string, color: THREE.Color): THREE.Sprite {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')!
  canvas.width = 256
  canvas.height = 64

  ctx.fillStyle = 'transparent'
  ctx.fillRect(0, 0, 256, 64)

  ctx.font = '28px KaiTi, STKaiti, serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillStyle = `rgb(${Math.floor(color.r * 255)}, ${Math.floor(color.g * 255)}, ${Math.floor(color.b * 255)})`
  ctx.shadowColor = 'rgba(0,0,0,0.8)'
  ctx.shadowBlur = 4
  ctx.fillText(text, 128, 32)

  const texture = new THREE.CanvasTexture(canvas)
  texture.needsUpdate = true
  const material = new THREE.SpriteMaterial({
    map: texture,
    transparent: true,
    depthWrite: false
  })
  const sprite = new THREE.Sprite(material)
  sprite.scale.set(16, 4, 1)
  return sprite
}

function createConnectionLines(poetData: Poet[]) {
  const byDynasty = new Map<string, Poet[]>()
  poetData.forEach(p => {
    const d = p.dynastyName || '未知'
    if (!byDynasty.has(d)) byDynasty.set(d, [])
    byDynasty.get(d)!.push(p)
  })

  byDynasty.forEach((group) => {
    for (let i = 0; i < group.length - 1 && i < 30; i++) {
      const a = poetMeshes.find(m => m.userData.poet?.id === group[i].id)
      const b = poetMeshes.find(m => m.userData.poet?.id === group[i + 1].id)
      if (!a || !b) continue

      const points = [a.position.clone(), b.position.clone()]
      const mid = new THREE.Vector3().addVectors(a.position, b.position).multiplyScalar(0.5)
      mid.y += 3
      points.splice(1, 0, mid)

      const curve = new THREE.QuadraticBezierCurve3(points[0], points[1], points[2])
      const curvePoints = curve.getPoints(20)
      const geometry = new THREE.BufferGeometry().setFromPoints(curvePoints)

      const color = (a.material as THREE.MeshBasicMaterial).color.clone()
      const material = new THREE.LineBasicMaterial({
        color: color,
        transparent: true,
        opacity: 0.08,
        depthWrite: false
      })

      const line = new THREE.Line(geometry, material)
      line.userData = { poetA: group[i].id, poetB: group[i + 1].id }
      scene.add(line)
      connectionLines.push(line)
    }
  })
}

// ==================== 诗星光点云 ====================
function createPoemCloud(poems: Poem[]) {
  // 清理旧的
  if (poemCloud) {
    scene.remove(poemCloud)
    poemCloud.geometry.dispose()
    ;(poemCloud.material as THREE.Material).dispose()
    poemCloud = null
  }

  const filtered = selectedDynasty.value === 'all'
    ? poems
    : poems.filter(p => p.dynastyName === selectedDynasty.value)

  const count = filtered.length
  if (count === 0) return

  const positions = new Float32Array(count * 3)
  const colors = new Float32Array(count * 3)
  const sizes = new Float32Array(count)

  const goldenAngle = Math.PI * (3 - Math.sqrt(5))

  filtered.forEach((poem, i) => {
    // 找到该诗所属诗人的位置
    const poetPos = poem.poetId ? poetPositionMap.get(poem.poetId) : null

    let x: number, y: number, z: number

    if (poetPos) {
      // 围绕诗人星散布，形成"小星系"
      const angle = i * goldenAngle
      const r = 1.5 + Math.random() * 4
      const phi = Math.acos(2 * Math.random() - 1)
      x = poetPos.x + r * Math.sin(phi) * Math.cos(angle)
      y = poetPos.y + r * Math.sin(phi) * Math.sin(angle) * 0.6
      z = poetPos.z + r * Math.cos(phi)
    } else {
      // 无归属的诗，随机散布
      const shell = getDynastyShell(poem.dynastyName)
      const shellR = SHELL_RADIUS_BASE + shell * SHELL_RADIUS_STEP
      const angle = i * goldenAngle
      const r = shellR + (Math.random() - 0.5) * 15
      x = r * Math.cos(angle)
      y = (Math.random() - 0.5) * 20
      z = r * Math.sin(angle)
    }

    positions[i * 3] = x
    positions[i * 3 + 1] = y
    positions[i * 3 + 2] = z

    const color = getCategoryColor(poem.categoryName)
    colors[i * 3] = color.r
    colors[i * 3 + 1] = color.g
    colors[i * 3 + 2] = color.b

    sizes[i] = 0.3 + Math.random() * 0.4
  })

  poemGeometry = new THREE.BufferGeometry()
  poemGeometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  poemGeometry.setAttribute('color', new THREE.BufferAttribute(colors, 3))
  poemGeometry.setAttribute('size', new THREE.BufferAttribute(sizes, 1))

  const vertexShader = `
    attribute float size;
    varying vec3 vColor;
    void main() {
      vColor = color;
      vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);
      gl_PointSize = size * (300.0 / -mvPosition.z);
      gl_Position = projectionMatrix * mvPosition;
    }
  `
  const fragmentShader = `
    varying vec3 vColor;
    varying float vDist;
    void main() {
      float d = length(gl_PointCoord - vec2(0.5));
      if (d > 0.5) discard;
      float alpha = smoothstep(0.5, 0.05, d);
      gl_FragColor = vec4(vColor, alpha * 0.75);
    }
  `

  const material = new THREE.ShaderMaterial({
    vertexShader,
    fragmentShader,
    vertexColors: true,
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending
  })

  poemCloud = new THREE.Points(poemGeometry, material)
  poemCloud.userData.type = 'poemCloud'
  poemCloud.userData.poems = filtered
  scene.add(poemCloud)
}

function highlightConnections(poetId: number) {
  connectionLines.forEach(line => {
    const mat = line.material as THREE.LineBasicMaterial
    if (line.userData.poetA === poetId || line.userData.poetB === poetId) {
      mat.opacity = 0.5
      mat.needsUpdate = true
    } else {
      mat.opacity = 0.05
      mat.needsUpdate = true
    }
  })
}

function resetConnections() {
  connectionLines.forEach(line => {
    const mat = line.material as THREE.LineBasicMaterial
    mat.opacity = 0.08
    mat.needsUpdate = true
  })
}

function animate() {
  animationId = requestAnimationFrame(animate)
  const elapsed = clock.getElapsedTime()

  controls.update()

  // 背景星缓慢旋转
  if (bgStars) {
    bgStars.rotation.y = elapsed * 0.003
  }

  // 诗人星点脉动
  poetMeshes.forEach((mesh, i) => {
    const base = 1.0
    const pulse = Math.sin(elapsed * 1.5 + i * 0.7) * 0.08
    mesh.scale.setScalar(base + pulse)

    const glow = mesh.children[0] as THREE.Mesh | undefined
    if (glow) {
      const glowPulse = 1.0 + Math.sin(elapsed * 1.2 + i * 0.5) * 0.15
      glow.scale.setScalar(glowPulse)
    }
  })

  // 标签面向相机
  poetLabels.forEach(label => {
    label.quaternion.copy(camera.quaternion)
  })

  // 射线检测 hover
  raycaster.setFromCamera(mouse, camera)
  const intersects = raycaster.intersectObjects(poetMeshes)

  if (hoveredMesh && (!intersects.length || intersects[0].object !== hoveredMesh)) {
    const mat = hoveredMesh.material as THREE.MeshBasicMaterial
    mat.opacity = 0.95
    const label = poetLabels.find(l => l.userData.poetId === hoveredMesh!.userData.poet?.id)
    if (label) label.visible = false
    hoveredMesh = null
    document.body.style.cursor = 'default'
  }

  if (intersects.length > 0) {
    const hit = intersects[0].object as THREE.Mesh
    if (hit !== hoveredMesh) {
      hoveredMesh = hit
      const mat = hit.material as THREE.MeshBasicMaterial
      mat.opacity = 1.0
      const label = poetLabels.find(l => l.userData.poetId === hit.userData.poet?.id)
      if (label) label.visible = true
      document.body.style.cursor = 'pointer'
    }
  }

  renderer.render(scene, camera)
}

function handleMouseMove(event: MouseEvent) {
  if (!canvasRef.value) return
  const rect = canvasRef.value.getBoundingClientRect()
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
}

function handleClick(event: MouseEvent) {
  if (!canvasRef.value) return
  const rect = canvasRef.value.getBoundingClientRect()
  const clickMouse = new THREE.Vector2(
    ((event.clientX - rect.left) / rect.width) * 2 - 1,
    -((event.clientY - rect.top) / rect.height) * 2 + 1
  )
  raycaster.setFromCamera(clickMouse, camera)
  const intersects = raycaster.intersectObjects(poetMeshes)

  if (intersects.length > 0) {
    const mesh = intersects[0].object as THREE.Mesh
    const poet = mesh.userData.poet as Poet
    if (poet) {
      selectPoet(poet)
      // 相机飞向选中的星
      flyToStar(mesh.position)
    }
  } else {
    closePanel()
  }
}

function flyToStar(target: THREE.Vector3) {
  const offset = new THREE.Vector3(10, 8, 15)
  const destination = target.clone().add(offset)
  const start = camera.position.clone()
  const startTarget = controls.target.clone()
  const endTarget = target.clone()
  const duration = 1200
  const startTime = performance.now()

  function step(now: number) {
    const t = Math.min((now - startTime) / duration, 1)
    const ease = t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2

    camera.position.lerpVectors(start, destination, ease)
    controls.target.lerpVectors(startTarget, endTarget, ease)

    if (t < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

function handleResize() {
  if (!canvasRef.value) return
  const w = canvasRef.value.clientWidth
  const h = canvasRef.value.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

// ==================== 数据加载 ====================
async function loadData() {
  loading.value = true
  try {
    const [dynastyRes, poetRes, poemRes] = await Promise.all([
      getDynastyList(),
      getPoetList({ pageNum: 1, pageSize: 300 }),
      getPoemList({ pageNum: 1, pageSize: 1000 })
    ])
    dynasties.value = dynastyRes.data || []
    poets.value = poetRes.data?.list || []
    allPoems.value = poemRes.data?.list || []
  } catch (error) {
    console.error('加载诗云数据失败:', error)
    ElMessage.warning('加载数据失败，使用默认数据展示')
    dynasties.value = [
      { id: 1, name: '先秦' }, { id: 2, name: '汉' }, { id: 3, name: '魏晋' },
      { id: 4, name: '南北朝' }, { id: 5, name: '隋' }, { id: 6, name: '唐' },
      { id: 7, name: '宋' }, { id: 8, name: '元' }, { id: 9, name: '明' },
      { id: 10, name: '清' }, { id: 11, name: '近代' }, { id: 12, name: '现代' }
    ]
  } finally {
    loading.value = false
  }
}

async function loadPoetPoems(poetId: number) {
  try {
    const res = await getPoemList({ pageNum: 1, pageSize: 10, poetId })
    poetPoems.value = res.data?.list || []
  } catch {
    poetPoems.value = []
  }
}

// ==================== 交互逻辑 ====================
const filteredDynasties = computed(() => [{ id: 0, name: 'all' }, ...dynasties.value])
const stats = computed(() => ({
  poetCount: poets.value.length,
  poemCount: allPoems.value.length
}))

function dynastyLabel(name: string) {
  return name === 'all' ? '全部' : name
}

function selectDynasty(name: string) {
  selectedDynasty.value = name
  createPoetStars(poets.value)
  createPoemCloud(allPoems.value)
}

function selectPoet(poet: Poet) {
  selectedPoet.value = poet
  showPanel.value = true
  highlightConnections(poet.id)
  loadPoetPoems(poet.id)
}

function closePanel() {
  showPanel.value = false
  selectedPoet.value = null
  poetPoems.value = []
  resetConnections()
}

function goToPoetDetail(poetId: number) {
  router.push(`/poet/${poetId}`)
}

function goToPoetList() {
  router.push('/poet')
}

function handleSearch() {
  if (searchQuery.value.trim()) {
    router.push({ path: '/poem', query: { q: searchQuery.value.trim() } })
  }
}

// ==================== 生命周期 ====================
onMounted(async () => {
  await nextTick()
  initScene()
  createBackgroundStars()
  animate()

  window.addEventListener('resize', handleResize)
  canvasRef.value?.addEventListener('mousemove', handleMouseMove)
  canvasRef.value?.addEventListener('click', handleClick)

  await loadData()
  createPoetStars(poets.value)
  createPoemCloud(allPoems.value)
})

onUnmounted(() => {
  cancelAnimationFrame(animationId)
  window.removeEventListener('resize', handleResize)
  canvasRef.value?.removeEventListener('mousemove', handleMouseMove)
  canvasRef.value?.removeEventListener('click', handleClick)

  // 清理诗人网格和标签
  poetMeshes.forEach(m => {
    scene.remove(m)
    m.geometry.dispose()
    ;(m.material as THREE.Material).dispose()
  })
  poetLabels.forEach(l => {
    scene.remove(l)
    l.material.dispose()
  })

  // 清理连线
  connectionLines.forEach(l => {
    scene.remove(l)
    l.geometry.dispose()
    ;(l.material as THREE.Material).dispose()
  })

  // 清理诗云
  if (poemCloud) {
    scene.remove(poemCloud)
    poemCloud.geometry.dispose()
    ;(poemCloud.material as THREE.Material).dispose()
  }

  // 清理背景星
  if (bgStars) {
    scene.remove(bgStars)
    bgStars.geometry.dispose()
    ;(bgStars.material as THREE.Material).dispose()
  }

  // 清理位置缓存
  poetPositionMap.clear()

  renderer?.dispose()
  controls?.dispose()
  scene?.clear()
})
</script>

<template>
  <div class="poetry-cloud">
    <!-- Three.js 画布 -->
    <div ref="canvasRef" class="canvas-container" />

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner" />
      <p class="loading-text">正在加载星空数据...</p>
    </div>

    <!-- 中央标题 -->
    <transition name="fade">
      <div v-if="!showPanel && !loading" class="center-title">
        <h1 class="title-main calligraphy">诗云</h1>
        <p class="title-sub">每位诗人是一颗星，每首诗都有自己的坐标</p>
        <p class="title-hint">鼠标拖动旋转 · 滚轮缩放 · 点击星点探索诗人</p>
      </div>
    </transition>

    <!-- 左侧控制面板 -->
    <aside class="control-panel glass-panel">
      <h2 class="panel-title calligraphy">星空控制台</h2>

      <div class="search-box">
        <el-input
          v-model="searchQuery"
          placeholder="输入诗句或诗人..."
          size="small"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <div class="filter-section">
        <h3 class="filter-label">朝代筛选</h3>
        <div class="dynasty-grid">
          <button
            v-for="d in filteredDynasties"
            :key="d.id"
            class="dynasty-btn"
            :class="{ active: selectedDynasty === d.name }"
            @click="selectDynasty(d.name)"
          >
            {{ dynastyLabel(d.name) }}
          </button>
        </div>
      </div>

      <div class="legend-section">
        <h3 class="filter-label">诗体颜色</h3>
        <div class="legend-items">
          <div class="legend-item">
            <span class="legend-dot" style="background:#6eb5ff" />
            <span>五言绝句</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot" style="background:#7dffb3" />
            <span>七言绝句</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot" style="background:#b88cff" />
            <span>五言律诗</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot" style="background:#ff9f43" />
            <span>七言律诗</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot" style="background:#ff6b9d" />
            <span>词</span>
          </div>
        </div>
      </div>

      <div class="legend-section">
        <h3 class="filter-label">操作说明</h3>
        <div class="legend-items">
          <div class="legend-item">
            <span class="legend-icon">🖱️</span>
            <span>左键拖动旋转</span>
          </div>
          <div class="legend-item">
            <span class="legend-icon">⚙️</span>
            <span>滚轮缩放</span>
          </div>
          <div class="legend-item">
            <span class="legend-icon">👆</span>
            <span>点击星点查看详情</span>
          </div>
          <div class="legend-item">
            <span class="legend-icon">⇔</span>
            <span>右键拖动平移</span>
          </div>
        </div>
      </div>

      <div class="stats-section">
        <div class="stat-item">
          <span class="stat-number">{{ stats.poetCount.toLocaleString() }}</span>
          <span class="stat-label">位诗人</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">{{ stats.poemCount.toLocaleString() }}</span>
          <span class="stat-label">首诗</span>
        </div>
      </div>

      <div class="quick-links">
        <el-button text size="small" @click="goToPoetList">浏览诗人列表</el-button>
      </div>
    </aside>

    <!-- 右侧信息面板 -->
    <transition name="slide-panel">
      <aside v-if="showPanel && selectedPoet" class="info-panel glass-panel">
        <div class="panel-header">
          <h2 class="panel-title calligraphy">诗人信息</h2>
          <el-button text circle size="small" @click="closePanel">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <div class="poet-info-card">
          <div class="poet-avatar">
            <span class="calligraphy">{{ selectedPoet.name?.charAt(0) }}</span>
          </div>
          <div class="poet-meta">
            <h3 class="poet-name">{{ selectedPoet.name }}</h3>
            <p class="poet-dynasty">{{ selectedPoet.dynastyName || '未知朝代' }}</p>
          </div>
        </div>

        <p v-if="selectedPoet.biography" class="poet-desc">
          {{ selectedPoet.biography }}
        </p>

        <div class="poet-stats-row">
          <span>朝代：{{ selectedPoet.dynastyName || '-' }}</span>
          <span v-if="selectedPoet.poetType">类型：{{ selectedPoet.poetType }}</span>
        </div>

        <!-- 代表作品 -->
        <div v-if="poetPoems.length > 0" class="poem-list">
          <h3 class="section-label">代表作品</h3>
          <div
            v-for="poem in poetPoems"
            :key="poem.id"
            class="poem-card"
            @click="router.push(`/poem/${poem.id}`)"
          >
            <h4 class="poem-title">{{ poem.title }}</h4>
            <p class="poem-preview">{{ poem.content?.substring(0, 30) }}...</p>
          </div>
        </div>

        <div class="panel-actions">
          <el-button type="primary" class="btn-explore" @click="goToPoetDetail(selectedPoet.id)">
            查看诗人详情
          </el-button>
          <el-button class="btn-share" @click="router.push('/poem')">
            浏览诗词库
          </el-button>
        </div>
      </aside>
    </transition>

    <!-- 底部状态栏 -->
    <footer class="status-bar glass-panel">
      <div class="status-left">
        <span class="status-item">诗云 · 墨渊古诗词系统</span>
        <span class="status-divider">|</span>
        <span class="status-item">{{ stats.poetCount.toLocaleString() }} 位诗人</span>
        <span class="status-divider">|</span>
        <span class="status-item">{{ stats.poemCount.toLocaleString() }} 首诗</span>
      </div>
      <div class="status-right">
        <span class="status-item clickable" @click="goToPoetList">诗人列表</span>
        <span class="status-item clickable" @click="router.push('/')">返回首页</span>
      </div>
    </footer>
  </div>
</template>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

$bg-deep: #050510;
$bg-card: rgba(10, 10, 30, 0.85);
$border-star: rgba(205, 133, 63, 0.2);
$border-star-focus: rgba(205, 133, 63, 0.5);
$text-star-primary: #f5f5f5;
$text-star-secondary: #b0b0b0;
$text-star-muted: #808080;
$text-star-accent: #cd853f;

.poetry-cloud {
  position: fixed;
  inset: 0;
  z-index: 999;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: $bg-deep;
  font-family: $font-family-base;
  color: $text-star-primary;
}

.canvas-container {
  position: absolute;
  inset: 0;
  z-index: 0;

  canvas {
    display: block;
  }
}

.calligraphy {
  font-family: 'YingBiXingShu', 'KaiTi', 'STKaiti', serif;
}

/* ==================== 中央标题 ==================== */
.center-title {
  position: absolute;
  top: 45%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  pointer-events: none;
  z-index: 5;

  .title-main {
    font-size: 64px;
    color: $text-star-accent;
    text-shadow: 0 0 40px rgba(205, 133, 63, 0.6), 0 2px 8px rgba(0, 0, 0, 0.6);
    margin-bottom: 16px;
    letter-spacing: 12px;
  }

  .title-sub {
    font-size: 16px;
    color: $text-star-secondary;
    margin-bottom: 10px;
    letter-spacing: 3px;
  }

  .title-hint {
    font-size: 13px;
    color: $text-star-muted;
    letter-spacing: 1px;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ==================== 加载状态 ==================== */
.loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 30;
  background: rgba($bg-deep, 0.9);

  .loading-spinner {
    width: 40px;
    height: 40px;
    border: 3px solid rgba(205, 133, 63, 0.2);
    border-top-color: $primary-color;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin-bottom: 16px;
  }

  .loading-text {
    color: $text-star-secondary;
    font-size: 14px;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ==================== 毛玻璃面板 ==================== */
.glass-panel {
  background: $bg-card;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid $border-star;
}

/* ==================== 左侧控制面板 ==================== */
.control-panel {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 220px;
  padding: 20px 14px;
  z-index: 40;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: rgba(205, 133, 63, 0.2); border-radius: 2px; }

  .panel-title {
    font-size: 18px;
    color: $text-star-accent;
    text-shadow: 0 0 10px rgba(205, 133, 63, 0.3);
    padding-bottom: 12px;
    border-bottom: 1px solid $border-star;
  }

  .search-box {
    :deep(.el-input__wrapper) {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(205, 133, 63, 0.15);
      box-shadow: none;
      border-radius: 20px;

      &:hover, &.is-focus {
        border-color: $border-star-focus;
      }
    }
    :deep(.el-input__inner) {
      color: $text-star-primary;
      &::placeholder { color: $text-star-muted; }
    }
    :deep(.el-input__prefix .el-icon) {
      color: $text-star-muted;
    }
  }

  .filter-section {
    .filter-label {
      font-size: 13px;
      color: $text-star-secondary;
      margin-bottom: 8px;
      font-weight: 500;
    }

    .dynasty-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 5px;
    }

    .dynasty-btn {
      padding: 4px 3px;
      font-size: 11px;
      color: $text-star-secondary;
      background: rgba(139, 69, 19, 0.08);
      border: 1px solid rgba(139, 69, 19, 0.15);
      border-radius: 5px;
      cursor: pointer;
      transition: all 0.2s ease;
      font-family: $font-family-base;

      &:hover {
        background: rgba(139, 69, 19, 0.2);
        border-color: rgba(139, 69, 19, 0.3);
      }

      &.active {
        background: rgba(139, 69, 19, 0.35);
        border-color: $primary-color;
        color: $text-star-accent;
      }
    }
  }

  .legend-section {
    .filter-label {
      font-size: 13px;
      color: $text-star-secondary;
      margin-bottom: 8px;
      font-weight: 500;
    }

    .legend-items {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .legend-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 11px;
      color: $text-star-muted;

      .legend-icon {
        width: 18px;
        text-align: center;
        font-size: 12px;
      }

      .legend-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        flex-shrink: 0;
      }
    }
  }

  .stats-section {
    margin-top: auto;
    padding-top: 12px;
    border-top: 1px solid $border-star;

    .stat-item {
      text-align: center;

      .stat-number {
        display: block;
        font-size: 24px;
        font-weight: 700;
        color: $text-star-accent;
        font-family: 'Georgia', serif;
      }

      .stat-label {
        font-size: 12px;
        color: $text-star-muted;
      }
    }
  }

  .quick-links {
    :deep(.el-button) {
      color: $text-star-secondary;
      &:hover { color: $text-star-accent; }
    }
  }
}

/* ==================== 右侧信息面板 ==================== */
.info-panel {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 40px;
  width: 300px;
  padding: 20px 14px;
  z-index: 40;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: rgba(205, 133, 63, 0.2); border-radius: 2px; }

  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .panel-title {
      font-size: 18px;
      color: $text-star-accent;
      text-shadow: 0 0 10px rgba(205, 133, 63, 0.3);
    }

    :deep(.el-button) {
      color: $text-star-secondary;
      &:hover { color: $text-star-primary; }
    }
  }

  .poet-info-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px;
    background: linear-gradient(135deg, rgba(20, 20, 50, 0.9), rgba(15, 15, 40, 0.9));
    border: 1px solid $border-star;
    border-radius: 12px;

    .poet-avatar {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      background: rgba(205, 133, 63, 0.15);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      .calligraphy {
        font-size: 22px;
        color: $text-star-accent;
      }
    }

    .poet-meta {
      .poet-name {
        font-size: 16px;
        font-weight: 600;
        color: $text-star-primary;
      }

      .poet-dynasty {
        font-size: 13px;
        color: $text-star-secondary;
        margin-top: 2px;
      }
    }
  }

  .poet-desc {
    font-size: 13px;
    color: $text-star-secondary;
    line-height: 1.6;
    max-height: 80px;
    overflow-y: auto;
  }

  .poet-stats-row {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: $text-star-muted;
    padding: 8px 0;
    border-top: 1px solid $border-star;
  }

  .poem-list {
    .section-label {
      font-size: 13px;
      color: $text-star-secondary;
      margin-bottom: 8px;
      font-weight: 500;
    }

    .poem-card {
      padding: 10px;
      background: linear-gradient(135deg, rgba(20, 20, 50, 0.8), rgba(15, 15, 40, 0.8));
      border: 1px solid $border-star;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s ease;
      margin-bottom: 6px;

      &:hover {
        border-color: $border-star-focus;
        transform: translateY(-1px);
      }

      .poem-title {
        font-size: 14px;
        color: $text-star-primary;
        font-weight: 500;
        margin-bottom: 4px;
      }

      .poem-preview {
        font-size: 12px;
        color: $text-star-muted;
        line-height: 1.4;
      }
    }
  }

  .panel-actions {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-top: auto;

    .btn-explore {
      background: linear-gradient(135deg, $primary-color, $accent-color);
      border: none;
      border-radius: 8px;
      font-family: $font-family-base;

      &:hover {
        background: linear-gradient(135deg, $primary-700, $primary-color);
      }
    }

    .btn-share {
      background: transparent;
      border: 1px solid $border-star;
      color: $text-star-secondary;
      border-radius: 8px;

      &:hover {
        border-color: $border-star-focus;
        color: $text-star-accent;
      }
    }
  }
}

.slide-panel-enter-active,
.slide-panel-leave-active {
  transition: transform 0.35s ease, opacity 0.35s ease;
}
.slide-panel-enter-from,
.slide-panel-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

/* ==================== 底部状态栏 ==================== */
.status-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  font-size: 12px;

  .status-left,
  .status-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .status-item {
    color: $text-star-muted;
  }

  .status-divider {
    color: rgba(255, 255, 255, 0.1);
  }

  .clickable {
    cursor: pointer;
    transition: color 0.2s ease;
    &:hover { color: $text-star-secondary; }
  }
}

/* ==================== 响应式 ==================== */
@media (max-width: 1024px) {
  .control-panel {
    width: 190px;
    padding: 16px 10px;

    .dynasty-grid {
      grid-template-columns: repeat(2, 1fr);
    }

    .legend-section { display: none; }
  }

  .info-panel {
    width: 260px;
  }

  .center-title .title-main {
    font-size: 44px;
  }
}

@media (max-width: 768px) {
  .control-panel {
    top: auto;
    bottom: 40px;
    left: 0;
    right: 0;
    width: 100%;
    height: auto;
    max-height: 35vh;
    flex-direction: row;
    flex-wrap: wrap;
    padding: 10px;
    gap: 10px;
    border-radius: 14px 14px 0 0;

    .panel-title { display: none; }

    .search-box { width: 100%; }

    .filter-section {
      width: 100%;

      .dynasty-grid {
        grid-template-columns: repeat(4, 1fr);
      }
    }

    .legend-section { display: none; }

    .stats-section {
      margin-top: 0;
      padding-top: 0;
      border-top: none;
      width: 100%;
    }

    .quick-links { display: none; }
  }

  .info-panel {
    top: auto;
    left: 0;
    right: 0;
    bottom: 40px;
    width: 100%;
    max-height: 50vh;
    border-radius: 14px 14px 0 0;
  }

  .center-title .title-main {
    font-size: 36px;
    letter-spacing: 6px;
  }

  .center-title .title-sub {
    font-size: 13px;
  }
}
</style>
