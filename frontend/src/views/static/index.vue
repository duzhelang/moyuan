<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getStaticPageByPageKey } from '@/api/modules/static-page'

const route = useRoute()

const pageType = computed(() => route.params.type as string)

const isAboutPage = computed(() => pageType.value === 'about')

const pageData = ref<any>(null)
const pageLoading = ref(false)

const fetchPageContent = async (key: string) => {
  if (key === 'about') return
  pageLoading.value = true
  try {
    const res = await getStaticPageByPageKey(key)
    if (res.data) {
      pageData.value = res.data
    }
  } catch {
    // 获取失败时使用默认内容
  } finally {
    pageLoading.value = false
  }
}

watch(() => pageType.value, (newType) => {
  if (newType && newType !== 'about') {
    pageData.value = null
    fetchPageContent(newType)
  }
})

// 粒子动画相关
const heroCanvasRef = ref<HTMLCanvasElement | null>(null)
const contentCanvasRef = ref<HTMLCanvasElement | null>(null)
const animationIds: number[] = []

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  size: number
  opacity: number
  color: string
}

const createParticles = (
  canvas: HTMLCanvasElement,
  count: number,
  colors: string[],
  opacityRange: [number, number] = [0.3, 0.6],
  sizeRange: [number, number] = [2, 4]
) => {
  const particles: Particle[] = []
  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.5,
      vy: (Math.random() - 0.5) * 0.5,
      size: Math.random() * sizeRange[1] + sizeRange[0],
      opacity: Math.random() * (opacityRange[1] - opacityRange[0]) + opacityRange[0],
      color: colors[Math.floor(Math.random() * colors.length)]
    })
  }
  return particles
}

const updateParticles = (particles: Particle[], canvas: HTMLCanvasElement) => {
  particles.forEach(p => {
    p.x += p.vx
    p.y += p.vy

    if (p.x < 0 || p.x > canvas.width) p.vx *= -1
    if (p.y < 0 || p.y > canvas.height) p.vy *= -1

    p.x = Math.max(0, Math.min(canvas.width, p.x))
    p.y = Math.max(0, Math.min(canvas.height, p.y))
  })
}

const drawParticles = (ctx: CanvasRenderingContext2D, particles: Particle[]) => {
  ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height)

  particles.forEach(p => {
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
    ctx.fillStyle = p.color
    ctx.globalAlpha = p.opacity
    ctx.fill()
    ctx.globalAlpha = 1
  })

  // 计算平均不透明度用于连接线
  const avgOpacity = particles.reduce((sum, p) => sum + p.opacity, 0) / particles.length

  // 绘制连接线
  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const distance = Math.sqrt(dx * dx + dy * dy)

      if (distance < 150) {
        ctx.beginPath()
        ctx.strokeStyle = particles[i].color
        ctx.globalAlpha = avgOpacity * 0.5 * (1 - distance / 150)
        ctx.lineWidth = 1
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.stroke()
        ctx.globalAlpha = 1
      }
    }
  }
}

const startAnimation = (canvas: HTMLCanvasElement, particles: Particle[]) => {
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const animate = () => {
    updateParticles(particles, canvas)
    drawParticles(ctx, particles)
    const id = requestAnimationFrame(animate)
    animationIds.push(id)
  }
  animate()
}

const initCanvas = (
  canvasRef: HTMLCanvasElement | null,
  particleCount: number,
  colors: string[],
  opacityRange: [number, number] = [0.3, 0.6],
  sizeRange: [number, number] = [2, 4]
) => {
  if (!canvasRef) return

  const container = canvasRef.parentElement
  if (!container) return

  canvasRef.width = container.offsetWidth
  canvasRef.height = container.offsetHeight

  const particles = createParticles(canvasRef, particleCount, colors, opacityRange, sizeRange)
  startAnimation(canvasRef, particles)
}

onMounted(() => {
  if (isAboutPage.value) {
    setTimeout(() => {
      // Hero 区域粒子：淡一点
      initCanvas(heroCanvasRef.value, 60, ['#d4af87', '#f0e4d7'], [0.25, 0.35], [1, 2.5])
      // 内容区域粒子：明显一点
      initCanvas(contentCanvasRef.value, 150, ['#d4af87', '#c9a06c', '#b8956a'], [0.3, 0.5], [1.5, 3])
    }, 300)
  } else {
    fetchPageContent(pageType.value)
  }
})

