# 首页导航组件重构实现计划

> **给自动化工作者：** 使用 subagent-driven-development（推荐）或 executing-plans 技能来逐任务实现此计划。

**目标：** 重构首页作品、朝代、流派三个导航组件，将硬编码数据改为从API获取，增加鼠标悬浮样式和自动滚动逻辑，并在管理员页面增加管理功能。

**架构：** 
- 后端：新建 `home_navigation` 表存储导航数据，在 `AdminController` 中添加管理接口
- 前端：创建独立的 `HomeNavigation.vue` 组件，管理员页面增加导航管理功能

**技术栈：** Vue 3 + TypeScript + Element Plus + Spring Boot 3 + MyBatis-Plus + MySQL

---

## 文件结构

### 后端文件
- 创建: `sc-moyuan-backend/src/main/java/com/moyuan/entity/HomeNavigation.java`
- 创建: `sc-moyuan-backend/src/main/java/com/moyuan/mapper/HomeNavigationMapper.java`
- 修改: `sc-moyuan-backend/src/main/java/com/moyuan/controller/AdminController.java`

### 前端文件
- 创建: `frontend/src/components/business/HomeNavigation.vue`
- 创建: `frontend/src/views/admin/home-navigation.vue`
- 修改: `frontend/src/api/modules/admin.ts`
- 修改: `frontend/src/views/home/index.vue`
- 修改: `frontend/src/views/admin/index.vue`
- 修改: `frontend/src/router/index.ts`

---

## 任务 1: 创建数据库表

**文件：**
- 执行SQL脚本创建表

- [ ] **步骤 1: 创建 home_navigation 表**

```sql
CREATE TABLE IF NOT EXISTS home_navigation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) NOT NULL COMMENT '类型：works-作品，genres-流派，dynasties-朝代',
    title VARCHAR(100) NOT NULL COMMENT '标题',
    subtitle VARCHAR(200) COMMENT '副标题/描述',
    image_url VARCHAR(500) COMMENT '图片URL',
    link_id BIGINT COMMENT '关联ID（诗词ID、分类ID、朝代ID等）',
    link_type VARCHAR(20) COMMENT '链接类型：poem-诗词，category-分类，dynasty-朝代',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status INT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页导航数据表';
```

- [ ] **步骤 2: 插入初始数据**

```sql
INSERT INTO home_navigation (type, title, subtitle, image_url, link_id, link_type, sort_order) VALUES
-- 作品数据
('works', '黄鹤楼', '唐代:崔颢', 'h6.jpg', 42, 'poem', 1),
('works', '春宫怨', '唐代:杜荀鹤', 'h6.2.jpg', 51, 'poem', 2),
('works', '秋日赴阙题潼关驿楼', '唐代:杜荀鹤', 'h6.3.jpg', 62, 'poem', 3),
('works', '次北固山下', '唐代:许浑', 'h6.4.jpg', 73, 'poem', 4),
('works', '黄鹤楼', '唐代:崔颢', 'h6.jpg', 142, 'poem', 5),
('works', '春宫怨', '唐代:杜荀鹤', 'h6.2.jpg', 151, 'poem', 6),
('works', '秋日赴阙题潼关驿楼', '唐代:杜荀鹤', 'h6.3.jpg', 162, 'poem', 7),
('works', '次北固山下', '唐代:许浑', 'h6.4.jpg', 173, 'poem', 8),
-- 流派数据
('genres', '边塞·豪放', NULL, 'h6_liupai_4.png', 1, 'category', 1),
('genres', '唐宋八大家', NULL, 'h6_liupai_1.jpg', 2, 'category', 2),
('genres', '竹林七贤', NULL, 'h6_liupai_3.png', 3, 'category', 3),
('genres', '元曲四大家', NULL, 'h6_liupai_2.jpg', 4, 'category', 4),
('genres', '两汉', NULL, 'h6_chaodai_1.jpg', 5, 'category', 5),
('genres', '唐朝', NULL, 'h6_chaodai_2.jpg', 6, 'category', 6),
('genres', '宋朝', NULL, 'h6_chaodai_3.jpg', 7, 'category', 7),
('genres', '元朝', NULL, 'cd_suolue (4).jpg', 8, 'category', 8),
-- 朝代数据
('dynasties', '先秦', '蒹葭苍苍，白露为霜。', 'h6_chaodai_1.jpg', 1, 'dynasty', 1),
('dynasties', '两汉', '青青园中葵，朝露待日晞。', 'h6_chaodai_1.jpg', 2, 'dynasty', 2),
('dynasties', '唐朝', '秋风清，秋月明。', 'h6_chaodai_2.jpg', 3, 'dynasty', 3),
('dynasties', '宋朝', '十年生死两茫茫，不思量，自难忘。', 'h6_chaodai_3.jpg', 4, 'dynasty', 4),
('dynasties', '元朝', '枯藤老树昏鸦，小桥流水人家。', 'cd_suolue (4).jpg', 5, 'dynasty', 5),
('dynasties', '南北朝', '滚滚长江东逝水，浪花淘尽英雄。', 'h6_chaodai_2.jpg', 6, 'dynasty', 6),
('dynasties', '金朝', '人生若只如初见，何事秋风悲画扇。', 'h6_chaodai_3.jpg', 7, 'dynasty', 7),
('dynasties', '明清', '春花秋月何时了？往事知多少。', 'cd_suolue (4).jpg', 8, 'dynasty', 8);
```

