package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyuan.entity.Poet;
import com.moyuan.entity.PoetDraft;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.PoetDraftMapper;
import com.moyuan.mapper.PoetMapper;
import com.moyuan.service.PoetDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PoetDraftServiceImpl implements PoetDraftService {

    private final PoetDraftMapper poetDraftMapper;
    private final PoetMapper poetMapper;

    @Override
    @Transactional
    public void saveDraft(PoetDraft draft) {
        Poet poet = poetMapper.selectById(draft.getPoetId());
        if (poet == null) {
            throw new BusinessException("诗人不存在");
        }

        String field = sectionToField(draft.getSection());
        String originalContent = getOriginalContent(poet, field);
        draft.setOriginalContent(originalContent);
        draft.setStatus(0);
        draft.setCreateTime(LocalDateTime.now());
        draft.setUpdateTime(LocalDateTime.now());
        poetDraftMapper.insert(draft);
    }

    @Override
    public IPage<PoetDraft> getDraftList(int pageNum, int pageSize, Long poetId, Integer status) {
        LambdaQueryWrapper<PoetDraft> wrapper = new LambdaQueryWrapper<>();
        if (poetId != null) {
            wrapper.eq(PoetDraft::getPoetId, poetId);
        }
        if (status != null) {
            wrapper.eq(PoetDraft::getStatus, status);
        }
        wrapper.orderByDesc(PoetDraft::getCreateTime);
        return poetDraftMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional
    public void reviewDraft(Long id, Integer status, String reviewComment, Long reviewerId) {
        PoetDraft draft = poetDraftMapper.selectById(id);
        if (draft == null) {
            throw new BusinessException("草稿不存在");
        }
        draft.setStatus(status);
        draft.setReviewComment(reviewComment);
        draft.setReviewerId(reviewerId);
        draft.setReviewTime(LocalDateTime.now());
        draft.setUpdateTime(LocalDateTime.now());
        poetDraftMapper.updateById(draft);

        if (status == 1) {
            publishDraft(draft);
        }
    }

    @Override
    public PoetDraft getDraftById(Long id) {
        return poetDraftMapper.selectById(id);
    }

    @Override
    public long countByStatus(Integer status) {
        LambdaQueryWrapper<PoetDraft> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(PoetDraft::getStatus, status);
        }
        return poetDraftMapper.selectCount(wrapper);
    }

    private void publishDraft(PoetDraft draft) {
        Poet poet = poetMapper.selectById(draft.getPoetId());
        if (poet == null) return;

        String field = draft.getSection();
        switch (field) {
            case "biography" -> poet.setBiography(draft.getContent());
            case "life_story" -> poet.setLifeStory(draft.getContent());
            case "influence" -> poet.setInfluence(draft.getContent());
            case "evaluation" -> poet.setEvaluation(draft.getContent());
            case "anecdotes" -> poet.setAnecdotes(draft.getContent());
        }
        poet.setUpdateTime(LocalDateTime.now());
        poetMapper.updateById(poet);
    }

    private String sectionToField(String section) {
        return switch (section) {
            case "biography" -> "biography";
            case "life_story" -> "lifeStory";
            case "influence" -> "influence";
            case "evaluation" -> "evaluation";
            case "anecdotes" -> "anecdotes";
            default -> throw new BusinessException("无效的板块");
        };
    }

    private String getOriginalContent(Poet poet, String field) {
        return switch (field) {
            case "biography" -> poet.getBiography();
            case "lifeStory" -> poet.getLifeStory();
            case "influence" -> poet.getInfluence();
            case "evaluation" -> poet.getEvaluation();
            case "anecdotes" -> poet.getAnecdotes();
            default -> "";
        };
    }
}