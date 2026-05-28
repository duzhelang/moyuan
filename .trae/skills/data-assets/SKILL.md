***

name: "data-assets"
description: "Software-ODA125 项目的数据资产知识库。包含所有数据文件、数据库表数据、文件目录结构、训练数据来源、模型文件位置等完整信息。当查找询问数据文件位置、训练集内容、模型存储、数据归档、目录结构时使用。"
------------------------------------------------------------------------------------------------------------------------

# data-assets

## 文档索引

### 数据资产专题

- **[data-assets/overview.md](../../docs/data-assets/overview.md)**: 运行时数据目录总览（data/, logs/, files/）
- **[data-assets/model-files.md](../../docs/data-assets/model-files.md)**: 模型文件存储（.pth, .pkl, .npy）
- **[data-assets/training-data.md](../../docs/data-assets/training-data.md)**: 训练数据集（python/data/train/）
- **[data-assets/database-assets.md](../../docs/data-assets/database-assets.md)**: 数据库预置数据（菜单、角色、预置统计）
- **[data-assets/notes.md](../../docs/data-assets/notes.md)**: 其他数据相关注意事项（目录自动创建等）

### 数据库

- **[database/schema.md](../../docs/database/schema.md)**: 表结构、字段、关系、实体映射
- **[database/migrations.md](../../docs/database/migrations.md)**: 迁移脚本及变更历史
- **[database/initialization.md](../../docs/database/initialization.md)**: 数据库初始化步骤、手动建表说明

### Python

- **[python/modules.md](../../docs/python/modules.md)**: Python脚本文件清单
- **[python/model-architecture.md](../../docs/python/model-architecture.md)**: 模型结构、8个特征
- **[python/training.md](../../docs/python/training.md)**: 在线训练（train.py/incremental\_train.py）、数据集、评估指标
- **[python/predictions.md](../../docs/python/predictions.md)**: 预测功能（单条/批量/SHAP）
- **[python/ai-chatbot.md](../../docs/python/ai-chatbot.md)**: 聊天机器人
- **[python/data-files.md](../../docs/python/data-files.md)**: Python数据文件（训练数据集、similar\_cases.npy等）

### 业务

- **[business/data-flow.md](../../docs/business/data-flow.md)**: 数据流转说明（训练→模型→预测→AI问答）
- **[business/online-training.md](../../docs/business/online-training.md)**: 在线训练功能（新建/增量训练）

### 指南

- **[guides/startup.md](../../docs/guides/startup.md)**: 启动顺序
- **[guides/configuration.md](../../docs/guides/configuration.md)**: API Key、application.yml关键配置
- **[guides/dependencies.md](../../docs/guides/dependencies.md)**: Maven/Python/前端依赖清单
- **[guides/best-practices.md](../../docs/guides/best-practices.md)**: 文件上传限制、前端运行时配置
- **[guides/gitignore.md](../../docs/guides/gitignore.md)**: .gitignore排除规则

## 关键数据表

| 表                      | 说明                                                              |
| ---------------------- | --------------------------------------------------------------- |
| sys\_user              | 用户表                                                             |
| sys\_role              | 角色表                                                             |
| sys\_menu              | 菜单表（32条，8大模块）                                                   |
| user\_health\_profiles | 健康档案                                                            |
| sys\_model\_version    | 模型版本（含 metrics JSON）                                            |
| sys\_train\_task       | 训练任务（含 accuracy/loss/precision/recall/f1/auc/confusion\_matrix） |
| sys\_result            | 预测结果（OnlineDate实体）                                              |
| sys\_treatment\_record | 诊疗档案（未在初始化SQL中定义）                                               |

## ⚠️ 已知问题

- `sys_treatment_record` 表未在初始化SQL中定义，需手动创建
- 实体类 `OnlineDate.java` 映射到 `sys_result` 表，语义不匹配
- 训练数据集文件（train\_10000.csv等）需手动放入 `python/data/` 目录

> 项目整体架构见 [oda-project](oda-project/SKILL.md) 技能
> 开发规范见 `docs/standards/`

