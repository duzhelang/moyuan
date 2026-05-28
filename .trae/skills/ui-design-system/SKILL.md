---
name: "ui-design-system"
description: "Software-ODA125 项目的完整 UI 设计规范与样式系统。包含全局色彩体系、组件级样式规则、交互状态规范、特殊视觉要求、字体与圆角统一规范。当需要新增页面、调整样式、统一视觉风格时调用此技能。"
---

# Software-ODA125 UI 设计系统

> **适用版本**：v1.0.1 | **最后更新**：2026-05-05  
> **技术上下文**：Vue 3 + Element Plus + `<style scoped>`  
> **核心原则**：渐变蓝绿品牌色系 + 毛玻璃卡片 + 克制阴影层级 + 2-5px 微交互上浮

---

## 1. 全局色彩系统

### 1.1 品牌主色 — 蓝色系

| 角色 | 色值 | 用途 |
|------|------|------|
| **Primary** | `#4080FF` | 主题色、链接、按钮、边框聚焦、图标强调 |
| Primary Hover | `#3366CC` | 按钮悬停、链接悬停 |
| Primary Deep | `#2563eb` / `#1d4ed8` | 激活态文字 |
| Primary Light | `#bfdbfe` | 卡片边框、悬浮面板边框 |
| Primary Lighter | `#dbeafe` | 激活态背景 |
| Primary Palette-50 | `#eff6ff` | 浅色背景、hover 渐变 |
| Primary Palette-100 | `#e0f2fe` | hover 渐变终点 |
| Primary Palette-200 | `#e0e7ff` | 导航栏标题分隔线 |

### 1.2 辅色 — 绿色系（积极/成功/健康）

| 角色 | 色值 | 用途 |
|------|------|------|
| **Success** | `#67C23A` / `#52C41A` | 正常指标、低风险、运动建议 |
| Success Light | `#f0f9eb` | 成功卡片背景、建议项背景 |
| Success Border | `#e1f3d8` / `#d1fae5` | 健康卡片边框 |
| Success Dark | `#10b981` | 健康计划卡片左边框 |

### 1.3 功能色 — 橙色（警告/中等风险）

| 角色 | 色值 | 用途 |
|------|------|------|
| **Warning** | `#E6A23C` / `#FF7D00` | 中风险等级、待诊断状态 |
| Warning Light | `#fdf6ec` / `#fffbeb` | 警告标签背景、食谱暖色背景 |
| Warning Dark | `#92400e` | 警告文字 |
| Warning Border | `#fde68a` | 中风险卡片边框 |

### 1.4 功能色 — 红色（危险/高风险/异常）

| 角色 | 色值 | 用途 |
|------|------|------|
| **Danger** | `#F56C6C` / `#f44336` | 高风险、异常指标、密码弱强度 |
| Danger Light | `#fef0f0` / `#ffebee` | 危险区背景 |
| Danger Border | `#fde2e2` / `#fde2c8` | 危险卡片边框 |

### 1.5 中性色 — 文字层级

| 角色 | 色值 | 用途 |
|------|------|------|
| **Text Primary** | `#303133` / `#1e293b` / `#1F2937` | 标题、重要文字 |
| **Text Regular** | `#606266` / `#475569` / `#374151` | 正文 |
| **Text Secondary** | `#909399` / `#64748b` / `#6B7280` | 辅助说明、描述 |
| **Text Placeholder** | `#C0C4CC` / `#9CA3AF` | 占位符、禁用态文字 |

### 1.6 中性色 — 背景层级

| 角色 | 色值 | 用途 |
|------|------|------|
| **Page Background** | `#f5f7fa` / `#F8FAFC` | 页面底色 |
| **Card Background** | `#FFFFFF` | 卡片底色 |
| **Card Background Hover** | `#f9fafb` | 卡片悬停 |
| **Input Background** | `#fafbfc` | 输入框底色 |

### 1.7 科技装饰色 — 特殊效果

| 用途 | 色值 |
|------|------|
| 渐变背景蓝-绿 | `linear-gradient(135deg, rgba(64,128,255,0.1), rgba(82,196,26,0.1))` |
| 渐变背景蓝-蓝 | `linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)` |
| 毛玻璃卡片 | `rgba(255, 255, 255, 0.7~0.95)` + `backdrop-filter: blur(4-20px)` |
| 发光阴影 | `rgba(64, 128, 255, 0.15~0.5)` |
| 返回顶部按钮渐变 | `linear-gradient(135deg, #36b37e, #4af7ac)` |
| 404页渐变 | `linear-gradient(135deg, #2c45b5, #2cb4ce)` |

