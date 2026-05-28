---
alwaysApply: false
description: 需要调用智能体时生效
---
# 项目智能体配置

| 智能体 | 代号 | 配置路径 | 职责 |
|--------|------|----------|------|
| **分析规划师** | super-brain | `.trae/agents/analysis-planner/AGENT.md` | 分析用户需求，制定任务计划，协调各智能体 |
| **代码开发** | developer | `.trae/agents/developer/AGENT.md` | 涉及代码编写时调用，先熟读开发规范 |
| **代码审查** | code-reviewer | `.trae/agents/code-reviewer/AGENT.md` | 依据开发规范检查代码是否符合规范 |
| **文档管理** | doc-manager | `.trae/agents/doc-manager/AGENT.md` | 检查 `docs` 目录下的文档，确认并更新需要变更的文档 |
| **后端架构** | backend-architect | `.trae/agents/backend-architect/AGENT.md` | 设计API、构建服务器逻辑、数据库设计 |
| **API测试** | api-test-pro | `.trae/agents/api-test-pro/AGENT.md` | API接口测试、性能测试、负载测试 |
| **前端测试** | frontend-test | `.trae/agents/frontend-test/AGENT.md` | Vue组件测试、页面测试、交互测试 |
| **UI设计师** | ui-designer | `.trae/agents/ui-designer/AGENT.md` | 创建用户界面、设计组件、改善视觉美学 |
| **本地测试** | local-fullstack-tester | `.trae/agents/local-fullstack-tester/AGENT.md` | 本地前后端服务联通测试，测试前必须读取 `.trae/test-env.yml` |
