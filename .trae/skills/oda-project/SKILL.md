***

name: "oda-project"
description: "Software-ODA125 项目知识库，包含项目结构、技术栈、核心功能、API文档、数据库设计等完整信息。当询问项目架构、功能模块、技术细节、业务逻辑，查找文件时使用。"
-----------------------------------------------------------------------------------------------------

# oda-project

## 项目概览

- **技术栈**: Spring Boot 3.3.5 + MyBatis Plus 3.5.9 + Vue 3 + Vite + Element Plus + PyTorch
- **核心功能**: 糖尿病风险预测、在线训练（新建/增量）、健康档案管理、智能对话、群体分析、诊疗记录管理
- **数据资产**: 详见 [data-assets](data-assets/SKILL.md) 技能

## 文档结构

```
docs/
├── architecture/               # 架构与概览
│   ├── overview.md             # 项目概览、技术栈、目录结构
│   ├── backend-modules.md      # 后端分层架构
│   └── frontend-overview.md    # 前端入口、配置、页面组件清单
├── api/                        # API文档
│   ├── endpoints.md            # 核心API端点清单
│   ├── auth.md                 # JWT认证机制
│   └── unified-response.md     # 统一响应格式
├── database/                   # 数据库
│   ├── schema.md               # 表清单、字段说明、实体映射
│   ├── migrations.md           # 迁移脚本及变更历史
│   ├── initialization.md       # 数据库初始化步骤、手动建表说明
│   └── known-issues.md         # 技术债务
├── frontend/                   # 前端深度文档
│   ├── routing.md              # 动态路由机制
│   ├── state-management.md     # Pinia状态管理
│   ├── composables.md          # Composables详解
│   ├── http-client.md          # HTTP客户端封装
│   ├── ui-components.md        # 公共组件
│   └── known-issues.md         # 前端技术债务
├── python/                     # Python算法层
│   ├── modules.md              # 文件清单、各脚本功能
│   ├── model-architecture.md   # PyTorch模型结构
│   ├── training.md             # 在线训练（train.py / incremental_train.py）
│   ├── predictions.md          # 预测功能
│   ├── ai-chatbot.md           # 糖尿病聊天机器人
│   └── data-files.md           # Python数据文件（训练数据集、similar_cases.npy等）
├── business/                   # 核心业务功能
│   ├── user-system.md          # 用户系统、RBAC
│   ├── ai-chat.md              # 智能对话
│   ├── prediction.md           # 预测工作台
│   ├── online-training.md      # 在线训练（新建模型/增量训练）
│   ├── health-profile.md       # 健康档案
│   ├── treatment-record.md     # 诊疗档案
│   ├── group-analysis.md       # 群体分析
│   ├── individual-insight.md   # 个体洞察
│   ├── data-preprocess.md      # 数据预处理
│   ├── repair-workflow.md      # 维修工单流程
│   ├── predict-results.md      # 预测结果管理
│   └── data-flow.md            # 数据流转说明（训练→模型→预测→AI问答）
├── data-assets/                # 数据资产专题（详见 data-assets 技能）
│   ├── overview.md             # 运行时数据目录总览
│   ├── model-files.md          # 模型文件存储
│   ├── database-assets.md      # 数据库预置数据
│   └── notes.md                # 数据相关注意事项
├── standards/                  # 开发规范（project_rules.md 引用）
│   ├── 开发规范.md             # 通用开发规范
│   ├── 开发注意事项.md         # 开发注意事项
│   ├── 前端开发规范.md         # 前端编码规范
│   ├── 后端开发规范.md         # 后端编码规范
│   └── 技术栈限制.md           # 技术栈使用限制
└── guides/                     # 指南与常见问题
    ├── startup.md              # 启动指南
    ├── configuration.md        # API Key、application.yml配置
    ├── dependencies.md         # Maven/Python/前端依赖清单
    ├── best-practices.md       # 文件上传限制、前端运行时配置
    ├── gitignore.md            # .gitignore排除规则
    └── technical-debt.md       # 技术债务汇总
```

## 快速参考

### 核心目录

- **后端代码**: `src/main/java/com/oda/springboot/`
- **前端代码**: `vue/src/`
- **Python算法**: `python/`
- **数据库脚本**: `sql/`

### 关键配置

- **后端配置**: `src/main/resources/application.yml`
- **前端运行时配置**: `vue/public/config.js`（导出 serverIp）
- **前端构建配置**: `vue/vite.config.js`
- **依赖管理**: `pom.xml`（后端）, `package.json`（前端）, `requirements.txt`（Python）

### 核心模块

- **用户系统**: RBAC权限控制，JWT认证
- **预测系统**: PyTorch模型，SHAP解释
- **在线训练**: 新建模型（train.py）、增量训练（incremental\_train.py）、AUC/混淆矩阵评估
- **健康档案**: 草稿持久化，DPF计算器
- **智能对话**: 多模型支持，流式响应

### 开发规范

- 后端遵循阿里巴巴Java开发手册
- 前端使用Composition API
- 数据库使用MyBatis Plus
- 代码注释使用中文
- 详细规范见 `docs/standards/`

## 使用场景

1. **项目结构查询**: 了解目录结构和文件位置
2. **技术栈咨询**: 查询使用的技术和框架
3. **功能模块说明**: 了解各功能模块的实现
4. **API接口查询**: 查看可用的API端点
5. **数据库设计**: 了解表结构和字段
6. **开发规范**: 获取编码规范和最佳实践
7. **数据资产**: 查询数据文件、模型存储等，参见 data-assets 技能