- [ ] **步骤 3: 验证表创建成功**

运行: `SELECT COUNT(*) FROM home_navigation;`
预期: 返回 24 条记录

---

## 任务 2: 创建后端实体类和Mapper

**文件：**
- 创建: `sc-moyuan-backend/src/main/java/com/moyuan/entity/HomeNavigation.java`
- 创建: `sc-moyuan-backend/src/main/java/com/moyuan/mapper/HomeNavigationMapper.java`

- [ ] **步骤 1: 创建 HomeNavigation 实体类**

```java
package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("home_navigation")
public class HomeNavigation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private String title;
    private String subtitle;
    private String imageUrl;
    private Long linkId;
    private String linkType;
    private Integer sortOrder;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
```

- [ ] **步骤 2: 创建 HomeNavigationMapper 接口**

```java
package com.moyuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyuan.entity.HomeNavigation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HomeNavigationMapper extends BaseMapper<HomeNavigation> {
}
```

- [ ] **步骤 3: 验证编译通过**

运行: `cd sc-moyuan-backend && mvn compile`
预期: BUILD SUCCESS

---

## 任务 3: 在AdminController中添加管理接口

**文件：**
- 修改: `sc-moyuan-backend/src/main/java/com/moyuan/controller/AdminController.java`

- [ ] **步骤 1: 添加 HomeNavigationMapper 依赖注入**

在 AdminController 类中添加字段：

```java
private final HomeNavigationMapper homeNavigationMapper;
```

- [ ] **步骤 2: 添加获取首页导航列表接口**

```java
@Operation(summary = "获取首页导航列表")
@GetMapping("/home-navigation")
public R<?> listHomeNavigation(
        @RequestParam(required = false) String type) {
    LambdaQueryWrapper<HomeNavigation> wrapper = new LambdaQueryWrapper<>();
    if (StringUtils.hasText(type)) {
        wrapper.eq(HomeNavigation::getType, type);
    }
    wrapper.eq(HomeNavigation::getStatus, 1)
           .orderByAsc(HomeNavigation::getSortOrder);
    return R.success(homeNavigationMapper.selectList(wrapper));
}
```

- [ ] **步骤 3: 添加管理端获取首页导航列表接口**

```java
@Operation(summary = "管理端获取首页导航列表")
@GetMapping("/home-navigation/manage")
public R<?> listHomeNavigationManage(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String type) {
    LambdaQueryWrapper<HomeNavigation> wrapper = new LambdaQueryWrapper<>();
    if (StringUtils.hasText(type)) {
        wrapper.eq(HomeNavigation::getType, type);
    }
    wrapper.orderByAsc(HomeNavigation::getType)
           .orderByAsc(HomeNavigation::getSortOrder);
    return R.success(homeNavigationMapper.selectPage(new Page<>(page, size), wrapper));
}
```

- [ ] **步骤 4: 添加创建首页导航接口**

```java
@Operation(summary = "创建首页导航")
@PostMapping("/home-navigation")
public R<HomeNavigation> createHomeNavigation(@RequestBody HomeNavigation homeNavigation) {
    homeNavigationMapper.insert(homeNavigation);
    return R.success(homeNavigation);
}
```

- [ ] **步骤 5: 添加更新首页导航接口**

