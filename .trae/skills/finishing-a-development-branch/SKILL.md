---
name: finishing-a-development-branch
description: "当实现完成、所有测试通过且需要决定如何集成工作时使用——通过呈现结构化选项（合并、PR、保持或丢弃）引导完成开发工作。"
---

# 完成开发分支

## 概述

通过呈现清晰选项并处理选择的工作流来引导完成开发工作。

**核心原则：** 验证测试 → 呈现选项 → 执行选择 → 清理。

**开始时声明：** "我正在使用 finishing-a-development-branch 技能完成此工作。"

## 流程

### 步骤 1：验证测试

在呈现选项之前，验证测试通过：

```bash
npm test / cargo test / pytest / go test ./...
```

如果测试失败：不能继续合并/PR直到测试通过。停止。
如果测试通过：继续步骤2。

### 步骤 2：确定基础分支

```bash
git merge-base HEAD main 2>/dev/null || git merge-base HEAD master 2>/dev/null
```

### 步骤 3：呈现选项

精确呈现这4个选项：

"实现完成。你想做什么？

1. 本地合并回 <base-branch>
2. 推送并创建 Pull Request
3. 保持分支原样（我稍后处理）
4. 丢弃此工作

选哪个？"

### 步骤 4：执行选择

#### 选项1：本地合并
```bash
git checkout <base-branch>
git pull
git merge <feature-branch>
# 运行测试
git branch -d <feature-branch>
```

#### 选项2：推送并创建PR
```bash
git push -u origin <feature-branch>
gh pr create --title "<title>" --body "..."
```

#### 选项3：保持原样
报告："保持分支 <name>。Worktree 保留在 <path>。"
不清理 worktree。

#### 选项4：丢弃
**首先确认：** 要求输入 'discard' 确认。
确认后：强制删除分支。

### 步骤5：清理 Worktree

选项1、2、4需要清理（选项2保留worktree）。

## 快速参考

| 选项 | 合并 | 推送 | 保留Worktree | 清理分支 |
|--------|-------|------|---------------|----------------|
| 1. 本地合并 | ✓ | - | - | ✓ |
| 2. 创建PR | - | ✓ | ✓ | - |
| 3. 保持原样 | - | - | ✓ | - |
| 4. 丢弃 | - | - | - | ✓（强制） |

## 常见错误

- 跳过测试验证 → 合并损坏代码
- 开放式问题 → 歧义
- 自动worktree清理 → 可能需要时移除
- 丢弃时无确认 → 意外删除工作

## 红旗

**绝不要：**
- 带着失败测试继续
- 未验证合并结果上的测试就合并
- 无确认删除工作
- 无明确请求强制推送

**始终：**
- 在提供选项前验证测试
- 精确呈现4个选项
- 选项4需要输入确认
- 仅选项1和4清理worktree
