package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.PoetFeatured;
import java.util.List;

public interface PoetFeaturedService extends IService<PoetFeatured> {
    IPage<PoetFeatured> getPoetFeaturedList(int pageNum, int pageSize);
    List<PoetFeatured> getRandomPoetFeatured(int count);
    PoetFeatured getPoetFeaturedDetail(Long id);
}