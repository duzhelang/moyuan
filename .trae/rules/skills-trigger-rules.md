---
alwaysApply: false
description: 需要使用技能时生效
---
# 项目特定技能触发规则

| 技能 | 触发条件 |
|------|----------|
| **moyuan-architecture** | 询问项目结构、功能模块、技术细节、业务逻辑时 |
| **moyuan-data-assets** | 询问数据文件位置、数据库表内容、数据流转、模型存储时 |
| **ui-design-system** | 新增页面、调整样式、统一视觉风格时 |
| **ui-implementer** | 根据设计规范实现具体UI组件时 |
| **analysis-planner** | 用户提出新需求、功能开发、问题排查、长任务、多步骤任务、复杂问题时，**必须优先触发**进行需求分析和任务分解 |
| **writing-plans** | 用户需要制定执行计划、拆解复杂任务、制定实施方案、规划多步骤工作流时，**必须触发**将规范或需求转化为详细的分步骤计划 |
| **executing-plans** | 存在书面执行计划需要执行时，**必须触发**，加载计划、批判性审查、执行所有任务、完成时报告 |
| **subagent-driven-development** | 执行具有独立子任务的实现计划时，**必须触发**，为每个任务调度子代理，配合两阶段审查 |
| **developer** | 涉及代码编写时调用，通用开发代理，要求先熟读开发规范 |
| **code-reviewer** | 代码编写完成后，**必须触发**依据规范检查代码合规性，不允许跳过 |
| **doc-manager** | 代码变更后，**必须触发**检查并更新 `docs` 目录下的相关文档 |
| **local-fullstack-tester** | 测试本地前后端服务联通性时，测试前自动读取 `.trae/agents/local-fullstack-tester/AGENT.md` 和 `.trae/test-env.yml` |
| **dispatching-parallel-agents** | 面对2个以上可独立处理的互不依赖的任务时，**必须触发**调度多个子代理并发工作 |
| **verification-before-completion** | 声称工作完成、修复或通过之前，**必须触发**运行验证命令确认结果 |
| **systematic-debugging** | 遇到bug、测试失败或意外行为时，**必须触发**进行四阶段根因调查后再提出修复 |

## 触发优先级
1. 用户明确指定技能 → 立即调用
2. 匹配触发条件 → **立即自动调用，不得跳过**
3. 多个技能匹配 → 按流程顺序依次调用
4. 无匹配技能 → 直接执行任务

## 强制触发规则
- 用户提出**任何多步骤/复杂需求**时，**必须**先触发 `analysis-planner` 或 `writing-plans`
- 任何代码编写完成后，**必须**触发 `code-reviewer`
- 任何代码变更完成后，**必须**触发 `doc-manager`
- 存在书面计划时，**必须**触发 `executing-plans` 或 `subagent-driven-development`
- 声称任务完成前，**必须**触发 `verification-before-completion`
- 遇到异常行为时，**必须**触发 `systematic-debugging`

## 禁止事项
- 不要为了调用技能而调用，必须与当前任务强相关
- 同一任务不要重复调用相同技能（但后续阶段可以再调用）
- 技能是辅助工具，任务完成才是目标
