package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.entity.PoetSuggestion;

public interface PoetSuggestionService {

    void submitSuggestion(PoetSuggestion suggestion);

    IPage<PoetSuggestion> getSuggestionList(int pageNum, int pageSize, String status);

    void reviewSuggestion(Long id, String status, String reviewComment, Long reviewerId);
}
