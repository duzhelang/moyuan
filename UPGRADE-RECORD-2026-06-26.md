# 墨渊前端页面升级记录

> 执行时间：2026-06-26  
> 升级范围：`frontend/` 目录（Vue 3 前端）  
> 核心原则：只改样式层和组件结构，不修改任何业务逻辑、路由、API

---

## 一、升级前问题诊断

| 问题 | 严重性 | 说明 |
|------|--------|------|
| 首页巨型单文件 3933 行 | 高 | `<script>` + `<template>` + 2500 行内联 CSS 全挤在一起 |
| 旧版 CSS 全局覆盖 | 高 | `original.css` 中 `body { font-size: 25px; font-family: cursive; background: url(...) }` 覆盖新设计系统 |
| 装饰元素绝对定位崩溃 | 高 | `.zhuang_s6` ~ `.zhuang_s11` 使用 `top: 2600px` 等魔法数字，内容变化即错位 |
| 响应式设计缺失 | 高 | 首页/列表页核心容器（轮播、论坛、AI 区域、古诗推选）无媒体查询，小屏溢出 |
| 设计令牌未统一应用 | 中 | `variables.scss` 已定义但列表页仍用硬编码颜色/字号 |
| 硬编码数据写死在组件里 | 中 | 10 首古诗 + 11 首当代诗词 + 6 首诗海拾贝 + 示例数据全写在 JS 中 |
| 手写轮播无优化 | 中 | `setInterval` + 手动 DOM 操作，无暂停/帧率控制 |
| 粒子效果全页面加载 | 低 | 每个页面都创建 Canvas + requestAnimationFrame，低端设备卡顿 |

---

## 二、升级执行清单

### Phase 1 — 设计系统基础

#### 1A. 扩展设计令牌（`variables.scss`）

**文件**：`frontend/src/assets/styles/variables.scss`（98 → 188 行）

新增内容（仅追加，未修改原有变量）：

| 类别 | 新增变量 | 说明 |
|------|----------|------|
| 色阶 | `$primary-50` ~ `$primary-900`、`$secondary-50` ~ `$secondary-900` | 主色/辅色 10 级色阶 |
| 表面色 | `$surface-primary`、`$surface-secondary`、`$surface-elevated`、`$surface-overlay` | 语义化表面层级 |
| 排版 | `$font-family-poetry`、`$font-family-seal`、`$font-size-poem`、`$line-height-poem`、`$letter-spacing-poem` | 中文诗词排版专用 |
| 间距 | `$spacing-3xs: 2px`、`$spacing-2xl: 64px` | 补充间距阶梯 |
| 组件 | `$card-hover-shadow`、`$transition-bounce`、`$badge-radius`、`$avatar-border-color`、`$tag-bg-light`、`$tag-text-color`、`$divider-color`、`$input-focus-ring` | 组件级令牌 |
| 深色预置 | `$dark-bg-base` ~ `$dark-comment-input-placeholder`（20 个） | 未来暗色模式预留 |

#### 1B. 扩展 Mixin（`mixins.scss`）

**文件**：`frontend/src/assets/styles/mixins.scss`（229 → 359 行）

新增 mixin：

| Mixin | 说明 |
|-------|------|
| `@mixin mobile` | `< 576px` 快捷断点 |
| `@mixin tablet` | `576px ~ 991px` 快捷断点 |
| `@mixin desktop` | `>= 992px` 快捷断点 |
| `@mixin decorative-border` | 中国风边框（渐变边框 + 四角装饰） |
| `@mixin ink-wash-bg` | 水墨渐变背景（径向光晕叠加） |
| `@mixin card-poetry` | 诗词卡片专用（顶部渐变线 + 宋体排版） |
| `@mixin card-hover-lift` | 悬停上浮效果（translateY -4px + 阴影） |

#### 1C. 提取首页内联 CSS

**操作**：从 `index.vue` 的 `<style>` 块提取 2514 行到独立文件

| 文件 | 变化 |
|------|------|
| 新建 `views/home/home-styles.scss` | 2514 行，首行 `.left-pic, .right-pic...` |
| 修改 `views/home/index.vue` | `<style>` 改为 `@import './home-styles.scss'` |
| `index.vue` 行数 | 3933 → 1420 行 |

#### 1D. 清理旧版 CSS 冲突

**文件**：`frontend/src/assets/styles/original.css`

删除了 3 处 `body` 全局覆盖：

```
[已删除] body { background: url('/img/dt_0.0.jpg') ... }
[已删除] body { font-size: 25px; font-family: cursive; }
[已删除] body { background: rgba(227,196,165,0.75); font-size: 25px; font-family: cursive; }
```

保留了基础重置（`* { box-sizing }`、`ul/li { list-style }`、`a` 链接样式、`h1/h2` 布局）。

---

### Phase 2 — 响应式布局

#### 2A. 首页响应式

**新建文件**：`views/home/home-responsive.scss`（428 行）

覆盖区域 × 断点矩阵：

