# 古今诗话——墨渊 前端项目

## 项目简介

古今诗话是一个诗词文化平台，采用 Vue 3 + TypeScript + Vite 构建。

## 技术栈

- **框架**: Vue 3.4+
- **语言**: TypeScript 5.x
- **构建工具**: Vite 5.x
- **状态管理**: Pinia 2.x
- **路由**: Vue Router 4.x
- **UI组件库**: Element Plus 2.x
- **HTTP客户端**: Axios 1.x
- **CSS预处理器**: SCSS

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API接口定义
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── composables/      # 组合式函数
│   ├── layouts/          # 布局组件
│   ├── router/           # 路由配置
│   ├── stores/           # 状态管理
│   ├── types/            # 类型定义
│   ├── utils/            # 工具函数
│   ├── views/            # 页面组件
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── public/               # 公共资源
├── index.html            # HTML模板
├── package.json          # 项目配置
├── tsconfig.json         # TypeScript配置
├── vite.config.ts        # Vite配置
└── README.md             # 项目说明
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

### 构建生产版本

```bash
npm run build
```

### 代码检查

```bash
npm run lint
```

### 类型检查

```bash
npm run type-check
```

## 主要功能

- 首页展示
- 诗词列表与详情
- 论坛交流
- 用户登录注册
- 个人中心

## 开发规范

- 使用 Composition API + `<script setup>` 语法
- 使用 TypeScript 进行类型约束
- 遵循 ESLint 代码规范
- 组件使用 scoped 样式
- 样式使用 SCSS 预处理器

## 相关文档

- [迁移开发文档](../docs/migration/migration-guide.md)
- [前端技术规范](../docs/standards/frontend-standards.md)
- [系统架构文档](../docs/architecture/system-architecture.md)