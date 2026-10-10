<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { LineMaterial } from 'three/examples/jsm/lines/LineMaterial.js'
import { LineSegments2 } from 'three/examples/jsm/lines/LineSegments2.js'
import { LineSegmentsGeometry } from 'three/examples/jsm/lines/LineSegmentsGeometry.js'
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
const centerTitleVisible = ref(true)
// 是否显示连线（诗人↔诗作、同朝代诗人赠诗线）
const showLinks = ref(true)
// 自动环绕：相机以极低速度自动旋转（默认开启），用户手动操作时暂停
const autoOrbit = ref(true)
const voidPoem = ref<Poem | null>(null)
const selectedPoem = ref<Poem | null>(null)
const offerRandomDisabled = ref(false)
const searchQuery = ref('')
const canvasRef = ref<HTMLDivElement | null>(null)
// 诗人姓名标注层：把诗人星的 3D 坐标投影到屏幕后跟随显示姓名
const labelLayerRef = ref<HTMLDivElement | null>(null)
// 悬浮提示：鼠标悬停某节点时显示它的含义
const hoverInfo = ref<{ type: 'poet' | 'poem'; title: string; meta: string; color: string } | null>(null)
const tooltipPos = ref({ x: 0, y: 0 })
// 鼠标在画布内的像素坐标（用于屏幕像素级命中检测）
const mousePx = { x: -9999, y: -9999 }

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

// ==================== Three.js 核心 ====================
let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let renderer: THREE.WebGLRenderer
let controls: OrbitControls
let raycaster: THREE.Raycaster
let mouse: THREE.Vector2
let poetMeshes: THREE.Mesh[] = []
let connectionLines: LineSegments2[] = []
let poemMeshes: THREE.Mesh[] = []
let poemLinkGroups: { poetId: number; line: LineSegments2 }[] = []
// 诗人姓名 DOM 标签，与 poetMeshes 一一对应
let poetLabels: { mesh: THREE.Mesh; el: HTMLDivElement }[] = []
// 节点尺寸倍率（由「配色实验室」的节点尺寸滑杆控制，作用于诗人星与诗作点的整体缩放）
let nodeScale = 1
const poemCountMap = new Map<number, number>()
let bgStars: THREE.Points
let clock: THREE.Clock
let animationId: number
let hoveredMesh: THREE.Mesh | null = null
let selectedMesh: THREE.Mesh | null = null
let voidStar: THREE.Mesh | null = null
let titleTimer: ReturnType<typeof setTimeout> | null = null
// 记录鼠标按下位置，用于区分「纯点击」与「拖动星云」
let pointerDownPos = { x: 0, y: 0 }
// 打开页面后首次点击虚空只用于关闭中央提示，不弹出诗
let firstVoidClick = true

const STAR_COUNT_BG = 3200
const SHELL_RADIUS_BASE = 25
const SHELL_RADIUS_STEP = 18
// ==================== 层级布局：强制最小间距 ====================
// 诗作点之间、诗作点与诗人星之间的最小距离，用于松弛迭代推开重叠节点，
// 使「诗人为中心、诗作环绕」的从属关系一目了然
const POEM_MIN_GAP = 7
const POEM_POET_MIN_GAP = 9
// ==================== 配色规范（按设计稿） ====================
const COLOR_BG = 0x0b1020               // 背景：深蓝黑
const COLOR_BG_STAR = '#d6e4ff'         // 背景星点
const COLOR_POEM_NORMAL = '#ffd24a'     // 普通诗作点
const COLOR_POEM_HIGHLIGHT = '#fff2a8'  // 高亮诗作点
const COLOR_LINK_NORMAL = '#ffffff'     // 普通连线（默认白色）
const COLOR_LINK_HIGHLIGHT = '#66e0e6'  // 高亮连线
const COLOR_LINK_SELECTED = '#ffb545'   // 选中关系线
// 连线不透明度
const LINK_OPACITY_NORMAL = 0.4
const LINK_OPACITY_HIGHLIGHT = 0.8
const LINK_OPACITY_SELECTED = 0.9
const LINK_OPACITY_DIM = 0.08
// 线宽：赠诗线 / 诗人↔诗作连线
const CONNECT_LINE_WIDTH = 1.5
const POEM_LINK_WIDTH = 2
// 相机自动环绕速度（越慢越舒缓）
const AUTO_ROTATE_SPEED = 0.15
// 用户操作结束后恢复自动环绕的延迟
const AUTO_ROTATE_RESUME_DELAY = 3000
// 相机「回归原点」的初始位置与目标点
const CAMERA_HOME_POS = new THREE.Vector3(0, 55, 140)
const CAMERA_HOME_TARGET = new THREE.Vector3(0, 0, 0)

// ==================== 配色实验室（页面控制模块） ====================
// 参考《星空诗歌图谱配色实验室》，把配色方案与光影参数做成页面级可视控件，
// 实时作用于 3D 星图（诗人主节点 / 诗作从节点 / 两类连线 / 背景 / 光晕 / 尺寸 / 亮度）
interface VizPalette {
  id: string
  name: string
  poet: string       // 诗人节点主色（关闭「诗人按朝代配色」时生效）
  poetEdge: string   // 诗人节点外圈色（同时作为选中关系线的默认色）
  work: string       // 普通诗作点主色
  workEdge: string   // 诗作点光晕色
  pp: string         // 诗人↔诗人 赠诗连线色
  pw: string         // 诗人↔诗作 连线色
  bg: string         // 星图背景色
  hl?: string        // 高亮连线色（缺省由 pw 提亮派生）
  sel?: string       // 选中关系线色（缺省取 poetEdge）
  workHi?: string    // 高亮诗作点色（缺省由 work 提亮派生）
  dynasty?: boolean  // 是否默认按朝代着色诗人
}

// 颜色提亮：向白色按比例混合
function lighten(hex: string, amount: number): string {
  const c = new THREE.Color(hex)
  c.lerp(new THREE.Color('#ffffff'), amount)
  return `#${c.getHexString()}`
}

const VIZ_PALETTES: VizPalette[] = [
  {
    id: 'amber', name: '琥珀星海', dynasty: true,
    poet: '#cd853f', poetEdge: '#ffd24a', work: '#ffd24a', workEdge: '#ffd24a',
    pp: '#ffffff', pw: '#ffffff', bg: '#0b1020', hl: '#66e0e6', sel: '#ffb545'
  },
  {
    id: 'jade', name: '青玉 × 暖琥珀',
    poet: '#8de0d0', poetEdge: '#43b9a8', work: '#d7f2e9', workEdge: '#86cdbd',
    pp: '#43b9a8', pw: '#719e9a', bg: '#081321'
  },
  {
    id: 'coral', name: '珊瑚 × 雾粉',
    poet: '#ffb19b', poetEdge: '#e98070', work: '#f5d8d2', workEdge: '#dda9a6',
    pp: '#a95f5c', pw: '#9e777b', bg: '#17101d'
  },
  {
    id: 'ice', name: '冰川蓝 × 银灰',
    poet: '#8ed5ff', poetEdge: '#4ba8e0', work: '#dceaf4', workEdge: '#9fbacd',
    pp: '#3d779e', pw: '#66869c', bg: '#081522'
  },
  {
    id: 'plum', name: '梅子紫 × 香槟白',
    poet: '#c6a6ff', poetEdge: '#9873e5', work: '#f0e7f7', workEdge: '#cdb8e0',
    pp: '#7359a6', pw: '#8c789f', bg: '#110f20'
  },
  {
    id: 'moss', name: '苔绿 × 月光银',
    poet: '#b7d99a', poetEdge: '#83b76a', work: '#e4ebdd', workEdge: '#b7c8ac',
    pp: '#55794d', pw: '#73866d', bg: '#0c1717'
  },
  {
    id: 'ember', name: '赤陶 × 骨瓷',
    poet: '#f2a17f', poetEdge: '#cf745b', work: '#f1e4d4', workEdge: '#cdb9a5',
    pp: '#8e5144', pw: '#9b7b6a', bg: '#19110f'
  }
]
// 未显式给出的高亮 / 选中 / 高亮诗作色，统一由基础色派生，保证方案内色调自洽
VIZ_PALETTES.forEach(p => {
  p.hl = p.hl ?? lighten(p.pw, 0.55)
  p.sel = p.sel ?? p.poetEdge
  p.workHi = p.workHi ?? lighten(p.work, 0.4)
})

