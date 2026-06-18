package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyuan.entity.PoetSuggestion;
import com.moyuan.mapper.PoetSuggestionMapper;
import com.moyuan.service.PoetSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PoetSuggestionServiceImpl implements PoetSuggestionService {

    private final PoetSuggestionMapper poetSuggestionMapper;

    @Override
    @Transactional
    public void submitSuggestion(PoetSuggestion suggestion) {
        suggestion.setStatus("pending");
        if (suggestion.getCategory() == null || suggestion.getCategory().isEmpty()) {
            suggestion.setCategory("other");
        }
        suggestion.setCreateTime(LocalDateTime.now());
        suggestion.setUpdateTime(LocalDateTime.now());
        poetSuggestionMapper.insert(suggestion);
    }

    @Override
    public IPage<PoetSuggestion> getSuggestionList(int pageNum, int pageSize, String status) {
        LambdaQueryWrapper<PoetSuggestion> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PoetSuggestion::getStatus, status);
        }
        wrapper.orderByDesc(PoetSuggestion::getCreateTime);
        return poetSuggestionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional
    public void reviewSuggestion(Long id, String status, String reviewComment, Long reviewerId) {
        PoetSuggestion suggestion = poetSuggestionMapper.selectById(id);
        if (suggestion == null) {
            throw new RuntimeException("修正意见不存在");
        }
        suggestion.setStatus(status);
        suggestion.setReviewComment(reviewComment);
        suggestion.setReviewerId(reviewerId);
        suggestion.setReviewTime(LocalDateTime.now());
        suggestion.setUpdateTime(LocalDateTime.now());
        poetSuggestionMapper.updateById(suggestion);
    }
}
