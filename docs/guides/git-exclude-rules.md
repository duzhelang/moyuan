# Git 屏蔽规范

## 概述

本项目使用两种方式管理 Git 排除规则：

| 方式 | 文件 | 用途 | 是否提交到仓库 |
|------|------|------|----------------|
| `.gitignore` | 项目根目录 | 通用排除规则（编译产物、依赖、敏感文件等） | 是 |
| `.git/info/exclude` | Git 本地配置 | 项目专属本地排除（文档、Trae配置等） | 否 |

## 排除规则分类

### 1. `.gitignore` 管理（提交到仓库）

适用于所有开发者通用的排除规则：

```gitignore
# 编译产物
target/
build/
frontend/dist/

# 依赖
node_modules/

# 环境配置
.env
.env.local

# 日志
logs/
*.log

# IDE 配置
.idea/
.vscode/
```

### 2. `.git/info/exclude` 管理（本地维护）

适用于项目专属的本地排除规则，**不提交到仓库**：

```gitignore
# docs/ - 项目本地文档（架构、API、数据库设计等）
# .trae/ - Trae规则、技能、智能体配置、任务持久化等
# secrets/ - 敏感配置（API密钥、数据库密码等）
docs/
.trae/
secrets/
README.md
```

## 新开发者配置指南

### 步骤 1：克隆仓库

```bash
git clone <repository-url>
cd SC_MoYuan2_
```

### 步骤 2：配置本地排除规则

编辑 `.git/info/exclude` 文件，添加以下内容：

```gitignore
# ==================== 墨渊项目本地排除规则 ====================
# 以下目录/文件为本地维护，不提交到远程仓库。
# 通过 .git/info/exclude 管理，该文件不会随仓库提交。

# docs/ - 项目本地文档（架构、API、数据库设计等）
# .trae/ - Trae规则、技能、智能体配置、任务持久化等
# secrets/ - 敏感配置（API密钥、数据库密码等）
# 以上目录可被 IDE/检索工具访问，但不纳入版本控制
docs/
.trae/
secrets/
README.md
```

### 步骤 3：创建本地目录

```bash
# 创建 docs 目录结构
mkdir -p docs/{api,architecture,business,constraints,database,guides,standards}

# 创建 .trae 目录结构
mkdir -p .trae/{rules,skills,tasks}
```

### 步骤 4：从团队共享位置获取文档

从团队文档共享平台（如飞书、Confluence）获取最新文档，放置到 `docs/` 目录。

## 目录结构说明

### docs/ 目录

```
docs/
├── api/                    # API 文档
│   ├── auth.md            # 认证接口
│   └── endpoints.md       # 接口列表
├── architecture/           # 架构文档
│   └── system-architecture.md
├── business/               # 业务文档
│   └── modules.md
├── constraints/            # 约束文档
│   └── tech-stack-constraints.md
├── database/               # 数据库文档
│   └── schema.md
├── guides/                 # 指南文档
│   ├── development-guide.md
│   └── sandbox-workflow.md
└── standards/              # 规范文档
    ├── backend-standards.md
    ├── database-standards.md
    └── frontend-standards.md
```

### .trae/ 目录

```
.trae/
├── rules/                  # Trae 规则文件
│   ├── project_rules_always.md
│   ├── project_rules_task_specific.md
│   ├── skills-trigger-rules.md
│   └── doc-change-mapping.md
├── skills/                 # Trae 技能配置
└── tasks/                  # 任务持久化文件
```

## 文档同步检查

### Git 提交前检查流程

根据 `project_rules_task_specific.md` 中的规则：

1. **识别影响范围**：根据代码变更判断涉及哪些文档
2. **检查文档状态**：逐一检查相关文档是否与代码一致
3. **决策处理**：
   - 已同步 → 直接提交
   - 未同步 → 更新文档后再提交

### 文档-变更映射表

| 变更类型 | 需检查的文档 |
|----------|-------------|
| 新增/修改 Vue 组件 | `docs/standards/frontend-standards.md`、`docs/architecture/system-architecture.md` |
| 新增/修改后端 API | `docs/api/endpoints.md`、`docs/api/auth.md` |
| 新增/修改数据库表 | `docs/database/schema.md` |
| 新增/修改业务模块 | `docs/business/modules.md` |
| 任何代码变更 | `README.md`（版本号和更新日期） |

## 常见问题

### Q1: 为什么 docs/ 不提交到仓库？

docs/ 包含项目专属文档，可能包含敏感信息或本地配置，且更新频率高，提交到仓库会增加仓库体积和合并冲突。

### Q2: 如何获取最新文档？

从团队文档共享平台获取，或联系项目负责人。

### Q3: .git/info/exclude 和 .gitignore 的区别？

- `.gitignore`：提交到仓库，所有开发者共享
- `.git/info/exclude`：本地文件，不提交到仓库，每个开发者独立配置

### Q4: 如何临时取消排除？

```bash
# 强制添加被排除的文件
git add -f docs/some-file.md
```

## 相关技能

- **git-commit**：提交前自动检查文档同步
- **doc-manager**：文档更新管理
- **moyuan-architecture**：架构文档索引
- **moyuan-data-assets**：数据资产文档索引
