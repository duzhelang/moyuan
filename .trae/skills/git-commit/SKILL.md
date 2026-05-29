---
name: "git-commit"
description: "Automates git workflow for task-based commits. Invoke when user uses /git command or wants to commit current task progress."
---

# Git Commit Skill

This skill automates the git commit workflow for task-based development.

## When to Use

- When user inputs `/git` command
- When user wants to commit current task progress
- After completing a logical unit of work
- Before switching to a different task

## Workflow

### 1. Get Task Context

Before committing, retrieve the current task information from `.trae/tasks/`:

- Scan `.trae/tasks/` for files matching the pattern `{YYYYMMDD}-{序号}-{名称}.md`
- Find the task file whose frontmatter `status` is "进行中" (in progress)
- Use the task title and summary as commit context
- If multiple tasks are in progress, ask user to specify which task this commit belongs to
- If no task is in progress, ask user for commit description

### 2. Check Documentation Sync

根据 `project_rules.md` 中的「Git 提交前文档同步检查」规则：

1. 根据本次变更内容，对照「文档-变更映射表」识别需检查的文档
2. 逐一读取相关文档，确认内容是否与代码变更一致
3. 若文档已同步 → 直接进入下一步
4. 若文档未同步 → 更新文档，然后以 `[文档]` 类型单独提交文档变更，再提交代码变更

### 3. Prepare Commit

```bash
# Check git status
git status

# Stage all changes
git add .

# 排除 docs 目录（docs 仅本地维护，不推送到远程）
git reset HEAD -- docs/

# 排除 AI 模型 & 权重文件（体积过大，禁止提交到 Git）
git reset HEAD -- data/models/pth_models/*.pth
git reset HEAD -- data/models/pkl_files/*.pkl
git reset HEAD -- data/models/npy_data/*.npy
git reset HEAD -- data/models/h5_models/*.h5
git reset HEAD -- data/models/**/*.pth
git reset HEAD -- data/models/**/*.pkl
git reset HEAD -- data/models/**/*.npy
git reset HEAD -- data/models/**/*.h5

# Verify staged changes
git diff --cached --stat
```

> **docs 目录排除说明**：`docs/` 目录从 `.gitignore` 中移除以保证 IDE 可见性，但提交时必须通过 `git reset HEAD -- docs/` 将其从暂存区移除，防止误提交到远程仓库。这是一个强制规则，所有提交都必须执行。

> **模型文件排除说明**：`data/models/` 下的 `.pth`、`.pkl`、`.npy`、`.h5` 文件对本地推理必需，但体积过大（通常 10MB-500MB），**绝对不能提交到 Git 仓库**。提交时必须通过 `git reset HEAD` 将其从暂存区移除。

**模型文件类型清单（禁止提交）**：

| 扩展名 | 说明 | 典型位置 |
|--------|------|----------|
| `*.pth` | PyTorch 模型权重 | `data/models/pth_models/` |
| `*.pkl` | Python 序列化（scaler/encoder） | `data/models/pkl_files/` |
| `*.npy` | NumPy 数组（SHAP背景数据） | `data/models/npy_data/` |
| `*.h5` | Keras/TensorFlow 模型 | `data/models/h5_models/` |

### 4. Execute Commit

Use the following commit message format:

```
[Task: <task-title>] <brief-description>

<task-summary-from-.trae/tasks/{当前任务文件}.md>
```

Example:
```bash
git commit -m "[Task: 用户登录功能] 实现JWT认证

- 完成用户登录API
- 添加JWT token生成
- 实现登录状态验证"
```

### 5. Push to Remote

```bash
# Push to current branch
git push

# If tracking upstream
git push -u origin HEAD
```

## Error Handling

### Merge Conflicts
If merge conflicts occur during push:
1. Stop and notify user
2. Display conflicting files
3. Ask user to resolve conflicts

### No Changes
If no changes to commit:
1. Display `git status` output
2. Inform user there are no changes
3. Suggest next steps

### Authentication Issues
If push fails due to authentication:
1. Check git remote configuration
2. Suggest credential helper setup
3. Provide manual push commands

## Integration with Task Management

This skill works with the task management system:
- Scans `.trae/tasks/` for task files matching `{YYYYMMDD}-{序号}-{名称}.md` pattern
- Each task is an independent file with its own frontmatter (title, status, summary)
- Uses task title and summary for commit messages
- After successful commit, can update task progress in the specific task file

## Examples

### Basic Commit
User: `/git`
Result: Commits all changes with current task context

### Commit with Custom Message
User: `/git 修复登录bug`
Result: Commits with custom message appended to task context

## Notes

- Always commit logical units of work
- Use meaningful commit messages
- Push to remote after successful commit
- Handle errors gracefully and inform user
- **必须在 `git add .` 后执行 `git reset HEAD -- docs/`，docs 目录禁止推送到远程**
- **必须在 `git add .` 后执行 `git reset HEAD -- data/models/**`，模型文件禁止推送到远程**