// 实验室的可视化参数（默认值与页面既有观感一致：琥珀方案 + 默认倍率）
const viz = reactive({
  palette: 0,
  glow: 65,        // 光晕强度 %（65 为基准）
  line: 55,        // 连线可见度 %（55 为基准）
  size: 100,       // 节点尺寸 %
  brightness: 100, // 整体亮度 %（走画布 CSS filter）
  labels: true,    // 显示诗人姓名
  dynastyPoet: true // 诗人节点按朝代配色
})
const showLab = ref(false)
const currentPalette = computed(() => VIZ_PALETTES[viz.palette])
// 整体亮度：直接作用在画布容器上，星点 / 节点 / 连线一并生效
const canvasFilter = computed(() => `brightness(${(viz.brightness / 100).toFixed(2)})`)
// 「诗人信息」面板打开时，实验室右移让位，避免互相遮挡
const labRight = computed(() => (showPanel.value && selectedPoet.value) ? '300px' : '0')

// 由滑杆换算的倍率（基准值处为 1.0）
const glowScale = () => viz.glow / 65
const lineScale = () => Math.min(viz.line / 55, 1.4)
const clamp01 = (v: number) => Math.max(0, Math.min(1, v))

function initScene() {
  if (!canvasRef.value) return

  clock = new THREE.Clock()
  scene = new THREE.Scene()
  scene.fog = new THREE.FogExp2(COLOR_BG, 0.003)

  camera = new THREE.PerspectiveCamera(
    60,
    canvasRef.value.clientWidth / canvasRef.value.clientHeight,
    0.1,
    2000
  )
  camera.position.copy(CAMERA_HOME_POS)

  renderer = new THREE.WebGLRenderer({
    antialias: true,
    alpha: false,
    powerPreference: 'high-performance'
  })
  renderer.setSize(canvasRef.value.clientWidth, canvasRef.value.clientHeight)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setClearColor(COLOR_BG)
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
  controls.minDistance = 15
  controls.maxDistance = 500
  controls.target.copy(CAMERA_HOME_TARGET)

  // 自动环绕：相机以极低速度绕目标点自动旋转，作为默认展示效果
  controls.autoRotate = autoOrbit.value
  controls.autoRotateSpeed = AUTO_ROTATE_SPEED
  // 鼠标操作：左键平移、右键旋转（滚轮缩放）
  controls.mouseButtons = {
    LEFT: THREE.MOUSE.PAN,
    MIDDLE: THREE.MOUSE.DOLLY,
    RIGHT: THREE.MOUSE.ROTATE
  }
  // 触屏操作：单指平移、双指缩放并旋转（与鼠标键位保持一致）
  controls.touches = {
    ONE: THREE.TOUCH.PAN,
    TWO: THREE.TOUCH.DOLLY_ROTATE
  }
  // 用户开始操作时暂停自动环绕，结束后延迟恢复
  controls.addEventListener('start', handleControlStart)
  controls.addEventListener('end', handleControlEnd)

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
  const alphas = new Float32Array(STAR_COUNT_BG)

  // 背景星点统一冷白蓝 #D6E4FF
  const baseColor = new THREE.Color(COLOR_BG_STAR)

  for (let i = 0; i < STAR_COUNT_BG; i++) {
    const r = 200 + Math.random() * 600
    const theta = Math.random() * Math.PI * 2
    const phi = Math.acos(2 * Math.random() - 1)
    positions[i * 3] = r * Math.sin(phi) * Math.cos(theta)
    positions[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta)
    positions[i * 3 + 2] = r * Math.cos(phi)

    // 亮度轻微抖动，避免整片死板
    const brightness = 0.82 + Math.random() * 0.18
    colors[i * 3] = baseColor.r * brightness
    colors[i * 3 + 1] = baseColor.g * brightness
    colors[i * 3 + 2] = baseColor.b * brightness

    sizes[i] = Math.random() * 0.7 + 0.2
    // 透明度 0.15–0.3，逐星随机，形成自然的疏密明暗
    alphas[i] = 0.15 + Math.random() * 0.15
  }

  const geometry = new THREE.BufferGeometry()
  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3))
  geometry.setAttribute('size', new THREE.BufferAttribute(sizes, 1))
  geometry.setAttribute('aAlpha', new THREE.BufferAttribute(alphas, 1))

  const vertexShader = `
    attribute float size;
    attribute float aAlpha;
    varying vec3 vColor;
    varying float vAlpha;
    void main() {
      vColor = color;
      vAlpha = aAlpha;
      vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);
      gl_PointSize = size * (200.0 / -mvPosition.z);
      gl_Position = projectionMatrix * mvPosition;
    }
  `
  const fragmentShader = `
    varying vec3 vColor;
    varying float vAlpha;
    void main() {
      float d = length(gl_PointCoord - vec2(0.5));
      if (d > 0.5) discard;
      float falloff = smoothstep(0.5, 0.1, d);
      gl_FragColor = vec4(vColor, falloff * vAlpha);
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
  // 清理旧的诗人姓名标签
  clearPoetLabels()
  // 清理旧的
  poetMeshes.forEach(m => {
    scene.remove(m)
    m.geometry.dispose()
    ;(m.material as THREE.Material).dispose()
  })
  poetMeshes = []
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

    const count = poemCountMap.get(poet.id) ?? 0
    // 作品越多，诗人星越大；设最小尺寸，保证屏幕可见且可被鼠标命中
    const starSize = 2.4 + Math.log10(count + 1) * 1.2
    // 配色实验室：默认按朝代着色；关闭该开关后统一使用当前方案的诗人主色
    const pal = currentPalette.value
    const dynColor = getDynastyColor(poet.dynastyName)
    const color = viz.dynastyPoet ? dynColor : new THREE.Color(pal.poet)
    const edgeColor = viz.dynastyPoet ? dynColor : new THREE.Color(pal.poetEdge)

    // 发光球体
    const geometry = new THREE.SphereGeometry(starSize, 12, 12)
    const material = new THREE.MeshBasicMaterial({
      color: color,
      transparent: true,
      opacity: 0.95,
      // 关闭色调映射，避免 ACES 把节点压暗，保证黑背景下对比明显（与连线一致）
      toneMapped: false
    })
    const mesh = new THREE.Mesh(geometry, material)
    mesh.position.set(x, y, z)
    mesh.userData = { poet, index }
    scene.add(mesh)
    poetMeshes.push(mesh)
    poetPositionMap.set(poet.id, new THREE.Vector3(x, y, z))

    // 光晕
    const glowGeo = new THREE.SphereGeometry(starSize * 1.8, 12, 12)
    const glowMat = new THREE.MeshBasicMaterial({
      color: edgeColor,
      transparent: true,
      opacity: 0.12 * glowScale(),
      depthWrite: false
    })
    const glowMesh = new THREE.Mesh(glowGeo, glowMat)
    mesh.add(glowMesh)

    // 诗人姓名标注：DOM 元素跟随诗人星的屏幕投影位置逐帧刷新
    const el = document.createElement('div')
    el.className = 'poet-label'
    el.textContent = poet.name
    labelLayerRef.value?.appendChild(el)
    poetLabels.push({ mesh, el })
  })

  // 创建赠诗连线（相邻同朝代诗人）
  createConnectionLines(filtered)
  applyLinkVisibility()
}

// 清理诗人姓名标签
function clearPoetLabels() {
  poetLabels.forEach(({ el }) => el.remove())
  poetLabels = []
}

// 每帧把诗人星投影到屏幕，更新姓名标签位置；相机背后的标签隐藏
function updatePoetLabels() {
  if (poetLabels.length === 0) return
  const w = canvasRef.value?.clientWidth ?? 1
  const h = canvasRef.value?.clientHeight ?? 1
  const v = new THREE.Vector3()
  const selectedId = selectedPoet.value?.id
  poetLabels.forEach(({ mesh, el }) => {
    // 「配色实验室」关闭标签显示时直接隐藏
    if (!viz.labels) {
      el.style.display = 'none'
      return
    }
    mesh.getWorldPosition(v).project(camera)
    if (v.z > 1 || v.z < -1) {
      el.style.display = 'none'
      return
    }
    const sx = ((v.x + 1) / 2) * w
    const sy = ((1 - v.y) / 2) * h
    if (sx < -80 || sx > w + 80 || sy < -30 || sy > h + 30) {
      el.style.display = 'none'
      return
    }
    el.style.display = ''
    el.style.left = `${sx}px`
    el.style.top = `${sy}px`
    el.classList.toggle('is-selected', selectedId != null && mesh.userData.poet?.id === selectedId)
  })
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
      // 折线转线段对：[p0,p1, p1,p2, ...]，LineSegments2 才能正确渲染且线宽可调
      const segPositions: number[] = []
      for (let k = 0; k < curvePoints.length - 1; k++) {
        const p0 = curvePoints[k]
        const p1 = curvePoints[k + 1]
        segPositions.push(p0.x, p0.y, p0.z, p1.x, p1.y, p1.z)
      }
      const geometry = new LineSegmentsGeometry()
      geometry.setPositions(segPositions)

      // 赠诗连线：颜色取当前方案的诗人连线色，透明度随「连线可见度」滑杆缩放
      const material = new LineMaterial({
        color: new THREE.Color(currentPalette.value.pp),
        linewidth: CONNECT_LINE_WIDTH,
        transparent: true,
        opacity: clamp01(LINK_OPACITY_NORMAL * lineScale()),
        depthWrite: false
      })
      material.toneMapped = false
      material.resolution.set(
        canvasRef.value?.clientWidth ?? window.innerWidth,
        canvasRef.value?.clientHeight ?? window.innerHeight
      )

      const line = new LineSegments2(geometry, material)
      line.userData = { poetA: group[i].id, poetB: group[i + 1].id }
      scene.add(line)
      connectionLines.push(line)
    }
  })
}

// ==================== 诗作节点 + 诗人↔诗连线 ====================
function createPoemCloud(poems: Poem[]) {
  // 清理旧的诗作节点与连线
  clearPoemNodes()
  clearPoemLinks()

  // 诗词列表接口不返回 dynastyName，需回退到所属诗人的朝代来判断归属
  const poetDynastyMap = new Map<number, string>()
  poets.value.forEach(pt => poetDynastyMap.set(pt.id, pt.dynastyName || ''))
  const poemDynasty = (p: Poem) =>
    p.dynastyName || (p.poetId != null ? poetDynastyMap.get(p.poetId) || '' : '')

  const target = selectedDynasty.value
  const filtered = target === 'all'
    ? poems
    : poems.filter(p => {
        const d = poemDynasty(p)
        return !!d && (d === target || d.includes(target) || target.includes(d))
      })

  if (filtered.length === 0) return

  const goldenAngle = Math.PI * (3 - Math.sqrt(5))

  // 按诗人分组计数，用于环绕半径自适应
  const groupCount = new Map<number, number>()
  filtered.forEach(p => {
    if (p.poetId != null) groupCount.set(p.poetId, (groupCount.get(p.poetId) ?? 0) + 1)
  })
  // 组内序号（诗人id → 已分配数量）：球面螺旋必须用组内序号，否则全局索引会超过该诗人的
  // 诗作数 n，使 Math.acos 参数越界返回 NaN，导致诗点坐标失效、完全不渲染
  const groupIndexMap = new Map<number, number>()

  // 第一遍：先算出所有诗作点的初始坐标（以所属诗人为中心环绕分布），留待松弛迭代
  const nodes: { pos: THREE.Vector3; poem: Poem; poetId?: number }[] = []

  filtered.forEach((poem, i) => {
    const poetPos = poem.poetId != null ? poetPositionMap.get(poem.poetId) ?? null : null
    const pos = new THREE.Vector3()

    if (poetPos) {
      // 围绕诗人星按球面螺旋局部放射，形成「诗人-作品」簇
      const n = groupCount.get(poem.poetId!) ?? 1
      const gi = groupIndexMap.get(poem.poetId!) ?? 0
      groupIndexMap.set(poem.poetId!, gi + 1)
      const starRef = poetMeshes.find(m => m.userData.poet?.id === poem.poetId)
      const starSize = starRef
        ? ((starRef.geometry as THREE.SphereGeometry).parameters.radius as number)
        : 2.2
      const angle = gi * goldenAngle
      // gi ∈ [0, n-1]，保证 (gi+0.5)/n ∈ (0,1)，acos 参数始终合法
      const phi = Math.acos(1 - 2 * (gi + 0.5) / n)
      // 环绕半径：起点在诗人星光晕（starSize*1.8）之外，诗作越多外环越远
      const r = Math.max(starSize * 1.8 + 4, 7) + Math.sqrt(gi / Math.max(n, 1)) * 9
      pos.x = poetPos.x + r * Math.sin(phi) * Math.cos(angle)
      pos.y = poetPos.y + r * Math.sin(phi) * Math.sin(angle) * 0.6
      pos.z = poetPos.z + r * Math.cos(phi)
    } else {
      // 无归属的诗，按朝代散布
      const shell = getDynastyShell(poem.dynastyName)
      const shellR = SHELL_RADIUS_BASE + shell * SHELL_RADIUS_STEP
      const angle = i * goldenAngle
      const r = shellR + (Math.random() - 0.5) * 15
      pos.x = r * Math.cos(angle)
      pos.y = (Math.random() - 0.5) * 20
      pos.z = r * Math.sin(angle)
    }

    nodes.push({ pos, poem, poetId: poem.poetId ?? undefined })
  })

  // 第二遍：松弛迭代强制拉开最小间距，消除诗点重叠与「诗点压住诗人星」
  relaxPoemPositions(nodes)

  // 第三遍：按最终坐标生成诗作点与连线数据
  const items: { pos: THREE.Vector3; poetId?: number }[] = []

  nodes.forEach(({ pos, poem, poetId }) => {
    // 诗作点：核心取方案诗作主色，光晕取方案诗作外圈色
    const pal = currentPalette.value
    const geo = new THREE.SphereGeometry(2.4, 16, 16)
    const mat = new THREE.MeshBasicMaterial({ color: pal.work, transparent: true, opacity: 0.9, depthWrite: false, toneMapped: false })
    const mesh = new THREE.Mesh(geo, mat)
    mesh.position.copy(pos)
    mesh.userData = { poem, poetId, poemId: poem.id }
    const glowGeo = new THREE.SphereGeometry(2.4 * 1.9, 12, 12)
    const glowMat = new THREE.MeshBasicMaterial({
      color: pal.workEdge,
      transparent: true,
      opacity: 0.3 * glowScale(),
      depthWrite: false,
      blending: THREE.AdditiveBlending,
      toneMapped: false
    })
    mesh.add(new THREE.Mesh(glowGeo, glowMat))
    scene.add(mesh)
    poemMeshes.push(mesh)
    items.push({ pos: pos.clone(), poetId })
  })

  createPoemLinks(items)
  applyLinkVisibility()
}

// 松弛迭代：诗作点两两推开，并让诗作点远离所有诗人星，强制满足最小间距。
// 同一位诗人麾下的诗作点使用更小的间距，既保持「诗作环绕诗人」的紧凑簇形，又不相互重叠
function relaxPoemPositions(nodes: { pos: THREE.Vector3; poetId?: number }[]) {
  const n = nodes.length
  if (n === 0) return

  const ITERATIONS = 8
  const poetPositions = [...poetPositionMap.values()]

  for (let iter = 0; iter < ITERATIONS; iter++) {
    // 1) 诗作点两两推开
    for (let i = 0; i < n; i++) {
      const a = nodes[i].pos
      for (let j = i + 1; j < n; j++) {
        const b = nodes[j].pos
        const minGap = nodes[i].poetId != null && nodes[i].poetId === nodes[j].poetId
          ? POEM_MIN_GAP * 0.6
          : POEM_MIN_GAP
        let dx = a.x - b.x
        let dy = a.y - b.y
        let dz = a.z - b.z
        let d2 = dx * dx + dy * dy + dz * dz
        if (d2 >= minGap * minGap) continue
        let d = Math.sqrt(d2)
        if (d < 1e-4) {
          // 完全重合时给一个随机方向，避免除零
          dx = Math.random() - 0.5
          dy = Math.random() - 0.5
          dz = Math.random() - 0.5
          d = Math.sqrt(dx * dx + dy * dy + dz * dz) || 1
        }
        const k = ((minGap - d) * 0.5) / d
        dx *= k
        dy *= k
        dz *= k
        a.x += dx; a.y += dy; a.z += dz
        b.x -= dx; b.y -= dy; b.z -= dz
      }
    }
    // 2) 诗作点远离所有诗人星，避免被星体与光晕吞没
    for (let i = 0; i < n; i++) {
      const p = nodes[i].pos
      for (const pp of poetPositions) {
        let dx = p.x - pp.x
        let dy = p.y - pp.y
        let dz = p.z - pp.z
        const d2 = dx * dx + dy * dy + dz * dz
        if (d2 >= POEM_POET_MIN_GAP * POEM_POET_MIN_GAP) continue
        let d = Math.sqrt(d2)
        if (d < 1e-4) {
          dx = Math.random() - 0.5
          dy = Math.random() - 0.5
          dz = Math.random() - 0.5
          d = Math.sqrt(dx * dx + dy * dy + dz * dz) || 1
        }
        const k = POEM_POET_MIN_GAP / d
        p.x = pp.x + dx * k
        p.y = pp.y + dy * k
        p.z = pp.z + dz * k
      }
    }
  }
}

function clearPoemNodes() {
  poemMeshes.forEach(m => {
    scene.remove(m)
    m.geometry.dispose()
    ;(m.material as THREE.Material).dispose()
  })
  poemMeshes = []
}

function clearPoemLinks() {
  poemLinkGroups.forEach(({ line }) => {
    scene.remove(line)
    line.geometry.dispose()
    ;(line.material as THREE.Material).dispose()
  })
  poemLinkGroups = []
}

// 渲染「诗人↔诗作」连线：按诗人分组，每组一个 LineSegments2，
// 以便聚焦某位诗人时只高亮其所属连线（LineMaterial 为材质级属性，无法逐段设置颜色/透明度）
function createPoemLinks(items: { pos: THREE.Vector3; poetId?: number }[]) {
  clearPoemLinks()

  // poetId → 扁平的线段端点坐标 [poetX,poetY,poetZ, poemX,poemY,poemZ, ...]
  const byPoet = new Map<number, number[]>()
  items.forEach(item => {
    if (item.poetId == null) return
    const poetPos = poetPositionMap.get(item.poetId)
    if (!poetPos) return
    if (!byPoet.has(item.poetId)) byPoet.set(item.poetId, [])
    byPoet.get(item.poetId)!.push(
      poetPos.x, poetPos.y, poetPos.z,
      item.pos.x, item.pos.y, item.pos.z
    )
  })

  const w = canvasRef.value?.clientWidth ?? window.innerWidth
  const h = canvasRef.value?.clientHeight ?? window.innerHeight

  byPoet.forEach((positions, poetId) => {
    const geo = new LineSegmentsGeometry()
    geo.setPositions(positions)
    const mat = new LineMaterial({
      color: new THREE.Color(currentPalette.value.pw),
      linewidth: POEM_LINK_WIDTH,
      transparent: true,
      opacity: clamp01(LINK_OPACITY_NORMAL * lineScale()),
      depthWrite: false
    })
    // 关闭色调映射，避免 ACES 把高亮连线压暗，保证在深色背景上对比明显
    mat.toneMapped = false
    mat.resolution.set(w, h)
    const line = new LineSegments2(geo, mat)
    line.userData = { poetId }
    scene.add(line)
    poemLinkGroups.push({ poetId, line })
  })
}

// ==================== 连线三态样式 ====================
type LinkState = 'normal' | 'highlight' | 'selected' | 'dim'

// 连线三态样式：baseColor 为当前方案下该类连线的普通色（诗人线取 pp、诗作线取 pw），
// 高亮 / 选中 / 透明度均由方案配色与「连线可见度」滑杆派生
function setLinkStyle(mat: LineMaterial, state: LinkState, baseWidth: number, baseColor: string) {
  const pal = currentPalette.value
  const ls = lineScale()
  switch (state) {
    case 'selected':
      mat.color.set(pal.sel ?? pal.poetEdge)
      mat.opacity = clamp01(LINK_OPACITY_SELECTED * ls)
      mat.linewidth = baseWidth + 1.5
      break
    case 'highlight':
      mat.color.set(pal.hl ?? lighten(baseColor, 0.55))
      mat.opacity = clamp01(LINK_OPACITY_HIGHLIGHT * ls)
      mat.linewidth = baseWidth + 0.5
      break
    case 'dim':
      mat.color.set(baseColor)
      mat.opacity = clamp01(LINK_OPACITY_DIM * ls)
      mat.linewidth = baseWidth
      break
    default:
      mat.color.set(baseColor)
      mat.opacity = clamp01(LINK_OPACITY_NORMAL * ls)
      mat.linewidth = baseWidth
  }
  mat.needsUpdate = true
}

// 以某位诗人为焦点统一设置两类连线：关联线用 state，其余压暗
function applyLinkFocus(poetId: number, state: 'highlight' | 'selected') {
  const pal = currentPalette.value
  connectionLines.forEach(line => {
    const related = line.userData.poetA === poetId || line.userData.poetB === poetId
    setLinkStyle(line.material as LineMaterial, related ? state : 'dim', CONNECT_LINE_WIDTH, pal.pp)
  })
  poemLinkGroups.forEach(({ poetId: pid, line }) => {
    setLinkStyle(line.material as LineMaterial, pid === poetId ? state : 'dim', POEM_LINK_WIDTH, pal.pw)
  })
}

// 选中诗人：关系线用方案强调色、略粗
function highlightConnections(poetId: number) {
  applyLinkFocus(poetId, 'selected')
}

// 悬停诗人：关系线用高亮色强调
function hoverConnections(poetId: number) {
  applyLinkFocus(poetId, 'highlight')
}

// 恢复所有连线为普通态
function resetConnections() {
  const pal = currentPalette.value
  connectionLines.forEach(line => setLinkStyle(line.material as LineMaterial, 'normal', CONNECT_LINE_WIDTH, pal.pp))
  poemLinkGroups.forEach(({ line }) => setLinkStyle(line.material as LineMaterial, 'normal', POEM_LINK_WIDTH, pal.pw))
}

// 统一应用「显示/隐藏连线」开关（重建节点与连线后也需调用）
function applyLinkVisibility() {
  poemLinkGroups.forEach(({ line }) => { line.visible = showLinks.value })
  connectionLines.forEach(l => { l.visible = showLinks.value })
}

// ==================== 配色实验室：应用参数 ====================
// 把当前方案与滑杆参数实时应用到 3D 场景（切方案、拖滑杆、重置时调用）
function applyVizStyles() {
  if (!renderer || !scene) return
  const pal = currentPalette.value
  nodeScale = viz.size / 100
  const gs = glowScale()

  // 背景 / 雾效跟随方案
  renderer.setClearColor(new THREE.Color(pal.bg))
  ;(scene.fog as THREE.FogExp2).color.set(pal.bg)

  // 诗人主节点：颜色（朝代色或方案色）+ 光晕
  poetMeshes.forEach(mesh => {
    const poet = mesh.userData.poet as Poet | undefined
    const dynColor = getDynastyColor(poet?.dynastyName)
    const core = viz.dynastyPoet ? dynColor : new THREE.Color(pal.poet)
    const edge = viz.dynastyPoet ? dynColor : new THREE.Color(pal.poetEdge)
    const mat = mesh.material as THREE.MeshBasicMaterial
    mat.color.copy(core)
    mat.needsUpdate = true
    const glow = mesh.children[0] as THREE.Mesh | undefined
    if (glow) {
      const gm = glow.material as THREE.MeshBasicMaterial
      gm.color.copy(edge)
      gm.opacity = (mesh === selectedMesh ? 0.5 : 0.12) * gs
      gm.needsUpdate = true
    }
  })

  // 诗作从节点：按当前聚焦状态重设颜色、光晕与尺寸
  refreshPoemStates()
  // 连线：先整体复位为普通态（应用新配色与新透明度），后续交互再行高亮
  resetConnections()
  applyLinkVisibility()
}

// 切换配色方案（方案自带是否按朝代着色诗人的默认值）
function selectVizPalette(i: number) {
  viz.palette = i
  viz.dynastyPoet = !!VIZ_PALETTES[i].dynasty
  applyVizStyles()
}

// 重置实验室参数为页面默认观感
function resetViz() {
  viz.palette = 0
  viz.glow = 65
  viz.line = 55
  viz.size = 100
  viz.brightness = 100
  viz.labels = true
  viz.dynastyPoet = true
  applyVizStyles()
}

// ==================== 自动环绕 ====================
let autoRotateResumeTimer: ReturnType<typeof setTimeout> | null = null

// 用户开始拖动/缩放：立即暂停自动环绕
function handleControlStart() {
  controls.autoRotate = false
  if (autoRotateResumeTimer) {
    clearTimeout(autoRotateResumeTimer)
    autoRotateResumeTimer = null
  }
}

// 用户松手：延迟一段时间后按开关状态恢复自动环绕，避免刚松手就立刻重启
function handleControlEnd() {
  if (autoRotateResumeTimer) clearTimeout(autoRotateResumeTimer)
  autoRotateResumeTimer = setTimeout(() => {
    autoRotateResumeTimer = null
    controls.autoRotate = autoOrbit.value
  }, AUTO_ROTATE_RESUME_DELAY)
}

// 「自动环绕」开关切换
function applyAutoOrbit() {
  if (autoRotateResumeTimer) {
    clearTimeout(autoRotateResumeTimer)
    autoRotateResumeTimer = null
  }
  controls.autoRotate = autoOrbit.value
}

function animate() {
  animationId = requestAnimationFrame(animate)
  const elapsed = clock.getElapsedTime()

  controls.update()

  // 背景星缓慢旋转
  if (bgStars) {
    bgStars.rotation.y = elapsed * 0.003
  }

  // 诗人星点脉动（选中星强化）；尺寸与光晕倍率由「配色实验室」控制
  const vizGs = glowScale()
  poetMeshes.forEach((mesh, i) => {
    const isSelected = mesh === selectedMesh
    const base = isSelected ? 1.18 : 1.0
    const speed = isSelected ? 4 : 1.5
    const pulse = Math.sin(elapsed * speed + i * 0.7) * (isSelected ? 0.16 : 0.08)
    mesh.scale.setScalar((base + pulse) * nodeScale)

    const glow = mesh.children[0] as THREE.Mesh | undefined
    if (glow) {
      const gMat = glow.material as THREE.MeshBasicMaterial
      if (isSelected) {
        const glowPulse = 2.1 + Math.sin(elapsed * 4) * 0.4
        glow.scale.setScalar(glowPulse)
        const target = 0.5 * vizGs
        if (gMat.opacity !== target) { gMat.opacity = target; gMat.needsUpdate = true }
      } else {
        const glowPulse = 1.0 + Math.sin(elapsed * 1.2 + i * 0.5) * 0.15
        glow.scale.setScalar(glowPulse)
        const target = 0.12 * vizGs
        if (gMat.opacity !== target) { gMat.opacity = target; gMat.needsUpdate = true }
      }
    }
  })

  // 虚空新星脉动
  if (voidStar) {
    const vPulse = 1.1 + Math.sin(elapsed * 3.5) * 0.3
    voidStar.scale.setScalar(vPulse)
    const vGlow = voidStar.children[0] as THREE.Mesh | undefined
    if (vGlow) {
      vGlow.scale.setScalar(1 + Math.sin(elapsed * 2.2) * 0.25)
    }
  }

  // 屏幕像素级命中检测（保证小节点也能被鼠标悬浮到）
  const hit = findNodeAtPx(mousePx.x, mousePx.y, 18)

  if (hoveredMesh && hit !== hoveredMesh) {
    const wasPoet = !!hoveredMesh.userData.poet
    const mat = hoveredMesh.material as THREE.MeshBasicMaterial
    if (wasPoet) {
      mat.opacity = 0.95
      // 恢复悬停时提亮的光晕
      if (hoveredMesh !== selectedMesh) {
        const g = hoveredMesh.children[0] as THREE.Mesh | undefined
        if (g) {
          const gMat = g.material as THREE.MeshBasicMaterial
          gMat.opacity = 0.1
          gMat.needsUpdate = true
        }
      }
      // 离开诗人星：已选中则维持选中态连线，否则恢复普通态
      if (selectedPoet.value) highlightConnections(selectedPoet.value.id)
      else resetConnections()
    } else {
      // 诗作点：按当前聚焦状态恢复（聚焦某诗人时，非其作品保持淡出）
      const poetId = selectedPoet.value?.id
      const state = poetId != null
        ? (hoveredMesh.userData.poetId === poetId ? 'highlight' : 'dim')
        : 'normal'
      applyPoemState(hoveredMesh, state)
    }
    hoveredMesh = null
    hoverInfo.value = null
    document.body.style.cursor = 'default'
  }

  if (hit && hit !== hoveredMesh) {
    hoveredMesh = hit
    const isPoet = !!hit.userData.poet
    const mat = hit.material as THREE.MeshBasicMaterial
    if (isPoet) {
      mat.opacity = 1.0
      const poet = hit.userData.poet as Poet
      hoverInfo.value = {
        type: 'poet',
        title: poet.name,
        meta: poet.dynastyName || '未知朝代',
        color: `#${mat.color.getHexString()}`
      }
      // 悬停星点光晕轻微提亮（选中星除外，保持选中强调）
      if (hit !== selectedMesh) {
        const hGlow = hit.children[0] as THREE.Mesh | undefined
        if (hGlow) {
          const hMat = hGlow.material as THREE.MeshBasicMaterial
          hMat.opacity = 0.3
          hMat.needsUpdate = true
        }
      }
      // 悬停诗人：青色高亮其关系连线（已选中某诗人时不覆盖选中态）
      if (!selectedPoet.value) hoverConnections(poet.id)
    } else {
      // 诗作点：悬停取高亮色
      applyPoemState(hit, 'highlight')
      // 诗作点：显示诗名 + 作者（列表接口不返回作者/朝代，回退到所属诗人）
      const poem = hit.userData.poem as Poem
      const poetOfPoem = poem.poetId != null ? poets.value.find(pt => pt.id === poem.poetId) : undefined
      const author = poem.poetName || poetOfPoem?.name || '佚名'
      const dynasty = poem.dynastyName || poetOfPoem?.dynastyName
      hoverInfo.value = {
        type: 'poem',
        title: poem.title,
        meta: `${author}${dynasty ? ' · ' + dynasty : ''}${poem.categoryName ? ' · ' + poem.categoryName : ''}`,
        color: `#${mat.color.getHexString()}`
      }
    }
    document.body.style.cursor = 'pointer'
  }

  // 诗人姓名标注跟随（投影到屏幕）
  updatePoetLabels()

  renderer.render(scene, camera)
}

