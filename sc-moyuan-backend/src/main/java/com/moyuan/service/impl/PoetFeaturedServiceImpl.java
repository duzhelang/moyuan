package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.entity.PoetFeatured;
import com.moyuan.mapper.PoetFeaturedMapper;
import com.moyuan.service.PoetFeaturedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PoetFeaturedServiceImpl extends ServiceImpl<PoetFeaturedMapper, PoetFeatured> implements PoetFeaturedService {

    private final PoetFeaturedMapper poetFeaturedMapper;

    @Override
    public IPage<PoetFeatured> getPoetFeaturedList(int pageNum, int pageSize) {
        LambdaQueryWrapper<PoetFeatured> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PoetFeatured::getStatus, 1)
               .orderByAsc(PoetFeatured::getSortOrder)
               .orderByDesc(PoetFeatured::getCreateTime);
        return poetFeaturedMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<PoetFeatured> getRandomPoetFeatured(int count) {
        LambdaQueryWrapper<PoetFeatured> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PoetFeatured::getStatus, 1);
        List<PoetFeatured> allList = poetFeaturedMapper.selectList(wrapper);
        if (allList.size() <= count) {
            return allList;
        }
        Collections.shuffle(allList);
        return new ArrayList<>(allList.subList(0, count));
    }

    @Override
    public PoetFeatured getPoetFeaturedDetail(Long id) {
        return poetFeaturedMapper.selectById(id);
    }
}