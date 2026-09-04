---
name: "git-commit"
description: "古今诗话——墨渊（SC_MoYuan2）项目的 Git 提交推送专用技能。当用户使用 /git 指令、要求提交代码、推送变更、查看 git 状态时触发。自动执行规范化的 git 工作流：状态检查 → 差异分析 → 智能暂存 → 规范提交 → 远程推送，并在提交前检查文档同步。"
---

# git-commit — Git 提交推送工作流

## 触发条件

- 用户输入 `/git` 指令
- 用户要求"提交代码"、"推送变更"、"git 提交"、"commit"
- 用户要求查看 git 状态、提交历史

## 提交规范

遵循 `<type>: <subject>` 格式（本项目实际提交风格，中文 subject）：

| type | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat: 新增韵律查询模块` |
| `fix` | Bug 修复 | `fix: 修复 JWT 过期未刷新` |
| `docs` | 文档变更 | `docs: 更新 API 端点文档` |
| `style` | 格式调整 | `style: 统一组件缩进` |
| `refactor` | 重构 | `refactor: 拆分首页子组件` |
| `test` | 测试 | `test: 补充前端和后端测试用例` |
| `chore` | 构建/工具 | `chore: 更新 .gitignore` |

**要求**：
- subject 不超过 50 字符，使用中文
- 不以句号结尾
- 如需详细说明，空一行后写描述
- 涉及任务时，可在描述中关联任务概要（`[最后更新：年-月-日 时:分] 任务标题`）

## 标准工作流

执行此技能时，按以下步骤顺序执行：

### 第一步：状态检查

```powershell
git status
git log --oneline -5
```

- 查看当前分支、未暂存/未跟踪文件
- 查看最近 5 条提交记录，了解提交风格

### 第二步：差异分析

```powershell
git diff
git diff --cached
```

- 分析工作区和暂存区的变更内容
- 确定变更类型（feat/fix/refactor 等）
- 确定影响范围，据此调用 doc-manager 检查文档是否需同步

### 第三步：智能暂存

根据变更内容选择性暂存，**禁止使用 `git add -A` 或 `git add .`**：

```powershell
# 按模块分别暂存
git add frontend/src/...
git add sc-moyuan-backend/src/...
git add .gitignore
```

**安全检查**：
- 确认不包含 `.env`、密钥文件、敏感配置
- 确认不包含 `docs/`、`.trae/`、`secrets/`、`README.md`（由 .git/info/exclude 控制）
- 确认不包含 `target/`、`node_modules/`、`dist/` 等构建产物

### 第四步：规范提交

使用 HEREDOC 格式提交：

```powershell
git commit -m "$(cat <<'EOF'
<type>: <subject>

<可选的详细描述>
EOF
)"
```

### 第五步：远程推送

```powershell
# 首次推送（设置上游分支）
git push -u origin <branch>

# 后续推送
git push
```

### 代理配置（网络受限时使用）

当 HTTPS 推送失败时，自动检测并使用本地代理：

```powershell
# 常见代理端口列表
$proxyPorts = @(7890, 1080, 10808, 10809, 8080, 33210)

# 自动检测可用代理端口
$availablePort = $null
foreach ($port in $proxyPorts) {
    $result = Test-NetConnection -ComputerName 127.0.0.1 -Port $port -WarningAction SilentlyContinue
    if ($result.TcpTestSucceeded) {
        $availablePort = $port
        break
    }
}

# 设置代理并推送
if ($availablePort) {
    git config --global http.proxy "http://127.0.0.1:$availablePort"
    git config --global https.proxy "http://127.0.0.1:$availablePort"
    git push
    git config --global --unset http.proxy
    git config --global --unset https.proxy
} else {
    Write-Host "未检测到可用代理，请检查网络连接"
}
```

**代理检测流程**：
1. 先尝试直接推送
2. 若失败，自动扫描常见代理端口（7890, 1080, 10808, 10809, 8080, 33210）
3. 找到可用端口后自动设置代理并重试
4. 推送成功后自动清理代理配置

## 文档同步检查

提交前必须检查代码变更涉及的文档是否需要同步更新，详见 [doc-manager](../doc-manager/SKILL.md) 技能：

1. 根据代码变更识别影响范围
2. 逐一检查相关文档是否与代码一致
3. 未同步 → 先更新文档再提交；已同步 → 直接提交

## .git/info/exclude 配置说明

本项目使用 `.git/info/exclude`（而非 .gitignore）排除以下本地内容：

| 排除项 | 原因 |
|--------|------|
| `docs/` | 项目本地文档（架构、API、数据库设计等），仅本地使用 |
| `.trae/` | Trae 规则、技能、智能体配置、任务持久化等 |
| `secrets/` | 敏感配置（API 密钥、数据库密码等） |
| `README.md` | 根目录说明文档 |

**重要**：这些文件在本地完整保留，编辑器可正常检索，但不会被 git 跟踪。详细规则见 [docs/guides/git-exclude-rules.md](../../docs/guides/git-exclude-rules.md)。

## 特殊指令

### `/git` — 快速提交

完整执行上述五步工作流，并在提交前进行文档同步检查。

### `/git status` — 仅查看状态

只执行第一步，不提交。

### `/git log` — 查看提交历史

```powershell
git log --oneline -20
```

## 安全红线

- **永远不要**提交 `.env`、密钥、Token 等敏感信息
- **永远不要**使用 `git push --force` 到 main/develop 分支
- **永远不要**在未检查 diff 的情况下提交
- **永远不要**使用 `git add -A` 或 `git add .`