```java
@Operation(summary = "更新首页导航")
@PutMapping("/home-navigation/{id}")
public R<HomeNavigation> updateHomeNavigation(@PathVariable Long id, @RequestBody HomeNavigation homeNavigation) {
    homeNavigation.setId(id);
    homeNavigationMapper.updateById(homeNavigation);
    return R.success(homeNavigation);
}
```

- [ ] **步骤 6: 添加删除首页导航接口**

```java
@Operation(summary = "删除首页导航")
@DeleteMapping("/home-navigation/{id}")
public R<Void> deleteHomeNavigation(@PathVariable Long id) {
    homeNavigationMapper.deleteById(id);
    return R.success();
}
```

- [ ] **步骤 7: 验证编译通过**

运行: `cd sc-moyuan-backend && mvn compile`
预期: BUILD SUCCESS

---

## 任务 4: 添加前端API接口

**文件：**
- 修改: `frontend/src/api/modules/admin.ts`

- [ ] **步骤 1: 添加首页导航管理API函数**

在 admin.ts 文件末尾添加：

```typescript
export function getHomeNavigationList(params?: { type?: string }) {
  return request.get<any[]>('/admin/home-navigation', { params })
}

export function getAdminHomeNavigation(params: { page?: number; size?: number; type?: string }) {
  return request.get<{ records: any[]; total: number }>('/admin/home-navigation/manage', { params })
}

export function createHomeNavigation(data: any) {
  return request.post<any>('/admin/home-navigation', data)
}

export function updateHomeNavigation(id: number, data: any) {
  return request.put<any>(`/admin/home-navigation/${id}`, data)
}

export function deleteHomeNavigation(id: number) {
  return request.delete<void>(`/admin/home-navigation/${id}`)
}
```

- [ ] **步骤 2: 验证TypeScript编译通过**

运行: `cd frontend && npm run type-check`
预期: 无类型错误

---

## 任务 5: 创建首页导航组件

**文件：**
- 创建: `frontend/src/components/business/HomeNavigation.vue`

- [ ] **步骤 1: 创建 HomeNavigation 组件**