onUnmounted(() => {
  animationIds.forEach(id => cancelAnimationFrame(id))
})

interface Section {
  title: string
  content: string
}

const parseSections = (content: string): Section[] => {
  try {
    const parsed = JSON.parse(content)
    if (Array.isArray(parsed)) {
      return parsed.map(item => ({
        title: item.title || '',
        content: item.content || ''
      }))
    }
  } catch {
    // 解析失败返回空数组
  }
  return []
}

const pageSections = computed<Section[]>(() => {
  if (pageData.value?.content) {
    return parseSections(pageData.value.content)
  }
  return []
})

const pageConfig = computed(() => {
  if (pageData.value) {
    return {
      title: pageData.value.title,
      content: ''
    }
  }

  const configs: Record<string, { title: string; subtitle?: string; content: string }> = {
    'about': {
      title: '关于我们',
      subtitle: '传承千年文脉，品味诗词之美',
      content: ''
    },
    'terms': {
      title: '使用条款',
      content: `欢迎使用古诗词汇平台。在使用我们的服务前，请仔细阅读以下使用条款。

一、服务说明
古诗词汇是一个提供古典诗词内容浏览、学习和交流服务的在线平台。用户通过注册账号可以享受更多个性化服务。

二、用户注册
1. 用户应提供真实、准确的注册信息
2. 用户应妥善保管账号密码，因个人原因导致的账号安全问题由用户自行负责
3. 每个用户只能注册一个账号

三、用户行为规范
1. 用户应遵守国家法律法规，不得利用平台从事违法违规活动
2. 用户应尊重其他用户，不得发布侮辱、诽谤、色情等不良信息
3. 用户应尊重知识产权，不得侵犯他人著作权

四、知识产权
1. 平台上的诗词内容版权归原作者或授权方所有
2. 用户发布的内容应为原创或已获得授权
3. 未经授权，不得将平台内容用于商业用途

五、免责声明
1. 平台不对用户发布的内容承担法律责任
2. 因不可抗力导致的服务中断，平台不承担责任
3. 平台有权对违规内容进行删除或处理

六、条款修改
平台有权根据需要修改使用条款，修改后的条款将在平台上公布。继续使用平台即视为同意修改后的条款。`
    },
    'privacy': {
      title: '隐私政策',
      content: `古诗词汇非常重视用户的隐私保护。本隐私政策说明了我们如何收集、使用和保护您的个人信息。

一、信息收集
我们可能收集以下信息：
1. 注册信息：用户名、邮箱、手机号等
2. 使用数据：浏览记录、搜索历史、收藏内容等
3. 设备信息：设备型号、操作系统、浏览器类型等

二、信息使用
我们使用收集的信息用于：
1. 提供和维护服务
2. 个性化用户体验
3. 改进平台功能
4. 发送服务通知
5. 防范安全风险

三、信息保护
我们采取以下措施保护您的信息：
1. 数据加密存储和传输
2. 访问权限控制
3. 定期安全审计
4. 员工保密培训

四、信息共享
未经您的同意，我们不会向第三方共享您的个人信息，但以下情况除外：
1. 法律法规要求
2. 保护平台或用户的合法权益
3. 经您明确同意

五、Cookie使用
我们使用Cookie来提升用户体验，您可以通过浏览器设置管理Cookie。

六、隐私政策更新
我们可能会不时更新隐私政策，更新后的政策将在平台上公布。

七、联系我们
如对隐私政策有任何疑问，请通过平台联系我们。`
    },
    'contact': {
      title: '联系我们',
      content: `感谢您对古诗词汇的关注和支持！我们非常重视您的意见和建议。

一、联系方式
• 客服邮箱：support@gushihui.com
• 商务合作：business@gushihui.com
• 意见反馈：feedback@gushihui.com

二、工作时间
周一至周五：9:00 - 18:00
法定节假日除外

三、常见问题
1. 账号问题：如忘记密码、账号异常等
2. 内容问题：如诗词纠错、诗人信息补充等
3. 功能建议：如新功能需求、界面优化等
4. 合作洽谈：如内容授权、广告合作等

四、反馈渠道
1. 邮件反馈：发送邮件至上述邮箱
2. 在线反馈：通过平台"意见反馈"功能提交
3. 社交媒体：关注我们的官方微博、微信公众号

五、地址
[公司地址信息]

我们会在收到您的消息后尽快回复，通常在1-3个工作日内处理完毕。

再次感谢您对古诗词汇的支持，让我们一起传承经典，品味诗词之美！`
    }
  }

  return configs[pageType.value] || configs['about']
})
</script>