// 屏幕像素级命中检测：把节点投影到屏幕坐标，返回距鼠标最近且在阈值内的节点
function findNodeAtPx(px: number, py: number, thresholdPx: number): THREE.Mesh | null {
  const w = canvasRef.value?.clientWidth ?? 1
  const h = canvasRef.value?.clientHeight ?? 1
  const nodes = [...poetMeshes, ...poemMeshes]
  let best: THREE.Mesh | null = null
  let bestDist = thresholdPx
  for (const m of nodes) {
    const v = m.getWorldPosition(new THREE.Vector3()).project(camera)
    if (v.z > 1 || v.z < -1) continue
    const sx = ((v.x + 1) / 2) * w
    const sy = ((1 - v.y) / 2) * h
    const d = Math.hypot(sx - px, sy - py)
    if (d < bestDist) {
      bestDist = d
      best = m
    }
  }
  return best
}

function handleMouseMove(event: MouseEvent) {
  if (!canvasRef.value) return
  const rect = canvasRef.value.getBoundingClientRect()
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
  mousePx.x = event.clientX - rect.left
  mousePx.y = event.clientY - rect.top
  tooltipPos.value = { x: event.clientX, y: event.clientY }
}

function handlePointerDown(event: MouseEvent) {
  pointerDownPos = { x: event.clientX, y: event.clientY }
}