```vue
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHomeNavigationList } from '@/api/modules/admin'

interface NavigationItem {
  id: number
  type: string
  title: string
  subtitle?: string
  imageUrl: string
  linkId: number
  linkType: string
  sortOrder: number
}

const props = defineProps<{
  type: 'works' | 'genres' | 'dynasties'
  title: string
}>()

const router = useRouter()
const items = ref<NavigationItem[]>([])
const scrollContainerRef = ref<HTMLElement | null>(null)
const autoScrollTimer = ref<ReturnType<typeof setInterval> | null>(null)
const isHovering = ref(false)

const fetchItems = async () => {
  try {
    const res = await getHomeNavigationList({ type: props.type })
    items.value = res.data || []
  } catch (error) {
    console.error(`获取${props.title}数据失败`, error)
  }
}

const handleClick = (item: NavigationItem) => {
  if (item.linkType === 'poem') {
    router.push(`/poem/${item.linkId}`)
  } else if (item.linkType === 'category') {
    router.push({ path: '/poem', query: { category: String(item.linkId) } })
  } else if (item.linkType === 'dynasty') {
    router.push({ path: '/poem', query: { dynastyId: String(item.linkId) } })
  }
}

const scrollLeft = () => {
  if (scrollContainerRef.value) {
    scrollContainerRef.value.scrollBy({ left: -295, behavior: 'smooth' })
  }
}

const scrollRight = () => {
  if (scrollContainerRef.value) {
    scrollContainerRef.value.scrollBy({ left: 295, behavior: 'smooth' })
  }
}

const startAutoScroll = () => {
  autoScrollTimer.value = setInterval(() => {
    if (!isHovering.value && scrollContainerRef.value) {
      const { scrollLeft, scrollWidth, clientWidth } = scrollContainerRef.value
      if (scrollLeft + clientWidth >= scrollWidth - 10) {
        scrollContainerRef.value.scrollTo({ left: 0, behavior: 'smooth' })
      } else {
        scrollContainerRef.value.scrollBy({ left: 295, behavior: 'smooth' })
      }
    }
  }, 3000)
}

const stopAutoScroll = () => {
  if (autoScrollTimer.value) {
    clearInterval(autoScrollTimer.value)
    autoScrollTimer.value = null
  }
}

const handleMouseEnter = () => {
  isHovering.value = true
}

const handleMouseLeave = () => {
  isHovering.value = false
}

onMounted(() => {
  fetchItems()
  startAutoScroll()
})

onUnmounted(() => {
  stopAutoScroll()
})
</script>

<template>
  <div class="h6">
    <h1 class="sy_cebiaoti">{{ title }}</h1>
    <div class="scrollableDiv h6_li" :class="'h6_li_' + type" @mouseenter="handleMouseEnter" @mouseleave="handleMouseLeave">
      <div class="leftButton" @click="scrollLeft">
        <img src="/img/jianzu (3).png" alt="">
      </div>
      <div class="rightButton" @click="scrollRight">
        <img src="/img/jianzu (4).png" alt="">
      </div>
      <div ref="scrollContainerRef" class="scroll-inner">
        <div
          v-for="item in items"
          :key="item.id"
          class="l1"
          @click="handleClick(item)"
        >
          <img :src="'/img/' + item.imageUrl" :alt="item.title">
          <li>{{ item.title }}</li>
          <p v-if="item.subtitle">{{ item.subtitle }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.h6 {
  position: relative;
}

.scrollableDiv {
  position: relative;
  overflow: hidden;
}

.scroll-inner {
  display: flex;
  overflow-x: auto;
  scroll-behavior: smooth;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.scroll-inner::-webkit-scrollbar {
  display: none;
}

.h6_li .l1 {
  width: 295px;
  min-width: 295px;
  height: 200px;
  text-align: center;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.h6_li .l1:hover {
  transform: scale(1.05);
}

.h6_li .l1 img {
  border-radius: 5%;
  width: 290px;
  height: 150px;
  display: block;
  transition: all 0.3s ease;
}

.h6_li .l1:hover img {
  border-radius: 20%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.h6_li .l1 li {
  display: block;
  text-align: center;
  list-style: none;
  font-size: 16px;
  font-weight: 500;
  margin-top: 8px;
  transition: color 0.3s ease;
}

.h6_li .l1:hover li {
  color: #409eff;
}

.h6_li .l1 p {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
  transition: color 0.3s ease;
}

.h6_li .l1:hover p {
  color: #333;
}

.leftButton,
.rightButton {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  opacity: 0;
}

.scrollableDiv:hover .leftButton,
.scrollableDiv:hover .rightButton {
  opacity: 1;
}

.leftButton {
  left: 10px;
}

.rightButton {
  right: 10px;
}

.leftButton:hover,
.rightButton:hover {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.leftButton img,
.rightButton img {
  width: 20px;
  height: 20px;
}
</style>
```

- [ ] **步骤 2: 验证组件创建成功**

运行: `cd frontend && npm run type-check`
预期: 无类型错误

---

## 任务 6: 修改首页使用新组件

**文件：**
- 修改: `frontend/src/views/home/index.vue`

- [ ] **步骤 1: 导入 HomeNavigation 组件**

在 script setup 中添加导入：

```typescript
import HomeNavigation from '@/components/business/HomeNavigation.vue'
```

- [ ] **步骤 2: 删除硬编码数据**

删除以下代码（约第67-98行）：

```typescript
const worksData = [
  { title: '黄鹤楼', dynasty: '唐代:崔颢', src: 'h6.jpg', id: 42 },
  // ... 其他数据
]

const genresData = [
  { title: '边塞·豪放', src: 'h6_liupai_4.png', link: 1 },
  // ... 其他数据
]

const dynastiesData = [
  { title: '先秦', content: '蒹葭苍苍，白露为霜。', src: 'h6_chaodai_1.jpg', link: 1 },
  // ... 其他数据
]
```

- [ ] **步骤 3: 删除 scrollContainer 函数**

删除以下代码（约第146-149行）：

```typescript
const scrollContainer = (className: string, direction: number) => {
  const el = document.querySelector('.' + className) as HTMLElement
  if (el) el.scrollLeft += direction * 295
}
```

- [ ] **步骤 4: 替换模板中的作品部分**

将原来的：

