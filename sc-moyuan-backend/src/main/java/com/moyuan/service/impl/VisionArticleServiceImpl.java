package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.common.ResultCode;
import com.moyuan.entity.VisionArticle;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.VisionArticleMapper;
import com.moyuan.service.VisionArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class VisionArticleServiceImpl extends ServiceImpl<VisionArticleMapper, VisionArticle> implements VisionArticleService {

    private final VisionArticleMapper visionArticleMapper;

    @Override
    public IPage<VisionArticle> getArticleList(int pageNum, int pageSize, String category) {
        LambdaQueryWrapper<VisionArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VisionArticle::getStatus, 1);
        if (StringUtils.hasText(category)) {
            wrapper.eq(VisionArticle::getCategory, category);
        }
        wrapper.orderByAsc(VisionArticle::getSortOrder).orderByDesc(VisionArticle::getCreateTime);
        return visionArticleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public IPage<VisionArticle> getFeaturedArticles(int pageNum, int pageSize) {
        LambdaQueryWrapper<VisionArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VisionArticle::getStatus, 1)
               .eq(VisionArticle::getIsFeatured, 1)
               .orderByAsc(VisionArticle::getSortOrder);
        return visionArticleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public VisionArticle getArticleDetail(Long id) {
        VisionArticle article = visionArticleMapper.selectById(id);
        if (article == null || article.getStatus() != 1) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_FOUND);
        }
        visionArticleMapper.update(null, new LambdaUpdateWrapper<VisionArticle>()
                .eq(VisionArticle::getId, id)
                .setSql("view_count = view_count + 1"));
        article.setViewCount(article.getViewCount() + 1);
        return article;
    }

    @Override
    public void toggleLike(Long id) {
        VisionArticle article = visionArticleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_FOUND);
        }
        visionArticleMapper.update(null, new LambdaUpdateWrapper<VisionArticle>()
                .eq(VisionArticle::getId, id)
                .setSql("like_count = like_count + 1"));
    }
}