| 区域 | `< 1200px` | `< 992px` | `< 768px` | `< 576px` |
|------|-----------|-----------|-----------|-----------|
| 主轮播 `.lbye` | — | width 95%, h420 | width 100%, h300, 去圆角 | — |
| 诗文轮播 `.lbye_shiwen` | — | width 95%, h320 | width 100%, h240 | — |
| 诗话视野 `.z_right` | width 400 | 纵向堆叠, 90% | width 95%, h400 | — |
| 论坛 `.luntan` | — | width 80% | width 95% | width 100% |
| 古诗推选/当代精选 | — | width 95% | 纵向堆叠 | 搜索栏堆叠 |
| AI 区域 `.ai-container` | — | — | 全宽, 去圆角 | 更紧凑 |
| 装饰元素 `.zhuang_s*` | 隐藏 s6-s11 | — | 隐藏全部 | — |
| 顶部栏 `.top` | — | — | h45, 隐藏时钟 | h40 |

#### 2B. 列表页响应式 + 设计令牌

**修改文件**：
- `views/poem/list.vue` — 11 处硬编码值替换为设计变量
- `views/poet/list.vue` — 已确认使用变量，无需修改

替换清单：

| 原始值 | 替换为 |
|--------|--------|
| `#8B4513` | `$primary-color` |
| `#D2691E` | `$secondary-color` |
| `#CD853F` | `$accent-color` |
| `border-radius: 12px` | `$border-radius-lg` |
| `font-size: 16px` | `$font-size-lg` |
| `transition: 0.3s ease` | `$transition-base` |

---

### Phase 3 — 性能优化

#### 3A. 粒子效果优化

**修改文件**：`frontend/src/composables/useParticles.ts`（→ 207 行）

| 优化项 | 实现方式 |
|--------|----------|
| `prefers-reduced-motion` 检测 | `window.matchMedia('(prefers-reduced-motion: reduce)')`，匹配则跳过渲染 |
| 页面可见性暂停 | `document.visibilitychange`，不可见时取消 animationFrame，可见时恢复 |
| 30fps 帧率限制 | `FPS_LIMIT = 30`，`FRAME_INTERVAL = 33ms`，未达间隔直接跳过绘制 |
| 清理增强 | `onUnmounted` 中清理 visibility 监听 + canvas 引用置空 |

---

### Phase 4 — 组件拆分 + 数据外置

#### 4A. 安装 Swiper

```
npm install swiper
```

版本：`^12.2.0`

#### 4B. 数据外置（3 个 JSON 文件）

| 文件 | 内容 |
|------|------|
| `src/data/home-ancient-poems.json` | 10 首经典古诗（静夜思、登鹳雀楼、春晓...） |
| `src/data/home-contemporary-poems.json` | 11 首当代诗词（常平逼王作品） |
| `src/data/home-poetry-library.json` | 6 首诗海拾贝 + 4 个分析示例 + 4 个对联示例 |

#### 4C. 创建 4 个首页子组件

| 组件 | 文件 | 行数 | Props | 功能 |
|------|------|------|-------|------|
| `HomeCarousel` | `components/home/HomeCarousel.vue` | 157 | `type: 'main' \| 'shiwen'` | Swiper 轮播，支持主轮播(6图/4s)和诗文轮播(7图/6s) |
| `ForumPreview` | `components/home/ForumPreview.vue` | 401 | `posts: ForumPost[]` | 论坛预览（标题/搜索框/滚动字幕/导航/精选板块/热帖） |
| `AncientPoemSelection` | `components/home/AncientPoemSelection.vue` | 523 | 无（自给自足） | 古诗推选（历史的印痕/为您推荐/搜索/详情弹窗） |
| `ContemporaryPoems` | `components/home/ContemporaryPoems.vue` | 530 | 无（自给自足） | 当代精选（分页/点赞/收藏/评论/登录检测） |

Barrel 导出：`components/home/index.ts`

#### 4D. 集成到首页

**修改文件**：`views/home/index.vue`（1420 → 819 行）

模板替换：

| 原始区域 | 替换为 |
|----------|--------|
| 主轮播（`.lbye` 内的 img/ul/arrows） | `<HomeCarousel type="main" />` |
| 诗文轮播（`.lbye_shiwen` 内的 img/ul） | `<HomeCarousel type="shiwen" />` |
| 论坛预览（`.luntan#luntan` 整段） | `<ForumPreview :posts="hotPosts" />` |
| 古诗推选（`.poem_selection_left`） | `<AncientPoemSelection />` |
| 当代精选（`.poem_selection_right`） | `<ContemporaryPoems />` |

