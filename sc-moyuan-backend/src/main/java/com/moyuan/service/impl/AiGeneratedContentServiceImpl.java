package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.ai.AiModelRegistry;
import com.moyuan.common.ResultCode;
import com.moyuan.entity.AiGeneratedContent;
import com.moyuan.entity.Dynasty;
import com.moyuan.entity.Poem;
import com.moyuan.entity.Poet;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.AiGeneratedContentMapper;
import com.moyuan.mapper.DynastyMapper;
import com.moyuan.mapper.PoemMapper;
import com.moyuan.mapper.PoetMapper;
import com.moyuan.service.AiGeneratedContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGeneratedContentServiceImpl extends ServiceImpl<AiGeneratedContentMapper, AiGeneratedContent>
        implements AiGeneratedContentService {

    private final AiGeneratedContentMapper aiGeneratedContentMapper;
    private final PoemMapper poemMapper;
    private final PoetMapper poetMapper;
    private final DynastyMapper dynastyMapper;
    private final AiModelRegistry aiModelRegistry;

    /** 诗词类可填充字段集合 */
    private static final Set<String> POEM_FIELDS = Set.of("translation", "appreciation", "background");

    /** 诗人类可填充字段集合 */
    private static final Set<String> POET_FIELDS = Set.of("biography", "life_story", "influence", "evaluation", "anecdotes");

    @Override
    public String generatePreview(String targetType, Long targetId, String fieldName, String moduleCode) {
        validateField(targetType, fieldName);

        String prompt;
        String systemPrompt;

        if ("poem".equals(targetType)) {
            Poem poem = poemMapper.selectById(targetId);
            if (poem == null) {
                throw new BusinessException(ResultCode.POEM_NOT_FOUND);
            }
            String poetName = "";
            String dynastyName = "";
            if (poem.getPoetId() != null) {
                Poet poet = poetMapper.selectById(poem.getPoetId());
                if (poet != null) {
                    poetName = poet.getName();
                    if (poet.getDynastyId() != null) {
                        Dynasty dynasty = dynastyMapper.selectById(poet.getDynastyId());
                        if (dynasty != null) {
                            dynastyName = dynasty.getName();
                        }
                    }
                }
            }
            systemPrompt = "你是中国古典诗词鉴赏专家，擅长精准、优美地解读古诗词。请严格按照要求生成内容，直接输出内容本身，不要包含任何前缀说明。";
            prompt = buildPoemPrompt(fieldName, poem.getTitle(), poetName, dynastyName, poem.getContent());
        } else {
            Poet poet = poetMapper.selectById(targetId);
            if (poet == null) {
                throw new BusinessException(ResultCode.POET_NOT_FOUND);
            }
            String dynastyName = "";
            if (poet.getDynastyId() != null) {
                Dynasty dynasty = dynastyMapper.selectById(poet.getDynastyId());
                if (dynasty != null) {
                    dynastyName = dynasty.getName();
                }
            }
            systemPrompt = "你是中国古典文学研究专家，擅长准确、全面地介绍古代诗人。请严格按照要求生成内容，直接输出内容本身，不要包含任何前缀说明。";
            prompt = buildPoetPrompt(fieldName, poet.getName(), dynastyName, moduleCode);
        }

        log.info("调用AI生成预览: targetType={}, targetId={}, fieldName={}, moduleCode={}", targetType, targetId, fieldName, moduleCode);
        var model = aiModelRegistry.getModelForModule(moduleCode);
        var adapter = aiModelRegistry.getAdapter(model.getProvider());
        return adapter.chat(prompt, model, systemPrompt);
    }

    @Override
    @Transactional
    public AiGeneratedContent submitForReview(String targetType, Long targetId, String fieldName, String content) {
        validateField(targetType, fieldName);

        LambdaQueryWrapper<AiGeneratedContent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiGeneratedContent::getTargetType, targetType)
               .eq(AiGeneratedContent::getTargetId, targetId)
               .eq(AiGeneratedContent::getFieldName, fieldName)
               .eq(AiGeneratedContent::getStatus, 0)
               .last("LIMIT 1");
        AiGeneratedContent existing = aiGeneratedContentMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setContent(content);
            aiGeneratedContentMapper.updateById(existing);
            log.info("更新已有待审核记录: id={}", existing.getId());
            return existing;
        }

        String targetName = "";
        if ("poem".equals(targetType)) {
            Poem poem = poemMapper.selectById(targetId);
            if (poem != null) targetName = poem.getTitle();
        } else {
            Poet poet = poetMapper.selectById(targetId);
            if (poet != null) targetName = poet.getName();
        }

        AiGeneratedContent record = new AiGeneratedContent();
        record.setTargetType(targetType);
        record.setTargetId(targetId);
        record.setTargetName(targetName);
        record.setFieldName(fieldName);
        record.setContent(content);
        record.setAiModel("default");
        record.setStatus(0);
        aiGeneratedContentMapper.insert(record);

        log.info("AI内容已提交审核: id={}", record.getId());
        return record;
    }

    @Override
    @Transactional
    public AiGeneratedContent generateContent(String targetType, Long targetId, String fieldName, String moduleCode) {
        validateField(targetType, fieldName);

        LambdaQueryWrapper<AiGeneratedContent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiGeneratedContent::getTargetType, targetType)
               .eq(AiGeneratedContent::getTargetId, targetId)
               .eq(AiGeneratedContent::getFieldName, fieldName)
               .eq(AiGeneratedContent::getStatus, 0)
               .last("LIMIT 1");
        AiGeneratedContent existing = aiGeneratedContentMapper.selectOne(wrapper);
        if (existing != null) {
            log.info("该字段已存在待审核记录，直接返回: targetType={}, targetId={}, fieldName={}", targetType, targetId, fieldName);
            return existing;
        }

        String prompt;
        String systemPrompt;
        String targetName;

        if ("poem".equals(targetType)) {
            Poem poem = poemMapper.selectById(targetId);
            if (poem == null) {
                throw new BusinessException(ResultCode.POEM_NOT_FOUND);
            }
            targetName = poem.getTitle();
            String poetName = "";
            String dynastyName = "";
            if (poem.getPoetId() != null) {
                Poet poet = poetMapper.selectById(poem.getPoetId());
                if (poet != null) {
                    poetName = poet.getName();
                    if (poet.getDynastyId() != null) {
                        Dynasty dynasty = dynastyMapper.selectById(poet.getDynastyId());
                        if (dynasty != null) {
                            dynastyName = dynasty.getName();
                        }
                    }
                }
            }
            systemPrompt = "你是中国古典诗词鉴赏专家，擅长精准、优美地解读古诗词。请严格按照要求生成内容，直接输出内容本身，不要包含任何前缀说明。";
            prompt = buildPoemPrompt(fieldName, poem.getTitle(), poetName, dynastyName, poem.getContent());
        } else {
            Poet poet = poetMapper.selectById(targetId);
            if (poet == null) {
                throw new BusinessException(ResultCode.POET_NOT_FOUND);
            }
            targetName = poet.getName();
            String dynastyName = "";
            if (poet.getDynastyId() != null) {
                Dynasty dynasty = dynastyMapper.selectById(poet.getDynastyId());
                if (dynasty != null) {
                    dynastyName = dynasty.getName();
                }
            }
            systemPrompt = "你是中国古典文学研究专家，擅长准确、全面地介绍古代诗人。请严格按照要求生成内容，直接输出内容本身，不要包含任何前缀说明。";
            prompt = buildPoetPrompt(fieldName, poet.getName(), dynastyName, moduleCode);
        }

        log.info("调用AI生成内容: targetType={}, targetId={}, fieldName={}, moduleCode={}", targetType, targetId, fieldName, moduleCode);
        var model = aiModelRegistry.getModelForModule(moduleCode);
        var adapter = aiModelRegistry.getAdapter(model.getProvider());
        String aiContent = adapter.chat(prompt, model, systemPrompt);

        // 5. 保存到审核表
        AiGeneratedContent record = new AiGeneratedContent();
        record.setTargetType(targetType);
        record.setTargetId(targetId);
        record.setTargetName(targetName);
        record.setFieldName(fieldName);
        record.setContent(aiContent);
        record.setAiModel("default");
        record.setStatus(0);
        aiGeneratedContentMapper.insert(record);

        log.info("AI内容生成完成并已存入审核表: id={}", record.getId());
        return record;
    }

    @Override
    public IPage<AiGeneratedContent> listByStatus(Integer status, int page, int size) {
        LambdaQueryWrapper<AiGeneratedContent> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(AiGeneratedContent::getStatus, status);
        }
        wrapper.orderByDesc(AiGeneratedContent::getCreateTime);
        return aiGeneratedContentMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void approve(Long id, Long reviewerId, String reviewComment) {
        AiGeneratedContent record = aiGeneratedContentMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("审核记录不存在");
        }
        if (record.getStatus() != 0) {
            throw new BusinessException("该记录已审核，无法重复操作");
        }

        // 更新审核记录状态
        record.setStatus(1);
        record.setReviewerId(reviewerId);
        record.setReviewComment(reviewComment);
        record.setReviewTime(LocalDateTime.now());
        aiGeneratedContentMapper.updateById(record);

        // 写入原表
        if ("poem".equals(record.getTargetType())) {
            writeContentToPoem(record);
        } else if ("poet".equals(record.getTargetType())) {
            writeContentToPoet(record);
        }

        log.info("AI内容审核通过并已写入原表: id={}, targetType={}, targetId={}, fieldName={}",
                id, record.getTargetType(), record.getTargetId(), record.getFieldName());
    }

    @Override
    public void reject(Long id, Long reviewerId, String reviewComment) {
        AiGeneratedContent record = aiGeneratedContentMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("审核记录不存在");
        }
        if (record.getStatus() != 0) {
            throw new BusinessException("该记录已审核，无法重复操作");
        }

        record.setStatus(2);
        record.setReviewerId(reviewerId);
        record.setReviewComment(reviewComment);
        record.setReviewTime(LocalDateTime.now());
        aiGeneratedContentMapper.updateById(record);

        log.info("AI内容审核已拒绝: id={}", id);
    }

    // ========== 私有方法 ==========

    private void validateField(String targetType, String fieldName) {
        if ("poem".equals(targetType)) {
            if (!POEM_FIELDS.contains(fieldName)) {
                throw new BusinessException("无效的诗词字段: " + fieldName);
            }
        } else if ("poet".equals(targetType)) {
            if (!POET_FIELDS.contains(fieldName)) {
                throw new BusinessException("无效的诗人字段: " + fieldName);
            }
        } else {
            throw new BusinessException("无效的目标类型: " + targetType);
        }
    }

    private String buildPoemPrompt(String fieldName, String title, String poetName, String dynastyName, String content) {
        String poemInfo = String.format("标题：%s\n作者：%s\n朝代：%s\n原文：\n%s",
                title, poetName, dynastyName, content);
        return switch (fieldName) {
            case "translation" -> "请为以下古诗词提供精准的白话文翻译，要求译文优美、忠于原意、保留诗词韵味。\n" + poemInfo;
            case "appreciation" -> "请对以下古诗词进行深入赏析，包括意境分析、修辞手法、关键词句赏析、情感表达等方面，语言优美、引经据典。\n" + poemInfo;
            case "background" -> "请详细介绍以下古诗词的创作背景，包括诗人当时的处境、创作缘由、历史背景等。\n" + poemInfo;
            default -> throw new BusinessException("无效的诗词字段: " + fieldName);
        };
    }

    private String buildPoetPrompt(String fieldName, String poetName, String dynastyName, String moduleCode) {
        String poetInfo = poetName + "（" + dynastyName + "）";
        int maxLength = 500;
        if (moduleCode != null) {
            var config = aiModelRegistry.getModuleConfig(moduleCode);
            if (config != null && config.getMaxLength() != null && config.getMaxLength() > 0) {
                maxLength = config.getMaxLength();
            }
        }
        String lengthHint = "，不超过" + maxLength + "字";
        return switch (fieldName) {
            case "biography" -> "请为诗人" + poetInfo + "撰写一段简洁的生平简介" + lengthHint + "，突出主要成就和历史地位。";
            case "life_story" -> "请详细介绍诗人" + poetInfo + "的生平经历" + lengthHint + "，包括重要人生阶段、仕途经历、重要事件等。";
            case "influence" -> "请分析诗人" + poetInfo + "的主要影响" + lengthHint + "，包括对当时文坛和后世文学的影响。";
            case "evaluation" -> "请详细介绍历代文人和学者对诗人" + poetInfo + "的评价" + lengthHint + "，包括：1.同时代名人对其评价 2.后世文学批评家的经典评述 3.历代典籍中的相关记载 4.正反两方面的评价观点。要求引用具体文献出处，内容详实有据。";
            case "anecdotes" -> "请讲述诗人" + poetInfo + "的几个著名轶事典故" + lengthHint + "，展现其性格特点和人生智慧。";
            default -> throw new BusinessException("无效的诗人字段: " + fieldName);
        };
    }

    private void writeContentToPoem(AiGeneratedContent record) {
        Poem poem = poemMapper.selectById(record.getTargetId());
        if (poem == null) {
            log.warn("审核通过写入原表失败：诗词不存在, poemId={}", record.getTargetId());
            return;
        }
        switch (record.getFieldName()) {
            case "translation" -> poem.setTranslation(record.getContent());
            case "appreciation" -> poem.setAppreciation(record.getContent());
            case "background" -> poem.setBackground(record.getContent());
            default -> log.warn("无效的诗词字段: {}", record.getFieldName());
        }
        poemMapper.updateById(poem);
    }

    private void writeContentToPoet(AiGeneratedContent record) {
        Poet poet = poetMapper.selectById(record.getTargetId());
        if (poet == null) {
            log.warn("审核通过写入原表失败：诗人不存在, poetId={}", record.getTargetId());
            return;
        }
        switch (record.getFieldName()) {
            case "biography" -> poet.setBiography(record.getContent());
            case "life_story" -> poet.setLifeStory(record.getContent());
            case "influence" -> poet.setInfluence(record.getContent());
            case "evaluation" -> poet.setEvaluation(record.getContent());
            case "anecdotes" -> poet.setAnecdotes(record.getContent());
            default -> log.warn("无效的诗人字段: {}", record.getFieldName());
        }
        poetMapper.updateById(poet);
    }
}