function handleClick(event: MouseEvent) {
  if (!canvasRef.value) return
  // 若刚刚发生了拖动（旋转/平移星云），则不视为点击，避免误选中节点或弹出虚空之诗
  const movedPx = Math.hypot(event.clientX - pointerDownPos.x, event.clientY - pointerDownPos.y)
  if (movedPx > 5) return
  // 点击画布后中央提示文字淡出
  centerTitleVisible.value = false
  const rect = canvasRef.value.getBoundingClientRect()
  const px = event.clientX - rect.left
  const py = event.clientY - rect.top
  const clickMouse = new THREE.Vector2(
    ((event.clientX - rect.left) / rect.width) * 2 - 1,
    -((event.clientY - rect.top) / rect.height) * 2 + 1
  )

  // 屏幕像素级命中检测（阈值稍大于 hover，便于点击）
  const mesh = findNodeAtPx(px, py, 22)

  if (mesh) {
    if (mesh.userData.poet) {
      // 命中诗人星 → 选中诗人并聚焦
      const poet = mesh.userData.poet as Poet
      selectPoet(poet)
      flyToStar(mesh.getWorldPosition(new THREE.Vector3()))
    } else {
      // 命中诗作点 → 打开诗作详情浮层
      selectPoem(mesh.userData.poem as Poem)
    }
  } else {
    // 点击虚空：打开页面后的首次点击仅收敛中央提示，不立即浮现诗
    if (firstVoidClick) {
      firstVoidClick = false
      return
    }
    // 在该处生成新星并飞向，随后浮现诗
    closePanel()
    pullVacuumPoem()
    spawnVoidStarAt(clickMouse)
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
  // 更新 LineMaterial 的 resolution，保证线宽在窗口缩放后仍正确
  poemLinkGroups.forEach(({ line }) => {
    ;(line.material as LineMaterial).resolution.set(w, h)
  })
  connectionLines.forEach(l => {
    ;(l.material as LineMaterial).resolution.set(w, h)
  })
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
    // 构建诗人诗作数映射（用于诗人星大小）
    poemCountMap.clear()
    allPoems.value.forEach(p => {
      if (p.poetId != null) poemCountMap.set(p.poetId, (poemCountMap.get(p.poetId) ?? 0) + 1)
    })
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
  voidPoem.value = null
  selectedPoem.value = null
  removeVoidStar()
  highlightConnections(poet.id)
  loadPoetPoems(poet.id)
  setSelectedStar(poet.id)
  focusPoetPoems(poet.id)
}

function setSelectedStar(poetId: number) {
  selectedMesh = poetMeshes.find(m => m.userData.poet?.id === poetId) || null
}

function clearSelectedStar() {
  selectedMesh = null
}

// 诗作点三态：normal 普通 / highlight 高亮 / dim 淡出（颜色与光晕均跟随当前方案）
function applyPoemState(mesh: THREE.Mesh, state: 'normal' | 'highlight' | 'dim') {
  const pal = currentPalette.value
  const gs = glowScale()
  const mat = mesh.material as THREE.MeshBasicMaterial
  const glow = mesh.children[0] as THREE.Mesh | undefined
  const gMat = glow?.material as THREE.MeshBasicMaterial | undefined
  if (state === 'highlight') {
    mat.color.set(pal.workHi!)
    mat.opacity = 1.0
    mesh.scale.setScalar(1.35 * nodeScale)
    if (gMat) { gMat.color.set(pal.workHi!); gMat.opacity = 0.55 * gs }
  } else if (state === 'dim') {
    mat.color.set(pal.work)
    mat.opacity = 0.12
    mesh.scale.setScalar(nodeScale)
    if (gMat) { gMat.color.set(pal.workEdge); gMat.opacity = 0.05 * gs }
  } else {
    mat.color.set(pal.work)
    mat.opacity = 0.9
    mesh.scale.setScalar(nodeScale)
    if (gMat) { gMat.color.set(pal.workEdge); gMat.opacity = 0.3 * gs }
  }
  mat.needsUpdate = true
  if (gMat) gMat.needsUpdate = true
}

// 依据当前聚焦的诗人为所有诗作点重新应用状态（切换方案 / 尺寸后刷新用）
function refreshPoemStates() {
  const poetId = selectedPoet.value?.id
  poemMeshes.forEach(m => {
    const state = poetId == null
      ? 'normal'
      : (m.userData.poetId === poetId ? 'highlight' : 'dim')
    applyPoemState(m, state)
  })
}

// 单诗人聚焦：该诗人诗作高亮（更亮色 + 放大），其余淡出
function focusPoetPoems(poetId: number) {
  poemMeshes.forEach(m => {
    applyPoemState(m, m.userData.poetId === poetId ? 'highlight' : 'dim')
  })
}

function resetPoemOpacity() {
  poemMeshes.forEach(m => applyPoemState(m, 'normal'))
}

function selectPoem(poem: Poem) {
  selectedPoem.value = poem
  voidPoem.value = null
  removeVoidStar()
  closePanel()
}

function closePoemPanel() {
  selectedPoem.value = null
}

function closePanel() {
  showPanel.value = false
  selectedPoet.value = null
  poetPoems.value = []
  resetConnections()
  clearSelectedStar()
  resetPoemOpacity()
}

function closeVoidPanel() {
  voidPoem.value = null
  offerRandomDisabled.value = false
  removeVoidStar()
}

// 相机回归原点：关闭所有面板后平滑飞回初始视角，防止迷失星云
function backToOrigin() {
  closePanel()
  closePoemPanel()
  closeVoidPanel()
  const start = camera.position.clone()
  const startTarget = controls.target.clone()
  const duration = 800
  const startTime = performance.now()
  function step(now: number) {
    const t = Math.min((now - startTime) / duration, 1)
    const ease = t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2
    camera.position.lerpVectors(start, CAMERA_HOME_POS, ease)
    controls.target.lerpVectors(startTarget, CAMERA_HOME_TARGET, ease)
    if (t < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

// 点击虚空 → 在当前朝代范围内随机浮现一首诗（参考「诗云」的虚空取诗）
function pullVacuumPoem() {
  if (offerRandomDisabled.value) return
  offerRandomDisabled.value = true
  setTimeout(() => { offerRandomDisabled.value = false }, 600)
  const pool = selectedDynasty.value === 'all'
    ? allPoems.value
    : allPoems.value.filter(p => p.dynastyName === selectedDynasty.value)
  if (pool.length === 0) {
    ElMessage.info('当前星海空空，先放宽筛选吧')
    return
  }
  voidPoem.value = pool[Math.floor(Math.random() * pool.length)]
}

// 在点击方向生成一颗「虚空新星」，并让相机飞向它
function spawnVoidStarAt(ndc: THREE.Vector2) {
  removeVoidStar()
  raycaster.setFromCamera(ndc, camera)
  // 取点击射线在星云中存在感最强的深度（相机到旋转中心距离处）
  const dist = camera.position.distanceTo(controls.target)
  const pos = raycaster.ray.at(dist, new THREE.Vector3())

  const color = new THREE.Color('#8ac4ff')
  const geometry = new THREE.SphereGeometry(1.1, 16, 16)
  const material = new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.95 })
  voidStar = new THREE.Mesh(geometry, material)
  voidStar.position.copy(pos)

  // 光晕
  const glowGeo = new THREE.SphereGeometry(3, 16, 16)
  const glowMat = new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.28, depthWrite: false })
  const glow = new THREE.Mesh(glowGeo, glowMat)
  voidStar.add(glow)

  scene.add(voidStar)
  flyToStar(pos)
}

