# PoetDefaultAvatar 流派维度设计方案

> 对应"已知缺口" #8：PoetDefaultAvatar 流派维度未实现（Poet 无 style 字段，第0级逻辑被注释）
> 优先级：低 | 状态：✅ 已落地（2026-09-05）
> 编制日期：2026-09-05 | 最后更新：2026-09-05

## 1. 现状分析

`PoetDefaultAvatar.getAvatar(Poet poet)` 的分配规则：
- `AVATAR_POOL`：key = `{地区}_{时期}_{性别}`（如 `cn_ancient_male`）——三级兜底候选池
- `STYLE_MAP`：**已定义**，key = 流派名（如 `豪放派`、`婉约派`、`浪漫主义`）→ 头像路径，但**从未被使用**
- 方法第0级注释："精确流派匹配，暂不支持，因为 Poet 没有 style 字段，跳过"
- `Poet` 实体字段确认：`id / name / courtesyName / pseudonym / dynastyId / dynastyName / birthYear / deathYear / birthplace / biography / lifeStory / influence / evaluation / anecdotes / avatar / poetType / status / createTime / updateTime / deleted` —— **无 style 字段**

## 2. 目标
让"流派 → 头像"维度成为最高优先级，仅当流派可解析时优先命中原生 `STYLE_MAP`；未知流派回退现有"地区+时期+性别"分层匹配，保证永不缺失。

## 3. 方案

### 3.1 数据模型
`poet` 表 + `Poet` 实体新增可选字段 `style`（流派），语义与原 `STYLE_MAP` 的 key 对齐：

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `style` | `VARCHAR(32)` NULL | 诗人流派 / 风格标签 | 豪放派、婉约派、浪漫主义、田园派… |

- 迁移脚本：`ALTER TABLE poet ADD COLUMN style VARCHAR(32) NULL COMMENT '诗人流派/风格'`
- 空值表示未知流派，不影响任何现有行

### 3.2 `STYLE_MAP` 扩展与规范化
- `STYLE_MAP` 保留，作为"流派→头像"权威映射
- 同一流派可映射到候选列表（而非单一路径），便于多份图片时随机：
  `"豪放派" -> List.of("….01.jpg","….02.jpg")`，与 `AVATAR_POOL` 结构一致
- 新增 `getAvatarStyle(poet)` 供外部复用，返回当前命中的流派 key 或 null（便于排查）

### 3.3 数据来源（style 怎么来）
`PoetServiceImpl` 在拉取诗人详情/列表时会调外部诗人 API（见 `system-workflow.md` 诗人数据懒加载章节）。实施时：
1. **外部 API 返回含风格字段时** → 映射写入 `poet.style`
2. **外部无该字段 / 本地诗人** → `style` 留空，走回退匹配（不阻塞现有逻辑）
3. 可选补充：`seed.sql` 为知名诗人显式标注流派，让第0级立刻生效

### 3.4 `getAvatar` 流程修订（第0级恢复）
```
1. style 非空 且 STYLE_MAP 存在该流派
   → 直接 randomPick(STYLE_MAP[style])   ← 第0级：流派优先
2. 否则 → 现有"地区+时期+性别"分层（第1~3级 + 全局兜底）
```

### 3.5 兼容与影响面
- 老数据 style 为空：行为与现在完全一致（第0级跳过）
- 有 style 数据：命中流派头像，意图更精准
- `getRandomAvatar()` 不受影响
- **只改后端**：`Poet` 实体、`PoetMapper`（自动映射 `style`）、`SmartSearchServiceImpl` 无需改；`PoetServiceImpl` 拉取逻辑按需补 `style` 填充
- DB 层：`schema.md`、`init.sql`、迁移脚本同步

## 4. 文件影响范围（实施时核对）
- `docs/database/migration_003_poet_style.sql`（或并入同一迁移批次）
- `sc-moyuan-backend/src/main/resources/db/init.sql`
- `sc-moyuan-backend/src/main/java/com/moyuan/entity/Poet.java`
- `sc-moyuan-backend/src/main/java/com/moyuan/util/PoetDefaultAvatar.java`
- `sc-moyuan-backend/src/main/java/com/moyuan/service/impl/PoetServiceImpl.java`（style 填充）
- `docs/database/schema.md`

## 5. 实施情况（2026-09-05）
- ✅ `poet` 表 + `Poet` 实体新增 `style` 字段（VARCHAR(32)），`init.sql` 与 `migration_003_poet_style.sql`、`schema.md` 已同步。
- ✅ `STYLE_MAP` 扩展为「流派→候选头像列表」；`getAvatar()` 恢复第 0 级：`style` 命中 `STYLE_MAP` 优先，否则回退「地区_时期_性别」分层（老数据 `style` 为空时行为不变）。
- ⏳ `style` 数据填充：外部诗人 API 暂无流派字段，`PoetServiceImpl` 不做强行解析；后续可经 `seed.sql` 标注或 API 增强补充。

## 6. 待用户确认 / 后续可选
- [ ] `style` 数据来源：是否后续在 `seed.sql` 为知名诗人显式标注流派（如苏轼=豪放派、李清照=婉约派），让第 0 级对演示立即生效。
- [ ] 流派优先已覆盖「地区_时期_性别」（推荐，已实现）。