<template>
  <div class="static-page">
    <!-- 关于我们 - 专门的丰富页面 -->
    <template v-if="isAboutPage">
      <div class="about-hero">
        <!-- 底层：CSS 渐变背景 -->
        <div class="hero-bg-gradient"></div>
        
        <!-- 中层：粒子动画 -->
        <canvas ref="heroCanvasRef" class="hero-particles"></canvas>
        
        <!-- 顶层：页面内容 -->
        <div class="hero-content">
          <h1 class="hero-title">关于我们</h1>
          <p class="hero-subtitle">传承千年文脉，品味诗词之美</p>
          <div class="hero-divider"></div>
        </div>
      </div>

      <div class="content-wrapper">
        <!-- 内容区域粒子动画背景 -->
        <canvas ref="contentCanvasRef" class="content-particles"></canvas>
        
        <div class="about-container">
        <!-- 平台简介 -->
        <section class="about-section">
          <div class="section-header">
            <div class="section-icon">📜</div>
            <h2>平台简介</h2>
          </div>
          <div class="section-content">
            <p>古诗词汇是一个致力于传承和弘扬中国古典诗词文化的数字化平台。我们深信，诗词是中华文明最璀璨的明珠之一，承载着千年的智慧与情感，是中华民族精神家园的重要组成部分。</p>
            <p>在这个信息爆炸的时代，我们希望通过现代科技手段，让更多人能够便捷地接触、学习和欣赏古典诗词，让传统文化在数字时代焕发新的生机与活力。</p>
          </div>
        </section>

        <!-- 我们的使命 -->
        <section class="about-section">
          <div class="section-header">
            <div class="section-icon">🎯</div>
            <h2>我们的使命</h2>
          </div>
          <div class="section-content">
            <div class="mission-grid">
              <div class="mission-card">
                <div class="mission-icon">📚</div>
                <h3>传承经典</h3>
                <p>系统整理和展示中国古典诗词的精华，让千年文脉得以延续</p>
              </div>
              <div class="mission-card">
                <div class="mission-icon">🌐</div>
                <h3>普及文化</h3>
                <p>降低诗词学习的门槛，让更多人能够轻松走进诗词的世界</p>
              </div>
              <div class="mission-card">
                <div class="mission-icon">💡</div>
                <h3>创新融合</h3>
                <p>探索传统文化与现代科技的结合，创造全新的学习体验</p>
              </div>
              <div class="mission-card">
                <div class="mission-icon">🤝</div>
                <h3>社区共建</h3>
                <p>打造诗词爱好者的交流平台，共同推动诗词文化的繁荣</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 平台特色 -->
        <section class="about-section">
          <div class="section-header">
            <div class="section-icon">✨</div>
            <h2>平台特色</h2>
          </div>
          <div class="section-content">
            <div class="features-list">
              <div class="feature-item">
                <div class="feature-number">01</div>
                <div class="feature-info">
                  <h3>海量诗词库</h3>
                  <p>收录从先秦到清代的经典诗词作品，涵盖各个朝代、流派和题材，为您提供最全面的诗词资源。每首诗词都配有详细的注释、译文和赏析，帮助您深入理解诗词的内涵。</p>
                </div>
              </div>
              <div class="feature-item">
                <div class="feature-number">02</div>
                <div class="feature-info">
                  <h3>诗人风采</h3>
                  <p>详细介绍历代著名诗人的生平、创作风格和代表作品。通过诗人的时间线和作品年表，让您全面了解诗人的创作历程和艺术成就。</p>
                </div>
              </div>
              <div class="feature-item">
                <div class="feature-number">03</div>
                <div class="feature-info">
                  <h3>智能搜索</h3>
                  <p>支持按朝代、流派、诗人、题材、意境等多维度检索，让您快速找到心仪的诗词作品。智能推荐系统会根据您的阅读习惯，为您推荐可能感兴趣的诗词。</p>
                </div>
              </div>
              <div class="feature-item">
                <div class="feature-number">04</div>
                <div class="feature-info">
                  <h3>社区交流</h3>
                  <p>为诗词爱好者提供互动交流的空间，您可以分享学习心得、讨论诗词赏析、参与诗词创作。定期举办诗词主题活动，让志同道合的朋友相聚一堂。</p>
                </div>
              </div>
              <div class="feature-item">
                <div class="feature-number">05</div>
                <div class="feature-info">
                  <h3>AI辅助创作</h3>
                  <p>探索人工智能与古典诗词的创新结合，提供智能诗词创作辅助功能。通过AI技术，帮助您理解诗词格律、学习创作技巧，激发创作灵感。</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 发展历程 -->
        <section class="about-section">
          <div class="section-header">
            <div class="section-icon">📅</div>
            <h2>发展历程</h2>
          </div>
          <div class="section-content">
            <div class="timeline">
              <div class="timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-content">
                  <div class="timeline-date">2024年</div>
                  <h3>项目启动</h3>
                  <p>古诗词汇项目正式启动，开始进行诗词数据的收集和整理工作</p>
                </div>
              </div>
              <div class="timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-content">
                  <div class="timeline-date">2025年</div>
                  <h3>平台上线</h3>
                  <p>古诗词汇网站正式上线，提供基础的诗词浏览和搜索功能</p>
                </div>
              </div>
              <div class="timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-content">
                  <div class="timeline-date">2026年</div>
                  <h3>功能完善</h3>
                  <p>新增社区交流、AI辅助创作等功能，用户体验全面提升</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 我们的团队 -->
        <section class="about-section">
          <div class="section-header">
            <div class="section-icon">👥</div>
            <h2>我们的团队</h2>
          </div>
          <div class="section-content">
            <p>古诗词汇团队由一群热爱中国传统文化的专业人士组成，包括诗词研究学者、软件工程师、设计师和内容编辑。我们致力于将专业的诗词知识与先进的技术相结合，为用户打造最优质的学习体验。</p>
            <div class="team-values">
              <div class="value-item">
                <span class="value-label">专业</span>
                <span class="value-desc">严谨的学术态度</span>
              </div>
              <div class="value-item">
                <span class="value-label">创新</span>
                <span class="value-desc">持续的技术探索</span>
              </div>
              <div class="value-item">
                <span class="value-label">传承</span>
                <span class="value-desc">文化的薪火相传</span>
              </div>
              <div class="value-item">
                <span class="value-label">共享</span>
                <span class="value-desc">知识的开放分享</span>
              </div>
            </div>
          </div>
        </section>

        <!-- 联系我们 -->
        <section class="about-section about-contact">
          <div class="section-header">
            <div class="section-icon">📧</div>
            <h2>联系我们</h2>
          </div>
          <div class="section-content">
            <p>如果您有任何问题、建议或合作意向，欢迎随时与我们联系：</p>
            <div class="contact-info">
              <div class="contact-item">
                <span class="contact-label">客服邮箱</span>
                <span class="contact-value">support@gushihui.com</span>
              </div>
              <div class="contact-item">
                <span class="contact-label">商务合作</span>
                <span class="contact-value">business@gushihui.com</span>
              </div>
              <div class="contact-item">
                <span class="contact-label">意见反馈</span>
                <span class="contact-value">feedback@gushihui.com</span>
              </div>
            </div>
          </div>
        </section>
        </div>
      </div>
    </template>

    <!-- 其他页面使用通用模板 -->
    <template v-else>
      <div class="page-header">
        <h1>{{ pageConfig.title }}</h1>
        <p v-if="'subtitle' in pageConfig && pageConfig.subtitle" class="page-subtitle">{{ pageConfig.subtitle }}</p>
      </div>
      <div class="page-content">
        <div class="content-container" v-loading="pageLoading">
          <!-- 从API获取的章节内容 -->
          <template v-if="pageSections.length > 0">
            <div v-for="(section, index) in pageSections" :key="index" class="content-section">
              <h2 class="section-title">{{ section.title }}</h2>
              <div class="section-content" v-html="section.content.replace(/\n/g, '<br>')"></div>
            </div>
          </template>
          <!-- 默认内容（API未获取到时显示） -->
          <template v-else>
            <div class="content-text" v-html="pageConfig.content.replace(/\n/g, '<br>')"></div>
          </template>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.static-page {
  min-height: 100vh;
  position: relative;
  background: linear-gradient(135deg, #f5ede3 0%, #ede3d7 50%, #f5ede3 100%);
}

/* ===== 关于我们页面专用样式 ===== */
.about-hero {
  padding: 100px 20px 60px;
  text-align: center;
  position: relative;
  overflow: hidden;
  min-height: 300px;
}

/* 底层：CSS 渐变背景 */
.hero-bg-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #5a3e2b 0%, #6b4c36 50%, #5a3e2b 100%);
  z-index: 0;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: url('/img/dt_3.jpg') no-repeat center / cover;
    opacity: 0.15;
  }
}