function removeVoidStar() {
  if (!voidStar) return
  scene.remove(voidStar)
  voidStar.geometry.dispose()
  ;(voidStar.material as THREE.Material).dispose()
  const glow = voidStar.children[0] as THREE.Mesh | undefined
  if (glow) {
    glow.geometry.dispose()
    ;(glow.material as THREE.Material).dispose()
  }
  voidStar = null
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

  // 中央提示文字若干秒后自动淡出，让星云完整显露
  titleTimer = setTimeout(() => {
    centerTitleVisible.value = false
  }, 6000)

  window.addEventListener('resize', handleResize)
  canvasRef.value?.addEventListener('mousemove', handleMouseMove)
  canvasRef.value?.addEventListener('mousedown', handlePointerDown)
  canvasRef.value?.addEventListener('click', handleClick)

  await loadData()
  createPoetStars(poets.value)
  createPoemCloud(allPoems.value)
  // 首次渲染按「配色实验室」默认方案统一着色
  applyVizStyles()
})

onUnmounted(() => {
  if (titleTimer) clearTimeout(titleTimer)
  cancelAnimationFrame(animationId)
  window.removeEventListener('resize', handleResize)
  canvasRef.value?.removeEventListener('mousemove', handleMouseMove)
  canvasRef.value?.removeEventListener('mousedown', handlePointerDown)
  canvasRef.value?.removeEventListener('click', handleClick)

  removeVoidStar()

  // 清理诗人网格
  poetMeshes.forEach(m => {
    scene.remove(m)
    m.geometry.dispose()
    ;(m.material as THREE.Material).dispose()
  })

  // 清理连线
  connectionLines.forEach(l => {
    scene.remove(l)
    l.geometry.dispose()
    ;(l.material as THREE.Material).dispose()
  })

  // 清理诗作节点与连线
  clearPoemNodes()
  clearPoemLinks()

  // 清理背景星
  if (bgStars) {
    scene.remove(bgStars)
    bgStars.geometry.dispose()
    ;(bgStars.material as THREE.Material).dispose()
  }

  // 清理诗人姓名标签
  clearPoetLabels()

  // 清理位置缓存
  poetPositionMap.clear()

  // 清理自动环绕相关监听与定时器
  if (autoRotateResumeTimer) {
    clearTimeout(autoRotateResumeTimer)
    autoRotateResumeTimer = null
  }
  controls?.removeEventListener('start', handleControlStart)
  controls?.removeEventListener('end', handleControlEnd)

  scene?.clear()
  renderer?.dispose()
  controls?.dispose()
})
</script>

