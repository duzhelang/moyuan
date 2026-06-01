package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.VisionArticle;

public interface VisionArticleService extends IService<VisionArticle> {
    IPage<VisionArticle> getArticleList(int pageNum, int pageSize, String category);
    IPage<VisionArticle> getFeaturedArticles(int pageNum, int pageSize);
    VisionArticle getArticleDetail(Long id);
    void toggleLike(Long id);
}
