---
name: using-git-worktrees
description: "当开始需要隔离的功能工作时使用，或在执行实现计划之前——创建隔离的git工作树以避免干扰当前工作区，并包含安全检查。"
---

# 使用 Git Worktrees

## 概述

Git worktrees 创建共享同一仓库的隔离工作区，允许同时处理多个分支而无需切换。

**核心原则：** 系统化目录选择 + 安全验证 = 可靠的隔离。

**开始时声明：** "我正在使用 using-git-worktrees 技能设置隔离工作区。"

## 目录选择过程

按此优先级顺序：

### 1. 检查现有目录

```bash
ls -d .worktrees 2>/dev/null     # 首选（隐藏）
ls -d worktrees 2>/dev/null      # 备选
```

如果存在：使用该目录。两者都存在时，`.worktrees` 胜出。

### 2. 检查 CLAUDE.md 或其他配置文件

### 3. 询问用户

如果没有目录存在且无配置偏好：

"未找到 worktree 目录。我应该在哪里创建 worktrees？
1. .worktrees/（项目本地，隐藏）
2. ~/.config/superpowers/worktrees/<project-name>/（全局位置）
你更偏好哪个？"

## 安全验证

### 项目本地目录

创建前必须验证目录被忽略：

```bash
git check-ignore -q .worktrees 2>/dev/null || git check-ignore -q worktrees 2>/dev/null
```

如果未忽略：添加 .gitignore 行并提交更改。

## 创建步骤

### 1. 检测项目名
### 2. 创建 Worktree
```bash
git worktree add "$path" -b "$BRANCH_NAME"
cd "$path"
```
### 3. 运行项目设置

自动检测并运行适当的设置：npm install / pip install / cargo build / go mod download

### 4. 验证干净基线

运行测试确保 worktree 从干净状态开始。

### 5. 报告位置

## 快速参考

| 情况 | 操作 |
|-----------|--------|
| `.worktrees/` 存在 | 使用它（验证被忽略） |
| `worktrees/` 存在 | 使用它（验证被忽略） |
| 两者存在 | 使用 `.worktrees/` |
| 都不存在 | 检查配置 → 询问用户 |
| 目录未被忽略 | 添加到 .gitignore + 提交 |
| 基线测试失败 | 报告失败 + 询问 |

## 红旗

**绝不要：**
- 在未验证被忽略的情况下创建 worktree（项目本地）
- 跳过基线测试验证
- 未经询问就以失败测试继续
- 在模糊时假设目录位置

**始终：**
- 遵循目录优先级：现有 > 配置 > 询问
- 验证项目本地目录被忽略
- 自动检测并运行项目设置
- 验证干净测试基线
