package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.Poem;

public interface PoemService extends IService<Poem> {
    IPage<Poem> getPoemList(int pageNum, int pageSize, Long dynastyId, Long poetId, Long categoryId, String keyword);
    Poem getPoemDetail(Long id);
    void toggleLike(Long userId, Long poemId);
    boolean isLike(Long userId, Long poemId);
    IPage<Poem> getFavorites(Long userId, int pageNum, int pageSize);
}