---

## 2. 组件级别样式规则

### 2.1 按钮

```css
/* 主按钮 — 蓝色渐变 */
.feature-btn {
  padding: 10px 24px;
  border-radius: 20px;                          /* 胶囊形 */
  background: linear-gradient(135deg, #4a90e2 0%, #357abd 100%);
  color: white;
  font-size: 14px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.15s, opacity 0.2s;
  box-shadow: 0 3px 10px rgba(74, 144, 226, 0.2);
}
.feature-btn:hover:not(:disabled) {
  box-shadow: 0 5px 16px rgba(74, 144, 226, 0.3);
  transform: translateY(-1px);
}
.feature-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

/* 绿色按钮变体 */
.feature-btn-green {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  box-shadow: 0 3px 10px rgba(16, 185, 129, 0.2);
}

/* 蓝色按钮变体 */
.feature-btn-blue {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 3px 10px rgba(59, 130, 246, 0.2);
}

/* 白色主按钮 (Home.vue Hero) */
.btn-primary {
  background: #fff;
  color: #005bea;
  border-radius: 20px;
  padding: 15px 35px;
  font-size: 16px;
  font-weight: bold;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

/* 透明描边按钮 */
.btn-plain {
  background: transparent;
  color: #fff;
  border: 2px solid rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  padding: 15px 35px;
  font-size: 16px;
  font-weight: bold;
}

/* 食谱生成按钮 — 橙色渐变 */
.recipe-gen-btn {
  width: 100%;
  padding: 10px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #ff9a56 0%, #e67e22 100%);
  color: white;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 3px 10px rgba(230, 126, 34, 0.2);
}

/* 返回按钮 */
.recipe-back-btn {
  width: 100%;
  padding: 8px 0;
  border: 1px solid #fde2c8;
  border-radius: 8px;
  background: linear-gradient(135deg, #fff7ed, #fffbeb);
  color: #e67e22;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

/* 折叠按钮 */
.collapse-btn {
  font-size: 12px;
  color: #4a90e2;
  background: #f0f7ff;
  border: 1px solid #d0e3f7;
  border-radius: 14px;
  padding: 4px 12px;
  cursor: pointer;
}
```

### 2.2 输入框

```css
/* Element Plus 穿透样式 */
:deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: none;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.08);
}

/* 悬停和聚焦 */
:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.15) !important;
}

/* 文本域 */
.report-textarea {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  background: #fafbfc;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}
.report-textarea:focus {
  border-color: #4a90e2;
  box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.12);
  background: white;
}

/* 下拉选择框 */
.plan-select {
  padding: 8px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.plan-select:focus {
  border-color: #4a90e2;
  box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.15);
}
```

### 2.3 卡片

```css
/* 基础卡片 */
.feature-card {
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  padding: 24px;
  border: 1px solid #eef2f6;
  position: relative;
  overflow: hidden;                      /* 配合 ::before 顶部渐变条 */
  transition: transform 0.2s, box-shadow 0.2s;
}

/* 顶部渐变色条 */
.feature-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 100%;
  height: 3px;
}

/* 健康计划卡片 — 绿色渐变背景 */
.card-plan {
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 50%, #f0f9ff 100%);
  border: 1px solid #d1fae5;
}
.card-plan::before {
  background: linear-gradient(90deg, #10b981, #34d399);
}

/* 报告解读卡片 — 蓝色渐变背景 */
.card-report {
  background: linear-gradient(135deg, #eff6ff 0%, #e0f2fe 50%, #f5f3ff 100%);
  border: 1px solid #bfdbfe;
}
.card-report::before {
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
}

/* 悬停上浮 */
.feature-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

/* Home.vue 特性卡片 */
.feature-card {                         /* 不同命名空间 */
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.05);
  border: 1px solid #eee;
  transition: all 0.3s;
}
.feature-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);
}

/* 科普页章节卡片 */
.section {
  background: #FFFFFF;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  padding: 24px;
  transition: all 0.3s ease;
}
.section:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

/* 统计卡片 (DoctorWorkbench) */
.stat-card {
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s;
}
.stat-card:hover {
  transform: translateY(-3px);
}
```

### 2.4 毛玻璃面板

