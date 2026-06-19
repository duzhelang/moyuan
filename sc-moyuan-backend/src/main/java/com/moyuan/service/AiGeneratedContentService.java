package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.AiGeneratedContent;

public interface AiGeneratedContentService extends IService<AiGeneratedContent> {

    String generatePreview(String targetType, Long targetId, String fieldName);

    AiGeneratedContent submitForReview(String targetType, Long targetId, String fieldName, String content);

    AiGeneratedContent generateContent(String targetType, Long targetId, String fieldName);

    IPage<AiGeneratedContent> listByStatus(Integer status, int page, int size);

    void approve(Long id, Long reviewerId, String reviewComment);

    void reject(Long id, Long reviewerId, String reviewComment);
}
