---
name: "icon-system"
description: "项目的图标系统使用指南，包括Element Plus图标库的使用方式、图标映射机制、后端图标接口等。当需要使用或修改图标时调用此技能。"
---

# 图标系统使用指南

## 一、图标库

项目使用 **`@element-plus/icons-vue`** (版本 `^2.3.2`) 作为唯一图标库，已在 `main.js` 中全局注册所有图标组件。

## 二、图标使用方式

### 方式1：组件化方式（推荐）

```html
<el-icon>
  <component :is="iconComponentName" />
</el-icon>
```

示例：
```html
<el-icon><House /></el-icon>
<el-icon><User /></el-icon>
<el-icon><Document /></el-icon>
```

### 方式2：直接引用组件名

```html
<el-icon><Upload /></el-icon>
<el-icon><Check /></el-icon>
```

### 方式3：`:prefix-icon` 绑定

```html
<el-input :prefix-icon="User" />
<el-input :prefix-icon="Lock" />
```

需要导入图标：
```javascript
import { User, Lock } from '@element-plus/icons-vue';
```

## 三、图标映射机制

数据库中存储的是旧格式图标名称（如 `el-icon-house`），需要映射为新组件名（如 `House`）。

### 映射函数

```javascript
const getIconComponent = (iconName) => {
  const iconMap = {
    'el-icon-coffee': 'Coffee',
    'el-icon-document': 'Document',
    'el-icon-house': 'House',
    'el-icon-user': 'User',
    'el-icon-s-grid': 'Grid',
    'el-icon-setting': 'Setting',
    'el-icon-menu': 'Menu',
    'el-icon-s-custom': 'UserFilled',
    'el-icon-upload': 'Upload',
    'el-icon-dashboard': 'Odometer',
    'el-icon-pie-chart': 'PieChart',
    'el-icon-data-analysis': 'DataAnalysis',
    'el-icon-chat-dot-round': 'ChatDotRound',
    'el-icon-book': 'Book',
    'el-icon-alert': 'Alert',
    'el-icon-s-data': 'DataLine',
    'el-icon-info': 'InfoFilled',
    'el-icon-search': 'Search',
    'el-icon-plus': 'Plus',
    'el-icon-download': 'Download',
    'el-icon-delete': 'Delete',
    'el-icon-edit': 'Edit',
    'el-icon-top': 'Top',
    'el-icon-loading': 'Loading',
    'el-icon-lock': 'Lock',
    'el-icon-phone': 'Phone'
  };
  return iconMap[iconName] || 'Menu';
};
```

### 使用示例

```html
<el-icon v-if="item.icon">
  <component :is="getIconComponent(item.icon)" />
</el-icon>
<el-icon v-else><Menu /></el-icon>
```

## 四、后端图标接口

### 接口地址

```
GET /menu/icons
```

### 返回数据

```json
{
  "code": "200",
  "msg": "success",
  "data": [
    { "name": "coffee", "value": "el-icon-coffee", "type": "icon" },
    { "name": "document", "value": "el-icon-document", "type": "icon" },
    { "name": "house", "value": "el-icon-house", "type": "icon" }
  ]
}
```

### 前端调用

```javascript
request.get("/menu/icons").then(res => {
  this.options = res.data || [];
});
```

## 五、常用图标对照表

| 旧格式 (数据库) | 新组件名 (Element Plus) | 用途 |
|-----------------|------------------------|------|
| el-icon-house | House | 主页 |
| el-icon-user | User | 用户 |
| el-icon-s-custom | UserFilled | 用户管理 |
| el-icon-s-grid | Grid | 系统管理 |
| el-icon-setting | Setting | 设置 |
| el-icon-menu | Menu | 菜单 |
| el-icon-document | Document | 文档 |
| el-icon-data-analysis | DataAnalysis | 数据分析 |
| el-icon-pie-chart | PieChart | 图表 |
| el-icon-s-data | DataLine | 数据面板 |
| el-icon-chat-dot-round | ChatDotRound | 智能对话 |
| el-icon-upload | Upload | 上传 |
| el-icon-download | Download | 下载 |
| el-icon-search | Search | 搜索 |
| el-icon-plus | Plus | 新增 |
| el-icon-edit | Edit | 编辑 |
| el-icon-delete | Delete | 删除 |

## 六、注意事项

1. **全局注册**：所有图标已在 `main.js` 中全局注册，无需单独导入
2. **映射函数重复**：`getIconComponent()` 在 `Aside.vue` 和 `Menu.vue` 中各维护一份
3. **旧式类名**：部分页面仍使用 `el-icon-*` CSS 类名格式，在 Element Plus 2.x 中不会生效
4. **默认图标**：未匹配的图标默认显示 `Menu`