```css
/* 登录/注册页毛玻璃卡片 */
.card {
  background: rgba(255, 255, 255, 0.7~0.75);
  backdrop-filter: blur(4~6px);
  -webkit-backdrop-filter: blur(4~6px);
  border-radius: 28~32px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.4~0.5);
}

/* 悬浮导航栏毛玻璃 */
.floating-nav {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 16px;
}

/* 对话页左侧面板毛玻璃 */
.health-panel {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid rgba(0, 0, 0, 0.06);
}

/* 弹窗毛玻璃 (HealthCheck.vue) */
.dialog-card {
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.dialog-overlay {
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  background: rgba(0, 0, 0, 0.3);
}
```

### 2.5 对话框

```css
/* 食谱弹窗 (el-dialog) */
.recipe-dialog .el-dialog__header {
  background: linear-gradient(135deg, #fff7ed 0%, #fffbeb 100%);
  border-radius: 12px 12px 0 0;
  padding: 18px 24px;
  border-bottom: 1px solid #fde2c8;
}

.recipe-dialog .el-dialog__body {
  padding: 20px 24px;
  max-height: 70vh;
  overflow-y: auto;
}

.recipe-dialog .el-dialog {
  border-radius: 12px;
  overflow: hidden;
}
```

### 2.5.1 弹窗/抽屉定位规范（关键）

> **背景**：`Manage.vue` 主布局中 `.manage-wrapper` 设置了 `overflow: hidden`，`.manage-main` 设置了 `backdrop-filter: blur(8px)`。两者都会创建新的层叠/定位上下文，导致子组件内的 `el-dialog` 和 `el-drawer` 的 `position: fixed` 定位基准被限制在容器内部，而非视口。当页面滚动后，弹窗位置不跟随视口，造成弹窗被裁切或展示不全。

**规则：项目中所有 `el-dialog` 和 `el-drawer` 必须添加 `append-to-body` 属性。**

```vue
<!-- ✅ 正确 -->
<el-dialog
  v-model="visible"
  title="标题"
  append-to-body
  top="15vh"
  width="600px"
>

<el-drawer
  v-model="visible"
  append-to-body
  size="680px"
>

<!-- ❌ 错误：缺少 append-to-body，在 Manage.vue 布局下会被裁切 -->
<el-dialog v-model="visible" title="标题" width="600px">
```

**原因分析**：
| Manage.vue 布局属性 | 影响 |
|---|---|
| `.manage-wrapper { overflow: hidden }` | 创建新的定位上下文，fixed 元素相对此容器而非视口 |
| `.manage-main { backdrop-filter: blur(8px) }` | 创建新的层叠上下文，影响 z-index 和 fixed 定位 |

**`append-to-body` 的作用**：将弹窗 DOM 挂载到 `<body>` 上，彻底脱离布局容器限制，始终相对视口定位。

**补充说明**：
- 使用 `append-to-body` 后，弹窗的 scoped CSS 可能失效（因为 DOM 不在原组件内），需要用全局样式或 `::v-deep` 处理
- 若弹窗需要居中，可配合 `align-center` 或 CSS `margin: auto !important`
- 抽屉（drawer）同样适用此规则，尤其是右侧滑出的详情面板

### 2.6 表格

```css
/* 科普信息表格 */
.info-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  overflow: hidden;                      /* 配合圆角裁剪 */
}

.info-table th {
  background: rgba(64, 128, 255, 0.05);
  font-weight: 600;
  color: #4080FF;
}

.info-table th, .info-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #E5E7EB;
}

.info-table tr:hover {
  background: rgba(64, 128, 255, 0.03);
}
```

### 2.7 侧边栏（导航栏）

