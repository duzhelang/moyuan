# 沙箱命令执行规范

> 本文档定义在 **Windows PowerShell 5 + trae-sandbox** 中执行命令的规范。
> **每次执行 `RunCommand` 前，必须查阅本文档。**

## 一、PowerShell 5 兼容性

### `&&` / `||` — 不支持的运算符

PowerShell 5 **不支持** `&&` 和 `||`（PowerShell 7+ 才支持）。

```powershell
# ❌ 错误 — 报错：标记"&&"不是此版本中的有效语句分隔符
cd frontend && npm run build
cd frontend || echo fail

# ✅ 正确 — 用 ; 分隔
cd frontend; npm run build

# ✅ 正确 — 用条件链
cd frontend; if ($?) { npm run build }
cd frontend; if (-not $?) { echo fail }
```

### 路径格式

```powershell
# ✅ 绝对路径直接用
cd D:\project\frontend; npm run build

# ✅ 带空格的路径用引号
cd "D:\project\my app"; npm run build
```

### 输入重定向 — `<` 不支持

PowerShell 5 **不支持** `<`（输入重定向运算符）。

```powershell
# ❌ 错误 — 报错：标记"<"不是此版本中的有效语句分隔符
mysql -u root -p < script.sql

# ✅ 正确 — 用 Get-Content + 管道
Get-Content script.sql | mysql -u root -p

# ✅ 或使用 cmd /c（仅限无空格路径）
cmd /c "mysql -u root -p < script.sql"
```

### 输出重定向

```powershell
# ✅ 合并 stderr 到 stdout
npm run build 2>&1

# ✅ 管道后截断
npx vue-tsc --noEmit 2>&1 | Select-Object -First 30
```

## 二、命令链式执行

| 场景 | 正确写法 |
|------|----------|
| 连续执行多条命令 | `cd path; cmd1; cmd2` |
| 换目录后执行 | `cd path; command` |
| 设置环境变量 | `$env:VAR='val'; command` |

## 三、RunCommand 参数选择

| 参数 | 值 | 适用场景 |
|------|----|----------|
| `blocking` | `true` | 编译/构建/测试/简单命令 |
| `blocking` | `false` | web 服务启动、长时间运行任务 |
| `requires_approval` | `false` | 读操作、构建命令 |
| `requires_approval` | `true` | 写文件、npm install、git 操作 |

## 四、输出截断

输出较长时必须截断，防止内容撑爆：

```powershell
# 截断前 N 行（推荐）
command 2>&1 | Select-Object -First 30

# 截断前 N 字符（避免超长行）
command 2>&1 | ForEach-Object { $_.Substring(0, [Math]::Min($_.Length, 500)) }

# 格式化宽输出
npm ls 2>&1 | Out-String -Width 200
```

### 输出过长时的分段读取

使用 `CheckCommandStatus` 配合 `skip_character_count` 分段读取：

1. 先取底部（最新的）输出：`output_priority: "bottom"`
2. 再取顶部（最旧的）输出：`output_priority: "top"`
3. 结合 `filter` 用正则筛选关键信息

## 五、常见错误速查

| 错误信息 | 原因 | 修复 |
|----------|------|------|
| `标记"&&"不是此版本中的有效语句分隔符` | 使用了 `&&` | 改用 `;` 或 `if ($?) {}` |
| `标记"<"不是此版本中的有效语句分隔符` | 使用了 `<` 输入重定向 | 改用 `Get-Content file \| command` |
| `无法将"xxx"项识别为 cmdlet、函数...` | 命令不存在或不在 PATH 中 | 用绝对路径或 npx |
| `拒绝访问` | 权限不足 | 不使用需管理员权限的命令 |
| `表达式或语句中包含意外的标记` | 引号未正确转义 | 检查引号匹配 |

## 六、错误诊断流程

1. **检查退出码**：`$LASTEXITCODE`（0=成功，非0=失败）
2. **检查输出末尾**：错误信息通常在最末尾
3. **筛选关键信息**：用 `filter` 正则匹配 `error`、`Error`、`fail` 等关键词

---

> 关联文件：
> - skill: `.trae/skills/sandbox-workflow/SKILL.md`
> - 触发规则: `.trae/rules/skills-trigger-rules.md`