<template>
  <div class="poetry-cloud">
    <!-- Three.js 画布（整体亮度由「配色实验室」滑杆经 CSS filter 控制） -->
    <div ref="canvasRef" class="canvas-container" :style="{ filter: canvasFilter }" />
    <!-- 诗人姓名标注层（跟随诗人星的屏幕投影） -->
    <div ref="labelLayerRef" class="label-layer" />

    <!-- 悬浮提示：显示节点含义 -->
    <div
      v-if="hoverInfo"
      class="hover-tip"
      :style="{ left: tooltipPos.x + 'px', top: tooltipPos.y + 'px', '--tip-color': hoverInfo.color }"
    >
      <span class="tip-type">{{ hoverInfo.type === 'poet' ? '诗人' : '诗作' }}</span>
      <span class="tip-title">{{ hoverInfo.title }}</span>
      <span class="tip-meta">{{ hoverInfo.meta }}</span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner" />
      <p class="loading-text">正在加载星空数据...</p>
    </div>

    <!-- 中央标题（自动或点击后淡出，露出星云） -->
    <transition name="fade">
      <div v-if="centerTitleVisible && !showPanel && !voidPoem && !selectedPoem && !loading" class="center-title">
        <h1 class="title-main calligraphy">诗云</h1>
        <p class="title-sub">每位诗人是一颗星，每首诗都有自己的坐标</p>
        <p class="title-hint">鼠标拖动旋转 · 滚轮缩放 · 点击星点探索诗人 · 点击虚空随机得诗</p>
      </div>
    </transition>

    <!-- 虚空之诗（点击星空空白处随机浮现一首诗） -->
    <transition name="fade">
      <div v-if="voidPoem" class="void-card glass-panel">
        <div class="void-head">
          <span class="void-badge calligraphy">虚空之诗</span>
          <button class="void-close" title="关闭" @click="closeVoidPanel">✕</button>
        </div>
        <h3 class="void-title">{{ voidPoem.title }}</h3>
        <p class="void-meta">
          {{ voidPoem.poetName }} · {{ voidPoem.dynastyName || '' }}
          {{ voidPoem.categoryName ? '· ' + voidPoem.categoryName : '' }}
        </p>
        <p class="void-content">{{ voidPoem.content }}</p>
        <div class="void-actions">
          <button class="void-btn primary" @click="pullVacuumPoem">再取一首</button>
          <button class="void-btn" @click="router.push(`/poem/${voidPoem.id}`)">查看详情</button>
        </div>
      </div>
    </transition>

    <!-- 诗作详情浮层（点击诗作点查看） -->
    <transition name="fade">
      <div v-if="selectedPoem" class="void-card glass-panel">
        <div class="void-head">
          <span class="void-badge calligraphy">诗作</span>
          <button class="void-close" title="关闭" @click="closePoemPanel">✕</button>
        </div>
        <h3 class="void-title">{{ selectedPoem.title }}</h3>
        <p class="void-meta">
          {{ selectedPoem.poetName }} · {{ selectedPoem.dynastyName || '' }}
          {{ selectedPoem.categoryName ? '· ' + selectedPoem.categoryName : '' }}
        </p>
        <p class="void-content">{{ selectedPoem.content }}</p>
        <!-- 跳转诗作详情页按钮（与「虚空之诗」保持一致，保留跳转入口） -->
        <div class="void-actions">
          <button class="void-btn primary" @click="router.push(`/poem/${selectedPoem.id}`)">查看详情</button>
        </div>
      </div>
    </transition>

    <!-- 左侧控制面板 -->
    <aside class="control-panel glass-panel">
      <h2 class="panel-title calligraphy">星海罗盘</h2>

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
        <h3 class="filter-label">图例</h3>
        <div class="legend-items">
          <div class="legend-item">
            <span class="legend-dot" style="background:#ffd24a" />
            <span>普通诗作点</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot" style="background:#fff2a8" />
            <span>高亮诗作点</span>
          </div>
          <div class="legend-item">
            <span class="legend-line" style="background:#ffffff" />
            <span>普通连线</span>
          </div>
          <div class="legend-item">
            <span class="legend-line" style="background:#66e0e6" />
            <span>高亮连线</span>
          </div>
          <div class="legend-item">
            <span class="legend-line" style="background:#ffb545" />
            <span>选中关系线</span>
          </div>
        </div>
      </div>

      <div class="legend-section">
        <h3 class="filter-label">操作说明</h3>
        <div class="legend-items">
          <div class="legend-item">
            <span class="legend-icon">🖱️</span>
            <span>左键拖动平移</span>
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
            <span class="legend-icon">🔄</span>
            <span>右键拖动旋转</span>
          </div>
        </div>
      </div>

      <div class="legend-section">
        <h3 class="filter-label">视图设置</h3>
        <div class="legend-items">
          <div class="legend-item switch-item">
            <span>显示连线</span>
            <el-switch v-model="showLinks" size="small" @change="applyLinkVisibility" />
          </div>
          <div class="legend-item switch-item">
            <span>自动环绕</span>
            <el-switch v-model="autoOrbit" size="small" @change="applyAutoOrbit" />
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

    <!-- 配色实验室：页面级可视化控制模块（配色 / 光晕 / 连线 / 尺寸 / 亮度 / 标签） -->
    <transition name="slide-up">
      <section v-if="showLab" class="viz-lab glass-panel" :style="{ right: labRight }">
        <div class="lab-head">
          <div class="lab-title-wrap">
            <h3 class="lab-title calligraphy">配色实验室</h3>
            <p class="lab-sub">选择方案后拖动参数，即时感受星图配色、光晕与线条的平衡</p>
          </div>
          <button class="lab-close" title="收起" @click="showLab = false">✕</button>
        </div>

        <div class="lab-body">
          <!-- 配色方案 -->
          <div class="lab-col lab-col-palette">
            <h4 class="lab-label">配色方案 <span class="lab-count">{{ VIZ_PALETTES.length }} 套</span></h4>
            <div class="palette-list">
              <button
                v-for="(p, i) in VIZ_PALETTES"
                :key="p.id"
                class="palette-chip"
                :class="{ active: viz.palette === i }"
                @click="selectVizPalette(i)"
              >
                <span class="chip-dots">
                  <i class="dot" :style="{ background: p.poet }" />
                  <i class="dot" :style="{ background: p.work }" />
                  <i class="dot" :style="{ background: p.pp }" />
                </span>
                <span class="chip-name">{{ p.name }}</span>
              </button>
            </div>
          </div>

          <!-- 参数微调 -->
          <div class="lab-col lab-col-sliders">
            <div class="lab-field">
              <label>光晕强度 <span>{{ viz.glow }}%</span></label>
              <input
                v-model.number="viz.glow"
                class="lab-range"
                type="range" min="0" max="130" step="1"
                @input="applyVizStyles"
              >
            </div>
            <div class="lab-field">
              <label>连线可见度 <span>{{ viz.line }}%</span></label>
              <input
                v-model.number="viz.line"
                class="lab-range"
                type="range" min="15" max="100" step="1"
                @input="applyVizStyles"
              >
            </div>
            <div class="lab-field">
              <label>节点尺寸 <span>{{ viz.size }}%</span></label>
              <input
                v-model.number="viz.size"
                class="lab-range"
                type="range" min="75" max="130" step="1"
                @input="applyVizStyles"
              >
            </div>
            <div class="lab-field">
              <label>整体亮度 <span>{{ viz.brightness }}%</span></label>
              <input v-model.number="viz.brightness" class="lab-range" type="range" min="50" max="150" step="1">
            </div>
          </div>

          <!-- 开关与操作 -->
          <div class="lab-col lab-col-toggles">
            <label class="lab-check">
              <input v-model="viz.labels" type="checkbox">
              <span>显示诗人姓名</span>
            </label>
            <label class="lab-check">
              <input v-model="viz.dynastyPoet" type="checkbox" @change="applyVizStyles">
              <span>诗人按朝代配色</span>
            </label>
            <button class="lab-btn" @click="resetViz">重置参数</button>
          </div>
        </div>
      </section>
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
        <span class="status-item clickable" :class="{ 'is-on': showLab }" @click="showLab = !showLab">配色实验室</span>
        <span class="status-item clickable" @click="backToOrigin">回归原点</span>
        <span class="status-item clickable" @click="goToPoetList">诗人列表</span>
        <span class="status-item clickable" @click="router.push('/')">返回首页</span>
      </div>
    </footer>
  </div>
