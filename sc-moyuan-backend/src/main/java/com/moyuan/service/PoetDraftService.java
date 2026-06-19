package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.entity.PoetDraft;

public interface PoetDraftService {

    void saveDraft(PoetDraft draft);

    IPage<PoetDraft> getDraftList(int pageNum, int pageSize, Long poetId, Integer status);

    void reviewDraft(Long id, Integer status, String reviewComment, Long reviewerId);

    PoetDraft getDraftById(Long id);

    long countByStatus(Integer status);
}