/* 中层：粒子动画 */
.hero-particles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

/* 顶层：页面内容 */
.hero-content {
  position: relative;
  z-index: 2;
}

.hero-title {
  font-size: 42px;
  color: #f8f0e6;
  font-weight: 700;
  margin: 0 0 15px;
  letter-spacing: 4px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
}

.hero-subtitle {
  font-size: 18px;
  color: #d9c7b2;
  margin: 0 0 25px;
  letter-spacing: 2px;
}

.hero-divider {
  width: 80px;
  height: 3px;
  background: linear-gradient(90deg, transparent, #d4af87, transparent);
  margin: 0 auto;
}

/* 内容区域包装器 */
.content-wrapper {
  position: relative;
  min-height: 600px;
}

/* 内容区域粒子动画 */
.content-particles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

.about-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 40px 20px 60px;
  position: relative;
  z-index: 1;
}

.about-section {
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  padding: 35px;
  margin-bottom: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  }
}

.section-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0e4d7;
}

.section-icon {
  font-size: 28px;
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8f0e6, #f0e4d7);
  border-radius: 12px;
}

.section-header h2 {
  font-size: 24px;
  color: #2c3e50;
  margin: 0;
  font-weight: 600;
}

.section-content {
  p {
    font-size: 16px;
    line-height: 1.8;
    color: #4a5568;
    margin: 0 0 15px;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

/* 使命卡片 */
.mission-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-top: 10px;
}

.mission-card {
  background: linear-gradient(135deg, #f8f0e6, #fff);
  border-radius: 12px;
  padding: 25px 20px;
  text-align: center;
  border: 1px solid #f0e4d7;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 25px rgba(90, 62, 43, 0.15);
    border-color: #d4af87;
  }

  .mission-icon {
    font-size: 36px;
    margin-bottom: 15px;
  }

  h3 {
    font-size: 18px;
    color: #5a3e2b;
    margin: 0 0 10px;
    font-weight: 600;
  }

  p {
    font-size: 14px;
    color: #6b5b4b;
    margin: 0;
    line-height: 1.6;
  }
}

/* 特色列表 */
.features-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feature-item {
  display: flex;
  gap: 20px;
  padding: 20px;
  background: #fafafa;
  border-radius: 12px;
  border-left: 4px solid #d4af87;
  transition: all 0.3s ease;

  &:hover {
    background: #f8f0e6;
    transform: translateX(5px);
  }
}

.feature-number {
  font-size: 28px;
  font-weight: 700;
  color: #d4af87;
  min-width: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.feature-info {
  flex: 1;

  h3 {
    font-size: 18px;
    color: #2c3e50;
    margin: 0 0 8px;
    font-weight: 600;
  }

  p {
    font-size: 14px;
    color: #666;
    margin: 0;
    line-height: 1.6;
  }
}

/* 时间线 */
.timeline {
  position: relative;
  padding-left: 30px;

  &::before {
    content: '';
    position: absolute;
    left: 8px;
    top: 0;
    bottom: 0;
    width: 2px;
    background: linear-gradient(to bottom, #d4af87, #f0e4d7);
  }
}

.timeline-item {
  position: relative;
  padding-bottom: 30px;

  &:last-child {
    padding-bottom: 0;
  }
}

.timeline-dot {
  position: absolute;
  left: -26px;
  top: 5px;
  width: 14px;
  height: 14px;
  background: #d4af87;
  border-radius: 50%;
  border: 3px solid white;
  box-shadow: 0 0 0 2px #d4af87;
}

.timeline-content {
  background: #fafafa;
  padding: 20px;
  border-radius: 10px;
  border: 1px solid #eee;

  .timeline-date {
    font-size: 14px;
    color: #d4af87;
    font-weight: 600;
    margin-bottom: 8px;
  }

  h3 {
    font-size: 18px;
    color: #2c3e50;
    margin: 0 0 8px;
    font-weight: 600;
  }

  p {
    font-size: 14px;
    color: #666;
    margin: 0;
    line-height: 1.6;
  }
}

/* 团队价值 */
.team-values {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 15px;
  margin-top: 20px;
}

.value-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #5a3e2b, #6b4c36);
  border-radius: 12px;
  transition: transform 0.3s ease;

  &:hover {
    transform: scale(1.05);
  }

  .value-label {
    font-size: 20px;
    font-weight: 700;
    color: #f8f0e6;
    margin-bottom: 5px;
    letter-spacing: 2px;
  }

  .value-desc {
    font-size: 12px;
    color: #d9c7b2;
  }
}

/* 联系信息 */
.about-contact {
  background: rgba(248, 240, 230, 0.98);
}

.contact-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
  margin-top: 15px;
}

.contact-item {
  display: flex;
  flex-direction: column;
  padding: 15px;
  background: white;
  border-radius: 10px;
  border: 1px solid #f0e4d7;

  .contact-label {
    font-size: 13px;
    color: #999;
    margin-bottom: 5px;
  }

  .contact-value {
    font-size: 15px;
    color: #5a3e2b;
    font-weight: 500;
  }
}

/* ===== 通用页面样式 ===== */
.page-header {
  text-align: center;
  padding: 100px 20px 40px;
  background: linear-gradient(135deg, #5a3e2b 0%, #6b4c36 50%, #5a3e2b 100%);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: url('/img/dt_3.jpg') no-repeat center / cover;
    opacity: 0.15;
  }

  h1 {
    font-size: 36px;
    color: #f8f0e6;
    font-weight: 700;
    margin: 0 0 10px;
    position: relative;
    z-index: 1;
    letter-spacing: 4px;
  }

  .page-subtitle {
    font-size: 16px;
    color: #d9c7b2;
    margin: 0;
    position: relative;
    z-index: 1;
    letter-spacing: 2px;
  }
}

.page-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 20px 60px;
}