```html
<div class="h6">
  <h1 class="sy_cebiaoti">作品</h1>
  <div class="h6_li_1 scrollableDiv h6_li">
    <div class="leftButton" @click="scrollContainer('h6_li_1', -1)">
      <img :src="asset('/img/jianzu (3).png')" alt="">
    </div>
    <div class="rightButton" @click="scrollContainer('h6_li_1', 1)">
      <img :src="asset('/img/jianzu (4).png')" alt="">
    </div>
    <div
      v-for="(work, idx) in worksData"
      :key="idx"
      class="l1"
      @click="handlePoemClick(work.id)"
    >
      <img :src="asset('/img/' + work.src)" :alt="work.title">
      <li>{{ work.title }}</li>
      <p>{{ work.dynasty }}</p>
    </div>
  </div>
</div>
```

替换为：

```html
<HomeNavigation type="works" title="作品" />
```

- [ ] **步骤 5: 替换模板中的流派部分**

将原来的：

```html
<div class="h6">
  <h1 class="sy_cebiaoti">流派</h1>
  <div class="h6_li_2 scrollableDiv h6_li">
    <div class="leftButton" @click="scrollContainer('h6_li_2', -1)">
      <img :src="asset('/img/jianzu (3).png')" alt="">
    </div>
    <div class="rightButton" @click="scrollContainer('h6_li_2', 1)">
      <img :src="asset('/img/jianzu (4).png')" alt="">
    </div>
    <div
      v-for="(genre, idx) in genresData"
      :key="idx"
      class="l1"
      @click="router.push({ path: '/poem', query: { category: String(genre.link) } })"
    >
      <img :src="asset('/img/' + genre.src)" :alt="genre.title">
      <li>{{ genre.title }}</li>
    </div>
  </div>
</div>
```

替换为：

```html
<HomeNavigation type="genres" title="流派" />
```

- [ ] **步骤 6: 替换模板中的朝代部分**

将原来的：

```html
<div class="h6">
  <h1 class="sy_cebiaoti">朝代</h1>
  <div class="h6_li_3 scrollableDiv h6_li">
    <div class="leftButton" @click="scrollContainer('h6_li_3', -1)">
      <img :src="asset('/img/jianzu (3).png')" alt="">
    </div>
    <div class="rightButton" @click="scrollContainer('h6_li_3', 1)">
      <img :src="asset('/img/jianzu (4).png')" alt="">
    </div>
    <div
      v-for="(dynastyItem, idx) in dynastiesData"
      :key="idx"
      class="l1"
      @click="router.push({ path: '/poem', query: { dynastyId: String(dynastyItem.link) } })"
    >
      <img :src="asset('/img/' + dynastyItem.src)" :alt="dynastyItem.title">
      <li>{{ dynastyItem.title }}</li>
    </div>
  </div>
</div>
```

替换为：

```html
<HomeNavigation type="dynasties" title="朝代" />
```

- [ ] **步骤 7: 验证页面功能正常**

运行: `cd frontend && npm run dev`
访问首页，验证三个导航组件正常显示和交互

---

## 任务 7: 创建管理员页面组件

**文件：**
- 创建: `frontend/src/views/admin/home-navigation.vue`

- [ ] **步骤 1: 创建 home-navigation.vue 组件**

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminHomeNavigation, createHomeNavigation, updateHomeNavigation, deleteHomeNavigation } from '@/api/modules/admin'

const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const filterType = ref('')

const form = ref({
  id: 0,
  type: 'works',
  title: '',
  subtitle: '',
  imageUrl: '',
  linkId: null as number | null,
  linkType: 'poem',
  sortOrder: 0,
  status: 1
})

const typeOptions = [
  { label: '作品', value: 'works' },
  { label: '流派', value: 'genres' },
  { label: '朝代', value: 'dynasties' }
]