</template>

<style scoped lang="scss">
$bg-deep: #0b1020;
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

  // 径向暗角：四周压暗、中心稍亮，把视觉重心引导到星图中央
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    pointer-events: none;
    background: radial-gradient(
      circle at 50% 45%,
      rgba(0, 0, 0, 0) 0%,
      rgba(0, 0, 0, 0) 38%,
      rgba(0, 0, 0, 0.62) 100%
    );
  }
}

/* ==================== 诗人姓名标注 ==================== */
// 标签由 JS（document.createElement）动态创建，不带 Vue scoped 的 data-v 属性，
// 故必须用 :deep() 穿透，否则所有 .poet-label 样式会被 scoped 选择器过滤掉而失效
.label-layer {
  position: absolute;
  inset: 0;
  z-index: 5;
  pointer-events: none;
  overflow: hidden;

  :deep(.poet-label) {
    position: absolute;
    transform: translate(-50%, -150%);
    white-space: nowrap;
    font-size: 12px;
    letter-spacing: 1px;
    color: rgba(232, 226, 208, 0.85);
    text-shadow: 0 0 6px rgba(0, 0, 0, 0.95), 0 1px 3px rgba(0, 0, 0, 0.95);
    pointer-events: none;
    will-change: left, top;
    transition: color 0.2s ease;

    &.is-selected {
      color: #ffd24a;
      font-weight: 700;
      text-shadow: 0 0 10px rgba(255, 210, 74, 0.65), 0 1px 3px rgba(0, 0, 0, 0.95);
    }
  }
}

