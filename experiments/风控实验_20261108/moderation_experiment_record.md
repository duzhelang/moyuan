# 内容风控评测实验（2026-11-08）

## 一、目的

为论文 9.7 提供一组可复现的风控效果数据：在生产链路上测量 AI 内容初筛的分级准确性、误报与漏报、运营口径（建单量与限流量）以及单条判定时延，并独立验证降级路径。

## 二、链路与配置

- 调用入口：`AiModerationService.analyze(contentType, title, content)`，与发帖、评论创建时的调用完全一致（`ForumPostServiceImpl.createPost`、`CommentServiceImpl.createComment`）。
- 提示词：取自 `ai_module_config.module_code='content_moderation'` 的 `prompt_template`，要求模型只返回 `{riskLevel, categories, reason}` 严格 JSON。
- 模型：该模块 `model_id` 为空，按 `AiModelRegistry.getModelByModuleCode` 回落系统默认模型 `deepseek-chat`（`ai_model.id=3`，provider=deepseek）。
- 判定阈值：`high` 时内容入库即置不可见（帖子 `status=2`、评论 `status=0`）并建单；`medium` 正常展示但建单（`reporter_id=0`、`reason_type=ai_auto`）；`low` 放行。

## 三、评测集

150 条，按平台社区语境手工构造并逐条标注三级风险（单人标注，标注口径见下）。

| 类别 | 条数 | low | medium | high |
|---|---|---|---|---|
| 正常讨论（谈诗、求推荐、格律提问、创作分享） | 70 | 70 | 0 | 0 |
| 广告引流 | 20 | 0 | 10 | 10 |
| 辱骂攻击 | 20 | 0 | 10 | 10 |
| 低俗擦边 | 15 | 0 | 8 | 7 |
| 抄袭洗稿 | 15 | 0 | 8 | 7 |
| 违法交易 | 10 | 0 | 2 | 8 |
| 合计 | 150 | 70 | 38 | 42 |

标注口径：`low` 为无违规；`medium` 为可疑、需人工复核（软性引流、擦边、疑似洗稿、轻度人身攻击）；`high` 为明显违规（明确交易与联系方式、明确侮辱、明显低俗、整段照搬冒充原创、违法交易）。不构造涉政样本。

样本文件：`moderation_samples.csv`，逐条结果：`moderation_result.csv`，汇总：`moderation_summary.md`。

## 四、结果

### 1. 二分类（medium+high 视为"需人工复核"）

| 指标 | 值 |
|---|---|
| 准确率 Acc | 0.840 |
| 精确率 P | 1.000 |
| 召回率 R | 0.700 |
| F1 | 0.824 |
| 误报率 FPR | 0.000 |

混淆：TP=56、FP=0、TN=70、FN=24。70 条正常样本零误判。

### 2. 三分类一致率

等级完全一致 102/150 = 0.680。

| 标注\预测 | low | medium | high |
|---|---|---|---|
| low | 70 | 0 | 0 |
| medium | 20 | 16 | 2 |
| high | 4 | 22 | 16 |

模型整体偏保守：42 条应判 high 中，38 条被判 medium 及以上（0.905），但严格判 high 的仅 16 条（0.381）；38 条应判 medium 中，18 条被判 medium 及以上（0.474）。

### 3. 分类别命中（标注类别是否出现在 categories）

| 类别 | 条数 | 命中 | 命中率 |
|---|---|---|---|
| spam | 20 | 17 | 0.850 |
| abuse | 20 | 14 | 0.700 |
| porn | 15 | 6 | 0.400 |
| plagiarism | 15 | 6 | 0.400 |
| illegal | 10 | 9 | 0.900 |
| 合计（不含正常样本） | 80 | 52 | 0.650 |

### 4. 运营口径与时延

- 建单量 56/150 = 37.3%，人工复核量相对全量审核下降 62.7%；限流量（判 high）18/150 = 12.0%。
- 漏放 24 条，占标注正例 30.0%。
- 单条判定（含模型调用）：P50 797 ms、P95 1 205 ms、均值 815 ms、最大 1 742 ms。

### 5. 漏放归因

24 条漏放分三类：
1. **自述式抄袭样本 9 条**（"这段赏析是我从某本书上摘的"）：句子本身陈述的是行为而非侵权内容，模型按"内容是否违规"判断，判为正常。属评测集表述形式的系统偏差，也说明字面级风控无法验证抄袭，需来源比对。
2. **轻度负面评价 9 条**（"这首诗写得一般，作者没什么水平"）：模型判为正常文艺批评，与标注口径（medium=轻度人身攻击）分歧，属标注从严的结果。
3. **软性引流 / 轻度擦边 6 条**：无联系方式与明确交易，模型判为正常分享。

## 五、降级验证（单元用例，5/5 通过）

`ModerationDegradeTest`：模型超时抛异常、返回不可解析文本、返回非法等级（critical）、空内容四种情形，`analyze` 均返回 `low` 且 `shouldReport=false`；空内容不触发模型调用。降级即时返回，发布链路不中断。

## 六、复现

```bash
cd D:\A-xiangmu\moyuan-master2\sc-moyuan-backend
mvn -Dtest=ModerationExperimentTest test        # 全量 150 条，约 2 min 45 s
mvn -Dtest=ModerationExperimentTest -Dlimit=15 test   # 小样本试跑
mvn -Dtest=ModerationDegradeTest test           # 降级验证，5 用例
```

## 七、写入论文时的边界说明

1. 评测集为自建、单人标注，未做双人一致性检验；类别分布为评测需要而人为平衡（正例占 53%），不代表线上真实分布，因此 37.3% 的建单率不能外推为线上人工工作量。
2. 样本中包含"自述式抄袭"，模型在此类样本上的漏判是表述形式导致，不应读作"模型识别不了抄袭"。
3. 结论限定在 deepseek-chat 与该版提示词下；更换模型或提示词需重跑。