const linkTypeOptions = [
  { label: '诗词', value: 'poem' },
  { label: '分类', value: 'category' },
  { label: '朝代', value: 'dynasty' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminHomeNavigation({ 
      page: page.value, 
      size: pageSize.value,
      type: filterType.value || undefined
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取首页导航列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { id: 0, type: 'works', title: '', subtitle: '', imageUrl: '', linkId: null, linkType: 'poem', sortOrder: 0, status: 1 }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该导航项吗？', '提示', { type: 'warning' })
    await deleteHomeNavigation(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') console.error('删除失败', error)
  }
}

const handleSave = async () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!form.value.imageUrl.trim()) {
    ElMessage.warning('请输入图片URL')
    return
  }
  
  loading.value = true
  try {
    if (isEdit.value) {
      await updateHomeNavigation(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createHomeNavigation(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('保存失败', error)
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  page.value = 1
  fetchData()
}

const getTypeLabel = (type: string) => {
  const option = typeOptions.find(o => o.value === type)
  return option ? option.label : type
}

const getLinkTypeLabel = (linkType: string) => {
  const option = linkTypeOptions.find(o => o.value === linkType)
  return option ? option.label : linkType
}

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">首页导航管理</h2>

    <el-card class="filter-card">
      <el-row :gutter="16" align="middle">
        <el-col :span="8">
          <el-select v-model="filterType" placeholder="筛选类型" clearable @change="handleFilter" style="width: 100%">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-col>
        <el-col :span="16" style="text-align: right">
          <el-button type="primary" @click="handleAdd">新增导航项</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 'works' ? 'primary' : row.type === 'genres' ? 'success' : 'warning'" size="small">
              {{ getTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" width="150" show-overflow-tooltip />
        <el-table-column prop="subtitle" label="副标题" width="180" show-overflow-tooltip />
        <el-table-column prop="imageUrl" label="图片" min-width="120" show-overflow-tooltip />
        <el-table-column label="链接类型" width="80">
          <template #default="{ row }">
            {{ getLinkTypeLabel(row.linkType) }}
          </template>
        </el-table-column>
        <el-table-column prop="linkId" label="关联ID" width="80" />
        <el-table-column prop="sortOrder" label="排序" width="60" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination 
          v-model:current-page="page" 
          v-model:page-size="pageSize" 
          :total="total" 
          layout="total, prev, pager, next" 
          @current-change="fetchData" 
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑导航项' : '新增导航项'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="类型" required>
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" placeholder="请输入副标题（可选）" />
        </el-form-item>
        <el-form-item label="图片URL" required>
          <el-input v-model="form.imageUrl" placeholder="请输入图片文件名" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="链接类型">
              <el-select v-model="form.linkType" style="width: 100%">
                <el-option v-for="opt in linkTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联ID">
              <el-input-number v-model="form.linkId" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-title {
  font-size: 24px;
  margin-bottom: 20px;
  color: #333;
}

.filter-card {
  margin-bottom: 16px;
}

.table-card {
  border-radius: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
```

- [ ] **步骤 2: 验证组件创建成功**

运行: `cd frontend && npm run type-check`
预期: 无类型错误

---

## 任务 8: 添加管理员页面路由和菜单

**文件：**
- 修改: `frontend/src/router/index.ts`
- 修改: `frontend/src/views/admin/index.vue`

- [ ] **步骤 1: 添加路由配置**

在 router/index.ts 的 admin 路由子项中添加：

```typescript
{
  path: 'home-navigation',
  name: 'AdminHomeNavigation',
  component: () => import('@/views/admin/home-navigation.vue'),
  meta: { title: '首页导航管理', requiresAuth: true, requiresAdmin: true }
}
```

- [ ] **步骤 2: 添加菜单项**

在 admin/index.vue 的 adminMenus 数组中添加：

```typescript
{ path: '/admin/home-navigation', name: 'AdminHomeNavigation', icon: 'Menu', label: '首页导航管理' }
```

- [ ] **步骤 3: 验证路由和菜单正常**

运行: `cd frontend && npm run dev`
访问管理后台，验证首页导航管理菜单显示正常

---

## 任务 9: 测试和验证

- [ ] **步骤 1: 启动后端服务**

运行: `cd sc-moyuan-backend && mvn spring-boot:run`
预期: 服务启动成功

- [ ] **步骤 2: 启动前端服务**

运行: `cd frontend && npm run dev`
预期: 前端服务启动成功

- [ ] **步骤 3: 测试管理员功能**

1. 访问管理后台
2. 点击"首页导航管理"菜单
3. 验证列表显示正常
4. 测试新增、编辑、删除功能
5. 测试按类型筛选功能

- [ ] **步骤 4: 测试首页显示**

1. 访问首页
2. 验证三个导航组件正常显示
3. 测试鼠标悬浮样式效果
4. 测试自动滚动功能
5. 测试手动滚动按钮
6. 测试点击跳转功能

---

## 执行交接

计划已完成并保存到 `docs/superpowers/plans/2026-06-01-home-navigation-refactor.md`。两种执行选项：

1. **子代理驱动（推荐）** - 每个任务调度新子代理，任务间进行审查，快速迭代
2. **内联执行** - 在此会话中使用 executing-plans 执行任务，批量执行含检查点

哪种方式？