.content-container {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.content-text {
  font-size: 16px;
  line-height: 1.8;
  color: #4a5568;
  white-space: pre-line;
}

.content-section {
  margin-bottom: 30px;
  padding-bottom: 30px;
  border-bottom: 1px solid #f0e4d7;

  &:last-child {
    margin-bottom: 0;
    padding-bottom: 0;
    border-bottom: none;
  }
}

.section-title {
  font-size: 22px;
  color: #5a3e2b;
  font-weight: 600;
  margin: 0 0 16px;
  padding-bottom: 10px;
  border-bottom: 2px solid #d4af87;
  display: inline-block;
}

.section-content {
  font-size: 16px;
  line-height: 1.8;
  color: #4a5568;
}

/* ===== 响应式设计 ===== */
@media (max-width: 768px) {
  .hero-title {
    font-size: 28px;
  }

  .hero-subtitle {
    font-size: 14px;
  }

  .about-section {
    padding: 25px 20px;
  }

  .section-header h2 {
    font-size: 20px;
  }

  .mission-grid {
    grid-template-columns: 1fr 1fr;
  }

  .feature-item {
    flex-direction: column;
    gap: 10px;
  }

  .feature-number {
    justify-content: flex-start;
  }

  .team-values {
    grid-template-columns: 1fr 1fr;
  }

  .contact-info {
    grid-template-columns: 1fr;
  }

  .page-header h1 {
    font-size: 28px;
  }

  .content-container {
    padding: 25px;
  }
}

@media (max-width: 480px) {
  .mission-grid {
    grid-template-columns: 1fr;
  }

  .team-values {
    grid-template-columns: 1fr 1fr;
  }
}
</style>