脚本清理：
- 删除轮播相关：`currentSlide`、`maxSlides`、`ltTimer`、`updateImage`、`nextSlide`、`prevSlide`、`goToSlide`
- 删除古诗搜索：`poemSearchKeyword`、`handlePoemSearch`、`clearPoemSearch` 等 10 个变量/函数
- 删除当代精选：`contemporaryPoems`、`contemporaryPage`、`handlePoemLike`、`formatDate` 等 15 个变量/函数
- 删除评论系统：`expandedPoemIdx`、`poemComments`、`handleSubmitPoemComment` 等 8 个变量/函数
- 删除无用 import：`getComments`、`createComment`、`likeComment`、`likePoem`、`favoritePoem`、`searchPoemsWithRecommend`、`PoetryDetailDialog`
- AI 区域数据从 JSON 导入：`analyzeExamples`、`poetryLibrary`、`explorePoem`、`exampleCouplets`

---

## 三、最终文件变更清单

### 新建文件（7 个）

```
frontend/src/views/home/home-styles.scss          # 首页样式（从 index.vue 提取）
frontend/src/views/home/home-responsive.scss      # 首页响应式 + 装饰修复
frontend/src/components/home/HomeCarousel.vue     # 轮播组件（Swiper）
frontend/src/components/home/ForumPreview.vue     # 论坛预览组件
frontend/src/components/home/AncientPoemSelection.vue  # 古诗推选组件
frontend/src/components/home/ContemporaryPoems.vue     # 当代精选拄件
frontend/src/components/home/index.ts             # Barrel 导出
frontend/src/data/home-ancient-poems.json         # 古诗数据
frontend/src/data/home-contemporary-poems.json    # 当代诗词数据
frontend/src/data/home-poetry-library.json        # AI 区域数据
```

### 修改文件（7 个）

| 文件 | 变化 |
|------|------|
| `variables.scss` | 98 → 188 行（+90 行令牌） |
| `mixins.scss` | 229 → 359 行（+130 行 mixin） |
| `original.css` | 删除 3 处 body 全局覆盖 |
| `views/home/index.vue` | 3933 → 819 行（-79%） |
| `views/poem/list.vue` | 11 处硬编码 → 设计变量 |
| `composables/useParticles.ts` | +4 项性能优化 |
| `package.json` | +swiper ^12.2.0 |

### 未修改文件

- 所有后端代码（`sc-moyuan-backend/`）
- 所有路由配置（`router/index.ts`）
- 所有 API 模块（`api/modules/`）
- 所有 Store（`stores/`）
- 所有其他页面（`views/poet/`、`views/forum/`、`views/user/`、`views/admin/` 等）
- 所有业务组件（`components/business/`、`components/common/`）
- 旧版静态 HTML（`index.html`、`html/`）— 未删除，未修改

---

## 四、质量验证

| 验证项 | 结果 |
|--------|------|
| `vue-tsc --noEmit` 类型检查 | 0 错误 |
| `npm run build` 构建 | 成功，34.10s，exit code 0 |
| 旧轮播代码残留扫描 | `currentSlide`/`ltTimer`/`updateImage` 在 index.vue 中 0 匹配 |
| body 全局覆盖扫描 | `original.css` 中仅剩 `body { margin: 0 auto }` × 2 |
| 设计令牌使用扫描 | `poem/list.vue` 中 `#8B4513`/`#D2691E` 0 匹配 |
| 组件集成扫描 | index.vue 中 `<HomeCarousel>`/`<ForumPreview>`/`<AncientPoemSelection>`/`<ContemporaryPoems>` 全部引用 |
| Swiper 依赖 | `package.json` 中 `"swiper": "^12.2.0"` |

---

## 五、审计修复记录

在最终审查阶段发现并修复了 3 个问题：

| # | 问题 | 原因 | 修复 |
|---|------|------|------|
| 1 | `original.css` 第 602 行 body 全局覆盖未清理 | Phase 1C 智能体遗漏了 `sy_caidan.css` 分区的 body 块 | 删除该 body 块 |
| 2 | `original.css` 第 2741 行 body 全局覆盖未清理 | Phase 1C 智能体遗漏了第二个 body 块 | 删除该 body 块 |
| 3 | `home-responsive.scss` 响应式规则丢失 | Phase 3A 智能体覆盖了文件而非追加，269 行核心响应式规则消失 | 重建完整 428 行响应式文件 |
| 4 | `index.vue` AI 区域数据定义被误删 | Phase 4D 集成时误删了 `analyzeExamples`/`explorePoem` 等定义 | 从 JSON 补回数据定义 |

---

## 六、后续可选优化

| 优化项 | 优先级 | 说明 |
|--------|--------|------|
| 删除旧版静态 HTML | 低 | `index.html` 和 `html/` 目录仍存在，可在确认无外部引用后删除 |
| Swiper 替换手动轮播 | 已完成 | HomeCarousel 已使用 Swiper |
| 诗人列表页组件化 | 低 | `poet/list.vue` 仍为单文件，可拆分筛选/卡片/详情为组件 |
| 深色模式实现 | 低 | `$dark-*` 变量已预置，可基于 CSS 变量实现主题切换 |
| 首页 AppHeader 统一 | 低 | 首页有独立的 `.top` 栏，其他页面用 `AppHeader`，可统一 |
| 路由级代码分割 | 低 | 已通过动态 import 实现，可进一步优化 chunk 策略 |