```css
/* 科普页右侧悬浮导航栏 - 精修版 */
.floating-nav {
  position: fixed;
  right: 32px;
  top: 100px;
  bottom: 32px;
  width: 190px;
  padding: 20px 16px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.18), rgba(248, 250, 252, 0.15));
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(64, 128, 255, 0.12), 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(64, 128, 255, 0.08);
  overflow-y: auto;
  z-index: 1000;
  max-height: calc(100vh - 200px);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 美化导航栏滚动条 */
.floating-nav::-webkit-scrollbar {
  width: 6px;
}
.floating-nav::-webkit-scrollbar-track {
  background: transparent;
  margin: 8px 0;
}
.floating-nav::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(64, 128, 255, 0.3), rgba(64, 128, 255, 0.15));
  border-radius: 3px;
  transition: all 0.2s ease;
}
.floating-nav::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(64, 128, 255, 0.5), rgba(64, 128, 255, 0.3));
}

/* 顶部标题层级设计 */
.floating-nav h3 {
  font-size: 14px;
  font-weight: 700;
  margin: 0 0 18px 0;
  color: #1e40af;
  border-bottom: 2px solid rgba(64, 128, 255, 0.1);
  padding-bottom: 12px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  display: flex;
  align-items: center;
  gap: 8px;
}
.floating-nav h3::before {
  content: '';
  width: 4px;
  height: 16px;
  background: linear-gradient(180deg, #4080FF, #3366CC);
  border-radius: 2px;
}

.floating-nav ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.floating-nav li {
  margin-bottom: 6px;
}

/* 导航项基础状态 */
.floating-nav a {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  color: #1e293b;
  text-decoration: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: transparent;
  border: 1px solid transparent;
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(193, 208, 241, 0.3), rgba(149, 167, 201, 0.4));
}
.floating-nav a::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%) scaleX(0);
  width: 3px;
  height: 20px;
  background: linear-gradient(180deg, #4080FF, #3366CC);
  border-radius: 0 2px 2px 0;
  transition: transform 0.3s ease;
}

/* 鼠标悬浮交互 */
.floating-nav a:hover {
  background: linear-gradient(135deg, rgba(64, 128, 255, 0.08), rgba(64, 128, 255, 0.02));
  color: #1d4ed8;
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(64, 128, 255, 0.08);
}
.floating-nav a:hover::before {
  transform: translateY(-50%) scaleX(1);
}

/* 激活态高亮 */
.floating-nav a.active {
  background: linear-gradient(135deg, rgba(64, 128, 255, 0.1), rgba(64, 128, 255, 0.05));
  color: #1e40af;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(64, 128, 255, 0.1);
}
.floating-nav a.active::before {
  transform: translateY(-50%) scaleX(1);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .floating-nav {
    display: none;
  }
}
```

### 2.8 标签/徽章

```css
/* 科普页标签 (Home.vue) */
.badge {
  display: inline-block;
  background: rgba(255, 255, 255, 0.2);
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
  backdrop-filter: blur(5px);
}

/* 症状标签 (DoctorWorkbench) */
.symptoms-tag {
  border-radius: 12px;
  background: #fdf6ec;
  color: #E6A23C;
}

/* Element Plus 标签 */
.el-tag {
  /* 使用 Element Plus 默认，蓝色系 */
}
```

---

## 3. 交互状态规范

### 3.1 悬停（Hover）

| 效果 | CSS | 适用范围 |
|------|-----|----------|
| 上浮 1px | `transform: translateY(-1px)` | 小按钮 |
| 上浮 2px | `transform: translateY(-2px)` | 卡片、导航标签 |
| 上浮 3px | `transform: translateY(-3px)` | 统计卡片、返回顶部 |
| 上浮 5px | `transform: translateY(-5px)` | 视频封面 |
| 上浮 10px | `transform: translateY(-10px)` | 首页特性卡片 |
| 右移 3px | `transform: translateX(3px)` | 导航项 |
| 缩放 1.05x | `transform: scale(1.05)` | 按钮 |
| 缩放 1.08x | `transform: scale(1.08)` | 图片 |
| 缩放 1.1x | `transform: scale(1.1)` | 头像、播放按钮 |

### 3.2 阴影增强层级

| 状态 | 阴影值 | 用途 |
|------|--------|------|
| 默认 | `0 2px 12px rgba(0,0,0,0.05)` | 标准卡片 |
| 默认（有主题色） | `0 2px 12px rgba(64,128,255,0.05)` | 含主题色的卡片 |
| 悬停 | `0 8px 24px rgba(0,0,0,0.08)` | 卡片悬停 |
| 悬停（强） | `0 15px 30px rgba(0,0,0,0.1)` | 首页特性卡悬停 |
| 按钮默认 | `0 3px 10px rgba(74,144,226,0.2)` | 蓝底按钮 |
| 按钮悬停 | `0 5px 16px rgba(74,144,226,0.3)` | 蓝底按钮悬停 |
| 聚焦 | `0 0 0 3px rgba(64,128,255,0.15)` | 输入框聚焦光晕 |
| 弹窗 | `0 25px 50px -12px rgba(0,0,0,0.25)` | 对话框 |

