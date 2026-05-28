---
alwaysApply: false
description: 特定任务才用规则 - 任务相关规则，技术栈与代码规范，文档-变更，Git 提交时生效
---
# Software-ODA125 项目规则 - 特定任务才用

> 本文件包含 Software-ODA125 项目特定任务类型才需要应用的规则。

## 本地测试智能体规范

### 测试前必须执行的步骤

调用 `local-fullstack-tester` 智能体前，**必须先读取 `.trae/test-env.yml`** 获取以下信息：

| 信息类别 | 说明 |
|----------|------|
| **前后端端口** | 前端 8080、后端 9090 |
| **测试账号** | admin/user 的用户名和密码 |
| **登录接口** | `/user/login`（POST） |
| **Token 传递** | 请求头 `token` 字段 |
| **代理配置** | `/api` → `http://localhost:9090` |

### 测试流程

1. 读取 `.trae/test-env.yml` 获取测试环境配置
2. 检查前后端服务端口是否可达
3. 使用测试账号调用登录接口获取 token
4. 携带 token 测试业务接口
5. 输出测试报告

---

## Git 提交前文档同步检查

每次执行 git 提交前，必须先检查本次变更涉及的文档是否已同步更新。

### 检查流程
1. **识别影响范围**：根据本次代码变更内容，判断涉及哪些文档（详见下方文档-变更映射表）
2. **检查文档状态**：逐一检查相关文档内容是否与代码变更一致
3. **决策处理**：
   - **已同步** → 直接提交
   - **未同步** → 自动更新文档后再一并提交

### 文档-变更映射表

> 详见 [doc-change-mapping.md](doc-change-mapping.md)

### 注意事项
- 文档更新使用 `[文档]` 类型前缀单独提交
- 若 `docs/` 在 `.gitignore` 中，文档更新仅保存在本地，不推送到远程
- README.md 始终参与提交（不在 .gitignore 中）

---

## 技术栈与代码规范

- 技术栈限制详见 `docs/standards/技术栈限制.md`
- 后端代码规范详见 `docs/standards/后端开发规范.md`
- 前端代码规范详见 `docs/standards/前端开发规范.md`
- 开发注意事项详见 `docs/standards/开发注意事项.md`

---

## 前端布局约束与弹窗规范

### 主布局限制

`Manage.vue` 为主布局容器，存在以下影响弹窗定位的关键属性：

| 选择器 | 属性 | 副作用 |
|--------|------|--------|
| `.manage-wrapper` | `overflow: hidden` | 创建新的定位上下文，`position: fixed` 子元素相对此容器而非视口 |
| `.manage-main` | `backdrop-filter: blur(8px)` | 创建新的层叠上下文，影响 z-index 和 fixed 定位 |


> 详细说明见 `ui-design-system` 技能 §2.5.1