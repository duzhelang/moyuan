---
name: "floating-marquee-panel"
description: "右侧悬浮跑马灯面板组件，包含无限循环滚动、文字框样式、悬停效果等功能。Invoke when user wants to modify the floating side panel with scrolling questions in DiabetesChat.vue."
---

# 右侧悬浮跑马灯面板

## 组件概述

该组件位于 DiabetesChat.vue 中，在对话窗口右侧展示可点击的示例问题，支持无限循环滚动效果。

## 核心功能

### 1. 无限循环滚动
- 使用 `requestAnimationFrame` 实现平滑连续滚动
- 速度可调（默认 0.3px/帧）
- 支持鼠标悬停暂停/恢复

### 2. 文字框样式
- **尺寸**: 宽度 240px，视口高度 360px
- **内边距**: 面板 12px，视口 12px，项目 12px 16px
- **字体**: 15px，行高 1.7，首行缩进 2em
- **背景**: 蓝色渐变 `#e4eeff → #d0e0ff`
- **边框**: 1px 蓝灰色 `#d0d7e2`，圆角 12px
- **阴影**: 双层阴影实现悬浮感

### 3. 悬停效果
- 背景渐变变深
- 边框高亮为蓝色
- 文字色变蓝
- 阴影增强
- 向右偏移 4px

## 关键代码位置

### 模板结构
```vue
<div class="floating-side-panel">
  <div class="panel-title-wrapper">...</div>
  <div class="marquee-viewport">
    <div class="marquee-track" ref="marqueeTrack">
      <div v-for="(q, i) in allQuestions" :key="'a'+i" class="marquee-item">{{ q }}</div>
      <div v-for="(q, i) in allQuestions" :key="'b'+i" class="marquee-item">{{ q }}</div>
    </div>
  </div>
</div>
```

### 滚动逻辑
```javascript
startMarquee() {
  const speed = 0.3;
  const animate = () => {
    if (!this.marqueePaused) {
      const currentY = parseFloat(track.style.transform?.match(/-?[\d.]+/)?.[0] || 0);
      const halfHeight = track.scrollHeight / 2;
      const newY = currentY - speed;
      track.style.transform = `translateY(${Math.abs(newY) >= halfHeight ? 0 : newY}px)`;
    }
    this.marqueeAnimId = requestAnimationFrame(animate);
  };
}
```

## 使用场景

- 糖尿病健康咨询助手的示例问题展示
- 引导用户提问的推荐问题列表
- 需要展示滚动内容的悬浮侧边栏

## 样式变量

| 变量 | 当前值 | 说明 |
|------|--------|------|
| `speed` | 0.3 | 滚动速度 (px/帧) |
| `panel-width` | 240px | 面板宽度 |
| `viewport-height` | 360px | 视口高度 |
| `font-size` | 15px | 文字大小 |
| `text-indent` | 2em | 首行缩进 |
| `bg-gradient` | #e4eeff → #d0e0ff | 背景渐变 |
| `border-color` | #d0d7e2 | 边框颜色 |

## 自定义建议

1. **调整滚动速度**: 修改 `startMarquee()` 中的 `speed` 值
2. **改变颜色主题**: 修改 `.marquee-item` 的 `background` 和 `color`
3. **调整尺寸**: 修改 `.floating-side-panel` 的 `width` 和 `.marquee-viewport` 的 `height`
4. **修改动画效果**: 调整 `transition` 属性和阴影参数