### 3.3 激活（Active/Current）

| 组件 | 样式 |
|------|------|
| 导航项激活 | `background: linear-gradient(135deg, #dbeafe, #bfdbfe); color: #1d4ed8; font-weight: 600; padding-left: 14px; border-color: #93c5fd;` |
| 表格行悬停 | `background: rgba(64, 128, 255, 0.03)` |
| 选中/高亮流程步骤 | `border: 2px solid #409EFF; background: #ecf5ff;` |

### 3.4 禁用（Disabled）

```css
/* 按钮禁用 */
opacity: 0.5;
cursor: not-allowed;
box-shadow: none;

/* 输入框禁用 */
background: #f5f7fa;
color: #C0C4CC;
cursor: not-allowed;
```

### 3.5 风险等级三色方案

| 等级 | 背景渐变 | 左边框 | 文字色 |
|------|----------|--------|--------|
| 低风险 | `linear-gradient(135deg, #e8f5e9, #c8e6c9)` | `5px solid #4caf50` | — |
| 中风险 | `linear-gradient(135deg, #fff3e0, #ffe0b2)` | `5px solid #ff9800` | — |
| 高风险 | `linear-gradient(135deg, #ffebee, #ffcdd2)` | `5px solid #f44336` | — |
| 弱密码 | — | — | `#f56c6c` |
| 中密码 | — | — | `#e6a23c` |
| 强密码 | — | — | `#67c23a` |

---

## 4. 特殊视觉要求

### 4.1 毛玻璃（Glassmorphism）

**使用场景**：登录/注册卡片、对话页左侧面板、弹窗、悬浮导航、返回顶部按钮

**必须同时设置 webkit 前缀**：
```css
background: rgba(255, 255, 255, 0.7~0.95);
backdrop-filter: blur(4~20px);
-webkit-backdrop-filter: blur(4~20px);   /* 必须 */
```

**模糊强度层级**：
| 强度 | 场景 |
|------|------|
| `blur(4px)` | 登录卡片、弹窗遮罩 |
| `blur(6px)` | 注册卡片 |
| `blur(10px)` | 健康检查卡片 |
| `blur(12px)` | 悬浮导航栏 |
| `blur(20px)` | 对话面板、弹窗卡片 |

### 4.2 渐变色

**所有渐变统一使用 `135deg` 方向**（左上→右下），特殊情况使用 `90deg` 水平。

| 渐变 | 用途 |
|------|------|
| `linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)` | 页面/容器背景 |
| `linear-gradient(135deg, rgba(64,128,255,0.1), rgba(82,196,26,0.1))` | 头部渐变 |
| `linear-gradient(135deg, #4080FF, #a0cfff)` | 蓝色功能图标 |
| `linear-gradient(135deg, #67C23A, #b3e19d)` | 绿色功能图标 |
| `linear-gradient(135deg, #8E44AD, #bb8fce)` | 紫色功能图标 |
| `linear-gradient(135deg, #00c6fb 0%, #005bea 100%)` | Hero Section |
| `linear-gradient(90deg, #4a90e2, #7bb3f0)` | 报告卡片顶部条 |
| `linear-gradient(90deg, #10b981, #34d399)` | 健康计划卡片顶部条 |
| `linear-gradient(90deg, #67c23a, #e6a23c, #f56c6c)` | 百分位进度条 |

**渐变文字效果**：
```css
background: linear-gradient(135deg, #4080FF, #52C41A);
-webkit-background-clip: text;
-webkit-text-fill-color: transparent;
background-clip: text;
```

### 4.3 发光效果

```css
/* 按钮发光 */
box-shadow: 0 3px 10px rgba(74, 144, 226, 0.2);

/* 悬停发光增强 */
box-shadow: 0 5px 16px rgba(74, 144, 226, 0.3);

/* 首页神经网络节点发光 */
box-shadow: 0 0 15px rgba(255, 255, 255, 0.8);

/* 聚焦蓝光晕 */
box-shadow: 0 0 0 3px rgba(64, 128, 255, 0.15);
```

### 4.4 滚动条样式

```css
/* 统一滚动条样式 */
::-webkit-scrollbar {
  width: 4~6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: #ddd;                      /* 或 rgba(64,128,255,0.25) */
  border-radius: 2~4px;
}
::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}
```

### 4.5 动画系统

**过渡时长规范**：