/* ==================== 悬浮提示 ==================== */
.hover-tip {
  position: fixed;
  z-index: 60;
  transform: translate(16px, -50%);
  pointer-events: none;
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 14px;
  background: #fffdf5;
  border: 2px solid var(--tip-color, #e8a33d);
  border-radius: 10px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.65), 0 0 0 4px rgba(0, 0, 0, 0.25);
  white-space: nowrap;
  max-width: 280px;

  .tip-type {
    font-size: 11px;
    font-weight: 700;
    color: #a97c1c;
    letter-spacing: 2px;
  }

  .tip-title {
    font-size: 15px;
    font-weight: 700;
    color: #2a2438;
    letter-spacing: 1px;
    white-space: normal;
  }

  .tip-meta {
    font-size: 13px;
    color: #6b6478;
    white-space: normal;
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
  // 半透明底纹：与近黑星云背景区分，提升文字可读性
  padding: 28px 48px;
  background: rgba(22, 20, 44, 0.72);
  border: 1px solid rgba(205, 133, 63, 0.28);
  border-radius: 18px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(6px);

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

/* ==================== 虚空之诗浮层 ==================== */
.void-card {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 360px;
  max-width: 88vw;
  max-height: 70vh;
  overflow-y: auto;
  padding: 20px 22px 18px;
  z-index: 35;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5);

  .void-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;

    .void-badge {
      font-size: 18px;
      color: $text-star-accent;
      letter-spacing: 2px;
    }

    .void-close {
      background: transparent;
      border: none;
      color: $text-star-muted;
      font-size: 16px;
      cursor: pointer;
      padding: 4px 8px;

      &:hover { color: $text-star-primary; }
    }
  }

  .void-title {
    font-size: 20px;
    color: $text-star-primary;
    font-family: $font-family-poetry;
    letter-spacing: 2px;
    margin-bottom: 4px;
  }

  .void-meta {
    font-size: 13px;
    color: $text-star-secondary;
    margin-bottom: 14px;
  }

  .void-content {
    font-size: 16px;
    line-height: 2;
    color: $text-star-primary;
    font-family: $font-family-poetry;
    white-space: pre-wrap;
  }

  .void-actions {
    display: flex;
    gap: 10px;
    margin-top: 16px;

    .void-btn {
      flex: 1;
      padding: 8px 0;
      border-radius: 8px;
      border: 1px solid $border-star;
      background: transparent;
      color: $text-star-secondary;
      font-family: $font-family-base;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        border-color: $border-star-focus;
        color: $text-star-primary;
      }

      &.primary {
        background: linear-gradient(135deg, $primary-color, $accent-color);
        border: none;
        color: #fff;

        &:hover { filter: brightness(1.1); color: #fff; }
      }
    }
  }
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
  // 接近纯黑的深暗底色 + 右侧细金色分隔线，与星图区形成清晰边界
  background: rgba(13, 13, 24, 0.96);
  border-right: 1px solid rgba(205, 133, 63, 0.35);

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
      color: #d0d0d0;
      background: rgba(139, 69, 19, 0.08);
      border: 1px solid rgba(139, 69, 19, 0.15);
      border-radius: 5px;
      cursor: pointer;
      transition: all 0.2s ease;
      font-family: $font-family-base;

      &:hover {
        background: rgba(139, 69, 19, 0.2);
        border-color: rgba(139, 69, 19, 0.3);
        color: #f0f0f0;
      }

      // 选中态：高饱和金色填充 + 深色文字，保证在暗面板上足够醒目
      &.active {
        background: #ffb100;
        border-color: #ffb100;
        color: #0d0d18;
        font-weight: 600;
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
      gap: 8px;
      font-size: 12px;
      font-weight: 500;
      color: $text-star-secondary;

      .legend-icon {
        width: 18px;
        text-align: center;
        font-size: 12px;
      }

      // 色标放大并加微光，提升与星点的对应识别度
      .legend-dot {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        flex-shrink: 0;
        border: 1px solid rgba(255, 255, 255, 0.35);
        box-shadow: 0 0 8px rgba(255, 255, 255, 0.2);
      }

      // 连线色标：短横线，与圆形星点色标区分
      .legend-line {
        width: 18px;
        height: 3px;
        border-radius: 2px;
        flex-shrink: 0;
      }

      &.switch-item {
        justify-content: space-between;

        :deep(.el-switch__core) {
          border-color: rgba(205, 133, 63, 0.4);
          background: rgba(255, 255, 255, 0.12);
        }

        :deep(.el-switch.is-checked .el-switch__core) {
          background: #ffb100;
          border-color: #ffb100;
        }
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
    &.is-on { color: $text-star-accent; }
  }
}

/* ==================== 配色实验室 ==================== */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}
.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

.viz-lab {
  position: fixed;
  left: 220px;
  right: 0;
  bottom: 40px;
  z-index: 45;
  max-height: 48vh;
  overflow-y: auto;
  padding: 16px 20px 18px;
  background: rgba(10, 10, 26, 0.94);
  backdrop-filter: blur(14px);
  border-top: 1px solid $border-star-focus;
  transition: right 0.35s ease;

  .lab-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 14px;

    .lab-title {
      font-size: 18px;
      color: $text-star-accent;
      margin: 0;
      letter-spacing: 2px;
    }

    .lab-sub {
      margin: 4px 0 0;
      font-size: 12px;
      color: $text-star-muted;
    }

    .lab-close {
      flex-shrink: 0;
      width: 26px;
      height: 26px;
      border-radius: 6px;
      background: transparent;
      border: 1px solid $border-star;
      color: $text-star-secondary;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        border-color: $border-star-focus;
        color: $text-star-accent;
      }
    }
  }

  .lab-body {
    display: grid;
    grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr) minmax(0, 0.7fr);
    gap: 22px;
    align-items: start;
  }

  .lab-label {
    font-size: 12px;
    font-weight: 500;
    color: $text-star-secondary;
    margin: 0 0 10px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .lab-count {
      font-size: 11px;
      color: $text-star-muted;
      font-weight: 400;
    }
  }

  // 配色方案 chip
  .palette-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
    gap: 8px;
  }

  .palette-chip {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 10px;
    border-radius: 9px;
    border: 1px solid $border-star;
    background: rgba(255, 255, 255, 0.03);
    color: $text-star-secondary;
    font-size: 12px;
    font-family: $font-family-base;
    cursor: pointer;
    transition: all 0.2s ease;
    text-align: left;

    &:hover {
      border-color: $border-star-focus;
      transform: translateY(-1px);
    }

    &.active {
      border-color: $text-star-accent;
      color: $text-star-primary;
      background: rgba(205, 133, 63, 0.12);
      box-shadow: 0 0 12px rgba(205, 133, 63, 0.2);
    }

    .chip-dots {
      display: flex;
      gap: 3px;
      flex-shrink: 0;

      .dot {
        width: 9px;
        height: 9px;
        border-radius: 50%;
        border: 1px solid rgba(255, 255, 255, 0.18);
      }
    }

    .chip-name {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  // 参数滑杆
  .lab-field {
    margin-bottom: 12px;

    label {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: $text-star-secondary;
      margin-bottom: 6px;

      span { color: $text-star-accent; }
    }
  }

  .lab-range {
    width: 100%;
    accent-color: $text-star-accent;
    cursor: pointer;
  }

  // 开关与操作
  .lab-col-toggles {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .lab-check {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: $text-star-secondary;
    cursor: pointer;

    input {
      accent-color: $text-star-accent;
      cursor: pointer;
    }
  }

  .lab-btn {
    margin-top: 4px;
    padding: 8px 12px;
    border-radius: 8px;
    border: 1px solid $border-star;
    background: rgba(255, 255, 255, 0.04);
    color: $text-star-secondary;
    font-size: 12px;
    font-family: $font-family-base;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      border-color: $border-star-focus;
      color: $text-star-accent;
    }
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

  .viz-lab {
    left: 190px;

    .lab-body {
      grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
    }
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

  // 小屏改为顶部抽屉，避免与底部控制台 / 信息面板重叠
  .viz-lab {
    top: 8px;
    left: 8px;
    right: 8px;
    bottom: auto;
    max-height: 62vh;
    border: 1px solid $border-star-focus;
    border-radius: 14px;

    .lab-body {
      grid-template-columns: 1fr;
      gap: 14px;
    }

    .palette-list {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
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
