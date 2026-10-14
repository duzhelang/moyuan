package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.VisionArticle;

public interface VisionArticleService extends IService<VisionArticle> {
    IPage<VisionArticle> getArticleList(int pageNum, int pageSize, String category);
    IPage<VisionArticle> getFeaturedArticles(int pageNum, int pageSize);
    VisionArticle getArticleDetail(Long id);
    void toggleLike(Long id);

    /**
     * 管理端分页查询文章列表（含草稿，与公开接口仅查已发布不同）
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param status   状态筛选（0-下架/草稿，1-已发布，可选）
     * @param category 分类筛选（可选）
     * @param keyword  关键字（匹配标题或作者，可选）
     * @return 分页结果
     */
    IPage<VisionArticle> getAdminArticleList(int pageNum, int pageSize, Integer status, String category, String keyword);
}