| 时长 | 用途 |
|------|------|
| `0.15s` | 微交互（按钮按下） |
| `0.2s` | 常规交互（导航、按钮、卡片） |
| `0.25s` | 导航项 |
| `0.3s` | 卡片、模态框、渐显 |
| `0.35s` | 侧边栏滑动（cubic-bezier） |
| `0.4s` | 面板折叠展开 |
| `0.5s` | 大图缩放 |

**缓动函数**：

| 函数 | 使用场景 |
|------|----------|
| `ease` | 默认全部使用 |
| `cubic-bezier(0.4, 0, 0.2, 1)` | 侧边栏位置过渡、面板滑动 |

**关键帧动画**：

```
@keyframes pulse          — 2s infinite, scale(1 → 1.2 → 1)
@keyframes float          — 3s ease-in-out infinite, translateY(0 → -10px → 0)
@keyframes blink          — 1s step-end infinite, 打字光标闪烁
@keyframes speakPulse     — 1.5s ease-in-out infinite, 语音按钮呼吸
@keyframes scrollMarquee  — 跑马灯连续滚动
@keyframes glow-pulse     — radial-gradient 光晕呼吸
@keyframes number-breathe — scale(1 → 1.05 → 1) 数字呼吸
@keyframes slideInRight   — translateX(50px → 0), opacity(0 → 1)
```

**Vue Transition 命名**：

| Name | 效果 |
|------|------|
| `fade` | `opacity + translateY(10px)` |
| `panel-slide` | `max-height + opacity` |

---

## 5. 字体与圆角统一规范

### 5.1 字号层级

| 层级 | 字号 | 用途 |
|------|------|------|
| H0 | `72px` | 404 大标题 |
| H1 | `48px` | Hero 主标题、风险概率值 |
| H2 | `40px` | 卡片图标 |
| H3 | `32px` | 区块标题、功能图标 |
| H4 | `28px` | 统计数字、页面标题 |
| H5 | `26px` | 注册页标题 |
| H6 | `24px` | 步骤数字、建议图标 |
| H7 | `22px` | 信息卡片标题 |
| H8 | `20px` | 功能卡片标题、导航标题 |
| H9 | `18px` | 描述文字、特征标题 |
| Body-L | `16px` | 按钮文字、章节描述 |
| Body | `14px` | 标准正文、表格内容 |
| Body-S | `13px` | 导航项、辅助说明 |
| Caption | `12px` | 标签、徽章、版权信息 |

### 5.2 圆角层级

| 层级 | 值 | 用途 |
|------|-----|------|
| 圆形 | `50%` | 头像、播放按钮、返回顶部 |
| 大胶囊 | `28~32px` | 登录/注册卡片 |
| 中胶囊 | `20px` | 按钮、导航标签 |
| 大圆角 | `16px` | 页面卡片、弹窗 |
| 标准圆角 | `14px` | 功能卡片 |
| 中圆角 | `12px` | 输入框、特性卡片、对话框 |
| 小圆角 | `10px` | 文本域、流程步骤 |
| 更小圆角 | `8px` | 表格、回复卡片、导航项 |
| 微圆角 | `6px` | 按钮（紧凑）、下拉框 |
| 极小圆角 | `4px` | 内嵌元素、进度条 |
| 滚动条 | `2~3px` | 滚动条 thumb |

### 5.3 阴影深度层级

| 层级 | 值 | 命名场景 |
|------|-----|----------|
| Level 0 | `none` | 扁平元素 |
| Level 1 | `0 2px 8px rgba(0,0,0,0.08)` | 表格 |
| Level 2 | `0 2px 12px rgba(0,0,0,0.05)` | 标准卡片 |
| Level 3 | `0 4px 12px rgba(0,0,0,0.05)` | 统计卡片 |
| Level 4 | `0 5px 20px rgba(0,0,0,0.05)` | 首页特性卡 |
| Level 5 | `0 8px 24px rgba(0,0,0,0.08)` | 卡片悬停 |
| Level 6 | `0 10px 40px rgba(0,0,0,0.15)` | 登录卡片 |
| Level 7 | `0 15px 50px rgba(0,0,0,0.2)` | 注册卡片 |
| Level 8 | `0 25px 50px -12px rgba(0,0,0,0.25)` | 弹窗 |
| Color Glow-1 | `0 3px 10px rgba(74,144,226,0.2)` | 按钮 |
| Color Glow-2 | `0 5px 16px rgba(74,144,226,0.3)` | 按钮悬停 |
| Color Glow-3 | `0 4px 16px rgba(64,128,255,0.25)` | 返回顶部 |

