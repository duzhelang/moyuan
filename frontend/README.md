# 古今诗话——墨渊 前端项目

> 墨渊平台前端 | Vue 3 + TypeScript + Vite

## 项目简介

古今诗话——墨渊是一个中国古典诗词文化交流平台，前端采用 Vue 3 + TypeScript + Vite 构建，使用 Element Plus 组件库、Pinia 状态管理，并以 Three.js 实现「诗云星空」3D 可视化。

## 技术栈

- **框架**：Vue 3.4+
- **语言**：TypeScript 5.4
- **构建工具**：Vite 5.x
- **状态管理**：Pinia 2.x
- **路由**：Vue Router 4.x
- **UI 组件库**：Element Plus 2.x（+ @element-plus/icons-vue）
- **HTTP 客户端**：Axios 1.x
- **CSS 预处理器**：Sass / SCSS
- **轮播组件**：Swiper 12.x
- **3D 可视化**：Three.js 0.185（诗云星空）
- **图表**：ECharts 6.x + vue-echarts 8.x（后台数据看板）
- **工具库**：dayjs、lodash-es
- **测试**：Vitest 1.x + @vue/test-utils

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API 接口定义（modules/ 按业务模块拆分，含单元测试）
│   ├── assets/styles/    # 设计系统（variables / mixins / global / ink-background / original.css）
│   ├── components/       # 公共组件
│   │   ├── business/     # 业务组件（PoemCard / PoetCard / ForumPost 等）
│   │   ├── common/       # 通用组件（AppHeader / AppFooter / ImageUpload 等）
│   │   └── home/         # 首页子组件（HomeCarousel / ForumPreview 等）
│   ├── composables/      # 组合式函数（useAuth / usePoem / useParticles 等）
│   ├── data/             # 静态数据（JSON）
│   ├── directives/       # 自定义指令（permission）
│   ├── layouts/          # 布局组件
│   ├── router/           # 路由配置（index / guards）
│   ├── stores/           # 状态管理（Pinia）
│   ├── test/             # 测试初始化
│   ├── types/            # 类型定义
│   ├── utils/            # 工具函数（request / format / validate / storage 等）
│   ├── views/            # 页面组件
│   │   ├── home/ poem/ poet/ forum/ vision/   # 首页/诗词/诗人/论坛/诗话视野
│   │   ├── poetry-cloud/ # 诗云星空（Three.js）
│   │   ├── communicate/  # 交流广场
│   │   ├── repair/       # 报修中心
│   │   ├── static/ contact/  # 静态页面/联系我们
│   │   ├── user/         # 登录/注册/个人中心/用户主页
│   │   └── admin/        # 后台管理页面
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── public/               # 公共资源（img/、fonts/、js/）
├── .env.development      # 开发环境变量
├── index.html            # HTML 模板
├── package.json          # 项目配置
├── tsconfig.json         # TypeScript 配置
├── vite.config.ts        # Vite 配置（含 /api、/uploads 代理）
├── vitest.config.ts      # 测试配置
└── README.md             # 本文件
```

## 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5173

> 开发服务器会将 `/api`、`/uploads` 代理到后端 `http://localhost:8085`，可通过环境变量 `VITE_BACKEND_URL` 覆盖目标地址。

### 构建生产版本

```bash
npm run build
```

构建产物输出到 `dist/`。

### 代码检查

```bash
# 类型检查（vue-tsc）
npm run type-check

# 代码规范检查（ESLint，含自动修复）
npm run lint
```

### 运行测试

```bash
npm run test          # watch 模式
npm run test:run      # 单次运行全部测试
npm run test:coverage # 生成覆盖率报告
npm run test:ui       # 测试 UI 界面
```

## 主要功能

- 首页展示（轮播、精选诗人、分类精赏、论坛预览）
- 诗词列表 / 详情 / 发布，诗人列表 / 详情
- 诗云星空（Three.js 3D 星云可视化）
- 交流广场、诗汇论坛（发帖 / 评论）、诗话视野
- 报修中心、联系我们、静态页面
- 用户登录 / 注册 / 个人中心 / 用户主页
- 后台管理（控制台、内容审核、AI 模型管理等）

## 开发规范

- 使用 Composition API + `<script setup>` 语法
- 使用 TypeScript 进行类型约束
- 遵循 ESLint 代码规范，组件使用 scoped 样式，样式使用 SCSS 预处理器
- 所有 HTTP 请求通过统一封装的 `utils/request.ts`（自动注入 Token、统一错误处理）
- 共享状态放入 Pinia store，可复用逻辑抽取到 `composables/`
- 主题色统一使用墨渊棕金色系（覆写 Element Plus 主色变量），避免默认蓝与纯白裸卡片

## 相关文档

- [迁移开发文档](../docs/migration/migration-guide.md)
- [前端技术规范](../docs/standards/frontend-standards.md)
- [系统架构文档](../docs/architecture/system-architecture.md)