### 5.4 字体族

```css
font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
             'Helvetica Neue', Helvetica, 'PingFang SC',
             'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
```

### 5.5 字距

| 场景 | letter-spacing |
|------|----------------|
| 大标题 | `2~3px` |
| 导航标题 | `1px` |
| 徽章 | `1px` |
| 按钮 | `2px` |

---

## 6. 文件-组件样式映射速查

| 文件 | 核心组件/样式 |
|------|--------------|
| [Login.vue](file:///d:/参赛作品/Software-ODA125/vue/src/views/Login.vue) | 毛玻璃卡片、渐变背景、输入框穿透 |
| [Register.vue](file:///d:/参赛作品/Software-ODA125/vue/src/views/Register.vue) | 同上，更大圆角 |
| [Home.vue](file:///d:/参赛作品/Software-ODA125/vue/src/views/Home.vue) | Hero渐变、特性卡片、流程步骤、神经网络动画、返回顶部 |
| [DiabetesChat.vue](file:///d:/参赛作品/Software-ODA125/vue/src/views/DiabetesChat.vue) | 聊天气泡、毛玻璃面板、跑马灯、食谱侧边栏、辅助工具卡片、弹窗 |
| [HealthCheck.vue](file:///d:/参赛作品/Software-ODA125/vue/src/views/HealthCheck.vue) | 三色风险等级、渐变文字、光晕动画、轮播组件、弹窗毛玻璃 |
| [DiabetesEducation.vue](file:///d:/参赛作品/Software-ODA125/vue/src/views/DiabetesEducation.vue) | 悬浮导航、章节卡片、信息表格、视频播放器、评论列表 |
| [DoctorWorkbench.vue](file:///d:/参赛作品/Software-ODA125/vue/src/views/DoctorWorkbench.vue) | 三色统计卡片、症状标签 |
| [Person.vue](file:///d:/参赛作品/Software-ODA125/vue/src/views/Person.vue) | 密码强度条三色、危险区红色边框、管理员快捷方式 |
| [Header.vue](file:///d:/参赛作品/Software-ODA125/vue/src/components/Header.vue) | 头像悬停缩放、下拉菜单 |
| [404.vue](file:///d:/参赛作品/Software-ODA125/vue/src/views/404.vue) | 全屏渐变背景、浮动动画 |

---

## 7. scoped 样式穿透规则

本项目使用 `<style scoped>`，**`v-html` 动态内容必须用 `:deep()` 穿透**：

```css
/* v-html 生成的动态内容需要 :deep() */
:deep(.recipe-text .recipe-food-row) {
  display: flex;
  border-bottom: 1px solid #d4c4a8;
}

/* 修改 Element Plus 内部组件样式 */
:deep(.el-input__wrapper) { ... }
:deep(.el-dropdown-menu) { ... }

/* 普通作用域内样式直接写 */
.my-component { ... }
```

---

## 8. 新增页面/组件开发检查清单

- [ ] 背景色使用 `#f5f7fa` 或 `#F8FAFC`（页面级）
- [ ] 卡片使用白色底 + `border-radius: 14~16px` + Level 2 阴影
- [ ] 按钮使用胶囊形 `border-radius: 20px` + 渐变背景 + 悬停上浮
- [ ] 输入框悬停/聚焦显示蓝色光晕 `0 0 0 3px rgba(64,128,255,0.15)`
- [ ] 悬停态使用 `translateY(-2px)` 上浮（按钮 -1px，强卡片 -5~-10px）
- [ ] 过渡动画默认 `ease`，常规交互 `0.2~0.3s`
- [ ] 毛玻璃效果必须同时设置 `backdrop-filter` 和 `-webkit-backdrop-filter`
- [ ] 渐变色统一 `135deg` 方向
- [ ] 风险/状态使用绿-橙-红三色语义系统
- [ ] 阴影使用标准层级，不随意创建新阴影值
- [ ] 文字字号在标准层级中选取，不随意使用中间值
- [ ] 圆角在标准层级中选取，保持统一
- [ ] 自定义滚动条样式使用项目的统一规范
- [ ] `v-html` 内容使用 `:deep()` 穿透 scoped
- [ ] 所有 `el-dialog` 和 `el-drawer` 必须添加 `append-to-body`（避免被 Manage.vue 布局裁切）
